package com.bleedthrough.meatscape.core.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Common configuration owned by the core mod. */
public final class MeatscapeConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Enable additional Meatscape diagnostic logging.")
            .define("debugLogging", false);

    public static final ForgeConfigSpec.IntValue EVOLUTION_GLOBAL_BUDGET = BUILDER
            .comment("Maximum candidate positions selected by the Evolution Scheduler per server tick.")
            .defineInRange("evolution.globalTickBudget", 64, 1, 4096);

    public static final ForgeConfigSpec.IntValue EVOLUTION_PER_RIFT_BUDGET = BUILDER
            .comment("Maximum candidate positions selected for one Rift per server tick.")
            .defineInRange("evolution.perRiftTickBudget", 8, 1, 1024);

    public static final ForgeConfigSpec.BooleanValue CREATE_BULK_MOVEMENT_SAFETY = BUILDER
            .comment("Enable the dependency-free provenance hook intended for optional Create moving structures.")
            .define("integrations.createBulkMovementSafety", true);

    public static final ForgeConfigSpec.BooleanValue BLEEDING_ENABLED = BUILDER
            .comment("Enable the first Nether-return Bleeding event; disabling also freezes a pending event.")
            .define("progression.bleedingEnabled", true);

    public static final ForgeConfigSpec.IntValue BLEEDING_DELAY = BUILDER
            .comment("Observed server ticks before the first Bleeding event. Pause or absent local observers freeze it.")
            .defineInRange("progression.bleedingDelayTicks", 600, 0, 72000);

    public static final ForgeConfigSpec.BooleanValue SOAK_TELEMETRY_ENABLED = BUILDER
            .comment("Write bounded periodic dedicated-server soak samples to the world data directory.")
            .define("diagnostics.soakTelemetryEnabled", false);

    public static final ForgeConfigSpec.IntValue SOAK_TELEMETRY_INTERVAL_TICKS = BUILDER
            .comment("Server ticks per bounded soak window. World-size scans run in a background path-only worker.")
            .defineInRange("diagnostics.soakTelemetryIntervalTicks", 1200, 20, 72000);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private MeatscapeConfig() {
    }
}
