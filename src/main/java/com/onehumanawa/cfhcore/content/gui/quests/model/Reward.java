package com.onehumanawa.cfhcore.content.gui.quests.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Reward {
	public static final String ITEM = "item";
	public static final String XP = "xp";
	public static final String COMMAND = "command";
	public static final String LOOT = "loot";
	public static final String CHOICE = "choice";
	public static final String RANDOM = "random";
	public static final String TEAM_XP = "team_xp";
	public static final String ADVANCEMENT = "advancement";
	public static final String ALL = "all";
	public static final String TOAST = "toast";
	public static final String STAGE = "stage";
	public static final String COUNTER = "counter";
	public static final String CURRENCY = "currency";
	public static final String CUSTOM = "custom";
	public String type = "";
	public String title = "";
	public String icon = "";
	public String item = "";
	public int count = 1;
	public int bonus;
	public int xp;
	public boolean levels;
	public String command = "";
	public int level;
	public String text = "";
	public String loot = "";
	public String advancement = "";
	public String criterion = "";
	public List<Reward> choices = new ArrayList<>();
	public int weight = 1;
	public int rolls = 1;
	public int empty;
	public String value = "";
	public boolean remove;
	public boolean team;
	public boolean auto;
	public boolean hidden;
	public boolean excluded;

	public boolean is(String kind) {
		return kind.equals(this.type);
	}

	public boolean group() {
		return this.is("choice") || this.is("random") || this.is("all");
	}

	public boolean atOnce() {
		return this.auto || this.hidden;
	}

	public void sanitize() {
		if (this.title == null) {
			this.title = "";
		}

		if (this.icon == null) {
			this.icon = "";
		}

		if (this.item == null) {
			this.item = "";
		}

		if (this.command == null) {
			this.command = "";
		}

		if (this.text == null) {
			this.text = "";
		}

		if (this.loot == null) {
			this.loot = "";
		}

		if (this.advancement == null) {
			this.advancement = "";
		}

		if (this.criterion == null) {
			this.criterion = "";
		}

		if (this.value == null) {
			this.value = "";
		}

		if (this.choices == null) {
			this.choices = new ArrayList<>();
		}

		this.choices.removeIf(cx -> cx == null);

		for (Reward c : this.choices) {
			c.sanitize();
		}

		if (this.type == null || this.type.isBlank()) {
			this.type = !this.item.isEmpty()
				? "item"
				: (
					!this.command.isEmpty()
						? "command"
						: (!this.loot.isEmpty() ? "loot" : (!this.advancement.isEmpty() ? "advancement" : (!this.choices.isEmpty() ? "choice" : "xp")))
				);
		}

		this.type = this.type.trim().toLowerCase(Locale.ROOT);
		if (this.count < 1) {
			this.count = 1;
		}

		if (this.bonus < 0) {
			this.bonus = 0;
		}

		if (this.weight < 1) {
			this.weight = 1;
		}

		if (this.rolls < 1) {
			this.rolls = 1;
		}

		if (this.empty < 0) {
			this.empty = 0;
		}

		if (this.level < 0 || this.level > 4) {
			this.level = 0;
		}

		if (this.xp < 0) {
			this.xp = 0;
		}
	}
}
