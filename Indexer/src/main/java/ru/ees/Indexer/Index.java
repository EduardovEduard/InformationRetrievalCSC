package ru.ees.Indexer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Index {
    private Map<String, Long> terms = new ConcurrentHashMap<>();
    private Map<String, Long> documents = new ConcurrentHashMap<>();
    private Path root;
    private IndexBackend backend;

    public Index(String root) {
        this.root = Paths.get(root);
    }

    public void start(String path) {
        backend = new IndexDBAccessor(Paths.get(path), false);
        backend.start();
    }

    public void finish() {
        backend.finish();
    }

    public void add(Collection<? extends String> set, String document) {
        for (String term : set)
            add(term, document);
    }

    public void add(String term, String document) {
        if (backend == null)
            throw new RuntimeException("Need to call ru.ees.Indexer.Index.start() before actual work!");

        if  (!documents.containsKey(document)) {
            long documentId = backend.addDocument(document);
            documents.put(document, documentId);
        }

        if (!terms.containsKey(term)) {
            long termId = backend.addTerm(term);
            terms.put(term, termId);
        }

        backend.addTermDocument(terms.get(term), documents.get(document));
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        finish();
    }
}
