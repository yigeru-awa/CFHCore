package com.onehumanawa.cfhcore.content.world.worldgen.densityfunction;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction.SimpleFunction;

public interface MarkerFunction extends SimpleFunction {
   @Override
   default double compute(FunctionContext ctx) {
      throw new UnsupportedOperationException();
   }

   @Override
   default double minValue() {
      return Double.NEGATIVE_INFINITY;
   }

   @Override
   default double maxValue() {
      return Double.POSITIVE_INFINITY;
   }

   public interface Mapped extends SimpleFunction {
      @Override
      default KeyDispatchDataCodec<NoiseFunction> codec() {
         throw new UnsupportedOperationException();
      }
   }
}
