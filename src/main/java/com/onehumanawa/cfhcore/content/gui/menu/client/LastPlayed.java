package com.onehumanawa.cfhcore.content.gui.menu.client;

import com.onehumanawa.cfhcore.content.gui.menu.MenuConfig;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.ServerActions;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.WorldActions;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelSummary;
import org.jetbrains.annotations.Nullable;

public final class LastPlayed {
   private static long sessionStartMs;

   public static Optional<Target> quick() {
      MenuConfig c = CFHGuiMenu.config();
      if ("server".equals(c.lastKind) && !c.lastServerAddress.isBlank()) {
         return Optional.of(new Target(LastPlayed.Kind.SERVER, c.lastServerAddress, c.lastServerName, c.lastPlayedAt, (LevelSummary)null));
      } else {
         return "world".equals(c.lastKind) && !c.lastWorld.isBlank() ? Optional.of(new Target(LastPlayed.Kind.WORLD, c.lastWorld, c.lastWorldName, c.lastPlayedAt, (LevelSummary)null)) : Optional.empty();
      }
   }

   public static void resolve(Consumer<Optional<Target>> callback) {
      Optional<Target> server = quick().filter((t) -> t.kind() == LastPlayed.Kind.SERVER);
      WorldActions.loadAll().whenCompleteAsync((list, err) -> {
         Target world = null;
         if (list != null) {
            for(LevelSummary s : list) {
               if (!s.isDisabled() && !s.isLocked() && (world == null || s.getLastPlayed() > world.at())) {
                  world = new Target(LastPlayed.Kind.WORLD, s.getLevelId(), s.getLevelName(), s.getLastPlayed(), s);
               }
            }
         }

         Optional<Target> best = Optional.ofNullable(world);
         if (server.isPresent() && (world == null || ((Target)server.get()).at() >= world.at())) {
            best = server;
         }

         callback.accept(best);
      }, Minecraft.getInstance());
   }

   public static void play(Target t, @Nullable Screen parent) {
      if (t != null) {
         if (t.kind() == LastPlayed.Kind.WORLD) {
            if (t.summary() != null) {
               WorldActions.play(t.summary(), parent);
            } else {
               WorldActions.play(t.id(), parent);
            }
         } else {
            ServerActions.join(t.id(), t.name(), parent);
         }

      }
   }

   public static void recordJoin() {
      Minecraft mc = Minecraft.getInstance();
      sessionStartMs = System.currentTimeMillis();
      long now = sessionStartMs;

      try {
         if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            IntegratedServer s = mc.getSingleplayerServer();
            String folder = "";

            try {
               folder = s.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString();
            } catch (Exception var7) {
            }

            String name = s.getWorldData().getLevelName();
            String worldFolder = folder;
            CFHGuiMenu.configFile().update((c) -> {
               c.lastKind = "world";
               c.lastWorld = worldFolder;
               c.lastWorldName = name;
               c.lastPlayedAt = now;
            });
         } else if (mc.getCurrentServer() != null && !mc.getCurrentServer().isLan() && !mc.getCurrentServer().isRealm()) {
            ServerData d = mc.getCurrentServer();
            String address = d.ip;
            String name = d.name == null ? d.ip : d.name;
            CFHGuiMenu.configFile().update((c) -> {
               c.lastKind = "server";
               c.lastServerAddress = address;
               c.lastServerName = name;
               c.lastPlayedAt = now;
               List<MenuConfig.RecentServer> recents = new ArrayList();
               recents.add(new MenuConfig.RecentServer(name, address, now));

               for(MenuConfig.RecentServer r : c.recentServers) {
                  if (r != null && r.address != null && !ServerActions.normalize(r.address).equals(ServerActions.normalize(address))) {
                     recents.add(r);
                  }
               }

               int max = Math.max(1, c.recentServersMax);
               c.recentServers = (List<MenuConfig.RecentServer>)(recents.size() > max ? new ArrayList(recents.subList(0, max)) : recents);
            });
         }
      } catch (Exception e) {
         CFHGuiMenu.LOGGER.warn("[CFHGui Menu] could not record last played: {}", e.toString());
      }

   }

   public static long sessionMs() {
      return Minecraft.getInstance().level != null && sessionStartMs != 0L ? System.currentTimeMillis() - sessionStartMs : 0L;
   }

   public static void clearSession() {
      sessionStartMs = 0L;
   }

   private LastPlayed() {
   }

   public static enum Kind {
      WORLD,
      SERVER;
   }

   public static record Target(Kind kind, String id, String name, long at, @Nullable LevelSummary summary) {
   }
}
