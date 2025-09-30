package com.aerospike.query;

import java.util.Collection;
import java.util.Set;

import com.aerospike.RecordStream;
import com.aerospike.Session;
import com.aerospike.client.Log;
import com.aerospike.client.Value;
import com.aerospike.client.command.ParticleType;
import com.aerospike.client.query.Filter;
import com.aerospike.dsl.DslParseException;
import com.aerospike.dsl.ExpressionContext;
import com.aerospike.dsl.Index;
import com.aerospike.dsl.IndexContext;
import com.aerospike.dsl.ParseResult;
import com.aerospike.dsl.ParsedExpression;
import com.aerospike.dsl.api.DSLParser;
import com.aerospike.dsl.impl.DSLParserImpl;

abstract class QueryImpl {
        private final Session session;
        private final QueryBuilder queryBuilder;
        
        public QueryImpl(QueryBuilder builder, Session session) {
            this.session = session;
            this.queryBuilder = builder;
        }
        public abstract RecordStream execute();
        
        public Session getSession() {
            return session;
        }
        
        protected QueryBuilder getQueryBuilder() {
            return queryBuilder;
        }
        
        public boolean hasPartitionFilter() {
            return queryBuilder.getStartPartition() > 0 || queryBuilder.getEndPartition() < 4096;
        }
        
        private String valTypeToString(int type) {
            switch(type) {
            case ParticleType.BLOB: return "BLOB";
            case ParticleType.GEOJSON: return "GeoJSON";
            case ParticleType.INTEGER: return "numeric";
            case ParticleType.STRING: return "string";
            default: return "Unknown(" + type + ")";
            }
        }
        private String shorten(Value value) {
            String val = value.toString();
            if (val.length() <= 8 ) {
                return val;
            }
            return val.substring(0, 5) + "...";
        }
        private String filterCriteriaToString(Filter filter) {
            if (filter.getEnd() != null) {
                return "(" + shorten(filter.getBegin()) + "-" + shorten(filter.getEnd());
            }
            else {
                return "(" + shorten(filter.getBegin()) + ")";
            }
        }
        private String formStringOfFilter(Filter filter, IndexContext indexContext) {
            StringBuffer sb = new StringBuffer();
            sb.append(filter.getName())
                    .append(" [")
                    .append(valTypeToString(filter.getValType()))
                    .append(" ] ")
                    .append(filterCriteriaToString(filter));
            if (indexContext != null) {
                Collection<Index> indexes = indexContext.getIndexes();
                sb.append("{");
                for (Index index : indexes) {
                    sb.append(index.getBinValuesRatio()).append(",");
                }
                sb.append("}");
            }
                ;
            return sb.toString();
        }
        public ParseResult getParseResultFromWhereClause(String dsl, String namespace, boolean allowIndexes) {
            DSLParser parser = new DSLParserImpl();
            
            ParsedExpression parseResult;
            IndexContext indexContext = null;
            ExpressionContext context = ExpressionContext.of(dsl);
            if (allowIndexes) {
                Set<Index> indexes = session.getCluster().getIndexes();
                indexContext = IndexContext.of(namespace, indexes);
                parseResult = parser.parseExpression(context, indexContext);
            }
            else {
                parseResult = parser.parseExpression(context);
            }
            ParseResult result = parseResult.getResult();
            if (result.getExp() == null && result.getFilter() == null) {
                throw new DslParseException("Unknown error parsing DSL: '" + dsl + "'");
            }
            
            if (Log.debugEnabled()) {
                if (allowIndexes && result.getFilter() != null) {
                    Filter filter = result.getFilter();
                    
                    Log.debug(String.format("Dsl('%s', '%s') => (Exp: %s, Filter: %s)", 
                            dsl,
                            namespace,
                            result.getExp(),
                            formStringOfFilter(filter, indexContext)));
                }
                else {
                    Log.debug(String.format("Dsl('%s', '%s') => (Exp: %s)", 
                            dsl,
                            namespace,
                            result.getExp()));
                }
            }
            
            return result;
//            AbstractPart tree = parseResult.getExpressionTree();
//            Map<String, List<Index>> indexMap = parseResult.getIndexesMap();
//            ParseResult result = parseResult.getResult();
//            Exp exp = tree.getExp();
        }
    }