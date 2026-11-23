package com.onehumanawa.cfhcore.content.gui.crafting.team.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.crafting.CFHGuiCrafting;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;

public final class GivenOnce {
	private final Map<String, Set<String>> given = new LinkedHashMap<>();
	@Nullable
	private final Path file;
	private boolean dirty;

	public GivenOnce(@Nullable Path file) {
		this.file = file;
	}

	public static String key(String world, String team) {
		return world + "/" + team;
	}

	public boolean has(String key, String recipe) {
		Set<String> set = this.given.get(key);
		return set != null && set.contains(recipe);
	}

	public List<String> due(String key, Collection<String> pinned) {
		List<String> out = new ArrayList<>();

		for (String recipe : pinned) {
			if (!this.has(key, recipe) && !out.contains(recipe)) {
				out.add(recipe);
			}
		}

		for (String recipex : out) {
			this.mark(key, recipex);
		}

		return out;
	}

	public void mark(String key, String recipe) {
		if (this.given.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(recipe)) {
			this.dirty = true;
		}
	}

	public void load() {
		this.given.clear();
		this.dirty = false;
		if (this.file != null && Files.isRegularFile(this.file)) {
			try {
				try (Reader r = Files.newBufferedReader(this.file, StandardCharsets.UTF_8)) {
					JsonObject json = (JsonObject)JsonConfig.GSON.fromJson(r, JsonObject.class);
					JsonElement all = json == null ? null : json.get("given");
					if (all != null && all.isJsonObject()) {
						for (Entry<String, JsonElement> e : all.getAsJsonObject().entrySet()) {
							if (e.getValue().isJsonArray()) {
								Set<String> set = new LinkedHashSet<>();

								for (JsonElement el : e.getValue().getAsJsonArray()) {
									if (el.isJsonPrimitive()) {
										set.add(el.getAsString());
									}
								}

								this.given.put(e.getKey(), set);
							}
						}

						return;
					}
				}
			} catch (Exception var11) {
				CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] could not read {} ({}); pinned recipes may be given again", this.file, var11.toString());
			}
		}
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("version", 1);
		JsonObject all = new JsonObject();

		for (Entry<String, Set<String>> e : this.given.entrySet()) {
			JsonArray arr = new JsonArray();

			for (String recipe : e.getValue()) {
				arr.add(recipe);
			}

			all.add(e.getKey(), arr);
		}

		json.add("given", all);
		return json;
	}

	public void save() {
		if (this.dirty) {
			this.dirty = false;
			if (this.file != null) {
				try {
					Files.createDirectories(this.file.getParent());
					Path tmp = this.file.resolveSibling(this.file.getFileName() + ".tmp");
					Files.writeString(tmp, JsonConfig.GSON.toJson(this.toJson()), StandardCharsets.UTF_8);

					try {
						Files.move(tmp, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
					} catch (IOException var3) {
						Files.move(tmp, this.file, StandardCopyOption.REPLACE_EXISTING);
					}
				} catch (IOException var4) {
					CFHGuiCrafting.LOGGER.error("[CFHGui Crafting] could not write {}", this.file, var4);
				}
			}
		}
	}
}
