package com.bleedthrough.meatscape.compatibility;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
public final class CompatibilityCapability { public static final Capability<CompatibilityData> INSTANCE = CapabilityManager.get(new CapabilityToken<>() { }); private CompatibilityCapability() { } }
