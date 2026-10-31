package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.play;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageBlend;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

final class OrbitNode extends StageNode {
   private static final int DASHES = 72;
   private final float radius;
   private final float width;
   private int color;

   OrbitNode(float radius, float width, int argb) {
      this.radius = radius;
      this.width = width;
      this.color = argb;
      this.bounds(-radius, -0.01F, -radius, radius, 0.01F, radius);
      this.pickable(false);
   }

   OrbitNode color(int argb) {
      this.color = argb;
      return this;
   }

   protected void draw(StageRenderContext ctx) {
   }

   public boolean hasTranslucentPass() {
      return true;
   }

   protected void drawTranslucent(StageRenderContext ctx) {
      float a = this.alpha() * (float)(this.color >>> 24 & 255) / 255.0F;
      if (!(a <= 0.004F)) {
         Matrix4f m = ctx.pose.last().pose();
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         RenderSystem.enableBlend();
         StageBlend.add();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.disableCull();
         int r = this.color >> 16 & 255;
         int g = this.color >> 8 & 255;
         int b = this.color & 255;
         float r0 = this.radius - this.width / 2.0F;
         float r1 = this.radius + this.width / 2.0F;
         BufferBuilder bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

         for(int i = 0; i < 72; ++i) {
            float a0 = ((float)i + 0.22F) / 72.0F * ((float)Math.PI * 2F);
            float a1 = ((float)i + 0.78F) / 72.0F * ((float)Math.PI * 2F);
            float front = (1.0F + Mth.cos((a0 + a1) / 2.0F)) / 2.0F;
            int alpha = Math.round(255.0F * a * (0.22F + 0.78F * front * front));
            bb.addVertex(m, Mth.sin(a0) * r0, 0.0F, Mth.cos(a0) * r0).setColor(r, g, b, alpha);
            bb.addVertex(m, Mth.sin(a0) * r1, 0.0F, Mth.cos(a0) * r1).setColor(r, g, b, alpha);
            bb.addVertex(m, Mth.sin(a1) * r1, 0.0F, Mth.cos(a1) * r1).setColor(r, g, b, alpha);
            bb.addVertex(m, Mth.sin(a1) * r0, 0.0F, Mth.cos(a1) * r0).setColor(r, g, b, alpha);
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
