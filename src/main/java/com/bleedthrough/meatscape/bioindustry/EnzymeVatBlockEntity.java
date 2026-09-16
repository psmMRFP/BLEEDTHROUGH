package com.bleedthrough.meatscape.bioindustry;

import com.bleedthrough.meatscape.core.registry.MeatscapeBlockEntities;
import com.bleedthrough.meatscape.core.registry.MeatscapeItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class EnzymeVatBlockEntity extends BlockEntity {
    public static final int PROCESS_TICKS = 100; private int tissue, catalyst, output, progress;
    public EnzymeVatBlockEntity(BlockPos pos, BlockState state) { super(MeatscapeBlockEntities.ENZYME_VAT.get(), pos, state); }
    public boolean addTissue() { if (tissue >= 16) return false; tissue++; setChanged(); return true; }
    public boolean addCatalyst() { if (catalyst >= 1) return false; catalyst++; setChanged(); return true; }
    public int output() { return output; }
    public int tissue() { return tissue; }
    public int catalyst() { return catalyst; }
    public int progress() { return progress; }
    public void takeOutput(Player player) {
        if (output <= 0) return;
        ItemStack result = new ItemStack(MeatscapeItems.NUTRIENT_PASTE.get(), output);
        if (!player.getInventory().add(result)) player.drop(result, false);
        output = 0;
        setChanged();
    }
    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, EnzymeVatBlockEntity vat) { if (vat.tissue <= 0 || vat.catalyst <= 0 || vat.output >= 16) return; if (++vat.progress >= PROCESS_TICKS) { vat.progress = 0; vat.tissue--; vat.output++; } vat.setChanged(); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("Tissue", tissue); tag.putInt("Catalyst", catalyst); tag.putInt("Output", output); tag.putInt("Progress", progress); }
    @Override public void load(CompoundTag tag) { super.load(tag); tissue=Math.max(0,Math.min(16,tag.getInt("Tissue"))); catalyst=Math.max(0,Math.min(1,tag.getInt("Catalyst"))); output=Math.max(0,Math.min(16,tag.getInt("Output"))); progress=Math.max(0,Math.min(PROCESS_TICKS-1,tag.getInt("Progress"))); }
}
