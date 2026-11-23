package com.onehumanawa.cfhcore.content.gui.quests.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsConfig;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.ItemSpecs;
import com.onehumanawa.cfhcore.content.gui.quests.mixin.client.ClientAdvancementsAccessor;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestState;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class AdvancementChapters implements QuestsState.Overlay {
	public static final String GROUP = "advancements";
	private static final float ROW = 0.96428573F;
	private static final int SUBTITLE_MAX = 72;
	private static final int MAX_ID = 64;
	private static final AdvancementChapters INSTANCE = new AdvancementChapters();
	@Nullable
	private ClientAdvancements source;
	private Map<ResourceLocation, AdvancementChapters.Tab> tabs = new LinkedHashMap<>();
	private final Map<ResourceLocation, AdvancementChapters.Entry> byId = new HashMap<>();
	@Nullable
	private Language words;
	private boolean stale = true;
	private final Set<ResourceLocation> touched = new HashSet<>();
	private final Set<ResourceLocation> progressed = new HashSet<>();
	private boolean given;

	private AdvancementChapters() {
	}

	static void init() {
		QuestsState.overlay(INSTANCE);
		CFHGuiEvents.CLIENT_TICK_END.register(INSTANCE::tick);
	}

	public static boolean shown() {
		return INSTANCE.given && !INSTANCE.tabs.isEmpty();
	}

	public static boolean is(@Nullable Chapter chapter) {
		return chapter != null && chapter.readOnly && "advancements".equals(chapter.group);
	}

	@Nullable
	public static String startChapter() {
		AdvancementChapters a = INSTANCE;
		if (shown() && a.source != null) {
			try {
				AdvancementHolder selected = ((ClientAdvancementsAccessor)a.source).slateQuests$selectedTab();
				AdvancementChapters.Tab tab = selected == null ? null : a.tabs.get(selected.id());
				if (tab != null) {
					return tab.chapter.id;
				}
			} catch (RuntimeException var3) {
			}

			return a.tabs.values().iterator().next().chapter.id;
		} else {
			return null;
		}
	}

	public static void looking(@Nullable Chapter chapter) {
		AdvancementChapters a = INSTANCE;
		if (is(chapter) && a.source != null) {
			for (AdvancementChapters.Tab tab : a.tabs.values()) {
				if (tab.chapter == chapter) {
					try {
						AdvancementHolder selected = ((ClientAdvancementsAccessor)a.source).slateQuests$selectedTab();
						AdvancementNode node = a.source.getTree().get(tab.root);
						if (node != null && (selected == null || !selected.id().equals(tab.root))) {
							a.source.setSelectedTab(node.holder(), true);
						}
					} catch (RuntimeException var6) {
					}

					return;
				}
			}
		}
	}

	public static String description(String questKey) {
		AdvancementChapters a = INSTANCE;
		Chapter c = QuestsState.book() == null ? null : QuestsState.book().chapter(Ids.chapterOf(questKey));
		if (!is(c)) {
			return "";
		} else {
			for (AdvancementChapters.Tab tab : a.tabs.values()) {
				if (tab.chapter == c) {
					for (AdvancementChapters.Entry e : tab.entries) {
						if (e.state.key.equals(questKey)) {
							return e.text;
						}
					}
				}
			}

			return "";
		}
	}

	public static void updated(ClientAdvancements advancements, ClientboundUpdateAdvancementsPacket packet) {
		AdvancementChapters a = INSTANCE;
		if (advancements == a.source) {
			if (packet.shouldReset()) {
				a.stale = true;
			} else {
				for (AdvancementHolder holder : packet.getAdded()) {
					a.touched.add(holder.id());
				}

				a.touched.addAll(packet.getRemoved());
			}

			a.progressed.addAll(packet.getProgress().keySet());
		}
	}

	private static boolean wanted(QuestBook book) {
		return book.advancements && QuestsConfig.get().advancements;
	}

	private void tick() {
		try {
			this.follow();
		} catch (RuntimeException var2) {
			CFHGuiQuests.LOGGER.error("[CFHGui Quests] the game's advancements could not be made into chapters", var2);
			this.tabs = new LinkedHashMap<>();
			this.byId.clear();
			this.touched.clear();
			this.progressed.clear();
			this.stale = false;
			if (this.given) {
				QuestsState.overlayChanged();
			}
		}
	}

	private void follow() {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		ClientAdvancements now = connection == null ? null : connection.getAdvancements();
		if (now != this.source) {
			this.source = now;
			this.tabs = new LinkedHashMap<>();
			this.byId.clear();
			this.touched.clear();
			this.progressed.clear();
			this.stale = true;
		}

		Language language = Language.getInstance();
		if (language != this.words) {
			this.words = language;
			this.stale = true;
		}

		QuestBook book = QuestsState.book();
		boolean ready = this.source != null && book != null && wanted(book);
		if (!ready) {
			if (!this.touched.isEmpty() || !this.progressed.isEmpty()) {
				this.stale = true;
				this.touched.clear();
				this.progressed.clear();
			}

			if (this.given) {
				if (book == null) {
					this.given = false;
				} else {
					QuestsState.overlayChanged();
				}
			}
		} else {
			boolean chapters = false;
			List<String> keys = new ArrayList<>();
			if (this.stale) {
				this.build(null);
				chapters = true;
			} else if (!this.touched.isEmpty() || !this.progressed.isEmpty()) {
				Set<ResourceLocation> roots = new HashSet<>();

				for (ResourceLocation id : this.touched) {
					this.rootOf(id, roots);
				}

				for (ResourceLocation id : this.progressed) {
					AdvancementChapters.Entry e = this.byId.get(id);
					if (e == null) {
						AdvancementNode node = this.source.getTree().get(id);
						if (node != null && node.advancement().display().isPresent() && this.done(node.holder())) {
							this.rootOf(id, roots);
						}
					} else if (e.hides && !this.done(e.holder)) {
						roots.add(e.tab.root);
					}
				}

				if (!roots.isEmpty()) {
					chapters = this.build(roots);
				}

				for (ResourceLocation idx : this.progressed) {
					AdvancementChapters.Entry e = this.byId.get(idx);
					if (e != null) {
						this.read(e);
						keys.add(e.state.key);
					}
				}
			}

			this.touched.clear();
			this.progressed.clear();
			if (chapters || this.given == this.tabs.isEmpty()) {
				QuestsState.overlayChanged();
			} else if (!keys.isEmpty()) {
				QuestsState.overlayProgress(keys);
			}
		}
	}

	private void rootOf(ResourceLocation id, Set<ResourceLocation> roots) {
		AdvancementChapters.Entry e = this.byId.get(id);
		if (e != null) {
			roots.add(e.tab.root);
		}

		if (this.tabs.containsKey(id)) {
			roots.add(id);
		}

		AdvancementNode node = this.source == null ? null : this.source.getTree().get(id);
		if (node != null) {
			roots.add(node.root().holder().id());
		}
	}

	private boolean done(AdvancementHolder holder) {
		AdvancementProgress p = this.progress().get(holder);
		return p != null && p.isDone();
	}

	private Map<AdvancementHolder, AdvancementProgress> progress() {
		return ((ClientAdvancementsAccessor)this.source).slateQuests$progress();
	}

	private boolean build(@Nullable Set<ResourceLocation> which) {
		this.stale = false;
		Map<ResourceLocation, AdvancementChapters.Tab> next = new LinkedHashMap<>();
		boolean changed = false;
		int order = 0;

		for (AdvancementNode root : this.source.getTree().roots()) {
			DisplayInfo display = (DisplayInfo)root.advancement().display().orElse(null);
			if (display != null) {
				ResourceLocation id = root.holder().id();
				AdvancementChapters.Tab tab = which == null ? null : this.tabs.get(id);
				if (tab == null || which.contains(id)) {
					tab = this.tab(root, display);
					changed = true;
				}

				if (tab.chapter.order != order) {
					tab.chapter.order = order;
					changed = true;
				}

				order++;
				next.put(id, tab);
			}
		}

		if (next.size() != this.tabs.size()) {
			changed = true;
		}

		this.tabs = next;
		this.byId.clear();

		for (AdvancementChapters.Tab tabx : this.tabs.values()) {
			for (AdvancementChapters.Entry e : tabx.entries) {
				this.byId.put(e.holder.id(), e);
			}
		}

		return changed;
	}

	private AdvancementChapters.Tab tab(AdvancementNode root, DisplayInfo display) {
		ResourceLocation rootId = root.holder().id();
		AdvancementChapters.Tab tab = new AdvancementChapters.Tab(rootId, shorten("adv." + rootId.getNamespace() + "." + rootId.getPath().replace('/', '.')));
		Chapter c = tab.chapter;
		c.id = tab.baseId;
		c.title = oneLine(display.getTitle().getString(), rootId.getPath());
		c.subtitle = oneLine(display.getDescription().getString(), "");
		c.icon = this.icon(display.getIcon(), "minecraft:book");
		c.group = "advancements";
		c.backdrop = display.getBackground().<String>map(ResourceLocation::toString).orElse("");
		c.sync = Quest.Sync.PLAYER;
		c.readOnly = true;
		AdvancementTree tree = this.source.getTree();
		Set<String> used = new HashSet<>();
		Deque<AdvancementNode> nodes = new ArrayDeque<>();
		Deque<String> parents = new ArrayDeque<>();
		nodes.push(root);
		parents.push("");

		while (!nodes.isEmpty()) {
			AdvancementNode node = nodes.pop();
			String parent = parents.pop();
			DisplayInfo d = (DisplayInfo)node.advancement().display().orElse(null);
			if (d != null && (!d.isHidden() || this.done(node.holder()))) {
				AdvancementChapters.Entry e = this.entry(tab, node, d, parent, used);
				tab.entries.add(e);
				c.quests.add(e.quest);
				parent = e.quest.id;
			}

			List<AdvancementNode> children = new ArrayList<>();

			for (AdvancementNode child : node.children()) {
				if (tree.get(child.holder().id()) == child) {
					children.add(child);
				}
			}

			for (int i = children.size() - 1; i >= 0; i--) {
				nodes.push(children.get(i));
				parents.push(parent);
			}
		}

		for (AdvancementChapters.Entry e : tab.entries) {
			e.state.key = Ids.key(c, e.quest);
			this.read(e);
		}

		return tab;
	}

	private AdvancementChapters.Entry entry(AdvancementChapters.Tab tab, AdvancementNode node, DisplayInfo d, String parent, Set<String> used) {
		ResourceLocation id = node.holder().id();
		Advancement advancement = node.advancement();
		Quest q = new Quest();
		String questId = shorten(id.getNamespace() + "." + id.getPath().replace('/', '.'));

		for (int n = 2; !used.add(questId); n++) {
			questId = numbered(questId, n);
		}

		q.id = questId;
		q.title = oneLine(d.getTitle().getString(), id.getPath());
		String text = d.getDescription().getString().strip();
		if (text.length() <= 72 && text.indexOf(10) < 0) {
			q.subtitle = oneLine(text, "");
		} else {
			for (String line : text.split("\n")) {
				q.description.add(rich(line));
			}
		}

		q.icon = this.icon(d.getIcon(), "");
		q.x = d.getX();
		q.y = d.getY() * 0.96428573F;
		q.kind = Quest.Kind.ADVANCEMENT;

		q.frame = switch (d.getType()) {
			case GOAL -> Quest.Frame.GOAL;
			case CHALLENGE -> Quest.Frame.CHALLENGE;
			default -> Quest.Frame.TASK;
		};
		q.sync = Quest.Sync.PLAYER;
		if (!parent.isEmpty()) {
			q.deps.add(parent);
		}

		q.looseDeps = true;
		Task task = new Task();
		task.type = "advancement";
		task.advancement = id.toString();
		task.count = (long)Math.max(1, advancement.requirements().size());
		task.title = task.count > 1L ? "{cfhgui_quests.vanilla.task.requirements}" : "{cfhgui_quests.vanilla.task.advancement}";
		q.tasks.add(task);
		AdvancementChapters.Entry e = new AdvancementChapters.Entry(node.holder(), tab, q, d.isHidden(), text);
		e.state.tasks = new long[1];
		return e;
	}

	private void read(AdvancementChapters.Entry e) {
		AdvancementProgress p = this.progress().get(e.holder);
		QuestState s = e.state;
		long count = e.quest.tasks.get(0).count;
		if (p == null) {
			s.done = false;
			s.tasks[0] = 0L;
			s.doneAt = 0L;
			s.doneBy = "";
			e.begun = false;
		} else {
			s.done = p.isDone();
			if (s.done) {
				s.tasks[0] = count;
				Instant last = null;

				for (String name : p.getCompletedCriteria()) {
					CriterionProgress c = p.getCriterion(name);
					Instant at = c == null ? null : c.getObtained();
					if (at != null && (last == null || at.isAfter(last))) {
						last = at;
					}
				}

				s.doneAt = last == null ? 0L : last.toEpochMilli();
				s.doneBy = Minecraft.getInstance().getUser().getName();
			} else {
				s.tasks[0] = Math.min(count - 1L, (long)e.holder.value().requirements().count(namex -> {
					CriterionProgress cx = p.getCriterion(namex);
					return cx != null && cx.isDone();
				}));
				s.doneAt = 0L;
				s.doneBy = "";
			}

			e.begun = s.done || s.tasks[0] > 0L;
		}
	}

	@Override
	public void chapters(QuestBook book) {
		this.given = false;
		if (this.source != null && !this.stale && !this.tabs.isEmpty() && wanted(book)) {
			QuestBook.Group group = new QuestBook.Group();
			group.id = "advancements";
			group.title = "{gui.advancements}";
			group.icon = "minecraft:knowledge_book";
			book.clientGroups.add(group);

			for (AdvancementChapters.Tab tab : this.tabs.values()) {
				String id = tab.baseId;

				for (int n = 2; book.chapters.containsKey(id); n++) {
					id = numbered(tab.baseId, n);
				}

				if (!id.equals(tab.chapter.id)) {
					tab.chapter.id = id;

					for (AdvancementChapters.Entry e : tab.entries) {
						e.state.key = Ids.key(tab.chapter, e.quest);
					}
				}

				book.chapters.put(id, tab.chapter);
			}

			this.given = true;
		}
	}

	@Override
	public void states(Map<String, QuestState> states) {
		if (this.given) {
			for (AdvancementChapters.Tab tab : this.tabs.values()) {
				for (AdvancementChapters.Entry e : tab.entries) {
					if (e.begun) {
						states.put(e.state.key, e.state);
					}
				}
			}
		}
	}

	private static String shorten(String id) {
		return id.length() <= 64 ? id : id.substring(0, 55) + "-" + String.format("%08x", id.hashCode());
	}

	private static String numbered(String id, int n) {
		String tail = "-" + n;
		return (id.length() + tail.length() > 64 ? id.substring(0, 64 - tail.length()) : id) + tail;
	}

	private static String oneLine(String text, String fallback) {
		String s = text.replace('\n', ' ').strip();
		if (s.isEmpty()) {
			return fallback;
		} else {
			s = s.replace("&", "&&");
			if (s.length() > 2 && s.charAt(0) == '{' && s.charAt(s.length() - 1) == '}' && s.indexOf(32) < 0) {
				s = s + " ";
			}

			return s;
		}
	}

	private static String rich(String line) {
		StringBuilder sb = new StringBuilder(line.length() + 8);
		String s = line.strip();
		if (!s.isEmpty() && "#-•!".indexOf(s.charAt(0)) >= 0) {
			sb.append('\\');
		}

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if ("\\*_~`[{&".indexOf(c) >= 0) {
				sb.append('\\');
			}

			sb.append(c);
		}

		return sb.toString();
	}

	private String icon(ItemStack stack, String fallback) {
		if (stack.isEmpty()) {
			return fallback;
		} else {
			String plain = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
			if (stack.getComponentsPatch().isEmpty()) {
				return plain;
			} else {
				try {
					ClientPacketListener connection = Minecraft.getInstance().getConnection();
					Provider registries = connection == null ? null : connection.registryAccess();
					if (registries == null) {
						return plain;
					}

					String spec = new ItemInput(stack.getItemHolder(), stack.getComponentsPatch()).serialize(registries);
					if (spec.length() <= 1024 && !ItemSpecs.stack(spec).isEmpty()) {
						return spec;
					}
				} catch (RuntimeException var7) {
				}

				return plain;
			}
		}
	}

	private static final class Entry {
		final AdvancementHolder holder;
		final AdvancementChapters.Tab tab;
		final Quest quest;
		final QuestState state = new QuestState();
		final boolean hides;
		final String text;
		boolean begun;

		Entry(AdvancementHolder holder, AdvancementChapters.Tab tab, Quest quest, boolean hides, String text) {
			this.holder = holder;
			this.tab = tab;
			this.quest = quest;
			this.hides = hides;
			this.text = text;
		}
	}

	private static final class Tab {
		final ResourceLocation root;
		final String baseId;
		final Chapter chapter = new Chapter();
		final List<AdvancementChapters.Entry> entries = new ArrayList<>();

		Tab(ResourceLocation root, String baseId) {
			this.root = root;
			this.baseId = baseId;
		}
	}
}
