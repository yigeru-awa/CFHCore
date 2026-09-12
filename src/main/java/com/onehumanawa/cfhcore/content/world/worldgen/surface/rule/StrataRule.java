package com.onehumanawa.cfhcore.content.world.worldgen.surface.rule;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import net.minecraft.world.level.levelgen.SurfaceRules.SurfaceRule;
import org.jetbrains.annotations.Nullable;
import com.onehumanawa.cfhcore.content.world.worldgen.CFHWorldGenRandomState;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.NoiseUtil;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noises;
import com.onehumanawa.cfhcore.content.world.worldgen.surface.CFHWorldGenSurfaceSystem;

public record StrataRule(ResourceLocation name, Holder<Noise> selector, List<StrataRule.Strata> strata, int iterations) implements RuleSource {
   public static final MapCodec<StrataRule> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               ResourceLocation.CODEC.fieldOf("name").forGetter(StrataRule::name),
               Noise.CODEC.fieldOf("selector").forGetter(StrataRule::selector),
               StrataRule.Strata.CODEC.listOf().fieldOf("strata").forGetter(StrataRule::strata),
               Codec.INT.fieldOf("iterations").forGetter(StrataRule::iterations)
            )
            .apply(instance, StrataRule::new)
   );

   public StrataRule(ResourceLocation name, Holder<Noise> selector, List<StrataRule.Strata> strata, int iterations) {
      List<StrataRule.Strata> var5 = ImmutableList.copyOf(strata);
      this.name = name;
      this.selector = selector;
      this.strata = var5;
      this.iterations = iterations;
   }

   public StrataRule.Source apply(Context ctx) {
      if (ctx.system instanceof CFHWorldGenSurfaceSystem cfhWorldGenSurfaceSystem && ctx.randomState instanceof CFHWorldGenRandomState cfhWorldGenRandomState) {
         return new StrataRule.Source(ctx, cfhWorldGenRandomState.seed(this.selector.value()), cfhWorldGenSurfaceSystem.getOrCreateStrata(this.name, this::generateStrata));
      }

      throw new IllegalStateException();
   }

   @Override
   public KeyDispatchDataCodec<StrataRule> codec() {
      return new KeyDispatchDataCodec<>(CODEC);
   }

   private List<List<StrataRule.Layer>> generateStrata(RandomSource random) {
      List<List<StrataRule.Layer>> layers = new ArrayList<>();

      for (int i = 0; i < this.iterations; i++) {
         List<StrataRule.Layer> layer = new ArrayList<>();

         for (StrataRule.Strata strata : this.strata) {
            layer.addAll(strata.generateLayers(random));
         }

         layers.add(layer);
      }

      return layers;
   }

   public static record Layer(BlockState material, Noise depth, int seed) {
      public float computeDepth(float x, float z) {
         return this.depth.compute(x, z, this.seed);
      }
   }

   private class Source implements SurfaceRule {
      private Context surfaceContext;
      private Noise selector;
      private List<List<StrataRule.Layer>> strata;
      private List<StrataRule.Layer> layers;
      private float[] depthBuffer;
      private long lastUpdateXZ;

      public Source(Context surfaceContext, Noise selector, List<List<StrataRule.Layer>> strata) {
         this.surfaceContext = surfaceContext;
         this.selector = selector;
         this.strata = strata;
         this.lastUpdateXZ = Long.MIN_VALUE;
      }

      @Nullable
      @Override
      public BlockState tryApply(int x, int y, int z) {
         if (this.lastUpdateXZ != this.surfaceContext.lastUpdateXZ) {
            this.initBuffer(x, z);
            this.lastUpdateXZ = this.surfaceContext.lastUpdateXZ;
         }

         StrataRule.Layer last = null;

         for (int i = 0; i < this.layers.size(); i++) {
            StrataRule.Layer layer = last = this.layers.get(i);
            if ((float)y > this.depthBuffer[i]) {
               return layer.material();
            }
         }

         return last != null ? last.material() : null;
      }

      private void initBuffer(int x, int z) {
         this.layers = this.selectLayers(x, z);
         int layerCount = this.layers.size();
         if (this.depthBuffer == null || this.depthBuffer.length < layerCount) {
            this.depthBuffer = new float[layerCount];
         }

         int localX = this.surfaceContext.blockX & 15;
         int localZ = this.surfaceContext.blockZ & 15;
         int height = this.surfaceContext.chunk.getHeight(Types.WORLD_SURFACE_WG, localX, localZ);
         float sum = 0.0F;

         for (int i = 0; i < layerCount; i++) {
            StrataRule.Layer layer = this.layers.get(i);
            float depth = layer.computeDepth((float)x, (float)z);
            sum += depth;
            this.depthBuffer[i] = depth;
         }

         int y = height;

         for (int i = 0; i < layerCount; i++) {
            this.depthBuffer[i] = (float)(y -= Math.round(this.depthBuffer[i] / sum * (float)height));
         }
      }

      private List<StrataRule.Layer> selectLayers(int x, int z) {
         float selector = this.selector.compute((float)x, (float)z, 0);
         int index = (int)(selector * (float)this.strata.size());
         index = Math.min(this.strata.size() - 1, index);
         return this.strata.get(index);
      }
   }

   public static record Strata(TagKey<Block> materials, Holder<Noise> noise, int attempts, int minLayers, int maxLayers, float minDepth, float maxDepth) {
      public static final Codec<StrataRule.Strata> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  TagKey.hashedCodec(Registries.BLOCK).fieldOf("materials").forGetter(StrataRule.Strata::materials),
                  Noise.CODEC.fieldOf("noise").forGetter(StrataRule.Strata::noise),
                  Codec.INT.fieldOf("attempts").forGetter(StrataRule.Strata::attempts),
                  Codec.INT.fieldOf("min_layers").forGetter(StrataRule.Strata::minLayers),
                  Codec.INT.fieldOf("max_layers").forGetter(StrataRule.Strata::maxLayers),
                  Codec.FLOAT.fieldOf("min_depth").forGetter(StrataRule.Strata::minDepth),
                  Codec.FLOAT.fieldOf("max_depth").forGetter(StrataRule.Strata::maxDepth)
               )
               .apply(instance, StrataRule.Strata::new)
      );

      public List<StrataRule.Layer> generateLayers(RandomSource random) {
         int lastIndex = -1;
         int layers = this.minLayers + NoiseUtil.round(random.nextFloat() * (float)(this.maxLayers - this.minLayers));
         List<StrataRule.Layer> result = new ArrayList<>();
         List<Holder<Block>> materials = ImmutableList.copyOf(BuiltInRegistries.BLOCK.getTagOrEmpty(this.materials));
         int seed = random.nextInt();

         for (int i = 0; i < layers; i++) {
            int attempts = this.attempts;
            int index = random.nextInt(materials.size());

            while (--attempts >= 0 && index == lastIndex) {
               index = random.nextInt(materials.size());
            }

            if (index != lastIndex) {
               lastIndex = index;
               BlockState material = materials.get(index).value().defaultBlockState();
               float depth = this.minDepth + random.nextFloat() * (this.maxDepth - this.minDepth);
               result.add(new StrataRule.Layer(material, Noises.shiftSeed(Noises.mul(this.noise.value(), depth), random.nextInt()), seed));
            }
         }

         return result;
      }
   }
}
