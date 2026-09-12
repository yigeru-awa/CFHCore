package com.onehumanawa.cfhcore.content.world.worldgen.biome.modifier.neoforge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo.BiomeInfo.Builder;
import com.onehumanawa.cfhcore.content.world.neoforge.mixin.MixinBiomeGenerationSettingsPlainsBuilder;

public record ReplaceModifier(Decoration step, Optional<HolderSet<Biome>> biomes, Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> replacements)
   implements ForgeBiomeModifier {
   public static final MapCodec<ReplaceModifier> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               Decoration.CODEC.fieldOf("step").forGetter(ReplaceModifier::step),
               Biome.LIST_CODEC.optionalFieldOf("biomes").forGetter(ReplaceModifier::biomes),
               Codec.unboundedMap(ResourceKey.codec(Registries.PLACED_FEATURE), PlacedFeature.CODEC)
                  .fieldOf("replacements")
                  .forGetter(ReplaceModifier::replacements)
            )
            .apply(instance, ReplaceModifier::new)
   );

   @Override
   public void modify(Holder<Biome> biome, Phase phase, Builder builder) {
      if (phase == Phase.AFTER_EVERYTHING) {
         if (!(builder.getGenerationSettings() instanceof MixinBiomeGenerationSettingsPlainsBuilder builderAccessor)) {
            throw new IllegalStateException();
         }

         if (this.biomes.isPresent() && !this.biomes.get().contains(biome)) {
            return;
         }

         List<List<Holder<PlacedFeature>>> featureSteps = builderAccessor.getFeatures();
         int index = this.step.ordinal();

         while (index >= featureSteps.size()) {
            featureSteps.add(Collections.emptyList());
         }

         List<Holder<PlacedFeature>> replaced = new ArrayList<>(featureSteps.get(index));
         replaced.replaceAll(f -> f.unwrapKey().map(this.replacements::get).orElse(f));
         featureSteps.set(index, replaced);
      }
   }

   @Override
   public MapCodec<ReplaceModifier> codec() {
      return CODEC;
   }
}
