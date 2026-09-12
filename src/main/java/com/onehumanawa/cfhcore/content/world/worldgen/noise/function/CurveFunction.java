package com.onehumanawa.cfhcore.content.world.worldgen.noise.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import com.onehumanawa.cfhcore.content.world.registries.CFHWorldGenBuiltInRegistries;

public interface CurveFunction {
   Codec<CurveFunction> CODEC = CFHWorldGenBuiltInRegistries.CURVE_FUNCTION_TYPE.byNameCodec().dispatch(CurveFunction::codec, Function.identity());

   float apply(float var1);

   MapCodec<? extends CurveFunction> codec();
}
