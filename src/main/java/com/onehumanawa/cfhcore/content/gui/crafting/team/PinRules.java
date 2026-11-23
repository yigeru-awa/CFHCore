package com.onehumanawa.cfhcore.content.gui.crafting.team;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class PinRules {
	public static final int MAX_PINS = 100;
	public static final int MAX_STEPS = 2000;
	public static final int MAX_TREE_BYTES = 30000;
	public static final int MAX_ID = 256;
	public static final String NO_TEAM = "no_team";
	public static final String OTHER_TEAM = "other_team";
	public static final String UNKNOWN_RECIPE = "unknown_recipe";
	public static final String FULL = "full";
	public static final String TOO_BIG = "too_big";
	public static final String ALREADY = "already";
	public static final String NO_PIN = "no_pin";
	public static final String NOT_YOURS = "not_yours";
	public static final String INVALID = "invalid";

	public static boolean recipeKnown(@Nullable String id, Predicate<ResourceLocation> serverRecipes) {
		if (id != null && !id.isEmpty() && id.length() <= 256) {
			ResourceLocation key = ResourceLocation.tryParse(id);
			if (key == null) {
				return false;
			} else {
				return key.getPath().startsWith("/") ? key.getPath().length() > 1 : serverRecipes.test(key);
			}
		} else {
			return false;
		}
	}

	@Nullable
	public static String refusePin(@Nullable TeamsLink.TeamInfo team, String askedTeam, List<Pin> pins, Pin pin, Predicate<ResourceLocation> serverRecipes) {
		if (team == null) {
			return "no_team";
		} else if (!team.id().toString().equals(askedTeam)) {
			return "other_team";
		} else if (!recipeKnown(pin.recipe, serverRecipes)) {
			return "unknown_recipe";
		} else {
			if (pin.isTree()) {
				if (pin.tree == null) {
					return "invalid";
				}

				if (!pin.recipe.equals(goalRecipe(pin.tree))) {
					return "invalid";
				}

				if (pin.steps < 1 || pin.steps > 2000 || entries(pin.tree) > 2000) {
					return "too_big";
				}
			}

			for (Pin p : pins) {
				if (same(p, pin)) {
					return "already";
				}
			}

			return pins.size() >= 100 ? "full" : null;
		}
	}

	@Nullable
	public static String refuseUnpin(@Nullable TeamsLink.TeamInfo team, String askedTeam, UUID player, @Nullable Pin pin) {
		if (team == null) {
			return "no_team";
		} else if (!team.id().toString().equals(askedTeam)) {
			return "other_team";
		} else if (pin == null) {
			return "no_pin";
		} else {
			return !player.equals(pin.by) && !team.isOfficer(player) ? "not_yours" : null;
		}
	}

	public static boolean same(Pin a, Pin b) {
		return a.kind.equals(b.kind) && a.recipe.equals(b.recipe) ? !a.isTree() || Objects.equals(a.tree, b.tree) : false;
	}

	@Nullable
	public static String goalRecipe(@Nullable JsonObject tree) {
		JsonElement el = tree == null ? null : tree.get("recipe");
		return el != null && el.isJsonPrimitive() ? el.getAsString() : null;
	}

	static int entries(JsonObject tree) {
		int most = 0;

		for (String key : new String[]{"resolutions", "found", "folded", "given", "islands"}) {
			JsonElement el = tree.get(key);
			if (el != null && el.isJsonArray()) {
				most = Math.max(most, el.getAsJsonArray().size());
			}
		}

		return most;
	}

	private PinRules() {
	}
}
