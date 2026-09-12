package com.onehumanawa.cfhcore.content.world.worldgen.feature.template.template;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

public class TemplateRegion {
   private static final int SIZE = 1;
   private int centerX;
   private int centerZ;
   private int minX;
   private int minZ;
   private int maxX;
   private int maxZ;

   public TemplateRegion init(BlockPos pos) {
      this.centerX = pos.getX() >> 4;
      this.centerZ = pos.getZ() >> 4;
      this.minX = this.centerX - 1;
      this.minZ = this.centerZ - 1;
      this.maxX = this.centerX + 1;
      this.maxZ = this.centerZ + 1;
      return this;
   }

   public boolean containsBlock(LevelAccessor world, BlockPos pos) {
      return this.containsChunk(world, pos.getX() >> 4, pos.getZ() >> 4);
   }

   public boolean containsChunk(LevelAccessor world, int cx, int cz) {
      return cx >= this.minX && cx <= this.maxX && cz >= this.minZ && cz <= this.maxZ;
   }
}
