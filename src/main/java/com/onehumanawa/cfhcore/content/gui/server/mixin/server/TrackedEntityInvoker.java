package com.onehumanawa.cfhcore.content.gui.server.mixin.server;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(
	targets = {"net.minecraft.server.level.ChunkMap$TrackedEntity"}
)
public interface TrackedEntityInvoker {
	@Invoker("updatePlayers")
	void cfhServer$updatePlayers(List<ServerPlayer> var1);
}
