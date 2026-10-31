package com.onehumanawa.cfhcore.content.worldgen.mixin;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Climate;
import com.onehumanawa.cfhcore.content.worldgen.biome.CFHClimateSampler;

@Mixin(Climate.Sampler.class)
@Implements(@Interface(iface = CFHClimateSampler.class, prefix = "cfhcore$CFHClimateSampler$"))
class MixinClimateSampler {
	private BlockPos spawnSearchCenter = BlockPos.ZERO;
	
	public void cfhcore$CFHClimateSampler$setSpawnSearchCenter(BlockPos spawnSearchCenter) {
		this.spawnSearchCenter = spawnSearchCenter;
	}
	
	public BlockPos cfhcore$CFHClimateSampler$getSpawnSearchCenter() {
		return this.spawnSearchCenter;
	}
}
