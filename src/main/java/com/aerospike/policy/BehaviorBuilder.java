package com.aerospike.policy;

import java.util.HashMap;
import java.util.Map;

import com.aerospike.policy.Behavior.CommandType;

public class BehaviorBuilder {
    private Map<CommandType, SettablePolicy> policies = new HashMap<>();
    
    protected BehaviorBuilder setPolicy(CommandType type, SettablePolicy policy) {
        this.policies.put(type, policy);
        return this;
    }
    
    protected Map<CommandType, SettablePolicy> getPolicies() {
        return this.policies;
    }
    
    public SettablePolicy.Builder forAllOperations() {
        return new SettablePolicy.Builder(this, CommandType.ALL, new SettablePolicy());
    }
    public SettableConsistencyModeReadPolicy.Builder onConsistencyModeReads() {
        return new SettableConsistencyModeReadPolicy.Builder(this, CommandType.READ_SC);
    }
    public SettableAvailabilityModeReadPolicy.Builder onAvailablityModeReads() {
        return new SettableAvailabilityModeReadPolicy.Builder(this, CommandType.READ_AP);
    }
    public SettableWritePolicy.Builder onRetryableWrites() {
        return new SettableWritePolicy.Builder(this, CommandType.WRITE_RETRYABLE);
    }
    public SettableWritePolicy.Builder onNonRetryableWrites() {
        return new SettableWritePolicy.Builder(this, CommandType.WRITE_NON_RETRYABLE);
    }
    public SettableBatchPolicy.Builder onBatchReads() {
        return new SettableBatchPolicy.Builder(this, CommandType.BATCH_READ);
    }
    public SettableBatchPolicy.Builder onBatchWrites() {
        return new SettableBatchPolicy.Builder(this, CommandType.BATCH_WRITE);
    }
    public SettableQueryPolicy.Builder onQuery() {
        return new SettableQueryPolicy.Builder(this, CommandType.QUERY);
    }
    public SettableInfoPolicy.Builder onInfo() {
        return new SettableInfoPolicy.Builder(this, CommandType.INFO);
    }

}