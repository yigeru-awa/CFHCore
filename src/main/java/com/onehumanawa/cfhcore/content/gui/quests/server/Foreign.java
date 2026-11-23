package com.onehumanawa.cfhcore.content.gui.quests.server;

import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.compat.TeamsFounding;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.net.QuestsNet;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class Foreign {
	public static boolean running() {
		return QuestsServer.engine() != null;
	}

	@Nullable
	public static BookIndex index() {
		return QuestsServer.index();
	}

	public static int absorb(UUID holder, Collection<Foreign.Quest> quests) {
		QuestEngine e = QuestsServer.engine();
		return e == null ? 0 : e.absorb(holder, quests);
	}

	public static UUID holderOf(UUID player) {
		return QuestsServer.holderOf(player);
	}

	public static void renamed(Map<String, String> keys) {
		QuestEngine e = QuestsServer.engine();
		if (e != null && !keys.isEmpty()) {
			e.store().rename(key -> keys.getOrDefault(key, key));
		}
	}

	public static void reload() {
		if (QuestsServer.engine() != null) {
			QuestsServer.reload();
		}
	}

	public static Path bookDir() {
		return QuestsServer.bookDir();
	}

	@Nullable
	public static QuestBook book() {
		return QuestsServer.book();
	}

	public static boolean mayEdit(ServerPlayer player) {
		return QuestsServer.mayEdit(player);
	}

	public static int tellEditors(MinecraftServer server, Component line) {
		int n = 0;

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (QuestsServer.mayEdit(player)) {
				player.sendSystemMessage(line);
				n++;
			}
		}

		return n;
	}

	public static boolean hasClient(ServerPlayer player) {
		return QuestsNet.has(player);
	}

	public static MutableComponent msg(String key, String english, Object... args) {
		return ServerText.msg(key, english, args);
	}

	public static Map<UUID, UUID> foundTeams(MinecraftServer server, Collection<Foreign.Party> parties) {
		if (!parties.isEmpty() && Mods.has("cfhgui_teams")) {
			try {
				return Foreign.TeamsSide.found(server, parties);
			} catch (Throwable var3) {
				CFHGuiQuests.LOGGER.warn("[CFHGui Quests] the parties could not be made into teams of CFHGui Teams; progress stays with each player ({})", var3.toString());
				return Map.of();
			}
		} else {
			return Map.of();
		}
	}

	private Foreign() {
	}

	public static record Member(String name, boolean officer) {
	}

	public static record Party(UUID id, String name, int color, UUID owner, Map<UUID, Foreign.Member> members) {
	}

	public static record Quest(String key, long[] tasks, boolean done, long startedAt, long doneAt, int times, Map<String, Long> claims, long teamClaims) {
	}

	private static final class TeamsSide {
		static Map<UUID, UUID> found(MinecraftServer server, Collection<Foreign.Party> parties) {
			return TeamsFounding.found(server, parties);
		}
	}
}
