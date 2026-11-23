package com.onehumanawa.cfhcore.content.gui.server.service;

import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms;
import com.onehumanawa.cfhcore.content.gui.server.ServerHooks;
import com.onehumanawa.cfhcore.content.gui.server.CFHGuiServer;
import com.onehumanawa.cfhcore.content.gui.server.api.ServerApi;
import com.onehumanawa.cfhcore.content.gui.server.net.ServerNet;
import com.onehumanawa.cfhcore.content.gui.server.permission.ExtraNodes;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class ServerRuntime {
	private static final int ACTIONS_PER_SECOND = 40;
	private static boolean initialised;
	@Nullable
	private static volatile ServerService service;
	private static final Object2IntOpenHashMap<UUID> ACTIONS = new Object2IntOpenHashMap();
	private static int ticks;

	public static synchronized void init() {
		if (!initialised) {
			initialised = true;
			ExtraNodes.init();
			CFHGuiEvents.SERVER_STARTED.register(ServerRuntime::started);
			CFHGuiEvents.SERVER_STOPPING.register(ServerRuntime::stopping);
			CFHGuiEvents.SERVER_TICK_END.register(ServerRuntime::tick);
			CFHGuiEvents.PLAYER_JOINED.register((Consumer<ServerPlayer>)player -> {
				ServerService s = service;
				if (s != null) {
					guarded("a player joining", () -> s.joined(player));
				}
			});
			CFHGuiEvents.PLAYER_LEFT.register((Consumer<ServerPlayer>)player -> {
				ServerService s = service;
				if (s != null) {
					guarded("a player leaving", () -> s.left(player));
				}

				ACTIONS.removeInt(player.getUUID());
			});
			ServerHooks.CHAT.register((ServerHooks.ChatFilter)(player, message) -> {
				ServerService s = service;
				return s == null || s.chat(player, message);
			});
			ServerHooks.DAMAGE.register((ServerHooks.DamageFilter)(player, source, amount) -> {
				ServerService s = service;
				return s == null || s.damage(player, source, amount);
			});
			ServerHooks.DEATH.register((Consumer<ServerPlayer>)player -> {
				ServerService s = service;
				if (s != null) {
					s.died(player);
				}
			});
			ServerHooks.COPIED.register((ServerHooks.Copy)(from, to, alive) -> {
				ServerService s = service;
				if (s != null) {
					s.copied(from, to, alive);
				}
			});
		}
	}

	@Nullable
	public static ServerService service() {
		return service;
	}

	private static void started(MinecraftServer server) {
		try {
			ServerService s = ServerService.start(server);
			service = s;
			Perms.install(s.perms);
			ServerApi.install(new Backend(s));
		} catch (RuntimeException var2) {
			service = null;
			Perms.install(null);
			CFHGuiServer.LOGGER.error("[CFHGui Server] could not start for this world; the server runs without it", var2);
		}
	}

	private static void stopping(MinecraftServer server) {
		ServerService s = service;
		if (s != null) {
			ServerApi.install(null);
			Perms.install(null);
			service = null;
			ACTIONS.clear();

			try {
				s.stop();
			} catch (RuntimeException var3) {
				CFHGuiServer.LOGGER.error("[CFHGui Server] could not save cleanly as the server stopped", var3);
			}
		}
	}

	private static void tick(MinecraftServer server) {
		ServerService s = service;
		if (s != null) {
			if (++ticks % 20 == 0) {
				ACTIONS.clear();
			}

			s.admin.tickEnded();
			guarded("a tick", s::tick);
		}
	}

	private static void guarded(String what, Runnable body) {
		try {
			body.run();
		} catch (RuntimeException var3) {
			CFHGuiServer.LOGGER.error("[CFHGui Server] {} failed", what, var3);
		}
	}

	public static void onAction(ServerPlayer player, ServerNet.Action action) {
		ServerService s = service;
		if (s == null) {
			ServerNet.send(player, ServerNet.Result.fail(action.request(), "disabled"));
		} else if (ACTIONS.addTo(player.getUUID(), 1) >= 40) {
			ServerNet.send(player, ServerNet.Result.fail(action.request(), "too_fast"));
		} else {
			OpResult result = s.ops.run(Actor.of(player), action.op(), action.args());
			guarded("telling a client what changed", s.sync::flush);
			s.ops.answer(player, action, result);
		}
	}

	static void standIn(ServerService other, Runnable body) {
		ServerService own = service;
		service = other;

		try {
			body.run();
		} finally {
			service = own;
		}
	}

	private ServerRuntime() {
	}
}
