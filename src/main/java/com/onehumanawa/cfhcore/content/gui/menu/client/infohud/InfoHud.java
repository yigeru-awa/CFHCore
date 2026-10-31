package com.onehumanawa.cfhcore.content.gui.menu.client.infohud;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiKeys;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class InfoHud {
   public static final KeyMapping TOGGLE_KEY;
   public static final KeyMapping MODE_KEY;
   private static final float SLIDE = 6.0F;
   private static final double USUAL_SCALE = 0.76;
   private static boolean initialised;
   private static boolean failed;
   private static boolean listening;
   private static boolean running;
   private static boolean hudHidden;
   private static volatile boolean worldChanged;
   private static long placeAtMs;
   private static long worldAtMs;
   private static long slowAtMs;
   private static long layoutAtMs;
   private static long measuredAtMs;
   private static long lastFrameNanos;
   private static boolean frameRateTurn;
   private static int seenRevision;
   private static @Nullable Theme seenTheme;
   private static InfoHudConfig.@Nullable Mode seenMode;
   private static final Smooth ALPHA;
   private static final Smooth EDGE_X;
   private static final Smooth EDGE_Y;
   static final Smooth RAM_BAR;
   static final Smooth RAM_GHOST;
   static final Smooth VRAM_BAR;
   private static final Rects TAKEN;
   private static final int[] SPOT;
   private static int lastSpotX;
   private static int lastSpotY;
   private static InfoHudConfig.Corner corner;
   private static float unit;
   private static boolean painting;
   private static @Nullable InfoHudConfig config;
   private static final Runnable PAINT;

   private InfoHud() {
   }

   public static synchronized void init() {
      if (!initialised) {
         initialised = true;
         InfoHudConfig.file();
         CFHGuiKeys.register(TOGGLE_KEY);
         CFHGuiKeys.register(MODE_KEY);
         CFHGuiEvents.CLIENT_TICK_END.register(InfoHud::tick);
         CFHGuiEvents.HUD_RENDER.register(InfoHud::render);
         CFHGuiEvents.CLIENT_JOINED_SERVER.register(() -> worldChanged = true);
         CFHGuiEvents.CLIENT_LEFT_SERVER.register(() -> worldChanged = true);
         InfoHudSettings.init();
         InfoHudHarness.init();
      }
   }

   public static void toggle() {
      InfoHudConfig cfg = InfoHudConfig.get();
      cfg.enabled = !cfg.enabled;
      InfoHudConfig.save();
   }

   public static void cycleMode() {
      InfoHudConfig cfg = InfoHudConfig.get();
      cfg.mode = (cfg.mode() == InfoHudConfig.Mode.FULL ? InfoHudConfig.Mode.COMPACT : InfoHudConfig.Mode.FULL).name();
      InfoHudConfig.save();
   }

   private static void tick() {
      Minecraft mc = Minecraft.getInstance();

      while(TOGGLE_KEY.consumeClick()) {
         toggle();
      }

      while(MODE_KEY.consumeClick()) {
         cycleMode();
      }

      InfoHudConfig cfg = InfoHudConfig.get();
      if (cfg.enabled && !failed) {
         if (mc.options.hideGui || mc.level == null) {
            FrameTimes.interrupt();
         }

         LatencyProbe.want(cfg.latency && !mc.options.hideGui && wanted(mc, cfg));
         LatencyProbe.tick();
         HudReadings.tick(mc, cfg);
      } else {
         LatencyProbe.want(false);
      }
   }

   private static boolean wanted(Minecraft mc, InfoHudConfig cfg) {
      return cfg.enabled && mc.player != null && mc.level != null && !mc.getDebugOverlay().showDebugScreen();
   }

   private static void render(GuiGraphics g, float partialTick) {
      if (!failed) {
         InfoHudConfig cfg = InfoHudConfig.get();
         if (!cfg.enabled && ALPHA.value <= 0.01F) {
            if (running) {
               running = false;
               FrameTimes.interrupt();
               ALPHA.snap(0.0F);
            }

         } else {
            try {
               long began = InfoHudHarness.PROFILE ? InfoHudHarness.began() : 0L;
               frame(g, Minecraft.getInstance(), cfg);
               if (InfoHudHarness.PROFILE) {
                  InfoHudHarness.ended(began);
               }
            } catch (RuntimeException e) {
               failed = true;
               CFHGuiMenu.LOGGER.error("[CFHGui Menu] drawing the info overlay failed; it is off for this session", e);
            }

         }
      }
   }

   private static void frame(GuiGraphics g, Minecraft mc, InfoHudConfig cfg) {
      long nanos = System.nanoTime();
      long millis = Util.getMillis();
      running = true;
      if (worldChanged) {
         worldChanged = false;
         FrameTimes.reset();
         LatencyProbe.reset();
         HudReadings.forget();
         lastSpotY = -1;
         lastSpotX = -1;
      }

      if (mc.options.hideGui) {
         hudHidden = true;
      } else {
         if (cfg.enabled) {
            FrameTimes.frame(nanos);
         }

         float dt = lastFrameNanos == 0L ? 16.0F : Math.min(100.0F, (float)(nanos - lastFrameNanos) / 1000000.0F);
         lastFrameNanos = nanos;
         Theme theme = Theme.variant(Theme.globalStyle());
         float motion = theme.motion();
         float quick = motion <= 0.0F ? 1.0F : 1.0F - (float)Math.exp((double)(-dt / (70.0F * motion)));
         float easy = motion <= 0.0F ? 1.0F : 1.0F - (float)Math.exp((double)(-dt / (160.0F * motion)));
         boolean shown = wanted(mc, cfg);
         InfoHudConfig.Mode mode = cfg.mode();
         boolean otherSize = seenMode != null && seenMode != mode;
         seenMode = mode;
         if (hudHidden) {
            hudHidden = false;
            ALPHA.snap(shown ? 1.0F : 0.0F);
         } else if (otherSize && motion > 0.0F) {
            ALPHA.snap(0.0F);
         }

         ALPHA.to(shown ? 1.0F : 0.0F);
         float alpha = ALPHA.step(quick);
         if (!(alpha <= 0.01F) && mc.player != null && mc.level != null) {
            Font font = mc.font;
            config = cfg;
            if (!listening) {
               listening = true;
               ResourceManager var18 = mc.getResourceManager();
               if (var18 instanceof ReloadableResourceManager) {
                  ReloadableResourceManager reloadable = (ReloadableResourceManager)var18;
                  reloadable.registerReloadListener((ResourceManagerReloadListener)(manager) -> BakedText.reloaded());
               }
            }

            boolean changed = seenRevision != InfoHudConfig.revision() || seenTheme != theme || otherSize;
            if (changed) {
               seenRevision = InfoHudConfig.revision();
               seenTheme = theme;
               layoutAtMs = 0L;
               slowAtMs = 0L;
               worldAtMs = 0L;
               placeAtMs = 0L;
            }

            if (millis - placeAtMs >= 100L) {
               placeAtMs = millis;
               HudReadings.place(mc, font, cfg);
            }

            if (millis - worldAtMs >= 250L) {
               worldAtMs = millis;
               frameRateTurn = !frameRateTurn;
               HudReadings.world(mc, font, cfg);
               HudReadings.game(mc, font, cfg, nanos, millis, frameRateTurn || changed);
            }

            if (millis - slowAtMs >= 1000L) {
               slowAtMs = millis;
               HudReadings.slow(mc, font, cfg, nanos, millis);
            }

            if (millis - measuredAtMs >= 2000L) {
               measuredAtMs = millis;
               Seg.remeasure(font);
               BakedText.invalidate();
            }

            Window window = mc.getWindow();
            double guiScale = Math.max((double)1.0F, window.getGuiScale());
            int sw = g.guiWidth();
            int sh = g.guiHeight();
            float pulse = motion <= 0.0F ? 0.0F : 0.5F + 0.5F * Mth.sin((float)(millis % 1100L) / 1100.0F * ((float)Math.PI * 2F));
            HudLook.begin(theme, cfg, font, alpha, cfg.corner().right, pulse);
            if (millis - layoutAtMs >= 250L) {
               layoutAtMs = millis;
               layout(mc, cfg, window, guiScale, sw, sh, millis, changed);
            }

            RAM_BAR.to(HudReadings.ramUsed);
            RAM_GHOST.to(HudReadings.ramAllocated);
            VRAM_BAR.to(HudReadings.vramUsed);
            RAM_BAR.step(easy);
            RAM_GHOST.step(easy);
            VRAM_BAR.step(easy);
            HudLook.step(cfg, quick, easy);
            int width = HudLook.width();
            int height = HudLook.height();
            float edgeX = EDGE_X.step(easy);
            float edgeY = EDGE_Y.step(easy);
            if (width > 4 && height > 4) {
               float slide = (1.0F - alpha) * 6.0F * unit;
               float x = corner.right ? edgeX - (float)width * unit + slide : edgeX - slide;
               float y = corner.bottom ? edgeY - (float)height * unit : edgeY;
               HudCanvas.begin(g, font, (float)((double)Math.round((double)x * guiScale) / guiScale), (float)((double)Math.round((double)y * guiScale) / guiScale), unit);
               painting = true;

               try {
                  batch(g);
               } finally {
                  painting = false;
                  HudCanvas.end();
               }

            }
         }
      }
   }

   private static void batch(GuiGraphics g) {
      if (InfoHudHarness.PROFILE) {
         InfoHudHarness.part(0);
      }

      g.drawManaged(PAINT);
      if (InfoHudHarness.PROFILE) {
         InfoHudHarness.part(3);
      }

   }

   private static void layout(Minecraft mc, InfoHudConfig cfg, Window window, double guiScale, int sw, int sh, long millis, boolean changed) {
      int wanted = (int)Math.max(1L, Math.round(Mth.clamp(cfg.scale, (double)0.5F, (double)2.0F) * guiScale));
      HudLook.measure(cfg, (int)((double)window.getWidth() * 0.4 / (double)wanted), changed);
      int w = HudLook.targetWidth;
      int h = HudLook.targetHeight;
      int pixels = wanted;
      if (cfg.scale <= 0.76) {
         while(pixels > 2 && (double)(h * pixels) > (double)window.getHeight() * 0.4) {
            --pixels;
         }
      }

      while(pixels > 1 && ((double)(w * pixels) > (double)window.getWidth() * 0.6 || (double)(h * pixels) > (double)window.getHeight() * 0.72)) {
         --pixels;
      }

      InfoHudConfig.Corner chosen = cfg.corner();
      HudNeighbours.collect(TAKEN, mc, sw, sh, millis);
      if (chosen != corner) {
         lastSpotY = -1;
         lastSpotX = -1;
      }

      int least = Math.min(pixels, Math.max(1, Mth.ceil(guiScale / (double)2.0F)));
      int gw = 0;
      int gh = 0;
      boolean free = false;

      for(int var22 = pixels; var22 >= least && !free; --var22) {
         unit = (float)((double)var22 / guiScale);
         gw = Mth.ceil((float)w * unit);
         gh = Mth.ceil((float)h * unit);
         free = HudPlacement.solve(chosen, gw, gh, sw, sh, TAKEN, lastSpotX, lastSpotY, SPOT);
      }

      if (!free && least < pixels) {
         unit = (float)((double)pixels / guiScale);
         gw = Mth.ceil((float)w * unit);
         gh = Mth.ceil((float)h * unit);
         HudPlacement.solve(chosen, gw, gh, sw, sh, TAKEN, lastSpotX, lastSpotY, SPOT);
      }

      float edgeX = chosen.right ? (float)(SPOT[0] + gw) : (float)SPOT[0];
      float edgeY = chosen.bottom ? (float)(SPOT[1] + gh) : (float)SPOT[1];
      if (changed || chosen.right != corner.right || chosen.bottom != corner.bottom) {
         EDGE_X.snap(edgeX);
         EDGE_Y.snap(edgeY);
      }

      corner = chosen;
      lastSpotX = SPOT[0];
      lastSpotY = SPOT[1];
      EDGE_X.to(edgeX);
      EDGE_Y.to(edgeY);
   }

   private static void paintGuarded() {
      try {
         InfoHudConfig cfg = config;
         if (!painting || cfg == null) {
            return;
         }

         if (InfoHudHarness.PROFILE) {
            InfoHudHarness.part(1);
         }

         HudLook.paint(cfg);
         if (InfoHudHarness.PROFILE) {
            InfoHudHarness.part(2);
         }
      } catch (RuntimeException e) {
         failed = true;
         CFHGuiMenu.LOGGER.error("[CFHGui Menu] drawing the info overlay failed; it is off for this session", e);
      }

   }

   static {
      TOGGLE_KEY = new KeyMapping("key.cfhgui_infohud.toggle", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.cfhgui");
      MODE_KEY = new KeyMapping("key.cfhgui_infohud.mode", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.cfhgui");
      seenRevision = -1;
      ALPHA = new Smooth();
      EDGE_X = new Smooth();
      EDGE_Y = new Smooth();
      RAM_BAR = new Smooth();
      RAM_GHOST = new Smooth();
      VRAM_BAR = new Smooth();
      TAKEN = new Rects();
      SPOT = new int[2];
      lastSpotX = -1;
      lastSpotY = -1;
      corner = InfoHudConfig.Corner.TOP_LEFT;
      unit = 1.0F;
      PAINT = InfoHud::paintGuarded;
      ALPHA.snap(0.0F);
   }
}
