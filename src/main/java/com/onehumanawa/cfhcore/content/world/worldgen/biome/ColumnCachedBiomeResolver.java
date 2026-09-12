package com.onehumanawa.cfhcore.content.world.worldgen.biome;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate.Sampler;

public class ColumnCachedBiomeResolver implements BiomeResolver {
   private final BiomeResolver resolver;
   private final int skyQuartY;
   private final Holder<Biome>[] cache = new Holder[16];

   public ColumnCachedBiomeResolver(BiomeResolver resolver, int skyQuartY) {
      this.resolver = resolver;
      this.skyQuartY = skyQuartY;
   }

   @Override
   public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Sampler sampler) {
      if (quartY >= this.skyQuartY) {
         int index = (quartX & 3) << 2 | quartZ & 3;
         Holder<Biome> biome = this.cache[index];
         if (biome == null) {
            this.cache[index] = biome = this.resolver.getNoiseBiome(quartX, this.skyQuartY, quartZ, sampler);
         }

         return biome;
      } else {
         return this.resolver.getNoiseBiome(quartX, quartY, quartZ, sampler);
      }
   }
}
