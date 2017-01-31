package com.aaron1011.crashreproducer.detector;

import java.util.regex.Pattern;

public class RegexErrorFinder implements ErrorStringFinder {

    private Pattern pattern;

    public RegexErrorFinder(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean hasError(String line) {
        return this.pattern.matcher(line).matches();
    }
}
