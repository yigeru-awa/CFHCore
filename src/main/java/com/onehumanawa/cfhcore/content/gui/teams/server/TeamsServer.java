package com.onehumanawa.cfhcore.content.gui.teams.server;

import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.TeamsServerConfig;
import com.onehumanawa.cfhcore.content.gui.teams.api.TeamsApi;
import com.onehumanawa.cfhcore.content.gui.teams.net.TeamsNet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class TeamsServer {
	private static final int ACTIONS_PER_SECOND = 40;
	private static boolean initialised;
	@Nullable
	private static volatile TeamService service;
	private static final Object2IntOpenHashMap<UUID> ACTIONS = new Object2IntOpenHashMap();
	private static int ticks;

	public static synchronized void init() {
		if (!initialised) {
			initialised = true;
			CFHGuiEvents.SERVER_STARTED.register(TeamsServer::started);
			CFHGuiEvents.SERVER_STOPPING.register(TeamsServer::stopping);
			CFHGuiEvents.SERVER_TICK_END.register(TeamsServer::tick);
			CFHGuiEvents.PLAYER_JOINED.register(TeamsServer::joined);
			CFHGuiEvents.PLAYER_LEFT.register(TeamsServer::left);
		}
	}

	@Nullable
	public static TeamService service() {
		return service;
	}

	private static void started(MinecraftServer server) {
		TeamsServerConfig.reload();

		try {
			TeamService s = TeamService.start(server);
			service = s;
			TeamsApi.install(new Backend(s));
		} catch (RuntimeException var2) {
			service = null;
			CFHGuiTeams.LOGGER.error("[CFHGui Teams] the teams of this world could not be started; the server runs without them", var2);
		}
	}

	private static void stopping(MinecraftServer server) {
		TeamService s = service;
		if (s != null) {
			TeamsApi.install(null);
			service = null;
			ACTIONS.clear();

			try {
				s.stop();
			} catch (RuntimeException var3) {
				CFHGuiTeams.LOGGER.error("[CFHGui Teams] the teams could not be saved cleanly as the server stopped", var3);
			}
		}
	}

	private static void tick(MinecraftServer server) {
		TeamService s = service;
		if (s != null) {
			if (++ticks % 20 == 0) {
				ACTIONS.clear();
			}

			s.tick();
		}
	}

	private static void joined(ServerPlayer player) {
		TeamService s = service;
		if (s != null) {
			s.joined(player);
		}
	}

	private static void left(ServerPlayer player) {
		TeamService s = service;
		if (s != null) {
			s.left(player);
		}

		ACTIONS.removeInt(player.getUUID());
	}

	public static void onAction(ServerPlayer player, TeamsNet.Action action) {
		TeamService s = service;
		if (s == null) {
			TeamsNet.send(player, TeamsNet.Result.fail(action.request(), "disabled"));
		} else if (ACTIONS.addTo(player.getUUID(), 1) >= 40) {
			TeamsNet.send(player, TeamsNet.Result.fail(action.request(), "too_fast"));
		} else {
			OpResult result = s.run(Actor.of(player), action.op(), action.args());
			s.sync.flush();
			TeamsNet.send(player, result.payload(action.request()));
		}
	}

	static void standIn(TeamService other, Runnable body) {
		TeamService own = service;
		service = other;

		try {
			body.run();
		} finally {
			service = own;
		}
	}

	static void reload() {
		TeamsServerConfig.reload();
		TeamService s = service;
		if (s != null) {
			s.reloaded();
		}
	}

	private TeamsServer() {
	}
}
