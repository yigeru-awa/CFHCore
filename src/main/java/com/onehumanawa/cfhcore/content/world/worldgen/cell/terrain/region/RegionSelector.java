package com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.region;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.CellPopulator;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.populator.WeightedPopulator;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.NoiseUtil;

public class RegionSelector implements CellPopulator {
   private int maxIndex;
   private CellPopulator[] nodes;

   public RegionSelector(List<CellPopulator> populators) {
      this.nodes = getWeightedArray(populators);
      this.maxIndex = this.nodes.length - 1;
   }

   @Override
   public void apply(Cell cell, float x, float y) {
      this.get(cell.terrainRegionId).apply(cell, x, y);
   }

   public CellPopulator get(float identity) {
      int index = NoiseUtil.round(identity * (float)this.maxIndex);
      return this.nodes[index];
   }

   private static CellPopulator[] getWeightedArray(List<CellPopulator> modules) {
      float smallest = Float.MAX_VALUE;

      for (CellPopulator p : modules) {
         if (p instanceof WeightedPopulator) {
            WeightedPopulator tp = (WeightedPopulator)p;
            if (tp.weight() != 0.0F) {
               smallest = Math.min(smallest, tp.weight());
            }
         } else {
            smallest = Math.min(smallest, 1.0F);
         }
      }

      if (smallest == Float.MAX_VALUE) {
         return modules.toArray(CellPopulator[]::new);
      } else {
         List<CellPopulator> result = new LinkedList<>();
         Iterator var8 = modules.iterator();

         while (true) {
            int count;
            CellPopulator p2;
            while (true) {
               if (!var8.hasNext()) {
                  if (result.isEmpty()) {
                     return modules.toArray(CellPopulator[]::new);
                  }

                  return result.toArray(CellPopulator[]::new);
               }

               p2 = (CellPopulator)var8.next();
               if (p2 instanceof WeightedPopulator tp2) {
                  if (tp2.weight() == 0.0F) {
                     continue;
                  }

                  count = Math.round(tp2.weight() / smallest);
                  break;
               }

               count = Math.round(1.0F / smallest);
               break;
            }

            while (count-- > 0) {
               result.add(p2);
            }
         }
      }
   }
}
