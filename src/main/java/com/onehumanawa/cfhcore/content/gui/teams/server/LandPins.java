package com.onehumanawa.cfhcore.content.gui.teams.server;

import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.model.Access;
import com.onehumanawa.cfhcore.content.gui.teams.model.LandRules;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public final class LandPins {
	public static final List<String> RULES = List.of("build", "interact", "containers", "entities", "explosions", "mobGriefing", "fireSpread", "pvp", "claimRank");
	@Nullable
	private Access build;
	@Nullable
	private Access interact;
	@Nullable
	private Access containers;
	@Nullable
	private Access entities;
	@Nullable
	private Boolean explosions;
	@Nullable
	private Boolean mobGriefing;
	@Nullable
	private Boolean fireSpread;
	@Nullable
	private Boolean pvp;
	@Nullable
	private Rank claimRank;
	private final List<String> names = new ArrayList<>();

	public static LandPins parse(@Nullable List<String> lines) {
		LandPins p = new LandPins();
		if (lines == null) {
			return p;
		} else {
			LandRules scratch = new LandRules();

			for (String line : lines) {
				if (line != null && !line.isBlank()) {
					int eq = line.indexOf(61);
					String rule = eq < 0 ? null : canonical(line.substring(0, eq));
					String value = eq < 0 ? "" : line.substring(eq + 1).trim();
					if (rule != null && set(scratch, rule, value)) {
						switch (rule) {
							case "build":
								p.build = scratch.build;
								break;
							case "interact":
								p.interact = scratch.interact;
								break;
							case "containers":
								p.containers = scratch.containers;
								break;
							case "entities":
								p.entities = scratch.entities;
								break;
							case "explosions":
								p.explosions = scratch.explosions;
								break;
							case "mobGriefing":
								p.mobGriefing = scratch.mobGriefing;
								break;
							case "fireSpread":
								p.fireSpread = scratch.fireSpread;
								break;
							case "pvp":
								p.pvp = scratch.pvp;
								break;
							default:
								p.claimRank = scratch.claimRank;
						}

						if (!p.names.contains(rule)) {
							p.names.add(rule);
						}
					} else {
						CFHGuiTeams.LOGGER.warn("[CFHGui Teams] pinnedLandRules: \"{}\" is not a rule with a value it can have (rules: {})", line, RULES);
					}
				}
			}

			return p;
		}
	}

	public List<String> names() {
		return this.names;
	}

	public boolean pins(String rule) {
		return this.names.contains(rule);
	}

	private static boolean teams(TeamData t) {
		return !TeamSummary.SERVER_LAND.equals(t.id);
	}

	public Access build(TeamData t) {
		return this.build != null && teams(t) ? this.build : t.land.build;
	}

	public Access interact(TeamData t) {
		return this.interact != null && teams(t) ? this.interact : t.land.interact;
	}

	public Access containers(TeamData t) {
		return this.containers != null && teams(t) ? this.containers : t.land.containers;
	}

	public Access entities(TeamData t) {
		return this.entities != null && teams(t) ? this.entities : t.land.entities;
	}

	public boolean explosions(TeamData t) {
		return this.explosions != null && teams(t) ? this.explosions : t.land.explosions;
	}

	public boolean mobGriefing(TeamData t) {
		return this.mobGriefing != null && teams(t) ? this.mobGriefing : t.land.mobGriefing;
	}

	public boolean fireSpread(TeamData t) {
		return this.fireSpread != null && teams(t) ? this.fireSpread : t.land.fireSpread;
	}

	public boolean pvp(TeamData t) {
		return this.pvp != null && teams(t) ? this.pvp : t.land.pvp;
	}

	public Rank claimRank(TeamData t) {
		return this.claimRank != null ? this.claimRank : t.land.claimRank;
	}

	public LandRules effective(TeamData t) {
		if (this.names.isEmpty()) {
			return t.land;
		} else {
			LandRules r = new LandRules();
			r.build = this.build(t);
			r.interact = this.interact(t);
			r.containers = this.containers(t);
			r.entities = this.entities(t);
			r.explosions = this.explosions(t);
			r.mobGriefing = this.mobGriefing(t);
			r.fireSpread = this.fireSpread(t);
			r.pvp = this.pvp(t);
			r.claimRank = this.claimRank(t);
			return r;
		}
	}

	@Nullable
	public static String canonical(@Nullable String rule) {
		if (rule == null) {
			return null;
		} else {
			String key = rule.trim().replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);

			for (String r : RULES) {
				if (r.toLowerCase(Locale.ROOT).equals(key)) {
					return r;
				}
			}

			return null;
		}
	}

	public static boolean set(LandRules rules, String rule, String value) {
		switch (rule) {
			case "build":
			case "interact":
			case "containers":
			case "entities":
				Access a = Access.parse(value, null);
				if (a == null) {
					return false;
				}

				switch (rule) {
					case "build":
						rules.build = a;
						break;
					case "interact":
						rules.interact = a;
						break;
					case "containers":
						rules.containers = a;
						break;
					default:
						rules.entities = a;
				}

				return true;
			case "explosions":
			case "mobGriefing":
			case "fireSpread":
			case "pvp":
				Boolean b = bool(value);
				if (b == null) {
					return false;
				}

				switch (rule) {
					case "explosions":
						rules.explosions = b;
						break;
					case "mobGriefing":
						rules.mobGriefing = b;
						break;
					case "fireSpread":
						rules.fireSpread = b;
						break;
					default:
						rules.pvp = b;
				}

				return true;
			case "claimRank":
				Rank r = Rank.parse(value, null);
				if (r == null) {
					return false;
				}

				rules.claimRank = r;
				return true;
			default:
				return false;
		}
	}

	public static String get(LandRules rules, String rule) {
		return switch (rule) {
			case "build" -> rules.build.name();
			case "interact" -> rules.interact.name();
			case "containers" -> rules.containers.name();
			case "entities" -> rules.entities.name();
			case "explosions" -> String.valueOf(rules.explosions);
			case "mobGriefing" -> String.valueOf(rules.mobGriefing);
			case "fireSpread" -> String.valueOf(rules.fireSpread);
			case "pvp" -> String.valueOf(rules.pvp);
			case "claimRank" -> rules.claimRank.name();
			default -> "";
		};
	}

	@Nullable
	private static Boolean bool(String value) {
		String v = value.trim().toLowerCase(Locale.ROOT);
		if (v.equals("true") || v.equals("on") || v.equals("yes") || v.equals("1")) {
			return Boolean.TRUE;
		} else {
			return !v.equals("false") && !v.equals("off") && !v.equals("no") && !v.equals("0") ? null : Boolean.FALSE;
		}
	}

	private LandPins() {
	}
}
