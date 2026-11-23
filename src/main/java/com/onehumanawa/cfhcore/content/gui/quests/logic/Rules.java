package com.onehumanawa.cfhcore.content.gui.quests.logic;

import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.HashSet;
import java.util.Set;

public final class Rules {
	public static boolean chapterOpen(BookIndex index, Chapter chapter, Rules.Progress progress) {
		return chapterOpen(index, chapter, progress, new HashSet<>());
	}

	private static boolean chapterOpen(BookIndex index, Chapter chapter, Rules.Progress progress, Set<String> walking) {
		if (!walking.add(chapter.id)) {
			return true;
		} else {
			try {
				for (String required : chapter.requires) {
					String key = Ids.resolve(chapter.id, required);
					if (index.get(key) != null && !progress.everDone(key)) {
						return false;
					}
				}

				if (chapter.isSubtree()) {
					boolean any = false;
					boolean open = false;

					for (String gateway : index.gateways(chapter.id)) {
						any = true;
						BookIndex.Entry g = index.get(gateway);
						if (g != null && (progress.everDone(gateway) || questOpen(index, g, progress, walking))) {
							open = true;
							break;
						}
					}

					if (any && !open) {
						return false;
					}

					Chapter parent = index.book().chapter(chapter.parent);
					if (!any && parent != null && !chapterOpen(index, parent, progress, walking)) {
						return false;
					}
				}

				return true;
			} finally {
				walking.remove(chapter.id);
			}
		}
	}

	public static boolean chapterDone(BookIndex index, Chapter chapter, Rules.Progress progress) {
		int counted = 0;

		for (Quest q : chapter.quests) {
			if (!q.optional) {
				counted++;
				if (!progress.everDone(Ids.key(chapter, q))) {
					return false;
				}
			}
		}

		return counted > 0;
	}

	public static int[] chapterCount(Chapter chapter, Rules.Progress progress) {
		int done = 0;
		int total = 0;

		for (Quest q : chapter.quests) {
			if (!q.optional) {
				total++;
				if (progress.everDone(Ids.key(chapter, q))) {
					done++;
				}
			}
		}

		return new int[]{done, total};
	}

	public static boolean questOpen(BookIndex index, BookIndex.Entry entry, Rules.Progress progress) {
		return questOpen(index, entry, progress, new HashSet<>());
	}

	private static boolean questOpen(BookIndex index, BookIndex.Entry entry, Rules.Progress progress, Set<String> walking) {
		return !chapterOpen(index, entry.chapter(), progress, walking) ? false : depsMet(index, entry, progress);
	}

	public static boolean depsMet(BookIndex index, BookIndex.Entry entry, Rules.Progress progress) {
		return depsMet(entry, progress) && !shut(index, entry, progress);
	}

	public static boolean depsMet(BookIndex.Entry entry, Rules.Progress progress) {
		if (!entry.deps().isEmpty() && !entry.quest().looseDeps) {
			boolean begun = entry.quest().depsStarted;
			int done = 0;

			for (String dep : entry.deps()) {
				if (begun ? progress.started(dep) : progress.everDone(dep)) {
					done++;
				}
			}

			return entry.quest().depMode == Quest.DepMode.ANY ? done >= Math.min(entry.deps().size(), Math.max(1, entry.quest().minDeps)) : done == entry.deps().size();
		} else {
			return true;
		}
	}

	public static boolean shut(BookIndex index, BookIndex.Entry entry, Rules.Progress progress) {
		for (String dep : entry.deps()) {
			BookIndex.Entry fork = index.get(dep);
			if (fork != null && fork.quest().maxDependents > 0) {
				int taken = 0;

				for (String other : index.dependents(dep)) {
					if (!other.equals(entry.key()) && progress.everDone(other)) {
						taken++;
					}
				}

				if (taken >= fork.quest().maxDependents) {
					return true;
				}
			}
		}

		return false;
	}

	public static Rules.Status status(BookIndex index, BookIndex.Entry entry, Rules.Progress progress) {
		String key = entry.key();
		if (progress.done(key)) {
			return progress.claimable(key) ? Rules.Status.CLAIMABLE : Rules.Status.COMPLETE;
		} else {
			Quest q = entry.quest();
			if (q.visibility == Quest.Visibility.INVISIBLE && !revealed(entry, progress)) {
				return Rules.Status.HIDDEN;
			} else if (!questOpen(index, entry, progress)) {
				return unseen(index, entry, progress, new HashSet<>()) ? Rules.Status.HIDDEN : Rules.Status.LOCKED;
			} else {
				return fraction(index, entry, progress) > 0.0F ? Rules.Status.IN_PROGRESS : Rules.Status.AVAILABLE;
			}
		}
	}

	private static boolean revealed(BookIndex.Entry entry, Rules.Progress progress) {
		Quest q = entry.quest();
		if (q.revealTasks <= 0) {
			return false;
		} else {
			int done = 0;

			for (int i = 0; i < q.tasks.size(); i++) {
				if (taskDone(q.tasks.get(i), progress.task(entry.key(), i))) {
					done++;
				}
			}

			return done >= Math.min(q.revealTasks, q.tasks.size());
		}
	}

	private static boolean unseen(BookIndex index, BookIndex.Entry entry, Rules.Progress progress, Set<String> walking) {
		Quest.Visibility v = entry.quest().visibility;
		if (v != Quest.Visibility.HIDDEN && v != Quest.Visibility.INVISIBLE) {
			if (v == Quest.Visibility.FOLLOW && walking.add(entry.key())) {
				for (String dep : entry.deps()) {
					BookIndex.Entry d = index.get(dep);
					if (d != null && !progress.everDone(dep)) {
						if (d.quest().visibility == Quest.Visibility.INVISIBLE) {
							if (!revealed(d, progress)) {
								return true;
							}
						} else if (!questOpen(index, d, progress) && unseen(index, d, progress, walking)) {
							return true;
						}
					}
				}

				return false;
			} else {
				return false;
			}
		} else {
			return true;
		}
	}

	public static float fraction(BookIndex index, BookIndex.Entry entry, Rules.Progress progress) {
		String key = entry.key();
		if (progress.done(key)) {
			return 1.0F;
		} else {
			Quest q = entry.quest();
			if (q.tasks.isEmpty()) {
				if (!q.isGateway()) {
					return 0.0F;
				} else {
					Chapter sub = index.book().chapter(q.subtree);
					if (sub == null) {
						return 0.0F;
					} else {
						int[] c = chapterCount(sub, progress);
						return c[1] == 0 ? 0.0F : (float)c[0] / (float)c[1];
					}
				}
			} else {
				boolean every = allOptional(q);
				float sum = 0.0F;
				float best = 0.0F;
				int counted = 0;

				for (int i = 0; i < q.tasks.size(); i++) {
					Task t = q.tasks.get(i);
					if (!t.optional || every) {
						float f = taskFraction(t, progress.task(key, i));
						sum += f;
						best = Math.max(best, f);
						counted++;
					}
				}

				return !q.anyTask && !every ? sum / (float)Math.max(1, counted) : best;
			}
		}
	}

	private static boolean allOptional(Quest quest) {
		for (Task t : quest.tasks) {
			if (!t.optional) {
				return false;
			}
		}

		return !quest.tasks.isEmpty();
	}

	public static boolean taskOpen(Quest quest, long[] counted, int index) {
		if (!quest.sequential) {
			return true;
		} else {
			for (int i = 0; i < index && i < quest.tasks.size(); i++) {
				if (!taskDone(quest.tasks.get(i), i < counted.length ? counted[i] : 0L)) {
					return false;
				}
			}

			return true;
		}
	}

	public static float taskFraction(Task task, long counted) {
		return task.count <= 0L ? 1.0F : Math.max(0.0F, Math.min(1.0F, (float)counted / (float)task.count));
	}

	public static boolean taskDone(Task task, long counted) {
		return counted >= task.count;
	}

	public static boolean tasksDone(Quest quest, long[] counted) {
		if (quest.tasks.isEmpty()) {
			return false;
		} else {
			boolean any = quest.anyTask || allOptional(quest);
			boolean all = true;

			for (int i = 0; i < quest.tasks.size(); i++) {
				Task t = quest.tasks.get(i);
				boolean d = taskDone(t, i < counted.length ? counted[i] : 0L);
				if (d && any) {
					return true;
				}

				if (!t.optional) {
					all &= d;
				}
			}

			return all && !any;
		}
	}

	private Rules() {
	}

	public interface Progress {
		boolean done(String var1);

		default boolean everDone(String key) {
			return this.done(key);
		}

		default boolean started(String key) {
			return this.everDone(key);
		}

		long task(String var1, int var2);

		boolean claimable(String var1);
	}

	public static enum Status {
		HIDDEN,
		LOCKED,
		AVAILABLE,
		IN_PROGRESS,
		CLAIMABLE,
		COMPLETE;

		public boolean open() {
			return this == AVAILABLE || this == IN_PROGRESS;
		}

		public boolean done() {
			return this == CLAIMABLE || this == COMPLETE;
		}

		public boolean visible() {
			return this != HIDDEN;
		}
	}
}
