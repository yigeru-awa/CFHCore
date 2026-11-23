package com.onehumanawa.cfhcore.content.gui.teams.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.model.Access;
import com.onehumanawa.cfhcore.content.gui.teams.model.BannerData;
import com.onehumanawa.cfhcore.content.gui.teams.model.LandRules;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public final class ServerLand {
	static final String DEFAULT_NAME = "Spawn";
	static final int DEFAULT_COLOR = 10467033;
	static final int NAME_MAX = 24;
	public static final List<String> RULES = List.of("build", "interact", "containers", "entities", "explosions", "mobGriefing", "fireSpread", "pvp");
	private static final List<String> WHO = List.of("build", "interact", "containers", "entities");
	public final TeamData data = new TeamData();
	@Nullable
	private final Path file;
	private boolean dirty;
	private long dirtySince;

	ServerLand(@Nullable Path file) {
		this.file = file;
		this.data.id = TeamSummary.SERVER_LAND;
		this.data.name = "Spawn";
		this.data.color = 10467033;
		this.data.banner = new BannerData("light_gray");
		this.data.land = closed();
	}

	static LandRules closed() {
		LandRules r = new LandRules();
		r.build = Access.MEMBERS;
		r.interact = Access.MEMBERS;
		r.containers = Access.MEMBERS;
		r.entities = Access.MEMBERS;
		r.explosions = false;
		r.mobGriefing = false;
		r.fireSpread = false;
		r.pvp = false;
		return r;
	}

	static boolean set(LandRules land, String rule, String value) {
		if (!RULES.contains(rule)) {
			return false;
		} else {
			String word = value.trim().toLowerCase(Locale.ROOT);
			if (!WHO.contains(rule)) {
				return LandPins.set(land, rule, word);
			} else if (word.equals("everyone")) {
				return LandPins.set(land, rule, Access.EVERYONE.name());
			} else {
				return !word.equals("nobody") && !word.equals("none") && !word.equals("members") && !word.equals("allies")
					? false
					: LandPins.set(land, rule, Access.MEMBERS.name());
			}
		}
	}

	static String get(LandRules land, String rule) {
		String value = LandPins.get(land, rule);
		if (!WHO.contains(rule)) {
			return value;
		} else {
			return value.equals(Access.EVERYONE.name()) ? "everyone" : "nobody";
		}
	}

	void load() {
		if (this.file != null && Files.isRegularFile(this.file)) {
			try {
				this.take(Files.readString(this.file, StandardCharsets.UTF_8));
			} catch (Exception var2) {
				this.data.land = closed();
				CFHGuiTeams.LOGGER.error("[CFHGui Teams] {} could not be read; server land keeps its defaults (closed to everyone)", this.file, var2);
			}
		}
	}

	private void take(String json) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		JsonElement name = root.get("name");
		JsonElement color = root.get("color");
		JsonElement banner = root.get("banner");
		JsonElement rules = root.get("rules");
		if (name != null && name.isJsonPrimitive()) {
			String n = Names.line(name.getAsString(), 24);
			if (!n.isEmpty()) {
				this.data.name = n;
			}
		}

		if (color != null && color.isJsonPrimitive()) {
			this.data.color = color.getAsInt() & 16777215;
		}

		if (banner != null && banner.isJsonObject()) {
			BannerData b = (BannerData)JsonConfig.GSON.fromJson(banner, BannerData.class);
			if (b != null) {
				b.sanitize();
				this.data.banner = b;
			}
		}

		LandRules land = closed();
		if (rules != null && rules.isJsonObject()) {
			for (String rule : RULES) {
				JsonElement value = rules.getAsJsonObject().get(rule);
				if (value != null && value.isJsonPrimitive()) {
					set(land, rule, value.getAsString());
				}
			}
		}

		this.data.land = land;
	}

	void touch() {
		if (!this.dirty) {
			this.dirtySince = System.currentTimeMillis();
		}

		this.dirty = true;
	}

	void tick(Saver saver) {
		if (this.dirty && System.currentTimeMillis() - this.dirtySince >= 5000L) {
			this.save(saver);
		}
	}

	void save(Saver saver) {
		if (this.dirty) {
			this.dirty = false;
			if (this.file != null) {
				saver.text(this.file, this.json());
			}
		}
	}

	String json() {
		JsonObject root = new JsonObject();
		root.addProperty("name", this.data.name);
		root.addProperty("color", this.data.color);
		root.add("banner", JsonConfig.GSON.toJsonTree(this.data.banner));
		JsonObject rules = new JsonObject();

		for (String rule : RULES) {
			String value = get(this.data.land, rule);
			if (WHO.contains(rule)) {
				rules.addProperty(rule, value);
			} else {
				rules.addProperty(rule, Boolean.parseBoolean(value));
			}
		}

		root.add("rules", rules);
		return JsonConfig.GSON.toJson(root);
	}

	static ServerLand read(String json) {
		ServerLand land = new ServerLand(null);
		land.take(json);
		return land;
	}
}
