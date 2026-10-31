package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.set;

import com.onehumanawa.cfhcore.content.gui.core.stage.StageLevel;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.BlocksNode;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockGear {
   public static final int LAYERS = 3;
   private static final int CORE = 1;
   private static final int LARGE = 21;
   private static final int SMALL = 13;
   private static final float LARGE_REACH = 0.9375F;
   private static final float SMALL_REACH = 0.5625F;
   private static final int NONE = -1;
   private static final int ROOT = 0;
   private static final int TIP = 1;

   private BlockGear() {
   }

   public static float scale(boolean large) {
      return large ? 0.08928572F : 0.086538464F;
   }

   public static BlocksNode build(StageLevel level, boolean large, boolean sceneLight) {
      int n = large ? 21 : 13;
      int c = n / 2;
      BlocksNode gear = new BlocksNode(level, n, 3, n);

      for(int x = 0; x < n; ++x) {
         for(int z = 0; z < n; ++z) {
            if (large) {
               large(gear, x, z, x - c, z - c);
            } else {
               small(gear, x, z, x - c, z - c);
            }
         }
      }

      gear.done().softLit(sceneLight);
      gear.meshOffset((float)(-n) / 2.0F, -1.5F, (float)(-n) / 2.0F);
      return gear;
   }

   private static void large(BlocksNode gear, int x, int z, int dx, int dz) {
      int r2 = dx * dx + dz * dz;
      int ax = Math.abs(dx);
      int az = Math.abs(dz);
      if (r2 <= 5) {
         hub(gear, x, z, dx, dz, r2);
      } else if (r2 < 40) {
         spoke(gear, x, z, dx, dz);
      } else if (r2 <= 73) {
         rim(gear, x, z, dx, dz, r2 == 49 || r2 == 50, false);
      } else {
         tooth(gear, x, z, dx, dz, largeTooth(ax, az));
      }

   }

   private static void small(BlocksNode gear, int x, int z, int dx, int dz) {
      int r2 = dx * dx + dz * dz;
      int ax = Math.abs(dx);
      int az = Math.abs(dz);
      if (r2 <= 2) {
         hub(gear, x, z, dx, dz, r2);
      } else if (r2 <= 20) {
         rim(gear, x, z, dx, dz, r2 == 9, r2 <= 8);
      } else {
         tooth(gear, x, z, dx, dz, smallTooth(ax, az));
      }

   }

   private static int largeTooth(int ax, int az) {
      int far = Math.max(ax, az);
      int near = Math.min(ax, az);
      if (near == 0) {
         return far == 9 ? 0 : (far == 10 ? 1 : -1);
      } else if (near == 4) {
         return far == 8 ? 0 : (far == 9 ? 1 : -1);
      } else if (far == 7) {
         return near == 6 ? 0 : (near == 7 ? 1 : -1);
      } else {
         return -1;
      }
   }

   private static int smallTooth(int ax, int az) {
      int far = Math.max(ax, az);
      int near = Math.min(ax, az);
      if (near == 0) {
         return far == 5 ? 0 : (far == 6 ? 1 : -1);
      } else if (far == 4) {
         return near == 3 ? 0 : (near == 4 ? 1 : -1);
      } else {
         return -1;
      }
   }

   private static void hub(BlocksNode gear, int x, int z, int dx, int dz, int r2) {
      if (r2 != 0) {
         gear.put(x, 1, z, mix(dx, dz, "dark_oak_planks", "dark_oak_planks", "stripped_dark_oak_wood"));
         if (r2 == 1) {
            plates(gear, x, z, "dark_oak_trapdoor");
         } else if (r2 == 2) {
            pegs(gear, x, z, "spruce_button");
         } else {
            slabs(gear, x, z, "dark_oak_slab");
         }

      } else {
         for(int y = 0; y < 3; ++y) {
            gear.put(x, y, z, Pick.of("stripped_dark_oak_log[axis=y]"));
         }

      }
   }

   private static void spoke(BlocksNode gear, int x, int z, int dx, int dz) {
      int ax = Math.abs(dx);
      int az = Math.abs(dz);
      if (dx != 0 && dz != 0) {
         if (ax == az) {
            gear.put(x, 1, z, Pick.of("dark_oak_planks"));
         } else if (ax + az == 7 && Math.min(ax, az) == 3) {
            String towardsHubX = dx > 0 ? "west" : "east";
            String awayX = dx > 0 ? "east" : "west";
            String towardsHubZ = dz > 0 ? "north" : "south";
            String awayZ = dz > 0 ? "south" : "north";
            String rails = ax > az ? towardsHubX + "=true," + awayZ + "=true" : towardsHubZ + "=true," + awayX + "=true";
            gear.put(x, 1, z, Pick.of("dark_oak_fence[" + rails + "]"));
         }
      } else {
         gear.put(x, 1, z, Pick.of("stripped_spruce_log[axis=" + (dz == 0 ? "x" : "z") + "]"));
      }

   }

   private static void rim(BlocksNode gear, int x, int z, int dx, int dz, boolean spokeEnd, boolean sunk) {
      gear.put(x, 1, z, mix(dx, dz, "spruce_planks", "spruce_planks", "spruce_planks", "stripped_spruce_wood", "stripped_spruce_wood", "dark_oak_planks"));
      if (!spokeEnd || dx != 0 && dz != 0 && Math.abs(dx) != Math.abs(dz)) {
         if (!sunk && (sector(dx, dz) & 1) == 0) {
            slabs(gear, x, z, pick(dx, dz, 4) == 0 ? "dark_oak_slab" : "spruce_slab");
         } else {
            plates(gear, x, z, "spruce_trapdoor");
         }
      } else {
         pegs(gear, x, z, "dark_oak_button");
      }

   }

   private static void tooth(BlocksNode gear, int x, int z, int dx, int dz, int part) {
      if (part != -1) {
         boolean diagonal = Math.abs(Math.abs(dx) - Math.abs(dz)) <= 1;
         String wood = diagonal ? "stripped_spruce_wood" : "stripped_spruce_log[axis=" + (Math.abs(dx) > Math.abs(dz) ? "x" : "z") + "]";
         gear.put(x, 1, z, Pick.of(wood));
         if (part == 0) {
            slabs(gear, x, z, "spruce_slab");
         }

      }
   }

   private static void slabs(BlocksNode gear, int x, int z, String slab) {
      gear.put(x, 2, z, Pick.of(slab + "[type=bottom]"));
      gear.put(x, 0, z, Pick.of(slab + "[type=top]"));
   }

   private static void plates(BlocksNode gear, int x, int z, String trapdoor) {
      gear.put(x, 2, z, Pick.of(trapdoor + "[half=bottom,open=false]"));
      gear.put(x, 0, z, Pick.of(trapdoor + "[half=top,open=false]"));
   }

   private static void pegs(BlocksNode gear, int x, int z, String button) {
      gear.put(x, 2, z, Pick.of(button + "[face=floor,facing=north]"));
      gear.put(x, 0, z, Pick.of(button + "[face=ceiling,facing=north]"));
   }

   private static int sector(int dx, int dz) {
      double turn = Math.atan2((double)dz, (double)dx) / (Math.PI * 2D) + (double)1.0F;
      return (int)Math.floor(turn * (double)16.0F + (double)0.5F) % 16;
   }

   private static BlockState mix(int dx, int dz, String... blocks) {
      return Pick.of(blocks[pick(dx, dz, blocks.length)]);
   }

   private static int pick(int dx, int dz, int n) {
      int h = dx * 374761393 + dz * 668265263;
      h = (h ^ h >>> 13) * 1274126177;
      h ^= h >>> 16;
      return Math.floorMod(h, n);
   }
}
