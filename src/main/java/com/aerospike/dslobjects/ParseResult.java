package com.aerospike.dslobjects;

import com.aerospike.client.exp.Exp;
import com.aerospike.client.query.Filter;

/**
 * Result of parsing a DSL expression.
 * Contains either an Aerospike expression (Exp) or a Filter for secondary index usage.
 */
public class ParseResult {
    private final Filter filter;
    private final Exp exp;
    
    public ParseResult(Filter filter, Exp exp) {
        this.filter = filter;
        this.exp = exp;
    }
    
    public Exp getExp() {
        return exp;
    }
    
    public Filter getFilter() {
        return filter;
    }
    
    public boolean hasFilter() {
        return filter != null;
    }
}

