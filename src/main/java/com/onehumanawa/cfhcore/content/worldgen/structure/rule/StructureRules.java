package com.onehumanawa.cfhcore.content.worldgen.structure.rule;

import com.google.common.collect.ImmutableSet;

import com.mojang.serialization.MapCodec;
import com.onehumanawa.cfhcore.content.worldgen.platform.RegistryUtil;
import com.onehumanawa.cfhcore.content.worldgen.registries.CFHBuiltInRegistries;
import com.onehumanawa.cfhcore.content.worldgen.cell.terrain.Terrain;

public class StructureRules {

	public static void bootstrap() {
		register("cell_test", CellTest.CODEC);
	}
	
	public static CellTest cellTest(float cutoff, Terrain... terrainTypeBlacklist) {
		return new CellTest(cutoff, ImmutableSet.copyOf(terrainTypeBlacklist));
	}

	private static void register(String name, MapCodec<? extends StructureRule> value) {
		RegistryUtil.register(CFHBuiltInRegistries.STRUCTURE_RULE_TYPE, name, value);
	}
}
