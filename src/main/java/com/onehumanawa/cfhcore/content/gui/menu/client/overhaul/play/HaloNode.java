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

final class HaloNode extends StageNode {
   static final float RADIUS = 0.82F;
   static final float WIDTH = 0.058F;
   static final float INNER = 0.46F;
   static final float OUTER = 1.2F;
   private static final int DASHES = 18;
   private static final int SEGMENTS = 36;
   private static final float[] BAND_AT = new float[]{0.46F, 0.64F, 0.75F, 0.82F, 0.89F, 1.02F, 1.2F};
   private static final float[] BAND_LIGHT = new float[]{0.0F, 0.07F, 0.19F, 0.3F, 0.19F, 0.06F, 0.0F};
   private final int color;

   HaloNode(int argb) {
      this.color = argb;
      this.bounds(-1.2F, -0.01F, -1.2F, 1.2F, 0.01F, 1.2F);
      this.pickable(false);
      this.hoverFeel(0.0F, 1.0F);
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
         BufferBuilder bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

         for(int ring = 0; ring < BAND_AT.length - 1; ++ring) {
            float r0 = BAND_AT[ring];
            float r1 = BAND_AT[ring + 1];
            int a0 = Math.round(255.0F * a * BAND_LIGHT[ring]);
            int a1 = Math.round(255.0F * a * BAND_LIGHT[ring + 1]);

            for(int i = 0; i < 36; ++i) {
               float t0 = (float)i / 36.0F * ((float)Math.PI * 2F);
               float t1 = (float)(i + 1) / 36.0F * ((float)Math.PI * 2F);
               bb.addVertex(m, Mth.sin(t0) * r0, 0.0F, Mth.cos(t0) * r0).setColor(r, g, b, a0);
               bb.addVertex(m, Mth.sin(t0) * r1, 0.0F, Mth.cos(t0) * r1).setColor(r, g, b, a1);
               bb.addVertex(m, Mth.sin(t1) * r1, 0.0F, Mth.cos(t1) * r1).setColor(r, g, b, a1);
               bb.addVertex(m, Mth.sin(t1) * r0, 0.0F, Mth.cos(t1) * r0).setColor(r, g, b, a0);
            }
         }

         float r0 = 0.791F;
         float r1 = 0.849F;
         int lit = Math.round(255.0F * a);

         for(int i = 0; i < 18; ++i) {
            for(int piece = 0; piece < 3; ++piece) {
               float t0 = ((float)i + 0.2F + 0.2F * (float)piece) / 18.0F * ((float)Math.PI * 2F);
               float t1 = ((float)i + 0.4F + 0.2F * (float)piece) / 18.0F * ((float)Math.PI * 2F);
               bb.addVertex(m, Mth.sin(t0) * 0.791F, 0.0F, Mth.cos(t0) * 0.791F).setColor(r, g, b, lit);
               bb.addVertex(m, Mth.sin(t0) * 0.849F, 0.0F, Mth.cos(t0) * 0.849F).setColor(r, g, b, lit);
               bb.addVertex(m, Mth.sin(t1) * 0.849F, 0.0F, Mth.cos(t1) * 0.849F).setColor(r, g, b, lit);
               bb.addVertex(m, Mth.sin(t1) * 0.791F, 0.0F, Mth.cos(t1) * 0.791F).setColor(r, g, b, lit);
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
