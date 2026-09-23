package com.bleedthrough.meatscape.bioindustry.neural;

import com.bleedthrough.meatscape.core.registry.MeatscapeBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Local, switchable neural component. Only the terminal emits vanilla redstone. */
public final class NeuralBlock extends BaseEntityBlock {
    public enum Role { SENSOR, FIBER, ACTUATOR }

    public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 15);
    private final Role role;

    public NeuralBlock(Role role, Properties properties) {
        super(properties);
        this.role = role;
        registerDefaultState(stateDefinition.any().setValue(ENABLED, true).setValue(POWER, 0));
    }

    public Role role() { return role; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ENABLED, POWER);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override @Nullable public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        BlockEntityType<NeuralNodeBlockEntity> type = switch (role) {
            case SENSOR -> MeatscapeBlockEntities.NEURAL_SENSOR.get();
            case FIBER -> MeatscapeBlockEntities.NEURAL_FIBER.get();
            case ACTUATOR -> MeatscapeBlockEntities.NEURAL_ACTUATOR.get();
        };
        return new NeuralNodeBlockEntity(type, pos, state);
    }

    @Override @Nullable public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        BlockEntityType<NeuralNodeBlockEntity> expected = switch (role) {
            case SENSOR -> MeatscapeBlockEntities.NEURAL_SENSOR.get();
            case FIBER -> MeatscapeBlockEntities.NEURAL_FIBER.get();
            case ACTUATOR -> MeatscapeBlockEntities.NEURAL_ACTUATOR.get();
        };
        return createTickerHelper(type, expected, NeuralNodeBlockEntity::serverTick);
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            boolean enabled = !state.getValue(ENABLED);
            level.setBlockAndUpdate(pos, state.setValue(ENABLED, enabled).setValue(POWER, enabled ? state.getValue(POWER) : 0));
            NeuralUpdates.enqueue((ServerLevel) level, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (level instanceof ServerLevel server && oldState.getBlock() != this) NeuralUpdates.enqueue(server, pos);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (level instanceof ServerLevel server && newState.getBlock() != this) NeuralUpdates.enqueueNeighbors(server, pos);
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moving);
        if (level instanceof ServerLevel server) NeuralUpdates.enqueue(server, pos);
    }

    @Override public boolean isSignalSource(BlockState state) { return role == Role.ACTUATOR; }

    @Override public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return role == Role.ACTUATOR ? state.getValue(POWER) : 0;
    }
}
