package com.onehumanawa.cfhcore.content.worldgen.platform;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameRules;

/**
 * NeoForge-only registry helpers (the old architectury platform facade, folded
 * onto its NeoForge implementation in {@link RegistryUtilImpl}).
 */
public final class RegistryUtil {

	public static <T> void register(Registry<T> registry, String name, T value) {
		RegistryUtilImpl.register(registry, name, value);
	}

	public static <T> Registry<T> createRegistry(ResourceKey<Registry<T>> key) {
		return RegistryUtilImpl.createRegistry(key);
	}

	public static <T> void createDataRegistry(ResourceKey<Registry<T>> key, Codec<T> codec, boolean synced) {
		RegistryUtilImpl.createDataRegistry(key, codec, synced);
	}

	public static <T extends GameRules.Value<T>> GameRules.Key<T> registerGameRule(String name, GameRules.Category category, GameRules.Type<T> type) {
		return RegistryUtilImpl.registerGameRule(name, category, type);
	}

	public static List<RegistryDataLoader.RegistryData<?>> getDynamicRegistries() {
		return RegistryUtilImpl.getDynamicRegistries();
	}

	public static List<RegistryDataLoader.RegistryData<?>> getDynamicRegistriesWithDimensions() {
		ImmutableList.Builder<RegistryDataLoader.RegistryData<?>> list = ImmutableList.builder();
		list.addAll(getDynamicRegistries());
		list.addAll(RegistryDataLoader.DIMENSION_REGISTRIES);
		return list.build();
	}
}
