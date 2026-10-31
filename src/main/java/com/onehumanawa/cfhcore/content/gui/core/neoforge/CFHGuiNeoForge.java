package com.onehumanawa.cfhcore.content.gui.core.neoforge;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

// Server side wiring of the CFHGui core; the client half is installed by CFHClient.
public final class CFHGuiNeoForge {
   public static void register(IEventBus modBus) {
      CFHGui.init();
      modBus.addListener(RegisterPayloadHandlersEvent.class, NeoForgeNetwork::flush);
      NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, (e) -> CFHGuiEvents.SERVER_STARTED.invoke((l) -> l.accept(e.getServer())));
      NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, (e) -> CFHGuiEvents.SERVER_STOPPING.invoke((l) -> l.accept(e.getServer())));
      NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, (e) -> CFHGuiEvents.SERVER_TICK_END.invoke((l) -> l.accept(e.getServer())));
      NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, (e) -> {
         Player patt0$temp = e.getEntity();
         if (patt0$temp instanceof ServerPlayer p) {
            CFHGuiEvents.PLAYER_JOINED.invoke((l) -> l.accept(p));
         }

      });
      NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, (e) -> {
         Player patt0$temp = e.getEntity();
         if (patt0$temp instanceof ServerPlayer p) {
            CFHGuiEvents.PLAYER_LEFT.invoke((l) -> l.accept(p));
         }

      });
   }

   private CFHGuiNeoForge() {
   }
}
