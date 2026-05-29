package com.aerospike;

import java.util.ArrayList;
import java.util.List;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Txn;
import com.aerospike.client.cluster.Partitions;
import com.aerospike.client.exp.Exp;
import com.aerospike.client.exp.Expression;
import com.aerospike.info.InfoCommands;
import com.aerospike.policy.Behavior;
import com.aerospike.query.IndexBasedQueryBuilderInterface;
import com.aerospike.query.KeyBasedQueryBuilderInterface;
import com.aerospike.query.QueryBuilder;
import com.aerospike.query.TypedQueryBuilder;

public class Session {
    private final Cluster cluster;
    private final Behavior behavior;
    private final IAerospikeClient client;
    
    protected Session(Cluster cluster, Behavior behavior) {
        this.cluster = cluster;
        this.behavior = behavior;
        this.client = cluster.getUnderlyingClient();
    }
    
    public class ExpressionBuilder {
        private Expression filterExpression = null;
        public ExpressionBuilder(Exp exp) {
            this.filterExpression = Exp.build(exp);
        }
        
        public ExpressionBuilder(Expression exp) {
            this.filterExpression = exp;
        }
        
        public Expression getFilterExpression() {
            return filterExpression;
        }
    }
    
    public Behavior getBehavior() {
        return this.behavior;
    }
    
    public Cluster getCluster() {
        return cluster;
    }
    
//    public NamespaceInfo getNamespaceInfo(String namespaceName) {
//        return getNamespaceInfo(namespaceName, -1);
//    }
//    public NamespaceInfo getNamespaceInfo(String namespaceName, int refreshIntervalInSecs) {
//        return new NamespaceInfo(namespaceName, null, this, refreshIntervalInSecs);
//    }
    
    public IAerospikeClient getClient() {
        return client;
    }
    
    public void truncate(DataSet set) {
        this.client.truncate(null, set.getNamespace(), set.getSet(), null);
    }

    public void truncate(TypedDataSet<?> set) {
        this.client.truncate(null, set.getNamespace(), set.getSet(), null);
    }
    
    public RecordMappingFactory getRecordMappingFactory() {
        return this.cluster.getRecordMappingFactory();
    }
    
    private List<Key> buildKeyList(Key key1, Key key2, Key ...keys) {
        List<Key> keyList = new ArrayList<>();
        keyList.add(key1);
        keyList.add(key2);
        for (Key thisKey : keys) {
            keyList.add(thisKey);
        }
        return keyList;
    }

    /**
     * Converts typed keys to native {@link Key} list (for batch APIs).
     */
    public static List<Key> nativeKeysFromTyped(List<? extends TypedKey<?>> keys) {
        List<Key> out = new ArrayList<>(keys.size());
        for (TypedKey<?> tk : keys) {
            out.add(tk.getNativeKey());
        }
        return out;
    }
    
    // --------------------------------------------
    // Query functionality
    // --------------------------------------------
    public IndexBasedQueryBuilderInterface<QueryBuilder> query(DataSet dataSet) {
        return new QueryBuilder(this, dataSet);
    }
    
    public KeyBasedQueryBuilderInterface<QueryBuilder> query(Key key) {
        return new QueryBuilder(this, key);
    }

    /**
     * Point or batch read with one or more keys. Query with no parameters is valid, so must have (Key, Key...) to differentiate
     * @param key
     * @param keys
     * @return
     */
    public KeyBasedQueryBuilderInterface<QueryBuilder> query(Key key1, Key key2, Key...keys) {
        return new QueryBuilder(this, buildKeyList(key1, key2, keys));
    }
    
    public KeyBasedQueryBuilderInterface<QueryBuilder> query(List<Key> keyList) {
        return new QueryBuilder(this, keyList);
    }

    public <T> TypedQueryBuilder<T> query(TypedDataSet<T> dataSet) {
        return new TypedQueryBuilder<>(this, dataSet);
    }

    public <T> TypedQueryBuilder<T> query(TypedKey<T> key) {
        return new TypedQueryBuilder<>(this, key);
    }

    public <T> TypedQueryBuilder<T> queryTypedKeys(List<TypedKey<T>> typedKeys) {
        return new TypedQueryBuilder<>(this, typedKeys);
    }

    @SafeVarargs
    public final <T> TypedQueryBuilder<T> query(TypedKey<T> key1, TypedKey<T> key2, TypedKey<T>... moreKeys) {
        List<TypedKey<T>> list = new ArrayList<>();
        list.add(key1);
        list.add(key2);
        for (TypedKey<T> k : moreKeys) {
            list.add(k);
        }
        return new TypedQueryBuilder<>(this, list);
    }

    /**
     * Starts a heterogeneous typed batch read: one server round-trip, results split per {@link MixedTypedBatchReadBuilder#segment}.
     */
    public MixedTypedBatchReadBuilder mixedRead() {
        return new MixedTypedBatchReadBuilder(this);
    }
    
    // -------------------
    // CUD functionality (chainable batch operations)
    // -------------------
    
    /**
     * Begin an insert operation. Supports chaining multiple heterogeneous operations.
     * 
     * <p>Example:
     * <pre>{@code
     * session.insert(users.id("user-1"))
     *     .bin("name").setTo("Alice")
     *     .update(users.id("user-2"))
     *     .bin("age").add(1)
     *     .execute();
     * }</pre>
     * 
     * @param key the key to insert
     * @return ChainableOperationBuilder for method chaining
     */
    public ChainableOperationBuilder insert(Key key) {
        return new ChainableOperationBuilder(this, OpType.INSERT).init(key, OpType.INSERT);
    }
    
    /**
     * Begin an insert operation on multiple keys.
     */
    public ChainableOperationBuilder insert(List<Key> keys) {
        return new ChainableOperationBuilder(this, OpType.INSERT).init(keys, OpType.INSERT);
    }
    
    /**
     * Begin an insert operation on multiple keys.
     */
    public ChainableOperationBuilder insert(Key key1, Key key2, Key... keys) {
        return new ChainableOperationBuilder(this, OpType.INSERT).init(buildKeyList(key1, key2, keys), OpType.INSERT);
    }

    public <T> ChainableOperationBuilder insert(TypedKey<T> key) {
        return insert(key.getNativeKey());
    }

    public <T> ChainableOperationBuilder insertKeys(List<TypedKey<T>> keys) {
        return insert(nativeKeysFromTyped(keys));
    }
    
    /**
     * Begin an update operation.
     */
    public ChainableOperationBuilder update(Key key) {
        return new ChainableOperationBuilder(this, OpType.UPDATE).init(key, OpType.UPDATE);
    }
    
    /**
     * Begin an update operation on multiple keys.
     */
    public ChainableOperationBuilder update(List<Key> keys) {
        return new ChainableOperationBuilder(this, OpType.UPDATE).init(keys, OpType.UPDATE);
    }
    
    /**
     * Begin an update operation on multiple keys.
     */
    public ChainableOperationBuilder update(Key key1, Key key2, Key... keys) {
        return new ChainableOperationBuilder(this, OpType.UPDATE).init(buildKeyList(key1, key2, keys), OpType.UPDATE);
    }

    public <T> ChainableOperationBuilder update(TypedKey<T> key) {
        return update(key.getNativeKey());
    }

    public <T> ChainableOperationBuilder updateKeys(List<TypedKey<T>> keys) {
        return update(nativeKeysFromTyped(keys));
    }
    
    /**
     * Begin an upsert operation.
     */
    public ChainableOperationBuilder upsert(Key key) {
        return new ChainableOperationBuilder(this, OpType.UPSERT).init(key, OpType.UPSERT);
    }
    
    /**
     * Begin an upsert operation on multiple keys.
     */
    public ChainableOperationBuilder upsert(List<Key> keys) {
        return new ChainableOperationBuilder(this, OpType.UPSERT).init(keys, OpType.UPSERT);
    }
    
    /**
     * Begin an upsert operation on multiple keys.
     */
    public ChainableOperationBuilder upsert(Key key1, Key key2, Key... keys) {
        return new ChainableOperationBuilder(this, OpType.UPSERT).init(buildKeyList(key1, key2, keys), OpType.UPSERT);
    }

    public <T> ChainableOperationBuilder upsert(TypedKey<T> key) {
        return upsert(key.getNativeKey());
    }

    public <T> ChainableOperationBuilder upsertKeys(List<TypedKey<T>> keys) {
        return upsert(nativeKeysFromTyped(keys));
    }
    
    /**
     * Begin a replace operation.
     */
    public ChainableOperationBuilder replace(Key key) {
        return new ChainableOperationBuilder(this, OpType.REPLACE).init(key, OpType.REPLACE);
    }
    
    /**
     * Begin a replace operation on multiple keys.
     */
    public ChainableOperationBuilder replace(List<Key> keys) {
        return new ChainableOperationBuilder(this, OpType.REPLACE).init(keys, OpType.REPLACE);
    }
    
    /**
     * Begin a replace operation on multiple keys.
     */
    public ChainableOperationBuilder replace(Key key1, Key key2, Key... keys) {
        return new ChainableOperationBuilder(this, OpType.REPLACE).init(buildKeyList(key1, key2, keys), OpType.REPLACE);
    }

    public <T> ChainableOperationBuilder replace(TypedKey<T> key) {
        return replace(key.getNativeKey());
    }

    public <T> ChainableOperationBuilder replaceKeys(List<TypedKey<T>> keys) {
        return replace(nativeKeysFromTyped(keys));
    }
    
    /**
     * Begin a touch operation. Chainable with other operations.
     */
    public ChainableNoBinsBuilder touch(Key key) {
        return new ChainableNoBinsBuilder(this, new java.util.ArrayList<>(), null, getCurrentTransaction())
                .touch(key);
    }

    /**
     * Begin a touch operation on multiple keys.
     */
    public ChainableNoBinsBuilder touch(Key key1, Key key2, Key ... keys) {
        return new ChainableNoBinsBuilder(this, new java.util.ArrayList<>(), null, getCurrentTransaction())
                .touch(buildKeyList(key1, key2, keys));
    }

    /**
     * Begin a touch operation on multiple keys.
     */
    public ChainableNoBinsBuilder touch(List<Key> keys) {
        return new ChainableNoBinsBuilder(this, new java.util.ArrayList<>(), null, getCurrentTransaction())
                .touch(keys);
    }

    public <T> ChainableNoBinsBuilder touch(TypedKey<T> key) {
        return touch(key.getNativeKey());
    }

    public <T> ChainableNoBinsBuilder touchKeys(List<TypedKey<T>> keys) {
        return touch(nativeKeysFromTyped(keys));
    }

    /**
     * Begin an exists operation. Chainable with other operations.
     */
    public ChainableNoBinsBuilder exists(Key key) {
        return new ChainableNoBinsBuilder(this, new java.util.ArrayList<>(), null, getCurrentTransaction())
                .exists(key);
    }
    
    /**
     * Begin an exists operation on multiple keys.
     */
    public ChainableNoBinsBuilder exists(Key key1, Key key2, Key ... keys) {
        return new ChainableNoBinsBuilder(this, new java.util.ArrayList<>(), null, getCurrentTransaction())
                .exists(buildKeyList(key1, key2, keys));
    }

    /**
     * Begin an exists operation on multiple keys.
     */
    public ChainableNoBinsBuilder exists(List<Key> keys) {
        return new ChainableNoBinsBuilder(this, new java.util.ArrayList<>(), null, getCurrentTransaction())
                .exists(keys);
    }

    public <T> ChainableNoBinsBuilder exists(TypedKey<T> key) {
        return exists(key.getNativeKey());
    }

    public <T> ChainableNoBinsBuilder existsKeys(List<TypedKey<T>> keys) {
        return exists(nativeKeysFromTyped(keys));
    }
    
    /**
     * Begin a delete operation. Chainable with other operations.
     */
    public ChainableNoBinsBuilder delete(Key key) {
        return new ChainableNoBinsBuilder(this, new java.util.ArrayList<>(), null, getCurrentTransaction())
                .delete(key);
    }
    
    /**
     * Begin a delete operation on multiple keys.
     */
    public ChainableNoBinsBuilder delete(Key key1, Key key2, Key ... keys) {
        return new ChainableNoBinsBuilder(this, new java.util.ArrayList<>(), null, getCurrentTransaction())
                .delete(buildKeyList(key1, key2, keys));
    }
    
    /**
     * Begin a delete operation on multiple keys.
     */
    public ChainableNoBinsBuilder delete(List<Key> keys) {
        return new ChainableNoBinsBuilder(this, new java.util.ArrayList<>(), null, getCurrentTransaction())
                .delete(keys);
    }

    public <T> ChainableNoBinsBuilder delete(TypedKey<T> key) {
        return delete(key.getNativeKey());
    }

    public <T> ChainableNoBinsBuilder deleteKeys(List<TypedKey<T>> keys) {
        return delete(nativeKeysFromTyped(keys));
    }

    // --------------------------------
    // Object mapping functionality
    // --------------------------------
    public <T> OperationObjectBuilder<T> insert(TypedDataSet<T> dataSet) {
        return new OperationObjectBuilder<T>(this, dataSet, OpType.INSERT);
    }
    
    public <T> OperationObjectBuilder<T> upsert(TypedDataSet<T> dataSet) {
        return new OperationObjectBuilder<T>(this, dataSet, OpType.UPSERT);
    }
    
    public <T> OperationObjectBuilder<T> update(TypedDataSet<T> dataSet) {
        return new OperationObjectBuilder<T>(this, dataSet, OpType.UPDATE);
    }
    

    // ---------------------------
    // Transaction functionality
    // ---------------------------
    /**
     * Return the current transaction, if any.
     * @return
     */
    public Txn getCurrentTransaction() {
        return null;
    }
    
    // --------------------------------------
    // Transaction helper methods
    // --------------------------------------
    // Functional interface for returning a result
    @FunctionalInterface
    public interface Transactional<T> {
        T execute(TransactionalSession txn);
    }
    
    // Functional interface for void-returning operations
    @FunctionalInterface
    public interface TransactionalVoid {
        void execute(TransactionalSession txn);
    }
    
    /**
     * Executes a transactional operation and returns a value.
     * 
     * <p>Use this method when your transaction needs to return a result, such as
     * reading data or computing a value based on transactional operations.</p>
     * 
     * <p><b>Why the different name?</b> This method is named differently from 
     * {@link #doInTransaction(TransactionalVoid)} to avoid Java type inference ambiguity 
     * with complex lambda bodies. Without distinct names, the compiler cannot determine 
     * which overload to use when the lambda contains control flow statements like 
     * {@code while} loops, forcing users to add explicit {@code return null;} statements.</p>
     * 
     * <p>The transaction provides automatic retry logic for transient failures and ensures
     * proper cleanup. Operations will be retried automatically for result codes like
     * MRT_BLOCKED, MRT_VERSION_MISMATCH, and TXN_FAILED.</p>
     * 
     * <p><b>Example usage:</b>
     * <pre>{@code
     * String userName = session.doInTransactionReturning(tx -> {
     *     RecordStream results = tx.query(users.id(userId)).execute();
     *     Record record = results.getFirst().get().recordOrThrow();
     *     return record.getString("name");
     * });
     * }</pre>
     * 
     * @param <T> the return type
     * @param operation the transactional operation to execute
     * @return the value returned by the operation
     * @throws AerospikeException if the operation fails with a non-retryable error
     * @throws RuntimeException if any other exception occurs during execution
     * @see TransactionalSession#doInTransactionReturning(Transactional)
     * @see #doInTransaction(TransactionalVoid)
     */
    public <T> T doInTransactionReturning(Transactional<T> operation) {
        return new TransactionalSession(cluster, behavior).doInTransactionReturning(operation);
    }
    
    /**
     * Executes a transactional operation that does not return a value.
     * 
     * <p>Use this method when your transaction only needs to perform operations
     * without returning a result to the caller. This is the most common case for
     * transactional writes and updates.</p>
     * 
     * <p>The transaction provides automatic retry logic for transient failures and ensures
     * proper cleanup. Operations will be retried automatically for result codes like
     * MRT_BLOCKED, MRT_VERSION_MISMATCH, and TXN_FAILED.</p>
     * 
     * <p><b>Example usage:</b>
     * <pre>{@code
     * session.doInTransaction(txn -> {
     *     txn.upsert(accounts.id("acc1"))
     *         .bin("balance").add(-100)
     *         .execute();
     *     txn.upsert(accounts.id("acc2"))
     *         .bin("balance").add(100)
     *         .execute();
     * });
     * }</pre>
     * 
     * @param operation the transactional operation to execute
     * @throws AerospikeException if the operation fails with a non-retryable error
     * @throws RuntimeException if any other exception occurs during execution
     * @see TransactionalSession#doInTransaction(TransactionalVoid)
     * @see #doInTransactionReturning(Transactional)
     */
    public void doInTransaction(TransactionalVoid operation) {
        new TransactionalSession(cluster, behavior).doInTransaction(txn -> {
            operation.execute(txn);
//            return null; // Hidden from user
        });
    }
    
    // ------------------------------------
    // Background Operations functionality
    // ------------------------------------
    /**
     * Enter background task mode for performing set-level operations asynchronously
     * on the server side. Background operations run as server-side scans/queries
     * and return an ExecuteTask for monitoring completion.
     * 
     * <p><b>Background Operations:</b></p>
     * <ul>
     *   <li>Run on entire sets (not specific keys)</li>
     *   <li>Cannot be part of transactions</li>
     *   <li>Return ExecuteTask (not record data)</li>
     *   <li>Support UPDATE, DELETE, and TOUCH operations only</li>
     * </ul>
     * 
     * <p><b>Use Cases:</b></p>
     * <ul>
     *   <li>Bulk updates based on criteria</li>
     *   <li>Cleaning up old records</li>
     *   <li>Extending TTL for active records</li>
     * </ul>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * // Update all customers over 30
     * ExecuteTask task = session.backgroundTask()
     *     .update(customerDataSet)
     *     .where("$.age > 30")
     *     .bin("category").setTo("senior")
     *     .execute();
     * 
     * task.waitTillComplete();
     * 
     * // Delete old inactive records
     * ExecuteTask deleteTask = session.backgroundTask()
     *     .delete(customerDataSet)
     *     .where("$.lastLogin < 1609459200000")
     *     .execute();
     * 
     * // Touch active users to extend TTL
     * ExecuteTask touchTask = session.backgroundTask()
     *     .touch(activeUsers)
     *     .where("$.status == 'active'")
     *     .expireRecordAfter(Duration.ofDays(30))
     *     .execute();
     * }</pre>
     * 
     * @return BackgroundTaskSession for creating background operations
     * @see BackgroundTaskSession
     * @see BackgroundOperationBuilder
     */
    public BackgroundTaskSession backgroundTask() {
        return new BackgroundTaskSession(this);
    }
    
    // ---------------------
    // Info functionality
    // ---------------------
    public InfoCommands info() {
        return new InfoCommands(this);
    }
    
    public boolean isNamespaceSC(String namespace) {
        Partitions partitionMap = this.getClient().getCluster().partitionMap.get(namespace);
        if (partitionMap == null) {
            throw new IllegalArgumentException("Unknown namespace " + namespace);
        }
        return partitionMap.scMode;
    }
}
