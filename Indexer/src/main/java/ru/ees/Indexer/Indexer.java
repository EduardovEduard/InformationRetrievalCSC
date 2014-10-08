package ru.ees.Indexer;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Indexer {
    private String directory;
    private Index index;
    ExecutorService service = Executors.newCachedThreadPool();

    public Indexer(String directory) {
        this.directory = directory;
        index = new Index(directory);
    }

    public Index buildIndex(String result) {
        try {
            index.start(result);
            Files.walk(Paths.get(directory)).forEach(file -> {
                if (Files.isRegularFile(file)) {
                    service.submit(new ProcessFileTask(file, index));
                }
            });

            service.shutdown();

            try {
                service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            index.finish();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return index;
    }

    private class ProcessFileTask implements Runnable {
        Index index;
        Path file;

        public ProcessFileTask(Path file, Index index) {
            this.index = index;
            this.file = file.toAbsolutePath();
        }

        @Override
        public void run() {
            System.out.println("Processing file: " + file.toString());
            FileProcessor processor = new FileProcessor(file);
            Set<String> terms = processor.getAllTerms();
            index.add(terms, file.toString());
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java -jar ru.ees.Indexer.Indexer.jar <Directory> <ResultFile>");
            return;
        }
        String directory = args[0];
        String result = args[1];
        Indexer indexer = new Indexer(directory);
        Index index = indexer.buildIndex(result);
    }
}
