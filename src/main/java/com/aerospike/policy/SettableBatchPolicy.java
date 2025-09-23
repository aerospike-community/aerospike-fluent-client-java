package com.aerospike.policy;

import java.util.Objects;

import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.policy.Behavior.CommandType;

public class SettableBatchPolicy extends SettablePolicy {
    private Boolean allowInlineMemoryAccess;            // AllowInline
    private Boolean allowInlineSsdAccess;               // AllowInlineSSD
    private Integer maxConcurrentServers;               // maxConcurrentNodes
    
    public static class Builder extends SettablePolicy.BuilderBase<Builder> {
        public Builder(BehaviorBuilder builder, CommandType type) {
            super(builder, type, new SettableBatchPolicy());
        }
        public SettableBatchPolicy.Builder maxConcurrentServers(int value) {
            checkMinValue(value, 0, "max concurrent servers");
            getPolicy().maxConcurrentServers = value;
            return this;
        }
        
        public SettableBatchPolicy.Builder allowInlineMemoryAccess(boolean allow) {
            getPolicy().allowInlineMemoryAccess = allow;
            return this;
        }
        public SettableBatchPolicy.Builder allowInlineSsdAccess(boolean allow) {
            getPolicy().allowInlineSsdAccess = allow;
            return this;
        }
        
        public SettableBatchPolicy getPolicy() {
            return ((SettableBatchPolicy)policy);
        }
        
    }
    protected void mergeFrom(SettableBatchPolicy thisPolicy) {
        if (thisPolicy == null) {
            return;
        }
        if (this.allowInlineMemoryAccess == null) {
            this.allowInlineMemoryAccess = thisPolicy.allowInlineMemoryAccess;
        }
        if (this.allowInlineSsdAccess == null) {
            this.allowInlineSsdAccess = thisPolicy.allowInlineSsdAccess;
        }
        if (this.maxConcurrentServers == null) {
            this.maxConcurrentServers = thisPolicy.maxConcurrentServers;
        }
        super.mergeFrom(thisPolicy);
    }
    
    protected BatchPolicy formPolicy(BatchPolicy policy) {
        if (allowInlineMemoryAccess != null) {
            policy.allowInline = allowInlineMemoryAccess;
        }
        if (allowInlineSsdAccess != null) {
            policy.allowInlineSSD = allowInlineSsdAccess;
        }
        if (maxConcurrentServers != null) {
            policy.maxConcurrentThreads = maxConcurrentServers;
        }
        super.formPolicy(policy);
        return policy;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + 
                Objects.hash(allowInlineMemoryAccess, allowInlineSsdAccess, maxConcurrentServers);
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
        SettableBatchPolicy other = (SettableBatchPolicy) obj;
        return Objects.equals(allowInlineMemoryAccess, other.allowInlineMemoryAccess)
                && Objects.equals(allowInlineSsdAccess, other.allowInlineSsdAccess)
                && Objects.equals(maxConcurrentServers, other.maxConcurrentServers);
    }
    
    
}