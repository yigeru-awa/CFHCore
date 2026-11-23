package com.onehumanawa.cfhcore.content.gui.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class AdminData {
	public static final int PAGE = 50;

	private AdminData() {
	}

	public static final class AuditEntry {
		public long at;
		public UUID actor = new UUID(0L, 0L);
		public String actorName = "";
		public String action = "";
		@Nullable
		public UUID target;
		public String targetName = "";
		public String detail = "";

		public AdminData.AuditEntry sanitize() {
			if (this.actor == null) {
				this.actor = new UUID(0L, 0L);
			}

			if (this.actorName == null) {
				this.actorName = "";
			}

			if (this.action == null) {
				this.action = "";
			}

			if (this.targetName == null) {
				this.targetName = "";
			}

			if (this.detail == null) {
				this.detail = "";
			}

			return this;
		}
	}

	public static final class AuditPage {
		public List<AdminData.AuditEntry> rows = new ArrayList<>();
		public int page;
		public int total;

		public AdminData.AuditPage sanitize() {
			if (this.rows == null) {
				this.rows = new ArrayList<>();
			}

			this.rows.removeIf(r -> r == null);

			for (AdminData.AuditEntry e : this.rows) {
				e.sanitize();
			}

			return this;
		}
	}

	public static final class Dashboard {
		public double tps = 20.0;
		public double tpsMinute = 20.0;
		public double mspt;
		public double msptWorst;
		public float[] msptHistory = new float[0];
		public int players;
		public int maxPlayers;
		public long memoryUsed;
		public long memoryTotal;
		public long memoryMax;
		public long uptimeMs;
		public List<AdminData.Dimension> dimensions = new ArrayList<>();
		public boolean maintenance;
		public String externalPermissions = "";

		public AdminData.Dashboard sanitize() {
			if (this.msptHistory == null) {
				this.msptHistory = new float[0];
			}

			if (this.dimensions == null) {
				this.dimensions = new ArrayList<>();
			}

			this.dimensions.removeIf(d -> d == null || d.id == null);
			if (this.externalPermissions == null) {
				this.externalPermissions = "";
			}

			return this;
		}
	}

	public static final class Dimension {
		public String id = "minecraft:overworld";
		public int players;
		public int entities;
		public int chunks;
		public int blockEntities;
	}

	public static final class DimensionTime {
		public String id = "minecraft:overworld";
		public long dayTime;
		public boolean raining;
		public boolean thundering;
	}

	public static final class GameRule {
		public String id = "";
		public String nameKey = "";
		public String categoryKey = "";
		public boolean bool;
		public String value = "";
	}

	public static final class NodeInfo {
		public String id = "";
		public int level;
		public boolean number;
		public int defaultNumber;
		public String description = "";
	}

	public static final class PlayerDetail {
		public AdminData.PlayerRow row = new AdminData.PlayerRow();
		public long firstSeen;
		public long sessions;
		public long playTicks;
		public long afkTicks;
		@Nullable
		public Place place;
		@Nullable
		public Place death;
		public List<Home> homes = new ArrayList<>();
		public List<Role.Grant> roles = new ArrayList<>();
		public List<Punishment> punishments = new ArrayList<>();
		public List<PlayerRecord.Note> notes = new ArrayList<>();
		public float health;
		public float maxHealth;
		public int food;
		public int level;
		public int ping;
		public String gameMode = "";
		public boolean god;
		public boolean fly;
		public boolean operator;
		public boolean mayActOn;

		public AdminData.PlayerDetail sanitize() {
			if (this.row == null) {
				this.row = new AdminData.PlayerRow();
			}

			this.row.sanitize();
			if (this.place != null) {
				this.place.sanitize();
			}

			if (this.death != null) {
				this.death.sanitize();
			}

			if (this.homes == null) {
				this.homes = new ArrayList<>();
			}

			this.homes.removeIf(h -> h == null);

			for (Home h : this.homes) {
				h.sanitize();
			}

			if (this.roles == null) {
				this.roles = new ArrayList<>();
			}

			this.roles.removeIf(g -> g == null);

			for (Role.Grant g : this.roles) {
				g.sanitize();
			}

			if (this.punishments == null) {
				this.punishments = new ArrayList<>();
			}

			this.punishments.removeIf(p -> p == null);

			for (Punishment p : this.punishments) {
				p.sanitize();
			}

			if (this.notes == null) {
				this.notes = new ArrayList<>();
			}

			this.notes.removeIf(nx -> nx == null);

			for (PlayerRecord.Note n : this.notes) {
				n.sanitize();
			}

			if (this.gameMode == null) {
				this.gameMode = "";
			}

			return this;
		}
	}

	public static final class PlayerPage {
		public List<AdminData.PlayerRow> rows = new ArrayList<>();
		public int page;
		public int total;

		public AdminData.PlayerPage sanitize() {
			if (this.rows == null) {
				this.rows = new ArrayList<>();
			}

			this.rows.removeIf(rx -> rx == null);

			for (AdminData.PlayerRow r : this.rows) {
				r.sanitize();
			}

			return this;
		}
	}

	public static final class PlayerRow {
		public UUID id = new UUID(0L, 0L);
		public String name = "";
		public String nick = "";
		@Nullable
		public Role.Tag role;
		public boolean online;
		public boolean afk;
		public boolean vanished;
		public boolean muted;
		public boolean banned;
		public boolean frozen;
		public long lastSeen;
		public String dimension = "";

		public AdminData.PlayerRow sanitize() {
			if (this.id == null) {
				this.id = new UUID(0L, 0L);
			}

			if (this.name == null) {
				this.name = "";
			}

			if (this.nick == null) {
				this.nick = "";
			}

			if (this.dimension == null) {
				this.dimension = "";
			}

			if (this.role != null) {
				this.role.sanitize();
			}

			return this;
		}
	}

	public static final class PunishmentPage {
		public List<AdminData.PunishmentRow> rows = new ArrayList<>();
		public int page;
		public int total;

		public AdminData.PunishmentPage sanitize() {
			if (this.rows == null) {
				this.rows = new ArrayList<>();
			}

			this.rows.removeIf(rx -> rx == null || rx.punishment == null);

			for (AdminData.PunishmentRow r : this.rows) {
				if (r.player == null) {
					r.player = new UUID(0L, 0L);
				}

				if (r.playerName == null) {
					r.playerName = "";
				}

				r.punishment.sanitize();
			}

			return this;
		}
	}

	public static final class PunishmentRow {
		public UUID player = new UUID(0L, 0L);
		public String playerName = "";
		public Punishment punishment = new Punishment();
	}

	public static final class RoleSet {
		public List<Role> roles = new ArrayList<>();
		public List<AdminData.NodeInfo> nodes = new ArrayList<>();
		public List<Integer> holders = new ArrayList<>();
		public int editableBelow = Integer.MAX_VALUE;
		public String externalPermissions = "";

		public AdminData.RoleSet sanitize() {
			if (this.roles == null) {
				this.roles = new ArrayList<>();
			}

			this.roles.removeIf(r -> r == null);

			for (Role r : this.roles) {
				r.sanitize();
			}

			if (this.nodes == null) {
				this.nodes = new ArrayList<>();
			}

			this.nodes.removeIf(nx -> nx == null || nx.id == null);

			for (AdminData.NodeInfo n : this.nodes) {
				if (n.description == null) {
					n.description = "";
				}
			}

			if (this.holders == null) {
				this.holders = new ArrayList<>();
			}

			if (this.externalPermissions == null) {
				this.externalPermissions = "";
			}

			return this;
		}
	}

	public static final class WorldInfo {
		public String difficulty = "normal";
		public boolean difficultyLocked;
		public boolean hardcore;
		public List<AdminData.DimensionTime> dimensions = new ArrayList<>();
		public List<AdminData.GameRule> gameRules = new ArrayList<>();

		public AdminData.WorldInfo sanitize() {
			if (this.difficulty == null) {
				this.difficulty = "normal";
			}

			if (this.dimensions == null) {
				this.dimensions = new ArrayList<>();
			}

			this.dimensions.removeIf(d -> d == null || d.id == null);
			if (this.gameRules == null) {
				this.gameRules = new ArrayList<>();
			}

			this.gameRules.removeIf(g -> g == null || g.id == null || g.value == null);
			return this;
		}
	}
}
