package com.bleedthrough.meatscape.debug;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.evolution.EvolutionSchedulerEvents;
import com.bleedthrough.meatscape.core.config.MeatscapeConfig;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

/** Opt-in diagnostics; path-only world scans never run on the server tick. */
@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID)
public final class SoakTelemetry {
    static final String HEADER = "timestamp,session_id,build_commit,started_at,server_tick,window_ticks,"
            + "mean_event_tick_ms,peak_event_tick_ms,heap_used_bytes,heap_max_bytes,world_bytes,"
            + "telemetry_bytes,gc_count_delta,gc_time_ms_delta,loaded_chunks,loaded_entities,rift_count,"
            + "queue_length,rollback_jobs,global_budget,per_rift_budget,forward_total,rollback_total,"
            + "forward_peak,rollback_peak,budget_violations,per_rift_violations,per_rift_work,overflow_rift_work";
    private static final long ROTATE_BYTES = 8L * 1024 * 1024;
    private static final Map<MinecraftServer, State> STATES = new IdentityHashMap<>();

    private SoakTelemetry() { }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) { close(event.getServer()); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void serverTick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = event.getServer();
        if (!MeatscapeConfig.SOAK_TELEMETRY_ENABLED.get()) {
            close(server);
            return;
        }
        State state = STATES.computeIfAbsent(server, SoakTelemetry::start);
        if (event.phase == TickEvent.Phase.START) {
            state.tickStarted = System.nanoTime();
            return;
        }
        if (state.tickStarted == 0) return;
        double elapsedMs = (System.nanoTime() - state.tickStarted) / 1_000_000.0;
        state.tickStarted = 0;
        var evolution = EvolutionSchedulerEvents.get(server);
        var rollback = EvolutionSchedulerEvents.getRollback(server).stats();
        int globalBudget = MeatscapeConfig.EVOLUTION_GLOBAL_BUDGET.get();
        int perRiftBudget = MeatscapeConfig.EVOLUTION_PER_RIFT_BUDGET.get();
        state.window.add(elapsedMs, evolution.stats().lastTickProcessed(), rollback.processed(),
                globalBudget, perRiftBudget, evolution.lastPerRift());
        if (state.window.ticks() >= MeatscapeConfig.SOAK_TELEMETRY_INTERVAL_TICKS.get()) {
            append(server, state, state.window.snapshotAndReset(), globalBudget, perRiftBudget);
        }
    }

    private static State start(MinecraftServer server) {
        Path root = server.getWorldPath(LevelResource.ROOT);
        State state = new State(root);
        try { ensureHeader(state.output); }
        catch (IOException exception) { Meatscape.LOGGER.warn("Unable to initialize soak telemetry at {}", state.output, exception); }
        return state;
    }

    private static void close(MinecraftServer server) {
        State state = STATES.remove(server);
        if (state != null) state.scanner.shutdownNow();
    }

    private static void append(MinecraftServer server, State state, SoakWindow.Snapshot window,
                               int globalBudget, int perRiftBudget) {
        if (state.scan != null && state.scan.isDone()) {
            state.worldBytes = state.scan.getNow(-1L);
            state.scan = null;
        }
        if (state.scan == null) state.scan = CompletableFuture.supplyAsync(() -> directorySize(state.root), state.scanner);
        var data = MeatscapeWorldData.get(server);
        Runtime runtime = Runtime.getRuntime();
        long[] gc = gcTotals();
        long gcCount = gc[0] < 0 || state.gcCount < 0 ? -1 : gc[0] - state.gcCount;
        long gcTime = gc[1] < 0 || state.gcTime < 0 ? -1 : gc[1] - state.gcTime;
        state.gcCount = gc[0]; state.gcTime = gc[1];
        int chunks = 0, entities = 0;
        for (var level : server.getAllLevels()) {
            chunks += level.getChunkSource().getLoadedChunksCount();
            for (var ignored : level.getAllEntities()) entities++;
        }
        var columns = new java.util.ArrayList<String>();
        java.util.Collections.addAll(columns, Instant.now().toString(), state.id.toString(),
                System.getProperty("meatscape.buildCommit", "unknown"), state.startedAt,
                Integer.toString(server.getTickCount()), Integer.toString(window.ticks()),
                Double.toString(window.meanMspt()), Double.toString(window.peakMspt()),
                Long.toString(runtime.totalMemory() - runtime.freeMemory()), Long.toString(runtime.maxMemory()),
                missing(state.worldBytes), missing(fileSize(state.output)), missing(gcCount), missing(gcTime),
                Integer.toString(chunks), Integer.toString(entities), Integer.toString(data.rifts().size()),
                Integer.toString(EvolutionSchedulerEvents.get(server).stats().queueLength()),
                Integer.toString(data.rollbackJobs().size()), Integer.toString(globalBudget),
                Integer.toString(perRiftBudget), Long.toString(window.forwardTotal()),
                Long.toString(window.rollbackTotal()), Integer.toString(window.forwardPeak()),
                Integer.toString(window.rollbackPeak()), Integer.toString(window.budgetViolations()),
                Integer.toString(window.perRiftViolations()), window.perRiftCsv(),
                Long.toString(window.overflowRiftWork()));
        try {
            state.output = rotateIfNeeded(state.output, state.id);
            ensureHeader(state.output);
            Files.writeString(state.output, String.join(",", columns) + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            state.writeFailed = false;
        } catch (IOException exception) {
            if (!state.writeFailed) Meatscape.LOGGER.warn("Unable to append soak telemetry at {}", state.output, exception);
            state.writeFailed = true;
        }
    }

    static Path rotateIfNeeded(Path path, UUID session) throws IOException {
        if (Files.exists(path) && Files.size(path) >= ROTATE_BYTES) {
            return path.resolveSibling("meatscape-soak-" + Instant.now().toEpochMilli() + "-" + session + ".csv");
        }
        return path;
    }

    static void ensureHeader(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        if (Files.exists(path) && Files.size(path) > 0) {
            try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                if (HEADER.equals(reader.readLine())) return;
            }
            Path legacy = path.resolveSibling("meatscape-soak-legacy-" + Instant.now().toEpochMilli()
                    + "-" + UUID.randomUUID() + ".csv");
            Files.move(path, legacy);
            Meatscape.LOGGER.warn("Preserved incompatible soak CSV at {} before writing new schema", legacy);
        }
        if (Files.notExists(path) || Files.size(path) == 0) {
            Files.writeString(path, HEADER + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    /** -1 means unavailable; excludes telemetry CSVs so diagnostics cannot inflate game-world size. */
    static long directorySize(Path root) {
        try (var files = Files.walk(root)) {
            return files.filter(Files::isRegularFile).filter(path -> !path.getFileName().toString().startsWith("meatscape-soak"))
                    .mapToLong(path -> {
                        try { return Files.size(path); }
                        catch (IOException exception) { throw new UncheckedIOException(exception); }
                    }).sum();
        } catch (IOException | UncheckedIOException exception) {
            Meatscape.LOGGER.warn("Unable to measure soak world size at {}", root, exception);
            return -1;
        }
    }

    private static long fileSize(Path path) {
        try { return Files.size(path); }
        catch (IOException exception) { return -1; }
    }

    private static String missing(long value) { return value < 0 ? "" : Long.toString(value); }

    private static long[] gcTotals() {
        long count = 0, time = 0;
        for (var bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (bean.getCollectionCount() < 0 || bean.getCollectionTime() < 0) return new long[] {-1, -1};
            count += bean.getCollectionCount(); time += bean.getCollectionTime();
        }
        return new long[] {count, time};
    }

    private static final class State {
        final Path root;
        final UUID id = UUID.randomUUID();
        final String startedAt = Instant.now().toString();
        final SoakWindow window = new SoakWindow();
        final ExecutorService scanner = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "meatscape-soak-world-size");
            thread.setDaemon(true);
            return thread;
        });
        Path output;
        CompletableFuture<Long> scan;
        long tickStarted;
        long worldBytes = -1;
        long gcCount = -1;
        long gcTime = -1;
        boolean writeFailed;

        State(Path root) { this.root = root; this.output = root.resolve("data/meatscape-soak.csv"); }
    }
}
