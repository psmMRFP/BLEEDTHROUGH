package com.bleedthrough.meatscape.compatibility;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
class CompatibilityDataTest {
    @Test void compatibilityIsBoundedPersistentAndPlayerOwned() {
        AtomicInteger dirty = new AtomicInteger(); CompatibilityData first = new CompatibilityData(dirty::incrementAndGet);
        assertTrue(first.add(120)); assertEquals(100, first.value()); assertFalse(first.add(1));
        CompatibilityData loaded = new CompatibilityData(() -> { }); loaded.load(first.save()); assertEquals(100, loaded.value());
        CompatibilityData clone = new CompatibilityData(() -> { }); clone.copyFrom(loaded); CompatibilityData other = new CompatibilityData(() -> { });
        assertEquals(100, clone.value()); assertEquals(0, other.value()); assertEquals(1, dirty.get());
    }
    @Test void unversionedCompatibilityMigratesAndOverflowStaysBounded() {
        CompatibilityData data = new CompatibilityData(() -> { });
        net.minecraft.nbt.CompoundTag old = new net.minecraft.nbt.CompoundTag(); old.putInt("Compatibility", 73);
        data.load(old); assertEquals(73, data.value());
        assertTrue(data.add(Integer.MAX_VALUE)); assertEquals(100, data.value());
        assertEquals(CompatibilityData.DATA_VERSION, data.save().getInt("DataVersion"));
        old.putInt("Compatibility", -50); data.load(old); assertEquals(0, data.value());
    }
}
