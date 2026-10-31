package com.onehumanawa.cfhcore.content.gui.menu.client.play.model;

import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.MenuIo;
import java.net.UnknownHostException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.ServerData.State;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class ServerPinger {
   private static final long TIMEOUT_MS = 12000L;
   private final ServerStatusPinger pinger = new ServerStatusPinger();
   private final Map<ServerData, Long> inFlight = new IdentityHashMap();
   private final Map<ServerData, Runnable> callbacks = new IdentityHashMap();

   public void ping(ServerData data, Runnable onChanged) {
      if (data.state() == State.INITIAL) {
         data.setState(State.PINGING);
         data.motd = CommonComponents.EMPTY;
         data.status = CommonComponents.EMPTY;
         synchronized(this.inFlight) {
            this.inFlight.put(data, System.currentTimeMillis());
            this.callbacks.put(data, onChanged);
         }

         MenuIo.POOL.submit(() -> {
            try {
               this.pinger.pingServer(data, () -> MenuIo.onClient(onChanged), () -> {
                  data.setState(data.protocol == SharedConstants.getCurrentVersion().getProtocolVersion() ? State.SUCCESSFUL : State.INCOMPATIBLE);
                  this.finish(data);
                  MenuIo.onClient(onChanged);
               });
            } catch (UnknownHostException var4) {
               this.fail(data, Component.translatable("multiplayer.status.cannot_resolve").withStyle(ChatFormatting.RED), onChanged);
            } catch (Exception e) {
               CFHGuiMenu.LOGGER.debug("[CFHGui Menu] ping {} failed: {}", data.ip, e.toString());
               this.fail(data, Component.translatable("multiplayer.status.cannot_connect").withStyle(ChatFormatting.RED), onChanged);
            }

         });
      }
   }

   public void refresh(ServerData data, Runnable onChanged) {
      if (data.state() != State.PINGING) {
         data.setState(State.INITIAL);
         this.ping(data, onChanged);
      }
   }

   private void fail(ServerData data, Component motd, Runnable onChanged) {
      data.setState(State.UNREACHABLE);
      data.motd = motd;
      data.status = CommonComponents.EMPTY;
      data.playerList = List.of();
      this.finish(data);
      MenuIo.onClient(onChanged);
   }

   private void finish(ServerData data) {
      synchronized(this.inFlight) {
         this.inFlight.remove(data);
         this.callbacks.remove(data);
      }
   }

   public void tick() {
      this.pinger.tick();
      long now = System.currentTimeMillis();
      String cantConnect = I18n.get("multiplayer.status.cannot_connect", new Object[0]);
      Map<ServerData, Runnable> expired = new IdentityHashMap();
      synchronized(this.inFlight) {
         for(Map.Entry<ServerData, Long> e : this.inFlight.entrySet()) {
            ServerData d = (ServerData)e.getKey();
            boolean refused = d.motd != null && cantConnect.equals(d.motd.getString());
            if (d.state() != State.PINGING || refused || now - (Long)e.getValue() > 12000L) {
               expired.put(d, (Runnable)this.callbacks.get(d));
            }
         }
      }

      for(Map.Entry<ServerData, Runnable> e : expired.entrySet()) {
         ServerData d = (ServerData)e.getKey();
         if (d.state() == State.PINGING) {
            d.setState(State.UNREACHABLE);
            d.status = CommonComponents.EMPTY;
            if (d.motd == null || d.motd.getString().isEmpty()) {
               d.motd = Component.translatable("multiplayer.status.cannot_connect").withStyle(ChatFormatting.RED);
            }
         }

         this.finish(d);
         if (e.getValue() != null) {
            ((Runnable)e.getValue()).run();
         }
      }

   }

   public void close() {
      this.pinger.removeAll();
      synchronized(this.inFlight) {
         this.inFlight.clear();
         this.callbacks.clear();
      }
   }

   public static boolean isOnline(ServerData d) {
      return d.state() == State.SUCCESSFUL || d.state() == State.INCOMPATIBLE;
   }

   public static String players(ServerData d) {
      return d.players == null ? "" : d.players.online() + "/" + d.players.max();
   }
}
