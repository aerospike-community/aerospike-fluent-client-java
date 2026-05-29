package com.aerospike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;

@ExtendWith(MockitoExtension.class)
class MixedTypedBatchReadResultTest {

    static final class Row {
        final int v;

        Row(int v) {
            this.v = v;
        }
    }

    @Mock
    private Session session;

    @Test
    void toObjects_usesFactoryAndFourArgPath() {
        Key k = new Key("ns", "set", 1);
        Record rec = new Record(Map.of("v", 42), 0, 0);
        List<List<RecordResult>> segments = List.of(List.of(new RecordResult(k, rec, 0)));

        RecordMapper<Row> mapper = new RecordMapper<>() {
            @Override
            public Row fromMap(Map<String, Object> map, Key recordKey, int generation, RecordReadContext<Row> ctx) {
                assertEquals(session, ctx.getSession());
                return new Row(((Number) map.get("v")).intValue());
            }

            @Override
            public Row fromMap(Map<String, Object> map, Key recordKey, int generation) {
                throw new AssertionError();
            }

            @Override
            public Map<String, Value> toMap(Row element) {
                return Map.of();
            }

            @Override
            public Object id(Row element) {
                return 0;
            }
        };

        when(session.getRecordMappingFactory()).thenReturn(new DefaultRecordMappingFactory(Map.of(Row.class, mapper)));

        MixedTypedBatchReadResult result = new MixedTypedBatchReadResult(session, segments);
        List<Row> rows = result.toObjects(0, Row.class);
        assertEquals(1, rows.size());
        assertEquals(42, rows.get(0).v);
    }
}
