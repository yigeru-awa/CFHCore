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

public class BurstNode extends StageNode {
   private final float[] vx;
   private final float[] vy;
   private final float[] vz;
   private final float[] life;
   private final float[] size;
   private final float[] twinkle;
   private int color;
   private float firedAtMs = -1.0F;
   private boolean pending;
   private final Vector3f right = new Vector3f();
   private final Vector3f up = new Vector3f();

   public BurstNode(int count, float reach, float size, int argb, long seed) {
      Random rnd = new Random(seed);
      this.color = argb;
      this.vx = new float[count];
      this.vy = new float[count];
      this.vz = new float[count];
      this.life = new float[count];
      this.size = new float[count];
      this.twinkle = new float[count];

      for(int i = 0; i < count; ++i) {
         float angle = rnd.nextFloat() * ((float)Math.PI * 2F);
         float out = (0.25F + rnd.nextFloat() * 0.75F) * reach;
         this.vx[i] = Mth.sin(angle) * out;
         this.vz[i] = Mth.cos(angle) * out * 0.6F;
         this.vy[i] = (0.7F + rnd.nextFloat() * 1.1F) * reach;
         this.life[i] = 520.0F + rnd.nextFloat() * 520.0F;
         this.size[i] = size * (0.6F + rnd.nextFloat() * 0.8F);
         this.twinkle[i] = rnd.nextFloat() * ((float)Math.PI * 2F);
      }

      this.bounds(-reach, 0.0F, -reach, reach, reach * 2.0F, reach);
      this.pickable(false);
      this.hoverFeel(0.0F, 1.0F);
      this.named("sparks");
   }

   public BurstNode color(int argb) {
      this.color = argb;
      return this;
   }

   public void fire() {
      this.pending = true;
   }

   public void update(StageRenderContext ctx) {
      if (this.pending) {
         this.pending = false;
         this.firedAtMs = ctx.timeMs;
      }

   }

   protected void draw(StageRenderContext ctx) {
   }

   public boolean hasTranslucentPass() {
      return true;
   }

   protected void drawTranslucent(StageRenderContext ctx) {
      float motion = Theme.current().motion();
      if (!(this.firedAtMs < 0.0F) && !(motion <= 0.0F)) {
         float age = (ctx.timeMs - this.firedAtMs) / motion;
         if (age > 1100.0F) {
            this.firedAtMs = -1.0F;
         } else {
            float base = (float)(this.color >>> 24 & 255) / 255.0F * this.alpha();
            if (!(base <= 0.004F)) {
               Matrix4f m = ctx.pose.last().pose();
               Matrix4f view = ctx.camera.view();
               this.right.set(view.m00(), view.m10(), view.m20());
               this.up.set(view.m01(), view.m11(), view.m21());
               this.modelInverse.transformDirection(this.right).normalize();
               this.modelInverse.transformDirection(this.up).normalize();
               int r = this.color >> 16 & 255;
               int g = this.color >> 8 & 255;
               int b = this.color & 255;
               RenderSystem.setShader(GameRenderer::getPositionColorShader);
               RenderSystem.enableBlend();
               StageBlend.add();
               RenderSystem.enableDepthTest();
               RenderSystem.depthMask(false);
               RenderSystem.disableCull();
               BufferBuilder bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

               for(int i = 0; i < this.vx.length; ++i) {
                  float t = age / this.life[i];
                  if (!(t >= 1.0F)) {
                     float travel = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
                     float seconds = age / 1000.0F;
                     float px = this.vx[i] * travel * 0.5F;
                     float pz = this.vz[i] * travel * 0.5F;
                     float py = this.vy[i] * travel * 0.5F - 0.55F * seconds * seconds;
                     float fade = (1.0F - t) * (0.65F + 0.35F * Mth.sin(age * 0.03F + this.twinkle[i]));
                     int pa = Math.round(255.0F * base * Math.max(0.0F, fade));
                     if (pa > 0) {
                        float s = this.size[i] * (1.0F - 0.5F * t) / 2.0F;
                        float rx = this.right.x * s;
                        float ry = this.right.y * s;
                        float rz = this.right.z * s;
                        float ux = this.up.x * s;
                        float uy = this.up.y * s;
                        float uz = this.up.z * s;
                        bb.addVertex(m, px - rx, py - ry, pz - rz).setColor(r, g, b, pa);
                        bb.addVertex(m, px - ux, py - uy, pz - uz).setColor(r, g, b, pa);
                        bb.addVertex(m, px + rx, py + ry, pz + rz).setColor(255, 255, 255, pa);
                        bb.addVertex(m, px + ux, py + uy, pz + uz).setColor(r, g, b, pa);
                     }
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
         }
      }
   }
}
