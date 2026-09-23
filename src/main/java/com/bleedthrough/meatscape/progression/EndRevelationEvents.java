package com.bleedthrough.meatscape.progression;

import com.bleedthrough.meatscape.Meatscape;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Order-independent End material observations with one visible completion notice. */
@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EndRevelationEvents {
    private static final TagKey<Item> END_STONE_SAMPLES = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "end_stone_samples"));
    private static final TagKey<Item> CHORUS_SAMPLES = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "chorus_samples"));

    private EndRevelationEvents() { }

    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 40 != 0) return;
        PlayerKnowledgeData knowledge = PlayerKnowledge.get(player);
        if (!knowledge.observed(KnowledgeObservation.END_STONE) || !knowledge.observed(KnowledgeObservation.CHORUS)) {
            for (var stack : player.getInventory().items) {
                observeSamples(player, stack);
                if (knowledge.observed(KnowledgeObservation.END_STONE) && knowledge.observed(KnowledgeObservation.CHORUS)) break;
            }
            if (!knowledge.observed(KnowledgeObservation.END_STONE) || !knowledge.observed(KnowledgeObservation.CHORUS)) {
                for (var stack : player.getInventory().offhand) observeSamples(player, stack);
            }
        }
        completeIfReady(player);
        syncAdvancements(player);
    }

    public static void anchorObserved(ServerPlayer player) {
        observe(player, KnowledgeObservation.ANCIENT_ANCHOR, "message.meatscape.anchor_observed");
        completeIfReady(player);
        syncAdvancements(player);
    }

    private static void observe(ServerPlayer player, KnowledgeObservation observation, String key) {
        if (PlayerKnowledge.observe(player, observation)) player.displayClientMessage(Component.translatable(key), false);
    }

    private static void observeSamples(ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
        if (stack.is(END_STONE_SAMPLES)) observe(player, KnowledgeObservation.END_STONE, "message.meatscape.end_stone_observed");
        if (stack.is(CHORUS_SAMPLES)) observe(player, KnowledgeObservation.CHORUS, "message.meatscape.chorus_observed");
    }

    private static void completeIfReady(ServerPlayer player) {
        PlayerKnowledgeData knowledge = PlayerKnowledge.get(player);
        if (PlayerKnowledge.endRevelationResearch(knowledge)
                && PlayerKnowledge.observe(player, KnowledgeObservation.END_REVELATION)) {
            player.displayClientMessage(Component.translatable("message.meatscape.end_revelation_complete"), false);
        }
    }

    /** Advancements mirror the versioned knowledge capability, including notes from older saves. */
    private static void syncAdvancements(ServerPlayer player) {
        PlayerKnowledgeData data = PlayerKnowledge.get(player);
        if (data.observed(KnowledgeObservation.END_STONE) || data.observed(KnowledgeObservation.CHORUS)
                || data.observed(KnowledgeObservation.ANCIENT_ANCHOR)) award(player, "root");
        if (data.observed(KnowledgeObservation.END_STONE)) award(player, "end_stone");
        if (data.observed(KnowledgeObservation.CHORUS)) award(player, "chorus");
        if (data.observed(KnowledgeObservation.ANCIENT_ANCHOR)) award(player, "ancient_anchor");
        if (data.observed(KnowledgeObservation.END_REVELATION)) award(player, "revelation");
    }

    private static void award(ServerPlayer player, String path) {
        Advancement advancement = player.getServer().getAdvancements().getAdvancement(
                ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "end_revelation/" + path));
        if (advancement != null && !player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            player.getAdvancements().award(advancement, "observed");
        }
    }
}
