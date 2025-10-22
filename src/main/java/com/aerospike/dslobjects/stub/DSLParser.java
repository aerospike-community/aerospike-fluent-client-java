package com.aerospike.dslobjects.stub;

/**
 * TEMPORARY STUB - Replace with com.aerospike.dsl.api.DSLParser when library is fixed
 * 
 * Stub interface for DSL parsing functionality
 */
public interface DSLParser {
    ParsedExpression parse(String dslString, IndexContext indexContext);
    ParsedExpression parseExpression(ExpressionContext context, IndexContext indexContext);
    ParsedExpression parseExpression(ExpressionContext context);
}

