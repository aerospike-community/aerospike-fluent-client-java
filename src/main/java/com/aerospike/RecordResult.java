package com.aerospike;

import com.aerospike.client.BatchRecord;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.ResultCode;
import com.aerospike.client.query.KeyRecord;
import com.aerospike.exception.AeroException;

public record RecordResult(Key key, Record recordOrNull, int resultCode, AeroException exception, boolean inDoubt, String message) {

    public RecordResult(Key key, Record rec) {
        this(key, rec, ResultCode.OK, null, false, null);
    }
    
    public RecordResult(Key key, int resultCode, boolean inDoubt, String message) {
        this(key, null, resultCode, null, inDoubt, message);
    }
    
    public RecordResult(Key key, AeroException ae) {
        this(key, null, ae.getResultCode(), ae, ae.isInDoubt(), ae.getMessage());
    }
    
    public RecordResult(KeyRecord keyRecord) {
        this(keyRecord.key, keyRecord.record, ResultCode.OK, null, false, null);
    }
    
    public RecordResult(BatchRecord batchRecord) {
        this(batchRecord.key, batchRecord.record, batchRecord.resultCode, null, batchRecord.inDoubt, ResultCode.getResultString(batchRecord.resultCode));
    }
    
    public RecordResult(BatchRecord batchRecord, AeroException ae) {
        this(batchRecord.key, batchRecord.record, batchRecord.resultCode, ae, batchRecord.inDoubt, ResultCode.getResultString(batchRecord.resultCode));
    }
    
    public boolean isOk() {
        return this.resultCode == ResultCode.OK;
    }
    
    /**
     * If this result contains an error, then throw the appropriate exception, otherwise return this object
     */
    public RecordResult orThrow() {
        if (!isOk()) {
            if (exception != null) {
                throw exception;
            }
            else {
                throw AeroException.resultCodeToException(resultCode, message(), inDoubt);
            }
        }
        return this;
    }
    
    public Record recordOrThrow() {
        orThrow();
        return recordOrNull;
    }
}
