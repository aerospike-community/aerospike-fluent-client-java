package com.aerospike.query;

import java.util.Iterator;

import com.aerospike.RecordResult;

public interface RecordStreamImpl extends Iterator<RecordResult>{
    boolean hasMorePages();
    boolean hasNext();
    RecordResult next();
    void close();
}
