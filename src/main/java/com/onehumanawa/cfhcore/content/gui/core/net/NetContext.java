package com.onehumanawa.cfhcore.content.gui.core.net;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public record NetContext(boolean isClient, @Nullable ServerPlayer sender) {
   public static NetContext client() {
      return new NetContext(true, (ServerPlayer)null);
   }

   public static NetContext server(ServerPlayer sender) {
      return new NetContext(false, sender);
   }
}
