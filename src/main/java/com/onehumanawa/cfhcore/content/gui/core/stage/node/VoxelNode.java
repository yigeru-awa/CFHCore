package com.onehumanawa.cfhcore.content.gui.core.stage.node;

import com.onehumanawa.cfhcore.content.gui.core.stage.StageLevel;
import java.util.Random;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class VoxelNode extends MeshedBlocksNode {
   private VoxelNode(StageLevel level) {
      super(level);
   }

   public static VoxelNode planet(StageLevel level, int radius, long seed) {
      VoxelNode n = new VoxelNode(level);
      int r = Mth.clamp(radius, 3, 24);
      int d = r * 2 + 1;
      Noise noise = new Noise(seed);
      Random rnd = new Random(seed * 31L + 7L);
      float freq = 1.6F / (float)r;

      for(int x = 0; x < d; ++x) {
         for(int y = 0; y < d; ++y) {
            for(int z = 0; z < d; ++z) {
               float fx = (float)(x - r);
               float fy = (float)(y - r);
               float fz = (float)(z - r);
               float dist = (float)Math.sqrt((double)(fx * fx + fy * fy + fz * fz)) / (float)r;
               float bump = noise.fbm(fx * freq, fy * freq, fz * freq) * 0.12F;
               float shell = dist + bump;
               if (!(shell > 1.0F)) {
                  float lat = fy / (float)r;
                  BlockState state;
                  if (shell > 0.9F) {
                     float m = noise.fbm(fx * freq * 2.3F + 11.0F, fy * freq * 2.3F, fz * freq * 2.3F + 5.0F);
                     if (Math.abs(lat) > 0.74F) {
                        state = Blocks.SNOW_BLOCK.defaultBlockState();
                     } else if (m < -0.22F) {
                        state = Blocks.WATER.defaultBlockState();
                     } else if (m < -0.12F) {
                        state = Blocks.SAND.defaultBlockState();
                     } else {
                        state = Blocks.GRASS_BLOCK.defaultBlockState();
                     }
                  } else if (shell > 0.74F) {
                     state = Blocks.DIRT.defaultBlockState();
                  } else {
                     state = Blocks.STONE.defaultBlockState();
                  }

                  n.set(x, y, z, state, (CompoundTag)null);
               }
            }
         }
      }

      for(int x = 0; x < d; ++x) {
         for(int y = 0; y < d; ++y) {
            for(int z = 0; z < d; ++z) {
               if (n.get(x, y, z).getBlock() == Blocks.WATER && (y == 0 || n.get(x, y - 1, z).isAir())) {
                  n.set(x, y, z, Blocks.SAND.defaultBlockState(), (CompoundTag)null);
               }
            }
         }
      }

      int trees = 0;

      for(int attempt = 0; attempt < 60 && trees < Math.max(1, r / 3); ++attempt) {
         int x = r + rnd.nextInt(Math.max(1, r)) - r / 2;
         int z = r + rnd.nextInt(Math.max(1, r)) - r / 2;

         for(int y = d - 1; y > r; --y) {
            if (n.get(x, y, z).getBlock() == Blocks.GRASS_BLOCK && n.get(x, y + 1, z).isAir()) {
               n.tree(x, y + 1, z, rnd, d);
               ++trees;
               break;
            }
         }
      }

      n.finishFill(new Vec3i(d, d + 5, d));
      n.meshOffset((float)(-r), (float)(-r), (float)(-r));
      n.bounds((float)(-r) - 0.5F, (float)(-r) - 0.5F, (float)(-r) - 0.5F, (float)r + 0.5F, (float)r + 0.5F, (float)r + 0.5F);
      n.named("planet");
      return n;
   }

   public static VoxelNode heightfield(StageLevel level, int width, int depth, long seed) {
      VoxelNode n = new VoxelNode(level);
      int w = Mth.clamp(width, 4, 64);
      int dp = Mth.clamp(depth, 4, 64);
      Noise noise = new Noise(seed);
      Random rnd = new Random(seed * 17L + 3L);
      int sea = 3;
      int maxH = 0;

      for(int x = 0; x < w; ++x) {
         for(int z = 0; z < dp; ++z) {
            float e = noise.fbm((float)x * 0.11F, 0.0F, (float)z * 0.11F) * 3.5F + noise.fbm((float)x * 0.3F + 9.0F, 0.0F, (float)z * 0.3F) * 1.2F;
            int h = Mth.clamp(Math.round(4.0F + e), 1, 12);
            maxH = Math.max(maxH, h);

            for(int y = 0; y < h; ++y) {
               Block b = y == h - 1 ? (h <= 3 ? Blocks.SAND : Blocks.GRASS_BLOCK) : (y >= h - 3 ? Blocks.DIRT : Blocks.STONE);
               n.set(x, y, z, b.defaultBlockState(), (CompoundTag)null);
            }

            for(int y = h; y < 3; ++y) {
               n.set(x, y, z, Blocks.WATER.defaultBlockState(), (CompoundTag)null);
            }

            if (h > 3 && h < 9) {
               float f = rnd.nextFloat();
               if (f < 0.1F) {
                  n.set(x, h, z, Blocks.SHORT_GRASS.defaultBlockState(), (CompoundTag)null);
               } else if (f < 0.13F) {
                  n.set(x, h, z, Blocks.POPPY.defaultBlockState(), (CompoundTag)null);
               } else if (f < 0.16F) {
                  n.set(x, h, z, Blocks.DANDELION.defaultBlockState(), (CompoundTag)null);
               }
            }
         }
      }

      int trees = 0;

      for(int attempt = 0; attempt < 40 && trees < Math.max(1, w * dp / 60); ++attempt) {
         int x = 2 + rnd.nextInt(Math.max(1, w - 4));
         int z = 2 + rnd.nextInt(Math.max(1, dp - 4));

         for(int y = 12; y > 3; --y) {
            if (n.get(x, y, z).getBlock() == Blocks.GRASS_BLOCK && n.get(x, y + 1, z).isAir()) {
               n.tree(x, y + 1, z, rnd, 64);
               ++trees;
               break;
            }
         }
      }

      n.finishFill(new Vec3i(w, maxH + 7, dp));
      n.named("terrain");
      return n;
   }

   private void tree(int x, int y, int z, Random rnd, int limit) {
      int h = 3 + rnd.nextInt(2);

      for(int i = 0; i < h; ++i) {
         this.set(x, y + i, z, Blocks.OAK_LOG.defaultBlockState(), (CompoundTag)null);
      }

      BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState();

      for(int dx = -2; dx <= 2; ++dx) {
         for(int dz = -2; dz <= 2; ++dz) {
            for(int dy = h - 2; dy <= h; ++dy) {
               int lx = x + dx;
               int ly = y + dy;
               int lz = z + dz;
               if (lx >= 0 && lz >= 0 && lx < limit && lz < limit && ly >= 0) {
                  int ring = Math.max(Math.abs(dx), Math.abs(dz));
                  if ((ring != 2 || dy != h && rnd.nextInt(4) != 0) && (ring != 2 || Math.abs(dx) != 2 || Math.abs(dz) != 2) && this.get(lx, ly, lz).isAir()) {
                     this.set(lx, ly, lz, leaves, (CompoundTag)null);
                  }
               }
            }
         }
      }

      this.set(x, y + h, z, leaves, (CompoundTag)null);
      if (rnd.nextBoolean()) {
         this.set(x, y + h + 1, z, leaves, (CompoundTag)null);
      }

   }

   static final class Noise {
      private final long seed;

      Noise(long seed) {
         this.seed = seed;
      }

      private float hash(int x, int y, int z) {
         long h = this.seed ^ (long)x * 374761393L ^ (long)y * 668265263L ^ (long)z * 2147483647L;
         h = (h ^ h >>> 13) * 1274126177L;
         h ^= h >>> 16;
         return (float)(h & 65535L) / 65535.0F * 2.0F - 1.0F;
      }

      float value(float x, float y, float z) {
         int x0 = Mth.floor(x);
         int y0 = Mth.floor(y);
         int z0 = Mth.floor(z);
         float tx = smooth(x - (float)x0);
         float ty = smooth(y - (float)y0);
         float tz = smooth(z - (float)z0);
         float c00 = Mth.lerp(tx, this.hash(x0, y0, z0), this.hash(x0 + 1, y0, z0));
         float c10 = Mth.lerp(tx, this.hash(x0, y0 + 1, z0), this.hash(x0 + 1, y0 + 1, z0));
         float c01 = Mth.lerp(tx, this.hash(x0, y0, z0 + 1), this.hash(x0 + 1, y0, z0 + 1));
         float c11 = Mth.lerp(tx, this.hash(x0, y0 + 1, z0 + 1), this.hash(x0 + 1, y0 + 1, z0 + 1));
         return Mth.lerp(tz, Mth.lerp(ty, c00, c10), Mth.lerp(ty, c01, c11));
      }

      float fbm(float x, float y, float z) {
         return this.value(x, y, z) * 0.65F + this.value(x * 2.1F + 3.0F, y * 2.1F, z * 2.1F - 2.0F) * 0.35F;
      }

      private static float smooth(float t) {
         return t * t * (3.0F - 2.0F * t);
      }
   }
}
