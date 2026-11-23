package com.onehumanawa.cfhcore.content.gui.quests.logic;

import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public final class BookIndex {
	private final QuestBook book;
	private final Map<String, BookIndex.Entry> entries = new LinkedHashMap<>();
	private final Map<String, List<String>> dependents = new HashMap<>();
	private final Map<String, List<String>> gateways = new HashMap<>();

	public BookIndex(QuestBook book) {
		this.book = book;

		for (Chapter c : book.chapters.values()) {
			for (Quest q : c.quests) {
				String key = Ids.key(c, q);
				this.entries.put(key, new BookIndex.Entry(key, c, q, new ArrayList<>()));
				if (q.isGateway()) {
					this.gateways.computeIfAbsent(q.subtree, k -> new ArrayList<>()).add(key);
				}
			}
		}

		for (BookIndex.Entry e : this.entries.values()) {
			for (String dep : e.quest().deps) {
				String key = Ids.resolve(e.chapter().id, dep);
				if (!key.equals(e.key()) && this.entries.containsKey(key) && !e.deps().contains(key)) {
					e.deps().add(key);
					this.dependents.computeIfAbsent(key, k -> new ArrayList<>()).add(e.key());
				}
			}
		}
	}

	public QuestBook book() {
		return this.book;
	}

	@Nullable
	public BookIndex.Entry get(@Nullable String key) {
		return key == null ? null : this.entries.get(key);
	}

	public Iterable<BookIndex.Entry> all() {
		return this.entries.values();
	}

	public int size() {
		return this.entries.size();
	}

	public List<String> dependents(String key) {
		List<String> d = this.dependents.get(key);
		return d == null ? List.of() : d;
	}

	public List<String> gateways(String chapterId) {
		List<String> g = this.gateways.get(chapterId);
		return g == null ? List.of() : g;
	}

	public List<BookIndex.Entry> of(Chapter chapter) {
		List<BookIndex.Entry> out = new ArrayList<>(chapter.quests.size());

		for (Quest q : chapter.quests) {
			BookIndex.Entry e = this.entries.get(Ids.key(chapter, q));
			if (e != null) {
				out.add(e);
			}
		}

		return out;
	}

	public static record Entry(String key, Chapter chapter, Quest quest, List<String> deps) {
		public Quest.Sync sync() {
			if (this.quest.isAdvancement()) {
				return Quest.Sync.PLAYER;
			} else {
				return this.quest.sync != null ? this.quest.sync : this.chapter.sync;
			}
		}

		public Quest.Shape shape() {
			return this.quest.shape != null ? this.quest.shape : this.chapter.shape;
		}
	}
}
