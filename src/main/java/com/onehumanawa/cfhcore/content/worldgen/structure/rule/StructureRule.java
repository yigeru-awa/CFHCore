package com.onehumanawa.cfhcore.content.worldgen.structure.rule;

import java.util.function.Function;

import com.mojang.serialization.Codec;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.RandomState;
import com.onehumanawa.cfhcore.content.worldgen.registries.CFHBuiltInRegistries;

public interface StructureRule {
    public static final Codec<StructureRule> DIRECT_CODEC = CFHBuiltInRegistries.STRUCTURE_RULE_TYPE.byNameCodec().dispatch(StructureRule::codec, Function.identity());

	boolean test(RandomState randomState, BlockPos pos);
	
	MapCodec<? extends StructureRule> codec();
}
