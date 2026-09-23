package com.bleedthrough.meatscape.gametest;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import com.bleedthrough.meatscape.world.end.AncientAnchorLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(Meatscape.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EndRevelationGameTests {
    private EndRevelationGameTests() { }

    @GameTest(template = "empty", batch = "phase811EndRevelation")
    public static void anchorFitsOneChunkAndResearchTagsLoad(GameTestHelper helper) {
        var level = helper.getLevel();
        ChunkPos chunk = new ChunkPos(helper.absolutePos(new BlockPos(2, 2, 2)));
        BlockPos wormhole = new BlockPos(chunk.getMinBlockX() + 8, 4, chunk.getMinBlockZ() + 8);
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            level.setBlockAndUpdate(wormhole.offset(x, -1, z), Blocks.END_STONE.defaultBlockState());
            level.setBlockAndUpdate(wormhole.offset(x, 0, z), Blocks.AIR.defaultBlockState());
        }
        var site = AncientAnchorLayout.find(level, wormhole, chunk);
        helper.assertTrue(site.isPresent(), "Flat End Stone did not accept a local Anchor layout");
        var layout = site.orElseThrow();
        helper.assertTrue(AncientAnchorLayout.fits(chunk, layout.anchor(), layout.markers())
                && layout.anchor().relative(layout.facing(), 2).equals(wormhole), "Anchor points outside its local wormhole scene");
        level.setBlockAndUpdate(layout.markers().get(0), Blocks.CHEST.defaultBlockState());
        helper.assertTrue(AncientAnchorLayout.find(level, wormhole, chunk).stream()
                .noneMatch(other -> other.anchor().equals(layout.anchor())), "Layout overwrote an occupied surface");
        helper.assertTrue(!ForgeRegistries.ITEMS.containsKey(ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "ancient_anchor")),
                "Natural Ancient Anchor must have no placeable BlockItem");
        helper.assertTrue(MeatscapeBlocks.ANCIENT_ANCHOR.get().defaultBlockState().is(MeatscapeBlocks.ANCIENT_ANCHOR.get()),
                "Ancient Anchor block is missing");
        TagKey<Item> endStone = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "end_stone_samples"));
        TagKey<Item> chorus = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "chorus_samples"));
        helper.assertTrue(new ItemStack(Blocks.END_STONE).is(endStone)
                && new ItemStack(net.minecraft.world.item.Items.CHORUS_FRUIT).is(chorus), "End research item tags did not load");
        for (String path : new String[] {"root", "end_stone", "chorus", "ancient_anchor", "revelation"}) {
            helper.assertTrue(level.getServer().getAdvancements().getAdvancement(
                    ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "end_revelation/" + path)) != null,
                    "End research advancement did not load: " + path);
        }
        helper.succeed();
    }
}
