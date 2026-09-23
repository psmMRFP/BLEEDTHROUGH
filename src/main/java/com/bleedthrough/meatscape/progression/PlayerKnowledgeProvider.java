package com.bleedthrough.meatscape.progression;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class PlayerKnowledgeProvider implements ICapabilitySerializable<CompoundTag> {
    private final PlayerKnowledgeData data = new PlayerKnowledgeData(this::setDirty);
    private final LazyOptional<PlayerKnowledgeData> optional = LazyOptional.of(() -> data);
    private boolean dirty;
    PlayerKnowledgeData data() { return data; }
    private void setDirty() { dirty = true; }
    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) { return PlayerKnowledgeCapability.INSTANCE.orEmpty(cap, optional); }
    @Override public CompoundTag serializeNBT() { dirty = false; return data.save(); }
    @Override public void deserializeNBT(CompoundTag tag) { data.load(tag); dirty = false; }
    void invalidate() { optional.invalidate(); }
}
