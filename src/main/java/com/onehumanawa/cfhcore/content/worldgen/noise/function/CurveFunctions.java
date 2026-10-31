package com.onehumanawa.cfhcore.content.worldgen.noise.function;

import com.mojang.serialization.Codec;

import com.mojang.serialization.MapCodec;
import com.onehumanawa.cfhcore.content.worldgen.platform.RegistryUtil;
import com.onehumanawa.cfhcore.content.worldgen.registries.CFHBuiltInRegistries;

public class CurveFunctions {

	public static void bootstrap() {
		register("interpolation", Interpolation.CODEC);
		register("scurve", SCurveFunction.CODEC);
	}

	public static CurveFunction scurve(float lower, float upper) {
		return new SCurveFunction(lower, upper);
	}
	
	private static void register(String name, MapCodec<? extends CurveFunction> value) {
		RegistryUtil.register(CFHBuiltInRegistries.CURVE_FUNCTION_TYPE, name, value);
	}
}
