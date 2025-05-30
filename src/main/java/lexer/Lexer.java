package lexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Lexer {

    private static final Set<String> KEYWORDS = Set.of(
            "auto", "break", "case", "char", "const", "continue", "default",
            "do", "double", "else", "enum", "extern", "float", "for",
            "goto", "if", "int", "long", "register", "return", "short",
            "signed", "sizeof", "static", "struct", "switch", "typedef",
            "union", "unsigned", "void", "volatile", "while", "class",
            "public", "private", "protected", "virtual", "template", "typename",
            "using", "namespace", "bool", "true", "false", "nullptr"
    );

    private static final Set<Character> OPERATORS_START = Set.of(
            '+', '-', '*', '/', '%', '=', '<', '>', '!', '&', '|', '^', '~', '?', ':', '.'
    );

    private static final Set<String> OPERATORS = Set.of(
            "+", "-", "*", "/", "%", "=", "==", "!=", "<", "<=", ">", ">=", "&&", "||",
            "++", "--", "+=", "-=", "*=", "/=", "%=", "&", "|", "^", "~", "<<", ">>",
            "<<=", ">>=", "->", ".", "?:", "?", ":"
    );

    private static final Set<Character> SEPARATORS = Set.of(
            '(', ')', '{', '}', '[', ']', ';', ',', '#'
    );

    private final String input;
    private final int length;
    private int pos = 0;
    private int line = 1;
    private int column = 1;

    private final List<String> errors = new ArrayList<>();

    private boolean stopOnError = false;

    public Lexer(String input) {
        this.input = input;
        this.length = input.length();
    }

    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }

    public List<String> getErrors() {
        return errors;
    }

    private char peek() {
        if (pos >= length) return '\0';
        return input.charAt(pos);
    }

    private char peekNext() {
        if (pos + 1 >= length) return '\0';
        return input.charAt(pos + 1);
    }

    private char advance() {
        char c = peek();
        pos++;
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private boolean match(char expected) {
        if (peek() == expected) {
            advance();
            return true;
        }
        return false;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        try {
            while (true) {
                Token token = nextToken();
                tokens.add(token);
                if (token.getType() == TokenType.EOF) {
                    break;
                }
            }
        } catch (LexicalException e) {
            System.err.printf("Лексическая ошибка: %s%n", e.getMessage());
            // Можно вернуть токены, собранные до ошибки
        }
        return tokens;
    }

    private Token nextToken() {
        skipWhitespaceAndComments();

        if (pos >= length) {
            return new Token(TokenType.EOF, "", line, column);
        }

        char c = peek();

        // Идентификаторы и ключевые слова
        if (isAlpha(c) || c == '_') {
            return identifierOrKeyword();
        }

        // Числа
        if (isDigit(c)) {
            return number();
        }

        // Строковые литералы
        if (c == '"') {
            return stringLiteral();
        }

        // Операторы и разделители
        if (OPERATORS_START.contains(c)) {
            return operator();
        }

        if (SEPARATORS.contains(c)) {
            int startLine = line;
            int startCol = column;
            char sep = advance();
            return new Token(TokenType.SEPARATOR, String.valueOf(sep), startLine, startCol);
        }

        // Если символ не распознан — ошибка
        int errLine = line;
        int errCol = column;
        char errChar = advance();
        String msg = String.format("Неожиданный символ '%c' в %d", errChar, errLine);
        errors.add(msg);

        if (stopOnError) {
            throw new LexicalException(msg, errLine, errCol);
        }

        return new Token(TokenType.ERROR, String.valueOf(errChar), errLine, errCol);
    }

    private void skipWhitespaceAndComments() {
        while (true) {
            char c = peek();
            if (c == ' ' || c == '\r' || c == '\t' || c == '\n') {
                advance();
            } else if (c == '/' && peekNext() == '/') {
                // Однострочный комментарий
                advance(); // '/'
                advance(); // '/'
                while (peek() != '\n' && peek() != '\0') {
                    advance();
                }
            } else if (c == '/' && peekNext() == '*') {
                // Многострочный комментарий
                advance(); // '/'
                advance(); // '*'
                while (true) {
                    if (peek() == '\0') {
                        errors.add(String.format("Незакрытый комментарий в %d", line));
                        if (stopOnError) {
                            throw new LexicalException("Незакрытый комментарий", line, column);
                        }
                        break;
                    }
                    if (peek() == '*' && peekNext() == '/') {
                        advance();
                        advance();
                        break;
                    }
                    advance();
                }
            } else {
                break;
            }
        }
    }

    private Token identifierOrKeyword() {
        int startPos = pos;
        int startLine = line;
        int startCol = column;

        while (isAlphaNumeric(peek()) || peek() == '_') {
            advance();
        }

        String lexeme = input.substring(startPos, pos);
        TokenType type = KEYWORDS.contains(lexeme) ? TokenType.KEYWORD : TokenType.IDENTIFIER;
        return new Token(type, lexeme, startLine, startCol);
    }

    private Token number() {
        int startPos = pos;
        int startLine = line;
        int startCol = column;

        boolean hasDot = false;
        boolean hasExp = false;

        while (true) {
            char c = peek();
            if (isDigit(c)) {
                advance();
            } else if (c == '.' && !hasDot) {
                hasDot = true;
                advance();
            } else if ((c == 'e' || c == 'E') && !hasExp) {
                hasExp = true;
                advance();
                if (peek() == '+' || peek() == '-') {
                    advance();
                }
            } else {
                break;
            }
        }

        String lexeme = input.substring(startPos, pos);

        if (lexeme.chars().filter(ch -> ch == '.').count() > 1) {
            String msg = String.format("Неверный формат числа '%s' в %d", lexeme, startLine);
            errors.add(msg);
            if (stopOnError) {
                throw new LexicalException(msg, startLine, startCol);
            }
            return new Token(TokenType.ERROR, lexeme, startLine, startCol);
        }

        return new Token(TokenType.NUMBER, lexeme, startLine, startCol);
    }

    private Token stringLiteral() {
        int startLine = line;
        int startCol = column;
        advance(); // пропускаем начальную кавычку

        StringBuilder sb = new StringBuilder();
        boolean closed = false;

        while (true) {
            char c = peek();
            if (c == '\0' || c == '\n') {
                break; // ошибка — не закрыта строка
            }
            if (c == '"') {
                closed = true;
                advance();
                break;
            }
            if (c == '\\') {
                advance();
                char next = peek();
                switch (next) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '\\' -> sb.append('\\');
                    case '"' -> sb.append('"');
                    default -> sb.append(next);
                }
                advance();
            } else {
                sb.append(c);
                advance();
            }
        }

        if (!closed) {
            String msg = String.format("Незакрытая строковая константа в %d", startLine);
            errors.add(msg);
            if (stopOnError) {
                throw new LexicalException(msg, startLine, startCol);
            }
            return new Token(TokenType.ERROR, sb.toString(), startLine, startCol);
        }

        return new Token(TokenType.STRING_LITERAL, sb.toString(), startLine, startCol);
    }

    private Token operator() {
        int startLine = line;
        int startCol = column;

        int maxLen = Math.min(3, length - pos);

        for (int len = maxLen; len > 0; len--) {
            String candidate = input.substring(pos, pos + len);
            if (OPERATORS.contains(candidate)) {
                for (int i = 0; i < len; i++) {
                    advance();
                }
                return new Token(TokenType.OPERATOR, candidate, startLine, startCol);
            }
        }

        char c = advance();
        String msg = String.format("Неизвестный оператор '%c' в %d", c, startLine);
        errors.add(msg);
        if (stopOnError) {
            throw new LexicalException(msg, startLine, startCol);
        }
        return new Token(TokenType.ERROR, String.valueOf(c), startLine, startCol);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    public static Lexer fromFile(Path path) throws IOException {
        String content = Files.readString(path);
        return new Lexer(content);
    }
}
