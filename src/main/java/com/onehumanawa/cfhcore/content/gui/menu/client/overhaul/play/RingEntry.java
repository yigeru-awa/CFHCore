package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.play;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.CubePlanetNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.ItemNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PivotNode;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.ServerActions;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.ServerMeta;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.ServerPinger;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.WorldEntry;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.WorldFavorites;
import java.util.Locale;
import net.minecraft.client.multiplayer.ServerData;
import org.jetbrains.annotations.Nullable;

abstract class RingEntry {
   @Nullable PivotNode holder;
   @Nullable CubePlanetNode planet;
   @Nullable ItemNode star;
   @Nullable HaloNode halo;
   float shown;
   final Anim play;

   RingEntry() {
      this.play = new Anim(0.0F, 200, Ease.OUT_CUBIC);
   }

   abstract String id();

   abstract String name();

   abstract boolean favorite();

   abstract void favorite(boolean var1);

   abstract long lastPlayed();

   abstract CubePlanetNode grow();

   boolean matches(String query) {
      if (query != null && !query.isBlank()) {
         String q = query.toLowerCase(Locale.ROOT).trim();
         return this.name().toLowerCase(Locale.ROOT).contains(q) || this.id().toLowerCase(Locale.ROOT).contains(q);
      } else {
         return true;
      }
   }

   static final class World extends RingEntry {
      final WorldEntry world;

      World(WorldEntry world) {
         this.world = world;
      }

      String id() {
         return this.world.id();
      }

      String name() {
         return this.world.name();
      }

      boolean favorite() {
         return this.world.favorite;
      }

      void favorite(boolean on) {
         WorldFavorites.setFavorite(this.world.id(), on);
         this.world.favorite = on;
      }

      long lastPlayed() {
         return this.world.summary.getLastPlayed();
      }

      boolean matches(String query) {
         return this.world.matches(query);
      }

      CubePlanetNode grow() {
         return CubePlanetNode.create(CubePlanetNode.seedOf("world:" + this.world.id()));
      }
   }

   static final class Server extends RingEntry {
      static final int PINGING = -7698304;
      static final int ONLINE = -10694529;
      static final int OUTDATED = -2055080;
      static final int OFFLINE = -1750963;
      final ServerData data;
      final long lastJoined;

      Server(ServerData data, long lastJoined) {
         this.data = data;
         this.lastJoined = lastJoined;
      }

      String id() {
         return ServerActions.normalize(this.data.ip);
      }

      String name() {
         return this.data.name != null && !this.data.name.isBlank() ? this.data.name : this.data.ip;
      }

      boolean favorite() {
         return ServerMeta.isFavorite(this.data.ip);
      }

      void favorite(boolean on) {
         ServerMeta.setFavorite(this.data.ip, on);
      }

      long lastPlayed() {
         return this.lastJoined;
      }

      boolean matches(String query) {
         if (super.matches(query)) {
            return true;
         } else {
            String q = query.toLowerCase(Locale.ROOT).trim();
            return this.data.motd != null && this.data.motd.getString().toLowerCase(Locale.ROOT).contains(q);
         }
      }

      CubePlanetNode grow() {
         return CubePlanetNode.server(CubePlanetNode.seedOf("server:" + this.id()));
      }

      int statusColor() {
         int var10000;
         switch (this.data.state()) {
            case SUCCESSFUL -> var10000 = -10694529;
            case INCOMPATIBLE -> var10000 = -2055080;
            case UNREACHABLE -> var10000 = -1750963;
            default -> var10000 = -7698304;
         }

         return var10000;
      }

      boolean online() {
         return ServerPinger.isOnline(this.data);
      }
   }
}
