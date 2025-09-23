package com.aerospike;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.KeyRecord;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;

public class DataSet_Old {
    private final String namespace;
    private final String setName;
    private final IAerospikeClient client;
    
    // Package visibility
    DataSet_Old(IAerospikeClient client, String namespace, String setName) {
        super();
        this.namespace = namespace;
        this.setName = setName;
        this.client = client;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getSetName() {
        return setName;
    }
    
    public Key id(String id) {
        return new Key(namespace, setName, id);
    }
    public Key id(byte[] id) {
        return new Key(namespace, setName, id);
    }
    public Key id(byte[] id, int offset, int length) {
        return new Key(namespace, setName, id, offset, length);
    }
    public Key id(int id) {
        return new Key(namespace, setName, id);
    }
    public Key id(long id) {
        return new Key(namespace, setName, id);
    }
    
    public Stream<KeyRecord> query() {
        return this.query((Filter)null);
    }
    /** 
     * Get a stream of records from the servers. Note that the streams <b>must</b> be closed when complete.
     * @param client
     * @param filter
     * @return
     */
    public Stream<KeyRecord> query(Filter filter) {
        Statement stmt = new Statement();
        stmt.setFilter(filter);
        stmt.setNamespace(namespace);
        stmt.setSetName(this.setName);
        final RecordSet recordSet = client.query(null, stmt);
        
        Stream<KeyRecord> records = StreamSupport.stream(Spliterators.spliteratorUnknownSize(recordSet.iterator(), Spliterator.NONNULL | Spliterator.IMMUTABLE), false);
        records.onClose(() -> recordSet.close());
        return records;
    }
    
    private Stream<KeyRecord> query(Key key) {
        Record rec = client.get(null, key);
        return Stream.of(new KeyRecord(key, rec));
    }
    
    public Stream<KeyRecord> query(long id) {
        return this.query(this.id(id));
    }

    public Stream<KeyRecord> query(long id1, long id2, long ...ids) {
        Key[] keys = new Key[ids.length + 2];
        keys[0] = this.id(id1);
        keys[1] = this.id(id2);
        for (int i = 0; i < ids.length; i++) {
            keys[2+i] = this.id(ids[i]);
        }
        Record[] rec = client.get(null, keys);
        Stream.Builder<KeyRecord> builder = Stream.builder();
        for (int i = 0; i < keys.length; i++) {
            builder.add(new KeyRecord(keys[i], rec[i]));
        }
        return builder.build();
    }

}
