package com.onehumanawa.cfhcore.content.gui.core.gfx;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Style;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

public final class Frost {
   private static final float REACH = 3.0F;
   private static final int CAP = 80;
   private static final int MAX_LEVELS = 5;
   private static final int BACK_UP = 2;
   private static final TextureTarget[] LEVELS = new TextureTarget[5];
   private static final int[] SCISSOR_WAS = new int[4];
   private static final Vector3f CORNER = new Vector3f();
   private static boolean failed;

   public static boolean available() {
      if (!failed && Theme.current().blurInGame()) {
         Minecraft mc = Minecraft.getInstance();
         return mc.options.getMenuBackgroundBlurriness() > 0 && GlStateManager.getBoundFramebuffer() == mc.getMainRenderTarget().frameBufferId;
      } else {
         return false;
      }
   }

   public static boolean under(GuiGraphics g, int x, int y, int w, int h, int radius, float alpha) {
      if (w > 0 && h > 0 && !(alpha <= 0.004F) && available()) {
         try {
            return frost(g, x, y, w, h, radius, Mth.clamp(alpha, 0.0F, 1.0F));
         } catch (RuntimeException e) {
            failed = true;
            CFHGui.LOGGER.warn("[CFHGui] frosted plates are off for this session (plates lay a plain shade): {}", e.toString());
            RenderSystem.blendEquation(32774);
            GlStateManager._colorMask(true, true, true, true);
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean frost(GuiGraphics g, int x, int y, int w, int h, int radius, float alpha) {
      Minecraft mc = Minecraft.getInstance();
      RenderTarget main = mc.getMainRenderTarget();
      int fw = main.width;
      int fh = main.height;
      float scale = (float)mc.getWindow().getGuiScale();
      if (fw >= 16 && fh >= 16 && !(scale <= 0.0F)) {
         int levels = Mth.clamp((int)Math.round(Math.log((double)(3.0F * scale)) / Math.log((double)2.0F)), 1, 5);
         int from = Math.max(1, levels - 2);
         Matrix4f pose = g.pose().last().pose();
         pose.transformPosition((float)x, (float)y, 0.0F, CORNER);
         float ax = CORNER.x;
         float ay = CORNER.y;
         pose.transformPosition((float)(x + w), (float)(y + h), 0.0F, CORNER);
         int reach = 2 << levels;
         int px0 = Mth.floor(Math.min(ax, CORNER.x) * scale) - reach;
         int px1 = Mth.ceil(Math.max(ax, CORNER.x) * scale) + reach;
         int py0 = fh - Mth.ceil(Math.max(ay, CORNER.y) * scale) - reach;
         int py1 = fh - Mth.floor(Math.min(ay, CORNER.y) * scale) + reach;
         if (px1 > 0 && py1 > 0 && px0 < fw && py0 < fh) {
            g.flush();

            for(int i = 1; i <= levels; ++i) {
               level(i, fw, fh);
            }

            boolean scissorWas = GL11.glIsEnabled(3089);
            GL11.glGetIntegerv(3088, SCISSOR_WAS);
            GlStateManager._enableScissorTest();
            int read = main.frameBufferId;
            int rw = fw;
            int rh = fh;

            for(int i = 1; i <= levels; ++i) {
               TextureTarget t = LEVELS[i - 1];
               blit(read, rw, rh, t, px0, py0, px1, py1, i);
               read = t.frameBufferId;
               rw = t.width;
               rh = t.height;
            }

            for(int i = levels - 1; i >= from; --i) {
               TextureTarget t = LEVELS[i - 1];
               blit(read, rw, rh, t, px0, py0, px1, py1, i);
               read = t.frameBufferId;
               rw = t.width;
               rh = t.height;
            }

            TextureTarget blurred = LEVELS[from - 1];
            GlStateManager._glBindFramebuffer(36160, blurred.frameBufferId);
            scissor(px0, py0, px1, py1, from, blurred);
            GlStateManager._colorMask(false, false, false, true);
            GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager._clear(16384, Minecraft.ON_OSX);
            GlStateManager._colorMask(true, true, true, true);
            main.bindWrite(true);
            if (scissorWas) {
               GlStateManager._scissorBox(SCISSOR_WAS[0], SCISSOR_WAS[1], SCISSOR_WAS[2], SCISSOR_WAS[3]);
            } else {
               GlStateManager._disableScissorTest();
            }

            int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
            boolean blendWas = GL11.glIsEnabled(3042);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, blurred.getColorTextureId());
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            BufferBuilder tex = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            shape(tex, pose, x, y, w, h, r, scale / (float)fw, scale / (float)fh, 0);
            draw(tex);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            int cap = Math.round(Mth.lerp(alpha, 255.0F, 80.0F));
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.blendEquation(32775);
            BufferBuilder min = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            shape(min, pose, x, y, w, h, r, 0.0F, 0.0F, -16777216 | cap << 16 | cap << 8 | cap);
            draw(min);
            RenderSystem.blendEquation(32774);
            if (!blendWas) {
               RenderSystem.disableBlend();
            }

            return true;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private static void draw(BufferBuilder b) {
      MeshData mesh = b.build();
      if (mesh != null) {
         BufferUploader.drawWithShader(mesh);
      }

   }

   private static TextureTarget level(int i, int fw, int fh) {
      int w = Math.max(1, fw + (1 << i) - 1 >> i);
      int h = Math.max(1, fh + (1 << i) - 1 >> i);
      TextureTarget t = LEVELS[i - 1];
      if (t == null || t.width != w || t.height != h) {
         if (t != null) {
            t.destroyBuffers();
         }

         t = new TextureTarget(w, h, false, Minecraft.ON_OSX);
         t.setFilterMode(9729);
         LEVELS[i - 1] = t;
      }

      return t;
   }

   private static void blit(int read, int rw, int rh, TextureTarget to, int px0, int py0, int px1, int py1, int level) {
      GlStateManager._glBindFramebuffer(36008, read);
      GlStateManager._glBindFramebuffer(36009, to.frameBufferId);
      scissor(px0, py0, px1, py1, level, to);
      GlStateManager._glBlitFrameBuffer(0, 0, rw, rh, 0, 0, to.width, to.height, 16384, 9729);
   }

   private static void scissor(int px0, int py0, int px1, int py1, int level, TextureTarget t) {
      int x0 = Math.max(0, (px0 >> level) - 1);
      int y0 = Math.max(0, (py0 >> level) - 1);
      int x1 = Math.min(t.width, (px1 >> level) + 2);
      int y1 = Math.min(t.height, (py1 >> level) + 2);
      GlStateManager._scissorBox(x0, y0, Math.max(0, x1 - x0), Math.max(0, y1 - y0));
   }

   private static void shape(BufferBuilder b, Matrix4f pose, int x, int y, int w, int h, int r, float su, float sv, int color) {
      quad(b, pose, (float)x, (float)(y + r), (float)w, (float)(h - 2 * r), su, sv, color);
      if (r > 0 && SmoothCorners.active()) {
         int s = SmoothCorners.scale();
         int rows = r * s;
         float row = 1.0F / (float)s;

         for(int i = 0; i < rows; ++i) {
            float inset = SmoothCorners.inset(rows, i) / (float)s;
            quad(b, pose, (float)x + inset, (float)y + (float)i * row, (float)w - 2.0F * inset, row, su, sv, color);
            quad(b, pose, (float)x + inset, (float)(y + h) - (float)(i + 1) * row, (float)w - 2.0F * inset, row, su, sv, color);
         }

      } else {
         for(int i = 0; i < r; ++i) {
            int inset = r - i;
            quad(b, pose, (float)(x + inset), (float)(y + i), (float)(w - 2 * inset), 1.0F, su, sv, color);
            quad(b, pose, (float)(x + inset), (float)(y + h - 1 - i), (float)(w - 2 * inset), 1.0F, su, sv, color);
         }

      }
   }

   private static void quad(BufferBuilder b, Matrix4f pose, float x, float y, float w, float h, float su, float sv, int color) {
      if (!(w <= 0.0F) && !(h <= 0.0F)) {
         vertex(b, pose, x, y, su, sv, color);
         vertex(b, pose, x, y + h, su, sv, color);
         vertex(b, pose, x + w, y + h, su, sv, color);
         vertex(b, pose, x + w, y, su, sv, color);
      }
   }

   private static void vertex(BufferBuilder b, Matrix4f pose, float x, float y, float su, float sv, int color) {
      if (color != 0) {
         b.addVertex(pose, x, y, 0.0F).setColor(color);
      } else {
         pose.transformPosition(x, y, 0.0F, CORNER);
         b.addVertex(pose, x, y, 0.0F).setUv(CORNER.x * su, 1.0F - CORNER.y * sv);
      }
   }

   public static void release() {
      for(int i = 0; i < LEVELS.length; ++i) {
         if (LEVELS[i] != null) {
            LEVELS[i].destroyBuffers();
            LEVELS[i] = null;
         }
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc.getMainRenderTarget() != null) {
         mc.getMainRenderTarget().bindWrite(true);
      }

   }

   private Frost() {
   }

   static {
      Theme.onChange(() -> {
         if (!Theme.variant(Style.CFHGUI).translucentPanels() && RenderSystem.isOnRenderThread()) {
            release();
         }

      });
   }
}
