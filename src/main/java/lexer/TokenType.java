package lexer;

public enum TokenType {
    // Ключевые слова
    KEYWORD,
    // Идентификаторы
    IDENTIFIER,
    // Числовые литералы
    NUMBER,
    // Строковые литералы
    STRING_LITERAL,
    // Операторы
    OPERATOR,
    // Разделители ( ; , { } ( ) и т.п.)
    SEPARATOR,
    // Комментарии (игнорируются, но можно для диагностики)
    COMMENT,
    // Ошибка лексического анализа
    ERROR,
    // Конец файла
    EOF
}

