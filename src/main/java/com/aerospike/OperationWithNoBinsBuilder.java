package com.aerospike;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.aerospike.client.BatchRecord;
import com.aerospike.client.BatchResults;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.ResultCode;
import com.aerospike.client.exp.Exp;
import com.aerospike.client.exp.Expression;
import com.aerospike.client.policy.BatchDeletePolicy;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.BatchWritePolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.dslobjects.BooleanExpression;
import com.aerospike.exception.AeroException;
import com.aerospike.policy.Behavior.OpKind;
import com.aerospike.policy.Behavior.OpShape;
import com.aerospike.query.PreparedDsl;
import com.aerospike.query.WhereClauseProcessor;

public class OperationWithNoBinsBuilder extends AbstractSessionOperationBuilder<OperationWithNoBinsBuilder> implements FilterableOperation<OperationWithNoBinsBuilder> {
    private final List<Key> keys;
    private final Key key;
    protected long expirationInSecondsForAll = 0;
    protected Boolean durablyDelete = null;  // null means use behavior default
    
    public OperationWithNoBinsBuilder(Session session, Key key, OpType type) {
        super(session, type);
        this.keys = null;
        this.key = key;
    }
    
    public OperationWithNoBinsBuilder(Session session, List<Key> keys, OpType type) {
        super(session, type);
        if (keys.size() == 1) {
            this.keys = null;
            this.key = keys.get(0);
        }
        else {
            this.key = null;
            this.keys = keys;
        }
    }
    
    /**
     * Set the expiration for all records in this operation relative to the current time.
     * This applies to all keys unless overridden by individual record expiration settings.
     * <p>
     * Note: This method is only available when multiple keys are specified.
     * 
     * @param duration The duration after which all records should expire
     * @return This builder for method chaining
     * @throws IllegalStateException if called when only a single key is specified
     */
    public OperationWithNoBinsBuilder expireAllRecordsAfter(Duration duration) {
        if (!isMultiKey()) {
            throw new IllegalStateException("expireAllRecordsAfter() is only available when multiple keys are specified");
        }
        this.expirationInSecondsForAll = duration.getSeconds();
        return this;
    }
    
    /**
     * Set the expiration for all records in this operation relative to the current time.
     * This applies to all keys unless overridden by individual record expiration settings.
     * <p>
     * Note: This method is only available when multiple keys are specified.
     * 
     * @param seconds The number of seconds after which all records should expire
     * @return This builder for method chaining
     * @throws IllegalStateException if called when only a single key is specified
     */
    public OperationWithNoBinsBuilder expireAllRecordsAfterSeconds(long seconds) {
        if (!isMultiKey()) {
            throw new IllegalStateException("expireAllRecordsAfterSeconds() is only available when multiple keys are specified");
        }
        this.expirationInSecondsForAll = seconds;
        return this;
    }
    
    /**
     * Set the expiration for all records in this operation to an absolute date/time.
     * This applies to all keys unless overridden by individual record expiration settings.
     * <p>
     * Note: This method is only available when multiple keys are specified.
     * 
     * @param dateTime The date/time at which all records should expire
     * @return This builder for method chaining
     * @throws IllegalStateException if called when only a single key is specified
     * @throws IllegalArgumentException if the date is in the past
     */
    public OperationWithNoBinsBuilder expireAllRecordsAt(LocalDateTime dateTime) {
        if (!isMultiKey()) {
            throw new IllegalStateException("expireAllRecordsAt() is only available when multiple keys are specified");
        }
        this.expirationInSecondsForAll = getExpirationInSecondsAndCheckValue(dateTime);
        return this;
    }

    /**
     * Set the expiration for all records in this operation to an absolute date/time.
     * This applies to all keys unless overridden by individual record expiration settings.
     * <p>
     * Note: This method is only available when multiple keys are specified.
     * 
     * @param date The date at which all records should expire
     * @return This builder for method chaining
     * @throws IllegalStateException if called when only a single key is specified
     * @throws IllegalArgumentException if the date is in the past
     */
    public OperationWithNoBinsBuilder expireAllRecordsAt(Date date) {
        if (!isMultiKey()) {
            throw new IllegalStateException("expireAllRecordsAt() is only available when multiple keys are specified");
        }
        this.expirationInSecondsForAll = getExpirationInSecondsAndCheckValue(date);
        return this;
    }
    
    /**
     * Set all records to never expire (TTL = -1).
     * This applies to all keys unless overridden by individual record expiration settings.
     * <p>
     * Note: This method is only available when multiple keys are specified.
     * 
     * @return This builder for method chaining
     * @throws IllegalStateException if called when only a single key is specified
     */
    public OperationWithNoBinsBuilder neverExpireAllRecords() {
        if (!isMultiKey()) {
            throw new IllegalStateException("neverExpireAllRecords() is only available when multiple keys are specified");
        }
        this.expirationInSecondsForAll = OperationBuilder.TTL_NEVER_EXPIRE;
        return this;
    }
    
    /**
     * Do not change the expiration of any records (TTL = -2).
     * This applies to all keys unless overridden by individual record expiration settings.
     * <p>
     * Note: This method is only available when multiple keys are specified.
     * 
     * @return This builder for method chaining
     * @throws IllegalStateException if called when only a single key is specified
     */
    public OperationWithNoBinsBuilder withNoChangeInExpirationForAllRecords() {
        if (!isMultiKey()) {
            throw new IllegalStateException("withNoChangeInExpirationForAllRecords() is only available when multiple keys are specified");
        }
        this.expirationInSecondsForAll = OperationBuilder.TTL_NO_CHANGE;
        return this;
    }
    
    /**
     * Use the server's default expiration for all records (TTL = 0).
     * This applies to all keys unless overridden by individual record expiration settings.
     * <p>
     * Note: This method is only available when multiple keys are specified.
     * 
     * @return This builder for method chaining
     * @throws IllegalStateException if called when only a single key is specified
     */
    public OperationWithNoBinsBuilder expiryFromServerDefaultForAllRecords() {
        if (!isMultiKey()) {
            throw new IllegalStateException("expiryFromServerDefaultForAllRecords() is only available when multiple keys are specified");
        }
        this.expirationInSecondsForAll = OperationBuilder.TTL_SERVER_DEFAULT;
        return this;
    }
    
    @Override
    public OperationWithNoBinsBuilder where(String dsl, Object ... params) {
        setWhereClause(createWhereClauseProcessor(false, dsl, params));
        return this;
    }
    
    @Override
    public OperationWithNoBinsBuilder where(BooleanExpression dsl) {
        setWhereClause(WhereClauseProcessor.from(dsl));
        return this;
    }
    
    @Override
    public OperationWithNoBinsBuilder where(PreparedDsl dsl, Object ... params) {
        setWhereClause(WhereClauseProcessor.from(false, dsl, params));
        return this;
    }
    
    @Override
    public OperationWithNoBinsBuilder where(Exp exp) {
        setWhereClause(WhereClauseProcessor.from(exp));
        return this;
    }
    
    @Override
    public OperationWithNoBinsBuilder failOnFilteredOut() {
        this.failOnFilteredOut = true;
        return this;
    }
    
    @Override
    public OperationWithNoBinsBuilder respondAllKeys() {
        this.respondAllKeys = true;
        return this;
    }
    
    private Key getAnyKey() {
        if (key != null) {
            return key;
        }
        else {
            return keys.get(0);
        }
    }
    
    private boolean isMultiKey() {
        return keys != null && keys.size() > 1;
    }
    
    @Override
    protected int getExpirationAsInt() {
        long effectiveExpiration = (expirationInSeconds != 0) ? expirationInSeconds : expirationInSecondsForAll;
        return super.getExpirationAsInt(effectiveExpiration);
    }
    
    /**
     * Convert boolean array results (from EXISTS batch) to RecordResult list.
     * true -> ResultCode.OK, false -> ResultCode.KEY_NOT_FOUND_ERROR
     */
    private List<RecordResult> toRecordResults(boolean[] booleanArray, Key[] keyArray) {
        List<RecordResult> results = new ArrayList<>();
        for (int i = 0; i < booleanArray.length; i++) {
            int resultCode = booleanArray[i] ? ResultCode.OK : ResultCode.KEY_NOT_FOUND_ERROR;
            results.add(new RecordResult(keyArray[i], resultCode, false, 
                    ResultCode.getResultString(resultCode), i));
        }
        return results;
    }
    
    /**
     * Apply filter expression and respondAllKeys to a batch policy.
     * Creates a new BatchPolicy if modifications are needed.
     */
    private BatchPolicy applyBatchPolicySettings(BatchPolicy batchPolicy, String namespace) {
        // Apply filter expression if set
        Expression filterExp = processWhereClause(namespace, session);
        if (filterExp != null) {
            batchPolicy = new BatchPolicy(batchPolicy);
            batchPolicy.filterExp = filterExp;
        }
        
        // Apply respondAllKeys flag
        if (respondAllKeys) {
            if (batchPolicy == null || filterExp == null) {
                batchPolicy = new BatchPolicy(batchPolicy);
            }
            batchPolicy.respondAllKeys = true;
        }
        
        return batchPolicy;
    }
    
    /**
     * Apply generation policy to a batch write or delete policy.
     */
    private void applyGenerationPolicy(Object policy) {
        if (generation > 0) {
            if (policy instanceof BatchWritePolicy) {
                BatchWritePolicy bwp = (BatchWritePolicy) policy;
                bwp.generationPolicy = com.aerospike.client.policy.GenerationPolicy.EXPECT_GEN_EQUAL;
                bwp.generation = generation;
            } else if (policy instanceof BatchDeletePolicy) {
                BatchDeletePolicy bdp = (BatchDeletePolicy) policy;
                bdp.generationPolicy = com.aerospike.client.policy.GenerationPolicy.EXPECT_GEN_EQUAL;
                bdp.generation = generation;
            }
        }
    }
    
    /**
     * Process batch results into a list of RecordResults, handling filtered out records.
     * OK -> ResultCode.OK, not OK -> original result code (including KEY_NOT_FOUND_ERROR)
     */
    private List<RecordResult> processBatchResults(BatchResults results) {
        List<RecordResult> recordResults = new ArrayList<>();
        int index = 0;
        for (BatchRecord record : results.records) {
            if (failOnFilteredOut && record.resultCode == ResultCode.FILTERED_OUT) {
                throw new RuntimeException("Record was filtered out by filter expression");
            }
            // Use the actual result code from the batch record
            recordResults.add(new RecordResult(record, index++));
        }
        return recordResults;
    }
    
    /**
     * Apply all settings to a write policy for single-key operations.
     * Creates a new WritePolicy as needed for modifications.
     */
    private WritePolicy applyWritePolicySettings(WritePolicy wp, String namespace) {
        // Apply expiration settings
        if (expirationInSeconds != 0) {
            wp = new WritePolicy(wp);
            wp.expiration = getExpirationAsInt();
        }
        
        // Apply generation if set
        if (generation > 0) {
            wp = new WritePolicy(wp);
            wp.generationPolicy = com.aerospike.client.policy.GenerationPolicy.EXPECT_GEN_EQUAL;
            wp.generation = generation;
        }
        
        // Apply filter expression if set
        Expression filterExp = processWhereClause(namespace, session);
        if (filterExp != null) {
            wp = new WritePolicy(wp);
            wp.filterExp = filterExp;
        }
        
        // Apply transaction if set
        if (txnToUse != null) {
            wp = new WritePolicy(wp);
            wp.txn = txnToUse;
        }
        
        // Apply durable delete if specified and operation is DELETE
        if (durablyDelete != null && opType == OpType.DELETE) {
            wp = new WritePolicy(wp);
            wp.durableDelete = durablyDelete;
        }
        
        return wp;
    }
    
    private RecordStream batchExecute(WritePolicy wp) {
        String namespace = getAnyKey().namespace;
        Key[] keyArray = keys.toArray(new Key[0]);
        List<RecordResult> recordResults;
        
        switch (opType) {
        case EXISTS: {
            BatchPolicy batchPolicy = session.getBehavior()
                    .getSettings(OpKind.READ, OpShape.BATCH, session.isNamespaceSC(namespace))
                    .asBatchPolicy();
            batchPolicy = applyBatchPolicySettings(batchPolicy, namespace);
            
            boolean[] results = session.getClient().exists(batchPolicy, keyArray);
            recordResults = toRecordResults(results, keyArray);
            break;
        }
        
        case TOUCH: {
            BatchPolicy batchPolicy = session.getBehavior()
                    .getSettings(OpKind.WRITE_RETRYABLE, OpShape.BATCH, session.isNamespaceSC(namespace))
                    .asBatchPolicy();
            batchPolicy = applyBatchPolicySettings(batchPolicy, namespace);
            
            BatchWritePolicy batchWritePolicy = new BatchWritePolicy();
            batchWritePolicy.sendKey = batchPolicy.sendKey;
            
            if (expirationInSecondsForAll != 0) {
                batchWritePolicy.expiration = (int) expirationInSecondsForAll;
            }
            applyGenerationPolicy(batchWritePolicy);
            
            BatchResults results = session.getClient().operate(batchPolicy, batchWritePolicy, keyArray, Operation.touch());
            recordResults = processBatchResults(results);
            break;
        }
            
        case DELETE: {
            BatchPolicy batchPolicy = session.getBehavior()
                    .getSettings(OpKind.WRITE_RETRYABLE, OpShape.BATCH, session.isNamespaceSC(namespace))
                    .asBatchPolicy();
            batchPolicy = applyBatchPolicySettings(batchPolicy, namespace);

            BatchDeletePolicy batchDeletePolicy = new BatchDeletePolicy();
            batchDeletePolicy.sendKey = batchPolicy.sendKey;
            
            applyGenerationPolicy(batchDeletePolicy);
            
            if (durablyDelete != null) {
                batchDeletePolicy.durableDelete = durablyDelete;
            }
            
            BatchResults results = session.getClient().delete(batchPolicy, batchDeletePolicy, keyArray);
            recordResults = processBatchResults(results);
            break;
        }
        
        default:
            throw new IllegalStateException("received an action of " + opType + " which should be handled elsewhere");
        }
        
        return new RecordStream(recordResults, 0L);
    }

    public RecordStream execute() {
        if (key != null) {
            // Single key operation
            return executeSingleKey();
        }
        else {
            // Multi-key (batch) operation
            WritePolicy wp = session.getBehavior()
                    .getSettings(OpKind.WRITE_RETRYABLE, OpShape.BATCH, session.isNamespaceSC(getAnyKey().namespace))
                    .asWritePolicy();
            return batchExecute(wp);
        }
    }
    
    private RecordStream executeSingleKey() {
        OpKind opKind = (opType == OpType.EXISTS) ? OpKind.READ : OpKind.WRITE_RETRYABLE;
        WritePolicy wp = session.getBehavior()
                .getSettings(opKind, OpShape.POINT, session.isNamespaceSC(key.namespace))
                .asWritePolicy();
        
        wp = applyWritePolicySettings(wp, key.namespace);
        
        boolean result;
        try {
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
        } catch (com.aerospike.client.AerospikeException e) {
            if (failOnFilteredOut && e.getResultCode() == ResultCode.FILTERED_OUT) {
                throw new RuntimeException("Record was filtered out by filter expression", e);
            }
            // For other exceptions, wrap in RecordResult
            return new RecordStream(new RecordResult(key, AeroException.from(e), 0));
        }
        
        // Convert boolean result to RecordResult
        // true -> ResultCode.OK, false -> ResultCode.KEY_NOT_FOUND_ERROR
        int resultCode = result ? ResultCode.OK : ResultCode.KEY_NOT_FOUND_ERROR;
        RecordResult recordResult = new RecordResult(key, resultCode, false, 
                ResultCode.getResultString(resultCode), 0);
        return new RecordStream(recordResult);
    }
}
