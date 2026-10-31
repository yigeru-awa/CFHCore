package com.onehumanawa.cfhcore.content.gui.core.gfx;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public final class SmoothCorners {
   private static final int SAMPLES = 16;
   private static final int MAX_RADIUS = 64;
   private static final int MAX_SCALE = 32;
   private static final Mask[][] MASKS = new Mask[SmoothCorners.Kind.values().length][2145];
   private static boolean failed;

   public static boolean active() {
      return !failed && Theme.current().smoothCorners();
   }

   public static boolean circles() {
      return active() && !Theme.current().isVanilla();
   }

   public static int scale() {
      return Math.max(1, Math.min(32, (int)Math.round(Minecraft.getInstance().getWindow().getGuiScale())));
   }

   private static int slot(int r, int s) {
      return r * 33 + s;
   }

   public static float inset(int r, int row) {
      double dy = (double)r - ((double)row + (double)0.5F);
      return (float)((double)r - Math.sqrt(Math.max((double)0.0F, (double)r * (double)r - dy * dy)));
   }

   static boolean fill(GuiGraphics g, int x, int y, int w, int h, int color, int r) {
      Mask m = mask(SmoothCorners.Kind.FILL, r);
      if (m == null) {
         return false;
      } else {
         BufferBuilder b = begin(g, m);
         Matrix4f pose = g.pose().last().pose();
         corners(b, pose, m, x, y, w, h, r, color);
         solid(b, pose, m, x + r, y, w - 2 * r, r, color);
         solid(b, pose, m, x, y + r, w, h - 2 * r, color);
         solid(b, pose, m, x + r, y + h - r, w - 2 * r, r, color);
         end(b);
         return true;
      }
   }

   static boolean outline(GuiGraphics g, int x, int y, int w, int h, int color, int r) {
      Mask m = mask(SmoothCorners.Kind.RING, r);
      if (m == null) {
         return false;
      } else {
         BufferBuilder b = begin(g, m);
         Matrix4f pose = g.pose().last().pose();
         corners(b, pose, m, x, y, w, h, r, color);
         solid(b, pose, m, x + r, y, w - 2 * r, 1, color);
         solid(b, pose, m, x + r, y + h - 1, w - 2 * r, 1, color);
         solid(b, pose, m, x, y + r, 1, h - 2 * r, color);
         solid(b, pose, m, x + w - 1, y + r, 1, h - 2 * r, color);
         end(b);
         return true;
      }
   }

   static boolean shadow(GuiGraphics g, int x, int y, int w, int h, int color, int r) {
      if (w >= 2 * r + 2 && h >= 2 * r + 2) {
         Mask m = mask(SmoothCorners.Kind.SHADOW, r);
         if (m == null) {
            return false;
         } else {
            BufferBuilder b = begin(g, m);
            Matrix4f pose = g.pose().last().pose();
            piece(b, pose, m, 0, x + w - r, y + 2, r + 2, r, false, false, color);
            solid(b, pose, m, x + w, y + 2 + r, 2, h - 2 * r - 2, color);
            piece(b, pose, m, 1, x + w - r, y + h - r, r + 2, r + 2, false, false, color);
            solid(b, pose, m, x + 2 + r, y + h, w - 2 * r - 2, 2, color);
            piece(b, pose, m, 2, x + 2, y + h - r, r, r + 2, false, false, color);
            end(b);
            return true;
         }
      } else {
         return false;
      }
   }

   static boolean disc(GuiGraphics g, int cx, int cy, int r, int color) {
      return round(g, SmoothCorners.Kind.DISC, cx, cy, r, color);
   }

   static boolean ring(GuiGraphics g, int cx, int cy, int r, int color) {
      return round(g, SmoothCorners.Kind.HOOP, cx, cy, r, color);
   }

   private static boolean round(GuiGraphics g, Kind kind, int cx, int cy, int r, int color) {
      Mask m = mask(kind, r);
      if (m == null) {
         return false;
      } else {
         BufferBuilder b = begin(g, m);
         piece(b, g.pose().last().pose(), m, 0, cx - r, cy - r, 2 * r + 1, 2 * r + 1, false, false, color);
         end(b);
         return true;
      }
   }

   private static void corners(BufferBuilder b, Matrix4f pose, Mask m, int x, int y, int w, int h, int r, int color) {
      piece(b, pose, m, 0, x, y, r, r, false, false, color);
      piece(b, pose, m, 0, x + w - r, y, r, r, true, false, color);
      piece(b, pose, m, 0, x, y + h - r, r, r, false, true, color);
      piece(b, pose, m, 0, x + w - r, y + h - r, r, r, true, true, color);
   }

   private static void piece(BufferBuilder b, Matrix4f pose, Mask m, int piece, int x, int y, int w, int h, boolean mirrorX, boolean mirrorY, int color) {
      if (w > 0 && h > 0) {
         float u0 = m.u(piece, 0.0F);
         float u1 = m.u(piece, (float)m.w[piece]);
         float v0 = m.v(0.0F);
         float v1 = m.v((float)m.h[piece]);
         float ul = mirrorX ? u1 : u0;
         float ur = mirrorX ? u0 : u1;
         float vt = mirrorY ? v1 : v0;
         float vb = mirrorY ? v0 : v1;
         b.addVertex(pose, (float)x, (float)y, 0.0F).setUv(ul, vt).setColor(color);
         b.addVertex(pose, (float)x, (float)(y + h), 0.0F).setUv(ul, vb).setColor(color);
         b.addVertex(pose, (float)(x + w), (float)(y + h), 0.0F).setUv(ur, vb).setColor(color);
         b.addVertex(pose, (float)(x + w), (float)y, 0.0F).setUv(ur, vt).setColor(color);
      }
   }

   private static void solid(BufferBuilder b, Matrix4f pose, Mask m, int x, int y, int w, int h, int color) {
      if (w > 0 && h > 0) {
         float u = m.solidU();
         float v = m.solidV();
         b.addVertex(pose, (float)x, (float)y, 0.0F).setUv(u, v).setColor(color);
         b.addVertex(pose, (float)x, (float)(y + h), 0.0F).setUv(u, v).setColor(color);
         b.addVertex(pose, (float)(x + w), (float)(y + h), 0.0F).setUv(u, v).setColor(color);
         b.addVertex(pose, (float)(x + w), (float)y, 0.0F).setUv(u, v).setColor(color);
      }
   }

   private static BufferBuilder begin(GuiGraphics g, Mask m) {
      g.flush();
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      RenderSystem.setShaderTexture(0, m.texture());
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
      RenderSystem.enableDepthTest();
      RenderSystem.depthFunc(515);
      return Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
   }

   private static void end(BufferBuilder b) {
      MeshData mesh = b.build();
      if (mesh != null) {
         BufferUploader.drawWithShader(mesh);
      }

      RenderSystem.disableDepthTest();
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   }

   private static Mask mask(Kind kind, int r) {
      if (r >= 1 && r <= 64) {
         int s = scale();
         Mask[] made = MASKS[kind.ordinal()];
         Mask m = made[slot(r, s)];
         if (m != null) {
            return m;
         } else {
            try {
               m = make(pieces(kind, r, s));
            } catch (RuntimeException e) {
               failed = true;
               CFHGui.LOGGER.warn("[CFHGui] smooth corners are off for this session (the pixel steps stand in): {}", e.toString());
               return null;
            }

            made[slot(r, s)] = m;
            return m;
         }
      } else {
         return null;
      }
   }

   private static Piece[] pieces(Kind kind, int r, int t) {
      int R = r * t;
      double rr = (double)R * (double)R;
      Piece[] var10000;
      switch (kind.ordinal()) {
         case 0:
            var10000 = new Piece[]{new Piece(R, R, (px, py) -> inCorner(px, py, (double)R, (double)R, rr))};
            break;
         case 1:
            double ri = (double)(R - t);
            var10000 = new Piece[]{new Piece(R, R, (px, py) -> inCorner(px, py, (double)R, (double)R, rr) && (!(px >= (double)t) || !(py >= (double)t) || !(px >= (double)R) && !(py >= (double)R) && (!(ri > (double)0.0F) || !(sq((double)R - px) + sq((double)R - py) <= ri * ri))))};
            break;
         case 2:
            int d = 2 * t;
            var10000 = new Piece[]{new Piece(R + d, R, (px, py) -> {
               boolean inS = px <= (double)(R + d) && py >= (double)0.0F && (px <= (double)d || py >= (double)R || sq(px - (double)d) + sq(py - (double)R) <= rr);
               boolean inP = px <= (double)R && py >= (double)(-d) && (px <= (double)0.0F || py >= (double)(R - d) || sq(px) + sq(py - (double)(R - d)) <= rr);
               return inS && !inP;
            }), new Piece(R + d, R + d, (px, py) -> {
               boolean inS = px <= (double)(R + d) && py <= (double)(R + d) && (px <= (double)d || py <= (double)d || sq(px - (double)d) + sq(py - (double)d) <= rr);
               boolean inP = px <= (double)R && py <= (double)R && (px <= (double)0.0F || py <= (double)0.0F || sq(px) + sq(py) <= rr);
               return inS && !inP;
            }), new Piece(R, R + d, (px, py) -> {
               boolean inS = px >= (double)0.0F && py <= (double)(R + d) && (px >= (double)R || py <= (double)d || sq(px - (double)R) + sq(py - (double)d) <= rr);
               boolean inP = py <= (double)R && px >= (double)(-d) && (px >= (double)(R - d) || py <= (double)0.0F || sq(px - (double)(R - d)) + sq(py) <= rr);
               return inS && !inP;
            })};
            break;
         case 3:
         case 4:
            int D = (2 * r + 1) * t;
            double c = (double)D / (double)2.0F;
            double outer = ((double)r + (double)0.5F) * (double)t;
            double inner = ((double)r - (double)0.5F) * (double)t;
            boolean hoop = kind == SmoothCorners.Kind.HOOP;
            var10000 = new Piece[]{new Piece(D, D, (px, py) -> {
               double d2 = sq(px - c) + sq(py - c);
               return d2 <= outer * outer && (!hoop || d2 >= inner * inner);
            })};
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private static boolean inCorner(double px, double py, double cx, double cy, double rr) {
      return px >= cx || py >= cy || sq(cx - px) + sq(cy - py) <= rr;
   }

   private static double sq(double v) {
      return v * v;
   }

   private static Mask make(Piece[] pieces) {
      int width = 3;
      int height = 1;

      for(int i = 0; i < pieces.length; ++i) {
         width += pieces[i].w() + (i > 0 ? 2 : 0);
         height = Math.max(height, pieces[i].h());
      }

      int[] at = new int[pieces.length];
      int[] pw = new int[pieces.length];
      int[] ph = new int[pieces.length];
      NativeImage image = new NativeImage(width, height, true);
      int x = 0;

      for(int i = 0; i < pieces.length; ++i) {
         Piece p = pieces[i];
         if (i > 0) {
            x += 2;
         }

         at[i] = x;
         pw[i] = p.w();
         ph[i] = p.h();

         for(int py = 0; py < height; ++py) {
            int row = Math.min(py, p.h() - 1);

            for(int px = 0; px < p.w(); ++px) {
               image.setPixelRGBA(x + px, py, white(coverage(p.shape(), px, row)));
            }

            image.setPixelRGBA(x + p.w(), py, image.getPixelRGBA(x + p.w() - 1, py));
            if (i > 0) {
               image.setPixelRGBA(x - 1, py, image.getPixelRGBA(x, py));
            }
         }

         x += p.w();
      }

      for(int py = 0; py < height; ++py) {
         image.setPixelRGBA(width - 2, py, white(1.0F));
         image.setPixelRGBA(width - 1, py, white(1.0F));
      }

      int texture = TextureUtil.generateTextureId();
      TextureUtil.prepareImage(texture, width, height);
      RenderSystem.bindTexture(texture);
      image.upload(0, 0, 0, 0, 0, width, height, true, true, false, true);
      return new Mask(texture, width, height, at, pw, ph);
   }

   private static float coverage(Shape shape, int px, int py) {
      int in = 0;

      for(int sy = 0; sy < 16; ++sy) {
         double y = (double)py + ((double)sy + (double)0.5F) / (double)16.0F;

         for(int sx = 0; sx < 16; ++sx) {
            if (shape.inside((double)px + ((double)sx + (double)0.5F) / (double)16.0F, y)) {
               ++in;
            }
         }
      }

      return (float)in / 256.0F;
   }

   private static int white(float alpha) {
      return Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F) << 24 | 16777215;
   }

   public static void release() {
      for(Mask[] kind : MASKS) {
         for(int i = 0; i < kind.length; ++i) {
            if (kind[i] != null) {
               TextureUtil.releaseTextureId(kind[i].texture());
            }

            kind[i] = null;
         }
      }

   }

   private SmoothCorners() {
   }

   private static enum Kind {
      FILL,
      RING,
      SHADOW,
      DISC,
      HOOP;
   }

   private static record Piece(int w, int h, Shape shape) {
   }

   private static record Mask(int texture, int width, int height, int[] at, int[] w, int[] h) {
      float u(int piece, float px) {
         return ((float)this.at[piece] + px) / (float)this.width;
      }

      float v(float py) {
         return py / (float)this.height;
      }

      float solidU() {
         return ((float)this.width - 1.0F) / (float)this.width;
      }

      float solidV() {
         return 0.5F / (float)this.height;
      }
   }

   @FunctionalInterface
   private interface Shape {
      boolean inside(double var1, double var3);
   }
}
