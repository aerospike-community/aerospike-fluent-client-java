package com.aerospike.query;

import java.util.Set;
import java.util.stream.Collectors;

import com.aerospike.RecordStream;
import com.aerospike.Session;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Log;
import com.aerospike.client.ResultCode;
import com.aerospike.client.policy.Policy;
import com.aerospike.policy.Behavior.CommandType;

class SingleKeyQueryBuilderImpl extends QueryImpl {
    private final Key key;
    public SingleKeyQueryBuilderImpl(QueryBuilder builder, Session session, Key key) {
        super(builder, session);
        this.key = key;
    }
    
    // No need to implement limit on single read
    @Override
    public RecordStream execute() {
        Policy policy = getSession().getBehavior().getMutablePolicy(CommandType.READ_SC);
        policy.txn = this.getQueryBuilder().getTxnToUse();
        if (!getQueryBuilder().isKeyInPartitionRange(key)) {
            return new RecordStream();
        }
        try {
            if (getQueryBuilder().getWithNoBins()) {
                return new RecordStream(key, getSession().getClient().getHeader(policy, key));
            }
            else {
                return new RecordStream(key, getSession().getClient().get(policy, key, getQueryBuilder().getBinNames()));
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