package com.onehumanawa.cfhcore.content.world.worldgen.feature.chance;

import com.mojang.serialization.MapCodec;
import com.onehumanawa.cfhcore.content.world.platform.RegistryUtil;
import com.onehumanawa.cfhcore.content.world.registries.CFHWorldGenBuiltInRegistries;

public class CFHWorldGenChanceModifiers {
   public static void bootstrap() {
      register("elevation", ElevationChanceModifier.CODEC);
      register("biome_edge", BiomeEdgeChanceModifier.CODEC);
   }

   public static ElevationChanceModifier elevation(float from, float to) {
      return elevation(from, to, false);
   }

   public static ElevationChanceModifier elevation(float from, float to, boolean exclusive) {
      return new ElevationChanceModifier(from, to, exclusive);
   }

   public static BiomeEdgeChanceModifier biomeEdge(float from, float to) {
      return biomeEdge(from, to, false);
   }

   public static BiomeEdgeChanceModifier biomeEdge(float from, float to, boolean exclusive) {
      return new BiomeEdgeChanceModifier(from, to, exclusive);
   }

   private static void register(String name, MapCodec<? extends ChanceModifier> placement) {
      RegistryUtil.register(CFHWorldGenBuiltInRegistries.CHANCE_MODIFIER_TYPE, name, placement);
   }
}
