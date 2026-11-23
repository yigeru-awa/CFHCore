package com.onehumanawa.cfhcore.content.gui.server.service;

import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.mixin.server.LevelTickersAccessor;
import com.onehumanawa.cfhcore.content.gui.server.model.AdminData;
import com.onehumanawa.cfhcore.content.gui.server.model.Home;
import com.onehumanawa.cfhcore.content.gui.server.model.Kit;
import com.onehumanawa.cfhcore.content.gui.server.model.KitBook;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import com.onehumanawa.cfhcore.content.gui.server.model.Punishment;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import com.onehumanawa.cfhcore.content.gui.server.model.RoleBook;
import com.onehumanawa.cfhcore.content.gui.server.model.ServerRules;
import com.onehumanawa.cfhcore.content.gui.server.net.ServerNet;
import com.onehumanawa.cfhcore.content.gui.server.permission.RolePerms;
import com.onehumanawa.cfhcore.content.gui.server.platform.ServerPlatform;
import com.onehumanawa.cfhcore.content.gui.server.storage.WorldStore;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.GameRules.GameRuleTypeVisitor;
import net.minecraft.world.level.GameRules.IntegerValue;
import net.minecraft.world.level.GameRules.Key;
import net.minecraft.world.level.GameRules.Type;
import org.jetbrains.annotations.Nullable;

public final class Admin {
	private static final Pattern PATTERN = Pattern.compile("\\*|[a-z0-9_]+(\\.[a-z0-9_-]+)*(\\.\\*)?");
	private static final int MAX_PATTERNS = 512;
	private static final int MAX_ROLES = 64;
	private static final int MAX_KITS = 128;
	private static final int MAX_KIT_ITEMS = 64;
	private static final int MAX_ITEM_CHARS = 30000;
	private static final int MAX_PUNISHED = Math.min(100, 112);
	static final Set<String> KIT_WORDS = Set.of("create", "delete", "give", "list", "info");
	private final ServerService s;
	private final float[] msptHistory = new float[60];
	private final float[] msptWorst = new float[60];
	private int seconds;
	private final long[] tickEnds = new long[1200];
	private int tickEnd;

	Admin(ServerService s) {
		this.s = s;
	}

	public OpResult watch(Actor a, boolean on) {
		if (!this.s.may(a, Nodes.ADMIN_DASHBOARD)) {
			return OpResult.fail("no_permission");
		} else {
			Session session = this.s.session(a);
			if (session == null) {
				return OpResult.fail("player_only");
			} else {
				session.watching = on;
				if (on && this.s.live && session.player != null) {
					ServerNet.send(session.player, ServerNet.State.of("dashboard", this.dashboard()));
				}

				return OpResult.OK;
			}
		}
	}

	void tickEnded() {
		this.tickEnds[this.tickEnd] = System.nanoTime();
		this.tickEnd = (this.tickEnd + 1) % this.tickEnds.length;
	}

	void tickDashboard() {
		if (this.s.live) {
			long[] times = this.s.server.getTickTimesNanos();
			double sum = 0.0;
			double worst = 0.0;
			int n = Math.min(20, times.length);
			int at = this.s.server.getTickCount();

			for (int i = 0; i < n; i++) {
				double ms = (double)times[Math.floorMod(at - 1 - i, times.length)] / 1000000.0;
				sum += ms;
				worst = Math.max(worst, ms);
			}

			System.arraycopy(this.msptHistory, 1, this.msptHistory, 0, this.msptHistory.length - 1);
			System.arraycopy(this.msptWorst, 1, this.msptWorst, 0, this.msptWorst.length - 1);
			this.msptHistory[this.msptHistory.length - 1] = n == 0 ? 0.0F : (float)(sum / (double)n);
			this.msptWorst[this.msptWorst.length - 1] = (float)worst;
			this.seconds = Math.min(60, this.seconds + 1);
			AdminData.Dashboard d = null;

			for (Session session : this.s.online()) {
				if (session.watching && session.player != null) {
					if (!this.s.may(session.actor(), Nodes.ADMIN_DASHBOARD)) {
						session.watching = false;
					} else {
						if (d == null) {
							d = this.dashboard();
						}

						ServerNet.send(session.player, ServerNet.State.of("dashboard", d));
					}
				}
			}
		}
	}

	private double tps(int spanSeconds) {
		long now = System.nanoTime();
		long from = now - (long)spanSeconds * 1000000000L;
		int count = 0;
		long oldest = now;

		for (long end : this.tickEnds) {
			if (end != 0L && end >= from) {
				count++;
				oldest = Math.min(oldest, end);
			}
		}

		if (count < 2) {
			return 20.0;
		} else {
			double span = Math.max((double)(now - oldest) / 1.0E9, 0.05);
			return Math.min(20.0, (double)Math.round((double)count / span * 10.0) / 10.0);
		}
	}

	AdminData.Dashboard dashboard() {
		AdminData.Dashboard d = new AdminData.Dashboard();
		d.tps = this.tps(5);
		d.tpsMinute = this.tps(60);
		int shown = Math.max(1, this.seconds);
		d.msptHistory = new float[shown];
		System.arraycopy(this.msptHistory, this.msptHistory.length - shown, d.msptHistory, 0, shown);
		d.mspt = (double)Math.round((double)this.msptHistory[this.msptHistory.length - 1] * 100.0) / 100.0;
		double worst = 0.0;

		for (int i = this.msptWorst.length - shown; i < this.msptWorst.length; i++) {
			worst = Math.max(worst, (double)this.msptWorst[i]);
		}

		d.msptWorst = (double)Math.round(worst * 100.0) / 100.0;
		d.players = this.s.server.getPlayerCount();
		d.maxPlayers = this.s.server.getMaxPlayers();
		Runtime rt = Runtime.getRuntime();
		d.memoryTotal = rt.totalMemory() >> 20;
		d.memoryUsed = rt.totalMemory() - rt.freeMemory() >> 20;
		d.memoryMax = rt.maxMemory() >> 20;
		d.uptimeMs = this.s.uptimeMs();

		for (ServerLevel level : this.s.server.getAllLevels()) {
			AdminData.Dimension dim = new AdminData.Dimension();
			dim.id = level.dimension().location().toString();
			dim.players = level.players().size();
			int entities = 0;

			for (Entity ignored : level.getAllEntities()) {
				entities++;
			}

			dim.entities = entities;
			dim.chunks = level.getChunkSource().getLoadedChunksCount();
			dim.blockEntities = ((LevelTickersAccessor)level).cfhServer$blockEntityTickers().size();
			d.dimensions.add(dim);
		}

		d.maintenance = this.s.rules().maintenance;
		String other = ServerPlatform.get().externalPermissions();
		d.externalPermissions = other == null ? "" : other;
		return d;
	}

	public OpResult players(Actor a, @Nullable String query, @Nullable String filter, int page) {
		if (!this.s.may(a, Nodes.ADMIN_PLAYERS)) {
			return OpResult.fail("no_permission");
		} else {
			String q = query == null ? "" : Text.line(query, 32).toLowerCase(Locale.ROOT);
			String f = filter != null && !filter.isBlank() ? filter.trim().toLowerCase(Locale.ROOT) : "all";
			if (!Set.of("all", "online", "offline", "muted", "banned", "frozen").contains(f)) {
				return OpResult.fail("invalid");
			} else {
				long now = this.s.now();
				List<WorldStore.Known> matching = new ArrayList<>();

				for (WorldStore.Known k : this.s.store.all()) {
					if (q.isEmpty() || k.name.toLowerCase(Locale.ROOT).contains(q) || Text.plainNick(k.nick).toLowerCase(Locale.ROOT).contains(q)) {
						boolean online = this.shownOnline(a, k);

						boolean keep = switch (f) {
							case "online" -> online;
							case "offline" -> !online;
							case "muted" -> k.muted(now);
							case "banned" -> k.banned(now);
							case "frozen" -> k.frozen(now);
							default -> true;
						};
						if (keep) {
							matching.add(k);
						}
					}
				}

				matching.sort(
					Comparator.<WorldStore.Known, Boolean>comparing(kx -> !this.shownOnline(a, kx))
						.thenComparing(Comparator.<WorldStore.Known>comparingLong(kx -> this.s.lastSeen(a, kx)).reversed())
				);
				AdminData.PlayerPage out = new AdminData.PlayerPage();
				out.page = Math.max(0, page);
				out.total = matching.size();
				int from = Math.min(matching.size(), out.page * 50);
				int to = Math.min(matching.size(), from + 50);

				for (int i = from; i < to; i++) {
					out.rows.add(this.row(a, matching.get(i), now));
				}

				return OpResult.answer(out);
			}
		}
	}

	private boolean shownOnline(Actor viewer, WorldStore.Known k) {
		Session session = this.s.session(k.id);
		return session != null && this.s.visible(viewer, session);
	}

	private AdminData.PlayerRow row(Actor viewer, WorldStore.Known k, long now) {
		AdminData.PlayerRow row = new AdminData.PlayerRow();
		Session session = this.s.session(k.id);
		boolean shown = session != null && this.s.visible(viewer, session);
		row.id = k.id;
		row.name = k.name;
		row.nick = k.nick;
		row.role = Sync.tag(this.s.perms.held(k.id, this.s.levelOf(k.id, k.name)));
		row.online = shown;
		row.afk = shown && session.afk;
		row.vanished = shown && session.record.vanished;
		row.muted = k.muted(now);
		row.banned = k.banned(now);
		row.frozen = k.frozen(now);
		row.lastSeen = this.s.lastSeen(viewer, k);
		ServerPlayer p = shown ? session.player : null;
		row.dimension = p == null ? "" : p.level().dimension().location().toString();
		return row;
	}

	public OpResult player(Actor a, @Nullable ServerService.Target t, String wanted) {
		if (!this.s.may(a, Nodes.ADMIN_PLAYERS)) {
			return OpResult.fail("no_permission");
		} else if (t == null) {
			return OpResult.fail("no_such_player", Text.line(wanted, 40));
		} else {
			PlayerRecord rec = t.record();
			long now = this.s.now();
			Session session = t.session();
			boolean shown = session != null && this.s.visible(a, session);
			this.reconcileBan(t);
			WorldStore.Known k = this.s.store.known(t.id());
			AdminData.PlayerDetail d = new AdminData.PlayerDetail();
			if (k != null) {
				d.row = this.row(a, k, now);
			}

			d.firstSeen = rec.firstSeen;
			d.sessions = rec.sessions;
			d.playTicks = this.s.statistics.value(t.id(), StatsLink.key("minecraft:custom", "minecraft:play_time"), session != null && !shown);
			d.afkTicks = rec.afkTicks;
			d.place = shown ? session.place() : rec.lastPlace;
			d.death = rec.death;

			for (Home h : rec.homes) {
				d.homes.add(Sync.copy(h));
			}

			d.roles = new ArrayList<>(rec.roles);
			if (this.s.may(a, Nodes.MOD_HISTORY)) {
				d.punishments = new ArrayList<>(rec.punishments);
				d.notes = new ArrayList<>(this.s.moderation.notesFor(a, t));
			}

			ServerPlayer p = shown ? session.player : null;
			if (p != null) {
				d.health = p.getHealth();
				d.maxHealth = p.getMaxHealth();
				d.food = p.getFoodData().getFoodLevel();
				d.level = p.experienceLevel;
				d.ping = p.connection.latency();
				d.gameMode = p.gameMode.getGameModeForPlayer().getName();
			}

			d.god = rec.god;
			d.fly = rec.fly;
			d.operator = this.s.levelOf(t.id(), t.name()) >= 2;
			d.mayActOn = this.s.outranks(a, t);
			return OpResult.answer(d);
		}
	}

	private void reconcileBan(ServerService.Target t) {
		if (this.s.live) {
			long now = this.s.now();
			Punishment ban = t.record().active(Punishment.Kind.BAN, now);
			if (ban != null && !this.s.server.getPlayerList().getBans().isBanned(new GameProfile(t.id(), t.name()))) {
				ban.liftedAt = now;
				ban.liftedByName = "/pardon";
				this.s.touch(t.record());
			}
		}
	}

	public OpResult roles(Actor a) {
		if (!this.s.may(a, Nodes.ADMIN_ROLES)) {
			return OpResult.fail("no_permission");
		} else {
			RoleBook book = this.s.roles();
			AdminData.RoleSet out = new AdminData.RoleSet();
			out.roles = new ArrayList<>(book.roles);

			for (Node node : Perms.declared()) {
				AdminData.NodeInfo info = new AdminData.NodeInfo();
				info.id = node.id();
				info.level = node.defaultLevel();
				info.number = node.number();
				info.defaultNumber = node.defaultNumber();
				info.description = describe(node);
				out.nodes.add(info);
			}

			Map<String, Integer> holders = new HashMap<>();

			for (WorldStore.Known k : this.s.store.all()) {
				for (String id : k.roles) {
					holders.merge(id, 1, Integer::sum);
				}
			}

			for (Role role : book.roles) {
				out.holders.add(holders.getOrDefault(role.id, 0));
			}

			out.editableBelow = this.s.perms.rank(a);
			String other = ServerPlatform.get().externalPermissions();
			out.externalPermissions = other == null ? "" : other;
			return OpResult.answer(out);
		}
	}

	static String describe(Node node) {
		String key = node.descriptionKey();
		if (node.id().startsWith("cfhgui_server.warp.to.")) {
			key = "cfhgui_server.perm.warp.to";
		} else if (node.id().startsWith("cfhgui_server.kit.use.")) {
			key = "cfhgui_server.perm.kit.use.restricted";
		}

		String english = Msg.english(key);
		return english == null ? "" : english;
	}

	public OpResult roleSave(Actor a, @Nullable Role given) {
		if (!this.s.may(a, Nodes.ADMIN_ROLES)) {
			return OpResult.fail("no_permission");
		} else if (given == null) {
			return OpResult.fail("invalid");
		} else {
			given.sanitize();
			String id = Text.id(given.id.isBlank() ? Role.idOf(given.name) : given.id);
			if (id == null) {
				return OpResult.fail("name_invalid");
			} else {
				RoleBook book = this.s.roles();
				Role old = book.get(id);
				if (old == null && book.roles.size() >= 64) {
					return OpResult.fail("invalid");
				} else {
					Role role = new Role();
					role.id = id;
					role.name = Text.line(given.name, 24);
					if (role.name.isEmpty()) {
						role.name = id;
					}

					role.color = given.color & 16777215;
					role.prefix = Text.line(given.prefix, 16);
					role.priority = Math.max(-1000, Math.min(1000, given.priority));
					role.everyone = given.everyone;
					role.operators = given.operators;

					for (String other : given.inherits) {
						String otherId = Text.id(other);
						if (otherId != null && !otherId.equals(id) && book.get(otherId) != null && !role.inherits.contains(otherId)) {
							role.inherits.add(otherId);
						}
					}

					OpResult refused = patterns(given.allow, role.allow);
					if (refused == null) {
						refused = patterns(given.deny, role.deny);
					}

					if (refused != null) {
						return refused;
					} else {
						for (Entry<String, Integer> e : given.numbers.entrySet()) {
							if (role.numbers.size() < 512 && e.getKey() != null && PATTERN.matcher(e.getKey()).matches() && !e.getKey().contains("*")) {
								role.numbers.put(e.getKey(), Math.max(0, Math.min(1000000, e.getValue())));
							}
						}

						if (this.s.perms.outranks(a, role.priority) && (old == null || this.s.perms.outranks(a, old.priority))) {
							for (String inherited : role.inherits) {
								if (!this.s.perms.outranks(a, book.get(inherited).priority)) {
									return OpResult.fail("role_too_high");
								}
							}

							if (this.s.perms.rank(a) != Integer.MAX_VALUE) {
								String beyond = this.beyondOwn(a, RolePerms.withInherited(book, role), old == null ? List.of() : RolePerms.withInherited(book, old));
								if (beyond != null) {
									return OpResult.fail("role_beyond_own", beyond);
								}

								RoleBook after = new RoleBook();
								after.roles.addAll(book.roles);
								if (old != null) {
									after.roles.set(after.roles.indexOf(old), role);
								} else {
									after.roles.add(role);
								}

								String taken = this.takesFromAbove(a, after.sanitize());
								if (taken != null) {
									return OpResult.fail("role_takes_from_above", taken);
								}
							}

							if (old != null) {
								book.roles.set(book.roles.indexOf(old), role);
							} else {
								book.roles.add(role);
							}

							book.sanitize();
							this.s.books.saveRoles();
							this.s.audit(a, "role_save", null, "", role.id);
							this.s.permissionsChanged(null);
							return OpResult.ok(role.id);
						} else {
							return OpResult.fail("role_too_high");
						}
					}
				}
			}
		}
	}

	@Nullable
	private static Boolean says(List<Role> order, String node) {
		for (Role role : order) {
			Boolean b = role.says(node);
			if (b != null) {
				return b;
			}
		}

		return null;
	}

	@Nullable
	private String beyondOwn(Actor a, List<Role> order, List<Role> before) {
		for (Node node : Perms.declared()) {
			if (!node.number() && Boolean.TRUE.equals(says(order, node.id())) && !Boolean.TRUE.equals(says(before, node.id())) && !this.s.may(a, node)) {
				return node.id();
			}
		}

		return null;
	}

	@Nullable
	private String takesFromAbove(Actor a, final RoleBook after) {
		RolePerms then = new RolePerms(new RolePerms.Source() {
			@Override
			public RoleBook roles() {
				return after;
			}

			@Override
			public ServerRules rules() {
				return Admin.this.s.rules();
			}

			@Override
			public List<String> grants(UUID player) {
				return Admin.this.s.grants(player);
			}

			@Override
			public long validUntil(UUID player) {
				return Admin.this.s.validUntil(player);
			}

			@Override
			public long now() {
				return Admin.this.s.now();
			}
		}, false);
		List<Node> nodes = Perms.declared();
		String taken = this.taken(a, then, Actor.NOBODY, 4, false, nodes);
		if (taken != null) {
			return taken;
		} else {
			boolean[] asked = new boolean[10];
			asked[8] = true;

			for (WorldStore.Known k : this.s.store.all()) {
				if (!k.id.equals(a.id())) {
					int level = Math.max(0, Math.min(4, this.s.levelOf(k.id, k.name)));
					boolean owner = this.s.ownerOf(k.id, k.name);
					boolean plain = k.roles.isEmpty();
					if (plain) {
						int kind = level * 2 + (owner ? 1 : 0);
						if (asked[kind]) {
							continue;
						}

						asked[kind] = true;
					}

					taken = this.taken(a, then, plain ? Actor.NOBODY : k.id, level, owner, nodes);
					if (taken != null) {
						return taken;
					}
				}
			}

			return null;
		}
	}

	@Nullable
	private String taken(Actor a, RolePerms then, UUID id, int level, boolean owner, List<Node> nodes) {
		if (this.s.perms.outranks(a, this.s.perms.rank(id, level, owner))) {
			return null;
		} else {
			for (Node node : nodes) {
				if (!node.number()
					&& Boolean.TRUE.equals(this.s.perms.hasOffline(id, level, owner, node.id()))
					&& !Boolean.TRUE.equals(then.hasOffline(id, level, owner, node.id()))) {
					return node.id();
				}
			}

			return null;
		}
	}

	@Nullable
	private static OpResult patterns(List<String> from, List<String> to) {
		for (String raw : from) {
			String p = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
			if (!p.isEmpty() && !to.contains(p)) {
				if (p.length() > 96 || !PATTERN.matcher(p).matches()) {
					return OpResult.fail("pattern_invalid", Text.line(p, 96));
				}

				if (to.size() >= 512) {
					return OpResult.fail("invalid");
				}

				to.add(p);
			}
		}

		return null;
	}

	public OpResult roleDelete(Actor a, String rawId) {
		if (!this.s.may(a, Nodes.ADMIN_ROLES)) {
			return OpResult.fail("no_permission");
		} else {
			RoleBook book = this.s.roles();
			Role role = rawId == null ? null : book.get(rawId.trim().toLowerCase(Locale.ROOT));
			if (role == null) {
				return OpResult.fail("no_such_role", Text.line(rawId, 24));
			} else if (role.id.equals("default")) {
				return OpResult.fail("role_protected");
			} else if (!this.s.perms.outranks(a, role.priority)) {
				return OpResult.fail("role_too_high");
			} else {
				if (this.s.perms.rank(a) != Integer.MAX_VALUE) {
					RoleBook after = new RoleBook();
					after.roles.addAll(book.roles);
					after.roles.remove(role);
					String taken = this.takesFromAbove(a, after);
					if (taken != null) {
						return OpResult.fail("role_takes_from_above", taken);
					}
				}

				book.roles.remove(role);

				for (Role other : book.roles) {
					other.inherits.remove(role.id);
				}

				this.s.books.saveRoles();
				this.s.audit(a, "role_delete", null, "", role.id);

				for (Session session : new ArrayList<>(this.s.online())) {
					this.s.dropExpiredGrants(session.record, session);
				}

				this.s.permissionsChanged(null);
				return OpResult.ok(role.id);
			}
		}
	}

	public OpResult roleGrant(Actor a, @Nullable ServerService.Target t, String wanted, String rawRole, int seconds) {
		if (!this.s.may(a, Nodes.ADMIN_ROLES)) {
			return OpResult.fail("no_permission");
		} else if (seconds < 0) {
			return OpResult.fail("invalid");
		} else if (t == null) {
			return OpResult.fail("no_such_player", Text.line(wanted, 32));
		} else if (t.byNickname()) {
			return this.s.nicknamed(t);
		} else {
			Role role = rawRole == null ? null : this.s.roles().get(rawRole.trim().toLowerCase(Locale.ROOT));
			if (role == null) {
				return OpResult.fail("no_such_role", Text.line(rawRole, 24));
			} else if (!this.s.perms.outranks(a, role.priority)) {
				return OpResult.fail("role_too_high");
			} else if (!t.id().equals(a.id()) && !this.s.outranks(a, t)) {
				return OpResult.fail("outranked", t.name());
			} else {
				if (this.s.perms.rank(a) != Integer.MAX_VALUE) {
					String beyond = this.beyondOwn(a, RolePerms.withInherited(this.s.roles(), role), List.of());
					if (beyond != null) {
						return OpResult.fail("role_beyond_own", beyond);
					}
				}

				PlayerRecord rec = t.record();
				long now = this.s.now();
				long expiresAt = seconds > 0 ? now + (long)Math.min(seconds, 315360000) * 1000L : 0L;
				Role.Grant grant = null;

				for (Role.Grant g : rec.roles) {
					if (g.role.equals(role.id) && !g.expired(now)) {
						grant = g;
					}
				}

				if (grant == null || !role.everyone && grant.expiresAt != expiresAt && (grant.expiresAt != 0L || expiresAt != 0L)) {
					if (grant == null && role.everyone) {
						return OpResult.fail("already_has_role", t.name());
					} else {
						if (grant == null) {
							grant = new Role.Grant();
							grant.role = role.id;
							rec.roles.add(grant);
						}

						grant.expiresAt = expiresAt;
						grant.by = a.console() ? Actor.NOBODY : a.id();
						grant.byName = a.name();
						grant.at = now;
						this.s.touch(rec);
						String length = seconds > 0 ? Text.span((long)Math.min(seconds, 315360000)) : "";
						this.s.audit(a, "role_grant", t.id(), t.name(), length.isEmpty() ? role.id : role.id + " (" + length + ")");
						if (t.session() != null) {
							this.s.notice(t.session(), "role_granted", role.color, null, role.name);
						}

						this.s.permissionsChanged(t.id());
						return OpResult.ok(t.name(), role.name, length);
					}
				} else {
					return OpResult.fail("already_has_role", t.name());
				}
			}
		}
	}

	public OpResult roleRevoke(Actor a, @Nullable ServerService.Target t, String wanted, String rawRole) {
		if (!this.s.may(a, Nodes.ADMIN_ROLES)) {
			return OpResult.fail("no_permission");
		} else if (t == null) {
			return OpResult.fail("no_such_player", Text.line(wanted, 32));
		} else if (t.byNickname()) {
			return this.s.nicknamed(t);
		} else {
			String id = rawRole == null ? "" : rawRole.trim().toLowerCase(Locale.ROOT);
			Role role = this.s.roles().get(id);
			if (role != null && !this.s.perms.outranks(a, role.priority)) {
				return OpResult.fail("role_too_high");
			} else if (!t.id().equals(a.id()) && !this.s.outranks(a, t)) {
				return OpResult.fail("outranked", t.name());
			} else {
				PlayerRecord rec = t.record();
				if (!rec.roles.removeIf(g -> g.role.equals(id))) {
					return role == null ? OpResult.fail("no_such_role", Text.line(rawRole, 24)) : OpResult.fail("role_not_held", t.name());
				} else {
					this.s.touch(rec);
					this.s.audit(a, "role_revoke", t.id(), t.name(), id);
					if (t.session() != null) {
						this.s.notice(t.session(), "role_revoked", role == null ? 0 : role.color, null, role == null ? id : role.name);
					}

					this.s.permissionsChanged(t.id());
					return OpResult.ok(t.name(), role == null ? id : role.name);
				}
			}
		}
	}

	public OpResult kits(Actor a) {
		return !this.s.may(a, Nodes.ADMIN_KITS) ? OpResult.fail("no_permission") : OpResult.answer(this.s.kits().kits.toArray(new Kit[0]));
	}

	public OpResult kitSave(Actor a, @Nullable Kit given, boolean fromInventory) {
		if (!this.s.may(a, Nodes.ADMIN_KITS)) {
			return OpResult.fail("no_permission");
		} else if (given == null) {
			return OpResult.fail("invalid");
		} else {
			given.sanitize();
			String id = Text.id(given.id.isBlank() ? Role.idOf(given.name) : given.id);
			if (id != null && !KIT_WORDS.contains(id)) {
				KitBook book = this.s.kits();
				Kit old = book.get(id);
				if (old == null && book.kits.size() >= 128) {
					return OpResult.fail("invalid");
				} else {
					Kit kit = new Kit();
					kit.id = id;
					kit.name = Text.line(given.name, 32);
					if (kit.name.isEmpty()) {
						kit.name = id;
					}

					kit.icon = Travel.icon(given.icon);
					kit.cooldownSeconds = Math.max(0, Math.min(315360000, given.cooldownSeconds));
					kit.oneTime = given.oneTime;
					kit.firstJoin = given.firstJoin;
					kit.restricted = given.restricted;
					Session session = this.s.session(a);
					ServerPlayer player = session == null ? null : session.player;
					if (fromInventory) {
						if (player == null) {
							return OpResult.fail("player_only");
						}

						List<ItemStack> carried = new ArrayList<>();
						carried.addAll(player.getInventory().items);
						carried.addAll(player.getInventory().armor);
						carried.addAll(player.getInventory().offhand);

						for (ItemStack stack : carried) {
							if (!stack.isEmpty() && kit.items.size() < 64) {
								kit.items.add(stack.save(player.registryAccess()).toString());
							}
						}

						if (kit.items.isEmpty()) {
							return OpResult.fail("kit_empty");
						}
					} else {
						for (String snbt : given.items) {
							if (kit.items.size() >= 64) {
								break;
							}

							if (snbt.length() > 30000 || !readable(snbt, player)) {
								return OpResult.fail("value_invalid");
							}

							kit.items.add(snbt);
						}
					}

					if (old != null) {
						book.kits.set(book.kits.indexOf(old), kit);
					} else {
						book.kits.add(kit);
					}

					this.s.books.saveKits();
					if (kit.restricted && this.s.live) {
						Perms.declare(kit.node(), 2);
					}

					this.s.audit(a, "kit_save", null, "", kit.id);
					this.s.sync.markAll("self");
					return OpResult.ok(kit.id, kit.items.size());
				}
			} else {
				return OpResult.fail("name_invalid");
			}
		}
	}

	private static boolean readable(String snbt, @Nullable ServerPlayer player) {
		try {
			CompoundTag tag = TagParser.parseTag(snbt);
			return tag.getString("id").isEmpty() ? false : player == null || ItemStack.parse(player.registryAccess(), tag).isPresent();
		} catch (Exception var3) {
			return false;
		}
	}

	public OpResult kitDelete(Actor a, String id) {
		if (!this.s.may(a, Nodes.ADMIN_KITS)) {
			return OpResult.fail("no_permission");
		} else {
			Kit kit = id == null ? null : this.s.kits().get(id.trim());
			if (kit == null) {
				return OpResult.fail("no_such_kit", Text.line(id, 24));
			} else {
				this.s.kits().kits.remove(kit);
				this.s.books.saveKits();
				this.s.audit(a, "kit_delete", null, "", kit.id);
				this.s.sync.markAll("self");
				return OpResult.ok(kit.id);
			}
		}
	}

	public OpResult kitGive(Actor a, @Nullable ServerService.Target t, String wanted, String id) {
		if (!this.s.may(a, Nodes.ADMIN_KITS)) {
			return OpResult.fail("no_permission");
		} else if (t == null) {
			return OpResult.fail("no_such_player", Text.line(wanted, 32));
		} else if (t.byNickname()) {
			return this.s.nicknamed(t);
		} else {
			Kit kit = id == null ? null : this.s.kits().get(id.trim());
			if (kit == null) {
				return OpResult.fail("no_such_kit", Text.line(id, 24));
			} else {
				Session session = t.session();
				if (session == null) {
					return OpResult.fail("player_offline", t.name());
				} else if (kit.items.isEmpty()) {
					return OpResult.fail("kit_empty");
				} else if (session.player != null && this.s.live && this.s.people.give(session.player, kit) == 0) {
					return OpResult.fail("kit_empty");
				} else {
					this.s.notice(session, "kit_received", 0, Msg.tr("cfhgui_server.chat.kit_received", kit.name), kit.name);
					this.s.audit(a, "kit_give", t.id(), t.name(), kit.id);
					return OpResult.ok(t.name(), kit.name);
				}
			}
		}
	}

	public OpResult punishments(Actor a, @Nullable String filter, @Nullable Punishment.Kind kind, int page) {
		if (!this.s.may(a, Nodes.MOD_HISTORY)) {
			return OpResult.fail("no_permission");
		} else {
			boolean active = filter == null || filter.isBlank() || filter.trim().equalsIgnoreCase("active");
			if (!active && !filter.trim().equalsIgnoreCase("all")) {
				return OpResult.fail("invalid");
			} else {
				long now = this.s.now();
				List<WorldStore.Known> punished = new ArrayList<>();

				for (WorldStore.Known k : this.s.store.all()) {
					if (active ? k.muted(now) || k.banned(now) || k.frozen(now) : k.punishments > 0) {
						punished.add(k);
					}
				}

				punished.sort(Comparator.<WorldStore.Known>comparingLong(kx -> kx.punishedAt).reversed());
				List<AdminData.PunishmentRow> rows = new ArrayList<>();

				for (int i = 0; i < punished.size() && i < MAX_PUNISHED; i++) {
					WorldStore.Known kx = punished.get(i);
					PlayerRecord rec = this.s.store.record(kx.id);
					if (rec != null) {
						for (Punishment p : rec.punishments) {
							if ((kind == null || p.kind == kind) && (!active || p.active(now))) {
								AdminData.PunishmentRow row = new AdminData.PunishmentRow();
								row.player = kx.id;
								row.playerName = kx.name;
								row.punishment = p;
								rows.add(row);
							}
						}
					}
				}

				rows.sort(Comparator.<AdminData.PunishmentRow>comparingLong(r -> r.punishment.at).reversed());
				AdminData.PunishmentPage out = new AdminData.PunishmentPage();
				out.page = Math.max(0, page);
				out.total = rows.size();
				int from = Math.min(rows.size(), out.page * 50);
				int to = Math.min(rows.size(), from + 50);
				out.rows = new ArrayList<>(rows.subList(from, to));
				return OpResult.answer(out);
			}
		}
	}

	public OpResult audit(Actor a, @Nullable String query, int page) {
		return !this.s.may(a, Nodes.ADMIN_AUDIT)
			? OpResult.fail("no_permission")
			: OpResult.answer(this.s.audit.page(query == null ? "" : Text.line(query, 48), page));
	}

	public OpResult rules(Actor a) {
		return !this.s.may(a, Nodes.ADMIN_RULES) ? OpResult.fail("no_permission") : OpResult.answer(this.s.rules());
	}

	static Map<String, Field> ruleFields() {
		Map<String, Field> out = new LinkedHashMap<>();

		for (Field f : ServerRules.class.getFields()) {
			if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers())) {
				out.put(f.getName(), f);
			}
		}

		return out;
	}

	public OpResult ruleSet(Actor a, @Nullable String rule, @Nullable String json) {
		if (!this.s.may(a, Nodes.ADMIN_RULES) && !this.s.may(a, Nodes.ADMIN_MAINTENANCE)) {
			return OpResult.fail("no_permission");
		} else {
			Field field = rule == null ? null : ruleFields().get(rule.trim());
			if (field == null) {
				return OpResult.fail("no_such_rule", Text.line(rule, 40));
			} else {
				boolean maintenance = field.getName().startsWith("maintenance");
				if (!this.s.may(a, maintenance ? Nodes.ADMIN_MAINTENANCE : Nodes.ADMIN_RULES)) {
					return OpResult.fail("no_permission");
				} else if (json != null && json.length() <= 16384) {
					ServerRules rules = this.s.rules();

					Object value;
					try {
						value = JsonConfig.GSON.fromJson(json, field.getGenericType());
					} catch (NumberFormatException | JsonParseException var10) {
						return OpResult.fail("value_invalid");
					}

					if (value != null && acceptable(value)) {
						try {
							field.set(rules, value);
						} catch (IllegalArgumentException | IllegalAccessException var9) {
							return OpResult.fail("value_invalid");
						}

						rules.sanitize();
						this.s.books.saveRules();
						String shown = json.length() <= 60 ? json : json.substring(0, 60) + "...";
						if (field.getName().equals("maintenance")) {
							this.s.audit(a, "maintenance", null, "", rules.maintenance ? "on" : "off");
						} else {
							this.s.audit(a, "rule_set", null, "", field.getName() + " = " + shown);
						}

						this.s.rulesChanged();
						if (field.getName().equals("maintenance")) {
							this.s.moderation.enforceMaintenance();
						}

						return OpResult.ok(field.getName(), shown);
					} else {
						return OpResult.fail("value_invalid");
					}
				} else {
					return OpResult.fail("value_invalid");
				}
			}
		}
	}

	private static boolean acceptable(Object value) {
		if (value instanceof Number n) {
			return n.longValue() >= 0L && n.longValue() <= 100000000L;
		} else if (value instanceof String str) {
			return str.length() <= 256 && Text.line(str, 256).length() == str.trim().length();
		} else {
			if (value instanceof List<?> list) {
				if (list.size() > 64) {
					return false;
				}

				for (Object o : list) {
					if (o instanceof String str) {
						if (str.length() > 256) {
							return false;
						}
					} else {
						if (!(o instanceof ServerRules.Link link)) {
							return false;
						}

						if (link.name == null || link.url == null || link.name.isBlank() || link.name.length() > 48 || link.url.length() > 256) {
							return false;
						}

						String url = link.url.toLowerCase(Locale.ROOT);
						if (!url.startsWith("https://") && !url.startsWith("http://")) {
							return false;
						}
					}
				}
			}

			return true;
		}
	}

	public OpResult rulesReload(Actor a) {
		if (!this.s.may(a, Nodes.ADMIN_RULES)) {
			return OpResult.fail("no_permission");
		} else {
			this.s.reload();
			this.s.audit(a, "rules_reload", null, "", "");
			return OpResult.OK;
		}
	}

	public OpResult world(Actor a) {
		if (!this.s.may(a, Nodes.ADMIN_WORLD)) {
			return OpResult.fail("no_permission");
		} else {
			final AdminData.WorldInfo out = new AdminData.WorldInfo();
			out.difficulty = this.s.server.getWorldData().getDifficulty().getKey();
			out.difficultyLocked = this.s.server.getWorldData().isDifficultyLocked();
			out.hardcore = this.s.server.isHardcore();

			for (ServerLevel level : this.s.server.getAllLevels()) {
				AdminData.DimensionTime t = new AdminData.DimensionTime();
				t.id = level.dimension().location().toString();
				t.dayTime = level.getDayTime();
				t.raining = level.isRaining();
				t.thundering = level.isThundering();
				out.dimensions.add(t);
			}

			final GameRules rules = this.s.server.getGameRules();
			GameRules.visitGameRuleTypes(new GameRuleTypeVisitor() {
				public void visitBoolean(Key<BooleanValue> key, Type<BooleanValue> type) {
					out.gameRules.add(Admin.rule(key, true, Boolean.toString(rules.getBoolean(key))));
				}

				public void visitInteger(Key<IntegerValue> key, Type<IntegerValue> type) {
					out.gameRules.add(Admin.rule(key, false, Integer.toString(rules.getInt(key))));
				}
			});
			out.gameRules.sort(Comparator.<AdminData.GameRule, String>comparing(g -> g.categoryKey).thenComparing(g -> g.id));
			return OpResult.answer(out);
		}
	}

	private static AdminData.GameRule rule(Key<?> key, boolean bool, String value) {
		AdminData.GameRule g = new AdminData.GameRule();
		g.id = key.getId();
		g.nameKey = key.getDescriptionId();
		g.categoryKey = key.getCategory().getDescriptionId();
		g.bool = bool;
		g.value = value;
		return g;
	}

	public OpResult worldSet(Actor a, @Nullable String what, @Nullable String key, @Nullable String rawValue, @Nullable String dimension) {
		if (!this.s.may(a, Nodes.ADMIN_WORLD)) {
			return OpResult.fail("no_permission");
		} else {
			final String value = rawValue == null ? "" : rawValue.trim().toLowerCase(Locale.ROOT);
			if (what != null && !value.isEmpty() && value.length() <= 32) {
				String detail;
				detail = what + " " + value;
				label121:
				switch (what) {
					case "time":
						long time;
						switch (value) {
							case "day":
								time = 1000L;
								break;
							case "noon":
								time = 6000L;
								break;
							case "night":
								time = 13000L;
								break;
							case "midnight":
								time = 18000L;
								break;
							default:
								try {
									time = Long.parseLong(value);
								} catch (NumberFormatException var15) {
									return OpResult.fail("value_invalid");
								}

								if (time < 0L || time > 24000000L) {
									return OpResult.fail("value_invalid");
								}
						}

						for (ServerLevel level : this.s.server.getAllLevels()) {
							level.setDayTime(time);
						}
						break;
					case "weather":
						ServerLevel level = this.s.server.overworld();
						switch (value) {
							case "clear":
								level.setWeatherParameters(ServerLevel.RAIN_DELAY.sample(level.getRandom()), 0, false, false);
								break label121;
							case "rain":
								level.setWeatherParameters(0, ServerLevel.RAIN_DURATION.sample(level.getRandom()), true, false);
								break label121;
							case "thunder":
								level.setWeatherParameters(0, ServerLevel.THUNDER_DURATION.sample(level.getRandom()), true, true);
								break label121;
							default:
								return OpResult.fail("value_invalid");
						}
					case "difficulty":
						Difficulty difficulty = Difficulty.byName(value);
						if (difficulty == null) {
							return OpResult.fail("value_invalid");
						}

						this.s.server.setDifficulty(difficulty, true);
						break;
					case "gamerule":
						if (key == null || key.isBlank()) {
							return OpResult.fail("no_such_rule", "");
						}

						final String id = key.trim();
						final boolean[] found = new boolean[2];
						GameRules.visitGameRuleTypes(new GameRuleTypeVisitor() {
							public void visitBoolean(Key<BooleanValue> k, Type<BooleanValue> type) {
								if (k.getId().equals(id)) {
									found[0] = true;
									if (value.equals("true") || value.equals("false")) {
										((BooleanValue)Admin.this.s.server.getGameRules().getRule(k)).set(Boolean.parseBoolean(value), Admin.this.s.server);
										found[1] = true;
									}
								}
							}

							public void visitInteger(Key<IntegerValue> k, Type<IntegerValue> type) {
								if (k.getId().equals(id)) {
									found[0] = true;
									IntegerValue read = (IntegerValue)type.createRule();
									if (read.tryDeserialize(value)) {
										((IntegerValue)Admin.this.s.server.getGameRules().getRule(k)).set(read.get(), Admin.this.s.server);
										found[1] = true;
									}
								}
							}
						});
						if (!found[0]) {
							return OpResult.fail("no_such_rule", Text.line(id, 48));
						}

						if (!found[1]) {
							return OpResult.fail("value_invalid");
						}

						detail = id + " = " + value;
						break;
					default:
						return OpResult.fail("invalid");
				}

				this.s.audit(a, "world_set", null, "", detail);
				return OpResult.ok(what, value);
			} else {
				return OpResult.fail("value_invalid");
			}
		}
	}
}
