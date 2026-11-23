package com.onehumanawa.cfhcore.content.gui.server.api;

import com.onehumanawa.cfhcore.content.gui.core.event.Event;
import com.onehumanawa.cfhcore.content.gui.server.model.Home;
import com.onehumanawa.cfhcore.content.gui.server.model.Place;
import com.onehumanawa.cfhcore.content.gui.server.model.Punishment;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import com.onehumanawa.cfhcore.content.gui.server.model.Warp;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class ServerApi {
	public static final Event<ServerApi.AfkListener> AFK_CHANGED = new Event("SERVER_AFK_CHANGED");
	public static final Event<ServerApi.TeleportListener> TELEPORTED = new Event("SERVER_TELEPORTED");
	public static final Event<ServerApi.PunishmentListener> PUNISHMENT_CHANGED = new Event("SERVER_PUNISHMENT_CHANGED");
	public static final Event<Consumer<UUID>> PERMISSIONS_CHANGED = new Event("SERVER_PERMISSIONS_CHANGED");
	@Nullable
	private static volatile ServerApi.Backend backend;

	public static void install(@Nullable ServerApi.Backend b) {
		backend = b;
	}

	public static boolean running() {
		return backend != null;
	}

	public static List<Home> homes(UUID player) {
		ServerApi.Backend b = backend;
		return b == null ? List.of() : b.homes(player);
	}

	public static List<Warp> warps() {
		ServerApi.Backend b = backend;
		return b == null ? List.of() : b.warps();
	}

	@Nullable
	public static Warp warp(String name) {
		ServerApi.Backend b = backend;
		return b == null ? null : b.warp(name);
	}

	@Nullable
	public static Place spawn() {
		ServerApi.Backend b = backend;
		return b == null ? null : b.spawn();
	}

	public static boolean teleport(ServerPlayer player, Place place, ServerApi.Cause cause) {
		ServerApi.Backend b = backend;
		return b != null && b.teleport(player, place, cause);
	}

	public static boolean afk(UUID player) {
		ServerApi.Backend b = backend;
		return b != null && b.afk(player);
	}

	public static boolean vanished(UUID player) {
		ServerApi.Backend b = backend;
		return b != null && b.vanished(player);
	}

	public static boolean muted(UUID player) {
		ServerApi.Backend b = backend;
		return b != null && b.active(player, Punishment.Kind.MUTE) != null;
	}

	public static boolean frozen(UUID player) {
		ServerApi.Backend b = backend;
		return b != null && b.active(player, Punishment.Kind.FREEZE) != null;
	}

	public static List<Role.Tag> roles(UUID player) {
		ServerApi.Backend b = backend;
		return b == null ? List.of() : b.roles(player);
	}

	@Nullable
	public static Role.Tag role(UUID player) {
		List<Role.Tag> roles = roles(player);
		return roles.isEmpty() ? null : roles.get(0);
	}

	@Nullable
	public static String nick(UUID player) {
		ServerApi.Backend b = backend;
		return b == null ? null : b.nick(player);
	}

	public static long extra(UUID player, String key) {
		ServerApi.Backend b = backend;
		return b == null ? 0L : b.extra(player, key);
	}

	public static void audit(@Nullable ServerPlayer actor, String action, @Nullable UUID target, String targetName, String detail) {
		ServerApi.Backend b = backend;
		if (b != null) {
			b.audit(actor, action, target, targetName, detail);
		}
	}

	private ServerApi() {
	}

	@FunctionalInterface
	public interface AfkListener {
		void changed(ServerPlayer var1, boolean var2);
	}

	public interface Backend {
		List<Home> homes(UUID var1);

		List<Warp> warps();

		@Nullable
		Warp warp(String var1);

		Place spawn();

		boolean teleport(ServerPlayer var1, Place var2, ServerApi.Cause var3);

		boolean afk(UUID var1);

		boolean vanished(UUID var1);

		@Nullable
		Punishment active(UUID var1, Punishment.Kind var2);

		List<Role.Tag> roles(UUID var1);

		@Nullable
		String nick(UUID var1);

		long extra(UUID var1, String var2);

		void audit(@Nullable ServerPlayer var1, String var2, @Nullable UUID var3, String var4, String var5);
	}

	public static enum Cause {
		HOME,
		WARP,
		SPAWN,
		BACK,
		REQUEST,
		RANDOM,
		POSITION,
		STAFF,
		OTHER;
	}

	@FunctionalInterface
	public interface PunishmentListener {
		void changed(UUID var1, Punishment var2, boolean var3);
	}

	@FunctionalInterface
	public interface TeleportListener {
		void teleported(ServerPlayer var1, Place var2, Place var3, ServerApi.Cause var4);
	}
}
