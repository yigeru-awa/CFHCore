package com.onehumanawa.cfhcore.content.gui.quests.model;

public final class QuestState {
	public String key = "";
	public long[] tasks = new long[0];
	public boolean done;
	public long doneAt;
	public String doneBy = "";
	public long claimed;
	public int times;
	public long readyAt;
	public boolean started;

	public QuestState() {
	}

	public QuestState(String key) {
		this.key = key;
	}

	public long task(int index) {
		return this.tasks != null && index >= 0 && index < this.tasks.length ? this.tasks[index] : 0L;
	}

	public boolean claimed(int reward) {
		return reward >= 0 && reward < 64 && (this.claimed & 1L << reward) != 0L;
	}

	public boolean claimable(Quest quest) {
		if (this.done && !quest.autoClaim && !quest.isAdvancement()) {
			for (int i = 0; i < quest.rewards.size() && i < 64; i++) {
				if (!this.claimed(i) && !quest.rewards.get(i).atOnce()) {
					return true;
				}
			}

			return false;
		} else {
			return false;
		}
	}

	public boolean begun() {
		if (!this.done && this.times <= 0 && !this.started) {
			if (this.tasks != null) {
				for (long t : this.tasks) {
					if (t > 0L) {
						return true;
					}
				}
			}

			return false;
		} else {
			return true;
		}
	}

	public QuestState sanitize() {
		if (this.key == null) {
			this.key = "";
		}

		if (this.tasks == null) {
			this.tasks = new long[0];
		}

		if (this.doneBy == null) {
			this.doneBy = "";
		}

		return this;
	}
}
