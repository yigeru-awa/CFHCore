package com.onehumanawa.cfhcore.content.world.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Climate.Sampler;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import com.onehumanawa.cfhcore.content.world.worldgen.biome.CFHWorldGenClimateSampler;

@Mixin({Sampler.class})
@Implements({@Interface(
      iface = CFHWorldGenClimateSampler.class,
      prefix = "cfhcore$CFHWorldGenClimateSampler$"
   )})
class MixinClimateSampler {
   private BlockPos spawnSearchCenter = BlockPos.ZERO;

   public void cfhcore$CFHWorldGenClimateSampler$setSpawnSearchCenter(BlockPos spawnSearchCenter) {
      this.spawnSearchCenter = spawnSearchCenter;
   }

   public BlockPos cfhcore$CFHWorldGenClimateSampler$getSpawnSearchCenter() {
      return this.spawnSearchCenter;
   }
}
