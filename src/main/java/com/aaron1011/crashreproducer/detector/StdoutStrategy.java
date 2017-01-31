package com.aaron1011.crashreproducer.detector;

import com.aaron1011.crashreproducer.CrashReproducer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class StdoutStrategy implements Strategy {

    private final ErrorStringFinder finder;
    private BufferedReader reader;

    public StdoutStrategy(ErrorStringFinder finder) {
        this.finder = finder;
    }

    @Override
    public void prepare(CrashReproducer reproducer, Process process) {
        this.reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    }

    @Override
    public boolean detectError() {
        try {
            if (!this.reader.ready()) {
                return false;
            }

            String line = this.reader.readLine();
            if (this.finder.hasError(line)) {
                System.err.println("Error detected in line: ");
                System.err.println(line);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
