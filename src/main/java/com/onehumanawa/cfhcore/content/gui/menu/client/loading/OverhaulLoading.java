package com.onehumanawa.cfhcore.content.gui.menu.client.loading;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.DevHarness;
import com.onehumanawa.cfhcore.content.gui.core.client.StartupTime;
import com.onehumanawa.cfhcore.content.gui.core.config.CoreConfig;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.platform.Loader;
import com.onehumanawa.cfhcore.content.gui.core.platform.ModInfo;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Style;
import com.onehumanawa.cfhcore.content.gui.core.theme.PixelFont;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.loading.scene.LoadingScene;
import com.onehumanawa.cfhcore.content.gui.menu.client.loading.scene.StartupClock;
import com.onehumanawa.cfhcore.content.gui.menu.client.loading.scene.StartupScene;
import com.onehumanawa.cfhcore.content.gui.menu.client.loading.scene.StartupTimeline;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.jetbrains.annotations.Nullable;

public final class OverhaulLoading {
   private static final int RED = -1101251;
   private static final int BLACK = -16777216;
   private static final int SHEETS = 20;
   private static @Nullable StartupScene scene;
   private static @Nullable String made;
   private static boolean failed;
   private static final Feed FEED = new Feed();

   public static boolean active() {
      if (failed) {
         return false;
      } else {
         String asked = DevHarness.autoLayout();
         if (asked == null) {
            return MenuSlots.effective("cfhgui:loading") == Layout.OVERHAUL;
         } else {
            return "OVERHAUL".equalsIgnoreCase(asked) && CFHGuiMenu.config().loadingScreens;
         }
      }
   }

   public static boolean draw(ReloadInstance reload, float alpha) {
      RenderSystem.assertOnRenderThread();
      Minecraft mc = Minecraft.getInstance();

      try {
         StartupScene s = scene(mc);
         FEED.frame(reload);
         s.draw(mc.getWindow().getWidth(), mc.getWindow().getHeight(), (float)mc.getWindow().getGuiScale(), false, alpha, FEED);
         DevHarness.loadingFrame("startup");
         return true;
      } catch (LinkageError | RuntimeException e) {
         failed = true;
         CFHGuiMenu.LOGGER.error("[CFHGui Menu] the Overhaul loading screen failed, the game's own takes over", e);
         drop();
         return false;
      }
   }

   private static StartupScene scene(Minecraft mc) {
      CoreConfig cfg = CFHGui.config();
      String skin = DevHarness.autoSkin();
      boolean vanilla = skin != null ? "VANILLA".equalsIgnoreCase(skin) : MenuSlots.style("cfhgui:loading") == Style.VANILLA;
      int accent = Theme.current().accent();
      int background = (Boolean)mc.options.darkMojangStudiosBackground().get() ? -16777216 : -1101251;
      String font = !vanilla && cfg.headingFont ? PixelFont.parse(cfg.pixelFont).key() : "monocraft";
      String kind = CFHGuiPlatform.get().isModLoaded("create") ? StartupScene.kind(cfg.startupScene) : "island";
      String title = MenuSlots.effective("minecraft:title").name();
      String key = accent + "/" + vanilla + "/" + background + "/" + font + "/" + kind + "/" + title;
      if (scene != null && key.equals(made)) {
         return scene;
      } else {
         drop();
         Loader loader = CFHGuiPlatform.get().loader();
         String version = (String)CFHGuiPlatform.get().modInfo(loader == Loader.FABRIC ? "fabricloader" : "neoforge").map(ModInfo::version).orElse("");
         scene = StartupScene.make(kind, new LoadingScene.Look(accent, vanilla, background, font, "Minecraft " + SharedConstants.getCurrentVersion().getName(), (loader.displayName + " " + version).trim()), OverhaulLoading::font, System::nanoTime, OverhaulLoading::asset, title, (e) -> CFHGuiMenu.LOGGER.warn("[CFHGui Menu] no island for the loading screen, the factory stands in: {}", e.toString()));
         StartupScene var13 = scene;
         if (var13 instanceof LoadingScene) {
            LoadingScene factory = (LoadingScene)var13;
            if (factory.create()) {
               CFHGuiMenu.LOGGER.info("[CFHGui Menu] the loading screen's factory is built of Create's blocks");
            } else if (CFHGuiPlatform.get().isModLoaded("create")) {
               CFHGuiMenu.LOGGER.warn("[CFHGui Menu] Create is installed, but the loading screen's factory could not be built of its blocks (CFHGui's own stands in): {}", String.valueOf(factory.createFailure()));
            } else {
               CFHGuiMenu.LOGGER.info("[CFHGui Menu] the loading screen's factory is CFHGui's own (Create is not installed)");
            }
         }

         made = key;
         return scene;
      }
   }

   private static byte @Nullable [] asset(String namespace, String path) {
      try {
         if (!namespace.equals("minecraft")) {
            Optional<Path> file = CFHGuiPlatform.get().modFile(namespace, "assets/" + namespace + "/" + path);
            return file.isPresent() ? Files.readAllBytes((Path)file.get()) : null;
         } else {
            IoSupplier<InputStream> file = Minecraft.getInstance().getVanillaPackResources().getResource(PackType.CLIENT_RESOURCES, ResourceLocation.withDefaultNamespace(path));
            if (file == null) {
               return null;
            } else {
               InputStream in = (InputStream)file.get();

               byte[] var4;
               try {
                  var4 = in.readAllBytes();
               } catch (Throwable var7) {
                  if (in != null) {
                     try {
                        in.close();
                     } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                     }
                  }

                  throw var7;
               }

               if (in != null) {
                  in.close();
               }

               return var4;
            }
         }
      } catch (RuntimeException | IOException var8) {
         return null;
      }
   }

   private static byte[] font(String file) {
      try {
         InputStream in = CFHGui.class.getResourceAsStream("/assets/cfhgui/font/" + file);

         byte[] var2;
         try {
            if (in == null) {
               throw new IllegalStateException("missing font " + file);
            }

            var2 = in.readAllBytes();
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

         return var2;
      } catch (IOException e) {
         throw new IllegalStateException("could not read font " + file, e);
      }
   }

   private static void drop() {
      StartupScene old = scene;
      scene = null;
      made = null;
      if (old != null) {
         try {
            old.dispose();
         } catch (LinkageError | RuntimeException var2) {
         }

      }
   }

   private OverhaulLoading() {
   }

   private static final class Feed implements LoadingScene.Feed {
      private @Nullable ReloadInstance reload;
      private @Nullable StartupTimeline startup;
      private long began;
      private long expected = -1L;
      private long doneAt = -1L;
      private float overall;
      private float doneFrom;
      private long sheetsBefore;
      private long used;
      private long max;

      void frame(ReloadInstance now) {
         long t = StartupClock.elapsedMs();
         if (now != this.reload) {
            if (this.reload != null) {
               this.sheetsBefore += (long)Math.floor((double)(this.overall * 20.0F)) + 1L;
            }

            this.reload = now;
            this.overall = 0.0F;
            this.doneAt = -1L;
            this.began = t;
            this.startup = null;
            if (StartupTime.running()) {
               StartupTimeline.mark("reload");
               this.startup = StartupTimeline.read(JsonConfig.dir());
            } else {
               this.expected = StartupTimeline.expectedReload(JsonConfig.dir());
            }
         }

         if (this.startup != null) {
            this.overall = this.startup.progress();
            if (this.startup.ended()) {
               String failed = StartupTimeline.save(JsonConfig.dir());
               if (failed != null) {
                  CFHGuiMenu.LOGGER.debug("[CFHGui Menu] could not keep the start-up's times: {}", failed);
               }
            }
         } else {
            this.overall = Math.max(this.overall, this.later(now, t - this.began));
         }

         Runtime rt = Runtime.getRuntime();
         this.max = rt.maxMemory();
         this.used = rt.totalMemory() - rt.freeMemory();
      }

      private float later(ReloadInstance now, long ms) {
         if (now.isDone()) {
            if (this.doneAt < 0L) {
               this.doneAt = ms;
               this.doneFrom = this.overall;
               String failed = StartupTimeline.saveReload(JsonConfig.dir(), ms);
               if (failed != null) {
                  CFHGuiMenu.LOGGER.debug("[CFHGui Menu] could not keep the reload's time: {}", failed);
               }
            }

            float p = 1.0F - (1.0F - this.doneFrom) * (float)Math.exp((double)(-(ms - this.doneAt)) / (double)150.0F);
            return p > 0.999F ? 1.0F : p;
         } else {
            double u = (double)ms / (double)(this.expected > 0L ? this.expected : 10000L);
            return (float)(u < 0.85 ? u : 0.85 + 0.14 * ((double)1.0F - Math.exp(-(u - 0.85) / 0.14)));
         }
      }

      public float overall() {
         return this.overall;
      }

      public String task() {
         return I18n.exists("cfhgui.loading.resources") ? I18n.get("cfhgui.loading.resources", new Object[0]) : "Loading resources";
      }

      public float taskProgress() {
         return this.overall * 20.0F - (float)Math.floor((double)(this.overall * 20.0F));
      }

      public @Nullable String taskCount() {
         return null;
      }

      public long tasksDone() {
         return this.sheetsBefore + (long)Math.floor((double)(this.overall * 20.0F));
      }

      public @Nullable String detail() {
         return null;
      }

      public long memoryUsed() {
         return this.used;
      }

      public long memoryMax() {
         return this.max;
      }

      public @Nullable String elapsed() {
         return CFHGui.config().startupTimer && StartupTime.running() ? StartupClock.clock(StartupClock.elapsedMs()) : null;
      }
   }
}
