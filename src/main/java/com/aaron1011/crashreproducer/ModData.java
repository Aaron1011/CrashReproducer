package com.aaron1011.crashreproducer;

import com.google.common.base.Joiner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ModData {

    public String id;
    public String rawDependencies;
    public List<String> parsedDependencies;
    public final Path location;
    private final Path disabledLocation;
    private boolean enabled = true;

    public ModData(String id, String dependencies, Path location) {
        this.id = id;
        this.rawDependencies = dependencies;
        this.location = location;
        this.disabledLocation = this.location.resolveSibling(this.location.getFileName() + ".disabled");
    }

    public String toString() {
        return this.id + "[" + Joiner.on(",").join(this.parsedDependencies) + "]";
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (enabled == this.enabled) {
            return;
        }

        Path from = this.enabled ? this.location : this.disabledLocation;
        Path to = this.enabled ? this.disabledLocation : this.location;
        try {
            Files.move(from, to);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.enabled = enabled;
    }
}
