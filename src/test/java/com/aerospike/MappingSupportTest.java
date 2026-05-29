package com.aerospike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.aerospike.client.Key;
import com.aerospike.client.Value;

class MappingSupportTest {

    @Test
    void requireMapperThrowsWhenMissing() {
        RecordMappingFactory empty = new RecordMappingFactory() {
            @Override
            public <T> RecordMapper<T> getMapper(Class<T> clazz) {
                return null;
            }
        };
        assertThrows(IllegalStateException.class, () -> MappingSupport.requireMapper(empty, String.class));
    }

    @Test
    void nativeKeysFromTypedPreservesOrder() {
        TypedDataSet<CustomerPojo> ds = TypedDataSet.of("ns", "set", CustomerPojo.class);
        List<Key> keys = Session.nativeKeysFromTyped(ds.ids(1L, 2L, 3L));
        assertEquals(3, keys.size());
        assertEquals("ns", keys.get(0).namespace);
        assertEquals("set", keys.get(0).setName);
    }

    @Test
    void recordMapperDefaultFourArgDelegatesToThreeArg() {
        AtomicInteger count = new AtomicInteger();
        RecordMapper<String> mapper = new RecordMapper<String>() {
            @Override
            public String fromMap(Map<String, Object> map, Key recordKey, int generation) {
                count.incrementAndGet();
                return "legacy";
            }

            @Override
            public Map<String, Value> toMap(String element) {
                return Map.of();
            }

            @Override
            public Object id(String element) {
                return "id";
            }
        };
        RecordReadContext<String> ctx = new RecordReadContext<>(null, String.class);
        assertEquals("legacy", mapper.fromMap(Map.of(), new Key("ns", "set", "pk"), 0, ctx));
        assertEquals(1, count.get());
    }

    /** Minimal type for TypedDataSet key smoke test */
    private static final class CustomerPojo {
    }
}
