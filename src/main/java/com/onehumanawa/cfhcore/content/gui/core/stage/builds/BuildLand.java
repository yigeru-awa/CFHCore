package com.onehumanawa.cfhcore.content.gui.core.stage.builds;

import java.util.Arrays;
import java.util.Random;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

final class BuildLand {
   private static final float KEEP_RISE = 0.06F;
   private static final float KEEP_REACH = 160.0F;

   private BuildLand() {
   }

   static Ring[] rings(BuildSpec.Land land, int sizeX, int sizeZ) {
      int[][] spec = land.rings();
      int n = spec.length;
      Ring[] out = new Ring[n];
      int ix = 0;
      int iz = 0;
      int iw = sizeX;
      int id = sizeZ;
      int reached = 0;

      for(int k = 0; k < n; ++k) {
         int cell = Math.max(1, spec[k][0]);
         int next = k + 1 < n ? Math.max(1, spec[k + 1][0]) : 1;
         int margin = Math.max(cell, roundUp(Math.max(0, spec[k][1] - reached), cell));
         int unit = lcm(cell, next);
         int w = roundUp(iw + 2 * margin, unit);
         int d = roundUp(id + 2 * margin, unit);
         int x0 = ix - margin;
         int z0 = iz - margin;
         Ring r = new Ring(k, cell, x0, z0, w / cell, d / cell, (ix - x0) / cell, (iz - z0) / cell, (ix - x0 + iw) / cell, (iz - z0 + id) / cell);
         out[k] = r;
         ix = x0;
         iz = z0;
         iw = w;
         id = d;
         reached += margin;
      }

      return out;
   }

   private static int roundUp(int v, int m) {
      return (v + m - 1) / m * m;
   }

   private static int lcm(int a, int b) {
      return a / gcd(a, b) * b;
   }

   private static int gcd(int a, int b) {
      return b == 0 ? a : gcd(b, a % b);
   }

   static Placed generate(BuildSpec spec, BuildVolume build, int index) {
      BuildSpec.Land land = spec.land();
      Ring[] rings = rings(land, build.sizeX(), build.sizeZ());
      Ring ring = rings[Math.min(index, rings.length - 1)];
      Rim rim = land.under() ? null : rim(build);
      Noise noise = new Noise(land.seed());
      Keep keep = keep(spec, build);
      int cell = ring.cell();
      float[] heights = new float[ring.columnsX() * ring.columnsZ()];
      float low = Float.MAX_VALUE;
      float high = -Float.MAX_VALUE;

      for(int cz = 0; cz < ring.columnsZ(); ++cz) {
         for(int cx = 0; cx < ring.columnsX(); ++cx) {
            int i = cz * ring.columnsX() + cx;
            boolean inner = cx >= ring.holeX0() && cx < ring.holeX1() && cz >= ring.holeZ0() && cz < ring.holeZ1();
            if (!inner || land.under() && ring.index() == 0) {
               float bx = (float)(ring.x0() + cx * cell);
               float bz = (float)(ring.z0() + cz * cell);
               float h;
               if (cell == 1) {
                  h = height(land, rim, noise, build, keep, bx + 0.5F, bz + 0.5F);
               } else {
                  h = Math.min(Math.min(height(land, rim, noise, build, keep, bx, bz), height(land, rim, noise, build, keep, bx + (float)cell, bz)), Math.min(height(land, rim, noise, build, keep, bx, bz + (float)cell), height(land, rim, noise, build, keep, bx + (float)cell, bz + (float)cell)));
                  h = Math.min(h, height(land, rim, noise, build, keep, bx + (float)cell * 0.5F, bz + (float)cell * 0.5F));
               }

               heights[i] = h;
               low = Math.min(low, h);
               high = Math.max(high, h);
            } else {
               heights[i] = Float.NaN;
            }
         }
      }

      if (low > high) {
         low = land.level();
      }

      float anchor = land.hasSea() ? land.sea() : (keep != null ? keep.level() : land.level());
      int below = (int)Math.ceil((double)((anchor - (low - 2.0F * (float)cell)) / (float)cell));
      float floor = anchor - (float)(below * cell);
      int sea = land.hasSea() ? Math.round((land.sea() - floor) / (float)cell) : 0;
      BuildVolume.Land columns = new BuildVolume.Land(ring.columnsX(), ring.columnsZ(), sea);
      int top = sea;

      for(int cz = 0; cz < ring.columnsZ(); ++cz) {
         for(int cx = 0; cx < ring.columnsX(); ++cx) {
            int i = cz * ring.columnsX() + cx;
            float h = heights[i];
            if (!Float.isNaN(h)) {
               int g = Math.max(1, Math.round((h - floor) / (float)cell));
               float bx = (float)ring.x0() + ((float)cx + 0.5F) * (float)cell;
               float bz = (float)ring.z0() + ((float)cz + 0.5F) * (float)cell;
               int cover = cover(land, rim, build, noise, heights, ring, cx, cz, h, bx, bz);
               int leaves = 0;
               if (cover == 0 && land.trees() > 0.0F && (!land.hasSea() || h >= land.sea() + 1.0F) && (keep == null || keep.distance(bx, bz) > keep.radius() + 12.0F)) {
                  float wood = noise.fbm(bx / 90.0F + 31.7F, bz / 90.0F - 12.3F, 3) * 0.5F + 0.5F;
                  if (wood > 1.0F - land.trees() && noise.hash(bx, bz) < 0.86F) {
                     leaves = cell == 1 ? 2 + (noise.hash(bz, bx) < 0.5F ? 1 : 0) : 1;
                  }
               }

               columns.set(cx, cz, g, cover, leaves);
               top = Math.max(top, g + leaves);
            }
         }
      }

      BuildVolume v = BuildVolume.land(columns, top + 2, 0, 0, 0);
      if (spec.grass() != 0 || spec.foliage() != 0 || spec.water() != 0) {
         v.tint(spec.grass(), spec.foliage(), spec.water());
      }

      return new Placed(v, ring, floor);
   }

   private static @Nullable Keep keep(BuildSpec spec, BuildVolume build) {
      BuildSpec.Land land = spec.land();
      if (land != null && !(land.keep() <= 0.0F)) {
         float cornerX = (float)build.originX() - 0.5F;
         float cornerZ = (float)build.originZ() - 0.5F;
         float px = -cornerX;
         float pz = -cornerZ;
         double yaw = Math.toRadians((double)spec.yaw());
         float cos = (float)Math.cos(yaw);
         float sin = (float)Math.sin(yaw);
         return new Keep(px * cos - pz * sin, px * sin + pz * cos, land.keep(), -1.0F - (float)build.originY());
      } else {
         return null;
      }
   }

   private static float height(BuildSpec.Land land, @Nullable Rim rim, Noise noise, BuildVolume build, @Nullable Keep keep, float x, float z) {
      float d = land.under() ? 1000000.0F : distance(build, x, z);
      float hills = land.hills() * (noise.fbm(x / land.hillSize(), z / land.hillSize(), 4) * 0.5F + 0.5F - 0.3F);
      float h = land.level() + hills;
      if (land.mountains() > 0.0F) {
         float rise = smooth((d - land.mountainFrom()) / Math.max(1.0F, land.mountainSize()));
         if (rise > 0.0F) {
            h += land.mountains() * rise * noise.ridged(x / land.mountainSize() + 7.1F, z / land.mountainSize() - 3.9F, 5);
         }
      }

      if (rim != null) {
         float t = smooth(d / Math.max(1.0F, land.blend()));
         if (t < 1.0F) {
            h = rimHeight(rim, build, x, z) * (1.0F - t) + h * t;
         }
      }

      if (keep != null) {
         float out = keep.distance(x, z) - keep.radius();
         float t = smooth(out / 24.0F);
         h = keep.level() * (1.0F - t) + h * t;
         float cap = keep.level() + Math.max(0.0F, out) * 0.06F;
         if (h > cap) {
            float near = 1.0F - smooth((out - 160.0F) / 160.0F);
            float awayFromBuild = land.under() ? 1.0F : smooth(d / Math.max(1.0F, land.blend()));
            float w = near * awayFromBuild;
            h = h * (1.0F - w) + cap * w;
         }
      }

      return h;
   }

   private static float distance(BuildVolume build, float x, float z) {
      float dx = Math.max(Math.max(-x, 0.0F), x - (float)build.sizeX());
      float dz = Math.max(Math.max(-z, 0.0F), z - (float)build.sizeZ());
      return (float)Math.sqrt((double)(dx * dx + dz * dz));
   }

   private static float smooth(float t) {
      if (t <= 0.0F) {
         return 0.0F;
      } else {
         return t >= 1.0F ? 1.0F : t * t * (3.0F - 2.0F * t);
      }
   }

   private static float rimHeight(Rim rim, BuildVolume build, float x, float z) {
      int cx = clamp((int)Math.floor((double)x), 0, build.sizeX() - 1);
      int cz = clamp((int)Math.floor((double)z), 0, build.sizeZ() - 1);
      float sum = 0.0F;
      int n = 0;
      if (z < 0.0F) {
         sum += rim.north()[cx];
         ++n;
      }

      if (z >= (float)build.sizeZ()) {
         sum += rim.south()[cx];
         ++n;
      }

      if (x < 0.0F) {
         sum += rim.west()[cz];
         ++n;
      }

      if (x >= (float)build.sizeX()) {
         sum += rim.east()[cz];
         ++n;
      }

      return n == 0 ? rim.north()[cx] : sum / (float)n;
   }

   private static byte rimCover(Rim rim, BuildVolume build, float x, float z) {
      int cx = clamp((int)Math.floor((double)x), 0, build.sizeX() - 1);
      int cz = clamp((int)Math.floor((double)z), 0, build.sizeZ() - 1);
      if (z < 0.0F) {
         return rim.coverN()[cx];
      } else if (z >= (float)build.sizeZ()) {
         return rim.coverS()[cx];
      } else {
         return x < 0.0F ? rim.coverW()[cz] : rim.coverE()[cz];
      }
   }

   private static int clamp(int v, int lo, int hi) {
      return v < lo ? lo : Math.min(v, hi);
   }

   private static int cover(BuildSpec.Land land, @Nullable Rim rim, BuildVolume build, Noise noise, float[] heights, Ring ring, int cx, int cz, float h, float x, float z) {
      if (rim != null && distance(build, x, z) < land.blend() * 0.5F) {
         return rimCover(rim, build, x, z);
      } else if (land.hasSea() && h < land.sea() + 1.5F) {
         return h < land.sea() - 6.0F ? 4 : 1;
      } else if (land.mountains() > 0.0F && h > land.level() + land.snow() * land.mountains() + noise.hash(x, z) * 6.0F) {
         return 3;
      } else {
         float slope = slope(heights, ring, cx, cz) / (float)ring.cell();
         return slope > 1.4F ? 2 : land.ground();
      }
   }

   private static float slope(float[] heights, Ring ring, int cx, int cz) {
      float h = heights[cz * ring.columnsX() + cx];
      float most = 0.0F;

      for(int k = 0; k < 4; ++k) {
         int nx = cx + (k == 0 ? 1 : (k == 1 ? -1 : 0));
         int nz = cz + (k == 2 ? 1 : (k == 3 ? -1 : 0));
         if (nx >= 0 && nz >= 0 && nx < ring.columnsX() && nz < ring.columnsZ()) {
            float n = heights[nz * ring.columnsX() + nx];
            if (!Float.isNaN(n)) {
               most = Math.max(most, Math.abs(n - h));
            }
         }
      }

      return most;
   }

   static Rim rim(BuildVolume build) {
      int sx = build.sizeX();
      int sz = build.sizeZ();
      float[] n = new float[sx];
      float[] s = new float[sx];
      float[] w = new float[sz];
      float[] e = new float[sz];
      byte[] cn = new byte[sx];
      byte[] cs = new byte[sx];
      byte[] cw = new byte[sz];
      byte[] ce = new byte[sz];

      for(int x = 0; x < sx; ++x) {
         ground(build, x, 0, n, cn, x);
         ground(build, x, sz - 1, s, cs, x);
      }

      for(int z = 0; z < sz; ++z) {
         ground(build, 0, z, w, cw, z);
         ground(build, sx - 1, z, e, ce, z);
      }

      return new Rim(smoothed(n), smoothed(s), smoothed(w), smoothed(e), cn, cs, cw, ce);
   }

   private static void ground(BuildVolume build, int x, int z, float[] heights, byte[] covers, int i) {
      heights[i] = Float.NaN;
      covers[i] = 0;

      for(int y = build.sizeY() - 1; y >= 0; --y) {
         BlockState st = build.get(x, y, z);
         if (!st.isAir()) {
            if (st.getFluidState().is(FluidTags.WATER)) {
               for(int b = y - 1; b >= 0; --b) {
                  BlockState under = build.get(x, b, z);
                  if (natural(under)) {
                     heights[i] = (float)(b + 1);
                     covers[i] = (byte)coverOf(under);
                     return;
                  }
               }

               heights[i] = 0.0F;
               covers[i] = 1;
               return;
            }

            if (natural(st)) {
               heights[i] = (float)(y + 1);
               covers[i] = (byte)coverOf(st);
               return;
            }
         }
      }

   }

   private static boolean natural(BlockState s) {
      return s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(Blocks.GRAVEL) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.CLAY) || s.is(Blocks.SANDSTONE) || s.is(Blocks.DIRT_PATH) || s.is(Blocks.FARMLAND) || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.PACKED_ICE) || s.is(Blocks.CALCITE) || s.is(Blocks.TERRACOTTA);
   }

   private static int coverOf(BlockState s) {
      if (!s.is(BlockTags.SAND) && !s.is(Blocks.SANDSTONE)) {
         if (!s.is(Blocks.SNOW_BLOCK) && !s.is(Blocks.POWDER_SNOW) && !s.is(Blocks.PACKED_ICE)) {
            if (s.is(Blocks.GRAVEL)) {
               return 4;
            } else if (!s.is(BlockTags.BASE_STONE_OVERWORLD) && !s.is(Blocks.CALCITE) && !s.is(Blocks.TERRACOTTA)) {
               return !s.is(Blocks.COARSE_DIRT) && !s.is(Blocks.DIRT_PATH) && !s.is(Blocks.FARMLAND) && !s.is(Blocks.DIRT) ? 0 : 5;
            } else {
               return 2;
            }
         } else {
            return 3;
         }
      } else {
         return 1;
      }
   }

   private static float[] smoothed(float[] raw) {
      int n = raw.length;
      float[] filled = (float[])((float[]) raw).clone();
      int known = -1;

      for(int i = 0; i < n; ++i) {
         if (!Float.isNaN(raw[i])) {
            known = i;
            break;
         }
      }

      if (known < 0) {
         Arrays.fill(filled, 0.0F);
         return filled;
      } else {
         for(int i = 0; i < n; ++i) {
            if (Float.isNaN(filled[i])) {
               int a = i - 1;

               int b;
               for(b = i + 1; a >= 0 && Float.isNaN(raw[a]); --a) {
               }

               while(b < n && Float.isNaN(raw[b])) {
                  ++b;
               }

               filled[i] = a < 0 ? raw[b] : (b >= n ? raw[a] : (raw[a] * (float)(b - i) + raw[b] * (float)(i - a)) / (float)(b - a));
            }
         }

         float[] median = new float[n];
         float[] window = new float[9];

         for(int i = 0; i < n; ++i) {
            int k = 0;

            for(int j = i - 4; j <= i + 4; ++j) {
               window[k++] = filled[clamp(j, 0, n - 1)];
            }

            Arrays.sort(window);
            median[i] = window[4];
         }

         float[] out = new float[n];

         for(int i = 0; i < n; ++i) {
            float sum = 0.0F;

            for(int j = i - 3; j <= i + 3; ++j) {
               sum += median[clamp(j, 0, n - 1)];
            }

            out[i] = sum / 7.0F;
         }

         return out;
      }
   }

   static record Ring(int index, int cell, int x0, int z0, int columnsX, int columnsZ, int holeX0, int holeZ0, int holeX1, int holeZ1) {
   }

   static record Rim(float[] north, float[] south, float[] west, float[] east, byte[] coverN, byte[] coverS, byte[] coverW, byte[] coverE) {
   }

   static record Placed(BuildVolume volume, Ring ring, float floor) {
   }

   static record Keep(float x, float z, float radius, float level) {
      float distance(float px, float pz) {
         float dx = px - this.x;
         float dz = pz - this.z;
         return (float)Math.sqrt((double)(dx * dx + dz * dz));
      }
   }

   static final class Noise {
      private final int[] perm = new int[512];
      private final long seed;

      Noise(long seed) {
         this.seed = seed;
         int[] p = new int[256];

         for(int i = 0; i < 256; p[i] = i++) {
         }

         Random r = new Random(seed);

         for(int i = 255; i > 0; --i) {
            int j = r.nextInt(i + 1);
            int t = p[i];
            p[i] = p[j];
            p[j] = t;
         }

         for(int i = 0; i < 512; ++i) {
            this.perm[i] = p[i & 255];
         }

      }

      float noise(float x, float z) {
         int xi = (int)Math.floor((double)x);
         int zi = (int)Math.floor((double)z);
         float xf = x - (float)xi;
         float zf = z - (float)zi;
         int a = this.perm[(xi & 255) + this.perm[zi & 255]];
         int b = this.perm[(xi + 1 & 255) + this.perm[zi & 255]];
         int c = this.perm[(xi & 255) + this.perm[zi + 1 & 255]];
         int d = this.perm[(xi + 1 & 255) + this.perm[zi + 1 & 255]];
         float u = fade(xf);
         float v = fade(zf);
         float x1 = lerp(grad(a, xf, zf), grad(b, xf - 1.0F, zf), u);
         float x2 = lerp(grad(c, xf, zf - 1.0F), grad(d, xf - 1.0F, zf - 1.0F), u);
         return lerp(x1, x2, v) * 1.41F;
      }

      float fbm(float x, float z, int octaves) {
         float sum = 0.0F;
         float amp = 1.0F;
         float total = 0.0F;
         float f = 1.0F;

         for(int o = 0; o < octaves; ++o) {
            sum += this.noise(x * f + (float)o * 17.3F, z * f - (float)o * 9.1F) * amp;
            total += amp;
            amp *= 0.5F;
            f *= 2.0F;
         }

         return Math.max(-1.0F, Math.min(1.0F, sum / total));
      }

      float ridged(float x, float z, int octaves) {
         float sum = 0.0F;
         float amp = 1.0F;
         float total = 0.0F;
         float f = 1.0F;

         for(int o = 0; o < octaves; ++o) {
            float r = 1.0F - Math.abs(this.noise(x * f + (float)o * 5.7F, z * f + (float)o * 3.3F));
            sum += r * r * amp;
            total += amp;
            amp *= 0.5F;
            f *= 2.0F;
         }

         return Math.max(0.0F, Math.min(1.0F, sum / total));
      }

      float hash(float x, float z) {
         long h = this.seed ^ (long)Float.floatToIntBits(x) * -7046029254386353131L ^ (long)Float.floatToIntBits(z) * -4417276706812531889L;
         h ^= h >>> 33;
         h *= -49064778989728563L;
         h ^= h >>> 33;
         return (float)(h >>> 40) / 1.6777216E7F;
      }

      private static float fade(float t) {
         return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
      }

      private static float lerp(float a, float b, float t) {
         return a + (b - a) * t;
      }

      private static float grad(int hash, float x, float z) {
         float var10000;
         switch (hash & 7) {
            case 0 -> var10000 = x + z;
            case 1 -> var10000 = -x + z;
            case 2 -> var10000 = x - z;
            case 3 -> var10000 = -x - z;
            case 4 -> var10000 = x;
            case 5 -> var10000 = -x;
            case 6 -> var10000 = z;
            default -> var10000 = -z;
         }

         return var10000;
      }
   }
}
