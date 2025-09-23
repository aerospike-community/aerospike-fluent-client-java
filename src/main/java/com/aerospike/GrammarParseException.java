package com.aerospike;

public class GrammarParseException extends RuntimeException {
    public GrammarParseException(String message, Object ...params) {
        super(String.format(message, params));
    }
}
