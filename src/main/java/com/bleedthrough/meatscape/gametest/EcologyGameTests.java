package com.bleedthrough.meatscape.gametest;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.coherence.MawCoherenceService;
import com.bleedthrough.meatscape.core.registry.MeatscapeBlocks;
import com.bleedthrough.meatscape.core.registry.MeatscapeEntities;
import com.bleedthrough.meatscape.ecology.EcologyEvents;
import com.bleedthrough.meatscape.ecology.EcologySpawnPolicy;
import com.bleedthrough.meatscape.ecology.MawGrazer;
import com.bleedthrough.meatscape.ecology.MawImmuneOrganism;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import com.bleedthrough.meatscape.world.data.WorldStage;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Meatscape.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EcologyGameTests {
    private EcologyGameTests() { }

    @GameTest(template = "empty", batch = "phase75Ecology")
    public static void taggedHabitatSpawningIsLocallyBounded(GameTestHelper helper) {
        var level = helper.getLevel();
        var world = MeatscapeWorldData.get(level.getServer());
        WorldStage previousStage = world.worldStage();
        boolean previousPause = world.isPaused();
        BlockPos fixture = helper.absolutePos(new BlockPos(2, 3, 2));
        ChunkPos chunk = new ChunkPos(fixture);
        BlockPos ground = new BlockPos(chunk.getMinBlockX() + 3, fixture.getY(), chunk.getMinBlockZ() + 3);
        int previousCoherence = MawCoherenceService.get(level.getChunkAt(ground));
        try {
            for (int attempt = 0; attempt < 5; attempt++) {
                level.setBlockAndUpdate(ground.offset(attempt * 2, 0, 0),
                        MeatscapeBlocks.NUTRIENT_MOUND.get().defaultBlockState());
            }
            world.setPaused(false);
            world.setWorldStage(WorldStage.BLEEDING);
            MawCoherenceService.set(level, chunk, 70);
            helper.assertTrue(level.getBlockState(ground).is(com.bleedthrough.meatscape.ecology.EcologyTags.GRAZER_FOOD),
                    "Nutrient mound is absent from the grazer food tag");
            helper.assertTrue(level.getBiome(ground.above()).is(com.bleedthrough.meatscape.ecology.EcologyTags.GRAZER_HABITATS),
                    "GameTest biome is absent from the grazer habitat tag");
            helper.assertTrue(EcologyEvents.Runtime.trySpawnAt(level, ground.above(), true),
                    "First Grazer could not spawn on a valid tagged habitat");
            for (int attempt = 1; attempt < 5; attempt++) {
                EcologyEvents.Runtime.trySpawnAt(level, ground.offset(attempt * 2, 1, 0), true);
            }
            int count = level.getEntitiesOfClass(MawGrazer.class,
                    new net.minecraft.world.phys.AABB(ground).inflate(32)).size();
            helper.assertTrue(count == EcologySpawnPolicy.GRAZER_LOCAL_CAP,
                    "Grazer spawning did not stop exactly at its local density cap: " + count);
            helper.succeed();
        } finally {
            level.getEntitiesOfClass(MawGrazer.class, new net.minecraft.world.phys.AABB(ground).inflate(32))
                    .forEach(Entity::discard);
            for (int attempt = 0; attempt < 5; attempt++) {
                level.removeBlock(ground.offset(attempt * 2, 0, 0), false);
            }
            MawCoherenceService.set(level, chunk, previousCoherence);
            world.setWorldStage(previousStage);
            world.setPaused(previousPause);
        }
    }

    @GameTest(template = "empty", batch = "phase75Ecology")
    public static void signatureBehaviorsHealAndPauseWithoutWorldMutation(GameTestHelper helper) {
        var level = helper.getLevel();
        var world = MeatscapeWorldData.get(level.getServer());
        boolean previousPause = world.isPaused();
        MawGrazer grazer = MeatscapeEntities.MAW_GRAZER.get().create(level);
        MawImmuneOrganism immune = MeatscapeEntities.IMMUNE_ORGANISM.get().create(level);
        Player player = helper.makeMockPlayer();
        helper.assertTrue(grazer != null && immune != null, "Ecology entities failed to create");
        try {
            grazer.setHealth(8.0F);
            grazer.finishGrazing();
            helper.assertTrue(grazer.getHealth() == 12.0F && grazer.grazeCooldown() == 600,
                    "Grazer browse reward or cooldown is incorrect");
            world.setPaused(false);
            helper.assertTrue(immune.immuneResponseActive(), "Immune response inactive in normal active world");
            immune.setTarget(player);
            world.setPaused(true);
            grazer.aiStep();
            immune.aiStep();
            helper.assertTrue(grazer.grazeCooldown() == 600,
                    "Pause advanced the Grazer signature-behavior cooldown");
            helper.assertTrue(immune.getTarget() == null && !immune.immuneResponseActive(),
                    "Pause did not clear and freeze the immune response");
            helper.succeed();
        } finally {
            world.setPaused(previousPause);
            grazer.discard();
            immune.discard();
            player.discard();
        }
    }
}
