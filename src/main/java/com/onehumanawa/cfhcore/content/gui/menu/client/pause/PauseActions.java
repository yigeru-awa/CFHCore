package com.onehumanawa.cfhcore.content.gui.menu.client.pause;

import com.mojang.realmsclient.RealmsMainScreen;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.module.Features;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.Fmt;
import com.onehumanawa.cfhcore.content.gui.menu.client.LastPlayed;
import com.onehumanawa.cfhcore.content.gui.menu.client.ModsScreenOpener;
import com.onehumanawa.cfhcore.content.gui.menu.client.screenshots.CFHGuiScreenshotsScreen;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerLinksScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerLinks;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Nullable;

public final class PauseActions {
   private static boolean disconnecting;

   public static List<Action> actions(Screen self, boolean keepLocked) {
      Minecraft mc = Minecraft.getInstance();
      List<Action> out = new ArrayList();
      boolean sp = mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null;
      out.add(new Action(Icon.PLAY, Component.translatable("menu.returnToGame"), () -> mc.setScreen((Screen)null), PauseActions.Kind.PRIMARY, true, (Component)null, (String)null));
      out.add(plain(Icon.TROPHY, Component.translatable("gui.advancements"), () -> {
         if (mc.player != null) {
            mc.setScreen(new AdvancementsScreen(mc.player.connection.getAdvancements(), self));
         }

      }));
      out.add(plain(Icon.HISTORY, Component.translatable("gui.stats"), () -> {
         if (mc.player != null) {
            mc.setScreen(new StatsScreen(self, mc.player.getStats()));
         }

      }));
      out.add(plain(Icon.SETTINGS, Component.translatable("menu.options"), () -> mc.setScreen(new OptionsScreen(self, mc.options))));
      ServerLinks links = mc.player != null ? mc.player.connection.serverLinks() : ServerLinks.EMPTY;
      if (sp) {
         boolean open = mc.getSingleplayerServer().isPublished();
         out.add(new Action(Icon.LAN, Component.translatable("menu.shareToLan"), () -> mc.setScreen(new ShareToLanScreen(self)), PauseActions.Kind.NORMAL, !open, open ? Component.translatable("cfhgui.pause.lan_open") : null, (String)null));
      } else if (!links.isEmpty()) {
         out.add(plain(Icon.LINK, Component.translatable("menu.server_links"), () -> mc.setScreen(new ServerLinksScreen(self, links))));
      } else {
         out.add(plain(Icon.SHIELD, Component.translatable("menu.playerReporting"), () -> mc.setScreen(new SocialInteractionsScreen(self))));
      }

      out.add(plain(Icon.CAMERA, Component.translatable("cfhgui.screenshots.title"), () -> mc.setScreen(new CFHGuiScreenshotsScreen(self))));

      if (Features.present("cfhgui_server") && !MenuSlots.available("cfhgui_server:hub").isEmpty()) {
         out.add(plain(Icon.SERVER, Component.translatable("cfhgui.slot.server_hub"), () -> MenuSlots.open("cfhgui_server:hub", self)));
      }

      if (CFHGuiMenu.config().showModsButton) {
         Optional<Function<Screen, Screen>> mods = ModsScreenOpener.factory();
         mods.ifPresent((f) -> out.add(plain(Icon.MODS, Component.translatable("cfhgui.title.mods"), () -> mc.setScreen((Screen)f.apply(self)))));
      }

      out.add(new Action(Icon.EXIT, Component.translatable(sp ? "menu.returnToMenu" : "menu.disconnect"), PauseActions::confirmDisconnect, PauseActions.Kind.DANGER, true, (Component)null, (String)null));
      return out;
   }

   private static Action plain(Icon icon, Component label, Runnable run) {
      return new Action(icon, label, run, PauseActions.Kind.NORMAL, true, (Component)null, (String)null);
   }

   public static void confirmDisconnect() {
      boolean sp = Minecraft.getInstance().hasSingleplayerServer();
      if (!CFHGuiMenu.config().confirmQuit) {
         disconnect();
      } else {
         CFHGuiModal.confirm(Component.translatable(sp ? "menu.returnToMenu" : "menu.disconnect"), leaveQuestion(sp), Component.translatable(sp ? "menu.returnToMenu" : "menu.disconnect"), () -> afterClick(PauseActions::leaveAsked));
      }
   }

   static Component leaveQuestion(boolean sp) {
      return Component.translatable(sp ? "cfhgui.pause.confirm_quit_sp" : "cfhgui.pause.confirm_quit_mp");
   }

   private static void leaveAsked() {
      if (Minecraft.getInstance().level != null) {
         disconnect();
      }

   }

   public static void afterClick(Runnable leave) {
      Minecraft.getInstance().tell(leave);
   }

   public static void disconnect() {
      if (!disconnecting) {
         disconnecting = true;

         try {
            Minecraft mc = Minecraft.getInstance();
            boolean local = mc.isLocalServer();
            ServerData server = mc.getCurrentServer();
            if (mc.level != null) {
               mc.level.disconnect();
            }

            if (local) {
               mc.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
            } else {
               mc.disconnect();
            }

            TitleScreen title = new TitleScreen();
            if (local) {
               mc.setScreen(title);
            } else if (server != null && server.isRealm()) {
               mc.setScreen(new RealmsMainScreen(title));
            } else {
               mc.setScreen(new JoinMultiplayerScreen(title));
            }
         } finally {
            disconnecting = false;
         }

      }
   }

   public static String worldName() {
      Minecraft mc = Minecraft.getInstance();
      String name = null;
      if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
         name = mc.getSingleplayerServer().getWorldData().getLevelName();
      } else if (mc.getCurrentServer() != null) {
         name = mc.getCurrentServer().name;
      }

      return name != null && !name.isBlank() ? name : Component.translatable("menu.game").getString();
   }

   public static boolean singleplayer() {
      Minecraft mc = Minecraft.getInstance();
      return mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null;
   }

   public static @Nullable Component mode() {
      Minecraft mc = Minecraft.getInstance();
      return mc.gameMode == null ? null : mc.gameMode.getPlayerMode().getShortDisplayName();
   }

   public static boolean survives() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.gameMode == null) {
         return false;
      } else {
         GameType type = mc.gameMode.getPlayerMode();
         return type == GameType.SURVIVAL || type == GameType.ADVENTURE;
      }
   }

   public static @Nullable Component dimension() {
      Minecraft mc = Minecraft.getInstance();
      return mc.level == null ? null : Component.literal(pretty(mc.level.dimension().location().getPath()));
   }

   public static @Nullable Component dayTime() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return null;
      } else {
         long time = mc.level.getDayTime();
         long day = time / 24000L + 1L;
         long tod = Math.floorMod(time, 24000L);
         int hour = (int)((tod / 1000L + 6L) % 24L);
         int minute = (int)(tod % 1000L * 60L / 1000L);
         return Component.translatable("cfhgui.pause.day_time", new Object[]{day, String.format(Locale.ROOT, "%02d:%02d", hour, minute)});
      }
   }

   public static @Nullable Component session() {
      return CFHGuiMenu.config().showSessionTime && LastPlayed.sessionMs() > 0L ? Component.translatable("cfhgui.pause.session", new Object[]{Fmt.duration(LastPlayed.sessionMs())}) : null;
   }

   public static @Nullable Component position() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer p = mc.player;
      if (p != null && !mc.showOnlyReducedInfo()) {
         int var10000 = Mth.floor(p.getX());
         return Component.literal(var10000 + "  " + Mth.floor(p.getY()) + "  " + Mth.floor(p.getZ()));
      } else {
         return null;
      }
   }

   public static @Nullable Component biome() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer p = mc.player;
      return p != null && mc.level != null && !mc.showOnlyReducedInfo() ? (Component)mc.level.getBiome(p.blockPosition()).unwrapKey().map((key) -> {
         String var10000 = key.location().getNamespace();
         return Component.translatable("biome." + var10000 + "." + key.location().getPath());
      }).orElse(null) : null;
   }

   private static String pretty(String path) {
      StringBuilder sb = new StringBuilder(path.length());
      boolean up = true;

      for(char c : path.toCharArray()) {
         if (c != '_' && c != '/' && c != ':') {
            sb.append(up ? Character.toUpperCase(c) : c);
            up = false;
         } else {
            sb.append(' ');
            up = true;
         }
      }

      return sb.toString();
   }

   private PauseActions() {
   }

   public static enum Kind {
      PRIMARY,
      NORMAL,
      DANGER;
   }

   public static record Action(Icon icon, Component label, Runnable run, Kind kind, boolean enabled, @Nullable Component tip, @Nullable String lockedModule) {
      public boolean locked() {
         return this.lockedModule != null;
      }
   }
}
