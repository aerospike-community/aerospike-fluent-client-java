package com.aerospike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;

@ExtendWith(MockitoExtension.class)
class TypedRecordStreamTest {

    @Mock
    private Session session;

    static final class Widget {
        final String name;

        Widget(String name) {
            this.name = name;
        }
    }

    @Test
    void sentinelStream_cannotMapToObjects() {
        TypedRecordStream<Widget> empty = new TypedRecordStream<>();
        assertThrows(IllegalStateException.class, empty::toObjectList);
    }

    @Test
    void toObjectList_noArg_invokesFourArgFromMapWithContext() {
        Key key = new Key("ns", "set", "k1");
        Record record = new Record(Map.of("name", "w1"), 1, 0);
        RecordStream underlying = new RecordStream(new RecordResult(key, record, 0));

        AtomicReference<RecordReadContext<Widget>> ctxRef = new AtomicReference<>();
        RecordMapper<Widget> mapper = new RecordMapper<>() {
            @Override
            public Widget fromMap(Map<String, Object> map, Key recordKey, int generation) {
                throw new AssertionError("three-arg fromMap should not be used when four-arg is overridden");
            }

            @Override
            public Widget fromMap(Map<String, Object> map, Key recordKey, int generation, RecordReadContext<Widget> ctx) {
                ctxRef.set(ctx);
                return new Widget((String) map.get("name"));
            }

            @Override
            public Map<String, Value> toMap(Widget element) {
                return Map.of();
            }

            @Override
            public Object id(Widget element) {
                return "id";
            }
        };

        when(session.getRecordMappingFactory()).thenReturn(new DefaultRecordMappingFactory(Map.of(Widget.class, mapper)));

        TypedRecordStream<Widget> stream = new TypedRecordStream<>(session, Widget.class, underlying);
        List<Widget> out = stream.toObjectList();

        assertEquals(1, out.size());
        assertEquals("w1", out.get(0).name);
        assertEquals(session, ctxRef.get().getSession());
        assertEquals(Widget.class, ctxRef.get().getEntityType());
    }

    @Test
    void failures_onAllOk_returnsEmptyStream_withSession_forDownstreamMapping() {
        Key key = new Key("ns", "set", "k1");
        Record record = new Record(Map.of("n", 1), 0, 0);
        RecordStream underlying = new RecordStream(new RecordResult(key, record, 0));

        RecordMapper<Widget> mapper = new RecordMapper<>() {
            @Override
            public Widget fromMap(Map<String, Object> map, Key recordKey, int generation) {
                return new Widget("x");
            }

            @Override
            public Map<String, Value> toMap(Widget element) {
                return Map.of();
            }

            @Override
            public Object id(Widget element) {
                return "id";
            }
        };
        when(session.getRecordMappingFactory()).thenReturn(new DefaultRecordMappingFactory(Map.of(Widget.class, mapper)));

        TypedRecordStream<Widget> orig = new TypedRecordStream<>(session, Widget.class, underlying);
        TypedRecordStream<Widget> failed = orig.failures();

        assertTrue(failed.toObjectList().isEmpty());
        assertEquals(session, failed.getSession());
        assertEquals(Widget.class, failed.getEntityType());
    }

    @Test
    void asNavigatableStream_toObjectList_usesFactory() {
        Key key = new Key("ns", "set", "k1");
        Record record = new Record(Map.of("name", "nav"), 0, 0);
        RecordStream underlying = new RecordStream(new RecordResult(key, record, 0));

        RecordMapper<Widget> mapper = new RecordMapper<>() {
            @Override
            public Widget fromMap(Map<String, Object> map, Key recordKey, int generation) {
                return new Widget((String) map.get("name"));
            }

            @Override
            public Map<String, Value> toMap(Widget element) {
                return Map.of();
            }

            @Override
            public Object id(Widget element) {
                return "id";
            }
        };
        when(session.getRecordMappingFactory()).thenReturn(new DefaultRecordMappingFactory(Map.of(Widget.class, mapper)));

        TypedRecordStream<Widget> stream = new TypedRecordStream<>(session, Widget.class, underlying);
        try (TypedNavigatableRecordStream<Widget> nav = stream.asNavigatableStream()) {
            List<Widget> list = nav.toObjectList();
            assertEquals(1, list.size());
            assertEquals("nav", list.get(0).name);
        }
    }
}
