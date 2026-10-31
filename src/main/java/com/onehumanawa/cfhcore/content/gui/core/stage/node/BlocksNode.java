package com.onehumanawa.cfhcore.content.gui.core.stage.node;

import com.onehumanawa.cfhcore.content.gui.core.stage.StageLevel;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class BlocksNode extends MeshedBlocksNode {
   private final int sizeX;
   private final int sizeY;
   private final int sizeZ;

   public BlocksNode(StageLevel level, int sizeX, int sizeY, int sizeZ) {
      super(level);
      this.sizeX = Math.max(1, sizeX);
      this.sizeY = Math.max(1, sizeY);
      this.sizeZ = Math.max(1, sizeZ);
      this.pickable(false);
      this.hoverFeel(0.0F, 1.0F);
      this.named("blocks");
   }

   public boolean inside(int x, int y, int z) {
      return x >= 0 && y >= 0 && z >= 0 && x < this.sizeX && y < this.sizeY && z < this.sizeZ;
   }

   public BlocksNode put(int x, int y, int z, BlockState state) {
      return this.put(x, y, z, state, (CompoundTag)null);
   }

   public BlocksNode put(int x, int y, int z, BlockState state, @Nullable CompoundTag nbt) {
      if (this.inside(x, y, z)) {
         this.set(x, y, z, state, nbt);
      }

      return this;
   }

   public BlocksNode put(int x, int y, int z, Block block) {
      return this.put(x, y, z, block.defaultBlockState(), (CompoundTag)null);
   }

   public BlocksNode putStatic(int x, int y, int z, BlockState state) {
      if (this.inside(x, y, z)) {
         this.setStatic(x, y, z, state);
      }

      return this;
   }

   public BlocksNode softLit(boolean on) {
      super.softLit(on);
      return this;
   }

   public BlocksNode fill(int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
      BlockState state = block.defaultBlockState();

      for(int x = Math.min(x0, x1); x <= Math.max(x0, x1); ++x) {
         for(int y = Math.min(y0, y1); y <= Math.max(y0, y1); ++y) {
            for(int z = Math.min(z0, z1); z <= Math.max(z0, z1); ++z) {
               this.put(x, y, z, state, (CompoundTag)null);
            }
         }
      }

      return this;
   }

   public BlockState at(int x, int y, int z) {
      return this.get(x, y, z);
   }

   public int top(int x, int z) {
      for(int y = this.sizeY - 1; y >= 0; --y) {
         if (!this.get(x, y, z).isAir()) {
            return y;
         }
      }

      return -1;
   }

   public BlocksNode done() {
      this.finishFill(new Vec3i(this.sizeX, this.sizeY, this.sizeZ));
      return this;
   }

   public BlocksNode centered() {
      super.centered();
      return this;
   }
}
