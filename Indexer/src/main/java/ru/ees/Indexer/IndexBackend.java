package ru.ees.Indexer;

import ru.ees.Indexer.exceptions.IncorrectQueryException;

import java.util.List;

public interface IndexBackend {
    public void start();
    public void finish();

    public long addDocument(String document);
    public long addTerm(String document);
    public void addTermDocument(String term, String document);
    public void addTermPositionsInDocument(String term, String document, List<Integer> positions);

    public List<String> processQuery(String query) throws IncorrectQueryException;
}
