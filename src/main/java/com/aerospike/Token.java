package com.aerospike;

class Token {
    private final String value;
    private final int token;
    private final long intVal;
    private final double doubleVal;
    
    public Token(String value, int token) {
        this.value = value;
        this.token = token;
        this.intVal = 0;
        this.doubleVal = 0.0;
    }
    
    public Token(long intVal, int token) {
        this.value = null;
        this.token = token;
        this.intVal = intVal;
        this.doubleVal = 0.0;
    }
    
    public Token(double doubleVal, int token) {
        this.value = null;
        this.token = token;
        this.intVal = 0;
        this.doubleVal = doubleVal;
    }
    
    public String getValue() {
        return value;
    }
    public int getToken() {
        return token;
    }
    public double getDoubleVal() {
        return doubleVal;
    }
    public long getIntVal() {
        return intVal;
    }
    
    @Override
    public String toString() {
        return String.format("{token=%d,intVal=%d,floatVal=%f,value=\"%s\"}", token, intVal, doubleVal, value);
    }
}