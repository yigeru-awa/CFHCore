package com.onehumanawa.cfhcore.content.gui.teams.client.ui.settings;

import com.onehumanawa.cfhcore.content.gui.teams.TeamsConfig;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class TeamsSettings {
	private static TeamsConfig cfg() {
		return TeamsConfig.get();
	}

	public static List<TeamsSettings.Group> groups() {
		TeamsConfig d = new TeamsConfig();
		List<TeamsSettings.Group> out = new ArrayList<>();
		List<TeamsSettings.Setting> party = new ArrayList<>();
		party.add(bool("partyFrames", () -> cfg().partyFrames, v -> cfg().partyFrames = v, d.partyFrames));
		party.add(
			choice("partyAnchor", () -> cfg().partyAnchor, v -> cfg().partyAnchor = v, d.partyAnchor, List.of("TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT"))
		);
		party.add(
			number(
				"partyScale",
				() -> cfg().partyScale,
				v -> cfg().partyScale = v,
				d.partyScale,
				0.5,
				2.0,
				0.05,
				v -> Component.literal(String.format(Locale.ROOT, "%.2f×", ((Number)v).doubleValue()))
			)
		);
		party.add(integer("partyMax", () -> cfg().partyMax, v -> cfg().partyMax = v, d.partyMax, 1, 16));
		party.add(bool("partyShowHealth", () -> cfg().partyShowHealth, v -> cfg().partyShowHealth = v, d.partyShowHealth));
		party.add(bool("partyShowDistance", () -> cfg().partyShowDistance, v -> cfg().partyShowDistance = v, d.partyShowDistance));
		party.add(integer("partyX", () -> cfg().partyX, v -> cfg().partyX = v, d.partyX, 0, 400));
		party.add(integer("partyY", () -> cfg().partyY, v -> cfg().partyY = v, d.partyY, 0, 400));
		out.add(new TeamsSettings.Group("party", party));
		List<TeamsSettings.Setting> world = new ArrayList<>();
		world.add(bool("worldMarkers", () -> cfg().worldMarkers, v -> cfg().worldMarkers = v, d.worldMarkers));
		world.add(integer("markerMinDistance", () -> cfg().markerMinDistance, v -> cfg().markerMinDistance = v, d.markerMinDistance, 0, 64));
		world.add(bool("worldWaypoints", () -> cfg().worldWaypoints, v -> cfg().worldWaypoints = v, d.worldWaypoints));
		world.add(bool("claimBorders", () -> cfg().claimBorders, v -> cfg().claimBorders = v, d.claimBorders));
		world.add(bool("landNotice", () -> cfg().landNotice, v -> cfg().landNotice = v, d.landNotice));
		out.add(new TeamsSettings.Group("world", world));
		List<TeamsSettings.Setting> notices = new ArrayList<>();
		notices.add(bool("notices", () -> cfg().notices, v -> cfg().notices = v, d.notices));
		notices.add(bool("pingSound", () -> cfg().pingSound, v -> cfg().pingSound = v, d.pingSound));
		notices.add(bool("inventoryButton", () -> cfg().inventoryButton, v -> cfg().inventoryButton = v, d.inventoryButton));
		out.add(new TeamsSettings.Group("notices", notices));
		List<TeamsSettings.Setting> maps = new ArrayList<>();
		maps.add(bool("mapTeammates", () -> cfg().mapTeammates, v -> cfg().mapTeammates = v, d.mapTeammates));
		maps.add(bool("mapClaims", () -> cfg().mapClaims, v -> cfg().mapClaims = v, d.mapClaims));
		maps.add(bool("mapWaypoints", () -> cfg().mapWaypoints, v -> cfg().mapWaypoints = v, d.mapWaypoints));
		maps.add(
			number(
				"mapClaimOpacity",
				() -> cfg().mapClaimOpacity,
				v -> cfg().mapClaimOpacity = v,
				d.mapClaimOpacity,
				0.1,
				0.9,
				0.05,
				v -> Component.literal(Math.round(((Number)v).doubleValue() * 100.0) + "%")
			)
		);
		out.add(new TeamsSettings.Group("maps", maps));
		return out;
	}

	public static int count() {
		int n = 0;

		for (TeamsSettings.Group g : groups()) {
			n += g.settings().size();
		}

		return n;
	}

	private static void save() {
		TeamsConfig.save();
	}

	private static TeamsSettings.Setting bool(String id, Supplier<Boolean> get, Consumer<Boolean> set, boolean def) {
		return new TeamsSettings.Setting(id, TeamsSettings.Type.BOOL, get::get, v -> {
			set.accept(Boolean.TRUE.equals(v));
			save();
		}, def, 0.0, 0.0, 0.0, List.of(), null);
	}

	private static TeamsSettings.Setting integer(String id, Supplier<Integer> get, Consumer<Integer> set, int def, int min, int max) {
		return new TeamsSettings.Setting(id, TeamsSettings.Type.INT, get::get, v -> {
			set.accept((int)Math.max((long)min, Math.min((long)max, Math.round(((Number)v).doubleValue()))));
			save();
		}, def, (double)min, (double)max, 1.0, List.of(), null);
	}

	private static TeamsSettings.Setting number(
		String id, Supplier<Double> get, Consumer<Double> set, double def, double min, double max, double step, Function<Object, Component> format
	) {
		return new TeamsSettings.Setting(id, TeamsSettings.Type.DOUBLE, get::get, v -> {
			set.accept(Math.max(min, Math.min(max, ((Number)v).doubleValue())));
			save();
		}, def, min, max, step, List.of(), format);
	}

	private static TeamsSettings.Setting choice(String id, Supplier<String> get, Consumer<String> set, String def, List<String> choices) {
		return new TeamsSettings.Setting(id, TeamsSettings.Type.CHOICE, get::get, v -> {
			set.accept(String.valueOf(v));
			save();
		}, def, 0.0, 0.0, 0.0, choices, null);
	}

	private TeamsSettings() {
	}

	public static record Group(String id, List<TeamsSettings.Setting> settings) {
		public Component title() {
			return TeamsText.t("settings.group." + this.id);
		}
	}

	public static record Setting(
		String id,
		TeamsSettings.Type type,
		Supplier<Object> get,
		Consumer<Object> set,
		Object def,
		double min,
		double max,
		double step,
		List<String> choices,
		@Nullable Function<Object, Component> format
	) {
		public Component label() {
			return TeamsText.t("settings." + this.id);
		}

		public Component tooltip() {
			return TeamsText.t("settings." + this.id + ".desc");
		}

		public Component choiceLabel(String choice) {
			return TeamsText.t("settings." + this.id + "." + choice.toLowerCase(Locale.ROOT));
		}
	}

	public static enum Type {
		BOOL,
		INT,
		DOUBLE,
		CHOICE;
	}
}
