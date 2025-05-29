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
            "<<=", ">>=", "->", ".", "?:", "?", ":", "::"
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
        char c = input.charAt(pos++);
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
        while (true) {
            Token token = nextToken();
            tokens.add(token);
            if (token.getType() == TokenType.EOF) {
                break;
            }
        }
        return tokens;
    }

    private Token nextToken() {
        skipWhitespaceAndComments();

        if (pos >= length) {
            return new Token(TokenType.EOF, "", line, column);
        }

        char c = peek();

        if (OPERATORS_START.contains(c)) { // OPERATORS_START — множество первых символов операторов
            return operator();
        }

        // Если символ — цифра, вызываем number()
        if (isDigit(c)) {
            return number();
        }

        // Если символ — буква или '_', вызываем identifierOrKeyword()
        if (isAlpha(c) || c == '_') {
            return identifierOrKeyword();
        }

        // Строковые литералы
        if (c == '"') {
            return stringLiteral();
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
        String msg = String.format("Неожиданный символ '%c' в %d:%d", errChar, errLine, errCol);
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

        char firstChar = peek();

        // Проверяем, что первый символ — буква или '_'
        if (!isAlpha(firstChar) && firstChar != '_') {
            String msg = String.format("Идентификатор не может начинаться с символа '%c' в %d:%d", firstChar, startLine, startCol);
            if (stopOnError) {
                throw new LexicalException(msg, startLine, startCol);
            } else {
                errors.add(msg);
                advance();
                return new Token(TokenType.ERROR, String.valueOf(firstChar), startLine, startCol);
            }
        }

        // Читаем идентификатор
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

        while (isDigit(peek())) {
            advance();
        }

        if (isAlpha(peek()) || peek() == '_') {
            String msg = String.format("Некорректный идентификатор, начинающийся с цифры в %d:%d", startLine, startCol);
            if (stopOnError) {
                throw new LexicalException(msg, startLine, startCol);
            } else {
                errors.add(msg);
                // Продолжаем читать как ошибочный токен
                while (isAlphaNumeric(peek()) || peek() == '_') {
                    advance();
                }
                String lexeme = input.substring(startPos, pos);
                return new Token(TokenType.ERROR, lexeme, startLine, startCol);
            }
        }

        boolean hasDot = false;

        while (true) {
            char c = peek();
            if (isDigit(c)) {
                advance();
            } else if (c == '.') {
                if (hasDot) {
                    String lexeme = input.substring(startPos, pos);
                    String msg = String.format("Неверный формат числа в %d:%d", startLine, startCol);
                    if (stopOnError) {
                        throw new LexicalException(msg, startLine, startCol);
                    } else {
                        errors.add(msg);
                        break;
                    }
                }
                hasDot = true;
                advance();
            } else {
                break;
            }
        }

        String lexeme = input.substring(startPos, pos);
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

        final int maxOpLength = 5; // увеличьте, если есть длинные операторы
        int maxLength = Math.min(maxOpLength, length - pos);

        // Попытаемся взять максимально длинную последовательность из операторских символов
        int endPos = pos;
        while (endPos < length && OPERATORS_START.contains(input.charAt(endPos)) && (endPos - pos) < maxOpLength) {
            endPos++;
        }

        String candidate = input.substring(pos, endPos);

        // Теперь пытаемся найти максимально длинный оператор из candidate, начиная с полной длины
        for (int len = candidate.length(); len > 0; len--) {
            String subOp = candidate.substring(0, len);
            if (OPERATORS.contains(subOp)) {
                // Проверяем, остались ли символы после subOp в candidate
                if (len < candidate.length()) {
                    // Остались дополнительные операторские символы — считаем ошибкой
                    String unknownOp = candidate;
                    String msg = String.format("Неизвестный оператор '%s' в %d:%d", unknownOp, startLine, startCol);
                    if (stopOnError) {
                        throw new LexicalException(msg, startLine, startCol);
                    } else {
                        // Продвигаем позицию на длину unknownOp, чтобы не зациклиться
                        for (int i = 0; i < unknownOp.length(); i++) {
                            advance();
                        }
                        errors.add(msg);
                        return new Token(TokenType.ERROR, unknownOp, startLine, startCol);
                    }
                } else {
                    // Оператор распознан корректно
                    for (int i = 0; i < subOp.length(); i++) {
                        advance();
                    }
                    return new Token(TokenType.OPERATOR, subOp, startLine, startCol);
                }
            }
        }

        // Если ни один оператор не распознан — ошибка
        String unknownOp = candidate;
        String msg = String.format("Неизвестный оператор '%s' в %d:%d", unknownOp, startLine, startCol);
        if (stopOnError) {
            throw new LexicalException(msg, startLine, startCol);
        } else {
            for (int i = 0; i < unknownOp.length(); i++) {
                advance();
            }
            errors.add(msg);
            return new Token(TokenType.ERROR, unknownOp, startLine, startCol);
        }
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
