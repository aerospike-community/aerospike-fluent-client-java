package com.aerospike;

import java.util.ListIterator;

import com.aerospike.client.Record;

public class RecordList implements ListIterator<Record> {
    private final Record[] records;
    private final Record record;
    private int index = 0;
    
    public RecordList(Record record) {
        this.record = record;
        this.records = null;
    }
    public RecordList(Record[] records) {
        this.records = records;
        this.record = null;
    }
    @Override
    public boolean hasNext() {
        return (record == null) ? (index < this.records.length) : index == 0;
    }

    @Override
    public Record next() {
        Record result = hasNext() ? (record == null ? records[index] : record) : null;
        if (result != null) {
            index++;
        }
        return result;
    }

    @Override
    public boolean hasPrevious() {
        return index > 0;
    }

    @Override
    public Record previous() {
        if (index > 0) {
            index--;
            return (record == null) ? records[index] : record;
        }
        return null;
    }

    @Override
    public int nextIndex() {
        return index;
    }

    @Override
    public int previousIndex() {
        return Math.max(0, index-1);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove is not suported");
    }

    @Override
    public void set(Record e) {
        throw new UnsupportedOperationException("Set is not suported");
    }

    @Override
    public void add(Record e) {
        throw new UnsupportedOperationException("Add is not suported");
    }

    public Record get(int index) {
        if (record != null) {
            if (index == 0) {
                return record;
            }
        }
        else {
            if (index >= 0 && index < records.length) {
                return records[index];
            }
        }
        throw new IndexOutOfBoundsException(index);
    }
    public Record first() {
        return get(0);
    }
    
    public Record last() {
        if (record != null) {
            return record;
        }
        else if (records.length > 0) {
            return records[records.length - 1];
        }
        throw new IndexOutOfBoundsException(0);
    }
    
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append('[');
        if (record != null) {
            buffer.append('{').append(record).append('}');
        }
        else {
            boolean first = true;
            for (Record r : records) {
                if (!first) {
                    buffer.append(',');
                }
                else {
                    first = false;
                }
                buffer.append('{').append(r).append('}');
            }
        }
        buffer.append(']');
        return buffer.toString();
    }
}
