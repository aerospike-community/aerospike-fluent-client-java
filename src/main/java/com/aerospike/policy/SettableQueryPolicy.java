package com.aerospike.policy;

import java.util.Objects;

import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.policy.Behavior.CommandType;
import com.aerospike.policy.SettableWritePolicy.Builder;

public class SettableQueryPolicy extends SettablePolicy {
    private Integer recordQueueSize;
    private Integer maxConcurrentServers;
    
    public static class Builder extends SettablePolicy.BuilderBase<Builder> {
        public Builder(BehaviorBuilder builder, CommandType type) {
            super(builder, type, new SettableQueryPolicy());
        }
        
        public SettableQueryPolicy.Builder recordQueueSize(int queueSize) {
            getPolicy().recordQueueSize = queueSize;
            return this;
        }
        public SettableQueryPolicy.Builder maxConcurrentServers(int value) {
            checkMinValue(value, 0, "max concurrent servers");
            getPolicy().maxConcurrentServers = value;
            return this;
        }
        
        public SettableQueryPolicy getPolicy() {
            return ((SettableQueryPolicy)policy);
        }
    }
    protected void mergeFrom(SettableQueryPolicy thisPolicy) {
        if (thisPolicy == null) {
            return;
        }
        if (this.recordQueueSize == null) {
            this.recordQueueSize = thisPolicy.recordQueueSize;
        }
        if (this.maxConcurrentServers == null) {
            maxConcurrentServers = thisPolicy.maxConcurrentServers;
        }
        super.mergeFrom(thisPolicy);
    }
    
    protected QueryPolicy formPolicy(QueryPolicy policy) {
        if (recordQueueSize != null) {
            policy.recordQueueSize = this.recordQueueSize;
        }
        if (maxConcurrentServers != null) {
            policy.maxConcurrentNodes = this.maxConcurrentServers;
        }
        super.formPolicy(policy);
        return policy;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(maxConcurrentServers, recordQueueSize);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        SettableQueryPolicy other = (SettableQueryPolicy) obj;
        return Objects.equals(maxConcurrentServers, other.maxConcurrentServers)
                && Objects.equals(recordQueueSize, other.recordQueueSize);
    }


}