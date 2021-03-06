package ru.ees.SearchEngine;

import ru.ees.Indexer.IndexBackend;
import ru.ees.Indexer.IndexDBAccessor;
import ru.ees.Indexer.exceptions.IncorrectQueryException;

import java.util.List;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SearchEngine {

    IndexBackend index;

    public SearchEngine(IndexBackend index) {
        this.index = index;
    }

    public void start(InputStream istream, OutputStream ostream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(istream));
        PrintWriter writer = new PrintWriter(ostream);
        String line;
        System.out.println("Enter \\quit to finish.");
        try {
            while ((line = reader.readLine()) != null) {
                if (line.equals("\\quit")) {
                    System.out.println("Thank you for using my search application!");
                    return;
                }
                try {
                    List<String> files = index.processQuery(line);
                    if (files.size() == 0) {
                        writer.println("\tno documents found!");
                        writer.flush();
                    } else {
                        writer.print("\tfound: ");
                        for (int i = 0; i < files.size(); ++i) {
                            if (i > 1) {
                                writer.print("and " + (files.size() - i) + " more...");
                                break;
                            }
                            writer.print(files.get(i) + " ");
                        }
                        writer.println();
                        writer.flush();
                    }
                } catch (IncorrectQueryException e) {
                    System.err.println("Incorrect query!");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage java -jar SearchEngine <Path-to-index>");
            return;
        }

        String indexPath = args[0];
        if (!Files.exists(Paths.get(indexPath))) {
            System.out.println("No such file: " + indexPath + " found!");
            return;
        }

        IndexBackend index = IndexDBAccessor.use(Paths.get(indexPath));
        SearchEngine engine = new SearchEngine(index);
        engine.start(System.in, System.out);
    }
}
