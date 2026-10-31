package com.onehumanawa.cfhcore.content.gui.menu.client.loading.scene;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class PixelFont {
   public static final int LATIN_1 = 224;
   public static final int ASCII = 95;
   private static final int FIRST = 32;
   private final STBTTBakedChar.Buffer chars;
   private final int count;
   private final int atlas;
   private final int texture;
   private final float scale;
   private final float capPx;

   private PixelFont(STBTTBakedChar.Buffer chars, int count, int atlas, int texture, float scale, float capPx) {
      this.chars = chars;
      this.count = count;
      this.atlas = atlas;
      this.texture = texture;
      this.scale = scale;
      this.capPx = capPx;
   }

   public static PixelFont bake(ByteBuffer ttf, float size, float scale, int count, boolean crisp) {
      float em = size * scale;
      MemoryStack stack = MemoryStack.stackPush();

      float pixelHeight;
      float capPx;
      try {
         STBTTFontinfo info = STBTTFontinfo.malloc(stack);
         if (!STBTruetype.stbtt_InitFont(info, ttf)) {
            throw new IllegalStateException("not a font");
         }

         IntBuffer asc = stack.mallocInt(1);
         IntBuffer desc = stack.mallocInt(1);
         STBTruetype.stbtt_GetFontVMetrics(info, asc, desc, (IntBuffer)null);
         float perUnit = STBTruetype.stbtt_ScaleForMappingEmToPixels(info, em);
         pixelHeight = (float)(asc.get(0) - desc.get(0)) * perUnit;
         IntBuffer x0 = stack.mallocInt(1);
         IntBuffer y0 = stack.mallocInt(1);
         IntBuffer x1 = stack.mallocInt(1);
         IntBuffer y1 = stack.mallocInt(1);
         STBTruetype.stbtt_GetCodepointBox(info, 72, x0, y0, x1, y1);
         capPx = (float)Math.round((float)y1.get(0) * perUnit);
      } catch (Throwable var23) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var21) {
               var23.addSuppressed(var21);
            }
         }

         throw var23;
      }

      if (stack != null) {
         stack.close();
      }

      STBTTBakedChar.Buffer chars = STBTTBakedChar.malloc(count);
      int side = 256;
      ByteBuffer bitmap = MemoryUtil.memAlloc(side * side);

      PixelFont var29;
      try {
         while(STBTruetype.stbtt_BakeFontBitmap(ttf, pixelHeight, bitmap, side, side, 32, chars) <= 0 && side < 4096) {
            MemoryUtil.memFree(bitmap);
            side *= 2;
            bitmap = MemoryUtil.memAlloc(side * side);
         }

         int filter = crisp ? 9728 : 9729;
         int tex = GL32C.glGenTextures();
         GL32C.glBindTexture(3553, tex);
         GlSave.plainUnpack();
         GL32C.glTexImage2D(3553, 0, 33321, side, side, 0, 6403, 5121, bitmap);
         GL32C.glTexParameteri(3553, 10241, filter);
         GL32C.glTexParameteri(3553, 10240, filter);
         GL32C.glTexParameteri(3553, 10242, 33071);
         GL32C.glTexParameteri(3553, 10243, 33071);
         var29 = new PixelFont(chars, count, side, tex, scale, capPx);
      } finally {
         MemoryUtil.memFree(bitmap);
      }

      return var29;
   }

   public void delete() {
      GL32C.glDeleteTextures(this.texture);
      this.chars.free();
   }

   public float capHeight() {
      return this.capPx / this.scale;
   }

   public float width(String s) {
      float w = 0.0F;

      for(int i = 0; i < s.length(); ++i) {
         int c = s.charAt(i) - 32;
         if (c >= 0 && c < this.count) {
            w += ((STBTTBakedChar)this.chars.get(c)).xadvance();
         }
      }

      return w / this.scale;
   }

   public String fit(String s, float maxWidth) {
      if (this.width(s) <= maxWidth) {
         return s;
      } else {
         float dots = this.width("...");

         int end;
         for(end = s.length(); end > 0 && this.width(s.substring(0, end)) + dots > maxWidth; --end) {
         }

         String var10000 = s.substring(0, end);
         return var10000.stripTrailing() + "...";
      }
   }

   public void draw(Gfx g, String s, float x, float y, int argb) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         STBTTAlignedQuad q = STBTTAlignedQuad.malloc(stack);
         FloatBuffer px = stack.floats((float)Math.round(x * this.scale));
         FloatBuffer py = stack.floats((float)Math.round(y * this.scale) + this.capPx);

         for(int i = 0; i < s.length(); ++i) {
            int c = s.charAt(i) - 32;
            if (c >= 0 && c < this.count) {
               STBTruetype.stbtt_GetBakedQuad(this.chars, this.atlas, this.atlas, c, px, py, q, true);
               g.glyph(this.texture, q.x0(), q.y0(), q.x1(), q.y1(), q.s0(), q.t0(), q.s1(), q.t1(), argb);
            }
         }
      } catch (Throwable var13) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var12) {
               var13.addSuppressed(var12);
            }
         }

         throw var13;
      }

      if (stack != null) {
         stack.close();
      }

   }
}
