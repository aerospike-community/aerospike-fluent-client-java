package com.aerospike;

import java.util.ArrayList;
import java.util.List;

import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.exp.Expression;

/**
 * Internal class representing a single operation specification in a chainable batch operation.
 * Each OperationSpec holds all the information needed to execute one logical operation
 * on one or more keys, including the operation type, bins to modify, filters, and policies.
 * 
 * <p>This class is used internally by {@link ChainableOperationBuilder}, 
 * {@link ChainableNoBinsBuilder}, and {@link ChainableQueryBuilder} to build up
 * a list of heterogeneous operations that will be executed as a single batch.</p>
 */
class OperationSpec {
    /** The keys that this operation applies to */
    final List<Key> keys;
    
    /** The type of operation (UPSERT, UPDATE, INSERT, REPLACE, DELETE, TOUCH, EXISTS, or null for query) */
    final OpType opType;
    
    /** The list of operations to perform on the bins (empty for DELETE, TOUCH, EXISTS) */
    final List<Operation> operations;
    
    /** Optional filter expression for conditional operations */
    Expression whereClause = null;
    
    /** Generation check value (0 means no generation check) */
    int generation = 0;
    
    /** Expiration in seconds (0 means server default, -1 means never expire, -2 means no change) */
    long expirationInSeconds = 0;
    
    /** Whether to fail if a record is filtered out by the where clause */
    boolean failOnFilteredOut = false;
    
    /** Whether to include results for keys that don't exist or are filtered out */
    boolean respondAllKeys = false;
    
    /** For DELETE operations: whether to use durable delete */
    Boolean durablyDelete = null;
    
    /** For QUERY operations: specific bins to read (null means all bins) */
    String[] projectedBins = null;
    
    /**
     * Create a write operation spec (upsert, update, insert, replace, delete, touch).
     */
    OperationSpec(List<Key> keys, OpType opType) {
        this.keys = keys;
        this.opType = opType;
        this.operations = new ArrayList<>();
    }
    
    /**
     * Create a query (read) operation spec.
     */
    OperationSpec(List<Key> keys) {
        this.keys = keys;
        this.opType = null; // Query operations don't have an OpType
        this.operations = new ArrayList<>();
    }
    
    /**
     * Returns true if this is a query/read operation.
     */
    boolean isQuery() {
        return opType == null;
    }
    
    /**
     * Returns true if this operation type can have bin operations.
     */
    boolean canHaveBinOperations() {
        return opType != null && 
               opType != OpType.DELETE && 
               opType != OpType.TOUCH && 
               opType != OpType.EXISTS;
    }
}



