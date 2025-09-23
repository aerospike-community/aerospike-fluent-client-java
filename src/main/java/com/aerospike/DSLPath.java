package com.aerospike;

import java.util.ArrayList;
import java.util.List;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.CTX;

public class DSLPath {
    private String expression;
    private List<ExpressionPart> expressionParts;
    public DSLPath(String expression) {
        this.expression = expression;
        this.expressionParts = parsePath(expression);
    }
    
    public static DSLPath from(String string) {
        return new DSLPath(string);
    }
    
    public static DSLPath of(String string) {
        if (string.startsWith("$.")) {
            return new DSLPath(string);
        }
        else {
            return new DSLPath("$." + string);
        }
    }
    public DSLPath child(String string) {
        this.expression += "." + string;
        this.expressionParts.add(new MapKeyPartString(string));
        return this;
    }
    public DSLPath child(long mapKey) {
        this.expression += "." + mapKey;
        this.expressionParts.add(new MapKeyPartLong(mapKey));
        return this;
    }
    public DSLPath index(int index) {
        this.expression += "[" + index + "]";
        this.expressionParts.add(new IndexPart(index));
        return this;
    }
    private enum Type {
        UNKNOWN,
        FLOAT,
        INTEGER,
        STRING
    }
    
    public static interface ExpressionPart {
        boolean isIndex();
        CTX asCTX();
        // TODO: This implementation is clunky.
        int getIndex();
        String getName();
    }
    public static class IndexPart implements ExpressionPart {
        private final int index;
        public IndexPart(int index) {
            this.index = index;
        }
        
        public int getIndex() {
            return index;
        }
        @Override
        public CTX asCTX() {
            return CTX.listIndex((int)getIndex());
        }
        @Override
        public boolean isIndex() {
            return true;
        }
        @Override
        public String getName() {
            throw new IllegalStateException("IndexParts do not have names");
        }
        @Override
        public String toString() {
            return "Index("+getIndex()+")";
        }
    }
    public static abstract class MapKeyPart implements ExpressionPart {
        @Override
        public boolean isIndex() {
            return false;
        }
        @Override
        public String getName() {
            return this.getKey().toString();
        }
        @Override
        public int getIndex() {
            throw new IllegalStateException("MapKeyParts do not have indexes");
        }        
        @Override
        public CTX asCTX() {
            return CTX.mapKey(Value.get(getKey()));
        }
        public abstract Object getKey();
    }
    
    public static class MapKeyPartString extends MapKeyPart {
        private final String key;
        
        public MapKeyPartString(String key) {
            this.key = key;
        }
        @Override
        public Object getKey() {
            return key;
        }
        @Override
        public String toString() {
            return "MapKey(\""+getKey()+"\")";
        }
    }
    
    public static class MapKeyPartLong extends MapKeyPart {
        private final long key;
        
        public MapKeyPartLong(long key) {
            this.key = key;
        }
        public Object getKey() {
            return key;
        }
        @Override
        public String toString() {
            return "MapKey("+getKey()+")";
        }
    }
    
    private Token checkAndConsumeSymbol(Tokenizer tokenizer, int expected) {
        Token token = tokenizer.next();
        if (token.getToken() != expected) {
            throw new AerospikeException(String.format("Invalid DSL: Expected '%s' but received '%s'", tokenizer.tokenIdToString(expected), token.getValue()));
        }
        return token;
    }
    private List<ExpressionPart> parsePath(String path) {
        List<ExpressionPart> parts = new ArrayList<>();
        Tokenizer tokenizer = new Tokenizer(path);
        checkAndConsumeSymbol(tokenizer, Tokenizer.DOLLAR);
        checkAndConsumeSymbol(tokenizer, Tokenizer.DOT);
        Token token;
        do {
            token = tokenizer.next();
            switch (token.getToken()) {
            case Tokenizer.IDENTIFIER:
            case Tokenizer.STRING:
                parts.add(new MapKeyPartString(token.getValue()));
                break;
            case Tokenizer.LBRACKET:
                token = checkAndConsumeSymbol(tokenizer, Tokenizer.INTEGER);
                checkAndConsumeSymbol(tokenizer, Tokenizer.RBRACKET);
                parts.add(new IndexPart((int)token.getIntVal()));
                break;
            default:
                throw new AerospikeException(String.format("Invalid DSL: Expected a string or number but received '%s'", token.getValue()));
            }
            token = tokenizer.next();
            if (token.getToken() != Tokenizer.EOF && token.getToken() != Tokenizer.DOT) {
                throw new AerospikeException(String.format("Invalid DSL: Expected a '.' or end-of-string but received '%s'", token.getValue()));
            }
        } while (token.getToken() != Tokenizer.EOF);
        
        return parts;
    }
    public String getBinName() {
        return expressionParts.get(0).toString();
    }
    public ExpressionPart getOperand() {
        if (expressionParts.size() > 1) {
            return expressionParts.get(expressionParts.size()-1);
        }
        return null;
    }
    public List<ExpressionPart> getContextParts() {
        if (expressionParts.size() > 2) {
            return expressionParts.subList(1, expressionParts.size()-1);
        }
        return null;
    }
    public boolean hasOperand() {
        return expressionParts.size() > 1;
    }
    public CTX[] getContext() {
        List<ExpressionPart> parts = getContextParts();
        if (parts == null) {
            return null;
        }
        else {
            CTX[] results = new CTX[parts.size()];
            for (int i = 0; i < parts.size(); i++) {
                results[i] = parts.get(i).asCTX();
            }
            return results;
        }
    }
    @Override
    public String toString() {
        return String.format("bin:%s, ctx:%s, op:%s", getBinName(), getContextParts(), getOperand());
    }
    
    public static void main(String[] args) {
        DSLPath pather = DSLPath.of("a").child("b").index(2).child("d").child(12);
        System.out.println(pather);
        DSLPath path = DSLPath.from("$.a.b.[2].d");
        System.out.println(path.expressionParts);
        System.out.println(new DSLPath("$.a.b.[2].d"));
        System.out.println(new DSLPath("$.a"));
        System.out.println(new DSLPath("$.a.b.d"));
    }
}
