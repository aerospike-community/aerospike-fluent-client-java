package com.aerospike;

import java.time.LocalDateTime;
import java.util.Date;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Txn;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.policy.Settings;

/**
 * Interface for operations that support the bins+values pattern.
 * This defines the methods that BinsValuesBuilder needs from its parent operation builder.
 */
interface BinsValuesOperations {
    /**
     * Get the session associated with this operation.
     */
    Session getSession();
    
    /**
     * Get the operation type.
     */
    OpType getOpType();
    
    /**
     * Get the number of keys in this operation.
     */
    int getNumKeys();
    
    /**
     * Check if this operation has multiple keys.
     */
    boolean isMultiKey();
    
    /**
     * Convert expiration in seconds to int, capping at Integer.MAX_VALUE.
     */
    int getExpirationAsInt(long expirationInSeconds);
    
    /**
     * Convert a Date to expiration in seconds and validate it.
     */
    long getExpirationInSecondsAndCheckValue(Date date);
    
    /**
     * Convert a LocalDateTime to expiration in seconds and validate it.
     */
    long getExpirationInSecondsAndCheckValue(LocalDateTime dateTime);
    
    /**
     * Create a WritePolicy with the specified settings.
     */
    WritePolicy getWritePolicy(Settings settings, int generation, OpType opType);
    
    /**
     * Show warnings for specific exception conditions.
     */
    void showWarningsOnException(AerospikeException ae, Txn txn, Key key, int expiration);
    
    /**
     * Execute a single operation and publish the result to an async stream.
     */
    void executeAndPublishSingleOperation(
            WritePolicy wp, 
            Key key, 
            Operation[] operations,
            AsyncRecordStream asyncStream,
            int index,
            boolean stackTraceOnException);
}

