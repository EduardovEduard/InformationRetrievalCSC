package ru.ees.Indexer;

import java.io.*;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.apache.commons.exec.*;

public class MyStem {
    public MyStem() throws IOException {
    }

    public List<String> lemmatize(Path file) throws IOException {
        String command = "mystem -dl " + file.toString();

        CommandLine line = CommandLine.parse(command);

        DefaultExecutor executor = new DefaultExecutor();
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        PumpStreamHandler handler = new PumpStreamHandler(stdout);

        executor.setStreamHandler(handler);
        executor.execute(line);

        String output = stdout.toString();

        return parseOutput(output);
    }

    private List<String> parseOutput(String stemOutput) {
        List<String> words = new ArrayList<>();
        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < stemOutput.length(); ++i) {
            char symbol = stemOutput.charAt(i);
            if (symbol == '?') {
                buffer.delete(0, buffer.length());
            }
            else if (symbol == '}' && buffer.length() != 0) {
                words.add(buffer.toString());
                buffer.delete(0, buffer.length());
            }
            else if (symbol != '{' && symbol != '}') {
                buffer.append(symbol);
            }
        }
        return words;
    }

    public static void main(String[] args) throws IOException {
        MyStem stem = new MyStem();
        Path file = Paths.get("resources/testData/a.txt");
        System.out.println(stem.lemmatize(file));
    }
}
