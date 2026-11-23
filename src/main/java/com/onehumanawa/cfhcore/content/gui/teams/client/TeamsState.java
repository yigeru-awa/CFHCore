package com.onehumanawa.cfhcore.content.gui.teams.client;

import com.onehumanawa.cfhcore.content.gui.core.client.DevHarness;
import com.onehumanawa.cfhcore.content.gui.core.event.Event;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.model.ClaimsUpdate;
import com.onehumanawa.cfhcore.content.gui.teams.model.InviteView;
import com.onehumanawa.cfhcore.content.gui.teams.model.Permission;
import com.onehumanawa.cfhcore.content.gui.teams.model.PingData;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.ServerInfo;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeammateState;
import com.onehumanawa.cfhcore.content.gui.teams.net.TeamsNet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

public final class TeamsState {
	public static final Event<Runnable> CHANGED = new Event("TEAMS_CHANGED");
	public static final Event<Runnable> TEAM_CHANGED = new Event("TEAMS_TEAM_CHANGED");
	public static final Event<Runnable> DIRECTORY_CHANGED = new Event("TEAMS_DIRECTORY_CHANGED");
	public static final Event<Runnable> INVITES_CHANGED = new Event("TEAMS_INVITES_CHANGED");
	public static final Event<Consumer<String>> CLAIMS_CHANGED = new Event("TEAMS_CLAIMS_CHANGED");
	public static final Event<Consumer<PingData>> PING = new Event("TEAMS_PING");
	public static final Event<Consumer<TeamsState.NoticeView>> NOTICE = new Event("TEAMS_NOTICE");
	@Nullable
	private static ServerInfo info;
	@Nullable
	private static TeamData mine;
	private static List<TeamSummary> directory = List.of();
	private static final Map<UUID, TeamSummary> BY_ID = new HashMap<>();
	private static List<InviteView> invites = List.of();
	private static Set<UUID> requested = Set.of();
	private static final Map<UUID, TeammateState> MATES = new LinkedHashMap<>();
	private static long matesAt;
	private static final Map<String, Long2ObjectOpenHashMap<TeamsState.Claim>> CLAIMS = new HashMap<>();
	private static final List<TeamsState.ActivePing> PINGS = new ArrayList<>();
	private static boolean preview;
	private static final boolean FORCE_PREVIEW = Boolean.getBoolean("cfh.teams.forcePreview");

	public static boolean connected() {
		return info != null && !preview;
	}

	public static boolean preview() {
		return preview;
	}

	public static boolean available() {
		return info != null;
	}

	public static ServerInfo info() {
		return info != null ? info : new ServerInfo();
	}

	public static UUID self() {
		return Minecraft.getInstance().getUser().getProfileId();
	}

	@Nullable
	public static TeamData team() {
		return mine != null && !mine.solo ? mine : null;
	}

	public static boolean hasTeam() {
		return team() != null;
	}

	@Nullable
	public static TeamData personal() {
		return mine != null && mine.solo ? mine : null;
	}

	@Nullable
	public static TeamData land() {
		return mine;
	}

	@Nullable
	public static Rank rank() {
		TeamData t = team();
		return t == null ? null : t.rankOf(self());
	}

	public static boolean may(Permission permission) {
		TeamData t = team();
		return t != null && t.may(self(), permission);
	}

	public static boolean mayClaim() {
		return mine != null ? mine.mayClaim(self()) : soloLand();
	}

	public static boolean soloLand() {
		return info != null && info.soloLand;
	}

	public static List<TeamSummary> directory() {
		return directory;
	}

	public static Collection<TeamSummary> holders() {
		return BY_ID.values();
	}

	@Nullable
	public static TeamSummary summary(@Nullable UUID teamId) {
		return teamId == null ? null : BY_ID.get(teamId);
	}

	@Nullable
	public static TeamSummary serverLand() {
		return BY_ID.get(TeamSummary.SERVER_LAND);
	}

	public static boolean isServerLand(@Nullable UUID teamId) {
		return TeamSummary.SERVER_LAND.equals(teamId);
	}

	public static List<InviteView> invites() {
		return invites;
	}

	public static Set<UUID> requested() {
		return requested;
	}

	public static Collection<TeammateState> teammates() {
		return MATES.values();
	}

	@Nullable
	public static TeammateState teammate(UUID player) {
		return MATES.get(player);
	}

	public static long teammatesAt() {
		return matesAt;
	}

	public static boolean isOwn(@Nullable UUID teamId) {
		return mine != null && mine.id.equals(teamId);
	}

	public static boolean isAlly(@Nullable UUID teamId) {
		TeamData t = team();
		return t != null && t.isAlly(teamId);
	}

	@Nullable
	public static TeamsState.Claim claimAt(String dimension, int chunkX, int chunkZ) {
		Long2ObjectOpenHashMap<TeamsState.Claim> m = CLAIMS.get(dimension);
		return m == null ? null : (TeamsState.Claim)m.get(ChunkPos.asLong(chunkX, chunkZ));
	}

	public static Long2ObjectMap<TeamsState.Claim> claims(String dimension) {
		Long2ObjectOpenHashMap<TeamsState.Claim> m = CLAIMS.get(dimension);
		return m == null ? new Long2ObjectOpenHashMap() : m;
	}

	public static int claimCount(@Nullable UUID teamId) {
		if (teamId == null) {
			return 0;
		} else {
			int n = 0;

			for (Long2ObjectOpenHashMap<TeamsState.Claim> m : CLAIMS.values()) {
				ObjectIterator var4 = m.values().iterator();

				while (var4.hasNext()) {
					TeamsState.Claim c = (TeamsState.Claim)var4.next();
					if (teamId.equals(c.team())) {
						n++;
					}
				}
			}

			return n;
		}
	}

	public static int forcedCount(@Nullable UUID teamId) {
		if (teamId == null) {
			return 0;
		} else {
			int n = 0;

			for (Long2ObjectOpenHashMap<TeamsState.Claim> m : CLAIMS.values()) {
				ObjectIterator var4 = m.values().iterator();

				while (var4.hasNext()) {
					TeamsState.Claim c = (TeamsState.Claim)var4.next();
					if (c.forced() && teamId.equals(c.team())) {
						n++;
					}
				}
			}

			return n;
		}
	}

	public static String currentDimension() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level == null ? "minecraft:overworld" : mc.level.dimension().location().toString();
	}

	public static List<TeamsState.ActivePing> pings() {
		long now = System.currentTimeMillis();
		PINGS.removeIf(p -> now - p.bornMs() > 9000L);
		return PINGS;
	}

	public static void onHello(TeamsNet.Hello p) {
		if (!FORCE_PREVIEW) {
			ServerInfo i = (ServerInfo)JsonWire.decode(p.info(), ServerInfo.class);
			if (i != null) {
				if (preview) {
					clear();
				}

				preview = false;
				info = i.sanitize();
				CHANGED.invoke(Runnable::run);
			}
		}
	}

	public static void onMyTeam(TeamsNet.MyTeam p) {
		if (!FORCE_PREVIEW) {
			TeamData t = p.team().length == 0 ? null : (TeamData)JsonWire.decode(p.team(), TeamData.class);
			mine = t == null ? null : t.sanitize();
			if (team() == null) {
				MATES.clear();
			}

			TEAM_CHANGED.invoke(Runnable::run);
			CHANGED.invoke(Runnable::run);
		}
	}

	public static void onDirectory(TeamsNet.Directory p) {
		if (!FORCE_PREVIEW) {
			TeamSummary[] list = (TeamSummary[])JsonWire.decode(p.teams(), TeamSummary[].class);
			setDirectory(list == null ? List.of() : List.of(list));
		}
	}

	public static void onInvites(TeamsNet.Invites p) {
		if (!FORCE_PREVIEW) {
			InviteView[] list = (InviteView[])JsonWire.decode(p.invites(), InviteView[].class);
			UUID[] asked = (UUID[])JsonWire.decode(p.requested(), UUID[].class);
			List<InviteView> out = new ArrayList<>();
			if (list != null) {
				for (InviteView v : list) {
					if (v != null) {
						out.add(v.sanitize());
					}
				}
			}

			invites = List.copyOf(out);
			Set<UUID> set = new HashSet<>();
			if (asked != null) {
				for (UUID u : asked) {
					if (u != null) {
						set.add(u);
					}
				}
			}

			requested = Set.copyOf(set);
			INVITES_CHANGED.invoke(Runnable::run);
			CHANGED.invoke(Runnable::run);
		}
	}

	public static void onTeammates(TeamsNet.Teammates p) {
		if (!FORCE_PREVIEW) {
			MATES.clear();
			UUID me = self();

			for (TeammateState s : p.states()) {
				if (!s.id().equals(me)) {
					MATES.put(s.id(), s);
				}
			}

			matesAt = System.currentTimeMillis();
		}
	}

	public static void onClaims(TeamsNet.Claims p) {
		if (!FORCE_PREVIEW) {
			ClaimsUpdate u = p.update();
			Long2ObjectOpenHashMap<TeamsState.Claim> m = CLAIMS.computeIfAbsent(u.dimension(), k -> new Long2ObjectOpenHashMap());
			if (u.reset()) {
				m.clear();
			}

			for (long c : u.removed()) {
				m.remove(c);
			}

			for (int i = 0; i < u.chunks().length; i++) {
				m.put(u.chunks()[i], new TeamsState.Claim(u.teams().get(u.owners()[i]), (u.flags()[i] & 1) != 0));
			}

			CLAIMS_CHANGED.invoke(l -> l.accept(u.dimension()));
			CHANGED.invoke(Runnable::run);
		}
	}

	public static void onPing(TeamsNet.Ping p) {
		addPing(p.ping());
	}

	public static void onNotice(TeamsNet.Notice p) {
		TeamsState.NoticeView v = new TeamsState.NoticeView(p.key(), p.args(), p.color());
		NOTICE.invoke(l -> l.accept(v));
	}

	static void addPing(PingData ping) {
		PINGS.removeIf(a -> a.ping().by().equals(ping.by()));
		PINGS.add(new TeamsState.ActivePing(ping, System.currentTimeMillis()));
		PING.invoke(l -> l.accept(ping));
	}

	private static void setDirectory(List<TeamSummary> list) {
		List<TeamSummary> out = new ArrayList<>();
		BY_ID.clear();

		for (TeamSummary s : list) {
			if (s != null) {
				s.sanitize();
				BY_ID.put(s.id, s);
				if (!s.server && !s.solo) {
					out.add(s);
				}
			}
		}

		directory = List.copyOf(out);
		DIRECTORY_CHANGED.invoke(Runnable::run);
		CHANGED.invoke(Runnable::run);
	}

	static void preview(
		ServerInfo i,
		@Nullable TeamData t,
		List<TeamSummary> teams,
		List<InviteView> open,
		List<TeammateState> mates,
		Map<String, Map<Long, TeamsState.Claim>> claims
	) {
		clear();
		preview = true;
		info = i;
		mine = t;
		invites = List.copyOf(open);

		for (TeammateState s : mates) {
			MATES.put(s.id(), s);
		}

		matesAt = System.currentTimeMillis();

		for (Entry<String, Map<Long, TeamsState.Claim>> e : claims.entrySet()) {
			Long2ObjectOpenHashMap<TeamsState.Claim> m = new Long2ObjectOpenHashMap();
			m.putAll(e.getValue());
			CLAIMS.put(e.getKey(), m);
		}

		setDirectory(teams);
		TEAM_CHANGED.invoke(Runnable::run);
	}

	static void disconnected() {
		if (!FORCE_PREVIEW) {
			clear();
			TEAM_CHANGED.invoke(Runnable::run);
			CHANGED.invoke(Runnable::run);
			ensurePreview();
		}
	}

	private static void clear() {
		info = null;
		mine = null;
		directory = List.of();
		BY_ID.clear();
		invites = List.of();
		requested = Set.of();
		MATES.clear();
		CLAIMS.clear();
		PINGS.clear();
		preview = false;
	}

	static void ensurePreview() {
		if (info == null && (DevHarness.sampleData() || FORCE_PREVIEW)) {
			try {
				SampleTeams.fill();
			} catch (Exception var1) {
				CFHGuiTeams.LOGGER.warn("[CFHGui Teams] the sample team could not be made: {}", var1.toString());
			}
		}
	}

	private TeamsState() {
	}

	public static record ActivePing(PingData ping, long bornMs) {
		public float age() {
			return (float)(System.currentTimeMillis() - this.bornMs) / 9000.0F;
		}
	}

	public static record Claim(UUID team, boolean forced) {
	}

	public static record NoticeView(String key, List<String> args, int color) {
	}
}
