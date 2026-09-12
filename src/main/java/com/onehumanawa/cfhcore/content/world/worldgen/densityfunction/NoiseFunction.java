package com.onehumanawa.cfhcore.content.world.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;

public record NoiseFunction(Holder<Noise> noise, int seed) implements MarkerFunction.Mapped {
   @Override
   public double compute(FunctionContext ctx) {
      return (double)this.noise.value().compute((float)ctx.blockX(), (float)ctx.blockZ(), this.seed);
   }

   @Override
   public double minValue() {
      return (double)this.noise.value().minValue();
   }

   @Override
   public double maxValue() {
      return (double)this.noise.value().maxValue();
   }

   public static record Marker(Holder<Noise> noise) implements MarkerFunction {
      public static final MapCodec<NoiseFunction.Marker> CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(Noise.CODEC.fieldOf("noise").forGetter(NoiseFunction.Marker::noise)).apply(instance, NoiseFunction.Marker::new)
      );

      @Override
      public KeyDispatchDataCodec<NoiseFunction.Marker> codec() {
         return new KeyDispatchDataCodec<>(CODEC);
      }

      @Override
      public DensityFunction mapAll(Visitor visitor) {
         DensityFunction self = visitor instanceof Noise.Visitor noiseVisitor
            ? new NoiseFunction.Marker(Holder.direct(this.noise.value().mapAll(noiseVisitor)))
            : this;
         return visitor.apply(self);
      }
   }
}
