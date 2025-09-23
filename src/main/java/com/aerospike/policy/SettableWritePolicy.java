package com.aerospike.policy;

import java.util.Objects;

import com.aerospike.client.policy.WritePolicy;
import com.aerospike.policy.Behavior.CommandType;

public class SettableWritePolicy extends SettablePolicy {
    private Boolean durableDelete;      // durableDelete
    
    public static class Builder extends SettablePolicy.BuilderBase<Builder> {
        public Builder(BehaviorBuilder builder, CommandType type) {
            super(builder, type, new SettableWritePolicy());
        }
        
        public SettableWritePolicy.Builder useDurableDelete(boolean useDurableDelete) {
            getPolicy().durableDelete = useDurableDelete;
            return this;
        }
        
        public SettableWritePolicy getPolicy() {
            return ((SettableWritePolicy)policy);
        }
    }
    
    protected void mergeFrom(SettableWritePolicy thisPolicy) {
        if (thisPolicy == null) {
            return;
        }
        if (this.durableDelete == null) {
            this.durableDelete = thisPolicy.durableDelete;
        }
        super.mergeFrom(thisPolicy);
    }
    
    protected WritePolicy formPolicy(WritePolicy policy) {
        if (durableDelete != null) {
            policy.durableDelete = this.durableDelete;
        }
        super.formPolicy(policy);
        return policy;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(durableDelete);
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
        SettableWritePolicy other = (SettableWritePolicy) obj;
        return Objects.equals(durableDelete, other.durableDelete);
    }
}