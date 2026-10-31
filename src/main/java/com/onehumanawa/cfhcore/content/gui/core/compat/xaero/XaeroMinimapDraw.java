package com.onehumanawa.cfhcore.content.gui.core.compat.xaero;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.client.HudKeepOut;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class XaeroMinimapDraw {
   private static final int MAX_LINES = 48;
   private static final int PLATE_PAD_X = 5;
   private static final int PLATE_PAD_TOP = 3;
   private static final int PLATE_PAD_BOTTOM = 2;
   private static final List<Component> LINE_TEXT = new ArrayList();
   private static final float[] LINE_X = new float[48];
   private static final float[] LINE_Y = new float[48];
   private static final int[] LINE_COLOR = new int[48];
   private static final int[] LINE_WIDTH = new int[48];
   private static boolean recording;
   private static PoseStack linePose;
   private static MultiBufferSource.BufferSource lineBuffers;
   private static boolean framed;
   private static boolean mapSeen;
   private static boolean frameOff;
   private static int compassCall = -1;
   private static final float[] CHIPS = new float[16];
   private static int chips;
   private static float mapMiddle;
   private static final float CHIP_GAP = 2.0F;
   private static final Vector3f CORNER = new Vector3f();
   private static final String KEEP_OUT = "xaero_minimap";
   private static ShaderInstance shaderWas;
   private static final Supplier<ShaderInstance> SHADER_WAS = () -> shaderWas;

   private XaeroMinimapDraw() {
   }

   private static float pixel(Matrix4f pose) {
      float sx = pose.m00();
      return sx > 1.0E-4F && Float.isFinite(sx) ? 1.0F / sx : 1.0F;
   }

   private static int steps(float w, float h, float p) {
      int most = (int)Math.floor((double)(Math.min(w, h) / (2.0F * p))) - 1;
      return Math.max(0, Math.min(XaeroStyle.radius(), most));
   }

   private static void quad(VertexConsumer c, Matrix4f m, float x0, float y0, float x1, float y1, int argb) {
      if (!(x1 <= x0) && !(y1 <= y0)) {
         c.addVertex(m, x0, y0, 0.0F).setColor(argb);
         c.addVertex(m, x0, y1, 0.0F).setColor(argb);
         c.addVertex(m, x1, y1, 0.0F).setColor(argb);
         c.addVertex(m, x1, y0, 0.0F).setColor(argb);
      }
   }

   private static void fillRound(VertexConsumer c, Matrix4f m, float x, float y, float w, float h, int r, float p, int argb) {
      quad(c, m, x, y + (float)r * p, x + w, y + h - (float)r * p, argb);

      for(int i = 0; i < r; ++i) {
         float inset = (float)(r - i) * p;
         quad(c, m, x + inset, y + (float)i * p, x + w - inset, y + (float)(i + 1) * p, argb);
         quad(c, m, x + inset, y + h - (float)(i + 1) * p, x + w - inset, y + h - (float)i * p, argb);
      }

   }

   private static void outlineRound(VertexConsumer c, Matrix4f m, float x, float y, float w, float h, int r, float p, int argb) {
      quad(c, m, x + (float)r * p, y, x + w - (float)r * p, y + p, argb);
      quad(c, m, x + (float)r * p, y + h - p, x + w - (float)r * p, y + h, argb);
      quad(c, m, x, y + (float)(r + 1) * p, x + p, y + h - (float)(r + 1) * p, argb);
      quad(c, m, x + w - p, y + (float)(r + 1) * p, x + w, y + h - (float)(r + 1) * p, argb);

      for(int i = 1; i <= r; ++i) {
         float dx = (float)(r - i) * p;
         float dy = (float)i * p;
         quad(c, m, x + dx, y + dy, x + dx + p, y + dy + p, argb);
         quad(c, m, x + w - dx - p, y + dy, x + w - dx, y + dy + p, argb);
         quad(c, m, x + dx, y + h - dy - p, x + dx + p, y + h - dy, argb);
         quad(c, m, x + w - dx - p, y + h - dy - p, x + w - dx, y + h - dy, argb);
      }

   }

   private static void shadowRound(VertexConsumer c, Matrix4f m, float x, float y, float w, float h, int r, float p, int argb) {
      float d = 2.0F * p;
      quad(c, m, x + w, y + d + (float)r * p, x + w + d, y + h + d - (float)r * p, argb);
      if (r >= 1) {
         quad(c, m, x + w, y + d + (float)(r - 1) * p, x + w + p, y + d + (float)r * p, argb);
         quad(c, m, x + w, y + h + d - (float)r * p, x + w + p, y + h + d - (float)(r - 1) * p, argb);
      }

      for(int j = 0; j < 2; ++j) {
         float inset = (float)Math.max(0, r - j) * p;
         quad(c, m, x + d + inset, y + h + (float)(1 - j) * p, Math.min(x + w, x + w + d - inset), y + h + (float)(2 - j) * p, argb);
      }

      for(int j = 0; j < r; ++j) {
         quad(c, m, x + w - (float)(r - j) * p, y + h - (float)(j + 1) * p, x + w, y + h - (float)j * p, argb);
      }

   }

   private static BufferBuilder begin() {
      return Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
   }

   private static void end(BufferBuilder builder) {
      MeshData mesh = builder.build();
      if (mesh != null) {
         shaderWas = RenderSystem.getShader();
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         BufferUploader.drawWithShader(mesh);
         if (shaderWas != null) {
            RenderSystem.setShader(SHADER_WAS);
         }

         shaderWas = null;
      }
   }

   private static int border(Palette p) {
      return p.borderStrong();
   }

   private static int shadow(Palette p) {
      return Colors.scaleAlpha(p.shadow(), 0.6F);
   }

   public static boolean frames() {
      mapSeen = true;
      if (framed) {
         framed = false;
         frameOff = true;
         XaeroStyle.frameUnknown();
      }

      return !frameOff;
   }

   public static boolean takeFramed() {
      boolean was = framed;
      if (!was && !mapSeen && !frameOff) {
         frameOff = true;
         XaeroStyle.frameUnknown();
      }

      framed = false;
      mapSeen = false;
      return was;
   }

   public static void squareMap(PoseStack pose, float x, float y, int u, int v, float w, float h, float texH, float texSize) {
      Matrix4f m = pose.last().pose();
      float p = pixel(m);
      int r = Math.max(0, steps(w + 2.0F * p, h + 2.0F * p, p) - 1);
      BufferBuilder b = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      strip(b, m, x, y, u, v, w, h, texH, texSize, 0.0F, (float)r * p, h - (float)r * p);

      for(int i = 0; i < r; ++i) {
         float inset = (float)(r - i) * p;
         strip(b, m, x, y, u, v, w, h, texH, texSize, inset, (float)i * p, (float)(i + 1) * p);
         strip(b, m, x, y, u, v, w, h, texH, texSize, inset, h - (float)(i + 1) * p, h - (float)i * p);
      }

      MeshData mesh = b.build();
      if (mesh != null) {
         BufferUploader.drawWithShader(mesh);
      }

   }

   private static void strip(BufferBuilder b, Matrix4f m, float x, float y, int u, int v, float w, float h, float texH, float texSize, float inset, float y0, float y1) {
      if (!(y1 <= y0) && !(w <= 2.0F * inset)) {
         float f = 1.0F / texSize;
         float u0 = ((float)u + inset) * f;
         float u1 = ((float)u + w - inset) * f;
         float v0 = ((float)v + texH * (1.0F - y0 / h)) * f;
         float v1 = ((float)v + texH * (1.0F - y1 / h)) * f;
         b.addVertex(m, x + inset, y + y1, 0.0F).setUv(u0, v1);
         b.addVertex(m, x + w - inset, y + y1, 0.0F).setUv(u1, v1);
         b.addVertex(m, x + w - inset, y + y0, 0.0F).setUv(u1, v0);
         b.addVertex(m, x + inset, y + y0, 0.0F).setUv(u0, v0);
      }
   }

   public static void squareFrame(PoseStack pose, float x, float y, float w, float h) {
      Matrix4f m = pose.last().pose();
      float p = pixel(m);
      Palette palette = XaeroStyle.palette();
      int r = steps(w + 2.0F * p, h + 2.0F * p, p);
      BufferBuilder b = begin();
      shadowRound(b, m, x - p, y - p, w + 2.0F * p, h + 2.0F * p, r, p, shadow(palette));
      outlineRound(b, m, x - p, y - p, w + 2.0F * p, h + 2.0F * p, r, p, border(palette));
      end(b);
      keepOut(m, x - p, y - p, x + w + 3.0F * p, y + h + 3.0F * p, true);
      framed(m, x + w / 2.0F, y + h / 2.0F);
   }

   private static void keepOut(Matrix4f m, float x0, float y0, float x1, float y1, boolean first) {
      m.transformPosition(x0, y0, 0.0F, CORNER);
      float ax = CORNER.x;
      float ay = CORNER.y;
      m.transformPosition(x1, y1, 0.0F, CORNER);
      if (first) {
         HudKeepOut.begin("xaero_minimap", ax, ay, CORNER.x, CORNER.y);
      } else {
         HudKeepOut.add("xaero_minimap", ax, ay, CORNER.x, CORNER.y);
      }

   }

   private static void framed(Matrix4f m, float x, float y) {
      m.transformPosition(x, y, 0.0F, CORNER);
      mapMiddle = CORNER.y;
      chips = 0;
      framed = true;
   }

   public static void roundFrame(PoseStack pose, double startAngle, int sides, float x, float y, float width) {
      if (sides >= 3) {
         Matrix4f m = pose.last().pose();
         float p = pixel(m);
         Palette palette = XaeroStyle.palette();
         float radius = width / 2.0F;
         float cx = x + radius;
         float cy = y + radius;
         float outer = radius + p;
         float d = 2.0F * p;
         int ring = border(palette);
         int shade = shadow(palette);
         BufferBuilder b = begin();
         float lastSin = 0.0F;
         float lastCos = 0.0F;

         for(int i = 0; i <= sides; ++i) {
            double angle = startAngle + (double)i / (double)sides * (Math.PI * 2D);
            float sin = (float)Math.sin(angle);
            float cos = (float)(-Math.cos(angle));
            if (i > 0) {
               b.addVertex(m, cx + radius * lastSin, cy + radius * lastCos, 0.0F).setColor(ring);
               b.addVertex(m, cx + radius * sin, cy + radius * cos, 0.0F).setColor(ring);
               b.addVertex(m, cx + outer * sin, cy + outer * cos, 0.0F).setColor(ring);
               b.addVertex(m, cx + outer * lastSin, cy + outer * lastCos, 0.0F).setColor(ring);
               if (lastSin + lastCos + sin + cos > 0.0F) {
                  b.addVertex(m, cx + outer * lastSin, cy + outer * lastCos, 0.0F).setColor(shade);
                  b.addVertex(m, cx + outer * sin, cy + outer * cos, 0.0F).setColor(shade);
                  b.addVertex(m, cx + outer * sin + d, cy + outer * cos + d, 0.0F).setColor(shade);
                  b.addVertex(m, cx + outer * lastSin + d, cy + outer * lastCos + d, 0.0F).setColor(shade);
               }
            }

            lastSin = sin;
            lastCos = cos;
         }

         end(b);
         keepOut(m, cx - outer, cy - outer, cx + outer + d, cy + outer + d, true);
         framed(m, cx, cy);
      }
   }

   public static void compassBegin() {
      compassCall = 0;
   }

   public static void compassEnd() {
      compassCall = -1;
   }

   public static boolean compassOpen() {
      return compassCall >= 0;
   }

   public static int compassPass(PoseStack pose, MultiBufferSource.BufferSource buffers, float x) {
      int call = compassCall++;
      Palette palette = XaeroStyle.palette();
      if ((call & 1) == 1) {
         return call >> 1 == 0 ? palette.accent() : palette.text();
      } else {
         int extra = Math.max(0, Math.round(1.0F - x) - 3);
         Matrix4f m = pose.last().pose();
         VertexConsumer c = buffers.getBuffer(RenderType.gui());
         float left = (float)(-5 - extra);
         float size = (float)(11 + 2 * extra);
         fillRound(c, m, left, left, size, size, 2, 1.0F, palette.surface());
         outlineRound(c, m, left, left, size, size, 2, 1.0F, call >> 1 == 0 ? Colors.mix(palette.borderStrong(), palette.accent(), 0.55F) : palette.borderStrong());
         int chip = (call >> 1) * 4;
         if (chip + 3 < CHIPS.length) {
            m.transformPosition(left, left, 0.0F, CORNER);
            CHIPS[chip] = CORNER.x;
            CHIPS[chip + 1] = CORNER.y;
            m.transformPosition(left + size, left + size, 0.0F, CORNER);
            CHIPS[chip + 2] = CORNER.x;
            CHIPS[chip + 3] = CORNER.y;
            chips = Math.max(chips, (call >> 1) + 1);
            HudKeepOut.add("xaero_minimap", CHIPS[chip], CHIPS[chip + 1], CHIPS[chip + 2], CHIPS[chip + 3]);
         }

         return 0;
      }
   }

   private static float clearOfChips(Matrix4f m, float x, float y, float w, float h, boolean under) {
      m.transformPosition(x, y, 0.0F, CORNER);
      float x0 = CORNER.x;
      float y0 = CORNER.y;
      m.transformPosition(x + w, y + h, 0.0F, CORNER);
      float x1 = CORNER.x;
      float y1 = CORNER.y;
      float move = 0.0F;

      for(int i = 0; i < chips; ++i) {
         float cx0 = CHIPS[i * 4];
         float cy0 = CHIPS[i * 4 + 1];
         float cx1 = CHIPS[i * 4 + 2];
         float cy1 = CHIPS[i * 4 + 3];
         if (!(cx1 <= x0) && !(cx0 >= x1) && !(cy1 + 2.0F <= y0) && !(cy0 - 2.0F >= y1)) {
            move = under ? Math.max(move, cy1 + 2.0F - y0) : Math.min(move, cy0 - 2.0F - y1);
         }
      }

      return move;
   }

   public static void infoBegin() {
      LINE_TEXT.clear();
      recording = true;
   }

   public static boolean infoOpen() {
      return recording;
   }

   public static boolean infoLine(PoseStack pose, MultiBufferSource.BufferSource buffers, Component text, float x, float y, int color) {
      int i = LINE_TEXT.size();
      if (recording && i < 48) {
         linePose = pose;
         lineBuffers = buffers;
         LINE_TEXT.add(text);
         LINE_X[i] = x;
         LINE_Y[i] = y;
         LINE_COLOR[i] = color;
         return true;
      } else {
         return false;
      }
   }

   public static void infoEnd() {
      recording = false;
      int n = LINE_TEXT.size();
      PoseStack pose = linePose;
      MultiBufferSource.BufferSource buffers = lineBuffers;
      linePose = null;
      lineBuffers = null;
      if (n != 0 && pose != null && buffers != null) {
         Font font = Minecraft.getInstance().font;
         Palette palette = XaeroStyle.palette();
         float left = Float.MAX_VALUE;
         float right = -Float.MAX_VALUE;
         float top = Float.MAX_VALUE;
         float bottom = -Float.MAX_VALUE;

         for(int i = 0; i < n; ++i) {
            LINE_WIDTH[i] = font.width((FormattedText)LINE_TEXT.get(i));
            left = Math.min(left, LINE_X[i]);
            right = Math.max(right, LINE_X[i] + (float)LINE_WIDTH[i]);
            top = Math.min(top, LINE_Y[i]);
            bottom = Math.max(bottom, LINE_Y[i] + 8.0F);
         }

         float x = left - 5.0F;
         float y = top - 3.0F;
         float w = right - left + 10.0F - 1.0F;
         float h = bottom - top + 3.0F + 2.0F;
         pose.pushPose();

         try {
            pose.last().pose().transformPosition(x, y, 0.0F, CORNER);
            float move = clearOfChips(pose.last().pose(), x, y, w, h, CORNER.y > mapMiddle);
            pose.translate(0.0F, move == 0.0F ? 0.0F : move * pixel(pose.last().pose()), 0.5F);
            Matrix4f m = pose.last().pose();
            float p = pixel(m);
            int r = Math.min(2, steps(w, h, p));
            keepOut(m, x, y, x + w + 2.0F * p, y + h + 2.0F * p, false);
            BufferBuilder b = begin();
            shadowRound(b, m, x, y, w, h, r, p, Colors.scaleAlpha(palette.shadow(), 0.5F));
            int fill = XaeroStyle.panel(palette.surface());
            if (Colors.alpha(fill) < 255) {
               fillRound(b, m, x, y, w, h, r, p, Colors.withAlpha(palette.bg(), 238));
            }

            fillRound(b, m, x, y, w, h, r, p, fill);
            outlineRound(b, m, x, y, w, h, r, p, border(palette));
            RenderSystem.disableDepthTest();
            end(b);
            RenderSystem.enableDepthTest();

            for(int i = 0; i < n; ++i) {
               int rgb = LINE_COLOR[i] & 16777215;
               int color = rgb != 16777215 ? -16777216 | rgb : (i == 0 ? palette.text() : palette.textMuted());
               font.drawInBatch((Component)LINE_TEXT.get(i), LINE_X[i], LINE_Y[i], color, false, m, buffers, DisplayMode.NORMAL, 0, 15728880);
            }

            buffers.endBatch();
         } finally {
            pose.popPose();
            LINE_TEXT.clear();
         }

      }
   }

   public static int labelPlate() {
      return Colors.withAlpha(XaeroStyle.palette().bg(), 210);
   }

   public static int labelText() {
      return XaeroStyle.palette().text();
   }
}
