package com.onehumanawa.cfhcore.content.gui.menu.client.infohud;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class HudCanvas {
   static final int FAINTEST = 26;
   private static final Matrix4f LOCAL = new Matrix4f();
   private static final Matrix4f SCALED = new Matrix4f();
   private static final Vector3f AT = new Vector3f();
   private static final float ATLAS_W = 256.0F;
   private static final float ATLAS_H = (float)(Icon.rows() * 16);
   private static MultiBufferSource.BufferSource source;
   private static VertexConsumer fills;
   private static VertexConsumer icons;
   private static RenderType iconType;
   private static boolean single;
   private static float whiteU;
   private static float whiteV;
   private static final RenderType[] OTHER = new RenderType[8];
   private static int others;
   private static int sweep = -1;
   private static boolean mixed;

   private HudCanvas() {
   }

   static void begin(GuiGraphics g, Font font, float x, float y, float unit) {
      LOCAL.set(g.pose().last().pose()).translate(x, y, 0.0F).scale(unit, unit, 1.0F);
      source = g.bufferSource();
      fills = null;
      icons = null;
      others = 0;
      sweep = -1;
      mixed = false;
      single = BakedText.white(font);
      if (single) {
         whiteU = BakedText.whiteU();
         whiteV = BakedText.whiteV();
      }

   }

   static void end() {
      source = null;
      fills = null;
      icons = null;
   }

   static void flat() {
      fills = source.getBuffer(single ? BakedText.whiteType() : RenderType.gui());
   }

   static void rect(int x, int y, int w, int h, int argb) {
      if (w > 0 && h > 0 && argb >>> 24 != 0) {
         if (single) {
            if (argb >>> 24 < 26) {
               return;
            }

            textured((float)x, (float)y, argb);
            textured((float)x, (float)(y + h), argb);
            textured((float)(x + w), (float)(y + h), argb);
            textured((float)(x + w), (float)y, argb);
         } else {
            plain((float)x, (float)y, argb);
            plain((float)x, (float)(y + h), argb);
            plain((float)(x + w), (float)(y + h), argb);
            plain((float)(x + w), (float)y, argb);
         }

      }
   }

   private static void plain(float x, float y, int argb) {
      LOCAL.transformPosition(x, y, 0.0F, AT);
      fills.addVertex(AT.x, AT.y, AT.z).setColor(argb);
   }

   private static void textured(float x, float y, int argb) {
      LOCAL.transformPosition(x, y, 0.0F, AT);
      fills.addVertex(AT.x, AT.y, AT.z).setColor(argb).setUv(whiteU, whiteV).setLight(15728880);
   }

   static void hline(int x, int y, int w, int argb) {
      rect(x, y, w, 1, argb);
   }

   static void pixelRound(int x, int y, int w, int h, int argb, int radius) {
      if (w > 0 && h > 0 && argb >>> 24 != 0) {
         int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
         if (r == 0) {
            rect(x, y, w, h, argb);
         } else {
            rect(x, y + r, w, h - 2 * r, argb);

            for(int i = 0; i < r; ++i) {
               int inset = r - i;
               rect(x + inset, y + i, w - 2 * inset, 1, argb);
               rect(x + inset, y + h - 1 - i, w - 2 * inset, 1, argb);
            }

         }
      }
   }

   static void outline(int x, int y, int w, int h, int argb, int radius) {
      if (w > 0 && h > 0 && argb >>> 24 != 0) {
         int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
         rect(x + r, y, w - 2 * r, 1, argb);
         rect(x + r, y + h - 1, w - 2 * r, 1, argb);
         rect(x, y + r, 1, h - 2 * r, argb);
         rect(x + w - 1, y + r, 1, h - 2 * r, argb);

         for(int i = 1; i <= r; ++i) {
            int px = r - i;
            int py = i - 1;
            rect(x + px, y + py + 1, 1, 1, argb);
            rect(x + w - 1 - px, y + py + 1, 1, 1, argb);
            rect(x + px, y + h - 2 - py, 1, 1, argb);
            rect(x + w - 1 - px, y + h - 2 - py, 1, 1, argb);
         }

      }
   }

   static void shadow(int x, int y, int w, int h, int argb, int radius) {
      if (w > 0 && h > 0 && argb >>> 24 != 0) {
         int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
         int runY = y + 2;
         int l0 = 0;
         int r0 = 0;
         int l1 = 0;
         int r1 = 0;

         for(int yy = y + 2; yy <= y + h + 2; ++yy) {
            int a0 = 0;
            int b0 = 0;
            int a1 = 0;
            int b1 = 0;
            if (yy < y + h + 2) {
               int so = inset(yy - y - 2, h, r);
               int ox0 = x + 2 + so;
               int ox1 = x + 2 + w - so;
               if (yy < y + h) {
                  int sp = inset(yy - y, h, r);
                  int px0 = x + sp;
                  int px1 = x + w - sp;
                  if (ox0 < px0) {
                     a0 = ox0;
                     b0 = Math.min(ox1, px0);
                  }

                  if (ox1 > px1) {
                     a1 = Math.max(ox0, px1);
                     b1 = ox1;
                  }
               } else {
                  a1 = ox0;
                  b1 = ox1;
               }
            }

            if (yy == y + h + 2 || a0 != l0 || b0 != r0 || a1 != l1 || b1 != r1) {
               if (yy > runY) {
                  if (r0 > l0) {
                     rect(l0, runY, r0 - l0, yy - runY, argb);
                  }

                  if (r1 > l1) {
                     rect(l1, runY, r1 - l1, yy - runY, argb);
                  }
               }

               runY = yy;
               l0 = a0;
               r0 = b0;
               l1 = a1;
               r1 = b1;
            }
         }

      }
   }

   private static int inset(int i, int h, int r) {
      if (i < r) {
         return r - i;
      } else {
         return i >= h - r ? r - (h - 1 - i) : 0;
      }
   }

   static void word(Font font, Seg seg, int x, int y, int argb, boolean shadow) {
      word(font, seg, x, y, 1, argb, shadow);
   }

   static void word(Font font, Seg seg, int x, int y, int scale, int argb, boolean shadow) {
      if (sweep >= 0) {
         seg.baked.emitRest(seg.s, source, LOCAL, (float)x, (float)y, (float)scale, argb, shadow, mixed ? null : OTHER[sweep]);
      } else if (!single || !seg.baked.emit(font, seg.s, source, LOCAL, (float)x, (float)y, (float)scale, argb, shadow)) {
         Matrix4f at = scale == 1 ? LOCAL : SCALED.set(LOCAL).translate((float)x, (float)y, 0.0F).scale((float)scale, (float)scale, 1.0F);
         float tx = scale == 1 ? (float)x : 0.0F;
         float ty = scale == 1 ? (float)y : 0.0F;
         font.drawInBatch(seg.s, tx, ty, argb, shadow, at, source, DisplayMode.NORMAL, 0, 15728880);
      }
   }

   static void later(RenderType texture) {
      if (!mixed) {
         for(int i = 0; i < others; ++i) {
            if (OTHER[i] == texture) {
               return;
            }
         }

         if (others < OTHER.length) {
            OTHER[others++] = texture;
         } else {
            mixed = true;
         }

      }
   }

   static boolean nextTexture() {
      ++sweep;
      if (mixed) {
         if (sweep == 0) {
            return true;
         }
      } else if (sweep < others) {
         return true;
      }

      sweep = -1;
      return false;
   }

   static void reloaded() {
      iconType = null;
   }

   static void icons() {
      if (iconType == null) {
         RenderType text = RenderType.text(Icons.ATLAS);
         RenderType twin = BakedText.unsorted(text);
         iconType = twin != null ? twin : text;
      }

      icons = source.getBuffer(iconType);
   }

   static void icon(Icon icon, float x, float y, float size, int argb) {
      if (icons != null && argb >>> 24 >= 26) {
         float u0 = (float)icon.u() / 256.0F;
         float v0 = (float)icon.v() / ATLAS_H;
         float u1 = (float)(icon.u() + 16) / 256.0F;
         float v1 = (float)(icon.v() + 16) / ATLAS_H;
         glyph(x, y, u0, v0, argb);
         glyph(x, y + size, u0, v1, argb);
         glyph(x + size, y + size, u1, v1, argb);
         glyph(x + size, y, u1, v0, argb);
      }
   }

   private static void glyph(float x, float y, float u, float v, int argb) {
      LOCAL.transformPosition(x, y, 0.0F, AT);
      icons.addVertex(AT.x, AT.y, AT.z).setColor(argb).setUv(u, v).setLight(15728880);
   }
}
