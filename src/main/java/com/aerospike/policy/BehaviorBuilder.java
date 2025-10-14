package com.aerospike.policy;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

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
    
    // OLD PATTERN METHODS - Package-private for YAML loader use
    SettablePolicy.Builder forAllOperations() {
        return new SettablePolicy.Builder(this, CommandType.ALL, new SettablePolicy());
    }
    SettableConsistencyModeReadPolicy.Builder onConsistencyModeReads() {
        return new SettableConsistencyModeReadPolicy.Builder(this, CommandType.READ_SC);
    }
    SettableAvailabilityModeReadPolicy.Builder onAvailabilityModeReads() {
        return new SettableAvailabilityModeReadPolicy.Builder(this, CommandType.READ_AP);
    }
    SettableWritePolicy.Builder onRetryableWrites() {
        return new SettableWritePolicy.Builder(this, CommandType.WRITE_RETRYABLE);
    }
    SettableWritePolicy.Builder onNonRetryableWrites() {
        return new SettableWritePolicy.Builder(this, CommandType.WRITE_NON_RETRYABLE);
    }
    SettableBatchPolicy.Builder onBatchReads() {
        return new SettableBatchPolicy.Builder(this, CommandType.BATCH_READ);
    }
    SettableBatchPolicy.Builder onBatchWrites() {
        return new SettableBatchPolicy.Builder(this, CommandType.BATCH_WRITE);
    }
    SettableQueryPolicy.Builder onQuery() {
        return new SettableQueryPolicy.Builder(this, CommandType.QUERY);
    }
    SettableInfoPolicy.Builder onInfo() {
        return new SettableInfoPolicy.Builder(this, CommandType.INFO);
    }
    
    // NEW LAMBDA-BASED PUBLIC API
    public BehaviorBuilder forAllOperations(Consumer<SettablePolicy.Builder> configurator) {
        SettablePolicy.Builder builder = forAllOperations();
        configurator.accept(builder);
        return builder.done();
    }
    
    public BehaviorBuilder onConsistencyModeReads(Consumer<SettableConsistencyModeReadPolicy.Builder> configurator) {
        SettableConsistencyModeReadPolicy.Builder builder = onConsistencyModeReads();
        configurator.accept(builder);
        return builder.done();
    }
    
    public BehaviorBuilder onAvailabilityModeReads(Consumer<SettableAvailabilityModeReadPolicy.Builder> configurator) {
        SettableAvailabilityModeReadPolicy.Builder builder = onAvailabilityModeReads();
        configurator.accept(builder);
        return builder.done();
    }
    
    public BehaviorBuilder onRetryableWrites(Consumer<SettableWritePolicy.Builder> configurator) {
        SettableWritePolicy.Builder builder = onRetryableWrites();
        configurator.accept(builder);
        return builder.done();
    }
    
    public BehaviorBuilder onNonRetryableWrites(Consumer<SettableWritePolicy.Builder> configurator) {
        SettableWritePolicy.Builder builder = onNonRetryableWrites();
        configurator.accept(builder);
        return builder.done();
    }
    
    public BehaviorBuilder onBatchReads(Consumer<SettableBatchPolicy.Builder> configurator) {
        SettableBatchPolicy.Builder builder = onBatchReads();
        configurator.accept(builder);
        return builder.done();
    }
    
    public BehaviorBuilder onBatchWrites(Consumer<SettableBatchPolicy.Builder> configurator) {
        SettableBatchPolicy.Builder builder = onBatchWrites();
        configurator.accept(builder);
        return builder.done();
    }
    
    public BehaviorBuilder onQuery(Consumer<SettableQueryPolicy.Builder> configurator) {
        SettableQueryPolicy.Builder builder = onQuery();
        configurator.accept(builder);
        return builder.done();
    }
    
    public BehaviorBuilder onInfo(Consumer<SettableInfoPolicy.Builder> configurator) {
        SettableInfoPolicy.Builder builder = onInfo();
        configurator.accept(builder);
        return builder.done();
    }

}