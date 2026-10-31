package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.set;

import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.BlocksNode;
import java.util.Random;
import net.minecraft.world.level.block.state.BlockState;

public final class Plot {
   private final BlocksNode node;
   private final int minX;
   private final int minY;
   private final int minZ;
   private final int maxX;
   private final int maxZ;
   private final int margin;

   public Plot(Stage stage, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      this(stage, minX, minY, minZ, maxX, maxY, maxZ, 0);
   }

   public Plot(Stage stage, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int margin) {
      this.minX = minX;
      this.minY = minY;
      this.minZ = minZ;
      this.maxX = maxX;
      this.maxZ = maxZ;
      this.margin = Math.max(0, margin);
      this.node = new BlocksNode(stage.level(), maxX - minX + 1 + 2 * this.margin, maxY - minY + 1, maxZ - minZ + 1 + 2 * this.margin);
   }

   public int minX() {
      return this.minX;
   }

   public int maxX() {
      return this.maxX;
   }

   public int minZ() {
      return this.minZ;
   }

   public int maxZ() {
      return this.maxZ;
   }

   public boolean covers(int x, int z) {
      return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
   }

   public Plot put(int x, int y, int z, BlockState state) {
      this.node.put(x - this.minX + this.margin, y - this.minY, z - this.minZ + this.margin, state);
      return this;
   }

   public Plot put(int x, int y, int z, String... names) {
      return this.put(x, y, z, Pick.of(names));
   }

   public Plot model(int x, int y, int z, BlockState state) {
      this.node.putStatic(x - this.minX + this.margin, y - this.minY, z - this.minZ + this.margin, state);
      return this;
   }

   public Plot model(int x, int y, int z, String... names) {
      return this.model(x, y, z, Pick.of(names));
   }

   public Plot fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
      for(int x = Math.min(x0, x1); x <= Math.max(x0, x1); ++x) {
         for(int y = Math.min(y0, y1); y <= Math.max(y0, y1); ++y) {
            for(int z = Math.min(z0, z1); z <= Math.max(z0, z1); ++z) {
               this.put(x, y, z, state);
            }
         }
      }

      return this;
   }

   public Plot fill(int x0, int y0, int z0, int x1, int y1, int z1, String... names) {
      return this.fill(x0, y0, z0, x1, y1, z1, Pick.of(names));
   }

   public Plot fillModel(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
      for(int x = Math.min(x0, x1); x <= Math.max(x0, x1); ++x) {
         for(int y = Math.min(y0, y1); y <= Math.max(y0, y1); ++y) {
            for(int z = Math.min(z0, z1); z <= Math.max(z0, z1); ++z) {
               this.model(x, y, z, state);
            }
         }
      }

      return this;
   }

   public Plot scatter(int x0, int y0, int z0, int x1, int y1, int z1, Random rnd, float chance, BlockState base, BlockState... variants) {
      for(int x = Math.min(x0, x1); x <= Math.max(x0, x1); ++x) {
         for(int y = Math.min(y0, y1); y <= Math.max(y0, y1); ++y) {
            for(int z = Math.min(z0, z1); z <= Math.max(z0, z1); ++z) {
               boolean vary = variants.length > 0 && rnd.nextFloat() < chance;
               this.put(x, y, z, vary ? variants[rnd.nextInt(variants.length)] : base);
            }
         }
      }

      return this;
   }

   public Plot tint(Stage stage, int grass, int foliage, int water) {
      stage.level().tintRegion(this.node.origin(), grass, foliage, water);
      return this;
   }

   public BlockState at(int x, int y, int z) {
      return this.node.at(x - this.minX + this.margin, y - this.minY, z - this.minZ + this.margin);
   }

   public boolean empty(int x, int y, int z) {
      return this.at(x, y, z).isAir();
   }

   public BlocksNode done(Stage stage) {
      this.node.done().softLit(true);
      this.node.rim(this.margin);
      ((BlocksNode)stage.add(this.node)).at((float)(this.minX - this.margin) - 0.5F, (float)this.minY, (float)(this.minZ - this.margin) - 0.5F);
      return this.node;
   }
}
