package com.bleedthrough.meatscape.debug;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Bounded, resettable counters; per-Rift values belong only to this sample window. */
final class SoakWindow {
    static final int MAX_RIFTS = 64;
    private final Map<UUID, Long> perRift = new HashMap<>();
    private int ticks;
    private double totalMspt;
    private double peakMspt;
    private long forwardTotal;
    private long rollbackTotal;
    private int forwardPeak;
    private int rollbackPeak;
    private int budgetViolations;
    private int perRiftViolations;
    private long overflowRiftWork;

    void add(double mspt, int forward, int rollback, int globalBudget, int perRiftBudget,
             Map<UUID, Integer> perRiftTick) {
        ticks++;
        totalMspt += mspt;
        peakMspt = Math.max(peakMspt, mspt);
        forwardTotal += forward;
        rollbackTotal += rollback;
        forwardPeak = Math.max(forwardPeak, forward);
        rollbackPeak = Math.max(rollbackPeak, rollback);
        if (forward + rollback > globalBudget) budgetViolations++;
        for (var entry : perRiftTick.entrySet()) {
            if (entry.getValue() > perRiftBudget) perRiftViolations++;
            if (perRift.containsKey(entry.getKey()) || perRift.size() < MAX_RIFTS) {
                perRift.merge(entry.getKey(), entry.getValue().longValue(), Long::sum);
            } else {
                overflowRiftWork += entry.getValue();
            }
        }
    }

    Snapshot snapshotAndReset() {
        Snapshot result = new Snapshot(ticks, ticks == 0 ? 0 : totalMspt / ticks, peakMspt,
                forwardTotal, rollbackTotal, forwardPeak, rollbackPeak, budgetViolations,
                perRiftViolations, Map.copyOf(perRift), overflowRiftWork);
        ticks = 0;
        totalMspt = peakMspt = 0;
        forwardTotal = rollbackTotal = overflowRiftWork = 0;
        forwardPeak = rollbackPeak = budgetViolations = perRiftViolations = 0;
        perRift.clear();
        return result;
    }

    int ticks() { return ticks; }

    record Snapshot(int ticks, double meanMspt, double peakMspt, long forwardTotal, long rollbackTotal,
                    int forwardPeak, int rollbackPeak, int budgetViolations, int perRiftViolations,
                    Map<UUID, Long> perRift, long overflowRiftWork) {
        String perRiftCsv() {
            return perRift.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(java.util.stream.Collectors.joining(";"));
        }
    }
}
