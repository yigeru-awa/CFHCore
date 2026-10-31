package com.onehumanawa.cfhcore.content.gui.menu.client.infohud;

import com.sun.management.ThreadMXBean;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.config.CoreConfig;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Style;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.options.CFHGuiOptionsScreen;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

final class InfoHudHarness {
   static final boolean PROFILE = Boolean.getBoolean("cfhgui.infohud.profile");
   private static final boolean STATES = Boolean.getBoolean("cfhgui.infohud.harness") || System.getProperty("cfhgui.autoScreens") != null || Boolean.getBoolean("cfhgui.autoWorld");
   static int pretendPing = -1;
   static float pretendFill = -1.0F;
   private static final int EVERY_FRAMES = 600;
   private static @Nullable ThreadMXBean threads;
   static final int READ = 0;
   static final int OTHERS = 1;
   static final int WRITE = 2;
   static final int SEND = 3;
   private static final String[] PARTS = new String[]{"reading and layout", "others' pending drawing", "writing into the buffers", "sending to the GPU"};
   private static final long[] nanosOf = new long[4];
   private static final long[] bytesOf = new long[4];
   private static long markNanos;
   private static long markBytes;
   private static long nanosSum;
   private static long nanosWorst;
   private static long bytesSum;
   private static long frameNanos;
   private static int frames;
   private static int drawn;
   private static long driverNanos;
   private static int driverAsked;
   private static @Nullable Runnable later;
   private static int laterIn;

   static void driver(long nanos) {
      driverNanos += nanos;
      ++driverAsked;
   }

   private InfoHudHarness() {
   }

   static void init() {
      if (PROFILE) {
         try {
            java.lang.management.ThreadMXBean var1 = ManagementFactory.getThreadMXBean();
            if (var1 instanceof ThreadMXBean) {
               ThreadMXBean bean = (ThreadMXBean)var1;
               threads = bean;
            }
         } catch (Throwable var4) {
            threads = null;
         }

         CFHGuiMenu.LOGGER.info("[CFHGui Menu] info overlay: its cost is measured and written here every {} frames", 600);
      }

      if (STATES) {
         config("on", (c) -> c.enabled = true);
         config("off", (c) -> c.enabled = false);
         config("full", (c) -> c.mode = "FULL");
         config("compact", (c) -> c.mode = "COMPACT");
         config("corner_tl", (c) -> c.corner = "TOP_LEFT");
         config("corner_tr", (c) -> c.corner = "TOP_RIGHT");
         config("corner_bl", (c) -> c.corner = "BOTTOM_LEFT");
         config("corner_br", (c) -> c.corner = "BOTTOM_RIGHT");
         config("lines_default", (c) -> lines(c, new InfoHudConfig()));
         config("lines_all", (c) -> {
            c.position = c.chunk = c.facing = c.biome = c.dimension = c.light = c.time = c.speed = true;
            c.fps = c.graph = c.latency = c.memory = c.videoMemory = true;
         });
         config("lines_few", (c) -> {
            c.position = c.fps = c.memory = true;
            c.chunk = c.facing = c.biome = c.dimension = c.light = c.time = c.speed = c.graph = c.latency = c.videoMemory = false;
         });

         for(int v = 0; v <= 2; ++v) {
            int variant = v;
            config("variant_" + v, (c) -> HudLook.variant = variant);
         }

         for(int ms : new int[]{42, 180, 420}) {
            config("ping_" + ms, (c) -> pretendPing = ms);
         }

         config("ping_off", (c) -> pretendPing = -1);

         for(int percent : new int[]{40, 78, 88, 97}) {
            config("full_" + percent, (c) -> pretendFill = (float)percent / 100.0F);
         }

         config("full_off", (c) -> pretendFill = -1.0F);

         for(int percent : new int[]{50, 75, 100, 150, 200}) {
            config("size_" + percent, (c) -> c.scale = (double)percent / (double)100.0F);
         }

         for(int percent : new int[]{0, 5, 15, 50, 85, 100}) {
            config("opacity_" + percent, (c) -> c.opacity = percent);
         }

         state("infohud/style_slate", () -> global(Style.CFHGUI));
         state("infohud/style_vanilla", () -> global(Style.VANILLA));
         state("infohud/accent_default", () -> accent(-7726052));
         state("infohud/accent_sky", () -> accent(-10770208));

         for(int[] size : new int[][]{{1920, 1080}, {1280, 720}, {2683, 1321}, {854, 480}, {960, 900}}) {
            state("infohud/window_" + size[0] + "x" + size[1], () -> Minecraft.getInstance().getWindow().setWindowed(size[0], size[1]));
         }

         state("infohud/hud_hidden", () -> Minecraft.getInstance().options.hideGui = true);
         state("infohud/hud_shown", () -> Minecraft.getInstance().options.hideGui = false);
         state("infohud/subtitles_on", () -> Minecraft.getInstance().options.showSubtitles().set(true));
         state("infohud/subtitles_off", () -> Minecraft.getInstance().options.showSubtitles().set(false));
         state("infohud/unicode_on", () -> Minecraft.getInstance().options.forceUnicodeFont().set(true));
         state("infohud/unicode_off", () -> Minecraft.getInstance().options.forceUnicodeFont().set(false));

         for(String code : new String[]{"en_us", "zh_cn", "ja_jp"}) {
            state("infohud/language_" + code, () -> language(code));
         }

         for(int scale = 0; scale <= 5; ++scale) {
            int guiScale = scale;
            state("infohud/gui_" + (scale == 0 ? "auto" : Integer.toString(scale)), () -> {
               Minecraft mc = Minecraft.getInstance();
               mc.options.guiScale().set(guiScale);
               mc.resizeDisplay();
            });
         }

         state("infohud/cost", InfoHudHarness::report);
         CoreActions.SCREEN_FACTORIES.put("cfhgui:infohud/options", (parent) -> options(parent, 0));
         CoreActions.SCREEN_FACTORIES.put("cfhgui:infohud/options_up", (parent) -> options(parent, 1));
         if (Modules.isLoaded("cfhgui_config")) {
            CoreActions.SCREEN_FACTORIES.put("cfhgui:infohud/settings", (parent) -> InfoHudSettingsTab.screen(parent));
         }

         CFHGuiEvents.CLIENT_TICK_END.register(InfoHudHarness::tick);
         state("debug/on", () -> debug(true));
         state("debug/off", () -> debug(false));
         state("debug/charts_fps", () -> charts(true, false));
         state("debug/charts_net", () -> charts(false, true));
         state("debug/charts_off", () -> charts(false, false));
         state("debug/style_slate", () -> debugStyle(Style.CFHGUI));
         state("debug/style_vanilla", () -> debugStyle(Style.VANILLA));
         state("debug/style_follow", () -> debugStyle((Style)null));
         CoreActions.SCREEN_FACTORIES.put("cfhgui:debug/menus", InfoHudHarness::menus);
         CFHGuiMenu.LOGGER.info("[CFHGui Menu] the info overlay's and the debug screen's states are registered for the harness");
      }
   }

   private static void tick() {
      if (later != null && --laterIn <= 0) {
         Runnable run = later;
         later = null;

         try {
            run.run();
         } catch (RuntimeException e) {
            CFHGuiMenu.LOGGER.error("[CFHGui Menu] a harness state's second step failed", e);
         }

      }
   }

   private static Screen menus(@Nullable Screen parent) {
      Function<Screen, Screen> table = (Function)CoreActions.SCREEN_FACTORIES.get("cfhgui_config:hub/interface/menus");
      if (table == null) {
         return parent;
      } else {
         Screen screen = (Screen)table.apply(parent);
         later = () -> scrollToEnd(screen.children());
         laterIn = 4;
         return screen;
      }
   }

   private static void scrollToEnd(List<? extends GuiEventListener> children) {
      for(GuiEventListener child : children) {
         if (child instanceof CFHGuiScrollPanel panel) {
            panel.snapScroll((double)panel.maxScroll());
         }

         if (child instanceof ContainerEventHandler container) {
            scrollToEnd(container.children());
         }
      }

   }

   private static Screen options(@Nullable Screen parent, int pagesUp) {
      CFHGuiOptionsScreen screen = new CFHGuiOptionsScreen(parent);
      later = () -> {
         screen.showPage("menu");

         for(GuiEventListener child : screen.children()) {
            if (child instanceof CFHGuiScrollPanel panel) {
               panel.snapScroll((double)(panel.maxScroll() - pagesUp * (panel.getHeight() - 20)));
            }
         }

      };
      laterIn = 3;
      return screen;
   }

   private static void config(String id, Consumer<InfoHudConfig> change) {
      state("infohud/" + id, () -> {
         change.accept(InfoHudConfig.get());
         InfoHudConfig.touch();
      });
   }

   private static void lines(InfoHudConfig c, InfoHudConfig from) {
      c.position = from.position;
      c.chunk = from.chunk;
      c.facing = from.facing;
      c.biome = from.biome;
      c.dimension = from.dimension;
      c.light = from.light;
      c.time = from.time;
      c.speed = from.speed;
      c.fps = from.fps;
      c.graph = from.graph;
      c.latency = from.latency;
      c.memory = from.memory;
      c.videoMemory = from.videoMemory;
   }

   private static void state(String id, Runnable set) {
      CoreActions.SCREEN_FACTORIES.put("cfhgui:" + id, (parent) -> {
         try {
            set.run();
         } catch (RuntimeException e) {
            CFHGuiMenu.LOGGER.error("[CFHGui Menu] harness state {} failed", id, e);
         }

         return parent;
      });
   }

   private static void global(Style style) {
      CFHGui.config().setStyle(style);
      MenuSlots.refresh();
      Reskin.invalidate();
   }

   private static void accent(int argb) {
      CFHGui.config().accent = Colors.toHex(argb);
      Theme.reload();
   }

   private static void language(String code) {
      Minecraft mc = Minecraft.getInstance();
      mc.getLanguageManager().setSelected(code);
      mc.options.languageCode = code;
      mc.reloadResourcePacks();
   }

   private static void debug(boolean on) {
      DebugScreenOverlay overlay = Minecraft.getInstance().getDebugOverlay();
      if (overlay.showDebugScreen() != on) {
         overlay.toggleOverlay();
      }

   }

   private static void charts(boolean frames, boolean network) {
      DebugScreenOverlay overlay = Minecraft.getInstance().getDebugOverlay();
      if (!overlay.showDebugScreen()) {
         overlay.toggleOverlay();
      }

      if (overlay.showFpsCharts() && !frames) {
         overlay.toggleFpsCharts();
      }

      if (overlay.showNetworkCharts() && !network) {
         overlay.toggleNetworkCharts();
      }

      if (frames && !overlay.showFpsCharts()) {
         overlay.toggleFpsCharts();
      }

      if (network && !overlay.showNetworkCharts()) {
         overlay.toggleNetworkCharts();
      }

   }

   private static void debugStyle(@Nullable Style style) {
      CoreConfig config = CFHGui.config();
      if (style == null) {
         if (config.screens != null) {
            config.screens.remove("minecraft:debug");
         }
      } else {
         config.overrideOrCreate("minecraft:debug").style = style.name();
      }

      MenuSlots.refresh();
   }

   private static long allocated() {
      ThreadMXBean bean = threads;
      return bean == null ? 0L : bean.getCurrentThreadAllocatedBytes();
   }

   static long began() {
      markBytes = allocated();
      markNanos = System.nanoTime();
      frameNanos = 0L;
      return markNanos;
   }

   static void part(int which) {
      long nanos = System.nanoTime();
      long bytes = allocated();
      long[] var10000 = nanosOf;
      var10000[which] += nanos - markNanos;
      var10000 = bytesOf;
      var10000[which] += bytes - markBytes;
      frameNanos += nanos - markNanos;
      bytesSum += bytes - markBytes;
      if (which == 3) {
         ++drawn;
      }

      markNanos = System.nanoTime();
      markBytes = allocated();
   }

   static void ended(long began) {
      part(0);
      nanosSum += frameNanos;
      if (frameNanos > nanosWorst) {
         nanosWorst = frameNanos;
      }

      if (++frames >= 600) {
         report();
      }

   }

   private static void report() {
      if (frames != 0) {
         StringBuilder parts = new StringBuilder();

         for(int i = 0; i < PARTS.length; ++i) {
            parts.append(i == 0 ? "" : "; ").append(PARTS[i]).append(' ').append(String.format(Locale.ROOT, "%.1f us", (double)nanosOf[i] / (double)1000.0F / (double)frames)).append(threads == null ? "" : ", " + bytesOf[i] / (long)frames + " B");
            nanosOf[i] = 0L;
            bytesOf[i] = 0L;
         }

         if (driverAsked > 0) {
            parts.append("; of the reading, the graphics driver asked ").append(driverAsked).append(" times, ").append(String.format(Locale.ROOT, "%.0f us", (double)driverNanos / (double)1000.0F / (double)driverAsked)).append(" each");
         }

         CFHGuiMenu.LOGGER.info("[CFHGui Menu] info overlay cost over {} frames ({} drawn, {}): {} us a frame on the render thread (worst {} us), {} bytes allocated a frame [{}]", new Object[]{frames, drawn, BakedText.working() ? "kept glyphs" : "the font's own drawing", String.format(Locale.ROOT, "%.1f", (double)nanosSum / (double)1000.0F / (double)frames), String.format(Locale.ROOT, "%.0f", (double)nanosWorst / (double)1000.0F), threads == null ? "?" : Long.toString(bytesSum / (long)frames), parts});
         driverAsked = 0;
         drawn = 0;
         frames = 0;
         driverNanos = 0L;
         bytesSum = 0L;
         nanosWorst = 0L;
         nanosSum = 0L;
      }
   }
}
