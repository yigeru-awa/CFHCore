package com.onehumanawa.cfhcore.content.gui.teams.compat;

import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.client.DevHarness;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.TeamsConfig;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.model.ClaimsUpdate;
import com.onehumanawa.cfhcore.content.gui.teams.model.PingData;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeammateState;
import com.onehumanawa.cfhcore.content.gui.teams.net.TeamsNet;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.ChunkPos;

final class MapDev {
	static void init() {
		if (CFHGuiPlatform.get().isDevelopmentEnvironment() || DevHarness.sampleData() || Boolean.getBoolean("cfh.teams.forcePreview")) {
			CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:dev/xaero_map", p -> map("xaeroworldmap", p));
			CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:dev/journeymap_map", p -> map("journeymap", p));
			CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:dev/map_ping", p -> {
				ping();
				return p;
			});
			CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:dev/map_off", p -> {
				switches(false);
				return p;
			});
			CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:dev/map_on", p -> {
				switches(true);
				return p;
			});
			CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:dev/map_feed", p -> {
				feed(false);
				return p;
			});
			CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:dev/map_feed_more", p -> {
				feed(true);
				return p;
			});
			CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:dev/map_state", p -> {
				CFHGuiTeams.LOGGER.info("[CFHGui Teams] maps: {}", TeamsCompat.describe());
				return p;
			});
		}
	}

	private static Screen map(String id, Screen parent) {
		try {
			MapBridge bridge = TeamsCompat.bridge(id);
			Screen map = bridge == null ? null : bridge.mapScreen();
			if (map != null) {
				return map;
			}
		} catch (Throwable var4) {
			CFHGuiTeams.LOGGER.warn("[CFHGui Teams] the map screen of {} could not be opened: {}", id, var4.toString());
		}

		return parent;
	}

	private static void switches(boolean on) {
		TeamsConfig cfg = TeamsConfig.get();
		cfg.mapTeammates = on;
		cfg.mapClaims = on;
		cfg.mapWaypoints = on;
	}

	private static void ping() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			UUID by = TeamsState.self();
			Iterator var2 = TeamsState.teammates().iterator();
			if (var2.hasNext()) {
				TeammateState s = (TeammateState)var2.next();
				by = s.id();
			}

			TeamsState.onPing(
				new TeamsNet.Ping(new PingData(by, TeamsState.currentDimension(), mc.player.getX() + 22.0, mc.player.getY(), mc.player.getZ() - 14.0, 1, -1))
			);
		}
	}

	private static UUID id(String name) {
		return UUID.nameUUIDFromBytes(("SlateMapHarness:" + name).getBytes(StandardCharsets.UTF_8));
	}

	private static void feed(boolean more) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			String dimension = TeamsState.currentDimension();
			ChunkPos at = mc.player.chunkPosition();
			UUID guild = id("guild");
			UUID rivals = id("rivals");
			UUID mate = id("Mossback");
			TeamData own = new TeamData();
			own.id = guild;
			own.name = "Harness Guild";
			own.tag = "HG";
			own.color = 6080680;
			TeamData.Member me = new TeamData.Member();
			me.id = TeamsState.self();
			me.name = mc.getUser().getName();
			me.rank = Rank.OWNER;
			own.members.add(me);
			TeamData.Member other = new TeamData.Member();
			other.id = mate;
			other.name = "Mossback";
			own.members.add(other);
			TeamData.Waypoint camp = new TeamData.Waypoint();
			camp.id = "camp";
			camp.name = more ? "Lookout" : "Camp";
			camp.dimension = dimension;
			camp.x = mc.player.getBlockX() - 20 + (more ? 12 : 0);
			camp.y = mc.player.getBlockY();
			camp.z = mc.player.getBlockZ() + 12;
			camp.icon = more ? "compass" : "home";
			camp.color = more ? 6007008 : 14722136;
			own.waypoints.add(camp);
			TeamData.Place home = new TeamData.Place();
			home.dimension = dimension;
			home.x = (double)mc.player.getBlockX() + 12.5;
			home.y = (double)mc.player.getBlockY();
			home.z = (double)mc.player.getBlockZ() - 9.5;
			own.home = home;
			TeamSummary second = new TeamSummary();
			second.id = rivals;
			second.name = "Rivals";
			second.tag = "RIV";
			second.color = 15026253;
			List<Long> chunks = new ArrayList<>();
			List<Integer> owners = new ArrayList<>();
			List<Long> removed = new ArrayList<>();

			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= (more ? 2 : 1); dz++) {
					chunks.add(ChunkPos.asLong(at.x + dx, at.z + dz));
					owners.add(0);
				}
			}

			for (int dx = 2; dx <= 4; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx != 3 || dz != 0) {
						if (more) {
							removed.add(ChunkPos.asLong(at.x + dx, at.z + dz));
						} else {
							chunks.add(ChunkPos.asLong(at.x + dx, at.z + dz));
							owners.add(1);
						}
					}
				}
			}

			byte[] flags = new byte[chunks.size()];

			for (int i = 0; i < flags.length; i++) {
				if (chunks.get(i) == at.toLong()) {
					flags[i] = 1;
				}
			}

			TeamsState.onDirectory(new TeamsNet.Directory(JsonWire.encode(new TeamSummary[]{TeamSummary.of(own, 2, 9), second})));
			TeamsState.onMyTeam(new TeamsNet.MyTeam(JsonWire.encode(own)));
			TeamsState.onClaims(
				new TeamsNet.Claims(
					new ClaimsUpdate(
						dimension,
						!more,
						List.of(guild, rivals),
						chunks.stream().mapToLong(Long::longValue).toArray(),
						owners.stream().mapToInt(Integer::intValue).toArray(),
						flags,
						removed.stream().mapToLong(Long::longValue).toArray()
					)
				)
			);
			TeamsState.onTeammates(
				new TeamsNet.Teammates(
					List.of(
						new TeammateState(mate, dimension, (float)mc.player.getX() + 26.0F, (float)mc.player.getY(), (float)mc.player.getZ() + 9.0F, 0.0F, 20.0F, 20.0F, 0, 20, 0)
					)
				)
			);
		}
	}

	private MapDev() {
	}
}
