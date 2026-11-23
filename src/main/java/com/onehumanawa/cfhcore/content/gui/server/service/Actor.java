package com.onehumanawa.cfhcore.content.gui.server.service;

import com.onehumanawa.cfhcore.content.gui.server.mixin.server.CommandSourceStackAccessor;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public record Actor(UUID id, String name, @Nullable ServerPlayer player, int level, boolean owner, boolean console) {
	public static final UUID NOBODY = new UUID(0L, 0L);

	public static Actor of(ServerPlayer player) {
		return new Actor(
			player.getUUID(),
			player.getGameProfile().getName(),
			player,
			player.server.getProfilePermissions(player.getGameProfile()),
			player.server.isSingleplayerOwner(player.getGameProfile()),
			false
		);
	}

	public static Actor console(String name) {
		return new Actor(NOBODY, name, null, 4, false, true);
	}

	public static Actor unnamed(String name, int level) {
		return new Actor(NOBODY, name, null, Math.max(0, Math.min(4, level)), false, false);
	}

	public static Actor fake(UUID id, String name, int level) {
		return new Actor(id, name, null, level, false, false);
	}

	public static Actor of(CommandSourceStack source) {
		ServerPlayer typist = typist(source);
		if (typist != null) {
			return of(typist);
		} else {
			int level = 0;

			while (level < 4 && source.hasPermission(level + 1)) {
				level++;
			}

			return source.getEntity() == null && level >= 4
				? console(source.getTextName())
				: unnamed(((CommandSourceStackAccessor)source).cfhServer$source() instanceof ServerPlayer p ? p.getGameProfile().getName() : source.getTextName(), level);
		}
	}

	@Nullable
	private static ServerPlayer typist(CommandSourceStack source) {
		if (source.getEntity() instanceof ServerPlayer p && ((CommandSourceStackAccessor)source).cfhServer$source() == p) {
			return p;
		}

		return null;
	}

	public boolean operator() {
		return this.level >= 2;
	}
}
