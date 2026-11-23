package com.onehumanawa.cfhcore.content.gui.server;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.server.model.KitBook;
import com.onehumanawa.cfhcore.content.gui.server.model.RoleBook;
import com.onehumanawa.cfhcore.content.gui.server.model.ServerRules;

public final class ServerConfigs {
	private static JsonConfig<ServerRules> rules;
	private static JsonConfig<RoleBook> roles;
	private static JsonConfig<KitBook> kits;

	public static synchronized JsonConfig<ServerRules> rulesFile() {
		if (rules == null) {
			rules = JsonConfig.of("server", ServerRules.class, ServerRules::new);
			((ServerRules)rules.get()).sanitize();
		}

		return rules;
	}

	public static synchronized JsonConfig<RoleBook> rolesFile() {
		if (roles == null) {
			roles = JsonConfig.of("server-roles", RoleBook.class, RoleBook::defaults);
			((RoleBook)roles.get()).sanitize();
		}

		return roles;
	}

	public static synchronized JsonConfig<KitBook> kitsFile() {
		if (kits == null) {
			kits = JsonConfig.of("server-kits", KitBook.class, KitBook::new);
			((KitBook)kits.get()).sanitize();
		}

		return kits;
	}

	public static ServerRules rules() {
		return (ServerRules)rulesFile().get();
	}

	public static RoleBook roles() {
		return (RoleBook)rolesFile().get();
	}

	public static KitBook kits() {
		return (KitBook)kitsFile().get();
	}

	public static synchronized void reload() {
		rulesFile().load();
		((ServerRules)rules.get()).sanitize();
		rolesFile().load();
		((RoleBook)roles.get()).sanitize();
		kitsFile().load();
		((KitBook)kits.get()).sanitize();
	}

	private ServerConfigs() {
	}
}
