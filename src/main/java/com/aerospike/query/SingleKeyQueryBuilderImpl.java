package com.aerospike.query;

import com.aerospike.RecordStream;
import com.aerospike.Session;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Log;
import com.aerospike.client.ResultCode;
import com.aerospike.client.policy.Policy;
import com.aerospike.policy.Behavior.OpKind;
import com.aerospike.policy.Behavior.OpShape;
import com.aerospike.policy.Behavior.Mode;

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
        boolean isNamespaceSC = getSession().isNamespaceSC(this.key.namespace);
        Policy policy = getSession().getBehavior().getSettings(OpKind.READ, OpShape.POINT, isNamespaceSC ? Mode.CP : Mode.AP).asReadPolicy();
        policy.txn = this.getQueryBuilder().getTxnToUse();
        policy.failOnFilteredOut = this.getQueryBuilder().isFailOnFilteredOut();
        if (!getQueryBuilder().isKeyInPartitionRange(key)) {
            if (this.getQueryBuilder().isRespondAllKeys()) {
                return new RecordStream(key, null, true);
            }
            return new RecordStream();
        }
        try {
            if (getQueryBuilder().getWithNoBins()) {
                return new RecordStream(key, getSession().getClient().getHeader(policy, key), this.getQueryBuilder().isRespondAllKeys());
            }
            else {
                return new RecordStream(key, 
                        getSession().getClient().get(policy, key, getQueryBuilder().getBinNames()),
                        this.getQueryBuilder().isRespondAllKeys()
                    );
            }
        }
        catch (AerospikeException ae) {
            if (Log.warnEnabled() && ae.getResultCode() == ResultCode.UNSUPPORTED_FEATURE) {
                if (this.getQueryBuilder().getTxnToUse() != null && !getSession().isNamespaceSC(key.namespace)) {
                    Log.warn(String.format("Namespace '%s' is involved in transaction, but it is not an SC namespace. "
                            + "This will throw an Unsupported Server Feature Exception.", key.namespace));
                }
            }
            throw ae;
        }
    }
}