package com.onehumanawa.cfhcore.content.gui.quests.client.ui;

import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class Trees {
	public static Chapter top(QuestBook book, Chapter chapter) {
		Chapter top = chapter;

		for (int guard = 0; top.isSubtree() && guard < 32; guard++) {
			Chapter parent = book.chapter(top.parent);
			if (parent == null || parent == top) {
				break;
			}

			top = parent;
		}

		return top;
	}

	public static List<Trees.Branch> branches(QuestBook book, Chapter top) {
		return branches(book, top, true);
	}

	public static List<Trees.Branch> branches(QuestBook book, Chapter top, boolean inlineToo) {
		List<Trees.Branch> out = new ArrayList<>();
		List<Chapter> seen = new ArrayList<>();
		out.add(new Trees.Branch(top, 0, null, null));
		seen.add(top);
		collect(book, top, 1, out, seen, inlineToo);
		return out;
	}

	private static void collect(QuestBook book, Chapter parent, int depth, List<Trees.Branch> into, List<Chapter> seen, boolean inlineToo) {
		if (depth <= 12 && seen.size() <= 256) {
			for (Chapter sub : book.subtrees(parent.id)) {
				if (!seen.contains(sub)) {
					seen.add(sub);
					if (!inlineToo && inline(book, sub)) {
						collect(book, sub, depth, into, seen, false);
					} else {
						into.add(new Trees.Branch(sub, depth, parent, gateway(parent, sub)));
						collect(book, sub, depth + 1, into, seen, inlineToo);
					}
				}
			}
		}
	}

	@Nullable
	public static String gateway(Chapter parent, Chapter sub) {
		for (Quest q : parent.quests) {
			if (q.isGateway() && q.subtree.equals(sub.id)) {
				return Ids.key(parent, q);
			}
		}

		return null;
	}

	@Nullable
	public static String wayDown(QuestBook book, Chapter above, Chapter below) {
		Chapter c = below;

		for (int guard = 0; c.isSubtree() && guard < 32; guard++) {
			Chapter parent = book.chapter(c.parent);
			if (parent == null || parent == c) {
				return null;
			}

			if (parent == above) {
				return gateway(above, c);
			}

			c = parent;
		}

		return null;
	}

	public static boolean inline(QuestBook book, Chapter chapter) {
		if (!chapter.isSubtree()) {
			return false;
		} else {
			Chapter parent = book.chapter(chapter.parent);
			if (parent != null && parent != chapter) {
				for (Quest q : parent.quests) {
					if (q.isGateway() && q.subtree.equals(chapter.id)) {
						return q.inline;
					}
				}

				return false;
			} else {
				return false;
			}
		}
	}

	public static Chapter host(QuestBook book, Chapter chapter) {
		Chapter c = chapter;

		for (int guard = 0; guard < 32 && inline(book, c); guard++) {
			Chapter parent = book.chapter(c.parent);
			if (parent == null) {
				break;
			}

			c = parent;
		}

		return c;
	}

	public static List<String> waysIn(QuestBook book, Chapter chapter) {
		List<String> out = new ArrayList<>();
		Chapter c = chapter;

		for (int guard = 0; guard < 32 && inline(book, c); guard++) {
			Chapter parent = book.chapter(c.parent);
			String way = parent == null ? null : gateway(parent, c);
			if (way == null) {
				break;
			}

			out.add(0, way);
			c = parent;
		}

		return out;
	}

	private Trees() {
	}

	public static record Branch(Chapter chapter, int depth, @Nullable Chapter parent, @Nullable String gateway) {
	}
}
