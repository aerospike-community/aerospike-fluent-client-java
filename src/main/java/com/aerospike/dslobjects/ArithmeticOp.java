package com.aerospike.dslobjects;

public enum ArithmeticOp {
    PLUS("+"),
    MINUS("-"),
    TIMES("*"),
    DIV("/");
    
    private final String value;
    private ArithmeticOp(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}