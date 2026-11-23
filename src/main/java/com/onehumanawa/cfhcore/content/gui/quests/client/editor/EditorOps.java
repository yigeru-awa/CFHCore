package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;

final class EditorOps {
	static final float GRID = 0.5F;

	static float snap(float v) {
		return tidy((float)Math.round(v / 0.5F) * 0.5F);
	}

	static float tidy(float v) {
		float r = (float)Math.round(v * 100.0F) / 100.0F;
		return r == 0.0F ? 0.0F : r;
	}

	static String validId(@Nullable String wanted, String fallback) {
		return Ids.valid(wanted) ? wanted : Ids.slug(wanted, fallback);
	}

	static String free(String base, Predicate<String> taken) {
		if (!taken.test(base)) {
			return base;
		} else {
			String stem = base.replaceFirst("_\\d+$", "");
			String root = stem.isEmpty() ? base : stem;

			for (int n = 2; n < 100000; n++) {
				String id = (root.length() > 56 ? root.substring(0, 56) : root) + "_" + n;
				if (!taken.test(id)) {
					return id;
				}
			}

			return root + "_" + System.nanoTime();
		}
	}

	static String freeQuestId(Chapter chapter, @Nullable String wanted, Set<String> also) {
		return free(validId(wanted, "quest"), id -> chapter.quest(id) != null || also.contains(id));
	}

	static String freeChapterId(QuestBook book, @Nullable String wanted) {
		return free(validId(wanted, "chapter"), book.chapters::containsKey);
	}

	static String freeGroupId(QuestBook book, @Nullable String wanted) {
		return free(validId(wanted, "group"), id -> book.group(id) != null);
	}

	static boolean generated(String id) {
		return id.matches("quest_[0-9a-z]{4}");
	}

	static String generatedId(Chapter chapter) {
		ThreadLocalRandom random = ThreadLocalRandom.current();

		for (int guard = 0; guard < 1000; guard++) {
			StringBuilder sb = new StringBuilder("quest_");

			for (int i = 0; i < 4; i++) {
				sb.append(Character.forDigit(random.nextInt(36), 36));
			}

			if (chapter.quest(sb.toString()) == null) {
				return sb.toString();
			}
		}

		return freeQuestId(chapter, "quest", Set.of());
	}

	static Quest newQuest(Chapter chapter, float x, float y) {
		Quest q = new Quest();
		q.id = generatedId(chapter);
		q.x = tidy(x);
		q.y = tidy(y);
		Task task = new Task();
		task.type = "checkmark";
		q.tasks.add(task);
		q.sanitize();
		chapter.quests.add(q);
		return q;
	}

	static void dropPlaceholder(Quest quest) {
		if (quest.tasks.size() == 1) {
			Task only = quest.tasks.get(0);
			if (only.is("checkmark") && only.title.isBlank() && only.icon.isBlank()) {
				quest.tasks.clear();
			}
		}
	}

	static void reId(Chapter chapter, String from, String to) {
		Quest quest = chapter.quest(from);
		if (quest != null && !from.equals(to)) {
			String key = Ids.key(chapter.id, from);
			quest.id = to;

			for (Quest q : chapter.quests) {
				q.deps.replaceAll(d -> !d.equals(from) && !d.equals(key) ? d : to);
			}
		}
	}

	static boolean dependsOn(Chapter chapter, Quest quest, String onId) {
		String key = Ids.key(chapter.id, onId);

		for (String d : quest.deps) {
			if (Ids.resolve(chapter.id, d).equals(key)) {
				return true;
			}
		}

		return false;
	}

	static boolean waitsFor(Chapter chapter, String id, String onId) {
		return waitsFor(chapter, id, onId, new HashSet<>());
	}

	private static boolean waitsFor(Chapter chapter, String id, String onId, Set<String> seen) {
		if (!seen.add(id)) {
			return false;
		} else {
			Quest q = chapter.quest(id);
			if (q == null) {
				return false;
			} else {
				String prefix = chapter.id + "/";

				for (String d : q.deps) {
					String dep = d.startsWith(prefix) ? d.substring(prefix.length()) : d;
					if (dep.indexOf(47) < 0 && (dep.equals(onId) || waitsFor(chapter, dep, onId, seen))) {
						return true;
					}
				}

				return false;
			}
		}
	}

	static EditorOps.Link canLink(Chapter chapter, String from, String to) {
		if (from.equals(to)) {
			return EditorOps.Link.SELF;
		} else {
			Quest target = chapter.quest(to);
			if (target == null || chapter.quest(from) == null) {
				return EditorOps.Link.GONE;
			} else if (dependsOn(chapter, target, from)) {
				return EditorOps.Link.EXISTS;
			} else {
				return waitsFor(chapter, from, to) ? EditorOps.Link.CIRCLE : EditorOps.Link.OK;
			}
		}
	}

	static EditorOps.Link link(Chapter chapter, String from, String to) {
		EditorOps.Link can = canLink(chapter, from, to);
		if (can == EditorOps.Link.OK) {
			chapter.quest(to).deps.add(from);
		}

		return can;
	}

	static boolean unlink(Chapter chapter, String from, String to) {
		Quest target = chapter.quest(to);
		if (target == null) {
			return false;
		} else {
			String key = Ids.key(chapter.id, from);
			return target.deps.removeIf(d -> Ids.resolve(chapter.id, d).equals(key));
		}
	}

	static void delete(Chapter chapter, Collection<String> questIds, Collection<Integer> decorations) {
		if (!questIds.isEmpty()) {
			Set<String> keys = new HashSet<>();

			for (String id : questIds) {
				keys.add(Ids.key(chapter.id, id));
			}

			chapter.quests.removeIf(q -> questIds.contains(q.id));

			for (Quest q : chapter.quests) {
				q.deps.removeIf(d -> keys.contains(Ids.resolve(chapter.id, d)));
			}
		}

		List<Integer> order = new ArrayList<>(decorations);
		order.sort(Comparator.reverseOrder());

		for (int i : order) {
			if (i >= 0 && i < chapter.decorations.size()) {
				chapter.decorations.remove(i);
			}
		}
	}

	static EditorOps.Clip copy(Chapter chapter, Collection<String> questIds, Collection<Integer> decorations) {
		EditorOps.Clip clip = new EditorOps.Clip();
		clip.chapter = chapter.id;

		for (Quest q : chapter.quests) {
			if (questIds.contains(q.id)) {
				clip.quests.add(BookJson.copy(q));
			}
		}

		for (int i = 0; i < chapter.decorations.size(); i++) {
			if (decorations.contains(i)) {
				clip.decorations.add(copy(chapter.decorations.get(i)));
			}
		}

		return clip;
	}

	static Chapter.Decoration copy(Chapter.Decoration d) {
		return (Chapter.Decoration)BookJson.GSON.fromJson(BookJson.GSON.toJson(d), Chapter.Decoration.class);
	}

	static String write(EditorOps.Clip clip) {
		return BookJson.GSON.toJson(clip);
	}

	@Nullable
	static EditorOps.Clip read(@Nullable String text) {
		if (text != null && text.length() >= 20 && text.contains("\"cfhgui_quests_clip\"")) {
			try {
				EditorOps.Clip clip = (EditorOps.Clip)BookJson.GSON.fromJson(text, EditorOps.Clip.class);
				if (clip != null && clip.cfhgui_quests_clip == 1) {
					if (clip.chapter == null) {
						clip.chapter = "";
					}

					if (clip.quests == null) {
						clip.quests = new ArrayList<>();
					}

					if (clip.decorations == null) {
						clip.decorations = new ArrayList<>();
					}

					clip.quests.removeIf(qx -> qx == null);
					clip.decorations.removeIf(d -> d == null);

					for (Quest q : clip.quests) {
						q.sanitize();
					}

					return clip;
				} else {
					return null;
				}
			} catch (Exception var4) {
				return null;
			}
		} else {
			return null;
		}
	}

	static EditorOps.Inserted insert(Chapter chapter, EditorOps.Clip clip, float dx, float dy) {
		Map<String, String> renamed = new HashMap<>();
		Set<String> taken = new HashSet<>();

		for (Quest q : clip.quests) {
			String id = freeQuestId(chapter, q.id, taken);
			taken.add(id);
			renamed.put(q.id, id);
		}

		boolean same = clip.chapter.isEmpty() || clip.chapter.equals(chapter.id);
		String prefix = clip.chapter + "/";
		List<String> quests = new ArrayList<>();

		for (Quest source : clip.quests) {
			Quest q = BookJson.copy(source);
			q.id = renamed.get(source.id);
			q.x = tidy(source.x + dx);
			q.y = tidy(source.y + dy);
			q.subtree = "";
			q.deps.replaceAll(dxx -> {
				String bare = dxx.startsWith(prefix) ? dxx.substring(prefix.length()) : dxx;
				if (bare.indexOf(47) < 0 && renamed.containsKey(bare)) {
					return renamed.get(bare);
				} else {
					return !same && dxx.indexOf(47) < 0 ? prefix + dxx : dxx;
				}
			});
			chapter.quests.add(q);
			quests.add(q.id);
		}

		List<Integer> decorations = new ArrayList<>();

		for (Chapter.Decoration source : clip.decorations) {
			Chapter.Decoration d = copy(source);
			d.x = tidy(source.x + dx);
			d.y = tidy(source.y + dy);
			chapter.decorations.add(d);
			decorations.add(chapter.decorations.size() - 1);
		}

		return new EditorOps.Inserted(quests, decorations);
	}

	static void align(Chapter chapter, Collection<String> ids, EditorOps.Align how) {
		float lo = Float.MAX_VALUE;
		float hi = -Float.MAX_VALUE;
		boolean across = how == EditorOps.Align.LEFT || how == EditorOps.Align.CENTRE || how == EditorOps.Align.RIGHT;
		List<Quest> quests = new ArrayList<>();

		for (Quest q : chapter.quests) {
			if (ids.contains(q.id)) {
				quests.add(q);
				float v = across ? q.x : q.y;
				lo = Math.min(lo, v);
				hi = Math.max(hi, v);
			}
		}

		if (quests.size() >= 2) {
			float to = switch (how) {
				case LEFT, TOP -> lo;
				default -> snap((lo + hi) / 2.0F);
				case RIGHT, BOTTOM -> hi;
			};

			for (Quest qx : quests) {
				if (across) {
					qx.x = tidy(to);
				} else {
					qx.y = tidy(to);
				}
			}
		}
	}

	static void distribute(Chapter chapter, Collection<String> ids, boolean across) {
		List<Quest> quests = new ArrayList<>();

		for (Quest q : chapter.quests) {
			if (ids.contains(q.id)) {
				quests.add(q);
			}
		}

		if (quests.size() >= 3) {
			quests.sort(Comparator.comparingDouble(qx -> across ? (double)qx.x : (double)qx.y));
			float lo = across ? quests.get(0).x : quests.get(0).y;
			float hi = across ? quests.get(quests.size() - 1).x : quests.get(quests.size() - 1).y;

			for (int i = 1; i < quests.size() - 1; i++) {
				float v = tidy(lo + (hi - lo) * (float)i / (float)(quests.size() - 1));
				if (across) {
					quests.get(i).x = v;
				} else {
					quests.get(i).y = v;
				}
			}
		}
	}

	static Chapter newChapter(QuestBook book, String title, String group, @Nullable Chapter parent) {
		Chapter c = new Chapter();
		c.id = freeChapterId(book, title);
		c.title = title;
		if (parent != null) {
			c.parent = parent.id;
			c.group = parent.group;
			c.accent = parent.accent;
			c.backdrop = parent.backdrop;
			c.sync = parent.sync;
			c.shape = parent.shape;
		} else {
			c.group = group;
			int last = -1;

			for (Chapter other : book.chapters.values()) {
				if (!other.isSubtree() && other.group.equals(group)) {
					last = Math.max(last, other.order);
				}
			}

			c.order = last + 1;
		}

		return c.sanitize();
	}

	static Chapter duplicate(QuestBook book, Chapter source, String title) {
		Chapter c = BookJson.copy(source);
		c.id = freeChapterId(book, source.id);
		c.title = title;
		c.order = source.order;
		String from = source.id + "/";

		for (Quest q : c.quests) {
			q.subtree = "";
			q.deps.replaceAll(d -> d.startsWith(from) ? d.substring(from.length()) : d);
		}

		return c;
	}

	static List<Chapter> siblings(QuestBook book, Chapter chapter) {
		List<Chapter> out = new ArrayList<>();
		if (chapter.isSubtree()) {
			for (Chapter c : book.subtrees(chapter.parent)) {
				if (!c.readOnly) {
					out.add(c);
				}
			}

			return out;
		} else {
			for (Chapter cx : book.topChapters()) {
				if (!cx.readOnly && cx.group.equals(chapter.group)) {
					out.add(cx);
				}
			}

			return out;
		}
	}

	private EditorOps() {
	}

	static enum Align {
		LEFT,
		CENTRE,
		RIGHT,
		TOP,
		MIDDLE,
		BOTTOM;
	}

	static final class Clip {
		int cfhgui_quests_clip = 1;
		String chapter = "";
		List<Quest> quests = new ArrayList<>();
		List<Chapter.Decoration> decorations = new ArrayList<>();

		boolean isEmpty() {
			return this.quests.isEmpty() && this.decorations.isEmpty();
		}

		float[] centre() {
			float x0 = Float.MAX_VALUE;
			float y0 = Float.MAX_VALUE;
			float x1 = -Float.MAX_VALUE;
			float y1 = -Float.MAX_VALUE;

			for (Quest q : this.quests) {
				x0 = Math.min(x0, q.x);
				y0 = Math.min(y0, q.y);
				x1 = Math.max(x1, q.x);
				y1 = Math.max(y1, q.y);
			}

			for (Chapter.Decoration d : this.decorations) {
				x0 = Math.min(x0, d.x);
				y0 = Math.min(y0, d.y);
				x1 = Math.max(x1, d.x);
				y1 = Math.max(y1, d.y);
			}

			return this.isEmpty() ? new float[]{0.0F, 0.0F} : new float[]{(x0 + x1) / 2.0F, (y0 + y1) / 2.0F};
		}
	}

	static record Inserted(List<String> quests, List<Integer> decorations) {
	}

	static enum Link {
		OK,
		SELF,
		EXISTS,
		CIRCLE,
		GONE;
	}
}
