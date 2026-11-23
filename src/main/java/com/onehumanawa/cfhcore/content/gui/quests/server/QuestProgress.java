package com.onehumanawa.cfhcore.content.gui.quests.server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

final class QuestProgress {
	long[] tasks = new long[0];
	boolean done;
	long doneAt;
	long startedAt;
	String doneBy = "";
	String doneById = "";
	int times;
	long readyAt;
	Map<String, Long> claims = new HashMap<>();
	long teamClaims;

	long task(int index) {
		return index >= 0 && index < this.tasks.length ? this.tasks[index] : 0L;
	}

	void task(int index, long value) {
		if (index >= this.tasks.length) {
			this.tasks = Arrays.copyOf(this.tasks, index + 1);
		}

		this.tasks[index] = value;
	}

	long claimsOf(UUID player) {
		Long bits = this.claims.get(player.toString());
		return bits == null ? 0L : bits;
	}

	void claim(UUID player, int reward) {
		this.claims.merge(player.toString(), 1L << reward, (a, b) -> a | b);
	}

	boolean any() {
		if (!this.done && this.times <= 0 && this.teamClaims == 0L && this.startedAt == 0L && this.claims.isEmpty()) {
			for (long t : this.tasks) {
				if (t != 0L) {
					return true;
				}
			}

			return false;
		} else {
			return true;
		}
	}

	void startOver() {
		Arrays.fill(this.tasks, 0L);
		this.done = false;
		this.readyAt = 0L;
		this.claims.clear();
		this.teamClaims = 0L;
	}

	QuestProgress copy(@Nullable UUID only) {
		QuestProgress c = new QuestProgress();
		c.tasks = (long[])this.tasks.clone();
		c.done = this.done;
		c.doneAt = this.doneAt;
		c.startedAt = this.startedAt;
		c.doneBy = this.doneBy;
		c.doneById = this.doneById;
		c.times = this.times;
		c.readyAt = this.readyAt;
		c.teamClaims = this.teamClaims;
		if (only == null) {
			c.claims.putAll(this.claims);
		} else if (this.claimsOf(only) != 0L) {
			c.claims.put(only.toString(), this.claimsOf(only));
		}

		return c;
	}

	boolean merge(QuestProgress other, UUID player) {
		if (other.tasks.length > this.tasks.length) {
			this.tasks = Arrays.copyOf(this.tasks, other.tasks.length);
		}

		for (int i = 0; i < other.tasks.length; i++) {
			this.tasks[i] = Math.max(this.tasks[i], other.tasks[i]);
		}

		boolean became = false;
		if (other.done && !this.done) {
			this.done = true;
			this.doneAt = other.doneAt;
			this.doneBy = other.doneBy;
			this.doneById = other.doneById;
			became = true;
		}

		this.times = Math.max(this.times, other.times);
		this.readyAt = Math.max(this.readyAt, other.readyAt);
		if (this.startedAt == 0L || other.startedAt != 0L && other.startedAt < this.startedAt) {
			this.startedAt = other.startedAt;
		}

		long theirs = other.claimsOf(player);
		if (theirs != 0L) {
			this.claims.merge(player.toString(), theirs, (a, b) -> a | b);
		}

		this.teamClaims = this.teamClaims | other.teamClaims;
		return became;
	}

	QuestProgress sanitize() {
		if (this.tasks == null) {
			this.tasks = new long[0];
		}

		if (this.doneBy == null) {
			this.doneBy = "";
		}

		if (this.doneById == null) {
			this.doneById = "";
		}

		if (this.claims == null) {
			this.claims = new HashMap<>();
		}

		this.claims.values().removeIf(v -> v == null);
		if (this.times < 0) {
			this.times = 0;
		}

		if (this.done && this.times == 0) {
			this.times = 1;
		}

		return this;
	}
}
