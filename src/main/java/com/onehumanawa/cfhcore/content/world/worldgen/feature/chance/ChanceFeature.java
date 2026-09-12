package com.onehumanawa.cfhcore.content.world.worldgen.feature.chance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class ChanceFeature extends Feature<ChanceFeature.Config> {
   public ChanceFeature(Codec<ChanceFeature.Config> codec) {
      super(codec);
   }

   @Override
   public boolean place(FeaturePlaceContext<ChanceFeature.Config> ctx) {
      RandomSource random = ctx.random();
      ChanceFeature.Config config = ctx.config();
      int entryCount = config.entries.size();
      ChanceContext chanceCtx = ChanceContext.make(entryCount);

      for (int i = 0; i < entryCount; i++) {
         ChanceFeature.Entry entry = config.entries.get(i);
         float chance = entry.getChance(chanceCtx, ctx);
         chanceCtx.record(i, chance);
      }

      int index = chanceCtx.nextIndex(random);
      return index > -1 ? config.entries.get(index).feature.value().place(ctx.level(), ctx.chunkGenerator(), random, ctx.origin()) : false;
   }

   public static record Config(List<ChanceFeature.Entry> entries) implements FeatureConfiguration {
      public static final Codec<ChanceFeature.Config> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(ChanceFeature.Entry.CODEC.listOf().fieldOf("entries").forGetter(ChanceFeature.Config::entries))
               .apply(instance, ChanceFeature.Config::new)
      );
   }

   public static record Entry(Holder<PlacedFeature> feature, float chance, List<ChanceModifier> modifiers) {
      public static final Codec<ChanceFeature.Entry> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  PlacedFeature.CODEC.fieldOf("feature").forGetter(ChanceFeature.Entry::feature),
                  Codec.FLOAT.fieldOf("chance").forGetter(ChanceFeature.Entry::chance),
                  ChanceModifier.CODEC.listOf().fieldOf("modifiers").forGetter(ChanceFeature.Entry::modifiers)
               )
               .apply(instance, ChanceFeature.Entry::new)
      );

      public float getChance(ChanceContext chanceCtx, FeaturePlaceContext<?> placeCtx) {
         float chance = this.chance;

         for (ChanceModifier modifier : this.modifiers) {
            chance *= modifier.getChance(chanceCtx, placeCtx);
         }

         return chance;
      }
   }
}
