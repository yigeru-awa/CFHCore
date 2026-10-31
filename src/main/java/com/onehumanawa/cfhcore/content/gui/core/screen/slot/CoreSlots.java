package com.onehumanawa.cfhcore.content.gui.core.screen.slot;

import com.onehumanawa.cfhcore.content.gui.core.client.CFHGuiHubScreen;
import com.onehumanawa.cfhcore.content.gui.core.client.setup.CFHGuiSetupScreen;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.BetterInventoryPalette;
import net.minecraft.network.chat.Component;

public final class CoreSlots {
   private static final String SCREENS = "net.minecraft.client.gui.screens.";
   public static final String TITLE = "minecraft:title";
   public static final String WORLDS = "minecraft:select_world";
   public static final String SERVERS = "minecraft:multiplayer";
   public static final String CREATE_WORLD = "minecraft:create_world";
   public static final String OPTIONS = "minecraft:options";
   public static final String PAUSE = "minecraft:pause";
   public static final String DEATH = "minecraft:death";
   public static final String CREATIVE = "minecraft:creative_inventory";
   public static final String DISCONNECTED = "minecraft:disconnected";
   public static final String LOADING = "cfhgui:loading";
   public static final String LEVEL_LOADING = "minecraft:level_loading";
   public static final String SCREENSHOTS = "cfhgui:screenshots";
   public static final String FRIENDS = "cfhgui_multiplayer:hub";
   public static final String PROFILE = "cfhgui_profile:profile";
   public static final String TEAMS = "cfhgui_teams:hub";
   public static final String QUESTS = "cfhgui_quests:book";
   public static final String ADVANCEMENTS = "minecraft:advancements";
   public static final String STATS = "minecraft:stats";
   public static final String SERVER_HUB = "cfhgui_server:hub";
   public static final String SERVER_ADMIN = "cfhgui_server:admin";
   public static final String SETUP = "cfhgui:setup";
   public static final String HUB = "cfhgui:hub";
   public static final String XAERO_MINIMAP = "xaero:minimap";
   public static final String XAERO_WORLD_MAP = "xaero:world_map";
   public static final String VOICECHAT = "voicechat:menu";
   public static final String JOURNEYMAP = "journeymap:fullscreen";
   public static final String JOURNEYMAP_MINIMAP = "journeymap:minimap";
   public static final String BETTER_INVENTORY = "betterinventory:inventory";
   public static final String DEBUG = "minecraft:debug";

   static void register() {
      MenuSlots.register(MenuSlot.vanilla("minecraft:title", name("title"), "cfhgui", "net.minecraft.client.gui.screens.TitleScreen"));
      MenuSlots.register(MenuSlot.vanilla("minecraft:select_world", name("select_world"), "cfhgui", "net.minecraft.client.gui.screens.worldselection.SelectWorldScreen"));
      MenuSlots.register(MenuSlot.vanilla("minecraft:multiplayer", name("multiplayer"), "cfhgui", "net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen"));
      MenuSlots.register(MenuSlot.vanilla("minecraft:create_world", name("create_world"), "cfhgui", "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen"));
      MenuSlots.register(MenuSlot.vanilla("minecraft:options", name("options"), "cfhgui_config", "net.minecraft.client.gui.screens.options.OptionsScreen"));
      MenuSlots.register(MenuSlot.vanilla("minecraft:pause", name("pause"), "cfhgui", "net.minecraft.client.gui.screens.PauseScreen"));
      MenuSlots.register(MenuSlot.vanilla("minecraft:death", name("death"), "cfhgui", "net.minecraft.client.gui.screens.DeathScreen"));
      MenuSlots.register(MenuSlot.vanilla("minecraft:creative_inventory", name("creative"), "cfhgui", "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen"));
      MenuSlots.register(MenuSlot.vanilla("minecraft:disconnected", name("disconnected"), "cfhgui", "net.minecraft.client.gui.screens.DisconnectedScreen"));
      MenuSlots.register(MenuSlot.vanilla("cfhgui:loading", name("loading"), "cfhgui"));
      MenuSlots.register(MenuSlot.vanilla("minecraft:level_loading", name("level_loading"), "cfhgui"));
      MenuSlots.register(MenuSlot.custom("cfhgui:screenshots", name("screenshots"), "cfhgui"));
      MenuSlots.register(MenuSlot.custom("cfhgui_multiplayer:hub", name("friends"), "cfhgui_multiplayer"));
      MenuSlots.register(MenuSlot.custom("cfhgui_profile:profile", name("profile"), "cfhgui_profile"));
      MenuSlots.register(MenuSlot.custom("cfhgui_teams:hub", name("teams"), "cfhgui_teams"));
      MenuSlots.register(MenuSlot.custom("cfhgui_quests:book", name("quests"), "cfhgui_quests"));
      MenuSlots.register(MenuSlot.vanilla("minecraft:advancements", name("advancements"), "cfhgui_quests", "net.minecraft.client.gui.screens.advancements.AdvancementsScreen"));
      MenuSlots.register(MenuSlot.vanilla("minecraft:stats", name("stats"), "cfhgui", "net.minecraft.client.gui.screens.achievement.StatsScreen"));
      MenuSlots.register(MenuSlot.custom("cfhgui_server:hub", name("server_hub"), "cfhgui_server"));
      MenuSlots.register(MenuSlot.custom("cfhgui_server:admin", name("server_admin"), "cfhgui_server"));
      MenuSlots.register(MenuSlot.custom("cfhgui:setup", name("setup"), "cfhgui"));
      MenuSlots.register(MenuSlot.custom("cfhgui:hub", name("hub"), "cfhgui"));
      if (CFHGuiPlatform.get().isModLoaded("cfhgui")) {
         MenuSlots.registerStyleOnly(MenuSlot.vanilla("minecraft:debug", name("debug"), "cfhgui"));
      }

      if (CFHGuiPlatform.get().isModLoaded("journeymap")) {
         MenuSlots.registerStyleOnly(MenuSlot.vanilla("journeymap:fullscreen", name("journeymap"), "cfhgui"));
         MenuSlots.registerStyleOnly(MenuSlot.vanilla("journeymap:minimap", name("journeymap_minimap"), "cfhgui"));
      }

      if (BetterInventoryPalette.installed()) {
         MenuSlots.registerStyleOnly(MenuSlot.vanilla("betterinventory:inventory", name("better_inventory"), "cfhgui"));
      }

      MenuSlots.provide("cfhgui:setup", Layout.CUSTOM, CFHGuiSetupScreen::new);
      MenuSlots.provide("cfhgui:hub", Layout.CUSTOM, CFHGuiHubScreen::new);
   }

   private static Component name(String key) {
      return Component.translatable("cfhgui.slot." + key);
   }

   private CoreSlots() {
   }
}
