package com.onehumanawa.cfhcore.content.world.worldgen.feature.chance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import com.onehumanawa.cfhcore.content.world.registries.CFHWorldGenBuiltInRegistries;

public interface ChanceModifier {
   Codec<ChanceModifier> CODEC = CFHWorldGenBuiltInRegistries.CHANCE_MODIFIER_TYPE.byNameCodec().dispatch(ChanceModifier::codec, Function.identity());

   float getChance(ChanceContext var1, FeaturePlaceContext<?> var2);

   MapCodec<? extends ChanceModifier> codec();
}
