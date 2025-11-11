package com.aerospike.query;

import com.aerospike.RecordResult;
import com.aerospike.RecordStream;
import com.aerospike.Session;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Log;
import com.aerospike.client.Record;
import com.aerospike.client.ResultCode;
import com.aerospike.client.policy.Policy;
import com.aerospike.exception.AeroException;
import com.aerospike.policy.Behavior.Mode;
import com.aerospike.policy.Behavior.OpKind;
import com.aerospike.policy.Behavior.OpShape;

class SingleKeyQueryBuilderImpl extends QueryImpl {
    private final Key key;
    public SingleKeyQueryBuilderImpl(QueryBuilder builder, Session session, Key key) {
        super(builder, session);
        this.key = key;
    }
    
    @Override
    public boolean allowsSecondaryIndexQuery() {
        return false;
    }
    // No need to implement limit on single read
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
        // Single key reads are fast; async and sync are effectively the same
        return executeInternal();
    }
    
    private RecordStream executeInternal() {
    	Session session = getSession();
        boolean isNamespaceSC = session.isNamespaceSC(this.key.namespace);
    	QueryBuilder qb = getQueryBuilder();
        boolean failOnFilteredOut = qb.isFailOnFilteredOut();

        Policy policy = getSession().getBehavior().getSettings(OpKind.READ, OpShape.POINT, isNamespaceSC ? Mode.CP : Mode.AP).asReadPolicy();
        policy.txn = qb.getTxnToUse();
        policy.failOnFilteredOut = failOnFilteredOut;
        if (!qb.isKeyInPartitionRange(key)) {
            if (qb.isRespondAllKeys()) {
                return new RecordStream(key, null);
            }
            return new RecordStream();
        }
        try {
            Record record;
            if (qb.getWithNoBins()) {
                record = session.getClient().getHeader(policy, key);
                //return new RecordStream(key, getSession().getClient().getHeader(policy, key), this.getQueryBuilder().isRespondAllKeys());
            }
            else {
                record = session.getClient().get(policy, key, qb.getBinNames());
//                return new RecordStream(key, 
//                        getSession().getClient().get(policy, key, getQueryBuilder().getBinNames()),
//                        this.getQueryBuilder().isRespondAllKeys()
//                    );
            }
            if (record != null || qb.isRespondAllKeys()) {
	        	return new RecordStream(key, record);
			}
			return new RecordStream();
        }
        catch (AerospikeException ae) {
            if (Log.warnEnabled() && ae.getResultCode() == ResultCode.UNSUPPORTED_FEATURE) {
                if (this.getQueryBuilder().getTxnToUse() != null && !getSession().isNamespaceSC(key.namespace)) {
                    Log.warn(String.format("Namespace '%s' is involved in transaction, but it is not an SC namespace. "
                            + "This will throw an Unsupported Server Feature Exception.", key.namespace));
                }
            }
            if (this.getQueryBuilder().shouldIncludeResult(0)) {
                return new RecordStream(new RecordResult(key, AeroException.from(ae), 0));
            }
            return new RecordStream();
        }
    }
}