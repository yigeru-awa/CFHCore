package com.onehumanawa.cfhcore.content.gui.quests.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

final class Holder {
	final UUID id;
	final Map<String, QuestProgress> quests = new HashMap<>();
	final Set<String> members = new HashSet<>();
	final Map<String, Long> counters = new HashMap<>();
	long emergencyAt;
	final List<Seat> seats = new ArrayList<>();
	boolean dirty;
	boolean queued;
	long dirtyAt;
	final Set<String> changed = new LinkedHashSet<>();
	final Set<String> resting = new HashSet<>();
	Map<TriggerIndex.Ref, Holder.Best> levels = new HashMap<>();
	Map<TriggerIndex.Ref, Holder.Best> looking = new HashMap<>();

	Holder(UUID id) {
		this.id = id;
	}

	@Nullable
	QuestProgress get(String key) {
		return this.quests.get(key);
	}

	QuestProgress progress(String key) {
		return this.quests.computeIfAbsent(key, k -> new QuestProgress());
	}

	long counter(String name) {
		Long v = this.counters.get(name);
		return v == null ? 0L : v;
	}

	boolean done(String key) {
		QuestProgress p = this.quests.get(key);
		return p != null && p.done;
	}

	static final class Best {
		long value;
		Seat seat;

		Best(long value, Seat seat) {
			this.value = value;
			this.seat = seat;
		}
	}
}
