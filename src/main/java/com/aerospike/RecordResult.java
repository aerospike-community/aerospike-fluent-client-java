package com.aerospike;

import com.aerospike.client.BatchRecord;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.ResultCode;
import com.aerospike.client.query.KeyRecord;
import com.aerospike.exception.AeroException;

public record RecordResult(Key key, Record recordOrNull, int resultCode, boolean inDoubt, String message) {

    public RecordResult(Key key, Record rec) {
        this(key, rec, ResultCode.OK, false, null);
    }
    
    public RecordResult(Key key, int resultCode, boolean inDoubt, String message) {
        this(key, null, resultCode, inDoubt, message);
    }
    
    public RecordResult(KeyRecord keyRecord) {
        this(keyRecord.key, keyRecord.record, ResultCode.OK, false, null);
    }
    
    public RecordResult(BatchRecord batchRecord) {
        this(batchRecord.key, batchRecord.record, batchRecord.resultCode, batchRecord.inDoubt, ResultCode.getResultString(batchRecord.resultCode));
    }
    
    public boolean isOk() {
        return this.resultCode == ResultCode.OK;
    }
    
    public Record recordOrThrow() {
        if (!isOk()) {
            throw AeroException.resultCodeToException(resultCode, message(), inDoubt);
        }
        return recordOrNull;
    }
}
