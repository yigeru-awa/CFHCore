package com.onehumanawa.cfhcore.content.gui.core.neoforge;

import com.onehumanawa.cfhcore.content.gui.core.net.Flow;
import com.onehumanawa.cfhcore.content.gui.core.net.NetContext;
import com.onehumanawa.cfhcore.content.gui.core.net.PayloadHandler;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NeoForgeNetwork implements CFHGuiNetwork {
   private static final List<Pending<?>> PENDING = new ArrayList();
   private static boolean flushed;

   static void flush(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar r = event.registrar("1").optional();
      synchronized(PENDING) {
         for(Pending<?> p : PENDING) {
            p.register(r);
         }

         PENDING.clear();
         flushed = true;
      }
   }

   public <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Flow flow, PayloadHandler<T> handler) {
      if (!"cfhgui".equals(type.id().getNamespace())) {
         throw new IllegalArgumentException("CFHGui payload ids must use the 'cfhgui' namespace: " + String.valueOf(type.id()));
      } else {
         synchronized(PENDING) {
            if (flushed) {
               throw new IllegalStateException("Payload " + String.valueOf(type.id()) + " registered after network registration closed; register in CFHGuiModule.init()");
            } else {
               PENDING.add(new Pending(type, codec, flow, handler));
            }
         }
      }
   }

   public void sendToServer(CustomPacketPayload payload) {
      if (Minecraft.getInstance().getConnection() != null && this.serverHasChannel(payload.type())) {
         PacketDistributor.sendToServer(payload, new CustomPacketPayload[0]);
      }
   }

   public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
      if (this.canSendToPlayer(player, payload.type())) {
         PacketDistributor.sendToPlayer(player, payload, new CustomPacketPayload[0]);
      }
   }

   public boolean canSendToPlayer(ServerPlayer player, CustomPacketPayload.Type<?> type) {
      return player.connection != null && NetworkRegistry.hasChannel(player.connection, type.id());
   }

   public boolean serverHasChannel(CustomPacketPayload.Type<?> type) {
      ClientPacketListener conn = Minecraft.getInstance().getConnection();
      return conn != null && NetworkRegistry.hasChannel(conn, type.id());
   }

   private static record Pending<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Flow flow, PayloadHandler<T> handler) {
      void register(PayloadRegistrar r) {
         switch (this.flow) {
            case C2S -> r.playToServer(this.type, this.codec, (p, ctx) -> this.handler.handle(p, NetContext.server((ServerPlayer)ctx.player())));
            case S2C -> r.playToClient(this.type, this.codec, (p, ctx) -> this.handler.handle(p, NetContext.client()));
            case BOTH -> r.playBidirectional(this.type, this.codec, (p, ctx) -> {
   if (ctx.flow().isServerbound()) {
      this.handler.handle(p, NetContext.server((ServerPlayer)ctx.player()));
   } else {
      this.handler.handle(p, NetContext.client());
   }

});
         }

      }
   }
}
