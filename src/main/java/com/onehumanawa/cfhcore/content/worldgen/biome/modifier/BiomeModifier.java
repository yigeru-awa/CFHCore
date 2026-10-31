package com.onehumanawa.cfhcore.content.worldgen.biome.modifier;

import java.util.function.Function;

import com.mojang.serialization.Codec;

import com.mojang.serialization.MapCodec;
import com.onehumanawa.cfhcore.content.worldgen.registries.CFHBuiltInRegistries;
import com.onehumanawa.cfhcore.content.worldgen.structure.rule.StructureRule;

public interface BiomeModifier {
    public static final Codec<BiomeModifier> DIRECT_CODEC = CFHBuiltInRegistries.BIOME_MODIFIER_TYPE.byNameCodec().dispatch(BiomeModifier::codec, Function.identity());

	MapCodec<? extends BiomeModifier> codec();
}
