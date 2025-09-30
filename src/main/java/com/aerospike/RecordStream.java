package com.aerospike;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.query.KeyRecord;
import com.aerospike.client.query.PartitionFilter;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import com.aerospike.query.FixedSizeRecordStream;
import com.aerospike.query.RecordStreamImpl;
import com.aerospike.query.ResettablePagination;
import com.aerospike.query.SingleItemRecordStream;
import com.aerospike.query.SortProperties;
import com.aerospike.query.Sortable;

public class RecordStream implements Iterator<KeyRecord>, Closeable {
    private final RecordStreamImpl impl;
    public RecordStream() {impl = null;}
    
    public RecordStream(Key key, Record record) {
        impl = new SingleItemRecordStream(key, record);
    }
    public RecordStream(Key[] keys, Record[] records, long limit, int pageSize, List<SortProperties> sortProperties) {
        impl = new FixedSizeRecordStream(keys, records, limit, pageSize, sortProperties);
    }
    
    public RecordStream(Session session, QueryPolicy queryPolicy, Statement statement,
            PartitionFilter filter, RecordSet recordSet, long limit, List<SortProperties> sortProperties) {

        boolean hasSortProperties = !(sortProperties == null || sortProperties.isEmpty());
        if (hasSortProperties && limit <= 0) {
            throw new IllegalArgumentException(
                    "A query with unbounded results must have a limit set on it if sorting is required. This is to ensure "
                    + "that results can be delivered in resonable time without excessive amounts of memory being used.");
        }
        if (limit <= 0) {
            limit = Long.MAX_VALUE;
        }
        if (!hasSortProperties) {
            // Not a sortable record set, just use the inbuilt stream / pagination interface
            impl = new PaginatedRecordStream(session, queryPolicy, statement, filter, recordSet, limit);
        }
        else {
            // Sortable record sets must use the array implementation, so fetch all records
            int count = 0;
            List<KeyRecord> recordList = new ArrayList<>();
            while (count < limit && !filter.isDone()) {
                while (count < limit && recordSet.next()) {
                    recordList.add(recordSet.getKeyRecord());
                    count++;
                }
                recordSet = session.getClient().queryPartitions(queryPolicy, statement, filter);
            }
            recordSet.close();
            impl = new FixedSizeRecordStream(recordList.toArray(new KeyRecord[0]), 0, (int)statement.getMaxRecords(), sortProperties);
        }
    }
    
    public boolean hasMorePages() {
        return impl == null ? false : impl.hasMorePages();
    }
    @Override
    public boolean hasNext() {
        return impl == null ? false : impl.hasNext();
    }

    @Override
    public KeyRecord next() {
        return impl == null ? null : impl.next();
    }

    /**
     * Convert the elements in this RecordStream into a Java Stream class. Note that this loses 
     * pagination information, all the records are accessible through the Stream.
     * @return
     */
    public Stream<KeyRecord> stream() {
        Stream<KeyRecord> records = StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                        this, Spliterator.NONNULL | Spliterator.IMMUTABLE), false);
        records.onClose(() -> {
            this.close();
        });
        return records;
    }
    
    /**
     * Return the records from the current page as a list of entities.
     * @param <T>
     * @param mapper
     * @return
     */
    public <T> List<T> toObjectList(RecordMapper<T> mapper) {
        List<T> result = new ArrayList<>();
        while (hasNext()) {
            KeyRecord keyRecord = next();
            result.add(mapper.fromMap(keyRecord.record.bins, keyRecord.key, keyRecord.record.generation));
        }
        return result;
    }

    /**
     * If the current record stream is able to be sorted, return a sortable interface to be able
     * to set the sort criteria. If the current record stream is not able to be sorted, {@code Optional.empty()}
     * is returned.
     * @return
     */
    public Optional<Sortable> asSortable() {
        if (impl instanceof Sortable) {
            return Optional.of((Sortable)impl);
        }
        return Optional.empty();
    }
    
    public Optional<ResettablePagination> asResettablePagination() {
        if (impl instanceof ResettablePagination) {
            ResettablePagination rp = (ResettablePagination)impl;
            if (rp.maxPages() > 1) {
                return Optional.of(rp);
            }
        }
        return Optional.empty();
    }
    
    public void forEach(Consumer<KeyRecord> consumer) {
        while (hasNext()) {
            consumer.accept(next());
        }
    }
    
    public Optional<Record> get(Key key) {
        while (hasNext()) {
            KeyRecord kr = next();
            if (kr.key.equals(key)) {
                return Optional.of(kr.record);
            }
        }
        return Optional.empty();
    }

        /**
         * Find a particular key in the stream and return the data associated with that key, or {@code Optional.empty}
         * if the key doesn't exist. Note that if the stream is not generated from a {@code Key} or {@code List<Key>}
         * then finding the key will consume elements in the stream which may not be able to be replayed.
         * @param <T> - The type of the object to be returned.
         * @param key - The key of the record
         * @param mapper - The mapper to use to convert the record to the class
         * @return An optional containing the data or empty
         */
    public <T> Optional<T> get(Key key, RecordMapper<T> mapper) {
        while (hasNext()) {
            KeyRecord kr = next();
            if (kr.key.equals(key)) {
                return Optional.of(mapper.fromMap(kr.record.bins, kr.key, kr.record.generation));
            }
        }
        return Optional.empty();
    }

    public Optional<KeyRecord> getFirst() {
        if (hasNext()) {
            return Optional.of(next());
        }
        return Optional.empty();
    }
    
    public <T> Optional<T> getFirst(RecordMapper<T> mapper) {
        if (hasNext()) {
            KeyRecord item = next();
            return Optional.of(mapper.fromMap(item.record.bins, item.key, item.record.generation));
        }
        return Optional.empty();
    }

    public static class ObjectWithMetadata<T> {
        private final int generation;
        private final int expiration;
        private final T object;
        public ObjectWithMetadata(T object, Record rec) {
            this.object = object;
            this.generation = rec.generation;
            this.expiration = rec.expiration;
        }
        
        public T get() {
            return object;
        }
        
        public int getExpiration() {
            return expiration;
        }
        
        public int getGeneration() {
            return generation;
        }
    }
    public <T> Optional<ObjectWithMetadata<T>> getFirstWithMetadata(RecordMapper<T> mapper) {
        if (hasNext()) {
            KeyRecord item = next();
            T object = mapper.fromMap(item.record.bins, item.key, item.record.generation);
            return Optional.of(new ObjectWithMetadata<T>(object, item.record));
        }
        return Optional.empty();
    }


    @Override
    public void close() {
        if (impl != null) {
            impl.close();
        }
    }
}
