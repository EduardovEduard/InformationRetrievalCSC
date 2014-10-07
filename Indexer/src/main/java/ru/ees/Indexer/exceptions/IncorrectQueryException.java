package ru.ees.Indexer.exceptions;

public class IncorrectQueryException extends Exception {
    public IncorrectQueryException() {}

    public IncorrectQueryException(String message) {
        super(message);
    }
}
