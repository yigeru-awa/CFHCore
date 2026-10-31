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
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.Random;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class SkyNode extends StageNode {
   private static final int SEGMENTS = 48;
   private static final float[] RINGS = new float[]{-30.0F, -6.0F, -1.5F, 0.0F, 2.0F, 5.0F, 9.0F, 14.0F, 21.0F, 30.0F, 42.0F, 56.0F, 72.0F, 90.0F};
   private int below = -15722464;
   private int horizon = -2060182;
   private int low = -5215616;
   private int high = -12959880;
   private int zenith = -15986640;
   private final Vector3f sun = (new Vector3f(0.0F, 0.05F, -1.0F)).normalize();
   private int sunColor = -14192;
   private float sunGlow = 0.6F;
   private float sunSize = 4.0F;
   private float sunReach = 40.0F;
   private float starBrightness;
   private final float[] starX;
   private final float[] starY;
   private final float[] starZ;
   private final float[] starSize;
   private final float[] starPhase;
   private final Vector3f scratch = new Vector3f();
   private final Vector3f right = new Vector3f();
   private final Vector3f up = new Vector3f();

   public SkyNode(int stars, long seed) {
      Random rnd = new Random(seed);
      this.starX = new float[stars];
      this.starY = new float[stars];
      this.starZ = new float[stars];
      this.starSize = new float[stars];
      this.starPhase = new float[stars];

      for(int i = 0; i < stars; ++i) {
         float yaw = rnd.nextFloat() * ((float)Math.PI * 2F);
         float el = (float)Math.asin((double)(0.04F + 0.96F * (float)Math.sqrt((double)rnd.nextFloat())));
         this.starX[i] = Mth.sin(yaw) * Mth.cos(el);
         this.starY[i] = Mth.sin(el);
         this.starZ[i] = -Mth.cos(yaw) * Mth.cos(el);
         this.starSize[i] = 0.0016F + rnd.nextFloat() * rnd.nextFloat() * 0.0042F;
         this.starPhase[i] = rnd.nextFloat() * ((float)Math.PI * 2F);
      }

      this.pickable(false);
      this.hoverFeel(0.0F, 1.0F);
      this.named("sky");
      this.bounds(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
   }

   public SkyNode colors(int below, int horizon, int low, int high, int zenith) {
      this.below = below;
      this.horizon = horizon;
      this.low = low;
      this.high = high;
      this.zenith = zenith;
      return this;
   }

   public SkyNode sun(float yawDeg, float elevationDeg, int argb, float glow, float discDeg, float glowDeg) {
      float yaw = (float)Math.toRadians((double)yawDeg);
      float el = (float)Math.toRadians((double)elevationDeg);
      this.sun.set(Mth.sin(yaw) * Mth.cos(el), Mth.sin(el), -Mth.cos(yaw) * Mth.cos(el)).normalize();
      this.sunColor = argb;
      this.sunGlow = Mth.clamp(glow, 0.0F, 2.0F);
      this.sunSize = Math.max(0.0F, discDeg);
      this.sunReach = Math.max(1.0F, glowDeg);
      return this;
   }

   public SkyNode stars(float brightness) {
      this.starBrightness = Mth.clamp(brightness, 0.0F, 1.0F);
      return this;
   }

   protected void draw(StageRenderContext ctx) {
      float radius = ctx.camera.far() * 0.8F;
      Vector3f eye = ctx.camera.rendered().position;
      ctx.pose.pushPose();
      ctx.pose.last().pose().set(ctx.camera.view()).translate(eye.x, eye.y, eye.z);
      Matrix4f m = ctx.pose.last().pose();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.disableCull();
      RenderSystem.enableBlend();
      StageBlend.over();
      float a = this.alpha();
      BufferBuilder bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

      for(int r = 0; r < RINGS.length - 1; ++r) {
         float e0 = (float)Math.toRadians((double)RINGS[r]);
         float e1 = (float)Math.toRadians((double)RINGS[r + 1]);

         for(int s = 0; s < 48; ++s) {
            float y0 = (float)s / 48.0F * ((float)Math.PI * 2F);
            float y1 = (float)(s + 1) / 48.0F * ((float)Math.PI * 2F);
            this.vertex(bb, m, radius, y0, e0, a);
            this.vertex(bb, m, radius, y0, e1, a);
            this.vertex(bb, m, radius, y1, e1, a);
            this.vertex(bb, m, radius, y1, e0, a);
         }
      }

      BufferUploader.drawWithShader(bb.buildOrThrow());
      StageBlend.add();
      Matrix4f view = ctx.camera.view();
      this.right.set(view.m00(), view.m10(), view.m20());
      this.up.set(view.m01(), view.m11(), view.m21());
      bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      float cx = this.sun.x * radius * 0.95F;
      float cy = this.sun.y * radius * 0.95F;
      float cz = this.sun.z * radius * 0.95F;
      float reach = (float)Math.tan(Math.toRadians((double)this.sunReach)) * radius * 0.95F;
      float disc = (float)Math.tan(Math.toRadians((double)(this.sunSize * 0.5F))) * radius * 0.95F;
      int sr = this.sunColor >> 16 & 255;
      int sg = this.sunColor >> 8 & 255;
      int sb = this.sunColor & 255;
      int rings = 10;

      for(int ring = 0; ring < 10; ++ring) {
         float t0 = (float)ring / 10.0F;
         float t1 = (float)(ring + 1) / 10.0F;
         float r0 = reach * t0;
         float r1 = reach * t1;
         int a0 = Math.round(255.0F * a * this.sunGlow * 0.55F * (float)Math.pow((double)(1.0F - t0), (double)3.0F));
         int a1 = Math.round(255.0F * a * this.sunGlow * 0.55F * (float)Math.pow((double)(1.0F - t1), (double)3.0F));
         this.fan(bb, m, cx, cy, cz, r0, r1, sr, sg, sb, a0, a1);
      }

      if (disc > 0.0F) {
         int da = Math.round(255.0F * a * Math.min(1.0F, this.sunGlow * 1.4F));
         this.fan(bb, m, cx, cy, cz, 0.0F, disc, sr, sg, sb, da, da);
         this.fan(bb, m, cx, cy, cz, disc, disc * 1.25F, sr, sg, sb, da, 0);
      }

      MeshData glowMesh = bb.build();
      if (glowMesh != null) {
         BufferUploader.drawWithShader(glowMesh);
      }

      if (this.starBrightness > 0.0F && this.starX.length > 0) {
         boolean twinkle = Theme.current().motion() > 0.0F;
         float t = ctx.seconds();
         bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

         for(int i = 0; i < this.starX.length; ++i) {
            float el = (float)Math.toDegrees(Math.asin((double)this.starY[i]));
            int under = this.skyAt(el, this.starX[i], this.starY[i], this.starZ[i]);
            float dark = 1.0F - Mth.clamp(Colors.luminance(under) * 2.2F, 0.0F, 1.0F);
            float tw = twinkle ? 0.65F + 0.35F * Mth.sin(t * (1.1F + (float)(i % 7) * 0.37F) + this.starPhase[i]) : 0.85F;
            int sa = Math.round(255.0F * a * this.starBrightness * dark * tw);
            if (sa > 2) {
               float s = this.starSize[i] * radius;
               float px = this.starX[i] * radius * 0.97F;
               float py = this.starY[i] * radius * 0.97F;
               float pz = this.starZ[i] * radius * 0.97F;
               float rx = this.right.x * s;
               float ry = this.right.y * s;
               float rz = this.right.z * s;
               float ux = this.up.x * s;
               float uy = this.up.y * s;
               float uz = this.up.z * s;
               bb.addVertex(m, px - rx - ux, py - ry - uy, pz - rz - uz).setColor(255, 250, 235, sa);
               bb.addVertex(m, px + rx - ux, py + ry - uy, pz + rz - uz).setColor(255, 250, 235, sa);
               bb.addVertex(m, px + rx + ux, py + ry + uy, pz + rz + uz).setColor(255, 250, 235, sa);
               bb.addVertex(m, px - rx + ux, py - ry + uy, pz - rz + uz).setColor(255, 250, 235, sa);
            }
         }

         MeshData starMesh = bb.build();
         if (starMesh != null) {
            BufferUploader.drawWithShader(starMesh);
         }
      }

      StageBlend.over();
      RenderSystem.enableCull();
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      ctx.pose.popPose();
   }

   private void vertex(BufferBuilder bb, Matrix4f m, float radius, float yaw, float el, float a) {
      float x = Mth.sin(yaw) * Mth.cos(el);
      float y = Mth.sin(el);
      float z = -Mth.cos(yaw) * Mth.cos(el);
      int c = this.skyAt((float)Math.toDegrees((double)el), x, y, z);
      bb.addVertex(m, x * radius, y * radius, z * radius).setColor(c >> 16 & 255, c >> 8 & 255, c & 255, Math.round(255.0F * a));
   }

   private int skyAt(float el, float x, float y, float z) {
      int c;
      if (el < 0.0F) {
         c = Colors.lerp(this.horizon, this.below, Mth.clamp(-el / 8.0F, 0.0F, 1.0F));
      } else if (el < 8.0F) {
         c = Colors.lerp(this.horizon, this.low, smooth(el / 8.0F));
      } else if (el < 40.0F) {
         c = Colors.lerp(this.low, this.high, smooth((el - 8.0F) / 32.0F));
      } else {
         c = Colors.lerp(this.high, this.zenith, smooth((el - 40.0F) / 50.0F));
      }

      this.scratch.set(x, y, z);
      float toward = Math.max(0.0F, this.scratch.dot(this.sun));
      float warm = (float)Math.pow((double)toward, (double)6.0F) * this.sunGlow * (0.45F + 0.55F * (1.0F - Mth.clamp(el / 30.0F, 0.0F, 1.0F)));
      if (warm > 0.003F) {
         c = Colors.lerp(c, this.sunColor, Mth.clamp(warm, 0.0F, 0.85F));
      }

      return c;
   }

   private static float smooth(float t) {
      float u = Mth.clamp(t, 0.0F, 1.0F);
      return u * u * (3.0F - 2.0F * u);
   }

   private void fan(BufferBuilder bb, Matrix4f m, float cx, float cy, float cz, float r0, float r1, int r, int g, int b, int a0, int a1) {
      int segments = 24;

      for(int i = 0; i < 24; ++i) {
         float t0 = (float)i / 24.0F * ((float)Math.PI * 2F);
         float t1 = (float)(i + 1) / 24.0F * ((float)Math.PI * 2F);
         float c0 = Mth.cos(t0);
         float s0 = Mth.sin(t0);
         float c1 = Mth.cos(t1);
         float s1 = Mth.sin(t1);
         bb.addVertex(m, cx + (this.right.x * c0 + this.up.x * s0) * r0, cy + (this.right.y * c0 + this.up.y * s0) * r0, cz + (this.right.z * c0 + this.up.z * s0) * r0).setColor(r, g, b, a0);
         bb.addVertex(m, cx + (this.right.x * c0 + this.up.x * s0) * r1, cy + (this.right.y * c0 + this.up.y * s0) * r1, cz + (this.right.z * c0 + this.up.z * s0) * r1).setColor(r, g, b, a1);
         bb.addVertex(m, cx + (this.right.x * c1 + this.up.x * s1) * r1, cy + (this.right.y * c1 + this.up.y * s1) * r1, cz + (this.right.z * c1 + this.up.z * s1) * r1).setColor(r, g, b, a1);
         bb.addVertex(m, cx + (this.right.x * c1 + this.up.x * s1) * r0, cy + (this.right.y * c1 + this.up.y * s1) * r0, cz + (this.right.z * c1 + this.up.z * s1) * r0).setColor(r, g, b, a0);
      }

   }
}
