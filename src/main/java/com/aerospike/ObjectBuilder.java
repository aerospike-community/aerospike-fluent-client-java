package com.aerospike;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.aerospike.client.BatchRecord;
import com.aerospike.client.BatchWrite;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.BatchWritePolicy;
import com.aerospike.client.policy.GenerationPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.policy.Behavior.CommandType;

public class ObjectBuilder<T> {
    private final OperationObjectBuilder<T> opBuilder;
    private final List<T> elements;
    private RecordMapper<T> recordMapper;
    private int generation = -1;
    private Map<Integer, WritePolicy> customPolicies;
    // TODO: Need to store generation, WriteCommitLevel and expiration per record(?)
    
    
    public ObjectBuilder(OperationObjectBuilder<T> opBuilder, List<T> elements) {
        this.opBuilder = opBuilder;
        this.elements = elements;
    }
    
    public ObjectBuilder(OperationObjectBuilder<T> opBuilder, T element) {
        this.opBuilder = opBuilder;
        this.elements = List.of(element);
    }
    
    public ObjectBuilder<T> using(RecordMapper<T> recordMapper) {
        if (recordMapper == null) {
            throw new NullPointerException("recordMapper parameter to 'using' call cannot be 'null'");
        }
        this.recordMapper = recordMapper;
        return this;
    }
    
    public ObjectBuilder<T> ensureGenerationIs(int generation) {
        this.generation = generation;
        return this;
    }
    /**
     * Get the record mapper for the given element. This could be from either an
     * explicitly set mapper or from the mapper factory on the connection.
     * @param element
     * @return
     */
    private RecordMapper<T> getMapper(T element) {
        if (this.recordMapper != null) {
            return this.recordMapper;
        }
        else {
            RecordMappingFactory factory = opBuilder.getSession().getRecordMappingFactory();
            if (factory != null) {
                RecordMapper<T> mapper = (RecordMapper<T>)factory.getMapper(element.getClass());
                if (mapper != null) {
                    return mapper;
                }
            }
        }
        throw new UnsupportedOperationException(String.format(
                "Could not find a mapper to convert objects of type %s. Did you specify a RcordMappingFactory on the connection?",
                element.getClass().getName()));
    }
    
    private Operation[] operationsForElement(RecordMapper mapper, T element) {
        Map<String, Value> map = mapper.toMap(element);
        Operation[] operations = new Operation[map.size()];
        int i = 0;
        for (String key : map.keySet()) {
            Value binData = map.get(key);
            operations[i++] = Operation.put(new Bin(key, binData ));
        }
        return operations;
    }
    
    private Key getKeyForElement(RecordMapper<T> mapper, T element) {
        Object id = mapper.id(element);
        return this.opBuilder.getDataSet().idForObject(id);
    }
    
    private RecordStream executeSingle(T element) {
        RecordMapper<T> recordMapper = getMapper(element);
        Key key = getKeyForElement(recordMapper, element); 
        Operation[] operations = operationsForElement(recordMapper, element);
        CommandType type = OperationBuilder.areOperationsRetryable(operations) ? CommandType.WRITE_RETRYABLE : CommandType.WRITE_NON_RETRYABLE;
        WritePolicy wp = this.opBuilder.getSession().getBehavior().getSharedPolicy(type);
        wp.txn = this.opBuilder.getSession().getCurrentTransaction();
        if (generation >= 0) {
            wp.generation = generation;
            wp.generationPolicy = GenerationPolicy.EXPECT_GEN_EQUAL;
        }
        Record record = this.opBuilder.getSession().getClient().operate(
                wp,
                key,
                operations
            );
                
        return new RecordStream(key, record);
    }
    
    public RecordStream execute() {
        if (elements.size() == 1) {
            return executeSingle(elements.get(0));
        }
        
        int size = elements.size();
        List<BatchRecord> batchWrites = new ArrayList<>();

        BatchPolicy batchPolicy = this.opBuilder.getSession().getBehavior().getMutablePolicy(CommandType.BATCH_WRITE);

        BatchWritePolicy bwp = new BatchWritePolicy();
        bwp.sendKey = batchPolicy.sendKey;
        for (T element : elements) {
            RecordMapper<T> recordMapper = getMapper(element);
            Key key = getKeyForElement(recordMapper, element);
            Operation[] operations = operationsForElement(recordMapper, element);
            batchWrites.add(new BatchWrite(bwp, key, operations));
        }

        batchPolicy.setTxn(this.opBuilder.getSession().getCurrentTransaction());
        
        this.opBuilder.getSession().getClient().operate(
                batchPolicy,
                batchWrites);
        
        Key[] keys = new Key[size];
        Record[] records = new Record[size];
        // TODO: What do we do about missing records?
        for (int i = 0; i < size; i++) {
            keys[i] = batchWrites.get(i).key;
            records[i] = batchWrites.get(i).record;
        }
        // TODO: consider limit here
        return new RecordStream(keys, records,0 , 0, null);
    }
}
