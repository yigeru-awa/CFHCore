package com.onehumanawa.cfhcore.content.gui.menu.client;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutBackground;
import com.onehumanawa.cfhcore.content.gui.core.layout.Placeholders;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop.Kind;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.menu.MenuConfig;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.creative.CreativeHarness;
import com.onehumanawa.cfhcore.content.gui.menu.client.creative.CFHGuiCreativeScreen;
import com.onehumanawa.cfhcore.content.gui.menu.client.death.CFHGuiDeathScreen;
import com.onehumanawa.cfhcore.content.gui.menu.client.debug.DebugRestyle;
import com.onehumanawa.cfhcore.content.gui.menu.client.disconnect.CFHGuiDisconnectedScreen;
import com.onehumanawa.cfhcore.content.gui.menu.client.infohud.InfoHud;
import com.onehumanawa.cfhcore.content.gui.menu.client.loading.LoadingPreviews;
import com.onehumanawa.cfhcore.content.gui.menu.client.loading.journey.Backdrop;
import com.onehumanawa.cfhcore.content.gui.menu.client.loading.journey.Journey;
import com.onehumanawa.cfhcore.content.gui.menu.client.lookat.LookAt;
import com.onehumanawa.cfhcore.content.gui.menu.client.options.CFHGuiOptionsScreen;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.OverhaulMenus;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.death.OverhaulDeathScreen;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.TitleBackdrop;
import com.onehumanawa.cfhcore.content.gui.menu.client.pause.PauseHarness;
import com.onehumanawa.cfhcore.content.gui.menu.client.pause.CFHGuiPauseScreen;
import com.onehumanawa.cfhcore.content.gui.menu.client.screenshots.ScreenshotViewer;
import com.onehumanawa.cfhcore.content.gui.menu.client.screenshots.Screenshots;
import com.onehumanawa.cfhcore.content.gui.menu.client.screenshots.CFHGuiScreenshotsScreen;
import com.onehumanawa.cfhcore.content.gui.menu.client.servers.CFHGuiServersScreen;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.StatsScreens;
import com.onehumanawa.cfhcore.content.gui.menu.client.tablist.TabListPreview;
import com.onehumanawa.cfhcore.content.gui.menu.client.title.CFHGuiTitleScreen;
import com.onehumanawa.cfhcore.content.gui.menu.client.worlds.CFHGuiWorldsScreen;
import com.onehumanawa.cfhcore.content.gui.menu.mixin.DisconnectedScreenAccessor;
import com.onehumanawa.cfhcore.content.gui.menu.mixin.JoinMultiplayerScreenAccessor;
import com.onehumanawa.cfhcore.content.gui.menu.mixin.OptionsScreenAccessor;
import com.onehumanawa.cfhcore.content.gui.menu.mixin.SelectWorldScreenAccessor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;

public final class MenuClient {
   private static boolean initialised;
   private static long screenshotCountMs;
   private static int screenshotCount = -1;

   public static synchronized void init() {
      if (!initialised) {
         initialised = true;
         ScreenIds.register(CFHGuiTitleScreen.class, "cfhgui:title", "CFHGui title screen");
         ScreenIds.register(CFHGuiWorldsScreen.class, "cfhgui:worlds", "CFHGui worlds");
         ScreenIds.register(CFHGuiServersScreen.class, "cfhgui:servers", "CFHGui servers");
         ScreenIds.register(CFHGuiPauseScreen.class, "cfhgui:pause", "CFHGui pause menu");
         ScreenIds.register(CFHGuiOptionsScreen.class, "cfhgui:options", "CFHGui options");
         ScreenIds.register(CFHGuiScreenshotsScreen.class, "cfhgui:screenshots", "Screenshots");
         ScreenIds.register(ScreenshotViewer.class, "cfhgui:screenshot_viewer", "Screenshot viewer");
         ScreenIds.register(CFHGuiDisconnectedScreen.class, "cfhgui:disconnected", "CFHGui disconnected");
         ScreenIds.register(CFHGuiDeathScreen.class, "cfhgui:death", "CFHGui respawn screen");
         ScreenIds.register(CFHGuiCreativeScreen.class, "cfhgui:creative", "CFHGui creative inventory");
         MenuSlots.provide("minecraft:title", Layout.CUSTOM, (s) -> Minecraft.getInstance().isDemo() ? null : new CFHGuiTitleScreen());
         MenuSlots.provide("minecraft:select_world", Layout.CUSTOM, (s) -> new CFHGuiWorldsScreen(((SelectWorldScreenAccessor)s).cfhgui$lastScreen()));
         MenuSlots.provide("minecraft:multiplayer", Layout.CUSTOM, (s) -> new CFHGuiServersScreen(((JoinMultiplayerScreenAccessor)s).cfhgui$lastScreen()));
         MenuSlots.provide("minecraft:pause", Layout.CUSTOM, (s) -> ((PauseScreen)s).showsPauseMenu() ? new CFHGuiPauseScreen() : null);
         MenuSlots.provide("minecraft:death", Layout.CUSTOM, CFHGuiDeathScreen::replacing);
         MenuSlots.provide("minecraft:creative_inventory", Layout.CUSTOM, CFHGuiCreativeScreen::replacing);
         MenuSlots.provide("minecraft:options", Layout.CUSTOM, -10, (s) -> new CFHGuiOptionsScreen(((OptionsScreenAccessor)s).cfhgui$lastScreen()));
         MenuSlots.provide("minecraft:disconnected", Layout.CUSTOM, (s) -> {
            DisconnectedScreenAccessor acc = (DisconnectedScreenAccessor)s;
            return new CFHGuiDisconnectedScreen(acc.cfhgui$parent(), s.getTitle(), acc.cfhgui$details());
         });
         MenuSlots.provide("cfhgui:screenshots", Layout.CUSTOM, CFHGuiScreenshotsScreen::new);
         StatsScreens.register();
         OverhaulMenus.register();
         refreshLoadingSupport();
         migrateScreenFlags();
         CoreActions.SCREEN_FACTORIES.put("cfhgui:title", (p) -> new CFHGuiTitleScreen());
         CoreActions.SCREEN_FACTORIES.put("cfhgui:worlds", CFHGuiWorldsScreen::new);
         CoreActions.SCREEN_FACTORIES.put("cfhgui:servers", CFHGuiServersScreen::new);
         CoreActions.SCREEN_FACTORIES.put("cfhgui:options", CFHGuiOptionsScreen::new);
         CoreActions.SCREEN_FACTORIES.put("cfhgui:screenshots", CFHGuiScreenshotsScreen::new);
         CoreActions.SCREEN_FACTORIES.put("cfhgui:death", (p) -> CFHGuiDeathScreen.preview(false));
         CoreActions.SCREEN_FACTORIES.put("cfhgui:death_hardcore", (p) -> MenuSlots.effective("minecraft:death") == Layout.OVERHAUL ? OverhaulDeathScreen.preview(true) : CFHGuiDeathScreen.preview(true));
         if (System.getProperty("cfhgui.autoScreens") != null) {
            LoadingPreviews.register();
         }

         if (System.getProperty("cfhgui.autoScreens") != null || Boolean.getBoolean("cfhgui.autoWorld")) {
            PauseHarness.register();
            CreativeHarness.register();
         }

         TabListPreview.register();
         InfoHud.init();
         DebugRestyle.init();
         LookAt.init();
         MenuElements.registerAll();
         Placeholders.register("last_world", () -> (String)LastPlayed.quick().map((t) -> t.name() != null && !t.name().isBlank() ? t.name() : t.id()).orElse(""));
         Placeholders.register("last_server", () -> {
            MenuConfig c = cfg();
            return c.lastServerName != null && !c.lastServerName.isBlank() ? c.lastServerName : c.lastServerAddress;
         });
         Placeholders.register("last_played", () -> (String)LastPlayed.quick().map((t) -> Fmt.ago(t.at()).getString()).orElse(""));
         Placeholders.register("session_time", () -> Fmt.duration(LastPlayed.sessionMs()));
         Placeholders.register("screenshots", () -> Integer.toString(screenshotCount()));
         CFHGuiEvents.CLIENT_JOINED_SERVER.register(LastPlayed::recordJoin);
         CFHGuiEvents.CLIENT_LEFT_SERVER.register(LastPlayed::clearSession);
         CFHGuiEvents.CLIENT_TICK_END.register(Journey::tick);
         Backdrop.register();
         TitleBackdrop.register();
         MenuBackdrop.painter((kind, g, width, height, partialTick) -> kind == Kind.TITLE ? LayoutBackground.paint(titleLayoutId(), g, width, height, partialTick, (PanoramaRenderer)null) || TitleBackdrop.paint(g, width, height, partialTick) : Backdrop.paint(g, width, height, partialTick));
      }
   }

   public static String titleLayoutId() {
      String var10000;
      switch (MenuSlots.effective("minecraft:title")) {
         case OVERHAUL -> var10000 = "cfhgui:title_overhaul";
         case CUSTOM -> var10000 = "cfhgui:title";
         default -> var10000 = "minecraft:title";
      }

      return var10000;
   }

   public static MenuConfig cfg() {
      return CFHGuiMenu.config();
   }

   /** @deprecated */
   @Deprecated
   public static boolean custom() {
      return MenuSlots.globalLayout() != Layout.VANILLA;
   }

   public static void refreshLoadingSupport() {
      boolean on = cfg().loadingScreens;
      MenuSlots.support("minecraft:level_loading", Layout.CUSTOM, on);
      MenuSlots.support("cfhgui:loading", Layout.CUSTOM, on);
      MenuSlots.support("cfhgui:loading", Layout.OVERHAUL, on);
      MenuSlots.support("minecraft:level_loading", Layout.OVERHAUL, on);
   }

   private static void migrateScreenFlags() {
      MenuConfig c = cfg();
      if (!c.layoutFlagsMigrated) {
         Map<String, Boolean> flags = new LinkedHashMap();
         flags.put("minecraft:title", c.titleScreen);
         flags.put("minecraft:select_world", c.worldsScreen);
         flags.put("minecraft:multiplayer", c.serversScreen);
         flags.put("minecraft:pause", c.pauseScreen);
         flags.put("minecraft:options", c.optionsScreen);
         flags.put("minecraft:disconnected", c.disconnectedScreen);
         List<String> vanilla = flags.entrySet().stream().filter((e) -> !(Boolean)e.getValue()).map(Map.Entry::getKey).toList();
         if (!vanilla.isEmpty()) {
            CFHGui.configFile().update((core) -> {
               for(String slot : vanilla) {
                  core.overrideOrCreate(slot).layout = Layout.VANILLA.name();
               }

            });
            MenuSlots.refresh();
            CFHGuiMenu.LOGGER.info("[CFHGui Menu] moved {} per-screen flag(s) into Core's menu overrides", vanilla.size());
         }

         CFHGuiMenu.configFile().update((m) -> m.layoutFlagsMigrated = true);
      }
   }

   private static int screenshotCount() {
      long now = System.currentTimeMillis();
      if (screenshotCount >= 0 && now - screenshotCountMs < 5000L) {
         return screenshotCount;
      } else {
         screenshotCountMs = now;
         int n = 0;
         Path dir = Screenshots.dir();
         if (Files.isDirectory(dir, new LinkOption[0])) {
            try {
               Stream<Path> s = Files.list(dir);

               try {
                  n = (int)s.filter(Screenshots::isImage).count();
               } catch (Throwable var8) {
                  if (s != null) {
                     try {
                        s.close();
                     } catch (Throwable var7) {
                        var8.addSuppressed(var7);
                     }
                  }

                  throw var8;
               }

               if (s != null) {
                  s.close();
               }
            } catch (Exception var9) {
            }
         }

         screenshotCount = n;
         return n;
      }
   }

   public static List<CFHGuiModule.HubEntry> hubEntries() {
      Minecraft mc = Minecraft.getInstance();
      return List.of(new CFHGuiModule.HubEntry(Component.translatable("cfhgui.screenshots.title"), Icon.CAMERA, () -> mc.setScreen(new CFHGuiScreenshotsScreen(mc.screen))), new CFHGuiModule.HubEntry(Component.translatable("menu.singleplayer"), Icon.SINGLEPLAYER, () -> mc.setScreen(new SelectWorldScreen(mc.screen))), new CFHGuiModule.HubEntry(Component.translatable("menu.multiplayer"), Icon.MULTIPLAYER, () -> mc.setScreen(new JoinMultiplayerScreen(mc.screen))), new CFHGuiModule.HubEntry(Component.translatable("menu.options"), Icon.SETTINGS, () -> mc.setScreen(new OptionsScreen(mc.screen, mc.options))));
   }

   private MenuClient() {
   }
}
