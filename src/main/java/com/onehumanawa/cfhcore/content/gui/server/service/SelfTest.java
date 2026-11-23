package com.onehumanawa.cfhcore.content.gui.server.service;

import com.mojang.authlib.GameProfile;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.Board;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.PlayerRef;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.Row;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.Snapshot;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.CFHGuiServer;
import com.onehumanawa.cfhcore.content.gui.server.model.AdminData;
import com.onehumanawa.cfhcore.content.gui.server.model.Hello;
import com.onehumanawa.cfhcore.content.gui.server.model.Home;
import com.onehumanawa.cfhcore.content.gui.server.model.Kit;
import com.onehumanawa.cfhcore.content.gui.server.model.KitBook;
import com.onehumanawa.cfhcore.content.gui.server.model.OnlinePlayer;
import com.onehumanawa.cfhcore.content.gui.server.model.Place;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerCard;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import com.onehumanawa.cfhcore.content.gui.server.model.Punishment;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import com.onehumanawa.cfhcore.content.gui.server.model.RoleBook;
import com.onehumanawa.cfhcore.content.gui.server.model.SelfState;
import com.onehumanawa.cfhcore.content.gui.server.model.ServerRules;
import com.onehumanawa.cfhcore.content.gui.server.model.TeleportRequest;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class SelfTest {
	private static final String OVERWORLD = "minecraft:overworld";
	private static final String NETHER = "minecraft:the_nether";
	private static final String PLAY = StatsLink.key("minecraft:custom", "minecraft:play_time");
	private final MinecraftServer server;
	private final Path dir;
	private final ServerRules rules = new ServerRules();
	private final RoleBook roles = RoleBook.defaults();
	private final KitBook kits = new KitBook();
	private final List<String> trace = new ArrayList<>();
	private final ServerService s;
	private long time = 1800000000000L;
	private int passed;
	private int failed;
	private final Actor alice;
	private final Actor bob;
	private final Actor carol;
	private final Actor dave;
	private final Actor erin;
	private final Actor frank;
	private final Actor gina;
	private final Actor hank;

	private SelfTest(MinecraftServer server) {
		this.server = server;
		this.dir = server.getWorldPath(LevelResource.ROOT).resolve("slate").resolve("server").resolve("selftest-" + Long.toHexString(System.nanoTime()));
		this.rules.maxHomes = 2;
		this.rules.warmupSeconds = 0;
		this.rules.cooldownSeconds = 0;
		this.rules.requestSeconds = 30;
		this.rules.mailBoxSize = 3;
		this.rules.randomMinDistance = 64;
		this.rules.randomMaxDistance = 128;
		this.rules.afkSeconds = 1000000;
		this.role("vip", 20, r -> {
			r.inherits.add("default");
			r.numbers.put(Nodes.HOMES_MAX.id(), 4);
			r.allow.add(Nodes.NICK_COLOR.id());
			r.deny.add(Nodes.RTP_USE.id());
		});
		this.role("jailed", 30, r -> r.deny.add(Nodes.HOME_USE.id()));
		this.role("helper", 40, r -> {
			r.inherits.add("default");
			r.allow.add(Nodes.ADMIN_ROLES.id());
		});
		this.role("both", 5, r -> {
			r.allow.add(Nodes.TRASH_USE.id());
			r.deny.add(Nodes.TRASH_USE.id());
		});
		this.roles.sanitize();
		this.kit("starter", "Starter", k -> {
			k.items.add("{id:\"minecraft:bread\",count:4}");
			k.cooldownSeconds = 60;
		});
		this.kit("once", "Once", k -> {
			k.items.add("{id:\"minecraft:apple\",count:1}");
			k.oneTime = true;
		});
		this.kit("secret", "Secret", k -> {
			k.items.add("{id:\"minecraft:diamond\",count:1}");
			k.restricted = true;
		});
		this.kit("empty", "Empty", k -> {
		});
		this.s = ServerService.scratch(server, this.dir, this.rules, this.roles, this.kits);
		this.s.clock(() -> this.time);
		this.s.trace = this.trace;
		this.alice = this.enter("Alice", 0, 0);
		this.bob = this.enter("Bob", 0, 100);
		this.carol = this.enter("Carol", 0, 200);
		this.dave = this.enter("Dave", 0, 300);
		this.erin = this.enter("Erin", 4, 400);
		this.frank = this.enter("Frank", 2, 500);
		this.gina = this.enter("Gina", 0, 600);
		this.hank = this.enter("Hank", 0, 700);
	}

	private void role(String id, int priority, Consumer<Role> fill) {
		Role r = new Role();
		r.id = id;
		r.name = id;
		r.priority = priority;
		fill.accept(r);
		this.roles.roles.add(r);
	}

	private void kit(String id, String name, Consumer<Kit> fill) {
		Kit k = new Kit();
		k.id = id;
		k.name = name;
		fill.accept(k);
		this.kits.kits.add(k);
	}

	private Actor enter(String name, int level, int x) {
		UUID id = UUID.nameUUIDFromBytes(("cfhgui-server-selftest:" + name).getBytes(StandardCharsets.UTF_8));
		return this.s.enter(id, name, level, place((double)x, 0.0), null).actor();
	}

	public static SelfTest.Outcome run(MinecraftServer server, @Nullable ServerPlayer player) {
		SelfTest t = new SelfTest(server);

		try {
			t.words();
			t.permissions();
			t.homes();
			t.machine();
			t.places();
			t.requests();
			t.people();
			t.statistics();
			t.moderation();
			t.administration();
			t.door();
			t.persistence();
			if (player != null) {
				t.inTheWorld(player);
			} else {
				CFHGuiServer.LOGGER.info("[CFHGui Server] selftest: run by the console, so the teleport machine and the game's hooks were not tried on a player");
			}
		} catch (RuntimeException var7) {
			t.failed++;
			CFHGuiServer.LOGGER.error("[CFHGui Server] selftest FAILED: the script itself broke", var7);
		} finally {
			t.cleanUp();
		}

		CFHGuiServer.LOGGER.info("[CFHGui Server] selftest: {} passed, {} failed", t.passed, t.failed);
		return new SelfTest.Outcome(t.passed, t.failed);
	}

	private void check(String what, boolean ok) {
		if (ok) {
			this.passed++;
		} else {
			this.failed++;
			CFHGuiServer.LOGGER.error("[CFHGui Server] selftest FAILED: {}", what);
		}
	}

	private void expect(String what, OpResult r, String code) {
		if (r.code().equals(code)) {
			this.passed++;
		} else {
			this.failed++;
			CFHGuiServer.LOGGER.error("[CFHGui Server] selftest FAILED: {}: expected {}, got {} {}", new Object[]{what, code, r.code(), r.args()});
		}
	}

	private void expect(String what, OpResult r, String code, String firstArg) {
		if (r.code().equals(code) && r.arg(0).equals(firstArg)) {
			this.passed++;
		} else {
			this.failed++;
			CFHGuiServer.LOGGER.error("[CFHGui Server] selftest FAILED: {}: expected {} [{}], got {} {}", new Object[]{what, code, firstArg, r.code(), r.args()});
		}
	}

	private static Place place(double x, double z) {
		return new Place("minecraft:overworld", x, 64.0, z, 0.0F, 0.0F);
	}

	private ServerService.Target t(Actor a) {
		return this.s.target(a.id(), null);
	}

	private Session at(Actor a) {
		return this.s.session(a.id());
	}

	private void put(Actor a, Place p) {
		this.at(a).fakePlace = p.copy();
	}

	private Place where(Actor a) {
		return this.at(a).fakePlace;
	}

	private static boolean same(@Nullable Place a, @Nullable Place b) {
		return a != null && b != null && a.dimension.equals(b.dimension) && Math.abs(a.x - b.x) < 0.01 && Math.abs(a.y - b.y) < 0.01 && Math.abs(a.z - b.z) < 0.01;
	}

	private int told(String who, String what) {
		String prefix = who + " <- " + what;
		int n = 0;

		for (String line : this.trace) {
			if (line.startsWith(prefix)) {
				n++;
			}
		}

		return n;
	}

	private void second() {
		this.time += 1000L;

		for (int i = 0; i < 20; i++) {
			this.s.tick();
		}
	}

	private String lastAudit() {
		AdminData.AuditEntry e = this.s.audit.last();
		return e == null ? "" : e.action;
	}

	private OpResult grant(Actor who, String role, int seconds) {
		return this.s.admin.roleGrant(this.erin, this.t(who), who.name(), role, seconds);
	}

	private void words() {
		this.check("a time is read with its units", Text.seconds("1d12h") == 129600 && Text.seconds("30s") == 30 && Text.seconds("1w") == 604800);
		this.check("a number without a unit is no time", Text.seconds("10") == -1 && Text.seconds("5x") == -1 && Text.seconds("") == -1 && Text.seconds("h") == -1);
		this.check("a time is written in its two largest units", Text.span(7500L).equals("2h 5m") && Text.span(45L).equals("45s") && Text.span(86400L).equals("1d"));
		this.check(
			"a name has no spaces and no more than 24 characters",
			Text.name("a b") == null && Text.name("x".repeat(25)) == null && "base-2".equals(Text.name(" base-2 "))
		);
		this.check("an id is lower case", "builder".equals(Text.id("Builder")) && Text.id("bad id!") == null);
		this.check("a line loses its formatting codes and control characters", Text.line("a§cb\u0007c", 10).equals("abc") && Text.line("abcdef", 3).equals("abc"));
		this.check(
			"a nickname without the permission has no colours",
			Text.nick("&aAlly", false) == null && "&aAlly".equals(Text.nick("&aAlly", true)) && Text.nick("ab", true) == null
		);
		this.check("a nickname is compared without its colours", Text.plainNick("&a&lAlly").equals("Ally") && Text.plainNick("Tom&Jerry").equals("Tom&Jerry"));
		this.check("a colour is a name or six hex digits", Text.color("#ff0000") == 16711680 && Text.color("red") == 16733525 && Text.color("reddish") == -1);
		this.check(
			"an icon is a glyph's name or an item's id",
			Travel.icon("HOME").equals("HOME") && Travel.icon("minecraft:red_bed").equals("minecraft:red_bed") && Travel.icon("<b>x</b>").isEmpty()
		);
		this.check(
			"a pattern covers its branch and nothing beside it",
			Role.matches("cfhgui_server.mod.*", "cfhgui_server.mod.mute")
				&& !Role.matches("cfhgui_server.mod.*", "cfhgui_server.modx")
				&& Role.matches("*", "anything.at.all")
				&& !Role.matches("cfhgui_server.mod", "cfhgui_server.mod.mute")
		);
	}

	private void permissions() {
		this.check("everyone has what asks no rank", this.s.may(this.alice, Nodes.HOME_USE));
		this.check("a player has not what asks operator level 2", !this.s.may(this.alice, Nodes.WARP_SET));
		this.check("an operator of level 2 has it, and not what asks level 3", this.s.may(this.frank, Nodes.WARP_SET) && !this.s.may(this.frank, Nodes.SPAWN_SET));
		this.check("an operator of level 4 has everything", this.s.may(this.erin, Nodes.ADMIN_RULES) && this.s.may(this.erin, Nodes.ADMIN_SELFTEST));
		this.check("the console has everything", this.s.may(Actor.console("Server"), Nodes.ADMIN_RULES));
		this.expect("giving the moderator role", this.grant(this.carol, "moderator", 0), "ok");
		this.expect("giving the admin role", this.grant(this.dave, "admin", 0), "ok");
		this.check("a branch allows what is below it", this.s.may(this.carol, Nodes.MOD_MUTE) && this.s.may(this.carol, Nodes.MOD_FREEZE));
		this.check("within a role a denial beats an allowance", !this.s.may(this.carol, Nodes.MOD_BAN_PERMANENT));
		this.check("a role that says nothing leaves it to the operator level", !this.s.may(this.carol, Nodes.ADMIN_ROLES));
		this.check(
			"the higher role speaks first: the admin's * before the moderator's denial",
			this.s.may(this.dave, Nodes.MOD_BAN_PERMANENT) && this.s.may(this.dave, Nodes.ADMIN_RULES)
		);
		this.check(
			"a limit is the best role's",
			this.s.perms.number(this.carol, Nodes.HOMES_MAX, this.rules.maxHomes) == 10 && this.s.perms.number(this.dave, Nodes.HOMES_MAX, this.rules.maxHomes) == 50
		);
		this.check("a limit no role sets is the rule", this.s.perms.number(this.alice, Nodes.HOMES_MAX, this.rules.maxHomes) == 2);
		this.check("an inherited limit holds: the admin has the moderator's warm-up", this.s.perms.number(this.dave, Nodes.TP_WARMUP, 3) == 0);
		this.check(
			"whoever stands above every role waits for nothing and has room for a hundred homes",
			this.s.limit(this.erin, Nodes.TP_WARMUP, 3) == 0
				&& this.s.limit(this.erin, Nodes.RTP_COOLDOWN, 300) == 0
				&& this.s.limit(this.erin, Nodes.HOMES_MAX, 2) == 100
				&& this.s.limit(Actor.console("Server"), Nodes.TP_COOLDOWN, 5) == 0
		);
		this.check(
			"everyone else has the rule, or their role's number",
			this.s.limit(this.bob, Nodes.TP_WARMUP, 3) == 3 && this.s.limit(this.frank, Nodes.HOMES_MAX, 2) == 2 && this.s.limit(this.carol, Nodes.HOMES_MAX, 2) == 10
		);
		this.rules.operatorsHaveEverything = false;
		this.check("and without the rule so do operators", this.s.limit(this.erin, Nodes.TP_WARMUP, 3) == 3);
		this.rules.operatorsHaveEverything = true;
		this.expect("giving a role that both allows and denies", this.grant(this.bob, "both", 0), "ok");
		this.check("deny beats allow in the same role", !this.s.may(this.bob, Nodes.TRASH_USE));
		this.expect("taking it back", this.s.admin.roleRevoke(this.erin, this.t(this.bob), "Bob", "both"), "ok");
		this.check("without it the node is everyone's again", this.s.may(this.bob, Nodes.TRASH_USE));
		this.expect("giving vip", this.grant(this.alice, "vip", 0), "ok");
		this.check(
			"vip sets a limit, allows and denies",
			this.s.perms.number(this.alice, Nodes.HOMES_MAX, this.rules.maxHomes) == 4
				&& this.s.may(this.alice, Nodes.NICK_COLOR)
				&& !this.s.may(this.alice, Nodes.RTP_USE)
		);
		this.expect("giving a higher role that denies", this.grant(this.alice, "jailed", 0), "ok");
		this.check("the higher of two roles decides", !this.s.may(this.alice, Nodes.HOME_USE));
		this.expect("taking the higher role", this.s.admin.roleRevoke(this.erin, this.t(this.alice), "Alice", "jailed"), "ok");
		this.check("and the lower one is heard again", this.s.may(this.alice, Nodes.HOME_USE));
		this.roles.get("default").deny.add(Nodes.TRASH_USE.id());
		this.s.permissionsChanged(null);
		this.check("what a role expressly denies it denies an operator of level 4 too", !this.s.may(this.erin, Nodes.TRASH_USE));
		this.roles.get("default").deny.clear();
		this.s.permissionsChanged(null);
		this.check("and with the denial gone they have it back", this.s.may(this.erin, Nodes.TRASH_USE));
		this.check(
			"rank is the highest role's priority",
			this.s.perms.rank(this.carol) == 50 && this.s.perms.rank(this.dave) == 100 && this.s.perms.rank(this.alice) == 20 && this.s.perms.rank(this.bob) == 0
		);
		this.check(
			"an operator of level 4 and the console stand above every role",
			this.s.perms.rank(this.erin) == Integer.MAX_VALUE && this.s.perms.rank(Actor.console("Server")) == Integer.MAX_VALUE
		);
		this.check(
			"the higher rank outranks the lower",
			this.s.outranks(this.carol, this.t(this.alice))
				&& this.s.outranks(this.erin, this.t(this.dave))
				&& this.s.outranks(Actor.console("Server"), this.t(this.erin))
		);
		this.check(
			"an equal or lower rank does not",
			!this.s.outranks(this.carol, this.t(this.dave))
				&& !this.s.outranks(this.dave, this.t(this.dave))
				&& !this.s.outranks(this.frank, this.t(this.alice))
				&& !this.s.outranks(this.bob, this.t(this.gina))
		);
		this.expect("giving a role for a minute", this.grant(this.bob, "jailed", 60), "ok");
		this.check("a role given for a time holds", !this.s.may(this.bob, Nodes.HOME_USE));
		this.time += 61000L;
		this.check("and stops holding the moment its time is up", this.s.may(this.bob, Nodes.HOME_USE));
		int revoked = this.told("Bob", "role_revoked");
		this.second();
		this.check(
			"the grant that ran out is dropped and its player told", this.at(this.bob).record.roles.isEmpty() && this.told("Bob", "role_revoked") == revoked + 1
		);
		Hello hello = this.s.sync.hello(this.alice);
		this.check(
			"a client is told what its player may",
			hello.may(Nodes.HOME_USE.id()) && hello.may(Nodes.NICK_COLOR.id()) && !hello.may(Nodes.WARP_SET.id()) && !hello.may(Nodes.RTP_USE.id())
		);
		this.check(
			"and their limits, roles and what is switched on",
			hello.number(Nodes.HOMES_MAX.id(), 0) == 4 && hello.feature("homes") && !hello.roles.isEmpty() && hello.roles.get(0).id.equals("vip")
		);
		UUID stranger = UUID.randomUUID();
		this.check(
			"somebody who is away is answered from their roles and the node's level",
			Boolean.TRUE.equals(this.s.perms.hasOffline(stranger, Nodes.HOME_USE.id()))
				&& Boolean.FALSE.equals(this.s.perms.hasOffline(stranger, Nodes.WARP_SET.id()))
				&& this.s.perms.hasOffline(stranger, "nobody.declared.this") == null
		);
		this.s.leave(this.gina.id());
		this.check(
			"the roles of somebody who left are still theirs",
			Boolean.TRUE.equals(this.s.perms.hasOffline(this.carol.id(), Nodes.MOD_MUTE.id()))
				&& this.s.target(this.gina.id(), null) != null
				&& !this.s.target(this.gina.id(), null).online()
		);
	}

	private void homes() {
		this.expect("setting the first home", this.s.travel.homeSet(this.bob, null, null, null), "ok", "home");
		this.check("a change marks the player's own state to be sent", this.s.sync.marked(this.bob.id(), "self"));
		this.s.sync.flush();
		this.check("and sending clears the mark", !this.s.sync.marked(this.bob.id(), "self"));
		this.expect("setting a second home", this.s.travel.homeSet(this.bob, "base", "minecraft:red_bed", 1122867), "ok", "base");
		this.expect("a third is one too many", this.s.travel.homeSet(this.bob, "third", null, null), "home_limit", "2");
		this.put(this.bob, place(120.0, 0.0));
		this.expect("setting an existing home again moves it", this.s.travel.homeSet(this.bob, "BASE", null, null), "ok", "base");
		this.check(
			"it moved, and kept its icon and colour",
			same(this.at(this.bob).record.home("base").place, place(120.0, 0.0))
				&& this.at(this.bob).record.home("base").icon.equals("minecraft:red_bed")
				&& this.at(this.bob).record.home("base").color == 1122867
				&& this.at(this.bob).record.homes.size() == 2
		);
		this.expect("a name with a space is none", this.s.travel.homeSet(this.alice, "my home", null, null), "name_invalid");
		this.expect("a name that is too long is none", this.s.travel.homeSet(this.alice, "x".repeat(25), null, null), "name_invalid");
		this.expect("renaming onto another home", this.s.travel.homeEdit(this.bob, "base", "home", null, null), "name_taken");
		this.expect("renaming", this.s.travel.homeEdit(this.bob, "base", "camp", "<script>", null), "ok", "camp");
		this.check("an icon that is none is dropped", this.at(this.bob).record.home("camp") != null && this.at(this.bob).record.home("camp").icon.isEmpty());
		this.expect("editing a home that is not there", this.s.travel.homeEdit(this.bob, "nope", "x", null, null), "no_such_home", "nope");
		this.expect("deleting a home that is not there", this.s.travel.homeDelete(this.bob, "nope", null), "no_such_home", "nope");
		this.put(this.bob, place(150.0, 0.0));
		this.expect("going home without a name", this.s.travel.homeGo(this.bob, null, null), "ok", "home");
		this.check("the player stands at the home", same(this.where(this.bob), place(100.0, 0.0)));
		this.check("and where they stood is where /back leads", same(this.at(this.bob).record.back, place(150.0, 0.0)) && !this.at(this.bob).record.backIsDeath);
		this.expect("going back", this.s.travel.back(this.bob), "ok");
		this.check("back is where they came from", same(this.where(this.bob), place(150.0, 0.0)));
		this.expect("going back again", this.s.travel.back(this.bob), "ok");
		this.check("which is the home again", same(this.where(this.bob), place(100.0, 0.0)));
		this.expect("somebody who went nowhere has no way back", this.s.travel.back(this.hank), "no_back");
		this.expect("a first home under a name of its own", this.s.travel.homeSet(this.carol, "den", null, null), "ok", "den");
		this.put(this.carol, place(250.0, 0.0));
		this.expect("the only home is meant when none is named", this.s.travel.homeGo(this.carol, null, null), "ok", "den");
		this.expect("no home at all", this.s.travel.homeGo(this.dave, null, null), "no_such_home", "home");
		this.expect("another's home without the permission", this.s.travel.homeGo(this.alice, "home", this.t(this.bob)), "no_permission");
		int before = this.s.audit.size();
		this.expect("another's home as a moderator", this.s.travel.homeGo(this.carol, "home", this.t(this.bob)), "ok", "home");
		this.check(
			"the moderator stands there and the log says so",
			same(this.where(this.carol), place(100.0, 0.0)) && this.s.audit.size() == before + 1 && this.lastAudit().equals("home_go")
		);
		this.expect("listing another's homes without the permission", this.s.travel.homes(this.alice, this.t(this.bob)), "no_permission");
		this.check(
			"listing them as a moderator",
			this.s.travel.homes(this.carol, this.t(this.bob)).data(Home[].class) != null
				&& this.s.travel.homes(this.carol, this.t(this.bob)).data(Home[].class).length == 2
		);
		this.expect("deleting another's home as a moderator", this.s.travel.homeDelete(this.carol, "camp", this.t(this.bob)), "ok", "camp");
		this.check("it is gone and logged", this.at(this.bob).record.homes.size() == 1 && this.lastAudit().equals("home_delete"));
		this.expect("a home of somebody higher", this.s.travel.homeSet(this.dave, "keep", null, null), "ok", "keep");
		this.expect("is theirs to go to, which does nothing to its owner", this.s.travel.homeGo(this.carol, "keep", this.t(this.dave)), "ok", "keep");
		this.expect("and not theirs to delete", this.s.travel.homeDelete(this.carol, "keep", this.t(this.dave)), "outranked", "Dave");
		this.expect("its owner deletes it", this.s.travel.homeDelete(this.dave, "keep", null), "ok", "keep");
		this.rules.noHomeDimensions.add("minecraft:overworld");
		this.expect("no home where the rules allow none", this.s.travel.homeSet(this.bob, "x", null, null), "dimension_closed");
		this.expect("unless one may teleport anywhere", this.s.travel.homeSet(this.frank, "x", null, null), "ok", "x");
		this.rules.noHomeDimensions.clear();
		this.rules.homes = false;
		this.expect("homes switched off", this.s.travel.homeGo(this.bob, null, null), "disabled");
		this.check("and a client is told so", !this.s.sync.hello(this.bob).feature("homes"));
		this.rules.homes = true;
		Session session = this.at(this.hank);
		this.s.travel.died(session);
		this.check(
			"dying keeps the place, tells the player and makes it the way back",
			same(session.record.death, place(700.0, 0.0)) && session.record.backIsDeath && this.told("Hank", "death_place") == 1
		);
		this.put(this.hank, place(710.0, 0.0));
		this.expect("going back to where one died", this.s.travel.back(this.hank), "ok");
		this.check("having been back, the place of death is no longer kept", same(this.where(this.hank), place(700.0, 0.0)) && session.record.death == null);
		this.s.travel.died(session);
		this.rules.backToDeath = false;
		this.s.travel.died(session);
		this.check("without the rule a death is not a way back", !session.record.backIsDeath || this.s.travel.back(this.hank).code().equals("no_back"));
		this.rules.backToDeath = true;
	}

	private void machine() {
		Session b = this.at(this.bob);
		this.put(this.bob, place(160.0, 0.0));
		this.rules.warmupSeconds = 3;
		this.expect("a teleport with a warm-up is taken, not done", this.s.travel.homeGo(this.bob, null, null), "warmup", "3");
		SelfState pending = this.s.sync.self(b);
		this.check(
			"the player's client is told what is pending and when",
			"home:home".equals(pending.pending) && pending.pendingAt == this.time + 3000L && same(this.where(this.bob), place(160.0, 0.0))
		);
		this.expect("a second teleport meanwhile", this.s.travel.spawnGo(this.bob), "already_pending");
		this.s.teleports.tick();
		this.check("standing still, it waits", b.pending != null);
		this.put(this.bob, place(162.0, 0.0));
		this.s.teleports.tick();
		this.check(
			"moving gives it up, and the player is told", b.pending == null && this.told("Bob", "cancelled_moved") == 1 && same(this.where(this.bob), place(162.0, 0.0))
		);
		this.rules.cooldownSeconds = 10;
		this.expect("taking it again", this.s.travel.homeGo(this.bob, null, null), "warmup", "3");
		this.time += 2999L;
		this.s.teleports.tick();
		this.check("a moment before its time it still waits", b.pending != null);
		this.time++;
		this.s.teleports.tick();
		this.check("when its time has come it happens", b.pending == null && same(this.where(this.bob), place(100.0, 0.0)) && same(b.record.back, place(162.0, 0.0)));
		this.check("and the client is told the cool-down", this.s.sync.self(b).cooldown("tp") == this.time + 10000L && this.s.sync.self(b).pending == null);
		this.expect("the next teleport has to wait", this.s.travel.back(this.bob), "cooldown", "10");
		this.time += 10000L;
		this.expect("after the cool-down it is taken", this.s.travel.back(this.bob), "warmup", "3");
		this.s.teleports.hurt(b);
		this.check("being hurt gives it up, and the player is told", b.pending == null && this.told("Bob", "cancelled_hurt") == 1);
		this.expect("a warm-up given up starts no cool-down", this.s.travel.back(this.bob), "warmup", "3");
		this.expect("giving it up oneself", this.s.travel.cancel(this.bob), "ok");
		this.expect("with nothing to give up", this.s.travel.cancel(this.bob), "nothing_pending");
		this.put(this.carol, place(260.0, 0.0));
		this.expect("a role's warm-up of nothing: at once, whatever the rules say", this.s.travel.homeGo(this.carol, "den", null), "ok", "den");
		this.check("the moderator is there", same(this.where(this.carol), place(200.0, 0.0)));
		this.rules.warmupSeconds = 0;
		this.rules.cooldownSeconds = 0;
		this.put(this.bob, new Place("minecraft:the_nether", 5.0, 70.0, 5.0, 0.0F, 0.0F));
		this.expect("a home in another dimension", this.s.travel.homeSet(this.bob, "hell", null, null), "ok", "hell");
		this.put(this.bob, place(160.0, 0.0));
		this.rules.noTeleportDimensions.add("minecraft:the_nether");
		this.expect("no teleport into a closed dimension", this.s.travel.homeGo(this.bob, "hell", null), "dimension_closed");
		this.rules.noTeleportDimensions.clear();
		this.rules.noTeleportDimensions.add("minecraft:overworld");
		this.expect("nor out of one", this.s.travel.homeGo(this.bob, "hell", null), "dimension_closed");
		this.rules.noTeleportDimensions.clear();
		this.rules.crossDimension = false;
		this.expect("no teleport across dimensions where the rules forbid it", this.s.travel.homeGo(this.bob, "hell", null), "no_cross_dimension");
		this.put(this.frank, new Place("minecraft:the_nether", 9.0, 70.0, 9.0, 0.0F, 0.0F));
		this.expect("a home for somebody who may teleport anywhere", this.s.travel.homeSet(this.frank, "far", null, null), "ok", "far");
		this.put(this.frank, place(500.0, 0.0));
		this.expect("whom the dimensions' rules do not bind", this.s.travel.homeGo(this.frank, "far", null), "ok", "far");
		this.check("they crossed", "minecraft:the_nether".equals(this.where(this.frank).dimension));
		this.put(this.frank, place(500.0, 0.0));
		this.rules.crossDimension = true;
		this.expect("and with the rule back everyone crosses", this.s.travel.homeGo(this.bob, "hell", null), "ok", "hell");
		this.put(this.bob, place(160.0, 0.0));
		this.rules.randomDimensions.clear();
		this.rules.randomDimensions.add("minecraft:overworld");
		this.expect("a random teleport", this.s.travel.random(this.bob, null), "ok");
		this.check(
			"lands on the ring and starts its own cool-down",
			Math.abs(this.where(this.bob).x - (double)(160 + this.rules.randomMinDistance)) < 0.01 && b.record.cooldowns.containsKey("rtp")
		);
		this.expect("the next has to wait", this.s.travel.random(this.bob, null), "cooldown", "300");
		this.expect("a home is not held up by the random teleport's cool-down", this.s.travel.homeGo(this.bob, null, null), "ok", "home");
		this.expect("a dimension the rules do not name", this.s.travel.random(this.hank, "minecraft:the_end"), "dimension_closed");
		this.expect("a role that denies it", this.s.travel.random(this.alice, null), "no_permission");
		this.expect("a role without a cool-down", this.s.travel.random(this.dave, null), "ok");
		this.expect("teleports again at once", this.s.travel.random(this.dave, null), "ok");
		this.rules.randomTeleport = false;
		this.expect("random teleports switched off", this.s.travel.random(this.hank, null), "disabled");
		this.rules.randomTeleport = true;
		this.expect("a place on a map without the permission", this.s.travel.position(this.alice, 10.0, null, 10.0, null), "no_permission");
		this.expect("a place on a map", this.s.travel.position(this.frank, 10.0, null, 10.0, null), "ok");
		this.s.teleports.tick();
		this.check(
			"is gone to on the next tick (its height has to be looked up)",
			Math.abs(this.where(this.frank).x - 10.0) < 0.01 && Math.abs(this.where(this.frank).z - 10.0) < 0.01
		);
		this.expect("a place that is no place", this.s.travel.position(this.frank, Double.NaN, null, 0.0, null), "invalid");
		this.expect("a place beyond the world", this.s.travel.position(this.frank, 4.0E7, 64.0, 0.0, null), "invalid");
		this.put(this.frank, place(500.0, 0.0));
	}

	private void places() {
		this.put(this.dave, place(300.0, 0.0));
		this.expect("a warp needs the permission", this.s.travel.warpSet(this.alice, "market", null, null, null, null), "no_permission");
		this.expect("creating a warp", this.s.travel.warpSet(this.dave, "market", "COMPASS", 65280, "Trade§c here", false), "ok", "market");
		this.check("its description is tidied and it is logged", this.s.store.warp("market").description.equals("Trade here") && this.lastAudit().equals("warp_set"));
		this.expect("a warp's name is checked like a home's", this.s.travel.warpSet(this.dave, "the market", null, null, null, null), "name_invalid");
		this.put(this.dave, place(310.0, 0.0));
		boolean declared = Perms.node("cfhgui_server.warp.to.vault") != null;
		this.expect("creating a restricted warp", this.s.travel.warpSet(this.dave, "vault", null, null, null, true), "ok", "vault");
		this.check(
			"a restricted warp has a node of its own, and a test's warp does not put it into the game's list of permissions",
			this.s.store.warp("vault").node().equals("cfhgui_server.warp.to.vault") && Perms.node("cfhgui_server.warp.to.vault") != null == declared
		);
		this.check(
			"a player is told only the warps they may use",
			this.s.sync.warps(this.alice).warps.size() == 1 && this.s.sync.warps(this.dave).warps.size() == 2 && this.s.sync.warps(this.alice).spawn != null
		);
		this.expect("going to a warp", this.s.travel.warpGo(this.alice, "MARKET"), "ok", "market");
		this.check("the player stands there and the visit is counted", same(this.where(this.alice), place(300.0, 0.0)) && this.s.store.warp("market").visits == 1L);
		this.expect("a restricted warp is not there for those who may not use it", this.s.travel.warpGo(this.alice, "vault"), "no_such_warp", "vault");
		this.expect("a warp that is not there", this.s.travel.warpGo(this.alice, "nowhere"), "no_such_warp", "nowhere");
		Role vip = (Role)JsonWire.GSON.fromJson(JsonWire.GSON.toJson(this.roles.get("vip")), Role.class);
		vip.allow.add(this.s.store.warp("vault").node());
		this.expect("a role is given the warp's own node", this.s.admin.roleSave(this.erin, vip), "ok", "vip");
		this.expect("and its holders may use it", this.s.travel.warpGo(this.alice, "vault"), "ok", "vault");
		this.expect("renaming a warp onto another", this.s.travel.warpEdit(this.dave, "market", "vault", null, null, null, null), "name_taken");
		this.expect("renaming a warp", this.s.travel.warpEdit(this.dave, "market", "bazaar", null, null, null, null), "ok", "bazaar");
		this.check("it is found under its new name only", this.s.store.warp("bazaar") != null && this.s.store.warp("market") == null);
		this.expect("deleting a warp", this.s.travel.warpDelete(this.dave, "bazaar"), "ok", "bazaar");
		this.expect("deleting it again", this.s.travel.warpDelete(this.dave, "bazaar"), "no_such_warp", "bazaar");
		this.rules.warps = false;
		this.expect("warps switched off", this.s.travel.warpGo(this.alice, "vault"), "disabled");
		this.check("and nobody is told any", this.s.sync.warps(this.dave).warps.isEmpty());
		this.rules.warps = true;
		this.expect("moving the spawn needs the permission", this.s.travel.spawnSet(this.frank), "no_permission");
		this.put(this.dave, place(333.0, 3.0));
		this.expect("moving the spawn", this.s.travel.spawnSet(this.dave), "ok");
		this.check(
			"it is kept, logged, and what /spawn leads to",
			same(this.s.store.spawn(), place(333.0, 3.0)) && this.lastAudit().equals("spawn_set") && same(this.s.travel.spawn(), place(333.0, 3.0))
		);
		this.expect("going to the spawn", this.s.travel.spawnGo(this.alice), "ok");
		this.check("the player stands there", same(this.where(this.alice), place(333.0, 3.0)));
		this.rules.spawn = false;
		this.expect("the spawn switched off", this.s.travel.spawnGo(this.alice), "disabled");
		this.check("and no client is told where it is", this.s.sync.warps(this.alice).spawn == null);
		this.rules.spawn = true;
	}

	private void requests() {
		this.put(this.alice, place(0.0, 0.0));
		this.put(this.bob, place(100.0, 0.0));
		this.expect("asking to come", this.s.requests.ask(this.alice, this.t(this.bob), "Bob", false), "ok", "Bob");
		TeleportRequest[] bobs = this.s.requests.view(this.bob.id());
		TeleportRequest[] alices = this.s.requests.view(this.alice.id());
		this.check(
			"both are told of the request, each from their side",
			bobs.length == 1
				&& bobs[0].incoming
				&& !bobs[0].here
				&& alices.length == 1
				&& !alices[0].incoming
				&& this.told("Bob", "tpa_received") == 1
				&& bobs[0].expiresAt == this.time + 30000L
		);
		this.expect("asking the same player again", this.s.requests.ask(this.alice, this.t(this.bob), "Bob", false), "already_requested", "Bob");
		this.expect("asking oneself", this.s.requests.ask(this.alice, this.t(this.alice), "Alice", false), "self");
		this.expect("asking nobody", this.s.requests.ask(this.alice, null, "Zed", false), "no_such_player", "Zed");
		this.expect("asking somebody who is away", this.s.requests.ask(this.alice, this.t(this.gina), "Gina", false), "player_offline", "Gina");
		this.expect("saying yes", this.s.requests.accept(this.bob, null), "ok", "Alice");
		this.check(
			"whoever asked travels and is told",
			same(this.where(this.alice), place(100.0, 0.0)) && this.told("Alice", "tpa_accepted") == 1 && this.s.requests.size() == 0
		);
		this.expect("with nothing to answer", this.s.requests.accept(this.bob, null), "no_request");
		this.put(this.alice, place(0.0, 0.0));
		this.expect("asking too fast", this.s.requests.ask(this.alice, this.t(this.carol), "Carol", false), "too_fast");
		this.time += 2000L;
		this.expect("asking somebody to come", this.s.requests.ask(this.alice, this.t(this.bob), "Bob", true), "ok", "Bob");
		this.check("they are told it is them who would travel", this.told("Bob", "tpahere_received") == 1 && this.s.requests.view(this.bob.id())[0].here);
		this.expect("saying yes to that", this.s.requests.accept(this.bob, this.alice.id()), "ok", "Alice");
		this.check("whoever said yes travels", same(this.where(this.bob), place(0.0, 0.0)));
		this.put(this.bob, place(100.0, 0.0));
		this.time += 2000L;
		this.expect("asking once more", this.s.requests.ask(this.alice, this.t(this.bob), "Bob", false), "ok", "Bob");
		this.expect("saying no", this.s.requests.deny(this.bob, null), "ok", "Alice");
		this.check("whoever asked is told, and stays", this.told("Alice", "tpa_denied") == 1 && same(this.where(this.alice), place(0.0, 0.0)));
		this.time += 2000L;
		this.expect("asking and", this.s.requests.ask(this.alice, this.t(this.bob), "Bob", false), "ok", "Bob");
		this.expect("taking it back", this.s.requests.cancel(this.alice, null), "ok", "1");
		this.expect("with nothing to take back", this.s.requests.cancel(this.alice, null), "no_request");
		this.expect("switching requests off for oneself", this.s.requests.toggle(this.bob, false), "ok", "false");
		this.time += 2000L;
		this.expect("asking somebody who takes none", this.s.requests.ask(this.alice, this.t(this.bob), "Bob", false), "requests_off", "Bob");
		this.expect("switching them back the other way round", this.s.requests.toggle(this.bob, null), "ok", "true");
		this.expect("and not off again at once: every change is told to every client", this.s.requests.toggle(this.bob, false), "too_fast");
		this.expect("asking for what is so already is no change", this.s.requests.toggle(this.bob, true), "ok", "true");
		this.time += 2000L;
		this.expect("a request that nobody answers", this.s.requests.ask(this.alice, this.t(this.bob), "Bob", false), "ok", "Bob");
		this.time += 31000L;
		this.s.requests.tick();
		this.check("runs out, and whoever asked is told", this.s.requests.size() == 0 && this.told("Alice", "tpa_expired") == 1);
		this.time += 2000L;
		this.expect("a request to somebody who then leaves", this.s.requests.ask(this.alice, this.t(this.hank), "Hank", false), "ok", "Hank");
		this.s.leave(this.hank.id());
		this.check("is gone with them", this.s.requests.size() == 0);
		this.s.enter(this.hank.id(), "Hank", 0, place(700.0, 0.0), null);
		this.rules.requests = false;
		this.expect("requests switched off", this.s.requests.ask(this.alice, this.t(this.bob), "Bob", false), "disabled");
		this.rules.requests = true;
	}

	private void people() {
		this.expect("going idle by hand", this.s.people.afk(this.alice, true), "ok", "true");
		boolean shownIdle = false;

		for (OnlinePlayer o : this.s.sync.online(this.bob)) {
			if (o.id.equals(this.alice.id())) {
				shownIdle = o.afk;
			}
		}

		this.check(
			"everyone is told, in chat and in the list of who is on",
			this.at(this.alice).afk && this.told("*", "afk_on") == 1 && shownIdle && this.s.sync.self(this.at(this.alice)).afk
		);
		this.expect("coming back at once is too fast: each change is a line in everybody's chat", this.s.people.afk(this.alice, null), "too_fast");
		this.check("and nothing was told", this.at(this.alice).afk && this.told("*", "afk_off") == 0);
		this.time += 3000L;
		this.expect("and coming back the other way round", this.s.people.afk(this.alice, null), "ok", "false");
		this.check("is told as well", !this.at(this.alice).afk && this.told("*", "afk_off") == 1);
		this.rules.afk = false;
		this.expect("idle switched off", this.s.people.afk(this.alice, true), "disabled");
		this.rules.afk = true;
		this.expect("a nickname that is too short", this.s.people.nick(this.alice, "ab"), "nick_invalid");
		this.expect("a nickname that is somebody's name", this.s.people.nick(this.alice, "bob"), "nick_taken");
		this.expect("a nickname", this.s.people.nick(this.alice, "Ally"), "ok", "Ally");
		this.check(
			"the player is found by it",
			this.alice.id().equals(this.s.store.idOf("ally")) && this.s.target(null, "ALLY") != null && this.s.sync.self(this.at(this.alice)).nick.equals("Ally")
		);
		this.check(
			"and whoever finds them so knows it was by a nickname",
			this.s.target(null, "ally").byNickname() && !this.s.target(null, "alice").byNickname() && !this.t(this.alice).byNickname()
		);
		this.expect(
			"which is not enough to do something to them",
			this.s.moderation.punish(this.carol, this.s.target(null, "Ally"), "Ally", Punishment.Kind.WARN, "", 0),
			"nickname_given",
			"Ally"
		);
		this.expect("or to give them a role", this.s.admin.roleGrant(this.erin, this.s.target(null, "Ally"), "Ally", "helper", 0), "nickname_given", "Ally");
		this.check("nothing came of either", this.at(this.alice).record.punishments.isEmpty() && this.at(this.alice).record.roles.size() == 1);
		this.expect("a nickname that is somebody's nickname", this.s.people.nick(this.bob, "Ally"), "nick_taken");
		this.expect("a nickname that is the console's name", this.s.people.nick(this.bob, "Server"), "nick_taken");
		this.expect("colours without the permission", this.s.people.nick(this.bob, "&aBobby"), "nick_invalid");
		this.expect("changing it again at once", this.s.people.nick(this.alice, "&aAlly"), "too_fast");
		this.time += 3000L;
		this.expect("colours with the permission", this.s.people.nick(this.alice, "&aAlly"), "ok", "Ally");
		this.check("the colours do not change what the player is found by", this.alice.id().equals(this.s.store.idOf("Ally")));
		this.time += 3000L;
		this.expect("giving the nickname up", this.s.people.nick(this.alice, ""), "ok", "Alice");
		this.check("it is nobody's any more", this.s.store.idOf("Ally") == null && this.at(this.alice).record.nick.isEmpty());
		this.rules.nicknames = false;
		this.expect("nicknames switched off", this.s.people.nick(this.alice, "Ally"), "disabled");
		this.rules.nicknames = true;
		this.expect("a letter", this.s.people.mailSend(this.alice, this.t(this.bob), "Bob", "hello §cthere"), "ok", "Bob");
		this.check(
			"arrives tidied, unread, and its receiver is told",
			this.at(this.bob).record.mail.size() == 1
				&& this.at(this.bob).record.mail.get(0).text.equals("hello there")
				&& this.at(this.bob).record.unreadMail() == 1
				&& this.told("Bob", "mail_received") == 1
				&& this.s.sync.self(this.at(this.bob)).unreadMail == 1
		);
		this.expect("a second letter at once", this.s.people.mailSend(this.alice, this.t(this.bob), "Bob", "again"), "too_fast");
		this.time += 3000L;
		this.expect("a letter to nobody", this.s.people.mailSend(this.alice, null, "Zed", "hello"), "no_such_player", "Zed");
		this.expect("a letter to oneself", this.s.people.mailSend(this.alice, this.t(this.alice), "Alice", "hello"), "self");
		this.expect("a letter that is too long", this.s.people.mailSend(this.alice, this.t(this.bob), "Bob", "x".repeat(257)), "text_invalid");
		this.expect("a letter that says nothing", this.s.people.mailSend(this.alice, this.t(this.bob), "Bob", "   "), "text_invalid");
		this.expect("a second letter", this.s.people.mailSend(this.alice, this.t(this.bob), "Bob", "two"), "ok", "Bob");
		this.time += 3000L;
		this.expect("a third letter", this.s.people.mailSend(this.alice, this.t(this.bob), "Bob", "three"), "ok", "Bob");
		this.time += 3000L;
		this.expect("a box full of unread letters takes no more", this.s.people.mailSend(this.alice, this.t(this.bob), "Bob", "four"), "mail_full", "Bob");
		String first = this.at(this.bob).record.mail.get(0).id;
		this.expect("reading one letter", this.s.people.mailRead(this.bob, first), "ok", "1");
		this.expect("reading a letter that is not there", this.s.people.mailRead(this.bob, "nope"), "no_such_mail");
		this.time += 3000L;
		this.expect("a full box gives way from its oldest read letter", this.s.people.mailSend(this.alice, this.t(this.bob), "Bob", "four"), "ok", "Bob");
		this.check(
			"the read one went, the box is as full as before",
			this.at(this.bob).record.mail.size() == 3 && this.at(this.bob).record.mail.stream().noneMatch(m -> m.id.equals(first))
		);
		this.expect("reading them all", this.s.people.mailRead(this.bob, null), "ok", "3");
		this.check("the letters are sent newest first", Sync.mail(this.at(this.bob).record)[0].text.equals("four") && this.at(this.bob).record.unreadMail() == 0);
		this.expect("deleting one letter", this.s.people.mailDelete(this.bob, this.at(this.bob).record.mail.get(0).id), "ok", "1");
		this.expect("deleting every read one", this.s.people.mailDelete(this.bob, null), "ok", "2");
		this.expect("deleting a letter that is not there", this.s.people.mailDelete(this.bob, "nope"), "no_such_mail");
		this.time += 3000L;
		this.expect("a letter to somebody who is away", this.s.people.mailSend(this.alice, this.t(this.gina), "Gina", "see you"), "ok", "Gina");
		this.check("waits in their record", this.s.target(this.gina.id(), null).record().mail.size() == 1);
		this.expect("the console writes too", this.s.people.mailSend(Actor.console("Server"), this.t(this.bob), "Bob", "from above"), "ok", "Bob");
		this.rules.mail = false;
		this.expect("mail switched off", this.s.people.mailSend(this.alice, this.t(this.bob), "Bob", "hello"), "disabled");
		this.rules.mail = true;
		this.expect("taking a kit", this.s.people.kitClaim(this.alice, "STARTER"), "ok", "Starter");
		this.expect("taking it again before its time", this.s.people.kitClaim(this.alice, "starter"), "kit_cooldown", "1m");
		List<Kit.View> views = this.s.people.kitViews(this.at(this.alice));
		this.check(
			"a player is told the kits they may take, what is in them and when",
			views.size() == 3
				&& views.get(0).id.equals("starter")
				&& views.get(0).readyAt == this.time + 60000L
				&& views.get(0).contents.size() == 1
				&& views.get(0).contents.get(0).item.equals("minecraft:bread")
				&& views.get(0).contents.get(0).count == 4
		);
		this.time += 60000L;
		this.expect("taking it when its time has come", this.s.people.kitClaim(this.alice, "starter"), "ok", "Starter");
		this.expect("a kit for once", this.s.people.kitClaim(this.alice, "once"), "ok", "Once");
		this.expect("is for once", this.s.people.kitClaim(this.alice, "once"), "kit_taken");
		this.check("and its player is told so", this.s.people.kitViews(this.at(this.alice)).get(1).readyAt == -1L);
		this.expect("a restricted kit is not there for those who may not take it", this.s.people.kitClaim(this.alice, "secret"), "no_such_kit", "secret");
		this.expect("and is for those who may", this.s.people.kitClaim(this.dave, "secret"), "ok", "Secret");
		this.expect("a kit with nothing in it", this.s.people.kitClaim(this.alice, "empty"), "kit_empty");
		this.expect("a kit that is not there", this.s.people.kitClaim(this.alice, "nope"), "no_such_kit", "nope");
		this.rules.kits = false;
		this.expect("kits switched off", this.s.people.kitClaim(this.alice, "starter"), "disabled");
		this.check("and no client is told any", this.s.people.kitViews(this.at(this.alice)).isEmpty());
		this.rules.kits = true;
		this.expect("the trash needs somebody in the world", this.s.people.open(this.alice, "trash"), "player_only");
		this.expect("the ender chest needs its permission", this.s.people.open(this.alice, "enderchest"), "no_permission");
		this.expect("something that cannot be opened", this.s.people.open(this.alice, "bank"), "invalid");
		PlayerCard card = this.s.people.card(this.alice, this.t(this.bob), "Bob").data(PlayerCard.class);
		this.check("anybody may read anybody's card", card != null && card.online && card.name.equals("Bob") && card.sessions >= 1L && card.firstSeen > 0L);
		this.expect("a card of nobody", this.s.people.card(this.alice, null, "Zed"), "no_such_player", "Zed");
		PlayerCard away = this.s.people.card(this.alice, this.t(this.gina), "Gina").data(PlayerCard.class);
		this.check("the card of somebody who is away says so", away != null && !away.online && away.lastSeen > 0L);
		this.expect("hiding one's statistics", this.s.people.statsHide(this.carol, true), "ok", "true");
		this.check("is kept, and told to their client", this.at(this.carol).record.statsHidden && this.s.sync.self(this.at(this.carol)).statsHidden);
		this.expect("and not shown again at once: each change throws the kept rankings away", this.s.people.statsHide(this.carol, false), "too_fast");
	}

	private void statistics() {
		this.count(this.alice, 100L);
		this.count(this.bob, 300L);
		this.count(this.carol, 500L);
		Board board = this.s.statistics.board(this.alice, PLAY, 0, 10).data(Board.class);
		this.check(
			"a ranking leaves out who hides their statistics",
			board != null
				&& board.total() == 2
				&& ((Row)board.rows().get(0)).name().equals("Bob")
				&& ((Row)board.rows().get(0)).value() == 300L
				&& ((Row)board.rows().get(0)).rank() == 1
		);
		this.check("and always has the asker's own line", board != null && board.self() != null && board.self().rank() == 2 && board.self().value() == 100L);
		Board all = this.s.statistics.board(this.dave, PLAY, 0, 10).data(Board.class);
		this.check("whoever may see hidden statistics is shown everyone", all != null && all.total() == 3 && ((Row)all.rows().get(0)).name().equals("Carol"));
		Board own = this.s.statistics.board(this.carol, PLAY, 0, 10).data(Board.class);
		this.check(
			"whoever is left out still has their own line, where it would stand",
			own != null && own.total() == 2 && own.self() != null && own.self().rank() == 1 && own.self().value() == 500L
		);
		Board page = this.s.statistics.board(this.dave, PLAY, 1, 1).data(Board.class);
		this.check("a ranking is paged", page != null && page.rows().size() == 1 && ((Row)page.rows().get(0)).rank() == 2 && page.total() == 3);
		this.expect("a key that is none", this.s.statistics.board(this.alice, "playtime", 0, 10), "invalid");
		Board none = this.s.statistics.board(this.alice, "nomod:custom|nomod:thing", 0, 10).data(Board.class);
		this.check("a statistic nobody has is an empty ranking", none != null && none.total() == 0 && none.self() == null);
		Board visits = this.s.statistics.board(this.alice, "sessions", 0, 100).data(Board.class);
		this.check("what only this module counts is ranked too", visits != null && visits.total() >= 6 && visits.self() != null);
		this.rules.rankings = false;
		this.expect("rankings switched off", this.s.statistics.board(this.alice, PLAY, 0, 10), "disabled");
		this.rules.rankings = true;
		Snapshot snap = this.s.statistics.stats(this.alice, this.t(this.bob), "Bob").data(Snapshot.class);
		this.check("another player's statistics", snap != null && snap.stat(PLAY) == 300L && snap.extra("sessions") >= 1L && snap.name().equals("Bob"));
		this.expect("hidden statistics", this.s.statistics.stats(this.alice, this.t(this.carol), "Carol"), "stats_hidden", "Carol");
		this.check(
			"are shown to who may see them, and to their owner",
			this.s.statistics.stats(this.dave, this.t(this.carol), "Carol").ok() && this.s.statistics.stats(this.carol, this.t(this.carol), "Carol").ok()
		);
		this.expect("statistics of nobody", this.s.statistics.stats(this.alice, null, "Zed"), "no_such_player", "Zed");
		this.rules.stats = false;
		this.expect("statistics switched off", this.s.statistics.stats(this.alice, this.t(this.bob), "Bob"), "disabled");
		this.check("one's own are always there", this.s.statistics.stats(this.alice, this.t(this.alice), "Alice").ok());
		this.expect("and so is the list of players", this.s.statistics.players(this.alice), "disabled");
		this.rules.stats = true;
		PlayerRef[] refs = this.s.statistics.players(this.alice).data(PlayerRef[].class);
		this.check(
			"everyone the server knows is listed, those who are on first", refs != null && refs.length == 8 && refs[0].online() && !refs[refs.length - 1].online()
		);
	}

	private void count(Actor a, long ticks) {
		Object2LongOpenHashMap<String> map = new Object2LongOpenHashMap();
		map.put(PLAY, ticks);
		this.s.statsFiles.put(a.id(), map);
	}

	private void moderation() {
		Session a = this.at(this.alice);
		this.expect(
			"punishing needs the permission", this.s.moderation.punish(this.alice, this.t(this.carol), "Carol", Punishment.Kind.MUTE, "", 60), "no_permission"
		);
		this.expect(
			"nothing can be used on a higher role", this.s.moderation.punish(this.carol, this.t(this.dave), "Dave", Punishment.Kind.MUTE, "", 60), "outranked", "Dave"
		);
		this.expect("nor on oneself", this.s.moderation.punish(this.carol, this.t(this.carol), "Carol", Punishment.Kind.MUTE, "", 60), "self");
		this.expect("nor on nobody", this.s.moderation.punish(this.carol, null, "Zed", Punishment.Kind.MUTE, "", 60), "no_such_player", "Zed");
		this.expect("a punishment of no kind", this.s.moderation.punish(this.carol, this.t(this.alice), "Alice", null, "", 60), "invalid");
		this.expect("muting", this.s.moderation.punish(this.carol, this.t(this.alice), "Alice", Punishment.Kind.MUTE, "spam §cplease", 60), "ok", "Alice");
		AdminData.AuditEntry muted = this.s.audit.last();
		this.check(
			"the log says who, whom, for how long and why",
			muted != null
				&& muted.action.equals("punish_mute")
				&& muted.actorName.equals("Carol")
				&& muted.targetName.equals("Alice")
				&& muted.detail.equals("1m, spam please")
				&& this.alice.id().equals(muted.target)
		);
		OpResult why = this.s.moderation.mutedResult(a);
		this.check(
			"the muted player is told why and until when",
			this.told("Alice", "muted") == 1
				&& why != null
				&& why.code().equals("muted")
				&& why.arg(0).equals("spam please, 1m")
				&& this.s.sync.self(a).mutedUntil == this.time + 60000L
		);
		int saidMuted = this.told("Alice", "muted");
		this.check("a mute stops chat, and says so", !this.s.moderation.chatAllowed(a) && this.told("Alice", "muted") == saidMuted + 1);
		this.expect("and mail", this.s.people.mailSend(this.alice, this.t(this.bob), "Bob", "psst"), "muted", "spam please, 1m");
		this.expect("and nicknames", this.s.people.nick(this.alice, "Quiet"), "muted");
		int saidAway = this.told("*", "afk_on");
		this.expect("going idle is still theirs", this.s.people.afk(this.alice, true), "ok", "true");
		this.check("but is not told to everybody: a mute takes their name out of the others' chat", a.afk && this.told("*", "afk_on") == saidAway);
		this.s.people.setAfk(a, false, false);
		this.check(
			"and the commands that speak, whatever they are prefixed with",
			this.s.moderation.commandRefused(a, "msg Bob hi")
				&& this.s.moderation.commandRefused(a, "/minecraft:me waves")
				&& this.s.moderation.commandRefused(a, "TELL Bob hi")
		);
		this.check("and no other command", !this.s.moderation.commandRefused(a, "home") && !this.s.moderation.commandRefused(a, "message"));
		this.check(
			"the index knows who is muted without opening their file",
			this.s.store.known(this.alice.id()).muted(this.time) && !this.s.store.known(this.bob.id()).muted(this.time)
		);
		this.expect("muting twice", this.s.moderation.punish(this.carol, this.t(this.alice), "Alice", Punishment.Kind.MUTE, "", 60), "already_punished", "Alice");
		this.expect("lifting needs the higher rank too", this.s.moderation.pardon(this.bob, this.t(this.alice), "Alice", Punishment.Kind.MUTE), "no_permission");
		this.expect("lifting a mute", this.s.moderation.pardon(this.carol, this.t(this.alice), "Alice", Punishment.Kind.MUTE), "ok", "Alice");
		this.check(
			"the player is told, may speak, and the log has it",
			this.told("Alice", "unmuted") == 1
				&& this.s.moderation.mutedResult(a) == null
				&& this.s.moderation.chatAllowed(a)
				&& this.lastAudit().equals("pardon_mute")
				&& this.s.sync.self(a).mutedUntil == 0L
		);
		this.expect("lifting what is not there", this.s.moderation.pardon(this.carol, this.t(this.alice), "Alice", Punishment.Kind.MUTE), "not_punished", "Alice");
		this.expect("a warning cannot be lifted", this.s.moderation.pardon(this.carol, this.t(this.alice), "Alice", Punishment.Kind.WARN), "invalid");
		this.expect("muting for ten seconds", this.s.moderation.punish(this.carol, this.t(this.alice), "Alice", Punishment.Kind.MUTE, "", 10), "ok", "Alice");

		for (int i = 0; i < 9; i++) {
			this.second();
		}

		this.check("it holds for as long", this.s.moderation.mutedResult(a) != null && this.told("Alice", "unmuted") == 1);
		this.second();
		this.second();
		this.check(
			"and ends by itself, the player told once",
			this.s.moderation.mutedResult(a) == null && this.told("Alice", "unmuted") == 2 && !this.s.store.known(this.alice.id()).muted(this.time)
		);
		this.expect("muting until lifted", this.s.moderation.punish(this.carol, this.t(this.alice), "Alice", Punishment.Kind.MUTE, "", 0), "ok", "Alice");
		this.check("is told as such", this.s.sync.self(a).mutedUntil == -1L && this.s.moderation.mutedResult(a).arg(0).equals("∞"));
		this.expect("and lifted", this.s.moderation.pardon(this.carol, this.t(this.alice), "Alice", Punishment.Kind.MUTE), "ok", "Alice");
		this.expect("muting somebody a role exempts", this.s.moderation.punish(this.erin, this.t(this.dave), "Dave", Punishment.Kind.MUTE, "", 0), "ok", "Dave");
		this.check(
			"is on the record and does not bind them",
			this.at(this.dave).record.active(Punishment.Kind.MUTE, this.time) != null && this.s.moderation.mutedResult(this.at(this.dave)) == null
		);
		this.expect("and lifted again", this.s.moderation.pardon(this.erin, this.t(this.dave), "Dave", Punishment.Kind.MUTE), "ok", "Dave");
		this.expect(
			"a ban for good needs its own permission", this.s.moderation.punish(this.carol, this.t(this.alice), "Alice", Punishment.Kind.BAN, "", 0), "no_permission"
		);
		this.expect("a ban for a time", this.s.moderation.punish(this.carol, this.t(this.alice), "Alice", Punishment.Kind.BAN, "griefing", 3600), "ok", "Alice");
		this.check(
			"is kept with its expiry",
			this.s.store.known(this.alice.id()).banned(this.time)
				&& a.record.active(Punishment.Kind.BAN, this.time).expiresAt == this.time + 3600000L
				&& this.lastAudit().equals("punish_ban")
		);
		this.expect("lifting a ban", this.s.moderation.pardon(this.dave, this.t(this.alice), "Alice", Punishment.Kind.BAN), "ok", "Alice");
		this.expect("a ban for good by who may", this.s.moderation.punish(this.dave, this.t(this.bob), "Bob", Punishment.Kind.BAN, "", 0), "ok", "Bob");
		this.check(
			"has no expiry", this.at(this.bob).record.active(Punishment.Kind.BAN, this.time).expiresAt == 0L && this.s.store.known(this.bob.id()).bannedUntil == -1L
		);
		this.expect("and is lifted", this.s.moderation.pardon(this.dave, this.t(this.bob), "Bob", Punishment.Kind.BAN), "ok", "Bob");
		this.expect("banning somebody who is away", this.s.moderation.punish(this.dave, this.t(this.gina), "Gina", Punishment.Kind.BAN, "alt", 60), "ok", "Gina");
		this.expect("and lifting it", this.s.moderation.pardon(this.dave, this.t(this.gina), "Gina", Punishment.Kind.BAN), "ok", "Gina");
		this.expect("warning", this.s.moderation.punish(this.carol, this.t(this.alice), "Alice", Punishment.Kind.WARN, "be nice", 0), "ok", "Alice");
		this.check(
			"the player is told, and it is history, not something that holds",
			this.told("Alice", "warned") == 1 && a.record.active(Punishment.Kind.WARN, this.time) == null && this.lastAudit().equals("punish_warn")
		);
		this.expect(
			"kicking somebody who is away", this.s.moderation.punish(this.carol, this.t(this.gina), "Gina", Punishment.Kind.KICK, "", 0), "player_offline", "Gina"
		);
		this.expect("kicking", this.s.moderation.punish(this.carol, this.t(this.alice), "Alice", Punishment.Kind.KICK, "out", 0), "ok", "Alice");
		this.put(this.alice, place(0.0, 0.0));
		this.expect("freezing", this.s.moderation.punish(this.carol, this.t(this.alice), "Alice", Punishment.Kind.FREEZE, "wait", 0), "ok", "Alice");
		this.check(
			"the player is told and held",
			this.told("Alice", "frozen") == 1 && this.s.moderation.frozen(a) && this.s.sync.self(a).frozen && this.s.store.known(this.alice.id()).frozen(this.time)
		);
		this.expect("a frozen player does not teleport", this.s.travel.spawnGo(this.alice), "frozen");
		this.expect("or ask to", this.s.requests.ask(this.alice, this.t(this.bob), "Bob", false), "frozen");
		this.check(
			"and has no command but the private message",
			this.s.moderation.commandRefused(a, "home")
				&& this.s.moderation.commandRefused(a, "spawn")
				&& !this.s.moderation.commandRefused(a, "msg Carol sorry")
				&& this.told("Alice", "frozen") >= 1
		);
		this.put(this.alice, place(4.0, 0.0));
		this.s.moderation.tick();
		this.check("walking away puts them back", same(this.where(this.alice), place(0.0, 0.0)));
		this.put(this.carol, place(200.0, 0.0));
		this.expect("a moderator fetches somebody who is frozen", this.s.moderation.playerDo(this.carol, this.t(this.alice), "Alice", "bring", null), "ok", "Alice");
		this.s.moderation.tick();
		this.check("who is held where they were taken, not taken back", same(this.where(this.alice), place(200.0, 0.0)));
		this.put(this.alice, place(203.0, 0.0));
		this.s.moderation.tick();
		this.check("and put back there when they walk away", same(this.where(this.alice), place(200.0, 0.0)));
		this.s.moderation.copied(a, null);
		this.put(this.alice, place(50.0, 0.0));
		this.s.moderation.tick();
		this.put(this.alice, place(53.0, 0.0));
		this.s.moderation.tick();
		this.check("coming back to life, they are held where they come to stand and not where they died", same(this.where(this.alice), place(50.0, 0.0)));
		this.put(this.alice, place(0.0, 0.0));
		this.s.moderation.copied(a, null);
		this.expect("unfreezing", this.s.moderation.pardon(this.carol, this.t(this.alice), "Alice", Punishment.Kind.FREEZE), "ok", "Alice");
		this.check("the player is told and free", this.told("Alice", "unfrozen") == 1 && !this.s.moderation.frozen(a) && !this.s.moderation.commandRefused(a, "home"));
		this.put(this.alice, place(4.0, 0.0));
		this.s.moderation.tick();
		this.check("and stays where they walk", same(this.where(this.alice), place(4.0, 0.0)));
		this.expect("a note needs the permission", this.s.moderation.noteAdd(this.alice, this.t(this.bob), "Bob", "hm"), "no_permission");
		this.expect("a note", this.s.moderation.noteAdd(this.carol, this.t(this.alice), "Alice", "watch §cthis one"), "ok", "Alice");
		this.expect("a note that says nothing", this.s.moderation.noteAdd(this.carol, this.t(this.alice), "Alice", ""), "text_invalid");
		this.check(
			"is kept tidied and logged", a.record.notes.size() == 1 && a.record.notes.get(0).text.equals("watch this one") && this.lastAudit().equals("note_add")
		);
		this.expect("deleting a note that is not there", this.s.moderation.noteDelete(this.carol, this.t(this.alice), "Alice", 5), "invalid");
		Moderation.History history = this.s.moderation.history(this.carol, this.t(this.alice), "Alice").data(Moderation.History.class);
		this.check("a player's history is all of it", history != null && history.punishments().size() == 7 && history.notes().size() == 1);
		this.expect("deleting a note", this.s.moderation.noteDelete(this.carol, this.t(this.alice), "Alice", 0), "ok", "Alice");
		this.expect("the history needs the permission", this.s.moderation.history(this.alice, this.t(this.bob), "Bob"), "no_permission");
		this.expect("a note written by somebody higher", this.s.moderation.noteAdd(this.dave, this.t(this.alice), "Alice", "mine"), "ok", "Alice");
		this.expect("is not a moderator's to delete", this.s.moderation.noteDelete(this.carol, this.t(this.alice), "Alice", 0), "outranked", "Dave");
		this.expect("but its writer's", this.s.moderation.noteDelete(this.dave, this.t(this.alice), "Alice", 0), "ok", "Alice");
		this.expect("a note about a moderator", this.s.moderation.noteAdd(this.dave, this.t(this.carol), "Carol", "keen"), "ok", "Carol");
		Moderation.History own = this.s.moderation.history(this.carol, this.t(this.carol), "Carol").data(Moderation.History.class);
		Moderation.History others = this.s.moderation.history(this.dave, this.t(this.carol), "Carol").data(Moderation.History.class);
		this.check("is never shown to them, in their own history either", own != null && own.notes().isEmpty() && others != null && others.notes().size() == 1);
		this.expect("nor is it theirs to delete", this.s.moderation.noteDelete(this.carol, this.t(this.carol), "Carol", 0), "self");
		this.expect("and nobody writes one about themself", this.s.moderation.noteAdd(this.carol, this.t(this.carol), "Carol", "fine"), "self");
		this.expect("it goes as it came", this.s.moderation.noteDelete(this.dave, this.t(this.carol), "Carol", 0), "ok", "Carol");
		this.expect("healing needs the permission, oneself too", this.s.moderation.playerDo(this.alice, this.t(this.alice), "Alice", "heal", null), "no_permission");
		this.expect("healing", this.s.moderation.playerDo(this.carol, this.t(this.alice), "Alice", "heal", null), "ok", "Alice");
		this.check("is logged", this.lastAudit().equals("player_heal"));
		this.expect("not on a higher role", this.s.moderation.playerDo(this.carol, this.t(this.dave), "Dave", "feed", null), "outranked", "Dave");
		if (Thirst.present()) {
			this.expect("a drink, with a thirst mod here", this.s.moderation.playerDo(this.carol, this.t(this.alice), "Alice", "drink", null), "ok", "Alice");
			this.check("is logged", this.lastAudit().equals("player_drink"));
		} else {
			this.expect("no drink without a thirst mod", this.s.moderation.playerDo(this.carol, this.t(this.alice), "Alice", "drink", null), "disabled");
		}

		this.expect("flying on", this.s.moderation.playerDo(this.carol, this.t(this.alice), "Alice", "fly", "on"), "ok", "Alice");
		this.check("is kept and told to the player's client", a.record.fly && this.s.sync.self(a).fly && this.told("Alice", "fly:on") == 1);
		int flight = this.told("Alice", "flight");
		this.s.moderation.copied(a, null);
		this.s.moderation.tick();
		this.s.moderation.tick();
		this.check("and told once more after a death, to the new player their client makes itself", this.told("Alice", "flight") == flight + 1);
		this.expect("flying the other way round", this.s.moderation.playerDo(this.carol, this.t(this.alice), "Alice", "fly", null), "ok", "Alice");
		this.check("is off", !a.record.fly);
		this.s.moderation.copied(a, null);
		this.s.moderation.tick();
		this.check("and a death tells their client nothing of it", this.told("Alice", "flight") == flight + 1);
		this.expect("flying maybe", this.s.moderation.playerDo(this.carol, this.t(this.alice), "Alice", "fly", "maybe"), "value_invalid");
		this.expect("god mode for oneself", this.s.moderation.playerDo(this.carol, this.t(this.carol), "Carol", "god", "on"), "ok", "Carol");
		this.check("is kept", this.at(this.carol).record.god && this.s.sync.self(this.at(this.carol)).god);
		this.expect("and off", this.s.moderation.playerDo(this.carol, this.t(this.carol), "Carol", "god", "off"), "ok", "Carol");
		this.expect("a speed beyond ten", this.s.moderation.playerDo(this.carol, this.t(this.alice), "Alice", "speed", "11"), "value_invalid");
		this.expect("a speed", this.s.moderation.playerDo(this.carol, this.t(this.alice), "Alice", "speed", "3"), "ok", "Alice");
		this.expect("a game mode that is none", this.s.moderation.playerDo(this.carol, this.t(this.alice), "Alice", "gamemode", "xyz"), "value_invalid");
		this.expect("a game mode", this.s.moderation.playerDo(this.carol, this.t(this.alice), "Alice", "gamemode", "creative"), "ok", "Alice");
		this.expect("something that is not done to players", this.s.moderation.playerDo(this.carol, this.t(this.alice), "Alice", "tickle", null), "invalid");
		this.expect(
			"something done to somebody who is away", this.s.moderation.playerDo(this.carol, this.t(this.gina), "Gina", "heal", null), "player_offline", "Gina"
		);
		this.put(this.alice, place(40.0, 40.0));
		this.put(this.carol, place(200.0, 0.0));
		this.expect("going to a player", this.s.moderation.playerDo(this.carol, this.t(this.alice), "Alice", "goto", null), "ok", "Alice");
		this.check(
			"the moderator stands with them, and may go back",
			same(this.where(this.carol), place(40.0, 40.0)) && same(this.at(this.carol).record.back, place(200.0, 0.0)) && this.lastAudit().equals("player_goto")
		);
		this.put(this.carol, place(200.0, 0.0));
		int brought = this.told("Alice", "brought");
		this.expect("bringing a player", this.s.moderation.playerDo(this.carol, this.t(this.alice), "Alice", "bring", null), "ok", "Alice");
		this.check("they stand with the moderator and are told", same(this.where(this.alice), place(200.0, 0.0)) && this.told("Alice", "brought") == brought + 1);
		this.expect("going to where somebody who is away last was", this.s.moderation.playerDo(this.carol, this.t(this.gina), "Gina", "goto", null), "ok", "Gina");
		this.check("which is where they left", same(this.where(this.carol), place(600.0, 0.0)));
		this.expect("going to a higher role is no offence", this.s.moderation.playerDo(this.carol, this.t(this.dave), "Dave", "goto", null), "ok", "Dave");
		this.expect("bringing one is", this.s.moderation.playerDo(this.carol, this.t(this.dave), "Dave", "bring", null), "outranked", "Dave");
		this.expect("going to oneself", this.s.moderation.playerDo(this.carol, this.t(this.carol), "Carol", "goto", null), "self");
		this.expect("looking into an inventory", this.s.moderation.playerDo(this.carol, this.t(this.alice), "Alice", "invsee", null), "ok", "Alice");
		this.expect("looking into one's own", this.s.moderation.playerDo(this.carol, this.t(this.carol), "Carol", "invsee", null), "self");
		this.expect("looking into an ender chest", this.s.moderation.playerDo(this.carol, this.t(this.alice), "Alice", "endersee", null), "ok", "Alice");
		this.expect("vanishing", this.s.moderation.playerDo(this.carol, this.t(this.carol), "Carol", "vanish", "on"), "ok", "Carol");
		boolean aliceSees = false;
		boolean daveSees = false;

		for (OnlinePlayer o : this.s.sync.online(this.alice)) {
			if (o.id.equals(this.carol.id())) {
				aliceSees = true;
			}
		}

		for (OnlinePlayer ox : this.s.sync.online(this.dave)) {
			if (ox.id.equals(this.carol.id())) {
				daveSees = ox.vanished;
			}
		}

		this.check(
			"a vanished player is on only for those who may see them",
			!aliceSees
				&& daveSees
				&& !this.s.visible(this.alice, this.at(this.carol))
				&& this.s.visible(this.dave, this.at(this.carol))
				&& this.s.visible(this.carol, this.at(this.carol))
		);
		PlayerCard hidden = this.s.people.card(this.alice, this.t(this.carol), "Carol").data(PlayerCard.class);
		this.check(
			"their card reads as away", hidden != null && !hidden.online && this.s.people.card(this.dave, this.t(this.carol), "Carol").data(PlayerCard.class).online
		);
		this.time += 2000L;
		this.expect("and a request to them finds nobody", this.s.requests.ask(this.alice, this.t(this.carol), "Carol", false), "player_offline", "Carol");
		this.check(
			"the index knows, for when they come back in",
			this.s.store.known(this.carol.id()).vanished && this.s.vanish.is(this.carol.id()) && this.lastAudit().equals("player_vanish")
		);
		this.expect("appearing again", this.s.moderation.playerDo(this.carol, this.t(this.carol), "Carol", "vanish", null), "ok", "Carol");
		this.check("they are on for everyone", this.s.visible(this.alice, this.at(this.carol)) && !this.s.vanish.is(this.carol.id()));
		Board seen = this.s.statistics.board(this.alice, "last_seen_ms", 0, 100).data(Board.class);
		this.check(
			"whoever is on was last seen now", seen != null && seen.rows().stream().anyMatch(r -> r.player().equals(this.frank.id()) && r.value() == this.time)
		);
		this.expect("an operator vanishes", this.s.moderation.playerDo(this.frank, this.t(this.frank), "Frank", "vanish", "on"), "ok", "Frank");
		long before = this.at(this.frank).seenBefore;
		Board unseen = this.s.statistics.board(this.alice, "last_seen_ms", 0, 100).data(Board.class);
		this.check(
			"and no ranking tells that they are still here",
			unseen != null
				&& unseen.rows().stream().noneMatch(r -> r.player().equals(this.frank.id()) && r.value() > before)
				&& this.s.statistics.value(this.frank.id(), "last_seen_ms", true) == before
				&& this.s.statistics.value(this.frank.id(), "last_seen_ms") == this.time
		);
		Snapshot hiddenStats = this.s.statistics.stats(this.alice, this.t(this.frank), "Frank").data(Snapshot.class);
		Snapshot shownStats = this.s.statistics.stats(this.dave, this.t(this.frank), "Frank").data(Snapshot.class);
		this.check(
			"nor do their statistics, but to those who may see them",
			hiddenStats != null && hiddenStats.extra("last_seen_ms") == before && shownStats != null && shownStats.extra("last_seen_ms") == this.time
		);
		PlayerRef[] listed = this.s.statistics.players(this.alice).data(PlayerRef[].class);
		this.check(
			"in the list of players they stand among those who are away",
			listed != null && Stream.of(listed).anyMatch(r -> r.id().equals(this.frank.id()) && !r.online())
		);
		this.expect("and appears again", this.s.moderation.playerDo(this.frank, this.t(this.frank), "Frank", "vanish", "off"), "ok", "Frank");
		this.expect("telling everyone needs the permission", this.s.moderation.broadcast(this.alice, "hello all"), "no_permission");
		this.expect("telling everyone nothing", this.s.moderation.broadcast(this.carol, "  "), "text_invalid");
		this.expect("telling everyone", this.s.moderation.broadcast(this.carol, "The server restarts soon"), "ok");
		this.check(
			"everyone hears it and the log has it",
			this.told("*", "broadcast") == 1 && this.lastAudit().equals("broadcast") && this.s.audit.last().detail.equals("The server restarts soon")
		);
		this.expect("the staff chat needs the permission", this.s.moderation.staffChat(this.alice, "psst"), "no_permission");
		int daveHeard = this.told("Dave", "staff");
		int aliceHeard = this.told("Alice", "staff");
		this.expect("the staff chat", this.s.moderation.staffChat(this.carol, "look at Alice"), "ok", "4");
		this.check(
			"is heard by staff and nobody else",
			this.told("Dave", "staff") == daveHeard + 1 && this.told("Carol", "staff") >= 1 && this.told("Alice", "staff") == aliceHeard
		);
		this.rules.staffChat = false;
		this.expect("the staff chat switched off", this.s.moderation.staffChat(this.carol, "hello"), "disabled");
		this.rules.staffChat = true;
	}

	private void administration() {
		this.expect("the players list needs the permission", this.s.admin.players(this.alice, "", "all", 0), "no_permission");
		AdminData.PlayerPage everyone = this.s.admin.players(this.dave, "", "all", 0).data(AdminData.PlayerPage.class);
		this.check(
			"the players list has everyone, those who are on first",
			everyone != null && everyone.total == 8 && everyone.rows.size() == 8 && everyone.rows.get(0).online && !everyone.rows.get(7).online
		);
		AdminData.PlayerPage on = this.s.admin.players(this.dave, null, "online", 0).data(AdminData.PlayerPage.class);
		this.check("it filters", on != null && on.total == 7 && this.s.admin.players(this.dave, "", "offline", 0).data(AdminData.PlayerPage.class).total == 1);
		AdminData.PlayerPage found = this.s.admin.players(this.dave, "ALI", "all", 0).data(AdminData.PlayerPage.class);
		this.check(
			"it searches",
			found != null && found.total == 1 && found.rows.get(0).name.equals("Alice") && found.rows.get(0).role != null && found.rows.get(0).role.id.equals("vip")
		);
		this.check("a page beyond the last is empty", this.s.admin.players(this.dave, "", "all", 9).data(AdminData.PlayerPage.class).rows.isEmpty());
		this.expect("a filter that is none", this.s.admin.players(this.dave, "", "sleepy", 0), "invalid");
		this.expect("muting for the list", this.s.moderation.punish(this.carol, this.t(this.bob), "Bob", Punishment.Kind.MUTE, "list", 600), "ok", "Bob");
		AdminData.PlayerPage mutedOnes = this.s.admin.players(this.dave, "", "muted", 0).data(AdminData.PlayerPage.class);
		this.check(
			"the muted are found without opening files",
			mutedOnes != null && mutedOnes.total == 1 && mutedOnes.rows.get(0).muted && mutedOnes.rows.get(0).name.equals("Bob")
		);
		AdminData.PlayerDetail detail = this.s.admin.player(this.dave, this.t(this.alice), "Alice").data(AdminData.PlayerDetail.class);
		this.check(
			"a player's detail is their whole record",
			detail != null
				&& detail.row.name.equals("Alice")
				&& detail.mayActOn
				&& detail.punishments.size() == 7
				&& detail.roles.size() == 1
				&& detail.homes.isEmpty()
				&& detail.place != null
				&& detail.sessions >= 1L
		);
		AdminData.PlayerDetail higher = this.s.admin.player(this.carol, this.t(this.dave), "Dave").data(AdminData.PlayerDetail.class);
		this.check("and says whether the asker may act on them", higher != null && !higher.mayActOn);
		this.expect("the detail of nobody", this.s.admin.player(this.dave, null, "Zed"), "no_such_player", "Zed");
		this.expect("the roles need the permission", this.s.admin.roles(this.carol), "no_permission");
		AdminData.RoleSet set = this.s.admin.roles(this.dave).data(AdminData.RoleSet.class);
		this.check(
			"the role editor is told every role, every node and how far the asker reaches",
			set != null
				&& set.roles.size() == this.roles.roles.size()
				&& set.holders.size() == set.roles.size()
				&& set.editableBelow == 100
				&& set.nodes.stream().anyMatch(n -> n.id.equals(Nodes.HOME_USE.id()) && !n.description.isEmpty())
				&& set.nodes.stream().anyMatch(n -> n.id.equals(Nodes.HOMES_MAX.id()) && n.number && n.defaultNumber == 3)
				&& set.roles.get(0).id.equals("admin")
		);
		this.check(
			"a warp's or a kit's own node is described by its family's line",
			!Admin.describe(new Node("cfhgui_server.warp.to.vault", 2, false, 0)).isEmpty()
				&& !Admin.describe(new Node("cfhgui_server.kit.use.secret", 2, false, 0)).isEmpty()
				&& Admin.describe(new Node("some_mod.thing", 2, false, 0)).isEmpty()
		);
		Role builder = new Role();
		builder.id = "Builder";
		builder.name = "Builder §cX";
		builder.priority = 10;
		builder.prefix = "Build";
		builder.allow.addAll(List.of("cfhgui_server.home.*", "NOT A NODE", "x y", "cfhgui_server.home.*"));
		builder.inherits.addAll(List.of("default", "nope", "builder"));
		builder.numbers.put(Nodes.HOMES_MAX.id(), 6);
		this.expect(
			"a role with a line that is no permission is refused, and the line named", this.s.admin.roleSave(this.dave, builder), "pattern_invalid", "not a node"
		);
		this.check("nothing of it is kept", this.roles.get("builder") == null);
		builder.allow.removeIf(line -> line.contains(" "));
		builder.deny.add("cfhgui_server.*.use");
		this.expect(
			"a denial that is none is not answered as though it were kept", this.s.admin.roleSave(this.dave, builder), "pattern_invalid", "cfhgui_server.*.use"
		);
		builder.deny.clear();
		this.expect("creating a role", this.s.admin.roleSave(this.dave, builder), "ok", "builder");
		Role saved = this.roles.get("builder");
		this.check(
			"it is kept tidied: an id, a clean name, each pattern once, only roles that are roles",
			saved != null
				&& saved.name.equals("Builder X")
				&& saved.allow.equals(List.of("cfhgui_server.home.*"))
				&& saved.inherits.equals(List.of("default"))
				&& saved.numbers.get(Nodes.HOMES_MAX.id()) == 6
				&& this.lastAudit().equals("role_save")
		);
		builder.priority = 100;
		this.expect("a role as high as one's own", this.s.admin.roleSave(this.dave, builder), "role_too_high");
		Role admin = (Role)JsonWire.GSON.fromJson(JsonWire.GSON.toJson(this.roles.get("admin")), Role.class);
		admin.name = "Boss";
		this.expect("one's own role", this.s.admin.roleSave(this.dave, admin), "role_too_high");
		this.expect("is for somebody above it to change", this.s.admin.roleSave(this.erin, admin), "ok", "admin");
		Role bad = new Role();
		bad.id = "bad id!";
		this.expect("a role without an id", this.s.admin.roleSave(this.dave, bad), "name_invalid");
		this.expect("no role at all", this.s.admin.roleSave(this.dave, null), "invalid");
		this.expect("a role that may edit roles", this.grant(this.hank, "helper", 0), "ok");
		Role more = (Role)JsonWire.GSON.fromJson(JsonWire.GSON.toJson(this.roles.get("builder")), Role.class);
		more.allow.add(Nodes.ADMIN_RULES.id());
		this.expect("cannot give a role what its holder has not", this.s.admin.roleSave(this.hank, more), "role_beyond_own", Nodes.ADMIN_RULES.id());
		more.allow.remove(Nodes.ADMIN_RULES.id());
		more.allow.add(Nodes.BACK_USE.id());
		this.expect("and can give it what they have", this.s.admin.roleSave(this.hank, more), "ok", "builder");
		more.inherits.add("admin");
		this.expect("nor let it inherit a role above them", this.s.admin.roleSave(this.hank, more), "role_too_high");
		this.expect("nor delete one", this.s.admin.roleDelete(this.hank, "moderator"), "role_too_high");
		Role everybodys = (Role)JsonWire.GSON.fromJson(JsonWire.GSON.toJson(this.roles.get("default")), Role.class);
		everybodys.deny.add("*");
		this.expect("nor take everything from those above them through the role everybody has", this.s.admin.roleSave(this.hank, everybodys), "role_takes_from_above");
		everybodys.deny.clear();
		everybodys.deny.add(Nodes.ADMIN_ROLES.id());
		this.expect("or the one permission it would take to undo that", this.s.admin.roleSave(this.hank, everybodys), "role_takes_from_above", Nodes.ADMIN_ROLES.id());
		this.check(
			"the role is as it was, and those above every role have what they had",
			this.roles.get("default").deny.isEmpty() && this.s.may(this.erin, Nodes.ADMIN_ROLES) && this.s.may(this.erin, Nodes.HOME_USE)
		);
		everybodys.deny.clear();
		everybodys.prefix = "P";
		this.expect("what takes nothing from anybody above is theirs to change there", this.s.admin.roleSave(this.hank, everybodys), "ok", "default");
		everybodys.prefix = "";
		this.expect("and to change back", this.s.admin.roleSave(this.hank, everybodys), "ok", "default");
		this.expect(
			"nor hand out a role that allows what they have not themself",
			this.s.admin.roleGrant(this.hank, this.t(this.bob), "Bob", "vip", 0),
			"role_beyond_own",
			Nodes.NICK_COLOR.id()
		);
		this.expect("to themself least of all", this.s.admin.roleGrant(this.hank, this.t(this.hank), "Hank", "vip", 0), "role_beyond_own", Nodes.NICK_COLOR.id());
		this.expect("the default role cannot be deleted", this.s.admin.roleDelete(this.erin, "default"), "role_protected");
		this.expect("deleting a role that is not there", this.s.admin.roleDelete(this.erin, "nope"), "no_such_role", "nope");
		this.expect("giving roles needs the permission", this.s.admin.roleGrant(this.carol, this.t(this.bob), "Bob", "builder", 0), "no_permission");
		this.expect("giving one's own role", this.s.admin.roleGrant(this.dave, this.t(this.bob), "Bob", "admin", 0), "role_too_high");
		this.expect("giving a role to somebody higher", this.s.admin.roleGrant(this.dave, this.t(this.erin), "Erin", "builder", 0), "outranked", "Erin");
		this.expect("giving a role that is not there", this.s.admin.roleGrant(this.dave, this.t(this.bob), "Bob", "nope", 0), "no_such_role", "nope");
		this.expect("giving the role everyone has", this.s.admin.roleGrant(this.dave, this.t(this.bob), "Bob", "default", 0), "already_has_role", "Bob");
		int granted = this.told("Bob", "role_granted");
		this.expect("giving a role for a time", this.s.admin.roleGrant(this.dave, this.t(this.bob), "Bob", "builder", 60), "ok", "Bob");
		this.check(
			"the player is told, has its limit, and the log has it",
			this.told("Bob", "role_granted") == granted + 1
				&& this.s.perms.number(this.bob, Nodes.HOMES_MAX, 2) == 6
				&& this.lastAudit().equals("role_grant")
				&& this.s.audit.last().detail.equals("builder (1m)")
				&& this.s.store.known(this.bob.id()).roles.contains("builder")
		);
		this.expect("giving it for good replaces the time", this.s.admin.roleGrant(this.dave, this.t(this.bob), "Bob", "builder", 0), "ok", "Bob");
		this.expect("giving it for good twice", this.s.admin.roleGrant(this.dave, this.t(this.bob), "Bob", "builder", 0), "already_has_role", "Bob");
		this.expect("taking a role that is not held", this.s.admin.roleRevoke(this.dave, this.t(this.alice), "Alice", "builder"), "role_not_held", "Alice");
		this.expect("deleting a role that is held", this.s.admin.roleDelete(this.dave, "builder"), "ok", "builder");
		this.check(
			"takes it from its holders",
			this.at(this.bob).record.roles.isEmpty() && this.roles.get("builder") == null && this.s.perms.number(this.bob, Nodes.HOMES_MAX, 2) == 2
		);
		this.expect("the kits need the permission", this.s.admin.kits(this.carol), "no_permission");
		this.check("the kits are listed whole", this.s.admin.kits(this.dave).data(Kit[].class) != null && this.s.admin.kits(this.dave).data(Kit[].class).length == 4);
		Kit tools = new Kit();
		tools.id = "Tools";
		tools.name = "Tools";
		tools.items.add("{id:\"minecraft:stick\",count:2}");
		tools.cooldownSeconds = -5;
		this.expect("saving a kit", this.s.admin.kitSave(this.dave, tools, false), "ok", "tools");
		this.check(
			"it is kept tidied and logged",
			this.kits.get("tools") != null
				&& this.kits.get("tools").cooldownSeconds == 0
				&& this.kits.get("tools").items.size() == 1
				&& this.lastAudit().equals("kit_save")
		);
		tools.items.add("this is {{ not an item");
		this.expect("a kit with something that is no item", this.s.admin.kitSave(this.dave, tools, false), "value_invalid");
		tools.id = "create";
		this.expect("a kit called like a command", this.s.admin.kitSave(this.dave, tools, false), "name_invalid");
		tools.id = "tools";
		this.expect("a kit from an inventory needs somebody in the world", this.s.admin.kitSave(this.dave, tools, true), "player_only");
		int received = this.told("Alice", "kit_received");
		this.expect("handing a kit over", this.s.admin.kitGive(this.dave, this.t(this.alice), "Alice", "tools"), "ok", "Alice");
		this.check("the player is told", this.told("Alice", "kit_received") == received + 1 && this.lastAudit().equals("kit_give"));
		this.expect("handing one to somebody who is away", this.s.admin.kitGive(this.dave, this.t(this.gina), "Gina", "tools"), "player_offline", "Gina");
		this.expect("deleting a kit", this.s.admin.kitDelete(this.dave, "tools"), "ok", "tools");
		this.expect("deleting it again", this.s.admin.kitDelete(this.dave, "tools"), "no_such_kit", "tools");
		this.expect("the punishments need the permission", this.s.admin.punishments(this.alice, "all", null, 0), "no_permission");
		AdminData.PunishmentPage every = this.s.admin.punishments(this.carol, "all", null, 0).data(AdminData.PunishmentPage.class);
		this.check(
			"every punishment there was, newest first",
			every != null && every.total == 11 && every.rows.get(0).playerName.equals("Bob") && every.rows.get(0).punishment.kind == Punishment.Kind.MUTE
		);
		AdminData.PunishmentPage mutes = this.s.admin.punishments(this.carol, "all", Punishment.Kind.MUTE, 0).data(AdminData.PunishmentPage.class);
		this.check("of one kind", mutes != null && mutes.total == 5 && mutes.rows.stream().allMatch(r -> r.punishment.kind == Punishment.Kind.MUTE));
		AdminData.PunishmentPage active = this.s.admin.punishments(this.carol, null, null, 0).data(AdminData.PunishmentPage.class);
		this.check("or only those that hold", active != null && active.total == 1 && active.rows.get(0).playerName.equals("Bob"));
		this.expect("a filter that is none", this.s.admin.punishments(this.carol, "old", null, 0), "invalid");
		this.expect("lifting the mute again", this.s.moderation.pardon(this.carol, this.t(this.bob), "Bob", Punishment.Kind.MUTE), "ok", "Bob");
		this.expect("the audit log needs the permission", this.s.admin.audit(this.carol, "", 0), "no_permission");
		AdminData.AuditPage log = this.s.admin.audit(this.dave, "", 0).data(AdminData.AuditPage.class);
		this.check(
			"the audit log is paged, newest first",
			log != null && log.total == this.s.audit.size() && log.rows.size() == Math.min(50, log.total) && log.rows.get(0).action.equals("pardon_mute")
		);
		AdminData.AuditPage search = this.s.admin.audit(this.dave, "PUNISH_MUTE", 0).data(AdminData.AuditPage.class);
		this.check("and searched", search != null && search.total == 5 && search.rows.stream().allMatch(e -> e.action.equals("punish_mute")));
		this.expect("the rules need the permission", this.s.admin.rules(this.carol), "no_permission");
		this.check("the rules are sent whole", this.s.admin.rules(this.dave).data(ServerRules.class) == this.rules);
		this.expect("setting a rule", this.s.admin.ruleSet(this.dave, "maxHomes", "5"), "ok", "maxHomes");
		this.check(
			"it holds at once, is logged, and clients are told again",
			this.rules.maxHomes == 5
				&& this.lastAudit().equals("rule_set")
				&& this.s.sync.marked(this.alice.id(), "hello")
				&& this.s.perms.number(this.bob, Nodes.HOMES_MAX, this.rules.maxHomes) == 5
		);
		this.expect("a rule that is not there", this.s.admin.ruleSet(this.dave, "maxDragons", "5"), "no_such_rule", "maxDragons");
		this.expect("a value of the wrong kind", this.s.admin.ruleSet(this.dave, "maxHomes", "\"many\""), "value_invalid");
		this.expect("a value out of range", this.s.admin.ruleSet(this.dave, "maxHomes", "-4"), "value_invalid");
		this.expect("no value", this.s.admin.ruleSet(this.dave, "maxHomes", "null"), "value_invalid");
		this.expect("a list", this.s.admin.ruleSet(this.dave, "rules", "[\"Be kind\", \"No griefing\"]"), "ok", "rules");
		this.check("is a list", this.rules.rules.size() == 2 && this.s.sync.hello(this.alice).rules.size() == 2);
		this.expect("a link that is no link", this.s.admin.ruleSet(this.dave, "links", "[{\"name\":\"Map\",\"url\":\"javascript:alert(1)\"}]"), "value_invalid");
		this.expect("a link", this.s.admin.ruleSet(this.dave, "links", "[{\"name\":\"Map\",\"url\":\"https://example.org/map\"}]"), "ok", "links");
		this.expect("a rule sanitised into its range", this.s.admin.ruleSet(this.dave, "afkSeconds", "1"), "ok", "afkSeconds");
		this.check("is within it", this.rules.afkSeconds == 30);
		this.rules.afkSeconds = 1000000;
		this.expect("an operator of level 2 does not set rules", this.s.admin.ruleSet(this.frank, "maxHomes", "9"), "no_permission");
		this.rules.maxHomes = 2;
		this.expect("maintenance by who may", this.s.admin.ruleSet(this.dave, "maintenance", "true"), "ok", "maintenance");
		this.check(
			"is on, logged as such, and told to clients",
			this.rules.maintenance && this.lastAudit().equals("maintenance") && this.s.audit.last().detail.equals("on") && this.s.sync.hello(this.alice).maintenance
		);
		this.expect("and off", this.s.admin.ruleSet(this.dave, "maintenance", "false"), "ok", "maintenance");
		Role keeper = new Role();
		keeper.id = "keeper";
		keeper.priority = 15;
		keeper.allow.add(Nodes.ADMIN_MAINTENANCE.id());
		this.expect("a role for maintenance alone", this.s.admin.roleSave(this.erin, keeper), "ok", "keeper");
		this.expect("is given", this.grant(this.bob, "keeper", 0), "ok");
		this.expect("its holder switches maintenance", this.s.admin.ruleSet(this.bob, "maintenance", "true"), "ok", "maintenance");
		this.expect("and its message", this.s.admin.ruleSet(this.bob, "maintenanceMessage", "\"Back soon\""), "ok", "maintenanceMessage");
		this.expect("and no other rule", this.s.admin.ruleSet(this.bob, "maxHomes", "9"), "no_permission");
		this.expect("maintenance off again", this.s.admin.ruleSet(this.bob, "maintenance", "false"), "ok", "maintenance");
		this.rules.maintenanceMessage = "";
		this.expect("reading the files again", this.s.admin.rulesReload(this.dave), "ok");
		this.expect("needs the permission", this.s.admin.rulesReload(this.carol), "no_permission");
		this.expect("the world needs the permission", this.s.admin.world(this.carol), "no_permission");
		AdminData.WorldInfo world = this.s.admin.world(this.dave).data(AdminData.WorldInfo.class);
		this.check(
			"the world is described: difficulty, dimensions, game rules",
			world != null
				&& !world.difficulty.isEmpty()
				&& !world.dimensions.isEmpty()
				&& world.gameRules.size() > 20
				&& world.gameRules.stream().anyMatch(g -> g.id.equals("doDaylightCycle") && g.bool && g.nameKey.equals("gamerule.doDaylightCycle"))
		);
		this.expect("changing the world needs the permission", this.s.admin.worldSet(this.carol, "time", null, "day", null), "no_permission");
		this.expect("changing something that is not the world's", this.s.admin.worldSet(this.dave, "gravity", null, "1", null), "invalid");
		this.expect("a time that is none", this.s.admin.worldSet(this.dave, "time", null, "soon", null), "value_invalid");
		this.expect("a weather that is none", this.s.admin.worldSet(this.dave, "weather", null, "frogs", null), "value_invalid");
		this.expect("a difficulty that is none", this.s.admin.worldSet(this.dave, "difficulty", null, "brutal", null), "value_invalid");
		this.expect("a game rule that is not there", this.s.admin.worldSet(this.dave, "gamerule", "doDragons", "true", null), "no_such_rule", "doDragons");
		this.expect("a game rule's value of the wrong kind", this.s.admin.worldSet(this.dave, "gamerule", "doDaylightCycle", "sometimes", null), "value_invalid");
		this.expect(
			"a number beyond what the rule itself takes", this.s.admin.worldSet(this.dave, "gamerule", "spawnChunkRadius", "100000000", null), "value_invalid"
		);
		this.expect("a number that is none", this.s.admin.worldSet(this.dave, "gamerule", "randomTickSpeed", "3 or so", null), "value_invalid");
		if (world != null) {
			String now = world.gameRules.stream().filter(g -> g.id.equals("doDaylightCycle")).findFirst().map(g -> g.value).orElse("true");
			this.expect("a game rule", this.s.admin.worldSet(this.dave, "gamerule", "doDaylightCycle", now, null), "ok");
			this.check("is logged", this.lastAudit().equals("world_set") && this.s.audit.last().detail.equals("doDaylightCycle = " + now));
		}

		this.expect("the dashboard needs the permission", this.s.admin.watch(this.alice, true), "no_permission");
		this.expect("watching the dashboard", this.s.admin.watch(this.dave, true), "ok");
		AdminData.Dashboard dash = this.s.admin.dashboard();
		this.check(
			"the dashboard is filled",
			this.at(this.dave).watching
				&& dash.tps > 0.0
				&& dash.tps <= 20.0
				&& dash.memoryMax > 0L
				&& dash.memoryUsed > 0L
				&& !dash.dimensions.isEmpty()
				&& dash.maxPlayers > 0
				&& dash.msptHistory.length >= 1
		);
		this.expect("and no longer", this.s.admin.watch(this.dave, false), "ok");
	}

	private void door() {
		String[] ops = new String[]{
			"home_set",
			"home_edit",
			"home_delete",
			"home_go",
			"back",
			"spawn_go",
			"spawn_set",
			"warp_go",
			"warp_set",
			"warp_edit",
			"warp_delete",
			"tpa",
			"tpa_accept",
			"tpa_deny",
			"tpa_cancel",
			"tpa_toggle",
			"rtp",
			"tp_cancel",
			"tp_position",
			"afk",
			"nick",
			"mail_send",
			"mail_read",
			"mail_delete",
			"kit_claim",
			"stats_hide",
			"open",
			"staff_chat",
			"sync",
			"players",
			"stats",
			"board",
			"card",
			"punish",
			"pardon",
			"note_add",
			"note_delete",
			"player_do",
			"broadcast",
			"admin_watch",
			"admin_players",
			"admin_player",
			"admin_roles",
			"role_save",
			"role_delete",
			"role_grant",
			"role_revoke",
			"admin_kits",
			"kit_save",
			"kit_delete",
			"kit_give",
			"admin_punishments",
			"admin_audit",
			"admin_rules",
			"rule_set",
			"rules_reload",
			"admin_world",
			"world_set"
		};
		int known = 0;
		StringBuilder unknown = new StringBuilder();

		for (String op : ops) {
			OpResult low = this.s.ops.run(this.hank, op, new CompoundTag());
			OpResult high = this.s.ops.run(this.erin, op, new CompoundTag());
			if (!low.code().equals("unknown") && !high.code().equals("unknown")) {
				known++;
			} else {
				unknown.append(op).append(' ');
			}
		}

		this.check("every op of the contract is answered (" + unknown.toString().trim() + ")", known == ops.length);
		this.s.teleports.drop(this.at(this.hank));
		this.s.teleports.drop(this.at(this.erin));
		this.expect("an op nobody knows", this.s.ops.run(this.alice, "make_coffee", new CompoundTag()), "unknown");
		this.expect("no op at all", this.s.ops.run(this.alice, null, null), "unknown");
		CompoundTag wrong = new CompoundTag();
		wrong.putInt("name", 7);
		wrong.putString("color", "red");
		OpResult typed = this.s.ops.run(this.dave, "home_set", wrong);
		this.check("a value of the wrong kind is not there: the default home is set", typed.ok() && typed.arg(0).equals("home"));
		CompoundTag nowhere = new CompoundTag();
		nowhere.putDouble("z", 4.0);
		this.expect("a place without an x", this.s.ops.run(this.frank, "tp_position", nowhere), "invalid");
		CompoundTag brokenRole = new CompoundTag();
		brokenRole.putString("role", "{\"id\": \"x\", \"allow\": 5");
		this.expect("a role that is not JSON", this.s.ops.run(this.dave, "role_save", brokenRole), "invalid");
		CompoundTag brokenKit = new CompoundTag();
		brokenKit.putString("kit", "[1, 2, 3]");
		this.expect("a kit that is not a kit", this.s.ops.run(this.dave, "kit_save", brokenKit), "invalid");
		CompoundTag kind = new CompoundTag();
		kind.putUUID("id", this.alice.id());
		kind.putString("kind", "TICKLE");
		this.expect("a punishment of a kind that is none", this.s.ops.run(this.carol, "punish", kind), "invalid");
		CompoundTag byId = new CompoundTag();
		byId.putUUID("id", this.bob.id());
		this.check("a player is named by id", this.s.ops.run(this.alice, "card", byId).data(PlayerCard.class) != null);
		CompoundTag byName = new CompoundTag();
		byName.putString("player", "bob");
		this.check("or by name, whatever the case", this.s.ops.run(this.alice, "card", byName).data(PlayerCard.class) != null);
		CompoundTag stranger = new CompoundTag();
		stranger.putUUID("id", UUID.randomUUID());
		this.expect("an id of nobody", this.s.ops.run(this.alice, "card", stranger), "no_such_player");
		CompoundTag longText = new CompoundTag();
		longText.putString("player", "Bob");
		longText.putString("text", "x".repeat(5000));
		this.expect("text beyond any limit", this.s.ops.run(this.erin, "mail_send", longText), "text_invalid");
		this.time += 1000L;
		this.check("one's own statistics need no id", this.s.ops.run(this.alice, "stats", new CompoundTag()).data(Snapshot.class) != null);
		this.expect("asked for again at once", this.s.ops.run(this.alice, "stats", new CompoundTag()), "too_fast");
		Session session = this.at(this.alice);
		this.check(
			"a limit on how often holds, and passes",
			session.allow("test", 1000L, this.time) && !session.allow("test", 1000L, this.time + 500L) && session.allow("test", 1000L, this.time + 1600L)
		);
		OpResult warm = OpResult.warmup(3);
		OpResult fail = OpResult.fail("cooldown", 5);
		OpResult answer = OpResult.answer("x");
		this.check(
			"a result goes onto the wire as it is",
			warm.payload(7).ok()
				&& warm.payload(7).request() == 7
				&& warm.payload(7).code().equals("warmup")
				&& !fail.payload(1).ok()
				&& fail.payload(1).args().equals(List.of("5"))
				&& answer.ok()
				&& "x".equals(answer.data(String.class))
				&& answer.data(Integer.class) == null
		);
		Component words = fail.message();
		this.check("and into words, with English for a client without the mod", words.getString().contains("5") && !words.getString().startsWith("cfhgui_server."));
	}

	private void persistence() {
		this.s.store.save(this.s.saver);
		this.check("everything is written in time", this.s.saver.await() && !this.s.store.pending());
		Path players = this.dir.resolve("players");
		this.check(
			"each player has a file named by their id, and the index, the warps and the log are there",
			Files.isRegularFile(players.resolve(this.alice.id() + ".json"))
				&& Files.isRegularFile(this.dir.resolve("names.json"))
				&& Files.isRegularFile(this.dir.resolve("warps.json"))
				&& Files.isRegularFile(this.dir.resolve("audit.jsonl"))
		);

		try (Stream<Path> files = Files.list(players)) {
			this.check("no temporary file is left behind", files.noneMatch(f -> f.getFileName().toString().endsWith(".tmp")));
		} catch (IOException var28) {
			this.check("the players' folder can be listed", false);
		}

		ServerService again = ServerService.scratch(this.server, this.dir, new ServerRules(), this.roles, this.kits);

		try {
			PlayerRecord a = again.store.record(this.alice.id());
			PlayerRecord b = again.store.record(this.bob.id());
			PlayerRecord g = again.store.record(this.gina.id());
			this.check(
				"after a restart the players are known",
				again.store.size() == this.s.store.size() && again.store.known(this.alice.id()).name.equals("Alice") && this.alice.id().equals(again.store.idOf("alice"))
			);
			this.check(
				"their records are whole: homes, roles, history, letters",
				a != null
					&& b != null
					&& g != null
					&& a.roles.size() == 1
					&& a.roles.get(0).role.equals("vip")
					&& a.punishments.size() == this.at(this.alice).record.punishments.size()
					&& b.homes.size() == this.at(this.bob).record.homes.size()
					&& b.home("hell") != null
					&& "minecraft:the_nether".equals(b.home("hell").place.dimension)
					&& g.mail.size() == 1
					&& g.mail.get(0).text.equals("see you")
					&& a.kitsTaken.contains("once")
			);
			this.check(
				"the warps and the spawn are back",
				again.store.warps().size() == 1
					&& again.store.warp("vault") != null
					&& again.store.warp("vault").restricted
					&& same(again.store.spawn(), this.s.store.spawn())
			);
			this.check(
				"the audit log is back, line for line",
				again.audit.size() == this.s.audit.size() && again.audit.last() != null && again.audit.last().action.equals(this.lastAudit())
			);
			this.check(
				"permissions hold from the files alone",
				Boolean.TRUE.equals(again.perms.hasOffline(this.carol.id(), Nodes.MOD_MUTE.id()))
					&& !Boolean.TRUE.equals(again.perms.hasOffline(this.alice.id(), Nodes.RTP_USE.id()))
			);
			again.clock(() -> this.time);
			Actor visitor = again.enter(this.hank.id(), "Hank", 0, place(700.0, 0.0), null).actor();
			CompoundTag one = new CompoundTag();
			CompoundTag other = new CompoundTag();
			one.putUUID("id", this.carol.id());
			other.putUUID("id", this.dave.id());
			this.check(
				"a record that is not in memory is read for whoever asks",
				!again.store.inMemory(this.carol.id()) && again.ops.run(visitor, "card", one).ok() && again.store.inMemory(this.carol.id())
			);
			this.expect("but not one after another as fast as a client can ask", again.ops.run(visitor, "card", other), "too_fast");
			this.check(
				"what is in memory is theirs to ask about as often as they like", again.ops.run(visitor, "card", one).ok() && !again.store.inMemory(this.dave.id())
			);
			this.time += 100L;
			this.check("and a moment later the next is read", again.ops.run(visitor, "card", other).ok() && again.store.inMemory(this.dave.id()));
		} finally {
			again.saver.close();
		}

		CFHGuiServer.LOGGER
			.info(
				"[CFHGui Server] selftest: three files of the test's own folder are spoilt on purpose now; the errors about unreadable files that follow are what is being tested"
			);

		try {
			Files.writeString(players.resolve(this.bob.id() + ".json"), "{ this is not a record", StandardCharsets.UTF_8);
			Files.delete(this.dir.resolve("names.json"));
			Files.writeString(this.dir.resolve("warps.json"), "[[[", StandardCharsets.UTF_8);
			Files.writeString(
				this.dir.resolve("audit.jsonl"), Files.readString(this.dir.resolve("audit.jsonl"), StandardCharsets.UTF_8) + "not a line\n", StandardCharsets.UTF_8
			);
		} catch (IOException var23) {
			this.check("the files can be spoilt for the next check", false);
			return;
		}

		ServerService mended = ServerService.scratch(this.server, this.dir, new ServerRules(), this.roles, this.kits);

		try {
			this.check(
				"a missing index is made again from the players' files",
				mended.store.size() == this.s.store.size() - 1 && mended.store.known(this.alice.id()) != null && this.alice.id().equals(mended.store.idOf("Alice"))
			);
			this.check(
				"a file that cannot be read is set aside, not fatal",
				Files.isRegularFile(players.resolve(this.bob.id() + ".json.broken"))
					&& Files.isRegularFile(this.dir.resolve("warps.json.broken"))
					&& mended.store.warps().isEmpty()
					&& mended.store.record(this.alice.id()) != null
			);
			this.check(
				"a line of the log that cannot be read is left out and the file kept",
				mended.audit.size() == this.s.audit.size() && Files.isRegularFile(this.dir.resolve("audit.jsonl.broken"))
			);
			PlayerRecord fresh = mended.store.open(this.bob.id(), "Bob", this.time);
			this.check("and its player starts with a new record", fresh != null && fresh.homes.isEmpty() && fresh.name.equals("Bob"));
			this.check("a second copy is put beside the first, not over it", Files.isRegularFile(players.resolve(this.bob.id() + ".json.broken.2")));
		} finally {
			mended.saver.close();
		}
	}

	private void inTheWorld(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 origin = player.position();
		float yaw = player.getYRot();
		float pitch = player.getXRot();
		Session me = this.s.enter(player.getUUID(), player.getGameProfile().getName(), 0, null, player);
		Actor actor = me.actor();

		try {
			this.rules.warmupSeconds = 0;
			this.rules.cooldownSeconds = 0;
			Place start = me.place();
			Actor typed = Actor.of(player.createCommandSourceStack());
			this.check("a command a player types is theirs", typed.player() == player && typed.id().equals(player.getUUID()) && !typed.console());
			this.check("the console's is the console's", Actor.of(this.server.createCommandSourceStack()).console());
			Actor block = Actor.of(this.server.createCommandSourceStack().withPermission(2));
			this.check(
				"a command block's has what an operator of its level has, and stands above nobody",
				!block.console()
					&& block.level() == 2
					&& this.s.may(block, Nodes.MOD_MUTE)
					&& !this.s.may(block, Nodes.ADMIN_ROLES)
					&& this.s.perms.rank(block) != Integer.MAX_VALUE
					&& !this.s.outranks(block, this.t(this.dave))
			);
			Actor puppet = Actor.of(this.server.createCommandSourceStack().withPermission(2).withEntity(player));
			this.check(
				"one that only runs as a player is not that player's: nobody's roles, nobody's homes",
				!puppet.console()
					&& puppet.player() == null
					&& puppet.id().equals(Actor.NOBODY)
					&& puppet.level() == 2
					&& this.s.session(puppet) == null
					&& this.s.travel.homeGo(puppet, null, null).code().equals("player_only")
			);
			this.check("nor the console's, whatever its level", !Actor.of(this.server.createCommandSourceStack().withEntity(player)).console());
			this.expect("a real player sets a home", this.s.travel.homeSet(actor, "selftest", null, null), "ok", "selftest");
			player.teleportTo(level, origin.x + 3.0, origin.y + 12.0, origin.z + 3.0, yaw, pitch);
			Place away = me.place();
			this.expect("and goes back to it from somewhere else", this.s.travel.homeGo(actor, "selftest", null), "ok", "selftest");
			this.check("the game moved them there", player.position().distanceToSqr(origin) < 4.0 && same(me.record.back, away));
			this.check("looking the way they looked when the home was set", Math.abs(player.getYRot() - start.yaw) < 0.5F);
			LevelChunk chunk = level.getChunkSource().getChunkNow(player.blockPosition().getX() >> 4, player.blockPosition().getZ() >> 4);
			this.check("the chunk they stand in is in memory", chunk != null);
			if (chunk != null) {
				boolean carefree = player.isCreative() || player.isSpectator() || player.getAbilities().invulnerable || player.getAbilities().flying;
				Place high = new Place(start.dimension, origin.x, origin.y + 5.0, origin.z, 0.0F, 0.0F);
				Vec3 landing = this.s.teleports.landing(level, chunk, high, player);
				this.check(
					"a place in the air is landed below it (or kept, for somebody who cannot fall)",
					landing != null && (carefree ? Math.abs(landing.y - high.y) < 0.01 : landing.y <= high.y)
				);
				this.rules.safeLanding = false;
				Vec3 exact = this.s.teleports.landing(level, chunk, high, player);
				this.check("without the rule a place is taken as it is", exact != null && Math.abs(exact.y - high.y) < 0.01);
				this.rules.safeLanding = true;
				Place outside = new Place(start.dimension, 4.0E7, 64.0, 0.0, 0.0F, 0.0F);
				this.check("a place beyond the border is no landing", this.s.teleports.landing(level, chunk, outside, player) == null);
				Double surface = this.s.teleports.spot(level, chunk, player.blockPosition().getX(), player.blockPosition().getZ(), true);
				this.check(
					"the surface of their column is found from the chunk alone",
					surface == null || surface > (double)level.getMinBuildHeight() && surface < (double)level.getMaxBuildHeight()
				);
			}

			Role shown = new Role();
			shown.id = "shown";
			shown.name = "Shown";
			shown.prefix = "Test";
			shown.color = 5621992;
			shown.priority = 90;
			shown.numbers.put(Nodes.TP_WARMUP.id(), 2);
			shown.numbers.put(Nodes.TP_COOLDOWN.id(), 0);
			shown.numbers.put(Nodes.RTP_COOLDOWN.id(), 300);
			this.roles.roles.add(shown);
			this.roles.sanitize();
			Role.Grant given = new Role.Grant();
			given.role = "shown";
			me.record.roles.add(given);
			me.record.nick = "Tester";
			this.s.touch(me.record);
			this.s.permissionsChanged(me.id);
			player.teleportTo(level, origin.x, origin.y, origin.z, yaw, pitch);
			this.expect("a role's warm-up binds them all the same", this.s.travel.homeGo(actor, "selftest", null), "warmup", "2");
			this.check("the teleport waits", me.pending != null);
			this.time += 2000L;
			this.s.teleports.tick();
			this.check("and happens when its time has come", me.pending == null && player.position().distanceToSqr(origin) < 4.0);
			this.rules.randomDimensions.clear();
			this.rules.randomDimensions.add(start.dimension);
			this.expect("a random teleport waits like any other", this.s.travel.random(actor, null), "warmup", "2");
			this.s.teleports.tick();
			this.check("and asks nothing of the world while it does", me.pending != null && !me.pending.asked && !me.record.cooldowns.containsKey("rtp"));
			this.expect("given up before the search began", this.s.travel.cancel(actor), "ok");
			this.expect("it has cost nothing", this.s.travel.random(actor, null), "warmup", "2");
			this.time += 2000L;
			this.s.teleports.tick();
			this.check("once the search is under way its cool-down runs", me.pending != null && me.pending.asked && me.record.cooldowns.containsKey("rtp"));
			this.s.teleports.hurt(me);
			this.check("and a blow no longer ends it", me.pending != null);
			this.expect("giving it up now", this.s.travel.cancel(actor), "ok");
			this.expect("is not free: the next has to wait", this.s.travel.random(actor, null), "cooldown", "300");
			me.record.cooldowns.remove("rtp");
			this.expect("the trash is theirs to open", this.s.people.open(actor, "trash"), "ok");
			this.check("the world's record of who is sent their entity is reached", Vanish.tracked(player));
			this.check("the game's counters are read whole", this.s.statistics.live(player).getLong(PLAY) > 0L);
			ServerRuntime.standIn(
				this.s,
				() -> {
					String name = player.getDisplayName().getString();
					this.check(
						"the game's own name of the player carries the role's prefix and the nickname (" + name + ")", name.contains("[Test]") && name.contains("Tester")
					);
					Component tab = player.getTabListDisplayName();
					this.check("and so does their line of the tab list", tab != null && tab.getString().contains("[Test]") && tab.getString().contains("Tester"));
					me.afk = true;
					Component idle = player.getTabListDisplayName();
					this.check(
						"which is marked while they are idle",
						idle != null && !idle.getString().equals(tab == null ? "" : tab.getString()) && idle.getString().contains("Tester")
					);
					this.check("somebody idle does not have to sleep", !Hooks.sleepers(List.of(player)).contains(player));
					this.rules.afkSkipsSleep = false;
					this.check("unless the rules count them", Hooks.sleepers(List.of(player)).contains(player));
					this.rules.afkSkipsSleep = true;
					me.afk = false;
					this.rules.prefixInChat = false;
					this.rules.prefixInTabList = false;
					this.check(
						"the prefix can be switched off, the nickname stays",
						!player.getDisplayName().getString().contains("[Test]") && player.getDisplayName().getString().contains("Tester")
					);
					this.rules.prefixInChat = true;
					this.rules.prefixInTabList = true;
					this.check("a player who is free is not held", !Hooks.held(player) && !Hooks.commandRefused(player.createCommandSourceStack(), "home"));
					Punishment freeze = new Punishment();
					freeze.kind = Punishment.Kind.FREEZE;
					freeze.at = this.time;
					me.record.punishments.add(freeze);
					this.roles.get("shown").deny.add(Nodes.MOD_EXEMPT.id());
					this.s.permissionsChanged(me.id);
					this.check(
						"a frozen one is, for the game's own hooks",
						Hooks.held(player)
							&& Hooks.commandRefused(player.createCommandSourceStack(), "home")
							&& !Hooks.commandRefused(player.createCommandSourceStack(), "msg Someone hello")
					);
					this.check(
						"a command run in their name by the console is not theirs to be refused",
						!Hooks.commandRefused(this.server.createCommandSourceStack(), "home")
							&& !Hooks.commandRefused(this.server.createCommandSourceStack().withEntity(player), "home")
					);
					me.wasFrozen = true;
					me.frozenAt = me.place();
					player.teleportTo(level, origin.x + 2.0, origin.y, origin.z, Set.of(), yaw, pitch);
					this.check(
						"a frozen player the game's own /tp moved is held there, not taken back", me.frozenAt != null && Math.abs(me.frozenAt.x - (origin.x + 2.0)) < 0.01
					);
					me.wasFrozen = false;
					me.frozenAt = null;
					player.teleportTo(level, origin.x, origin.y, origin.z, yaw, pitch);
					me.record.punishments.remove(freeze);
					GameProfile stranger = new GameProfile(UUID.randomUUID(), "Stranger");
					this.check("the door is open", Hooks.loginRefusal(stranger) == null);
					this.rules.maintenance = true;
					this.check(
						"and closed during maintenance to whoever may not pass",
						Hooks.loginRefusal(stranger) != null && Hooks.loginRefusal(new GameProfile(this.carol.id(), "Carol")) == null
					);
					this.rules.maintenanceMessage = "Back at &enoon";
					this.check("with the server's own words", Hooks.loginRefusal(stranger).getString().equals("Back at noon"));
					this.rules.maintenance = false;
					this.rules.maintenanceMessage = "";
					me.record.vanished = true;
					this.s.touch(me.record);
					this.check(
						"a vanished player is left out of a list for nobody in particular",
						Hooks.shown(List.of(player), null).isEmpty() && Hooks.shown(List.of(player), player).size() == 1
					);
					me.record.vanished = false;
				}
			);
		} finally {
			this.s.leave(player.getUUID());
			player.teleportTo(level, origin.x, origin.y, origin.z, yaw, pitch);
		}
	}

	private void cleanUp() {
		try {
			this.s.saver.close();
		} catch (RuntimeException var5) {
		}

		if (Files.isDirectory(this.dir)) {
			try (Stream<Path> files = Files.walk(this.dir)) {
				for (Path p : files.sorted(Comparator.reverseOrder()).toList()) {
					Files.deleteIfExists(p);
				}
			} catch (IOException var7) {
				CFHGuiServer.LOGGER.warn("[CFHGui Server] selftest: could not remove its folder {}: {}", this.dir, var7.toString());
			}
		}
	}

	public static record Outcome(int passed, int failed) {
	}
}
