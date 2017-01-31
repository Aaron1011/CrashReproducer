package com.aaron1011.crashreproducer;

import com.aaron1011.crashreproducer.graph.DirectedGraph;
import com.aaron1011.crashreproducer.graph.TopologicalOrder;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ModParser {

    private static final Splitter PART_SEPARATOR = Splitter.on(':').omitEmptyStrings().trimResults();
    private static final Splitter DEPS_SEPARATOR = Splitter.on(";").omitEmptyStrings().trimResults();

    private static final Splitter VERSION_SEPARATOR = Splitter.on('@').omitEmptyStrings().trimResults();

    public static DirectedGraph<ModData> constructGraph(Map<String, ModData> mods) {
        DirectedGraph<ModData> graph = new DirectedGraph<>();

        for (ModData mod: mods.values()) {
            parseDependencies(mod);
        }

        for (ModData mod: mods.values()) {
            for (String dependency: mod.parsedDependencies) {
                ModData otherMod = mods.get(dependency);
                if (otherMod != null) {
                    graph.addEdge(otherMod, mod);
                } else {
                    CrashReproducer.LOGGER.log(Level.WARNING, String.format("Dependency %s of mod %s not found - skipping", dependency, mod.id));
                }
            }
        }
        graph = TopologicalOrder.createOrderedLoad(graph);
        return graph;
    }

    private static void parseDependencies(ModData mod) {
        mod.parsedDependencies = Lists.newArrayList();
        String raw = mod.rawDependencies;
        for (String dep : DEPS_SEPARATOR.split(raw)) {
            String rawTargetMod = Iterables.get(PART_SEPARATOR.split(dep), 1);
            String finalMod = VERSION_SEPARATOR.split(rawTargetMod).iterator().next();

            mod.parsedDependencies.add(finalMod);
        }
    }

}
