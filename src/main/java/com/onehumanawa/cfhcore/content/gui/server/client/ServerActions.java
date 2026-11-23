package com.onehumanawa.cfhcore.content.gui.server.client;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.Board;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.PlayerRef;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.Row;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.Snapshot;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.server.model.AdminData;
import com.onehumanawa.cfhcore.content.gui.server.model.Kit;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerCard;
import com.onehumanawa.cfhcore.content.gui.server.model.Punishment;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import com.onehumanawa.cfhcore.content.gui.server.model.ServerRules;
import com.onehumanawa.cfhcore.content.gui.server.net.ServerNet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class ServerActions {
	private static int nextRequest = 1;
	private static final Map<Integer, Consumer<ServerNet.Result>> WAITING = new HashMap<>();
	private static final Map<Integer, ServerActions.Question<?>> QUESTIONS = new HashMap<>();

	public static void homeSet(@Nullable String name, @Nullable String icon, int color, @Nullable Consumer<ServerNet.Result> then) {
		CompoundTag a = new CompoundTag();
		if (name != null) {
			a.putString("name", name);
		}

		if (icon != null) {
			a.putString("icon", icon);
		}

		if (color >= 0) {
			a.putInt("color", color & 16777215);
		}

		send("home_set", a, then);
	}

	public static void homeEdit(String name, CompoundTag changes, @Nullable Consumer<ServerNet.Result> then) {
		CompoundTag a = changes.copy();
		a.putString("name", name);
		send("home_edit", a, then);
	}

	public static void homeDelete(String name, @Nullable UUID owner, @Nullable Consumer<ServerNet.Result> then) {
		CompoundTag a = string("name", name);
		if (owner != null) {
			a.putUUID("owner", owner);
		}

		send("home_delete", a, then);
	}

	public static void homeGo(@Nullable String name, @Nullable UUID owner) {
		CompoundTag a = new CompoundTag();
		if (name != null) {
			a.putString("name", name);
		}

		if (owner != null) {
			a.putUUID("owner", owner);
		}

		send("home_go", a, null);
	}

	public static void back() {
		send("back", new CompoundTag(), null);
	}

	public static void spawnGo() {
		send("spawn_go", new CompoundTag(), null);
	}

	public static void spawnSet(@Nullable Consumer<ServerNet.Result> then) {
		send("spawn_set", new CompoundTag(), then);
	}

	public static void warpGo(String name) {
		send("warp_go", string("name", name), null);
	}

	public static void warpSet(
		String name, @Nullable String icon, int color, @Nullable String description, boolean restricted, @Nullable Consumer<ServerNet.Result> then
	) {
		CompoundTag a = string("name", name);
		if (icon != null) {
			a.putString("icon", icon);
		}

		if (color >= 0) {
			a.putInt("color", color & 16777215);
		}

		if (description != null) {
			a.putString("description", description);
		}

		a.putBoolean("restricted", restricted);
		send("warp_set", a, then);
	}

	public static void warpEdit(String name, CompoundTag changes, @Nullable Consumer<ServerNet.Result> then) {
		CompoundTag a = changes.copy();
		a.putString("name", name);
		send("warp_edit", a, then);
	}

	public static void warpDelete(String name, @Nullable Consumer<ServerNet.Result> then) {
		send("warp_delete", string("name", name), then);
	}

	public static void tpa(UUID player, boolean here, @Nullable Consumer<ServerNet.Result> then) {
		CompoundTag a = id("id", player);
		a.putBoolean("here", here);
		send("tpa", a, then);
	}

	public static void tpaAccept(@Nullable UUID from) {
		send("tpa_accept", optionalId(from), null);
	}

	public static void tpaDeny(@Nullable UUID from) {
		send("tpa_deny", optionalId(from), null);
	}

	public static void tpaCancel(@Nullable UUID to) {
		send("tpa_cancel", optionalId(to), null);
	}

	public static void tpaToggle(boolean on) {
		send("tpa_toggle", bool("on", on), null);
	}

	public static void rtp(@Nullable String dimension) {
		CompoundTag a = new CompoundTag();
		if (dimension != null) {
			a.putString("dimension", dimension);
		}

		send("rtp", a, null);
	}

	public static void tpCancel() {
		send("tp_cancel", new CompoundTag(), r -> {
		});
	}

	public static void tpPosition(@Nullable String dimension, double x, @Nullable Double y, double z) {
		CompoundTag a = new CompoundTag();
		a.putDouble("x", x);
		if (y != null) {
			a.putDouble("y", y);
		}

		a.putDouble("z", z);
		if (dimension != null) {
			a.putString("dimension", dimension);
		}

		send("tp_position", a, null);
	}

	public static void afk(boolean on) {
		send("afk", bool("on", on), null);
	}

	public static void nick(String nick, @Nullable Consumer<ServerNet.Result> then) {
		send("nick", string("nick", nick), then);
	}

	public static void mailSend(String playerName, String text, @Nullable Consumer<ServerNet.Result> then) {
		CompoundTag a = string("player", playerName);
		a.putString("text", text);
		send("mail_send", a, then);
	}

	public static void mailRead(@Nullable String id) {
		send("mail_read", optionalString("id", id), r -> {
		});
	}

	public static void mailDelete(@Nullable String id) {
		send("mail_delete", optionalString("id", id), null);
	}

	public static void kitClaim(String kit, @Nullable Consumer<ServerNet.Result> then) {
		send("kit_claim", string("kit", kit), then);
	}

	public static void statsHide(boolean on) {
		send("stats_hide", bool("on", on), null);
	}

	public static void open(String what) {
		send("open", string("what", what), null);
	}

	public static void staffChat(String message) {
		send("staff_chat", string("message", message), null);
	}

	public static void sync() {
		send("sync", new CompoundTag(), r -> {
		});
	}

	public static void players(Consumer<Optional<List<PlayerRef>>> then) {
		ask("players", new CompoundTag(), PlayerRef[].class, answer -> then.accept(answer.map(refs -> {
				List<PlayerRef> out = new ArrayList<>(refs.length);

				for (PlayerRef r : refs) {
					if (r != null && r.id() != null && r.name() != null) {
						out.add(r);
					}
				}

				return out;
			})));
	}

	public static void stats(UUID player, Consumer<Optional<Snapshot>> then) {
		stats(player, then, null);
	}

	public static void stats(UUID player, Consumer<Optional<Snapshot>> then, @Nullable Consumer<ServerNet.Result> refused) {
		ask(
			"stats",
			id("id", player),
			Snapshot.class,
			answer -> then.accept(
					answer.map(
						s -> new Snapshot(s.player() == null ? player : s.player(), s.name() == null ? "" : s.name(), numbers(s.stats()), numbers(s.extras()), s.atMs())
					)
				),
			refused
		);
	}

	public static void board(String key, int offset, int count, Consumer<Optional<Board>> then) {
		CompoundTag a = string("key", key);
		a.putInt("offset", offset);
		a.putInt("count", count);
		ask("board", a, Board.class, answer -> then.accept(answer.map(b -> {
				List<Row> rows = new ArrayList<>();
				if (b.rows() != null) {
					for (Row r : b.rows()) {
						if (whole(r)) {
							rows.add(r);
						}
					}
				}

				return new Board(b.key() == null ? key : b.key(), rows, Math.max(b.total(), rows.size()), whole(b.self()) ? b.self() : null);
			})));
	}

	private static boolean whole(@Nullable Row row) {
		return row != null && row.player() != null && row.name() != null;
	}

	private static Map<String, Long> numbers(@Nullable Map<String, Long> sent) {
		Map<String, Long> out = new HashMap<>();
		if (sent != null) {
			sent.forEach((k, v) -> {
				if (k != null && v != null) {
					out.put(k, v);
				}
			});
		}

		return out;
	}

	public static void card(UUID player, Consumer<Optional<PlayerCard>> then) {
		ask("card", id("id", player), PlayerCard.class, then);
	}

	public static void punish(UUID player, Punishment.Kind kind, String reason, int seconds, @Nullable Consumer<ServerNet.Result> then) {
		CompoundTag a = id("id", player);
		a.putString("kind", kind.name());
		a.putString("reason", reason);
		if (seconds > 0) {
			a.putInt("seconds", seconds);
		}

		send("punish", a, then);
	}

	public static void pardon(UUID player, Punishment.Kind kind, @Nullable Consumer<ServerNet.Result> then) {
		CompoundTag a = id("id", player);
		a.putString("kind", kind.name());
		send("pardon", a, then);
	}

	public static void noteAdd(UUID player, String text, @Nullable Consumer<ServerNet.Result> then) {
		CompoundTag a = id("id", player);
		a.putString("text", text);
		send("note_add", a, then);
	}

	public static void noteDelete(UUID player, int index) {
		noteDelete(player, index, null);
	}

	public static void noteDelete(UUID player, int index, @Nullable Consumer<ServerNet.Result> then) {
		CompoundTag a = id("id", player);
		a.putInt("index", index);
		send("note_delete", a, then);
	}

	public static void playerDo(UUID player, String what, @Nullable String value, @Nullable Consumer<ServerNet.Result> then) {
		CompoundTag a = id("id", player);
		a.putString("what", what);
		if (value != null) {
			a.putString("value", value);
		}

		send("player_do", a, then);
	}

	public static void broadcast(String text, @Nullable Consumer<ServerNet.Result> then) {
		send("broadcast", string("text", text), then);
	}

	public static void adminWatch(boolean on) {
		ServerState.watching(on);
		send("admin_watch", bool("on", on), r -> {
		});
	}

	public static void adminPlayers(String query, String filter, int page, Consumer<Optional<AdminData.PlayerPage>> then) {
		CompoundTag a = string("query", query);
		a.putString("filter", filter);
		a.putInt("page", page);
		ask("admin_players", a, AdminData.PlayerPage.class, then);
	}

	public static void adminPlayer(UUID player, Consumer<Optional<AdminData.PlayerDetail>> then) {
		ask("admin_player", id("id", player), AdminData.PlayerDetail.class, then);
	}

	public static void adminRoles(Consumer<Optional<AdminData.RoleSet>> then) {
		ask("admin_roles", new CompoundTag(), AdminData.RoleSet.class, then);
	}

	public static void roleSave(Role role, @Nullable Consumer<ServerNet.Result> then) {
		send("role_save", string("role", JsonWire.GSON.toJson(role)), then);
	}

	public static void roleDelete(String roleId, @Nullable Consumer<ServerNet.Result> then) {
		send("role_delete", string("id", roleId), then);
	}

	public static void roleGrant(UUID player, String roleId, int seconds, @Nullable Consumer<ServerNet.Result> then) {
		CompoundTag a = id("id", player);
		a.putString("role", roleId);
		if (seconds > 0) {
			a.putInt("seconds", seconds);
		}

		send("role_grant", a, then);
	}

	public static void roleRevoke(UUID player, String roleId, @Nullable Consumer<ServerNet.Result> then) {
		CompoundTag a = id("id", player);
		a.putString("role", roleId);
		send("role_revoke", a, then);
	}

	public static void adminKits(Consumer<Optional<List<Kit>>> then) {
		ask("admin_kits", new CompoundTag(), Kit[].class, answer -> then.accept(answer.map(kits -> {
				List<Kit> out = new ArrayList<>(kits.length);

				for (Kit k : kits) {
					if (k != null) {
						out.add(k.sanitize());
					}
				}

				return out;
			})));
	}

	public static void kitSave(Kit kit, boolean fromInventory, @Nullable Consumer<ServerNet.Result> then) {
		CompoundTag a = string("kit", JsonWire.GSON.toJson(kit));
		a.putBoolean("from_inventory", fromInventory);
		send("kit_save", a, then);
	}

	public static void kitDelete(String kitId, @Nullable Consumer<ServerNet.Result> then) {
		send("kit_delete", string("id", kitId), then);
	}

	public static void kitGive(UUID player, String kitId, @Nullable Consumer<ServerNet.Result> then) {
		CompoundTag a = id("id", player);
		a.putString("kit", kitId);
		send("kit_give", a, then);
	}

	public static void adminPunishments(String filter, @Nullable Punishment.Kind kind, int page, Consumer<Optional<AdminData.PunishmentPage>> then) {
		CompoundTag a = string("filter", filter);
		if (kind != null) {
			a.putString("kind", kind.name());
		}

		a.putInt("page", page);
		ask("admin_punishments", a, AdminData.PunishmentPage.class, then);
	}

	public static void adminAudit(String query, int page, Consumer<Optional<AdminData.AuditPage>> then) {
		CompoundTag a = string("query", query);
		a.putInt("page", page);
		ask("admin_audit", a, AdminData.AuditPage.class, then);
	}

	public static void adminRules(Consumer<Optional<ServerRules>> then) {
		ask("admin_rules", new CompoundTag(), ServerRules.class, then);
	}

	public static void ruleSet(String rule, Object value, @Nullable Consumer<ServerNet.Result> then) {
		CompoundTag a = string("rule", rule);
		a.putString("value", JsonWire.GSON.toJson(value));
		send("rule_set", a, then);
	}

	public static void rulesReload(@Nullable Consumer<ServerNet.Result> then) {
		send("rules_reload", new CompoundTag(), then);
	}

	public static void adminWorld(Consumer<Optional<AdminData.WorldInfo>> then) {
		ask("admin_world", new CompoundTag(), AdminData.WorldInfo.class, then);
	}

	public static void worldSet(String what, @Nullable String key, String value, @Nullable String dimension, @Nullable Consumer<ServerNet.Result> then) {
		CompoundTag a = string("what", what);
		if (key != null) {
			a.putString("key", key);
		}

		a.putString("value", value);
		if (dimension != null) {
			a.putString("dimension", dimension);
		}

		send("world_set", a, then);
	}

	public static void send(String op, CompoundTag args, @Nullable Consumer<ServerNet.Result> then) {
		int request = nextRequest++;
		if (ServerState.preview()) {
			deliver(SampleServer.act(request, op, args), then);
		} else {
			CFHGuiNetwork net = CFHGuiNetwork.get();
			if (ServerState.connected() && net.serverHasChannel(ServerNet.Action.TYPE)) {
				if (then != null) {
					WAITING.put(request, then);
				}

				net.sendToServer(new ServerNet.Action(request, op, args));
			} else {
				deliver(ServerNet.Result.fail(request, "disabled"), then);
			}
		}
	}

	public static <T> void ask(String op, CompoundTag args, Class<T> type, Consumer<Optional<T>> then) {
		ask(op, args, type, then, null);
	}

	public static <T> void ask(String op, CompoundTag args, Class<T> type, Consumer<Optional<T>> then, @Nullable Consumer<ServerNet.Result> refused) {
		int request = nextRequest++;
		if (ServerState.preview()) {
			then.accept(Optional.ofNullable(SampleServer.answer(op, args, type)));
		} else {
			CFHGuiNetwork net = CFHGuiNetwork.get();
			if (ServerState.connected() && net.serverHasChannel(ServerNet.Action.TYPE)) {
				QUESTIONS.put(request, new ServerActions.Question<>(type, then, refused));
				net.sendToServer(new ServerNet.Action(request, op, args));
			} else {
				then.accept(Optional.empty());
			}
		}
	}

	public static void onReply(ServerNet.Reply reply) {
		ServerActions.Question<?> q = QUESTIONS.remove(reply.request());
		if (q != null) {
			q.answer(reply.data());
		}
	}

	public static void onResult(ServerNet.Result result) {
		ServerActions.Question<?> q = QUESTIONS.remove(result.request());
		if (q != null) {
			q.answer(null);
			deliver(result, q.refused());
		} else {
			deliver(result, WAITING.remove(result.request()));
		}
	}

	private static void deliver(ServerNet.Result result, @Nullable Consumer<ServerNet.Result> then) {
		if (then != null) {
			then.accept(result);
		} else {
			if (!result.ok()) {
				CFHGuiToasts.show(Component.translatable("cfhgui_server.result.failed"), describe(result), Icon.WARNING);
			}
		}
	}

	public static Component describe(ServerNet.Result result) {
		return Component.translatable("cfhgui_server.result." + result.code(), result.args().toArray());
	}

	static void reset() {
		WAITING.clear();
		Map<Integer, ServerActions.Question<?>> open = new HashMap<>(QUESTIONS);
		QUESTIONS.clear();

		for (ServerActions.Question<?> q : open.values()) {
			q.answer(null);
		}
	}

	private static CompoundTag string(String key, String value) {
		CompoundTag a = new CompoundTag();
		a.putString(key, value == null ? "" : value);
		return a;
	}

	private static CompoundTag optionalString(String key, @Nullable String value) {
		CompoundTag a = new CompoundTag();
		if (value != null) {
			a.putString(key, value);
		}

		return a;
	}

	private static CompoundTag bool(String key, boolean value) {
		CompoundTag a = new CompoundTag();
		a.putBoolean(key, value);
		return a;
	}

	private static CompoundTag id(String key, UUID value) {
		CompoundTag a = new CompoundTag();
		a.putUUID(key, value);
		return a;
	}

	private static CompoundTag optionalId(@Nullable UUID value) {
		CompoundTag a = new CompoundTag();
		if (value != null) {
			a.putUUID("id", value);
		}

		return a;
	}

	private ServerActions() {
	}

	private static record Question<T>(Class<T> type, Consumer<Optional<T>> then, @Nullable Consumer<ServerNet.Result> refused) {
		void answer(@Nullable byte[] data) {
			T value = (T)(data == null ? null : JsonWire.decode(data, this.type));
			this.then.accept(Optional.ofNullable(value));
		}
	}
}
