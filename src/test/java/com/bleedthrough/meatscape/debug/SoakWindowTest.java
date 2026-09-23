package com.bleedthrough.meatscape.debug;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SoakWindowTest {
    @Test void peaksViolationsAndResetAreWindowBounded() {
        var rift = UUID.randomUUID();
        var window = new SoakWindow();
        window.add(12, 4, 2, 8, 4, Map.of(rift, 4));
        window.add(35, 7, 3, 8, 4, Map.of(rift, 7));
        var sample = window.snapshotAndReset();
        assertEquals(2, sample.ticks());
        assertEquals(23.5, sample.meanMspt());
        assertEquals(35, sample.peakMspt());
        assertEquals(11, sample.forwardTotal());
        assertEquals(5, sample.rollbackTotal());
        assertEquals(7, sample.forwardPeak());
        assertEquals(3, sample.rollbackPeak());
        assertEquals(1, sample.budgetViolations());
        assertEquals(1, sample.perRiftViolations());
        assertEquals(11L, sample.perRift().get(rift));
        assertEquals(0, window.snapshotAndReset().ticks());
    }

    @Test void riftCapacityHasOverflowAndDoesNotRetainOldWindow() {
        var work = new HashMap<UUID, Integer>();
        for (int i = 0; i < 70; i++) work.put(UUID.randomUUID(), 1);
        var window = new SoakWindow();
        window.add(1, 70, 0, 100, 8, work);
        var sample = window.snapshotAndReset();
        assertEquals(SoakWindow.MAX_RIFTS, sample.perRift().size());
        assertEquals(6, sample.overflowRiftWork());
        assertTrue(window.snapshotAndReset().perRift().isEmpty());
    }
}
