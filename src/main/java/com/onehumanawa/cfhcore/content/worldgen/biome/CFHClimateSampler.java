package com.onehumanawa.cfhcore.content.worldgen.biome;

import net.minecraft.core.BlockPos;

public interface CFHClimateSampler {
	void setSpawnSearchCenter(BlockPos center);
	
	BlockPos getSpawnSearchCenter();
}
