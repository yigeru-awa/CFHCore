package com.onehumanawa.cfhcore.content.gui.core.compat.maps;

import com.mojang.blaze3d.platform.NativeImage;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;

public final class PinImages {
   private static final int RIM = -15724526;
   private static final int RIM_LIT = -1;
   public static final int PLATE = 14;
   public static final int WIDTH = 14;
   public static final int HEIGHT = 17;
   public static final int TEXELS = 2;
   public static final int TEXTURE_WIDTH = 28;
   public static final int TEXTURE_HEIGHT = 34;
   private static final Map<Long, ResourceLocation> MADE = new HashMap();
   private static int made;
   private static int generation;
   private static @Nullable NativeImage glyphs;
   private static boolean glyphsTried;

   static int generation() {
      return generation;
   }

   public static ResourceLocation of(Icon icon, int rgb, boolean lit) {
      long key = (long)icon.ordinal() << 25 | (lit ? 1L : 0L) << 24 | (long)(rgb & 16777215);
      ResourceLocation location = (ResourceLocation)MADE.get(key);
      if (location == null) {
         NativeImage image = new NativeImage(28, 34, true);
         int fill = -16777216 | (lit ? Colors.brighten(rgb, 0.12F) : rgb) & 16777215;
         shape(image, lit ? -1 : -15724526, fill);
         glyph(image, icon, fill);
         int var10000 = made++;
         location = CFHGui.id("maps/pin/" + var10000);
         Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
         MADE.put(key, location);
      }

      return location;
   }

   static int unit(int x, int y) {
      if (y < 13) {
         return stepped(x - 1, y - 1, 12, 2) ? 2 : (stepped(x, y, 14, 3) ? 1 : 0);
      } else {
         int inset = 3 + (y - 13);
         if (x >= inset && x < 14 - inset) {
            boolean edge = x == inset || x == 14 - inset - 1;
            return y == 13 ? (x >= inset + 2 && x < 14 - inset - 2 ? 2 : 1) : (!edge && y != 16 ? 2 : 1);
         } else {
            return 0;
         }
      }
   }

   private static boolean stepped(int x, int y, int size, int r) {
      if (x >= 0 && y >= 0 && x < size && y < size) {
         int inset = y < r ? r - y : (y >= size - r ? r - (size - 1 - y) : 0);
         return x >= inset && x < size - inset;
      } else {
         return false;
      }
   }

   private static void shape(NativeImage out, int rim, int fill) {
      int rimPixel = abgr(rim);
      int fillPixel = abgr(fill);

      for(int y = 0; y < 34; ++y) {
         for(int x = 0; x < 28; ++x) {
            int kind = unit(x / 2, y / 2);
            out.setPixelRGBA(x, y, kind == 2 ? fillPixel : (kind == 1 ? rimPixel : 0));
         }
      }

   }

   private static void glyph(NativeImage out, Icon icon, int fill) {
      NativeImage atlas = glyphs();
      if (atlas != null) {
         int tint = Colors.readableOn(fill);
         int left = 6;
         int top = 6;

         for(int v = 0; v < 16; ++v) {
            for(int u = 0; u < 16; ++u) {
               int sx = icon.u() + u;
               int sy = icon.v() + v;
               if (sx < atlas.getWidth() && sy < atlas.getHeight()) {
                  int a = atlas.getPixelRGBA(sx, sy) >>> 24;
                  if (a != 0) {
                     out.setPixelRGBA(6 + u, 6 + v, blend(out.getPixelRGBA(6 + u, 6 + v), abgr(Colors.withAlpha(tint, a))));
                  }
               }
            }
         }

      }
   }

   private static @Nullable NativeImage glyphs() {
      if (glyphs == null && !glyphsTried) {
         glyphsTried = true;

         try {
            Resource resource = Minecraft.getInstance().getResourceManager().getResourceOrThrow(Icons.ATLAS);
            InputStream in = resource.open();

            try {
               glyphs = NativeImage.read(in);
            } catch (Throwable var5) {
               if (in != null) {
                  try {
                     in.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (in != null) {
               in.close();
            }
         } catch (Exception e) {
            CFHGui.LOGGER.warn("[CFHGui] the glyphs could not be read for the pins on the maps: {}", e.toString());
         }

         return glyphs;
      } else {
         return glyphs;
      }
   }

   static void clear() {
      TextureManager textures = Minecraft.getInstance().getTextureManager();

      for(ResourceLocation location : MADE.values()) {
         textures.release(location);
      }

      MADE.clear();
      ++generation;
      if (glyphs != null) {
         glyphs.close();
         glyphs = null;
      }

      glyphsTried = false;
   }

   private static int abgr(int argb) {
      return argb & -16711936 | argb >> 16 & 255 | (argb & 255) << 16;
   }

   private static int blend(int under, int over) {
      int a = over >>> 24;
      if (a >= 255) {
         return over;
      } else if (a == 0) {
         return under;
      } else {
         int r = ((over & 255) * a + (under & 255) * (255 - a)) / 255;
         int g = ((over >> 8 & 255) * a + (under >> 8 & 255) * (255 - a)) / 255;
         int b = ((over >> 16 & 255) * a + (under >> 16 & 255) * (255 - a)) / 255;
         int ua = under >>> 24;
         int oa = a + ua * (255 - a) / 255;
         return oa << 24 | b << 16 | g << 8 | r;
      }
   }

   private PinImages() {
   }
}
