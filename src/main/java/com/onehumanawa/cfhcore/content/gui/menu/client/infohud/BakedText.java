package com.onehumanawa.cfhcore.content.gui.menu.client.infohud;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class BakedText {
   static final int FULL_BRIGHT = 15728880;
   private static final Matrix4f ORIGIN = new Matrix4f();
   private static final Vector3f AT = new Vector3f();
   private static final Recorder RECORDER = new Recorder();
   private static final Map<RenderType, RenderType> UNSORTED = new IdentityHashMap();
   private static int epoch = 1;
   private static boolean broken;
   private static final BakedText WHITE = new BakedText();
   private static float whiteU;
   private static float whiteV;
   private static RenderType whiteType;
   private float[] xyz = new float[0];
   private float[] uv = new float[0];
   private int[] tint = new int[0];
   private int[] light = new int[0];
   private RenderType[] runType = new RenderType[1];
   private int[] runEnd = new int[1];
   private int count;
   private int runs;
   private String bakedOf = "";
   private boolean bakedShadow;
   private int bakedEpoch;

   static void invalidate() {
      ++epoch;
   }

   static void reloaded() {
      ++epoch;
      UNSORTED.clear();
      HudCanvas.reloaded();
   }

   static boolean working() {
      return !broken;
   }

   static boolean white(Font font) {
      if (broken) {
         return false;
      } else if (WHITE.bakedEpoch == epoch) {
         return true;
      } else {
         WHITE.count = 0;
         WHITE.runs = 0;

         label65: {
            boolean var2;
            try {
               RECORDER.into = WHITE;
               font.drawInBatch(" ", 0.0F, 0.0F, -1, false, ORIGIN, RECORDER, DisplayMode.NORMAL, -16777216, 15728880);
               break label65;
            } catch (Throwable t) {
               failed("recording the font's white rectangle threw " + String.valueOf(t));
               var2 = false;
            } finally {
               RECORDER.into = null;
            }

            return var2;
         }

         if (WHITE.count == 4 && WHITE.runs == 1 && WHITE.settle()) {
            whiteU = (WHITE.uv[0] + WHITE.uv[2] + WHITE.uv[4] + WHITE.uv[6]) / 4.0F;
            whiteV = (WHITE.uv[1] + WHITE.uv[3] + WHITE.uv[5] + WHITE.uv[7]) / 4.0F;
            whiteType = WHITE.runType[0];
            WHITE.bakedEpoch = epoch;
            return true;
         } else {
            failed("the font drew no white rectangle behind a space");
            return false;
         }
      }
   }

   static RenderType whiteType() {
      return whiteType;
   }

   static float whiteU() {
      return whiteU;
   }

   static float whiteV() {
      return whiteV;
   }

   boolean emit(Font font, String text, MultiBufferSource source, Matrix4f pose, float x, float y, float scale, int argb, boolean shadow) {
      if (broken) {
         return false;
      } else if ((this.bakedEpoch != epoch || this.bakedShadow != shadow || !this.bakedOf.equals(text)) && !this.bake(font, text, shadow)) {
         return false;
      } else {
         for(int run = 0; run < this.runs; ++run) {
            if (this.runType[run] != whiteType && this.runEnd[run] > (run == 0 ? 0 : this.runEnd[run - 1])) {
               HudCanvas.later(this.runType[run]);
            }
         }

         this.write(source, pose, x, y, scale, argb, whiteType, false);
         return true;
      }
   }

   void emitRest(String text, MultiBufferSource source, Matrix4f pose, float x, float y, float scale, int argb, boolean shadow, @Nullable RenderType texture) {
      if (!broken && this.bakedEpoch == epoch && this.bakedShadow == shadow && this.bakedOf.equals(text)) {
         this.write(source, pose, x, y, scale, argb, texture, texture == null);
      }
   }

   private void write(MultiBufferSource source, Matrix4f pose, float x, float y, float scale, int argb, @Nullable RenderType only, boolean others) {
      int a = argb >>> 24;
      int r = argb >> 16 & 255;
      int g = argb >> 8 & 255;
      int b = argb & 255;
      int v = 0;

      for(int run = 0; run < this.runs; ++run) {
         RenderType type = this.runType[run];
         int end = this.runEnd[run];
         if (v != end) {
            label38: {
               if (others) {
                  if (type == whiteType) {
                     break label38;
                  }
               } else if (type != only) {
                  break label38;
               }

               for(VertexConsumer out = source.getBuffer(type); v < end; ++v) {
                  pose.transformPosition(this.xyz[v * 3] * scale + x, this.xyz[v * 3 + 1] * scale + y, this.xyz[v * 3 + 2], AT);
                  int t = this.tint[v];
                  out.addVertex(AT.x, AT.y, AT.z).setColor((t >> 16 & 255) * r / 255, (t >> 8 & 255) * g / 255, (t & 255) * b / 255, (t >>> 24) * a / 255).setUv(this.uv[v * 2], this.uv[v * 2 + 1]).setLight(this.light[v]);
               }
               continue;
            }
         }

         v = end;
      }

   }

   private boolean bake(Font font, String text, boolean shadow) {
      this.count = 0;
      this.runs = 0;

      label80: {
         boolean var5;
         try {
            RECORDER.into = this;
            font.drawInBatch(text, 0.0F, 0.0F, -1, shadow, ORIGIN, RECORDER, DisplayMode.NORMAL, 0, 15728880);
            break label80;
         } catch (Throwable t) {
            failed("recording them threw " + String.valueOf(t));
            var5 = false;
         } finally {
            RECORDER.into = null;
         }

         return var5;
      }

      if (this.count % 4 != 0 || this.count == 0 && !text.isBlank()) {
         failed(this.count == 0 ? "the font drew nothing into the recorder" : "the font drew something that is not glyphs");
         return false;
      } else if (!this.settle()) {
         failed("the font draws with a render type that is not the game's text");
         return false;
      } else {
         this.bakedOf = text;
         this.bakedShadow = shadow;
         this.bakedEpoch = epoch;
         return true;
      }
   }

   private boolean settle() {
      for(int run = 0; run < this.runs; ++run) {
         RenderType twin = unsorted(this.runType[run]);
         if (twin == null) {
            return false;
         }

         this.runType[run] = twin;
      }

      return true;
   }

   static RenderType unsorted(RenderType text) {
      RenderType twin = (RenderType)UNSORTED.get(text);
      if (twin == null) {
         if (text.format() != DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP || text.mode() != Mode.QUADS) {
            return null;
         }

         VertexFormat var10003 = text.format();
         VertexFormat.Mode var10004 = text.mode();
         Objects.requireNonNull(text);
         Runnable var10008 = text::setupRenderState;
         Objects.requireNonNull(text);
         twin = new RenderType("cfhgui_hud_text", var10003, var10004, 1536, false, false, var10008, text::clearRenderState) {
         };
         UNSORTED.put(text, twin);
      }

      return twin;
   }

   private static void failed(String why) {
      if (!broken) {
         broken = true;
         CFHGuiMenu.LOGGER.info("[CFHGui Menu] info overlay: it draws as the rest of the HUD does after all ({}); another mod draws text its own way", why);
      }
   }

   private void run(RenderType type) {
      if (this.runs <= 0 || this.runType[this.runs - 1] != type) {
         if (this.runs == this.runType.length) {
            this.runType = (RenderType[])Arrays.copyOf(this.runType, this.runs * 2);
            this.runEnd = Arrays.copyOf(this.runEnd, this.runs * 2);
         }

         this.runType[this.runs] = type;
         this.runEnd[this.runs] = this.count;
         ++this.runs;
      }
   }

   private int corner(float x, float y, float z) {
      if (this.runs == 0) {
         return -1;
      } else {
         if (this.count * 3 == this.xyz.length) {
            int room = Math.max(32, this.count * 2);
            this.xyz = Arrays.copyOf(this.xyz, room * 3);
            this.uv = Arrays.copyOf(this.uv, room * 2);
            this.tint = Arrays.copyOf(this.tint, room);
            this.light = Arrays.copyOf(this.light, room);
         }

         int at = this.count++;
         this.xyz[at * 3] = x;
         this.xyz[at * 3 + 1] = y;
         this.xyz[at * 3 + 2] = z;
         this.tint[at] = -1;
         this.light[at] = 15728880;
         this.runEnd[this.runs - 1] = this.count;
         return at;
      }
   }

   private static final class Recorder implements MultiBufferSource, VertexConsumer {
      BakedText into;
      private int at = -1;

      public VertexConsumer getBuffer(RenderType type) {
         if (this.into != null) {
            this.into.run(type);
         }

         return this;
      }

      public VertexConsumer addVertex(float x, float y, float z) {
         this.at = this.into == null ? -1 : this.into.corner(x, y, z);
         return this;
      }

      public VertexConsumer setColor(int red, int green, int blue, int alpha) {
         if (this.at >= 0) {
            this.into.tint[this.at] = (alpha & 255) << 24 | (red & 255) << 16 | (green & 255) << 8 | blue & 255;
         }

         return this;
      }

      public VertexConsumer setUv(float u, float v) {
         if (this.at >= 0) {
            this.into.uv[this.at * 2] = u;
            this.into.uv[this.at * 2 + 1] = v;
         }

         return this;
      }

      public VertexConsumer setUv1(int u, int v) {
         return this;
      }

      public VertexConsumer setUv2(int u, int v) {
         if (this.at >= 0) {
            this.into.light[this.at] = u & '\uffff' | (v & '\uffff') << 16;
         }

         return this;
      }

      public VertexConsumer setNormal(float x, float y, float z) {
         return this;
      }
   }
}
