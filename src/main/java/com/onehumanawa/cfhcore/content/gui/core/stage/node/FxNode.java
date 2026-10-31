package com.onehumanawa.cfhcore.content.gui.core.stage.node;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageBlend;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.Random;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class FxNode extends StageNode {
   private static final int SEGMENTS = 32;
   private static final int RINGS = 12;
   private final Kind kind;
   private final float[] px;
   private final float[] py;
   private final float[] pz;
   private final float[] sx;
   private final float[] sy;
   private final float[] sz;
   private final float[] phase;
   private final float spreadX;
   private final float spreadZ;
   private int color = -1;
   private float opacity;
   private float drift;
   private float fade;
   private float clipX;
   private float clipZ;
   private final Vector3f right = new Vector3f();
   private final Vector3f up = new Vector3f();
   private final Vector3f a = new Vector3f();
   private final Vector3f b = new Vector3f();

   private float falloff(float t) {
      float u = 1.0F - Mth.clamp(t, 0.0F, 1.0F);
      return this.fade > 0.0F ? (float)Math.pow((double)u, (double)this.fade) : u * u * (3.0F - 2.0F * u);
   }

   private FxNode(Kind kind, int count, float spreadX, float spreadZ) {
      this.kind = kind;
      this.spreadX = spreadX;
      this.spreadZ = spreadZ;
      this.px = new float[count];
      this.py = new float[count];
      this.pz = new float[count];
      this.sx = new float[count];
      this.sy = new float[count];
      this.sz = new float[count];
      this.phase = new float[count];
      this.pickable(false);
   }

   public static FxNode clouds(int clusters, float spreadX, float spreadZ, float height, float cellSize, long seed, float driftBlocksPerSec) {
      Random rnd = new Random(seed);
      int cells = clusters * 5;
      FxNode fx = new FxNode(FxNode.Kind.CLOUDS, cells, spreadX, spreadZ);
      int i = 0;

      for(int c = 0; c < clusters; ++c) {
         float cx = (rnd.nextFloat() - 0.5F) * spreadX;
         float cz = (rnd.nextFloat() - 0.5F) * spreadZ;
         float cy = height + (rnd.nextFloat() - 0.5F) * cellSize;
         int n = 2 + rnd.nextInt(4);
         float ox = 0.0F;
         float oz = 0.0F;

         for(int k = 0; k < 5; ++k) {
            if (k < n) {
               fx.px[i] = cx + ox;
               fx.py[i] = cy;
               fx.pz[i] = cz + oz;
               fx.sx[i] = cellSize * (0.8F + rnd.nextFloat() * 0.6F);
               fx.sy[i] = cellSize * 0.3F;
               fx.sz[i] = cellSize * (0.8F + rnd.nextFloat() * 0.6F);
               ox += rnd.nextBoolean() ? fx.sx[i] * 0.8F : 0.0F;
               oz += ox != 0.0F && !rnd.nextBoolean() ? 0.0F : fx.sz[i] * 0.7F;
            } else {
               fx.sx[i] = 0.0F;
            }

            fx.phase[i] = rnd.nextFloat();
            ++i;
         }
      }

      fx.opacity = 0.8F;
      fx.drift = driftBlocksPerSec;
      fx.bounds(-spreadX / 2.0F, height - cellSize, -spreadZ / 2.0F, spreadX / 2.0F, height + cellSize, spreadZ / 2.0F);
      fx.named("clouds");
      return fx;
   }

   public static FxNode sparkles(int count, float spreadX, float spreadY, float spreadZ, float size, int argb, long seed) {
      Random rnd = new Random(seed);
      FxNode fx = new FxNode(FxNode.Kind.SPARKLES, count, spreadX, spreadZ);

      for(int i = 0; i < count; ++i) {
         fx.px[i] = (rnd.nextFloat() - 0.5F) * spreadX;
         fx.py[i] = (rnd.nextFloat() - 0.5F) * spreadY;
         fx.pz[i] = (rnd.nextFloat() - 0.5F) * spreadZ;
         fx.sx[i] = size * (0.6F + rnd.nextFloat() * 0.8F);
         fx.phase[i] = rnd.nextFloat() * ((float)Math.PI * 2F);
         fx.sy[i] = 1.5F + rnd.nextFloat() * 2.0F;
      }

      fx.color = argb;
      fx.opacity = 1.0F;
      fx.bounds(-spreadX / 2.0F, -spreadY / 2.0F, -spreadZ / 2.0F, spreadX / 2.0F, spreadY / 2.0F, spreadZ / 2.0F);
      fx.named("sparkles");
      return fx;
   }

   public static FxNode cloudRing(int clusters, float radius, float band, float height, float cellSize, long seed) {
      Random rnd = new Random(seed);
      FxNode fx = new FxNode(FxNode.Kind.CLOUDS, clusters * 5, (radius + band) * 2.0F, (radius + band) * 2.0F);
      int i = 0;

      for(int c = 0; c < clusters; ++c) {
         float angle = ((float)c + (rnd.nextFloat() - 0.5F) * 0.6F) / (float)clusters * ((float)Math.PI * 2F);
         float r = radius + (rnd.nextFloat() - 0.5F) * band;
         float cx = Mth.sin(angle) * r;
         float cz = Mth.cos(angle) * r;
         float cy = height + (rnd.nextFloat() - 0.5F) * cellSize * 1.6F;
         float tx = Mth.cos(angle);
         float tz = -Mth.sin(angle);
         int n = 2 + rnd.nextInt(3);
         float along = (float)(-(n - 1)) * cellSize * 0.35F;

         for(int k = 0; k < 5; ++k) {
            if (k < n) {
               float out = (rnd.nextFloat() - 0.5F) * cellSize * 0.5F;
               fx.px[i] = cx + tx * along + Mth.sin(angle) * out;
               fx.py[i] = cy + (k % 2 == 0 ? 0.0F : cellSize * 0.12F);
               fx.pz[i] = cz + tz * along + Mth.cos(angle) * out;
               fx.sx[i] = cellSize * (0.85F + rnd.nextFloat() * 0.5F);
               fx.sy[i] = cellSize * 0.32F;
               fx.sz[i] = cellSize * (0.85F + rnd.nextFloat() * 0.5F);
               along += cellSize * 0.7F;
            } else {
               fx.sx[i] = 0.0F;
            }

            fx.phase[i] = rnd.nextFloat();
            ++i;
         }
      }

      fx.opacity = 0.85F;
      fx.bounds(-radius - band, height - cellSize, -radius - band, radius + band, height + cellSize, radius + band);
      fx.named("clouds");
      return fx;
   }

   public static FxNode glow(float radius, int argb) {
      FxNode fx = new FxNode(FxNode.Kind.GLOW, 1, radius * 2.0F, radius * 2.0F);
      fx.sx[0] = radius;
      fx.color = argb;
      fx.opacity = (float)(argb >>> 24 & 255) / 255.0F;
      fx.bounds(-radius, -0.01F, -radius, radius, 0.01F, radius);
      fx.named("glow");
      return fx;
   }

   public static FxNode cone(float topRadius, float bottomRadius, float height, int argb) {
      FxNode fx = new FxNode(FxNode.Kind.CONE, 1, bottomRadius * 2.0F, bottomRadius * 2.0F);
      fx.sx[0] = topRadius;
      fx.sz[0] = bottomRadius;
      fx.sy[0] = height;
      fx.color = argb;
      fx.opacity = (float)(argb >>> 24 & 255) / 255.0F;
      fx.bounds(-bottomRadius, 0.0F, -bottomRadius, bottomRadius, height, bottomRadius);
      fx.named("spotlight");
      return fx;
   }

   public FxNode color(int argb) {
      this.color = argb;
      return this;
   }

   public FxNode fade(float power) {
      this.fade = Math.max(0.0F, power);
      return this;
   }

   public FxNode clip(float halfX, float halfZ) {
      this.clipX = Math.max(0.0F, halfX);
      this.clipZ = Math.max(0.0F, halfZ);
      return this;
   }

   public FxNode opacity(float o) {
      this.opacity = Mth.clamp(o, 0.0F, 1.0F);
      return this;
   }

   public void update(StageRenderContext ctx) {
      if (this.kind == FxNode.Kind.CLOUDS && this.drift != 0.0F && Theme.current().motion() > 0.0F) {
         float dx = this.drift * ctx.deltaMs / 1000.0F;

         for(int i = 0; i < this.px.length; ++i) {
            if (!(this.sx[i] <= 0.0F)) {
               float[] var10000 = this.px;
               var10000[i] += dx;
               if (this.px[i] > this.spreadX / 2.0F + this.sx[i]) {
                  var10000 = this.px;
                  var10000[i] -= this.spreadX + 2.0F * this.sx[i];
               }

               if (this.px[i] < -this.spreadX / 2.0F - this.sx[i]) {
                  var10000 = this.px;
                  var10000[i] += this.spreadX + 2.0F * this.sx[i];
               }
            }
         }
      }

   }

   protected void draw(StageRenderContext ctx) {
   }

   public boolean hasTranslucentPass() {
      return true;
   }

   protected void drawTranslucent(StageRenderContext ctx) {
      Matrix4f m = ctx.pose.last().pose();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.enableBlend();
      StageBlend.over();
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(false);
      float alpha = this.opacity * this.alpha();
      int cr = this.color >> 16 & 255;
      int cg = this.color >> 8 & 255;
      int cb = this.color & 255;
      if (this.kind == FxNode.Kind.CONE) {
         StageBlend.add();
      }

      BufferBuilder bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      if (this.kind == FxNode.Kind.GLOW) {
         RenderSystem.disableCull();
         float r = this.sx[0];

         for(int ring = 0; ring < 12; ++ring) {
            float t0 = (float)ring / 12.0F;
            float t1 = (float)(ring + 1) / 12.0F;
            int c0 = Math.round(alpha * 255.0F * this.falloff(t0));
            int c1 = Math.round(alpha * 255.0F * this.falloff(t1));
            float r0 = r * t0;
            float r1 = r * t1;

            for(int i = 0; i < 32; ++i) {
               float a0 = (float)i / 32.0F * ((float)Math.PI * 2F);
               float a1 = (float)(i + 1) / 32.0F * ((float)Math.PI * 2F);
               bb.addVertex(m, this.cutX(Mth.sin(a0) * r0), 0.0F, this.cutZ(Mth.cos(a0) * r0)).setColor(cr, cg, cb, c0);
               bb.addVertex(m, this.cutX(Mth.sin(a0) * r1), 0.0F, this.cutZ(Mth.cos(a0) * r1)).setColor(cr, cg, cb, c1);
               bb.addVertex(m, this.cutX(Mth.sin(a1) * r1), 0.0F, this.cutZ(Mth.cos(a1) * r1)).setColor(cr, cg, cb, c1);
               bb.addVertex(m, this.cutX(Mth.sin(a1) * r0), 0.0F, this.cutZ(Mth.cos(a1) * r0)).setColor(cr, cg, cb, c0);
            }
         }
      } else if (this.kind == FxNode.Kind.CONE) {
         RenderSystem.disableCull();
         int top = Math.round(alpha * 255.0F);
         int foot = Math.round(alpha * 255.0F * 0.12F);
         float r0 = this.sx[0];
         float r1 = this.sz[0];
         float h = this.sy[0];

         for(int i = 0; i < 32; ++i) {
            float a0 = (float)i / 32.0F * ((float)Math.PI * 2F);
            float a1 = (float)(i + 1) / 32.0F * ((float)Math.PI * 2F);
            bb.addVertex(m, Mth.sin(a0) * r0, h, Mth.cos(a0) * r0).setColor(cr, cg, cb, top);
            bb.addVertex(m, Mth.sin(a0) * r1, 0.0F, Mth.cos(a0) * r1).setColor(cr, cg, cb, foot);
            bb.addVertex(m, Mth.sin(a1) * r1, 0.0F, Mth.cos(a1) * r1).setColor(cr, cg, cb, foot);
            bb.addVertex(m, Mth.sin(a1) * r0, h, Mth.cos(a1) * r0).setColor(cr, cg, cb, top);
         }
      } else if (this.kind == FxNode.Kind.CLOUDS) {
         RenderSystem.enableCull();
         int ca = Math.round(alpha * 255.0F);

         for(int i = 0; i < this.px.length; ++i) {
            if (!(this.sx[i] <= 0.0F)) {
               float x0 = this.px[i] - this.sx[i] / 2.0F;
               float x1 = this.px[i] + this.sx[i] / 2.0F;
               float y0 = this.py[i] - this.sy[i] / 2.0F;
               float y1 = this.py[i] + this.sy[i] / 2.0F;
               float z0 = this.pz[i] - this.sz[i] / 2.0F;
               float z1 = this.pz[i] + this.sz[i] / 2.0F;
               cube(bb, m, x0, y0, z0, x1, y1, z1, cr, cg, cb, ca);
            }
         }
      } else {
         RenderSystem.disableCull();
         Matrix4f view = ctx.camera.view();
         this.right.set(view.m00(), view.m10(), view.m20());
         this.up.set(view.m01(), view.m11(), view.m21());
         this.modelInverse.transformDirection(this.right).normalize();
         this.modelInverse.transformDirection(this.up).normalize();
         float t = ctx.seconds();
         boolean anim = Theme.current().motion() > 0.0F;

         for(int i = 0; i < this.px.length; ++i) {
            float tw = anim ? 0.55F + 0.45F * Mth.sin(t * this.sy[i] + this.phase[i]) : 0.8F;
            float s = this.sx[i] * (0.7F + 0.3F * tw);
            int sa = Math.round(alpha * tw * 255.0F);
            this.a.set(this.right).add(this.up).mul(s * 0.7071F);
            this.b.set(this.up).sub(this.right).mul(s * 0.7071F);
            float cx = this.px[i];
            float cy = this.py[i];
            float cz = this.pz[i];
            bb.addVertex(m, cx - this.a.x, cy - this.a.y, cz - this.a.z).setColor(cr, cg, cb, sa);
            bb.addVertex(m, cx - this.b.x, cy - this.b.y, cz - this.b.z).setColor(cr, cg, cb, sa);
            bb.addVertex(m, cx + this.a.x, cy + this.a.y, cz + this.a.z).setColor(255, 255, 255, sa);
            bb.addVertex(m, cx + this.b.x, cy + this.b.y, cz + this.b.z).setColor(cr, cg, cb, sa);
         }
      }

      MeshData mesh = bb.build();
      if (mesh != null) {
         BufferUploader.drawWithShader(mesh);
      }

      StageBlend.over();
      RenderSystem.enableCull();
      RenderSystem.depthMask(true);
   }

   private float cutX(float x) {
      return this.clipX > 0.0F ? Mth.clamp(x, -this.clipX, this.clipX) : x;
   }

   private float cutZ(float z) {
      return this.clipZ > 0.0F ? Mth.clamp(z, -this.clipZ, this.clipZ) : z;
   }

   private static void cube(BufferBuilder bb, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, int r, int g, int b, int a) {
      quad(bb, m, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b, a, 1.0F);
      quad(bb, m, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, r, g, b, a, 0.7F);
      quad(bb, m, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b, a, 0.8F);
      quad(bb, m, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, r, g, b, a, 0.8F);
      quad(bb, m, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, r, g, b, a, 0.9F);
      quad(bb, m, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b, a, 0.9F);
   }

   private static void quad(BufferBuilder bb, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, int r, int g, int b, int a, float shade) {
      int sr = Math.round((float)r * shade);
      int sg = Math.round((float)g * shade);
      int sb = Math.round((float)b * shade);
      bb.addVertex(m, ax, ay, az).setColor(sr, sg, sb, a);
      bb.addVertex(m, bx, by, bz).setColor(sr, sg, sb, a);
      bb.addVertex(m, cx, cy, cz).setColor(sr, sg, sb, a);
      bb.addVertex(m, dx, dy, dz).setColor(sr, sg, sb, a);
   }

   public static enum Kind {
      CLOUDS,
      SPARKLES,
      GLOW,
      CONE;
   }
}
