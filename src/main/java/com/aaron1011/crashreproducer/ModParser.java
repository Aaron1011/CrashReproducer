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

    public static List<ModData> sortMods(Map<String, ModData> mods) {
        DirectedGraph<ModData> graph = new DirectedGraph<>();

        for (ModData mod: mods.values()) {
            parseDependencies(mod);
        }

        // Edges in the graph point from dependency to dependant.
        // For example, if a mod A depends on a mod B, the graph will contain
        // an edge from B to A
        // The graph is then sorted into a list such that a mod always
        // comes after all of its dependants. This allows mods to removed off of
        // the front of the list while estill ensuring that all dependencies are always met
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
        return TopologicalOrder.createOrderedLoad(graph);
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
