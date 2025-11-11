package com.aerospike;

import java.util.ArrayList;
import java.util.List;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Txn;
import com.aerospike.client.cluster.Node;
import com.aerospike.client.cluster.Partitions;
import com.aerospike.client.exp.Exp;
import com.aerospike.client.exp.Expression;
import com.aerospike.info.InfoCommands;
import com.aerospike.policy.Behavior;
import com.aerospike.query.BaseQueryBuilder;
import com.aerospike.query.KeyBasedQueryBuilderInterface;
import com.aerospike.query.QueryBuilder;

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
    
    // TODO: Remove ASNode, InfoData
    public ASNode[] getNodes() {
        Node[] nodes = this.client.getNodes();
        ASNode[] asNodes = new ASNode[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            asNodes[i] = new ASNode(nodes[i]);
        }
        return asNodes;
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

    public Cluster getCluster() {
        return cluster;
    }
    
    public void truncate(DataSet set) {
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
    
    // --------------------------------------------
    // Query functionality
    // --------------------------------------------
    public BaseQueryBuilder<QueryBuilder> query(DataSet dataSet) {
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
    
    // -------------------
    // CUD functionality
    // -------------------
    public OperationBuilder insert(Key key) {
        return new OperationBuilder(this, key, OpType.INSERT);
    }
    
    public OperationBuilder update(Key key) {
        return new OperationBuilder(this, key, OpType.UPDATE);
    }
    
    public OperationBuilder upsert(Key key) {
        return new OperationBuilder(this, key, OpType.UPSERT);
    }
    
    public OperationBuilder replace(Key key) {
        return new OperationBuilder(this, key, OpType.UPSERT);
    }
    
    public OperationBuilder upsert(List<Key> keys) {
        return new OperationBuilder(this, keys, OpType.UPSERT);
    }
    
    public OperationBuilder upsert(Key key1, Key key2, Key... keys) {
        List<Key> keyList = buildKeyList(key1, key2, keys);
        return new OperationBuilder(this, keyList, OpType.UPSERT);
    }
    
    public OperationBuilder insert(List<Key> keys) {
        return new OperationBuilder(this, keys, OpType.INSERT);
    }
    
    public OperationBuilder insert(Key key1, Key key2, Key... keys) {
        List<Key> keyList = buildKeyList(key1, key2, keys);
        return new OperationBuilder(this, keyList, OpType.INSERT);
    }
    
    public OperationBuilder update(List<Key> keys) {
        return new OperationBuilder(this, keys, OpType.UPDATE);
    }
    
    public OperationBuilder update(Key key1, Key key2, Key... keys) {
        List<Key> keyList = buildKeyList(key1, key2, keys);
        return new OperationBuilder(this, keyList, OpType.UPDATE);
    }
    
    public OperationBuilder replace(List<Key> keys) {
        return new OperationBuilder(this, keys, OpType.REPLACE);
    }
    
    public OperationBuilder replace(Key key1, Key key2, Key... keys) {
        List<Key> keyList = buildKeyList(key1, key2, keys);
        return new OperationBuilder(this, keyList, OpType.REPLACE);
    }
    
    public OperationWithNoBinsBuilder touch(Key key) {
        return new OperationWithNoBinsBuilder(this, key, OpType.TOUCH);
    }

    public OperationWithNoBinsBuilder touch(Key key1, Key key2, Key ... keys) {
        return new OperationWithNoBinsBuilder(this, buildKeyList(key1, key2, keys), OpType.TOUCH);
    }

    public OperationWithNoBinsBuilder touch(List<Key> keys) {
        return new OperationWithNoBinsBuilder(this, keys, OpType.TOUCH);
    }

    public OperationWithNoBinsBuilder exists(Key key) {
        return new OperationWithNoBinsBuilder(this, key, OpType.EXISTS);
    }
    
    public OperationWithNoBinsBuilder exists(Key key1, Key key2, Key ... keys) {
        return new OperationWithNoBinsBuilder(this, buildKeyList(key1, key2, keys), OpType.EXISTS);
    }

    public OperationWithNoBinsBuilder exists(List<Key> keys) {
        return new OperationWithNoBinsBuilder(this, keys, OpType.EXISTS);
    }
    public OperationWithNoBinsBuilder delete(Key key) {
        return new OperationWithNoBinsBuilder(this, key, OpType.DELETE);
    }

    public OperationWithNoBinsBuilder delete(Key key1, Key key2, Key ... keys) {
        return new OperationWithNoBinsBuilder(this, buildKeyList(key1, key2, keys), OpType.DELETE);
    }

    public OperationWithNoBinsBuilder delete(List<Key> keys) {
        return new OperationWithNoBinsBuilder(this, keys, OpType.DELETE);
    }

    // --------------------------------
    // Object mapping functionality
    // --------------------------------
    public OperationObjectBuilder insert(DataSet dataSet) {
        return new OperationObjectBuilder(this, dataSet, OpType.INSERT);
    }
    
    public <T> OperationObjectBuilder<T> insert(TypeSafeDataSet<T> dataSet) {
        return new OperationObjectBuilder<T>(this, dataSet, OpType.INSERT);
    }
    
    public OperationObjectBuilder upsert(DataSet dataSet) {
        return new OperationObjectBuilder(this, dataSet, OpType.UPSERT);
    }
    
    public <T> OperationObjectBuilder<T> upsert(TypeSafeDataSet<T> dataSet) {
        return new OperationObjectBuilder<T>(this, dataSet, OpType.UPSERT);
    }
    
    public OperationObjectBuilder update(DataSet dataSet) {
        return new OperationObjectBuilder(this, dataSet, OpType.UPDATE);
    }
    
    public <T> OperationObjectBuilder<T> update(TypeSafeDataSet<T> dataSet) {
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
    
    public <T> T doInTransaction(Transactional<T> operation) {
        return new TransactionalSession(cluster, behavior).doInTransaction(operation);
    }
    
    public void doInTransaction(TransactionalVoid operation) {
        new TransactionalSession(cluster, behavior).doInTransaction(txn -> {
            operation.execute(txn);
//            return null; // Hidden from user
        });
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
