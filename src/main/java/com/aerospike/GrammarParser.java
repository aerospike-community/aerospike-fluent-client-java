package com.aerospike;

import com.aerospike.client.exp.Exp;

public class GrammarParser {
    
    private enum Type {
        UNKNOWN,
        FLOAT,
        INTEGER,
        STRING
    }
    
    private class Expr {
        private Token token;
        private Expr[] children;
        private Type type;
        
        public Expr(Token token, Type type, Expr ... children) {
            this.token = token;
            this.type = type;
            this.children = children;
        }
        
        public Expr(Token token, Type type, Expr child, Expr[] children) {
            this.token = token;
            this.type = type;
            this.children = new Expr[children.length + 1];
            this.children[0] = child;
            for (int i = 0; i < children.length; i++) {
                this.children[i+1] = children[i];
            }
        }
        
        public Expr[] getChildren() {
            return children;
        }
        public Type getType() {
            return type;
        }
        public Token getToken() {
            return token;
        }
    }
    
    private Expr parseExpression(Tokenizer tokenizer) {
        Expr result = parseExpr(tokenizer);
        return result;
    }
    private Expr parseExpr(Tokenizer tokenizer) {
        Expr expr1 = parseTerm(tokenizer);
        return expr1;
    }
    
    private Expr parseTerm(Tokenizer tokenizer) {
        Expr expr1 = parseFactor(tokenizer);
        Token token = tokenizer.peek();
        if (token.getToken() == Tokenizer.TIMES) {
            token = tokenizer.next();
            Expr expr2 = parseTerm(tokenizer);
            if (expr2.getToken().getToken() == Tokenizer.TIMES) {
                // Chain these together
                return new Expr(token, expr1.getType(), expr1, expr2.children);
            }
            else {
                return new Expr(token, expr1.getType(), expr1, expr2);
            }
        }
        else if (token.getToken() == Tokenizer.DIV) {
            token = tokenizer.next();
            Expr expr2 = parseTerm(tokenizer);
            if (expr2.getToken().getToken() == Tokenizer.DIV) {
                // Chain these together
                return new Expr(token, expr1.getType(), expr1, expr2.children);
            }
            else {
                return new Expr(token, expr1.getType(), expr1, expr2);
            }
        }
        return expr1;
    }
    
    private Expr parseFactor(Tokenizer tokenizer) {
        Token token = tokenizer.next();
        switch (token.getToken()) {
        case Tokenizer.LPAREN: 
            Expr child = parseExpression(tokenizer);
            return new Expr(token, child.getType(), child);
            
        case Tokenizer.IDENTIFIER:
            return new Expr(token, Type.UNKNOWN);
            
        case Tokenizer.FLOAT:
            return new Expr(token, Type.FLOAT);
            
        case Tokenizer.INTEGER:
            return new Expr(token, Type.INTEGER);
            
        case Tokenizer.STRING:
            return new Expr(token, Type.STRING);
        }
        throw new GrammarParseException("Unexpected token %s in expression", token);
    }
    
    private Exp[] childrenToExp(Expr expr) {
        Exp[] childrenExp = new Exp[expr.children.length];
        for (int i = 0; i < expr.children.length; i++) {
            childrenExp[i] = toExp(expr.children[i]);
        }
        return childrenExp;
    }
    public Exp toExp(Expr expr) {
        Token token = expr.getToken();
        switch(token.getToken()) {
        case Tokenizer.FLOAT:   return Exp.val(token.getDoubleVal());
        case Tokenizer.INTEGER:   return Exp.val(token.getIntVal());
        case Tokenizer.STRING:  return Exp.val(token.getValue());
        case Tokenizer.IDENTIFIER: return Exp.intBin(token.getValue()); // TODO: Fix up the type here
        
        case Tokenizer.TIMES: return Exp.mul(childrenToExp(expr));
        case Tokenizer.DIV: return Exp.div(childrenToExp(expr));
        default: 
            throw new GrammarParseException("Unexpected expression: ", token);
        }
    }

    private String childrenToString(String fnName, Expr expr) {
        StringBuffer sb = new StringBuffer();
        sb.append(fnName).append('(');
        for (int i = 0; i < expr.children.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(toExpString(expr.children[i]));
        }
        return sb.append(')').toString();
    }
    public String toExpString(Expr expr) {
        Token token = expr.getToken();
        switch(token.getToken()) {
        case Tokenizer.FLOAT:   return Double.toString(token.getDoubleVal());
        case Tokenizer.INTEGER:   return Long.toString(token.getIntVal());
        case Tokenizer.STRING:  return  "\""+token.getValue()+"\"";
        case Tokenizer.IDENTIFIER: return "Bin:" + token.getValue();
        
        case Tokenizer.TIMES: return childrenToString("mul", expr);
        case Tokenizer.DIV: return childrenToString("div", expr);
        default: 
            throw new GrammarParseException("Unexpected expression: ", token);
        }
    }
// Egs:
    // A + B == 12 => Exp.eq(Exp.add(Exp.intBin("A"), Exp.intBin("B")), Exp.val(12)
    //
    public void parse(String string) {
        System.out.println("Input string: " + string);
        Tokenizer tokenizer = new Tokenizer(string);
        for (Token token = tokenizer.next(); token != Tokenizer.EOF_TOKEN; token = tokenizer.next()) {
            System.out.println(token);
        }
        tokenizer.reset();
        Expr expr = parseExpression(tokenizer);
        System.out.println(toExpString(expr));
    }
    
    
    
    public static void main(String[] args) {
        GrammarParser parser = new GrammarParser();
        parser.parse("17 * a * B * 12 / 7");
        System.out.println();
//        parser.parse("17 > 12 OR 3.1415 != 442.1 ALPHA");
    }
}
