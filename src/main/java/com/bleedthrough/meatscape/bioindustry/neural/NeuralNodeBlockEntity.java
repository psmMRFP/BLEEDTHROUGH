package com.bleedthrough.meatscape.bioindustry.neural;

import com.bleedthrough.meatscape.core.registry.MeatscapeBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Periodic reconciliation also repairs block states after chunk reload or missed neighbors. */
public final class NeuralNodeBlockEntity extends BlockEntity {
    private boolean firstTick = true;

    public NeuralNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static NeuralNodeBlockEntity sensor(BlockPos pos, BlockState state) {
        return new NeuralNodeBlockEntity(MeatscapeBlockEntities.NEURAL_SENSOR.get(), pos, state);
    }

    public static NeuralNodeBlockEntity fiber(BlockPos pos, BlockState state) {
        return new NeuralNodeBlockEntity(MeatscapeBlockEntities.NEURAL_FIBER.get(), pos, state);
    }

    public static NeuralNodeBlockEntity actuator(BlockPos pos, BlockState state) {
        return new NeuralNodeBlockEntity(MeatscapeBlockEntities.NEURAL_ACTUATOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NeuralNodeBlockEntity node) {
        if (!(level instanceof ServerLevel server)) return;
        if (node.firstTick || Math.floorMod(level.getGameTime() + pos.asLong(), 20) == 0) {
            node.firstTick = false;
            NeuralUpdates.enqueue(server, pos);
        }
    }
}
