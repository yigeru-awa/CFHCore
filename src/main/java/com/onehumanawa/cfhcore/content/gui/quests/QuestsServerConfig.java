package com.onehumanawa.cfhcore.content.gui.quests;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;

public final class QuestsServerConfig {
	public boolean installExamples = true;
	public boolean teamProgress = true;
	public boolean mergeOnJoin = true;
	public boolean keepOnLeave = true;
	public boolean announceTeamQuests = true;
	public boolean announceAdvancementQuests = true;
	public int editorPermissionLevel = 2;
	public boolean ownerMayEdit = true;
	public int checkIntervalTicks = 20;
	public int defaultTeamXp = 10;
	public boolean ftbConvert = true;
	public boolean ftbProgress = true;
	public boolean ftbQuiet = true;
	private static JsonConfig<QuestsServerConfig> file;

	public static synchronized JsonConfig<QuestsServerConfig> file() {
		if (file == null) {
			file = JsonConfig.of("quests-server", QuestsServerConfig.class, QuestsServerConfig::new);
		}

		return file;
	}

	public static QuestsServerConfig get() {
		return (QuestsServerConfig)file().get();
	}

	public static void reload() {
		file().load();
	}
}
