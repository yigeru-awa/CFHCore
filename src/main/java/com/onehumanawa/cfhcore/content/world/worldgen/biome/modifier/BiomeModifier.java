package com.onehumanawa.cfhcore.content.world.worldgen.biome.modifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import com.onehumanawa.cfhcore.content.world.registries.CFHWorldGenBuiltInRegistries;

public interface BiomeModifier {
   Codec<BiomeModifier> DIRECT_CODEC = CFHWorldGenBuiltInRegistries.BIOME_MODIFIER_TYPE.byNameCodec().dispatch(BiomeModifier::codec, Function.identity());

   MapCodec<? extends BiomeModifier> codec();
}
