package com.bleedthrough.meatscape.compatibility;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import com.bleedthrough.meatscape.core.registry.MeatscapeEffects;
import com.bleedthrough.meatscape.world.maw.MawDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class CompatibilityEvents {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "compatibility");
    private CompatibilityEvents() { }
    @Mod.EventBusSubscriber(modid = Meatscape.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration { @SubscribeEvent public static void register(RegisterCapabilitiesEvent event) { event.register(CompatibilityData.class); } }
    @Mod.EventBusSubscriber(modid = Meatscape.MOD_ID)
    public static final class Runtime {
        @SubscribeEvent public static void attach(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) { if (event.getObject() instanceof net.minecraft.world.entity.player.Player) { CompatibilityProvider provider = new CompatibilityProvider(); event.addCapability(ID, provider); event.addListener(provider::invalidate); } }
        @SubscribeEvent public static void clone(PlayerEvent.Clone event) { event.getOriginal().reviveCaps(); event.getOriginal().getCapability(CompatibilityCapability.INSTANCE).ifPresent(old -> event.getEntity().getCapability(CompatibilityCapability.INSTANCE).ifPresent(next -> next.copyFrom(old))); event.getOriginal().invalidateCaps(); }
        @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) { if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 200 != 0 || !player.level().dimension().equals(MawDimensions.MAW) || !player.hasEffect(MeatscapeEffects.MAW_ADAPTATION.get())) return; player.getCapability(CompatibilityCapability.INSTANCE).ifPresent(data -> data.add(1)); }
        @SubscribeEvent public static void stoneblight(EntityPlaceEvent event) { if (!(event.getEntity() instanceof ServerPlayer player) || !player.level().dimension().equals(MawDimensions.MAW)) return; var placed = event.getPlacedBlock(); if (!(placed.is(Blocks.STONE) || placed.is(Blocks.DIRT) || placed.is(Blocks.OAK_PLANKS))) return; BlockPos target = event.getPos().above(); if (player.level().isEmptyBlock(target)) player.level().setBlock(target, MeatscapeBlocks.DERMAL_FILM.get().defaultBlockState(), 3); }
    }
}
