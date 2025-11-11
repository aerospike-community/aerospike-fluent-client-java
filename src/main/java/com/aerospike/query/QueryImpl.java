package com.aerospike.query;

import com.aerospike.RecordStream;
import com.aerospike.Session;

abstract class QueryImpl {
    private final Session session;
    private final QueryBuilder queryBuilder;
    
    public QueryImpl(QueryBuilder builder, Session session) {
        this.session = session;
        this.queryBuilder = builder;
    }
    
    public abstract RecordStream execute();
    public abstract RecordStream executeSync();
    public abstract RecordStream executeAsync();
	public abstract boolean allowsSecondaryIndexQuery();
    
    public Session getSession() {
        return session;
    }
    
    protected QueryBuilder getQueryBuilder() {
        return queryBuilder;
    }
    
    public boolean hasPartitionFilter() {
        return queryBuilder.getStartPartition() > 0 || queryBuilder.getEndPartition() < 4096;
    }
}