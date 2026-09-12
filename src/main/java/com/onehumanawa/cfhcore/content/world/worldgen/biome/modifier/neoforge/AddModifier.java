package com.onehumanawa.cfhcore.content.world.worldgen.biome.modifier.neoforge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo.BiomeInfo.Builder;
import org.jetbrains.annotations.Nullable;
import com.onehumanawa.cfhcore.content.world.neoforge.mixin.MixinBiomeGenerationSettingsPlainsBuilder;
import com.onehumanawa.cfhcore.content.world.worldgen.biome.modifier.Filter;
import com.onehumanawa.cfhcore.content.world.worldgen.biome.modifier.Order;

public record AddModifier(Order order, Decoration step, Optional<Filter> biomes, HolderSet<PlacedFeature> features) implements ForgeBiomeModifier {
   public static final MapCodec<AddModifier> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               Order.CODEC.fieldOf("order").forGetter(AddModifier::order),
               Decoration.CODEC.fieldOf("step").forGetter(AddModifier::step),
               Filter.CODEC.optionalFieldOf("biomes").forGetter(AddModifier::biomes),
               PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(AddModifier::features)
            )
            .apply(instance, AddModifier::new)
   );

   @Override
   public void modify(Holder<Biome> biome, Phase phase, Builder builder) {
      if (phase == Phase.AFTER_EVERYTHING) {
         if (!(builder.getGenerationSettings() instanceof MixinBiomeGenerationSettingsPlainsBuilder builderAccessor)) {
            throw new IllegalStateException();
         }

         if (this.biomes.isPresent() && !this.biomes.get().test(biome)) {
            return;
         }

         List<List<Holder<PlacedFeature>>> featureSteps = builderAccessor.getFeatures();
         int index = this.step.ordinal();

         while (index >= featureSteps.size()) {
            featureSteps.add(Collections.emptyList());
         }

         featureSteps.set(index, this.add(featureSteps.get(index)));
      }
   }

   @Override
   public MapCodec<AddModifier> codec() {
      return CODEC;
   }

   private List<Holder<PlacedFeature>> add(@Nullable List<Holder<PlacedFeature>> values) {
      return values == null ? this.features.stream().toList() : this.order.add(values, this.features.stream().toList());
   }
}
