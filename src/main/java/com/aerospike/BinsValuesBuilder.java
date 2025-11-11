package com.aerospike;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.BatchRecord;
import com.aerospike.client.BatchWrite;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Log;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.ResultCode;
import com.aerospike.client.Txn;
import com.aerospike.client.Value;
import com.aerospike.client.exp.Exp;
import com.aerospike.client.exp.Expression;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.BatchWritePolicy;
import com.aerospike.client.policy.GenerationPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.dslobjects.BooleanExpression;
import com.aerospike.exception.AeroException;
import com.aerospike.policy.Behavior.OpKind;
import com.aerospike.policy.Behavior.OpShape;
import com.aerospike.policy.Settings;
import com.aerospike.query.PreparedDsl;
import com.aerospike.query.WhereClauseProcessor;

/**
 * Builder for the bins+values pattern in OperationBuilder.
 * This allows setting multiple bin names and then providing values for each record.
 */
public class BinsValuesBuilder extends AbstractFilterableBuilder implements FilterableOperation<BinsValuesBuilder> {
    private static class ValueData {
        private Object[] values;
        private int generation = 0;
        private long expirationInSeconds = Long.MIN_VALUE;
        
        public ValueData(Object[] values) {
            this.values = values;
        }
    }
    
    private long expirationInSecondsForAll = 0; 
    private final OperationBuilder opBuilder;
    private final String[] binNames;
    private final Map<Key, ValueData> valueSets = new HashMap<>();
    private final List<Key> keys;
    private ValueData current = null;
    private Txn txnToUse;
    
    public BinsValuesBuilder(OperationBuilder opBuilder, List<Key> keys, String binName, String... binNames) {
        this.opBuilder = opBuilder;
        this.binNames = new String[1 + binNames.length];
        this.binNames[0] = binName;
        System.arraycopy(binNames, 0, this.binNames, 1, binNames.length);
        this.keys = keys;
        this.txnToUse = opBuilder.getTxnToUse();
    }
    
    /**
     * Add a set of values for one record. The number of values must match the number of bins.
     * Multiple calls to this method can be chained together.
     * 
     * @param values The values for this record
     * @return This builder for chaining
     */
    public BinsValuesBuilder values(Object... values) {
        if (values.length != binNames.length) {
            throw new IllegalArgumentException(
                    String.format("When calling '.values(...)' to specify the values for multiple bins,"
                + " the number of values must match the number of bins specified in the '.bins(...)' call."
                            + " This call specified %d bins, but supplied %d values.", binNames.length, values.length));
        }
        
        checkRoomToAddAnotherValue();
        current = new ValueData(values);
        valueSets.put(keys.get(valueSets.size()),current);
        return this;
    }
    
    private void checkValuesExist(String name) {
        if (valueSets.size() == 0) {
            throw new IllegalArgumentException(
                    String.format("%s was called when no values were defined (by calling '.values'). This method"
                    + " sets parameters on the values for that record, so call '.values' before"
                    + " calling this method", name));
        }
    }
    
    private void checkRoomToAddAnotherValue() {
        if (valueSets.size() >= keys.size()) {
            throw new IllegalArgumentException(String.format(
                    "The number of '.values(...)' must match the number of specified keys (%d), but there are too many .values(...) calls",
                    keys.size()));
        }
    }
    
    public BinsValuesBuilder ensureGenerationIs(int generation) {
        if (generation <= 0) {
            throw new IllegalArgumentException("Generation must be greater than 0");
        }
        checkValuesExist("ensureGenerationIs");
        current.generation = generation;
        return this;
    }
    
    public BinsValuesBuilder expireRecordAfter(Duration duration) {
        checkValuesExist("expireRecordAfter");
        current.expirationInSeconds = duration.toSeconds();
        return this;
    }
    
    public BinsValuesBuilder expireRecordAfterSeconds(int expirationInSeconds) {
        checkValuesExist("expireRecordAfter");
        current.expirationInSeconds = expirationInSeconds;
        return this;
    }
    
    public BinsValuesBuilder expireRecordAt(Date date) {
        checkValuesExist("expireRecordAfter");
        current.expirationInSeconds = opBuilder.getExpirationInSecondsAndCheckValue(date);
        return this;
    }
    
    public BinsValuesBuilder expireRecordAt(LocalDateTime date) {
        checkValuesExist("expireRecordAfter");
        current.expirationInSeconds = opBuilder.getExpirationInSecondsAndCheckValue(date);
        return this;
    }
    
    public BinsValuesBuilder withNoChangeInExpiration() {
        checkValuesExist("expireRecordAfter");
        current.expirationInSeconds = OperationBuilder.TTL_NO_CHANGE;
        return this;
    }
    
    public BinsValuesBuilder neverExpire() {
        checkValuesExist("expireRecordAfter");
        current.expirationInSeconds = OperationBuilder.TTL_NEVER_EXPIRE;
        return this;
    }
    
    public BinsValuesBuilder expiryFromServerDefault() {
        checkValuesExist("expireRecordAfter");
        current.expirationInSeconds = OperationBuilder.TTL_SERVER_DEFAULT;
        return this;
    }
    
    public BinsValuesBuilder notInAnyTransaction() {
        this.txnToUse = null;
        return this;
    }
    
    public BinsValuesBuilder inTransaction(Txn txn) {
        this.txnToUse = txn;
        return this;
    }
    
    // Multi-key expiry methods (only available for multiple keys)
    public BinsValuesBuilder expireAllRecordsAfter(Duration duration) {
        if (!opBuilder.isMultiKey()) {
            throw new IllegalStateException(
                    "expireAllRecordsAfter() is only available when multiple keys are specified");
        }
        this.expirationInSecondsForAll = duration.getSeconds();
        return this;
    }
    
    public BinsValuesBuilder expireAllRecordsAfterSeconds(long seconds) {
        if (!opBuilder.isMultiKey()) {
            throw new IllegalStateException(
                    "expireAllRecordsAfterSeconds() is only available when multiple keys are specified");
        }
        this.expirationInSecondsForAll = seconds;
        return this;
    }
    
    public BinsValuesBuilder expireAllRecordsAt(LocalDateTime dateTime) {
        if (!opBuilder.isMultiKey()) {
            throw new IllegalStateException("expireAllRecordsAt() is only available when multiple keys are specified");
        }
        this.expirationInSecondsForAll = opBuilder.getExpirationInSecondsAndCheckValue(dateTime);
        return this;
    }

    public BinsValuesBuilder expireAllRecordsAt(Date date) {
        if (!opBuilder.isMultiKey()) {
            throw new IllegalStateException("expireAllRecordsAt() is only available when multiple keys are specified");
        }
        this.expirationInSecondsForAll = opBuilder.getExpirationInSecondsAndCheckValue(date);
        return this;
    }
    
    public BinsValuesBuilder neverExpireAllRecords() {
        if (!opBuilder.isMultiKey()) {
            throw new IllegalStateException(
                    "neverExpireAllRecords() is only available when multiple keys are specified");
        }
        this.expirationInSecondsForAll = OperationBuilder.TTL_NEVER_EXPIRE;
        return this;
    }
    
    public BinsValuesBuilder withNoChangeInExpirationForAllRecords() {
        if (!opBuilder.isMultiKey()) {
            throw new IllegalStateException(
                    "withNoChangeInExpirationForAllRecords() is only available when multiple keys are specified");
        }
        this.expirationInSecondsForAll = OperationBuilder.TTL_NO_CHANGE;
        return this;
    }
    
    public BinsValuesBuilder expiryFromServerDefaultForAllRecords() {
        if (!opBuilder.isMultiKey()) {
            throw new IllegalStateException(
                    "expiryFromServerDefaultForAllRecords() is only available when multiple keys are specified");
        }
        this.expirationInSecondsForAll = OperationBuilder.TTL_SERVER_DEFAULT;
        return this;
    }
    
    /**
     * Apply a where clause filter to these operations. Only records matching the filter will be affected.
     * <p>
     * The DSL string can contain parameters which are replaced with the passed arguments. For example:
     * <pre>
     * builder.where("$.age > %d", 21)
     * </pre>
     * 
     * @param dsl The DSL string defining the filter condition
     * @param params Optional parameters to be substituted into the DSL string
     * @return This builder for method chaining
     */
    @Override
    public BinsValuesBuilder where(String dsl, Object ... params) {
        setWhereClause(createWhereClauseProcessor(false, dsl, params));
        return this;
    }
    
    /**
     * Apply a where clause filter to these operations using a boolean expression.
     * Only records matching the filter will be affected.
     * 
     * @param dsl The boolean expression defining the filter condition
     * @return This builder for method chaining
     */
    @Override
    public BinsValuesBuilder where(BooleanExpression dsl) {
        setWhereClause(WhereClauseProcessor.from(dsl));
        return this;
    }
    
    /**
     * Apply a where clause filter to these operations using a prepared DSL.
     * Only records matching the filter will be affected.
     * 
     * @param dsl The prepared DSL defining the filter condition
     * @param params Parameters to be substituted into the prepared DSL
     * @return This builder for method chaining
     */
    @Override
    public BinsValuesBuilder where(PreparedDsl dsl, Object ... params) {
        setWhereClause(WhereClauseProcessor.from(false, dsl, params));
        return this;
    }
    
    /**
     * Apply a where clause filter to these operations using an Aerospike expression.
     * Only records matching the filter will be affected.
     * 
     * @param exp The Aerospike expression defining the filter condition
     * @return This builder for method chaining
     */
    @Override
    public BinsValuesBuilder where(Exp exp) {
        setWhereClause(WhereClauseProcessor.from(exp));
        return this;
    }
    
    /**
     * If a where clause is specified and a record is filtered out, it will appear in the
     * result stream with an exception code of {@link ResultCode#FILTERED_OUT} rather than 
     * being silently omitted from the results.
     * 
     * @return This builder for method chaining
     */
    @Override
    public BinsValuesBuilder failOnFilteredOut() {
        this.failOnFilteredOut = true;
        return this;
    }
    
    /**
     * By default, if a key does not map to a record (or is filtered out), nothing will be 
     * returned in the stream for that key. If this flag is specified, a result will be 
     * included in the stream for every key, even if the record doesn't exist or was filtered out.
     * 
     * @return This builder for method chaining
     */
    @Override
    public BinsValuesBuilder respondAllKeys() {
        this.respondAllKeys = true;
        return this;
    }

    /**
     * Execute operations with default behavior (synchronous).
     * All operations complete before this method returns, making it safe for transactions.
     * 
     * @return RecordStream containing the results
     */
    public RecordStream execute() {
        return executeSync();
    }
    
    /**
     * Execute operations synchronously. All operations complete before this method returns.
     * <p>
     * Operations are parallelized using virtual threads, but all threads are joined before
     * returning. This ensures transaction safety and deterministic behavior.
     * 
     * @return RecordStream containing the results
     */
    public RecordStream executeSync() {
        if (Log.debugEnabled()) {
            Log.debug("BinsValuesBuilder.executeSync() called for " + keys.size() + " key(s), transaction: "
                    + (txnToUse != null ? "yes" : "no"));
        }
        
        if (valueSets.size() != opBuilder.getNumKeys()) {
            throw new IllegalArgumentException(String.format(
                "The number of '.values(...)' calls (%d) must match the number of specified keys (%d)",
                valueSets.size(), opBuilder.getNumKeys()));
        }
        
        if (keys.size() >= OperationBuilder.getBatchOperationThreshold()) {
            return executeBatchSync();
        } else {
            return executeIndividualSync();
        }
    }
    
    /**
     * Execute operations asynchronously using virtual threads for parallel execution.
     * Method returns immediately; results are consumed via the RecordStream.
     * <p>
     * <b>WARNING:</b> Using this in transactions may lead to operations still being in flight
     * when commit() is called, potentially leading to inconsistent state. A warning will be logged.
     * 
     * @return RecordStream that will be populated as results arrive
     */
    public RecordStream executeAsync() {
        if (Log.debugEnabled()) {
            Log.debug("BinsValuesBuilder.executeAsync() called for " + keys.size() + " key(s), transaction: "
                    + (txnToUse != null ? "yes" : "no"));
        }
        
        if (this.txnToUse != null && Log.warnEnabled()) {
            Log.warn("executeAsync() called within a transaction. "
                    + "Async operations may still be in flight when commit() is called, "
                    + "which could lead to inconsistent state. "
                    + "Consider using executeSync() or execute() for transactional safety.");
        }
        
        if (valueSets.size() != opBuilder.getNumKeys()) {
            throw new IllegalArgumentException(String.format(
                "The number of '.values(...)' calls (%d) must match the number of specified keys (%d)",
                valueSets.size(), opBuilder.getNumKeys()));
        }
        
        if (keys.size() >= OperationBuilder.getBatchOperationThreshold()) {
            return executeBatchSync();
        } else {
            return executeIndividualAsync();
        }
    }
    protected RecordStream executeBatchSync() {
        Settings settings = opBuilder.getSession().getBehavior()
                .getSettings(OpKind.WRITE_NON_RETRYABLE, OpShape.BATCH, opBuilder.getSession().isNamespaceSC(keys.get(0).namespace));
        BatchPolicy batchPolicy = settings.asBatchPolicy();

        batchPolicy.setTxn(txnToUse);
        
        // Apply where clause if present
        batchPolicy.filterExp = keys.isEmpty() ? null : processWhereClause(keys.get(0).namespace, opBuilder.getSession());
        batchPolicy.failOnFilteredOut = this.failOnFilteredOut;
        
        List<BatchRecord> batchRecords = new ArrayList<>(keys.size());

        for (Key key : keys) {
            ValueData valueSet = valueSets.get(key);
            Operation[] ops = getOperationsForValueData(valueSet);

            BatchWritePolicy bwp = new BatchWritePolicy();
            if (valueSet.generation > 0) {
                bwp.generation = valueSet.generation;
                bwp.generationPolicy = GenerationPolicy.EXPECT_GEN_EQUAL;
            }
            bwp.expiration = getExpiration(valueSet);
            bwp.recordExistsAction = OperationBuilder.recordExistsActionFromOpType(opBuilder.opType);
            bwp.sendKey = settings.getSendKey();
            bwp.durableDelete = settings.getUseDurableDelete();
            batchRecords.add(new BatchWrite(bwp, key, ops));
        }
        batchPolicy.txn = this.txnToUse;
        
        opBuilder.getSession().getClient().operate(batchPolicy, batchRecords);
        
        List<RecordResult> results = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            BatchRecord br = batchRecords.get(i);
            if (switch (br.resultCode) {
                case ResultCode.KEY_NOT_FOUND_ERROR -> respondAllKeys;
                case ResultCode.FILTERED_OUT -> failOnFilteredOut || respondAllKeys;
                default -> true;
            }) {
                if (settings.getStackTraceOnException() && br.resultCode != ResultCode.OK) {
                    results.add(new RecordResult(br, AeroException.resultCodeToException(br.resultCode, null, br.inDoubt), i));
                } else {
                    results.add(new RecordResult(br, i));
                }
            }
        }
        
        return new RecordStream(results, 0);
    }
    
    
    /**
     * Execute operations synchronously for individual keys (< batch threshold).
     * All virtual threads are joined before returning.
     */
    protected RecordStream executeIndividualSync() {
        // Apply where clause if present
        final Expression whereExp = keys.isEmpty() ? null : processWhereClause(keys.get(0).namespace, opBuilder.getSession());
        
        // Single key: synchronous execution
        if (keys.size() == 1) {
            Key firstKey = keys.get(0);
            ValueData valueSet = valueSets.get(firstKey);
            Operation[] ops = getOperationsForValueData(valueSet);
            Settings settings = opBuilder.getSession().getBehavior()
                    .getSettings(OpKind.WRITE_RETRYABLE, OpShape.POINT, opBuilder.getSession().isNamespaceSC(firstKey.namespace));
            WritePolicy wp = opBuilder.getWritePolicy(settings, valueSet.generation, this.opBuilder.opType);
            wp.expiration = getExpiration(valueSet);
            wp.txn = this.txnToUse;
            wp.filterExp = whereExp;
            wp.generation = valueSet.generation;
            wp.generationPolicy = wp.generation <= 0 ? GenerationPolicy.NONE : GenerationPolicy.EXPECT_GEN_EQUAL;
            
            boolean stackTraceOnException = settings.getStackTraceOnException();
            
            try {
                Record record = opBuilder.getSession().getClient().operate(wp, firstKey, ops);
                if (respondAllKeys || record != null) {
                    return new RecordStream(firstKey, record);
                }
            } catch (AerospikeException ae) {
                if (shouldPublishException(ae)) {
                    if (ae.getResultCode() != ResultCode.FILTERED_OUT) {
                        opBuilder.showWarningsOnException(ae, txnToUse, firstKey, wp.expiration);
                    }
                    return new RecordStream(new RecordResult(firstKey, AeroException.from(ae), 0));
                }
            }
            return new RecordStream();
        }
        
        // Multiple keys: parallel execution with virtual threads, JOINED before return
        AsyncRecordStream stream = new AsyncRecordStream(keys.size());
        CountDownLatch latch = new CountDownLatch(keys.size());
        
        Settings settings = opBuilder.getSession().getBehavior()
                .getSettings(OpKind.WRITE_RETRYABLE, OpShape.POINT, opBuilder.getSession().isNamespaceSC(keys.get(0).namespace));
        
        for (int i = 0; i < keys.size(); i++) {
            final int index = i;
            final Key key = keys.get(i);
            ValueData valueSet = valueSets.get(key);
            Thread.startVirtualThread(() -> {
                try {
                    Operation[] ops = getOperationsForValueData(valueSet);
                    WritePolicy wp = opBuilder.getWritePolicy(settings, valueSet.generation, this.opBuilder.opType);
                    wp.expiration = getExpiration(valueSet);
                    wp.txn = this.txnToUse;
                    wp.filterExp = whereExp;
                    wp.generation = valueSet.generation;
                    wp.generationPolicy = wp.generation <= 0 ? GenerationPolicy.NONE : GenerationPolicy.EXPECT_GEN_EQUAL;
                    
                    try {
                        Record record = opBuilder.getSession().getClient().operate(wp, key, ops);
                        if (respondAllKeys || record != null) {
                            stream.publish(new RecordResult(key, record, index));
                        }
                    } catch (AerospikeException ae) {
                        if (shouldPublishException(ae)) {
                            if (ae.getResultCode() != ResultCode.FILTERED_OUT) {
                                opBuilder.showWarningsOnException(ae, txnToUse, key, wp.expiration);
                            }
                            stream.publish(new RecordResult(key, AeroException.from(ae), index));
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // WAIT for all threads to complete
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for operations to complete", e);
        }
        
        return new RecordStream(stream.complete());
    }
    
    /**
     * Execute operations asynchronously for individual keys (< batch threshold).
     * Returns immediately; virtual threads complete in background.
     */
    protected RecordStream executeIndividualAsync() {
        // Apply where clause if present
        final Expression whereExp = keys.isEmpty() ? null : processWhereClause(keys.get(0).namespace, opBuilder.getSession());
        
        // Even single key: use async execution with virtual thread
        AsyncRecordStream asyncStream = new AsyncRecordStream(keys.size());
        AtomicInteger pendingOps = new AtomicInteger(keys.size());
        
        Settings settings = opBuilder.getSession().getBehavior()
                .getSettings(OpKind.WRITE_RETRYABLE, OpShape.POINT, opBuilder.getSession().isNamespaceSC(keys.get(0).namespace));
        boolean stackTraceOnException = settings.getStackTraceOnException();
        
        for (int i = 0; i < keys.size(); i++) {
            final int index = i;
            final Key key = keys.get(i);
            ValueData valueSet = valueSets.get(key);
            Thread.startVirtualThread(() -> {
                try {
                    Operation[] ops = getOperationsForValueData(valueSet);
                    WritePolicy wp = opBuilder.getWritePolicy(settings, valueSet.generation, this.opBuilder.opType);
                    wp.expiration = getExpiration(valueSet);
                    wp.txn = this.txnToUse;
                    wp.filterExp = whereExp;
                    
                    opBuilder.executeAndPublishSingleOperation(wp, key, ops, asyncStream, index, stackTraceOnException);
                } finally {
                    if (pendingOps.decrementAndGet() == 0) {
                        asyncStream.complete();
                    }
                }
            });
        }
        
        return new RecordStream(asyncStream);
    }

    private Operation[] getOperationsForValueData(ValueData valueData) {
        Object[] values = valueData.values;
        Operation[] ops = new Operation[binNames.length];
        for (int i = 0; i < binNames.length; i++) {
            ops[i] = Operation.put(new Bin(binNames[i], Value.get(values[i])));
        }
        return ops;
    }
    
    private int getExpiration(ValueData valueData) {
        if (valueData.expirationInSeconds != Long.MIN_VALUE) {
            return opBuilder.getExpirationAsInt(valueData.expirationInSeconds);
        } else {
            return opBuilder.getExpirationAsInt(expirationInSecondsForAll);
        }
    }
    

} 