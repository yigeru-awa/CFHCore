package com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile.filter;

import com.onehumanawa.cfhcore.content.world.preset.settings.FilterSettings;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Levels;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.NoiseUtil;

public record Smoothing(float smoothingRadius, float smoothingRate, Modifier modifier) implements Filter {
   @Override
   public void apply(Filterable map, int seedX, int seedZ, int iterations) {
      while (iterations-- > 0) {
         this.apply(map);
      }
   }

   private void apply(Filterable cellMap) {
      int radius = NoiseUtil.round(this.smoothingRadius + 0.5F);
      float radiusSq = this.smoothingRadius * this.smoothingRadius;
      int diameter = radius * 2 + 1;
      float[] kernel = new float[diameter * diameter];

      for (int dz = -radius; dz <= radius; dz++) {
         for (int dx = -radius; dx <= radius; dx++) {
            float dist2 = (float)(dx * dx + dz * dz);
            kernel[(dz + radius) * diameter + dx + radius] = dist2 <= radiusSq ? 1.0F - dist2 / radiusSq : -1.0F;
         }
      }

      int maxZ = cellMap.getBlockSize().total() - radius;
      int maxX = cellMap.getBlockSize().total() - radius;

      for (int z = radius; z < maxZ; z++) {
         for (int x = radius; x < maxX; x++) {
            Cell cell = cellMap.getCellRaw(x, z);
            if (!cell.erosionMask) {
               float total = 0.0F;
               float weights = 0.0F;

               for (int dz = -radius; dz <= radius; dz++) {
                  for (int dx = -radius; dx <= radius; dx++) {
                     float weight = kernel[(dz + radius) * diameter + dx + radius];
                     if (weight >= 0.0F) {
                        int px = x + dx;
                        int pz = z + dz;
                        Cell neighbour = cellMap.getCellRaw(px, pz);
                        if (!neighbour.isAbsent()) {
                           total += neighbour.height * weight;
                           weights += weight;
                        }
                     }
                  }
               }

               if (weights > 0.0F) {
                  float dif = cell.height - total / weights;
                  cell.height = cell.height - this.modifier.modify(cell, dif * this.smoothingRate);
               }
            }
         }
      }
   }

   public static Smoothing make(FilterSettings.Smoothing settings, Levels levels) {
      return new Smoothing(settings.smoothingRadius, settings.smoothingRate, Modifier.range(levels.ground(1), levels.ground(120)).invert());
   }
}
