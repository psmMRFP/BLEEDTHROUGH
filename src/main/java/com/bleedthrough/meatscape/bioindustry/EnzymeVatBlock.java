package com.bleedthrough.meatscape.bioindustry;

import com.bleedthrough.meatscape.core.registry.MeatscapeBlockEntities;
import com.bleedthrough.meatscape.core.registry.MeatscapeItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Small Core-only enzymatic processor: tissue is reclaimed while collagen remains a catalyst. */
public final class EnzymeVatBlock extends BaseEntityBlock {
    public EnzymeVatBlock(Properties properties) { super(properties); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override @Nullable public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new EnzymeVatBlockEntity(pos, state); }
    @Override @Nullable public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return level.isClientSide ? null : createTickerHelper(type, MeatscapeBlockEntities.ENZYME_VAT.get(), EnzymeVatBlockEntity::serverTick); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof EnzymeVatBlockEntity vat)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (!level.isClientSide && held.is(MeatscapeItems.COLLAGEN.get()) && vat.addCatalyst()) { if (!player.getAbilities().instabuild) held.shrink(1); return InteractionResult.CONSUME; }
        if (!level.isClientSide && held.is(MeatscapeItems.RAW_TISSUE.get()) && vat.addTissue()) { if (!player.getAbilities().instabuild) held.shrink(1); return InteractionResult.CONSUME; }
        if (!level.isClientSide && held.isEmpty()) vat.takeOutput(player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
