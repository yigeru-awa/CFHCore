package com.onehumanawa.cfhcore.content.gui.core.net;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

@FunctionalInterface
public interface PayloadHandler<T extends CustomPacketPayload> {
   void handle(T var1, NetContext var2);
}
