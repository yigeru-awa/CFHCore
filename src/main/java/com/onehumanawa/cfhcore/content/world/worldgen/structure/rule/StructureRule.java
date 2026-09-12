package com.onehumanawa.cfhcore.content.world.worldgen.structure.rule;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.RandomState;
import com.onehumanawa.cfhcore.content.world.registries.CFHWorldGenBuiltInRegistries;

public interface StructureRule {
   Codec<StructureRule> DIRECT_CODEC = CFHWorldGenBuiltInRegistries.STRUCTURE_RULE_TYPE.byNameCodec().dispatch(StructureRule::codec, Function.identity());

   boolean test(RandomState var1, BlockPos var2);

   MapCodec<? extends StructureRule> codec();
}
