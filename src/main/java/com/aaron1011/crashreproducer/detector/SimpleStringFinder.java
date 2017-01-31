package com.aaron1011.crashreproducer.detector;

public class SimpleStringFinder implements ErrorStringFinder {

    private final String string;
    private final boolean caseSensitive;

    public SimpleStringFinder(String string, boolean caseSensitive) {
        this.string = string;
        this.caseSensitive = caseSensitive;
    }

    @Override
    public boolean hasError(String line) {
        return this.caseSensitive ? line.contains(this.string) : line.toLowerCase().contains(this.string.toLowerCase());
    }
}
