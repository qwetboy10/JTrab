package ml.dent.trab;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static ml.dent.trab.TokenType.*;

public class Parser {
    private static class ParseError extends RuntimeException {
    }

    private List<Token> tokens;
    private int cur;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        LinkedList<Stmt> statements = new LinkedList<>();
        while (!isAtEnd()) {
            try {
                statements.add(statement());
            } catch (Parser.ParseError p) {
                synchronise();
            }
        }
        return statements;

    }

    private void synchronise() {
        while (!isAtEnd()) {
            advance();
            if (previous().type == NEWLINE)
                return;
        }
    }

    private Stmt statement() {
        //while (match(NEWLINE)) ;
        if (match(FOR)) {
            return forStmt();
        } else if (match(LEFT_BRACKET)) {
            return block();
        } else if (match(CLASS)) {
            return classStmt();
        } else if (match(FN)) {
            return function();
        } else if (match(IF)) {
            return ifStmt(previous());
        } else if (match(RETURN)) {
            return returnStmt();
        } else if (match(VAR)) {
            return varStmt();
        } else if (match(WHILE)) {
            return whileStmt(previous());
        } else {
            Expr expr = expression();
            Stmt stmt = new Stmt.PrintableExpression(expr);
            return stmt;
        }
    }

    private Stmt classStmt() {
        Token name = consume(IDENTIFIER, "Identifier expected");
        consume(LEFT_BRACKET, "Expect '{' after class declaration");
        List<Stmt.Function> methods = new ArrayList<>();
        while (!match(RIGHT_BRACKET) && !isAtEnd()) {
            methods.add(function());
        }
        if (isAtEnd() && previous().type != RIGHT_BRACKET)
            throw new RuntimeError(previous(), "Expect '}' after class declaration");
        return new Stmt.Class(name, methods);
    }

    private Stmt.Function function() {
        consume(IDENTIFIER, "Identifier expected");
        Token name = previous();
        consume(LEFT_PAREN, "Expect '(' after function declaration");
        List<Token> arguments = new ArrayList<>();
        if (!match(RIGHT_PAREN)) {
            do {
                arguments.add(consume(IDENTIFIER, "Expect identifier in argument list"));
            } while (match(COMMA));
            consume(RIGHT_PAREN, "Expect ')' after argument list");
        }
        Stmt body = null;
        if (match(ARROW)) {
            body = new Stmt.Block(List.of(new Stmt.Return(name, expression())));
        } else if (match(LEFT_BRACKET)) body = block();
        else throw new RuntimeError(name, "Expect '->' or '{' after function declaration");
        return new Stmt.Function(name, arguments, body);
    }

    private Stmt forStmt() {
        Token forToken = previous();
        consume(LEFT_PAREN, "Expect '(' after for");
        Stmt initializer = null;
        Expr condition = null;
        Expr increment = null;
        if (!match(SEMICOLON)) {
            initializer = statement();
            consume(SEMICOLON, "Expect ';' after expression");
        }
        if (!match(SEMICOLON)) {
            condition = expression();
            consume(SEMICOLON, "Expect ';' after expression");
        }
        if (!match(RIGHT_PAREN)) {
            increment = expression();
            consume(RIGHT_PAREN, "Expect ';' after expression ");
        }
        Stmt body = statement();
        Stmt block;
        if (increment != null) block = new Stmt.Block(List.<Stmt>of(body, new Stmt.Expression(increment)));
        else block = body;
        Stmt con;
        if (condition == null) con = new Stmt.Expression(new Expr.Literal(true));
        else con = new Stmt.Expression(condition);
        Stmt whil = new Stmt.While(forToken, condition, block);
        if (initializer != null) return new Stmt.Block(List.<Stmt>of(initializer, whil));
        else return whil;
    }

    private Stmt returnStmt() {
        return new Stmt.Return(previous(), expression());
    }

    private Stmt varStmt() {
        consume(IDENTIFIER, "Expect identifier after var");
        Expr.Variable expr = new Expr.Variable(previous());
        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }
        //if (!isAtEnd()) consume(NEWLINE, "Expect newline after statement");
        return new Stmt.Var(expr.name, initializer);
    }

    private Stmt ifStmt(Token ifToken) {
        consume(LEFT_PAREN, "Expect '(' after if");
        Expr conditional = expression();
        consume(RIGHT_PAREN, "Expect ')' after if");
        Stmt then = new Stmt.Block(List.of(statement()));
        Stmt el = null;
        if (match(ELSE)) {
            el = new Stmt.Block(List.of(statement()));
        }
        return new Stmt.If(ifToken, conditional, then, el);
    }

    private Stmt whileStmt(Token whileToken) {
        consume(LEFT_PAREN, "Expect '(' after while");
        Expr conditional = expression();
        consume(RIGHT_PAREN, "Expect ')' after while");
        Stmt body = new Stmt.Block(List.of(statement()));
        return new Stmt.While(whileToken, conditional, body);
    }

    private Stmt block() {
        List<Stmt> statements = new LinkedList<>();
        while (peek().type != RIGHT_BRACKET) {
            statements.add(statement());
        }
        consume(RIGHT_BRACKET, "Expect '}' after block declaration");
        return new Stmt.Block(statements);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = lambda();
        if (match(EQUAL, PLUS_EQUAL, MINUS_EQUAL, STAR_EQUAL, SLASH_EQUAL, PERCENT_EQUAL, STAR_STAR_EQUAL)) {
            Token equals = previous();
            Expr right = expression();
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, equals, right);
            }
            error(equals, "Invalid assignment target");
        }
        return expr;
    }

    private Expr lambda() {
        if (match(BACKSLASH)) {
            Token operator = previous();
            List<Token> arguments = new LinkedList<>();
            do {
                if (match(IDENTIFIER))
                    arguments.add(previous());
                else
                    error(peek(), "Identifier expected");
            } while (match(COMMA));
            consume(ARROW, "Expect '->' after lambda declaration");
            if (match(LEFT_BRACKET)) {
                Stmt stmt = block();
                return new Expr.Lambda(arguments, stmt);
            } else {
                Expr e = expression();
                return new Expr.Lambda(arguments, new Stmt.Return(previous(), e));
            }
        }
        return or();
    }

    private Expr or() {
        Expr expr = and();
        while (match(OR)) {
            expr = new Expr.Logical(expr, previous(), and());
        }
        return expr;
    }

    private Expr and() {
        Expr expr = equality();
        while (match(AND)) {
            expr = new Expr.Logical(expr, previous(), equality());
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            expr = new Expr.Binary(expr, previous(), comparison());
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = addition();
        while (match(LESS, LESS_EQUAL, GREATER, GREATER_EQUAL)) {
            expr = new Expr.Binary(expr, previous(), addition());
        }
        return expr;
    }

    private Expr addition() {
        Expr expr = multiplication();
        while (match(PLUS, MINUS)) {
            expr = new Expr.Binary(expr, previous(), multiplication());
        }
        return expr;
    }

    private Expr multiplication() {
        Expr expr = power();
        while (match(SLASH, STAR, PERCENT)) {
            expr = new Expr.Binary(expr, previous(), power());
        }
        return expr;
    }

    private Expr power() {
        Expr expr = unary();
        while (match(STAR_STAR)) {
            expr = new Expr.Binary(expr, previous(), unary());
        }
        return expr;
    }

    private Expr unary() {
        if (match(MINUS, BANG, PLUS_PLUS, MINUS_MINUS, QUESTION)) {
            return new Expr.Unary(previous(), unary());
        }
        return post();

    }

    private Expr post() {
        Expr expr = index();
        while (match(PLUS_PLUS, MINUS_MINUS)) {
            expr = new Expr.Post(previous(), expr);
        }
        return expr;
    }

    private Expr index() {
        Expr expr = primary();
        while (true) {
            if (match(LEFT_PAREN)) {
                Token paren = previous();
                LinkedList<Expr> args = new LinkedList<>();
                if (!match(RIGHT_PAREN)) {
                    do {
                        args.add(expression());
                    } while (match(COMMA));
                    consume(RIGHT_PAREN, "Expect ')' at end of argument list");
                }
                expr = new Expr.Call(paren, expr, args);
            } else if (match(DOT)) {
                consume(IDENTIFIER, "Expect identifier");
                expr = new Expr.Get(expr, previous());
            } else if (match(LEFT_BRACE)) {
                Token brace = previous();
                Expr start = null;
                if (!check(COLON)) start = expression();
                Expr end = null;
                Expr step = null;
                if (match(COLON)) {
                    if (!check(COLON)) end = expression();
                    if (match(COLON)) {
                        if (!check(RIGHT_BRACE)) step = expression();
                    }
                }
                consume(RIGHT_BRACE, "Expect ']' after list index");
                expr = new Expr.Index(expr, start, end, step, brace);
            } else
                break;
        }
        return expr;
    }

    private Expr primary() {
        // Literals and variables
        if (match(TRUE)) {
            return new Expr.Literal(true);
        }
        if (match(FALSE)) {
            return new Expr.Literal(false);
        }
        if (match(NULL)) {
            return new Expr.Literal(new TrabNull());
        }
        if (match(NUMBER)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(QUOTE)) {
            return string();
        }
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        // Parenthesis
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression");
            return new Expr.Grouping(expr);
        }
        // Lists
        if (match(LEFT_BRACE)) {
            return list(previous());
        }
        throw error(peek(), "Expect expression");
    }

    private Expr string() {
        List<Expr> parts = new LinkedList<>();
        while (!isAtEnd() && !match(QUOTE)) {
            if (match(STRING_PART)) {
                parts.add(new Expr.Literal(previous().literal));
            } else if (match(DOLLAR)) {
                if (match(LEFT_BRACKET)) {
                    parts.add(expression());
                    consume(RIGHT_BRACKET, "Expect '}' after expression");
                } else {
                    consume(IDENTIFIER, "Expect identifier in string");
                    parts.add(new Expr.Variable(previous()));
                }
            } else
                throw error(peek(), "Malformed String");
        }
        if (previous().type != QUOTE)
            Trab.error(previous(), "Unclosed String");
        return new Expr.TrabString(parts);
    }

    private Expr list(Token start) {
        List<Expr> values = new LinkedList<>();
        if(match(RIGHT_BRACE)) return new Expr.TrabList(values, start);
        do {
            values.add(expression());
        } while (match(COMMA));
        consume(RIGHT_BRACE, "Expect ']' after list");
        return new Expr.TrabList(values, start);
    }

    private ParseError error(Token token, String message) {
        Trab.error(token, message);
        return new ParseError();
    }

    private Token peek() {
        return tokens.get(cur);
    }

    private Token previous() {
        return tokens.get(cur - 1);
    }

    private boolean check(TokenType type) {
        return peek().type == type;
    }

    private Token advance() {
        return tokens.get(cur++);
    }

    private Token consume(TokenType type, String error) {
        if (!isAtEnd() && match(type))
            return previous();
        throw (error(peek(), error));
    }

    private boolean match(TokenType... types) {
        for (TokenType t : types)
            if (check(t) && !isAtEnd()) {
                cur++;
                return true;
            }
        return false;
    }

    private boolean isAtEnd() {
        return check(EOF);
    }
}
