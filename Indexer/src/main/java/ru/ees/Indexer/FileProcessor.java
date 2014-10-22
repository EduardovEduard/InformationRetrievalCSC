package ru.ees.Indexer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class FileProcessor {
    private Path path;
    private MyStem myStem;

    public FileProcessor(Path path) {
        this.path = path;

        try {
            myStem = new MyStem();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Collection<WordOccurences> getAllTerms() {
        try {
            List<String> terms = myStem.lemmatize(path);
            Map<String, WordOccurences> occurences = new HashMap<>();
            for (int i = 0; i < terms.size(); ++i) {
                String term = terms.get(i);
                System.out.println(term);
                if (occurences.containsKey(term)) {
                    occurences.get(term).add(i);
                } else {
                    occurences.put(term, new WordOccurences(term, path.toString()));
                    occurences.get(term).add(i);
                }
            }

            return occurences.values();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
