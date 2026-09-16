package com.bleedthrough.meatscape.compatibility;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
public final class CompatibilityProvider implements ICapabilitySerializable<CompoundTag> {
    private final CompatibilityData data = new CompatibilityData(() -> { }); private final LazyOptional<CompatibilityData> optional = LazyOptional.of(() -> data);
    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) { return CompatibilityCapability.INSTANCE.orEmpty(capability, optional); }
    @Override public CompoundTag serializeNBT() { return data.save(); }
    @Override public void deserializeNBT(CompoundTag tag) { data.load(tag); }
    public void invalidate() { optional.invalidate(); }
}
