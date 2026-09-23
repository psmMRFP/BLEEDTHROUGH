package com.bleedthrough.meatscape.progression;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.thermal.ThermalRules;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

public final class PlayerKnowledgeEvents {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "player_knowledge");
    private PlayerKnowledgeEvents() { }
    @Mod.EventBusSubscriber(modid = Meatscape.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration { @SubscribeEvent public static void register(RegisterCapabilitiesEvent event) { event.register(PlayerKnowledgeData.class); } }
    @Mod.EventBusSubscriber(modid = Meatscape.MOD_ID)
    public static final class Runtime {
        @SubscribeEvent public static void attach(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) {
            if (!(event.getObject() instanceof net.minecraft.world.entity.player.Player)) return;
            PlayerKnowledgeProvider provider = new PlayerKnowledgeProvider(); event.addCapability(ID, provider); event.addListener(provider::invalidate);
        }
        @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
            event.getOriginal().reviveCaps();
            event.getOriginal().getCapability(PlayerKnowledgeCapability.INSTANCE).ifPresent(old -> event.getEntity().getCapability(PlayerKnowledgeCapability.INSTANCE).ifPresent(next -> next.copyFrom(old)));
            event.getOriginal().invalidateCaps();
        }
        @SubscribeEvent(priority = EventPriority.LOWEST) public static void observeRift(PlayerInteractEvent.RightClickBlock event) {
            if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) return;
            if (event.isCanceled() || event.getUseBlock() == net.minecraftforge.eventbus.api.Event.Result.DENY
                    || event.getUseItem() == net.minecraftforge.eventbus.api.Event.Result.DENY) return;
            if (event.getLevel().getBlockState(event.getPos()).is(MeatscapeBlocks.RIFT_CORE.get())) PlayerKnowledge.observe(player, KnowledgeObservation.RIFT);
        }
        @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 40 != 0) return;
            if (!PlayerKnowledge.get(player).observed(KnowledgeObservation.WHITE_SANCTUARY)
                    && ThermalRules.frozen(player.serverLevel(), player.blockPosition())) {
                PlayerKnowledge.observe(player, KnowledgeObservation.WHITE_SANCTUARY);
            }
            KnowledgeResearch.sync(player);
        }
    }
}
