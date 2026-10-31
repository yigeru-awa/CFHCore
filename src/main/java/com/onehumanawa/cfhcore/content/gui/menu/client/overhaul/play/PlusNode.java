package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.play;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

final class PlusNode extends StageNode {
   private static final float ARM = 0.5F;
   private static final int GLYPH_FROM = 3;
   private static final int GLYPH_TO = 13;
   private final Quaternionf facing = new Quaternionf();
   private int color;

   PlusNode(int argb) {
      this.color = argb;
      this.bounds(-0.5F, -0.5F, -0.5F, 0.5F, 0.5F, 0.5F);
      this.hoverFeel(0.06F, 1.1F);
   }

   PlusNode color(int argb) {
      this.color = argb;
      return this;
   }

   protected void draw(StageRenderContext ctx) {
      ctx.pose.pushPose();
      ctx.pose.last().pose().getNormalizedRotation(this.facing);
      this.facing.conjugate();
      ctx.pose.mulPose(this.facing);
      Matrix4f m = ctx.pose.last().pose();
      int lit = Colors.lerp(this.color, -1, 0.3F + 0.3F * this.hover());
      int r = lit >> 16 & 255;
      int g = lit >> 8 & 255;
      int b = lit & 255;
      int a = Math.round((float)(lit >>> 24 & 255) * this.alpha());
      float atlasW = 256.0F;
      float atlasH = (float)(Icon.rows() * 16);
      float u0 = (float)(Icon.PLUS.u() + 3) / 256.0F;
      float u1 = (float)(Icon.PLUS.u() + 13) / 256.0F;
      float v0 = (float)(Icon.PLUS.v() + 3) / atlasH;
      float v1 = (float)(Icon.PLUS.v() + 13) / atlasH;
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      RenderSystem.setShaderTexture(0, Icons.ATLAS);
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.disableCull();
      BufferBuilder bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      bb.addVertex(m, -0.5F, -0.5F, 0.0F).setUv(u0, v1).setColor(r, g, b, a);
      bb.addVertex(m, 0.5F, -0.5F, 0.0F).setUv(u1, v1).setColor(r, g, b, a);
      bb.addVertex(m, 0.5F, 0.5F, 0.0F).setUv(u1, v0).setColor(r, g, b, a);
      bb.addVertex(m, -0.5F, 0.5F, 0.0F).setUv(u0, v0).setColor(r, g, b, a);
      MeshData mesh = bb.build();
      if (mesh != null) {
         BufferUploader.drawWithShader(mesh);
      }

      RenderSystem.enableCull();
      ctx.pose.popPose();
   }
}
