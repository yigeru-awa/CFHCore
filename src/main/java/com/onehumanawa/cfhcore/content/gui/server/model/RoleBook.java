package com.onehumanawa.cfhcore.content.gui.server.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class RoleBook {
	public List<Role> roles = new ArrayList<>();

	public static RoleBook defaults() {
		RoleBook book = new RoleBook();
		Role everyone = new Role();
		everyone.id = "default";
		everyone.name = "Player";
		everyone.color = 11184810;
		everyone.priority = 0;
		everyone.everyone = true;
		book.roles.add(everyone);
		Role moderator = new Role();
		moderator.id = "moderator";
		moderator.name = "Moderator";
		moderator.color = 5621992;
		moderator.prefix = "Mod";
		moderator.priority = 50;
		moderator.inherits.add("default");
		moderator.allow.add("cfhgui_server.mod.*");
		moderator.allow.add("cfhgui_server.admin.menu");
		moderator.allow.add("cfhgui_server.admin.dashboard");
		moderator.allow.add("cfhgui_server.admin.players");
		moderator.allow.add("cfhgui_server.home.others");
		moderator.allow.add("cfhgui_server.enderchest.use");
		moderator.allow.add("cfhgui_server.workbench.use");
		moderator.allow.add("cfhgui_server.maintenance.bypass");
		moderator.deny.add("cfhgui_server.mod.ban.permanent");
		moderator.deny.add("cfhgui_server.mod.invsee.edit");
		moderator.deny.add("cfhgui_server.mod.exempt");
		moderator.numbers.put("cfhgui_server.homes.max", 10);
		moderator.numbers.put("cfhgui_server.tp.warmup", 0);
		moderator.numbers.put("cfhgui_server.tp.cooldown", 0);
		book.roles.add(moderator);
		Role admin = new Role();
		admin.id = "admin";
		admin.name = "Admin";
		admin.color = 15226170;
		admin.prefix = "Admin";
		admin.priority = 100;
		admin.inherits.add("moderator");
		admin.allow.add("*");
		admin.numbers.put("cfhgui_server.homes.max", 50);
		admin.numbers.put("cfhgui_server.rtp.cooldown", 0);
		book.roles.add(admin);
		return book.sanitize();
	}

	@Nullable
	public Role get(String id) {
		for (Role r : this.roles) {
			if (r.id.equals(id)) {
				return r;
			}
		}

		return null;
	}

	public RoleBook sanitize() {
		if (this.roles == null) {
			this.roles = new ArrayList<>();
		}

		this.roles.removeIf(rx -> rx == null || rx.id == null || rx.id.isBlank());

		for (Role r : this.roles) {
			r.sanitize();
		}

		if (this.get("default") == null) {
			Role everyone = new Role();
			everyone.id = "default";
			everyone.name = "Player";
			everyone.everyone = true;
			this.roles.add(everyone);
		}

		this.roles.sort(Comparator.<Role>comparingInt(rx -> rx.priority).reversed());
		return this;
	}
}
