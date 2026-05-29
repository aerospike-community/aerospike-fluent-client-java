package com.aerospike;

/**
 * Internal helpers for resolving {@link RecordMapper} instances from a {@link RecordMappingFactory}.
 */
public final class MappingSupport {

    private MappingSupport() {}

    /**
     * Returns a non-null mapper for {@code clazz}, or throws {@link IllegalStateException}.
     */
    public static <T> RecordMapper<T> requireMapper(RecordMappingFactory factory, Class<T> clazz) {
        RecordMapper<T> mapper = factory.getMapper(clazz);
        if (mapper == null) {
            throw new IllegalStateException(
                    "No RecordMapper registered for " + clazz.getName()
                            + ". Register it on the cluster RecordMappingFactory.");
        }
        return mapper;
    }
}
