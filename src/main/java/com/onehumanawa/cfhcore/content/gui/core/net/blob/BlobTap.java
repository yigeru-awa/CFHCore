package com.onehumanawa.cfhcore.content.gui.core.net.blob;

import net.minecraft.server.level.ServerPlayer;

public interface BlobTap {
   default void started(ServerPlayer sender, BlobPayloads.Start start) {
   }

   default void chunk(ServerPlayer sender, BlobPayloads.Chunk chunk) {
   }

   default void ended(ServerPlayer sender, String id, boolean complete) {
   }
}
