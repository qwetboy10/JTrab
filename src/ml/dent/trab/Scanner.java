package ml.dent.trab;

import java.util.*;

import static ml.dent.trab.TokenType.*;

public class Scanner {

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fn", FN);
        keywords.put("if", IF);
        keywords.put("null", NULL);
        keywords.put("or", OR);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
        keywords.put("xor", XOR);
    }

    private static List<Token> tokens;
    private static String source;
    private static int start;
    private static int current;
    private static int line;
    private static Stack<StringState> state;

    private enum StringState {
        OUTSIDE, IN_STRING, IN_EXPRESSION
    }

    public static List<Token> scan(String s) {
        source = s;
        tokens = new LinkedList<Token>();
        line = 1;
        start = 0;
        current = 0;
        state = new Stack<>();
        state.push(StringState.OUTSIDE);
        while (!isAtEnd()) {
            scanToken();
            start = current;
        }
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private static void scanToken() {
        char c = advance();
        if (state.peek() == StringState.IN_STRING && c != '"' && c != '$' && c != '{') {
            if (c != '{')
                part();
        } else switch (c) {
            // one character tokens
            case '(':
                addToken(LEFT_PAREN);
                break;
            case ')':
                addToken(RIGHT_PAREN);
                break;
            case '{':
                addToken(LEFT_BRACKET);
                break;
            case '}':
                addToken(RIGHT_BRACKET);
                if (state.peek() == StringState.IN_EXPRESSION) state.pop();
                break;
            case ',':
                addToken(COMMA);
                break;
            case '.':
                addToken(DOT);
                break;
            case ';':
                addToken(SEMICOLON);
                break;
            case '[':
                addToken(LEFT_BRACE);
                break;
            case ']':
                addToken(RIGHT_BRACE);
                break;
            case ':':
                addToken(COLON);
                break;
            case '?':
                addToken(QUESTION);
                break;
            case '\\':
                addToken(BACKSLASH);
                break;
            case '$':
                addToken(DOLLAR);
                if (state.peek() == StringState.IN_STRING) state.push(StringState.IN_EXPRESSION);
                break;
            case '\n':
                //addToken(NEWLINE);
                line++;
                break;
            case ' ':
            case '\t':
            case '\r':
                break;

            // one or two character token
            case '-':
                if (match('-'))
                    addToken(MINUS_MINUS);
                else if (match('='))
                    addToken(MINUS_EQUAL);
                else if (match('>'))
                    addToken(ARROW);
                else
                    addToken(MINUS);
                break;
            case '+':
                addToken(match('+') ? PLUS_PLUS : match('=') ? PLUS_EQUAL : PLUS);
                break;
            case '/':
                if (match('='))
                    addToken(SLASH_EQUAL);
                else if (match('/'))
                    while (!isAtEnd() && peek() != '\n')
                        advance();
                else
                    addToken(SLASH);
                break;
            case '*':
                addToken(match('=') ? STAR_EQUAL : match('*') ? match('=') ? STAR_STAR_EQUAL : STAR_STAR : STAR);
                break;
            case '%':
                addToken(match('=') ? STAR_EQUAL : PERCENT);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            // more complex parsing
            case '"':
                addToken(QUOTE);
                if (state.peek() == StringState.OUTSIDE || state.peek() == StringState.IN_EXPRESSION)
                    state.push(StringState.IN_STRING);
                else if (state.peek() == StringState.IN_STRING) {
                    state.pop();
                }
                break;
            default:
                if (isNumeric(c))
                    number();
                else if (isAlphabetic(c))
                    identifier();
                else
                    Trab.error("Unexpected character '" + c + "' found", line);

        }
    }

    private static void number() {
        while (!isAtEnd() && isNumeric(peek())) {
            advance();
        }
        if (isNumeric(peekNext()) && match('.')) {

            while (!isAtEnd() && isNumeric(peek())) {
                advance();
            }
        }
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private static void identifier() {
        while (isAlphanumeric(peek())) {
            advance();
        }
        String identifier = source.substring(start, current);
        if (keywords.containsKey(identifier))
            addToken(keywords.get(identifier));
        else
            addToken(IDENTIFIER, identifier);
    }

    private static void part() {
        StringBuffer buf = new StringBuffer(previous() + "");
        while (!isAtEnd() && peek() != '"' && peek() != '$') {
            if (match('\\')) {
                if (match('"'))
                    buf.append(previous());
                else if (match('\\'))
                    buf.append(previous());
                else if (match('t'))
                    buf.append('\t');
                else if (match('n'))
                    buf.append('\n');
                else
                    Trab.error("Invalid escape sequence", line);
            } else
                buf.append(advance());
        }
        addToken(STRING_PART, buf.toString());
    }

    private static char advance() {
        return source.charAt(current++);
    }

    private static char peek() {
        return !isAtEnd() ? source.charAt(current) : '\0';
    }

    private static char peekNext() {
        return current + 1 < source.length() ? source.charAt(current + 1) : '\0';
    }

    private static char previous() {
        return current - 1 >= 0 ? source.charAt(current - 1) : '\0';
    }

    private static boolean match(char expected) {
        if (peek() == expected) {
            advance();
            return true;
        }
        return false;
    }

    private static void addToken(TokenType type) {
        addToken(type, null);
    }

    private static void addToken(TokenType type, Object value) {
        tokens.add(new Token(type, source.substring(start, current), value, line));
    }

    private static boolean isAtEnd() {
        return current >= source.length();
    }

    private static boolean isNumeric(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isAlphabetic(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_';
    }

    private static boolean isAlphanumeric(char c) {
        return isNumeric(c) || isAlphabetic(c);
    }
}
