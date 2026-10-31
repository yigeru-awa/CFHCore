package com.onehumanawa.cfhcore.content.gui.core.stage.node;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class HazeNode extends StageNode {
   private static final int BANDS = 24;
   private static final Matrix4f IDENTITY = new Matrix4f();
   private int sky = -15194297;
   private int haze = -2119036;
   private final Vector3f line = new Vector3f();
   private final Vector3f ndc = new Vector3f();
   private float least = 0.25F;

   public HazeNode() {
      this.pickable(false);
      this.named("sky");
   }

   public HazeNode colors(int sky, int haze) {
      this.sky = sky;
      this.haze = haze;
      return this;
   }

   public HazeNode line(float x, float y, float z) {
      this.line.set(x, y, z);
      return this;
   }

   public HazeNode least(float part) {
      this.least = Mth.clamp(part, 0.05F, 1.0F);
      return this;
   }

   private int at(float t) {
      float u = Mth.clamp(t, 0.0F, 1.0F);
      return Colors.lerp(this.haze, this.sky, u * u * (3.0F - 2.0F * u));
   }

   protected void draw(StageRenderContext ctx) {
      float y = ctx.camera.project(this.line.x, this.line.y, this.line.z, this.ndc) ? Mth.clamp(this.ndc.y, -1.0F, 1.0F) : -1.0F;
      float span = Math.max(this.least * 2.0F, 1.0F - y);
      float[] was = (float[])RenderSystem.getShaderColor().clone();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.disableCull();
      RenderSystem.setProjectionMatrix(IDENTITY, VertexSorting.ORTHOGRAPHIC_Z);
      BufferBuilder bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      float a = this.alpha();
      if (y > -1.0F) {
         band(bb, -1.0F, y, this.haze, this.haze, a);
      }

      float y0 = y;
      int c0 = this.haze;

      for(int i = 1; i <= 24; ++i) {
         float y1 = y + (1.0F - y) * (float)i / 24.0F;
         int c1 = this.at((y1 - y) / span);
         band(bb, y0, y1, c0, c1, a);
         y0 = y1;
         c0 = c1;
      }

      BufferUploader.drawWithShader(bb.buildOrThrow());
      RenderSystem.setProjectionMatrix(ctx.camera.projection(), VertexSorting.DISTANCE_TO_ORIGIN);
      RenderSystem.setShaderColor(was[0], was[1], was[2], was[3]);
      RenderSystem.enableCull();
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
   }

   private static void band(BufferBuilder bb, float y0, float y1, int bottom, int top, float alpha) {
      int b = Colors.scaleAlpha(bottom, alpha);
      int t = Colors.scaleAlpha(top, alpha);
      bb.addVertex(-1.0F, y1, 0.0F).setColor(t);
      bb.addVertex(-1.0F, y0, 0.0F).setColor(b);
      bb.addVertex(1.0F, y0, 0.0F).setColor(b);
      bb.addVertex(1.0F, y1, 0.0F).setColor(t);
   }
}
