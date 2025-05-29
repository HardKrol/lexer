package lexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Main {

    private static final Set<TokenType> GROUP_TYPES = Set.of(
            TokenType.IDENTIFIER,
            TokenType.STRING_LITERAL,
            TokenType.NUMBER
    );

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Использование: java -jar lexanalyzer.jar [--stopOnError] <файл1.cpp> [файл2.cpp ...]");
            System.exit(1);
        }

        boolean stopOnError = false;
        List<String> fileArgs = new ArrayList<>();

        for (String arg : args) {
            if (arg.equals("--stopOnError")) {
                stopOnError = true;
            } else {
                fileArgs.add(arg);
            }
        }

        if (fileArgs.isEmpty()) {
            System.err.println("Не указаны входные файлы.");
            System.exit(1);
        }

        StringBuilder combinedInput = new StringBuilder();

        for (String fileName : fileArgs) {
            Path path = Path.of(fileName);
            if (!Files.exists(path)) {
                System.err.printf("Файл не найден: %s%n", fileName);
                continue;
            }
            try {
                String content = Files.readString(path);
                combinedInput.append(content).append("\n");
            } catch (IOException e) {
                System.err.printf("Ошибка чтения файла %s: %s%n", fileName, e.getMessage());
            }
        }

        Lexer lexer = new Lexer(combinedInput.toString());
        lexer.setStopOnError(stopOnError);

        List<Token> tokens = lexer.tokenize();

        // Считаем лексемы для групп IDENTIFIER, STRING_LITERAL, NUMBER
        Map<TokenType, Integer> groupCounts = new EnumMap<>(TokenType.class);

        // Считаем остальные лексемы по отдельности
        Map<String, Integer> otherLexemeCounts = new HashMap<>();

        int totalTokens = 0;

        for (Token token : tokens) {
            TokenType type = token.getType();

            if (type == TokenType.EOF || type == TokenType.ERROR) {
                continue; // пропускаем EOF и ошибки
            }

            totalTokens++;

            if (GROUP_TYPES.contains(type)) {
                groupCounts.merge(type, 1, Integer::sum);
            } else {
                otherLexemeCounts.merge(token.getLexeme(), 1, Integer::sum);
            }
        }

        System.out.printf("%nВсего лексем: %d%n", totalTokens);
        // Выводим статистику по остальным лексемам

        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(otherLexemeCounts.entrySet());
        sortedEntries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        // Выводим статистику по группам (IDENTIFIER, STRING_LITERAL, NUMBER)
        System.out.println("Статистика по группам лексем:");
        for (TokenType groupType : GROUP_TYPES) {
            int count = groupCounts.getOrDefault(groupType, 0);
            double relative = totalTokens > 0 ? (count * 100.0) / totalTokens : 0;
            System.out.printf("  %-15s : %5d (%.2f%%)%n", groupType.name(), count, relative);
        }


        for (Map.Entry<String, Integer> entry : sortedEntries) {
            String lexeme = entry.getKey();
            int count = entry.getValue();
            double relative = totalTokens > 0 ? (count * 100.0) / totalTokens : 0;
            System.out.printf("  %-15s : %5d (%.2f%%)%n", lexeme, count, relative);
        }

        if (!lexer.getErrors().isEmpty()) {
            System.err.println("\nОшибки лексического анализа:");
            lexer.getErrors().forEach(System.err::println);
            System.exit(2);
        }
    }
}
