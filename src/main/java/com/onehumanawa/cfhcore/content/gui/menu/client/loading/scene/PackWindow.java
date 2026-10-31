package com.onehumanawa.cfhcore.content.gui.menu.client.loading.scene;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import org.lwjgl.opengl.GL32C;

public final class PackWindow {
   public static final String FILE = "startup-window.json";
   private static final long MAX_FILE = 33554432L;
   private static final int MAX_SIDE = 8192;
   private static final int DEFAULT_LOGO_HEIGHT = 40;
   private static final int MAX_LOGO_HEIGHT = 160;
   public final int background;
   public final int panel;
   public final int border;
   public final int track;
   public final int text;
   public final int muted;
   public final int dim;
   public final int accent;
   public final String title;
   public final String subtitle;
   public final String footer;
   public final List<String> tips;
   public final float tipSeconds;
   public final float vignette;
   public final float imageDim;
   private final float logoHeight;
   private final java.nio.file.Path imageFile;
   private final java.nio.file.Path logoFile;
   private final Consumer<String> warn;
   private int imageTex;
   private int logoTex;
   private int imageW;
   private int imageH;
   private int logoW;
   private int logoH;
   private boolean imageTried;
   private boolean logoTried;

   private PackWindow(Map<String, Object> o, java.nio.file.Path cfhguiDir, java.nio.file.Path gameDir, Consumer<String> warn) {
      this.warn = warn;
      this.background = colour(o, "background", -15329771);
      this.panel = colour(o, "panel", -266988011);
      this.border = colour(o, "border", -13421777);
      this.track = colour(o, "track", -15000806);
      this.text = colour(o, "text", -1250588);
      this.muted = colour(o, "muted", -6185065);
      this.dim = colour(o, "dim", -9540506);
      this.accent = colour(o, "accent", 0);
      this.title = Json.string(o.get("title"), "Minecraft");
      this.subtitle = Json.string(o.get("subtitle"), "Java Edition {mc}");
      this.footer = Json.string(o.get("footer"), "{loader}");
      List<String> lines = new ArrayList();

      for(Object t : Json.list(o.get("tips"))) {
         if (t instanceof String s) {
            if (!s.isBlank()) {
               lines.add(s.strip());
            }
         }
      }

      this.tips = List.copyOf(lines);
      this.tipSeconds = Math.max(2.0F, Json.number(o.get("tipSeconds"), 6.0F));
      this.vignette = clamp01(Json.number(o.get("vignette"), 0.35F));
      this.imageDim = clamp01(Json.number(o.get("imageDim"), 0.0F));
      this.logoHeight = Math.max(8.0F, Math.min(160.0F, Json.number(o.get("logoHeight"), 40.0F)));
      this.imageFile = file(Json.string(o.get("image"), ""), cfhguiDir, gameDir);
      this.logoFile = file(Json.string(o.get("logo"), ""), cfhguiDir, gameDir);
   }

   public static PackWindow read(java.nio.file.Path cfhguiDir, java.nio.file.Path gameDir, Consumer<String> warn) {
      java.nio.file.Path file = cfhguiDir.resolve("startup-window.json");
      if (!Files.isRegularFile(file, new LinkOption[0])) {
         return null;
      } else {
         try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            if (!text.isEmpty() && text.charAt(0) == '\ufeff') {
               text = text.substring(1);
            }

            Map<String, Object> o = Json.object(Json.parse(text));
            return Boolean.FALSE.equals(o.get("enabled")) ? null : new PackWindow(o, cfhguiDir, gameDir, warn);
         } catch (RuntimeException | IOException e) {
            warn.accept("config/cfhgui/startup-window.json could not be read, the start-up window is CFHGui's own: " + ((Exception)e).getMessage());
            return null;
         }
      }
   }

   private static java.nio.file.Path file(String name, java.nio.file.Path cfhguiDir, java.nio.file.Path gameDir) {
      if (name.isBlank()) {
         return null;
      } else {
         try {
            java.nio.file.Path p = java.nio.file.Path.of(name.strip());
            if (p.isAbsolute()) {
               return p;
            } else {
               java.nio.file.Path inGame = gameDir.resolve(p);
               return Files.isRegularFile(inGame, new LinkOption[0]) ? inGame : cfhguiDir.resolve(p);
            }
         } catch (RuntimeException var5) {
            return null;
         }
      }
   }

   private static int colour(Map<String, Object> o, String key, int fallback) {
      String v = Json.string(o.get(key), "").strip().replace("#", "");

      try {
         if (v.length() == 6) {
            return -16777216 | Integer.parseUnsignedInt(v, 16);
         }

         if (v.length() == 8) {
            return Integer.parseUnsignedInt(v, 16);
         }
      } catch (NumberFormatException var5) {
      }

      return fallback;
   }

   private static float clamp01(float v) {
      return Math.max(0.0F, Math.min(1.0F, v));
   }

   public int accent(int fallback) {
      return this.accent != 0 ? this.accent : fallback;
   }

   public static String fill(String line, String mc, String loader) {
      return line.replace("{mc}", mc).replace("{loader}", loader).strip();
   }

   public String tip(long nanos) {
      return this.tips.isEmpty() ? null : (String)this.tips.get((int)(nanos / (long)(this.tipSeconds * 1.0E9F) % (long)this.tips.size()));
   }

   public void drawBackground(Gfx gfx, float w, float h) {
      gfx.rect(0.0F, 0.0F, w, h, this.background);
      if (this.image()) {
         float s = Math.max(w / (float)this.imageW, h / (float)this.imageH);
         float dw = (float)this.imageW * s;
         float dh = (float)this.imageH * s;
         gfx.image(this.imageTex, (w - dw) / 2.0F, (h - dh) / 2.0F, dw, dh, 0.0F, 0.0F, 1.0F, 1.0F, -1);
         if (this.imageDim > 0.0F) {
            gfx.rect(0.0F, 0.0F, w, h, Math.round(this.imageDim * 255.0F) << 24);
         }
      }

      if (this.vignette > 0.0F) {
         gfx.vignette(w, h, Math.round(this.vignette * 255.0F) << 24);
      }

   }

   public boolean hasLogo() {
      return this.logo();
   }

   public float[] logoSize(float maxW) {
      float lh = Math.min(this.logoHeight, (float)this.logoH);
      float lw = (float)this.logoW * lh / (float)this.logoH;
      if (lw > maxW) {
         lh *= maxW / lw;
         lw = maxW;
      }

      return new float[]{lw, lh};
   }

   public void drawLogo(Gfx gfx, float x, float y, float w, float h) {
      if (this.logo()) {
         gfx.image(this.logoTex, x, y, w, h, 0.0F, 0.0F, 1.0F, 1.0F, -1);
      }

   }

   private boolean image() {
      if (!this.imageTried) {
         this.imageTried = true;
         int[] made = this.texture(this.imageFile, "picture");
         if (made != null) {
            this.imageTex = made[0];
            this.imageW = made[1];
            this.imageH = made[2];
         }
      }

      return this.imageTex != 0;
   }

   private boolean logo() {
      if (!this.logoTried) {
         this.logoTried = true;
         int[] made = this.texture(this.logoFile, "logo");
         if (made != null) {
            this.logoTex = made[0];
            this.logoW = made[1];
            this.logoH = made[2];
         }
      }

      return this.logoTex != 0;
   }

   private int[] texture(java.nio.file.Path file, String what) {
      if (file == null) {
         return null;
      } else {
         try {
            if (!Files.isRegularFile(file, new LinkOption[0])) {
               throw new IOException("no such file");
            } else if (Files.size(file) > 33554432L) {
               throw new IOException("larger than 32 MB");
            } else {
               Picture p = Picture.read(Files.readAllBytes(file));
               if (p == null) {
                  throw new IOException("not a picture this window can read (PNG, JPEG, BMP, TGA)");
               } else if (p.w() <= 8192 && p.h() <= 8192) {
                  return new int[]{p.texture(), p.w(), p.h()};
               } else {
                  throw new IOException("larger than 8192 pixels a side");
               }
            }
         } catch (RuntimeException | OutOfMemoryError | IOException e) {
            this.warn.accept("the start-up window's " + what + " (" + String.valueOf(file) + ") is not shown: " + ((Throwable)e).getMessage());
            return null;
         }
      }
   }

   public void dispose() {
      if (this.imageTex != 0) {
         GL32C.glDeleteTextures(this.imageTex);
      }

      if (this.logoTex != 0) {
         GL32C.glDeleteTextures(this.logoTex);
      }

      this.imageTex = this.logoTex = 0;
   }

   public String toString() {
      return String.format(Locale.ROOT, "%s (picture %s, logo %s, %d tips)", "startup-window.json", this.imageFile, this.logoFile, this.tips.size());
   }
}
