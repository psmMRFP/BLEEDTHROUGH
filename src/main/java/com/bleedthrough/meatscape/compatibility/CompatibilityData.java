package com.bleedthrough.meatscape.compatibility;

import net.minecraft.nbt.CompoundTag;

/** Persistent but bounded player adaptation; temporary effects remain independently reversible. */
public final class CompatibilityData {
    public static final int DATA_VERSION = 1;
    public static final int MAX = 100;
    private int value;
    private final Runnable dirty;
    public CompatibilityData(Runnable dirty) { this.dirty = dirty; }
    public int value() { return value; }
    public boolean add(int amount) { int next = (int) Math.max(0L, Math.min(MAX, (long) value + amount)); if (next == value) return false; value = next; dirty.run(); return true; }
    public CompoundTag save() { CompoundTag tag = new CompoundTag(); tag.putInt("DataVersion", DATA_VERSION); tag.putInt("Compatibility", value); return tag; }
    public void load(CompoundTag tag) { value = Math.max(0, Math.min(MAX, tag.getInt("Compatibility"))); }
    public void copyFrom(CompatibilityData other) { value = other.value; dirty.run(); }
}
