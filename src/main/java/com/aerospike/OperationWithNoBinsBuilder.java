package com.aerospike;

import java.util.ArrayList;
import java.util.List;

import com.aerospike.client.BatchRecord;
import com.aerospike.client.BatchResults;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.ResultCode;
import com.aerospike.client.policy.BatchDeletePolicy;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.BatchWritePolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.policy.Behavior.OpKind;
import com.aerospike.policy.Behavior.OpShape;

public class OperationWithNoBinsBuilder {
    private final List<Key> keys;
    private final Key key;
    private final OpType opType;
    private final Session session;
    
    public OperationWithNoBinsBuilder(Session session, Key key, OpType type) {
        this.keys = null;
        this.key = key;
        this.opType = type;
        this.session = session;
    }
    
    public OperationWithNoBinsBuilder(Session session, List<Key> keys, OpType type) {
        if (keys.size() == 1) {
            this.keys = null;
            this.key = keys.get(0);
        }
        else {
            this.key = null;
            this.keys = keys;
        }
        this.opType = type;
        this.session = session;
    }
    
    private Key getAnyKey() {
        if (key != null) {
            return key;
        }
        else {
            return keys.get(0);
        }
    }
    private List<Boolean> toList(boolean[] booleanArray) {
        List<Boolean> results = new ArrayList<>();
        for (int i = 0; i < booleanArray.length; i++) {
            results.add(booleanArray[i]);
        }
        return results;
    }
    
    private List<Boolean> batchExecute(WritePolicy wp) {
        // TODO: Exceptions!!
        
        switch (opType) {
        case EXISTS: {
            BatchPolicy batchPolicy = session.getBehavior()
                    .getSettings(OpKind.READ, OpShape.BATCH, session.isNamespaceSC(getAnyKey().namespace))
                    .asBatchPolicy();
            boolean[] results = session.getClient().exists(batchPolicy, keys.toArray(new Key[0]));
            return toList(results);
        }
        
        case TOUCH: {
            BatchPolicy batchPolicy = session.getBehavior()
                    .getSettings(OpKind.WRITE_RETRYABLE, OpShape.BATCH, session.isNamespaceSC(keys.get(0).namespace))
                    .asBatchPolicy();
            BatchWritePolicy batchWritePolicy = new BatchWritePolicy();
            batchWritePolicy.sendKey = batchPolicy.sendKey;
            
            BatchResults results = session.getClient().operate(batchPolicy, batchWritePolicy, keys.toArray(Key[]::new), Operation.touch());
            List<Boolean> booleanArray = new ArrayList<>();
            for (BatchRecord record : results.records) {
                booleanArray.add(record.resultCode == ResultCode.OK);
            }
            return booleanArray;
        }
            
        case DELETE: {
            BatchPolicy batchPolicy = session.getBehavior()
                    .getSettings(OpKind.WRITE_RETRYABLE, OpShape.BATCH, session.isNamespaceSC(getAnyKey().namespace))
                    .asBatchPolicy();

            BatchDeletePolicy batchDeletePolicy = new BatchDeletePolicy();
            batchDeletePolicy.sendKey = batchPolicy.sendKey;
            BatchResults results = session.getClient().delete(batchPolicy, batchDeletePolicy, keys.toArray(Key[]::new));
            List<Boolean> booleanArray = new ArrayList<>();
            for (BatchRecord record : results.records) {
                booleanArray.add(record.resultCode == ResultCode.OK);
            }
            return booleanArray;
        }
        default:
            throw new IllegalStateException("received an action of " + opType + " which should be handled elsewhere");
        }
    }

    public List<Boolean> execute() {
        WritePolicy wp = session.getBehavior()
                .getSettings(OpKind.WRITE_RETRYABLE, OpShape.POINT, session.isNamespaceSC(getAnyKey().namespace))
                .asWritePolicy();
        if (key != null) {
            boolean result;
            switch (opType) {
            case EXISTS:
                result = session.getClient().exists(wp, key);
                break;
            case TOUCH:
                result = session.getClient().touched(wp, key);
                break;
            case DELETE:
                result = session.getClient().delete(wp, key);
                break;
            default:
                throw new IllegalStateException("received an action of " + opType + " which should be handled elsewhere");
            }
            return List.of(result);
        }
        else {
            return batchExecute(wp);
        }
    }
}
