package com.onehumanawa.cfhcore.content.gui.server.client.stats;

import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.Board;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.PlayerRef;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.Provider;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.Snapshot;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class ServerStats implements Provider {
	private static boolean initialised;

	public static synchronized void init() {
		if (!initialised) {
			initialised = true;
			StatsLink.provide(new ServerStats());
		}
	}

	private static boolean others() {
		return ServerState.feature("stats") && ServerState.may(Nodes.STATS_OTHERS);
	}

	private static boolean rankings() {
		return ServerState.feature("rankings") && ServerState.may(Nodes.RANKINGS_USE);
	}

	public boolean available() {
		return ServerState.available() && !ServerState.newerServer() && (others() || rankings());
	}

	public void players(Consumer<List<PlayerRef>> callback) {
		if (this.available() && others()) {
			ServerActions.players(answer -> callback.accept(answer.orElse(List.of())));
		} else {
			callback.accept(List.of());
		}
	}

	public void stats(UUID player, Consumer<Optional<Snapshot>> callback) {
		if (this.available() && ServerState.feature("stats") && (player.equals(ServerState.selfId()) || others())) {
			ServerActions.stats(player, callback, refused -> {
			});
		} else {
			callback.accept(Optional.empty());
		}
	}

	public void board(String key, int offset, int count, Consumer<Board> callback) {
		Board none = new Board(key, List.of(), 0, null);
		if (this.available() && rankings()) {
			ServerActions.board(key, Math.max(0, offset), Math.max(1, Math.min(100, count)), answer -> callback.accept(answer.orElse(none)));
		} else {
			callback.accept(none);
		}
	}

	private ServerStats() {
	}
}
