package com.bleedthrough.meatscape.gametest;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.bioindustry.EnzymeVatBlockEntity;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import com.bleedthrough.meatscape.core.registry.MeatscapeEffects;
import com.bleedthrough.meatscape.core.registry.MeatscapeItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** One actual item path from renewable food through Core-only industry into Maw adaptation. */
@GameTestHolder(Meatscape.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Phase8LoopGameTests {
    private Phase8LoopGameTests() { }

    @GameTest(template = "empty", batch = "phase812SupplyIndustry")
    public static void harvestedTissueBecomesCatalyzedAdaptationFood(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 2, 1));
        for (int i = 0; i < 3; i++) {
            BlockPos mound = origin.east(i);
            level.setBlockAndUpdate(mound, MeatscapeBlocks.NUTRIENT_MOUND.get().defaultBlockState());
            MeatscapeBlocks.NUTRIENT_MOUND.get().use(level.getBlockState(mound), level, mound, player,
                    InteractionHand.MAIN_HAND, hit(mound));
        }
        ItemStack raw = find(player.getInventory().items, MeatscapeItems.RAW_TISSUE.get());
        helper.assertTrue(raw.getCount() == 3, "Renewable harvest did not supply three Raw Tissue");

        BlockPos pump = origin.east(3);
        BlockPos vatPos = origin.east(4);
        level.setBlockAndUpdate(pump, MeatscapeBlocks.HEART_PUMP.get().defaultBlockState());
        level.setBlockAndUpdate(vatPos, MeatscapeBlocks.ENZYME_VAT.get().defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, raw);
        MeatscapeBlocks.HEART_PUMP.get().use(level.getBlockState(pump), level, pump, player,
                InteractionHand.MAIN_HAND, hit(pump));
        ItemStack collagen = find(player.getInventory().items, MeatscapeItems.COLLAGEN.get());
        helper.assertTrue(raw.getCount() == 1 && collagen.getCount() == 1, "Heart Pump did not conserve tissue and collagen");

        player.setItemInHand(InteractionHand.MAIN_HAND, collagen);
        MeatscapeBlocks.ENZYME_VAT.get().use(level.getBlockState(vatPos), level, vatPos, player,
                InteractionHand.MAIN_HAND, hit(vatPos));
        player.setItemInHand(InteractionHand.MAIN_HAND, raw);
        MeatscapeBlocks.ENZYME_VAT.get().use(level.getBlockState(vatPos), level, vatPos, player,
                InteractionHand.MAIN_HAND, hit(vatPos));
        var vat = (EnzymeVatBlockEntity) level.getBlockEntity(vatPos);
        for (int i = 0; i < EnzymeVatBlockEntity.PROCESS_TICKS; i++) {
            EnzymeVatBlockEntity.serverTick(level, vatPos, level.getBlockState(vatPos), vat);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        MeatscapeBlocks.ENZYME_VAT.get().use(level.getBlockState(vatPos), level, vatPos, player,
                InteractionHand.MAIN_HAND, hit(vatPos));
        ItemStack paste = find(player.getInventory().items, MeatscapeItems.NUTRIENT_PASTE.get());
        helper.assertTrue(!paste.isEmpty() && vat.output() == 0, "Vat output was lost or duplicated");
        paste.finishUsingItem(level, player);
        helper.assertTrue(player.hasEffect(MeatscapeEffects.MAW_ADAPTATION.get()),
                "The supply and industry loop did not produce Maw Adaptation");
        helper.succeed();
    }

    private static ItemStack find(java.util.List<ItemStack> stacks, Item item) {
        for (ItemStack stack : stacks) if (stack.is(item)) return stack;
        return ItemStack.EMPTY;
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }
}
