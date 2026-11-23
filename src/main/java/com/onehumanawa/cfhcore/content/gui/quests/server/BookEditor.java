package com.onehumanawa.cfhcore.content.gui.quests.server;

import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsServerConfig;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import com.onehumanawa.cfhcore.content.gui.quests.net.QuestsNet;
import com.onehumanawa.cfhcore.content.gui.quests.server.ftb.FtbAuto;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;

final class BookEditor {
	static final String WRITE_FAILED = "write_failed";
	private static final int MAX_QUESTS = 4096;
	private static final int MAX_TASKS = 64;
	private static final int MAX_REWARDS = 64;
	private static final int MAX_CHOICES = 1024;
	private static final int MAX_LINES = 1024;
	private static final int MAX_REFS = 256;
	private static final int MAX_GROUPS = 128;
	private static final int MAX_DECORATIONS = 4096;
	private static final int MAX_TAGS = 64;
	private static final int TITLE = 256;
	private static final int LINE = 4096;
	private static final int SPEC = 2048;

	static QuestEngine.Outcome editHead(QuestEngine engine, Path dir, byte[] packed) {
		QuestBook head = (QuestBook)JsonWire.decode(BookJson.GSON, packed, QuestBook.class);
		if (head == null) {
			return QuestEngine.Outcome.fail("invalid");
		} else {
			clean(head);
			QuestBook book = engine.book();
			String title = book.title;
			String subtitle = book.subtitle;
			List<QuestBook.Group> groups = book.groups;
			boolean advancements = book.advancements;
			boolean closed = book.closed;
			List<Reward> emergency = book.emergency;
			int cooldown = book.emergencyCooldownSeconds;
			long revision = book.revision;
			book.title = head.title;
			book.subtitle = head.subtitle;
			book.groups = head.groups;
			book.advancements = head.advancements;
			book.closed = head.closed;
			book.emergency = head.emergency;
			book.emergencyCooldownSeconds = head.emergencyCooldownSeconds;
			book.revision = Math.max(book.revision + 1L, System.currentTimeMillis());

			try {
				BookJson.saveHead(dir, book);
			} catch (IOException var16) {
				CFHGuiQuests.LOGGER.error("[CFHGui Quests] the head of the book could not be written to {}: {}", dir, var16.toString());
				book.title = title;
				book.subtitle = subtitle;
				book.groups = groups;
				book.advancements = advancements;
				book.closed = closed;
				book.emergency = emergency;
				book.emergencyCooldownSeconds = cooldown;
				book.revision = revision;
				return QuestEngine.Outcome.fail("write_failed");
			}

			engine.repack("");

			for (Seat s : engine.seats()) {
				engine.sendBook(s);
			}

			return QuestEngine.Outcome.OK;
		}
	}

	static QuestEngine.Outcome editChapter(QuestEngine engine, Path dir, String id, byte[] packed) {
		if (!Ids.valid(id)) {
			return QuestEngine.Outcome.fail("invalid");
		} else {
			Chapter chapter = (Chapter)JsonWire.decode(BookJson.GSON, packed, Chapter.class);
			if (chapter == null) {
				return QuestEngine.Outcome.fail("invalid");
			} else {
				chapter.id = id;
				clean(chapter);
				levels(chapter, engine.book().chapter(id));

				try {
					BookJson.saveChapter(dir, chapter);
				} catch (IOException var6) {
					CFHGuiQuests.LOGGER.error("[CFHGui Quests] the chapter {} could not be written to {}: {}", new Object[]{id, dir, var6.toString()});
					return QuestEngine.Outcome.fail("write_failed");
				}

				QuestBook book = engine.book();
				book.chapters.put(id, chapter);
				book.revision = Math.max(book.revision + 1L, System.currentTimeMillis());
				engine.repack(id);
				engine.tellAll(new QuestsNet.ChapterData(id, engine.packed(chapter)));
				engine.book(book);
				return QuestEngine.Outcome.OK;
			}
		}
	}

	static QuestEngine.Outcome deleteChapter(QuestEngine engine, Path dir, String id) {
		QuestBook book = engine.book();
		if (!book.chapters.containsKey(id)) {
			return QuestEngine.Outcome.fail("no_such_chapter");
		} else {
			try {
				BookJson.deleteChapter(dir, id);
			} catch (IOException var5) {
				CFHGuiQuests.LOGGER.error("[CFHGui Quests] the chapter {} could not be deleted from {}: {}", new Object[]{id, dir, var5.toString()});
				return QuestEngine.Outcome.fail("write_failed");
			}

			book.chapters.remove(id);
			book.revision = Math.max(book.revision + 1L, System.currentTimeMillis());
			engine.repack(id);
			engine.tellAll(new QuestsNet.ChapterGone(id));
			engine.book(book);
			return QuestEngine.Outcome.OK;
		}
	}

	static QuestEngine.Outcome renameChapter(QuestEngine engine, Path dir, String id, String to) {
		QuestBook book = engine.book();
		if (!book.chapters.containsKey(id)) {
			return QuestEngine.Outcome.fail("no_such_chapter");
		} else if (!Ids.valid(to)) {
			return QuestEngine.Outcome.fail("invalid");
		} else if (id.equals(to)) {
			return QuestEngine.Outcome.OK;
		} else if (book.chapters.containsKey(to)) {
			return QuestEngine.Outcome.fail("id_taken");
		} else {
			Set<String> changed = renameChapter(book, id, to);
			boolean written = write(dir, book, changed, id);
			book.revision = Math.max(book.revision + 1L, System.currentTimeMillis());
			engine.tellAll(new QuestsNet.ChapterGone(id));
			tell(engine, book, changed);
			String from = id + "/";
			String into = to + "/";
			engine.renamed(key -> key.startsWith(from) ? into + key.substring(from.length()) : key);
			FtbAuto.renamedChapter(dir, id, to, base -> base.chapters.containsKey(id) && !base.chapters.containsKey(to) ? renameChapter(base, id, to) : Set.of());
			return written ? QuestEngine.Outcome.OK : QuestEngine.Outcome.fail("write_failed");
		}
	}

	static QuestEngine.Outcome renameQuest(QuestEngine engine, Path dir, String key, String to) {
		QuestBook book = engine.book();
		Chapter chapter = book.chapter(Ids.chapterOf(key));
		if (chapter == null || chapter.quest(Ids.questOf(key)) == null) {
			return QuestEngine.Outcome.fail("no_such_quest");
		} else if (!Ids.valid(to)) {
			return QuestEngine.Outcome.fail("invalid");
		} else if (Ids.questOf(key).equals(to)) {
			return QuestEngine.Outcome.OK;
		} else if (chapter.quest(to) != null) {
			return QuestEngine.Outcome.fail("id_taken");
		} else {
			Set<String> changed = renameQuest(book, key, to);
			boolean written = write(dir, book, changed, null);
			book.revision = Math.max(book.revision + 1L, System.currentTimeMillis());
			tell(engine, book, changed);
			String renamed = Ids.key(chapter.id, to);
			engine.renamed(k -> k.equals(key) ? renamed : k);
			FtbAuto.renamedQuest(dir, key, renamed, base -> {
				Chapter home = base.chapter(chapter.id);
				return home != null && home.quest(Ids.questOf(key)) != null && home.quest(to) == null ? renameQuest(base, key, to) : Set.of();
			});
			return written ? QuestEngine.Outcome.OK : QuestEngine.Outcome.fail("write_failed");
		}
	}

	private static boolean write(Path dir, QuestBook book, Set<String> chapters, String gone) {
		try {
			for (String id : chapters) {
				Chapter c = book.chapter(id);
				if (c != null) {
					BookJson.saveChapter(dir, c);
				}
			}

			if (gone != null) {
				BookJson.deleteChapter(dir, gone);
			}

			return true;
		} catch (IOException var7) {
			CFHGuiQuests.LOGGER.error("[CFHGui Quests] a rename could not be written to {}: {}", dir, var7.toString());
			return false;
		}
	}

	private static void tell(QuestEngine engine, QuestBook book, Set<String> chapters) {
		for (String id : chapters) {
			Chapter c = book.chapter(id);
			if (c != null) {
				engine.repack(id);
				engine.tellAll(new QuestsNet.ChapterData(id, engine.packed(c)));
			}
		}
	}

	static Set<String> renameChapter(QuestBook book, String id, String to) {
		Chapter renamed = book.chapters.get(id);
		Map<String, Chapter> next = new LinkedHashMap<>();

		for (Entry<String, Chapter> e : book.chapters.entrySet()) {
			next.put(e.getKey().equals(id) ? to : e.getKey(), e.getValue());
		}

		book.chapters = next;
		renamed.id = to;
		Set<String> changed = new LinkedHashSet<>();
		changed.add(to);
		String from = id + "/";
		String into = to + "/";

		for (Chapter c : book.chapters.values()) {
			boolean touched = false;
			if (id.equals(c.parent)) {
				c.parent = to;
				touched = true;
			}

			touched |= prefix(c.requires, from, into);
			touched |= links(c.description, "{quest:" + from, "{quest:" + into);

			for (Quest q : c.quests) {
				if (id.equals(q.subtree)) {
					q.subtree = to;
					touched = true;
				}

				touched |= prefix(q.deps, from, into);
				touched |= links(q.description, "{quest:" + from, "{quest:" + into);

				for (Task t : q.tasks) {
					if (t.quest.startsWith(from)) {
						t.quest = into + t.quest.substring(from.length());
						touched = true;
					}

					if (id.equals(t.chapter)) {
						t.chapter = to;
						touched = true;
					}
				}
			}

			if (touched) {
				changed.add(c.id);
			}
		}

		return changed;
	}

	static Set<String> renameQuest(QuestBook book, String key, String to) {
		String chapterId = Ids.chapterOf(key);
		String old = Ids.questOf(key);
		String renamed = Ids.key(chapterId, to);
		Chapter home = book.chapter(chapterId);
		home.quest(old).id = to;
		Set<String> changed = new LinkedHashSet<>();
		changed.add(chapterId);

		for (Chapter c : book.chapters.values()) {
			boolean same = c == home;
			boolean touched = exact(c.requires, key, renamed, same ? old : null, to);
			touched |= links(c.description, "{quest:" + key + "}", "{quest:" + renamed + "}");

			for (Quest q : c.quests) {
				touched |= exact(q.deps, key, renamed, same ? old : null, to);
				touched |= links(q.description, "{quest:" + key + "}", "{quest:" + renamed + "}");

				for (Task t : q.tasks) {
					if (t.quest.equals(key)) {
						t.quest = renamed;
						touched = true;
					} else if (same && t.quest.equals(old)) {
						t.quest = to;
						touched = true;
					}
				}
			}

			if (touched) {
				changed.add(c.id);
			}
		}

		return changed;
	}

	private static boolean prefix(List<String> refs, String from, String into) {
		boolean touched = false;

		for (int i = 0; i < refs.size(); i++) {
			String r = refs.get(i);
			if (r.startsWith(from)) {
				refs.set(i, into + r.substring(from.length()));
				touched = true;
			}
		}

		return touched;
	}

	private static boolean exact(List<String> refs, String key, String renamed, String bare, String bareRenamed) {
		boolean touched = false;

		for (int i = 0; i < refs.size(); i++) {
			String r = refs.get(i);
			if (r.equals(key)) {
				refs.set(i, renamed);
				touched = true;
			} else if (bare != null && r.equals(bare)) {
				refs.set(i, bareRenamed);
				touched = true;
			}
		}

		return touched;
	}

	private static boolean links(List<String> lines, String from, String into) {
		boolean touched = false;

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.contains(from)) {
				lines.set(i, line.replace(from, into));
				touched = true;
			}
		}

		return touched;
	}

	private static void levels(Chapter arriving, @Nullable Chapter standing) {
		int editors = Math.max(2, Math.min(4, QuestsServerConfig.get().editorPermissionLevel));
		Set<String> granted = new HashSet<>();
		if (standing != null) {
			for (Quest q : standing.quests) {
				for (Reward r : q.rewards) {
					granted(r, granted, 0);
				}
			}
		}

		for (Quest q : arriving.quests) {
			for (Reward r : q.rewards) {
				hold(r, granted, editors, 0);
			}
		}
	}

	private static void granted(Reward r, Set<String> into, int depth) {
		if (r.is("command") && r.level > 0) {
			into.add(r.level + "\n" + r.command.trim());
		}

		if (depth < 4) {
			for (Reward c : r.choices) {
				granted(c, into, depth + 1);
			}
		}
	}

	private static void hold(Reward r, Set<String> granted, int editors, int depth) {
		if (r.is("command") && r.level > editors && !granted.contains(r.level + "\n" + r.command.trim())) {
			r.level = editors;
		}

		if (depth < 4) {
			for (Reward c : r.choices) {
				hold(c, granted, editors, depth + 1);
			}
		}
	}

	static void clean(QuestBook head) {
		head.sanitize();
		head.title = cut(head.title, 256);
		head.subtitle = cut(head.subtitle, 256);
		if (head.groups.size() > 128) {
			head.groups.subList(128, head.groups.size()).clear();
		}

		Set<String> seen = new HashSet<>();
		Iterator<QuestBook.Group> it = head.groups.iterator();

		while (it.hasNext()) {
			QuestBook.Group g = it.next();
			if (!Ids.valid(g.id)) {
				g.id = Ids.slug(g.id, "");
			}

			if (!g.id.isEmpty() && seen.add(g.id)) {
				g.title = cut(g.title, 256);
				g.icon = cut(g.icon, 2048);
			} else {
				it.remove();
			}
		}

		head.emergency.removeIf(rx -> !rx.is("item") && !rx.is("xp"));
		if (head.emergency.size() > 27) {
			head.emergency.subList(27, head.emergency.size()).clear();
		}

		for (Reward r : head.emergency) {
			clean(r, 3);
		}
	}

	static Chapter clean(Chapter c) {
		c.sanitize();
		c.title = cut(c.title, 256);
		c.subtitle = cut(c.subtitle, 256);
		lines(c.description);
		c.icon = cut(c.icon, 2048);
		c.group = cut(c.group, 64);
		if (!c.accent.isEmpty() && !c.accent.matches("#[0-9a-fA-F]{6}")) {
			c.accent = "";
		}

		c.backdrop = cut(c.backdrop, 2048);
		if (!c.parent.isEmpty() && (!Ids.valid(c.parent) || c.parent.equals(c.id))) {
			c.parent = "";
		}

		refs(c.requires);
		if (c.quests.size() > 4096) {
			c.quests.subList(4096, c.quests.size()).clear();
		}

		Set<String> ids = new HashSet<>();

		for (Quest q : c.quests) {
			if (Ids.valid(q.id)) {
				ids.add(q.id);
			}
		}

		Set<String> taken = new HashSet<>();

		for (Quest qx : c.quests) {
			if (!Ids.valid(qx.id) || !taken.add(qx.id)) {
				String base = Ids.slug(qx.id, "quest");
				String id = base;

				for (int n = 2; ids.contains(id) || taken.contains(id); n++) {
					id = base + "_" + n;
				}

				qx.id = id;
				taken.add(id);
			}

			clean(qx, c.id);
		}

		if (c.decorations.size() > 4096) {
			c.decorations.subList(4096, c.decorations.size()).clear();
		}

		for (Chapter.Decoration d : c.decorations) {
			d.type = cut(d.type, 32);
			d.text = cut(d.text, 4096);
			d.image = cut(d.image, 2048);
			d.color = cut(d.color, 16);
			d.x = finite(d.x);
			d.y = finite(d.y);
			d.w = finite(d.w);
			d.h = finite(d.h);
			d.scale = Float.isFinite(d.scale) && d.scale > 0.0F ? Math.min(d.scale, 16.0F) : 1.0F;
			d.alpha = Float.isFinite(d.alpha) ? Math.max(0.0F, Math.min(1.0F, d.alpha)) : 1.0F;
			d.rotation = Float.isFinite(d.rotation) ? d.rotation % 360.0F : 0.0F;
			if (d.hover.size() > 32) {
				d.hover.subList(32, d.hover.size()).clear();
			}

			d.hover.replaceAll(l -> cut(l, 256));
			d.link = cut(d.link, 4096);
			d.requires = cut(d.requires, 160);
		}

		c.focus = cut(c.focus, 64);
		return c;
	}

	private static void clean(Quest q, String chapterId) {
		q.title = cut(q.title, 256);
		q.subtitle = cut(q.subtitle, 256);
		lines(q.description);
		q.icon = cut(q.icon, 2048);
		q.x = finite(q.x);
		q.y = finite(q.y);
		if (!Float.isFinite(q.size)) {
			q.size = 1.0F;
		}

		refs(q.deps);
		if (q.minDeps > 256) {
			q.minDeps = 256;
		}

		if (!q.subtree.isEmpty() && (!Ids.valid(q.subtree) || q.subtree.equals(chapterId))) {
			q.subtree = "";
		}

		if (q.tags.size() > 64) {
			q.tags.subList(64, q.tags.size()).clear();
		}

		q.tags.replaceAll(t -> cut(t, 64));
		if (q.tasks.size() > 64) {
			q.tasks.subList(64, q.tasks.size()).clear();
		}

		for (Task t : q.tasks) {
			t.type = cut(t.type, 32);
			t.title = cut(t.title, 256);
			t.icon = cut(t.icon, 2048);
			t.item = cut(t.item, 16384);
			t.entity = cut(t.entity, 256);
			t.block = cut(t.block, 256);
			t.advancement = cut(t.advancement, 256);
			t.stat = cut(t.stat, 256);
			t.dimension = cut(t.dimension, 256);
			t.biome = cut(t.biome, 256);
			t.structure = cut(t.structure, 256);
			t.quest = cut(t.quest, 160);
			t.chapter = cut(t.chapter, 64);
			t.value = cut(t.value, 256);
			t.name = cut(t.name, 256);
			t.criterion = cut(t.criterion, 256);
			t.fluid = cut(t.fluid, 256);
			if (t.radius < 0) {
				t.radius = 0;
			}
		}

		q.guide = cut(q.guide, 256);
		if (!Float.isFinite(q.iconScale)) {
			q.iconScale = 1.0F;
		}

		if (q.rewards.size() > 64) {
			q.rewards.subList(64, q.rewards.size()).clear();
		}

		for (Reward r : q.rewards) {
			clean(r, 0);
		}

		if (q.teamXp < -1) {
			q.teamXp = -1;
		}
	}

	private static void clean(Reward r, int depth) {
		r.type = cut(r.type, 32);
		r.title = cut(r.title, 256);
		r.icon = cut(r.icon, 2048);
		r.item = cut(r.item, 2048);
		r.command = cut(r.command, 4096);
		r.loot = cut(r.loot, 256);
		r.advancement = cut(r.advancement, 256);
		r.criterion = cut(r.criterion, 256);
		r.text = cut(r.text, 4096);
		r.value = cut(r.value, 256);
		if (depth >= 3) {
			r.choices.clear();
		}

		if (r.choices.size() > 1024) {
			r.choices.subList(1024, r.choices.size()).clear();
		}

		for (Reward c : r.choices) {
			clean(c, depth + 1);
		}
	}

	private static void lines(List<String> lines) {
		if (lines.size() > 1024) {
			lines.subList(1024, lines.size()).clear();
		}

		lines.replaceAll(l -> cut(l, 4096));
	}

	private static void refs(List<String> refs) {
		if (refs.size() > 256) {
			refs.subList(256, refs.size()).clear();
		}

		refs.replaceAll(r -> cut(r.trim(), 160));
		refs.removeIf(String::isEmpty);
	}

	private static String cut(String s, int max) {
		return s == null ? "" : (s.length() > max ? s.substring(0, max) : s);
	}

	private static float finite(float v) {
		return Float.isFinite(v) ? Math.max(-100000.0F, Math.min(100000.0F, v)) : 0.0F;
	}

	private BookEditor() {
	}
}
