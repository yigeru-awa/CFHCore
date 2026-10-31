package com.onehumanawa.cfhcore.content.gui.core.layout;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.ImageFiles;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class LayoutBackground {
   private static final Set<String> REQUESTED = ConcurrentHashMap.newKeySet();
   private static final Map<String, Path> FILES = new ConcurrentHashMap();
   private static final Map<String, Long> IMAGE_STAMPS = new ConcurrentHashMap();
   private static @Nullable Screen frameScreen;
   private static boolean drawnThisFrame;
   private static long lastErrorMs;

   public static void beginFrame(Screen screen) {
      frameScreen = screen;
      drawnThisFrame = false;
   }

   public static boolean overrides(@Nullable Screen screen) {
      return background(screen) != null;
   }

   private static ScreenLayout.@Nullable Background background(@Nullable Screen screen) {
      if (screen != null && !ScreenIds.isContainer(screen)) {
         try {
            return background(LayoutApplier.layoutId(screen));
         } catch (Exception var2) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static ScreenLayout.@Nullable Background background(String layoutId) {
      ScreenLayout layout = LayoutStore.get(layoutId);
      return layout.hasBackground() ? layout.background : null;
   }

   public static boolean render(Screen screen, GuiGraphics g, float partialTick, @Nullable PanoramaRenderer panorama) {
      if (screen instanceof Host) {
         return false;
      } else {
         ScreenLayout.Background bg = background(screen);
         if (bg == null) {
            return false;
         } else if (screen == frameScreen && drawnThisFrame) {
            return true;
         } else {
            frameScreen = screen;
            drawnThisFrame = true;
            draw(bg, g, screen.width, screen.height, partialTick, panorama, ScreenIds.of(screen));
            return true;
         }
      }
   }

   public static boolean drawOwn(Screen host, GuiGraphics g, float partialTick, @Nullable PanoramaRenderer panorama) {
      ScreenLayout.Background bg = background(host);
      if (bg == null) {
         return false;
      } else {
         draw(bg, g, host.width, host.height, partialTick, panorama, ScreenIds.of(host));
         return true;
      }
   }

   public static boolean paint(String layoutId, GuiGraphics g, int w, int h, float partialTick, @Nullable PanoramaRenderer panorama) {
      ScreenLayout.Background bg = background(layoutId);
      if (bg != null && (panorama != null || !"panorama".equals(kind(bg)))) {
         draw(bg, g, w, h, partialTick, panorama, layoutId);
         return true;
      } else {
         return false;
      }
   }

   private static String kind(ScreenLayout.Background bg) {
      return bg.kind == null ? "default" : bg.kind.trim().toLowerCase(Locale.ROOT);
   }

   private static void draw(ScreenLayout.Background bg, GuiGraphics g, int w, int h, float partialTick, @Nullable PanoramaRenderer panorama, String what) {
      try {
         switch (kind(bg)) {
            case "none":
            default:
               break;
            case "color":
            case "colour":
               g.fill(0, 0, w, h, Colors.fromHex(bg.value, -16777216));
               break;
            case "image":
               drawImage(g, bg, w, h);
               break;
            case "panorama":
               if (panorama != null) {
                  panorama.render(g, w, h, 1.0F, partialTick);
               }

               if (bg.dim) {
                  Screen.renderMenuBackgroundTexture(g, Screen.MENU_BACKGROUND, 0, 0, 0.0F, 0.0F, w, h);
               }
         }
      } catch (Exception e) {
         long now = Clock.nowMs();
         if (now - lastErrorMs > 5000L) {
            lastErrorMs = now;
            CFHGui.LOGGER.warn("[CFHGui] layout background of {} failed: {}", what, e.toString());
         }
      }

   }

   private static void drawImage(GuiGraphics g, ScreenLayout.Background bg, int w, int h) {
      String src = bg.value == null ? "" : bg.value.trim();
      if (src.isEmpty()) {
         g.fill(0, 0, w, h, -16777216);
      } else {
         if (isResource(src)) {
            ResourceLocation rl = ResourceLocation.tryParse(src);
            if (rl != null) {
               RenderSystem.enableBlend();
               g.blit(rl, 0, 0, w, h, 0.0F, 0.0F, 1, 1, 1, 1);
               RenderSystem.disableBlend();
            }
         } else {
            Path p = (Path)FILES.computeIfAbsent(src, LayoutBackground::file);
            if (REQUESTED.add(src)) {
               IMAGE_STAMPS.put(src, stamp(p));
            }

            ImageFiles.Picture img = ImageFiles.get(p);
            ResourceLocation frame = img == null ? null : img.frame();
            if (frame == null) {
               g.fill(0, 0, w, h, -16777216);
               return;
            }

            float s = Math.max((float)w / (float)Math.max(1, img.width()), (float)h / (float)Math.max(1, img.height()));
            int dw = Math.max(1, Math.round((float)img.width() * s));
            int dh = Math.max(1, Math.round((float)img.height() * s));
            int dx = (w - dw) / 2;
            int dy = (h - dh) / 2;
            g.fill(0, 0, w, h, -16777216);
            RenderSystem.enableBlend();
            g.blit(frame, dx, dy, dw, dh, 0.0F, 0.0F, 1, 1, 1, 1);
            RenderSystem.disableBlend();
         }

         if (bg.dim) {
            g.fill(0, 0, w, h, 1711276032);
         }

      }
   }

   private static Path file(String src) {
      Path inGame = CFHGuiPlatform.get().gameDir().resolve(src);
      if (Files.isRegularFile(inGame, new LinkOption[0])) {
         return inGame;
      } else {
         Path inConfig = JsonConfig.dir().resolve(src);
         return Files.isRegularFile(inConfig, new LinkOption[0]) ? inConfig : inGame;
      }
   }

   private static long stamp(Path p) {
      try {
         return Files.isRegularFile(p, new LinkOption[0]) ? Files.getLastModifiedTime(p).toMillis() * 31L + Files.size(p) : 0L;
      } catch (Exception var2) {
         return -1L;
      }
   }

   private static boolean isResource(String src) {
      return src.matches("^[a-z0-9_.-]{2,}:[a-z0-9_.-][a-z0-9_./-]*$");
   }

   public static void forgetImages() {
      REQUESTED.clear();
      IMAGE_STAMPS.clear();
      FILES.clear();
   }

   public static void checkImages() {
      for(String src : (String[])REQUESTED.toArray(new String[0])) {
         Long was = (Long)IMAGE_STAMPS.get(src);
         Path p = file(src);
         if (was == null || was != stamp(p)) {
            REQUESTED.remove(src);
            IMAGE_STAMPS.remove(src);
            Path found = (Path)FILES.remove(src);
            if (found != null && !found.equals(p)) {
               ImageFiles.invalidate(found);
            }

            ImageFiles.invalidate(p);
         }
      }

   }

   private LayoutBackground() {
   }

   public interface Host {
   }
}
