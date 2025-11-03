package com.aerospike.query;

import java.util.List;

import com.aerospike.DataSet;
import com.aerospike.RecordStream;
import com.aerospike.Session;
import com.aerospike.client.Log;
import com.aerospike.client.exp.Exp;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.query.PartitionFilter;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import com.aerospike.dsl.ParseResult;
import com.aerospike.policy.Behavior.Mode;
import com.aerospike.policy.Behavior.OpKind;
import com.aerospike.policy.Behavior.OpShape;

class IndexQueryBuilderImpl extends QueryImpl {
    private final DataSet dataSet;
    public IndexQueryBuilderImpl(QueryBuilder builder, Session session, DataSet dataSet) {
        super(builder, session);
        this.dataSet = dataSet;
    }
    
    @Override
    public boolean allowsSecondaryIndexQuery() {
        return true;
    }
    
    @Override
    public RecordStream execute() {
        // Query default: async unless in transaction
        if (getQueryBuilder().getTxnToUse() != null) {
            return executeSync();
        } else {
            return executeAsync();
        }
    }
    
    @Override
    public RecordStream executeSync() {
        return executeInternal();
    }
    
    @Override
    public RecordStream executeAsync() {
        if (getQueryBuilder().getTxnToUse() != null && com.aerospike.client.Log.warnEnabled()) {
            Log.warn(
                "executeAsync() called within a transaction. " +
                "Async operations may still be in flight when commit() is called, " +
                "which could lead to inconsistent state. " +
                "Consider using executeSync() or execute() for transactional safety."
            );
        }
        // Index queries stream results; async and sync behave similarly
        return executeInternal();
    }
    
    private RecordStream executeInternal() {
        boolean isNamespaceSC = getSession().isNamespaceSC(this.dataSet.getNamespace());
        QueryPolicy queryPolicy = getSession().getBehavior().getSettings(OpKind.READ, OpShape.QUERY, isNamespaceSC ? Mode.CP : Mode.AP).asQueryPolicy();
        if (this.getQueryBuilder().getWithNoBins()) {
            queryPolicy.includeBinData = false;
        }
        
        long pageSize = getQueryBuilder().getPageSize();
        long limit = getQueryBuilder().getLimit();
        List<SortProperties> sortInfo = getQueryBuilder().getSortInfo();
        
        Statement stmt = new Statement();
        stmt.setNamespace(dataSet.getNamespace());
        stmt.setSetName(dataSet.getSet());
        stmt.setBinNames(getQueryBuilder().getBinNames());

        if (getQueryBuilder().getDsl() != null) {
            ParseResult parseResult = getQueryBuilder().getDsl().process(this.dataSet.getNamespace(), getSession());
            queryPolicy.filterExp = parseResult.getExp() == null ? null : Exp.build(parseResult.getExp());
            stmt.setFilter(parseResult.getFilter());
        }

        if (pageSize > 0) {
            stmt.setMaxRecords(pageSize);
        }
        else if (limit > 0 && pageSize == 0) {
            stmt.setMaxRecords(limit);
        }

        // No need to set transactions, they're not supported by queries
        // queryPolicy.txn = this.getQueryBuilder().getTxnToUse();

        PartitionFilter filter = PartitionFilter.range(
                getQueryBuilder().getStartPartition(), 
                getQueryBuilder().getEndPartition() - getQueryBuilder().getStartPartition());
        
//        RecordSet queryResults = getSession().getClient().queryPartitions(queryPolicy, stmt, filter);
        return new RecordStream(getSession(), queryPolicy, stmt, filter, limit, sortInfo);
    }
}