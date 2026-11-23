package com.onehumanawa.cfhcore.content.gui.quests.server.ftb;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

public final class FtbImport {
	private static final int PLACES = 60;
	private final FtbBook src;
	@Nullable
	private final FtbImport.IdMap before;
	private final Map<String, Object> data;
	private final Map<String, Object> words;
	private final FtbItems items;
	private final FtbText text;
	private final FtbImport.Counts counts = new FtbImport.Counts();
	private final FtbImport.IdMap ids = new FtbImport.IdMap();
	private final Map<String, String> questKeys = new HashMap<>();
	private final Map<String, String> chapterIds = new HashMap<>();
	private final Map<String, String> groupIds = new HashMap<>();
	private final Map<String, Map<String, Object>> questsById = new HashMap<>();
	private final Map<String, Map<String, Object>> chaptersById = new HashMap<>();
	private final Map<String, String> questTitles = new HashMap<>();
	private final Map<String, String> crateTables = new HashMap<>();
	private final Map<String, List<String>> approx = new LinkedHashMap<>();
	private final Map<String, List<String>> needs = new LinkedHashMap<>();
	private final Map<String, List<String>> turned = new LinkedHashMap<>();
	private final Map<String, List<String>> left = new LinkedHashMap<>();
	private final Set<String> standingChapters = new HashSet<>();
	private final Map<String, Set<String>> standingQuests = new HashMap<>();
	private final Map<String, String> moved = new TreeMap<>();
	private boolean untitled;
	public static final String REPORT = "ftb-report.txt";
	public static final String IDS = "ftb-ids.json";
	private static final JsonObject CHAPTER_DEFAULTS = BookJson.GSON.toJsonTree(new Chapter()).getAsJsonObject();
	private static final JsonObject QUEST_DEFAULTS = BookJson.GSON.toJsonTree(new Quest()).getAsJsonObject();
	private static final JsonObject TASK_DEFAULTS = BookJson.GSON.toJsonTree(new Task()).getAsJsonObject();
	private static final JsonObject REWARD_DEFAULTS = BookJson.GSON.toJsonTree(new Reward()).getAsJsonObject();
	private static final JsonObject DECORATION_DEFAULTS = BookJson.GSON.toJsonTree(new Chapter.Decoration()).getAsJsonObject();

	private FtbImport(FtbBook src, @Nullable FtbImport.IdMap before, Collection<String> standing) {
		this.src = src;
		this.before = before;

		for (String id : standing) {
			this.stands(id);
		}

		if (before != null) {
			for (String id : before.retired) {
				this.stands(id);
			}
		}

		this.data = src.data;
		Map<String, Object> own = src.lang.get(src.locale());
		this.words = own != null
			? own
			: (src.lang.containsKey("en_us") ? src.lang.get("en_us") : (src.lang.isEmpty() ? Map.of() : src.lang.values().iterator().next()));
		FtbItems.Notes notes = new FtbItems.Notes() {
			@Override
			public void approx(String kind, String place) {
				FtbImport.note(FtbImport.this.approx, kind, place);
			}

			@Override
			public void turn(String kind, String place) {
				FtbImport.note(FtbImport.this.turned, kind, place);
			}

			@Override
			public void needs(String kind, String place) {
				FtbImport.note(FtbImport.this.needs, kind, place);
			}
		};
		this.items = new FtbItems(src.itemVersion(), src.format != FtbBook.Format.LANG, notes);
		this.text = new FtbText(notes, this::linkTarget);
	}

	@Nullable
	public static FtbImport.Result read(Path folder) throws IOException {
		return read(folder, null);
	}

	@Nullable
	public static FtbImport.Result read(Path folder, @Nullable FtbImport.IdMap before) throws IOException {
		FtbBook book = FtbBook.read(folder);
		return book == null ? null : convert(book, before);
	}

	public static FtbImport.Result convert(FtbBook book, @Nullable FtbImport.IdMap before) {
		return convert(book, before, List.of());
	}

	public static FtbImport.Result convert(FtbBook book, @Nullable FtbImport.IdMap before, Collection<String> standing) {
		return new FtbImport(book, before == null ? null : before.sanitize(), standing).convert();
	}

	private void stands(String id) {
		if (id.indexOf(47) < 0) {
			this.standingChapters.add(id);
		} else {
			this.standingQuests.computeIfAbsent(Ids.chapterOf(id), k -> new HashSet<>()).add(Ids.questOf(id));
		}
	}

	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("usage: FtbImport <ftbquests folder> <folder to write to>");
		} else {
			Path into = Path.of(args[1]);
			FtbImport.Result r = read(Path.of(args[0]), ids(into));
			if (r == null) {
				System.out.println("no FTB Quests book in " + args[0]);
			} else {
				write(into, r);

				for (String line : r.summary()) {
					System.out.println(line);
				}
			}
		}
	}

	public static void write(Path into, FtbImport.Result result) throws IOException {
		Files.createDirectories(into.resolve("chapters"));
		BookJson.saveHead(into, result.book());

		for (Chapter c : result.book().chapters.values()) {
			put(into.resolve("chapters").resolve(c.id + ".json"), compact(c) + "\n");
		}

		for (Entry<String, Map<String, Object>> e : result.translations().entrySet()) {
			Files.createDirectories(into.resolve("lang"));
			put(into.resolve("lang").resolve(e.getKey() + ".json"), BookJson.PRETTY.toJson(e.getValue()) + "\n");
		}

		put(into.resolve("ftb-report.txt"), String.join("\n", result.report()) + "\n");
		put(into.resolve("ftb-ids.json"), BookJson.PRETTY.toJson(result.ids()) + "\n");
	}

	@Nullable
	public static FtbImport.IdMap ids(Path dir) {
		Path file = dir.resolve("ftb-ids.json");
		if (!Files.isRegularFile(file)) {
			return null;
		} else {
			try {
				FtbImport.IdMap map = (FtbImport.IdMap)BookJson.GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), FtbImport.IdMap.class);
				return map == null ? null : map.sanitize();
			} catch (Exception var3) {
				return null;
			}
		}
	}

	static void put(Path file, String text) throws IOException {
		Files.createDirectories(file.getParent());
		Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
		Files.writeString(tmp, text, StandardCharsets.UTF_8);

		try {
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException var4) {
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private FtbImport.Result convert() {
		for (String bad : this.src.unreadable) {
			note(this.approx, "A file of the book that could not be read (what was in it is missing here)", bad);
		}

		QuestBook book = new QuestBook();
		String title = this.words("file", "0000000000000001", this.data);
		this.untitled = title.isBlank();
		book.title = this.untitled ? "{cfhgui_quests.title}" : FtbText.text(title);
		book.source = "ftbquests";
		book.closed = Snbt.flag(this.data, "disable_gui", false);
		this.counts.tables = this.src.tables.size();

		for (Entry<String, Map<String, Object>> t : this.src.tables.entrySet()) {
			Map<String, Object> crate = Snbt.map(t.getValue().get("loot_crate"));
			if (!crate.isEmpty()) {
				this.crateTables.put(Snbt.string(crate, "string_id", t.getKey().toLowerCase(Locale.ROOT)), t.getKey());
			}
		}

		Set<String> taken = new HashSet<>();
		Map<String, String> groupsKept = reserve(this.src.groups, this.before == null ? null : this.before.groups, taken);

		for (Map<String, Object> group : this.src.groups) {
			String ftbId = FtbBook.id(group);
			String groupTitle = FtbText.text(this.words("chapter_group", ftbId, group));
			if (!groupTitle.isBlank()) {
				QuestBook.Group out = new QuestBook.Group();
				out.id = groupsKept.containsKey(ftbId) ? groupsKept.get(ftbId) : free(slug(groupTitle, ftbId.toLowerCase(Locale.ROOT)), taken);
				out.title = groupTitle;
				this.groupIds.put(ftbId, out.id);
				this.ids.groups.put(ftbId, out.id);
				book.groups.add(out);
				this.counts.groups++;
			}
		}

		Set<String> chapterTaken = new HashSet<>();
		Map<String, String> chaptersKept = reserve(this.src.chapters, this.before == null ? null : this.before.chapters, chapterTaken);
		chapterTaken.addAll(this.standingChapters);

		for (Map<String, Object> chapter : this.src.chapters) {
			String ftbId = FtbBook.id(chapter);
			String named = Snbt.string(chapter, "filename", ftbId).toLowerCase(Locale.ROOT);
			String wanted = Ids.valid(named) ? named : Ids.slug(named, "chapter");
			String slateId = chaptersKept.containsKey(ftbId) ? chaptersKept.get(ftbId) : free(wanted, chapterTaken);
			this.chapterIds.put(ftbId, slateId);
			this.chaptersById.put(ftbId, chapter);
			this.ids.chapters.put(ftbId, slateId);
			chapter.put("\u0000slate", slateId);
			Set<String> questTaken = new HashSet<>();
			Map<String, String> again = new HashMap<>();

			for (Object q : Snbt.list(chapter.get("quests"))) {
				this.keep(FtbBook.id(Snbt.map(q)), slateId, questTaken, again);
			}

			for (Object l : Snbt.list(chapter.get("quest_links"))) {
				this.keep(FtbBook.id(Snbt.map(l)), slateId, questTaken, again);
			}

			questTaken.addAll(this.standingQuests.getOrDefault(slateId, Set.of()));

			for (Object q : Snbt.list(chapter.get("quests"))) {
				Map<String, Object> quest = Snbt.map(q);
				String questId = FtbBook.id(quest);
				if (!questId.isEmpty()) {
					String questTitle = this.words("quest", questId, quest);
					String slug = again.containsKey(questId) ? again.get(questId) : free(slug(questTitle, questId.toLowerCase(Locale.ROOT)), questTaken);
					quest.put("\u0000slate", slug);
					String ours = Ids.key(slateId, slug);
					this.questKeys.put(questId, ours);
					this.questsById.put(questId, quest);
					this.questTitles.put(questId, questTitle);
					this.ids.quests.put(questId, ours);
					String was = this.before == null ? null : this.before.quests.get(questId);
					if (was != null && !was.equals(ours)) {
						this.moved.put(was, ours);
					}
				}
			}

			chapter.put("\u0000taken", questTaken);
			chapter.put("\u0000kept", again);
		}

		for (Map<String, Object> chapter : this.src.chapters) {
			Chapter c = this.chapter(chapter);
			book.chapters.put(c.id, c.sanitize());
			this.counts.chapters++;
		}

		this.bookSettings(book);
		book.sanitize();

		for (Chapter c : book.chapters.values()) {
			this.counts.writtenQuests = this.counts.writtenQuests + c.quests.size();

			for (Quest qx : c.quests) {
				this.counts.writtenTasks = this.counts.writtenTasks + qx.tasks.size();
				this.counts.writtenRewards = this.counts.writtenRewards + qx.rewards.size();
			}
		}

		this.counts.approximated = total(this.approx);
		this.counts.needsMod = total(this.needs);
		this.counts.rewritten = total(this.turned);
		this.counts.left = total(this.left);
		if (this.before != null) {
			this.ids.retired.addAll(this.before.retired);
			this.ids.retired.addAll(this.before.chapters.values());
			this.ids.retired.addAll(this.before.quests.values());
			this.ids.retired.removeAll(new HashSet<>(this.ids.chapters.values()));
			this.ids.retired.removeAll(new HashSet<>(this.ids.quests.values()));
		}

		return new FtbImport.Result(
			book, this.report(book), this.summary(book), this.counts, this.ids, this.translations(book), this.src.hash, this.src.format, this.moved
		);
	}

	private static Map<String, String> reserve(List<Map<String, Object>> things, @Nullable Map<String, String> was, Set<String> taken) {
		Map<String, String> kept = new HashMap<>();
		if (was == null) {
			return kept;
		} else {
			for (Map<String, Object> thing : things) {
				String ftbId = FtbBook.id(thing);
				String id = was.get(ftbId);
				if (id != null && Ids.valid(id) && taken.add(id)) {
					kept.put(ftbId, id);
				}
			}

			return kept;
		}
	}

	private void keep(String ftbId, String chapterId, Set<String> taken, Map<String, String> again) {
		String was = this.before == null ? null : this.before.quests.get(ftbId);
		if (was != null && Ids.chapterOf(was).equals(chapterId) && Ids.valid(Ids.questOf(was)) && taken.add(Ids.questOf(was))) {
			again.put(ftbId, Ids.questOf(was));
		}
	}

	private void bookSettings(QuestBook book) {
		for (Object e : Snbt.list(this.data.get("emergency_items"))) {
			FtbItems.Item item = this.items.read(e, "the book's emergency items");
			if (!item.isEmpty() && !item.filter()) {
				Reward r = new Reward();
				r.type = "item";
				r.item = item.full();
				r.count = (int)Math.min(2147483647L, item.count());
				r.sanitize();
				book.emergency.add(r);
			}
		}

		book.emergencyCooldownSeconds = (int)Math.min(2147483647L, duration(this.data.get("emergency_items_cooldown"), 300L));
		if (this.data.containsKey("icon")) {
			this.left("The book's own icon (CFHGui's book is opened by a key, a hub entry and a button that are the same for every book)", "book");
		}

		if (Snbt.flag(this.data, "drop_loot_crates", false)) {
			note(
				this.approx,
				"Loot crates that drop from mobs (not converted: a crate is an item of FTB Quests, and CFHGui Quests adds no items to the game so that it stays optional on either side)",
				"book"
			);
		}

		if (Snbt.flag(this.data, "pause_game", false)) {
			this.left("pause_game (whether a screen pauses the game is the same for every screen of CFHGui)", "book");
		}

		if (!Snbt.string(this.data, "lock_message", "").isBlank()) {
			this.left("lock_message (a locked quest says what it waits for, by name, in every layout)", "book");
		}

		if (Snbt.number(this.data, "detection_delay", 20L) != 20L) {
			this.left(
				"detection_delay (how often items are looked for is the server's setting checkIntervalTicks, not the book's)",
				"book: " + Snbt.number(this.data, "detection_delay", 20L)
			);
		}

		if (this.data.containsKey("show_lock_icons") && !Snbt.flag(this.data, "show_lock_icons", true)) {
			this.left("show_lock_icons off (a locked quest is drawn dimmed with a lock in every layout)", "book");
		}

		if (Snbt.flag(this.data, "drop_book_on_death", false)) {
			this.left("drop_book_on_death (CFHGui's book is not an item)", "book");
		}

		if (Snbt.flag(this.data, "hide_excluded_quests", false)) {
			this.left("hide_excluded_quests (a quest a fork has shut is shown locked, with the reason)", "book");
		}

		if (Snbt.flag(this.data, "default_quest_disable_jei", false)) {
			this.left("default_quest_disable_jei (the book does not open a recipe viewer by itself)", "book");
		}

		if (Snbt.flag(this.data, "suppress_all_autoclaiming", false)) {
			this.left("suppress_all_autoclaiming (a switch for testing a book in FTB Quests)", "book");
		}

		if (this.data.containsKey("grid_scale")) {
			this.left("grid_scale (the grid of FTB's editor; CFHGui's editor has its own, and quests keep the places they have)", "book");
		}

		if (Snbt.flag(this.data, "verify_on_load", false)) {
			this.left("verify_on_load (FTB Quests checking its own book as it loads; here tools/quests/ValidateBook checks a book)", "book");
		}

		if (this.data.containsKey("loot_crate_no_drop") && Snbt.flag(this.data, "drop_loot_crates", false)) {
			this.left("loot_crate_no_drop (how often a mob drops no crate; crates do not drop here)", "book");
		}
	}

	private static long duration(@Nullable Object value, long fallback) {
		if (value instanceof Number n) {
			return Math.max(0L, n.longValue());
		} else {
			if (value instanceof String s && !s.isBlank()) {
				String t = s.trim().toLowerCase(Locale.ROOT);

				try {
					long n = Long.parseLong(t.replaceAll("[^0-9]", ""));
					return t.endsWith("h") ? n * 3600L : (t.endsWith("m") ? n * 60L : (t.endsWith("d") ? n * 86400L : n));
				} catch (NumberFormatException var7) {
					return fallback;
				}
			}

			return fallback;
		}
	}

	private static boolean tri(Map<String, Object> quest, String key, Map<String, Object> chapter, String chapterKey, boolean fallback) {
		if (!(quest.get(key) instanceof Boolean) && !(quest.get(key) instanceof Number)) {
			return chapterKey.isEmpty() || !(chapter.get(chapterKey) instanceof Boolean) && !(chapter.get(chapterKey) instanceof Number)
				? fallback
				: Snbt.flag(chapter, chapterKey, fallback);
		} else {
			return Snbt.flag(quest, key, fallback);
		}
	}

	private Chapter chapter(Map<String, Object> from) {
		Chapter c = new Chapter();
		String ftbId = FtbBook.id(from);
		c.id = (String)from.get("\u0000slate");
		String title = FtbText.text(this.words("chapter", ftbId, from));
		if (title.length() > 2 && title.charAt(0) == '&') {
			int code = "0123456789abcdef".indexOf(Character.toLowerCase(title.charAt(1)));
			if (code >= 0) {
				c.accent = FtbText.COLORS[code];
				title = title.substring(2);
			}
		}

		c.title = title.isBlank() ? c.id : title;
		List<String> subtitle = this.lines("chapter", ftbId, "chapter_subtitle", from, "subtitle");
		if (!subtitle.isEmpty()) {
			c.subtitle = FtbText.text(subtitle.get(0));
			c.description = this.text.paragraphs(subtitle.subList(1, subtitle.size()), c.id);
		}

		c.icon = this.items.icon(from.get("icon"), c.id);
		if (c.icon.isEmpty()) {
			c.icon = "minecraft:book";
		}

		c.group = this.groupIds.getOrDefault(from.get("group") instanceof String s ? s.toUpperCase(Locale.ROOT) : "", "");
		c.order = (int)Snbt.number(from, "order_index", 0L);
		Quest.Shape shape = shape(Snbt.string(from, "default_quest_shape", ""));
		Quest.Shape bookShape = shape(Snbt.string(this.data, "default_quest_shape", "circle"));
		c.shape = shape != null ? shape : (bookShape != null ? bookShape : Quest.Shape.CIRCLE);
		c.unlisted = Snbt.flag(from, "always_invisible", false);
		if (Snbt.number(from, "default_min_width", 0L) > 0L) {
			this.left("default_min_width / min_width (the width of FTB's quest window; CFHGui's sheet has one width in every layout)", c.id);
		}

		if (Snbt.flag(from, "disable_toast", false)) {
			note(this.approx, "A chapter that is finished without a notice (here: the chapter's banner shows; every player can switch it off)", c.id);
		}

		for (Object q : Snbt.list(from.get("quests"))) {
			Map<String, Object> quest = Snbt.map(q);
			if (!FtbBook.id(quest).isEmpty()) {
				c.quests.add(this.quest(quest, c, from));
			}
		}

		Set<String> taken = (Set<String>)from.get("\u0000taken");
		Map<String, String> kept = (Map<String, String>)from.get("\u0000kept");

		for (Object l : Snbt.list(from.get("quest_links"))) {
			this.link(Snbt.map(l), c, taken, kept);
		}

		int order = 0;

		for (Object i : Snbt.list(from.get("images"))) {
			this.image(Snbt.map(i), c, order++);
		}

		String focus = this.questKeys.get(Snbt.string(from, "autofocus_id", "").toUpperCase(Locale.ROOT));
		if (focus != null && Ids.chapterOf(focus).equals(c.id)) {
			c.focus = Ids.questOf(focus);
		}

		return c;
	}

	private Quest quest(Map<String, Object> from, Chapter chapter, Map<String, Object> ch) {
		Quest q = new Quest();
		String ftbId = FtbBook.id(from);
		q.id = (String)from.get("\u0000slate");
		String where = chapter.id + "/" + q.id;
		this.counts.quests++;
		q.title = FtbText.text(this.words("quest", ftbId, from));
		q.subtitle = FtbText.text(first(this.lines("quest", ftbId, "quest_subtitle", from, "subtitle")));
		q.description = this.text.paragraphs(this.lines("quest", ftbId, "quest_desc", from, "description"), where);
		q.icon = this.items.icon(from.get("icon"), where);
		q.x = (float)Snbt.decimal(from, "x", 0.0);
		q.y = (float)Snbt.decimal(from, "y", 0.0);
		Map<String, Object> preset = Snbt.map(
			Snbt.map(this.data.get("presets")).get(Snbt.string(from, "preset", Snbt.string(ch, "preset", Snbt.string(this.data, "preset", ""))))
		);
		double size = Snbt.decimal(from, "size", 0.0);
		if (size <= 0.0) {
			size = Snbt.decimal(preset, "size", 0.0);
		}

		if (size <= 0.0) {
			size = Snbt.decimal(ch, "default_quest_size", 0.0);
		}

		if (size <= 0.0) {
			size = 1.0;
		}

		if (size < 0.5 || size > 4.0) {
			note(this.approx, "A quest larger or smaller than a node can be here (here: the nearest size between 0.5 and 4)", where + ": " + size);
		}

		q.size = (float)size;
		String shapeName = Snbt.string(from, "shape", "");
		q.shape = shape(!shapeName.isBlank() && !shapeName.equals("default") ? shapeName : Snbt.string(preset, "shape", ""));
		q.iconScale = (float)Snbt.decimal(from, "icon_scale", 1.0);
		q.optional = Snbt.flag(from, "optional", false);
		q.hideLines = tri(from, "hide_dependency_lines", ch, "default_hide_dependency_lines", false);
		q.hideOutLines = Snbt.flag(from, "hide_dependent_lines", false);
		q.repeatable = tri(from, "can_repeat", ch, "default_repeatable_quest", false);
		q.cooldownSeconds = (int)Math.max(0L, Math.min(2147483647L, Snbt.number(from, "repeat_cooldown", 0L)));
		q.silent = Snbt.flag(from, "disable_toast", false);
		q.hideText = tri(from, "hide_text_until_complete", ch, "hide_text_until_complete", false);
		q.hideDetails = tri(from, "hide_details_until_startable", ch, "hide_quest_details_until_startable", false);
		q.sequential = tri(from, "require_sequential_tasks", ch, "require_sequential_tasks", false);
		q.maxDependents = (int)Math.max(0L, Snbt.number(from, "max_completable_dependents", 0L));
		q.guide = Snbt.string(from, "guide_page", "").trim();
		if (!q.guide.isEmpty()) {
			note(this.needs, "A page of a guide book (kept: the quest's sheet opens it where Patchouli is installed)", where + ": " + q.guide);
		}

		if (Snbt.flag(from, "invisible", false)) {
			q.visibility = Quest.Visibility.INVISIBLE;
			q.revealTasks = (int)Math.max(0L, Snbt.number(from, "invisible_until_tasks", 0L));
		} else if (tri(from, "hide_until_deps_complete", ch, "hide_quest_until_deps_complete", false)) {
			q.visibility = Quest.Visibility.HIDDEN;
		} else if (tri(from, "hide_until_deps_visible", ch, "hide_quest_until_deps_visible", false)) {
			q.visibility = Quest.Visibility.FOLLOW;
		}

		for (Object t : Snbt.list(from.get("tags"))) {
			if (t instanceof String) {
				String s = (String)t;
				if (!s.isBlank()) {
					q.tags.add(s);
				}
			}
		}

		String requirement = Snbt.string(from, "dependency_requirement", "all_completed");
		boolean one = requirement.startsWith("one_");

		for (Object d : Snbt.list(from.get("dependencies"))) {
			String dep = String.valueOf(d).toUpperCase(Locale.ROOT);
			String key = this.questKeys.get(dep);
			if (key != null) {
				String named = Ids.chapterOf(key).equals(chapter.id) ? Ids.questOf(key) : key;
				if (!q.deps.contains(named)) {
					q.deps.add(named);
				}
			} else if (!this.chaptersById.containsKey(dep)) {
				note(this.approx, "A dependency on something that is not in the book (left out: it could never have been done)", where + ": " + d);
			} else {
				List<String> all = new ArrayList<>();

				for (Object o : Snbt.list(this.chaptersById.get(dep).get("quests"))) {
					Map<String, Object> other = Snbt.map(o);
					String otherKey = this.questKeys.get(FtbBook.id(other));
					if (otherKey != null && !Snbt.flag(other, "optional", false) && !otherKey.equals(where)) {
						all.add(otherKey);
					}
				}

				if (one) {
					note(
						this.approx,
						"A quest that opens when one of several things is done, one of them a whole chapter (here: the chapter's quests count one by one)",
						where + ": " + this.chapterIds.get(dep)
					);
				} else {
					note(
						this.turned,
						"A quest that waits for a whole chapter (here: for every quest of that chapter that is not optional)",
						where + ": " + this.chapterIds.get(dep) + " (" + all.size() + ")"
					);
				}

				for (String k : all) {
					if (!q.deps.contains(k)) {
						q.deps.add(k);
					}
				}
			}
		}

		long min = Snbt.number(from, "min_required_dependencies", 0L);
		if (one) {
			q.depMode = Quest.DepMode.ANY;
			q.minDeps = 1;
		}

		if (min > 0L && min < (long)q.deps.size()) {
			q.depMode = Quest.DepMode.ANY;
			q.minDeps = (int)min;
		}

		q.depsStarted = requirement.endsWith("_started");
		String mode = Snbt.string(from, "progression_mode", Snbt.string(ch, "progression_mode", Snbt.string(this.data, "progression_mode", "default")));
		if (mode.equals("flexible")) {
			note(
				this.approx,
				"Flexible progression: tasks that count before the quest is open (here: a task counts once its quest is open; what is looked for, as items held, counts at that moment)",
				where
			);
		}

		if (Snbt.number(from, "min_width", 0L) > 0L) {
			this.left("default_min_width / min_width (the width of FTB's quest window; CFHGui's sheet has one width in every layout)", where);
		}

		if (from.containsKey("dep_control_pts") && !Snbt.list(from.get("dep_control_pts")).isEmpty()) {
			this.left("dep_control_pts (bends in dependency lines; lines here run straight from quest to quest)", where);
		}

		if (from.containsKey("disable_recipe_mod") || from.containsKey("disable_jei")) {
			this.left("disable_recipe_mod (the book does not open a recipe viewer by itself)", where);
		}

		if (Snbt.flag(from, "ignore_reward_blocking", false)) {
			this.left("ignore_reward_blocking (CFHGui has no switch that blocks a team's rewards, so nothing is exempt from it)", where);
		}

		if (Snbt.flag(from, "hide_lock_icon", false)) {
			this.left("hide_lock_icon (a locked quest is drawn dimmed with a lock in every layout)", where);
		}

		boolean consumeDefault = tri(Map.of(), "", ch, "consume_items", Snbt.flag(this.data, "default_consume_items", false));

		for (Object tx : Snbt.list(from.get("tasks"))) {
			Map<String, Object> taskMap = Snbt.map(tx);
			this.counts.tasks++;
			q.tasks.add(this.task(taskMap, where, consumeDefault));
			String taskId = FtbBook.id(taskMap);
			if (!taskId.isEmpty()) {
				this.ids.tasks.put(taskId, where + "#" + (q.tasks.size() - 1));
			}
		}

		for (Object r : Snbt.list(from.get("rewards"))) {
			Map<String, Object> rewardMap = Snbt.map(r);
			this.counts.rewards++;
			q.rewards.add(this.reward(rewardMap, where, false));
			String rewardId = FtbBook.id(rewardMap);
			if (!rewardId.isEmpty()) {
				this.ids.rewards.put(rewardId, where + "#" + (q.rewards.size() - 1));
			}
		}

		if (q.rewards.size() > 64) {
			Reward rest = new Reward();
			rest.type = "all";
			rest.choices = new ArrayList<>(q.rewards.subList(63, q.rewards.size()));

			for (Entry<String, String> e : this.ids.rewards.entrySet()) {
				int hash = e.getValue().lastIndexOf(35);
				if (e.getValue().startsWith(where + "#") && Integer.parseInt(e.getValue().substring(hash + 1)) > 63) {
					e.setValue(where + "#63");
				}
			}

			q.rewards.subList(63, q.rewards.size()).clear();
			q.rewards.add(rest);
			note(this.turned, "A quest with more than 64 rewards (here: the 64th gives all the rest at once)", where);
		}

		return q;
	}

	private void link(Map<String, Object> from, Chapter chapter, Set<String> taken, Map<String, String> kept) {
		this.counts.links++;
		String linked = Snbt.string(from, "linked_quest", "").toUpperCase(Locale.ROOT);
		String key = this.questKeys.get(linked);
		String ftbId = FtbBook.id(from);
		Quest q = new Quest();
		q.id = kept.containsKey(ftbId) ? kept.get(ftbId) : free("link_" + (key == null ? ftbId.toLowerCase(Locale.ROOT) : Ids.questOf(key)), taken);
		String where = chapter.id + "/" + q.id;
		if (!ftbId.isEmpty()) {
			this.ids.quests.put(ftbId, where);
		}

		q.x = (float)Snbt.decimal(from, "x", 0.0);
		q.y = (float)Snbt.decimal(from, "y", 0.0);
		q.optional = true;
		q.silent = true;
		if (key == null) {
			q.title = "?";
			q.icon = "icon:question";
			chapter.quests.add(q);
			note(this.approx, "A link to a quest that is not in the book (here: an empty quest in its place)", where + ": " + linked);
		} else {
			Map<String, Object> target = this.questsById.get(linked);
			q.title = FtbText.text(this.questTitles.getOrDefault(linked, ""));
			q.icon = this.items.icon(from.containsKey("icon") ? from.get("icon") : target.get("icon"), where);
			double size = Snbt.decimal(from, "size", 0.0);
			if (size <= 0.0) {
				size = Snbt.decimal(target, "size", 0.0);
			}

			q.size = (float)(size <= 0.0 ? 1.0 : size);
			q.shape = shape(Snbt.string(from, "shape", Snbt.string(target, "shape", "")));
			Task t = new Task();
			t.type = "quest";
			t.quest = key;
			t.sanitize();
			q.tasks.add(t);
			chapter.quests.add(q);
			note(
				this.turned,
				"A link to a quest of another chapter (here: a quest of its own that is done when that one is; it is optional and gives nothing)",
				where + " -> " + key
			);
		}
	}

	private void image(Map<String, Object> from, Chapter chapter, int index) {
		this.counts.images++;
		String image = Snbt.string(from, "image", "");
		Chapter.Decoration d = new Chapter.Decoration();
		d.type = "image";
		d.image = image.isBlank() ? "" : FtbItems.texture(image);
		d.w = (float)Snbt.decimal(from, "width", 1.0);
		d.h = (float)Snbt.decimal(from, "height", 1.0);
		d.x = (float)Snbt.decimal(from, "x", 0.0);
		d.y = (float)Snbt.decimal(from, "y", 0.0);
		if (Snbt.flag(from, "corner", false)) {
			d.x = d.x + d.w / 2.0F;
			d.y = d.y + d.h / 2.0F;
		}

		label144: {
			d.rotation = (float)Snbt.decimal(from, "rotation", 0.0);
			d.order = (int)Snbt.number(from, "order", 0L);
			if (from.get("color") instanceof Number n && (n.intValue() & 16777215) != 16777215) {
				d.color = String.format("#%06X", n.intValue() & 16777215);
				break label144;
			}

			if (from.get("color") instanceof String s && !s.isBlank()) {
				d.color = s.startsWith("#") ? s : "#" + s;
			}
		}

		d.alpha = (float)Math.max(0.0, Math.min(1.0, Snbt.decimal(from, "alpha", 255.0) / 255.0));
		Object hover = from.get("hover");
		if (hover instanceof String s) {
			for (String line : s.split("\n")) {
				if (!line.isBlank()) {
					d.hover.add(words(line));
				}
			}
		} else {
			for (Object h : Snbt.list(hover)) {
				if (!String.valueOf(h).isBlank()) {
					d.hover.add(words(String.valueOf(h)));
				}
			}
		}

		d.editorOnly = Snbt.flag(from, "dev", false);
		Object dependency = from.get("dependency");
		String needsQuest = dependency instanceof Number n
			? String.format("%016X", n.longValue())
			: (dependency instanceof String s ? s.toUpperCase(Locale.ROOT) : "");
		if (!needsQuest.isBlank() && !needsQuest.matches("0+") && !needsQuest.equals("-1")) {
			String key = this.questKeys.get(needsQuest);
			if (key != null) {
				d.requires = key;
			} else {
				note(this.approx, "A picture that shows once something is done that is not a quest of the book (here: it always shows)", chapter.id + ": " + image);
			}
		}

		String click = Snbt.string(from, "click", "").trim();
		Map<String, Object> action = Snbt.map(from.get("click_action"));
		if (!action.isEmpty()) {
			String kind = Snbt.string(action, "type", "").toLowerCase(Locale.ROOT);
			String value = Snbt.string(action, "data", "").trim();
			click = kind.contains("url") || kind.isEmpty() ? value : (!kind.contains("quest") && !kind.contains("object") ? kind + ":" + value : "#" + value);
		}

		if (!click.isEmpty()) {
			if (click.startsWith("http://") || click.startsWith("https://")) {
				d.link = click;
			} else if (click.startsWith("#")) {
				String id = click.substring(1).toUpperCase(Locale.ROOT);
				if (this.questKeys.containsKey(id)) {
					d.link = this.questKeys.get(id);
				} else if (this.chapterIds.containsKey(id)) {
					d.link = "chapter:" + this.chapterIds.get(id);
				} else {
					note(this.approx, "A picture whose click leads to something that is not in the book (here: it is not clicked)", chapter.id + ": " + click);
				}
			} else {
				note(this.approx, "A picture whose click runs a command or a script (here: it is not clicked)", chapter.id + ": " + click);
			}
		}

		if (Snbt.flag(from, "position_locked", false)) {
			this.left("position_locked (the editor here moves a picture only when it is dragged on purpose)", chapter.id);
		}

		chapter.decorations.add(d);
		String onImage = Snbt.string(from, "text_on_image", "").trim();
		if (!onImage.isEmpty()) {
			Chapter.Decoration label = new Chapter.Decoration();
			label.type = "label";
			label.text = FtbText.text(onImage);
			label.x = d.x;
			label.y = d.y;
			label.order = d.order + 1;
			label.requires = d.requires;
			label.editorOnly = d.editorOnly;
			label.rotation = d.rotation;
			chapter.decorations.add(label);
			if (from.containsKey("text_h_align") || from.containsKey("text_v_align") || from.containsKey("text_inset")) {
				String hx = Snbt.string(from, "text_h_align", "center");
				String v = Snbt.string(from, "text_v_align", "center");
				if (!hx.equals("center") || !v.equals("center")) {
					note(this.approx, "Words on a picture set to one of its edges (here: over its middle)", chapter.id + ": " + onImage);
				}
			}

			note(this.turned, "Words written on a picture (here: a label over its middle)", chapter.id + ": " + onImage);
		}

		if (image.isBlank() && onImage.isEmpty()) {
			note(this.approx, "A picture that names no texture (kept, and shows nothing)", chapter.id + " #" + (index + 1));
		}
	}

	private static String words(String line) {
		String s = line.trim();
		return s.matches("[a-z0-9_]+(\\.[a-z0-9_]+)+") ? "{" + s + "}" : FtbText.text(s);
	}

	private Task task(Map<String, Object> from, String where, boolean consumeDefault) {
		String type = Snbt.string(from, "type", "");
		if (type.startsWith("ftbquests:")) {
			type = type.substring("ftbquests:".length());
		}

		Task t = new Task();
		t.title = FtbText.text(this.words("task", FtbBook.id(from), from));
		t.icon = from.containsKey("icon") ? this.items.icon(from.get("icon"), where) : "";
		t.optional = Snbt.flag(from, "optional_task", false);
		switch (type) {
			case "item":
				this.itemTask(from, t, where, consumeDefault);
				break;
			case "xp":
				t.type = "xp";
				t.count = Math.max(1L, Snbt.number(from, "value", 1L));
				t.points = Snbt.flag(from, "points", false);
				t.consume = true;
				break;
			case "advancement":
				t.type = "advancement";
				t.advancement = Snbt.string(from, "advancement", "");
				t.criterion = Snbt.string(from, "criterion", "").trim();
				break;
			case "checkmark":
				t.type = "checkmark";
				break;
			case "kill":
				t.type = "kill";
				String tag = Snbt.string(from, "entityTypeTag", Snbt.string(from, "entity_type_tag", "")).trim();
				t.entity = !tag.isBlank() ? (tag.startsWith("#") ? tag : "#" + tag) : Snbt.string(from, "entity", "");
				t.count = Math.max(1L, Snbt.number(from, "value", 1L));
				t.name = Snbt.string(from, "custom_name", "").trim();
				if (!Snbt.string(from, "nbt_filter", "").isBlank()) {
					note(this.approx, "A kill task that asks for mobs with certain data (here: any mob of the kind)", where + ": " + t.entity);
				}
				break;
			case "stat":
				t.type = "stat";
				t.stat = this.stat(Snbt.string(from, "stat", ""), where);
				t.count = Math.max(1L, Snbt.number(from, "value", 1L));
				break;
			case "dimension":
				t.type = "location";
				t.dimension = from.containsKey("dim") ? this.dimension(Snbt.number(from, "dim", 0L), where) : Snbt.string(from, "dimension", "");
				break;
			case "biome":
				t.type = "location";
				t.biome = Snbt.string(from, "biome", "");
				break;
			case "structure":
				t.type = "location";
				t.structure = Snbt.string(from, "structure", "");
				break;
			case "location":
				t.type = "location";
				if (!Snbt.flag(from, "ignore_dimension", false)) {
					t.dimension = from.containsKey("dim") ? this.dimension(Snbt.number(from, "dim", 0L), where) : Snbt.string(from, "dimension", "");
				}

				List<Object> at = Snbt.list(from.get("position"));
				List<Object> size = Snbt.list(from.get("size"));
				if (at.size() == 3 && size.size() == 3) {
					t.x = (int)num(at.get(0));
					t.y = (int)num(at.get(1));
					t.z = (int)num(at.get(2));
					t.sizeX = (int)Math.max(1.0, num(size.get(0)));
					t.sizeY = (int)Math.max(1.0, num(size.get(1)));
					t.sizeZ = (int)Math.max(1.0, num(size.get(2)));
				} else if (from.containsKey("w")) {
					t.x = (int)Snbt.number(from, "x", 0L);
					t.y = (int)Snbt.number(from, "y", 0L);
					t.z = (int)Snbt.number(from, "z", 0L);
					t.sizeX = (int)Math.max(1L, Snbt.number(from, "w", 1L));
					t.sizeY = (int)Math.max(1L, Snbt.number(from, "h", 1L));
					t.sizeZ = (int)Math.max(1L, Snbt.number(from, "d", 1L));
				}
				break;
			case "observation":
				this.observation(from, t, where);
				break;
			case "fluid":
				t.type = "fluid";
				Object fluid = from.get("fluid");
				Map<String, Object> map = Snbt.map(fluid);
				String id = fluid instanceof String s ? s.trim() : Snbt.string(map, "id", Snbt.string(map, "fluid", "")).trim();
				if (!id.isEmpty() && id.indexOf(58) < 0 && this.src.format == FtbBook.Format.LEGACY) {
					note(this.approx, "A fluid named as 1.12 named them (here: looked for under minecraft: by that name; set its id in the editor)", where + ": " + id);
					id = "minecraft:" + id.toLowerCase(Locale.ROOT);
				}

				t.fluid = id;
				t.count = Math.max(1L, Snbt.number(map, "amount", Snbt.number(from, "amount", 1000L)));
				t.consume = true;
				if (!Snbt.map(map.get("components")).isEmpty() || !Snbt.string(from, "nbt", "").isBlank()) {
					note(this.approx, "A fluid with data (here: the fluid, whatever its data)", where + ": " + id);
				}
				break;
			case "gamestage":
			case "stage":
				t.type = "stage";
				t.value = Snbt.string(from, "stage", "").trim();
				if (Snbt.flag(from, "team_stage", false)) {
					note(this.approx, "A stage of the whole team (here: a stage of the player; a team's quest is done when any member has it)", where + ": " + t.value);
				}
				break;
			case "custom":
				t.type = "custom";
				t.value = "ftb:" + FtbBook.id(from).toLowerCase(Locale.ROOT);
				t.count = Math.max(1L, Snbt.number(from, "max_progress", Snbt.number(from, "value", 1L)));
				note(
					this.needs,
					"A custom task, which a script of the pack finished (here: finished by /cfhquests task or QuestsApi.progress under the name given)",
					where + ": " + t.value
				);
				break;
			default:
				if (type.endsWith("key_value") && !Snbt.string(from, "key", "").isBlank()) {
					t.type = "counter";
					t.value = Snbt.string(from, "key", "").trim();
					t.count = Math.max(1L, Snbt.number(from, "value", 1L));
				} else if (!type.endsWith("energy") && !type.equals("fe") && !type.equals("tr_energy")) {
					t.type = "custom";
					t.value = !Snbt.string(from, "key", "").isBlank() ? Snbt.string(from, "key", "") : (type.isBlank() ? "unknown" : type);
					t.count = Math.max(1L, Snbt.number(from, "value", 1L));
					note(
						this.approx,
						"A task of the type \"" + type + "\", which no mod here knows (here: a custom task, finished by /cfhquests task or by another mod)",
						where + ": " + t.value
					);
				} else {
					t.type = "energy";
					t.count = Math.max(1L, Snbt.number(from, "value", 1L));
					if (Snbt.number(from, "max_input", 0L) > 0L) {
						this.left("max_input of an energy task (how fast a Task Screen block took energy in; here it is handed in at once)", where);
					}
				}
		}

		t.sanitize();
		return t;
	}

	private void itemTask(Map<String, Object> from, Task t, String where, boolean consumeDefault) {
		List<FtbItems.Item> read = new ArrayList<>();
		if (from.containsKey("items")) {
			for (Object o : Snbt.list(from.get("items"))) {
				read.add(this.items.read(o, where));
			}
		} else {
			read.add(this.items.read(from.get("item"), where));
		}

		read.removeIf(FtbItems.Item::isEmpty);
		String components = Snbt.string(from, "match_components", "none").toLowerCase(Locale.ROOT);
		boolean exact = this.src.format == FtbBook.Format.LEGACY
			? Snbt.number(from, "ignore_nbt", 0L) == 0L
			: !components.equals("none") || Snbt.flag(from, "match_nbt", false);
		if (components.equals("strict")) {
			note(this.approx, "An item that has to match in all its data (here: in the data the book names; other data may differ)", where);
		}

		List<String> specs = new ArrayList<>();

		for (FtbItems.Item item : read) {
			String spec = !item.filter() && !exact ? item.spec() : item.full();
			if (!specs.contains(spec)) {
				specs.add(spec);
			}

			if (t.icon.isEmpty() && !item.filter() && !item.full().equals(item.spec()) && !exact) {
				t.icon = item.full().length() > 200 ? "" : item.full();
			}
		}

		boolean crafting = Snbt.flag(from, "only_from_crafting", false);
		t.type = crafting ? "craft" : "item";
		t.item = String.join(" | ", specs);
		long count = Snbt.number(from, "count", 0L);
		if (count <= 0L) {
			for (FtbItems.Item item : read) {
				count = Math.max(count, item.count());
			}
		}

		t.count = Math.max(1L, count);
		t.consume = !crafting && tri(from, "consume_items", Map.of(), "", consumeDefault);
		if (crafting) {
			note(this.turned, "An item task that only counts what is crafted (here: a craft task)", where);
		}

		if (Snbt.flag(from, "task_screen_only", false)) {
			note(this.approx, "A task that only takes items from a Task Screen block (here: from the player, like the others; CFHGui Quests adds no blocks)", where);
		}

		if (t.item.isEmpty()) {
			note(this.approx, "An item task whose item could not be read (here: a checkmark to tick off)", where);
			t.type = "checkmark";
			if (t.title.isBlank()) {
				t.title = "?";
			}

			t.count = 1L;
			t.consume = false;
		}
	}

	private void observation(Map<String, Object> from, Task t, String where) {
		t.type = "observe";
		String kind = Snbt.string(from, "observation_type", "").toLowerCase(Locale.ROOT);
		if (kind.isEmpty() || !from.containsKey("observation_type")) {
			String[] kinds = new String[]{"block", "block_tag", "block_state", "block_entity", "block_entity_type", "entity_type", "entity_type_tag"};
			long n = Snbt.number(from, "observe_type", 0L);
			kind = n >= 0L && n < (long)kinds.length ? kinds[(int)n] : "block";
		}

		String what = Snbt.string(from, "to_observe", "").trim();
		switch (kind) {
			case "block":
				t.block = what;
				break;
			case "block_tag":
				t.block = what.startsWith("#") ? what : "#" + what;
				break;
			case "entity_type":
				t.entity = what;
				break;
			case "entity_type_tag":
				t.entity = what.startsWith("#") ? what : "#" + what;
				break;
			default:
				if (what.indexOf(91) > 0) {
					what = what.substring(0, what.indexOf(91));
				}

				if (what.indexOf(123) > 0) {
					what = what.substring(0, what.indexOf(123));
				}

				t.block = what;
				note(this.approx, "An observation of a block in one state or of a block entity (here: of the block)", where + ": " + kind + " " + what);
		}

		long ticks = Math.max(0L, Snbt.number(from, "timer", 0L));
		t.seconds = (int)Math.min(2147483647L, (ticks + 19L) / 20L);
		if (ticks % 20L != 0L) {
			note(this.approx, "A look for a time that is not whole seconds (here: the next whole second)", where + ": " + ticks + " ticks");
		}
	}

	private String stat(String ftb, String where) {
		String s = ftb.trim();
		if (s.startsWith("stat.")) {
			StringBuilder snake = new StringBuilder();

			for (char c : s.substring("stat.".length()).toCharArray()) {
				if (Character.isUpperCase(c)) {
					snake.append('_').append(Character.toLowerCase(c));
				} else {
					snake.append(c == '.' ? '_' : c);
				}
			}

			note(this.approx, "A statistic named as 1.12 named them (here: the custom statistic of that name, if this game has one)", where + ": " + s);
			return "minecraft:custom/minecraft:" + snake;
		} else {
			return s.indexOf(47) >= 0 ? s : "minecraft:custom/" + s;
		}
	}

	private String dimension(long number, String where) {
		if (number == 0L) {
			return "minecraft:overworld";
		} else if (number == -1L) {
			return "minecraft:the_nether";
		} else if (number == 1L) {
			return "minecraft:the_end";
		} else {
			note(
				this.approx,
				"A dimension known only by its 1.12 number (here: a place task that names the number; set the dimension's id in the editor)",
				where + ": " + number
			);
			return "legacy:dimension_" + (number < 0L ? "m" + -number : Long.toString(number));
		}
	}

	private Reward reward(Map<String, Object> from, String where, boolean inTable) {
		String type = Snbt.string(from, "type", from.containsKey("item") ? "item" : "");
		if (type.startsWith("ftbquests:")) {
			type = type.substring("ftbquests:".length());
		}

		Reward r = new Reward();
		r.title = FtbText.text(this.words("reward", FtbBook.id(from), from));
		if (from.containsKey("icon")) {
			r.icon = this.items.icon(from.get("icon"), where);
		}

		if (!inTable) {
			r.team = tri(from, "team_reward", Map.of(), "", Snbt.flag(this.data, "default_reward_team", false));
			String auto = Snbt.string(from, "auto", "default");
			String how = auto.equals("default") ? Snbt.string(this.data, "default_autoclaim_rewards", "disabled") : auto;
			r.auto = how.equals("enabled") || how.equals("no_toast");
			r.hidden = how.equals("invisible");
			r.excluded = Snbt.flag(from, "exclude_from_claim_all", false);
			if (Snbt.flag(from, "ignore_reward_blocking", false)) {
				this.left("ignore_reward_blocking (CFHGui has no switch that blocks a team's rewards, so nothing is exempt from it)", where);
			}

			if (from.containsKey("disable_reward_screen_blur")) {
				this.left("disable_reward_screen_blur (a setting of FTB's own reward window)", where);
			}
		}

		switch (type) {
			case "item":
				Object raw = from.get("item");
				FtbItems.Item item = this.items.read(raw, where);
				String crate = this.crate(raw);
				if (crate != null && this.src.tables.containsKey(crate)) {
					Map<String, Object> tablex = this.src.tables.get(crate);
					this.table(tablex, r, where, "random");
					r.rolls = (int)Math.max(1L, Snbt.number(tablex, "loot_size", 1L) * Math.max(1L, Snbt.number(from, "count", item.count())));
					note(this.turned, "A loot crate as a reward (here: the draws from its table that opening it gave; CFHGui Quests adds no items to the game)", where);
				} else {
					r.type = "item";
					r.item = item.filter() ? "" : item.full();
					r.count = (int)Math.max(1L, Math.min(2147483647L, Snbt.number(from, "count", item.count())));
					r.bonus = (int)Math.max(0L, Math.min(2147483647L, Snbt.number(from, "random_bonus", 0L)));
					if (Snbt.flag(from, "only_one", false)) {
						note(this.approx, "An item reward that is only given to someone who has none of the item (here: always given)", where);
					}

					if (r.item.isEmpty()) {
						note(this.approx, "An item reward that names no single item (kept, and gives nothing until an item is set in the editor)", where);
						if (r.title.isBlank()) {
							r.title = "?";
						}
					}
				}
				break;
			case "xp":
				r.type = "xp";
				r.xp = (int)Math.max(0L, Math.min(2147483647L, Snbt.number(from, "xp", 100L)));
				break;
			case "xp_levels":
				r.type = "xp";
				r.xp = (int)Math.max(0L, Math.min(2147483647L, Snbt.number(from, "xp_levels", 5L)));
				r.levels = true;
				break;
			case "command":
				r.type = "command";
				r.command = this.command(Snbt.string(from, "command", Snbt.string(from, "player_command", "")), where);
				r.level = (int)Math.max(0L, Math.min(4L, Snbt.number(from, "permission_level", Snbt.flag(from, "elevate_perms", false) ? 4L : 0L)));
				r.text = FtbText.text(Snbt.string(from, "feedback_message", ""));
				if (from.containsKey("silent") && !Snbt.flag(from, "silent", true) && r.text.isEmpty()) {
					this.left(
						"silent off on a command reward (what the command itself answers is not shown to the player; a line of the book's own can be: the reward's text)", where
					);
				}
				break;
			case "advancement":
				r.type = "advancement";
				r.advancement = Snbt.string(from, "advancement", "");
				r.criterion = Snbt.string(from, "criterion", "").trim();
				break;
			case "random":
			case "loot":
			case "choice":
			case "all_table": {
				Map<String, Object> inline = Snbt.map(from.get("table_data"));
				Object ref = from.containsKey("table_id") ? from.get("table_id") : from.get("table");
				String tableId = ref instanceof Number n ? String.format("%016X", n.longValue()) : (ref instanceof String s ? s.toUpperCase(Locale.ROOT) : "");
				Map<String, Object> table = !inline.isEmpty() ? inline : this.src.tables.get(tableId);
				if (table == null && ref instanceof Number n) {
					List<Map<String, Object>> all = new ArrayList<>(this.src.tables.values());
					if (this.src.format == FtbBook.Format.LEGACY && n.intValue() >= 0 && n.intValue() < all.size()) {
						table = all.get(n.intValue());
					}

					for (Entry<String, Map<String, Object>> e : this.src.tables.entrySet()) {
						if (table == null && e.getKey().replaceFirst("^0+", "").equals(tableId.replaceFirst("^0+", ""))) {
							table = e.getValue();
						}
					}
				}

				String kind = type.equals("choice") ? "choice" : (type.equals("all_table") ? "all" : "random");
				if (table == null) {
					r.type = kind;
					if (r.title.isBlank()) {
						r.title = "?";
					}

					note(this.approx, "A reward from a reward table that is not in the book (kept, and gives nothing)", where + ": " + tableId);
				} else {
					this.table(table, r, where, kind);
					if (type.equals("loot")) {
						r.rolls = (int)Math.max(1L, Math.min(64L, Snbt.number(table, "loot_size", 1L)));
					}
				}
				break;
			}
			case "toast":
				r.type = "toast";
				r.text = FtbText.text(Snbt.string(from, "description", ""));
				break;
			case "gamestage":
			case "stage":
				r.type = "stage";
				r.value = Snbt.string(from, "stage", "").trim();
				r.remove = Snbt.flag(from, "remove", false);
				break;
			case "currency":
				r.type = "currency";
				r.count = (int)Math.max(1L, Math.min(2147483647L, Snbt.number(from, "amount", 1L)));
				note(
					this.needs,
					"Money as a reward (kept: handed out where FTB Library and a mod that keeps money are installed, and said plainly where they are not)",
					where + ": " + r.count
				);
				break;
			case "custom":
				r.type = "custom";
				r.value = "ftb:" + FtbBook.id(from).toLowerCase(Locale.ROOT);
				note(
					this.needs,
					"A custom reward, which a script of the pack handed out (here: handed to whoever listens for it, QuestsApi.REWARD, under the name given)",
					where + ": " + r.value
				);
				break;
			default:
				if (type.endsWith("key_value") && !Snbt.string(from, "key", "").isBlank()) {
					r.type = "counter";
					r.value = Snbt.string(from, "key", "").trim();
					r.count = (int)Math.max(1L, Math.min(2147483647L, Snbt.number(from, "value", 1L)));
					if (from.containsKey("ignore") && !Snbt.flag(from, "ignore", true)) {
						note(
							this.approx,
							"A counter reward that only counts for quests open at that moment (here: the counter keeps what is added, whenever its quests open)",
							where + ": " + r.value
						);
					}
				} else {
					r.type = "custom";
					r.value = type.isBlank() ? "unknown" : type;
					note(
						this.approx,
						"A reward of the type \"" + type + "\", which no mod here knows (kept as a custom reward: handed to whoever listens for it, and gives nothing by itself)",
						where
					);
				}
		}

		r.sanitize();
		return r;
	}

	@Nullable
	private String crate(@Nullable Object item) {
		Map<String, Object> map = Snbt.map(item);
		if (!Snbt.string(map, "id", "").equals("ftbquests:lootcrate")) {
			return null;
		} else {
			String name = Snbt.string(Snbt.map(map.get("components")), "ftbquests:loot_crate", Snbt.string(Snbt.map(map.get("tag")), "type", ""));
			return this.crateTables.get(name);
		}
	}

	private void table(Map<String, Object> table, Reward r, String where, String kind) {
		List<Object> entries = Snbt.list(table.get("rewards"));
		String lootTable = Snbt.string(table, "loot_table_id", "").trim();
		if (!lootTable.isEmpty() && entries.isEmpty()) {
			r.type = "loot";
			r.loot = lootTable;
			if (r.title.isBlank()) {
				r.title = FtbText.text(this.words("reward_table", FtbBook.id(table), table));
			}

			if (r.icon.isBlank() && table.containsKey("icon")) {
				r.icon = this.items.icon(table.get("icon"), where);
			}

			note(this.turned, "A reward table that draws from one of the game's loot tables (here: a roll of that loot table)", where + ": " + lootTable);
		} else {
			double emptyWeight = Snbt.decimal(table, "empty_weight", 0.0);
			int scale = 1;

			for (Object e : entries) {
				scale = Math.max(scale, scale(Snbt.decimal(Snbt.map(e), "weight", 1.0)));
			}

			scale = Math.max(scale, scale(emptyWeight));
			r.type = kind;

			for (Object e : entries) {
				Map<String, Object> entry = Snbt.map(e);
				Reward one = this.reward(entry, where, true);
				one.weight = (int)Math.max(1L, Math.round(Snbt.decimal(entry, "weight", 1.0) * (double)scale));
				r.choices.add(one);
			}

			if (kind.equals("random")) {
				r.empty = (int)Math.max(0L, Math.round(emptyWeight * (double)scale));
			}

			String tableId = FtbBook.id(table);
			if (r.title.isBlank()) {
				r.title = FtbText.text(this.words("reward_table", tableId, table));
			}

			if (r.icon.isBlank() && table.containsKey("icon")) {
				r.icon = this.items.icon(table.get("icon"), where);
			}

			if (Snbt.flag(table, "hide_tooltip", false)) {
				note(this.approx, "A reward table that keeps what is in it to itself (here: the reward's tooltip lists it)", where);
			}

			if (r.choices.size() > 64 && kind.equals("choice")) {
				note(this.approx, "A choice of more than 64 things (all are kept; the list to pick from is long)", where + ": " + r.choices.size());
			}

			if (table.containsKey("loot_crate")) {
				this.left(
					"loot_crate of a reward table (a crate is an item of FTB Quests; CFHGui Quests adds no items. The table itself is converted wherever a reward draws from it)",
					"table " + tableId
				);
			}
		}
	}

	private static int scale(double weight) {
		if (weight == Math.rint(weight)) {
			return 1;
		} else {
			return weight * 10.0 == Math.rint(weight * 10.0) ? 10 : (weight * 100.0 == Math.rint(weight * 100.0) ? 100 : 1000);
		}
	}

	private String command(String ftb, String where) {
		String c = ftb.trim();
		if (c.startsWith("/")) {
			c = c.substring(1);
		}

		return c.replace("{p}", "{player}")
			.replace("@p", "{player}")
			.replace("{x}", "~")
			.replace("{y}", "~")
			.replace("{z}", "~")
			.replace("{long_team_id}", "{team_id}")
			.replace("{member_count}", "{members}")
			.replace("{online_member_count}", "{online}")
			.replace("{quest}", where)
			.replace("{chapter}", Ids.chapterOf(where));
	}

	@Nullable
	private static Quest.Shape shape(String ftb) {
		String var1 = ftb.trim().toLowerCase(Locale.ROOT);

		return switch (var1) {
			case "circle" -> Quest.Shape.CIRCLE;
			case "square" -> Quest.Shape.SQUARE;
			case "rsquare" -> Quest.Shape.ROUNDED;
			case "diamond" -> Quest.Shape.DIAMOND;
			case "pentagon" -> Quest.Shape.PENTAGON;
			case "hexagon" -> Quest.Shape.HEXAGON;
			case "octagon" -> Quest.Shape.OCTAGON;
			case "gear" -> Quest.Shape.GEAR;
			case "heart" -> Quest.Shape.HEART;
			case "none" -> Quest.Shape.NONE;
			default -> null;
		};
	}

	private String words(String kind, String id, Map<String, Object> own) {
		String title = Snbt.string(own, "title", "");
		if (!title.isBlank()) {
			return title;
		} else {
			return this.words.get(kind + "." + id + ".title") instanceof String s ? s : "";
		}
	}

	private List<String> lines(String kind, String id, String field, Map<String, Object> own, String ownField) {
		Object v = this.words.get(kind + "." + id + "." + field);
		if (v == null) {
			v = own.get(ownField);
		}

		return strings(v);
	}

	private static List<String> strings(@Nullable Object v) {
		List<String> out = new ArrayList<>();
		if (v instanceof String s) {
			out.add(s);
		} else {
			for (Object line : Snbt.list(v)) {
				out.add(String.valueOf(line));
			}
		}

		return out;
	}

	private static String first(List<String> lines) {
		return lines.isEmpty() ? "" : lines.get(0);
	}

	private static String slug(String title, String fallback) {
		String t = FtbText.plain(FtbText.text(title)).trim();
		if (t.length() > 2 && t.charAt(0) == '{' && t.charAt(t.length() - 1) == '}' && t.indexOf(32) < 0) {
			t = t.substring(1, t.length() - 1).replaceFirst("^(quests?|ftbquests)\\.", "").replaceFirst("\\.(title|name)$", "");
		}

		return Ids.slug(t, fallback);
	}

	@Nullable
	private String linkTarget(String ftbId) {
		String quest = this.questKeys.get(ftbId);
		if (quest != null) {
			return quest;
		} else {
			Map<String, Object> chapter = this.chaptersById.get(ftbId);
			if (chapter == null) {
				return null;
			} else {
				String focus = this.questKeys.get(Snbt.string(chapter, "autofocus_id", "").toUpperCase(Locale.ROOT));
				if (focus != null) {
					return focus;
				} else {
					for (Object q : Snbt.list(chapter.get("quests"))) {
						String key = this.questKeys.get(FtbBook.id(Snbt.map(q)));
						if (key != null) {
							return key;
						}
					}

					return null;
				}
			}
		}
	}

	private static double num(Object v) {
		return v instanceof Number n ? n.doubleValue() : 0.0;
	}

	private static String free(String wanted, Set<String> taken) {
		String id = wanted;

		for (int n = 2; !taken.add(id); n++) {
			String suffix = "_" + n;
			id = (wanted.length() + suffix.length() > 64 ? wanted.substring(0, 64 - suffix.length()) : wanted) + suffix;
		}

		return id;
	}

	private Map<String, Map<String, Object>> translations(QuestBook book) {
		Map<String, Map<String, Object>> out = new TreeMap<>();

		for (Entry<String, Map<String, Object>> locale : this.src.lang.entrySet()) {
			if (locale.getValue() != this.words) {
				Map<String, Object> made = new TreeMap<>();

				for (Entry<String, Object> e : locale.getValue().entrySet()) {
					String[] parts = e.getKey().split("\\.");
					if (parts.length == 3) {
						String id = parts[1].toUpperCase(Locale.ROOT);
						String field = parts[2];
						List<String> lines = strings(e.getValue());
						if (!lines.isEmpty()) {
							String var12 = parts[0];
							switch (var12) {
								case "file":
									if (field.equals("title")) {
										made.put("book.title", FtbText.text(lines.get(0)));
									}
									break;
								case "chapter_group":
									if (this.groupIds.containsKey(id) && field.equals("title")) {
										made.put("group." + this.groupIds.get(id) + ".title", FtbText.text(lines.get(0)));
									}
									break;
								case "chapter":
									String chapter = this.chapterIds.get(id);
									if (chapter != null) {
										if (field.equals("title")) {
											String title = FtbText.text(lines.get(0));
											if (title.length() > 2 && title.charAt(0) == '&' && "0123456789abcdef".indexOf(Character.toLowerCase(title.charAt(1))) >= 0) {
												title = title.substring(2);
											}

											made.put("chapter." + chapter + ".title", title);
										} else if (field.equals("chapter_subtitle")) {
											made.put("chapter." + chapter + ".subtitle", FtbText.text(lines.get(0)));
											if (lines.size() > 1) {
												made.put("chapter." + chapter + ".description", this.text.paragraphs(lines.subList(1, lines.size()), chapter));
											}
										}
									}
									break;
								case "quest":
									String key = this.questKeys.get(id);
									if (key != null) {
										if (field.equals("title")) {
											made.put("quest." + key + ".title", FtbText.text(lines.get(0)));
										} else if (field.equals("quest_subtitle")) {
											made.put("quest." + key + ".subtitle", FtbText.text(lines.get(0)));
										} else if (field.equals("quest_desc")) {
											made.put("quest." + key + ".description", this.text.paragraphs(lines, key));
										}
									}
									break;
								case "task":
									if (this.ids.tasks.containsKey(id) && field.equals("title")) {
										made.put("task." + this.ids.tasks.get(id).replace('#', '.') + ".title", FtbText.text(lines.get(0)));
									}
									break;
								case "reward":
									if (this.ids.rewards.containsKey(id) && field.equals("title")) {
										made.put("reward." + this.ids.rewards.get(id).replace('#', '.') + ".title", FtbText.text(lines.get(0)));
									}
							}
						}
					}
				}

				if (!made.isEmpty()) {
					out.put(locale.getKey(), made);
				}
			}
		}

		return out;
	}

	private static void note(Map<String, List<String>> into, String kind, String place) {
		into.computeIfAbsent(kind, k -> new ArrayList<>()).add(place);
	}

	private void left(String what, String place) {
		note(this.left, what, place);
	}

	private static int total(Map<String, List<String>> notes) {
		int n = 0;

		for (List<String> places : notes.values()) {
			n += places.size();
		}

		return n;
	}

	private List<String> summary(QuestBook book) {
		List<String> out = new ArrayList<>();
		out.add(
			this.counts.chapters
				+ " chapters, "
				+ this.counts.quests
				+ " quests, "
				+ this.counts.tasks
				+ " tasks, "
				+ this.counts.rewards
				+ " rewards, "
				+ this.counts.groups
				+ " groups from "
				+ this.src.dir
				+ " ("
				+ this.src.format.words
				+ ")"
		);
		out.add(
			"written: "
				+ book.chapters.size()
				+ " chapters, "
				+ this.counts.writtenQuests
				+ " quests ("
				+ this.counts.links
				+ " of them were links), "
				+ this.counts.writtenTasks
				+ " tasks, "
				+ this.counts.writtenRewards
				+ " rewards; approximated: "
				+ this.counts.approximated
				+ ", need another mod: "
				+ this.counts.needsMod
		);

		for (Entry<String, List<String>> e : this.approx.entrySet()) {
			out.add("approximated: " + e.getValue().size() + " x " + e.getKey());
		}

		for (Entry<String, List<String>> e : this.needs.entrySet()) {
			out.add("needs a mod: " + e.getValue().size() + " x " + e.getKey());
		}

		return out;
	}

	private static void kinds(List<String> out, Map<String, List<String>> kinds, boolean places) {
		for (Entry<String, List<String>> e : kinds.entrySet()) {
			out.add("");
			out.add(e.getValue().size() + " x " + e.getKey());
			if (places) {
				List<String> where = e.getValue();

				for (int i = 0; i < where.size() && i < 60; i++) {
					out.add("    " + where.get(i));
				}

				if (where.size() > 60) {
					out.add("    ... and " + (where.size() - 60) + " more");
				}
			}
		}
	}

	private List<String> report(QuestBook book) {
		List<String> out = new ArrayList<>();
		out.add("CFHGui Quests: an FTB Quests book, converted");
		out.add("");
		out.add("From:    " + this.src.dir);
		out.add("Format:  FTB Quests " + this.src.format.words);
		out.add(
			"Book:    "
				+ (
					this.untitled ? "no title of its own: it is called what the game calls a quest book, in each player's language" : "\"" + FtbText.plain(book.title) + "\""
				)
		);
		out.add("Source fingerprint: " + this.src.hash);
		out.add("");
		out.add(
			"READ      "
				+ this.counts.chapters
				+ " chapters in "
				+ this.counts.groups
				+ " groups, "
				+ this.counts.quests
				+ " quests, "
				+ this.counts.tasks
				+ " tasks, "
				+ this.counts.rewards
				+ " rewards, "
				+ this.counts.links
				+ " quest links, "
				+ this.counts.images
				+ " pictures, "
				+ this.counts.tables
				+ " reward tables"
		);
		out.add(
			"WRITTEN   "
				+ book.chapters.size()
				+ " chapters in "
				+ book.groups.size()
				+ " groups, "
				+ this.counts.writtenQuests
				+ " quests ("
				+ this.counts.quests
				+ " quests and "
				+ this.counts.links
				+ " links), "
				+ this.counts.writtenTasks
				+ " tasks ("
				+ this.counts.tasks
				+ " and one for each link that found its quest), "
				+ this.counts.writtenRewards
				+ " rewards"
		);
		int dropped = this.counts.quests + this.counts.links - this.counts.writtenQuests;
		out.add(
			"DROPPED   "
				+ Math.max(0, dropped)
				+ " quests, "
				+ Math.max(0, this.counts.tasks - this.counts.writtenTasks)
				+ " tasks, "
				+ Math.max(0, this.counts.rewards - this.counts.writtenRewards)
				+ " rewards"
		);
		out.add("");
		out.add("APPROXIMATED        " + this.counts.approximated + "   the book says something CFHGui cannot say exactly; the nearest thing was made");
		out.add("NEEDS ANOTHER MOD   " + this.counts.needsMod + "   converted as it is; works where that mod is installed and says so where it is not");
		out.add("WRITTEN ANOTHER WAY " + this.counts.rewritten + "   the same thing in CFHGui's words; nothing lost");
		out.add("LEFT BEHIND         " + this.counts.left + "   properties that have no meaning in CFHGui's book, with why");
		if (this.text.joined > 0) {
			out.add("");
			out.add(this.text.joined + " lines of descriptions were joined to the line before them: FTB keeps a description line by line,");
			out.add("and text that was wrapped by hand is one paragraph here. A line of 60 letters or more is taken to run on.");
		}

		out.add("");
		out.add("Chapters keep their file names as ids; quests have ids made from their titles. ftb-ids.json beside this file");
		out.add("says which FTB id became which quest, task and reward: the next conversion of this book keeps those ids, so");
		out.add("progress and what was edited in CFHGui's editor stay with their quests. Progress made in FTB Quests is carried");
		out.add("over by the same list (once per world).");
		out.add("");
		if (this.approx.isEmpty()) {
			out.add("APPROXIMATED: nothing. Everything in the book had an equal here.");
		} else {
			out.add("APPROXIMATED (" + this.counts.approximated + "), by kind, with the quests concerned:");
			kinds(out, this.approx, true);
		}

		if (!this.needs.isEmpty()) {
			out.add("");
			out.add("NEEDS ANOTHER MOD (" + this.counts.needsMod + "):");
			kinds(out, this.needs, true);
		}

		if (!this.turned.isEmpty()) {
			out.add("");
			out.add("WRITTEN ANOTHER WAY (" + this.counts.rewritten + "):");
			kinds(out, this.turned, true);
		}

		if (!this.left.isEmpty()) {
			out.add("");
			out.add("LEFT BEHIND (" + this.counts.left + "): properties of FTB Quests that have no meaning in CFHGui's book. Nothing a player does differs.");
			kinds(out, this.left, false);
		}

		return out;
	}

	public static String compact(Chapter chapter) {
		return BookJson.PRETTY.toJson(compactTree(chapter));
	}

	public static JsonObject compactTree(Chapter chapter) {
		JsonObject c = BookJson.GSON.toJsonTree(chapter).getAsJsonObject();
		c.remove("id");
		each(c, "quests", q -> {
			each(q, "tasks", t -> prune(t, TASK_DEFAULTS));
			each(q, "rewards", FtbImport::pruneReward);
			JsonElement x = q.get("x");
			JsonElement y = q.get("y");
			prune(q, QUEST_DEFAULTS);
			if (x != null) {
				q.add("x", x);
			}

			if (y != null) {
				q.add("y", y);
			}
		});
		each(c, "decorations", d -> prune(d, DECORATION_DEFAULTS));
		prune(c, CHAPTER_DEFAULTS);
		return c;
	}

	private static void pruneReward(JsonObject r) {
		each(r, "choices", FtbImport::pruneReward);
		prune(r, REWARD_DEFAULTS);
	}

	private static void each(JsonObject parent, String field, Consumer<JsonObject> what) {
		JsonElement list = parent.get(field);
		if (list instanceof JsonArray) {
			for (JsonElement e : (JsonArray)list) {
				if (e.isJsonObject()) {
					what.accept(e.getAsJsonObject());
				}
			}
		}
	}

	private static void prune(JsonObject object, JsonObject defaults) {
		object.entrySet().removeIf(e -> ((JsonElement)e.getValue()).equals(defaults.get((String)e.getKey())));
	}

	public static final class Counts {
		public int chapters;
		public int groups;
		public int quests;
		public int tasks;
		public int rewards;
		public int tables;
		public int links;
		public int images;
		public int writtenQuests;
		public int writtenTasks;
		public int writtenRewards;
		public int approximated;
		public int needsMod;
		public int rewritten;
		public int left;
	}

	public static final class IdMap {
		public Map<String, String> chapters = new TreeMap<>();
		public Map<String, String> quests = new TreeMap<>();
		public Map<String, String> tasks = new TreeMap<>();
		public Map<String, String> rewards = new TreeMap<>();
		public Map<String, String> groups = new TreeMap<>();
		public Set<String> retired = new TreeSet<>();

		public FtbImport.IdMap sanitize() {
			if (this.chapters == null) {
				this.chapters = new TreeMap<>();
			}

			if (this.quests == null) {
				this.quests = new TreeMap<>();
			}

			if (this.tasks == null) {
				this.tasks = new TreeMap<>();
			}

			if (this.rewards == null) {
				this.rewards = new TreeMap<>();
			}

			if (this.groups == null) {
				this.groups = new TreeMap<>();
			}

			Set<String> sorted = new TreeSet<>();
			if (this.retired != null) {
				for (String id : this.retired) {
					if (id != null) {
						sorted.add(id);
					}
				}
			}

			this.retired = sorted;
			return this;
		}
	}

	public static record Result(
		QuestBook book,
		List<String> report,
		List<String> summary,
		FtbImport.Counts counts,
		FtbImport.IdMap ids,
		Map<String, Map<String, Object>> translations,
		String hash,
		FtbBook.Format format,
		Map<String, String> moved
	) {
		public int unconverted() {
			return this.counts.approximated;
		}
	}
}
