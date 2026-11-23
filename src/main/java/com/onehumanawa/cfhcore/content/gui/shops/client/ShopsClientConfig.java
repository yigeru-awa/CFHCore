package com.onehumanawa.cfhcore.content.gui.shops.client;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;

public final class ShopsClientConfig {
	public boolean inventoryButton = true;
	private static JsonConfig<ShopsClientConfig> file;

	public static synchronized JsonConfig<ShopsClientConfig> file() {
		if (file == null) {
			file = JsonConfig.of("shops-client", ShopsClientConfig.class, ShopsClientConfig::new);
		}

		return file;
	}

	public static ShopsClientConfig get() {
		return (ShopsClientConfig)file().get();
	}
}
