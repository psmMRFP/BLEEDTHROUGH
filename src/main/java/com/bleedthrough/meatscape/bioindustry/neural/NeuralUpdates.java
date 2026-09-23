package com.bleedthrough.meatscape.bioindustry.neural;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.bioindustry.ArteryBlockEntity;
import com.bleedthrough.meatscape.bioindustry.HeartPumpBlockEntity;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One bounded queue per level; entries are positions, never live chunk or node references. */
@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NeuralUpdates {
    public static final int MAX_UPDATES_PER_LEVEL_TICK = 64;
    private static final Map<ServerLevel, Pending> PENDING = new WeakHashMap<>();

    private NeuralUpdates() { }

    public static void enqueue(ServerLevel level, BlockPos pos) {
        Pending pending = PENDING.computeIfAbsent(level, ignored -> new Pending());
        BlockPos immutable = pos.immutable();
        if (pending.unique.add(immutable)) pending.queue.addLast(immutable);
    }

    public static void enqueueNeighbors(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) enqueue(level, pos.relative(direction));
    }

    @SubscribeEvent public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        process(level, MAX_UPDATES_PER_LEVEL_TICK);
    }

    @SubscribeEvent public static void levelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) PENDING.remove(level);
    }

    public static int process(ServerLevel level, int budget) {
        Pending pending = PENDING.get(level);
        if (pending == null) return 0;
        int processed = 0;
        while (processed < budget && !pending.queue.isEmpty()) {
            BlockPos pos = pending.queue.removeFirst();
            pending.unique.remove(pos);
            processed++;
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof NeuralBlock block)) continue;
            int power = desiredPower(level, pos, state, block.role());
            if (power != state.getValue(NeuralBlock.POWER)) {
                level.setBlock(pos, state.setValue(NeuralBlock.POWER, power), Block.UPDATE_ALL);
                enqueueNeighbors(level, pos);
            }
        }
        if (pending.queue.isEmpty()) PENDING.remove(level);
        return processed;
    }

    private static int desiredPower(ServerLevel level, BlockPos pos, BlockState state, NeuralBlock.Role role) {
        if (!state.getValue(NeuralBlock.ENABLED)) return 0;
        int strongest = 0;
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = pos.relative(direction);
            if (!level.hasChunkAt(adjacent)) continue;
            if (role == NeuralBlock.Role.SENSOR) {
                var node = level.getBlockEntity(adjacent);
                int hematic = node instanceof HeartPumpBlockEntity pump ? pump.hematic()
                        : node instanceof ArteryBlockEntity artery ? artery.hematic() : 0;
                if (hematic >= 100) return 15;
            } else {
                BlockState other = level.getBlockState(adjacent);
                if (!(other.getBlock() instanceof NeuralBlock otherBlock) || !other.getValue(NeuralBlock.ENABLED)) continue;
                if (role == NeuralBlock.Role.FIBER && (otherBlock.role() == NeuralBlock.Role.SENSOR
                        || otherBlock.role() == NeuralBlock.Role.FIBER)) {
                    strongest = Math.max(strongest, other.getValue(NeuralBlock.POWER) - 1);
                } else if (role == NeuralBlock.Role.ACTUATOR && otherBlock.role() == NeuralBlock.Role.FIBER) {
                    strongest = Math.max(strongest, other.getValue(NeuralBlock.POWER));
                }
            }
        }
        return Math.max(0, strongest);
    }

    private static final class Pending {
        private final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        private final HashSet<BlockPos> unique = new HashSet<>();
    }
}
