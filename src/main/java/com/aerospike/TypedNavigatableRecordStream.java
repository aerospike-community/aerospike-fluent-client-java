package com.aerospike;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.exception.AeroException;
import com.aerospike.query.ResettablePagination;
import com.aerospike.query.SortDir;
import com.aerospike.query.SortProperties;

/**
 * Navigable in-memory view of records with type-safe object mapping via {@link RecordMappingFactory}.
 *
 * @param <T> entity type
 */
public final class TypedNavigatableRecordStream<T> implements ResettablePagination, Closeable {

    private final Session session;
    private final Class<T> entityType;
    private final NavigatableRecordStream delegate;

    public TypedNavigatableRecordStream(Session session, Class<T> entityType, NavigatableRecordStream delegate) {
        this.session = Objects.requireNonNull(session, "session");
        this.entityType = Objects.requireNonNull(entityType, "entityType");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public Class<T> getEntityType() {
        return entityType;
    }

    public Session getSession() {
        return session;
    }

    public NavigatableRecordStream asUntypedNavigatableRecordStream() {
        return delegate;
    }

    public TypedNavigatableRecordStream<T> pageSize(int pageSize) {
        delegate.pageSize(pageSize);
        return this;
    }

    public TypedNavigatableRecordStream<T> sortBy(String field) {
        delegate.sortBy(field);
        return this;
    }

    public TypedNavigatableRecordStream<T> sortBy(String field, boolean caseInsensitive) {
        delegate.sortBy(field, caseInsensitive);
        return this;
    }

    public TypedNavigatableRecordStream<T> sortBy(String field, SortDir sortDir) {
        delegate.sortBy(field, sortDir);
        return this;
    }

    public TypedNavigatableRecordStream<T> sortBy(String field, SortDir sortDir, boolean caseSensitive) {
        delegate.sortBy(field, sortDir, caseSensitive);
        return this;
    }

    public TypedNavigatableRecordStream<T> sortBy(List<SortProperties> sortPropertyList) {
        delegate.sortBy(sortPropertyList);
        return this;
    }

    public TypedNavigatableRecordStream<T> sortBy(SortProperties... sortPropertyList) {
        delegate.sortBy(sortPropertyList);
        return this;
    }

    public TypedNavigatableRecordStream<T> sortBy(SortProperties sortProperty) {
        delegate.sortBy(sortProperty);
        return this;
    }

    public boolean hasMorePages() {
        return delegate.hasMorePages();
    }

    public boolean hasNext() {
        return delegate.hasNext();
    }

    public RecordResult next() {
        return delegate.next();
    }

    @Override
    public int currentPage() {
        return delegate.currentPage();
    }

    @Override
    public int maxPages() {
        return delegate.maxPages();
    }

    @Override
    public void setPageTo(int newPage) {
        delegate.setPageTo(newPage);
    }

    public Stream<RecordResult> stream() {
        return delegate.stream();
    }

    public void forEach(Consumer<RecordResult> consumer) {
        delegate.forEach(consumer);
    }

    public List<T> toObjectList() {
        RecordReadContext<T> ctx = mappingContext();
        RecordMapper<T> mapper = MappingSupport.requireMapper(ctx.getRecordMappingFactory(), entityType);
        List<T> result = new ArrayList<>();
        while (delegate.hasNext()) {
            RecordResult keyRecord = delegate.next();
            Record rec = keyRecord.recordOrThrow();
            result.add(mapper.fromMap(rec.bins, keyRecord.key(), rec.generation, ctx));
        }
        return result;
    }

    public List<T> toObjectList(RecordMapper<T> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        RecordReadContext<T> ctx = mappingContext();
        List<T> result = new ArrayList<>();
        while (delegate.hasNext()) {
            RecordResult keyRecord = delegate.next();
            Record rec = keyRecord.recordOrThrow();
            result.add(mapper.fromMap(rec.bins, keyRecord.key(), rec.generation, ctx));
        }
        return result;
    }

    public void forEachObject(Consumer<T> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        RecordReadContext<T> ctx = mappingContext();
        RecordMapper<T> mapper = MappingSupport.requireMapper(ctx.getRecordMappingFactory(), entityType);
        while (delegate.hasNext()) {
            RecordResult keyRecord = delegate.next();
            Record rec = keyRecord.recordOrThrow();
            consumer.accept(mapper.fromMap(rec.bins, keyRecord.key(), rec.generation, ctx));
        }
    }

    public Stream<T> streamObjects() {
        RecordReadContext<T> ctx = mappingContext();
        RecordMapper<T> mapper = MappingSupport.requireMapper(ctx.getRecordMappingFactory(), entityType);
        return delegate.stream().map(rr -> {
            Record rec = rr.recordOrThrow();
            return mapper.fromMap(rec.bins, rr.key(), rec.generation, ctx);
        });
    }

    public Optional<RecordResult> getFirst() throws AeroException {
        return delegate.getFirst();
    }

    public Optional<RecordResult> getFirst(boolean throwException) {
        return delegate.getFirst(throwException);
    }

    public Optional<T> getFirstObject() {
        RecordReadContext<T> ctx = mappingContext();
        RecordMapper<T> mapper = MappingSupport.requireMapper(ctx.getRecordMappingFactory(), entityType);
        if (!delegate.hasNext()) {
            return Optional.empty();
        }
        RecordResult item = delegate.next();
        Record rec = item.recordOrThrow();
        return Optional.of(mapper.fromMap(rec.bins, item.key(), rec.generation, ctx));
    }

    public Optional<T> getFirst(RecordMapper<T> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        RecordReadContext<T> ctx = mappingContext();
        if (!delegate.hasNext()) {
            return Optional.empty();
        }
        RecordResult item = delegate.next();
        Record rec = item.recordOrThrow();
        return Optional.of(mapper.fromMap(rec.bins, item.key(), rec.generation, ctx));
    }

    public int size() {
        return delegate.size();
    }

    public TypedNavigatableRecordStream<T> reset() {
        delegate.reset();
        return this;
    }

    private RecordReadContext<T> mappingContext() {
        return new RecordReadContext<>(session, entityType);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
