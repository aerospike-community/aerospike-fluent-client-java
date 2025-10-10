package com.aerospike.query;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.aerospike.RecordStream;
import com.aerospike.Session;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Log;
import com.aerospike.client.ResultCode;
import com.aerospike.client.exp.Exp;
import com.aerospike.client.exp.Expression;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.dsl.ParseResult;
import com.aerospike.policy.Behavior.CommandType;

class BatchKeyQueryBuilderImpl extends QueryImpl {
    private final List<Key> keyList;
    public BatchKeyQueryBuilderImpl(QueryBuilder builder, Session session, List<Key> keyList) {
        super(builder, session);
        this.keyList = keyList;
    }

    @Override
    public RecordStream execute() {
        Key[] keys;
        if (keyList.size() == 0) {
            return new RecordStream();
        }
        Expression whereExp = null;
        if (getQueryBuilder().dslString != null) {
            ParseResult parseResult = this.getParseResultFromWhereClause(getQueryBuilder().dslString, this.keyList.get(0).namespace, false);
            whereExp = Exp.build(parseResult.getExp());
        }

        BatchPolicy policy = getSession().getBehavior().getMutablePolicy(CommandType.BATCH_READ);
        policy.filterExp = whereExp;

        long limit = 0;
        
        // TODO: Make this work with partition filter and respondAllops
        if (whereExp != null) {
            // We cannot use the limit here as we don't know how many records will match.
            if (hasPartitionFilter()) {
                keys = keyList.stream()
                        .filter(getQueryBuilder()::isKeyInPartitionRange)
                        .toArray(Key[]::new);
            }
            else {
                keys = keyList.toArray(new Key[0]);
            }
            limit = getQueryBuilder().getLimit();
        }
        else if (hasPartitionFilter()) {
            // The user has set partition limits on this range, pick only the records in the partition range
            keys = keyList.stream()
                    .filter(getQueryBuilder()::isKeyInPartitionRange)
                    .limit(getQueryBuilder().getLimit())
                    .toArray(Key[]::new);
        }
        else if (getQueryBuilder().getLimit() > 0) {
            keys = keyList.subList(0, (int)getQueryBuilder().getLimit()).toArray(new Key[0]);
        }
        else {
            keys = keyList.toArray(new Key[0]);
        }

        policy.setTxn(this.getQueryBuilder().getTxnToUse());
        policy.failOnFilteredOut = this.getQueryBuilder().isFailOnFilteredOut();
        try {
            if (getQueryBuilder().getWithNoBins()) {
                return new RecordStream(keys, 
                        getSession().getClient().getHeader(policy, keys), 
                        limit,
                        getQueryBuilder().getPageSize(),
                        getQueryBuilder().getSortInfo(),
                        this.getQueryBuilder().isRespondAllKeys());
            }
            else {
                return new RecordStream(keys, 
                        getSession().getClient().get(policy, keys, getQueryBuilder().getBinNames()), 
                        limit,
                        getQueryBuilder().getPageSize(),
                        getQueryBuilder().getSortInfo(),
                        this.getQueryBuilder().isRespondAllKeys());
            }
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