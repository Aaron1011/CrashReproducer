package com.aaron1011.crashreproducer.detector;

import com.aaron1011.crashreproducer.CrashReproducer;

public interface Strategy {

    void prepare(CrashReproducer reproducer, Process process);

    boolean detectError();

}
