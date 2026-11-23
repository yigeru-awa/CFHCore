package com.onehumanawa.cfhcore.content.gui.earlywindow.scene;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongSupplier;

public interface StartupScene {
	String FACTORY = "factory";
	String ISLAND = "island";

	void draw(int var1, int var2, float var3, boolean var4, float var5, LoadingScene.Feed var6);

	void dispose();

	static String kind(String setting) {
		return setting != null && "island".equals(setting.trim().toLowerCase(Locale.ROOT)) ? "island" : "factory";
	}

	static StartupScene make(
		String kind, LoadingScene.Look look, Function<String, byte[]> fonts, LongSupplier clock, Assets assets, String title, Consumer<RuntimeException> failed
	) {
		if ("island".equals(kind(kind))) {
			try {
				return new IslandScene(look, fonts.apply(LoadingScene.fontFile("monocraft")), clock, assets, title);
			} catch (RuntimeException var8) {
				failed.accept(var8);
			}
		}

		return new LoadingScene(look, fonts.apply(LoadingScene.fontFile(look.font())), clock, assets);
	}
}
