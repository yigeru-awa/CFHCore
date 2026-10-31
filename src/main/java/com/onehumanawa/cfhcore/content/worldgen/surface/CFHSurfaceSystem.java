package com.onehumanawa.cfhcore.content.worldgen.surface;

import java.util.List;
import java.util.function.Function;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import com.onehumanawa.cfhcore.content.worldgen.surface.rule.StrataRule;

public interface CFHSurfaceSystem {
	List<List<StrataRule.Layer>> getOrCreateStrata(ResourceLocation name, Function<RandomSource, List<List<StrataRule.Layer>>> factory);
}
