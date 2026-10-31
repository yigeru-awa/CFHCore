package com.onehumanawa.cfhcore.content.gui.core.screen;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.jetbrains.annotations.Nullable;

public final class ScreenIds {
   private static final Map<String, String> BY_CLASS = new ConcurrentHashMap();
   private static final Map<String, String> DISPLAY = new ConcurrentHashMap();

   private static void vanilla(String cls, String id, String display) {
      BY_CLASS.put("net.minecraft.client.gui.screens." + cls, "minecraft:" + id);
      DISPLAY.put("minecraft:" + id, display);
   }

   public static void register(Class<? extends Screen> cls, String id, String display) {
      BY_CLASS.put(cls.getName(), id);
      DISPLAY.put(id, display);
   }

   public static String of(@Nullable Screen screen) {
      return screen == null ? "none" : of(screen.getClass());
   }

   public static String of(Class<?> cls) {
      String known = (String)BY_CLASS.get(cls.getName());
      if (known != null) {
         return known;
      } else {
         String var10000 = guessNamespace(cls.getName());
         return var10000 + ":" + cls.getSimpleName();
      }
   }

   public static String display(String id) {
      String d = (String)DISPLAY.get(id);
      if (d != null) {
         return d;
      } else {
         int i = id.indexOf(58);
         return i < 0 ? id : id.substring(i + 1);
      }
   }

   public static boolean isVanilla(Screen screen) {
      String n = screen.getClass().getName();
      return n.startsWith("net.minecraft.") || n.startsWith("com.mojang.");
   }

   public static boolean isCFHGui(Screen screen) {
      return screen.getClass().getName().startsWith("com.onehumanawa.cfhcore.content.gui.");
   }

   public static boolean isContainer(@Nullable Screen screen) {
      return screen instanceof AbstractContainerScreen;
   }

   public static String guessNamespace(String className) {
      String[] parts = className.split("\\.");
      if (parts.length < 2) {
         return "unknown";
      } else {
         int i = 0;
         if (parts[0].matches("com|net|dev|me|io|org|de|fr|uk|eu|xyz|cc|tv|gg") && parts.length > 2) {
            i = 2;
         } else {
            i = 1;
         }

         String ns = parts[Math.min(i, parts.length - 2)].toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
         return ns.isEmpty() ? "unknown" : ns;
      }
   }

   public static List<String> known() {
      return DISPLAY.keySet().stream().sorted().toList();
   }

   private ScreenIds() {
   }

   static {
      vanilla("TitleScreen", "title", "Title screen");
      vanilla("worldselection.SelectWorldScreen", "select_world", "Select world");
      vanilla("worldselection.CreateWorldScreen", "create_world", "Create world");
      vanilla("worldselection.EditWorldScreen", "edit_world", "Edit world");
      vanilla("worldselection.OptimizeWorldScreen", "optimize_world", "Optimize world");
      vanilla("multiplayer.JoinMultiplayerScreen", "multiplayer", "Multiplayer");
      vanilla("multiplayer.EditServerScreen", "edit_server", "Edit server");
      vanilla("multiplayer.DirectJoinServerScreen", "direct_connect", "Direct connect");
      vanilla("multiplayer.ServerSelectionList", "server_list", "Server list");
      vanilla("ConnectScreen", "connect", "Connecting");
      vanilla("DisconnectedScreen", "disconnected", "Disconnected");
      vanilla("PauseScreen", "pause", "Pause menu");
      vanilla("DeathScreen", "death", "Death screen");
      vanilla("ChatScreen", "chat", "Chat");
      vanilla("options.OptionsScreen", "options", "Options");
      vanilla("options.VideoSettingsScreen", "video_settings", "Video settings");
      vanilla("options.SoundOptionsScreen", "sound_options", "Music & sounds");
      vanilla("options.controls.ControlsScreen", "controls", "Controls");
      vanilla("options.controls.KeyBindsScreen", "key_binds", "Key binds");
      vanilla("options.MouseSettingsScreen", "mouse_settings", "Mouse settings");
      vanilla("options.ChatOptionsScreen", "chat_options", "Chat settings");
      vanilla("options.SkinCustomizationScreen", "skin_customization", "Skin customization");
      vanilla("options.LanguageSelectScreen", "language", "Language");
      vanilla("options.AccessibilityOptionsScreen", "accessibility", "Accessibility");
      vanilla("options.OnlineOptionsScreen", "online_options", "Online options");
      vanilla("options.TelemetryInfoScreen", "telemetry", "Telemetry");
      vanilla("options.FontOptionsScreen", "font_options", "Font settings");
      vanilla("options.CreditsAndAttributionScreen", "credits", "Credits");
      vanilla("packs.PackSelectionScreen", "packs", "Resource packs");
      vanilla("ShareToLanScreen", "share_to_lan", "Open to LAN");
      vanilla("achievement.StatsScreen", "stats", "Statistics");
      vanilla("advancements.AdvancementsScreen", "advancements", "Advancements");
      vanilla("LevelLoadingScreen", "level_loading", "Loading world");
      vanilla("ReceivingLevelScreen", "receiving_level", "Loading terrain");
      vanilla("GenericMessageScreen", "generic_message", "Message");
      vanilla("ConfirmScreen", "confirm", "Confirm");
      vanilla("AlertScreen", "alert", "Alert");
      vanilla("ConfirmLinkScreen", "confirm_link", "Open link");
      vanilla("BackupConfirmScreen", "backup_confirm", "Backup");
      vanilla("ProgressScreen", "progress", "Progress");
      vanilla("OutOfMemoryScreen", "out_of_memory", "Out of memory");
      vanilla("social.SocialInteractionsScreen", "social_interactions", "Social interactions");
      vanilla("reporting.ReportPlayerScreen", "report_player", "Report player");
      vanilla("WinScreen", "win", "End credits");
      vanilla("InBedChatScreen", "in_bed_chat", "Sleeping");
      vanilla("DemoIntroScreen", "demo_intro", "Demo");
      vanilla("AccessibilityOnboardingScreen", "accessibility_onboarding", "Accessibility onboarding");
      vanilla("Realms32bitWarningScreen", "realms_warning", "Realms warning");
      BY_CLASS.put("com.mojang.realmsclient.RealmsMainScreen", "minecraft:realms");
      DISPLAY.put("minecraft:realms", "Realms");
   }
}
