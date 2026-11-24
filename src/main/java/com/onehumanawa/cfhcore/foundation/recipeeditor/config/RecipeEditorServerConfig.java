package com.onehumanawa.cfhcore.foundation.recipeeditor.config;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class RecipeEditorServerConfig {
	public boolean enabled = true;
	public String permission = "USE";
	public int maxRecipesPerSearch = 512;
	public boolean showHiddenRecipes = false;
	public boolean allowDeferredSync = false;
	public String cacheDir = "recipeeditor";
	private static JsonConfig<RecipeEditorServerConfig> file;
	private static volatile int version;

	public RecipeEditorServerConfig.Permission permission() {
		try {
			return RecipeEditorServerConfig.Permission.valueOf(this.permission.trim().toUpperCase(Locale.ROOT));
		} catch (RuntimeException var2) {
			return RecipeEditorServerConfig.Permission.USE;
		}
	}

	public RecipeEditorServerConfig sanitize() {
		if (this.permission == null) {
			this.permission = "USE";
		}
		this.permission = this.permission().name();
		this.maxRecipesPerSearch = Math.max(64, Math.min(8192, this.maxRecipesPerSearch));
		if (this.cacheDir == null || this.cacheDir.isBlank()) {
			this.cacheDir = "recipeeditor";
		}
		return this;
	}

	public RecipeEditorServerConfig copy() {
		return ((RecipeEditorServerConfig)JsonConfig.GSON.fromJson(JsonConfig.GSON.toJson(this), RecipeEditorServerConfig.class)).sanitize();
	}

	public static synchronized JsonConfig<RecipeEditorServerConfig> file() {
		if (file == null) {
			file = JsonConfig.of("recipeeditor-server", RecipeEditorServerConfig.class, RecipeEditorServerConfig::new);
			((RecipeEditorServerConfig)file.get()).sanitize();
			version++;
		}
		return file;
	}

	public static RecipeEditorServerConfig get() {
		return (RecipeEditorServerConfig)file().get();
	}

	public static synchronized void reload() {
		file().load();
		((RecipeEditorServerConfig)file.get()).sanitize();
		version++;
	}

	public static synchronized void edit(Consumer<RecipeEditorServerConfig> change) {
		file().update(c -> {
			change.accept(c);
			c.sanitize();
		});
		version++;
	}

	public static int version() {
		file();
		return version;
	}

	public static boolean enabled() {
		return file().get().enabled;
	}

	public static enum Permission {
		USE,
		ADMIN;
	}
}
