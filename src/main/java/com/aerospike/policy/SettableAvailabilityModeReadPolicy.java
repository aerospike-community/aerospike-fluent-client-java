package com.aerospike.policy;

import java.util.Objects;

import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.ReadModeAP;
import com.aerospike.policy.Behavior.CommandType;

public class SettableAvailabilityModeReadPolicy extends SettablePolicy {
    private ReadModeAP migrationReadConsistency;        // readModeAP 

    public static class Builder extends SettablePolicy.BuilderBase<Builder> {
        public Builder(BehaviorBuilder builder, CommandType type) {
            super(builder, type, new SettableAvailabilityModeReadPolicy());
        }

        public SettableAvailabilityModeReadPolicy.Builder migrationReadConsistency(ReadModeAP migrationReadConsistency) {
            getPolicy().migrationReadConsistency = migrationReadConsistency;
            return this;
        }
        
        public SettableAvailabilityModeReadPolicy getPolicy() {
            return ((SettableAvailabilityModeReadPolicy)policy);
        }
    }
    
    protected void mergeFrom(SettableAvailabilityModeReadPolicy thisPolicy) {
        if (thisPolicy == null) {
            return;
        }
        if (this.migrationReadConsistency == null) {
            this.migrationReadConsistency = thisPolicy.migrationReadConsistency;
        }
        super.mergeFrom(thisPolicy);
    }
    
    protected Policy formPolicy(Policy policy) {
        if (migrationReadConsistency != null) {
            policy.readModeAP = this.migrationReadConsistency;
        }
        super.formPolicy(policy);
        return policy;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(migrationReadConsistency);
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
        SettableAvailabilityModeReadPolicy other = (SettableAvailabilityModeReadPolicy) obj;
        return migrationReadConsistency == other.migrationReadConsistency;
    }

    
}