package lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static lox.TokenType.*;

/**
 * @author chenpeng
 * @since 2018-12-28 15:55
 */
class Parser {

    static final int MAX_ARGS_SIZE = 8;

    private static class ParserError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    Expr parseExpr() {
        return expression();
    }

    private Stmt declaration() {
        try {
            if (match(CLASS)) return classDeclaration();
            if (match(VAR)) return varDeclaration();
            if (match(FUN)) return function("function");

            return statement();
        } catch (ParserError e) {
            synchronize();
            return null;
        }
    }

    private Stmt classDeclaration() {
        Token name = consume(IDENTIFIER, "Expect class name.");
        consume(LEFT_BRACE, "Expect '{' before class body.");

        List<Stmt.Function> methods = new ArrayList<>();
        List<Stmt.Function> staticMethods = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            if (match(CLASS)) {
                staticMethods.add(function("method"));
            } else {
                methods.add(function("method"));
            }
        }

        consume(RIGHT_BRACE, "Expect '}' after class body");

        return new Stmt.Class(name, methods, staticMethods);

    }

    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, String.format("Expect %s name.", kind));
        consume(LEFT_PAREN, String.format("Expect '(' after %s name.", kind));

        List<Token> params = parameters();

        consume(LEFT_BRACE, String.format("Expect '{' before %s body.", kind));

        List<Stmt> body = block();
        return new Stmt.Function(name, params, body);

    }

    private Stmt varDeclaration() {

        List<Stmt.Var> vars = new ArrayList<>();
        vars.add(varDeclHelper());
        while (match(COMMA)) {
            vars.add(varDeclHelper());
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");

        return new Stmt.MultiVar(vars);
    }

    private Stmt.Var varDeclHelper() {
        Token name = consume(IDENTIFIER, "Expect variable name");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        return new Stmt.Var(name, initializer);
    }

    private Stmt statement() {
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
//        if (match(PRINT)) return printStatement();
        if (match(RETURN)) return returnStatement();
        if (match(WHILE)) return whileStatement();

        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }
        consume(SEMICOLON, "Expect ';' after return value.");

        return new Stmt.Return(keyword, value);
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }

        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clause.");

        Stmt body = statement();
        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after while condition.");

        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");

        return statements;
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression");
        return new Stmt.Expression(expr);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = lambda();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get)expr;
                return new Expr.Set(get.object, get.name, value);
            }

            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    private Expr lambda() {
        if (match(LAMBDA)) {
            Token name = previous();
            consume(LEFT_PAREN, "Expect '(' after lambda.");
            List<Token> params = parameters();

            List<Stmt> body = null;
            if (match(LEFT_BRACE)) {
                body = block();
            } else {
                if (match(RETURN)) {
                    error(previous(), "Expect lambda expression, not return statement.");
                }
                Token keyword = new Token(RETURN, "return", null, peek().line);
                Expr value = expression();
                body = new ArrayList<>();
                body.add(new Stmt.Return(keyword, value));
            }

            return new Expr.Lambda(name, params, body);

        }

        return ternary();
    }

    private List<Token> parameters() {
        List<Token> params = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (params.size() > MAX_ARGS_SIZE) {
                    error(peek(), "Cannot have more than 8 arguments.");
                }
                params.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");
        return params;
    }

    private Expr ternary() {
        Expr expr = or();

        if (match(QUESTION)) {
            Token mark = previous();
            Expr then = ternary();
            if (match(COLON)) {
                Expr other = ternary();
                return new Expr.Ternary(expr, then, other);
            }

            error(mark, "Invalid expression.");
        }
        return expr;
    }

    private Expr or() {
        return logicalHelper(this::and, OR);
    }

    private Expr and() {
        return logicalHelper(this::equality, AND);
    }

    private Expr logicalHelper(Supplier<Expr> logicalSupplier, TokenType type) {
        Expr expr = logicalSupplier.get();
        while (match(type)) {
            Token operator = previous();
            Expr right = logicalSupplier.get();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr equality() {
        return binaryHelper(this::comparision, BANG_EQUAL, EQUAL_EQUAL);
    }

    private Expr binaryHelper(Supplier<Expr> exprSupplier, TokenType... types) {
        Expr expr = exprSupplier.get();
        while (match(types)) {
            Token operator = previous();
            Expr right = exprSupplier.get();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparision() {
        return binaryHelper(this::addition, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL);
    }

    private Expr addition() {
        return binaryHelper(this::multiplication, MINUS, PLUS);
    }

    private Expr multiplication() {
        return binaryHelper(this::unary, SLASH, STAR, MODULO);
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {
                Token name = consume(IDENTIFIER, "Expect property name after '.'.");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() > MAX_ARGS_SIZE) {
                    error(peek(), "Cannot have more than 8 arguments");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }
        Token paren = consume(RIGHT_PAREN, "Expect '(' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(THIS)) return new Expr.This(previous());

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    private ParserError error(Token token, String message) {
        Lox.error(token, message);
        return new ParserError();
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }


    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
//                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }
}
