package com.aerospike.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.aerospike.RecordStream;
import com.aerospike.Session;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.BatchRead;
import com.aerospike.client.BatchRecord;
import com.aerospike.client.Key;
import com.aerospike.client.Log;
import com.aerospike.client.ResultCode;
import com.aerospike.client.exp.Exp;
import com.aerospike.client.exp.Expression;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.dsl.ParseResult;
import com.aerospike.policy.Behavior.OpKind;
import com.aerospike.policy.Behavior.OpShape;
import com.aerospike.policy.Behavior.Mode;

class BatchKeyQueryBuilderImpl extends QueryImpl {
    private final List<Key> keyList;
    public BatchKeyQueryBuilderImpl(QueryBuilder builder, Session session, List<Key> keyList) {
        super(builder, session);
        this.keyList = keyList;
    }
    
    @Override
    public boolean allowsSecondaryIndexQuery() {
        return false;
    }

    @Override
    public RecordStream execute() {
        // Query default: async unless in transaction
        if (getQueryBuilder().getTxnToUse() != null) {
            return executeSync();
        } else {
            return executeAsync();
        }
    }
    
    @Override
    public RecordStream executeSync() {
        return executeInternal();
    }
    
    @Override
    public RecordStream executeAsync() {
        if (getQueryBuilder().getTxnToUse() != null && Log.warnEnabled()) {
            Log.warn(
                "executeAsync() called within a transaction. " +
                "Async operations may still be in flight when commit() is called, " +
                "which could lead to inconsistent state. " +
                "Consider using executeSync() or execute() for transactional safety."
            );
        }
        // For batch operations, async and sync are effectively the same
        // since we need to wait for the batch to complete anyway
        return executeInternal();
    }
    
    private RecordStream executeInternal() {
        if (keyList.size() == 0) {
            return new RecordStream();
        }
        Expression whereExp = null;
        if (getQueryBuilder().getDsl() != null) {
            ParseResult parseResult = getQueryBuilder().getDsl().process(this.keyList.get(0).namespace, getSession());
            whereExp = Exp.build(parseResult.getExp());
        }
        
        long limit = getQueryBuilder().getLimit();
        List<BatchRecord> batchRecords = new ArrayList<>();
        List<BatchRecord> batchRecordsForServer = hasPartitionFilter() ? new ArrayList<>() : batchRecords;
        
        for (Key thisKey : keyList) {
            // If there is no "where" clause and the limit has been exceeded, exit the loop
            if (whereExp == null && limit > 0 && batchRecords.size() >= limit) {
                break;
            }
            if (hasPartitionFilter() && !getQueryBuilder().isKeyInPartitionRange(thisKey)) {
                // We know this one will fail
                if (!getQueryBuilder().isRespondAllKeys()) {
                    // Filter it out
                    continue;
                }
                else {
                    // Need to include a record but do not send it to the server
                    batchRecords.add(new BatchRecord(thisKey, false));
                }
            }
            else {
                BatchRecord thisBatchRecord;
                if (getQueryBuilder().getWithNoBins()) {
                    thisBatchRecord = new BatchRead(thisKey, false);
                }
                else if (getQueryBuilder().getBinNames() != null) {
                    thisBatchRecord = new BatchRead(thisKey, getQueryBuilder().getBinNames());
                }
                else {
                    thisBatchRecord = new BatchRead(thisKey, true);
                }
                batchRecordsForServer.add(thisBatchRecord);
            }
        }

        boolean isNamespaceSC = getSession().isNamespaceSC(this.keyList.get(0).namespace);
        BatchPolicy policy = getSession().getBehavior().getSettings(OpKind.READ, OpShape.BATCH, isNamespaceSC ? Mode.CP : Mode.AP).asBatchPolicy();
        policy.filterExp = whereExp;
        policy.setTxn(this.getQueryBuilder().getTxnToUse());
        policy.failOnFilteredOut = this.getQueryBuilder().isFailOnFilteredOut();
        
        try {
            getSession().getClient().operate(policy, batchRecordsForServer);
            if (!getQueryBuilder().isRespondAllKeys()) {
                // Remove any items which have been filtered out.
                batchRecordsForServer.removeIf(br -> (br.resultCode == ResultCode.OK && br.record == null) 
                        || (br.resultCode == ResultCode.KEY_NOT_FOUND_ERROR)
                        || (br.resultCode == ResultCode.FILTERED_OUT && !getQueryBuilder().isFailOnFilteredOut()));
            }
            if (hasPartitionFilter()) {
                // Add the server results into any that were filtered out earlier
                batchRecords.addAll(batchRecordsForServer);
            }
            
            // TODO: ResultsInKeyOrder?
            return new RecordStream(batchRecords,
                    limit,
                    getQueryBuilder().getPageSize(),
                    getQueryBuilder().getSortInfo());
        }
        catch (AerospikeException ae) {
            if (Log.warnEnabled() && ae.getResultCode() == ResultCode.UNSUPPORTED_FEATURE) {
                if (this.getQueryBuilder().getTxnToUse() != null) {
                    Set<String> namespaces = keyList.stream().map(key->key.namespace).collect(Collectors.toSet());
                    namespaces.forEach(namespace -> {
                        if (!getSession().isNamespaceSC(namespace)) {
                            Log.warn(String.format("Namespace '%s' is involved in transaction, but it is not an SC namespace. "
                                    + "This will throw an Unsupported Server Feature exception.", namespace));
                        }

                    });
                }
            }
            throw ae;
        }
    }
}