package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {     //TODO
        List<Ast.Method> methodList = new ArrayList<>();
        List<Ast.Field> fieldList = new ArrayList<>();

        while (peek("LET")) {
            Ast.Field field = parseField();
            fieldList.add(field);
        }

        while (peek("DEF")) {
            Ast.Method method = parseMethod();
            methodList.add(method);
        }

        if (tokens.has(1)) {
            throw new ParseException("Error, more tokens in Source after methods", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
        }

        return new Ast.Source(fieldList, methodList);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {   //TODO
        if (match("LET")) {
            if (match(Token.Type.IDENTIFIER)) {
                Ast.Expr.Access id = new Ast.Expr.Access(Optional.empty(), tokens.get(-1).getLiteral());

                if (match(":")) {
                    if (match(Token.Type.IDENTIFIER)) {
                        Ast.Expr.Access type = new Ast.Expr.Access(Optional.empty(), tokens.get(-1).getLiteral());
                        Ast.Field field = new Ast.Field(id.getName(), type.getName(), Optional.empty());

                        if (match("=")) {
                            Ast.Stmt.Expression expr = new Ast.Stmt.Expression(parseExpression());
                            field = new Ast.Field(id.getName(), type.getName(), Optional.of(expr.getExpression()));
                        }

                        if (match(";")) {
                            return field;
                        } else {
                            throw new ParseException("Expected semi-colon ';'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                            //TODO fix thrown index value
                        }
                    } else {
                        throw new ParseException("Expected Identifier", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                        //TODO fix thrown index
                    }
                } else {
                    throw new ParseException("Expected colon ':'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                    //TODO fix thrown index value
                }

            } else {
                throw new ParseException("Expected Identifier", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                //TODO fix thrown index
            }
        } else {
            throw new ParseException("Expected 'LET'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
            //TODO fix thrown index
        }
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {     //TODO
        if (match("DEF")) {

            if (match(Token.Type.IDENTIFIER)) {
                Ast.Expr.Access id = new Ast.Expr.Access(Optional.empty(), tokens.get(-1).getLiteral());
                List<String> paramList = new ArrayList<>();
                List<String> paramTypes = new ArrayList<>();
                Optional<String> returnType = Optional.empty();

                if (match("(")) {

                    if (!peek(")")) {
                        if (match(Token.Type.IDENTIFIER)) {
                            paramList.add(tokens.get(-1).getLiteral());
                            if (match(":")) {
                                if (match(Token.Type.IDENTIFIER)) {
                                     paramTypes.add(tokens.get(-1).getLiteral());
                                } else {
                                    throw new ParseException("Expected Identifier", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                                    //TODO fix thrown index
                                }
                            } else {
                                throw new ParseException("Expected colon ':'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                                //TODO fix thrown index
                            }
                        } else {
                            throw new ParseException("Expected Identifier", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                            // TODO fix index thrown
                        }

                        while (match(",")) {
                            if (match(Token.Type.IDENTIFIER)) {
                                paramList.add(tokens.get(-1).getLiteral());
                                if (match(":")) {
                                    if (match(Token.Type.IDENTIFIER)) {
                                        paramTypes.add(tokens.get(-1).getLiteral());
                                    } else {
                                        throw new ParseException("Expected Identifier", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                                        //TODO fix thrown index
                                    }
                                } else {
                                    throw new ParseException("Expected colon ':'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                                    //TODO fix thrown index
                                }
                            } else {
                                throw new ParseException("Expected Identifier", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                                // TODO fix index thrown
                            }
                        }
                    }

                    if (!match(")")) {
                        throw new ParseException("Expected ')'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                        // TODO fix index thrown
                    }
                } else {
                    throw new ParseException("Expected '('", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                    // TODO fix index thrown
                }

                if (match(":")) {
                    if (match(Token.Type.IDENTIFIER)) {
                        returnType = Optional.of(tokens.get(-1).getLiteral());
                    } else {
                        throw new ParseException("Expected IDENTIFIER", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                        // TODO fix index thrown
                    }
                }

                if (match("DO")) {
                    List<Ast.Stmt> stmtList = new ArrayList<>();
                    while (!peek("END")) {
                        stmtList.add(parseStatement());
                    }

                    if (match("END")) {
                        return new Ast.Method(id.getName(), paramList, paramTypes, returnType, stmtList);
                    } else {
                        throw new ParseException("Expected 'END'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                        // TODO fix index thrown
                    }
                } else {
                    throw new ParseException("Expected 'DO", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                    //TODO fix thrown index
                }
            } else {
                throw new ParseException("Expected Identifier", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                // TODO fix index thrown
            }
        } else {
            throw new ParseException("Expected 'DEF'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
            //TODO fix thrown index
        }
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException {
        //TODO

        if (peek("LET")) {
            return parseDeclarationStatement();
        }

        if (peek("IF")) {
            return parseIfStatement();
        } else if (peek("FOR")) {
            return parseForStatement();
        } else if (peek("WHILE")) {
            return parseWhileStatement();
        } else if (peek("RETURN")) {
            return parseReturnStatement();
        } else {
            Ast.Expr expr1 = parseExpression();
            Ast.Stmt stmt = new Ast.Stmt.Expression(expr1);

            if (match("=")) {
                Ast.Expr expr2 = parseExpression();
                stmt = new Ast.Stmt.Assignment(expr1, expr2);
            }

            if (match(";")) {
                return stmt;
            } else {
                throw new ParseException("Expected closing semi-colon ';'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                //TODO fix index thrown
            }

        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {     //TODO
        if (match("LET")) {
            if (match(Token.Type.IDENTIFIER)) {
                Ast.Expr.Access id = new Ast.Expr.Access(Optional.empty(), tokens.get(-1).getLiteral());
                Optional<String> type = Optional.empty();

                if (match(":")) {
                    if (match(Token.Type.IDENTIFIER)) {
                        type = Optional.of(tokens.get(-1).getLiteral());
                    } else {
                        throw new ParseException("Expected IDENTIFIER", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                        //TODO fix index thrown
                    }
                }

                Ast.Stmt.Declaration declaration = new Ast.Stmt.Declaration(id.getName(), type, Optional.empty());

                if (match("=")) {
                    Ast.Stmt.Expression expr = new Ast.Stmt.Expression(parseExpression());
                    declaration = new Ast.Stmt.Declaration(id.getName(), type, Optional.of(expr.getExpression()));
                }

                if (match(";")) {
                    return declaration;
                } else {
                    throw new ParseException("Expected semi-colon ';'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                    //TODO fix thrown index value
                }
            } else {
                throw new ParseException("Expected Identifier", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                //TODO fix thrown index
            }
        } else {
            throw new ParseException("Expected 'LET'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
            //TODO fix thrown index
        }
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {       //TODO
        if (match("IF")) {
            Ast.Expr expr = parseExpression();
            if (match("DO")) {
                List<Ast.Stmt> thenStmts = new ArrayList<>();
                List<Ast.Stmt> elseStmts = new ArrayList<>();
                while (!peek("END")) {
                    if (peek("ELSE")) {
                        break;
                    }
                    thenStmts.add(parseStatement());
                }

                if (match("ELSE")) {
                    while (!peek("END")) {
                        elseStmts.add(parseStatement());
                    }
                }

                if (match("END")) {
                    return new Ast.Stmt.If(expr, thenStmts, elseStmts);
                } else {
                    throw new ParseException("Expected 'END'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                    // TODO fix index thrown
                }
            } else {
                throw new ParseException("Expected 'DO'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                //TODO fix thrown index
            }
        } else {
            throw new ParseException("Expected 'IF'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
            //TODO fix thrown index
        }
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {     //TODO
        if (match("FOR")) {
            if (match(Token.Type.IDENTIFIER)) {
                Ast.Expr.Access id = new Ast.Expr.Access(Optional.empty(), tokens.get(-1).getLiteral());
                if (match("IN")) {
                    Ast.Expr expr = parseExpression();
                    if (match("DO")) {
                        List<Ast.Stmt> stmtList = new ArrayList<>();
                        while (!peek("END")) {
                            stmtList.add(parseStatement());
                        }

                        if (match("END")) {
                            return new Ast.Stmt.For(id.getName(), expr, stmtList);
                        } else {
                            throw new ParseException("Expected 'END'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                            // TODO fix index thrown
                        }
                    } else {
                        throw new ParseException("Expected 'DO'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                        // TODO fix index thrown
                    }
                } else {
                    throw new ParseException("Expected 'IN'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                    // TODO fix index thrown
                }
            } else {
                throw new ParseException("Expected 'IDENTIFIER'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                // TODO fix index thrown
            }
        } else {
            throw new ParseException("Expected 'FOR'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
            // TODO fix index thrown
        }
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {     //TODO
        if (match("WHILE")) {
            Ast.Expr expr = parseExpression();
            if (match("DO")) {
                List<Ast.Stmt> stmtList = new ArrayList<>();
                while (!peek("END")) {
                    stmtList.add(parseStatement());
                }

                if (match("END")) {
                    return new Ast.Stmt.While(expr, stmtList);
                } else {
                    throw new ParseException("Expected 'END'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                    // TODO fix index thrown
                }
            } else {
                throw new ParseException("Expected 'DO'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                // TODO fix index thrown
            }
        } else {
            throw new ParseException("Expected 'WHILE'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
            // TODO fix index thrown
        }
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {       //TODO
        if (match("RETURN")) {
            Ast.Expr expr = parseExpression();
            if (match(";")) {
                return new Ast.Stmt.Return(expr);
            } else {
                throw new ParseException("Expected ';'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                // TODO fix index thrown
            }
        } else {
            throw new ParseException("Expected 'RETURN'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
            // TODO fix index thrown
        }
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {   //TODO
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() throws ParseException {    //TODO
        Ast.Expr first = parseEqualityExpression();

        while (match("AND") || match("OR")) {
            String op = tokens.get(-1).getLiteral();
            Ast.Expr second = parseEqualityExpression();
            first = new Ast.Expr.Binary(op, first, second);
        }

        return first;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expr parseEqualityExpression() throws ParseException {   //TODO
        Ast.Expr first = parseAdditiveExpression();

        while (match("<") || match("<=") || match(">") || match(">=") || match("==") || match("!=")) {
            String op = tokens.get(-1).getLiteral();
            Ast.Expr second = parseAdditiveExpression();
            first = new Ast.Expr.Binary(op, first, second);
        }

        return first;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {   //TODO
        Ast.Expr first = parseMultiplicativeExpression();

        while (match("+") || match("-")) {
            String op = tokens.get(-1).getLiteral();
            Ast.Expr second = parseMultiplicativeExpression();
            first = new Ast.Expr.Binary(op, first, second);
        }

        return first;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException { //TODO
        Ast.Expr first = parseSecondaryExpression();

        while (match("*") || match("/")) {
            String op = tokens.get(-1).getLiteral();
            Ast.Expr second = parseSecondaryExpression();
            first = new Ast.Expr.Binary(op, first, second);
        }

        return first;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expr parseSecondaryExpression() throws ParseException {  //TODO
        Ast.Expr inner = parsePrimaryExpression();

        while (match(".")) {
            if (!peek(Token.Type.IDENTIFIER)) {
                throw new ParseException("Expected identifier", tokens.index);
            }

            Ast.Expr outer = parsePrimaryExpression();

            if (outer instanceof Ast.Expr.Function) {
                if (inner instanceof Ast.Expr.Function) {
                    inner = new Ast.Expr.Function(Optional.of(new Ast.Expr.Function(((Ast.Expr.Function) inner).getReceiver(), ((Ast.Expr.Function) inner).getName(), ((Ast.Expr.Function) inner).getArguments())), ((Ast.Expr.Function) outer).getName(), ((Ast.Expr.Function) outer).getArguments());
                } else {
                    inner = new Ast.Expr.Function(Optional.of(new Ast.Expr.Access(((Ast.Expr.Access) inner).getReceiver(), ((Ast.Expr.Access) inner).getName())), ((Ast.Expr.Function) outer).getName(), ((Ast.Expr.Function) outer).getArguments());
                }
            } else {
                if (inner instanceof Ast.Expr.Function) {
                    inner = new Ast.Expr.Access(Optional.of(new Ast.Expr.Function(((Ast.Expr.Function) inner).getReceiver(), ((Ast.Expr.Function) inner).getName(), ((Ast.Expr.Function) inner).getArguments())), ((Ast.Expr.Access) outer).getName());
                } else {
                    inner = new Ast.Expr.Access(Optional.of(new Ast.Expr.Access(((Ast.Expr.Access) inner).getReceiver(), ((Ast.Expr.Access) inner).getName())), ((Ast.Expr.Access) outer).getName());
                }
            }
        }

        return inner;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expr parsePrimaryExpression() throws ParseException {    //TODO
        if (match("TRUE")) {
            return new Ast.Expr.Literal(true);
        } else if (match("NIL")) {
            return new Ast.Expr.Literal(null);
        } else if (match("FALSE")) {
            return new Ast.Expr.Literal(false);
        } else if (match(Token.Type.INTEGER)) {
            return new Ast.Expr.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.DECIMAL)) {
            return new Ast.Expr.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.CHARACTER)) {
            char c = tokens.get(-1).getLiteral().charAt(1);
            return new Ast.Expr.Literal(c);
        } else if (match(Token.Type.STRING)) {
            String name = tokens.get(-1).getLiteral();
            name = name.substring(1, name.length() - 1);
            name = name.replaceAll("\\\\n", "\n");
            name = name.replaceAll("\\\\r", "\r");
            name = name.replaceAll("\\\\t", "\t");
            name = name.replaceAll("\\\\b", "\b");
            name = name.replaceAll("\\\\f", "\f");
            return new Ast.Expr.Literal(name);
        } else if (match("(")) {
            Ast.Expr expr = parseExpression();
            if (!match(")")) {
                throw new ParseException("Expected closing parenthesis", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
            }
            return new Ast.Expr.Group(expr);
        } else if (match(Token.Type.IDENTIFIER)) {
            Ast.Expr.Access first = new Ast.Expr.Access(Optional.empty(), tokens.get(-1).getLiteral());

            if (match("(")) {
                List<Ast.Expr> argList = new ArrayList<>();
                if (!peek(")")) {
                    Ast.Expr arg = parseExpression();
                    argList.add(arg);
                    while (match(",")) {
                        arg = parseExpression();
                        argList.add(arg);
                    }
                }

                if (match(")")) {
                    return new Ast.Expr.Function(Optional.empty(), first.getName(), argList);
                } else {
                    throw new ParseException("Expected ')'", (tokens.index - 1) + tokens.get(-1).getLiteral().length());
                }
            }

            return first;
        } else {
            throw new ParseException("Invalid Primary Expression", tokens.index);
            // TODO fix the index thrown/ stored (access the tokens index)
        }

    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
