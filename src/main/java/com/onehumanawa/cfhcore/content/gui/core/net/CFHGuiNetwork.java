package com.onehumanawa.cfhcore.content.gui.core.net;

import com.onehumanawa.cfhcore.content.gui.core.platform.Services;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public interface CFHGuiNetwork {
   static CFHGuiNetwork get() {
      return (CFHGuiNetwork)Services.load(CFHGuiNetwork.class);
   }

   <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> var1, StreamCodec<? super RegistryFriendlyByteBuf, T> var2, Flow var3, PayloadHandler<T> var4);

   void sendToServer(CustomPacketPayload var1);

   void sendToPlayer(ServerPlayer var1, CustomPacketPayload var2);

   boolean canSendToPlayer(ServerPlayer var1, CustomPacketPayload.Type<?> var2);

   boolean serverHasChannel(CustomPacketPayload.Type<?> var1);
}
