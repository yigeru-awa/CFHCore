package com.onehumanawa.cfhcore.content.gui.quests.model;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class Quest {
	public String id = "";
	public String title = "";
	public String subtitle = "";
	public List<String> description = new ArrayList<>();
	public String icon = "";
	public float x;
	public float y;
	@Nullable
	public Quest.Shape shape;
	public float size = 1.0F;
	public float iconScale = 1.0F;
	public Quest.Kind kind = Quest.Kind.QUEST;
	public Quest.Frame frame = Quest.Frame.TASK;
	@Nullable
	public Quest.Sync sync;
	public List<String> deps = new ArrayList<>();
	public Quest.DepMode depMode = Quest.DepMode.ALL;
	public int minDeps = 1;
	public boolean depsStarted;
	public int maxDependents;
	public Quest.Visibility visibility = Quest.Visibility.VISIBLE;
	public int revealTasks;
	public boolean hideLines;
	public boolean hideOutLines;
	public boolean hideText;
	public boolean hideDetails;
	public boolean looseDeps;
	public boolean optional;
	public List<Task> tasks = new ArrayList<>();
	public boolean anyTask;
	public boolean sequential;
	public String guide = "";
	public List<Reward> rewards = new ArrayList<>();
	public boolean autoClaim;
	public String subtree = "";
	public boolean inline;
	public boolean repeatable;
	public int cooldownSeconds;
	public List<String> tags = new ArrayList<>();
	public boolean silent;
	public int teamXp = -1;

	public boolean isGateway() {
		return this.subtree != null && !this.subtree.isEmpty();
	}

	public boolean isInline() {
		return this.inline && this.isGateway();
	}

	public boolean isAdvancement() {
		return this.kind == Quest.Kind.ADVANCEMENT;
	}

	public void sanitize() {
		if (this.id == null) {
			this.id = "";
		}

		if (this.title == null) {
			this.title = "";
		}

		if (this.subtitle == null) {
			this.subtitle = "";
		}

		if (this.description == null) {
			this.description = new ArrayList<>();
		}

		this.description.removeIf(s -> s == null);
		if (this.icon == null) {
			this.icon = "";
		}

		if (this.size <= 0.0F) {
			this.size = 1.0F;
		}

		this.size = Math.max(0.5F, Math.min(4.0F, this.size));
		if (this.iconScale <= 0.0F) {
			this.iconScale = 1.0F;
		}

		this.iconScale = Math.max(0.25F, Math.min(4.0F, this.iconScale));
		if (this.maxDependents < 0) {
			this.maxDependents = 0;
		}

		if (this.revealTasks < 0) {
			this.revealTasks = 0;
		}

		if (this.guide == null) {
			this.guide = "";
		}

		if (this.kind == null) {
			this.kind = Quest.Kind.QUEST;
		}

		if (this.frame == null) {
			this.frame = Quest.Frame.TASK;
		}

		if (this.deps == null) {
			this.deps = new ArrayList<>();
		}

		this.deps.removeIf(s -> s == null || s.isBlank());
		if (this.depMode == null) {
			this.depMode = Quest.DepMode.ALL;
		}

		if (this.minDeps < 1) {
			this.minDeps = 1;
		}

		if (this.visibility == null) {
			this.visibility = Quest.Visibility.VISIBLE;
		}

		if (this.tasks == null) {
			this.tasks = new ArrayList<>();
		}

		this.tasks.removeIf(t -> t == null);

		for (Task t : this.tasks) {
			t.sanitize();
		}

		if (this.rewards == null) {
			this.rewards = new ArrayList<>();
		}

		this.rewards.removeIf(rx -> rx == null);

		for (Reward r : this.rewards) {
			r.sanitize();
		}

		if (this.subtree == null) {
			this.subtree = "";
		}

		if (this.tags == null) {
			this.tags = new ArrayList<>();
		}

		this.tags.removeIf(s -> s == null);
		if (this.cooldownSeconds < 0) {
			this.cooldownSeconds = 0;
		}
	}

	public static enum DepMode {
		ALL,
		ANY;
	}

	public static enum Frame {
		TASK,
		GOAL,
		CHALLENGE;
	}

	public static enum Kind {
		QUEST,
		ADVANCEMENT;
	}

	public static enum Shape {
		CIRCLE,
		SQUARE,
		ROUNDED,
		DIAMOND,
		HEXAGON,
		OCTAGON,
		PENTAGON,
		SHIELD,
		GEAR,
		STAR,
		HEART,
		NONE;
	}

	public static enum Sync {
		TEAM,
		PLAYER;
	}

	public static enum Visibility {
		VISIBLE,
		HIDDEN,
		SECRET,
		FOLLOW,
		INVISIBLE;
	}
}
