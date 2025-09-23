package com.aerospike.policy;

import java.util.Objects;

import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.ReadModeSC;
import com.aerospike.policy.Behavior.CommandType;

public class SettableConsistencyModeReadPolicy extends SettablePolicy {
    private ReadModeSC readConsistency;                 // readModeSC
    
    public static class Builder extends SettablePolicy.BuilderBase<Builder> {
        public Builder(BehaviorBuilder builder, CommandType type) {
            super(builder, type, new SettableConsistencyModeReadPolicy());
        }
        public SettableConsistencyModeReadPolicy.Builder readConsistency(ReadModeSC readMode) {
            getPolicy().readConsistency = readMode;
            return this;
        }
        
        public SettableConsistencyModeReadPolicy getPolicy() {
            return ((SettableConsistencyModeReadPolicy)policy);
        }
    }
    protected void mergeFrom(SettableConsistencyModeReadPolicy thisPolicy) {
        if (thisPolicy == null) {
            return;
        }
        if (this.readConsistency == null) {
            this.readConsistency = thisPolicy.readConsistency;
        }
        super.mergeFrom(thisPolicy);
    }
    
    protected Policy formPolicy(Policy policy) {
        if (readConsistency != null) {
            policy.readModeSC = this.readConsistency;
        }
        super.formPolicy(policy);
        return policy;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(readConsistency);
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
        SettableConsistencyModeReadPolicy other = (SettableConsistencyModeReadPolicy) obj;
        return readConsistency == other.readConsistency;
    }

    
}