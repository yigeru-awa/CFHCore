package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.pause;

import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.BlocksNode;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import java.util.ArrayDeque;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class Diorama {
   public static final int SIDE = 4;
   public static final int BACK = 5;
   public static final int FRONT = 2;
   public static final int DEEP = 3;
   public static final int HIGH = 6;
   private static final int ROOF = 8;
   static final int WIDTH = 9;
   static final int LENGTH = 8;
   static final int HEIGHT = 9;

   public static @Nullable Piece cut(Stage stage, LocalPlayer player, ClientLevel level, float round) {
      return cut(stage, player, level, round, player.getY());
   }

   public static @Nullable Piece cut(Stage stage, LocalPlayer player, ClientLevel level, float round, double feetY) {
      try {
         return build(stage, player, level, round, feetY);
      } catch (RuntimeException e) {
         CFHGuiMenu.LOGGER.warn("[CFHGui Menu] the piece of the world round the player could not be made, left out: {}", e.toString());
         return null;
      }
   }

   private static Piece build(Stage stage, LocalPlayer player, ClientLevel level, float round, double feetY) {
      float viewX = -Mth.sin((float)Math.toRadians((double)round));
      float viewZ = -Mth.cos((float)Math.toRadians((double)round));
      Rotation var10000;
      switch (player.getDirection()) {
         case EAST -> var10000 = Rotation.COUNTERCLOCKWISE_90;
         case SOUTH -> var10000 = Rotation.CLOCKWISE_180;
         case WEST -> var10000 = Rotation.CLOCKWISE_90;
         default -> var10000 = Rotation.NONE;
      }

      Rotation turn = var10000;
      switch (turn) {
         case CLOCKWISE_90 -> var10000 = Rotation.COUNTERCLOCKWISE_90;
         case COUNTERCLOCKWISE_90 -> var10000 = Rotation.CLOCKWISE_90;
         default -> var10000 = turn;
      }

      Rotation back = var10000;
      int px = Mth.floor(player.getX());
      int pz = Mth.floor(player.getZ());
      int base = Mth.ceil(feetY - 1.0E-4);
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
      int top = 5;
      boolean roofed = false;

      for(int y = 2; y < 6; ++y) {
         pos.set(px, base + y, pz);
         if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            top = y - 1;
            roofed = true;
            break;
         }
      }

      boolean[][][] filled = new boolean[9][7][5];
      boolean[][] covered = new boolean[9][5];

      for(int lx = -4; lx <= 4; ++lx) {
         for(int lz = -5; lz < 0; ++lz) {
            BlockPos offset = (new BlockPos(lx, 0, lz)).rotate(back);

            for(int ly = 0; ly <= top; ++ly) {
               pos.set(px + offset.getX(), base + ly, pz + offset.getZ());
               filled[lx + 4][ly][lz + 5] = !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
            }

            if (roofed) {
               for(int ly = top + 1; ly <= top + 8; ++ly) {
                  pos.set(px + offset.getX(), base + ly, pz + offset.getZ());
                  if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                     covered[lx + 4][lz + 5] = true;
                     break;
                  }
               }
            }
         }
      }

      BlockState[][][] kept = new BlockState[9][9][8];
      ArrayDeque<int[]> reach = new ArrayDeque();

      for(int lx = -4; lx <= 4; ++lx) {
         for(int lz = -5; lz <= 2; ++lz) {
            BlockPos offset = (new BlockPos(lx, 0, lz)).rotate(back);

            for(int ly = -3; ly <= top; ++ly) {
               pos.set(px + offset.getX(), base + ly, pz + offset.getZ());
               BlockState state = level.getBlockState(pos);
               if (!state.isAir() && wanted(level, pos, state, lx, ly, lz) && drawable(state) && (!roofed || ly < 0 || lz >= 0 || !hides(filled, covered, lx, ly, lz, viewX, viewZ))) {
                  kept[lx + 4][ly + 3][lz + 5] = state;
                  if (ly < 0) {
                     reach.add(new int[]{lx + 4, ly + 3, lz + 5});
                  }
               }
            }
         }
      }

      boolean[][][] standing = new boolean[9][9][8];

      for(int[] seed : reach) {
         standing[seed[0]][seed[1]][seed[2]] = true;
      }

      while(!reach.isEmpty()) {
         int[] at = (int[])reach.poll();

         for(Direction d : Direction.values()) {
            int x = at[0] + d.getStepX();
            int y = at[1] + d.getStepY();
            int z = at[2] + d.getStepZ();
            if (x >= 0 && y >= 0 && z >= 0 && x < 9 && y < 9 && z < 8 && !standing[x][y][z] && kept[x][y][z] != null) {
               standing[x][y][z] = true;
               reach.add(new int[]{x, y, z});
            }
         }
      }

      BlocksNode land = new BlocksNode(stage.level(), 9, 9, 8);
      int placed = 0;
      int lowest = 0;
      int highest = -3;

      for(int lx = -4; lx <= 4; ++lx) {
         for(int lz = -5; lz <= 2; ++lz) {
            BlockPos offset = (new BlockPos(lx, 0, lz)).rotate(back);

            for(int ly = -3; ly <= top; ++ly) {
               BlockState state = kept[lx + 4][ly + 3][lz + 5];
               if (state != null && standing[lx + 4][ly + 3][lz + 5]) {
                  pos.set(px + offset.getX(), base + ly, pz + offset.getZ());
                  if (modelOnly(state)) {
                     land.putStatic(lx + 4, ly + 3, lz + 5, state.rotate(turn));
                  } else {
                     land.put(lx + 4, ly + 3, lz + 5, state.rotate(turn), data(level, pos, state));
                  }

                  ++placed;
                  lowest = Math.min(lowest, ly);
                  highest = Math.max(highest, ly);
               }
            }
         }
      }

      if (placed == 0) {
         land.done();
         land.dispose();
         return null;
      } else {
         BlockPos at = player.blockPosition();
         stage.level().tintRegion(land.origin(), BiomeColors.getAverageGrassColor(level, at), BiomeColors.getAverageFoliageColor(level, at), BiomeColors.getAverageWaterColor(level, at));
         land.done();
         return new Piece(land, (float)(feetY - (double)base), (float)lowest, (float)highest + 1.0F);
      }
   }

   private static boolean wanted(ClientLevel level, BlockPos pos, BlockState state, int lx, int ly, int lz) {
      if (ly >= 0 && lz >= 0) {
         if (ly <= 0 && (lx != 0 || lz != 0)) {
            return state.getFluidState().isEmpty() && state.getCollisionShape(level, pos).isEmpty() && level.getBlockState(pos.above()).isAir();
         } else {
            return false;
         }
      } else {
         return true;
      }
   }

   private static boolean filled(boolean[][][] filled, int lx, int ly, int lz) {
      return lx >= -4 && lx <= 4 && lz >= -5 && lz < 0 && ly >= 0 && ly <= 6 && filled[lx + 4][ly][lz + 5];
   }

   private static boolean wall(boolean[][][] filled, int lx, int ly, int lz) {
      return filled(filled, lx, ly, lz) && (ly >= 1 || filled(filled, lx, ly + 1, lz));
   }

   private static boolean hides(boolean[][][] filled, boolean[][] covered, int lx, int ly, int lz, float viewX, float viewZ) {
      if (!wall(filled, lx, ly, lz)) {
         return false;
      } else {
         float edge = viewX < 0.0F ? -0.5F : 0.5F;

         for(int ray = 0; ray < 2; ++ray) {
            float x0 = (float)lx + (ray == 0 ? 0.0F : edge);
            float z0 = (float)lz;

            for(float t = 0.5F; t <= 9.0F; t += 0.5F) {
               int cx = Math.round(x0 + viewX * t);
               int cz = Math.round(z0 + viewZ * t);
               if (cx != lx || cz != lz) {
                  if (cx < -4 || cx > 4 || cz < -5 || cz >= 0) {
                     break;
                  }

                  if (!wall(filled, cx, ly, cz)) {
                     if (covered[cx + 4][cz + 5]) {
                        return true;
                     }
                     break;
                  }
               }
            }
         }

         return false;
      }
   }

   private static boolean drawable(BlockState state) {
      return vanilla(state) || state.getRenderShape() == RenderShape.MODEL;
   }

   private static boolean modelOnly(BlockState state) {
      return !vanilla(state) && state.hasBlockEntity();
   }

   private static boolean vanilla(BlockState state) {
      return "minecraft".equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
   }

   private static @Nullable CompoundTag data(ClientLevel level, BlockPos pos, BlockState state) {
      if (!state.hasBlockEntity()) {
         return null;
      } else {
         try {
            BlockEntity be = level.getBlockEntity(pos);
            return be == null ? null : be.saveWithId(level.registryAccess());
         } catch (RuntimeException var4) {
            return null;
         }
      }
   }

   private Diorama() {
   }

   public static record Piece(BlocksNode land, float feet, float bottom, float top) {
   }
}
