package com.bleedthrough.meatscape.core.registry;

import com.bleedthrough.meatscape.Meatscape;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import com.bleedthrough.meatscape.bioindustry.HeartPumpBlock;
import com.bleedthrough.meatscape.bioindustry.ArteryBlock;
import com.bleedthrough.meatscape.bioindustry.HematicActuatorBlock;
import com.bleedthrough.meatscape.bioindustry.EnzymeVatBlock;
import com.bleedthrough.meatscape.coherence.rift.RiftCoreBlock;
import com.bleedthrough.meatscape.architecture.RegenerativeMembraneBlock;
import com.bleedthrough.meatscape.world.maw.MawGatewayBlock;
import com.bleedthrough.meatscape.world.maw.NutrientMoundBlock;
import com.bleedthrough.meatscape.world.nether.BurningWoundBlock;
import com.bleedthrough.meatscape.world.end.EndWormholeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Minimal Phase 4 placeholders; later art phases may replace their presentation, not their IDs. */
public final class MeatscapeBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Meatscape.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Meatscape.MOD_ID);

    public static final RegistryObject<Block> BASE_ANCHOR = block("base_anchor", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(8.0F, 1200.0F)));
    public static final RegistryObject<Block> CHANGED_STONE = block("changed_stone", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.5F, 6.0F)));
    public static final RegistryObject<Block> CHARRED_SCAR = block("charred_scar", () -> simple(MapColor.COLOR_BLACK, 1.5F));
    public static final RegistryObject<Block> DERMAL_FILM = block("dermal_film", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.15F).noCollission()));
    public static final RegistryObject<Block> DERMAL_SOIL = block("dermal_soil", () -> simple(MapColor.COLOR_PINK, 0.7F));
    public static final RegistryObject<Block> OSSIFIED_STONE = block("ossified_stone", () -> simple(MapColor.QUARTZ, 2.0F));
    public static final RegistryObject<Block> VASCULAR_MAT = block("vascular_mat", () -> simple(MapColor.COLOR_RED, 0.5F));
    public static final RegistryObject<Block> NUTRIENT_MOUND = block("nutrient_mound", () -> new NutrientMoundBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.8F)));
    public static final RegistryObject<Block> DERMAL_PANEL = block("dermal_panel", () -> simple(MapColor.COLOR_PINK, 1.2F));
    public static final RegistryObject<Block> GESTATION_POD = block("gestation_pod", () -> simple(MapColor.COLOR_PURPLE, 1.0F));
    public static final RegistryObject<Block> RIFT_CORE = block("rift_core", () -> new RiftCoreBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(6.0F, 1200.0F)
                    .lightLevel(state -> state.getValue(RiftCoreBlock.ACTIVE) ? 7 : 0)));
    public static final RegistryObject<Block> HEART_PUMP = block("heart_pump", () -> new HeartPumpBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0F).lightLevel(state -> 3)));
    public static final RegistryObject<Block> ARTERY = block("artery", () -> new ArteryBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.0F)));
    public static final RegistryObject<Block> HEMATIC_ACTUATOR = block("hematic_actuator", () -> new HematicActuatorBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.0F).lightLevel(state -> state.getValue(HematicActuatorBlock.POWERED) ? 7 : 0)));
    public static final RegistryObject<Block> ENZYME_VAT = block("enzyme_vat", () -> new EnzymeVatBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(2.0F)));
    public static final RegistryObject<Block> REGENERATIVE_MEMBRANE = block("regenerative_membrane", () ->
            new RegenerativeMembraneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK)
                    .strength(2.5F, 6.0F)));
    public static final RegistryObject<Block> MAW_GATEWAY = block("maw_gateway", () -> new MawGatewayBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(8.0F, 1200.0F).lightLevel(state -> 5)));
    public static final RegistryObject<Block> BURNING_WOUND = BLOCKS.register("burning_wound", () -> new BurningWoundBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(8.0F, 1200.0F).lightLevel(state -> 9)));
    public static final RegistryObject<Block> END_WORMHOLE = BLOCKS.register("end_wormhole", () -> new EndWormholeBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(8.0F, 1200.0F).lightLevel(state -> 11)));

    private MeatscapeBlocks() { }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    private static RegistryObject<Block> block(String name, Supplier<Block> supplier) {
        RegistryObject<Block> block = BLOCKS.register(name, supplier);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static Block simple(MapColor color, float strength) {
        return new Block(BlockBehaviour.Properties.of().mapColor(color).strength(strength));
    }
}
