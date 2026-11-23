package com.onehumanawa.cfhcore.content.gui.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Kit {
	public static final int ID_MAX = 24;
	public static final int NAME_MAX = 32;
	public String id = "";
	public String name = "";
	public String icon = "";
	public List<String> items = new ArrayList<>();
	public int cooldownSeconds = 0;
	public boolean oneTime = false;
	public boolean firstJoin = false;
	public boolean restricted = false;

	public String node() {
		return "cfhgui_server.kit.use." + this.id.toLowerCase(Locale.ROOT);
	}

	public Kit sanitize() {
		if (this.id == null) {
			this.id = "";
		}

		if (this.name == null || this.name.isBlank()) {
			this.name = this.id;
		}

		if (this.icon == null) {
			this.icon = "";
		}

		if (this.items == null) {
			this.items = new ArrayList<>();
		}

		this.items.removeIf(s -> s == null || s.isBlank());
		if (this.cooldownSeconds < 0) {
			this.cooldownSeconds = 0;
		}

		return this;
	}

	public static final class View {
		public String id = "";
		public String name = "";
		public String icon = "";
		public List<Kit.View.Entry> contents = new ArrayList<>();
		public long readyAt;

		public Kit.View sanitize() {
			if (this.id == null) {
				this.id = "";
			}

			if (this.name == null) {
				this.name = this.id;
			}

			if (this.icon == null) {
				this.icon = "";
			}

			if (this.contents == null) {
				this.contents = new ArrayList<>();
			}

			this.contents.removeIf(e -> e == null || e.item == null);
			return this;
		}

		public static final class Entry {
			public String item = "minecraft:air";
			public int count = 1;
		}
	}
}
