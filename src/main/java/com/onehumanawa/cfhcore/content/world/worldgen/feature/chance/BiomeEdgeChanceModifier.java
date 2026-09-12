package com.onehumanawa.cfhcore.content.world.worldgen.feature.chance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import com.onehumanawa.cfhcore.content.world.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.world.worldgen.CFHWorldGenRandomState;
import com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile.Tile;

class BiomeEdgeChanceModifier extends RangeChanceModifier {
   public static final MapCodec<BiomeEdgeChanceModifier> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               Codec.FLOAT.fieldOf("from").forGetter(o -> o.from),
               Codec.FLOAT.fieldOf("to").forGetter(o -> o.to),
               Codec.BOOL.fieldOf("exclusive").forGetter(o -> o.exclusive)
            )
            .apply(instance, BiomeEdgeChanceModifier::new)
   );

   public BiomeEdgeChanceModifier(float from, float to, boolean exclusive) {
      super(from, to, exclusive);
   }

   @Override
   public MapCodec<BiomeEdgeChanceModifier> codec() {
      return CODEC;
   }

   @Override
   protected float getValue(ChanceContext chanceCtx, FeaturePlaceContext<?> placeCtx) {
      BlockPos pos = placeCtx.origin();
      GeneratorContext generatorContext;
      if (placeCtx.level().getLevel().getChunkSource().randomState() instanceof CFHWorldGenRandomState cfhWorldGenRandomState
         && (generatorContext = cfhWorldGenRandomState.generatorContext()) != null) {
         int x = pos.getX();
         int z = pos.getZ();
         int chunkX = SectionPos.blockToSectionCoord(x);
         int chunkZ = SectionPos.blockToSectionCoord(z);
         Tile.Chunk chunk = generatorContext.cache.provideAtChunk(chunkX, chunkZ).getChunkReader(chunkX, chunkZ);
         return chunk.getCell(x, z).biomeRegionEdge;
      }

      throw new UnsupportedOperationException();
   }
}
