package com.bleedthrough.meatscape.gametest;

import com.bleedthrough.meatscape.Meatscape;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Meatscape.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResearchGameTests {
    private ResearchGameTests() { }

    @GameTest(template = "empty", batch = "phase76Research")
    public static void researchPageResourcesLoadOnDedicatedServer(GameTestHelper helper) {
        for (String path : new String[] {"root", "rift", "cauterization", "white_sanctuary", "bioindustry", "severance", "symbiosis"}) {
            var advancement = helper.getLevel().getServer().getAdvancements().getAdvancement(
                    ResourceLocation.fromNamespaceAndPath(Meatscape.MOD_ID, "research/" + path));
            helper.assertTrue(advancement != null, "Research advancement missing: " + path);
        }
        helper.succeed();
    }
}
