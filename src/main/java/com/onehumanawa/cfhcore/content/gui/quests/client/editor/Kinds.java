package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.List;

final class Kinds {
	static final List<String> TASKS = List.of(
		"item",
		"craft",
		"kill",
		"break",
		"place",
		"advancement",
		"stat",
		"xp",
		"location",
		"observe",
		"checkmark",
		"quest",
		"chapter",
		"team",
		"custom",
		"fluid",
		"energy",
		"stage",
		"counter"
	);
	static final List<String> REWARDS = List.of(
		"item", "xp", "command", "loot", "choice", "random", "team_xp", "advancement", "all", "toast", "stage", "counter", "currency", "custom"
	);
	static final List<String> TEAM_VALUES = List.of("members", "claims", "level");

	static Icon taskIcon(String type) {
		return switch (type) {
			case "item" -> Icon.PACK;
			case "craft" -> Icon.HAMMER;
			case "kill" -> Icon.SWORD;
			case "break" -> Icon.CHISEL;
			case "place" -> Icon.BLOCK;
			case "advancement" -> Icon.TROPHY;
			case "stat" -> Icon.SIGNAL;
			case "xp" -> Icon.SPARKLE;
			case "location" -> Icon.COMPASS;
			case "observe" -> Icon.EYE;
			case "checkmark" -> Icon.CHECK;
			case "quest", "chapter" -> Icon.QUEST;
			case "team" -> Icon.TEAM;
			case "fluid" -> Icon.CLOUD;
			case "energy" -> Icon.BOLT;
			case "stage" -> Icon.TAG;
			case "counter" -> Icon.HASH;
			default -> Icon.CODE;
		};
	}

	static Icon rewardIcon(String type) {
		return switch (type) {
			case "item" -> Icon.PACK;
			case "xp" -> Icon.SPARKLE;
			case "command" -> Icon.TERMINAL;
			case "loot" -> Icon.FOLDER;
			case "choice" -> Icon.LIST;
			case "random" -> Icon.QUESTION;
			case "team_xp" -> Icon.TEAM;
			case "all" -> Icon.LAYERS;
			case "toast" -> Icon.BELL;
			case "stage" -> Icon.TAG;
			case "counter" -> Icon.HASH;
			case "currency" -> Icon.STAR;
			case "custom" -> Icon.CODE;
			default -> Icon.TROPHY;
		};
	}

	static Task task(String type) {
		Task t = new Task();
		t.type = type;
		if (type.equals("team")) {
			t.value = "members";
			t.count = 2L;
		}

		if (type.equals("observe")) {
			t.seconds = 3;
		}

		if (type.equals("xp")) {
			t.count = 5L;
		}

		if (type.equals("fluid")) {
			t.fluid = "minecraft:water";
			t.count = 1000L;
			t.consume = true;
		}

		if (type.equals("energy")) {
			t.count = 10000L;
		}

		t.sanitize();
		return t;
	}

	static Reward reward(String type) {
		Reward r = new Reward();
		r.type = type;
		if (type.equals("xp")) {
			r.xp = 50;
		}

		if (type.equals("team_xp")) {
			r.xp = 25;
		}

		if (type.equals("currency")) {
			r.count = 10;
		}

		r.sanitize();
		return r;
	}

	static Task copy(Task task) {
		return (Task)BookJson.GSON.fromJson(BookJson.GSON.toJson(task), Task.class);
	}

	static Reward copy(Reward reward) {
		return (Reward)BookJson.GSON.fromJson(BookJson.GSON.toJson(reward), Reward.class);
	}

	static Task tidy(Task t) {
		String type = t.type;
		boolean item = type.equals("item") || type.equals("craft");
		boolean block = type.equals("break") || type.equals("place") || type.equals("observe");
		boolean entity = type.equals("kill") || type.equals("observe");
		boolean place = type.equals("location");
		if (!item) {
			t.item = "";
		}

		if (!block) {
			t.block = "";
		}

		if (!entity) {
			t.entity = "";
		}

		if (!type.equals("advancement")) {
			t.advancement = "";
		}

		if (!type.equals("stat")) {
			t.stat = "";
		}

		if (!place) {
			t.dimension = t.biome = t.structure = "";
			t.x = t.y = t.z = t.radius = 0;
			t.sizeX = t.sizeY = t.sizeZ = 0;
		}

		if (!type.equals("xp")) {
			t.points = false;
		}

		if (!type.equals("item") && !type.equals("xp") && !type.equals("fluid")) {
			t.consume = false;
		}

		if (!type.equals("observe")) {
			t.seconds = 1;
		}

		if (!type.equals("quest")) {
			t.quest = "";
		}

		if (!type.equals("chapter")) {
			t.chapter = "";
		}

		if (!type.equals("team") && !type.equals("custom") && !type.equals("stage") && !type.equals("counter")) {
			t.value = "";
		}

		if (!type.equals("kill")) {
			t.name = "";
		}

		if (!type.equals("advancement")) {
			t.criterion = "";
		}

		if (!type.equals("fluid")) {
			t.fluid = "";
		}

		t.name = t.name.trim();
		t.criterion = t.criterion.trim();
		t.title = t.title.trim();
		t.sanitize();
		return t;
	}

	static Reward tidy(Reward r) {
		String type = r.type;
		if (!type.equals("item")) {
			r.item = "";
			r.bonus = 0;
		}

		if (!type.equals("item") && !type.equals("counter") && !type.equals("currency")) {
			r.count = 1;
		}

		if (!type.equals("xp") && !type.equals("team_xp")) {
			r.xp = 0;
		}

		if (!type.equals("xp")) {
			r.levels = false;
		}

		if (!type.equals("command")) {
			r.command = "";
			r.level = 0;
		}

		if (!type.equals("command") && !type.equals("toast")) {
			r.text = "";
		}

		if (!type.equals("loot")) {
			r.loot = "";
		}

		if (!type.equals("advancement")) {
			r.advancement = "";
			r.criterion = "";
		}

		if (!r.group()) {
			r.choices.clear();
		}

		if (!type.equals("random")) {
			r.rolls = 1;
			r.empty = 0;
		}

		if (!type.equals("stage") && !type.equals("counter") && !type.equals("custom")) {
			r.value = "";
		}

		if (!type.equals("stage")) {
			r.remove = false;
		}

		r.value = r.value.trim();
		r.criterion = r.criterion.trim();
		r.title = r.title.trim();

		for (Reward c : r.choices) {
			tidy(c);
		}

		r.sanitize();
		return r;
	}

	static boolean counts(String type) {
		return !type.equals("advancement")
			&& !type.equals("location")
			&& !type.equals("checkmark")
			&& !type.equals("quest")
			&& !type.equals("chapter")
			&& !type.equals("observe")
			&& !type.equals("stage");
	}

	private Kinds() {
	}
}
