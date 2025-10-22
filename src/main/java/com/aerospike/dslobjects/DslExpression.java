package com.aerospike.dslobjects;

import com.aerospike.client.exp.Exp;

/**
 * Represents any DSL expression. This is the base interface for all expressions.
 */
public interface DslExpression {
    String toAerospikeExpr();
    Exp toAerospikeExp();
}
