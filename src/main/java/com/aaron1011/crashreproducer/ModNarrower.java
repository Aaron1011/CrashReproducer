package com.aaron1011.crashreproducer;

import com.aaron1011.crashreproducer.graph.DirectedGraph;
import com.sun.org.apache.xpath.internal.operations.Mod;

import java.util.List;

public class ModNarrower {

    public void mainLoop(CrashReproducer reproducer, DirectedGraph<ModData> mods) {
        DirectedGraph<ModData> lastMods = DirectedGraph.copy(mods);
        int total = mods.getNodeCount();
        System.err.println(String.format("Total number: %s", mods.getNodeCount()));

        do {
            boolean errored = runServer(reproducer, mods);
            if (errored) {
                lastMods = DirectedGraph.copy(mods);
                mods = splitMods(mods);
            }

        }
        System.err.println("Performing binary search...");
    }

    private DirectedGraph<ModData> splitMods(DirectedGraph<ModData> mods) {
        List<DirectedGraph.DataNode<ModData>> newMods = mods.getNodes().subList(0, mods.getNodes().size() / 2);
    }

}
