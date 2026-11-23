package com.onehumanawa.cfhcore.content.gui.server.service;

import com.mojang.authlib.GameProfile;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.ServerConfigs;
import com.onehumanawa.cfhcore.content.gui.server.CFHGuiServer;
import com.onehumanawa.cfhcore.content.gui.server.api.ServerApi;
import com.onehumanawa.cfhcore.content.gui.server.model.AdminData;
import com.onehumanawa.cfhcore.content.gui.server.model.Hello;
import com.onehumanawa.cfhcore.content.gui.server.model.Kit;
import com.onehumanawa.cfhcore.content.gui.server.model.KitBook;
import com.onehumanawa.cfhcore.content.gui.server.model.OnlinePlayer;
import com.onehumanawa.cfhcore.content.gui.server.model.Place;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import com.onehumanawa.cfhcore.content.gui.server.model.RoleBook;
import com.onehumanawa.cfhcore.content.gui.server.model.SelfState;
import com.onehumanawa.cfhcore.content.gui.server.model.ServerRules;
import com.onehumanawa.cfhcore.content.gui.server.model.Warp;
import com.onehumanawa.cfhcore.content.gui.server.model.WarpList;
import com.onehumanawa.cfhcore.content.gui.server.net.ServerNet;
import com.onehumanawa.cfhcore.content.gui.server.permission.RolePerms;
import com.onehumanawa.cfhcore.content.gui.server.platform.ServerPlatform;
import com.onehumanawa.cfhcore.content.gui.server.storage.AuditLog;
import com.onehumanawa.cfhcore.content.gui.server.storage.Saver;
import com.onehumanawa.cfhcore.content.gui.server.storage.StatsFiles;
import com.onehumanawa.cfhcore.content.gui.server.storage.WorldStore;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

public final class ServerService implements RolePerms.Source {
	private static final Set<String> CONSOLE_NAMES = Set.of("server", "console", "rcon");
	final MinecraftServer server;
	final boolean live;
	final ServerService.Books books;
	final WorldStore store;
	final AuditLog audit;
	final Saver saver;
	final StatsFiles statsFiles = new StatsFiles();
	final RolePerms perms;
	final Sync sync;
	final Teleports teleports;
	final Requests requests;
	final Travel travel;
	final People people;
	final Statistics statistics;
	final Moderation moderation;
	final Admin admin;
	final Vanish vanish;
	final Ops ops;
	final Map<UUID, Session> sessions = Collections.synchronizedMap(new LinkedHashMap<>());
	private LongSupplier clock = System::currentTimeMillis;
	@Nullable
	List<String> trace;
	int ticks;
	private final long startedAt;
	private long announcedAt;
	private int announcement;

	private ServerService(MinecraftServer server, boolean live, ServerService.Books books, @Nullable Path dir) {
		this.server = server;
		this.live = live;
		this.books = books;
		this.saver = new Saver();
		this.store = new WorldStore(dir);
		this.audit = new AuditLog(dir == null ? null : dir.resolve("audit.jsonl"), this.saver);
		this.perms = new RolePerms(this, live);
		this.sync = new Sync(this);
		this.teleports = new Teleports(this);
		this.requests = new Requests(this);
		this.travel = new Travel(this);
		this.people = new People(this);
		this.statistics = new Statistics(this);
		this.moderation = new Moderation(this);
		this.admin = new Admin(this);
		this.vanish = new Vanish(this);
		this.ops = new Ops(this);
		this.startedAt = System.currentTimeMillis();
		this.announcedAt = this.startedAt;
	}

	static ServerService start(MinecraftServer server) {
		ServerService.Books books = new ServerService.ConfigBooks();
		books.reload();
		ServerService s = new ServerService(server, true, books, server.getWorldPath(LevelResource.ROOT).resolve("slate").resolve("server"));
		long now = s.now();
		s.store.load(now);
		s.audit.load(books.rules().auditDays, now);
		s.declareNamedNodes();
		s.statistics.refresh();
		String other = ServerPlatform.get().externalPermissions();
		CFHGuiServer.LOGGER
			.info(
				"[CFHGui Server] started: {} players known, {} warps, {} roles, {} kits{}",
				new Object[]{
					s.store.size(),
					s.store.warps().size(),
					books.roles().roles.size(),
					books.kits().kits.size(),
					other == null ? "" : "; permissions are asked of " + other + " first"
				}
			);
		return s;
	}

	static ServerService scratch(MinecraftServer server, @Nullable Path dir, ServerRules rules, RoleBook roles, KitBook kits) {
		ServerService s = new ServerService(server, false, new ServerService.ScratchBooks(rules, roles, kits), dir);
		s.store.load(s.now());
		s.audit.load(0, s.now());
		return s;
	}

	@Override
	public long now() {
		return this.clock.getAsLong();
	}

	void clock(LongSupplier clock) {
		this.clock = clock;
	}

	@Override
	public ServerRules rules() {
		return this.books.rules();
	}

	@Override
	public RoleBook roles() {
		return this.books.roles();
	}

	public KitBook kits() {
		return this.books.kits();
	}

	@Override
	public List<String> grants(UUID player) {
		Session session = this.sessions.get(player);
		PlayerRecord rec = session == null ? null : session.record;
		if (rec == null) {
			WorldStore.Known k = this.store.known(player);
			if (k == null || k.roles.isEmpty()) {
				return List.of();
			}

			if (k.rolesUntil == 0L || this.now() < k.rolesUntil || !this.server.isSameThread()) {
				return k.roles;
			}

			rec = this.store.record(player);
		}

		if (rec != null && !rec.roles.isEmpty()) {
			long now = this.now();
			List<String> out = new ArrayList<>(rec.roles.size());

			for (Role.Grant g : rec.roles) {
				if (!g.expired(now)) {
					out.add(g.role);
				}
			}

			return out;
		} else {
			return List.of();
		}
	}

	@Override
	public long validUntil(UUID player) {
		Session session = this.sessions.get(player);
		if (session == null) {
			WorldStore.Known k = this.store.known(player);
			if (k == null || k.rolesUntil == 0L) {
				return Long.MAX_VALUE;
			}

			if (this.now() < k.rolesUntil) {
				return k.rolesUntil;
			}

			if (!this.server.isSameThread()) {
				return this.now() + 1000L;
			}
		}

		PlayerRecord rec = session != null ? session.record : this.store.record(player);
		long until = Long.MAX_VALUE;
		long now = this.now();
		if (rec != null) {
			for (Role.Grant g : rec.roles) {
				if (g.expiresAt > now) {
					until = Math.min(until, g.expiresAt);
				}
			}
		}

		return until;
	}

	public boolean may(Actor actor, Node node) {
		return this.perms.may(actor, node);
	}

	int limit(Actor actor, Node node, int rule) {
		Integer byRole = actor.console() ? null : this.perms.number(actor.id(), actor.level(), node.id());
		if (byRole != null) {
			return Math.max(0, byRole);
		} else if (this.rules().operatorsHaveEverything && this.perms.rank(actor) == Integer.MAX_VALUE) {
			return node == Nodes.HOMES_MAX ? Math.max(rule, 100) : 0;
		} else {
			return Math.max(0, rule);
		}
	}

	boolean mayNamed(Actor actor, String node) {
		return this.perms.may(actor, node, 2);
	}

	void declareNamedNodes() {
		if (this.live) {
			for (Warp w : this.store.warps()) {
				if (w.restricted) {
					Perms.declare(w.node(), 2);
				}
			}

			for (Kit k : this.kits().kits) {
				if (k.restricted) {
					Perms.declare(k.node(), 2);
				}
			}
		}
	}

	int levelOf(UUID id, String name) {
		Session s = this.sessions.get(id);
		if (s != null && s.player == null) {
			return s.fakeLevel;
		} else {
			return !this.live && s == null ? 0 : this.server.getProfilePermissions(new GameProfile(id, name));
		}
	}

	boolean ownerOf(UUID id, String name) {
		Session s = this.sessions.get(id);
		return s != null && s.player == null ? false : (this.live || s != null) && !name.isEmpty() && this.server.isSingleplayerOwner(new GameProfile(id, name));
	}

	int rankOf(ServerService.Target t) {
		return this.perms.rank(t.id(), this.levelOf(t.id(), t.name()), this.ownerOf(t.id(), t.name()));
	}

	boolean outranks(Actor actor, ServerService.Target t) {
		if (actor.console()) {
			return true;
		} else {
			return actor.id().equals(t.id()) ? false : this.perms.outranks(actor, this.rankOf(t));
		}
	}

	void permissionsChanged(@Nullable UUID player) {
		if (player == null) {
			this.perms.changed();
		} else {
			this.perms.changed(player);
		}

		if (player == null) {
			this.sync.markAll("hello", "self", "warps", "online");
		} else {
			this.sync.mark(player, "hello", "self", "warps");
			this.sync.markAll("online");
		}

		if (this.live) {
			for (Session s : this.sessions.values()) {
				if (s.player != null && (player == null || player.equals(s.id))) {
					this.server.getCommands().sendCommands(s.player);
					this.refreshName(s.player);
				}
			}

			ServerApi.PERMISSIONS_CHANGED.invoke(l -> l.accept(player));
		}
	}

	void refreshName(ServerPlayer player) {
		if (this.live) {
			this.server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(Action.UPDATE_DISPLAY_NAME, player));
		}
	}

	@Nullable
	public Session session(@Nullable UUID id) {
		return id == null ? null : this.sessions.get(id);
	}

	@Nullable
	Session session(Actor actor) {
		return actor.console() ? null : this.sessions.get(actor.id());
	}

	Collection<Session> online() {
		return this.sessions.values();
	}

	boolean visible(Actor viewer, Session other) {
		return !other.record.vanished || viewer.console() || viewer.id().equals(other.id) || this.may(viewer, Nodes.MOD_SEE_VANISHED);
	}

	long lastSeen(Actor viewer, WorldStore.Known k) {
		Session session = this.sessions.get(k.id);
		return session != null && !this.visible(viewer, session) ? session.seenBefore : k.lastSeen;
	}

	@Nullable
	public ServerService.Target target(@Nullable UUID id, @Nullable String name) {
		UUID who = id;
		boolean byNickname = false;
		if (id == null && name != null && !name.isBlank()) {
			String wanted = name.trim();

			for (Session s : this.sessions.values()) {
				if (s.name.equalsIgnoreCase(wanted)) {
					who = s.id;
					break;
				}
			}

			if (who == null) {
				who = this.store.idOfAccount(wanted);
			}

			if (who == null) {
				who = this.store.idOf(wanted);
				byNickname = who != null;
			}
		}

		if (who == null) {
			return null;
		} else {
			Session sx = this.sessions.get(who);
			if (sx != null) {
				return new ServerService.Target(who, sx.name, sx, sx.record, byNickname);
			} else {
				PlayerRecord rec = this.store.record(who);
				if (rec == null) {
					return null;
				} else {
					this.dropExpiredGrants(rec, null);
					return new ServerService.Target(who, rec.name, null, rec, byNickname);
				}
			}
		}
	}

	@Nullable
	OpResult nicknamed(@Nullable ServerService.Target t) {
		return t != null && t.byNickname() ? OpResult.fail("nickname_given", Text.shown(t.record()), t.name()) : null;
	}

	boolean nameReserved(String name) {
		if (CONSOLE_NAMES.contains(name.toLowerCase(Locale.ROOT))) {
			return true;
		} else {
			for (String operator : this.server.getPlayerList().getOpNames()) {
				if (operator.equalsIgnoreCase(name)) {
					return true;
				}
			}

			for (String listed : this.server.getPlayerList().getWhiteListNames()) {
				if (listed.equalsIgnoreCase(name)) {
					return true;
				}
			}

			return false;
		}
	}

	void touch(PlayerRecord rec) {
		this.store.touch(rec, this.now());
	}

	void dropExpiredGrants(PlayerRecord rec, @Nullable Session s) {
		if (!rec.roles.isEmpty()) {
			long now = this.now();
			RoleBook book = this.roles();
			if (rec.roles.removeIf(gx -> book.get(gx.role) == null)) {
				this.touch(rec);
				this.perms.changed(rec.id);
			}

			List<Role.Grant> gone = null;

			for (Role.Grant g : rec.roles) {
				if (g.expired(now)) {
					if (gone == null) {
						gone = new ArrayList<>(1);
					}

					gone.add(g);
				}
			}

			if (gone != null) {
				rec.roles.removeAll(gone);
				this.touch(rec);
				if (s == null) {
					this.perms.changed(rec.id);
				} else {
					for (Role.Grant gx : gone) {
						Role role = this.roles().get(gx.role);
						String shown = role == null ? gx.role : role.name;
						this.notice(s, "role_revoked", role == null ? 0 : role.color, null, shown);
					}

					this.permissionsChanged(rec.id);
				}
			}
		}
	}

	public Travel travel() {
		return this.travel;
	}

	public Requests requests() {
		return this.requests;
	}

	public People people() {
		return this.people;
	}

	public Statistics statistics() {
		return this.statistics;
	}

	public Moderation moderation() {
		return this.moderation;
	}

	public Admin admin() {
		return this.admin;
	}

	public void flush() {
		this.sync.flush();
	}

	public boolean present(Actor actor) {
		return this.session(actor) != null;
	}

	@Nullable
	public SelfState self(Actor actor) {
		Session session = this.session(actor);
		return session == null ? null : this.sync.self(session);
	}

	public Hello hello(Actor actor) {
		return this.sync.hello(actor);
	}

	public WarpList warps(Actor actor) {
		return this.sync.warps(actor);
	}

	public PlayerRecord.Mail[] mail(Actor actor) {
		Session session = this.session(actor);
		return session == null ? new PlayerRecord.Mail[0] : Sync.mail(session.record);
	}

	public OnlinePlayer[] online(Actor actor) {
		return this.sync.online(actor);
	}

	public List<String> askers(Actor actor) {
		return actor.console() ? List.of() : this.requests.askers(actor.id());
	}

	public List<String> knownNames(Actor actor, String typed, int limit) {
		String prefix = typed.toLowerCase(Locale.ROOT);
		List<String> out = new ArrayList<>();

		for (Session s : this.sessions.values()) {
			if (out.size() >= limit) {
				return out;
			}

			if (this.visible(actor, s) && s.name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
				out.add(s.name);
			}
		}

		List<WorldStore.Known> away = new ArrayList<>();

		for (WorldStore.Known k : this.store.all()) {
			Session on = this.sessions.get(k.id);
			if ((on == null || !this.visible(actor, on)) && !k.name.isEmpty() && k.name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
				away.add(k);
			}
		}

		away.sort(Comparator.<WorldStore.Known>comparingLong(kx -> this.lastSeen(actor, kx)).reversed());

		for (WorldStore.Known kx : away) {
			if (out.size() >= limit) {
				break;
			}

			out.add(kx.name);
		}

		return out;
	}

	public boolean open(ServerPlayer player, String what, String arg) {
		if (this.live && ServerNet.has(player)) {
			ServerNet.send(player, new ServerNet.Open(what, arg));
			return true;
		} else {
			return false;
		}
	}

	void notice(Session s, String key, int color, @Nullable Component chat, Object... args) {
		List<String> strings = new ArrayList<>(args.length);

		for (Object a : args) {
			if (strings.size() >= 8) {
				break;
			}

			String text = String.valueOf(a);
			strings.add(text.length() > 256 ? text.substring(0, 256) : text);
		}

		if (this.trace != null) {
			this.trace.add(s.name + " <- " + key + strings);
		}

		ServerPlayer p = s.player;
		if (this.live && p != null) {
			if (ServerNet.has(p)) {
				ServerNet.send(p, new ServerNet.Notice(key, strings, color & 16777215));
			} else {
				p.sendSystemMessage((Component)(chat != null ? chat : Msg.tr("cfhgui_server.notice." + key + ".body", strings.toArray())));
			}
		}
	}

	void say(Session s, String what, Component line) {
		if (this.trace != null) {
			this.trace.add(s.name + " <- " + what);
		}

		if (this.live && s.player != null) {
			s.player.sendSystemMessage(line);
		}
	}

	void sayAll(String what, Component line) {
		if (this.trace != null) {
			this.trace.add("* <- " + what);
		}

		if (this.live) {
			for (Session s : this.sessions.values()) {
				if (s.player != null) {
					s.player.sendSystemMessage(line);
				}
			}
		}
	}

	void audit(Actor actor, String action, @Nullable UUID target, String targetName, String detail) {
		AdminData.AuditEntry e = new AdminData.AuditEntry();
		e.at = this.now();
		e.actor = actor.console() ? Actor.NOBODY : actor.id();
		e.actorName = actor.name();
		e.action = action;
		e.target = target;
		e.targetName = targetName == null ? "" : targetName;
		e.detail = detail == null ? "" : Text.line(detail, 200);
		this.audit.add(e);
		if (this.live) {
			String english = Msg.english("cfhgui_server.audit." + action);

			try {
				CFHGuiServer.LOGGER
					.info(
						"[CFHGui Server] audit: {}", english == null ? action + " " + e.targetName + " " + e.detail : String.format(english, e.actorName, e.targetName, e.detail)
					);
			} catch (RuntimeException var9) {
				CFHGuiServer.LOGGER.info("[CFHGui Server] audit: {} {} {} {}", new Object[]{e.actorName, action, e.targetName, e.detail});
			}
		}
	}

	void tick() {
		this.ticks++;
		this.teleports.tick();
		this.moderation.tick();
		if (this.ticks % 10 == 0) {
			this.people.tickIdle();
		}

		if (this.ticks % 20 == 0) {
			this.requests.tick();

			for (Session s : new ArrayList<>(this.sessions.values())) {
				this.dropExpiredGrants(s.record, s);
				int level = s.player == null ? s.levelSeen : this.server.getProfilePermissions(s.player.getGameProfile());
				if (level != s.levelSeen) {
					s.levelSeen = level;
					this.permissionsChanged(s.id);
				}
			}

			this.admin.tickDashboard();
			this.statistics.tick();
			this.announce();
		}

		if (this.ticks % 6000 == 0) {
			for (Session sx : this.sessions.values()) {
				this.touch(sx.record);
			}
		}

		this.store.tick(this.saver);
		this.sync.flush();
	}

	private void announce() {
		ServerRules rules = this.rules();
		if (this.live && !rules.announcements.isEmpty() && !this.sessions.isEmpty()) {
			long now = System.currentTimeMillis();
			if (now - this.announcedAt >= (long)rules.announcementMinutes * 60000L) {
				this.announcedAt = now;
				String line = rules.announcements.get(Math.floorMod(this.announcement++, rules.announcements.size()));
				if (line != null && !line.isBlank()) {
					this.sayAll("announcement", Msg.tr("cfhgui_server.chat.announcement", Text.styled(line)));
				}
			}
		}
	}

	void joined(ServerPlayer player) {
		if (!ServerPlatform.get().isFakePlayer(player)) {
			Session known = this.sessions.get(player.getUUID());
			if (known != null) {
				known.player = player;
			} else {
				long now = this.now();
				UUID id = player.getUUID();
				PlayerRecord rec = this.store.open(id, player.getGameProfile().getName(), now);
				boolean first = rec.sessions == 0L;
				long seenBefore = rec.lastSeen;
				if (rec.firstSeen == 0L) {
					rec.firstSeen = now;
				}

				rec.sessions++;
				rec.lastSeen = now;
				Session s = new Session(id, player.getGameProfile().getName(), player, rec, now, 0);
				s.seenBefore = seenBefore;
				this.sessions.put(id, s);
				this.perms.changed(id);
				this.dropExpiredGrants(rec, s);
				this.moderation.joined(s);
				this.people.joined(s, first);
				this.vanish.joined(s);
				this.touch(rec);
				this.sync.joined(s);
				this.sync.markAll("online");
				this.refreshName(player);
			}
		}
	}

	void left(ServerPlayer player) {
		Session s = this.sessions.get(player.getUUID());
		if (s != null) {
			this.leaving(s);
			this.sessions.remove(s.id);
			this.store.release(s.id);
			this.sync.forget(s.id);
			this.sync.markAll("online");
		}
	}

	private void leaving(Session s) {
		this.teleports.drop(s);
		this.requests.left(s.id);
		this.people.left(s);
		s.record.lastSeen = this.now();
		s.record.lastPlace = s.place();
		if (s.player != null && this.live) {
			this.statistics.left(s.player);
		}

		this.touch(s.record);
	}

	Session enter(UUID id, String name, int level, @Nullable Place at, @Nullable ServerPlayer player) {
		long now = this.now();
		PlayerRecord rec = this.store.open(id, name, now);
		if (rec.firstSeen == 0L) {
			rec.firstSeen = now;
		}

		long seenBefore = rec.lastSeen;
		rec.sessions++;
		rec.lastSeen = now;
		Session s = new Session(id, name, player, rec, now, level);
		s.seenBefore = seenBefore;
		s.fakePlace = at;
		this.sessions.put(id, s);
		this.perms.changed(id);
		this.moderation.joined(s);
		this.touch(rec);
		this.sync.markAll("online");
		return s;
	}

	void leave(UUID id) {
		Session s = this.sessions.get(id);
		if (s != null) {
			this.leaving(s);
			this.sessions.remove(id);
			this.store.release(id);
			this.sync.forget(id);
			this.sync.markAll("online");
		}
	}

	void copied(ServerPlayer from, ServerPlayer to, boolean alive) {
		Session s = this.sessions.get(to.getUUID());
		if (s != null) {
			s.player = to;
			this.moderation.copied(s, to);
		}
	}

	void died(ServerPlayer player) {
		Session s = this.sessions.get(player.getUUID());
		if (s != null) {
			this.travel.died(s);
		}
	}

	boolean chat(ServerPlayer player, String message) {
		Session s = this.sessions.get(player.getUUID());
		return s == null || this.moderation.chatAllowed(s);
	}

	boolean damage(ServerPlayer player, DamageSource source, float amount) {
		Session s = this.sessions.get(player.getUUID());
		if (s == null) {
			return true;
		} else if (!this.moderation.damageAllowed(s, player, source)) {
			return false;
		} else {
			if (amount > 0.0F) {
				this.teleports.hurt(s);
			}

			return true;
		}
	}

	void reload() {
		this.books.reload();
		this.declareNamedNodes();
		this.rulesChanged();
	}

	void rulesChanged() {
		this.permissionsChanged(null);
		this.sync.markAll("hello", "self", "warps");
		if (this.live) {
			for (Session s : this.sessions.values()) {
				if (s.player != null) {
					s.player.serverLevel().updateSleepingPlayerList();
				}
			}
		}
	}

	void worldSaved() {
		for (Session s : this.sessions.values()) {
			this.touch(s.record);
		}

		this.store.save(this.saver);
	}

	void stop() {
		for (Session s : new ArrayList<>(this.sessions.values())) {
			this.leaving(s);
		}

		this.sessions.clear();
		this.store.save(this.saver);
		this.saver.close();
	}

	long uptimeMs() {
		return System.currentTimeMillis() - this.startedAt;
	}

	interface Books {
		ServerRules rules();

		RoleBook roles();

		KitBook kits();

		void saveRules();

		void saveRoles();

		void saveKits();

		void reload();
	}

	private static final class ConfigBooks implements ServerService.Books {
		@Override
		public ServerRules rules() {
			return ServerConfigs.rules();
		}

		@Override
		public RoleBook roles() {
			return ServerConfigs.roles();
		}

		@Override
		public KitBook kits() {
			return ServerConfigs.kits();
		}

		@Override
		public void saveRules() {
			ServerConfigs.rulesFile().save();
		}

		@Override
		public void saveRoles() {
			ServerConfigs.rolesFile().save();
		}

		@Override
		public void saveKits() {
			ServerConfigs.kitsFile().save();
		}

		@Override
		public void reload() {
			ServerConfigs.reload();
		}
	}

	private static final class ScratchBooks implements ServerService.Books {
		private final ServerRules rules;
		private final RoleBook roles;
		private final KitBook kits;

		ScratchBooks(ServerRules rules, RoleBook roles, KitBook kits) {
			this.rules = rules;
			this.roles = roles;
			this.kits = kits;
		}

		@Override
		public ServerRules rules() {
			return this.rules;
		}

		@Override
		public RoleBook roles() {
			return this.roles;
		}

		@Override
		public KitBook kits() {
			return this.kits;
		}

		@Override
		public void saveRules() {
		}

		@Override
		public void saveRoles() {
		}

		@Override
		public void saveKits() {
		}

		@Override
		public void reload() {
		}
	}

	public static record Target(UUID id, String name, @Nullable Session session, PlayerRecord record, boolean byNickname) {
		public boolean online() {
			return this.session != null;
		}

		@Nullable
		public ServerPlayer player() {
			return this.session == null ? null : this.session.player;
		}
	}
}
