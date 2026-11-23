package com.onehumanawa.cfhcore.content.gui.server.client.map;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.map.MapAction;
import com.onehumanawa.cfhcore.content.gui.core.map.MapPoint;
import com.onehumanawa.cfhcore.content.gui.core.map.MapPoints;
import com.onehumanawa.cfhcore.content.gui.core.map.MapSpots;
import com.onehumanawa.cfhcore.content.gui.core.map.MapPoints.Provider;
import com.onehumanawa.cfhcore.content.gui.core.map.MapSpots.Spot;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.ServerClientConfig;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.dialog.Forms;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.server.model.Home;
import com.onehumanawa.cfhcore.content.gui.server.model.Place;
import com.onehumanawa.cfhcore.content.gui.server.model.SelfState;
import com.onehumanawa.cfhcore.content.gui.server.model.Warp;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class ServerMaps implements Provider, com.onehumanawa.cfhcore.content.gui.core.map.MapSpots.Provider {
	private static final String HOME = "home:";
	private static final String WARP = "warp:";
	private static final String SPAWN = "spawn:spawn";
	private static final String DEATH = "death:last";
	private static final int NEAR = 6;
	private static final ServerMaps INSTANCE = new ServerMaps();
	private static boolean initialised;
	private List<MapPoint> points = List.of();

	public static synchronized void init() {
		if (!initialised) {
			initialised = true;
			MapPoints.register(INSTANCE);
			MapSpots.register(INSTANCE);
			ServerState.SELF_CHANGED.register(ServerMaps::refresh);
			ServerState.WARPS_CHANGED.register(ServerMaps::refresh);
			ServerState.HELLO_CHANGED.register(ServerMaps::refresh);
			refresh();
		}
	}

	public static void refresh() {
		List<MapPoint> now = INSTANCE.make();
		if (!now.equals(INSTANCE.points)) {
			INSTANCE.points = now;
			MapPoints.changed();
		}
	}

	private List<MapPoint> make() {
		List<MapPoint> out = new ArrayList<>();
		if (ServerState.available() && !ServerState.newerServer()) {
			ServerClientConfig config = ServerClientConfig.get();
			SelfState self = ServerState.self();
			if (config.mapHomes && ServerState.feature("homes")) {
				for (Home h : self.homes) {
					out.add(point("home:" + h.name, "homes", Component.literal(h.name), h.place, h.color, glyph(h.icon, Icon.HOME)));
				}
			}

			if (config.mapWarps && ServerState.feature("warps")) {
				for (Warp w : ServerState.warps().warps) {
					out.add(point("warp:" + w.name, "warps", Component.literal(w.name), w.place, w.color, glyph(w.icon, Icon.PIN)));
				}
			}

			Place spawn = ServerState.warps().spawn;
			if (config.mapSpawn && spawn != null && ServerState.feature("spawn")) {
				out.add(point("spawn:spawn", "spawn", Txt.t("map.spawn"), spawn, 7323486, Icon.WORLD));
			}

			if (config.mapDeath && self.death != null) {
				out.add(point("death:last", "deaths", Txt.t("map.death"), self.death, 14707787, Icon.CLOSE));
			}

			return out;
		} else {
			return out;
		}
	}

	private static MapPoint point(String id, String group, Component label, Place p, int rgb, Icon icon) {
		return new MapPoint(id, group, label, p.dimension, p.x, p.y, p.z, rgb & 16777215, icon, null);
	}

	private static Icon glyph(String mark, Icon fallback) {
		Icon icon = Ui.glyph(mark);
		return icon == null ? fallback : icon;
	}

	public String id() {
		return "cfhgui_server";
	}

	public List<MapPoint> points() {
		return this.points;
	}

	public Component groupName(String group) {
		return switch (group) {
			case "homes" -> Txt.t("map.group.homes");
			case "warps" -> Txt.t("map.group.warps");
			case "spawn" -> Txt.t("map.group.spawn");
			case "deaths" -> Txt.t("map.group.deaths");
			default -> Component.literal(group);
		};
	}

	public List<MapAction> actions(MapPoint point) {
		List<MapAction> out = new ArrayList<>();
		String id = point.id();
		if (id.startsWith("home:")) {
			String name = id.substring("home:".length());
			Home home = home(name);
			if (home == null) {
				return out;
			}

			if (ServerState.may(Nodes.HOME_USE)) {
				out.add(MapAction.of("teleport", Txt.t("map.teleport"), Icon.ARROW_RIGHT, () -> ServerActions.homeGo(name, null)));
			}

			if (ServerState.may(Nodes.HOME_SET)) {
				out.add(MapAction.of("edit", Txt.t("home.edit"), Icon.EDIT, () -> new Forms.HomeForm(home).show()));
				out.add(
					MapAction.danger(
						"delete",
						Txt.t("form.delete"),
						Icon.TRASH,
						() -> Forms.confirmDelete(Txt.t("home.delete.title", name), Txt.t("home.delete.body"), () -> ServerActions.homeDelete(name, null, null))
					)
				);
			}
		} else if (id.startsWith("warp:")) {
			String namex = id.substring("warp:".length());
			Warp warp = warp(namex);
			if (warp == null) {
				return out;
			}

			if (ServerState.may(Nodes.WARP_USE)) {
				out.add(MapAction.of("teleport", Txt.t("map.teleport"), Icon.ARROW_RIGHT, () -> ServerActions.warpGo(namex)));
			}

			if (ServerState.may(Nodes.WARP_SET)) {
				out.add(MapAction.of("edit", Txt.t("warp.edit"), Icon.EDIT, () -> new Forms.WarpForm(warp).show()));
				out.add(
					MapAction.danger(
						"delete",
						Txt.t("form.delete"),
						Icon.TRASH,
						() -> Forms.confirmDelete(Txt.t("warp.delete.title", namex), Txt.t("warp.delete.body"), () -> ServerActions.warpDelete(namex, null))
					)
				);
			}
		} else if (id.equals("spawn:spawn")) {
			if (ServerState.may(Nodes.SPAWN_USE)) {
				out.add(MapAction.of("teleport", Txt.t("map.teleport"), Icon.ARROW_RIGHT, ServerActions::spawnGo));
			}
		} else if (id.equals("death:last")) {
			SelfState self = ServerState.self();
			if (self.backIsDeath && self.back != null && ServerState.feature("back") && ServerState.may(Nodes.BACK_USE) && ServerState.may(Nodes.BACK_DEATH)) {
				out.add(MapAction.of("teleport", Txt.t("map.back"), Icon.UNDO, ServerActions::back));
			}
		}

		return out;
	}

	@Nullable
	private static Home home(String name) {
		for (Home h : ServerState.self().homes) {
			if (h.name.equals(name)) {
				return h;
			}
		}

		return null;
	}

	@Nullable
	private static Warp warp(String name) {
		for (Warp w : ServerState.warps().warps) {
			if (w.name.equals(name)) {
				return w;
			}
		}

		return null;
	}

	public List<MapAction> at(Spot spot) {
		List<MapAction> out = new ArrayList<>();
		if (ServerState.available() && !ServerState.newerServer()) {
			if (ServerState.may(Nodes.TP_POSITION)) {
				out.add(
					MapAction.of(
						"teleport",
						Txt.t("map.teleport_here"),
						Icon.ARROW_RIGHT,
						() -> ServerActions.tpPosition(spot.dimension(), (double)spot.x() + 0.5, spot.hasY() ? (double)spot.y() : null, (double)spot.z() + 0.5)
					)
				);
			}

			if (!near(spot)) {
				return out;
			} else {
				if (ServerState.feature("homes") && ServerState.may(Nodes.HOME_SET)) {
					int max = ServerState.number(Nodes.HOMES_MAX);
					MapAction set = MapAction.of("home", Txt.t("home.set_here"), Icon.HOME, () -> new Forms.HomeForm(null).show());
					out.add(ServerState.self().homes.size() >= max ? set.disabled(Txt.t("home.full", max)) : set);
				}

				if (ServerState.feature("warps") && ServerState.may(Nodes.WARP_SET)) {
					out.add(MapAction.of("warp", Txt.t("warp.create_here"), Icon.PIN, () -> new Forms.WarpForm(null).show()));
				}

				return out;
			}
		} else {
			return out;
		}
	}

	private static boolean near(Spot spot) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		return p != null && mc.level != null && mc.level.dimension().location().toString().equals(spot.dimension())
			? Math.abs(p.getX() - ((double)spot.x() + 0.5)) <= 6.0 && Math.abs(p.getZ() - ((double)spot.z() + 0.5)) <= 6.0
			: false;
	}

	private ServerMaps() {
	}
}
