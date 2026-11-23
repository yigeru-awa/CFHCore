package com.onehumanawa.cfhcore.content.gui.teams.server;

import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.teams.TeamsServerConfig;
import com.onehumanawa.cfhcore.content.gui.teams.model.ClaimsUpdate;
import com.onehumanawa.cfhcore.content.gui.teams.model.InviteView;
import com.onehumanawa.cfhcore.content.gui.teams.model.LandRules;
import com.onehumanawa.cfhcore.content.gui.teams.model.ServerInfo;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeammateState;
import com.onehumanawa.cfhcore.content.gui.teams.net.TeamsNet;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

final class Sync {
	private static final long DIRECTORY_LAZY_MS = 5000L;
	private static final int MAX_MATES = 512;
	private final TeamService teams;
	private final Set<TeamData> dirtyTeams = new LinkedHashSet<>();
	private final Set<TeamData> dirtyLimits = new HashSet<>();
	private final Set<UUID> dirtyPlayers = new HashSet<>();
	private final Set<UUID> dirtyInvites = new HashSet<>();
	private boolean directoryDirty;
	private long directoryDue;
	private final Object2IntOpenHashMap<UUID> lastMates = new Object2IntOpenHashMap();
	private final Object2BooleanOpenHashMap<UUID> toldOperator = new Object2BooleanOpenHashMap();

	Sync(TeamService teams) {
		this.teams = teams;
	}

	void team(TeamData t) {
		this.dirtyTeams.add(t);
	}

	void limits(TeamData t) {
		this.dirtyLimits.add(t);
	}

	void player(UUID player) {
		this.dirtyPlayers.add(player);
	}

	void invites(UUID player) {
		this.dirtyInvites.add(player);
	}

	void directory(boolean now) {
		long due = System.currentTimeMillis() + (now ? 0L : 5000L);
		if (!this.directoryDirty || due < this.directoryDue) {
			this.directoryDue = due;
		}

		this.directoryDirty = true;
	}

	void flush() {
		if (!this.teams.live) {
			this.dirtyTeams.clear();
			this.dirtyLimits.clear();
			this.dirtyPlayers.clear();
			this.dirtyInvites.clear();
			this.directoryDirty = false;
		} else {
			if (!this.dirtyPlayers.isEmpty()) {
				for (UUID id : this.dirtyPlayers) {
					ServerPlayer p = this.teams.online(id);
					if (p != null && TeamsNet.has(p)) {
						this.hello(p);
						this.myTeam(p);
						this.invites(p);
					}
				}
			}

			if (!this.dirtyLimits.isEmpty()) {
				for (TeamData t : this.dirtyLimits) {
					if (this.teams.store.get(t.id) == t) {
						for (TeamData.Member m : t.members) {
							if (!this.dirtyPlayers.contains(m.id)) {
								ServerPlayer p = this.teams.online(m.id);
								if (p != null) {
									this.hello(p);
								}
							}
						}
					}
				}

				this.dirtyLimits.clear();
			}

			if (!this.dirtyTeams.isEmpty()) {
				for (TeamData tx : this.dirtyTeams) {
					if (this.teams.store.get(tx.id) == tx) {
						TeamsNet.MyTeam payload = null;

						for (TeamData.Member mx : tx.members) {
							if (!this.dirtyPlayers.contains(mx.id)) {
								ServerPlayer p = this.teams.online(mx.id);
								if (p != null && TeamsNet.has(p)) {
									if (payload == null) {
										payload = new TeamsNet.MyTeam(this.pack(tx));
									}

									TeamsNet.send(p, payload);
								}
							}
						}
					}
				}

				this.dirtyTeams.clear();
			}

			if (!this.dirtyInvites.isEmpty()) {
				for (UUID idx : this.dirtyInvites) {
					if (!this.dirtyPlayers.contains(idx)) {
						ServerPlayer p = this.teams.online(idx);
						if (p != null && TeamsNet.has(p)) {
							this.invites(p);
						}
					}
				}

				this.dirtyInvites.clear();
			}

			this.dirtyPlayers.clear();
			if (this.directoryDirty && System.currentTimeMillis() >= this.directoryDue) {
				this.directoryDirty = false;
				this.all(new TeamsNet.Directory(this.directoryBytes()));
			}

			for (ClaimsUpdate u : this.teams.claims.drain()) {
				this.all(new TeamsNet.Claims(u));
			}
		}
	}

	void everything(ServerPlayer p) {
		if (this.teams.live && TeamsNet.has(p)) {
			this.hello(p);
			this.myTeam(p);
			TeamsNet.send(p, new TeamsNet.Directory(this.directoryBytes()));
			this.invites(p);

			for (ClaimsUpdate u : this.teams.claims.whole()) {
				TeamsNet.send(p, new TeamsNet.Claims(u));
			}

			this.lastMates.removeInt(p.getUUID());
		}
	}

	void forget(UUID player) {
		this.lastMates.removeInt(player);
		this.toldOperator.removeBoolean(player);
	}

	private void all(CustomPacketPayload payload) {
		for (ServerPlayer p : this.teams.server.getPlayerList().getPlayers()) {
			TeamsNet.send(p, payload);
		}
	}

	void hello(ServerPlayer p) {
		ServerInfo info = this.info(p);
		this.toldOperator.put(p.getUUID(), info.admin);
		TeamsNet.send(p, new TeamsNet.Hello(JsonWire.encode(info)));
	}

	void operators() {
		if (this.teams.live && !this.toldOperator.isEmpty()) {
			for (ServerPlayer p : this.teams.server.getPlayerList().getPlayers()) {
				UUID id = p.getUUID();
				if (this.toldOperator.containsKey(id) && this.toldOperator.getBoolean(id) != p.hasPermissions(2)) {
					this.hello(p);
				}
			}
		}
	}

	private void myTeam(ServerPlayer p) {
		TeamData t = this.teams.store.ofPlayer(p.getUUID());
		TeamsNet.send(p, new TeamsNet.MyTeam(t == null ? new byte[0] : this.pack(t)));
	}

	private void invites(ServerPlayer p) {
		UUID id = p.getUUID();
		List<InviteView> views = this.teams.invitesOf(id);
		UUID[] asked = this.teams.store.askedBy(id).toArray(new UUID[0]);
		TeamsNet.send(p, new TeamsNet.Invites(JsonWire.encode(views.toArray(new InviteView[0])), JsonWire.encode(asked)));
	}

	private byte[] pack(TeamData t) {
		LandRules own = t.land;
		t.land = this.teams.pins().effective(t);

		byte[] var3;
		try {
			var3 = JsonWire.encode(t);
		} finally {
			t.land = own;
		}

		return var3;
	}

	private byte[] directoryBytes() {
		return JsonWire.encode(this.summaries().toArray(new TeamSummary[0]));
	}

	List<TeamSummary> summaries() {
		List<TeamSummary> out = new ArrayList<>(this.teams.store.size() + 1);

		for (TeamData t : this.teams.store.all()) {
			out.add(TeamSummary.of(t, this.teams.onlineCount(t, null), this.teams.claims.count(t.id)));
		}

		TeamSummary land = this.serverLand();
		if (land != null) {
			out.add(land);
		}

		return out;
	}

	@Nullable
	TeamSummary serverLand() {
		int chunks = this.teams.claims.count(TeamSummary.SERVER_LAND);
		if (chunks <= 0) {
			return null;
		} else {
			TeamSummary s = TeamSummary.of(this.teams.serverLand.data, 0, chunks);
			s.server = true;
			return s;
		}
	}

	ServerInfo info(ServerPlayer p) {
		TeamsServerConfig c = this.teams.cfg();
		TeamData t = this.teams.store.ofPlayer(p.getUUID());
		ServerInfo i = new ServerInfo();
		i.maxMembers = c.maxMembers;
		i.maxAllies = c.maxAllies;
		i.maxWaypoints = c.maxWaypoints;
		i.claims = c.claims;
		i.soloLand = c.soloLand;
		i.maxClaims = this.teams.claimLimit(t);
		i.maxForceLoads = this.teams.forceLimit(t);
		i.claimDimensions = new ArrayList<>(c.claimDimensions == null ? List.of() : c.claimDimensions);
		i.claimReach = Math.max(0, c.claimReach);
		i.pinnedLandRules = new ArrayList<>(this.teams.pins().names());
		i.homeTeleport = c.homeTeleport;
		i.homeCooldownSeconds = c.homeCooldownSeconds;
		i.stash = c.stash;
		i.pings = c.pings;
		i.playersCreateTeams = c.playersCreateTeams;
		i.admin = p.hasPermissions(2);
		return i;
	}

	void teammates() {
		if (this.teams.live) {
			List<ServerPlayer> players = this.teams.server.getPlayerList().getPlayers();
			if (!players.isEmpty()) {
				Map<TeamData, List<ServerPlayer>> online = new IdentityHashMap<>();

				for (ServerPlayer p : players) {
					TeamData t = this.teams.teamOf(p.getUUID());
					if (t != null) {
						online.computeIfAbsent(t, k -> new ArrayList<>(4)).add(p);
					}
				}

				if (!online.isEmpty()) {
					boolean allies = this.teams.cfg().allyLocations;

					for (Entry<TeamData, List<ServerPlayer>> e : online.entrySet()) {
						TeamData t = e.getKey();
						List<TeammateState> states = new ArrayList<>(e.getValue().size());

						for (ServerPlayer px : e.getValue()) {
							if (states.size() < 512) {
								states.add(state(px, 0));
							}
						}

						if (allies) {
							for (UUID allyId : t.allies) {
								TeamData ally = this.teams.store.get(allyId);
								if (ally != null && ally.isAlly(t.id)) {
									List<ServerPlayer> theirs = online.get(ally);
									if (theirs != null) {
										for (ServerPlayer pxx : theirs) {
											if (states.size() < 512) {
												states.add(state(pxx, 4));
											}
										}
									}
								}
							}
						}

						TeamsNet.Teammates payload = null;

						for (ServerPlayer pxxx : e.getValue()) {
							if (TeamsNet.has(pxxx)) {
								int before = this.lastMates.put(pxxx.getUUID(), states.size());
								if (states.size() > 1 || before != states.size()) {
									if (payload == null) {
										payload = new TeamsNet.Teammates(states);
									}

									TeamsNet.send(pxxx, payload);
								}
							}
						}
					}
				}
			}
		}
	}

	private static TeammateState state(ServerPlayer p, int extraFlags) {
		int flags = extraFlags;
		if (p.isDeadOrDying()) {
			flags = extraFlags | 1;
		}

		if (p.isShiftKeyDown()) {
			flags |= 2;
		}

		return new TeammateState(
			p.getUUID(),
			p.level().dimension().location().toString(),
			(float)p.getX(),
			(float)p.getY(),
			(float)p.getZ(),
			p.getYRot(),
			p.getHealth(),
			p.getMaxHealth(),
			p.getArmorValue(),
			p.getFoodData().getFoodLevel(),
			flags
		);
	}

	static TeamsNet.Notice notice(String key, int color, Object... args) {
		List<String> out = new ArrayList<>(args.length);

		for (Object a : args) {
			if (out.size() >= 8) {
				break;
			}

			String s = String.valueOf(a);
			out.add(s.length() > 256 ? s.substring(0, 256) : s);
		}

		return new TeamsNet.Notice(key, out, color & 16777215);
	}
}
