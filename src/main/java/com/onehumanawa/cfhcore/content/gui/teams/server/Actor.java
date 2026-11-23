package com.onehumanawa.cfhcore.content.gui.teams.server;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public record Actor(UUID id, String name, @Nullable ServerPlayer player, boolean admin, @Nullable String dimension, int chunkX, int chunkZ) {
	private static final UUID NOBODY = new UUID(0L, 0L);

	public static Actor of(ServerPlayer player) {
		return new Actor(
			player.getUUID(),
			player.getGameProfile().getName(),
			player,
			player.hasPermissions(2),
			player.level().dimension().location().toString(),
			player.chunkPosition().x,
			player.chunkPosition().z
		);
	}

	public static Actor console(String name) {
		return new Actor(NOBODY, name, null, true, null, 0, 0);
	}

	public static Actor fake(UUID id, String name, String dimension, int chunkX, int chunkZ) {
		return new Actor(id, name, null, false, dimension, chunkX, chunkZ);
	}

	public Actor asAdmin() {
		return new Actor(this.id, this.name, this.player, true, this.dimension, this.chunkX, this.chunkZ);
	}

	public boolean isConsole() {
		return NOBODY.equals(this.id);
	}
}
