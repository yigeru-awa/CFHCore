package com.onehumanawa.cfhcore.content.world.worldgen.surface.rule;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import com.onehumanawa.cfhcore.content.world.platform.RegistryUtil;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;

public class CFHWorldGenSurfaceRules {
   public static void bootstrap() {
      register("strata", StrataRule.CODEC);
   }

   public static StrataRule strata(ResourceLocation name, Holder<Noise> selector, List<StrataRule.Strata> strata, int iterations) {
      return new StrataRule(name, selector, strata, iterations);
   }

   public static void register(String name, MapCodec<? extends RuleSource> value) {
      RegistryUtil.register(BuiltInRegistries.MATERIAL_RULE, name, value);
   }
}
