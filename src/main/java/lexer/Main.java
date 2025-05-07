package lexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Запуск: java -jar lexanalyzer.jar <file1.cpp> [file2.cpp ...]");
            System.exit(1);
        }

        StringBuilder combinedInput = new StringBuilder();

        for (String fileName : args) {
            Path path = Path.of(fileName);
            if (!Files.exists(path)) {
                System.err.printf("Файл не найден: %s%n", fileName);
                continue;
            }
            try {
                String content = Files.readString(path);
                combinedInput.append(content).append("\n");
            } catch (IOException e) {
                System.err.printf("Не удалось прочитать файл %s: %s%n", fileName, e.getMessage());
            }
        }

        Lexer lexer = new Lexer(combinedInput.toString());
        List<Token> tokens = lexer.tokenize();

        for (Token token : tokens) {
            System.out.println(token);
        }

        if (!lexer.getErrors().isEmpty()) {
            System.err.println("\nErrors:");
            lexer.getErrors().forEach(System.err::println);
            System.exit(2);
        }
    }
}

