package com.onehumanawa.cfhcore.foundation.recipeeditor.config;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import java.util.Locale;
import java.util.function.Consumer;

public final class RecipeEditorClientConfig {
	public boolean openRecipeEditor = true;
	public String theme = "DARK";
	public int maxRows = 12;
	public boolean highlightExact = true;
	private static JsonConfig<RecipeEditorClientConfig> file;
	private static volatile int version;

	public RecipeEditorClientConfig sanitize() {
		if (this.theme == null || this.theme.isBlank()) {
			this.theme = "DARK";
		}
		this.theme = this.theme().name();
		this.maxRows = Math.max(4, Math.min(30, this.maxRows));
		return this;
	}

	public RecipeEditorClientConfig copy() {
		return ((RecipeEditorClientConfig)JsonConfig.GSON.fromJson(JsonConfig.GSON.toJson(this), RecipeEditorClientConfig.class)).sanitize();
	}

	public RecipeEditorClientConfig.Theme theme() {
		try {
			return RecipeEditorClientConfig.Theme.valueOf(this.theme.trim().toUpperCase(Locale.ROOT));
		} catch (RuntimeException var2) {
			return RecipeEditorClientConfig.Theme.DARK;
		}
	}

	public static synchronized JsonConfig<RecipeEditorClientConfig> file() {
		if (file == null) {
			file = JsonConfig.of("recipeeditor-client", RecipeEditorClientConfig.class, RecipeEditorClientConfig::new);
			((RecipeEditorClientConfig)file.get()).sanitize();
			version++;
		}
		return file;
	}

	public static RecipeEditorClientConfig get() {
		return (RecipeEditorClientConfig)file().get();
	}

	public static synchronized void reload() {
		file().load();
		((RecipeEditorClientConfig)file.get()).sanitize();
		version++;
	}

	public static synchronized void edit(Consumer<RecipeEditorClientConfig> change) {
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

	public static enum Theme {
		DARK,
		LIGHT;
	}
}
