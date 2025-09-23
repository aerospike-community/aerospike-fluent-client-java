package com.aerospike.policy;

import java.time.Duration;
import java.util.Objects;

import com.aerospike.client.policy.InfoPolicy;
import com.aerospike.policy.Behavior.CommandType;

public class SettableInfoPolicy extends SettablePolicy {
    public static class Builder {
        private final BehaviorBuilder builder;
        private SettableInfoPolicy policy;
        
        public Builder(BehaviorBuilder builder, CommandType type) {
            this.builder = builder; 
            this.policy = new SettableInfoPolicy();
        }
        
        public SettableInfoPolicy.Builder abandonCallAfter(Duration duration) {
            long value = duration.toMillis();
            int intValue = value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)value;
            if (intValue < 0) {
                throw new IllegalArgumentException("Duration parameter to abandonCallAfter must reflect a positive time");
            }
            policy.abandonCallAfter = intValue;
            return this;
        }
        
        public BehaviorBuilder done() {
            builder.setPolicy(CommandType.INFO, policy);
            return builder;
        }
        
        public SettableInfoPolicy getPolicy() {
            return policy;
        }
    }
    
    protected void mergeFrom(SettableInfoPolicy thisPolicy) {
        if (thisPolicy == null) {
            return;
        }
        if (this.abandonCallAfter == null) {
            this.abandonCallAfter = thisPolicy.abandonCallAfter;
        }
    }
    
    protected InfoPolicy formPolicy(InfoPolicy policy) {
        if (abandonCallAfter != null) {
            policy.timeout = this.abandonCallAfter;
        }
        return policy;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(abandonCallAfter);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        SettableInfoPolicy other = (SettableInfoPolicy)obj;
        return Objects.equals(abandonCallAfter, other.abandonCallAfter);
    }
}