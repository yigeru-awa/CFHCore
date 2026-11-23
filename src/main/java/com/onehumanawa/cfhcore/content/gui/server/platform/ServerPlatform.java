package com.onehumanawa.cfhcore.content.gui.server.platform;

import com.onehumanawa.cfhcore.content.gui.core.platform.Services;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public interface ServerPlatform {
	static ServerPlatform get() {
		return (ServerPlatform)Services.load(ServerPlatform.class);
	}

	@Nullable
	String externalPermissions();

	@Nullable
	Boolean externalPermission(ServerPlayer var1, String var2);

	boolean isFakePlayer(Player var1);
}
