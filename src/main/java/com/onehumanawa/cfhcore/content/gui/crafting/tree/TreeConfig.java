package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;

public final class TreeConfig {
	public int dockWidth = 0;
	public boolean names = true;
	public boolean openDockWithNewTree = true;
	public int chips = 5;
	public int dockKey = 84;
	public boolean craftIntoInventory = true;
	public boolean findRecipes = true;
	public boolean doneToast = true;
	public boolean workstations = true;
	public boolean plannerGraph = true;
	public boolean plannerPanel = true;
	private static JsonConfig<TreeConfig> file;

	public static synchronized JsonConfig<TreeConfig> file() {
		if (file == null) {
			file = JsonConfig.of("crafting", TreeConfig.class, TreeConfig::new);
		}

		return file;
	}

	public static TreeConfig get() {
		return (TreeConfig)file().get();
	}

	public static void save() {
		file().save();
	}
}
