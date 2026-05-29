package com.aerospike.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.aerospike.DataSet;
import com.aerospike.Session;
import com.aerospike.TypedDataSet;
import com.aerospike.TypedKey;
import com.aerospike.TypedRecordStream;
import com.aerospike.client.Key;
import com.aerospike.client.Txn;
import com.aerospike.client.exp.Exp;
import com.aerospike.dslobjects.BooleanExpression;

/**
 * Type-safe query builder: same filters and execution semantics as {@link QueryBuilder},
 * but {@code execute()} returns {@link TypedRecordStream} for factory-based mapping.
 *
 * @param <T> entity type (from {@link TypedDataSet} or {@link TypedKey})
 */
public final class TypedQueryBuilder<T> {

    private final Session session;
    private final Class<T> entityClass;
    private final QueryBuilder inner;

    public TypedQueryBuilder(Session session, TypedDataSet<T> dataSet) {
        this.session = Objects.requireNonNull(session, "session");
        this.entityClass = Objects.requireNonNull(dataSet.getClazz(), "entity class");
        this.inner = new QueryBuilder(session, DataSet.of(dataSet.getNamespace(), dataSet.getSet()));
    }

    public TypedQueryBuilder(Session session, TypedKey<T> key) {
        this.session = Objects.requireNonNull(session, "session");
        this.entityClass = Objects.requireNonNull(key.getClazz(), "entity class");
        this.inner = new QueryBuilder(session, key.getNativeKey());
    }

    public TypedQueryBuilder(Session session, List<TypedKey<T>> keys) {
        this.session = Objects.requireNonNull(session, "session");
        Objects.requireNonNull(keys, "keys");
        if (keys.isEmpty()) {
            throw new IllegalArgumentException("At least one TypedKey is required");
        }
        this.entityClass = Objects.requireNonNull(keys.get(0).getClazz(), "entity class");
        List<Key> nativeKeys = new ArrayList<>(keys.size());
        for (TypedKey<T> tk : keys) {
            if (!tk.getClazz().equals(entityClass)) {
                throw new IllegalArgumentException(
                        "All TypedKey instances must use the same entity class; expected "
                                + entityClass.getName() + " but found " + tk.getClazz().getName());
            }
            nativeKeys.add(tk.getNativeKey());
        }
        this.inner = new QueryBuilder(session, nativeKeys);
    }

    public TypedQueryBuilder<T> readingOnlyBins(String... binNames) {
        inner.readingOnlyBins(binNames);
        return this;
    }

    public TypedQueryBuilder<T> withNoBins() {
        inner.withNoBins();
        return this;
    }

    public TypedQueryBuilder<T> limit(long limit) {
        inner.limit(limit);
        return this;
    }

    public TypedQueryBuilder<T> chunkSize(int chunkSize) {
        inner.chunkSize(chunkSize);
        return this;
    }

    public TypedQueryBuilder<T> onPartition(int partId) {
        inner.onPartition(partId);
        return this;
    }

    public TypedQueryBuilder<T> onPartitionRange(int startIncl, int endExcl) {
        inner.onPartitionRange(startIncl, endExcl);
        return this;
    }

    public TypedQueryBuilder<T> failOnFilteredOut() {
        inner.failOnFilteredOut();
        return this;
    }

    public TypedQueryBuilder<T> respondAllKeys() {
        inner.respondAllKeys();
        return this;
    }

    public TypedQueryBuilder<T> recordsPerSecond(int recordsPerSecond) {
        inner.recordsPerSecond(recordsPerSecond);
        return this;
    }

    public TypedQueryBuilder<T> where(String dsl, Object... params) {
        inner.where(dsl, params);
        return this;
    }

    public TypedQueryBuilder<T> where(BooleanExpression dsl) {
        inner.where(dsl);
        return this;
    }

    public TypedQueryBuilder<T> where(PreparedDsl dsl, Object... params) {
        inner.where(dsl, params);
        return this;
    }

    public TypedQueryBuilder<T> where(Exp exp) {
        inner.where(exp);
        return this;
    }

    public TypedQueryBuilder<T> notInAnyTransaction() {
        inner.notInAnyTransaction();
        return this;
    }

    public TypedQueryBuilder<T> inTransaction(Txn txn) {
        inner.inTransaction(txn);
        return this;
    }

    public TypedRecordStream<T> execute() {
        return new TypedRecordStream<>(session, entityClass, inner.execute());
    }

    public TypedRecordStream<T> executeSync() {
        return new TypedRecordStream<>(session, entityClass, inner.executeSync());
    }

    public TypedRecordStream<T> executeAsync() {
        return new TypedRecordStream<>(session, entityClass, inner.executeAsync());
    }
}
