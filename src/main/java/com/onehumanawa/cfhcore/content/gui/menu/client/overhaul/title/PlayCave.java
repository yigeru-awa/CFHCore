package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.QuadMesh;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class PlayCave {
   static final int N = 8;
   static final int T = 16;
   static final Direction[] OUT;
   static final float FLOOR = 2.0F;
   static final float POOL = 1.86F;
   private static final int GRASS_TINT = -7226023;
   private static final int WATER_TINT = -12618012;
   private static final int SPILL = -13942;
   private static final float SPILL_REACH = 2.2F;
   private static final float AMBIENT_R = 0.2F;
   private static final float AMBIENT_G = 0.185F;
   private static final float AMBIENT_B = 0.19F;
   private static final float[] SHADE;
   private static final float RIM_SHADE = 0.74F;
   private static final float RIM = 0.12F;
   private static final float LEDGE = 0.5F;
   private static final float THROUGH = 1.0F;
   private static final String[][] MOUTHS;
   private final Mat[] cells = new Mat[512];
   private final float[][] cut;
   private final List<Lamp> lamps;
   final List<Piece> pieces;
   final List<Glint> glints;
   final List<Glow> glows;
   static final float TORCH_X = 2.02F;
   static final float TORCH_Y = 3.3F;
   static final float TORCH_Z = 5.5F;
   static final float DRIP_X = 5.5F;
   static final float DRIP_Y = 3.6F;
   static final float DRIP_Z = 3.5F;
   static final float LAVA_X = 4.5F;
   private static final int FRINGE = 10;
   private static final BlockQuads.Paint WHITE;
   private static final BlockQuads.Paint GREEN;
   static final float RAIL_LIFT = 0.03F;

   PlayCave() {
      this.cut = new float[OUT.length][256];
      this.lamps = new ArrayList();
      this.pieces = new ArrayList();
      this.glints = new ArrayList();
      this.glows = new ArrayList();

      for(int x = 0; x < 8; ++x) {
         for(int y = 0; y < 8; ++y) {
            for(int z = 0; z < 8; ++z) {
               boolean rim = x == 0 || x == 7 || z == 0 || z == 7 || y == 0;
               this.cells[index(x, y, z)] = y == 7 ? PlayCave.Mat.GRASS : (!rim && y != 6 ? PlayCave.Mat.STONE : PlayCave.Mat.DIRT);
            }
         }
      }

      this.mouths();
      this.cave();
      this.glints();
   }

   private void mouths() {
      for(int face = 0; face < OUT.length; ++face) {
         String[] rows = MOUTHS[face];
         float midA = 0.0F;
         float midRow = 0.0F;
         int through = 0;

         for(int line = 0; line < 16; ++line) {
            for(int col = 0; col < 16; ++col) {
               char c = rows[line].charAt(col);
               if (c != '.') {
                  int a = face == 0 ? col : 15 - col;
                  int row = 15 - line;
                  this.cut[face][row * 16 + a] = c == '#' ? 1.0F : (c == 'r' ? 0.5F : 0.12F);
                  if (c == '#') {
                     midA += (float)a + 0.5F;
                     midRow += (float)row + 0.5F;
                     ++through;
                  }
               }
            }
         }

         midA /= (float)Math.max(1, through);
         midRow /= (float)Math.max(1, through);

         for(int row = 0; row < 16; ++row) {
            for(int a = 0; a < 16; ++a) {
               float depth = this.cut[face][row * 16 + a];
               if (!(depth <= 0.0F)) {
                  float far = (float)Math.hypot((double)((float)a + 0.5F - midA), (double)((float)row + 0.5F - midRow)) / 7.5F;
                  this.pieces.add(new Piece(face, a, row, depth, Mth.clamp(far + (hash(face * 977 + row * 31 + a) - 0.5F) * 0.14F, 0.0F, 1.0F)));
               }
            }
         }
      }

   }

   private float cut(int face, int a, int row) {
      return a >= 0 && row >= 0 && a < 16 && row < 16 ? this.cut[face][row * 16 + a] : 0.0F;
   }

   private boolean breached(int x, int y, int z) {
      int face = z == 7 ? 0 : (x == 7 ? 1 : -1);
      if (face < 0) {
         return false;
      } else {
         int a = (face == 0 ? x : z) * 2;
         int row = y * 2;
         int through = 0;

         for(int i = 0; i < 4; ++i) {
            if (this.cut(face, a + (i & 1), row + (i >> 1)) >= 1.0F) {
               ++through;
            }
         }

         return through >= 2;
      }
   }

   private void cave() {
      this.box(2, 2, 5, 6, 5, 6, PlayCave.Mat.AIR);
      this.box(4, 2, 2, 6, 5, 4, PlayCave.Mat.AIR);
      this.set(1, 2, 6, PlayCave.Mat.AIR);
      this.set(0, 2, 6, PlayCave.Mat.DARK);
      this.set(6, 2, 1, PlayCave.Mat.AIR);
      this.set(6, 2, 0, PlayCave.Mat.DARK);
      this.set(3, 2, 4, PlayCave.Mat.DIAMOND);
      this.set(3, 3, 4, PlayCave.Mat.DIAMOND);
      this.set(2, 3, 4, PlayCave.Mat.DIAMOND);
      this.set(2, 5, 4, PlayCave.Mat.COAL);
      this.set(3, 4, 4, PlayCave.Mat.IRON);
      this.set(1, 3, 6, PlayCave.Mat.GOLD);
      this.set(1, 4, 5, PlayCave.Mat.IRON);
      this.set(1, 5, 6, PlayCave.Mat.COAL);
      this.set(4, 4, 1, PlayCave.Mat.MOLTEN);
      this.set(4, 1, 2, PlayCave.Mat.AIR);
      this.set(5, 1, 3, PlayCave.Mat.AIR);
      this.set(5, 3, 1, PlayCave.Mat.EMERALD);
      this.set(5, 4, 1, PlayCave.Mat.EMERALD);
      this.set(6, 4, 1, PlayCave.Mat.LAPIS);
      this.set(5, 2, 1, PlayCave.Mat.MOSSY);
      this.set(6, 5, 1, PlayCave.Mat.GOLD);
      this.set(3, 3, 2, PlayCave.Mat.REDSTONE);
      this.set(3, 2, 3, PlayCave.Mat.MOSSY);
      this.set(3, 4, 3, PlayCave.Mat.REDSTONE);
      this.lamps.add(new Lamp(2.44F, 4.02F, 5.5F, 2.6F, 1.9F, 1.15F, 5.2F));
      this.lamps.add(new Lamp(4.2F, 4.3F, 5.9F, 0.8F, 0.62F, 0.42F, 5.0F));
      this.glows.add(new Glow(2.32F, 3.99F, 5.5F, 0.2F, -251669894, true, 1.9F));
      this.glows.add(new Glow(2.32F, 3.97F, 5.5F, 1.05F, 822061136, true, 1.9F));
      this.lamps.add(new Lamp(4.5F, 3.1F, 2.45F, 3.0F, 1.25F, 0.28F, 4.6F));
      this.lamps.add(new Lamp(4.5F, 2.3F, 2.5F, 1.8F, 0.8F, 0.2F, 3.0F));
      this.glows.add(new Glow(4.5F, 3.1F, 2.35F, 1.2F, 1090484766, false, 0.7F));
      this.glows.add(new Glow(4.5F, 2.05F, 2.5F, 1.0F, 956262932, false, 2.9F));
      this.lamps.add(new Lamp(5.5F, 2.4F, 3.5F, 0.3F, 0.55F, 0.85F, 2.6F));
      this.lamps.add(new Lamp(4.5F, 4.2F, 9.4F, 0.7F, 0.76F, 0.88F, 5.8F));
      this.lamps.add(new Lamp(9.4F, 4.2F, 4.5F, 0.7F, 0.76F, 0.88F, 5.8F));
   }

   private void glints() {
      for(int x = 0; x < 8; ++x) {
         for(int y = 0; y < 8; ++y) {
            for(int z = 0; z < 8; ++z) {
               Mat m = this.cells[index(x, y, z)];
               if (m.glint != 0) {
                  for(Direction d : Direction.values()) {
                     int nx = x + d.getStepX();
                     int ny = y + d.getStepY();
                     int nz = z + d.getStepZ();
                     if (within(nx, ny, nz) && this.open(nx, ny, nz)) {
                        for(int i = 0; i < 2; ++i) {
                           int seed = index(x, y, z) * 12 + d.ordinal() * 2 + i;
                           float a = (hash(seed) - 0.5F) * 0.64F;
                           float c = (hash(seed + 977) - 0.5F) * 0.64F;
                           float ox = d.getAxis() == Axis.X ? 0.0F : a;
                           float oy = d.getAxis() == Axis.Y ? 0.0F : (d.getAxis() == Axis.X ? a : c);
                           float oz = d.getAxis() == Axis.Z ? 0.0F : c;
                           this.glints.add(new Glint((float)x + 0.5F + (float)d.getStepX() * 0.54F + ox, (float)y + 0.5F + (float)d.getStepY() * 0.54F + oy, (float)z + 0.5F + (float)d.getStepZ() * 0.54F + oz, d.getAxis(), m.glint, 1.1F + hash(seed + 31) * 1.5F, hash(seed + 63) * ((float)Math.PI * 2F)));
                        }
                     }
                  }
               }
            }
         }
      }

   }

   private static int index(int x, int y, int z) {
      return (x * 8 + y) * 8 + z;
   }

   private static boolean within(int x, int y, int z) {
      return x >= 0 && y >= 0 && z >= 0 && x < 8 && y < 8 && z < 8;
   }

   private boolean open(int x, int y, int z) {
      return !within(x, y, z) || this.cells[index(x, y, z)] == PlayCave.Mat.AIR || this.breached(x, y, z);
   }

   private void set(int x, int y, int z, Mat mat) {
      this.cells[index(x, y, z)] = mat;
   }

   private void box(int x0, int y0, int z0, int x1, int y1, int z1, Mat mat) {
      for(int x = x0; x <= x1; ++x) {
         for(int y = y0; y <= y1; ++y) {
            for(int z = z0; z <= z1; ++z) {
               this.set(x, y, z, mat);
            }
         }
      }

   }

   private static float hash(int seed) {
      int h = seed * -1640531535;
      h ^= h >>> 15;
      h *= -2048144789;
      h ^= h >>> 13;
      return (float)(h & '\uffff') / 65535.0F;
   }

   private int bake(float x, float y, float z, float nx, float ny, float nz) {
      boolean upright = Math.abs(ny) > 0.5F;
      boolean sideways = Math.abs(nx) > 0.5F;
      int solid = 0;
      float ox = 0.0F;
      float oy = 0.0F;
      float oz = 0.0F;

      for(int i = 0; i < 4; ++i) {
         float a = (i & 1) == 0 ? -0.5F : 0.5F;
         float c = (i & 2) == 0 ? -0.5F : 0.5F;
         float dx = upright ? a : (sideways ? 0.0F : a);
         float dy = upright ? 0.0F : (sideways ? a : c);
         float dz = upright ? c : (sideways ? c : 0.0F);
         if (this.open(Mth.floor(x + nx * 0.5F + dx), Mth.floor(y + ny * 0.5F + dy), Mth.floor(z + nz * 0.5F + dz))) {
            ox += dx;
            oy += dy;
            oz += dz;
         } else {
            ++solid;
         }
      }

      float len = (float)Math.sqrt((double)(ox * ox + oy * oy + oz * oz));
      float in = len > 0.001F ? 0.3F / len : 0.0F;
      return this.lit(x + nx * 0.3F + ox * in, y + ny * 0.3F + oy * in, z + nz * 0.3F + oz * in, nx, ny, nz, SHADE[Math.min(3, solid)], true);
   }

   private int lit(float x, float y, float z, float nx, float ny, float nz, float shade, boolean shadowed) {
      float r = 0.2F;
      float g = 0.185F;
      float b = 0.19F;
      boolean facing = nx != 0.0F || ny != 0.0F || nz != 0.0F;

      for(Lamp l : this.lamps) {
         float dx = l.x - x;
         float dy = l.y - y;
         float dz = l.z - z;
         float far = (float)Math.sqrt((double)(dx * dx + dy * dy + dz * dz));
         if (!(far >= l.reach)) {
            float take = 1.0F - far / l.reach;
            take *= take;
            if (facing && far > 0.001F) {
               take *= Mth.clamp(((nx * dx + ny * dy + nz * dz) / far + 0.45F) / 1.45F, 0.0F, 1.0F);
            } else {
               take *= 0.8F;
            }

            if (!(take <= 0.004F) && (!shadowed || this.sees(x, y, z, l.x, l.y, l.z, far))) {
               r += l.r * take;
               g += l.g * take;
               b += l.b * take;
            }
         }
      }

      return -16777216 | channel(r * shade) << 16 | channel(g * shade) << 8 | channel(b * shade);
   }

   private static int channel(float v) {
      return Mth.clamp(Math.round(v * 255.0F), 0, 255);
   }

   private boolean sees(float x, float y, float z, float lx, float ly, float lz, float far) {
      int steps = (int)(far / 0.14F);

      for(int i = 1; i < steps; ++i) {
         float t = (float)i / (float)steps;
         if ((1.0F - t) * far < 0.35F) {
            break;
         }

         if (!this.open(Mth.floor(x + (lx - x) * t), Mth.floor(y + (ly - y) * t), Mth.floor(z + (lz - z) * t))) {
            return false;
         }
      }

      return true;
   }

   int lightAt(float x, float y, float z) {
      return this.lit(x, y, z, 0.0F, 0.0F, 0.0F, 1.05F, true);
   }

   private static void outer(BlockQuads.Builder b, boolean clearLayer, Sprites s, Direction d, int xt, int yt, int zt, float off, BlockQuads.Paint paint, BlockQuads.Paint green) {
      float w = 0.0625F;
      float c = 0.5F;
      float x = (float)xt * 0.5F;
      float y = (float)yt * 0.5F;
      float z = (float)zt * 0.5F;
      switch (d) {
         case UP:
            b.rect(clearLayer, d, x, y + 0.5F + off, z, x + 0.5F, y + 0.5F + off, z + 0.5F, s.top, (float)xt * 0.0625F, (float)zt * 0.0625F, (float)(xt + 1) * 0.0625F, (float)(zt + 1) * 0.0625F, green);
            break;
         case DOWN:
            b.rect(clearLayer, d, x, y - off, z, x + 0.5F, y - off, z + 0.5F, s.dirt, (float)xt * 0.0625F, (float)zt * 0.0625F, (float)(xt + 1) * 0.0625F, (float)(zt + 1) * 0.0625F, paint);
            break;
         default:
            boolean alongX = d.getAxis() == Axis.Z;
            float u0 = (float)(alongX ? xt : zt) * 0.0625F;
            float v0 = (float)(15 - yt) * 0.0625F;
            float px = d == Direction.EAST ? x + 0.5F + off : (d == Direction.WEST ? x - off : x);
            float pz = d == Direction.SOUTH ? z + 0.5F + off : (d == Direction.NORTH ? z - off : z);
            float qx = alongX ? x + 0.5F : px;
            float qz = alongX ? pz : z + 0.5F;
            b.rect(clearLayer, d, px, y, pz, qx, y + 0.5F, qz, s.side, u0, v0, u0 + 0.0625F, v0 + 0.0625F, paint);
            if (yt >= 10) {
               b.rect(clearLayer, d, px, y, pz, qx, y + 0.5F, qz, s.fringe, u0, v0, u0 + 0.0625F, v0 + 0.0625F, green);
            }
      }

   }

   private static int face(Direction d) {
      return d == OUT[0] ? 0 : (d == OUT[1] ? 1 : -1);
   }

   BlockQuads[] shell(boolean whole) {
      Sprites s = new Sprites();
      BlockQuads[] sides = new BlockQuads[6];

      for(Direction d : Direction.values()) {
         BlockQuads.Builder b = BlockQuads.builder();
         int face = face(d);

         for(int i = 0; i < 16; ++i) {
            for(int j = 0; j < 16; ++j) {
               int xt = d.getAxis() == Axis.X ? (d == Direction.EAST ? 15 : 0) : i;
               int yt = d.getAxis() == Axis.Y ? (d == Direction.UP ? 15 : 0) : (d.getAxis() == Axis.X ? i : j);
               int zt = d.getAxis() == Axis.Z ? (d == Direction.SOUTH ? 15 : 0) : j;
               if (whole || face < 0 || !(this.cut(face, face == 0 ? xt : zt, yt) > 0.0F)) {
                  outer(b, false, s, d, xt, yt, zt, 0.0F, WHITE, GREEN);
               }
            }
         }

         sides[d.ordinal()] = b.build();
      }

      return sides;
   }

   BlockQuads spill() {
      Sprites s = new Sprites();
      BlockQuads.Builder b = BlockQuads.builder().decal();
      float c = 0.5F;

      for(int f = 0; f < OUT.length; ++f) {
         Direction d = OUT[f];
         int face = f;
         BlockQuads.Paint light = (x, y, z, nx, ny, nz) -> Mth.clamp(Math.round(this.spilt(face, (face == 0 ? x : z) / 0.5F, y / 0.5F) * 255.0F), 0, 255) << 24 | 16763274;
         BlockQuads.Paint green = (x, y, z, nx, ny, nz) -> light.at(x, y, z, nx, ny, nz) & -16777216 | mul(-13942, -7226023) & 16777215;

         for(int a = 0; a < 16; ++a) {
            for(int row = 0; row < 16; ++row) {
               if (!(this.cut(f, a, row) > 0.0F)) {
                  float most = Math.max(Math.max(this.spilt(f, (float)a, (float)row), this.spilt(f, (float)(a + 1), (float)row)), Math.max(this.spilt(f, (float)a, (float)(row + 1)), this.spilt(f, (float)(a + 1), (float)(row + 1))));
                  if (most > 0.02F) {
                     outer(b, true, s, d, f == 0 ? a : 15, row, f == 0 ? 15 : a, 0.02F, light, green);
                  }
               }
            }
         }
      }

      return b.build();
   }

   private float spilt(int face, float a, float row) {
      float reach = 4.4F;
      float near = 4.4F;

      for(Piece p : this.pieces) {
         if (p.face == face && !(p.depth < 1.0F)) {
            float da = Math.max(0.0F, Math.max((float)p.a - a, a - (float)(p.a + 1)));
            float dr = Math.max(0.0F, Math.max((float)p.row - row, row - (float)(p.row + 1)));
            near = Math.min(near, (float)Math.sqrt((double)(da * da + dr * dr)));
         }
      }

      float t = 1.0F - near / 4.4F;
      return t * t * 0.92F;
   }

   private static int mul(int a, int b) {
      return -16777216 | (a >> 16 & 255) * (b >> 16 & 255) / 255 << 16 | (a >> 8 & 255) * (b >> 8 & 255) / 255 << 8 | (a & 255) * (b & 255) / 255;
   }

   static void place(Piece p, float[] out) {
      float c = 0.5F;
      float along = ((float)p.a + 0.5F) * 0.5F;
      float in = 8.0F - p.depth / 2.0F;
      out[0] = p.face == 0 ? along : in;
      out[1] = ((float)p.row + 0.5F) * 0.5F;
      out[2] = p.face == 0 ? in : along;
   }

   void piece(VertexConsumer vc, PoseStack.Pose pose, Sprites s, Piece p) {
      Direction d = OUT[p.face];
      float w = 0.0625F;
      float h = 0.25F;
      float deep = p.depth / 2.0F;
      float u0 = (float)p.a * 0.0625F;
      float v0 = (float)(15 - p.row) * 0.0625F;
      float hx = d.getAxis() == Axis.X ? deep : 0.25F;
      float hz = d.getAxis() == Axis.Z ? deep : 0.25F;
      float fx = (float)d.getStepX() * hx;
      float fz = (float)d.getStepZ() * hz;
      BlockQuads.rect(vc, pose, d, d.getStepX() != 0 ? fx : -hx, -0.25F, d.getStepZ() != 0 ? fz : -hz, d.getStepX() != 0 ? fx : hx, 0.25F, d.getStepZ() != 0 ? fz : hz, s.side, u0, v0, u0 + 0.0625F, v0 + 0.0625F, -1);
      if (p.row >= 10) {
         BlockQuads.rect(vc, pose, d, d.getStepX() != 0 ? fx : -hx, -0.25F, d.getStepZ() != 0 ? fz : -hz, d.getStepX() != 0 ? fx : hx, 0.25F, d.getStepZ() != 0 ? fz : hz, s.fringe, u0, v0, u0 + 0.0625F, v0 + 0.0625F, -7226023);
      }

      for(Direction side : Direction.values()) {
         if (side.getAxis() != d.getAxis()) {
            float sx = (float)side.getStepX() * hx;
            float sy = (float)side.getStepY() * 0.25F;
            float sz = (float)side.getStepZ() * hz;
            BlockQuads.rect(vc, pose, side, side.getStepX() != 0 ? sx : -hx, side.getStepY() != 0 ? sy : -0.25F, side.getStepZ() != 0 ? sz : -hz, side.getStepX() != 0 ? sx : hx, side.getStepY() != 0 ? sy : 0.25F, side.getStepZ() != 0 ? sz : hz, s.dirt, u0, v0, u0 + 0.0625F, v0 + 0.0625F, side == Direction.UP ? -3092272 : -5723992);
         }
      }

   }

   BlockQuads inside() {
      Sprites s = new Sprites();
      BlockQuads.Builder b = BlockQuads.builder();
      BlockQuads.Paint baked = this::bake;
      BlockQuads.Paint molten = (xx, yx, zx, nxx, nyx, nzx) -> -2697514;
      BlockQuads.Paint dark = (xx, yx, zx, nxx, nyx, nzx) -> -16382715;
      BlockQuads.Paint wet = (xx, yx, zx, nxx, nyx, nzx) -> -872415232 | mul(max(this.bake(xx, yx, zx, nxx, nyx, nzx), -4934476), -12618012) & 16777215;

      for(int x = 0; x < 8; ++x) {
         for(int y = 0; y < 8; ++y) {
            for(int z = 0; z < 8; ++z) {
               Mat m = this.cells[index(x, y, z)];
               if (m != PlayCave.Mat.AIR && x != 7 && z != 7) {
                  for(Direction d : Direction.values()) {
                     int nx = x + d.getStepX();
                     int ny = y + d.getStepY();
                     int nz = z + d.getStepZ();
                     if (within(nx, ny, nz) && this.cells[index(nx, ny, nz)] == PlayCave.Mat.AIR) {
                        float px = d == Direction.EAST ? (float)(x + 1) : (float)x;
                        float py = d == Direction.UP ? (float)(y + 1) : (float)y;
                        float pz = d == Direction.SOUTH ? (float)(z + 1) : (float)z;
                        b.rect(false, d, px, py, pz, d.getAxis() == Axis.X ? px : (float)(x + 1), d.getAxis() == Axis.Y ? py : (float)(y + 1), d.getAxis() == Axis.Z ? pz : (float)(z + 1), s.of(m), 0.0F, 0.0F, 1.0F, 1.0F, m == PlayCave.Mat.MOLTEN ? molten : (m == PlayCave.Mat.DARK ? dark : baked));
                     }
                  }
               }
            }
         }
      }

      this.rims(b, s);
      float[] q = new float[20];
      this.torchStick(b, s, q);
      float z = 2.07F;
      float x0 = 4.08F;
      float x1 = 4.92F;

      for(int y = 2; y <= 3; ++y) {
         b.rect(false, Direction.SOUTH, 4.08F, (float)y, 2.07F, 4.92F, (float)(y + 1), 2.07F, s.lavaFlow, 0.04F, 0.0F, 0.46F, 0.5F, molten);
      }

      b.rect(false, Direction.SOUTH, 4.08F, 1.86F, 2.07F, 4.92F, 2.0F, 2.07F, s.lavaFlow, 0.04F, 0.0F, 0.46F, 0.06999999F, molten);
      b.rect(false, Direction.UP, 4.0F, 1.86F, 2.0F, 5.0F, 1.86F, 3.0F, s.lavaStill, 0.0F, 0.0F, 1.0F, 1.0F, molten);
      float piece = 1.2F;
      this.crossed(b, s.dripstoneRoot, q, 5.5F, 4.8F, 6.0F, 3.5F, 0.4F, 1.1F);
      this.crossed(b, s.dripstone, q, 5.5F, 3.6F, 4.8F, 3.5F, 0.4F, 1.1F);
      b.rect(true, Direction.UP, 5.0F, 1.86F, 3.0F, 6.0F, 1.86F, 4.0F, s.water, 0.0F, 0.0F, 1.0F, 1.0F, wet);
      return b.build();
   }

   BlockQuads rails() {
      Sprites s = new Sprites();
      BlockQuads.Builder b = BlockQuads.builder().decal();
      BlockQuads.Paint baked = this::bake;
      float[] q = new float[20];
      float lift = 2.03F;

      for(int x = 1; x <= 5; ++x) {
         corner(q, 0, (float)x, 2.03F, 6.0F, 0.0F, 0.0F);
         corner(q, 1, (float)x, 2.03F, 7.0F, 1.0F, 0.0F);
         corner(q, 2, (float)(x + 1), 2.03F, 7.0F, 1.0F, 1.0F);
         corner(q, 3, (float)(x + 1), 2.03F, 6.0F, 0.0F, 1.0F);
         b.quad(false, s.rail, q, 0.0F, 1.0F, 0.0F, baked);
      }

      for(int z = 1; z <= 5; ++z) {
         b.rect(false, Direction.UP, 6.0F, 2.03F, (float)z, 7.0F, 2.03F, (float)(z + 1), s.rail, 0.0F, 0.0F, 1.0F, 1.0F, baked);
      }

      b.rect(false, Direction.UP, 6.0F, 2.03F, 6.0F, 7.0F, 2.03F, 7.0F, s.railCorner, 1.0F, 1.0F, 0.0F, 0.0F, baked);
      return b.build();
   }

   private void rims(BlockQuads.Builder b, Sprites s) {
      float c = 0.5F;
      float w = 0.0625F;
      BlockQuads.Paint rock = (x, y, z, nx, ny, nz) -> max(scale(this.lit(x + nx * 0.2F, y + ny * 0.2F, z + nz * 0.2F, nx, ny, nz, 1.0F, false), 0.74F), least(nx, ny, nz, 0.74F));
      BlockQuads.Paint wall = (x, y, z, nx, ny, nz) -> max(scale(this.lit(x + nx * 0.2F, y + ny * 0.2F, z + nz * 0.2F, nx, ny, nz, 1.0F, false), 0.9F), least(nx, ny, nz, 0.9F));

      for(int f = 0; f < OUT.length; ++f) {
         Direction d = OUT[f];

         for(int a = 0; a < 16; ++a) {
            for(int row = 0; row < 16; ++row) {
               float depth = this.cut(f, a, row);
               if (!(depth <= 0.0F)) {
                  float u0 = (float)a * 0.0625F;
                  float v0 = (float)(15 - row) * 0.0625F;
                  float a0 = (float)a * 0.5F;
                  float a1 = a0 + 0.5F;
                  float y0 = (float)row * 0.5F;
                  float y1 = y0 + 0.5F;
                  if (depth < 1.0F) {
                     float at = 8.0F - depth;
                     if (f == 0) {
                        b.rect(false, d, a0, y0, at, a1, y1, at, s.stone, u0, v0, u0 + 0.0625F, v0 + 0.0625F, rock);
                     } else {
                        b.rect(false, d, at, y0, a0, at, y1, a1, s.stone, u0, v0, u0 + 0.0625F, v0 + 0.0625F, rock);
                     }
                  }

                  for(int i = 0; i < 4; ++i) {
                     int da = i == 0 ? -1 : (i == 1 ? 1 : 0);
                     int dr = i == 2 ? -1 : (i == 3 ? 1 : 0);
                     float beside = this.cut(f, a + da, row + dr);
                     if (!(beside >= depth)) {
                        TextureAtlasSprite sprite = beside <= 0.0F ? s.dirt : s.stone;
                        float near = 8.0F - depth;
                        float far = 8.0F - beside;
                        Direction looks = f == 0 ? (da < 0 ? Direction.EAST : (da > 0 ? Direction.WEST : (dr < 0 ? Direction.UP : Direction.DOWN))) : (da < 0 ? Direction.SOUTH : (da > 0 ? Direction.NORTH : (dr < 0 ? Direction.UP : Direction.DOWN)));
                        float edge = da < 0 ? a0 : a1;
                        float level = dr < 0 ? y0 : y1;
                        if (f == 0) {
                           if (da != 0) {
                              b.rect(false, looks, edge, y0, near, edge, y1, far, sprite, u0, v0, u0 + 0.0625F, v0 + 0.0625F, wall);
                           } else {
                              b.rect(false, looks, a0, level, near, a1, level, far, sprite, u0, v0, u0 + 0.0625F, v0 + 0.0625F, wall);
                           }
                        } else if (da != 0) {
                           b.rect(false, looks, near, y0, edge, far, y1, edge, sprite, u0, v0, u0 + 0.0625F, v0 + 0.0625F, wall);
                        } else {
                           b.rect(false, looks, near, level, a0, far, level, a1, sprite, u0, v0, u0 + 0.0625F, v0 + 0.0625F, wall);
                        }
                     }
                  }
               }
            }
         }
      }

   }

   private static int least(float nx, float ny, float nz, float share) {
      int v = channel((ny > 0.5F ? 0.8F : (nx > 0.5F ? 0.72F : (nz > 0.5F ? 0.52F : 0.4F))) * share);
      return -16777216 | v << 16 | v << 8 | v;
   }

   private static int scale(int argb, float by) {
      return -16777216 | channel((float)(argb >> 16 & 255) / 255.0F * by) << 16 | channel((float)(argb >> 8 & 255) / 255.0F * by) << 8 | channel((float)(argb & 255) / 255.0F * by);
   }

   private static void corner(float[] q, int i, float x, float y, float z, float u, float v) {
      int o = i * 5;
      q[o] = x;
      q[o + 1] = y;
      q[o + 2] = z;
      q[o + 3] = u;
      q[o + 4] = v;
   }

   private static int max(int a, int b) {
      return -16777216 | Math.max(a >> 16 & 255, b >> 16 & 255) << 16 | Math.max(a >> 8 & 255, b >> 8 & 255) << 8 | Math.max(a & 255, b & 255);
   }

   private void torchStick(BlockQuads.Builder b, Sprites s, float[] q) {
      float w = 0.0625F;
      float bx = 2.02F;
      float by = 3.3F;
      float tx = 2.3F;
      float ty = 3.92F;
      float z = 5.5F;
      float len = (float)Math.hypot((double)0.27999997F, (double)0.6200001F);
      float px = 0.6200001F / len * 0.0625F;
      float py = -0.27999997F / len * 0.0625F;
      float u0 = 0.4375F;
      float u1 = 0.5625F;
      float v0 = 0.375F;
      float v1 = 1.0F;
      BlockQuads.Paint wood = (ax, ay, az, nx, ny, nz) -> max(this.lit(ax, ay, az, 0.0F, 0.0F, 0.0F, 1.0F, true), -7570846);
      BlockQuads.Paint tip = (ax, ay, az, nx, ny, nz) -> -1;
      corner(q, 0, 2.02F + px, 3.3F + py, 5.5625F, 0.4375F, 1.0F);
      corner(q, 1, 2.02F + px, 3.3F + py, 5.4375F, 0.5625F, 1.0F);
      corner(q, 2, 2.3F + px, 3.92F + py, 5.4375F, 0.5625F, 0.375F);
      corner(q, 3, 2.3F + px, 3.92F + py, 5.5625F, 0.4375F, 0.375F);
      b.quad(false, s.torch, q, px / 0.0625F, py / 0.0625F, 0.0F, wood);
      corner(q, 0, 2.02F - px, 3.3F - py, 5.4375F, 0.4375F, 1.0F);
      corner(q, 1, 2.02F - px, 3.3F - py, 5.5625F, 0.5625F, 1.0F);
      corner(q, 2, 2.3F - px, 3.92F - py, 5.5625F, 0.5625F, 0.375F);
      corner(q, 3, 2.3F - px, 3.92F - py, 5.4375F, 0.4375F, 0.375F);
      b.quad(false, s.torch, q, -px / 0.0625F, -py / 0.0625F, 0.0F, wood);
      corner(q, 0, 2.02F - px, 3.3F - py, 5.5625F, 0.4375F, 1.0F);
      corner(q, 1, 2.02F + px, 3.3F + py, 5.5625F, 0.5625F, 1.0F);
      corner(q, 2, 2.3F + px, 3.92F + py, 5.5625F, 0.5625F, 0.375F);
      corner(q, 3, 2.3F - px, 3.92F - py, 5.5625F, 0.4375F, 0.375F);
      b.quad(false, s.torch, q, 0.0F, 0.0F, 1.0F, wood);
      corner(q, 0, 2.02F + px, 3.3F + py, 5.4375F, 0.4375F, 1.0F);
      corner(q, 1, 2.02F - px, 3.3F - py, 5.4375F, 0.5625F, 1.0F);
      corner(q, 2, 2.3F - px, 3.92F - py, 5.4375F, 0.5625F, 0.375F);
      corner(q, 3, 2.3F + px, 3.92F + py, 5.4375F, 0.4375F, 0.375F);
      b.quad(false, s.torch, q, 0.0F, 0.0F, -1.0F, wood);
      corner(q, 0, 2.3F - px, 3.92F - py, 5.5625F, 0.4375F, 0.5F);
      corner(q, 1, 2.3F + px, 3.92F + py, 5.5625F, 0.5625F, 0.5F);
      corner(q, 2, 2.3F + px, 3.92F + py, 5.4375F, 0.5625F, 0.375F);
      corner(q, 3, 2.3F - px, 3.92F - py, 5.4375F, 0.4375F, 0.375F);
      b.quad(false, s.torch, q, 0.27999997F / len, 0.6200001F / len, 0.0F, tip);
   }

   private void crossed(BlockQuads.Builder b, TextureAtlasSprite sprite, float[] q, float x, float y0, float y1, float z, float half, float shade) {
      BlockQuads.Paint paint = (px, py, pz, nx, ny, nz) -> this.lit(px, py, pz, 0.0F, 0.0F, 0.0F, shade, true);
      float s = 0.7071F;

      for(int plane = 0; plane < 2; ++plane) {
         float dz = plane == 0 ? half : -half;

         for(int side = 0; side < 2; ++side) {
            float a = side == 0 ? -half : half;
            float c = side == 0 ? 0.0F : 1.0F;
            corner(q, 0, x + a, y0, z + (side == 0 ? -dz : dz), c, 1.0F);
            corner(q, 1, x - a, y0, z + (side == 0 ? dz : -dz), 1.0F - c, 1.0F);
            corner(q, 2, x - a, y1, z + (side == 0 ? dz : -dz), 1.0F - c, 0.0F);
            corner(q, 3, x + a, y1, z + (side == 0 ? -dz : dz), c, 0.0F);
            float sign = side == 0 ? 1.0F : -1.0F;
            b.quad(false, sprite, q, -sign * 0.7071F * Math.signum(dz), 0.0F, sign * 0.7071F, paint);
         }
      }

   }

   static {
      OUT = new Direction[]{Direction.SOUTH, Direction.EAST};
      SHADE = new float[]{1.0F, 0.8F, 0.62F, 0.46F};
      MOUTHS = new String[][]{{"................", "................", "................", ".........RRR....", ".......RR###R...", ".....RR######R..", "...RR#########R.", ".r.R##########R.", ".rRR##########R.", "..Rr##########R.", "...r#########rR.", "...Rr#######rR..", "....RrrrrrrrR...", "................", "................", "................"}, {"................", "................", "................", "..RRR...........", ".R###RR.........", ".R#####RR.......", ".R#######RR.....", ".R#########R....", ".R##########R.R.", ".R##########R.r.", ".R##########R...", "..r########RR...", "..RrrrrrrrR.....", "................", "................", "................"}};
      WHITE = (x, y, z, nx, ny, nz) -> -1;
      GREEN = (x, y, z, nx, ny, nz) -> -7226023;
   }

   static enum Mat {
      AIR((String)null, 0),
      GRASS((String)null, 0),
      DIRT("dirt", 0),
      STONE("stone", 0),
      MOSSY("mossy_cobblestone", 0),
      COAL("coal_ore", 0),
      IRON("iron_ore", -11602),
      GOLD("gold_ore", -6810),
      DIAMOND("diamond_ore", -7929866),
      REDSTONE("redstone_ore", -44224),
      LAPIS("lapis_ore", -9463041),
      EMERALD("emerald_ore", -10619493),
      MOLTEN("lava_still", 0),
      DARK("stone", 0);

      final @Nullable String texture;
      final int glint;

      private Mat(final String texture, final int glint) {
         this.texture = texture;
         this.glint = glint;
      }
   }

   static record Piece(int face, int a, int row, float depth, float delay) {
   }

   private static record Lamp(float x, float y, float z, float r, float g, float b, float reach) {
   }

   static record Glint(float x, float y, float z, Direction.Axis across, int argb, float pace, float phase) {
   }

   static record Glow(float x, float y, float z, float radius, int argb, boolean flame, float phase) {
   }

   static final class Sprites {
      final TextureAtlasSprite top = QuadMesh.sprite("grass_block_top");
      final TextureAtlasSprite side = QuadMesh.sprite("grass_block_side");
      final TextureAtlasSprite fringe = QuadMesh.sprite("grass_block_side_overlay");
      final TextureAtlasSprite dirt = QuadMesh.sprite("dirt");
      final TextureAtlasSprite stone = QuadMesh.sprite("stone");
      final TextureAtlasSprite rail = QuadMesh.sprite("rail");
      final TextureAtlasSprite railCorner = QuadMesh.sprite("rail_corner");
      final TextureAtlasSprite torch = QuadMesh.sprite("torch");
      final TextureAtlasSprite lavaFlow = QuadMesh.sprite("lava_flow");
      final TextureAtlasSprite lavaStill = QuadMesh.sprite("lava_still");
      final TextureAtlasSprite water = QuadMesh.sprite("water_still");
      final TextureAtlasSprite dripstone = QuadMesh.sprite("pointed_dripstone_down_tip");
      final TextureAtlasSprite dripstoneRoot = QuadMesh.sprite("pointed_dripstone_down_frustum");
      private final TextureAtlasSprite[] mats = new TextureAtlasSprite[PlayCave.Mat.values().length];

      TextureAtlasSprite of(Mat m) {
         TextureAtlasSprite s = this.mats[m.ordinal()];
         if (s == null) {
            s = m.texture == null ? this.dirt : QuadMesh.sprite(m.texture);
            this.mats[m.ordinal()] = s;
         }

         return s;
      }
   }
}
