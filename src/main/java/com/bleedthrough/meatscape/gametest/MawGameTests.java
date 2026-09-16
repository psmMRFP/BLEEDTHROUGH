package com.bleedthrough.meatscape.gametest;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.world.maw.MawDimensions;
import com.bleedthrough.meatscape.world.maw.NutrientMoundBlock;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import com.bleedthrough.meatscape.core.registry.MeatscapeItems;
import com.bleedthrough.meatscape.bioindustry.ArteryBlockEntity;
import com.bleedthrough.meatscape.bioindustry.HeartPumpBlockEntity;
import com.bleedthrough.meatscape.bioindustry.HematicActuatorBlock;
import com.bleedthrough.meatscape.bioindustry.HematicActuatorBlockEntity;
import com.bleedthrough.meatscape.bioindustry.EnzymeVatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Runtime data-pack verification for the minimum 8.1 dimension contract. */
@GameTestHolder(Meatscape.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MawGameTests {
    private MawGameTests() {
    }

    @GameTest(template = "empty", batch = "phase81Maw")
    public static void mawDimensionLoadsWithExpectedBuildRange(GameTestHelper helper) {
        var maw = helper.getLevel().getServer().getLevel(MawDimensions.MAW);
        helper.assertTrue(maw != null, "The Maw dimension was not registered from its data pack");
        helper.assertTrue(maw != null && maw.getMinBuildHeight() == -64 && maw.getMaxBuildHeight() == 320,
                "The Maw build range is not -64..319");
        helper.assertTrue(maw != null && maw.getBiome(new BlockPos(0, 64, 0)).unwrapKey()
                        .map(key -> key.location().equals(MawDimensions.MAW_ID.withPath("subdermal_expanse"))).orElse(false),
                "The Maw is not using the fixed Subdermal Expanse placeholder biome");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "phase82Maw")
    public static void nutrientMoundHarvestAndScheduledRegrowthAreBounded(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var mound = MeatscapeBlocks.NUTRIENT_MOUND.get();
        level.setBlockAndUpdate(pos, mound.defaultBlockState());
        var player = helper.makeMockPlayer();
        mound.use(level.getBlockState(pos), level, pos, player, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),
                        net.minecraft.core.Direction.UP, pos, false));
        helper.assertTrue(!level.getBlockState(pos).getValue(NutrientMoundBlock.NOURISHED), "Harvest did not deplete mound");
        helper.assertTrue(player.getInventory().contains(new net.minecraft.world.item.ItemStack(MeatscapeItems.RAW_TISSUE.get())),
                "Harvest did not grant Raw Tissue");
        mound.tick(level.getBlockState(pos), level, pos, level.random);
        helper.assertTrue(level.getBlockState(pos).getValue(NutrientMoundBlock.NOURISHED), "Scheduled regrowth did not restore mound");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "phase84Wormhole")
    public static void endWormholeIsRegisteredWithoutPlaceableItem(GameTestHelper helper) {
        helper.assertTrue(MeatscapeBlocks.END_WORMHOLE.get().defaultBlockState().is(MeatscapeBlocks.END_WORMHOLE.get()),
                "End Wormhole block was not registered");
        helper.assertTrue(!ForgeRegistries.ITEMS.containsKey(ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "end_wormhole")),
                "End Wormhole must remain a natural-only entrance without a BlockItem");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "phase85Canopy")
    public static void vascularCanopyFeatureIsRegistered(GameTestHelper helper) {
        helper.assertTrue(ForgeRegistries.FEATURES.containsKey(ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "vascular_canopy")),
                "Vascular Canopy feature was not registered");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "phase86Hematic")
    public static void hematicCircuitTransfersBoundedlyAndConsumesAtLoad(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pumpPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos arteryPos = pumpPos.east(); BlockPos actuatorPos = arteryPos.east();
        level.setBlockAndUpdate(pumpPos, MeatscapeBlocks.HEART_PUMP.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.EAST));
        level.setBlockAndUpdate(arteryPos, MeatscapeBlocks.ARTERY.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.EAST));
        level.setBlockAndUpdate(actuatorPos, MeatscapeBlocks.HEMATIC_ACTUATOR.get().defaultBlockState());
        var pump = (HeartPumpBlockEntity) level.getBlockEntity(pumpPos); var artery = (ArteryBlockEntity) level.getBlockEntity(arteryPos); var actuator = (HematicActuatorBlockEntity) level.getBlockEntity(actuatorPos);
        helper.assertTrue(pump != null && artery != null && actuator != null, "Hematic node BlockEntity missing");
        pump.addHematic(HeartPumpBlockEntity.TISSUE_YIELD);
        HeartPumpBlockEntity.serverTick(level, pumpPos, level.getBlockState(pumpPos), pump);
        ArteryBlockEntity.serverTick(level, arteryPos, level.getBlockState(arteryPos), artery);
        HematicActuatorBlockEntity.serverTick(level, actuatorPos, level.getBlockState(actuatorPos), actuator);
        helper.assertTrue(pump.hematic() == 200 && artery.hematic() == 0 && actuator.hematic() == 40, "Hematic volume was not conserved across the bounded circuit");
        helper.assertTrue(level.getBlockState(actuatorPos).getValue(HematicActuatorBlock.POWERED), "Actuator did not activate after consuming Hematic fluid");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "phase88Enzymatic")
    public static void enzymeVatPreservesCatalystAndProgressAcrossReload(GameTestHelper helper) {
        var level = helper.getLevel(); BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, MeatscapeBlocks.ENZYME_VAT.get().defaultBlockState());
        var vat = (EnzymeVatBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(vat != null && vat.addCatalyst() && vat.addTissue(), "Enzyme Vat did not accept bounded inputs");
        for (int tick = 0; tick < 40; tick++) EnzymeVatBlockEntity.serverTick(level, pos, level.getBlockState(pos), vat);
        var saved = vat.saveWithoutMetadata(); var reloaded = new EnzymeVatBlockEntity(pos, level.getBlockState(pos)); reloaded.load(saved);
        helper.assertTrue(reloaded.catalyst() == 1 && reloaded.tissue() == 1 && reloaded.progress() == 40, "Vat state did not persist");
        for (int tick = 0; tick < 60; tick++) EnzymeVatBlockEntity.serverTick(level, pos, level.getBlockState(pos), reloaded);
        helper.assertTrue(reloaded.catalyst() == 1 && reloaded.tissue() == 0 && reloaded.output() == 1, "Catalyst was consumed or output was duplicated");
        helper.succeed();
    }
}
