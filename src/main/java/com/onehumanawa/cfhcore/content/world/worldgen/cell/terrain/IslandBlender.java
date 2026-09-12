package com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain;

import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.CellPopulator;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Levels;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.populator.ArchipelagoPopulator;

public class IslandBlender implements CellPopulator {
   private CellPopulator baseTerrain;
   private ArchipelagoPopulator archipelago;
   private Levels levels;

   public IslandBlender(CellPopulator baseTerrain, ArchipelagoPopulator archipelago, Levels levels) {
      this.baseTerrain = baseTerrain;
      this.archipelago = archipelago;
      this.levels = levels;
   }

   @Override
   public void apply(Cell cell, float x, float z) {
      this.baseTerrain.apply(cell, x, z);
      if (cell.terrain == null || !cell.terrain.isOverground() || cell.terrain.isCoast()) {
         this.archipelago.apply(cell, x, z);
      }
   }
}
