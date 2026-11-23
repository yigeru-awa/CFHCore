package com.onehumanawa.cfhcore.content.gui.teams.server;

import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.TeamsServerConfig;
import com.onehumanawa.cfhcore.content.gui.teams.api.TeamsApi;
import com.onehumanawa.cfhcore.content.gui.teams.model.BannerData;
import com.onehumanawa.cfhcore.content.gui.teams.model.InviteView;
import com.onehumanawa.cfhcore.content.gui.teams.model.Permission;
import com.onehumanawa.cfhcore.content.gui.teams.model.PingData;
import com.onehumanawa.cfhcore.content.gui.teams.model.Privacy;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import com.onehumanawa.cfhcore.content.gui.teams.net.TeamsNet;
import com.onehumanawa.cfhcore.content.gui.teams.server.protect.Protection;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.HoverEvent.Action;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

public final class TeamService {
	static final int MAX_CHUNKS_PER_ACTION = 1024;
	private static final int MAX_REQUESTS_PER_TEAM = 64;
	private static final int MAX_REQUESTS_PER_PLAYER = 8;
	private static final int WAYPOINT_NAME_MAX = 32;
	private static final int CHAT_MAX = 256;
	private static final long PING_GAP_MS = 250L;
	private static final long CHAT_GAP_MS = 400L;
	public final MinecraftServer server;
	public final boolean live;
	private final Supplier<TeamsServerConfig> config;
	private LandPins pins;
	final TeamStore store;
	final ClaimMap claims;
	final ServerLand serverLand;
	final Sync sync;
	final Stashes stashes;
	final Homes homes;
	final ForceLoads forceLoads;
	final ScoreboardMirror mirror;
	final Expiry expiry;
	public final Protection protection;
	@Nullable
	final Saver saver;
	@Nullable
	final Path dir;
	private LongSupplier clock = System::currentTimeMillis;
	@Nullable
	private List<String> trace;
	private final Object2LongOpenHashMap<UUID> lastPing = new Object2LongOpenHashMap();
	private final Object2LongOpenHashMap<UUID> lastChat = new Object2LongOpenHashMap();
	private int ticks;
	public static final String CHAT_MARK = "cfhgui_teams:chat";

	private TeamService(MinecraftServer server, boolean live, Supplier<TeamsServerConfig> config, @Nullable Path dir) {
		this.server = server;
		this.live = live;
		this.config = config;
		this.dir = dir;
		this.pins = LandPins.parse(config.get().pinnedLandRules);
		this.store = new TeamStore(dir);
		this.claims = new ClaimMap(dir == null ? null : dir.resolve("claims.json"));
		this.serverLand = new ServerLand(dir == null ? null : dir.resolve("server-land.json"));
		this.saver = live ? new Saver() : null;
		this.sync = new Sync(this);
		this.stashes = new Stashes(this);
		this.homes = new Homes(this);
		this.forceLoads = new ForceLoads(this);
		this.mirror = new ScoreboardMirror(this);
		this.expiry = new Expiry(this);
		this.protection = new Protection(this);
	}

	static TeamService start(MinecraftServer server) {
		Path dir = server.getWorldPath(LevelResource.ROOT).resolve("slate").resolve("teams").normalize();
		TeamService s = new TeamService(server, true, TeamsServerConfig::get, dir);
		s.store.load();
		s.serverLand.load();
		s.claims.load(s::holder);
		int released = s.expiry.run().size();
		s.mirror.syncAll();
		s.forceLoads.refreshAll(null);
		CFHGuiTeams.LOGGER
			.info(
				"[CFHGui Teams] {} team(s), {} claimed chunk(s), {} of them the server's own ({})",
				new Object[]{s.store.size(), s.claims.total(), s.claims.count(TeamSummary.SERVER_LAND), dir}
			);
		if (released > 0) {
			CFHGuiTeams.LOGGER.info("[CFHGui Teams] {} team(s) had been away for more than {} day(s): their land is free again", released, s.cfg().claimExpiryDays);
		}

		return s;
	}

	@Nullable
	TeamData holder(UUID id) {
		return TeamSummary.SERVER_LAND.equals(id) ? this.serverLand.data : this.store.get(id);
	}

	static TeamService scratch(MinecraftServer server, TeamsServerConfig rules) {
		return new TeamService(server, false, () -> rules, null);
	}

	void tick() {
		this.ticks++;
		this.homes.tick();
		if (this.ticks % 10 == 0) {
			this.sync.teammates();
		}

		if (this.ticks % 200 == 0) {
			this.expireInvites();
		}

		if (this.ticks % 20 == 0) {
			this.stashes.tick();
		}

		if (this.ticks % 100 == 0) {
			this.sync.operators();
		}

		if (this.ticks % 72000 == 0) {
			this.expiry.run();
		}

		this.sync.flush();
		if (this.saver != null) {
			this.store.tick(this.saver);
			this.claims.tick(this.saver);
			this.serverLand.tick(this.saver);
		}
	}

	void joined(ServerPlayer player) {
		UUID id = player.getUUID();
		String name = player.getGameProfile().getName();
		this.store.remember(id, name);
		TeamData t = this.store.ofPlayer(id);
		if (t != null) {
			TeamData.Member m = t.member(id);
			if (m != null && !name.equals(m.name)) {
				this.mirror.rename(t, m.name, name);
				m.name = name;
				if (t.solo) {
					this.store.rename(t, name);
				}

				this.changed(t);
			}

			this.expiry.seen(t);
			this.mirror.update(t);
			this.forceLoads.refresh(t, null);
			this.sync.directory(false);
		}

		this.sync.everything(player);
		if (!TeamsNet.has(player)) {
			for (InviteView v : this.invitesOf(id)) {
				player.sendSystemMessage(inviteLine(v.teamName, v.color, v.byName));
			}
		}
	}

	void left(ServerPlayer player) {
		UUID id = player.getUUID();
		this.homes.cancel(id);
		this.sync.forget(id);
		this.protection.forget(id);
		this.lastPing.removeLong(id);
		this.lastChat.removeLong(id);
		TeamData t = this.store.ofPlayer(id);
		if (t != null) {
			TeamData.Member m = t.member(id);
			if (m != null) {
				m.lastSeen = this.now();
				this.changed(t);
			}

			this.expiry.seen(t);
			this.forceLoads.refresh(t, id);
			this.sync.directory(false);
		}
	}

	void stop() {
		long now = this.now();

		for (ServerPlayer p : this.server.getPlayerList().getPlayers()) {
			TeamData t = this.store.ofPlayer(p.getUUID());
			TeamData.Member m = t == null ? null : t.member(p.getUUID());
			if (m != null) {
				m.lastSeen = now;
				t.lastSeen = now;
				this.store.touch(t);
			}
		}

		this.forceLoads.stopping();
		if (this.saver != null) {
			this.stashes.saveAll();
			this.store.save(this.saver);
			this.claims.save(this.saver);
			this.serverLand.save(this.saver);
			this.saver.close();
		}
	}

	void repin() {
		this.pins = LandPins.parse(this.cfg().pinnedLandRules);
	}

	void reloaded() {
		this.repin();
		this.mirror.syncAll();
		this.forceLoads.refreshAll(null);

		for (ServerPlayer p : this.server.getPlayerList().getPlayers()) {
			this.sync.player(p.getUUID());
		}

		this.sync.directory(true);
	}

	public TeamsServerConfig cfg() {
		return this.config.get();
	}

	public LandPins pins() {
		return this.pins;
	}

	long now() {
		return this.clock.getAsLong();
	}

	void clock(LongSupplier clock) {
		this.clock = clock;
	}

	void trace(@Nullable List<String> trace) {
		this.trace = trace;
	}

	public TeamStore store() {
		return this.store;
	}

	public ClaimMap claims() {
		return this.claims;
	}

	@Nullable
	public TeamData teamOf(@Nullable UUID player) {
		TeamData t = this.store.ofPlayer(player);
		return t != null && !t.solo ? t : null;
	}

	@Nullable
	public TeamData holderOf(@Nullable UUID player) {
		return this.store.ofPlayer(player);
	}

	@Nullable
	public TeamData personalOf(@Nullable UUID player) {
		TeamData t = this.store.ofPlayer(player);
		return t != null && t.solo ? t : null;
	}

	@Nullable
	public TeamData team(@Nullable UUID id) {
		TeamData t = this.store.get(id);
		return t != null && !t.solo ? t : null;
	}

	public List<TeamData> teams() {
		List<TeamData> out = new ArrayList<>(this.store.size());

		for (TeamData t : this.store.all()) {
			if (!t.solo) {
				out.add(t);
			}
		}

		return out;
	}

	@Nullable
	public ServerPlayer online(UUID player) {
		return this.server.getPlayerList().getPlayer(player);
	}

	int onlineCount(TeamData t, @Nullable UUID excluding) {
		if (!this.live) {
			return 0;
		} else {
			int n = 0;

			for (TeamData.Member m : t.members) {
				if (!m.id.equals(excluding) && this.online(m.id) != null) {
					n++;
				}
			}

			return n;
		}
	}

	boolean anyOnline(TeamData t) {
		for (TeamData.Member m : t.members) {
			if (this.online(m.id) != null) {
				return true;
			}
		}

		return false;
	}

	public int claimLimit(@Nullable TeamData t) {
		TeamsServerConfig c = this.cfg();
		int members = t == null ? 1 : t.members.size();
		int level = t == null ? 1 : t.level;
		long limit = (long)c.baseClaims + (long)c.claimsPerMember * (long)members + (long)c.claimsPerLevel * (long)(level - 1);
		return (int)Math.max(0L, Math.min((long)c.maxClaims, limit));
	}

	public int forceLimit(@Nullable TeamData t) {
		TeamsServerConfig c = this.cfg();
		long limit = (long)c.baseForceLoads + (long)c.forceLoadsPerLevel * (long)((t == null ? 1 : t.level) - 1);
		return (int)Math.max(0L, Math.min((long)c.maxForceLoads, limit));
	}

	void changed(TeamData t) {
		this.store.touch(t);
		this.sync.team(t);
	}

	private void record(String what) {
		if (this.trace != null) {
			this.trace.add(what);
		}
	}

	private static OpResult fail(String code, Object... args) {
		return OpResult.fail(code, args);
	}

	public OpResult create(Actor a, @Nullable String rawName, @Nullable String rawTag, @Nullable Integer color) {
		if (a.isConsole()) {
			return fail("player_only");
		} else if (!this.cfg().playersCreateTeams && !a.admin()) {
			return fail("disabled");
		} else {
			TeamData held = this.store.ofPlayer(a.id());
			if (held != null && !held.solo) {
				return fail("already_in_team");
			} else {
				String name = Names.team(rawName);
				if (name == null) {
					return fail("name_invalid");
				} else if (this.store.named(name) != null) {
					return fail("name_taken");
				} else {
					String tag = Names.tag(rawTag);
					if (tag == null) {
						return fail("tag_invalid");
					} else if (this.store.tagged(tag) != null) {
						return fail("tag_taken");
					} else if (held != null) {
						return this.found(held, a, name, tag, color);
					} else {
						TeamData t = new TeamData();

						do {
							t.id = UUID.randomUUID();
						} while (this.store.get(t.id) != null);

						t.name = name;
						t.tag = tag;
						t.color = color != null ? color & 16777215 : Names.colorFor(name);
						t.banner = new BannerData(Names.nearestDye(t.color).getName());
						t.createdAt = this.now();
						t.lastSeen = t.createdAt;
						TeamData.Member owner = new TeamData.Member();
						owner.id = a.id();
						owner.name = a.name();
						owner.rank = Rank.OWNER;
						owner.joinedAt = t.createdAt;
						owner.lastSeen = t.createdAt;
						t.members.add(owner);
						this.store.add(t);
						this.store.remember(a.id(), a.name());
						this.forgetAsks(a.id());
						this.append(t, "created", a.id(), a.name(), "");
						this.changed(t);
						this.sync.player(a.id());
						this.sync.directory(true);
						this.mirror.update(t);
						this.record("event created " + t.name);
						this.record("event joined " + a.name());
						if (this.live) {
							TeamsApi.TEAM_CREATED.invoke(l -> l.accept(t));
							TeamsApi.MEMBER_JOINED.invoke(l -> l.changed(t, a.id()));
						}

						return OpResult.ok(t.name);
					}
				}
			}
		}
	}

	private OpResult found(TeamData t, Actor a, String name, String tag, @Nullable Integer color) {
		this.store.becomeTeam(t, name, tag);
		if (color != null) {
			t.color = color & 16777215;
		}

		t.banner = new BannerData(Names.nearestDye(t.color).getName());
		t.createdAt = this.now();
		t.lastSeen = t.createdAt;
		TeamData.Member owner = t.member(a.id());
		if (owner != null) {
			owner.name = a.name();
			owner.rank = Rank.OWNER;
		}

		this.store.remember(a.id(), a.name());
		this.forgetAsks(a.id());
		this.append(t, "created", a.id(), a.name(), "");
		this.changed(t);
		this.sync.player(a.id());
		this.sync.directory(true);
		this.mirror.update(t);
		this.record("event created " + t.name);
		this.record("event joined " + a.name());
		if (this.live) {
			TeamsApi.TEAM_CREATED.invoke(l -> l.accept(t));
			TeamsApi.MEMBER_JOINED.invoke(l -> l.changed(t, a.id()));
		}

		return OpResult.ok(t.name);
	}

	public OpResult disband(Actor a) {
		TeamData t = this.store.ofPlayer(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (t.solo) {
			return this.giveUp(t, a);
		} else if (!t.may(a.id(), Permission.OWN)) {
			return fail("no_permission");
		} else {
			this.dissolve(t, a);
			return OpResult.ok(t.name);
		}
	}

	private TeamData personal(Actor a) {
		TeamData t = new TeamData();

		do {
			t.id = UUID.randomUUID();
		} while (this.store.get(t.id) != null || TeamSummary.SERVER_LAND.equals(t.id));

		t.solo = true;
		t.name = a.name();
		t.color = Names.colorFor(a.name());
		t.banner = new BannerData(Names.nearestDye(t.color).getName());
		t.createdAt = this.now();
		t.lastSeen = t.createdAt;
		TeamData.Member owner = new TeamData.Member();
		owner.id = a.id();
		owner.name = a.name();
		owner.rank = Rank.OWNER;
		owner.joinedAt = t.createdAt;
		owner.lastSeen = t.createdAt;
		t.members.add(owner);
		this.store.add(t);
		this.store.remember(a.id(), a.name());
		return t;
	}

	private OpResult giveUp(TeamData t, Actor a) {
		int chunks = this.claims.count(t.id);
		this.drop(t);
		this.notice(a.id(), "land_given_up", t, chunks);
		return OpResult.ok(t.name, chunks);
	}

	void drop(TeamData t) {
		for (ClaimMap.Entry e : this.claims.of(t)) {
			this.free(e.dim().id, e.chunk(), e.claim());
		}

		for (TeamData.Member m : new ArrayList<>(t.members)) {
			this.store.removeMember(t, m.id);
			this.homes.cancel(m.id);
			this.sync.player(m.id);
		}

		this.store.remove(t);
		this.sync.directory(true);
		this.record("dropped " + t.name);
	}

	private void dropIfBare(TeamData t) {
		if (t.solo && this.store.get(t.id) == t && this.claims.count(t.id) == 0) {
			this.drop(t);
		}
	}

	private void bringIn(List<ClaimMap.Entry> land, TeamData t, UUID player, String name) {
		if (!land.isEmpty()) {
			land.sort((x, y) -> Boolean.compare(y.claim().forced, x.claim().forced));
			int limit = this.claimLimit(t);
			int loads = this.forceLimit(t);
			int moved = 0;
			int freed = 0;

			for (ClaimMap.Entry e : land) {
				boolean forced = e.claim().forced;
				this.free(e.dim().id, e.chunk(), e.claim());
				ClaimMap.Claim c = this.claims.count(t.id) < limit ? this.claims.put(e.dim().id, e.chunk(), t) : null;
				if (c == null) {
					freed++;
				} else {
					moved++;
					if (forced && this.claims.forced(t.id) < loads) {
						this.claims.setForced(e.dim(), e.chunk(), c, true);
						this.forceLoads.apply(e.dim(), e.chunk(), c, null);
					}
				}
			}

			if (moved > 0) {
				this.append(t, "claimed", player, name, String.valueOf(moved));
				this.changed(t);
			}

			this.sync.directory(true);
			this.notice(player, freed == 0 ? "land_moved" : (moved == 0 ? "land_freed" : "land_cut"), t, t.name, moved, freed);
		}
	}

	void dissolve(TeamData t, Actor by) {
		for (TeamData.Member m : t.members) {
			this.notice(m.id, "disbanded", t, t.name, by.name());
		}

		for (ClaimMap.Entry e : this.claims.of(t)) {
			this.forceLoads.release(e.dim(), e.chunk(), e.claim());
			this.claims.remove(e.dim().id, e.chunk());
		}

		this.stashes.dissolve(t, by.player());

		for (TeamData other : this.store.all()) {
			if (other != t && other.allies.remove(t.id)) {
				this.changed(other);
			}
		}

		List<TeamData.Invite> invites = new ArrayList<>(t.invites);
		List<TeamData.Request> requests = new ArrayList<>(t.requests);

		for (TeamData.Member m : new ArrayList<>(t.members)) {
			this.store.removeMember(t, m.id);
			this.homes.cancel(m.id);
			this.sync.player(m.id);
			this.record("event left " + m.name);
			if (this.live) {
				TeamsApi.MEMBER_LEFT.invoke(l -> l.changed(t, m.id));
			}
		}

		this.record("event disbanded " + t.name);
		if (this.live) {
			TeamsApi.TEAM_DISBANDED.invoke(l -> l.accept(t));
		}

		this.store.remove(t);

		for (TeamData.Invite i : invites) {
			this.sync.invites(i.player);
		}

		for (TeamData.Request r : requests) {
			this.sync.invites(r.player);
		}

		this.mirror.remove(t);
		this.sync.directory(true);
	}

	public OpResult leave(Actor a) {
		TeamData t = this.store.ofPlayer(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (t.solo) {
			return this.giveUp(t, a);
		} else {
			TeamData.Member m = t.member(a.id());
			if (m == null) {
				return fail("not_in_team");
			} else if (m.rank == Rank.OWNER) {
				return fail("owner_must_transfer");
			} else {
				this.exit(t, m);
				this.append(t, "left", m.id, m.name, "");

				for (TeamData.Member other : t.members) {
					this.notice(other.id, "left", t, m.name, t.name);
				}

				return OpResult.ok(t.name);
			}
		}
	}

	private void enter(TeamData t, UUID player, String name) {
		TeamData own = this.personalOf(player);
		List<ClaimMap.Entry> land = (List<ClaimMap.Entry>)(own == null ? new ArrayList<>() : this.claims.of(own));
		if (own != null) {
			this.store.removeMember(own, player);
			this.store.remove(own);
		}

		for (TeamData.Member other : t.members) {
			this.notice(other.id, "joined", t, name, t.name);
		}

		TeamData.Member m = new TeamData.Member();
		m.id = player;
		m.name = name;
		m.rank = Rank.MEMBER;
		m.joinedAt = this.now();
		m.lastSeen = m.joinedAt;
		this.store.addMember(t, m);
		this.store.removeInvite(t, player);
		this.store.removeRequest(t, player);
		this.store.remember(player, name);
		this.forgetAsks(player);
		t.lastSeen = m.joinedAt;
		this.append(t, "joined", player, name, "");
		this.changed(t);
		this.sync.limits(t);
		this.sync.player(player);
		this.sync.directory(true);
		this.mirror.update(t);
		this.forceLoads.refresh(t, null);
		this.record("event joined " + name);
		if (this.live) {
			TeamsApi.MEMBER_JOINED.invoke(l -> l.changed(t, player));
		}

		if (own != null) {
			this.bringIn(land, t, player, name);
		}
	}

	private void exit(TeamData t, TeamData.Member m) {
		this.store.removeMember(t, m.id);
		this.homes.cancel(m.id);
		ServerPlayer p = this.live ? this.online(m.id) : null;
		if (p != null) {
			this.stashes.closeFor(p, t);
		}

		this.mirror.update(t);
		this.changed(t);
		this.sync.limits(t);
		this.sync.player(m.id);
		this.sync.directory(true);
		this.forceLoads.refresh(t, null);
		this.record("event left " + m.name);
		if (this.live) {
			TeamsApi.MEMBER_LEFT.invoke(l -> l.changed(t, m.id));
		}
	}

	private void forgetAsks(UUID player) {
		for (UUID teamId : new ArrayList<>(this.store.invitersOf(player))) {
			TeamData other = this.store.get(teamId);
			if (other != null && this.store.removeInvite(other, player) != null) {
				this.changed(other);
			}
		}

		for (UUID teamIdx : new ArrayList<>(this.store.askedBy(player))) {
			TeamData other = this.store.get(teamIdx);
			if (other != null && this.store.removeRequest(other, player) != null) {
				this.changed(other);
			}
		}

		this.sync.invites(player);
	}

	public OpResult rename(Actor a, @Nullable String rawName) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!t.may(a.id(), Permission.SETTINGS)) {
			return fail("no_permission");
		} else {
			String name = Names.team(rawName);
			if (name == null) {
				return fail("name_invalid");
			} else {
				TeamData holder = this.store.named(name);
				if (holder != null && holder != t) {
					return fail("name_taken");
				} else if (name.equals(t.name)) {
					return OpResult.ok(name);
				} else {
					this.store.rename(t, name);
					this.append(t, "renamed", a.id(), a.name(), name);
					this.changed(t);
					this.sync.directory(true);
					this.mirror.update(t);
					return OpResult.ok(name);
				}
			}
		}
	}

	public OpResult setTag(Actor a, @Nullable String rawTag) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!t.may(a.id(), Permission.SETTINGS)) {
			return fail("no_permission");
		} else {
			String tag = Names.tag(rawTag);
			if (tag == null) {
				return fail("tag_invalid");
			} else {
				TeamData holder = this.store.tagged(tag);
				if (holder != null && holder != t) {
					return fail("tag_taken");
				} else if (tag.equals(t.tag)) {
					return OpResult.ok(tag);
				} else {
					this.store.retag(t, tag);
					this.changed(t);
					this.sync.directory(true);
					this.mirror.update(t);
					return OpResult.ok(tag);
				}
			}
		}
	}

	public OpResult setColor(Actor a, int rgb) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!t.may(a.id(), Permission.SETTINGS)) {
			return fail("no_permission");
		} else {
			t.color = rgb & 16777215;
			this.changed(t);
			this.sync.directory(true);
			this.mirror.update(t);
			return OpResult.OK;
		}
	}

	public OpResult setMotto(Actor a, @Nullable String motto) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!t.may(a.id(), Permission.SETTINGS)) {
			return fail("no_permission");
		} else {
			t.motto = Names.line(motto, 96);
			this.changed(t);
			this.sync.directory(true);
			return OpResult.OK;
		}
	}

	public OpResult setBanner(Actor a, @Nullable BannerData banner) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!t.may(a.id(), Permission.SETTINGS)) {
			return fail("no_permission");
		} else if (banner == null) {
			return fail("invalid");
		} else {
			t.banner = this.cleanBanner(banner);
			this.changed(t);
			this.sync.directory(true);
			return OpResult.OK;
		}
	}

	private BannerData cleanBanner(BannerData in) {
		in.sanitize();
		BannerData out = new BannerData(dye(in.base, DyeColor.WHITE).getName());
		Registry<BannerPattern> patterns = (Registry<BannerPattern>)this.server.registryAccess().registry(Registries.BANNER_PATTERN).orElse(null);

		for (BannerData.Layer l : in.layers) {
			ResourceLocation id = ResourceLocation.tryParse(l.pattern);
			DyeColor c = dye(l.color, null);
			if (id != null && c != null && (patterns == null || patterns.containsKey(id))) {
				out.layer(id.toString(), c.getName());
			}
		}

		return out;
	}

	@Nullable
	private static DyeColor dye(String name, @Nullable DyeColor fallback) {
		return DyeColor.byName(name == null ? "" : name.toLowerCase(Locale.ROOT), fallback);
	}

	public OpResult setPrivacy(Actor a, @Nullable Privacy privacy) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!t.may(a.id(), Permission.SETTINGS)) {
			return fail("no_permission");
		} else if (privacy == null) {
			return fail("invalid");
		} else {
			t.privacy = privacy;
			this.changed(t);
			this.sync.directory(true);
			return OpResult.OK;
		}
	}

	public OpResult setFriendlyFire(Actor a, boolean on) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!t.may(a.id(), Permission.SETTINGS)) {
			return fail("no_permission");
		} else {
			t.friendlyFire = on;
			this.changed(t);
			this.mirror.update(t);
			return OpResult.OK;
		}
	}

	public OpResult setLandRule(Actor a, @Nullable String rawRule, @Nullable String value) {
		TeamData t = this.store.ofPlayer(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!t.may(a.id(), Permission.SETTINGS)) {
			return fail("no_permission");
		} else {
			String rule = LandPins.canonical(rawRule);
			if (rule == null || value == null) {
				return fail("invalid");
			} else if (this.pins.pins(rule)) {
				return fail("pinned");
			} else if (!LandPins.set(t.land, rule, value)) {
				return fail("invalid");
			} else {
				this.changed(t);
				return OpResult.ok(rule, LandPins.get(t.land, rule));
			}
		}
	}

	public OpResult invite(Actor a, @Nullable UUID id, @Nullable String name) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!t.may(a.id(), Permission.INVITE)) {
			return fail("no_permission");
		} else {
			UUID target = id;
			String targetName = null;
			if (id != null) {
				ServerPlayer p = this.live ? this.online(id) : null;
				targetName = p != null ? p.getGameProfile().getName() : this.store.nameOf(id);
			} else if (name != null && !name.isBlank()) {
				ServerPlayer p = this.live ? this.server.getPlayerList().getPlayerByName(name.trim()) : null;
				target = p != null ? p.getUUID() : this.store.idOf(name.trim());
				targetName = p != null ? p.getGameProfile().getName() : (target == null ? null : this.store.nameOf(target));
			}

			if (target != null && targetName != null) {
				if (t.isMember(target)) {
					return fail("already_member");
				} else {
					UUID who = target;

					for (TeamData.Request r : t.requests) {
						if (r.player.equals(who)) {
							return this.acceptRequest(a, who);
						}
					}

					TeamData.Invite open = this.invite(t, target);
					if (open != null) {
						return fail("already_invited");
					} else if (t.members.size() >= this.cfg().maxMembers) {
						return fail("team_full");
					} else {
						TeamData.Invite i = new TeamData.Invite();
						i.player = target;
						i.playerName = targetName;
						i.by = a.id();
						i.byName = a.name();
						i.expiresAt = this.now() + (long)Math.max(1, this.cfg().inviteMinutes) * 60000L;
						this.store.addInvite(t, i);
						this.changed(t);
						this.sync.invites(target);
						this.notice(target, "invited", t, t.name, a.name());
						return OpResult.ok(targetName);
					}
				}
			} else {
				return fail("no_such_player", name != null ? name : String.valueOf(id));
			}
		}
	}

	@Nullable
	private TeamData.Invite invite(TeamData t, UUID player) {
		for (TeamData.Invite i : t.invites) {
			if (i.player.equals(player)) {
				if (i.expiresAt > this.now()) {
					return i;
				}

				this.store.removeInvite(t, player);
				this.changed(t);
				this.sync.invites(player);
				return null;
			}
		}

		return null;
	}

	public OpResult cancelInvite(Actor a, @Nullable UUID player) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!t.may(a.id(), Permission.INVITE)) {
			return fail("no_permission");
		} else if (player != null && this.store.removeInvite(t, player) != null) {
			this.changed(t);
			this.sync.invites(player);
			return OpResult.OK;
		} else {
			return fail("not_invited");
		}
	}

	public OpResult acceptInvite(Actor a, @Nullable UUID teamId) {
		TeamData t = this.team(teamId);
		if (t == null) {
			return fail("no_such_team");
		} else if (this.invite(t, a.id()) == null) {
			return fail("not_invited");
		} else if (this.teamOf(a.id()) != null) {
			return fail("already_in_team");
		} else if (t.members.size() >= this.cfg().maxMembers) {
			return fail("team_full");
		} else {
			this.enter(t, a.id(), a.name());
			return OpResult.ok("joined", t.name);
		}
	}

	public OpResult declineInvite(Actor a, @Nullable UUID teamId) {
		TeamData t = this.store.get(teamId);
		if (t == null) {
			return fail("no_such_team");
		} else {
			TeamData.Invite i = this.store.removeInvite(t, a.id());
			if (i == null) {
				return fail("not_invited");
			} else {
				this.changed(t);
				this.sync.invites(a.id());
				this.notice(i.by, "declined", t, a.name(), t.name);
				return OpResult.ok(t.name);
			}
		}
	}

	public OpResult join(Actor a, @Nullable UUID teamId) {
		if (a.isConsole()) {
			return fail("player_only");
		} else {
			TeamData t = this.team(teamId);
			if (t == null) {
				return fail("no_such_team");
			} else if (this.teamOf(a.id()) != null) {
				return fail("already_in_team");
			} else {
				boolean invited = this.invite(t, a.id()) != null;
				if (!invited && t.privacy != Privacy.OPEN) {
					if (t.privacy == Privacy.INVITE) {
						return fail("not_invited");
					} else {
						for (TeamData.Request r : t.requests) {
							if (r.player.equals(a.id())) {
								return fail("already_requested");
							}
						}

						if (t.requests.size() >= 64) {
							return fail("team_full");
						} else if (this.store.askedBy(a.id()).size() >= 8) {
							return fail("too_fast");
						} else {
							TeamData.Request rx = new TeamData.Request();
							rx.player = a.id();
							rx.playerName = a.name();
							rx.at = this.now();
							this.store.addRequest(t, rx);
							this.store.remember(a.id(), a.name());
							this.changed(t);
							this.sync.invites(a.id());

							for (TeamData.Member m : t.members) {
								if (Permission.INVITE.allows(m.rank)) {
									this.notice(m.id, "request", t, a.name(), t.name);
								}
							}

							return OpResult.ok("requested", t.name);
						}
					}
				} else if (t.members.size() >= this.cfg().maxMembers) {
					return fail("team_full");
				} else {
					this.enter(t, a.id(), a.name());
					return OpResult.ok("joined", t.name);
				}
			}
		}
	}

	public OpResult cancelRequest(Actor a, @Nullable UUID teamId) {
		TeamData t = this.store.get(teamId);
		if (t == null) {
			return fail("no_such_team");
		} else if (this.store.removeRequest(t, a.id()) == null) {
			return fail("no_request");
		} else {
			this.changed(t);
			this.sync.invites(a.id());
			return OpResult.ok(t.name);
		}
	}

	public OpResult acceptRequest(Actor a, @Nullable UUID player) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!t.may(a.id(), Permission.INVITE)) {
			return fail("no_permission");
		} else {
			TeamData.Request found = null;

			for (TeamData.Request r : t.requests) {
				if (r.player.equals(player)) {
					found = r;
				}
			}

			if (found == null) {
				return fail("no_request");
			} else if (this.teamOf(found.player) != null) {
				this.store.removeRequest(t, found.player);
				this.changed(t);
				this.sync.invites(found.player);
				return fail("player_in_team");
			} else if (t.members.size() >= this.cfg().maxMembers) {
				return fail("team_full");
			} else {
				ServerPlayer p = this.live ? this.online(found.player) : null;
				String name = p != null ? p.getGameProfile().getName() : found.playerName;
				this.notice(found.player, "accepted", t, t.name, a.name());
				this.enter(t, found.player, name);
				return OpResult.ok(name);
			}
		}
	}

	public OpResult denyRequest(Actor a, @Nullable UUID player) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!t.may(a.id(), Permission.INVITE)) {
			return fail("no_permission");
		} else if (player != null && this.store.removeRequest(t, player) != null) {
			this.changed(t);
			this.sync.invites(player);
			this.notice(player, "denied", t, t.name, a.name());
			return OpResult.OK;
		} else {
			return fail("no_request");
		}
	}

	private void expireInvites() {
		long now = this.now();

		for (UUID player : this.store.invited()) {
			for (UUID teamId : new ArrayList<>(this.store.invitersOf(player))) {
				TeamData t = this.store.get(teamId);
				if (t != null) {
					boolean ranOut = false;

					for (TeamData.Invite i : t.invites) {
						if (i.player.equals(player) && i.expiresAt <= now) {
							ranOut = true;
						}
					}

					if (ranOut) {
						this.store.removeInvite(t, player);
						this.changed(t);
						this.sync.invites(player);
					}
				}
			}
		}
	}

	void sweep() {
		this.expireInvites();
	}

	List<InviteView> invitesOf(UUID player) {
		Set<UUID> from = this.store.invitersOf(player);
		if (from.isEmpty()) {
			return List.of();
		} else {
			long now = this.now();
			List<InviteView> out = new ArrayList<>(from.size());

			for (UUID teamId : from) {
				TeamData t = this.store.get(teamId);
				if (t != null) {
					for (TeamData.Invite i : t.invites) {
						if (i.player.equals(player) && i.expiresAt > now) {
							InviteView v = new InviteView();
							v.team = t.id;
							v.teamName = t.name;
							v.tag = t.tag;
							v.color = t.color;
							v.banner = t.banner;
							v.members = t.members.size();
							v.byName = i.byName;
							v.expiresAt = i.expiresAt;
							out.add(v);
						}
					}
				}
			}

			return out;
		}
	}

	public OpResult kick(Actor a, @Nullable UUID target) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!t.may(a.id(), Permission.KICK)) {
			return fail("no_permission");
		} else {
			TeamData.Member m = t.member(target);
			if (m == null) {
				return fail("not_a_member");
			} else if (m.id.equals(a.id())) {
				return fail("not_yourself");
			} else {
				Rank mine = t.rankOf(a.id());
				if (mine != null && mine.above(m.rank)) {
					this.remove(t, m, a);
					return OpResult.ok(m.name);
				} else {
					return fail("rank_too_low");
				}
			}
		}
	}

	void remove(TeamData t, TeamData.Member m, Actor by) {
		this.exit(t, m);
		this.append(t, "kicked", by.id(), by.name(), m.name);
		this.notice(m.id, "kicked", t, t.name, by.name());

		for (TeamData.Member other : t.members) {
			if (!other.id.equals(by.id())) {
				this.notice(other.id, "left", t, m.name, t.name);
			}
		}
	}

	public OpResult promote(Actor a, @Nullable UUID target) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!t.may(a.id(), Permission.RANKS)) {
			return fail("no_permission");
		} else {
			TeamData.Member m = t.member(target);
			if (m == null) {
				return fail("not_a_member");
			} else if (m.id.equals(a.id())) {
				return fail("not_yourself");
			} else {
				Rank mine = t.rankOf(a.id());
				Rank next = m.rank.promoted();
				if (next == null) {
					return fail("cannot_promote");
				} else if (mine != null && mine.above(next)) {
					m.rank = next;
					this.append(t, "promoted", a.id(), a.name(), m.name);
					this.changed(t);
					this.notice(m.id, "promoted", t, next.key(), a.name());
					return OpResult.ok(m.name, next.key());
				} else {
					return fail("rank_too_low");
				}
			}
		}
	}

	public OpResult demote(Actor a, @Nullable UUID target) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!t.may(a.id(), Permission.RANKS)) {
			return fail("no_permission");
		} else {
			TeamData.Member m = t.member(target);
			if (m == null) {
				return fail("not_a_member");
			} else if (m.id.equals(a.id())) {
				return fail("not_yourself");
			} else {
				Rank mine = t.rankOf(a.id());
				if (mine != null && mine.above(m.rank)) {
					Rank next = m.rank.demoted();
					if (next == null) {
						return fail("cannot_demote");
					} else {
						m.rank = next;
						this.append(t, "demoted", a.id(), a.name(), m.name);
						this.changed(t);
						this.notice(m.id, "demoted", t, next.key(), a.name());
						return OpResult.ok(m.name, next.key());
					}
				} else {
					return fail("rank_too_low");
				}
			}
		}
	}

	public OpResult transfer(Actor a, @Nullable UUID target) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!t.may(a.id(), Permission.OWN)) {
			return fail("no_permission");
		} else {
			TeamData.Member m = t.member(target);
			if (m == null) {
				return fail("not_a_member");
			} else if (m.id.equals(a.id())) {
				return fail("not_yourself");
			} else {
				this.handOver(t, m, a);
				return OpResult.ok(m.name);
			}
		}
	}

	void handOver(TeamData t, TeamData.Member to, Actor by) {
		for (TeamData.Member m : t.members) {
			if (m.rank == Rank.OWNER) {
				m.rank = Rank.ADMIN;
			}
		}

		to.rank = Rank.OWNER;
		this.append(t, "transferred", by.id(), by.name(), to.name);
		this.changed(t);
		this.sync.directory(true);

		for (TeamData.Member mx : t.members) {
			if (!mx.id.equals(by.id())) {
				this.notice(mx.id, "transferred", t, to.name, by.name());
			}
		}
	}

	public OpResult ally(Actor a, @Nullable UUID teamId) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!t.may(a.id(), Permission.ALLIES)) {
			return fail("no_permission");
		} else {
			TeamData other = this.team(teamId);
			if (other == null) {
				return fail("no_such_team");
			} else if (other == t) {
				return fail("not_yourself");
			} else if (t.isAlly(other.id)) {
				return fail("already_ally");
			} else if (t.allies.size() >= this.cfg().maxAllies) {
				return fail("ally_limit");
			} else {
				t.allies.add(other.id);
				this.append(t, "ally", a.id(), a.name(), other.name);
				this.changed(t);
				this.sync.directory(true);
				boolean mutual = other.isAlly(t.id);

				for (TeamData.Member m : other.members) {
					this.notice(m.id, mutual ? "ally_mutual" : "ally", t, t.name, other.name);
				}

				return OpResult.ok(other.name);
			}
		}
	}

	public OpResult unally(Actor a, @Nullable UUID teamId) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!t.may(a.id(), Permission.ALLIES)) {
			return fail("no_permission");
		} else {
			TeamData other = this.store.get(teamId);
			if (teamId != null && t.allies.remove(teamId)) {
				String otherName = other == null ? "" : other.name;
				this.append(t, "unally", a.id(), a.name(), otherName);
				this.changed(t);
				this.sync.directory(true);
				if (other != null) {
					for (TeamData.Member m : other.members) {
						this.notice(m.id, "unally", t, t.name, other.name);
					}
				}

				return OpResult.ok(otherName);
			} else {
				return fail("not_ally");
			}
		}
	}

	public OpResult setHome(Actor a) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!t.may(a.id(), Permission.HOME)) {
			return fail("no_permission");
		} else {
			ServerPlayer p = a.player();
			if (p == null) {
				return fail("player_only");
			} else {
				TeamData.Place home = new TeamData.Place();
				home.dimension = p.level().dimension().location().toString();
				home.x = p.getX();
				home.y = p.getY();
				home.z = p.getZ();
				home.yaw = p.getYRot();
				home.pitch = p.getXRot();
				t.home = home;
				this.append(t, "home", a.id(), a.name(), "");
				this.changed(t);
				return OpResult.OK;
			}
		}
	}

	public OpResult clearHome(Actor a) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!t.may(a.id(), Permission.HOME)) {
			return fail("no_permission");
		} else if (t.home == null) {
			return fail("no_home");
		} else {
			t.home = null;
			this.changed(t);
			return OpResult.OK;
		}
	}

	public OpResult goHome(Actor a) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!this.cfg().homeTeleport) {
			return fail("disabled");
		} else if (t.home == null) {
			return fail("no_home");
		} else {
			ServerPlayer p = a.player();
			return p == null ? fail("player_only") : this.homes.start(p, t);
		}
	}

	public OpResult addWaypoint(Actor a, @Nullable String rawName, int x, int y, int z, @Nullable String dimension, @Nullable Integer color, @Nullable String icon) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (t.waypoints.size() >= this.cfg().maxWaypoints) {
			return fail("waypoint_limit");
		} else {
			String name = Names.line(rawName, 32);
			String dim = dimensionId(dimension != null ? dimension : a.dimension());
			if (!name.isEmpty() && dim != null) {
				TeamData.Waypoint w = new TeamData.Waypoint();

				do {
					w.id = UUID.randomUUID().toString().substring(0, 8);
				} while (t.waypoint(w.id) != null);

				w.name = name;
				w.dimension = dim;
				w.x = x;
				w.y = y;
				w.z = z;
				if (color != null) {
					w.color = color & 16777215;
				}

				w.icon = iconName(icon, w.icon);
				w.by = a.id();
				w.at = this.now();
				t.waypoints.add(w);
				this.append(t, "waypoint", a.id(), a.name(), name);
				this.changed(t);
				return OpResult.ok(name, w.id);
			} else {
				return fail("invalid");
			}
		}
	}

	public OpResult editWaypoint(Actor a, @Nullable String id, CompoundTag changes) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else {
			TeamData.Waypoint w = t.waypoint(id);
			if (w == null) {
				return fail("no_such_waypoint");
			} else if (!w.by.equals(a.id()) && !t.may(a.id(), Permission.WAYPOINTS)) {
				return fail("no_permission");
			} else {
				if (changes.contains("name", 8)) {
					String name = Names.line(changes.getString("name"), 32);
					if (name.isEmpty()) {
						return fail("invalid");
					}

					w.name = name;
				}

				if (changes.contains("dimension", 8)) {
					String dim = dimensionId(changes.getString("dimension"));
					if (dim == null) {
						return fail("invalid");
					}

					w.dimension = dim;
				}

				if (changes.contains("x", 99)) {
					w.x = changes.getInt("x");
				}

				if (changes.contains("y", 99)) {
					w.y = changes.getInt("y");
				}

				if (changes.contains("z", 99)) {
					w.z = changes.getInt("z");
				}

				if (changes.contains("color", 99)) {
					w.color = changes.getInt("color") & 16777215;
				}

				if (changes.contains("icon", 8)) {
					w.icon = iconName(changes.getString("icon"), w.icon);
				}

				this.changed(t);
				return OpResult.ok(w.name);
			}
		}
	}

	public OpResult removeWaypoint(Actor a, @Nullable String id) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else {
			TeamData.Waypoint w = t.waypoint(id);
			if (w == null) {
				return fail("no_such_waypoint");
			} else if (!w.by.equals(a.id()) && !t.may(a.id(), Permission.WAYPOINTS)) {
				return fail("no_permission");
			} else {
				t.waypoints.remove(w);
				this.changed(t);
				return OpResult.ok(w.name);
			}
		}
	}

	@Nullable
	TeamData.Waypoint waypointNamed(TeamData t, String nameOrId) {
		TeamData.Waypoint byId = t.waypoint(nameOrId);
		if (byId != null) {
			return byId;
		} else {
			for (TeamData.Waypoint w : t.waypoints) {
				if (w.name.equalsIgnoreCase(nameOrId.trim())) {
					return w;
				}
			}

			return null;
		}
	}

	private static String iconName(@Nullable String icon, String fallback) {
		if (icon == null) {
			return fallback;
		} else {
			String s = icon.trim().toLowerCase(Locale.ROOT);
			return !s.isEmpty() && s.length() <= 32 && s.matches("[a-z0-9_]+") ? s : fallback;
		}
	}

	@Nullable
	private static String dimensionId(@Nullable String raw) {
		if (raw == null) {
			return null;
		} else {
			ResourceLocation id = ResourceLocation.tryParse(raw.trim());
			return id == null ? null : id.toString();
		}
	}

	public OpResult claim(Actor a, @Nullable String dimension, long[] chunks) {
		TeamData t = this.store.ofPlayer(a.id());
		if (t == null) {
			return this.claimOwn(a, dimension, chunks);
		} else {
			return !this.mayClaim(t, a.id()) ? fail("no_permission") : this.claimFor(t, a, dimension, chunks);
		}
	}

	private OpResult claimOwn(Actor a, @Nullable String dimension, long[] chunks) {
		if (!a.isConsole() && this.cfg().soloLand) {
			TeamData t = this.personal(a);
			OpResult r = this.claimFor(t, a, dimension, chunks);
			if (!r.ok()) {
				this.store.removeMember(t, a.id());
				this.store.remove(t);
				return r;
			} else {
				this.sync.player(a.id());
				this.sync.directory(true);
				this.record("personal " + a.name());
				return r;
			}
		} else {
			return fail("not_in_team");
		}
	}

	private OpResult noLand(Actor a) {
		return fail(this.cfg().soloLand && !a.isConsole() ? "not_claimed" : "not_in_team");
	}

	private boolean mayClaim(TeamData t, UUID player) {
		Rank r = t.rankOf(player);
		return r != null && r.atLeast(this.pins.claimRank(t));
	}

	OpResult claimFor(TeamData t, Actor a, @Nullable String dimension, long[] chunks) {
		TeamsServerConfig c = this.cfg();
		if (!c.claims) {
			return fail("claims_disabled");
		} else {
			String dim = dimensionId(dimension != null ? dimension : a.dimension());
			if (dim != null && this.level(dim) != null) {
				if (c.claimDimensions != null && !c.claimDimensions.isEmpty() && !c.claimDimensions.contains(dim)) {
					return fail("wrong_dimension");
				} else {
					long[] asked = distinct(chunks);
					if (asked.length == 0) {
						return fail("invalid");
					} else {
						int limit = this.claimLimit(t);
						OpResult[] why = new OpResult[asked.length];
						boolean[] done = new boolean[asked.length];
						int taken = 0;
						boolean progress = true;

						while (progress) {
							progress = false;

							for (int i = 0; i < asked.length; i++) {
								if (!done[i]) {
									long chunk = asked[i];
									ClaimMap.Claim held = this.claims.at(dim, chunk);
									if (held != null) {
										why[i] = fail("already_claimed", held.team.name);
										done[i] = true;
									} else if (!a.admin() && !inReach(a, dim, chunk, c.claimReach)) {
										why[i] = fail("too_far");
										done[i] = true;
									} else if (this.claims.count(t.id) >= limit) {
										why[i] = fail("claim_limit", limit);
										done[i] = true;
									} else if (c.claimsMustTouch && this.claims.count(t.id) > 0 && !this.claims.touches(dim, chunk, t)) {
										why[i] = fail("must_touch");
									} else if (this.claims.put(dim, chunk, t) == null) {
										why[i] = fail("wrong_dimension");
										done[i] = true;
									} else {
										why[i] = null;
										done[i] = true;
										taken++;
										progress = true;
									}
								}
							}
						}

						if (taken == 0) {
							for (OpResult r : why) {
								if (r != null) {
									return r;
								}
							}

							return fail("invalid");
						} else {
							this.append(t, "claimed", a.id(), a.name(), String.valueOf(taken));
							this.changed(t);
							this.sync.directory(false);
							return OpResult.ok(taken, asked.length - taken);
						}
					}
				}
			} else {
				return fail("wrong_dimension");
			}
		}
	}

	public OpResult unclaim(Actor a, @Nullable String dimension, long[] chunks) {
		TeamData t = this.store.ofPlayer(a.id());
		if (t == null) {
			return this.noLand(a);
		} else if (!this.mayClaim(t, a.id())) {
			return fail("no_permission");
		} else {
			String dim = dimensionId(dimension != null ? dimension : a.dimension());
			if (dim == null) {
				return fail("wrong_dimension");
			} else {
				long[] asked = distinct(chunks);
				if (asked.length == 0) {
					return fail("invalid");
				} else {
					int freed = 0;

					for (long chunk : asked) {
						ClaimMap.Claim held = this.claims.at(dim, chunk);
						if (held != null && held.team == t) {
							this.free(dim, chunk, held);
							freed++;
						}
					}

					if (freed == 0) {
						return fail("not_claimed");
					} else {
						this.append(t, "unclaimed", a.id(), a.name(), String.valueOf(freed));
						this.changed(t);
						this.sync.directory(false);
						this.dropIfBare(t);
						return OpResult.ok(freed, asked.length - freed);
					}
				}
			}
		}
	}

	public OpResult unclaimAll(Actor a) {
		TeamData t = this.store.ofPlayer(a.id());
		if (t == null) {
			return this.noLand(a);
		} else {
			return !this.mayClaim(t, a.id()) ? fail("no_permission") : this.adminUnclaimAll(t, a);
		}
	}

	void free(String dimension, long chunk, ClaimMap.Claim held) {
		ClaimMap.Dim d = this.claims.dim(dimension);
		if (d != null) {
			this.forceLoads.release(d, chunk, held);
		}

		this.claims.remove(dimension, chunk);
	}

	public OpResult forceLoad(Actor a, @Nullable String dimension, long[] chunks, boolean on) {
		TeamData t = this.store.ofPlayer(a.id());
		if (t == null) {
			return this.noLand(a);
		} else if (!this.mayClaim(t, a.id())) {
			return fail("no_permission");
		} else if (!this.cfg().claims) {
			return fail("claims_disabled");
		} else {
			String dim = dimensionId(dimension != null ? dimension : a.dimension());
			ClaimMap.Dim d = dim == null ? null : this.claims.dim(dim);
			if (d == null) {
				return fail("not_claimed");
			} else {
				long[] asked = distinct(chunks);
				if (asked.length == 0) {
					return fail("invalid");
				} else {
					int limit = this.forceLimit(t);
					OpResult why = null;
					int changed = 0;

					for (long chunk : asked) {
						ClaimMap.Claim held = (ClaimMap.Claim)d.chunks.get(chunk);
						if (held != null && held.team == t) {
							if (held.forced != on) {
								if (!on || this.claims.forced(t.id) < limit) {
									this.claims.setForced(d, chunk, held, on);
									this.forceLoads.apply(d, chunk, held, null);
									changed++;
								} else if (why == null) {
									why = fail("force_load_limit", limit);
								}
							}
						} else if (why == null) {
							why = fail("not_claimed");
						}
					}

					return changed == 0 && why != null ? why : OpResult.ok(changed, asked.length - changed);
				}
			}
		}
	}

	private static boolean inReach(Actor a, String dimension, long chunk, int reach) {
		return a.dimension() != null && a.dimension().equals(dimension)
			? Math.max(Math.abs(ChunkPos.getX(chunk) - a.chunkX()), Math.abs(ChunkPos.getZ(chunk) - a.chunkZ())) <= Math.max(0, reach)
			: false;
	}

	private static long[] distinct(@Nullable long[] chunks) {
		if (chunks != null && chunks.length != 0) {
			LongLinkedOpenHashSet seen = new LongLinkedOpenHashSet();

			for (long c : chunks) {
				if (seen.size() >= 1024) {
					break;
				}

				seen.add(c);
			}

			return seen.toLongArray();
		} else {
			return new long[0];
		}
	}

	@Nullable
	ServerLevel level(String dimension) {
		ResourceLocation id = ResourceLocation.tryParse(dimension);
		return id == null ? null : this.server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
	}

	@Nullable
	public TeamData ownerOf(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
		ClaimMap.Claim c = this.claims.at(dimension, chunkX, chunkZ);
		return c == null ? null : c.team;
	}

	public OpResult chat(Actor a, @Nullable String raw) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else {
			String message = Names.line(raw, 256);
			if (message.isEmpty()) {
				return fail("message_empty");
			} else if (this.tooSoon(this.lastChat, a.id(), 400L)) {
				return fail("too_fast");
			} else {
				Component line = chatLine(t, a.name(), message);
				if (this.live) {
					for (TeamData.Member m : t.members) {
						ServerPlayer p = this.online(m.id);
						if (p != null) {
							p.sendSystemMessage(line);
						}
					}

					CFHGuiTeams.LOGGER.info("[CFHGui Teams] [{}] {}: {}", new Object[]{t.name, a.name(), message});
				}

				this.record("chat " + line.getString());
				return OpResult.OK;
			}
		}
	}

	static Component chatLine(TeamData t, String sender, String message) {
		String label = t.tag.isEmpty() ? t.name : t.tag;
		MutableComponent tag = Component.literal("[" + label + "] ")
			.withStyle(
				Style.EMPTY
					.withColor(t.color & 16777215)
					.withInsertion("cfhgui_teams:chat")
					.withHoverEvent(new HoverEvent(Action.SHOW_TEXT, Msg.tr("cfhgui_teams.chat.hover", t.name)))
			);
		return Component.empty().append(tag).append(Component.literal(sender).withStyle(Style.EMPTY.withInsertion(sender))).append(Component.literal(": " + message));
	}

	public OpResult ping(Actor a, double x, double y, double z, int kind, int entity) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!this.cfg().pings) {
			return fail("disabled");
		} else if (a.dimension() == null) {
			return fail("player_only");
		} else if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) {
			if (this.tooSoon(this.lastPing, a.id(), 250L)) {
				return fail("too_fast");
			} else {
				PingData ping = new PingData(a.id(), a.dimension(), x, y, z, Math.max(0, Math.min(4, kind)), Math.max(-1, entity));
				if (this.live) {
					TeamsNet.Ping payload = new TeamsNet.Ping(ping);

					for (TeamData.Member m : t.members) {
						ServerPlayer p = this.online(m.id);
						if (p != null) {
							TeamsNet.send(p, payload);
						}
					}
				}

				this.record("ping " + PingData.kindKey(ping.kind()));
				return OpResult.OK;
			}
		} else {
			return fail("invalid");
		}
	}

	private boolean tooSoon(Object2LongOpenHashMap<UUID> last, UUID player, long gapMs) {
		long now = System.currentTimeMillis();
		if (last.containsKey(player) && now - last.getLong(player) < gapMs) {
			return true;
		} else {
			last.put(player, now);
			return false;
		}
	}

	public OpResult openStash(Actor a) {
		TeamData t = this.teamOf(a.id());
		if (t == null) {
			return fail("not_in_team");
		} else if (!this.cfg().stash) {
			return fail("disabled");
		} else {
			ServerPlayer p = a.player();
			if (p == null) {
				return fail("player_only");
			} else {
				this.stashes.open(p, t);
				return OpResult.OK;
			}
		}
	}

	public OpResult sync(Actor a) {
		if (a.player() != null) {
			this.sync.everything(a.player());
		}

		return OpResult.OK;
	}

	public void addXp(TeamData t, long amount) {
		if (amount > 0L) {
			t.xp += amount;
			boolean levelled = false;

			while (t.xp >= TeamData.xpForLevel(t.level)) {
				t.xp = t.xp - TeamData.xpForLevel(t.level);
				t.level++;
				levelled = true;
				this.append(t, "level", new UUID(0L, 0L), "", String.valueOf(t.level));
			}

			this.changed(t);
			if (levelled) {
				this.sync.limits(t);
				this.sync.directory(false);

				for (TeamData.Member m : t.members) {
					this.notice(m.id, "level", t, String.valueOf(t.level), t.name);
				}
			}
		}
	}

	public void count(TeamData t, String counter, long delta) {
		if (counter != null && !counter.isEmpty() && delta != 0L) {
			t.counters.merge(counter, delta, Long::sum);
			this.changed(t);
			if (counter.equals("quests")) {
				this.sync.directory(false);
			}
		}
	}

	public void log(TeamData t, String type, UUID actor, String actorName, String subject) {
		this.append(t, type, actor, actorName, subject);
		this.changed(t);
	}

	private void append(TeamData t, String type, UUID actor, String actorName, String subject) {
		TeamData.LogEntry e = new TeamData.LogEntry();
		e.at = this.now();
		e.type = type;
		e.actor = actor;
		e.actorName = actorName == null ? "" : actorName;
		e.subject = subject == null ? "" : subject;
		t.log.add(e);

		while (t.log.size() > 60) {
			t.log.remove(0);
		}
	}

	void notice(UUID player, String key, TeamData team, Object... args) {
		this.record("notice " + key + " " + this.nameFor(player));
		if (this.live) {
			ServerPlayer p = this.online(player);
			if (p != null) {
				if (TeamsNet.has(p)) {
					TeamsNet.send(p, Sync.notice(key, team.color, args));
				} else if (key.equals("invited")) {
					p.sendSystemMessage(inviteLine(team.name, team.color, args.length > 1 ? String.valueOf(args[1]) : ""));
				} else {
					p.sendSystemMessage(prefix(team.color).append(Msg.tr("cfhgui_teams.notice." + key + ".body", args)));
				}
			}
		}
	}

	private String nameFor(UUID player) {
		String n = this.store.nameOf(player);
		return n == null ? player.toString() : n;
	}

	private static MutableComponent prefix(int color) {
		return Component.empty().append(Msg.tr("cfhgui_teams.chat.prefix").withStyle(Style.EMPTY.withColor(color & 16777215))).append(" ");
	}

	private static Component inviteLine(String teamName, int color, String byName) {
		MutableComponent accept = Msg.tr("cfhgui_teams.invite.accept")
			.withStyle(
				Style.EMPTY
					.withColor(6074742)
					.withBold(true)
					.withClickEvent(new ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/team accept " + teamName))
					.withHoverEvent(new HoverEvent(Action.SHOW_TEXT, Component.literal("/team accept " + teamName)))
			);
		MutableComponent decline = Msg.tr("cfhgui_teams.invite.decline")
			.withStyle(
				Style.EMPTY
					.withColor(15026253)
					.withClickEvent(new ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/team decline " + teamName))
					.withHoverEvent(new HoverEvent(Action.SHOW_TEXT, Component.literal("/team decline " + teamName)))
			);
		return prefix(color).append(Msg.tr("cfhgui_teams.notice.invited.body", teamName, byName)).append(" ").append(accept).append(" ").append(decline);
	}

	public OpResult run(Actor a, String op, CompoundTag args) {
		try {
			return switch (op) {
				case "create" -> this.create(a, args.getString("name"), args.getString("tag"), number(args, "color"));
				case "disband" -> this.disband(a);
				case "leave" -> this.leave(a);
				case "rename" -> this.rename(a, args.getString("name"));
				case "set_tag" -> this.setTag(a, args.getString("tag"));
				case "set_color" -> args.contains("color", 99) ? this.setColor(a, args.getInt("color")) : fail("invalid");
				case "set_motto" -> this.setMotto(a, args.getString("motto"));
				case "set_banner" -> this.setBanner(a, banner(args.getString("banner")));
				case "set_privacy" -> this.setPrivacy(a, Privacy.parse(args.getString("privacy"), null));
				case "set_friendly_fire" -> this.setFriendlyFire(a, args.getBoolean("on"));
				case "set_land_rule" -> this.setLandRule(a, args.getString("rule"), args.getString("value"));
				case "invite" -> this.invite(a, id(args, "id"), args.contains("player", 8) ? args.getString("player") : null);
				case "cancel_invite" -> this.cancelInvite(a, id(args, "id"));
				case "accept_invite" -> this.acceptInvite(a, id(args, "team"));
				case "decline_invite" -> this.declineInvite(a, id(args, "team"));
				case "join" -> this.join(a, id(args, "team"));
				case "cancel_request" -> this.cancelRequest(a, id(args, "team"));
				case "accept_request" -> this.acceptRequest(a, id(args, "id"));
				case "deny_request" -> this.denyRequest(a, id(args, "id"));
				case "kick" -> this.kick(a, id(args, "id"));
				case "promote" -> this.promote(a, id(args, "id"));
				case "demote" -> this.demote(a, id(args, "id"));
				case "transfer" -> this.transfer(a, id(args, "id"));
				case "ally" -> this.ally(a, id(args, "team"));
				case "unally" -> this.unally(a, id(args, "team"));
				case "set_home" -> this.setHome(a);
				case "clear_home" -> this.clearHome(a);
				case "go_home" -> this.goHome(a);
				case "add_waypoint" -> this.addWaypoint(
				a, args.getString("name"), args.getInt("x"), args.getInt("y"), args.getInt("z"), text(args, "dimension"), number(args, "color"), text(args, "icon")
			);
				case "edit_waypoint" -> this.editWaypoint(a, args.getString("id"), args);
				case "remove_waypoint" -> this.removeWaypoint(a, args.getString("id"));
				case "claim" -> args.getBoolean("server")
				? this.claimServerLand(a, text(args, "dimension"), args.getLongArray("chunks"))
				: this.claim(a, text(args, "dimension"), args.getLongArray("chunks"));
				case "unclaim" -> args.getBoolean("server")
				? this.unclaimServerLand(a, text(args, "dimension"), args.getLongArray("chunks"))
				: this.unclaim(a, text(args, "dimension"), args.getLongArray("chunks"));
				case "force_load" -> this.forceLoad(a, text(args, "dimension"), args.getLongArray("chunks"), !args.contains("on") || args.getBoolean("on"));
				case "chat" -> this.chat(a, args.getString("message"));
				case "ping" -> this.ping(
				a, args.getDouble("x"), args.getDouble("y"), args.getDouble("z"), args.getInt("kind"), args.contains("entity", 99) ? args.getInt("entity") : -1
			);
				case "open_stash" -> this.openStash(a);
				case "sync" -> this.sync(a);
				default -> fail("unknown");
			};
		} catch (RuntimeException var6) {
			CFHGuiTeams.LOGGER.warn("[CFHGui Teams] the action {} of {} failed", new Object[]{op, a.name(), var6});
			return fail("invalid");
		}
	}

	@Nullable
	private static UUID id(CompoundTag args, String key) {
		return args.hasUUID(key) ? args.getUUID(key) : null;
	}

	@Nullable
	private static Integer number(CompoundTag args, String key) {
		return args.contains(key, 99) ? args.getInt(key) : null;
	}

	@Nullable
	private static String text(CompoundTag args, String key) {
		return args.contains(key, 8) ? args.getString(key) : null;
	}

	@Nullable
	private static BannerData banner(String json) {
		if (json != null && !json.isEmpty() && json.length() <= 4096) {
			try {
				return (BannerData)JsonWire.GSON.fromJson(json, BannerData.class);
			} catch (RuntimeException var2) {
				return null;
			}
		} else {
			return null;
		}
	}

	@Nullable
	private UUID known(String name) {
		ServerPlayer p = this.live ? this.server.getPlayerList().getPlayerByName(name) : null;
		return p != null ? p.getUUID() : this.store.idOf(name);
	}

	OpResult adminAdd(String playerName, @Nullable TeamData t) {
		if (t == null) {
			return fail("no_such_team");
		} else {
			UUID id = this.known(playerName);
			if (id == null) {
				return fail("no_such_player", playerName);
			} else if (this.teamOf(id) != null) {
				return fail("player_in_team");
			} else if (t.members.size() >= this.cfg().maxMembers) {
				return fail("team_full");
			} else {
				ServerPlayer p = this.live ? this.online(id) : null;
				String name = p != null ? p.getGameProfile().getName() : this.store.nameOf(id);
				this.enter(t, id, name == null ? playerName : name);
				return OpResult.ok(name == null ? playerName : name);
			}
		}
	}

	OpResult adminRemove(String playerName, Actor by) {
		UUID id = this.known(playerName);
		TeamData t = id == null ? null : this.store.ofPlayer(id);
		TeamData.Member m = t == null ? null : t.member(id);
		if (m == null) {
			return fail("no_such_player", playerName);
		} else if (t.solo) {
			this.drop(t);
			return OpResult.ok(m.name);
		} else if (m.rank == Rank.OWNER) {
			if (t.members.size() > 1) {
				return fail("owner_must_transfer");
			} else {
				this.dissolve(t, by);
				return OpResult.ok(m.name);
			}
		} else {
			this.remove(t, m, by);
			return OpResult.ok(m.name);
		}
	}

	OpResult adminOwner(String playerName, Actor by) {
		UUID id = this.known(playerName);
		TeamData t = id == null ? null : this.teamOf(id);
		TeamData.Member m = t == null ? null : t.member(id);
		if (m == null) {
			return fail("no_such_player", playerName);
		} else {
			if (m.rank != Rank.OWNER) {
				this.handOver(t, m, by);
			}

			return OpResult.ok(m.name);
		}
	}

	OpResult adminUnclaim(Actor a, long[] chunks) {
		String dim = a.dimension();
		if (dim == null) {
			return fail("player_only");
		} else {
			int freed = 0;
			Set<TeamData> touched = new HashSet<>();

			for (long chunk : distinct(chunks)) {
				ClaimMap.Claim held = this.claims.at(dim, chunk);
				if (held != null) {
					this.free(dim, chunk, held);
					touched.add(held.team);
					freed++;
				}
			}

			if (freed == 0) {
				return fail("not_claimed");
			} else {
				for (TeamData t : touched) {
					if (t != this.serverLand.data) {
						this.changed(t);
					}
				}

				this.sync.directory(touched.contains(this.serverLand.data));

				for (TeamData tx : touched) {
					this.dropIfBare(tx);
				}

				return OpResult.ok(freed);
			}
		}
	}

	OpResult adminUnclaimAll(TeamData t, Actor a) {
		List<ClaimMap.Entry> all = this.claims.of(t);
		if (all.isEmpty()) {
			return fail("not_claimed");
		} else {
			for (ClaimMap.Entry e : all) {
				this.free(e.dim().id, e.chunk(), e.claim());
			}

			this.append(t, "unclaimed", a.id(), a.name(), String.valueOf(all.size()));
			this.changed(t);
			this.sync.directory(false);
			this.dropIfBare(t);
			return OpResult.ok(all.size());
		}
	}

	public boolean isServerLand(@Nullable TeamData land) {
		return land == this.serverLand.data;
	}

	public ServerLand serverLand() {
		return this.serverLand;
	}

	public OpResult claimServerLand(Actor a, @Nullable String dimension, long[] chunks) {
		if (!a.admin()) {
			return fail("operators_only");
		} else {
			String dim = dimensionId(dimension != null ? dimension : a.dimension());
			if (dim != null && this.level(dim) != null) {
				long[] asked = distinct(chunks);
				if (asked.length == 0) {
					return fail("invalid");
				} else {
					TeamData land = this.serverLand.data;
					TeamData inTheWay = null;
					int taken = 0;

					for (long chunk : asked) {
						ClaimMap.Claim held = this.claims.at(dim, chunk);
						if (held != null) {
							if (inTheWay == null || inTheWay == land && held.team != land) {
								inTheWay = held.team;
							}
						} else if (this.claims.put(dim, chunk, land) != null) {
							taken++;
						}
					}

					if (taken == 0) {
						return inTheWay != null ? fail("already_claimed", inTheWay.name) : fail("wrong_dimension");
					} else {
						this.sync.directory(true);
						if (this.live) {
							CFHGuiTeams.LOGGER.info("[CFHGui Teams] {} claimed {} chunk(s) in {} for the server ({})", new Object[]{a.name(), taken, dim, land.name});
						}

						return OpResult.ok(taken, asked.length - taken);
					}
				}
			} else {
				return fail(dimension == null && a.dimension() == null ? "player_only" : "wrong_dimension");
			}
		}
	}

	public OpResult unclaimServerLand(Actor a, @Nullable String dimension, long[] chunks) {
		if (!a.admin()) {
			return fail("operators_only");
		} else {
			String dim = dimensionId(dimension != null ? dimension : a.dimension());
			if (dim == null) {
				return fail(a.dimension() == null ? "player_only" : "wrong_dimension");
			} else {
				long[] asked = distinct(chunks);
				if (asked.length == 0) {
					return fail("invalid");
				} else {
					int freed = 0;

					for (long chunk : asked) {
						ClaimMap.Claim held = this.claims.at(dim, chunk);
						if (held != null && held.team == this.serverLand.data) {
							this.free(dim, chunk, held);
							freed++;
						}
					}

					if (freed == 0) {
						return fail("not_server_land");
					} else {
						this.sync.directory(true);
						if (this.live) {
							CFHGuiTeams.LOGGER.info("[CFHGui Teams] {} gave up {} chunk(s) of the server's land in {}", new Object[]{a.name(), freed, dim});
						}

						return OpResult.ok(freed, asked.length - freed);
					}
				}
			}
		}
	}

	OpResult nameServerLand(Actor a, @Nullable String raw) {
		if (!a.admin()) {
			return fail("operators_only");
		} else {
			String name = Names.line(raw, 24);
			if (name.isEmpty()) {
				return fail("invalid");
			} else {
				this.serverLand.data.name = name;
				this.serverLand.touch();
				this.sync.directory(true);
				return OpResult.ok(name);
			}
		}
	}

	OpResult colorServerLand(Actor a, int rgb) {
		if (!a.admin()) {
			return fail("operators_only");
		} else {
			this.serverLand.data.color = rgb & 16777215;
			this.serverLand.touch();
			this.sync.directory(true);
			return OpResult.OK;
		}
	}

	OpResult ruleServerLand(Actor a, @Nullable String rawRule, @Nullable String rawValue) {
		if (!a.admin()) {
			return fail("operators_only");
		} else {
			String rule = LandPins.canonical(rawRule);
			if (rule != null && rawValue != null && ServerLand.set(this.serverLand.data.land, rule, rawValue)) {
				this.serverLand.touch();
				return OpResult.ok(rule, this.serverLandRule(rule));
			} else {
				return fail("invalid");
			}
		}
	}

	String serverLandRule(String rule) {
		return ServerLand.get(this.serverLand.data.land, rule);
	}
}
