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

public class MotesNode extends StageNode {
   private final float[] x;
   private final float[] y;
   private final float[] z;
   private final float[] rise;
   private final float[] sway;
   private final float[] swaySpeed;
   private final float[] phase;
   private final float[] size;
   private final float spreadY;
   private int color;
   private float strength = 1.0F;
   private float speed = 1.0F;
   private float swayScale = 1.0F;
   private float sizeScale = 1.0F;
   private float steady;
   private boolean additive = true;
   private final Vector3f right = new Vector3f();
   private final Vector3f up = new Vector3f();

   public MotesNode(int count, float spreadX, float spreadY, float spreadZ, float size, int argb, long seed) {
      Random rnd = new Random(seed);
      this.spreadY = spreadY;
      this.color = argb;
      this.x = new float[count];
      this.y = new float[count];
      this.z = new float[count];
      this.rise = new float[count];
      this.sway = new float[count];
      this.swaySpeed = new float[count];
      this.phase = new float[count];
      this.size = new float[count];

      for(int i = 0; i < count; ++i) {
         this.x[i] = (rnd.nextFloat() - 0.5F) * spreadX;
         this.y[i] = rnd.nextFloat() * spreadY;
         this.z[i] = (rnd.nextFloat() - 0.5F) * spreadZ;
         this.rise[i] = 0.018F + rnd.nextFloat() * 0.05F;
         this.sway[i] = 0.03F + rnd.nextFloat() * 0.09F;
         this.swaySpeed[i] = 0.25F + rnd.nextFloat() * 0.6F;
         this.phase[i] = rnd.nextFloat() * ((float)Math.PI * 2F);
         this.size[i] = size * (0.55F + rnd.nextFloat() * 0.9F);
      }

      this.bounds(-spreadX / 2.0F, 0.0F, -spreadZ / 2.0F, spreadX / 2.0F, spreadY, spreadZ / 2.0F);
      this.pickable(false);
      this.hoverFeel(0.0F, 1.0F);
      this.named("motes");
   }

   public MotesNode color(int argb) {
      this.color = argb;
      return this;
   }

   public MotesNode strength(float s) {
      this.strength = Mth.clamp(s, 0.0F, 1.0F);
      return this;
   }

   public MotesNode rise(float factor) {
      this.speed = factor;
      return this;
   }

   public MotesNode sway(float factor) {
      this.swayScale = Math.max(0.0F, factor);
      return this;
   }

   public MotesNode grow(float factor) {
      this.sizeScale = Math.max(0.0F, factor);
      return this;
   }

   public MotesNode steady(float amount) {
      this.steady = Mth.clamp(amount, 0.0F, 1.0F);
      return this;
   }

   public MotesNode additive(boolean on) {
      this.additive = on;
      return this;
   }

   protected void draw(StageRenderContext ctx) {
   }

   public boolean hasTranslucentPass() {
      return true;
   }

   protected void drawTranslucent(StageRenderContext ctx) {
      float a = (float)(this.color >>> 24 & 255) / 255.0F * this.strength * this.alpha();
      if (!(a <= 0.004F) && this.x.length != 0) {
         Matrix4f m = ctx.pose.last().pose();
         Matrix4f view = ctx.camera.view();
         this.right.set(view.m00(), view.m10(), view.m20());
         this.up.set(view.m01(), view.m11(), view.m21());
         this.modelInverse.transformDirection(this.right).normalize();
         this.modelInverse.transformDirection(this.up).normalize();
         float t = Theme.current().motion() > 0.0F ? ctx.seconds() : 0.0F;
         int r = this.color >> 16 & 255;
         int g = this.color >> 8 & 255;
         int b = this.color & 255;
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         RenderSystem.enableBlend();
         if (this.additive) {
            StageBlend.add();
         } else {
            StageBlend.over();
         }

         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.disableCull();
         BufferBuilder bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

         for(int i = 0; i < this.x.length; ++i) {
            float py = Mth.positiveModulo(this.y[i] + this.rise[i] * this.speed * t, this.spreadY);
            float px = this.x[i] + Mth.sin(t * this.swaySpeed[i] + this.phase[i]) * this.sway[i] * this.swayScale;
            float pz = this.z[i] + Mth.cos(t * this.swaySpeed[i] * 0.8F + this.phase[i] * 1.7F) * this.sway[i] * this.swayScale;
            float edge = Math.min(1.0F, Math.min(py, this.spreadY - py) / (this.spreadY * 0.18F));
            float breath = Math.max(0.0F, 0.45F + 0.55F * Mth.sin(t * 0.7F * this.swaySpeed[i] + this.phase[i] * 2.3F));
            int pa = Math.round(255.0F * a * Math.max(0.0F, edge) * (breath + (1.0F - Math.min(1.0F, breath)) * this.steady));
            if (pa > 0) {
               float s = this.size[i] / 2.0F * this.sizeScale;
               float rx = this.right.x * s;
               float ry = this.right.y * s;
               float rz = this.right.z * s;
               float ux = this.up.x * s;
               float uy = this.up.y * s;
               float uz = this.up.z * s;
               bb.addVertex(m, px - rx - ux, py - ry - uy, pz - rz - uz).setColor(r, g, b, pa);
               bb.addVertex(m, px + rx - ux, py + ry - uy, pz + rz - uz).setColor(r, g, b, pa);
               bb.addVertex(m, px + rx + ux, py + ry + uy, pz + rz + uz).setColor(r, g, b, pa);
               bb.addVertex(m, px - rx + ux, py - ry + uy, pz - rz + uz).setColor(r, g, b, pa);
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
