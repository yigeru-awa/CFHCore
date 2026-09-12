package com.onehumanawa.cfhcore.content.world.preset.settings;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.Cloner.Factory;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import com.onehumanawa.cfhcore.content.world.CFHWorldGenCommon;
import com.onehumanawa.cfhcore.content.world.preset.PresetBiomeModifierData;
import com.onehumanawa.cfhcore.content.world.preset.PresetConfiguredFeatures;
import com.onehumanawa.cfhcore.content.world.preset.PresetDimensionTypes;
import com.onehumanawa.cfhcore.content.world.preset.PresetNoiseData;
import com.onehumanawa.cfhcore.content.world.preset.PresetNoiseGeneratorSettings;
import com.onehumanawa.cfhcore.content.world.preset.PresetNoiseRouterData;
import com.onehumanawa.cfhcore.content.world.preset.PresetPlacedFeatures;
import com.onehumanawa.cfhcore.content.world.preset.PresetStructureRuleData;
import com.onehumanawa.cfhcore.content.world.registries.CFHWorldGenRegistries;
import com.onehumanawa.cfhcore.content.world.worldgen.biome.modifier.BiomeModifier;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.structure.rule.StructureRule;

public record Preset(
   WorldSettings world,
   SurfaceSettings surface,
   CaveSettings caves,
   ClimateSettings climate,
   TerrainSettings terrain,
   RiverSettings rivers,
   IslandSettings island,
   FilterSettings filters,
   StructureSettings structures,
   MiscellaneousSettings miscellaneous
) {
   public Preset copy() {
      return new Preset(
         this.world.copy(),
         this.surface.copy(),
         this.caves.copy(),
         this.climate.copy(),
         this.terrain.copy(),
         this.rivers.copy(),
         this.island.copy(),
         this.filters.copy(),
         this.structures.copy(),
         this.miscellaneous.copy()
      );
   }

   /**
    * Builds the dynamic-registry patch derived from this preset. Used at datagen time to emit
    * the built-in datapack that makes CFHWorldGen's worldgen actually kick in.
    */
   public RegistrySetBuilder.PatchedRegistries buildPatch(Provider registries) {
      RegistrySetBuilder builder = new RegistrySetBuilder();
      this.addPatch(builder, CFHWorldGenRegistries.NOISE, PresetNoiseData::bootstrap);
      this.addPatch(builder, CFHWorldGenRegistries.BIOME_MODIFIER, PresetBiomeModifierData::bootstrap);
      this.addPatch(builder, CFHWorldGenRegistries.STRUCTURE_RULE, PresetStructureRuleData::bootstrap);
      this.addPatch(builder, Registries.CONFIGURED_FEATURE, PresetConfiguredFeatures::bootstrap);
      this.addPatch(builder, Registries.PLACED_FEATURE, PresetPlacedFeatures::bootstrap);
      this.addPatch(builder, Registries.DIMENSION_TYPE, PresetDimensionTypes::bootstrap);
      this.addPatch(builder, Registries.DENSITY_FUNCTION, (preset, ctx) -> PresetNoiseRouterData.bootstrap(preset, ctx));
      this.addPatch(builder, Registries.NOISE_SETTINGS, PresetNoiseGeneratorSettings::bootstrap);
      Factory factory = new Factory();
      Set<ResourceKey<? extends Registry<?>>> armedRegistries = new HashSet<>();
      RegistryDataLoader.WORLDGEN_REGISTRIES.forEach(registryData -> {
         ResourceKey<? extends Registry<?>> key = registryData.key();
         if (isOwnNamespace(key)) {
            registryData.runWithArguments(factory::addCodec);
            armedRegistries.add(key);
         }
      });
      armedRegistries.add(Registries.STRUCTURE_SET);
      this.addAndTrack(factory, armedRegistries, CFHWorldGenRegistries.NOISE, Noise.DIRECT_CODEC);
      this.addAndTrack(factory, armedRegistries, CFHWorldGenRegistries.BIOME_MODIFIER, BiomeModifier.DIRECT_CODEC);
      this.addAndTrack(factory, armedRegistries, CFHWorldGenRegistries.STRUCTURE_RULE, StructureRule.DIRECT_CODEC);
      Provider safeSource = filterToArmedOnly(registries, armedRegistries);
      return builder.buildPatch(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), safeSource, factory);
   }

   private static boolean isOwnNamespace(ResourceKey<? extends Registry<?>> key) {
      String namespace = key.location().getNamespace();
      return namespace.equals("minecraft") || namespace.equals(CFHWorldGenCommon.MOD_ID);
   }

   private <T> void addAndTrack(Factory factory, Set<ResourceKey<? extends Registry<?>>> set, ResourceKey<? extends Registry<T>> key, Codec<T> codec) {
      factory.addCodec(key, codec);
      set.add(key);
   }

   /**
    * Restricts the source lookup to registries we have a {@link net.minecraft.core.Cloner} for,
    * so that dynamic registries contributed by unrelated mods are ignored.
    */
   private static Provider filterToArmedOnly(Provider original, Set<ResourceKey<? extends Registry<?>>> armed) {
      return new Provider() {
         @Override
         public Stream<ResourceKey<? extends Registry<?>>> listRegistries() {
            return original.listRegistries().filter(key -> isOwnNamespace(key) || armed.contains(key));
         }

         @Override
         public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) {
            return isOwnNamespace(key) || armed.contains(key) ? original.lookup(key) : Optional.empty();
         }
      };
   }

   private <T> void addPatch(RegistrySetBuilder builder, ResourceKey<? extends Registry<T>> key, Preset.Patch<T> patch) {
      builder.add(key, ctx -> patch.apply(this, ctx));
   }

   private interface Patch<T> {
      void apply(Preset var1, BootstrapContext<T> var2);
   }
}
