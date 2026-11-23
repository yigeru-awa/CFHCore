package com.onehumanawa.cfhcore.content.gui.server.service;

import com.google.gson.JsonParseException;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.server.CFHGuiServer;
import com.onehumanawa.cfhcore.content.gui.server.model.Kit;
import com.onehumanawa.cfhcore.content.gui.server.model.Punishment;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import com.onehumanawa.cfhcore.content.gui.server.net.ServerNet;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

final class Ops {
	private static final int MAX_TEXT = 1024;
	private static final int MAX_JSON = 30000;
	private static final long PLAYERS_GAP_MS = 250L;
	private static final long STATS_GAP_MS = 100L;
	private static final long READ_GAP_MS = 100L;
	private static final String[] PLAYER_IDS = new String[]{"id", "owner"};
	private final ServerService s;

	Ops(ServerService s) {
		this.s = s;
	}

	OpResult run(Actor a, @Nullable String op, @Nullable CompoundTag args) {
		if (op == null) {
			return OpResult.fail("unknown");
		} else {
			try {
				return this.dispatch(a, op, args == null ? new CompoundTag() : args);
			} catch (RuntimeException var5) {
				CFHGuiServer.LOGGER.warn("[CFHGui Server] the action {} of {} failed", new Object[]{op, a.name(), var5});
				return OpResult.fail("invalid");
			}
		}
	}

	private boolean readsTooFast(Actor a, CompoundTag t) {
		Session session = this.s.session(a);
		if (session == null) {
			return false;
		} else {
			String name = text(t, "player");
			UUID read = null;

			for (int i = 0; i <= PLAYER_IDS.length; i++) {
				UUID id = i < PLAYER_IDS.length ? uuid(t, PLAYER_IDS[i]) : (name == null ? null : this.s.store.idOf(name.trim()));
				if (id != null && !id.equals(read) && !this.s.store.inMemory(id)) {
					if (read != null) {
						return true;
					}

					read = id;
				}
			}

			return read != null && !session.allow("read", 100L, this.s.now());
		}
	}

	private OpResult dispatch(Actor a, String op, CompoundTag t) {
		if (this.readsTooFast(a, t)) {
			return OpResult.fail("too_fast");
		} else {
			return switch (op) {
				case "home_set" -> this.s.travel.homeSet(a, text(t, "name"), text(t, "icon"), integer(t, "color"));
				case "home_edit" -> this.s.travel.homeEdit(a, string(t, "name"), text(t, "rename"), text(t, "icon"), integer(t, "color"));
				case "home_delete" -> {
					UUID owner = uuid(t, "owner");
					ServerService.Target target = owner == null ? null : this.s.target(owner, null);
					yield owner != null && target == null ? OpResult.fail("no_such_player", owner) : this.s.travel.homeDelete(a, string(t, "name"), target);
				}
				case "home_go" -> {
					UUID owner = uuid(t, "owner");
					ServerService.Target target = owner == null ? null : this.s.target(owner, null);
					yield owner != null && target == null ? OpResult.fail("no_such_player", owner) : this.s.travel.homeGo(a, text(t, "name"), target);
				}
				case "back" -> this.s.travel.back(a);
				case "spawn_go" -> this.s.travel.spawnGo(a);
				case "spawn_set" -> this.s.travel.spawnSet(a);
				case "warp_go" -> this.s.travel.warpGo(a, string(t, "name"));
				case "warp_set" -> this.s.travel.warpSet(a, string(t, "name"), text(t, "icon"), integer(t, "color"), text(t, "description"), bool(t, "restricted"));
				case "warp_edit" -> this.s
				.travel
				.warpEdit(a, string(t, "name"), text(t, "rename"), text(t, "icon"), integer(t, "color"), text(t, "description"), bool(t, "restricted"));
				case "warp_delete" -> this.s.travel.warpDelete(a, string(t, "name"));
				case "tpa" -> this.s.requests.ask(a, this.target(t), wanted(t), Boolean.TRUE.equals(bool(t, "here")));
				case "tpa_accept" -> this.s.requests.accept(a, uuid(t, "id"));
				case "tpa_deny" -> this.s.requests.deny(a, uuid(t, "id"));
				case "tpa_cancel" -> this.s.requests.cancel(a, uuid(t, "id"));
				case "tpa_toggle" -> this.s.requests.toggle(a, bool(t, "on"));
				case "rtp" -> this.s.travel.random(a, text(t, "dimension"));
				case "tp_cancel" -> this.s.travel.cancel(a);
				case "tp_position" -> t.contains("x", 99) && t.contains("z", 99)
				? this.s.travel.position(a, t.getDouble("x"), t.contains("y", 99) ? t.getDouble("y") : null, t.getDouble("z"), text(t, "dimension"))
				: OpResult.fail("invalid");
				case "afk" -> this.s.people.afk(a, bool(t, "on"));
				case "nick" -> this.s.people.nick(a, string(t, "nick"));
				case "mail_send" -> this.s.people.mailSend(a, this.target(t), wanted(t), string(t, "text"));
				case "mail_read" -> this.s.people.mailRead(a, text(t, "id"));
				case "mail_delete" -> this.s.people.mailDelete(a, text(t, "id"));
				case "kit_claim" -> this.s.people.kitClaim(a, string(t, "kit"));
				case "stats_hide" -> this.s.people.statsHide(a, Boolean.TRUE.equals(bool(t, "on")));
				case "open" -> this.s.people.open(a, string(t, "what"));
				case "staff_chat" -> this.s.moderation.staffChat(a, string(t, "message"));
				case "sync" -> {
					Session session = this.s.session(a);
					if (session == null) {
						yield OpResult.fail("player_only");
					} else {
						this.s.sync.joined(session);
						yield OpResult.OK;
					}
				}
				case "players" -> {
					Session session = this.s.session(a);
					yield session != null && !session.allow("players", 250L, this.s.now()) ? OpResult.fail("too_fast") : this.s.statistics.players(a);
				}
				case "stats" -> {
					Session asking = this.s.session(a);
					if (asking != null && !asking.allow("stats", 100L, this.s.now())) {
						yield OpResult.fail("too_fast");
					} else {
						ServerService.Target who = !t.hasUUID("id") && !t.contains("player", 8) ? this.s.target(a.id(), null) : this.target(t);
						yield this.s.statistics.stats(a, who, wanted(t));
					}
				}
				case "board" -> this.s.statistics.board(a, string(t, "key"), t.getInt("offset"), t.contains("count", 99) ? t.getInt("count") : 10);
				case "card" -> this.s.people.card(a, this.target(t), wanted(t));
				case "punish" -> this.s
				.moderation
				.punish(a, this.target(t), wanted(t), Punishment.Kind.parse(text(t, "kind")), text(t, "reason"), Math.max(0, t.getInt("seconds")));
				case "pardon" -> this.s.moderation.pardon(a, this.target(t), wanted(t), Punishment.Kind.parse(text(t, "kind")));
				case "note_add" -> this.s.moderation.noteAdd(a, this.target(t), wanted(t), string(t, "text"));
				case "note_delete" -> this.s.moderation.noteDelete(a, this.target(t), wanted(t), t.contains("index", 99) ? t.getInt("index") : -1);
				case "player_do" -> this.s.moderation.playerDo(a, this.target(t), wanted(t), text(t, "what"), text(t, "value"));
				case "broadcast" -> this.s.moderation.broadcast(a, string(t, "text"));
				case "admin_watch" -> this.s.admin.watch(a, Boolean.TRUE.equals(bool(t, "on")));
				case "admin_players" -> this.s.admin.players(a, text(t, "query"), text(t, "filter"), t.getInt("page"));
				case "admin_player" -> this.s.admin.player(a, this.target(t), wanted(t));
				case "admin_roles" -> this.s.admin.roles(a);
				case "role_save" -> this.s.admin.roleSave(a, json(t, "role", Role.class));
				case "role_delete" -> this.s.admin.roleDelete(a, string(t, "id"));
				case "role_grant" -> this.s.admin.roleGrant(a, this.target(t), wanted(t), string(t, "role"), Math.max(0, t.getInt("seconds")));
				case "role_revoke" -> this.s.admin.roleRevoke(a, this.target(t), wanted(t), string(t, "role"));
				case "admin_kits" -> this.s.admin.kits(a);
				case "kit_save" -> this.s.admin.kitSave(a, json(t, "kit", Kit.class), Boolean.TRUE.equals(bool(t, "from_inventory")));
				case "kit_delete" -> this.s.admin.kitDelete(a, string(t, "id"));
				case "kit_give" -> this.s.admin.kitGive(a, this.target(t), wanted(t), string(t, "kit"));
				case "admin_punishments" -> this.s.admin.punishments(a, text(t, "filter"), Punishment.Kind.parse(text(t, "kind")), t.getInt("page"));
				case "admin_audit" -> this.s.admin.audit(a, text(t, "query"), t.getInt("page"));
				case "admin_rules" -> this.s.admin.rules(a);
				case "rule_set" -> this.s.admin.ruleSet(a, text(t, "rule"), raw(t, "value"));
				case "rules_reload" -> this.s.admin.rulesReload(a);
				case "admin_world" -> this.s.admin.world(a);
				case "world_set" -> this.s.admin.worldSet(a, text(t, "what"), text(t, "key"), text(t, "value"), text(t, "dimension"));
				default -> OpResult.fail("unknown");
			};
		}
	}

	void answer(ServerPlayer player, ServerNet.Action action, OpResult result) {
		if (result.ok() && result.data() != null) {
			ServerNet.send(player, ServerNet.Reply.of(action.request(), action.op(), result.data()));
		}

		ServerNet.send(player, result.payload(action.request()));
	}

	@Nullable
	private ServerService.Target target(CompoundTag t) {
		UUID id = uuid(t, "id");
		return id != null ? this.s.target(id, null) : this.s.target(null, text(t, "player"));
	}

	private static String wanted(CompoundTag t) {
		String name = text(t, "player");
		if (name != null) {
			return name;
		} else {
			UUID id = uuid(t, "id");
			return id == null ? "" : id.toString();
		}
	}

	@Nullable
	private static String text(CompoundTag t, String key) {
		if (!t.contains(key, 8)) {
			return null;
		} else {
			String v = t.getString(key);
			return v.length() > 1024 ? v.substring(0, 1024) : v;
		}
	}

	private static String string(CompoundTag t, String key) {
		String v = text(t, key);
		return v == null ? "" : v;
	}

	@Nullable
	private static String raw(CompoundTag t, String key) {
		if (!t.contains(key, 8)) {
			return null;
		} else {
			String v = t.getString(key);
			return v.length() > 30000 ? null : v;
		}
	}

	@Nullable
	private static <T> T json(CompoundTag t, String key, Class<T> type) {
		String v = raw(t, key);
		if (v == null) {
			return null;
		} else {
			try {
				return (T)JsonWire.GSON.fromJson(v, type);
			} catch (JsonParseException var5) {
				return null;
			}
		}
	}

	@Nullable
	private static Integer integer(CompoundTag t, String key) {
		return t.contains(key, 99) ? t.getInt(key) : null;
	}

	@Nullable
	private static Boolean bool(CompoundTag t, String key) {
		return t.contains(key, 99) ? t.getBoolean(key) : null;
	}

	@Nullable
	private static UUID uuid(CompoundTag t, String key) {
		return t.hasUUID(key) ? t.getUUID(key) : null;
	}
}
