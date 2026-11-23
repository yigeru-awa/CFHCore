package com.onehumanawa.cfhcore.content.gui.teams.client;

import com.onehumanawa.cfhcore.content.gui.teams.model.Access;
import com.onehumanawa.cfhcore.content.gui.teams.model.BannerData;
import com.onehumanawa.cfhcore.content.gui.teams.model.InviteView;
import com.onehumanawa.cfhcore.content.gui.teams.model.Privacy;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.ServerInfo;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeammateState;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;

final class SampleTeams {
	private static final long MINUTE = 60000L;
	private static final long HOUR = 3600000L;
	private static final long DAY = 86400000L;

	private static UUID id(String name) {
		return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
	}

	private static UUID team(String name) {
		return UUID.nameUUIDFromBytes(("SlateSampleTeam:" + name).getBytes(StandardCharsets.UTF_8));
	}

	private static TeamData.Member member(String name, Rank rank, long joined, long seen) {
		TeamData.Member m = new TeamData.Member();
		m.id = id(name);
		m.name = name;
		m.rank = rank;
		m.joinedAt = joined;
		m.lastSeen = seen;
		return m;
	}

	private static TeamData.LogEntry log(long at, String type, String actor, String subject) {
		TeamData.LogEntry e = new TeamData.LogEntry();
		e.at = at;
		e.type = type;
		e.actor = id(actor);
		e.actorName = actor;
		e.subject = subject;
		return e;
	}

	private static TeamData.Waypoint waypoint(String name, int x, int y, int z, String dimension, int color, String icon, String by, long at) {
		TeamData.Waypoint w = new TeamData.Waypoint();
		w.id = name.toLowerCase(Locale.ROOT).replace(' ', '_');
		w.name = name;
		w.x = x;
		w.y = y;
		w.z = z;
		w.dimension = dimension;
		w.color = color;
		w.icon = icon;
		w.by = id(by);
		w.at = at;
		return w;
	}

	private static TeamSummary other(
		String name,
		String tag,
		int color,
		BannerData banner,
		String motto,
		Privacy privacy,
		int members,
		int online,
		int level,
		int claims,
		String owner,
		long quests,
		long created
	) {
		TeamSummary s = new TeamSummary();
		s.id = team(name);
		s.name = name;
		s.tag = tag;
		s.color = color;
		s.banner = banner;
		s.motto = motto;
		s.privacy = privacy;
		s.members = members;
		s.online = online;
		s.level = level;
		s.claims = claims;
		s.owner = id(owner);
		s.ownerName = owner;
		s.quests = quests;
		s.createdAt = created;
		return s;
	}

	static void fill() {
		Minecraft mc = Minecraft.getInstance();
		long now = System.currentTimeMillis();
		UUID self = mc.getUser().getProfileId();
		String selfName = mc.getUser().getName();
		TeamData t = new TeamData();
		t.id = team("Emberwatch");
		t.name = "Emberwatch";
		t.tag = "EMB";
		t.color = 14254174;
		t.motto = "We keep the fires lit.";
		t.banner = new BannerData("black").layer("minecraft:gradient_up", "orange").layer("minecraft:flow", "yellow").layer("minecraft:border", "orange");
		t.privacy = Privacy.REQUEST;
		t.createdAt = now - 5529600000L;
		TeamData.Member me = new TeamData.Member();
		me.id = self;
		me.name = selfName;
		me.rank = Rank.OWNER;
		me.joinedAt = t.createdAt;
		me.lastSeen = now;
		t.members.add(me);
		t.members.add(member("Mossback", Rank.ADMIN, now - 5443200000L, now));
		t.members.add(member("Quillfeather", Rank.MODERATOR, now - 4320000000L, now));
		t.members.add(member("TinLantern", Rank.MEMBER, now - 2678400000L, now));
		t.members.add(member("Brackenfoot", Rank.MEMBER, now - 1728000000L, now - 10800000L));
		t.members.add(member("Saltwick", Rank.MEMBER, now - 1036800000L, now - 172800000L));
		t.members.add(member("Fennelseed", Rank.MEMBER, now - 432000000L, now - 777600000L));
		t.allies.add(team("Hollowpine Rail"));
		t.allies.add(team("Saltmarsh"));
		TeamData.Invite invite = new TeamData.Invite();
		invite.player = id("Cindermoth");
		invite.playerName = "Cindermoth";
		invite.by = self;
		invite.byName = selfName;
		invite.expiresAt = now + 1320000L;
		t.invites.add(invite);
		TeamData.Request request = new TeamData.Request();
		request.player = id("Waxwing");
		request.playerName = "Waxwing";
		request.at = now - 2400000L;
		t.requests.add(request);
		t.home = new TeamData.Place();
		t.home.x = 118.5;
		t.home.y = 71.0;
		t.home.z = -42.5;
		t.waypoints.add(waypoint("Keep", 118, 71, -42, "minecraft:overworld", 14254174, "home", selfName, now - 5184000000L));
		t.waypoints.add(waypoint("Iron farm", 240, 64, 96, "minecraft:overworld", 10134964, "farm", "Mossback", now - 2592000000L));
		t.waypoints.add(waypoint("Deep mine", -60, -32, 130, "minecraft:overworld", 6007008, "mine", "TinLantern", now - 1209600000L));
		t.waypoints.add(waypoint("Fortress", 31, 68, -12, "minecraft:the_nether", 15026253, "danger", "Quillfeather", now - 518400000L));
		t.waypoints.add(waypoint("Stronghold", -912, 34, 1460, "minecraft:overworld", 10192608, "portal", "Quillfeather", now - 172800000L));
		t.land.interact = Access.ALLIES;
		t.level = 7;
		t.xp = 310L;
		t.counters.put("quests", 86L);
		t.counters.put("chapters", 4L);
		t.log.add(log(now - 259200000L, "claimed", "Mossback", "6"));
		t.log.add(log(now - 172800000L, "waypoint", "Quillfeather", "Stronghold"));
		t.log.add(log(now - 108000000L, "quest", "TinLantern", "Into the Deep"));
		t.log.add(log(now - 93600000L, "promoted", selfName, "Quillfeather"));
		t.log.add(log(now - 32400000L, "ally", selfName, "Saltmarsh"));
		t.log.add(log(now - 18000000L, "joined", "Fennelseed", ""));
		t.log.add(log(now - 4200000L, "chapter", "Mossback", "The Nether"));
		t.log.add(log(now - 720000L, "level", "Mossback", "7"));
		t.sanitize();
		List<TeamSummary> teams = new ArrayList<>();
		teams.add(TeamSummary.of(t, 4, 23));
		teams.add(
			other(
				"Hollowpine Rail",
				"RAIL",
				6007008,
				new BannerData("light_blue").layer("minecraft:stripe_center", "white").layer("minecraft:stripe_middle", "white").layer("minecraft:border", "blue"),
				"End of the line is just the start.",
				Privacy.OPEN,
				9,
				3,
				9,
				41,
				"OldHarrow",
				132L,
				now - 10368000000L
			)
		);
		teams.add(
			other(
				"Saltmarsh",
				"SALT",
				6080680,
				new BannerData("cyan").layer("minecraft:triangles_bottom", "white").layer("minecraft:globe", "lime"),
				"Tide in, tide out.",
				Privacy.REQUEST,
				5,
				2,
				5,
				18,
				"Saltwick",
				61L,
				now - 3801600000L
			)
		);
		teams.add(
			other(
				"The Long Night",
				"NGHT",
				10192608,
				new BannerData("purple").layer("minecraft:gradient", "black").layer("minecraft:skull", "white"),
				"",
				Privacy.INVITE,
				12,
				5,
				12,
				77,
				"Cindermoth",
				188L,
				now - 17280000000L
			)
		);
		teams.add(
			other(
				"Bramble & Co",
				"BRMB",
				8106858,
				new BannerData("green").layer("minecraft:flower", "pink").layer("minecraft:bricks", "lime"),
				"Farms, mostly.",
				Privacy.OPEN,
				3,
				0,
				2,
				6,
				"Waxwing",
				17L,
				now - 777600000L
			)
		);
		teams.add(
			other(
				"Redwake",
				"RED",
				15026253,
				new BannerData("red").layer("minecraft:cross", "black").layer("minecraft:rhombus", "white"),
				"Ask first. Then run.",
				Privacy.INVITE,
				7,
				1,
				6,
				30,
				"Brackenfoot",
				95L,
				now - 6134400000L
			)
		);
		teams.get(1).allies.add(t.id);
		List<InviteView> invites = new ArrayList<>();
		InviteView iv = new InviteView();
		iv.team = team("Hollowpine Rail");
		iv.teamName = "Hollowpine Rail";
		iv.tag = "RAIL";
		iv.color = 6007008;
		iv.banner = teams.get(1).banner;
		iv.members = 9;
		iv.byName = "OldHarrow";
		iv.expiresAt = now + 840000L;
		invites.add(iv);
		List<TeammateState> mates = new ArrayList<>();
		mates.add(new TeammateState(id("Mossback"), "minecraft:overworld", 131.0F, 70.0F, -38.0F, 40.0F, 17.0F, 20.0F, 15, 18, 0));
		mates.add(new TeammateState(id("Quillfeather"), "minecraft:the_nether", 28.0F, 68.0F, -9.0F, 180.0F, 8.5F, 20.0F, 20, 12, 0));
		mates.add(new TeammateState(id("TinLantern"), "minecraft:overworld", -52.0F, -30.0F, 126.0F, 270.0F, 20.0F, 24.0F, 7, 20, 2));
		mates.add(new TeammateState(id("OldHarrow"), "minecraft:overworld", 204.0F, 66.0F, 150.0F, 0.0F, 20.0F, 20.0F, 10, 20, 4));
		Map<Long, TeamsState.Claim> overworld = new HashMap<>();
		blob(overworld, t.id, 7, -3, 23, 11L, 2);
		blob(overworld, team("Hollowpine Rail"), 13, 7, 41, 12L, 0);
		blob(overworld, team("Saltmarsh"), -4, 9, 18, 13L, 0);
		blob(overworld, team("Redwake"), -9, -8, 30, 14L, 0);
		blob(overworld, team("The Long Night"), 20, -10, 40, 15L, 0);
		Map<String, Map<Long, TeamsState.Claim>> claims = new HashMap<>();
		claims.put("minecraft:overworld", overworld);
		ServerInfo info = new ServerInfo();
		info.maxClaims = 110;
		info.maxForceLoads = 8;
		TeamsState.preview(info, t, teams, invites, mates, claims);
	}

	private static void blob(Map<Long, TeamsState.Claim> into, UUID team, int cx, int cz, int count, long seed, int forced) {
		Random rnd = new Random(seed);
		List<long[]> edge = new ArrayList<>();
		edge.add(new long[]{(long)cx, (long)cz});
		int placed = 0;

		while (placed < count && !edge.isEmpty()) {
			long[] c = edge.remove(rnd.nextInt(Math.min(edge.size(), 4)));
			long key = ChunkPos.asLong((int)c[0], (int)c[1]);
			if (!into.containsKey(key)) {
				into.put(key, new TeamsState.Claim(team, placed < forced));
				placed++;
				edge.add(new long[]{c[0] + 1L, c[1]});
				edge.add(new long[]{c[0] - 1L, c[1]});
				edge.add(new long[]{c[0], c[1] + 1L});
				edge.add(new long[]{c[0], c[1] - 1L});
			}
		}
	}

	private SampleTeams() {
	}
}
