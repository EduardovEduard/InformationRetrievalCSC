package ru.ees.Indexer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Index {
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
        backend.addTermDocument(term, document);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        finish();
    }
}
