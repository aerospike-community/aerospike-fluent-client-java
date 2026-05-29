package com.aerospike;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.ResultCode;

/**
 * Type-safe record stream carrying {@code Class<T>} for factory-based object mapping.
 *
 * <p>Wraps a {@link RecordStream}; iteration and resource management delegate to it.</p>
 *
 * @param <T> mapped entity type
 */
public class TypedRecordStream<T> implements Iterator<RecordResult>, Closeable {

    private final Session session;
    private final Class<T> entityType;
    private final RecordStream underlying;

    /**
     * Sentinel empty stream (no session; mapping methods throw).
     */
    public TypedRecordStream() {
        this.session = null;
        this.entityType = null;
        this.underlying = new RecordStream();
    }

    /**
     * Full constructor used by queries and object operations.
     */
    public TypedRecordStream(Session session, Class<T> entityType, RecordStream underlying) {
        this.session = Objects.requireNonNull(session, "session");
        this.entityType = Objects.requireNonNull(entityType, "entityType");
        this.underlying = underlying != null ? underlying : new RecordStream();
    }

    public Class<T> getEntityType() {
        return entityType;
    }

    public Session getSession() {
        return session;
    }

    public RecordStream asUntypedRecordStream() {
        return underlying;
    }

    public boolean hasMoreChunks() {
        return underlying.hasMoreChunks();
    }

    @Override
    public boolean hasNext() {
        return underlying.hasNext();
    }

    @Override
    public RecordResult next() {
        return underlying.next();
    }

    public Stream<RecordResult> stream() {
        Stream<RecordResult> records = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(this, Spliterator.NONNULL | Spliterator.IMMUTABLE), false);
        records.onClose(this::close);
        return records;
    }

    public TypedRecordStream<T> failures() {
        List<RecordResult> failedRecords = new ArrayList<>();
        while (hasNext()) {
            RecordResult result = next();
            if (result.resultCode() != ResultCode.OK) {
                failedRecords.add(result);
            }
        }
        close();
        if (session != null && entityType != null) {
            return new TypedRecordStream<>(session, entityType, new RecordStream(failedRecords, 0L));
        }
        return new TypedRecordStream<>();
    }

    /**
     * Maps all remaining records using the {@link RecordMappingFactory} (no explicit mapper).
     */
    public List<T> toObjectList() {
        RecordReadContext<T> ctx = mappingContext();
        RecordMapper<T> mapper = MappingSupport.requireMapper(ctx.getRecordMappingFactory(), entityType);
        List<T> result = new ArrayList<>();
        while (hasNext()) {
            RecordResult keyRecord = next();
            Record rec = keyRecord.recordOrThrow();
            result.add(mapper.fromMap(rec.bins, keyRecord.key(), rec.generation, ctx));
        }
        return result;
    }

    /**
     * Maps all remaining records using an explicit mapper.
     */
    public List<T> toObjectList(RecordMapper<T> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        RecordReadContext<T> ctx = mappingContext();
        List<T> result = new ArrayList<>();
        while (hasNext()) {
            RecordResult keyRecord = next();
            Record rec = keyRecord.recordOrThrow();
            result.add(mapper.fromMap(rec.bins, keyRecord.key(), rec.generation, ctx));
        }
        return result;
    }

    public void forEachObject(Consumer<T> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        RecordReadContext<T> ctx = mappingContext();
        RecordMapper<T> mapper = MappingSupport.requireMapper(ctx.getRecordMappingFactory(), entityType);
        while (hasNext()) {
            RecordResult keyRecord = next();
            Record rec = keyRecord.recordOrThrow();
            consumer.accept(mapper.fromMap(rec.bins, keyRecord.key(), rec.generation, ctx));
        }
    }

    public Stream<T> streamObjects() {
        RecordReadContext<T> ctx = mappingContext();
        RecordMapper<T> mapper = MappingSupport.requireMapper(ctx.getRecordMappingFactory(), entityType);
        Stream<RecordResult> base = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(this, Spliterator.NONNULL | Spliterator.IMMUTABLE), false);
        return base.onClose(this::close).map(rr -> {
            Record rec = rr.recordOrThrow();
            return mapper.fromMap(rec.bins, rr.key(), rec.generation, ctx);
        });
    }

    public TypedNavigatableRecordStream<T> asNavigatableStream() {
        RecordStream bufferSource = drainToRecordStream(0);
        return new TypedNavigatableRecordStream<>(session, entityType, new NavigatableRecordStream(bufferSource));
    }

    public TypedNavigatableRecordStream<T> asNavigatableStream(long limit) {
        RecordStream bufferSource = drainToRecordStream(limit);
        return new TypedNavigatableRecordStream<>(session, entityType, new NavigatableRecordStream(bufferSource));
    }

    private RecordStream drainToRecordStream(long limit) {
        List<RecordResult> recordList = new ArrayList<>();
        try {
            int count = 0;
            while (underlying.hasNext() && (limit <= 0 || count < limit)) {
                recordList.add(underlying.next());
                count++;
            }
        } finally {
            underlying.close();
        }
        return new RecordStream(recordList, 0L);
    }

    public void forEach(Consumer<RecordResult> consumer) {
        underlying.forEach(consumer);
    }

    public Optional<Record> get(Key key) {
        return underlying.get(key);
    }

    public <M> Optional<M> get(Key key, RecordMapper<M> mapper) {
        return underlying.get(key, mapper);
    }

    public Record getFirstRecord() {
        return underlying.getFirstRecord();
    }

    public Optional<RecordResult> getFirst() {
        return underlying.getFirst();
    }

    public Optional<RecordResult> getFirst(boolean throwException) {
        return underlying.getFirst(throwException);
    }

    public Optional<T> getFirstObject() {
        RecordReadContext<T> ctx = mappingContext();
        RecordMapper<T> mapper = MappingSupport.requireMapper(ctx.getRecordMappingFactory(), entityType);
        if (!hasNext()) {
            return Optional.empty();
        }
        RecordResult item = next();
        Record rec = item.recordOrThrow();
        return Optional.of(mapper.fromMap(rec.bins, item.key(), rec.generation, ctx));
    }

    public Optional<T> getFirst(RecordMapper<T> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        RecordReadContext<T> ctx = mappingContext();
        if (!hasNext()) {
            return Optional.empty();
        }
        RecordResult item = next();
        Record rec = item.recordOrThrow();
        return Optional.of(mapper.fromMap(rec.bins, item.key(), rec.generation, ctx));
    }

    public Optional<Boolean> getFirstBoolean() {
        return underlying.getFirstBoolean();
    }

    public Optional<ObjectWithMetadata<T>> getFirstObjectWithMetadata() {
        RecordReadContext<T> ctx = mappingContext();
        RecordMapper<T> mapper = MappingSupport.requireMapper(ctx.getRecordMappingFactory(), entityType);
        if (!hasNext()) {
            return Optional.empty();
        }
        RecordResult item = next();
        Record rec = item.recordOrThrow();
        T object = mapper.fromMap(rec.bins, item.key(), rec.generation, ctx);
        return Optional.of(new ObjectWithMetadata<>(object, rec));
    }

    public Optional<ObjectWithMetadata<T>> getFirstWithMetadata(RecordMapper<T> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        RecordReadContext<T> ctx = mappingContext();
        if (!hasNext()) {
            return Optional.empty();
        }
        RecordResult item = next();
        Record rec = item.recordOrThrow();
        T object = mapper.fromMap(rec.bins, item.key(), rec.generation, ctx);
        return Optional.of(new ObjectWithMetadata<>(object, rec));
    }

    public static class ObjectWithMetadata<E> {
        private final int generation;
        private final int expiration;
        private final E object;

        public ObjectWithMetadata(E object, Record rec) {
            this.object = object;
            this.generation = rec.generation;
            this.expiration = rec.expiration;
        }

        public E get() {
            return object;
        }

        public int getExpiration() {
            return expiration;
        }

        public int getGeneration() {
            return generation;
        }
    }

    private RecordReadContext<T> mappingContext() {
        if (session == null || entityType == null) {
            throw new IllegalStateException(
                    "TypedRecordStream has no session or entity type; object mapping is not available on this stream");
        }
        return new RecordReadContext<>(session, entityType);
    }

    @Override
    public void close() {
        underlying.close();
    }
}
