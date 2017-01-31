package com.aaron1011.crashreproducer;

import com.aaron1011.crashreproducer.detector.SimpleStringFinder;
import com.aaron1011.crashreproducer.detector.StdoutStrategy;
import com.aaron1011.crashreproducer.detector.Strategy;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ModNarrower {

    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean timedOut = false;

    public void mainLoop(CrashReproducer reproducer, List<ModData> mods) {
        List<ModData> originalMods = mods;
        List<ModData> lastMods = mods;
        int total = mods.size();
        System.err.println(String.format("Total number: %s", mods.size()));
        System.err.println("Performing binary search...");

        boolean ranSuccessfully = false;
        while (true) {
            ranSuccessfully = runServer(reproducer, mods);
            if (!ranSuccessfully) {
                lastMods = mods;
                if (mods.size() == 0) {
                    System.err.println("Server error detected even with no mods installed!");
                    break;
                }

                mods = splitMods(mods);
                this.updateMods(originalMods, mods);
            } else {
                break;
            }

        }

        if (!ranSuccessfully) {
            // TODO: Do something?
            return;
        }
        System.err.println("Found non-crashing mod list: " + mods);
        System.err.println("Re-adding mods until server crashes again...");

        while (true) {
            mods = this.reAddMods(lastMods, mods);
            this.updateMods(originalMods, mods);

            ranSuccessfully = runServer(reproducer, mods);
            if (ranSuccessfully) {
                if (mods.size() == lastMods.size()) {
                    throw new IllegalStateException("Inconsistent results! Server previously failed with modlist '" + lastMods + "' but succeeded this time with list '" + mods + "'");
                }
                continue;
            }
            break;
        }
        System.err.println("Server failed on addition of mod '" + mods.get(0) + "'");
        System.err.println("Finished! Suspect mod: " + mods.get(0));
        System.err.println(String.format("Complete modlist for final run: %s", mods));
        System.err.println("Done!");

        this.scheduler.shutdownNow();
        //System.err.println("Performing final run with list '" + finalList + "'");

        /*if (!runServer(reproducer, finalList)) {
            throw new IllegalStateException(String.format("Inconsistency! Server suceeded previously with modlist '%s', but on final run, failed with the same modlist!", finalList);
        }*/
    }

    private boolean runServer(CrashReproducer reproducer, List<ModData> mods) {
        Process serverProcess = null;
        ScheduledFuture<?> fut = null;
        try {
            this.timedOut = false;
            fut = scheduler.schedule(() -> {
                ModNarrower.this.timedOut = true;
            }, reproducer.timeout, TimeUnit.MILLISECONDS);

            serverProcess = new ProcessBuilder(reproducer.serverCommand.split(" "))
                    .directory(reproducer.serverDir)
                    .redirectErrorStream(true)
                    .start();

            List<Strategy> strategies = Lists.newArrayList(new StdoutStrategy(new SimpleStringFinder("cyclopscore", true)));

            for (Strategy strategy: strategies) {
                strategy.prepare(reproducer, serverProcess);
            }

            while (!timedOut) {
                for (Strategy strategy: strategies) {
                    if (strategy.detectError()) {
                        System.err.println("Stopping server process!");
                        return false;
                    }
                }
            }
            return true;

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (serverProcess != null) {
                serverProcess.destroy();
            }
            if (fut != null && !fut.isCancelled() && !fut.isDone() && !fut.cancel(true)) {
                throw new IllegalStateException("Unable to cancel future!");
            }

        }
    }

    // Mods are sorted such that a mod comes before all of its dependencies.
    private List<ModData> splitMods(List<ModData> mods) {
        if (mods.size() == 1) {
            return Lists.newArrayList();
        }
        return mods.subList(mods.size() / 2, mods.size());
    }

    private List<ModData> reAddMods(List<ModData> lastMods, List<ModData> currentMods) {
        // Add one element to list (move back one position towards the start)
        int startIndex = (lastMods.size() - currentMods.size()) - 1;
        return lastMods.subList(startIndex, lastMods.size());
    }

    private void updateMods(List<ModData> allMods, List<ModData> enabledMods) {
        allMods.forEach(m -> m.setEnabled(enabledMods.contains(m)));
    }

}
