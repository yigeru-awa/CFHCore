package com.onehumanawa.cfhcore.content.gui.quests.server;

import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

final class Seat {
	final UUID id;
	final String name;
	@Nullable
	ServerPlayer player;
	Holder own;
	Holder team;
	final Set<String> open = new HashSet<>();
	final Set<String> looking = new HashSet<>();
	final Set<String> observing = new HashSet<>();
	final Map<TriggerIndex.Ref, Integer> gazes = new HashMap<>();
	final Set<String> finished = new HashSet<>();
	int actionsSince;
	int actions;
	final Rules.Progress progress;

	Seat(UUID id, String name, @Nullable ServerPlayer player, final QuestEngine engine) {
		this.id = id;
		this.name = name;
		this.player = player;
		this.progress = new Rules.Progress() {
			@Override
			public boolean done(String key) {
				QuestProgress p = engine.peek(Seat.this, key);
				return p != null && p.done;
			}

			@Override
			public boolean everDone(String key) {
				QuestProgress p = engine.peek(Seat.this, key);
				return p != null && (p.done || p.times > 0);
			}

			@Override
			public boolean started(String key) {
				QuestProgress p = engine.peek(Seat.this, key);
				return p != null && (p.done || p.times > 0 || p.startedAt != 0L);
			}

			@Override
			public long task(String key, int index) {
				QuestProgress p = engine.peek(Seat.this, key);
				return p == null ? 0L : p.task(index);
			}

			@Override
			public boolean claimable(String key) {
				BookIndex.Entry e = engine.index().get(key);
				QuestProgress p = e == null ? null : engine.peek(Seat.this, key);
				return p != null && p.done && !e.quest().autoClaim && !e.quest().isAdvancement()
					? (engine.unclaimed(Seat.this, e, p) & ~QuestEngine.atOnce(e.quest())) != 0L
					: false;
			}
		};
	}

	Holder holder(BookIndex.Entry entry) {
		return entry.sync() == Quest.Sync.TEAM ? this.team : this.own;
	}
}
