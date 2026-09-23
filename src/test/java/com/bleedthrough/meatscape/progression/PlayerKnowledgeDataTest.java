package com.bleedthrough.meatscape.progression;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PlayerKnowledgeDataTest {
    @Test void observationsAreIdempotentAndPersistIndependently() {
        AtomicInteger dirty = new AtomicInteger(); PlayerKnowledgeData data = new PlayerKnowledgeData(dirty::incrementAndGet);
        assertTrue(data.observe(KnowledgeObservation.RIFT)); assertFalse(data.observe(KnowledgeObservation.RIFT));
        assertTrue(data.observe(KnowledgeObservation.CAUTERIZATION)); assertEquals(2, dirty.get());
        assertTrue(data.observe(KnowledgeObservation.WORMHOLE)); assertEquals(3, dirty.get());
        PlayerKnowledgeData loaded = new PlayerKnowledgeData(() -> { }); loaded.load(data.save());
        assertTrue(loaded.observed(KnowledgeObservation.RIFT)); assertTrue(loaded.observed(KnowledgeObservation.CAUTERIZATION));
        assertTrue(loaded.observed(KnowledgeObservation.WORMHOLE));
        assertFalse(loaded.observed(KnowledgeObservation.BIOINDUSTRY));
    }
    @Test void unknownAndLegacyDataDoNotInventObservations() {
        PlayerKnowledgeData data = new PlayerKnowledgeData(() -> { });
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        net.minecraft.nbt.ListTag values = new net.minecraft.nbt.ListTag(); values.add(net.minecraft.nbt.StringTag.valueOf("future")); tag.put("Observed", values);
        data.load(tag); assertTrue(data.observations().isEmpty());
    }
    @Test void cloneCopiesOnlyTheSamePlayersKnowledge() {
        PlayerKnowledgeData first = new PlayerKnowledgeData(() -> { }); first.observe(KnowledgeObservation.RIFT);
        PlayerKnowledgeData respawn = new PlayerKnowledgeData(() -> { }); respawn.copyFrom(first);
        PlayerKnowledgeData secondPlayer = new PlayerKnowledgeData(() -> { });
        assertTrue(respawn.observed(KnowledgeObservation.RIFT)); assertTrue(secondPlayer.observations().isEmpty());
    }
    @Test void endRevelationIsOrderIndependentAndOldKnowledgeRemainsIncomplete() {
        PlayerKnowledgeData old = new PlayerKnowledgeData(() -> { });
        old.observe(KnowledgeObservation.WORMHOLE);
        PlayerKnowledgeData loaded = new PlayerKnowledgeData(() -> { });
        loaded.load(old.save());
        assertFalse(PlayerKnowledge.endRevelationResearch(loaded));
        assertTrue(loaded.observe(KnowledgeObservation.ANCIENT_ANCHOR));
        assertTrue(loaded.observe(KnowledgeObservation.CHORUS));
        assertFalse(PlayerKnowledge.endRevelationResearch(loaded));
        assertTrue(loaded.observe(KnowledgeObservation.END_STONE));
        assertTrue(PlayerKnowledge.endRevelationResearch(loaded));
        assertTrue(loaded.observe(KnowledgeObservation.END_REVELATION));
        assertFalse(loaded.observe(KnowledgeObservation.END_REVELATION));
        PlayerKnowledgeData restarted = new PlayerKnowledgeData(() -> { });
        restarted.load(loaded.save());
        assertTrue(restarted.observed(KnowledgeObservation.END_REVELATION));
        assertTrue(PlayerKnowledge.endRevelationResearch(restarted));
    }
    @Test void bothResearchPathsAreIndependentOfObservationOrderAndSurviveReload() {
        PlayerKnowledgeData data = new PlayerKnowledgeData(() -> { });
        data.observe(KnowledgeObservation.BIOINDUSTRY);
        data.observe(KnowledgeObservation.WHITE_SANCTUARY);
        data.observe(KnowledgeObservation.CAUTERIZATION);
        assertFalse(PlayerKnowledge.severanceResearch(data));
        assertFalse(PlayerKnowledge.symbiosisResearch(data));
        data.observe(KnowledgeObservation.RIFT);
        assertTrue(PlayerKnowledge.severanceResearch(data));
        assertTrue(PlayerKnowledge.symbiosisResearch(data));
        data.observe(KnowledgeObservation.SEVERANCE_RESEARCH);
        data.observe(KnowledgeObservation.SYMBIOSIS_RESEARCH);
        PlayerKnowledgeData loaded = new PlayerKnowledgeData(() -> { });
        loaded.load(data.save());
        assertTrue(loaded.observed(KnowledgeObservation.SEVERANCE_RESEARCH));
        assertTrue(loaded.observed(KnowledgeObservation.SYMBIOSIS_RESEARCH));
        assertFalse(loaded.observe(KnowledgeObservation.SYMBIOSIS_RESEARCH));
    }
    @Test void providerSerializesKnowledgeAndKeepsPlayersSeparate() {
        PlayerKnowledgeProvider first = new PlayerKnowledgeProvider();
        PlayerKnowledgeProvider second = new PlayerKnowledgeProvider();
        PlayerKnowledgeData firstData = first.data();
        firstData.observe(KnowledgeObservation.RIFT);
        firstData.observe(KnowledgeObservation.BIOINDUSTRY);
        PlayerKnowledgeProvider reloaded = new PlayerKnowledgeProvider();
        reloaded.deserializeNBT(first.serializeNBT());
        assertTrue(reloaded.data().observed(KnowledgeObservation.BIOINDUSTRY));
        assertTrue(second.data().observations().isEmpty());
    }
}
