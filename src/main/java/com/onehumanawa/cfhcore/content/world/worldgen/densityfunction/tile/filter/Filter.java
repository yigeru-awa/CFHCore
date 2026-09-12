package com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile.filter;

import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;

public interface Filter {
   void apply(Filterable var1, int var2, int var3, int var4);

   default void iterate(Filterable map, Filter.Visitor visitor) {
      for (int dz = 0; dz < map.getBlockSize().total(); dz++) {
         for (int dx = 0; dx < map.getBlockSize().total(); dx++) {
            Cell cell = map.getCellRaw(dx, dz);
            visitor.visit(map, cell, dx, dz);
         }
      }
   }

   public interface Visitor {
      void visit(Filterable var1, Cell var2, int var3, int var4);
   }
}
