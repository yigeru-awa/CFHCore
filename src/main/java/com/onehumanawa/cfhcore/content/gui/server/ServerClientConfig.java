package com.onehumanawa.cfhcore.content.gui.server;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;

public final class ServerClientConfig {
	public boolean inventoryButtons = true;
	public boolean inventoryAdmin = true;
	public boolean notices = true;
	public boolean noticeSound = true;
	public boolean requestToast = true;
	public boolean mailToast = true;
	public boolean hudWarmup = true;
	public boolean hudStatus = true;
	public boolean mutedNote = true;
	public boolean mapHomes = true;
	public boolean mapWarps = true;
	public boolean mapSpawn = true;
	public boolean mapDeath = true;
	public boolean confirmDeletes = true;
	private static JsonConfig<ServerClientConfig> file;

	public static synchronized JsonConfig<ServerClientConfig> file() {
		if (file == null) {
			file = JsonConfig.of("server-client", ServerClientConfig.class, ServerClientConfig::new);
		}

		return file;
	}

	public static ServerClientConfig get() {
		return (ServerClientConfig)file().get();
	}

	public static void save() {
		file().save();
	}
}
