package com.onehumanawa.cfhcore.content.gui.config;

import com.onehumanawa.cfhcore.content.gui.config.curated.CuratedPages;
import com.onehumanawa.cfhcore.content.gui.config.editor.FileEditorScreen;
import com.onehumanawa.cfhcore.content.gui.config.hub.ConfigHubScreen;
import com.onehumanawa.cfhcore.content.gui.config.resolver.Resolvers;
import com.onehumanawa.cfhcore.content.gui.config.ui.ApplyQueue;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType.Arg;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenSwaps;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.ChatOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OnlineOptionsScreen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CFHGuiConfig implements CFHGuiModule {
   public static final String MOD_ID = "cfhgui_config";
   public static final Logger LOGGER = LoggerFactory.getLogger("CFHGui Config");
   public static final CFHGuiConfig MODULE = new CFHGuiConfig();
   private static final List<String> PAGE_IDS = List.of("general", "gameplay", "gameplay/general", "video", "controls", "controls/keys", "audio", "multiplayer", "multiplayer/online", "multiplayer/chat", "multiplayer/skin", "interface", "interface/menus", "language_accessibility", "language_accessibility/language", "language_accessibility/accessibility", "customization", "customization/mods", "customization/packs", "customization/shaders", "advanced", "advanced/video", "advanced/controls", "advanced/multiplayer", "advanced/interface", "advanced/favourites", "advanced/presets", "advanced/files", "developer");

   private CFHGuiConfig() {
   }

   public static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath("cfhgui_config", path);
   }

   public String id() {
      return "cfhgui_config";
   }

   public Component displayName() {
      return Component.translatable("cfhgui_config.name");
   }

   public Icon icon() {
      return Icon.SLIDERS;
   }

   public void init() {
      CFHGui.init();
      LOGGER.info("[CFHGui Config] init");
   }

   public void initClient() {
      ConfigSettings.file();
      Resolvers.registerAll();
      CuratedPages.bootstrap();
      ScreenIds.register(ConfigHubScreen.class, "cfhgui_config:hub", "CFHGui settings");
      ScreenIds.register(FileEditorScreen.class, "cfhgui_config:editor", "Config file editor");
      CoreActions.SCREEN_FACTORIES.put("cfhgui_config:hub", (p) -> ConfigHubScreen.forLayout(p, (String)null));
      MenuSlots.provide("minecraft:options", Layout.CUSTOM, 10, (s) -> new ConfigHubScreen(Minecraft.getInstance().screen, (String)null, ConfigHubScreen.Presentation.SIDEBAR));
      MenuSlots.provide("minecraft:options", Layout.OVERHAUL, 10, (s) -> new ConfigHubScreen(Minecraft.getInstance().screen, (String)null, ConfigHubScreen.Presentation.OVERHAUL));

      for(String path : PAGE_IDS) {
         CoreActions.SCREEN_FACTORIES.put("cfhgui_config:hub/" + path, (p) -> ConfigHubScreen.forLayout(p, path));
      }

      CFHGuiEvents.CLIENT_TICK_END.register(ApplyQueue::tick);
      installSwaps();
      LOGGER.info("[CFHGui Config] client init ({} curated page(s))", CuratedPages.load().size());
      if (System.getenv("CFH_CONFIG_SMOKE") != null || String.valueOf(System.getProperty("cfhgui.autoScreens")).contains("cfhgui_config:smoke")) {
         SmokeTest.install();
      }

   }

   private static void installSwaps() {
      if (ConfigSettings.get().swapVanillaScreens) {
         swap(VideoSettingsScreen.class, "video");
         swap(SoundOptionsScreen.class, "audio");
         swap(ControlsScreen.class, "controls");
         swap(KeyBindsScreen.class, "controls/keys");
         swap(ChatOptionsScreen.class, "multiplayer/chat");
         swap(LanguageSelectScreen.class, "language_accessibility/language");
         swap(AccessibilityOptionsScreen.class, "language_accessibility/accessibility");
         swap(OnlineOptionsScreen.class, "multiplayer/online");

      }
   }

   public static boolean redirecting() {
      return MenuSlots.effective("minecraft:options") != Layout.VANILLA;
   }

   private static void swap(Class<? extends Screen> cls, String page) {
      ScreenSwaps.register(cls, (original) -> redirecting() ? ConfigHubScreen.forLayout(Minecraft.getInstance().screen, page) : null);
   }

   public List<CFHGuiModule.HubEntry> hubEntries() {
      return List.of(new CFHGuiModule.HubEntry(Component.translatable("cfhgui_config.hub.open"), Icon.SLIDERS, () -> CFHGuiConfigApi.openHub(Minecraft.getInstance().screen, (String)null)));
   }

   public List<ActionType> actions() {
      return List.of(new ActionType("cfhgui_config:open", Component.translatable("cfhgui_config.action.open"), List.of(Arg.choice("page", Component.translatable("cfhgui_config.action.arg.page"), "video", PAGE_IDS)), (a) -> CFHGuiConfigApi.openHub(Minecraft.getInstance().screen, (String)a.getOrDefault("page", "video"))));
   }
}
