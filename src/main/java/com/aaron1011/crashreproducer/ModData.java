package com.aaron1011.crashreproducer;

import com.google.common.base.Joiner;

import java.nio.file.Path;
import java.util.List;

public class ModData {

    public String id;
    public String rawDependencies;
    public List<String> parsedDependencies;
    public Path location;

    public ModData(String id, String dependencies, Path location) {
        this.id = id;
        this.rawDependencies = dependencies;
        this.location = location;
    }

    public String toString() {
        return this.id + "[" + Joiner.on(",").join(this.parsedDependencies) + "]";
    }
}
