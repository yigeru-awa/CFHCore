package com.onehumanawa.cfhcore.content.world.worldgen.surface;

import java.util.List;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import com.onehumanawa.cfhcore.content.world.worldgen.surface.rule.StrataRule;

public interface CFHWorldGenSurfaceSystem {
   List<List<StrataRule.Layer>> getOrCreateStrata(ResourceLocation var1, Function<RandomSource, List<List<StrataRule.Layer>>> var2);
}
