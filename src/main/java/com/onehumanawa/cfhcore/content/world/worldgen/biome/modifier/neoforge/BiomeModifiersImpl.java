package com.onehumanawa.cfhcore.content.world.worldgen.biome.modifier.neoforge;

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
import com.onehumanawa.cfhcore.content.world.worldgen.biome.modifier.BiomeModifier;
import com.onehumanawa.cfhcore.content.world.worldgen.biome.modifier.Filter;
import com.onehumanawa.cfhcore.content.world.worldgen.biome.modifier.Order;

public class BiomeModifiersImpl {
   public static void bootstrap() {
      register("add", AddModifier.CODEC);
      register("replace", ReplaceModifier.CODEC);
   }

   public static BiomeModifier add(Order order, Decoration step, Optional<Pair<Filter.Behavior, HolderSet<Biome>>> biomes, HolderSet<PlacedFeature> features) {
      return new AddModifier(order, step, biomes.map(p -> new Filter(p.getSecond(), p.getFirst())), features);
   }

   public static BiomeModifier replace(Decoration step, Optional<HolderSet<Biome>> biomes, Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> replacements) {
      return new ReplaceModifier(step, biomes, replacements);
   }

   public static void register(String name, MapCodec<? extends BiomeModifier> value) {
      RegistryUtil.register(CFHWorldGenBuiltInRegistries.BIOME_MODIFIER_TYPE, name, value);
   }
}
