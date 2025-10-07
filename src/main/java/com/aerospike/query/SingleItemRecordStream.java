package com.aerospike.query;

import com.aerospike.RecordResult;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.query.KeyRecord;

public class SingleItemRecordStream implements RecordStreamImpl {
    private final RecordResult record;
    private boolean read = false;
    private boolean isFirstPage = true;
    
    public SingleItemRecordStream(Key key, Record record) {
        this.record = new RecordResult(key, record);
        if (record == null) {
            // no data, mark as read
            this.read = true;
        }
    }

    @Override
    public boolean hasMorePages() {
        if (isFirstPage) {
            isFirstPage = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean hasNext() {
        if (!read) {
            read = true;
            return true;
        }
        return false;
    }

    @Override
    public RecordResult next() {
        return record;
    }

    @Override
    public void close() {
    }
}
