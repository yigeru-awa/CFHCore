package com.onehumanawa.cfhcore.content.gui.core.net.blob;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface BlobRouter {
   List<ServerPlayer> recipients(ServerPlayer var1, BlobPayloads.Start var2);
}
