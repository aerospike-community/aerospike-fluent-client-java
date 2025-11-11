package com.aerospike.query;

import java.util.Iterator;

import com.aerospike.RecordResult;

/**
 * Internal interface for RecordStream implementations.
 * 
 * <p>This interface defines the contract for different types of record streams,
 * including chunked server-side streaming and simple iteration.</p>
 */
public interface RecordStreamImpl extends Iterator<RecordResult>{
    /**
     * Checks if there are more chunks available from the server.
     * 
     * <p>For chunked streams (like {@link com.aerospike.ChunkedRecordStream}), this returns
     * true if more data chunks are available from the server. For simple streams
     * (like {@link com.aerospike.AsyncRecordStream}), this returns true on first call
     * and false afterward for API consistency.</p>
     * 
     * <p><b>Note:</b> This is distinct from client-side pagination (pages) provided by
     * {@link com.aerospike.NavigatableRecordStream}. Chunks are server-side streaming
     * units, while pages are client-side navigation units.</p>
     * 
     * @return true if more chunks are available, false otherwise
     */
    boolean hasMoreChunks();
    boolean hasNext();
    RecordResult next();
    void close();
}
