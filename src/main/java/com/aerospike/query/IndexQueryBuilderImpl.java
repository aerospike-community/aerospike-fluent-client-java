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
import com.aerospike.policy.Behavior.CommandType;

class IndexQueryBuilderImpl extends QueryImpl {
    private final DataSet dataSet;
    public IndexQueryBuilderImpl(QueryBuilder builder, Session session, DataSet dataSet) {
        super(builder, session);
        this.dataSet = dataSet;
    }
    
    @Override
    public RecordStream execute() {
        QueryPolicy queryPolicy = getSession().getBehavior().getMutablePolicy(CommandType.QUERY);
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

        if (getQueryBuilder().dslString != null) {
            ParseResult parseResult = this.getParseResultFromWhereClause(getQueryBuilder().dslString, this.dataSet.getNamespace(), true);
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
        
        RecordSet queryResults = getSession().getClient().queryPartitions(queryPolicy, stmt, filter);
        return new RecordStream(getSession(), queryPolicy, stmt, filter, queryResults, limit, sortInfo);
    }
}