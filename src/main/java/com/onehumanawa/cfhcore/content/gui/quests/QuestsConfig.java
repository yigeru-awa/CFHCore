package com.onehumanawa.cfhcore.content.gui.quests;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import java.util.ArrayList;
import java.util.List;

public final class QuestsConfig {
	public boolean tracker = true;
	public String trackerAnchor = "TOP_RIGHT";
	public int trackerX = 6;
	public int trackerY = 40;
	public double trackerScale = 1.0;
	public int trackerMax = 4;
	public boolean trackerAuto = false;
	public List<String> pinned = new ArrayList<>();
	public boolean toasts = true;
	public boolean chapterBanner = true;
	public boolean celebrations = true;
	public boolean sounds = true;
	public boolean flowingLines = true;
	public boolean subtreePreview = true;
	public int subtreePreviewDelayMs = 220;
	public boolean zoomToPointer = true;
	public boolean dimCompleted = false;
	public boolean inventoryButton = true;
	public boolean advancements = true;
	public boolean overview = false;
	public boolean turnTallTrees = true;
	public boolean ftbTakeover = true;
	public String lastChapter = "";
	public List<String> seenChapters = new ArrayList<>();
	public List<String> openTrees = new ArrayList<>();
	private static JsonConfig<QuestsConfig> file;

	public static synchronized JsonConfig<QuestsConfig> file() {
		if (file == null) {
			file = JsonConfig.of("quests", QuestsConfig.class, QuestsConfig::new);
		}

		return file;
	}

	public static QuestsConfig get() {
		return (QuestsConfig)file().get();
	}

	public static void save() {
		file().save();
	}
}
