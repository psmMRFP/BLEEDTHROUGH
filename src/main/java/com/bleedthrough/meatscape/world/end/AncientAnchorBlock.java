package com.bleedthrough.meatscape.world.end;

import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import com.bleedthrough.meatscape.progression.EndRevelationEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/** A local pointer to its nearby aperture; no global structure index or long-lived link. */
public final class AncientAnchorBlock extends HorizontalDirectionalBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public AncientAnchorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        BlockPos aperture = pos.relative(state.getValue(FACING), 2);
        boolean present = server.hasChunkAt(aperture) && server.getBlockState(aperture).is(MeatscapeBlocks.END_WORMHOLE.get());
        if (present) {
            EndRevelationEvents.anchorObserved(serverPlayer);
            serverPlayer.displayClientMessage(Component.translatable("message.meatscape.anchor_found", aperture.getX(),
                    aperture.getY(), aperture.getZ()), false);
        } else {
            serverPlayer.displayClientMessage(Component.translatable("message.meatscape.anchor_missing"), false);
        }
        return InteractionResult.CONSUME;
    }
}
