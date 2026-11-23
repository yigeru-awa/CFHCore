package com.onehumanawa.cfhcore.content.gui.teams.compat;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.teams.TeamsConfig;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.client.WaypointIcons;
import com.onehumanawa.cfhcore.content.gui.teams.model.PingData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeammateState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class MapMarks {
	private static final int ALLY_RGB = 6007008;
	private static final Map<String, MapMark> BY_KEY = new HashMap<>();
	private static List<MapMark> marks = List.of();
	private static String dimensionNow = "minecraft:overworld";
	private static long toldAt;
	private static long toldBefore;
	private static boolean cleared;

	public static List<MapMark> marks() {
		return marks;
	}

	public static String dimensionNow() {
		return dimensionNow;
	}

	static boolean update() {
		TeamsConfig cfg = TeamsConfig.get();
		TeamData team = TeamsState.team();
		dimensionNow = TeamsState.currentDimension();
		List<MapMark> next = new ArrayList<>();
		if (cfg.mapWaypoints && team != null) {
			for (TeamData.Waypoint w : team.waypoints) {
				MapMark m = mark(MapMark.Kind.WAYPOINT, "waypoint:" + w.id);
				m.moveTo(w.dimension, (double)w.x + 0.5, (double)w.y, (double)w.z + 0.5, 0L);
				m.color = w.color & 16777215;
				m.icon = WaypointIcons.get(w.icon);
				if (!w.name.equals(m.label.getString())) {
					m.label = Component.literal(w.name);
				}

				next.add(m);
			}
		}

		if (cfg.mapTeammates) {
			ClientLevel level = Minecraft.getInstance().level;
			long told = TeamsState.teammatesAt();
			boolean fresh = told != toldAt;
			if (fresh) {
				toldBefore = toldAt;
				toldAt = told;
			}

			long glide = toldBefore <= 0L ? 0L : Mth.clamp(toldAt - toldBefore, 250L, 1000L);

			for (TeammateState s : TeamsState.teammates()) {
				String key = "mate:" + s.id();
				boolean known = BY_KEY.containsKey(key);
				MapMark m = mark(MapMark.Kind.TEAMMATE, key);
				if (!known || fresh) {
					m.moveTo(s.dimension(), (double)s.x(), (double)s.y(), (double)s.z(), known ? glide : 0L);
				}

				m.player = s.id();
				m.entity = level != null && dimensionNow.equals(s.dimension()) ? level.getPlayerByUUID(s.id()) : null;
				m.ally = s.ally();
				m.dead = s.dead();
				String name = name(team, s.id());
				if (!name.equals(m.label.getString())) {
					m.label = Component.literal(name);
				}

				m.color = s.ally() ? allyColor(s.id()) : (team == null ? 6007008 : team.color & 16777215);
				next.add(m);
			}
		}

		if (cfg.mapWaypoints) {
			for (TeamsState.ActivePing a : TeamsState.pings()) {
				PingData p = a.ping();
				String keyx = "ping:" + p.by();
				MapMark mx = BY_KEY.get(keyx);
				if (mx == null || mx.bornMs != a.bornMs()) {
					mx = new MapMark(MapMark.Kind.PING, keyx);
					BY_KEY.put(keyx, mx);
					mx.moveTo(p.dimension(), p.x(), p.y(), p.z(), 0L);
					mx.bornMs = a.bornMs();
					mx.lifeMs = 9000L;
					mx.player = p.by();
					mx.color = pingColor(p.kind());
					mx.icon = pingIcon(p.kind());
					String who = name(team, p.by());
					Component what = Component.translatable("cfhgui_teams.ping." + PingData.kindKey(p.kind()));
					mx.label = (Component)(who.isEmpty() ? what : Component.translatable("cfhgui_teams.map.ping", new Object[]{who, what}));
				}

				next.add(mx);
			}
		}

		boolean changed = cleared || next.size() != marks.size();
		cleared = false;

		for (int i = 0; !changed && i < next.size(); i++) {
			changed = next.get(i) != marks.get(i);
		}

		if (changed) {
			BY_KEY.clear();

			for (MapMark mx : next) {
				BY_KEY.put(mx.key, mx);
			}

			marks = List.copyOf(next);
		}

		return changed;
	}

	private static MapMark mark(MapMark.Kind kind, String key) {
		return BY_KEY.computeIfAbsent(key, k -> new MapMark(kind, k));
	}

	private static String name(@Nullable TeamData team, UUID id) {
		if (team != null) {
			TeamData.Member m = team.member(id);
			if (m != null && !m.name.isEmpty()) {
				return m.name;
			}
		}

		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection != null) {
			PlayerInfo info = connection.getPlayerInfo(id);
			if (info != null && info.getProfile().getName() != null) {
				return info.getProfile().getName();
			}
		}

		for (TeamSummary s : TeamsState.directory()) {
			if (id.equals(s.owner)) {
				return s.ownerName;
			}
		}

		return "";
	}

	private static int allyColor(UUID id) {
		for (TeamSummary s : TeamsState.directory()) {
			if (id.equals(s.owner)) {
				return s.color & 16777215;
			}
		}

		return 6007008;
	}

	public static Icon pingIcon(int kind) {
		return switch (kind) {
			case 1 -> Icon.WARNING;
			case 2 -> Icon.STAR_FILLED;
			case 3 -> Icon.SEND;
			case 4 -> Icon.BELL;
			default -> Icon.PIN;
		};
	}

	public static int pingColor(int kind) {
		Palette p = Palette.dark(-7726052);

		return 16777215 & switch (kind) {
			case 1 -> p.danger();
			case 2 -> p.warning();
			case 3 -> p.success();
			case 4 -> 15034762;
			default -> 6007008;
		};
	}

	static void clear() {
		if (!marks.isEmpty()) {
			cleared = true;
		}

		BY_KEY.clear();
		marks = List.of();
		toldAt = 0L;
		toldBefore = 0L;
	}

	private MapMarks() {
	}
}
