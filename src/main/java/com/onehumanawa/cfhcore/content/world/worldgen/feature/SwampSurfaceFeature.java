package com.onehumanawa.cfhcore.content.world.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noises;

public class SwampSurfaceFeature extends Feature<SwampSurfaceFeature.Config> {
   private static final Noise MATERIAL_NOISE = makeMaterialNoise();

   public SwampSurfaceFeature(Codec<SwampSurfaceFeature.Config> codec) {
      super(codec);
   }

   @Override
   public boolean place(FeaturePlaceContext<SwampSurfaceFeature.Config> ctx) {
      SwampSurfaceFeature.Config config = ctx.config();
      BlockPos origin = ctx.origin();
      ChunkPos chunkPos = new ChunkPos(origin);
      WorldGenLevel level = ctx.level();
      ChunkGenerator generator = ctx.chunkGenerator();
      MutableBlockPos pos = new MutableBlockPos();
      int waterY = generator.getSeaLevel() - 1;

      for (int x = 0; x < 16; x++) {
         for (int z = 0; z < 16; z++) {
            int worldX = chunkPos.getBlockX(x);
            int worldZ = chunkPos.getBlockZ(z);
            int surfaceY = level.getHeight(Types.WORLD_SURFACE, worldX, worldZ);
            double noise = Biome.BIOME_INFO_NOISE.getValue((double)worldX * 0.25, (double)worldZ * 0.25, false);
            BlockState filler = getMaterial(worldX, waterY, worldZ, waterY, config);
            pos.set(worldX, surfaceY, worldZ);
            if (level.getBiome(pos).is(Biomes.SWAMP)) {
               if (noise > 0.0) {
                  for (int y = surfaceY; y >= surfaceY - 10; y--) {
                     pos.set(worldX, y, worldZ);
                     if (!level.getBlockState(pos).isAir()) {
                        if (y == waterY && !level.getFluidState(pos).isEmpty()) {
                           level.setBlock(pos, filler, 2);
                        }
                        break;
                     }
                  }
               }

               int oceanFloorY = level.getHeight(Types.OCEAN_FLOOR_WG, worldX, worldZ);
               if (oceanFloorY <= waterY) {
                  pos.set(worldX, oceanFloorY, worldZ);
                  level.setBlock(pos, getMaterial(worldX, oceanFloorY, worldZ, waterY, config), 2);
               }
            }
         }
      }

      return true;
   }

   private static BlockState getMaterial(int x, int y, int z, int waterY, SwampSurfaceFeature.Config config) {
      float value = MATERIAL_NOISE.compute((float)x, (float)z, 0);
      if ((double)value > 0.6) {
         return (double)value < 0.75 && y < waterY ? config.clayMaterial() : config.gravelMaterial();
      } else {
         return config.dirtMaterial();
      }
   }

   private static Noise makeMaterialNoise() {
      Noise base = Noises.simplex(23, 40, 2);
      return Noises.warpWhite(base, 213, 2, 4.0F);
   }

   public static record Config(BlockState clayMaterial, BlockState gravelMaterial, BlockState dirtMaterial) implements FeatureConfiguration {
      public static final Codec<SwampSurfaceFeature.Config> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  BlockState.CODEC.fieldOf("clay_material").forGetter(SwampSurfaceFeature.Config::clayMaterial),
                  BlockState.CODEC.fieldOf("gravel_material").forGetter(SwampSurfaceFeature.Config::gravelMaterial),
                  BlockState.CODEC.fieldOf("dirt_material").forGetter(SwampSurfaceFeature.Config::dirtMaterial)
               )
               .apply(instance, SwampSurfaceFeature.Config::new)
      );
   }
}
