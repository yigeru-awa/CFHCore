package com.onehumanawa.cfhcore.content.gui.core.theme;

import net.minecraft.util.Mth;

public final class Colors {
   public static int argb(int a, int r, int g, int b) {
      return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
   }

   public static int alpha(int c) {
      return c >>> 24;
   }

   public static int red(int c) {
      return c >> 16 & 255;
   }

   public static int green(int c) {
      return c >> 8 & 255;
   }

   public static int blue(int c) {
      return c & 255;
   }

   public static int withAlpha(int c, int a) {
      return Mth.clamp(a, 0, 255) << 24 | c & 16777215;
   }

   public static int scaleAlpha(int c, float f) {
      return withAlpha(c, Math.round((float)alpha(c) * Mth.clamp(f, 0.0F, 1.0F)));
   }

   public static int lerp(int a, int b, float t) {
      float k = Mth.clamp(t, 0.0F, 1.0F);
      return argb(Math.round((float)alpha(a) + (float)(alpha(b) - alpha(a)) * k), Math.round((float)red(a) + (float)(red(b) - red(a)) * k), Math.round((float)green(a) + (float)(green(b) - green(a)) * k), Math.round((float)blue(a) + (float)(blue(b) - blue(a)) * k));
   }

   public static int mix(int a, int b, float t) {
      return withAlpha(lerp(a, b, t), alpha(a));
   }

   public static int brighten(int c, float f) {
      return f >= 0.0F ? mix(c, -1, f) : mix(c, -16777216, -f);
   }

   public static int scale(int c, float f) {
      return argb(alpha(c), Math.round(Mth.clamp((float)red(c) * f, 0.0F, 255.0F)), Math.round(Mth.clamp((float)green(c) * f, 0.0F, 255.0F)), Math.round(Mth.clamp((float)blue(c) * f, 0.0F, 255.0F)));
   }

   public static int fromHex(String hex, int fallback) {
      if (hex == null) {
         return fallback;
      } else {
         String h = hex.trim();
         if (h.startsWith("#")) {
            h = h.substring(1);
         }

         try {
            if (h.length() == 6) {
               return -16777216 | Integer.parseInt(h, 16);
            }

            if (h.length() == 8) {
               return (int)Long.parseLong(h, 16);
            }
         } catch (NumberFormatException var4) {
         }

         return fallback;
      }
   }

   public static String toHex(int c) {
      return "#%02X%02X%02X".formatted(red(c), green(c), blue(c));
   }

   public static float luminance(int c) {
      return (0.2126F * (float)red(c) + 0.7152F * (float)green(c) + 0.0722F * (float)blue(c)) / 255.0F;
   }

   public static double relativeLuminance(int c) {
      return 0.2126 * linear(red(c)) + 0.7152 * linear(green(c)) + 0.0722 * linear(blue(c));
   }

   private static double linear(int channel) {
      double v = (double)channel / (double)255.0F;
      return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
   }

   public static double contrast(int a, int b) {
      double la = relativeLuminance(a);
      double lb = relativeLuminance(b);
      return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
   }

   public static int legibleOn(int c, int bg, double minContrast) {
      if (contrast(c, bg) >= minContrast) {
         return c;
      } else {
         float[] hsv = rgbToHsv(c);
         float s = hsv[1];
         float v = hsv[2];
         int out = c;

         for(int i = 0; i < 48 && contrast(out, bg) < minContrast; ++i) {
            if (v < 1.0F) {
               v = Math.min(1.0F, v + 0.04F);
            } else {
               s = Math.max(0.0F, s - 0.04F);
            }

            out = withAlpha(hsvToRgb(hsv[0], s, v), alpha(c));
         }

         return out;
      }
   }

   public static int readableOn(int bg) {
      return luminance(bg) > 0.55F ? -15461357 : -658193;
   }

   public static int hover(int c) {
      return brighten(c, 0.12F);
   }

   public static int hsvToRgb(float h, float s, float v) {
      int rgb = Mth.hsvToRgb((h % 1.0F + 1.0F) % 1.0F, Mth.clamp(s, 0.0F, 1.0F), Mth.clamp(v, 0.0F, 1.0F));
      return -16777216 | rgb;
   }

   public static float[] rgbToHsv(int c) {
      float r = (float)red(c) / 255.0F;
      float g = (float)green(c) / 255.0F;
      float b = (float)blue(c) / 255.0F;
      float max = Math.max(r, Math.max(g, b));
      float min = Math.min(r, Math.min(g, b));
      float d = max - min;
      float h = 0.0F;
      if (d > 0.0F) {
         if (max == r) {
            h = (g - b) / d % 6.0F;
         } else if (max == g) {
            h = (b - r) / d + 2.0F;
         } else {
            h = (r - g) / d + 4.0F;
         }

         h /= 6.0F;
         if (h < 0.0F) {
            ++h;
         }
      }

      return new float[]{h, max == 0.0F ? 0.0F : d / max, max};
   }

   private Colors() {
   }
}
