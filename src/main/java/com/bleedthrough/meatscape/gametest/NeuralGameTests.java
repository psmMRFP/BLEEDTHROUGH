package com.bleedthrough.meatscape.gametest;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.bioindustry.ArteryBlockEntity;
import com.bleedthrough.meatscape.bioindustry.neural.NeuralBlock;
import com.bleedthrough.meatscape.bioindustry.neural.NeuralUpdates;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Meatscape.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NeuralGameTests {
    private NeuralGameTests() { }

    @GameTest(template = "empty", batch = "phase810Neural", timeoutTicks = 80)
    public static void serverTickerPropagatesWithoutManualProcessing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos source = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos sensor = source.east();
        BlockPos fiber = sensor.east();
        BlockPos terminal = fiber.east();
        level.setBlockAndUpdate(source, MeatscapeBlocks.ARTERY.get().defaultBlockState());
        level.setBlockAndUpdate(sensor, MeatscapeBlocks.NEURAL_SENSOR.get().defaultBlockState());
        level.setBlockAndUpdate(fiber, MeatscapeBlocks.NEURAL_FIBER.get().defaultBlockState());
        level.setBlockAndUpdate(terminal, MeatscapeBlocks.NEURAL_ACTUATOR.get().defaultBlockState());
        ((ArteryBlockEntity) level.getBlockEntity(source)).addHematic(150);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(power(level, terminal) == 14, "Server ticker did not propagate the source signal");
            level.setBlockAndUpdate(source, Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(power(level, terminal) == 0, "Server ticker retained a signal after source removal");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", batch = "phase810Neural")
    public static void sensorFiberActuatorSwitchAndLoop(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos source = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos sensor = source.east();
        BlockPos first = sensor.east();
        BlockPos second = first.east();
        BlockPos terminal = second.east();
        BlockPos loop = first.south();
        BlockPos loopEnd = second.south();
        level.setBlockAndUpdate(source, MeatscapeBlocks.ARTERY.get().defaultBlockState());
        level.setBlockAndUpdate(sensor, MeatscapeBlocks.NEURAL_SENSOR.get().defaultBlockState());
        level.setBlockAndUpdate(first, MeatscapeBlocks.NEURAL_FIBER.get().defaultBlockState());
        level.setBlockAndUpdate(second, MeatscapeBlocks.NEURAL_FIBER.get().defaultBlockState());
        level.setBlockAndUpdate(loop, MeatscapeBlocks.NEURAL_FIBER.get().defaultBlockState());
        level.setBlockAndUpdate(loopEnd, MeatscapeBlocks.NEURAL_FIBER.get().defaultBlockState());
        level.setBlockAndUpdate(terminal, MeatscapeBlocks.NEURAL_ACTUATOR.get().defaultBlockState());
        ((ArteryBlockEntity) level.getBlockEntity(source)).addHematic(150);
        NeuralUpdates.enqueue(level, sensor);
        helper.assertTrue(NeuralUpdates.process(level, 1) == 1, "Budget was exceeded");
        settle(level);
        helper.assertTrue(power(level, sensor) == 15 && power(level, first) == 14 && power(level, second) == 13,
                "Signal did not decay one step per fiber");
        helper.assertTrue(power(level, terminal) == 13
                && MeatscapeBlocks.NEURAL_ACTUATOR.get().getSignal(level.getBlockState(terminal), level, terminal, Direction.UP) == 13,
                "Terminal did not output the transmitted redstone level");

        var player = helper.makeMockPlayer();
        var hit = new BlockHitResult(Vec3.atCenterOf(first), Direction.UP, first, false);
        MeatscapeBlocks.NEURAL_FIBER.get().use(level.getBlockState(first), level, first, player, InteractionHand.MAIN_HAND, hit);
        settle(level);
        helper.assertTrue(!level.getBlockState(first).getValue(NeuralBlock.ENABLED) && power(level, terminal) == 0,
                "Switch did not cut the circuit");
        MeatscapeBlocks.NEURAL_FIBER.get().use(level.getBlockState(first), level, first, player, InteractionHand.MAIN_HAND, hit);
        settle(level);
        helper.assertTrue(power(level, terminal) == 13, "Switch did not restore the circuit");

        level.setBlockAndUpdate(source, Blocks.AIR.defaultBlockState());
        NeuralUpdates.enqueue(level, sensor);
        settle(level);
        helper.assertTrue(power(level, sensor) == 0 && power(level, first) == 0 && power(level, second) == 0
                && power(level, loop) == 0 && power(level, loopEnd) == 0 && power(level, terminal) == 0,
                "Disconnected feedback loop retained a signal");
        helper.succeed();
    }

    private static int power(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getValue(NeuralBlock.POWER);
    }

    private static void settle(ServerLevel level) {
        for (int i = 0; i < 20; i++) {
            if (NeuralUpdates.process(level, 64) == 0) return;
        }
    }
}
