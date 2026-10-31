package com.onehumanawa.cfhcore.content.gui.core.stage.builds;

import com.onehumanawa.cfhcore.content.gui.core.stage.StageRegistries;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public final class BuildVolume {
   private static final BlockState AIR;
   private static final int SHIFT = 4;
   private static final int EDGE = 16;
   private static final int MASK = 15;
   private static final int SECTION = 4096;
   private final int sizeX;
   private final int sizeY;
   private final int sizeZ;
   private final int secX;
   private final int secZ;
   private final BlockState[] palette;
   private final short[][] sections;
   private final List<Entity> entities;
   private final int blocks;
   private @Nullable byte[][] light;
   private int grass;
   private int foliage;
   private int water;
   private final int originX;
   private final int originY;
   private final int originZ;
   private final @Nullable Land land;
   private volatile @Nullable Map<Long, Entity> entityAt;
   private final Map<Long, Optional<BlockEntity>> made;
   private volatile @Nullable boolean[] solid;
   static final long MAX_CELLS = 268435456L;

   private BuildVolume(int sizeX, int sizeY, int sizeZ, BlockState[] palette, short[][] sections, List<Entity> entities, int blocks, int originX, int originY, int originZ) {
      this(sizeX, sizeY, sizeZ, palette, sections, entities, blocks, originX, originY, originZ, (Land)null);
   }

   private BuildVolume(int sizeX, int sizeY, int sizeZ, BlockState[] palette, short[][] sections, List<Entity> entities, int blocks, int originX, int originY, int originZ, @Nullable Land land) {
      this.made = new ConcurrentHashMap();
      this.sizeX = sizeX;
      this.sizeY = sizeY;
      this.sizeZ = sizeZ;
      this.secX = sections(sizeX);
      this.secZ = sections(sizeZ);
      this.palette = palette;
      this.sections = sections;
      this.entities = List.copyOf(entities);
      this.blocks = blocks;
      this.originX = originX;
      this.originY = originY;
      this.originZ = originZ;
      this.land = land;
   }

   public static BuildVolume land(Land land, int sizeY, int originX, int originY, int originZ) {
      int columns = 0;

      for(short g : land.ground) {
         if (g > 0) {
            ++columns;
         }
      }

      return new BuildVolume(land.sizeX, Math.max(1, sizeY), land.sizeZ, BuildVolume.Land.palette(), new short[0][], List.of(), columns, originX, originY, originZ, land);
   }

   public boolean isLand() {
      return this.land != null;
   }

   private int landIndex(Land l, int x, int y, int z) {
      int i = z * l.sizeX + x;
      int g = l.ground[i];
      if (g <= 0) {
         return 0;
      } else if (y < g) {
         int c = l.cover[i];
         if (y == g - 1) {
            return 4 + c * 2;
         } else {
            return y >= g - 4 ? 5 + c * 2 : 3;
         }
      } else {
         int leaves = l.canopy[i];
         if (leaves > 0 && g >= l.sea && y < g + leaves) {
            return 2;
         } else {
            return y < l.sea ? 1 : 0;
         }
      }
   }

   private static int sections(int size) {
      return size + 15 >> 4;
   }

   private int section(int x, int y, int z) {
      return ((y >> 4) * this.secZ + (z >> 4)) * this.secX + (x >> 4);
   }

   private static int cell(int x, int y, int z) {
      return (y & 15) << 8 | (z & 15) << 4 | x & 15;
   }

   public int originX() {
      return this.originX;
   }

   public int originY() {
      return this.originY;
   }

   public int originZ() {
      return this.originZ;
   }

   public int sizeX() {
      return this.sizeX;
   }

   public int sizeY() {
      return this.sizeY;
   }

   public int sizeZ() {
      return this.sizeZ;
   }

   public int blocks() {
      return this.blocks;
   }

   public List<Entity> entities() {
      return this.entities;
   }

   public @Nullable BlockEntity blockEntity(BlockPos pos) {
      if (!this.entities.isEmpty() && this.inside(pos.getX(), pos.getY(), pos.getZ())) {
         Map<Long, Entity> at = this.entityAt;
         if (at == null) {
            Map<Long, Entity> m = new HashMap(this.entities.size() * 2);

            for(Entity e : this.entities) {
               m.put(e.pos().asLong(), e);
            }

            at = m;
            this.entityAt = m;
         }

         Entity e = (Entity)at.get(pos.asLong());
         return e != null && e.nbt() != null ? (BlockEntity)((Optional)this.made.computeIfAbsent(pos.asLong(), (k) -> Optional.ofNullable(BuildEntities.make(e.pos(), e.state(), e.nbt())))).orElse(null) : null;
      } else {
         return null;
      }
   }

   public boolean inside(int x, int y, int z) {
      return x >= 0 && y >= 0 && z >= 0 && x < this.sizeX && y < this.sizeY && z < this.sizeZ;
   }

   public BlockState get(int x, int y, int z) {
      if (!this.inside(x, y, z)) {
         return AIR;
      } else if (this.land != null) {
         return this.palette[this.landIndex(this.land, x, y, z)];
      } else {
         short[] s = this.sections[this.section(x, y, z)];
         return s == null ? AIR : this.palette[s[cell(x, y, z)]];
      }
   }

   public boolean emptyBetween(int x0, int y0, int z0, int x1, int y1, int z1) {
      int ax = Math.max(0, x0);
      int ay = Math.max(0, y0);
      int az = Math.max(0, z0);
      int bx = Math.min(this.sizeX, x1);
      int by = Math.min(this.sizeY, y1);
      int bz = Math.min(this.sizeZ, z1);
      if (ax < bx && ay < by && az < bz) {
         if (this.land != null) {
            for(int z = az; z < bz; ++z) {
               for(int x = ax; x < bx; ++x) {
                  int i = z * this.land.sizeX + x;
                  int g = this.land.ground[i];
                  if (g > 0) {
                     int top = Math.max(g + (g >= this.land.sea ? this.land.canopy[i] : 0), this.land.sea);
                     if (top > ay) {
                        return false;
                     }
                  }
               }
            }

            return true;
         } else {
            for(int sy = ay >> 4; sy <= by - 1 >> 4; ++sy) {
               for(int sz = az >> 4; sz <= bz - 1 >> 4; ++sz) {
                  for(int sx = ax >> 4; sx <= bx - 1 >> 4; ++sx) {
                     short[] s = this.sections[(sy * this.secZ + sz) * this.secX + sx];
                     if (s != null) {
                        int ox = sx << 4;
                        int oy = sy << 4;
                        int oz = sz << 4;

                        for(int y = Math.max(ay, oy); y < Math.min(by, oy + 16); ++y) {
                           for(int z = Math.max(az, oz); z < Math.min(bz, oz + 16); ++z) {
                              for(int x = Math.max(ax, ox); x < Math.min(bx, ox + 16); ++x) {
                                 if (s[cell(x, y, z)] != 0) {
                                    return false;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            return true;
         }
      } else {
         return true;
      }
   }

   public int light(int x, int y, int z) {
      byte[][] l = this.light;
      if (l != null && this.inside(x, y, z)) {
         byte[] s = l[this.section(x, y, z)];
         return s == null ? 0 : s[cell(x, y, z)];
      } else {
         return 0;
      }
   }

   private int index(int x, int y, int z) {
      short[] s = this.sections[this.section(x, y, z)];
      return s == null ? 0 : s[cell(x, y, z)];
   }

   private boolean[] solid() {
      boolean[] s = this.solid;
      if (s == null) {
         s = new boolean[this.palette.length];

         for(int i = 0; i < s.length; ++i) {
            try {
               s[i] = this.palette[i].isSolidRender(BuildVolume.EmptyGetter.INSTANCE, BlockPos.ZERO);
            } catch (RuntimeException var4) {
               s[i] = false;
            }
         }

         this.solid = s;
      }

      return s;
   }

   public boolean clearLine(float sx, float sy, float sz, float ex, float ey, float ez) {
      if (this.land != null) {
         return this.clearOverLand(this.land, sx, sy, sz, ex, ey, ez);
      } else {
         boolean[] opaque = this.solid();
         float dx = ex - sx;
         float dy = ey - sy;
         float dz = ez - sz;
         float length = (float)Math.sqrt((double)(dx * dx + dy * dy + dz * dz));
         if (length < 1.0E-4F) {
            return true;
         } else {
            dx /= length;
            dy /= length;
            dz /= length;
            float end = Math.min(length, exit(sx, sy, sz, dx, dy, dz, 0.0F, 0.0F, 0.0F, (float)this.sizeX, (float)this.sizeY, (float)this.sizeZ));
            int startX = (int)Math.floor((double)sx);
            int startY = (int)Math.floor((double)sy);
            int startZ = (int)Math.floor((double)sz);
            float t = 0.0F;
            int guard = 0;

            label140:
            while(t < end && guard++ < 100000) {
               float x = sx + dx * t;
               float y = sy + dy * t;
               float z = sz + dz * t;
               int cx = (int)Math.floor((double)x);
               int cy = (int)Math.floor((double)y);
               int cz = (int)Math.floor((double)z);
               if (!this.inside(cx, cy, cz)) {
                  float enter = this.enter(sx, sy, sz, dx, dy, dz, t);
                  if (enter < 0.0F) {
                     return true;
                  }

                  t = enter + 0.001F;
               } else {
                  int si = this.section(cx, cy, cz);
                  if (this.sections[si] == null) {
                     int ox = cx & -16;
                     int oy = cy & -16;
                     int oz = cz & -16;
                     t = exit(sx, sy, sz, dx, dy, dz, (float)ox, (float)oy, (float)oz, (float)(ox + 16), (float)(oy + 16), (float)(oz + 16)) + 0.001F;
                  } else {
                     int stepX = dx > 0.0F ? 1 : -1;
                     int stepY = dy > 0.0F ? 1 : -1;
                     int stepZ = dz > 0.0F ? 1 : -1;
                     float tdx = dx != 0.0F ? Math.abs(1.0F / dx) : Float.MAX_VALUE;
                     float tdy = dy != 0.0F ? Math.abs(1.0F / dy) : Float.MAX_VALUE;
                     float tdz = dz != 0.0F ? Math.abs(1.0F / dz) : Float.MAX_VALUE;
                     float tx = dx != 0.0F ? t + (dx > 0.0F ? (float)(cx + 1) - x : x - (float)cx) * tdx : Float.MAX_VALUE;
                     float ty = dy != 0.0F ? t + (dy > 0.0F ? (float)(cy + 1) - y : y - (float)cy) * tdy : Float.MAX_VALUE;
                     float tz = dz != 0.0F ? t + (dz > 0.0F ? (float)(cz + 1) - z : z - (float)cz) * tdz : Float.MAX_VALUE;
                     short[] s = this.sections[si];
                     int sxs = cx >> 4;
                     int sys = cy >> 4;
                     int szs = cz >> 4;

                     while(cx == startX && cy == startY && cz == startZ || !opaque[s[cell(cx, cy, cz)]]) {
                        float next;
                        if (tx <= ty && tx <= tz) {
                           cx += stepX;
                           next = tx;
                           tx += tdx;
                        } else if (ty <= tz) {
                           cy += stepY;
                           next = ty;
                           ty += tdy;
                        } else {
                           cz += stepZ;
                           next = tz;
                           tz += tdz;
                        }

                        if (next >= end) {
                           return true;
                        }

                        if (cx >> 4 != sxs || cy >> 4 != sys || cz >> 4 != szs || !this.inside(cx, cy, cz)) {
                           t = next + 1.0E-4F;
                           continue label140;
                        }
                     }

                     return false;
                  }
               }
            }

            return true;
         }
      }
   }

   private boolean clearOverLand(Land l, float sx, float sy, float sz, float ex, float ey, float ez) {
      float dx = ex - sx;
      float dy = ey - sy;
      float dz = ez - sz;
      float flat = (float)Math.sqrt((double)(dx * dx + dz * dz));
      if (flat < 1.0E-4F) {
         return true;
      } else {
         int cx = (int)Math.floor((double)sx);
         int cz = (int)Math.floor((double)sz);
         int stepX = dx > 0.0F ? 1 : -1;
         int stepZ = dz > 0.0F ? 1 : -1;
         float tdx = dx != 0.0F ? Math.abs(1.0F / dx) : Float.MAX_VALUE;
         float tdz = dz != 0.0F ? Math.abs(1.0F / dz) : Float.MAX_VALUE;
         float tx = dx != 0.0F ? (dx > 0.0F ? (float)(cx + 1) - sx : sx - (float)cx) * tdx : Float.MAX_VALUE;
         float tz = dz != 0.0F ? (dz > 0.0F ? (float)(cz + 1) - sz : sz - (float)cz) * tdz : Float.MAX_VALUE;
         float t = 0.0F;
         int guard = 0;

         while(true) {
            if (t < 1.0F && guard++ < 1000000) {
               float next = Math.min(tx, tz);
               if (tx <= tz) {
                  cx += stepX;
                  tx += tdx;
               } else {
                  cz += stepZ;
                  tz += tdz;
               }

               t = next;
               if (next >= 1.0F) {
                  return true;
               }

               if (cx >= 0 && cz >= 0 && cx < l.sizeX && cz < l.sizeZ) {
                  int g = l.ground[cz * l.sizeX + cx];
                  if (g > 0) {
                     float out = Math.min(Math.min(tx, tz), 1.0F);
                     float low = Math.min(sy + dy * next, sy + dy * out);
                     if (low < (float)g) {
                        return false;
                     }
                  }
                  continue;
               }

               return true;
            }

            return true;
         }
      }
   }

   private static float exit(float px, float py, float pz, float dx, float dy, float dz, float x0, float y0, float z0, float x1, float y1, float z1) {
      float t = Float.MAX_VALUE;
      if (dx > 0.0F) {
         t = Math.min(t, (x1 - px) / dx);
      } else if (dx < 0.0F) {
         t = Math.min(t, (x0 - px) / dx);
      }

      if (dy > 0.0F) {
         t = Math.min(t, (y1 - py) / dy);
      } else if (dy < 0.0F) {
         t = Math.min(t, (y0 - py) / dy);
      }

      if (dz > 0.0F) {
         t = Math.min(t, (z1 - pz) / dz);
      } else if (dz < 0.0F) {
         t = Math.min(t, (z0 - pz) / dz);
      }

      return Math.max(0.0F, t);
   }

   private float enter(float px, float py, float pz, float dx, float dy, float dz, float from) {
      float lo = from;
      float hi = Float.MAX_VALUE;
      float[] p = new float[]{px, py, pz};
      float[] d = new float[]{dx, dy, dz};
      float[] max = new float[]{(float)this.sizeX, (float)this.sizeY, (float)this.sizeZ};

      for(int a = 0; a < 3; ++a) {
         if (Math.abs(d[a]) < 1.0E-9F) {
            if (p[a] < 0.0F || p[a] > max[a]) {
               return -1.0F;
            }
         } else {
            float t0 = (0.0F - p[a]) / d[a];
            float t1 = (max[a] - p[a]) / d[a];
            if (t0 > t1) {
               float swap = t0;
               t0 = t1;
               t1 = swap;
            }

            lo = Math.max(lo, t0);
            hi = Math.min(hi, t1);
            if (lo > hi) {
               return -1.0F;
            }
         }
      }

      return lo;
   }

   public boolean lit() {
      return this.light != null;
   }

   public BuildVolume tint(int grass, int foliage, int water) {
      this.grass = grass & 16777215;
      this.foliage = foliage & 16777215;
      this.water = water & 16777215;
      return this;
   }

   public BuildVolume bakeLight() {
      if (this.land != null) {
         return this;
      } else {
         byte[][] l = new byte[this.sections.length][];
         IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
         int[] block = new int[this.palette.length];
         int[] emits = new int[this.palette.length];

         for(int i = 0; i < this.palette.length; ++i) {
            BlockState s = this.palette[i];
            emits[i] = s.getLightEmission();

            int b;
            try {
               b = s.getLightBlock(BuildVolume.EmptyGetter.INSTANCE, BlockPos.ZERO);
            } catch (RuntimeException var21) {
               b = s.canOcclude() ? 15 : 0;
            }

            block[i] = b;
         }

         int secY = sections(this.sizeY);

         for(int sy = 0; sy < secY; ++sy) {
            for(int sz = 0; sz < this.secZ; ++sz) {
               for(int sx = 0; sx < this.secX; ++sx) {
                  short[] s = this.sections[(sy * this.secZ + sz) * this.secX + sx];
                  if (s != null) {
                     for(int c = 0; c < 4096; ++c) {
                        int e = emits[s[c]];
                        if (e > 0) {
                           int x = (sx << 4) + (c & 15);
                           int z = (sz << 4) + (c >> 4 & 15);
                           int y = (sy << 4) + (c >> 8);
                           if (this.inside(x, y, z)) {
                              this.lit(l, x, y, z, e);
                              queue.enqueue((y * this.sizeZ + z) * this.sizeX + x);
                           }
                        }
                     }
                  }
               }
            }
         }

         int layer = this.sizeX * this.sizeZ;

         while(!queue.isEmpty()) {
            int i = queue.dequeueInt();
            int x = i % this.sizeX;
            int z = i / this.sizeX % this.sizeZ;
            int y = i / layer;
            int level = this.lightIn(l, x, y, z);
            if (level > 1) {
               for(Direction d : Direction.values()) {
                  int nx = x + d.getStepX();
                  int ny = y + d.getStepY();
                  int nz = z + d.getStepZ();
                  if (nx >= 0 && ny >= 0 && nz >= 0 && nx < this.sizeX && ny < this.sizeY && nz < this.sizeZ) {
                     int opacity = block[this.index(nx, ny, nz)];
                     if (opacity < 15) {
                        int next = level - Math.max(1, opacity);
                        if (next > this.lightIn(l, nx, ny, nz)) {
                           this.lit(l, nx, ny, nz, next);
                           queue.enqueue((ny * this.sizeZ + nz) * this.sizeX + nx);
                        }
                     }
                  }
               }
            }
         }

         this.light = l;
         return this;
      }
   }

   private int lightIn(byte[][] l, int x, int y, int z) {
      byte[] s = l[this.section(x, y, z)];
      return s == null ? 0 : s[cell(x, y, z)];
   }

   private void lit(byte[][] l, int x, int y, int z, int level) {
      int si = this.section(x, y, z);
      byte[] s = l[si];
      if (s == null) {
         s = l[si] = new byte[4096];
      }

      s[cell(x, y, z)] = (byte)level;
   }

   public BlockAndTintGetter view(boolean flatShade) {
      return new View(this, flatShade);
   }

   static {
      AIR = Blocks.AIR.defaultBlockState();
   }

   public static record Entity(BlockPos pos, BlockState state, @Nullable CompoundTag nbt) {
   }

   public static final class Land {
      public static final int GRASS = 0;
      public static final int SAND = 1;
      public static final int STONE = 2;
      public static final int SNOW = 3;
      public static final int GRAVEL = 4;
      public static final int DIRT = 5;
      private static final String[][] COVERS = new String[][]{{"minecraft:grass_block", "minecraft:dirt"}, {"minecraft:sand", "minecraft:sandstone"}, {"minecraft:stone", "minecraft:stone"}, {"minecraft:snow_block", "minecraft:stone"}, {"minecraft:gravel", "minecraft:stone"}, {"minecraft:coarse_dirt", "minecraft:dirt"}};
      final int sizeX;
      final int sizeZ;
      final short[] ground;
      final byte[] cover;
      final byte[] canopy;
      final int sea;

      public Land(int sizeX, int sizeZ, int sea) {
         this.sizeX = sizeX;
         this.sizeZ = sizeZ;
         this.ground = new short[sizeX * sizeZ];
         this.cover = new byte[sizeX * sizeZ];
         this.canopy = new byte[sizeX * sizeZ];
         this.sea = Math.max(0, sea);
      }

      public void set(int x, int z, int ground, int cover, int leaves) {
         int i = z * this.sizeX + x;
         this.ground[i] = (short)Math.max(0, Math.min(32767, ground));
         this.cover[i] = (byte)cover;
         this.canopy[i] = (byte)Math.max(0, Math.min(8, leaves));
      }

      public int ground(int x, int z) {
         return this.ground[z * this.sizeX + x];
      }

      static BlockState[] palette() {
         BlockState[] p = new BlockState[4 + COVERS.length * 2];
         p[0] = BuildVolume.AIR;
         p[1] = Blocks.WATER.defaultBlockState();
         BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState();
         p[2] = leaves.hasProperty(LeavesBlock.PERSISTENT) ? (BlockState)leaves.setValue(LeavesBlock.PERSISTENT, true) : leaves;
         p[3] = Blocks.STONE.defaultBlockState();

         for(int i = 0; i < COVERS.length; ++i) {
            p[4 + i * 2] = state(COVERS[i][0]);
            p[5 + i * 2] = state(COVERS[i][1]);
         }

         return p;
      }

      private static BlockState state(String id) {
         BlockState s = BuildStates.parse(id);
         return s == null ? Blocks.STONE.defaultBlockState() : s;
      }
   }

   public static final class Builder {
      private final int sizeX;
      private final int sizeY;
      private final int sizeZ;
      private final int secX;
      private final int secY;
      private final int secZ;
      private final short[][] sections;
      private final List<BlockState> palette = new ArrayList();
      private final Map<BlockState, Integer> index = new IdentityHashMap();
      private final List<Entity> entities = new ArrayList();

      public Builder(int sizeX, int sizeY, int sizeZ) {
         if ((long)sizeX * (long)sizeY * (long)sizeZ > 268435456L) {
            throw new IllegalArgumentException("a build of " + sizeX + "x" + sizeY + "x" + sizeZ + " is too large for a scene (crop it)");
         } else {
            this.sizeX = Math.max(1, sizeX);
            this.sizeY = Math.max(1, sizeY);
            this.sizeZ = Math.max(1, sizeZ);
            this.secX = BuildVolume.sections(this.sizeX);
            this.secY = BuildVolume.sections(this.sizeY);
            this.secZ = BuildVolume.sections(this.sizeZ);
            this.sections = new short[this.secX * this.secY * this.secZ][];
            this.palette.add(BuildVolume.AIR);
            this.index.put(BuildVolume.AIR, 0);
         }
      }

      private int section(int x, int y, int z) {
         return ((y >> 4) * this.secZ + (z >> 4)) * this.secX + (x >> 4);
      }

      private void each(CellVisitor visit) {
         for(int sy = 0; sy < this.secY; ++sy) {
            for(int sz = 0; sz < this.secZ; ++sz) {
               for(int sx = 0; sx < this.secX; ++sx) {
                  short[] s = this.sections[(sy * this.secZ + sz) * this.secX + sx];
                  if (s != null) {
                     for(int c = 0; c < 4096; ++c) {
                        if (s[c] != 0) {
                           visit.cell((sx << 4) + (c & 15), (sy << 4) + (c >> 8), (sz << 4) + (c >> 4 & 15), s[c]);
                        }
                     }
                  }
               }
            }
         }

      }

      public int sizeX() {
         return this.sizeX;
      }

      public int sizeY() {
         return this.sizeY;
      }

      public int sizeZ() {
         return this.sizeZ;
      }

      public boolean inside(int x, int y, int z) {
         return x >= 0 && y >= 0 && z >= 0 && x < this.sizeX && y < this.sizeY && z < this.sizeZ;
      }

      public void set(int x, int y, int z, BlockState state, @Nullable CompoundTag nbt) {
         if (this.inside(x, y, z)) {
            int si = this.section(x, y, z);
            int c = BuildVolume.cell(x, y, z);
            short[] s = this.sections[si];
            if (s != null && s[c] != 0 && ((BlockState)this.palette.get(s[c])).hasBlockEntity()) {
               this.entities.removeIf((e) -> e.pos().getX() == x && e.pos().getY() == y && e.pos().getZ() == z);
            }

            if (state.isAir()) {
               if (s != null) {
                  s[c] = 0;
               }

            } else {
               Integer id = (Integer)this.index.get(state);
               if (id == null) {
                  if (this.palette.size() >= 32767) {
                     return;
                  }

                  id = this.palette.size();
                  this.palette.add(state);
                  this.index.put(state, id);
               }

               if (s == null) {
                  s = this.sections[si] = new short[4096];
               }

               s[c] = (short)(int)id;
               if (state.hasBlockEntity()) {
                  this.entities.add(new Entity(new BlockPos(x, y, z), state, nbt));
               }

            }
         }
      }

      public BlockState get(int x, int y, int z) {
         if (!this.inside(x, y, z)) {
            return BuildVolume.AIR;
         } else {
            short[] s = this.sections[this.section(x, y, z)];
            return s == null ? BuildVolume.AIR : (BlockState)this.palette.get(s[BuildVolume.cell(x, y, z)]);
         }
      }

      public @Nullable int[] bounds() {
         int[] b = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, -1, -1, -1};
         this.each((x, y, z, p) -> {
            if (x < b[0]) {
               b[0] = x;
            }

            if (y < b[1]) {
               b[1] = y;
            }

            if (z < b[2]) {
               b[2] = z;
            }

            if (x > b[3]) {
               b[3] = x;
            }

            if (y > b[4]) {
               b[4] = y;
            }

            if (z > b[5]) {
               b[5] = z;
            }

         });
         return b[3] < 0 ? null : b;
      }

      public Builder crop(int x0, int y0, int z0, int x1, int y1, int z1) {
         Builder c = new Builder(x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1);
         this.each((x, y, z, px) -> {
            if (x >= x0 && x <= x1 && y >= y0 && y <= y1 && z >= z0 && z <= z1) {
               c.set(x - x0, y - y0, z - z0, (BlockState)this.palette.get(px), (CompoundTag)null);
            }

         });

         for(Entity e : this.entities) {
            BlockPos p = e.pos();
            if (p.getX() >= x0 && p.getX() <= x1 && p.getY() >= y0 && p.getY() <= y1 && p.getZ() >= z0 && p.getZ() <= z1) {
               c.entities.removeIf((o) -> o.pos().equals(p.offset(-x0, -y0, -z0)));
               c.entities.add(new Entity(p.offset(-x0, -y0, -z0), e.state(), e.nbt()));
            }
         }

         return c;
      }

      public BuildVolume build(int originX, int originY, int originZ) {
         int[] blocks = new int[]{0};
         this.each((x, y, z, p) -> {
            int var10002 = blocks[0]++;
         });

         for(int i = 0; i < this.sections.length; ++i) {
            short[] s = this.sections[i];
            if (s != null) {
               boolean empty = true;

               for(short c : s) {
                  if (c != 0) {
                     empty = false;
                     break;
                  }
               }

               if (empty) {
                  this.sections[i] = null;
               }
            }
         }

         return new BuildVolume(this.sizeX, this.sizeY, this.sizeZ, (BlockState[])this.palette.toArray(new BlockState[0]), this.sections, this.entities, blocks[0], originX, originY, originZ);
      }
   }

   private static final class View implements BlockAndTintGetter {
      private final BuildVolume v;
      private final boolean flat;
      private @Nullable LevelLightEngine engine;

      View(BuildVolume v, boolean flat) {
         this.v = v;
         this.flat = flat;
      }

      public float getShade(Direction direction, boolean shade) {
         if (!this.flat && shade) {
            float var10000;
            switch (direction) {
               case DOWN:
                  var10000 = 0.5F;
                  break;
               case UP:
                  var10000 = 1.0F;
                  break;
               case NORTH:
               case SOUTH:
                  var10000 = 0.8F;
                  break;
               default:
                  var10000 = 0.6F;
            }

            return var10000;
         } else {
            return 1.0F;
         }
      }

      public LevelLightEngine getLightEngine() {
         if (this.engine == null) {
            this.engine = new LevelLightEngine(new NoChunks(this), false, false);
         }

         return this.engine;
      }

      public int getBrightness(LightLayer layer, BlockPos pos) {
         return layer == LightLayer.SKY ? 15 : this.v.light(pos.getX(), pos.getY(), pos.getZ());
      }

      public int getRawBrightness(BlockPos pos, int amount) {
         return Math.max(15 - amount, this.v.light(pos.getX(), pos.getY(), pos.getZ()));
      }

      public boolean canSeeSky(BlockPos pos) {
         return true;
      }

      public int getBlockTint(BlockPos pos, ColorResolver resolver) {
         int own = resolver == BiomeColors.GRASS_COLOR_RESOLVER ? this.v.grass : (resolver == BiomeColors.FOLIAGE_COLOR_RESOLVER ? this.v.foliage : (resolver == BiomeColors.WATER_COLOR_RESOLVER ? this.v.water : 0));
         return own != 0 ? own : resolver.getColor((Biome)StageRegistries.biome().value(), (double)pos.getX(), (double)pos.getZ());
      }

      public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
         return this.v.blockEntity(pos);
      }

      public BlockState getBlockState(BlockPos pos) {
         return this.v.get(pos.getX(), pos.getY(), pos.getZ());
      }

      public FluidState getFluidState(BlockPos pos) {
         return this.getBlockState(pos).getFluidState();
      }

      public int getHeight() {
         return Math.max(16, this.v.sizeY + 16);
      }

      public int getMinBuildHeight() {
         return 0;
      }
   }

   private static record NoChunks(BlockGetter level) implements LightChunkGetter {
      public @Nullable LightChunk getChunkForLighting(int x, int z) {
         return null;
      }

      public BlockGetter getLevel() {
         return this.level;
      }
   }

   private static enum EmptyGetter implements BlockGetter {
      INSTANCE;

      public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
         return null;
      }

      public BlockState getBlockState(BlockPos pos) {
         return BuildVolume.AIR;
      }

      public FluidState getFluidState(BlockPos pos) {
         return BuildVolume.AIR.getFluidState();
      }

      public int getHeight() {
         return 384;
      }

      public int getMinBuildHeight() {
         return -64;
      }
   }

   @FunctionalInterface
   private interface CellVisitor {
      void cell(int var1, int var2, int var3, int var4);
   }
}
