package com.aerospike.query;

import java.util.Iterator;

import com.aerospike.client.query.KeyRecord;

public interface RecordStreamImpl extends Iterator<KeyRecord>{
    boolean hasMorePages();
    boolean hasNext();
    KeyRecord next();
    void close();
}
