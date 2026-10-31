package com.onehumanawa.cfhcore.content.gui.menu.client.loading.journey;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageResources;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageSoft;
import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.QuadMesh;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.create.Aerial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

final class LandNode extends StageNode {
   private static final int GROUPS = 3;
   private static final float LATER = 0.34F;
   private static final float DROP = 15.0F;
   private static final float RISE_MS = 760.0F;
   private static final float SINK_MS = 620.0F;
   private static final int TEXELS = 8;
   private static final int FINE = 5;
   private static final int SEEN_PAST = 28;
   private static final int WHITE = -1;
   private static final int MAX_BUILDS = 2;
   private static final float EDGE_FROM = 0.94F;
   private static final float EDGE_ROUND = 16.0F;
   static final int SHELF = 10;
   private final Land land;
   private final int[] groupOf = new int[320];
   private final Group[] groups;
   private final float rings;
   private final Map<String, TextureAtlasSprite> sprites = new HashMap();
   private final float[] shades = new float[6];
   private final Vector3f normal = new Vector3f();
   private float reach;
   private float leaving;
   private int resources = -1;
   private boolean soft;
   private boolean broken;
   private int up;

   LandNode(Land land) {
      this.land = land;
      float mx = 10.0F;
      float mz = 8.0F;
      int count = (int)Math.floor(Math.sqrt((double)164.0F)) + 1;
      this.rings = (float)count + 0.68F;
      Group[] all = new Group[count * 3];

      for(int r = 0; r < count; ++r) {
         for(int g = 0; g < 3; ++g) {
            all[r * 3 + g] = new Group(r * 3 + g, (float)r + (float)g * 0.34F);
         }
      }

      for(int tz = 0; tz < 16; ++tz) {
         for(int tx = 0; tx < 20; ++tx) {
            float dx = (float)tx + 0.5F - 10.0F;
            float dz = (float)tz + 0.5F - 8.0F;
            int ring = Math.min(count - 1, (int)Math.floor(Math.sqrt((double)(dx * dx + dz * dz))));
            int group = ring * 3 + Math.floorMod(tx * 7 + tz * 13 + (tx ^ tz) * 3, 3);
            this.groupOf[tz * 20 + tx] = group;
            all[group].tiles.add(new int[]{tx, tz});
         }
      }

      this.groups = (Group[])Arrays.stream(all).filter((gx) -> !gx.tiles.isEmpty()).sorted(Comparator.comparingDouble((gx) -> (double)gx.from)).toArray((x$0) -> new Group[x$0]);
      this.bounds(-80.0F, 0.0F, -64.0F, 80.0F, 80.0F, 64.0F);
      this.hoverFeel(0.0F, 1.0F);
      this.pickable(false);
      this.named("land");
   }

   LandNode reach(float part) {
      this.reach = Mth.clamp(part, 0.0F, 1.0F);
      return this;
   }

   LandNode leave(float part) {
      this.leaving = Mth.clamp(part, 0.0F, 1.0F);
      return this;
   }

   void settle(float nowMs) {
      for(Group g : this.groups) {
         if (g.upAt < 0.0F && g.from <= this.reach * this.rings) {
            g.upAt = nowMs - 760.0F;
         }
      }

   }

   int up() {
      return this.up;
   }

   private TextureAtlasSprite sprite(String texture) {
      return (TextureAtlasSprite)this.sprites.computeIfAbsent(texture, (t) -> t.indexOf(58) >= 0 ? QuadMesh.sprite(ResourceLocation.parse(t)) : QuadMesh.sprite(t));
   }

   private static String stratum(Land.Cell c, int below) {
      if (below <= 0) {
         return c.top();
      } else {
         return below <= c.strata() ? c.under() : "stone";
      }
   }

   private static int tint(Land.Cell c, int below) {
      return below <= 0 ? c.topTint() : (below <= c.strata() ? c.underTint() : -1);
   }

   private Land.@Nullable Cell at(int x, int z) {
      Land.Cell c = this.land.at(x, z);
      int left = 127 - z;
      int sea = this.land.sea;
      if (c != null && left < 10 && c.ground() > sea) {
         int ground = Math.max(sea, Math.round((float)sea + (float)(c.ground() - sea) * Aerial.shelf(left, 10)));
         return ground == c.ground() ? c : c.raised(ground - c.ground());
      } else {
         return c;
      }
   }

   private boolean ready(Group g) {
      for(int[] t : g.tiles) {
         if (!this.land.ready(t[0], t[1])) {
            return false;
         }
      }

      return true;
   }

   private int groupAt(int x, int z) {
      return x >= 0 && z >= 0 && x < 160 && z < 128 ? this.groupOf[z / 8 * 20 + x / 8] : -1;
   }

   private static int shaded(int argb, float shade) {
      int r = Math.round((float)(argb >> 16 & 255) * shade);
      int g = Math.round((float)(argb >> 8 & 255) * shade);
      int b = Math.round((float)(argb & 255) * shade);
      return argb & -16777216 | r << 16 | g << 8 | b;
   }

   private void pieces(Sheet sheet, Direction d, int wx, int wz, int from, int to, Land.Cell c) {
      if (seen(d, wx)) {
         int y = from;

         while(y < to) {
            int below = Math.max(1, c.ground() - 1 - y);
            if ((y & 1) == 0 && y + 2 <= to) {
               sheet.pair(d, wx, y, wz, this.sprite(stratum(c, below)), tint(c, below));
               y += 2;
            } else {
               sheet.face(d, wx, y, wz, this.sprite(stratum(c, below)), 8, tint(c, below));
               ++y;
            }
         }

      }
   }

   private static boolean seen(Direction d, int wx) {
      boolean var10000;
      switch (d) {
         case NORTH:
            var10000 = false;
            break;
         case SOUTH:
         default:
            var10000 = true;
            break;
         case WEST:
            var10000 = wx > -28;
            break;
         case EAST:
            var10000 = wx < 28;
      }

      return var10000;
   }

   private void build(Group group) {
      Sheet solid = new Sheet();
      Sheet clear = new Sheet();
      int ox = -80;
      int oz = -64;

      for(int[] tile : group.tiles) {
         for(int z = tile[1] * 8; z < (tile[1] + 1) * 8; ++z) {
            for(int x = tile[0] * 8; x < (tile[0] + 1) * 8; ++x) {
               Land.Cell c = this.at(x, z);
               if (c != null) {
                  int g = c.ground();
                  int level = c.level();
                  int wx = -80 + x;
                  int wz = -64 + z;
                  int water = c.waterTint() & 16777215 | -771751936;
                  solid.face(Direction.UP, wx, g - 1, wz, this.sprite(c.top()), 8, c.topTint());
                  if (c.water() > 0) {
                     if (c.frozen()) {
                        solid.face(Direction.UP, wx, level - 1, wz, this.sprite("ice"), 8, -1);
                     } else {
                        clear.face(Direction.UP, wx, level - 1, wz, this.sprite("water_still"), 8, water);
                     }
                  }

                  for(Direction d : Plane.HORIZONTAL) {
                     int nx = x + d.getStepX();
                     int nz = z + d.getStepZ();
                     Land.Cell n = this.at(nx, nz);
                     boolean together = n != null && this.groupAt(nx, nz) == group.index;
                     int ng = n == null ? 0 : n.ground();
                     int fine = Math.max(ng, g - 5);

                     for(int y = fine; y < g; ++y) {
                        solid.face(d, wx, y, wz, this.sprite(stratum(c, g - 1 - y)), 8, tint(c, g - 1 - y));
                     }

                     if (ng < fine) {
                        this.pieces(solid, d, wx, wz, ng, fine, c);
                     }

                     if (!together && n != null) {
                        this.pieces(solid, d, wx, wz, 0, Math.min(ng, g), c);
                     }

                     if (c.water() > 0) {
                        int nl = n == null ? 0 : n.level();

                        for(int y = Math.max(g, nl); y < level; ++y) {
                           if (c.frozen() && y == level - 1) {
                              solid.face(d, wx, y, wz, this.sprite("ice"), 8, -1);
                           } else {
                              clear.face(d, wx, y, wz, this.sprite("water_still"), 8, water);
                           }
                        }
                     }
                  }

                  if (c.tree()) {
                     TextureAtlasSprite bark = this.sprite(c.trunk());
                     TextureAtlasSprite leaves = this.sprite(c.canopy());
                     solid.face(Direction.UP, wx, g + 1, wz, leaves, 8, c.canopyTint());

                     for(Direction d : Plane.HORIZONTAL) {
                        Land.Cell n = this.at(x + d.getStepX(), z + d.getStepZ());
                        int ng = n == null ? 0 : n.ground();
                        boolean beside = n != null && n.tree() && ng == g && this.groupAt(x + d.getStepX(), z + d.getStepZ()) == group.index;
                        if (!beside && ng <= g) {
                           solid.face(d, wx, g, wz, bark, 16, -1);
                        }

                        if (!beside && ng <= g + 1) {
                           solid.face(d, wx, g + 1, wz, leaves, 8, c.canopyTint());
                        }
                     }
                  }
               }
            }
         }
      }

      group.solid = solid.upload();
      group.clear = clear.upload();
      group.built = true;
   }

   private static void close(Group g) {
      if (g.solid != null) {
         g.solid.close();
         g.solid = null;
      }

      if (g.clear != null) {
         g.clear.close();
         g.clear = null;
      }

      g.built = false;
   }

   private void shade(StageRenderContext ctx) {
      Vector3f sun = ctx.lighting.sun();

      for(Direction d : Direction.values()) {
         this.normal.set((float)d.getStepX(), (float)d.getStepY(), (float)d.getStepZ());
         this.model.transformDirection(this.normal);
         if (this.normal.lengthSquared() > 1.0E-8F) {
            this.normal.normalize();
         }

         this.shades[d.ordinal()] = Mth.clamp(0.5F + 0.5F * Math.max(0.0F, this.normal.dot(sun)), 0.0F, 1.0F);
      }

   }

   public void update(StageRenderContext ctx) {
      if (!this.broken) {
         int gen = StageResources.generation();
         if (gen != this.resources || this.soft != ctx.soft) {
            this.sprites.clear();

            for(Group g : this.groups) {
               if (g.built) {
                  close(g);
               }
            }

            this.resources = gen;
            this.soft = ctx.soft;
            this.shade(ctx);
         }

         float within = this.reach * this.rings;
         int builds = 0;
         int count = 0;

         try {
            for(Group g : this.groups) {
               if (g.upAt >= 0.0F) {
                  ++count;
                  if (!g.built && this.ready(g)) {
                     this.build(g);
                  }
               } else {
                  if (g.from > within || builds >= 2 || !this.ready(g)) {
                     break;
                  }

                  this.build(g);
                  g.upAt = ctx.timeMs;
                  ++builds;
                  ++count;
               }
            }
         } catch (Exception e) {
            this.broken = true;
            CFHGuiMenu.LOGGER.error("[CFHGui Menu] the land could not be built", e);
         }

         this.up = count;
         float rim = 0.0F;

         for(Group g : this.groups) {
            if (g.upAt >= 0.0F && (g.solid != null || g.clear != null)) {
               rim = Math.max(rim, g.from);
            }
         }

         float gone = this.leaving * (rim + 1.0F);

         for(Group g : this.groups) {
            if (!(g.upAt < 0.0F)) {
               boolean goes = this.leaving > 0.0F && rim - g.from < gone;
               if (goes && g.downAt < 0.0F) {
                  g.downAt = ctx.timeMs;
               } else if (!goes && g.downAt >= 0.0F) {
                  g.downAt = -1.0F;
               }
            }
         }

      }
   }

   private static float under(Group g, float nowMs) {
      float y = 0.0F;
      if (g.upAt >= 0.0F) {
         y = -15.0F * (1.0F - Ease.OUT_BACK.apply((nowMs - g.upAt) / 760.0F));
      }

      if (g.downAt >= 0.0F) {
         float t = (nowMs - g.downAt) / 620.0F;
         if (t >= 1.0F) {
            return Float.NaN;
         }

         y -= 24.0F * t * t * t;
      }

      return y;
   }

   private void draw(StageRenderContext ctx, boolean translucent) {
      if (!this.broken) {
         float a = this.alpha();
         RenderType type = translucent ? RenderType.translucent() : RenderType.cutoutMipped();
         float[] was = (float[])RenderSystem.getShaderColor().clone();
         boolean any = false;
         if (this.soft) {
            StageSoft.footprint(0.0F, 0.0F, 80.0F, 64.0F, 0.94F, 16.0F);
         }

         try {
            for(Group g : this.groups) {
               VertexBuffer vbo = translucent ? g.clear : g.solid;
               if (vbo != null && !(g.upAt < 0.0F)) {
                  float y = under(g, ctx.timeMs);
                  if (!Float.isNaN(y)) {
                     ctx.pose.pushPose();
                     ctx.pose.translate(0.0F, y, 0.0F);
                     if (this.soft) {
                        StageSoft.draw(vbo, ctx.pose.last().pose(), ctx.camera.projection(), TextureAtlas.LOCATION_BLOCKS, a, translucent);
                     } else {
                        if (!any) {
                           type.setupRenderState();
                           any = true;
                        }

                        float b = ctx.lighting.brightness();
                        RenderSystem.setShaderColor(b, b, b, a);
                        vbo.bind();
                        vbo.drawWithShader(ctx.pose.last().pose(), ctx.camera.projection(), RenderSystem.getShader());
                     }

                     ctx.pose.popPose();
                  }
               }
            }
         } finally {
            if (this.soft) {
               StageSoft.noFootprint();
            }

         }

         if (any) {
            VertexBuffer.unbind();
            type.clearRenderState();
         }

         RenderSystem.setShaderColor(was[0], was[1], was[2], was[3]);
      }
   }

   protected void draw(StageRenderContext ctx) {
      if (this.alpha() >= 1.0F) {
         this.draw(ctx, false);
      }

   }

   public boolean hasTranslucentPass() {
      return true;
   }

   protected void drawTranslucent(StageRenderContext ctx) {
      if (this.alpha() < 1.0F) {
         this.draw(ctx, false);
      }

      this.draw(ctx, true);
   }

   public void dispose() {
      for(Group g : this.groups) {
         close(g);
      }

   }

   private static final class Group {
      final int index;
      final float from;
      final List<int[]> tiles = new ArrayList();
      @Nullable VertexBuffer solid;
      @Nullable VertexBuffer clear;
      boolean built;
      float upAt = -1.0F;
      float downAt = -1.0F;

      Group(int index, float from) {
         this.index = index;
         this.from = from;
      }
   }

   private final class Sheet {
      private final ByteBufferBuilder memory = new ByteBufferBuilder(32768);
      private final BufferBuilder buffer;

      private Sheet() {
         this.buffer = new BufferBuilder(this.memory, Mode.QUADS, DefaultVertexFormat.BLOCK);
      }

      void face(Direction face, int x, int y, int z, TextureAtlasSprite sprite, int texels, int argb) {
         int a = face.getAxis() == Axis.X ? z : x;
         int b = face.getAxis() == Axis.Y ? z : y;
         int t = Math.max(1, Math.min(16, texels));
         int span = Math.max(1, 16 / t);
         float u0 = (float)(Math.floorMod(a, span) * t) / 16.0F;
         int row = face.getAxis() == Axis.Y ? Math.floorMod(b, span) : span - 1 - Math.floorMod(b, span);
         float v0 = (float)(row * t) / 16.0F;
         this.quad(face, (float)x, (float)y, (float)y + 1.0F, (float)z, sprite, u0, u0 + (float)t / 16.0F, v0, v0 + (float)t / 16.0F, argb);
      }

      void pair(Direction face, int x, int y, int z, TextureAtlasSprite sprite, int argb) {
         int a = face.getAxis() == Axis.X ? z : x;
         float u0 = (float)Math.floorMod(a, 2) * 0.5F;
         this.quad(face, (float)x, (float)y, (float)y + 2.0F, (float)z, sprite, u0, u0 + 0.5F, 0.0F, 1.0F, argb);
      }

      private void quad(Direction face, float x, float y, float y1, float z, TextureAtlasSprite sprite, float u0, float u1, float v0, float v1, int tint) {
         float x1 = x + 1.0F;
         float z1 = z + 1.0F;
         float nx = (float)face.getStepX();
         float ny = (float)face.getStepY();
         float nz = (float)face.getStepZ();
         int argb = LandNode.this.soft ? tint : LandNode.shaded(tint, LandNode.this.shades[face.ordinal()]);
         switch (face) {
            case UP:
               this.vertex(sprite, x, y1, z, u0, v0, argb, nx, ny, nz);
               this.vertex(sprite, x, y1, z1, u0, v1, argb, nx, ny, nz);
               this.vertex(sprite, x1, y1, z1, u1, v1, argb, nx, ny, nz);
               this.vertex(sprite, x1, y1, z, u1, v0, argb, nx, ny, nz);
               break;
            case DOWN:
               this.vertex(sprite, x, y, z1, u0, v1, argb, nx, ny, nz);
               this.vertex(sprite, x, y, z, u0, v0, argb, nx, ny, nz);
               this.vertex(sprite, x1, y, z, u1, v0, argb, nx, ny, nz);
               this.vertex(sprite, x1, y, z1, u1, v1, argb, nx, ny, nz);
               break;
            case NORTH:
               this.vertex(sprite, x, y, z, u0, v1, argb, nx, ny, nz);
               this.vertex(sprite, x, y1, z, u0, v0, argb, nx, ny, nz);
               this.vertex(sprite, x1, y1, z, u1, v0, argb, nx, ny, nz);
               this.vertex(sprite, x1, y, z, u1, v1, argb, nx, ny, nz);
               break;
            case SOUTH:
               this.vertex(sprite, x1, y, z1, u1, v1, argb, nx, ny, nz);
               this.vertex(sprite, x1, y1, z1, u1, v0, argb, nx, ny, nz);
               this.vertex(sprite, x, y1, z1, u0, v0, argb, nx, ny, nz);
               this.vertex(sprite, x, y, z1, u0, v1, argb, nx, ny, nz);
               break;
            case WEST:
               this.vertex(sprite, x, y, z1, u1, v1, argb, nx, ny, nz);
               this.vertex(sprite, x, y1, z1, u1, v0, argb, nx, ny, nz);
               this.vertex(sprite, x, y1, z, u0, v0, argb, nx, ny, nz);
               this.vertex(sprite, x, y, z, u0, v1, argb, nx, ny, nz);
               break;
            default:
               this.vertex(sprite, x1, y, z, u0, v1, argb, nx, ny, nz);
               this.vertex(sprite, x1, y1, z, u0, v0, argb, nx, ny, nz);
               this.vertex(sprite, x1, y1, z1, u1, v0, argb, nx, ny, nz);
               this.vertex(sprite, x1, y, z1, u1, v1, argb, nx, ny, nz);
         }

      }

      private void vertex(TextureAtlasSprite sprite, float x, float y, float z, float u, float v, int argb, float nx, float ny, float nz) {
         this.buffer.addVertex(x, y, z).setColor(argb).setUv(sprite.getU(u), sprite.getV(v)).setLight(15728880).setNormal(nx, ny, nz);
      }

      @Nullable VertexBuffer upload() {
         VertexBuffer vbo;
         try {
            MeshData data = this.buffer.build();
            if (data != null) {
               vbo = new VertexBuffer(Usage.STATIC);
               vbo.bind();
               vbo.upload(data);
               VertexBuffer.unbind();
               VertexBuffer var3 = vbo;
               return var3;
            }

            vbo = null;
         } finally {
            this.memory.close();
         }

         return vbo;
      }
   }
}
