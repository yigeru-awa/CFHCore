package com.onehumanawa.cfhcore.content.gui.crafting.tree;

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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class TreeStore {
	public static final int RECENT = 8;
	private final List<TreeEntry> entries = new ArrayList<>();
	@Nullable
	public String shown;
	public boolean dockOpen;
	@Nullable
	private final Path file;
	private boolean dirty;

	public TreeStore(@Nullable Path file) {
		this.file = file;
	}

	public static Path defaultFile() {
		return JsonConfig.dir().resolve("crafting-trees.json");
	}

	public List<TreeEntry> all() {
		return this.entries;
	}

	public List<TreeEntry> pinned() {
		List<TreeEntry> out = new ArrayList<>();

		for (TreeEntry e : this.entries) {
			if (e.saved && e.pinned) {
				out.add(e);
			}
		}

		return out;
	}

	public List<TreeEntry> saved() {
		List<TreeEntry> out = new ArrayList<>();

		for (TreeEntry e : this.entries) {
			if (e.saved) {
				out.add(e);
			}
		}

		out.sort(Comparator.<TreeEntry>comparingLong(ex -> ex.lastUsed).reversed());
		return out;
	}

	public List<TreeEntry> recent() {
		List<TreeEntry> out = new ArrayList<>();

		for (TreeEntry e : this.entries) {
			if (!e.saved) {
				out.add(e);
			}
		}

		out.sort(Comparator.<TreeEntry>comparingLong(ex -> ex.lastUsed).reversed());
		return out;
	}

	@Nullable
	public TreeEntry byId(@Nullable String id) {
		if (id == null) {
			return null;
		} else {
			for (TreeEntry e : this.entries) {
				if (e.id.equals(id)) {
					return e;
				}
			}

			return null;
		}
	}

	public TreeEntry create() {
		TreeEntry e = new TreeEntry(UUID.randomUUID().toString());
		e.lastUsed = System.currentTimeMillis();
		this.entries.add(e);
		this.dirty = true;
		return e;
	}

	public void remove(TreeEntry entry) {
		if (this.entries.remove(entry)) {
			this.dirty = true;
		}

		if (entry.id.equals(this.shown)) {
			this.shown = null;
		}
	}

	public void trimRecent(@Nullable TreeEntry keep) {
		String recipe = keep == null ? null : TreeCodec.goalRecipe(keep.json);
		boolean keepIsRecent = keep != null && !keep.saved;
		int kept = keepIsRecent ? 1 : 0;

		for (TreeEntry e : this.recent()) {
			if (e != keep) {
				boolean again = keepIsRecent && recipe != null && recipe.equals(TreeCodec.goalRecipe(e.json));
				if (!again) {
					if (++kept <= 8) {
						continue;
					}
				}

				this.entries.remove(e);
				this.dirty = true;
			}
		}
	}

	public void move(TreeEntry entry, int index) {
		List<TreeEntry> pinned = this.pinned();
		int from = pinned.indexOf(entry);
		if (from >= 0) {
			List<TreeEntry> rest = new ArrayList<>(pinned);
			rest.remove(entry);
			int to = Math.max(0, Math.min(index, rest.size()));
			if (to != from) {
				this.entries.remove(entry);
				if (to < rest.size()) {
					this.entries.add(this.entries.indexOf(rest.get(to)), entry);
				} else {
					this.entries.add(this.entries.indexOf(rest.get(rest.size() - 1)) + 1, entry);
				}

				this.dirty = true;
			}
		}
	}

	public void touch() {
		this.dirty = true;
	}

	public boolean dirty() {
		return this.dirty;
	}

	public void load() {
		this.entries.clear();
		this.shown = null;
		this.dirty = false;
		if (this.file != null && Files.isRegularFile(this.file)) {
			try (Reader r = Files.newBufferedReader(this.file, StandardCharsets.UTF_8)) {
				this.read((JsonObject)JsonConfig.GSON.fromJson(r, JsonObject.class));
			} catch (Exception var6) {
				CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] could not read {} ({}); starting without saved trees", this.file, var6.toString());
			}
		}
	}

	public void read(@Nullable JsonObject json) {
		this.entries.clear();
		this.shown = null;
		if (json != null) {
			this.dockOpen = bool(json.get("dockOpen"));
			JsonElement trees = json.get("trees");
			if (trees != null && trees.isJsonArray()) {
				for (JsonElement el : trees.getAsJsonArray()) {
					if (el.isJsonObject()) {
						JsonObject o = el.getAsJsonObject();
						JsonElement id = o.get("id");
						JsonElement tree = o.get("tree");
						if (id != null && id.isJsonPrimitive() && tree != null && tree.isJsonObject() && this.byId(id.getAsString()) == null) {
							TreeEntry e = new TreeEntry(id.getAsString());
							e.saved = bool(o.get("saved"));
							e.pinned = bool(o.get("pinned"));
							JsonElement used = o.get("lastUsed");
							e.lastUsed = used != null && used.isJsonPrimitive() ? used.getAsLong() : 0L;
							e.json = tree.getAsJsonObject();
							this.entries.add(e);
						}
					}
				}
			}

			JsonElement was = json.get("shown");
			if (was != null && was.isJsonPrimitive() && this.byId(was.getAsString()) != null) {
				this.shown = was.getAsString();
			}
		}
	}

	private static boolean bool(@Nullable JsonElement el) {
		return el != null && el.isJsonPrimitive() && el.getAsBoolean();
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("version", 1);
		json.addProperty("dockOpen", this.dockOpen);
		TreeEntry now = this.byId(this.shown);
		if (now != null && now.json != null) {
			json.addProperty("shown", now.id);
		}

		JsonArray trees = new JsonArray();

		for (TreeEntry e : this.entries) {
			if (e.json != null) {
				JsonObject o = new JsonObject();
				o.addProperty("id", e.id);
				if (e.saved) {
					o.addProperty("saved", true);
				}

				if (e.pinned) {
					o.addProperty("pinned", true);
				}

				o.addProperty("lastUsed", e.lastUsed);
				o.add("tree", e.json);
				trees.add(o);
			}
		}

		json.add("trees", trees);
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
