package com.aerospike.dsl.stub;

import com.aerospike.client.exp.Exp;
import com.aerospike.dsl.Index;
import com.aerospike.dsl.ParseResult;

/**
 * TEMPORARY STUB - Replace when library is fixed
 */
public class ParsedExpression {
    private final Exp exp;
    private final Index index;
    
    public ParsedExpression(Exp exp, Index index) {
        this.exp = exp;
        this.index = index;
    }
    
    public Exp getExp() {
        return exp;
    }
    
    public Index getIndex() {
        return index;
    }
    
    public boolean hasIndex() {
        return index != null;
    }
    
    public ParseResult getResult() {
        // Convert ParsedExpression to ParseResult
        // ParseResult constructor is (Filter, Exp)
        return new ParseResult(index != null ? index.getFilter() : null, exp);
    }
}

