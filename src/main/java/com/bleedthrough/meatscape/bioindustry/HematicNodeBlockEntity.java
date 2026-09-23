package com.bleedthrough.meatscape.bioindustry;

import com.bleedthrough.meatscape.core.registry.MeatscapeBlockEntities;
import com.bleedthrough.meatscape.architecture.RegenerativeMembraneBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

abstract class HematicNodeBlockEntity extends BlockEntity {
    static final int DATA_VERSION = 1;
    static final int TRANSFER_PER_TICK = 50;
    protected final HematicVolume hematic;
    HematicNodeBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state); hematic = new HematicVolume(capacity);
    }
    public int hematic() { return hematic.amount(); }
    public int capacity() { return hematic.capacity(); }
    public int addHematic(int amount) { int accepted = hematic.fill(amount); if (accepted > 0) setChanged(); return accepted; }
    protected int transferForward(net.minecraft.core.Direction direction) {
        if (level == null) return 0;
        BlockEntity target = level.getBlockEntity(worldPosition.relative(direction));
        if (target instanceof HematicNodeBlockEntity node) {
            int moved = HematicVolume.transfer(hematic, node.hematic, TRANSFER_PER_TICK);
            if (moved > 0) { setChanged(); node.setChanged(); }
            return moved;
        }
        if (target instanceof RegenerativeMembraneBlockEntity membrane) {
            int moved = membrane.addHematic(Math.min(TRANSFER_PER_TICK, hematic.amount()));
            if (moved > 0) { hematic.drain(moved); setChanged(); }
            return moved;
        }
        return 0;
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("DataVersion", DATA_VERSION); tag.putInt("Hematic", hematic.amount()); }
    @Override public void load(CompoundTag tag) { super.load(tag); hematic.load(tag.getInt("Hematic")); }
}
