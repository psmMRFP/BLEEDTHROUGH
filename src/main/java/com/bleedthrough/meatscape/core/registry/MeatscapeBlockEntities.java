package com.bleedthrough.meatscape.core.registry;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.architecture.RegenerativeMembraneBlockEntity;
import com.bleedthrough.meatscape.bioindustry.ArteryBlockEntity;
import com.bleedthrough.meatscape.bioindustry.HeartPumpBlockEntity;
import com.bleedthrough.meatscape.bioindustry.HematicActuatorBlockEntity;
import com.bleedthrough.meatscape.bioindustry.EnzymeVatBlockEntity;
import com.bleedthrough.meatscape.bioindustry.neural.NeuralNodeBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MeatscapeBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Meatscape.MOD_ID);

    public static final RegistryObject<BlockEntityType<RegenerativeMembraneBlockEntity>> REGENERATIVE_MEMBRANE =
            TYPES.register("regenerative_membrane", () -> BlockEntityType.Builder.of(
                    RegenerativeMembraneBlockEntity::new, MeatscapeBlocks.REGENERATIVE_MEMBRANE.get()).build(null));
    public static final RegistryObject<BlockEntityType<HeartPumpBlockEntity>> HEART_PUMP = TYPES.register("heart_pump", () -> BlockEntityType.Builder.of(HeartPumpBlockEntity::new, MeatscapeBlocks.HEART_PUMP.get()).build(null));
    public static final RegistryObject<BlockEntityType<ArteryBlockEntity>> ARTERY = TYPES.register("artery", () -> BlockEntityType.Builder.of(ArteryBlockEntity::new, MeatscapeBlocks.ARTERY.get()).build(null));
    public static final RegistryObject<BlockEntityType<HematicActuatorBlockEntity>> HEMATIC_ACTUATOR = TYPES.register("hematic_actuator", () -> BlockEntityType.Builder.of(HematicActuatorBlockEntity::new, MeatscapeBlocks.HEMATIC_ACTUATOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<EnzymeVatBlockEntity>> ENZYME_VAT = TYPES.register("enzyme_vat", () -> BlockEntityType.Builder.of(EnzymeVatBlockEntity::new, MeatscapeBlocks.ENZYME_VAT.get()).build(null));
    public static final RegistryObject<BlockEntityType<NeuralNodeBlockEntity>> NEURAL_SENSOR = TYPES.register("neural_sensor", () ->
            BlockEntityType.Builder.of(NeuralNodeBlockEntity::sensor, MeatscapeBlocks.NEURAL_SENSOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<NeuralNodeBlockEntity>> NEURAL_FIBER = TYPES.register("neural_fiber", () ->
            BlockEntityType.Builder.of(NeuralNodeBlockEntity::fiber, MeatscapeBlocks.NEURAL_FIBER.get()).build(null));
    public static final RegistryObject<BlockEntityType<NeuralNodeBlockEntity>> NEURAL_ACTUATOR = TYPES.register("neural_actuator", () ->
            BlockEntityType.Builder.of(NeuralNodeBlockEntity::actuator, MeatscapeBlocks.NEURAL_ACTUATOR.get()).build(null));

    private MeatscapeBlockEntities() { }

    public static void register(IEventBus bus) { TYPES.register(bus); }
}
