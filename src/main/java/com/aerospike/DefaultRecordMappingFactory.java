package com.aerospike;

import java.util.Map;

/**
 * Default implementation of {@link RecordMappingFactory} that uses a map to store
 * record mappers for different Java classes.
 * 
 * <p>This factory provides a simple way to register record mappers for different
 * object types. It maintains a mapping from Java class to {@link RecordMapper}
 * instance, allowing automatic object serialization and deserialization when
 * working with typed datasets.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * CustomerMapper customerMapper = new CustomerMapper();
 * AddressMapper addressMapper = new AddressMapper();
 * 
 * DefaultRecordMappingFactory factory = new DefaultRecordMappingFactory(Map.of(
 *     Customer.class, customerMapper,
 *     Address.class, addressMapper
 * ));
 * 
 * cluster.setRecordMappingFactory(factory);
 * }</pre>
 * 
 * @see RecordMappingFactory
 * @see RecordMapper
 * @see Cluster#setRecordMappingFactory(RecordMappingFactory)
 */
public class DefaultRecordMappingFactory implements RecordMappingFactory {
    
    private final Map<Class<? extends Object>, RecordMapper<? extends Object>> map;
    
    /**
     * Creates a new DefaultRecordMappingFactory with the specified mapper mappings.
     * 
     * <p>The map should contain entries where the key is a Java class and the value
     * is the corresponding RecordMapper instance that can handle objects of that class.</p>
     * 
     * @param map a map from Java class to RecordMapper instance
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public DefaultRecordMappingFactory(Map<Class<? extends Object>, RecordMapper<? extends Object>> map) {
        this.map = (Map)map;
    }
    
    public static <T> DefaultRecordMappingFactory of(Class<T> clazz, RecordMapper<T> mapper) {
        return new DefaultRecordMappingFactory(Map.of(clazz, mapper));
    }

    public static <T1, T2> DefaultRecordMappingFactory of(
            Class<T1> clazz1, RecordMapper<T1> mapper1,
            Class<T2> clazz2, RecordMapper<T2> mapper2) {
        return new DefaultRecordMappingFactory(Map.of(
                clazz1, mapper1,
                clazz2, mapper2
                ));
    }

    public static <T1, T2, T3> DefaultRecordMappingFactory of(
            Class<T1> clazz1, RecordMapper<T1> mapper1,
            Class<T2> clazz2, RecordMapper<T2> mapper2,
            Class<T3> clazz3, RecordMapper<T3> mapper3) {
        return new DefaultRecordMappingFactory(Map.of(
                clazz1, mapper1,
                clazz2, mapper2,
                clazz3, mapper3
                ));
    }

    public static <T1, T2, T3, T4> DefaultRecordMappingFactory of(
            Class<T1> clazz1, RecordMapper<T1> mapper1,
            Class<T2> clazz2, RecordMapper<T2> mapper2,
            Class<T3> clazz3, RecordMapper<T3> mapper3,
            Class<T4> clazz4, RecordMapper<T4> mapper4) {
        return new DefaultRecordMappingFactory(Map.of(
                clazz1, mapper1,
                clazz2, mapper2,
                clazz3, mapper3,
                clazz4, mapper4
                ));
    }

    /**
     * Gets the record mapper for the specified class.
     * 
     * <p>This method looks up the appropriate RecordMapper for the given class
     * in the internal map. If no mapper is found for the class, null is returned.</p>
     * 
     * @param <T> the type of object the mapper handles
     * @param clazz the class to get a mapper for
     * @return the RecordMapper for the class, or null if not found
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> RecordMapper<T> getMapper(Class<T> clazz) {
        return (RecordMapper<T>) this.map.get(clazz);
    }
}
