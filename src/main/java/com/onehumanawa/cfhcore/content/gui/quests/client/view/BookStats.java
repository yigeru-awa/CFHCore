package com.onehumanawa.cfhcore.content.gui.quests.client.view;

import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

public final class BookStats {
	private static final Map<String, BookStats.Stat> OWN = new HashMap<>();
	private static final Map<String, BookStats.Stat> DEEP = new HashMap<>();
	private static final Map<String, Boolean> OPEN = new HashMap<>();
	@Nullable
	private static BookStats.Stat book;
	private static int revision;

	private static void changed() {
		OWN.clear();
		DEEP.clear();
		OPEN.clear();
		book = null;
		revision++;
	}

	public static int revision() {
		return revision;
	}

	public static BookStats.Stat of(@Nullable Chapter chapter) {
		return chapter == null ? BookStats.Stat.NONE : OWN.computeIfAbsent(chapter.id, k -> count(chapter));
	}

	public static BookStats.Stat deep(@Nullable Chapter chapter) {
		if (chapter == null) {
			return BookStats.Stat.NONE;
		} else {
			BookStats.Stat had = DEEP.get(chapter.id);
			if (had != null) {
				return had;
			} else {
				BookStats.Stat s = deep(chapter, new HashSet<>());
				DEEP.put(chapter.id, s);
				return s;
			}
		}
	}

	private static BookStats.Stat deep(Chapter chapter, Set<String> walking) {
		BookStats.Stat s = of(chapter);
		QuestBook b = QuestsState.book();
		if (b != null && walking.add(chapter.id)) {
			for (Chapter sub : b.subtrees(chapter.id)) {
				if (!walking.contains(sub.id)) {
					s = s.plus(deep(sub, walking));
				}
			}

			return s;
		} else {
			return s;
		}
	}

	public static BookStats.Stat book() {
		if (book == null) {
			BookStats.Stat s = BookStats.Stat.NONE;
			QuestBook b = QuestsState.book();
			if (b != null) {
				for (Chapter c : b.chapters.values()) {
					if (!c.readOnly) {
						s = s.plus(of(c));
					}
				}
			}

			book = s;
		}

		return book;
	}

	public static boolean open(@Nullable Chapter chapter) {
		return chapter == null ? false : OPEN.computeIfAbsent(chapter.id, k -> QuestsState.chapterOpen(chapter));
	}

	public static List<String> needs(@Nullable Chapter chapter) {
		List<String> out = new ArrayList<>();
		QuestBook b = QuestsState.book();
		BookIndex index = QuestsState.index();
		if (chapter != null && b != null && index != null && !open(chapter)) {
			Set<String> walked = new HashSet<>();

			for (Chapter c = chapter; c != null && walked.add(c.id); c = c.isSubtree() ? b.chapter(c.parent) : null) {
				for (String need : c.requires) {
					String key = Ids.resolve(c.id, need);
					if (index.get(key) != null && !QuestsState.progress().everDone(key) && !out.contains(key)) {
						out.add(key);
					}
				}

				if (!c.isSubtree()) {
					break;
				}

				List<String> ways = new ArrayList<>();
				boolean through = false;

				for (String gateway : index.gateways(c.id)) {
					if (index.get(gateway) != null) {
						ways.add(gateway);
						Rules.Status s = QuestsState.status(gateway);
						if (s != Rules.Status.LOCKED && s != Rules.Status.HIDDEN) {
							through = true;
						}
					}
				}

				if (!ways.isEmpty()) {
					if (!through) {
						for (String gatewayx : ways) {
							if (!out.contains(gatewayx)) {
								out.add(gatewayx);
							}
						}
					}
					break;
				}
			}

			return out;
		} else {
			return out;
		}
	}

	private static BookStats.Stat count(Chapter chapter) {
		int done = 0;
		int total = 0;
		int claimable = 0;
		int open = 0;

		for (Quest q : chapter.quests) {
			Rules.Status status = QuestsState.status(Ids.key(chapter, q));
			if (!q.optional) {
				total++;
				if (status.done()) {
					done++;
				}
			}

			if (status == Rules.Status.CLAIMABLE) {
				claimable++;
			}

			if (status.open()) {
				open++;
			}
		}

		return new BookStats.Stat(done, total, claimable, open);
	}

	private BookStats() {
	}

	static {
		QuestsState.BOOK_CHANGED.register(BookStats::changed);
		QuestsState.PROGRESS_CHANGED.register((Consumer<List<String>>)keys -> changed());
	}

	public static record Stat(int done, int total, int claimable, int open) {
		public static final BookStats.Stat NONE = new BookStats.Stat(0, 0, 0, 0);

		public float fraction() {
			return this.total <= 0 ? 0.0F : Math.min(1.0F, (float)this.done / (float)this.total);
		}

		public boolean complete() {
			return this.total > 0 && this.done >= this.total;
		}

		BookStats.Stat plus(BookStats.Stat o) {
			return new BookStats.Stat(this.done + o.done, this.total + o.total, this.claimable + o.claimable, this.open + o.open);
		}
	}
}
