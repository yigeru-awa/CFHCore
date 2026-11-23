package com.onehumanawa.cfhcore.content.gui.quests.server.adv;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class AdvOptions {
	public static final String TRANSLATE = "translate";
	public static final String ADVANCEMENT = "advancement";
	public static final String TREE = "tree";
	public static final String FREE = "free";
	public static final String QUEST = "quest";
	public static final String REPLACE = "replace";
	public static final String MERGE = "merge";
	public static final String COPY = "copy";
	public String tasks = "translate";
	public String order = "tree";
	public String kind = "quest";
	public boolean rewards = false;
	public boolean keys = true;
	public float spacing = 1.5F;
	public String group = "";
	public String groupTitle = "";
	public String exists = "merge";
	public Map<String, AdvOptions.Name> names = new LinkedHashMap<>();
	public static final String[] WORDS = new String[]{
		"tasks=translate",
		"tasks=advancement",
		"order=tree",
		"order=free",
		"kind=quest",
		"kind=advancement",
		"rewards=true",
		"names=words",
		"spacing=1",
		"spacing=2",
		"exists=replace",
		"exists=merge",
		"exists=copy",
		"group="
	};

	public boolean translate() {
		return !"advancement".equals(this.tasks);
	}

	public boolean free() {
		return "free".equals(this.order);
	}

	public boolean advancementKind() {
		return "advancement".equals(this.kind);
	}

	public AdvOptions sanitize() {
		this.tasks = one(this.tasks, "translate", "advancement");
		this.order = one(this.order, "tree", "free");
		this.kind = one(this.kind, "quest", "advancement");
		this.exists = one(this.exists, "merge", "replace", "copy");
		if (!Float.isFinite(this.spacing)) {
			this.spacing = 1.5F;
		}

		this.spacing = Math.max(1.0F, Math.min(3.0F, (float)Math.round(this.spacing * 4.0F) / 4.0F));
		this.group = this.group == null ? "" : this.group.trim();
		this.groupTitle = this.groupTitle == null ? "" : this.groupTitle.trim();
		if (this.groupTitle.length() > 128) {
			this.groupTitle = this.groupTitle.substring(0, 128);
		}

		if (this.names == null) {
			this.names = new LinkedHashMap<>();
		}

		this.names.values().removeIf(nx -> nx == null);

		for (AdvOptions.Name n : this.names.values()) {
			n.id = n.id == null ? "" : n.id.trim();
			n.title = n.title == null ? "" : n.title.trim();
			if (n.title.length() > 256) {
				n.title = n.title.substring(0, 256);
			}
		}

		return this;
	}

	private static String one(String value, String... allowed) {
		String v = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);

		for (String a : allowed) {
			if (a.equals(v)) {
				return a;
			}
		}

		return allowed[0];
	}

	public boolean word(String word) {
		int eq = word.indexOf(61);
		if (eq <= 0) {
			return false;
		} else {
			String key = word.substring(0, eq).trim().toLowerCase(Locale.ROOT);
			String value = word.substring(eq + 1).trim();
			switch (key) {
				case "tasks":
					this.tasks = value;
					break;
				case "order":
					this.order = value;
					break;
				case "kind":
					this.kind = value;
					break;
				case "rewards":
					if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("on") && !value.equalsIgnoreCase("yes")) {
						if (!value.equalsIgnoreCase("false") && !value.equalsIgnoreCase("off") && !value.equalsIgnoreCase("no")) {
							return false;
						}

						this.rewards = false;
					} else {
						this.rewards = true;
					}
					break;
				case "names":
					this.keys = !value.equalsIgnoreCase("words") && !value.equalsIgnoreCase("text");
					break;
				case "spacing":
					try {
						this.spacing = Float.parseFloat(value);
						break;
					} catch (NumberFormatException var8) {
						return false;
					}
				case "group":
					this.group = value;
					break;
				case "exists":
					this.exists = value;
					break;
				default:
					return false;
			}

			return true;
		}
	}

	public static final class Name {
		public String id = "";
		public String title = "";

		public Name() {
		}

		public Name(String id, String title) {
			this.id = id;
			this.title = title;
		}
	}
}
