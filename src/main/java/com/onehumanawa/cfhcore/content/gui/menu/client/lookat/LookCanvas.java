package com.onehumanawa.cfhcore.content.gui.menu.client.lookat;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class LookCanvas {
   private static final int FULL_BRIGHT = 15728880;
   private static final float ATLAS_W = 256.0F;
   private static final float ATLAS_H = (float)(Icon.rows() * 16);
   private static final Map<RenderType, RenderType> UNSORTED = new IdentityHashMap();
   private static final Words WORDS = new Words();
   private static final Vector3f AT = new Vector3f();
   private static MultiBufferSource.@Nullable BufferSource source;
   private static @Nullable Matrix4f pose;
   private static @Nullable VertexConsumer quads;

   private LookCanvas() {
   }

   static void begin(GuiGraphics g) {
      source = g.bufferSource();
      pose = g.pose().last().pose();
      WORDS.into = source;
      quads = null;
   }

   static void end() {
      source = null;
      pose = null;
      WORDS.into = null;
      quads = null;
   }

   static void reloaded() {
      UNSORTED.clear();
   }

   static void word(Font font, FormattedCharSequence words, int x, int y, int argb, boolean shadow) {
      font.drawInBatch(words, (float)x, (float)y, argb, shadow, pose, WORDS, DisplayMode.NORMAL, 0, 15728880);
   }

   private static RenderType unsorted(RenderType text) {
      RenderType twin = (RenderType)UNSORTED.get(text);
      if (twin == null) {
         if (text.format() != DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP || text.mode() != Mode.QUADS) {
            return text;
         }

         VertexFormat var10003 = text.format();
         VertexFormat.Mode var10004 = text.mode();
         Objects.requireNonNull(text);
         Runnable var10008 = text::setupRenderState;
         Objects.requireNonNull(text);
         twin = new RenderType("cfhgui_lookat_text", var10003, var10004, 1536, false, false, var10008, text::clearRenderState) {
         };
         UNSORTED.put(text, twin);
      }

      return twin;
   }

   static void texture(ResourceLocation texture) {
      quads = source.getBuffer(unsorted(RenderType.text(texture)));
   }

   static void icon(Icon icon, int x, int y, int size, int argb) {
      quad(x, y, size, (float)icon.u() / 256.0F, (float)icon.v() / ATLAS_H, (float)(icon.u() + 16) / 256.0F, (float)(icon.v() + 16) / ATLAS_H, argb);
   }

   static void sprite(TextureAtlasSprite sprite, int x, int y, int size, int argb) {
      quad(x, y, size, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), argb);
   }

   private static void quad(int x, int y, int size, float u0, float v0, float u1, float v1, int argb) {
      if (quads != null && argb >>> 24 >= 26) {
         corner((float)x, (float)y, u0, v0, argb);
         corner((float)x, (float)(y + size), u0, v1, argb);
         corner((float)(x + size), (float)(y + size), u1, v1, argb);
         corner((float)(x + size), (float)y, u1, v0, argb);
      }
   }

   private static void corner(float x, float y, float u, float v, int argb) {
      pose.transformPosition(x, y, 0.0F, AT);
      quads.addVertex(AT.x, AT.y, AT.z).setColor(argb).setUv(u, v).setLight(15728880);
   }

   private static final class Words implements MultiBufferSource {
      MultiBufferSource.@Nullable BufferSource into;

      public VertexConsumer getBuffer(RenderType type) {
         return this.into.getBuffer(LookCanvas.unsorted(type));
      }
   }
}
