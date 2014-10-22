package ru.ees.Indexer;

import java.util.ArrayList;
import java.util.List;

public class WordOccurences {
    String word;
    String document;
    List<Integer> coordinates = new ArrayList<>();

    public WordOccurences(String word, String document) {
        this.word = word;
        this.document = document;
    }

    public List<Integer> getCoordinates() {
        return coordinates;
    }

    public String getWord() {
        return word;
    }

    public String getDocument() {
        return document;
    }

    void add(int index) {
        coordinates.add(index);
    }

    @Override
    public String toString() {
        return word + " in " + document + " -> " + coordinates.toString();
    }
}
