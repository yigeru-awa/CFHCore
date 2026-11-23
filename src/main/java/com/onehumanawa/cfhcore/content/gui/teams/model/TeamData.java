package com.onehumanawa.cfhcore.content.gui.teams.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class TeamData {
	public static final int NAME_MIN = 3;
	public static final int NAME_MAX = 24;
	public static final int TAG_MIN = 2;
	public static final int TAG_MAX = 5;
	public static final int MOTTO_MAX = 96;
	public static final int LOG_MAX = 60;
	public UUID id = new UUID(0L, 0L);
	public String name = "";
	public String tag = "";
	public int color = 14254174;
	public String motto = "";
	public BannerData banner = new BannerData();
	public Privacy privacy = Privacy.INVITE;
	public boolean friendlyFire = false;
	public long createdAt;
	public long lastSeen;
	public boolean solo;
	public List<TeamData.Member> members = new ArrayList<>();
	public List<UUID> allies = new ArrayList<>();
	public List<TeamData.Invite> invites = new ArrayList<>();
	public List<TeamData.Request> requests = new ArrayList<>();
	@Nullable
	public TeamData.Place home;
	public List<TeamData.Waypoint> waypoints = new ArrayList<>();
	public LandRules land = new LandRules();
	public int level = 1;
	public long xp;
	public Map<String, Long> counters = new LinkedHashMap<>();
	public List<TeamData.LogEntry> log = new ArrayList<>();

	@Nullable
	public TeamData.Member member(@Nullable UUID player) {
		if (player == null) {
			return null;
		} else {
			for (TeamData.Member m : this.members) {
				if (player.equals(m.id)) {
					return m;
				}
			}

			return null;
		}
	}

	public boolean isMember(@Nullable UUID player) {
		return this.member(player) != null;
	}

	@Nullable
	public Rank rankOf(@Nullable UUID player) {
		TeamData.Member m = this.member(player);
		return m == null ? null : m.rank;
	}

	public boolean may(@Nullable UUID player, Permission permission) {
		return permission.allows(this.rankOf(player));
	}

	public boolean mayClaim(@Nullable UUID player) {
		Rank r = this.rankOf(player);
		return r != null && r.atLeast(this.land.claimRank);
	}

	@Nullable
	public TeamData.Member owner() {
		for (TeamData.Member m : this.members) {
			if (m.rank == Rank.OWNER) {
				return m;
			}
		}

		return this.members.isEmpty() ? null : this.members.get(0);
	}

	public boolean isAlly(@Nullable UUID team) {
		return team != null && this.allies.contains(team);
	}

	@Nullable
	public TeamData.Waypoint waypoint(@Nullable String waypointId) {
		if (waypointId == null) {
			return null;
		} else {
			for (TeamData.Waypoint w : this.waypoints) {
				if (waypointId.equals(w.id)) {
					return w;
				}
			}

			return null;
		}
	}

	public long counter(String key) {
		Long v = this.counters == null ? null : this.counters.get(key);
		return v == null ? 0L : v;
	}

	public static long xpForLevel(int level) {
		long l = (long)Math.max(1, level);
		return 100L + 60L * (l - 1L) + 15L * (l - 1L) * (l - 1L);
	}

	public float levelProgress() {
		return Math.max(0.0F, Math.min(1.0F, (float)this.xp / (float)xpForLevel(this.level)));
	}

	public TeamData sanitize() {
		if (this.id == null) {
			this.id = UUID.randomUUID();
		}

		if (this.name == null) {
			this.name = "";
		}

		if (this.tag == null) {
			this.tag = "";
		}

		if (this.motto == null) {
			this.motto = "";
		}

		if (this.banner == null) {
			this.banner = new BannerData();
		}

		this.banner.sanitize();
		if (this.privacy == null) {
			this.privacy = Privacy.INVITE;
		}

		if (this.members == null) {
			this.members = new ArrayList<>();
		}

		this.members.removeIf(m -> m == null || m.id == null);

		for (TeamData.Member m : this.members) {
			if (m.name == null) {
				m.name = "";
			}

			if (m.rank == null) {
				m.rank = Rank.MEMBER;
			}
		}

		if (this.allies == null) {
			this.allies = new ArrayList<>();
		}

		this.allies.removeIf(a -> a == null);
		if (this.invites == null) {
			this.invites = new ArrayList<>();
		}

		this.invites.removeIf(i -> i == null || i.player == null);
		if (this.requests == null) {
			this.requests = new ArrayList<>();
		}

		this.requests.removeIf(r -> r == null || r.player == null);
		if (this.waypoints == null) {
			this.waypoints = new ArrayList<>();
		}

		this.waypoints.removeIf(wx -> wx == null || wx.id == null);

		for (TeamData.Waypoint w : this.waypoints) {
			if (w.name == null) {
				w.name = "";
			}

			if (w.dimension == null) {
				w.dimension = "minecraft:overworld";
			}

			if (w.icon == null) {
				w.icon = "flag";
			}
		}

		if (this.home != null && this.home.dimension == null) {
			this.home.dimension = "minecraft:overworld";
		}

		if (this.land == null) {
			this.land = new LandRules();
		}

		this.land.sanitize();
		if (this.level < 1) {
			this.level = 1;
		}

		if (this.xp < 0L) {
			this.xp = 0L;
		}

		if (this.counters == null) {
			this.counters = new LinkedHashMap<>();
		}

		if (this.log == null) {
			this.log = new ArrayList<>();
		}

		this.log.removeIf(e -> e == null);

		while (this.log.size() > 60) {
			this.log.remove(0);
		}

		return this;
	}

	public static final class Invite {
		public UUID player = new UUID(0L, 0L);
		public String playerName = "";
		public UUID by = new UUID(0L, 0L);
		public String byName = "";
		public long expiresAt;
	}

	public static final class LogEntry {
		public long at;
		public String type = "";
		public UUID actor = new UUID(0L, 0L);
		public String actorName = "";
		public String subject = "";
	}

	public static final class Member {
		public UUID id = new UUID(0L, 0L);
		public String name = "";
		public Rank rank = Rank.MEMBER;
		public long joinedAt;
		public long lastSeen;
	}

	public static final class Place {
		public String dimension = "minecraft:overworld";
		public double x;
		public double y;
		public double z;
		public float yaw;
		public float pitch;
	}

	public static final class Request {
		public UUID player = new UUID(0L, 0L);
		public String playerName = "";
		public long at;
	}

	public static final class Waypoint {
		public String id = "";
		public String name = "";
		public String dimension = "minecraft:overworld";
		public int x;
		public int y;
		public int z;
		public int color = 14722136;
		public String icon = "flag";
		public UUID by = new UUID(0L, 0L);
		public long at;
	}
}
