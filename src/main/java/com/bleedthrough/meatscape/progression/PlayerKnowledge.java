package com.bleedthrough.meatscape.progression;

import net.minecraft.server.level.ServerPlayer;

public final class PlayerKnowledge {
    private PlayerKnowledge() { }
    public static PlayerKnowledgeData get(ServerPlayer player) { return player.getCapability(PlayerKnowledgeCapability.INSTANCE).orElseThrow(() -> new IllegalStateException("Missing player knowledge capability")); }
    public static boolean observe(ServerPlayer player, KnowledgeObservation observation) { return get(player).observe(observation); }
    public static boolean severanceResearch(ServerPlayer player) { var data=get(player); return data.observed(KnowledgeObservation.RIFT) && data.observed(KnowledgeObservation.CAUTERIZATION) && data.observed(KnowledgeObservation.WHITE_SANCTUARY); }
    public static boolean symbiosisResearch(ServerPlayer player) { var data=get(player); return data.observed(KnowledgeObservation.RIFT) && data.observed(KnowledgeObservation.BIOINDUSTRY); }
    public static boolean endRevelationResearch(PlayerKnowledgeData data) {
        return data.observed(KnowledgeObservation.END_STONE)
                && data.observed(KnowledgeObservation.CHORUS)
                && data.observed(KnowledgeObservation.ANCIENT_ANCHOR);
    }
}
