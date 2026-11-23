package com.onehumanawa.cfhcore.content.gui.server.client;

import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
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
import com.onehumanawa.cfhcore.content.gui.server.model.Warp;
import com.onehumanawa.cfhcore.content.gui.server.model.WarpList;
import com.onehumanawa.cfhcore.content.gui.server.net.ServerNet;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.GameRules.GameRuleTypeVisitor;
import net.minecraft.world.level.GameRules.Key;
import net.minecraft.world.level.GameRules.Type;
import net.minecraft.world.level.GameRules.Value;
import org.jetbrains.annotations.Nullable;

final class SampleServer {
	static final String FULL = "full";
	static final String EMPTY = "empty";
	static final String PLAYER = "player";
	private static final long SECOND = 1000L;
	private static final long MINUTE = 60000L;
	private static final long HOUR = 3600000L;
	private static final long DAY = 86400000L;
	private static final String OVERWORLD = "minecraft:overworld";
	private static final String NETHER = "minecraft:the_nether";
	private static final String END = "minecraft:the_end";
	private static final int KNOWN = 2000;
	private static final String[] FIRST = new String[]{
		"Al",
		"Bri",
		"Cor",
		"Dan",
		"El",
		"Fen",
		"Gar",
		"Hal",
		"Ira",
		"Jun",
		"Kor",
		"Lin",
		"Mar",
		"Nor",
		"Ori",
		"Per",
		"Quin",
		"Ros",
		"Syl",
		"Tor",
		"Una",
		"Val",
		"Wen",
		"Xan",
		"Yor",
		"Zel"
	};
	private static final String[] LAST = new String[]{"a", "an", "ric", "da", "en", "is", "o", "us", "wyn", "ix", "ara", "el", "ith", "or"};
	private static final String[] ON = new String[]{"Mira", "Tobias", "Juno", "Aldric", "Wren", "Sable", "Oskar", "Petra", "Lior", "Hana", "Bram"};
	private static final int[] RANK_COLORS = new int[]{14722136, 8106858, 6007008, 15034762, 10192608, 6080680};
	private static final String[] REASONS = new String[]{
		"Spamming the chat", "Griefing at the market", "Rude to other players", "Advertising", "X-ray", "Ignoring a warning", "Kill aura"
	};
	private static RoleBook roles = RoleBook.defaults();
	private static List<Kit> kits = new ArrayList<>();
	private static ServerRules rules = new ServerRules();
	private static AdminData.WorldInfo world = new AdminData.WorldInfo();
	private static final List<AdminData.PlayerRow> directory = new ArrayList<>();
	private static final Map<UUID, AdminData.PlayerDetail> details = new HashMap<>();
	private static final List<AdminData.PunishmentRow> punishments = new ArrayList<>();
	private static final List<AdminData.AuditEntry> audit = new ArrayList<>();
	private static boolean watching;
	@Nullable
	private static AdminData.Dashboard vitalSigns;
	private static int ticks;
	private static Place pendingPlace;

	private static UUID id(String name) {
		return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
	}

	private static String selfName() {
		return Minecraft.getInstance().getUser().getName();
	}

	static void fill(String variant) {
		long now = System.currentTimeMillis();
		boolean empty = "empty".equals(variant);
		boolean staff = "full".equals(variant);
		roles = RoleBook.defaults();
		extraRoles();
		Role admin = roles.get("admin");
		Role moderator = roles.get("moderator");
		Role vip = roles.get("vip");
		Role builder = roles.get("builder");
		Hello hello = new Hello();
		hello.features = new ServerRules().features();
		if (staff) {
			hello.features.add("thirst");
		}

		for (Node n : Perms.declared()) {
			if (n.module().equals("cfhgui_server")) {
				if (n.number()) {
					hello.numbers.put(n.id(), n.defaultNumber());
				} else if (staff || n.defaultLevel() == 0) {
					hello.may.add(n.id());
				}
			}
		}

		hello.numbers.put(Nodes.HOMES_MAX.id(), staff ? 5 : 3);
		if (staff && admin != null) {
			hello.roles.add(new Role.Tag(admin));
		} else if (!empty && vip != null) {
			hello.roles.add(new Role.Tag(vip));
		}

		rules = new ServerRules();
		rules.rules = new ArrayList<>(
			List.of(
				"Be kind to each other.",
				"No griefing: claim your land with a team.",
				"Ask before you take from a chest that is not yours.",
				"Keep the spawn tidy: build your own things at least a hundred blocks away from it."
			)
		);
		rules.links.add(link("Live map", "https://example.org/map"));
		rules.links.add(link("Discord", "https://example.org/discord"));
		rules.links.add(link("The pack", "https://example.org/pack"));
		rules.joinMessage = new ArrayList<>(List.of("Welcome back, %player%! %online% are on.", "Type /menu to see what this server offers."));
		rules.announcements = new ArrayList<>(List.of("Vote for the next build contest in the market hall.", "The end resets on the first of the month."));
		rules.maintenanceMessage = "Back in an hour: we are moving to a faster machine.";
		hello.rules = empty ? new ArrayList<>() : new ArrayList<>(rules.rules);
		if (!empty) {
			hello.links.addAll(rules.links);
		}

		hello.featuredRankings = new ArrayList<>(rules.featuredRankings);
		SelfState self = new SelfState();
		if (!empty) {
			self.homes.add(home("home", "minecraft:overworld", 128.5, 71.0, -342.5, 14254174, "HOME", now - 1814400000L));
			self.homes.add(home("mine", "minecraft:overworld", -804.5, 12.0, 1190.5, 10134964, "minecraft:iron_pickaxe", now - 777600000L));
			self.homes.add(home("fortress", "minecraft:the_nether", 44.5, 68.0, -120.5, 14707787, "minecraft:blaze_rod", now - 172800000L));
			self.death = new Place("minecraft:overworld", -211.5, 34.0, 902.5, 0.0F, 0.0F);
			self.deathAt = now - 840000L;
			self.back = self.death.copy();
			self.backIsDeath = true;
			self.unreadMail = 1;
			self.cooldowns.put("rtp", now + 240000L + 20000L);
		}

		kits = new ArrayList<>();
		kits.add(
			kit(
				"starter",
				"Starter",
				"minecraft:bread",
				0,
				false,
				true,
				false,
				"minecraft:stone_sword:1",
				"minecraft:stone_pickaxe:1",
				"minecraft:stone_axe:1",
				"minecraft:bread:16",
				"minecraft:torch:32",
				"minecraft:oak_log:16",
				"minecraft:white_bed:1"
			)
		);
		kits.add(
			kit(
				"daily",
				"Daily ration",
				"minecraft:cooked_beef",
				86400,
				false,
				false,
				false,
				"minecraft:cooked_beef:8",
				"minecraft:golden_carrot:4",
				"minecraft:experience_bottle:6",
				"minecraft:firework_rocket:16"
			)
		);
		kits.add(
			kit(
				"builder",
				"Builder",
				"minecraft:scaffolding",
				3600,
				false,
				false,
				true,
				"minecraft:scaffolding:64",
				"minecraft:stone_bricks:64",
				"minecraft:glass:64",
				"minecraft:oak_planks:64",
				"minecraft:lantern:16",
				"minecraft:water_bucket:1"
			)
		);
		kits.add(kit("welcome", "Welcome gift", "minecraft:diamond", 0, true, false, false, "minecraft:diamond:3", "minecraft:elytra:1", "minecraft:name_tag:1"));
		if (!empty) {
			self.kits.add(view(kits.get(0), 0L));
			self.kits.add(view(kits.get(1), now + 7200000L + 300000L));
			if (staff) {
				self.kits.add(view(kits.get(2), 0L));
			}

			self.kits.add(view(kits.get(3), -1L));
		}

		WarpList warps = new WarpList();
		warps.spawn = new Place("minecraft:overworld", 0.5, 68.0, 0.5, 0.0F, 0.0F);
		if (!empty) {
			warps.warps
				.add(warp("market", "minecraft:overworld", 64.5, 70.0, 18.5, 14722136, "minecraft:emerald", "Stalls and the trading hall.", false, now - 3456000000L, 312L));
			warps.warps
				.add(
					warp(
						"end_portal",
						"minecraft:overworld",
						-1420.5,
						28.0,
						760.5,
						10192608,
						"minecraft:ender_eye",
						"The stronghold, lit and safe.",
						false,
						now - 2851200000L,
						97L
					)
				);
			warps.warps.add(warp("arena", "minecraft:overworld", 510.5, 82.0, -260.5, 14707787, "SWORD", "Friday evenings.", false, now - 1036800000L, 41L));
			warps.warps
				.add(
					warp(
						"farms",
						"minecraft:overworld",
						-96.5,
						64.0,
						244.5,
						8106858,
						"minecraft:wheat",
						"Public crops, iron and wool. Replant what you take.",
						false,
						now - 2160000000L,
						208L
					)
				);
			warps.warps
				.add(
					warp("nether_hub", "minecraft:the_nether", 8.5, 110.0, -3.5, 15034762, "minecraft:obsidian", "Ice roads to every base.", false, now - 1641600000L, 153L)
				);
			if (staff) {
				warps.warps.add(warp("staff_room", "minecraft:overworld", 12.5, 96.0, -6.5, 6007008, "SHIELD", "", true, now - 2592000000L, 18L));
			}
		}

		List<OnlinePlayer> online = new ArrayList<>();
		online.add(online(ServerState.selfId(), selfName(), "", staff ? admin : (empty ? null : vip), false, now - 3000000L));
		if (!empty) {
			online.add(online(id("Mira"), "Mira", "", moderator, false, now - 10800000L));
			online.add(online(id("Tobias"), "Tobias", "Toby", builder, true, now - 5700000L));
			online.add(online(id("Juno"), "Juno", "", null, false, now - 1200000L));
			online.add(online(id("Aldric"), "Aldric", "&6Gold&ebeard", vip, false, now - 360000L));
			online.add(online(id("Wren"), "Wren", "", null, false, now - 14400000L));
			online.add(online(id("Sable"), "Sable", "Night_Owl", null, true, now - 25200000L));
			online.add(online(id("Oskar"), "Oskar", "", builder, false, now - 1980000L));
			OnlinePlayer petra = online(id("Petra"), "Petra", "", null, false, now - 7200000L);
			petra.acceptsRequests = false;
			online.add(petra);
		}

		List<TeleportRequest> requests = new ArrayList<>();
		if (!empty) {
			requests.add(request("Juno", true, false, now + 45000L));
			requests.add(request("Oskar", false, true, now + 28000L));
		}

		List<PlayerRecord.Mail> mail = new ArrayList<>();
		if (!empty) {
			mail.add(
				letter(
					"1",
					"Mira",
					"The market stall next to yours is free again, if you still want it. I can hold it until Sunday, after that it goes to whoever asks first.",
					now - 7200000L,
					false
				)
			);
			mail.add(letter("2", "Aldric", "Thanks for the diamonds. I left the pickaxe in your barrel.", now - 259200000L, true));
			mail.add(letter("3", "Wren", "Are you coming to the arena on Friday? We are one short for the second team.", now - 432000000L, true));
		}

		AdminData.Dashboard dashboard = new AdminData.Dashboard();
		dashboard.msptHistory = new float[60];

		for (int i = 0; i < 60; i++) {
			dashboard.msptHistory[i] = sampleTick(i);
		}

		dashboard.players = online.size();
		dashboard.maxPlayers = 20;
		dashboard.memoryUsed = 3180L;
		dashboard.memoryTotal = 4096L;
		dashboard.memoryMax = 6144L;
		dashboard.uptimeMs = 277200000L;
		dashboard.dimensions.add(dimension("minecraft:overworld", Math.max(1, online.size() - 2), 1840, 961, 412));
		dashboard.dimensions.add(dimension("minecraft:the_nether", online.size() > 2 ? 2 : 0, 310, 225, 37));
		dashboard.dimensions.add(dimension("minecraft:the_end", 0, 12, 49, 3));
		vitals(dashboard);
		makeDirectory(now, online);
		makeHistory(now);
		makeWorld();
		details.clear();
		watching = false;
		vitalSigns = dashboard;
		ServerState.installPreview(hello, self, warps, requests, online, mail, dashboard);
	}

	private static void extraRoles() {
		Role vip = new Role();
		vip.id = "vip";
		vip.name = "VIP";
		vip.color = 14722136;
		vip.prefix = "VIP";
		vip.priority = 10;
		vip.inherits.add("default");
		vip.allow.add("cfhgui_server.nick.color");
		vip.allow.add("cfhgui_server.enderchest.use");
		vip.numbers.put("cfhgui_server.homes.max", 6);
		vip.numbers.put("cfhgui_server.rtp.cooldown", 120);
		roles.roles.add(vip);
		Role builder = new Role();
		builder.id = "builder";
		builder.name = "Builder";
		builder.color = 8106858;
		builder.prefix = "Build";
		builder.priority = 20;
		builder.inherits.add("vip");
		builder.allow.add("cfhgui_server.warp.set");
		builder.allow.add("cfhgui_server.workbench.use");
		builder.allow.add("cfhgui_server.kit.use.builder");
		builder.deny.add("cfhgui_server.tp.anywhere");
		roles.roles.add(builder);
		Role helper = new Role();
		helper.id = "helper";
		helper.name = "Helper";
		helper.color = 6080680;
		helper.prefix = "Help";
		helper.priority = 30;
		helper.inherits.add("vip");
		helper.allow.add("cfhgui_server.mod.warn");
		helper.allow.add("cfhgui_server.mod.mute");
		helper.allow.add("cfhgui_server.mod.history");
		helper.allow.add("cfhgui_server.mod.staffchat");
		helper.allow.add("cfhgui_server.admin.menu");
		helper.allow.add("cfhgui_server.admin.players");
		roles.roles.add(helper);
		int more = Math.max(0, Math.min(18, Integer.getInteger("cfh.server.sampleRoles", 0)));

		for (int i = 1; i <= more; i++) {
			Role rank = new Role();
			rank.id = "rank" + i;
			rank.name = "Rank " + i;
			rank.color = RANK_COLORS[i % RANK_COLORS.length];
			rank.priority = 30 + i;
			rank.inherits.add("vip");
			roles.roles.add(rank);
		}

		roles.sanitize();
	}

	private static float sampleTick(int second) {
		return (float)(
			17.0
				+ 4.0 * Math.sin((double)second / 5.0)
				+ 1.5 * Math.sin((double)second / 1.7)
				+ (double)(second % 17 == 0 ? 24 : 0)
				+ (double)(second % 41 == 0 ? 38 : 0)
		);
	}

	private static void vitals(AdminData.Dashboard d) {
		float sum = 0.0F;
		float worst = 0.0F;

		for (float v : d.msptHistory) {
			sum += v;
			worst = Math.max(worst, v);
		}

		float last = d.msptHistory.length == 0 ? 0.0F : d.msptHistory[d.msptHistory.length - 1];
		d.mspt = (double)last;
		d.msptWorst = (double)worst;
		d.tps = Math.min(20.0, 1000.0 / Math.max(50.0, (double)last));
		d.tpsMinute = Math.min(20.0, 1000.0 / Math.max(50.0, (double)(sum / (float)Math.max(1, d.msptHistory.length))));
	}

	private static void makeDirectory(long now, List<OnlinePlayer> online) {
		directory.clear();

		for (OnlinePlayer p : online) {
			AdminData.PlayerRow r = new AdminData.PlayerRow();
			r.id = p.id;
			r.name = p.name;
			r.nick = p.nick;
			r.role = p.role;
			r.online = true;
			r.afk = p.afk;
			r.lastSeen = p.joinedAt;
			r.dimension = directory.size() % 4 == 2 ? "minecraft:the_nether" : "minecraft:overworld";
			directory.add(r);
		}

		Role vip = roles.get("vip");

		for (int i = 0; directory.size() < 2000; i++) {
			String name = FIRST[i % FIRST.length]
				+ LAST[i / FIRST.length % LAST.length]
				+ (i >= FIRST.length * LAST.length ? Integer.toString(i / (FIRST.length * LAST.length) * 7 + i % 7) : "");
			AdminData.PlayerRow r = new AdminData.PlayerRow();
			r.id = id(name);
			r.name = name;
			r.lastSeen = now - (long)(i + 1) * 37L * 60000L;
			r.muted = i % 37 == 5;
			r.banned = i % 53 == 9;
			r.frozen = false;
			if (i % 11 == 3 && vip != null) {
				r.role = new Role.Tag(vip);
			}

			directory.add(r);
		}
	}

	private static void makeHistory(long now) {
		punishments.clear();
		audit.clear();
		Punishment.Kind[] kinds = new Punishment.Kind[]{
			Punishment.Kind.MUTE, Punishment.Kind.WARN, Punishment.Kind.BAN, Punishment.Kind.KICK, Punishment.Kind.MUTE, Punishment.Kind.WARN, Punishment.Kind.FREEZE
		};
		String[] staff = new String[]{"Mira", selfName(), "Mira", "Hana"};

		for (int i = 0; i < 137; i++) {
			AdminData.PlayerRow who = directory.get(Math.min(directory.size() - 1, 12 + i * 29 % 400));
			AdminData.PunishmentRow row = new AdminData.PunishmentRow();
			row.player = who.id;
			row.playerName = who.name;
			Punishment p = row.punishment;
			p.id = Integer.toString(i);
			p.kind = kinds[i % kinds.length];
			p.reason = REASONS[i % REASONS.length];
			p.byName = staff[i % staff.length];
			p.by = id(p.byName);
			p.at = now - (long)i * 5L * 3600000L - 1200000L;
			if (p.kind.lasts()) {
				p.expiresAt = i % 9 == 2 ? 0L : p.at + (i % 4 == 0 ? 604800000L : (i % 4 == 1 ? 7200000L : (i % 4 == 2 ? 86400000L : 1800000L)));
				if (i % 5 == 3) {
					p.liftedAt = p.at + 2400000L;
					p.liftedByName = selfName();
				}
			}

			punishments.add(row);
		}

		String[] actions = new String[]{
			"punish_mute",
			"role_grant",
			"warp_set",
			"player_goto",
			"rule_set",
			"punish_warn",
			"kit_give",
			"pardon_mute",
			"player_gamemode",
			"broadcast",
			"player_invsee",
			"punish_ban",
			"role_save",
			"world_set",
			"note_add",
			"player_bring",
			"home_go",
			"kit_save",
			"spawn_set",
			"maintenance"
		};
		String[] detail = new String[]{
			"10m: Spamming the chat",
			"VIP",
			"market",
			"",
			"afkSeconds",
			"Rude to other players",
			"Starter",
			"",
			"creative",
			"Restart in five minutes.",
			"",
			"7d: X-ray",
			"Builder",
			"time day",
			"",
			"",
			"base",
			"Daily ration",
			"0, 68, 0",
			"on"
		};

		for (int i = 0; i < 312; i++) {
			AdminData.AuditEntry e = new AdminData.AuditEntry();
			e.at = now - (long)i * 47L * 60000L - 180000L;
			e.actorName = staff[i * 3 % staff.length];
			e.actor = id(e.actorName);
			e.action = actions[i % actions.length];
			AdminData.PlayerRow who = directory.get(Math.min(directory.size() - 1, 1 + i * 13 % 300));
			e.target = who.id;
			e.targetName = who.name;
			e.detail = detail[i % detail.length];
			audit.add(e);
		}
	}

	private static void makeWorld() {
		world = new AdminData.WorldInfo();
		world.difficulty = "normal";
		world.dimensions.add(time("minecraft:overworld", 6200L, false, false));
		world.dimensions.add(time("minecraft:the_nether", 6200L, false, false));
		world.dimensions.add(time("minecraft:the_end", 6200L, false, false));

		try {
			final GameRules game = new GameRules();
			GameRules.visitGameRuleTypes(new GameRuleTypeVisitor() {
				public <T extends Value<T>> void visit(Key<T> key, Type<T> type) {
					AdminData.GameRule r = new AdminData.GameRule();
					T value = (T)game.getRule(key);
					r.id = key.getId();
					r.nameKey = key.getDescriptionId();
					r.categoryKey = key.getCategory().getDescriptionId();
					r.bool = value instanceof BooleanValue;
					r.value = value.serialize();
					SampleServer.world.gameRules.add(r);
				}
			});
		} catch (RuntimeException var1) {
			CFHGuiServer.LOGGER.debug("[CFHGui Server] the sample world has no game rules: {}", var1.toString());
		}
	}

	static void tick() {
		if (ServerState.preview()) {
			long now = System.currentTimeMillis();
			SelfState self = ServerState.self();
			if (self.pending != null && now >= self.pendingAt) {
				if (pendingPlace != null) {
					self.back = pendingPlace;
					self.backIsDeath = false;
				}

				self.pending = null;
				self.cooldowns.put("tp", now + 5000L);
				ServerState.previewPut("self", self);
			}

			if (++ticks % 20 == 0) {
				List<TeleportRequest> open = new ArrayList<>(ServerState.requests());
				if (open.removeIf(r -> r.expiresAt <= now)) {
					ServerState.previewPut("requests", open);
				}

				AdminData.Dashboard d = ServerState.dashboard();
				if (watching && d != null) {
					float[] h = new float[60];
					System.arraycopy(d.msptHistory, Math.max(0, d.msptHistory.length - 59), h, 0, Math.min(59, d.msptHistory.length));
					h[59] = sampleTick(ticks / 20 + 60);
					d.msptHistory = h;
					d.uptimeMs += 1000L;
					d.memoryUsed = (long)(3000 + ticks / 20 * 37 % 900);
					d.dimensions.get(0).entities = 1840 + ticks / 20 * 13 % 60;
					vitals(d);
					ServerState.previewPut("dashboard", d);
				}
			}
		}
	}

	static void scene(String scene) {
		long now = System.currentTimeMillis();
		SelfState self = ServerState.self();
		switch (scene) {
			case "request":
				List<TeleportRequest> open = new ArrayList<>(ServerState.requests());
				open.removeIf(r -> r.name.equals("Wren"));
				open.add(request("Wren", true, false, now + 60000L));
				ServerState.previewPut("requests", open);
				break;
			case "warmup":
				travel(0, "home:mine");
				break;
			case "status":
				self.afk = true;
				self.vanished = true;
				self.frozen = true;
				ServerState.previewPut("self", self);
				break;
			case "muted":
				self.mutedUntil = now + 7200000L - 120000L;
				ServerState.previewPut("self", self);
				ServerState.previewNotice("muted", 0, "Spamming the chat");
				break;
			case "notices":
				ServerState.previewNotice("mail_received", 0, "Mira");
				ServerState.previewNotice("role_granted", 8106858, "Builder");
				ServerState.previewNotice("warned", 0, "Keep the spawn tidy, please.", "Mira");
		}
	}

	@Nullable
	static <T> T answer(String op, CompoundTag a, Class<T> type) {
		Object out = switch (op) {
			case "players" -> {
				List<PlayerRef> refs = new ArrayList<>();

				for (int i = 0; i < Math.min(60, directory.size()); i++) {
					refs.add(new PlayerRef(directory.get(i).id, directory.get(i).name, directory.get(i).online));
				}

				yield refs.toArray(new PlayerRef[0]);
			}
			case "stats" -> stats(a.hasUUID("id") ? a.getUUID("id") : ServerState.selfId());
			case "board" -> board(a.getString("key"), a.getInt("offset"), Math.max(1, Math.min(100, a.getInt("count"))));
			case "card" -> card(a.hasUUID("id") ? a.getUUID("id") : ServerState.selfId());
			case "admin_players" -> players(a.getString("query"), a.getString("filter"), a.getInt("page"));
			case "admin_player" -> a.hasUUID("id") ? detail(a.getUUID("id")) : null;
			case "admin_roles" -> roleSet();
			case "admin_kits" -> (Kit[])kits.toArray(new Kit[0]);
			case "admin_punishments" -> punishmentPage(a.getString("filter"), Punishment.Kind.parse(a.contains("kind") ? a.getString("kind") : null), a.getInt("page"));
			case "admin_audit" -> auditPage(a.getString("query"), a.getInt("page"));
			case "admin_rules" -> rules;
			case "admin_world" -> world;
			default -> null;
		};
		return (T)(out == null ? null : JsonWire.decode(JsonWire.encode(out), type));
	}

	private static long stat(UUID who, String key) {
		long seed = (who.getLeastSignificantBits() ^ (long)key.hashCode() * -7046029254386353131L) & Long.MAX_VALUE;
		if (key.endsWith("play_time") || key.equals("afk_ticks")) {
			return seed % (key.contains("afk") ? 600000L : 9000000L);
		} else if (key.endsWith("_one_cm")) {
			return seed % 80000000L;
		} else if (key.equals("sessions")) {
			return 1L + seed % 400L;
		} else {
			return key.endsWith("_seen_ms") ? System.currentTimeMillis() - seed % 17280000000L : seed % 4000L;
		}
	}

	private static Snapshot stats(UUID who) {
		Map<String, Long> stats = new LinkedHashMap<>();
		Map<String, Long> extras = new LinkedHashMap<>();

		for (String key : rules.featuredRankings) {
			stats.put(key, stat(who, key));
		}

		for (String key : List.of("afk_ticks", "sessions", "first_seen_ms", "last_seen_ms")) {
			extras.put(key, stat(who, key));
		}

		String name = selfName();

		for (AdminData.PlayerRow r : directory) {
			if (r.id.equals(who)) {
				name = r.name;
			}
		}

		return new Snapshot(who, name, stats, extras, System.currentTimeMillis());
	}

	private static Board board(String key, int offset, int count) {
		int ranked = 412;
		List<AdminData.PlayerRow> all = new ArrayList<>(directory.subList(0, Math.min(directory.size(), 412)));
		all.sort(Comparator.<AdminData.PlayerRow>comparingLong(rx -> stat(rx.id, key)).reversed());
		List<Row> rows = new ArrayList<>();
		Row self = null;

		for (int i = 0; i < all.size(); i++) {
			AdminData.PlayerRow r = all.get(i);
			Row row = new Row(r.id, r.name, stat(r.id, key), i + 1);
			if (i >= offset && i < offset + count) {
				rows.add(row);
			}

			if (r.id.equals(ServerState.selfId())) {
				self = row;
			}
		}

		return new Board(key, rows, all.size(), self);
	}

	private static PlayerCard card(UUID who) {
		PlayerCard c = new PlayerCard();
		c.id = who;

		for (AdminData.PlayerRow r : directory) {
			if (r.id.equals(who)) {
				c.name = r.name;
				c.nick = r.nick;
				c.online = r.online;
				c.afk = r.afk;
				c.lastSeen = r.lastSeen;
				if (r.role != null) {
					c.roles.add(r.role);
				}
			}
		}

		c.firstSeen = stat(who, "first_seen_ms");
		c.sessions = stat(who, "sessions");
		c.playTicks = stat(who, "minecraft:custom|minecraft:play_time");
		c.afkTicks = stat(who, "afk_ticks");
		return c;
	}

	private static AdminData.PlayerPage players(String query, String filter, int page) {
		String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		List<AdminData.PlayerRow> match = new ArrayList<>();

		for (AdminData.PlayerRow r : directory) {
			if (q.isEmpty() || r.name.toLowerCase(Locale.ROOT).contains(q) || r.nick.toLowerCase(Locale.ROOT).contains(q)) {
				String var8 = filter == null ? "" : filter;

				boolean keep = switch (var8) {
					case "online" -> r.online;
					case "offline" -> !r.online;
					case "muted" -> r.muted;
					case "banned" -> r.banned;
					case "frozen" -> r.frozen;
					default -> true;
				};
				if (keep) {
					match.add(r);
				}
			}
		}

		AdminData.PlayerPage out = new AdminData.PlayerPage();
		out.total = match.size();
		out.page = Math.max(0, Math.min(page, Math.max(0, (match.size() - 1) / 50)));
		out.rows = new ArrayList<>(match.subList(Math.min(match.size(), out.page * 50), Math.min(match.size(), (out.page + 1) * 50)));
		return out;
	}

	@Nullable
	private static AdminData.PlayerRow row(UUID who) {
		for (AdminData.PlayerRow r : directory) {
			if (r.id.equals(who)) {
				return r;
			}
		}

		return null;
	}

	@Nullable
	private static AdminData.PlayerDetail detail(UUID who) {
		AdminData.PlayerRow row = row(who);
		return row == null ? null : details.computeIfAbsent(who, id -> {
			long now = System.currentTimeMillis();
			AdminData.PlayerDetail d = new AdminData.PlayerDetail();
			d.row = row;
			d.firstSeen = stat(id, "first_seen_ms");
			d.sessions = stat(id, "sessions");
			d.playTicks = stat(id, "minecraft:custom|minecraft:play_time");
			d.afkTicks = stat(id, "afk_ticks");
			d.place = new Place(row.dimension.isEmpty() ? "minecraft:overworld" : row.dimension, 340.5, 67.0, -118.5, 0.0F, 0.0F);
			d.death = new Place("minecraft:overworld", -41.5, 22.0, 310.5, 0.0F, 0.0F);
			d.homes.add(home("home", "minecraft:overworld", 300.5, 66.0, -90.5, 6007008, "minecraft:red_bed", now - 2592000000L));
			d.homes.add(home("farm", "minecraft:overworld", 1210.5, 70.0, 44.5, 8106858, "minecraft:wheat", now - 950400000L));
			if (row.role != null) {
				Role.Grant g = new Role.Grant();
				g.role = row.role.id;
				g.byName = "Mira";
				g.at = now - 1728000000L;
				g.expiresAt = row.role.id.equals("vip") ? now + 777600000L : 0L;
				d.roles.add(g);
			}

			for (AdminData.PunishmentRow p : punishments) {
				if (p.player.equals(id)) {
					d.punishments.add(0, p.punishment);
				}
			}

			if (d.punishments.isEmpty() && !id.equals(ServerState.selfId())) {
				d.punishments.add(punishment("w1", Punishment.Kind.WARN, "Rude to other players", "Mira", now - 1036800000L, 0L, 0L));
				d.punishments.add(punishment("m1", Punishment.Kind.MUTE, "Spamming the chat", "Mira", now - 259200000L, now - 259200000L + 1800000L, 0L));
			}

			PlayerRecord.Note note = new PlayerRecord.Note();
			note.byName = "Mira";
			note.by = id("Mira");
			note.text = "Asked twice about the stall rules; friendly, just new. No action needed.";
			note.at = now - 518400000L;
			d.notes.add(note);
			d.health = 17.5F;
			d.maxHealth = 20.0F;
			d.food = 14;
			d.level = 31;
			d.ping = 42;
			d.gameMode = "survival";
			d.mayActOn = !id.equals(ServerState.selfId());
			return d;
		});
	}

	private static AdminData.RoleSet roleSet() {
		AdminData.RoleSet set = new AdminData.RoleSet();
		roles.sanitize();
		set.roles = roles.roles;

		for (Role role : roles.roles) {
			short var10001;
			if (role.everyone) {
				var10001 = 2000;
			} else {
				String info = role.id;
				switch (info) {
					case "admin":
						var10001 = 2;
						break;
					case "moderator":
						var10001 = 3;
						break;
					case "helper":
						var10001 = 6;
						break;
					case "builder":
						var10001 = 14;
						break;
					case "vip":
						var10001 = 87;
						break;
					default:
						var10001 = 0;
				}
			}

			set.holders.add(Integer.valueOf(var10001));
		}

		for (Node n : Perms.declared()) {
			AdminData.NodeInfo info = new AdminData.NodeInfo();
			info.id = n.id();
			info.level = n.defaultLevel();
			info.number = n.number();
			info.defaultNumber = n.defaultNumber();
			set.nodes.add(info);
		}

		for (String named : List.of("cfhgui_server.warp.to.staff_room", "cfhgui_server.kit.use.builder")) {
			AdminData.NodeInfo info = new AdminData.NodeInfo();
			info.id = named;
			info.level = 2;
			set.nodes.add(info);
		}

		return set;
	}

	private static AdminData.PunishmentPage punishmentPage(String filter, @Nullable Punishment.Kind kind, int page) {
		long now = System.currentTimeMillis();
		List<AdminData.PunishmentRow> match = new ArrayList<>();

		for (AdminData.PunishmentRow r : punishments) {
			if ((kind == null || r.punishment.kind == kind) && (!"active".equals(filter) || r.punishment.active(now))) {
				match.add(r);
			}
		}

		AdminData.PunishmentPage out = new AdminData.PunishmentPage();
		out.total = match.size();
		out.page = Math.max(0, Math.min(page, Math.max(0, (match.size() - 1) / 50)));
		out.rows = new ArrayList<>(match.subList(Math.min(match.size(), out.page * 50), Math.min(match.size(), (out.page + 1) * 50)));
		return out;
	}

	private static AdminData.AuditPage auditPage(String query, int page) {
		String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		List<AdminData.AuditEntry> match = new ArrayList<>();

		for (AdminData.AuditEntry e : audit) {
			if (q.isEmpty()
				|| e.actorName.toLowerCase(Locale.ROOT).contains(q)
				|| e.targetName.toLowerCase(Locale.ROOT).contains(q)
				|| e.action.contains(q)
				|| e.detail.toLowerCase(Locale.ROOT).contains(q)) {
				match.add(e);
			}
		}

		AdminData.AuditPage out = new AdminData.AuditPage();
		out.total = match.size();
		out.page = Math.max(0, Math.min(page, Math.max(0, (match.size() - 1) / 50)));
		out.rows = new ArrayList<>(match.subList(Math.min(match.size(), out.page * 50), Math.min(match.size(), (out.page + 1) * 50)));
		return out;
	}

	static ServerNet.Result act(int request, String op, CompoundTag a) {
		try {
			return play(request, op, a);
		} catch (RuntimeException var4) {
			CFHGuiServer.LOGGER.warn("[CFHGui Server] the sample server could not play {}: {}", op, var4.toString());
			return ServerNet.Result.fail(request, "invalid");
		}
	}

	private static ServerNet.Result ok(int request) {
		return new ServerNet.Result(request, true, "ok", List.of());
	}

	private static ServerNet.Result play(int request, String op, CompoundTag a) {
		long now = System.currentTimeMillis();
		SelfState self = ServerState.self();
		String name = a.getString("name");
		switch (op) {
			case "home_set":
				String n = name.isEmpty() ? "home" : name;
				Home home = null;

				for (Home h : self.homes) {
					if (h.name.equalsIgnoreCase(n)) {
						home = h;
					}
				}

				if (home == null) {
					int max = ServerState.number(Nodes.HOMES_MAX);
					if (self.homes.size() >= max) {
						return ServerNet.Result.fail(request, "home_limit", Integer.toString(max));
					}

					home = home(n, here().dimension, here().x, here().y, here().z, 14254174, "HOME", now);
					self.homes.add(home);
				} else {
					home.place = here();
				}

				if (a.contains("icon")) {
					home.icon = a.getString("icon");
				}

				if (a.contains("color")) {
					home.color = a.getInt("color");
				}

				ServerState.previewPut("self", self);
				break;
			case "home_edit":
				for (Home hx : self.homes) {
					if (hx.name.equalsIgnoreCase(name)) {
						String renamex = a.getString("rename");
						if (!renamex.isEmpty()) {
							for (Home otherx : self.homes) {
								if (otherx != hx && otherx.name.equalsIgnoreCase(renamex)) {
									return ServerNet.Result.fail(request, "name_taken");
								}
							}

							hx.name = renamex;
						}

						if (a.contains("icon")) {
							hx.icon = a.getString("icon");
						}

						if (a.contains("color")) {
							hx.color = a.getInt("color");
						}
					}
				}

				ServerState.previewPut("self", self);
				break;
			case "home_delete":
				if (a.hasUUID("owner")) {
					AdminData.PlayerDetail dx = details.get(a.getUUID("owner"));
					if (dx != null) {
						dx.homes.removeIf(h -> h.name.equalsIgnoreCase(name));
					}
				} else {
					self.homes.removeIf(h -> h.name.equalsIgnoreCase(name));
					ServerState.previewPut("self", self);
				}
				break;
			case "home_go":
				if (a.hasUUID("owner")) {
					return ok(request);
				}

				Home target = self.homes.stream().filter(h -> h.name.equalsIgnoreCase(name.isEmpty() ? "home" : name)).findFirst().orElse(null);
				if (target == null) {
					return ServerNet.Result.fail(request, "no_such_home", name);
				}

				return travel(request, "home:" + target.name);
			case "warp_go":
				return travel(request, "warp:" + name);
			case "spawn_go":
				return travel(request, "spawn");
			case "back":
				return self.back == null ? ServerNet.Result.fail(request, "no_back") : travel(request, "back");
			case "rtp":
				if (self.cooldown("rtp") > now) {
					return ServerNet.Result.fail(request, "cooldown", Long.toString((self.cooldown("rtp") - now) / 1000L));
				}

				self.cooldowns.put("rtp", now + 300000L);
				return travel(request, "rtp");
			case "tp_position":
				return travel(request, "position");
			case "tp_cancel":
				if (self.pending == null) {
					return ServerNet.Result.fail(request, "nothing_pending");
				}

				self.pending = null;
				ServerState.previewPut("self", self);
				break;
			case "spawn_set": {
				WarpList warps = ServerState.warps();
				warps.spawn = here();
				ServerState.previewPut("warps", warps);
				break;
			}
			case "warp_set":
			case "warp_edit": {
				WarpList warps = ServerState.warps();
				Warp warp = null;

				for (Warp w : warps.warps) {
					if (w.name.equalsIgnoreCase(name)) {
						warp = w;
					}
				}

				if (warp == null) {
					if (op.equals("warp_edit")) {
						return ServerNet.Result.fail(request, "no_such_warp", name);
					}

					warp = warp(name, here().dimension, here().x, here().y, here().z, 7317724, "PIN", "", false, now, 0L);
					warp.createdByName = selfName();
					warps.warps.add(warp);
				} else if (op.equals("warp_set")) {
					warp.place = here();
				}

				String rename = a.getString("rename");
				if (!rename.isEmpty()) {
					for (Warp other : warps.warps) {
						if (other != warp && other.name.equalsIgnoreCase(rename)) {
							return ServerNet.Result.fail(request, "name_taken");
						}
					}

					warp.name = rename;
				}

				if (a.contains("icon")) {
					warp.icon = a.getString("icon");
				}

				if (a.contains("color")) {
					warp.color = a.getInt("color");
				}

				if (a.contains("description")) {
					warp.description = a.getString("description");
				}

				if (a.contains("restricted")) {
					warp.restricted = a.getBoolean("restricted");
				}

				ServerState.previewPut("warps", warps);
				break;
			}
			case "warp_delete": {
				WarpList warps = ServerState.warps();
				warps.warps.removeIf(w -> w.name.equalsIgnoreCase(name));
				ServerState.previewPut("warps", warps);
				break;
			}
			case "tpa":
				if (!a.hasUUID("id")) {
					return ServerNet.Result.fail(request, "invalid");
				}

				UUID to = a.getUUID("id");
				List<TeleportRequest> open = new ArrayList<>(ServerState.requests());

				for (OnlinePlayer pxx : ServerState.online()) {
					if (pxx.id.equals(to)) {
						if (!pxx.acceptsRequests) {
							return ServerNet.Result.fail(request, "requests_off", pxx.shownName());
						}

						TeleportRequest rxx = request(pxx.name, false, a.getBoolean("here"), now + 60000L);
						rxx.player = pxx.id;
						open.add(rxx);
					}
				}

				ServerState.previewPut("requests", open);
				break;
			case "tpa_accept":
			case "tpa_deny":
			case "tpa_cancel":
				boolean incoming = !op.equals("tpa_cancel");
				List<TeleportRequest> pending = new ArrayList<>(ServerState.requests());
				TeleportRequest hit = null;

				for (TeleportRequest r : pending) {
					if (r.incoming == incoming && (!a.hasUUID("id") || r.player.equals(a.getUUID("id")))) {
						hit = r;
					}
				}

				if (hit == null) {
					return ServerNet.Result.fail(request, "no_request");
				}

				pending.remove(hit);
				ServerState.previewPut("requests", pending);
				if (op.equals("tpa_accept") && hit.here) {
					return travel(request, "player:" + hit.name);
				}
				break;
			case "tpa_toggle":
				self.acceptsRequests = a.contains("on") ? a.getBoolean("on") : !self.acceptsRequests;
				ServerState.previewPut("self", self);
				break;
			case "afk":
				self.afk = a.contains("on") ? a.getBoolean("on") : !self.afk;
				List<OnlinePlayer> online = new ArrayList<>(ServerState.online());

				for (OnlinePlayer px : online) {
					if (px.id.equals(ServerState.selfId())) {
						px.afk = self.afk;
					}
				}

				ServerState.previewPut("online", online);
				ServerState.previewPut("self", self);
				break;
			case "nick":
				self.nick = a.getString("nick");
				List<OnlinePlayer> onlineList = new ArrayList<>(ServerState.online());

				for (OnlinePlayer p : onlineList) {
					if (p.id.equals(ServerState.selfId())) {
						p.nick = self.nick;
					}
				}

				ServerState.previewPut("online", onlineList);
				ServerState.previewPut("self", self);
				break;
			case "stats_hide":
				self.statsHidden = a.getBoolean("on");
				ServerState.previewPut("self", self);
				break;
			case "mail_read":
			case "mail_delete":
				List<PlayerRecord.Mail> mail = new ArrayList<>(ServerState.mail());
				String which = a.contains("id") ? a.getString("id") : null;
				if (op.equals("mail_read")) {
					for (PlayerRecord.Mail m : mail) {
						if (which == null || m.id.equals(which)) {
							m.read = true;
						}
					}
				} else {
					mail.removeIf(mx -> which == null ? mx.read : mx.id.equals(which));
				}

				int unread = 0;

				for (PlayerRecord.Mail mx : mail) {
					if (!mx.read) {
						unread++;
					}
				}

				self.unreadMail = unread;
				ServerState.previewPut("mail", mail);
				ServerState.previewPut("self", self);
				break;
			case "kit_claim":
				for (Kit.View v : self.kits) {
					if (v.id.equals(a.getString("kit"))) {
						Kit kit = kits.stream().filter(k -> k.id.equals(v.id)).findFirst().orElse(null);
						v.readyAt = kit != null && kit.oneTime ? -1L : now + (long)(kit != null && kit.cooldownSeconds != 0 ? kit.cooldownSeconds : 10) * 1000L;
						ServerState.previewNotice("kit_received", 0, v.name);
					}
				}

				ServerState.previewPut("self", self);
				break;
			case "admin_watch":
				watching = a.getBoolean("on");
				if (watching && vitalSigns != null) {
					ServerState.previewPut("dashboard", vitalSigns);
				}
				break;
			case "role_save":
				Role role = (Role)JsonWire.GSON.fromJson(a.getString("role"), Role.class);
				if (role == null || role.sanitize().id.isEmpty()) {
					return ServerNet.Result.fail(request, "invalid");
				}

				roles.roles.removeIf(rxx -> rxx.id.equals(role.id));
				roles.roles.add(role);
				roles.sanitize();
				break;
			case "role_delete":
				if ("default".equals(a.getString("id"))) {
					return ServerNet.Result.fail(request, "role_protected");
				}

				roles.roles.removeIf(r -> r.id.equals(a.getString("id")));
				break;
			case "role_grant":
			case "role_revoke":
				AdminData.PlayerDetail dxxx = a.hasUUID("id") ? detail(a.getUUID("id")) : null;
				Role grantRole = roles.get(a.getString("role"));
				if (dxxx == null || grantRole == null) {
					return ServerNet.Result.fail(request, "no_such_role", a.getString("role"));
				}

				boolean had = dxxx.roles.removeIf(g -> g.role.equals(grantRole.id));
				if (op.equals("role_grant")) {
					if (had) {
						Role.Grant keep = new Role.Grant();
						keep.role = grantRole.id;
						dxxx.roles.add(keep);
						return ServerNet.Result.fail(request, "already_has_role", dxxx.row.name);
					}

					Role.Grant g = new Role.Grant();
					g.role = grantRole.id;
					g.byName = selfName();
					g.at = now;
					g.expiresAt = a.contains("seconds") ? now + (long)a.getInt("seconds") * 1000L : 0L;
					dxxx.roles.add(g);
					dxxx.row.role = new Role.Tag(grantRole);
				} else if (dxxx.row.role != null && dxxx.row.role.id.equals(grantRole.id)) {
					dxxx.row.role = null;
				}
				break;
			case "kit_save":
				Kit kit = (Kit)JsonWire.GSON.fromJson(a.getString("kit"), Kit.class);
				if (kit == null || kit.sanitize().id.isEmpty()) {
					return ServerNet.Result.fail(request, "invalid");
				}

				Kit old = kits.stream().filter(k -> k.id.equals(kit.id)).findFirst().orElse(null);
				if (!a.getBoolean("from_inventory") && old != null) {
					kit.items = old.items;
				} else {
					kit.items = new ArrayList<>(
						List.of("{id:\"minecraft:iron_sword\",count:1}", "{id:\"minecraft:cooked_beef\",count:16}", "{id:\"minecraft:torch\",count:32}")
					);
				}

				kits.removeIf(k -> k.id.equals(kit.id));
				kits.add(kit);
				break;
			case "kit_delete":
				kits.removeIf(k -> k.id.equals(a.getString("id")));
				break;
			case "rule_set":
				try {
					Field f = ServerRules.class.getField(a.getString("rule"));
					f.set(rules, JsonWire.GSON.fromJson(a.getString("value"), f.getGenericType()));
					rules.sanitize();
					Hello hello = ServerState.hello();
					boolean thirst = hello.feature("thirst");
					hello.maintenance = rules.maintenance;
					hello.features = rules.features();
					if (thirst) {
						hello.features.add("thirst");
					}

					hello.rules = new ArrayList<>(rules.rules);
					hello.links = new ArrayList<>(rules.links);
					ServerState.previewPut("hello", hello);
					break;
				} catch (NoSuchFieldException var16) {
					return ServerNet.Result.fail(request, "no_such_rule", a.getString("rule"));
				} catch (RuntimeException | IllegalAccessException var17) {
					return ServerNet.Result.fail(request, "value_invalid");
				}
			case "world_set":
				String value = a.getString("value");
				String var40 = a.getString("what");
				switch (var40) {
					case "difficulty":
						world.difficulty = value;
						return ok(request);
					case "gamerule":
						for (AdminData.GameRule rx : world.gameRules) {
							if (rx.id.equals(a.getString("key"))) {
								rx.value = value;
							}
						}

						return ok(request);
					case "time":
						long t = switch (value) {
							case "day" -> 1000L;
							case "noon" -> 6000L;
							case "night" -> 13000L;
							case "midnight" -> 18000L;
							default -> 0L;
						};

						for (AdminData.DimensionTime dxx : world.dimensions) {
							if (!a.contains("dimension") || dxx.id.equals(a.getString("dimension"))) {
								dxx.dayTime = t;
							}
						}

						return ok(request);
					case "weather":
						for (AdminData.DimensionTime dx : world.dimensions) {
							if (!a.contains("dimension") || dx.id.equals(a.getString("dimension"))) {
								dx.raining = !value.equals("clear");
								dx.thundering = value.equals("thunder");
							}
						}

						return ok(request);
					default:
						return ServerNet.Result.fail(request, "invalid");
				}
			case "punish":
			case "pardon":
				AdminData.PlayerDetail dxxxx = a.hasUUID("id") ? detail(a.getUUID("id")) : null;
				Punishment.Kind kind = Punishment.Kind.parse(a.getString("kind"));
				if (dxxxx == null || kind == null) {
					return ServerNet.Result.fail(request, "invalid");
				}

				if (op.equals("punish")) {
					Punishment pxxx = punishment(
						"n" + now, kind, a.getString("reason"), selfName(), now, kind.lasts() && a.contains("seconds") ? now + (long)a.getInt("seconds") * 1000L : 0L, 0L
					);
					dxxxx.punishments.add(pxxx);
					AdminData.PunishmentRow row = new AdminData.PunishmentRow();
					row.player = dxxxx.row.id;
					row.playerName = dxxxx.row.name;
					row.punishment = pxxx;
					punishments.add(0, row);
				} else {
					boolean lifted = false;

					for (Punishment pxxx : dxxxx.punishments) {
						if (pxxx.kind == kind && pxxx.active(now)) {
							pxxx.liftedAt = now;
							pxxx.liftedByName = selfName();
							lifted = true;
						}
					}

					if (!lifted) {
						return ServerNet.Result.fail(request, "not_punished", dxxxx.row.name);
					}
				}

				dxxxx.row.muted = dxxxx.punishments.stream().anyMatch(pxxxx -> pxxxx.kind == Punishment.Kind.MUTE && pxxxx.active(now));
				dxxxx.row.banned = dxxxx.punishments.stream().anyMatch(pxxxx -> pxxxx.kind == Punishment.Kind.BAN && pxxxx.active(now));
				dxxxx.row.frozen = dxxxx.punishments.stream().anyMatch(pxxxx -> pxxxx.kind == Punishment.Kind.FREEZE && pxxxx.active(now));
				break;
			case "note_add":
			case "note_delete":
				AdminData.PlayerDetail noted = a.hasUUID("id") ? detail(a.getUUID("id")) : null;
				if (noted == null) {
					return ServerNet.Result.fail(request, "invalid");
				}

				if (op.equals("note_add")) {
					PlayerRecord.Note note = new PlayerRecord.Note();
					note.byName = selfName();
					note.by = ServerState.selfId();
					note.text = a.getString("text");
					note.at = now;
					noted.notes.add(note);
				} else if (a.getInt("index") >= 0 && a.getInt("index") < noted.notes.size()) {
					noted.notes.remove(a.getInt("index"));
				}
				break;
			case "player_do":
				AdminData.PlayerDetail subject = a.hasUUID("id") ? detail(a.getUUID("id")) : null;
				if (subject == null) {
					return ServerNet.Result.fail(request, "invalid");
				}

				boolean on = "on".equals(a.getString("value"));
				String lifted = a.getString("what");
				switch (lifted) {
					case "heal":
						subject.health = subject.maxHealth;
						break;
					case "feed":
						subject.food = 20;
						break;
					case "fly":
						subject.fly = on;
						break;
					case "god":
						subject.god = on;
						break;
					case "vanish":
						subject.row.vanished = on;
						break;
					case "gamemode":
						subject.gameMode = a.getString("value");
				}
		}

		return ok(request);
	}

	private static ServerNet.Result travel(int request, String target) {
		SelfState self = ServerState.self();
		long now = System.currentTimeMillis();
		if (self.pending != null) {
			return ServerNet.Result.fail(request, "already_pending");
		} else if (self.cooldown("tp") > now) {
			return ServerNet.Result.fail(request, "cooldown", Long.toString((self.cooldown("tp") - now + 999L) / 1000L));
		} else {
			int warmup = Math.max(1, ServerState.number(Nodes.TP_WARMUP));
			self.pending = target;
			self.pendingAt = now + (long)warmup * 1000L;
			pendingPlace = here();
			ServerState.previewPut("self", self);
			return new ServerNet.Result(request, true, "warmup", List.of(Integer.toString(warmup)));
		}
	}

	private static Place here() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && mc.level != null
			? new Place(mc.level.dimension().location().toString(), mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot())
			: new Place("minecraft:overworld", 24.5, 70.0, -12.5, 0.0F, 0.0F);
	}

	private static ServerRules.Link link(String name, String url) {
		ServerRules.Link l = new ServerRules.Link();
		l.name = name;
		l.url = url;
		return l;
	}

	private static Home home(String name, String dimension, double x, double y, double z, int color, String icon, long at) {
		Home h = new Home();
		h.name = name;
		h.place = new Place(dimension, x, y, z, 0.0F, 0.0F);
		h.color = color;
		h.icon = icon;
		h.createdAt = at;
		return h;
	}

	private static Warp warp(
		String name, String dimension, double x, double y, double z, int color, String icon, String description, boolean restricted, long at, long visits
	) {
		Warp w = new Warp();
		w.name = name;
		w.place = new Place(dimension, x, y, z, 0.0F, 0.0F);
		w.color = color;
		w.icon = icon;
		w.description = description;
		w.restricted = restricted;
		w.createdBy = id("Mira");
		w.createdByName = "Mira";
		w.createdAt = at;
		w.visits = visits;
		return w;
	}

	private static Kit kit(String id, String name, String icon, int cooldown, boolean oneTime, boolean firstJoin, boolean restricted, String... items) {
		Kit k = new Kit();
		k.id = id;
		k.name = name;
		k.icon = icon;
		k.cooldownSeconds = cooldown;
		k.oneTime = oneTime;
		k.firstJoin = firstJoin;
		k.restricted = restricted;

		for (String item : items) {
			int colon = item.lastIndexOf(58);
			k.items.add("{id:\"" + item.substring(0, colon) + "\",count:" + item.substring(colon + 1) + "}");
		}

		return k;
	}

	private static Kit.View view(Kit kit, long readyAt) {
		Kit.View v = new Kit.View();
		v.id = kit.id;
		v.name = kit.name;
		v.icon = kit.icon;
		v.readyAt = readyAt;

		for (String snbt : kit.items) {
			Kit.View.Entry e = new Kit.View.Entry();
			e.item = snbt.substring(snbt.indexOf(34) + 1, snbt.lastIndexOf(34));
			e.count = Integer.parseInt(snbt.substring(snbt.lastIndexOf(58) + 1, snbt.length() - 1));
			v.contents.add(e);
		}

		return v;
	}

	private static OnlinePlayer online(UUID id, String name, String nick, @Nullable Role role, boolean afk, long joinedAt) {
		OnlinePlayer p = new OnlinePlayer();
		p.id = id;
		p.name = name;
		p.nick = nick;
		p.role = role == null ? null : new Role.Tag(role);
		p.afk = afk;
		p.joinedAt = joinedAt;
		return p;
	}

	private static TeleportRequest request(String name, boolean incoming, boolean here, long expiresAt) {
		TeleportRequest r = new TeleportRequest();
		r.player = id(name);
		r.name = name;
		r.incoming = incoming;
		r.here = here;
		r.expiresAt = expiresAt;
		return r;
	}

	private static PlayerRecord.Mail letter(String id, String from, String text, long at, boolean read) {
		PlayerRecord.Mail m = new PlayerRecord.Mail();
		m.id = id;
		m.from = id(from);
		m.fromName = from;
		m.text = text;
		m.at = at;
		m.read = read;
		return m;
	}

	private static Punishment punishment(String id, Punishment.Kind kind, String reason, String by, long at, long expiresAt, long liftedAt) {
		Punishment p = new Punishment();
		p.id = id;
		p.kind = kind;
		p.reason = reason;
		p.byName = by;
		p.by = id(by);
		p.at = at;
		p.expiresAt = expiresAt;
		p.liftedAt = liftedAt;
		return p;
	}

	private static AdminData.Dimension dimension(String id, int players, int entities, int chunks, int blockEntities) {
		AdminData.Dimension d = new AdminData.Dimension();
		d.id = id;
		d.players = players;
		d.entities = entities;
		d.chunks = chunks;
		d.blockEntities = blockEntities;
		return d;
	}

	private static AdminData.DimensionTime time(String id, long dayTime, boolean raining, boolean thundering) {
		AdminData.DimensionTime t = new AdminData.DimensionTime();
		t.id = id;
		t.dayTime = dayTime;
		t.raining = raining;
		t.thundering = thundering;
		return t;
	}

	private SampleServer() {
	}
}
