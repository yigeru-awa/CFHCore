package com.onehumanawa.cfhcore.content.world.worldgen.biome.modifier;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import com.onehumanawa.cfhcore.content.world.platform.RegistryUtil;
import com.onehumanawa.cfhcore.content.world.registries.CFHWorldGenBuiltInRegistries;
import com.onehumanawa.cfhcore.content.world.worldgen.biome.modifier.neoforge.BiomeModifiersImpl;

public class BiomeModifiers {
   public static void bootstrap() {
      BiomeModifiersImpl.bootstrap();
   }

   @SafeVarargs
   public static BiomeModifier add(Order order, Decoration step, Holder<PlacedFeature>... features) {
      return add(order, step, HolderSet.direct(features));
   }

   public static BiomeModifier add(Order order, Decoration step, HolderSet<PlacedFeature> features) {
      return add(order, step, Optional.empty(), features);
   }

   @SafeVarargs
   public static BiomeModifier add(Order order, Decoration step, Filter.Behavior filterBehavior, HolderSet<Biome> biomes, Holder<PlacedFeature>... features) {
      return add(order, step, filterBehavior, biomes, HolderSet.direct(features));
   }

   public static BiomeModifier add(Order order, Decoration step, Filter.Behavior filterBehavior, HolderSet<Biome> biomes, HolderSet<PlacedFeature> features) {
      return add(order, step, Optional.of(Pair.of(filterBehavior, biomes)), features);
   }

   public static BiomeModifier add(Order order, Decoration step, Optional<Pair<Filter.Behavior, HolderSet<Biome>>> biomes, HolderSet<PlacedFeature> features) {
      return BiomeModifiersImpl.add(order, step, biomes, features);
   }

   public static BiomeModifier replace(Decoration step, Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> replacements) {
      return replace(step, Optional.empty(), replacements);
   }

   public static BiomeModifier replace(Decoration step, HolderSet<Biome> biomes, Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> replacements) {
      return replace(step, Optional.of(biomes), replacements);
   }

   public static BiomeModifier replace(Decoration step, Optional<HolderSet<Biome>> biomes, Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> replacements) {
      return BiomeModifiersImpl.replace(step, biomes, replacements);
   }

   public static void register(String name, MapCodec<? extends BiomeModifier> value) {
      RegistryUtil.register(CFHWorldGenBuiltInRegistries.BIOME_MODIFIER_TYPE, name, value);
   }
}
