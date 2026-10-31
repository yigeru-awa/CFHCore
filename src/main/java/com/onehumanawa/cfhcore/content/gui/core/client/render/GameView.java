package com.onehumanawa.cfhcore.content.gui.core.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public final class GameView {
   public static final int MAX_WIDTH = 1280;
   private static final long KEEP_MS = 500L;
   private static @Nullable TextureTarget copy;
   private static long wantedUntil;
   private static boolean hasFrame;

   public static void request() {
      wantedUntil = Util.getMillis() + 500L;
   }

   public static boolean hasFrame() {
      return hasFrame && copy != null;
   }

   public static int width() {
      return copy == null ? 0 : copy.width;
   }

   public static int height() {
      return copy == null ? 0 : copy.height;
   }

   public static void afterLevelRender() {
      if (Util.getMillis() > wantedUntil) {
         if (copy != null) {
            release();
         }

      } else {
         Minecraft mc = Minecraft.getInstance();
         RenderTarget main = mc.getMainRenderTarget();
         int mw = main.width;
         int mh = main.height;
         if (mw > 0 && mh > 0) {
            int scale = Math.max(1, (int)Math.ceil((double)mw / (double)1280.0F));
            int cw = Math.max(1, mw / scale);
            int ch = Math.max(1, mh / scale);
            if (copy == null || copy.width != cw || copy.height != ch) {
               if (copy != null) {
                  copy.destroyBuffers();
               }

               copy = new TextureTarget(cw, ch, false, Minecraft.ON_OSX);
               copy.setFilterMode(9729);
            }

            GlStateManager._glBindFramebuffer(36008, main.frameBufferId);
            GlStateManager._glBindFramebuffer(36009, copy.frameBufferId);
            GlStateManager._glBlitFrameBuffer(0, 0, mw, mh, 0, 0, cw, ch, 16384, 9729);
            main.bindWrite(true);
            hasFrame = true;
         }
      }
   }

   public static boolean draw(GuiGraphics g, int x, int y, int w, int h, float alpha) {
      return drawRegion(g, x, y, w, h, 0.0F, 1.0F, 0.0F, 1.0F, alpha);
   }

   public static boolean drawCover(GuiGraphics g, int x, int y, int w, int h, float alpha) {
      int fw = width();
      int fh = height();
      if (fw > 0 && fh > 0 && w > 0 && h > 0) {
         float frame = (float)fw / (float)fh;
         float target = (float)w / (float)h;
         float u0 = 0.0F;
         float u1 = 1.0F;
         float v0 = 0.0F;
         float v1 = 1.0F;
         if (frame > target) {
            float keep = target / frame;
            u0 = (1.0F - keep) / 2.0F;
            u1 = 1.0F - u0;
         } else if (frame < target) {
            float keep = frame / target;
            v0 = (1.0F - keep) / 2.0F;
            v1 = 1.0F - v0;
         }

         return drawRegion(g, x, y, w, h, u0, u1, v0, v1, alpha);
      } else {
         return false;
      }
   }

   private static boolean drawRegion(GuiGraphics g, int x, int y, int w, int h, float u0, float u1, float v0, float v1, float alpha) {
      if (hasFrame() && w > 0 && h > 0 && !(alpha <= 0.004F)) {
         g.flush();
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderTexture(0, copy.getColorTextureId());
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         Matrix4f m = g.pose().last().pose();
         BufferBuilder b = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
         b.addVertex(m, (float)x, (float)y, 0.0F).setUv(u0, v1);
         b.addVertex(m, (float)x, (float)(y + h), 0.0F).setUv(u0, v0);
         b.addVertex(m, (float)(x + w), (float)(y + h), 0.0F).setUv(u1, v0);
         b.addVertex(m, (float)(x + w), (float)y, 0.0F).setUv(u1, v1);
         BufferUploader.drawWithShader(b.buildOrThrow());
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         return true;
      } else {
         return false;
      }
   }

   public static int[] fit(int w, int h) {
      int fw = width();
      int fh = height();
      if (fw > 0 && fh > 0) {
         int rw = w;
         int rh = Math.round((float)w * ((float)fh / (float)fw));
         if (rh > h) {
            rh = h;
            rw = Math.round((float)h * ((float)fw / (float)fh));
         }

         return new int[]{(w - rw) / 2, (h - rh) / 2, rw, rh};
      } else {
         return new int[]{0, 0, w, h};
      }
   }

   public static void release() {
      if (copy != null) {
         copy.destroyBuffers();
         copy = null;
      }

      hasFrame = false;
   }

   private GameView() {
   }
}
