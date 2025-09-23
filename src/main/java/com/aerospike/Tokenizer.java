package com.aerospike;

import java.util.ArrayList;
import java.util.List;

import com.aerospike.client.AerospikeException;

class Tokenizer {
    public final static int GE = 1;
    public final static int GT = 2;
    public final static int LE = 3;
    public final static int LT = 4;
    public final static int EQ = 5;
    public final static int NE = 6;
    public final static int PLUS = 10;
    public final static int MINUS = 11;
    public final static int TIMES = 12;
    public final static int DIV = 13;
    public final static int AND = 20;
    public final static int OR = 21;
    public final static int NOT = 22;
    public final static int LPAREN = 30;
    public final static int RPAREN = 31;
    public final static int LBRACKET = 32;
    public final static int RBRACKET = 33;
    public final static int LBRACE = 34;
    public final static int RBRACE = 35;
    public final static int DOLLAR = 40;
    public final static int DOT = 41;
    
    public final static int INTEGER = -2;
    public final static int STRING = -3;
    public final static int FLOAT = -4;
    public final static int IDENTIFIER = -5;
    public final static int EOF = -1;
    
    public final static Token EOF_TOKEN = new Token("EOF", EOF);
    private static final Token[] TOKENS = {
        new Token(">=", GE),
        new Token(">", GT),
        new Token("<=", LE),
        new Token("<", LT),
        new Token("==", EQ),
        new Token("!=", NE),
        new Token("+", PLUS),
        new Token("-", MINUS),
        new Token("*", TIMES),
        new Token("/", DIV),
        new Token("(", LPAREN),
        new Token(")", RPAREN),
        new Token("[", LBRACKET),
        new Token("]", RBRACKET),
        new Token("{", LBRACE),
        new Token("}", RBRACE),
        new Token("$", DOLLAR),
        new Token(".", DOT),
        new Token("AND", AND),
        new Token("OR", OR),
        new Token("NOT", NOT)
    };
    
    // These tokens must be sorted by length, longest to shortest
    private final String inputString;
    private int location = 0;
    private List<Token> tokenList = new ArrayList<>();
    private int tokenIndex = 0;
    
    public Tokenizer(String input) {
        this.inputString = input;
        for (Token token = this.nextToken(); token != EOF_TOKEN; token = this.nextToken()) {
            tokenList.add(token);
        }
    }
    
    public void reset() {
        this.tokenIndex = 0;
    }
    
    public String tokenIdToString(int id) {
        for (int i = 0; i < TOKENS.length; i++) {
            if (TOKENS[i].getToken() == id) {
                return TOKENS[i].getValue();
            }
        }
        throw new AerospikeException("Unexpected token id " + id);
    }
    private boolean isInRange() {
        return location < inputString.length();
    }
    
    private char current() {
        return inputString.charAt(location);
    }
    
    private char currentAndAdvance() {
        return inputString.charAt(location++);
    }
    
    private void skipWhitespace() {
        while (isInRange() && Character.isWhitespace(current())) {
            location++;
        }
    }
    private String consumeString() {
        StringBuffer sb = new StringBuffer();
        int initialLocation = this.location;
        char startingChar = currentAndAdvance();
        while (true) {
            if (!isInRange()) {
                throw new GrammarParseException("String started at location %d is not properly terminated", initialLocation);
            }
            char current = currentAndAdvance();
            if (current == startingChar) {
                break;
            }
            if (current == '\\') {
                // Escape character, work out value
                if (!isInRange()) {
                    throw new GrammarParseException("String started at location %d is not properly terminated", initialLocation);
                }
                current = currentAndAdvance();
                switch (current) {
                case '\\':   sb.append("\\"); break;
                case '"':    sb.append("\""); break;
                case '\'':   sb.append("'"); break;
                case '`':    sb.append("`"); break;
                case 'n':    sb.append("\n"); break;
                case 'r':    sb.append("\r"); break;
                case 'b':    sb.append("\b"); break;
                case 'f':    sb.append("\f"); break;
                case 't':    sb.append("\t"); break;
                default: throw new GrammarParseException("Invalid escape sequence '\\%c at position %d in string started at location %d",
                        current, location, initialLocation);
                }
            }
            else {
                sb.append(current);
            }
        }
        return sb.toString();
    }
    private Token matchStringToken() {
        switch (current()){
        case '"':
        case '\'':
        case '`':
            return new Token(consumeString(), STRING);
        default:
            return null;
        }
    }
    private Token matchNumberToken() {
        char current = current();
        int startLocation = location;
        if ((current == '+' || current == '-') && ((location+1) < inputString.length())) {
            current = inputString.charAt(location + 1);
            if (!Character.isDigit(current) && current != '.') {
                return null;
            }
            location++;
        }
        while (isInRange() && Character.isDigit(current())) {
            location++;
        }
        if (isInRange() && current() == '.') {
            location ++;
            if (!Character.isDigit(current())) {
                throw new GrammarParseException("Digit expected after decimal point at location %d in \"%s\"", location, inputString);
            }
            while (isInRange() && Character.isDigit(current())) {
                location++;
            }
            return new Token(Double.parseDouble(inputString.substring(startLocation, location)), FLOAT);
        }
        else {
            if (startLocation == location) {
                return null;
            }
            return new Token(Long.parseLong(inputString.substring(startLocation, location)), INTEGER);
        }
    }
    private Token matchStandardTokens() {
        for (Token thisToken : TOKENS) {
            if (inputString.startsWith(thisToken.getValue(), location)) {
                location += thisToken.getValue().length();
                return thisToken;
            }
        }
        return null;
    }
    
    private Token matchIdentifierToken() {
        if (Character.isJavaIdentifierStart(current())) {
            int startLocation = location;
             while (isInRange() && Character.isJavaIdentifierPart(current())) {
                 location++;
             }
            return new Token(inputString.substring(startLocation, location), IDENTIFIER);
        }
        return null;
    }
    
    private Token nextToken() {
        skipWhitespace();
        Token token;
        if (!isInRange()) {
            return EOF_TOKEN;
        }
        if ((token = matchStandardTokens()) != null) return token;
        if ((token = matchStringToken()) != null) return token;
        if ((token = matchNumberToken()) != null) return token;
        if ((token = matchIdentifierToken()) != null) return token;
        throw new GrammarParseException("Unrecognized input at location %d in %s", location, inputString);
    }
    
    public Token next() {
        if (tokenIndex < tokenList.size()) {
            return tokenList.get(tokenIndex++);
        }
        return EOF_TOKEN;
    }
    
    public Token peek() {
        if (tokenIndex < tokenList.size()) {
            return tokenList.get(tokenIndex);
        }
        return EOF_TOKEN;
    }
    public Token prev() {
        if (tokenIndex > 0) {
            return tokenList.get(--tokenIndex);
        }
        return EOF_TOKEN;
    }
    
    public Token peekPrev() {
        if (tokenIndex > 0) {
            return tokenList.get(tokenIndex-1);
        }
        return EOF_TOKEN;
    }
}