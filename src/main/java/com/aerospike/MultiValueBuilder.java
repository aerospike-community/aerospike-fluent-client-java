package com.aerospike;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.BatchRecord;
import com.aerospike.client.BatchWrite;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.Txn;
import com.aerospike.client.Value;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.BatchWritePolicy;
import com.aerospike.client.policy.GenerationPolicy;
import com.aerospike.client.policy.WritePolicy;
// CommandType removed - using new Behavior API
import com.aerospike.policy.Behavior.OpKind;
import com.aerospike.policy.Behavior.OpShape;

/**
 * Builder for operations that can handle multiple sets of values for multiple keys.
 * Allows chaining multiple .values() calls before calling other methods.
 */
public class MultiValueBuilder {
    private static class RecordValues {
        protected Object[] values;
        protected int generation = 0;
        protected long expirationInSeconds = Long.MIN_VALUE;
        protected final Key key;
        
        public RecordValues(Key key, Object[] values) {
            this.key = key;
            this.values = values;
        }
    }
    
    private final Session session;
    private final List<Key> keys;
    private String[] binNames;
    private final List<RecordValues> valueSets = new ArrayList<>();
    private RecordValues currentValue = null;
    private final OpType opType;
    private boolean valuesFinalized = false;
    private Txn txnToUse = null;
    
    public MultiValueBuilder(Session session, List<Key> keys, OpType opType) {
        this.session = session;
        this.keys = keys;
        this.opType = opType;
        this.txnToUse = session.getCurrentTransaction();
    }
    
    protected int getNumKeys() {
        return keys.size();
    }

    /**
     * Set the bin names for this operation. This must be called before any .values() calls.
     * 
     * @param binName The first bin name
     * @param binNames Additional bin names
     * @return This builder for chaining
     */
    public MultiValueBuilder bins(String binName, String... binNames) {
        if (!valueSets.isEmpty()) {
            throw new IllegalStateException("Cannot call .bins() after calling .values()");
        }
        
        String[] allBinNames = new String[1 + binNames.length];
        allBinNames[0] = binName;
        System.arraycopy(binNames, 0, allBinNames, 1, binNames.length);
        
        // Update the bin names
        this.binNames = allBinNames;
        
        return this;
    }
    
    /**
     * Add a set of values for one record. The number of values must match the number of bins.
     * Multiple calls to this method can be chained together.
     * 
     * @param values The values for this record
     * @return This builder for chaining
     */
    public MultiValueBuilder values(Object... values) {
        if (valuesFinalized) {
            throw new IllegalStateException("Cannot call .values() after calling other methods like .expireRecordAfter()");
        }
        
        if (values.length != binNames.length) {
            throw new IllegalArgumentException(String.format(
                "When calling '.values(...)' to specify the values for multiple bins,"
                + " the number of values must match the number of bins specified in the '.bins(...)' call."
                + " This call specified %d bins, but supplied %d values.",
                binNames.length,
                values.length));
        }
        int index = valueSets.size();
        if (index >= keys.size()) {
            throw new IllegalArgumentException(String.format(
                    "The number of '.values(...)' calls must match the number of specified keys. There are only %d keys "
                    + "but more '.values(...) calls.",
                    keys.size()));
        }
        currentValue = new RecordValues(keys.get(index), values);
        valueSets.add(currentValue);
        
        return this;
    }
    
    // Delegate methods that finalize values and apply to all keys
    
    public MultiValueBuilder expireAllRecordsAfter(Duration duration) {
        valuesFinalized = true;
        long expirationInSeconds = duration.toSeconds();
        
        for (RecordValues thisValue : this.valueSets) {
            if (thisValue.expirationInSeconds == Long.MIN_VALUE) {
                thisValue.expirationInSeconds = expirationInSeconds;
            }
        }
        return this;
    }
    
    public MultiValueBuilder expireAllRecordsAfterSeconds(int expirationInSeconds) {
        valuesFinalized = true;
        
        for (RecordValues thisValue : this.valueSets) {
            if (thisValue.expirationInSeconds == Long.MIN_VALUE) {
                thisValue.expirationInSeconds = expirationInSeconds;
            }
        }
        return this;
    }
    
    public MultiValueBuilder expireAllRecordsAt(Date date) {
        valuesFinalized = true;
        
        long expirationInSeconds = (date.getTime() - new Date().getTime())/ 1000L;
        if (expirationInSeconds < 0) {
            throw new IllegalArgumentException("Expiration must be set in the future, not to " + date);
        }

        for (RecordValues thisValue : this.valueSets) {
            if (thisValue.expirationInSeconds == Long.MIN_VALUE) {
                thisValue.expirationInSeconds = expirationInSeconds;
            }
        }
        return this;
    }
    
    public MultiValueBuilder expireAllRecordsAt(LocalDateTime date) {
        valuesFinalized = true;
        
        LocalDateTime now = LocalDateTime.now();
        long expirationInSeconds = ChronoUnit.SECONDS.between(now, date);
        if (expirationInSeconds < 0) {
            throw new IllegalArgumentException("Expiration must be set in the future, not to " + date);
        }
        for (RecordValues thisValue : this.valueSets) {
            if (thisValue.expirationInSeconds == Long.MIN_VALUE) {
                thisValue.expirationInSeconds = expirationInSeconds;
            }
        }
        return this;
    }
    
    public MultiValueBuilder withNoChangeInExpiration() {
        valuesFinalized = true;
        
        for (RecordValues thisValue : this.valueSets) {
            if (thisValue.expirationInSeconds == Long.MIN_VALUE) {
                thisValue.expirationInSeconds = OperationBuilder.TTL_NO_CHANGE;
            }
        }
        
        return this;
    }
    
    public MultiValueBuilder neverExpire() {
        valuesFinalized = true;
        
        for (RecordValues thisValue : this.valueSets) {
            if (thisValue.expirationInSeconds == Long.MIN_VALUE) {
                thisValue.expirationInSeconds = OperationBuilder.TTL_NEVER_EXPIRE;
            }
        }
        
        return this;
    }
    
    public MultiValueBuilder expiryFromServerDefault() {
        valuesFinalized = true;
        
        for (RecordValues thisValue : this.valueSets) {
            if (thisValue.expirationInSeconds == Long.MIN_VALUE) {
                thisValue.expirationInSeconds = OperationBuilder.TTL_SERVER_DEFAULT;
            }
        }
        return this;
    }
    
    public MultiValueBuilder ensureGenerationIs(int generation) {
        if (this.currentValue == null) {
            throw new IllegalArgumentException("ensureGenerationIs(...) can only be called on the latest 'values(...)'. No values have been specified.");
        }
        this.currentValue.generation = generation;
        return this;
    }
    
    /**
     * Specify that these operations are not to be included in any transaction, even if a
     * transaction exists on the underlying session
     */
    public MultiValueBuilder notInAnyTransaction() {
        this.valuesFinalized = true;
        this.txnToUse = null;
        return this;
    }
    
    /**
     * Specify the transaction to use for this call. Note that this should not be commonly used.
     * A better pattern is to use the {@code doInTransaction} method on {@link Session}:
     * <pre>
     * session.doInTransaction(txnSession -> {
     *     Optional<KeyRecord> result = txnSession.query(customerDataSet.id(1)).execute().getFirst();
     *     // Do stuff...
     *     txnSession.insert(customerDataSet.id(3));
     *     txnSession.delete(customerDataSet.id(3));
     * });
     * </pre> 
     * 
     * This method should only be used in situations where different parts of a transaction are not all
     * within the same context, for example forming a transaction on callbacks from a file system. 
     * @param txn - the transaction to use
     */
    public MultiValueBuilder inTransaction(Txn txn) {
        this.valuesFinalized = true;
        this.txnToUse = txn;
        return this;
    }
    

    /**
     * Finalize the values and return an operation builder for the first key.
     * This method should be called after all .values() calls are complete.
     * 
     * @return A stream of the records returned from the database operations
     */
    public RecordStream execute() {
        if (valueSets.isEmpty()) {
            throw new IllegalStateException("No values have been specified. Call .values() at least once.");
        }
        
        if (valueSets.size() != keys.size()) {
            throw new IllegalArgumentException(String.format(
                "The number of value sets (%d) must match the number of keys (%d).",
                valueSets.size(),
                keys.size()));
        }
        
        if (valueSets.size() >= OperationBuilder.getBatchOperationThreshold()) {
            return executeViaBatch();
        }
        else {
            return executeViaSingleOperations();
        }
    }
    
    private int getExpiration(RecordValues values) {
        long expirationInSeconds = values.expirationInSeconds;
        if (expirationInSeconds == Long.MIN_VALUE) {
            // Hasn't been set, use the default
            expirationInSeconds = OperationBuilder.TTL_SERVER_DEFAULT;
        }
        if (expirationInSeconds > (long)Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        else {
            return (int) expirationInSeconds;
        }

    }
    private BatchWrite toBatchWrite(RecordValues values) {
        Operation[] ops = new Operation[binNames.length];
        for (int i = 0; i < binNames.length; i++) {
            ops[i] = Operation.add(new Bin(binNames[i], Value.get(values.values[i])));
        }
        BatchWritePolicy bwp = new BatchWritePolicy();
        bwp.recordExistsAction = OperationBuilder.recordExistsActionFromOpType(opType);
        bwp.expiration = getExpiration(values);
        int generation = values.generation;
        if (generation > 0) {
            bwp.generation = generation;
            bwp.generationPolicy = GenerationPolicy.EXPECT_GEN_EQUAL;
        }
        
        return new BatchWrite(values.key, ops);
    }
    
    private RecordStream executeViaBatch() {
        // TODO: In the real client we will just stream these back to the stream asynchronously,
        // but for now we're going to do everything sync.
//        List<BatchRecord>
        BatchPolicy batchPolicy = session.getBehavior()
                .getSettings(OpKind.WRITE_RETRYABLE, OpShape.BATCH, 
                        session.isNamespaceSC(valueSets.get(0).key.namespace)).asBatchPolicy();
        batchPolicy.txn = this.txnToUse;
        
        List<BatchRecord> batchRecords = valueSets.stream()
                .map(valueSet -> toBatchWrite(valueSet))
                .collect(Collectors.toList());
        
        session.getClient().operate(batchPolicy, batchRecords);
        
        // TODO: Exception handling!
        Key[] keys = batchRecords.stream().map(batchRecord -> batchRecord.key).toArray(Key[]::new);
        Record[] records = batchRecords.stream().map(batchRecord -> batchRecord.record).toArray(Record[]::new);
        return new RecordStream(keys, records, 0, 0, null, true);
    }
    
    private RecordStream executeViaSingleOperations() {
        // TODO: In the real client we will just stream these back to the stream asynchronously,
        // but for now we're going to do everything sync.
        Key[] keys = new Key[valueSets.size()];
        Record[] records = new Record[valueSets.size()];
        AerospikeException[] exceptions = new AerospikeException[valueSets.size()];
        
        int count = 0;
        
        WritePolicy wp = session.getBehavior()
                .getSettings(OpKind.WRITE_RETRYABLE, OpShape.POINT, 
                        session.isNamespaceSC(valueSets.get(0).key.namespace)).asWritePolicy();
        
        wp.recordExistsAction = OperationBuilder.recordExistsActionFromOpType(opType);
        for (RecordValues theseValues : valueSets) {
            int generation = theseValues.generation;
            if (generation > 0) {
                wp.generation = generation;
                wp.generationPolicy = GenerationPolicy.EXPECT_GEN_EQUAL;
            }
            wp.txn = this.txnToUse;
            wp.expiration = getExpiration(theseValues);
            Record record = null;
            AerospikeException aerospikeException = null;
            try {
                Operation[] ops = new Operation[binNames.length];
                for (int i = 0; i < binNames.length; i++) {
                    ops[i] = Operation.add(new Bin(binNames[i], Value.get(theseValues.values[i])));
                }
                record = session.getClient().operate(wp, theseValues.key, ops);
            }
            catch (AerospikeException ae) {
                aerospikeException = ae; 
                throw ae;
            }
            finally {
                keys[count] = theseValues.key;
                exceptions[count] = aerospikeException;
                records[count++] = record;
            }
        }
        // TODO: need to handle exceptions
        // TODO: If we use REPLACE we get write_master: modify op can't have record-level replace flag 1693171444c19e9c821d842b806388566df2949d
        // Raise with Brian N?
        return new RecordStream(keys, records, 0, 0, null, true);
    }

} 