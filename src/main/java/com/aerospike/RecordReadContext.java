package com.aerospike;

/**
 * Context passed when deserializing a record into a domain object, for mappers that need
 * the {@link Session} (e.g. to load related entities) or the declared entity type.
 *
 * @param <T> the entity type being mapped
 */
public final class RecordReadContext<T> {

    private final Session session;
    private final Class<T> entityType;

    public RecordReadContext(Session session, Class<T> entityType) {
        this.session = session;
        this.entityType = entityType;
    }

    public Session getSession() {
        return session;
    }

    public Class<T> getEntityType() {
        return entityType;
    }

    public RecordMappingFactory getRecordMappingFactory() {
        return session.getRecordMappingFactory();
    }
}
