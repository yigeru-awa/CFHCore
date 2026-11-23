package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public final class TreeMarks {
	public String name = "";
	public final Set<String> folded = new LinkedHashSet<>();
	public final Set<String> given = new LinkedHashSet<>();
	public final List<Island> islands = new ArrayList<>();

	@Nullable
	public TreeRows.Refusal addIsland(TreeOutline outline, String first, String second) {
		TreeRows.Refusal refusal = TreeRows.refusal(outline, this.islands, first, second);
		if (refusal == null) {
			this.islands.add(new Island(first, second));
		}

		return refusal;
	}

	@Nullable
	public Island island(String first, String second) {
		Island probe = new Island(first, second);

		for (Island island : this.islands) {
			if (island.sameEnds(probe)) {
				return island;
			}
		}

		return null;
	}

	public boolean prune(TreeOutline outline) {
		boolean dropped = this.folded.removeIf(path -> !outline.has(path));
		dropped |= this.given.removeIf(path -> !outline.has(path));
		return dropped | this.islands.removeIf(island -> !outline.has(island.a) || !outline.has(island.b));
	}

	public List<TreeRow> rows(TreeOutline outline) {
		return TreeRows.flatten(outline, this.folded, this.islands);
	}

	public void write(JsonObject json) {
		if (!this.name.isEmpty()) {
			json.addProperty("name", this.name);
		}

		if (!this.folded.isEmpty()) {
			json.add("folded", paths(this.folded));
		}

		if (!this.given.isEmpty()) {
			json.add("given", paths(this.given));
		}

		if (!this.islands.isEmpty()) {
			JsonArray array = new JsonArray();

			for (Island island : this.islands) {
				JsonObject o = new JsonObject();
				o.add("a", path(island.a));
				o.add("b", path(island.b));
				if (island.open) {
					o.addProperty("open", true);
				}

				array.add(o);
			}

			json.add("islands", array);
		}
	}

	public static TreeMarks read(JsonObject json) {
		TreeMarks marks = new TreeMarks();
		JsonElement name = json.get("name");
		if (name != null && name.isJsonPrimitive()) {
			marks.name = name.getAsString();
		}

		readPaths(json.get("folded"), marks.folded);
		readPaths(json.get("given"), marks.given);
		JsonElement islands = json.get("islands");
		if (islands != null && islands.isJsonArray()) {
			for (JsonElement el : islands.getAsJsonArray()) {
				if (el.isJsonObject()) {
					JsonObject o = el.getAsJsonObject();
					String a = readPath(o.get("a"));
					String b = readPath(o.get("b"));
					if (a != null && b != null && !a.equals(b)) {
						Island island = new Island(a, b);
						JsonElement open = o.get("open");
						island.open = open != null && open.isJsonPrimitive() && open.getAsBoolean();
						boolean fits = true;

						for (Island other : marks.islands) {
							Island.Relation relation = island.to(other);
							if (relation == Island.Relation.SAME || relation == Island.Relation.CROSSING) {
								fits = false;
								break;
							}
						}

						if (fits) {
							marks.islands.add(island);
						}
					}
				}
			}
		}

		return marks;
	}

	private static JsonArray paths(Set<String> paths) {
		JsonArray array = new JsonArray();

		for (String path : paths) {
			array.add(path(path));
		}

		return array;
	}

	private static JsonArray path(String path) {
		JsonArray array = new JsonArray();

		for (String key : TreePaths.keys(path)) {
			array.add(key);
		}

		return array;
	}

	private static void readPaths(@Nullable JsonElement json, Set<String> into) {
		if (json != null && json.isJsonArray()) {
			for (JsonElement el : json.getAsJsonArray()) {
				String path = readPath(el);
				if (path != null) {
					into.add(path);
				}
			}
		}
	}

	@Nullable
	private static String readPath(@Nullable JsonElement json) {
		if (json != null && json.isJsonArray()) {
			List<String> keys = new ArrayList<>();

			for (JsonElement key : json.getAsJsonArray()) {
				if (!key.isJsonPrimitive()) {
					return null;
				}

				keys.add(key.getAsString());
			}

			return TreePaths.of(keys);
		} else {
			return null;
		}
	}
}
