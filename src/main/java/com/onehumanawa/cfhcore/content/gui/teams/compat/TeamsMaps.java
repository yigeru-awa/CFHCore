package com.onehumanawa.cfhcore.content.gui.teams.compat;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.map.MapAction;
import com.onehumanawa.cfhcore.content.gui.core.map.MapAreas;
import com.onehumanawa.cfhcore.content.gui.core.map.MapPoint;
import com.onehumanawa.cfhcore.content.gui.core.map.MapPoints;
import com.onehumanawa.cfhcore.content.gui.core.map.MapSpots;
import com.onehumanawa.cfhcore.content.gui.core.map.MapAreas.Area;
import com.onehumanawa.cfhcore.content.gui.core.map.MapAreas.Provider;
import com.onehumanawa.cfhcore.content.gui.core.map.MapSpots.Spot;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamActions;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.model.Permission;
import com.onehumanawa.cfhcore.content.gui.teams.model.ServerInfo;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.net.TeamsNet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

final class TeamsMaps implements Provider, com.onehumanawa.cfhcore.content.gui.core.map.MapSpots.Provider, com.onehumanawa.cfhcore.content.gui.core.map.MapPoints.Provider {
	private static final String GROUP = "team";
	private static final String HOME_ID = "team:home";
	private static final double HERE = 8.0;
	private static final int MAX_SELECTION = 4096;
	private static final int WAYPOINT_RGB = (new TeamData.Waypoint()).color;
	private List<MapPoint> points = List.of();
	@Nullable
	private TeamData seenTeam;
	private boolean seen;
	private static final TeamsMaps INSTANCE = new TeamsMaps();

	static void init() {
		MapAreas.register(INSTANCE);
		MapSpots.register(INSTANCE);
		MapPoints.register(INSTANCE);
	}

	static void tick() {
		INSTANCE.refresh();
	}

	public String id() {
		return "cfhgui_teams";
	}

	private static Component t(String key, Object... args) {
		return Component.translatable("cfhgui_teams.map." + key, args);
	}

	private static String ofLand(String key) {
		return TeamsState.hasTeam() ? key : key + ".own";
	}

	@Nullable
	public Area at(String dimension, int chunkX, int chunkZ) {
		MapClaims.Team team = MapClaims.of(dimension).at(chunkX, chunkZ);
		return team == null ? null : team.area;
	}

	public long revision(String dimension) {
		return MapClaims.revision(dimension);
	}

	public List<MapPoint> points() {
		return this.points;
	}

	private void refresh() {
		TeamData team = TeamsState.team();
		if (!this.seen || team != this.seenTeam) {
			this.seen = true;
			this.seenTeam = team;
			TeamData.Place home = team == null ? null : team.home;
			List<MapPoint> next = home == null
				? List.of()
				: List.of(new MapPoint("team:home", "team", t("home"), home.dimension, home.x, home.y, home.z, team.color & 16777215, Icon.HOME, null));
			if (!next.equals(this.points)) {
				this.points = next;
				MapPoints.changed();
			}
		}
	}

	public Component groupName(String group) {
		return t("group");
	}

	public List<MapAction> actions(MapPoint point) {
		if ("team:home".equals(point.id()) && TeamsState.hasTeam()) {
			List<MapAction> out = new ArrayList<>();
			MapAction go = MapAction.of("teleport", t("home.teleport"), Icon.SEND, () -> {
				TeamActions.goHome();
				Minecraft.getInstance().setScreen(null);
			});
			out.add(TeamsState.info().homeTeleport ? go : go.disabled(t("home.teleport.off")));
			if (TeamsState.may(Permission.HOME)) {
				out.add(
					MapAction.danger(
						"delete",
						t("home.clear"),
						Icon.TRASH,
						() -> CFHGuiModal.confirmDanger(t("home.clear.title"), t("home.clear.body"), t("home.clear"), TeamActions::clearHome)
					)
				);
			}

			return out;
		} else {
			return List.of();
		}
	}

	public List<MapAction> at(Spot spot) {
		TeamData team = TeamsState.team();
		TeamData land = TeamsState.land();
		if (TeamsState.available() && (team != null || land != null || TeamsState.soloLand())) {
			ServerInfo info = TeamsState.info();
			String dimension = spot.dimension();
			List<MapAction> out = new ArrayList<>();
			if (claimable(info, dimension)) {
				long chunk = ChunkPos.asLong(spot.chunkX(), spot.chunkZ());
				TeamsState.Claim claim = TeamsState.claimAt(dimension, spot.chunkX(), spot.chunkZ());
				if (claim == null) {
					MapAction take = MapAction.of("claim", t("spot.claim"), Icon.PLUS, () -> TeamActions.claim(dimension, List.of(chunk), told("claim", 1)));
					TeamsMaps.Reach reach = TeamsMaps.Reach.of(info, dimension);
					out.add(
						TeamsState.claimCount(land == null ? null : land.id) >= info.maxClaims
							? take.disabled(t(ofLand("spot.claim.limit"), info.maxClaims))
							: (reach.has(spot.chunkX(), spot.chunkZ()) ? take : take.disabled(reach.why()))
					);
				} else if (TeamsState.isOwn(claim.team())) {
					if (claim.forced()) {
						out.add(MapAction.of("force_load", t("spot.unload"), Icon.UNLOCK, () -> TeamActions.forceLoad(dimension, List.of(chunk), false, told("unload", 1))));
					} else if (info.maxForceLoads > 0) {
						MapAction keep = MapAction.of("force_load", t("spot.load"), Icon.LOCK, () -> TeamActions.forceLoad(dimension, List.of(chunk), true, told("load", 1)));
						out.add(
							TeamsState.forcedCount(land == null ? null : land.id) < info.maxForceLoads ? keep : keep.disabled(t(ofLand("spot.load.limit"), info.maxForceLoads))
						);
					}

					out.add(MapAction.danger("unclaim", t("spot.unclaim"), Icon.TRASH, () -> TeamActions.unclaim(dimension, List.of(chunk), told("unclaim", 1))));
				}
			}

			if (team == null) {
				return out;
			} else {
				if (TeamsState.may(Permission.HOME)) {
					MapAction home = MapAction.of("team_home", t("spot.home"), Icon.HOME, TeamActions::setHome);
					out.add(standsAt(spot) ? home : home.disabled(t("spot.home.far")));
				}

				int y = height(spot);
				MapAction waypoint = MapAction.of(
					"team_waypoint", t("spot.waypoint"), Icon.PIN, () -> CFHGuiModal.prompt(t("spot.waypoint.title"), t("spot.waypoint.name"), "", name -> {
							String named = name != null && !name.isBlank() ? name.trim() : t("spot.waypoint.default").getString();
							TeamActions.addWaypoint(named, spot.x(), y, spot.z(), dimension, WAYPOINT_RGB, "flag", null);
						})
				);
				out.add(team.waypoints.size() < info.maxWaypoints ? waypoint : waypoint.disabled(t("spot.waypoint.limit", info.maxWaypoints)));
				if (info.pings && dimension.equals(TeamsState.currentDimension())) {
					out.add(MapAction.of("team_ping", t("spot.ping"), Icon.PING, () -> TeamActions.ping((double)spot.x() + 0.5, (double)y, (double)spot.z() + 0.5, 0, -1)));
				}

				return out;
			}
		} else {
			return List.of();
		}
	}

	public List<MapAction> in(com.onehumanawa.cfhcore.content.gui.core.map.MapSpots.Area area) {
		TeamData land = TeamsState.land();
		if (TeamsState.available() && (land != null || TeamsState.soloLand()) && area.chunks() <= 4096) {
			ServerInfo info = TeamsState.info();
			String dimension = area.dimension();
			if (!claimable(info, dimension)) {
				return List.of();
			} else {
				TeamsMaps.Reach reach = TeamsMaps.Reach.of(info, dimension);
				List<Long> free = new ArrayList<>();
				List<Long> own = new ArrayList<>();
				int near = 0;

				for (int x = area.minChunkX(); x <= area.maxChunkX(); x++) {
					for (int z = area.minChunkZ(); z <= area.maxChunkZ(); z++) {
						TeamsState.Claim claim = TeamsState.claimAt(dimension, x, z);
						if (claim == null) {
							free.add(ChunkPos.asLong(x, z));
							if (reach.has(x, z)) {
								near++;
							}
						} else if (TeamsState.isOwn(claim.team())) {
							own.add(ChunkPos.asLong(x, z));
						}
					}
				}

				List<MapAction> out = new ArrayList<>();
				if (!free.isEmpty()) {
					MapAction take = MapAction.of(
						"claim", t("area.claim", TeamsText.chunks(free.size())), Icon.PLUS, () -> TeamActions.claim(dimension, free, told("claim", free.size()))
					);
					int left = info.maxClaims - TeamsState.claimCount(land == null ? null : land.id);
					out.add(near == 0 ? take.disabled(reach.why()) : (near <= left ? take : take.disabled(t(ofLand("area.claim.limit"), TeamsText.chunks(Math.max(0, left))))));
				}

				if (!own.isEmpty()) {
					out.add(
						MapAction.danger(
							"unclaim",
							t("area.unclaim", TeamsText.chunks(own.size())),
							Icon.TRASH,
							() -> CFHGuiModal.confirmDanger(
									t("area.unclaim.title"),
									t(ofLand("area.unclaim.body"), TeamsText.chunks(own.size())),
									t("area.unclaim.ok"),
									() -> TeamActions.unclaim(dimension, own, told("unclaim", own.size()))
								)
						)
					);
				}

				return out;
			}
		} else {
			return List.of();
		}
	}

	private static Consumer<TeamsNet.Result> told(String deed, int asked) {
		return result -> {
			if (!result.ok()) {
				CFHGuiToasts.show(TeamsText.shared("result.failed"), TeamActions.describe(result), Icon.WARNING);
			} else {
				int done = asked;
				int skipped = 0;

				try {
					if (!result.args().isEmpty()) {
						done = Integer.parseInt(result.args().get(0).trim());
					}

					if (result.args().size() > 1) {
						skipped = Integer.parseInt(result.args().get(1).trim());
					}
				} catch (NumberFormatException var6) {
				}

				Component words = skipped > 0
					? TeamsText.t("map.done." + deed + ".skipped", TeamsText.chunks(done), skipped)
					: TeamsText.t("map.done." + deed, TeamsText.chunks(done));
				CFHGuiToasts.show(TeamsText.page("land"), words, skipped > 0 ? Icon.INFO : Icon.CHECK);
			}
		};
	}

	private static boolean claimable(ServerInfo info, String dimension) {
		return info.claims && TeamsState.mayClaim() && (info.claimDimensions == null || info.claimDimensions.isEmpty() || info.claimDimensions.contains(dimension));
	}

	private static boolean standsAt(Spot spot) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null && spot.dimension().equals(TeamsState.currentDimension())) {
			double dx = (double)spot.x() + 0.5 - player.getX();
			double dz = (double)spot.z() + 0.5 - player.getZ();
			return dx * dx + dz * dz <= 64.0;
		} else {
			return false;
		}
	}

	private static int height(Spot spot) {
		if (spot.hasY()) {
			return spot.y();
		} else {
			LocalPlayer player = Minecraft.getInstance().player;
			return player != null && spot.dimension().equals(TeamsState.currentDimension()) ? player.getBlockY() : 64;
		}
	}

	private TeamsMaps() {
	}

	private static record Reach(boolean bound, boolean here, int chunkX, int chunkZ, int chunks) {
		static TeamsMaps.Reach of(ServerInfo info, String dimension) {
			LocalPlayer player = Minecraft.getInstance().player;
			if (!info.admin && info.claimReach >= 0 && player != null) {
				ChunkPos stands = player.chunkPosition();
				return new TeamsMaps.Reach(true, dimension.equals(TeamsState.currentDimension()), stands.x, stands.z, info.claimReach);
			} else {
				return new TeamsMaps.Reach(false, true, 0, 0, 0);
			}
		}

		boolean has(int x, int z) {
			return !this.bound || this.here && Math.max(Math.abs(x - this.chunkX), Math.abs(z - this.chunkZ)) <= this.chunks;
		}

		Component why() {
			return this.here ? TeamsMaps.t("claim.far", TeamsText.chunks(this.chunks)) : TeamsMaps.t("claim.elsewhere");
		}
	}
}
