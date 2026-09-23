package com.bleedthrough.meatscape.progression;

import com.bleedthrough.meatscape.Meatscape;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Mirrors independent player knowledge into a small visible research page. */
public final class KnowledgeResearch {
    private KnowledgeResearch() { }

    static void observed(ServerPlayer player, KnowledgeObservation observation) {
        String key = switch (observation) {
            case RIFT -> "rift";
            case CAUTERIZATION -> "cauterization";
            case WHITE_SANCTUARY -> "white_sanctuary";
            case BIOINDUSTRY -> "bioindustry";
            default -> null;
        };
        if (key != null) {
            player.displayClientMessage(Component.translatable("message.meatscape.research." + key), false);
            sync(player);
        }
    }

    /** Also repairs visible progress after login, respawn, or datapack reload. */
    public static void sync(ServerPlayer player) {
        PlayerKnowledgeData data = PlayerKnowledge.get(player);
        if (PlayerKnowledge.severanceResearch(data) && data.observe(KnowledgeObservation.SEVERANCE_RESEARCH)) {
            player.displayClientMessage(Component.translatable("message.meatscape.research.severance"), false);
        }
        if (PlayerKnowledge.symbiosisResearch(data) && data.observe(KnowledgeObservation.SYMBIOSIS_RESEARCH)) {
            player.displayClientMessage(Component.translatable("message.meatscape.research.symbiosis"), false);
        }
        if (data.observed(KnowledgeObservation.RIFT) || data.observed(KnowledgeObservation.CAUTERIZATION)
                || data.observed(KnowledgeObservation.WHITE_SANCTUARY) || data.observed(KnowledgeObservation.BIOINDUSTRY)) {
            award(player, "root");
        }
        if (data.observed(KnowledgeObservation.RIFT)) award(player, "rift");
        if (data.observed(KnowledgeObservation.CAUTERIZATION)) award(player, "cauterization");
        if (data.observed(KnowledgeObservation.WHITE_SANCTUARY)) award(player, "white_sanctuary");
        if (data.observed(KnowledgeObservation.BIOINDUSTRY)) award(player, "bioindustry");
        if (data.observed(KnowledgeObservation.SEVERANCE_RESEARCH)) award(player, "severance");
        if (data.observed(KnowledgeObservation.SYMBIOSIS_RESEARCH)) award(player, "symbiosis");
    }

    private static void award(ServerPlayer player, String path) {
        Advancement advancement = player.getServer().getAdvancements().getAdvancement(
                ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "research/" + path));
        if (advancement != null && !player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            player.getAdvancements().award(advancement, "observed");
        }
    }
}
