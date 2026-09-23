package com.bleedthrough.meatscape.coherence.evolution;

import com.bleedthrough.meatscape.coherence.rift.DimensionChunkKey;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Bounded round-robin scheduler. It records candidates but never mutates the world. */
public final class EvolutionScheduler {
    private final int globalBudget;
    private final int perRiftBudget;
    private final ArrayDeque<EvolutionTask> queue = new ArrayDeque<>();
    private final Set<EvolutionTask> queued = new HashSet<>();
    private final EnumMap<EvolutionSkipReason, Long> skipped = new EnumMap<>(EvolutionSkipReason.class);
    private List<EvolutionCandidate> lastCandidates = List.of();
    private Map<UUID, Integer> lastPerRift = Map.of();
    private long ticks;
    private long totalProcessed;
    private long lastTickNanos;

    public EvolutionScheduler(int globalBudget, int perRiftBudget) {
        if (globalBudget < 1 || perRiftBudget < 1) {
            throw new IllegalArgumentException("Evolution budgets must be positive");
        }
        this.globalBudget = globalBudget;
        this.perRiftBudget = perRiftBudget;
    }

    public boolean enqueue(EvolutionTask task) {
        if (!queued.add(task)) {
            return false;
        }
        queue.addLast(task);
        return true;
    }

    public int removeChunk(DimensionChunkKey chunk) {
        int before = queue.size();
        queue.removeIf(task -> task.chunk().equals(chunk));
        queued.removeIf(task -> task.chunk().equals(chunk));
        return before - queue.size();
    }

    public void clear() {
        queue.clear();
        queued.clear();
        lastCandidates = List.of();
        lastPerRift = Map.of();
    }

    public List<EvolutionCandidate> tick(boolean paused, long gameTime, EvolutionEnvironment environment) {
        return tick(paused, gameTime, environment, globalBudget);
    }

    public List<EvolutionCandidate> tick(
            boolean paused, long gameTime, EvolutionEnvironment environment, int availableBudget) {
        long started = System.nanoTime();
        ticks++;
        if (paused) {
            skip(EvolutionSkipReason.PAUSED);
            lastCandidates = List.of();
            lastPerRift = Map.of();
            lastTickNanos = System.nanoTime() - started;
            return lastCandidates;
        }

        int tasksAtStart = queue.size();
        int tickBudget = Math.max(0, Math.min(globalBudget, availableBudget));
        List<EvolutionCandidate> candidates = new ArrayList<>(Math.min(tickBudget, tasksAtStart));
        Map<UUID, Integer> perRift = new HashMap<>();
        for (int visited = 0; visited < tasksAtStart && candidates.size() < tickBudget; visited++) {
            EvolutionTask task = poll();
            if (!environment.riftExists(task.riftId())) {
                skip(EvolutionSkipReason.RIFT_MISSING);
                continue;
            }
            if (!environment.chunkLoaded(task.chunk())) {
                skip(EvolutionSkipReason.CHUNK_UNLOADED);
                continue;
            }
            if (perRift.getOrDefault(task.riftId(), 0) >= perRiftBudget) {
                skip(EvolutionSkipReason.PER_RIFT_BUDGET);
                enqueue(task);
                continue;
            }
            if (environment.coherence(task.chunk()) <= 0) {
                skip(EvolutionSkipReason.ZERO_COHERENCE);
                enqueue(task);
                continue;
            }
            candidates.add(new EvolutionCandidate(
                    task.riftId(), task.chunk(), CandidateSampler.sample(task, gameTime, environment)));
            perRift.merge(task.riftId(), 1, Integer::sum);
            enqueue(task);
        }
        lastCandidates = List.copyOf(candidates);
        lastPerRift = Map.copyOf(perRift);
        totalProcessed += candidates.size();
        lastTickNanos = System.nanoTime() - started;
        return lastCandidates;
    }

    private EvolutionTask poll() {
        EvolutionTask task = queue.removeFirst();
        queued.remove(task);
        return task;
    }

    private void skip(EvolutionSkipReason reason) {
        skipped.merge(reason, 1L, Long::sum);
    }

    public List<EvolutionCandidate> lastCandidates() {
        return lastCandidates;
    }

    /** Counts only the current tick; no stale Rift references are retained. */
    public Map<UUID, Integer> lastPerRift() { return lastPerRift; }

    public EvolutionStats stats() {
        return new EvolutionStats(
                ticks, totalProcessed, lastCandidates.size(), queue.size(), lastTickNanos, skipped);
    }
}
