package com.onehumanawa.cfhcore.content.gui.quests.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsConfig;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestActions;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsClient;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.editor.QuestEditor;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.EmergencyItems;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.QuestsUi;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.Trees;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.BookStats;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CanvasDraw;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestCanvas;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.TreeFilter;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.TreeView;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class VanillaBookScreen extends Screen implements QuestPage.Host {
	private static final String GROUP_TAB = "group:";
	private static final Map<String, String> LAST_OF_GROUP = new HashMap<>();
	@Nullable
	private final Screen parent;
	private final VanillaQuestPainter painter = new VanillaQuestPainter();
	private final QuestCanvas canvas = new QuestCanvas(0, 0, 10, 10, this.painter);
	private final TabStrip groups = new TabStrip(WindowArt.Side.LEFT, this::pickGroup, null);
	private final TabStrip chapters = new TabStrip(WindowArt.Side.ABOVE, this::pickChapter, this::chapterAgain);
	private final TabStrip trees = new TabStrip(WindowArt.Side.RIGHT, this::pickTree, id -> this.about());
	private final QuestPage page = new QuestPage(this);
	private final BookSearch search = new BookSearch(this::goTo);
	private List<VanillaBookScreen.Group> groupList = List.of();
	@Nullable
	private String topId;
	@Nullable
	private String shownId;
	@Nullable
	private String pendingQuest;
	private boolean shownOnce;
	private boolean hadBook;
	private Rect window = new Rect(0, 0, 0, 0);
	private Rect inside = new Rect(0, 0, 0, 0);
	private boolean roomy;
	@Nullable
	private CycleButton<TreeFilter> filterButton;
	@Nullable
	private Button claimAll;
	@Nullable
	private Button editButton;
	private final List<AbstractWidget> footer = new ArrayList<>();
	private final Anim bookFill = new Anim(0.0F, 700, Ease.OUT_CUBIC);
	private int titleLeft;
	private int titleRight;
	private final Anim titleHover = new Anim(0.0F, 120, Ease.OUT_CUBIC);
	private boolean titleHot;
	private long titleHotSinceMs;
	private boolean swallowChar;
	@Nullable
	private Consumer<VanillaBookScreen> onReady;
	private final Runnable bookListener = this::onBookChanged;
	private final Consumer<List<String>> progressListener = this::onProgressChanged;
	private boolean listening;
	private int awayRevision = -1;

	public VanillaBookScreen(@Nullable Screen parent) {
		super(Component.translatable("cfhgui_quests.title"));
		this.parent = parent;
		BookStats.revision();
		this.canvas.onOpen(key -> {
			if (key == null) {
				this.closeQuest();
			} else {
				this.openQuest(key);
			}
		});
		this.canvas.onEnter(this::enterTree);
		this.canvas.onContext(this::context);
		QuestsClient.Target target = QuestsClient.takeTarget();
		QuestBook book = QuestsState.book();
		String chapter = target != null && target.chapter() != null ? target.chapter() : QuestsConfig.get().lastChapter;
		if (book != null && book.chapter(chapter) == null) {
			chapter = firstChapter(book);
		}

		this.choose(chapter);
		if (target != null && target.quest() != null) {
			this.pendingQuest = target.quest();
		}
	}

	@Nullable
	private static String firstChapter(QuestBook book) {
		List<Chapter> top = book.topChapters();

		for (Chapter c : top) {
			if (BookStats.open(c) && !c.readOnly) {
				return c.id;
			}
		}

		for (Chapter cx : top) {
			if (BookStats.open(cx)) {
				return cx.id;
			}
		}

		return top.isEmpty() ? null : top.get(0).id;
	}

	@Nullable
	Screen parentScreen() {
		return this.parent;
	}

	public Component getTitle() {
		QuestBook book = QuestsState.book();
		return book == null ? super.getTitle() : QuestText.title(book);
	}

	public boolean isPauseScreen() {
		return false;
	}

	public void onClose() {
		Popups.closeAll();
		this.minecraft.setScreen(this.parent);
	}

	@Nullable
	Chapter shown() {
		QuestBook book = QuestsState.book();
		return book == null ? null : book.chapter(this.shownId);
	}

	QuestCanvas canvas() {
		return this.canvas;
	}

	QuestPage page() {
		return this.page;
	}

	BookSearch search() {
		return this.search;
	}

	private void choose(@Nullable String chapterId) {
		QuestBook book = QuestsState.book();
		Chapter c = book == null ? null : book.chapter(chapterId);
		if (c == null) {
			this.shownId = null;
			this.topId = null;
		} else {
			this.shownId = c.id;
			this.topId = Trees.top(book, c).id;
		}
	}

	private static List<VanillaBookScreen.Group> groupsOf(QuestBook book) {
		Map<String, VanillaBookScreen.Group> byId = new LinkedHashMap<>();

		for (Chapter c : book.topChapters()) {
			if (!c.hidden || BookStats.open(c)) {
				String id = c.group == null ? "" : c.group;
				VanillaBookScreen.Group group = byId.get(id);
				if (group == null) {
					QuestBook.Group named = book.group(id);
					Component title = (Component)(id.isEmpty() ? QuestText.title(book) : (named == null ? Component.literal(id) : QuestText.text(named.title)));
					String icon = id.isEmpty() ? "minecraft:writable_book" : (named != null && !named.icon.isBlank() ? named.icon : c.icon);
					group = new VanillaBookScreen.Group(id, title, icon, new ArrayList<>());
					byId.put(id, group);
				}

				group.chapters().add(c);
			}
		}

		return new ArrayList<>(byId.values());
	}

	@Nullable
	private VanillaBookScreen.Group groupOf(@Nullable String chapterId) {
		for (VanillaBookScreen.Group g : this.groupList) {
			for (Chapter c : g.chapters()) {
				if (c.id.equals(chapterId)) {
					return g;
				}
			}
		}

		return null;
	}

	protected void init() {
		if (!this.listening) {
			this.listening = true;
			QuestsState.BOOK_CHANGED.register(this.bookListener);
			QuestsState.PROGRESS_CHANGED.register(this.progressListener);
		}

		QuestBook book = QuestsState.book();
		this.hadBook = book != null;
		this.arrange();
		this.footer.clear();
		this.filterButton = null;
		this.claimAll = null;
		this.editButton = null;
		if (book != null) {
			this.addWidget(this.search.field());
			this.addWidget(this.page);
			this.addWidget(this.groups);
			this.addWidget(this.chapters);
			this.addWidget(this.trees);
			this.filterButton = CycleButton.<TreeFilter>builder(f -> Component.translatable("cfhgui_quests.filter." + f.key()))
				.withValues(java.util.Arrays.asList(TreeFilter.values()))
				.withInitialValue(this.canvas.filter())
				.withTooltip(f -> Tooltip.create(Component.translatable("cfhgui_quests.filter.tip")))
				.create(0, 0, 100, 20, Component.translatable("cfhgui_quests.vanilla.filter"), (button, value) -> this.canvas.filter(value));
			this.footer.add((AbstractWidget)this.addRenderableWidget(this.filterButton));
			this.claimAll = Button.builder(Component.translatable("cfhgui_quests.claim_all"), b -> this.claimEverything()).size(80, 20).build();
			this.footer.add((AbstractWidget)this.addRenderableWidget(this.claimAll));
			if (EmergencyItems.has()) {
				Button emergency = Button.builder(Component.literal("♥"), b -> EmergencyItems.fetch()).size(20, 20).build();
				MutableComponent tip = Component.empty();
				List<Component> lines = EmergencyItems.tip();

				for (int i = 0; i < lines.size(); i++) {
					tip.append(i == 0 ? "" : "\n").append(lines.get(i));
				}

				emergency.setTooltip(Tooltip.create(tip));
				this.footer.add((AbstractWidget)this.addRenderableWidget(emergency));
			}

			if (QuestsUi.hasSettings()) {
				Button settings = Button.builder(Component.translatable("cfhgui_quests.vanilla.settings"), b -> QuestsUi.openSettings(this)).size(70, 20).build();
				settings.setTooltip(Tooltip.create(Component.translatable("cfhgui_quests.settings.open_from_book")));
				this.footer.add((AbstractWidget)this.addRenderableWidget(settings));
			}

			if (QuestEditor.available()) {
				this.editButton = Button.builder(Component.translatable("cfhgui_quests.vanilla.edit"), b -> QuestEditor.open(this, this.shownId)).size(60, 20).build();
				this.footer.add((AbstractWidget)this.addRenderableWidget(this.editButton));
			}

			Button keys = Button.builder(Component.literal("?"), b -> {
			}).size(20, 20).build();
			keys.setTooltip(Tooltip.create(keysHelp()));
			this.footer.add((AbstractWidget)this.addRenderableWidget(keys));
		}

		this.footer
			.add((AbstractWidget)this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose()).size(book == null ? 200 : 80, 20).build()));
		if (book != null) {
			this.addWidget(this.canvas);
		}

		this.placeFooter();
		if (book != null) {
			if (book.chapter(this.shownId) == null) {
				this.choose(firstChapter(book));
				this.shownOnce = false;
			}

			Chapter asked = book.chapter(this.shownId);
			List<String> ways = asked == null ? List.of() : Trees.waysIn(book, asked);
			if (!ways.isEmpty()) {
				keepOpen(ways);
				this.choose(Trees.host(book, asked).id);
			}

			this.fillTabs();
			if (!this.shownOnce) {
				this.shownOnce = true;
				Chapter c = book.chapter(this.shownId);
				if (c != null) {
					QuestsConfig.get().lastChapter = c.id;
					keepOpen(waysTo(book, this.pendingQuest));
					if (this.pendingQuest != null) {
						this.canvas.cover(0, 0, this.page.openWidth(), 0);
					} else {
						this.recover();
					}

					this.canvas.show(c, TreeView.Travel.ENTRANCE, null);
					if (this.pendingQuest == null && !ways.isEmpty()) {
						this.canvas.focus(ways.get(ways.size() - 1));
					}

					AdvancementChapters.looking(c);
				}

				if (this.pendingQuest != null) {
					String key = this.pendingQuest;
					this.pendingQuest = null;
					if (this.canvas.focus(key)) {
						this.openQuest(key);
					} else {
						this.recover();
					}
				}
			} else {
				if (this.awayRevision >= 0 && this.awayRevision != BookStats.revision()) {
					Chapter cx = book.chapter(this.shownId);
					if (cx != null) {
						this.canvas.rebuild(cx);
					}

					this.page.refresh();
				}

				this.recover();
			}

			this.awayRevision = -1;
			this.chrome();
			this.setFocused(this.canvas);
			if (this.onReady != null) {
				Consumer<VanillaBookScreen> ready = this.onReady;
				this.onReady = null;
				ready.accept(this);
			}
		}
	}

	VanillaBookScreen onReady(Consumer<VanillaBookScreen> then) {
		this.onReady = then;
		return this;
	}

	private void arrange() {
		this.roomy = this.height >= 300;
		int head = this.roomy ? 31 : 20;
		int foot = this.roomy ? 30 : 26;
		int side = this.width >= 400 ? 34 : 30;
		int top = head + 28;
		this.window = new Rect(side, top, Math.max(120, this.width - side * 2), Math.max(70, this.height - foot - top));
		this.inside = new Rect(this.window.x() + 9, this.window.y() + 18, this.window.w() - 9 - 9, this.window.h() - 18 - 9);
		this.canvas.place(this.inside.x(), this.inside.y(), this.inside.w(), this.inside.h());
		this.page.place(this.inside);
		this.groups.place(this.window);
		this.chapters.place(this.window);
		this.trees.place(this.window);
		this.search.place(this.window.right() - 8, this.window.y() + 3, Mth.clamp(this.window.w() / 4, 74, 110));
	}

	private void placeFooter() {
		int gap = 4;
		int room = this.width - 12;
		int total = -4;

		for (AbstractWidget w : this.footer) {
			if (w != this.footer.get(this.footer.size() - 1) || this.footer.size() == 1) {
				w.setWidth(w instanceof CycleButton ? this.filterWidth() : Math.max(w.getWidth(), this.font.width(w.getMessage()) + 14));
			}

			total += w.getWidth() + 4;
		}

		if (total > room) {
			float squeeze = (float)room / (float)total;
			total = -4;

			for (AbstractWidget w : this.footer) {
				w.setWidth(Math.max(Math.min(w.getWidth(), 20), Math.round((float)w.getWidth() * squeeze) - 1));
				total += w.getWidth() + 4;
			}
		}

		int x = (this.width - total) / 2;
		int y = this.window.bottom() + (this.height - this.window.bottom() - 20 + 1) / 2;

		for (AbstractWidget w : this.footer) {
			w.setX(x);
			w.setY(y);
			x += w.getWidth() + 4;
		}
	}

	private int filterWidth() {
		int widest = 0;

		for (TreeFilter f : TreeFilter.values()) {
			widest = Math.max(
				widest,
				this.font
					.width(CommonComponents.optionNameValue(Component.translatable("cfhgui_quests.vanilla.filter"), Component.translatable("cfhgui_quests.filter." + f.key())))
			);
		}

		return widest + 14;
	}

	private static Component keysHelp() {
		MutableComponent out = Component.translatable("cfhgui_quests.keys.title");

		for (String k : new String[]{"drag", "wheel", "space", "tab", "enter", "twice", "back", "page"}) {
			out.append("\n").append(Component.translatable("cfhgui_quests.keys." + k));
		}

		out.append("\n").append(Component.translatable("cfhgui_quests.vanilla.keys.groups"));
		out.append("\n").append(Component.translatable("cfhgui_quests.keys.slash"));
		out.append("\n").append(Component.translatable("cfhgui_quests.vanilla.keys.esc"));
		return out;
	}

	private void fillTabs() {
		QuestBook book = QuestsState.book();
		if (book == null) {
			this.groupList = List.of();
			this.groups.tabs(List.of());
			this.chapters.tabs(List.of());
			this.trees.tabs(List.of());
		} else {
			this.groupList = groupsOf(book);
			VanillaBookScreen.Group mine = this.groupOf(this.topId);
			if (mine == null && !this.groupList.isEmpty()) {
				mine = this.groupList.get(0);
			}

			List<TabStrip.Tab> groupTabs = new ArrayList<>();
			if (this.groupList.size() > 1) {
				for (VanillaBookScreen.Group g : this.groupList) {
					TabStrip.Tab tab = new TabStrip.Tab("group:" + g.id(), g.title(), QuestIcons.of(g.icon()));
					int done = 0;
					int total = 0;
					int waiting = 0;
					boolean any = false;

					for (Chapter c : g.chapters()) {
						BookStats.Stat deep = BookStats.deep(c);
						done += deep.done();
						total += deep.total();
						waiting += deep.claimable();
						any |= BookStats.open(c);
					}

					tab.locked = !any;
					tab.mark = waiting > 0 ? 1 : (total > 0 && done >= total ? 2 : 0);
					int n = total;
					int w = waiting;
					int count = g.chapters().size();
					final int done0 = done;
					final int n0 = n;
					final int w0 = w;
					final int count0 = count;
					tab.tip = () -> {
						List<Component> out = new ArrayList<>();
						out.add(g.title());
						out.add(
							Component.translatable(count0 == 1 ? "cfhgui_quests.vanilla.group.chapter" : "cfhgui_quests.vanilla.group.chapters", new Object[]{count0})
								.withStyle(s -> s.withColor(10526880))
						);
						if (n0 > 0) {
							out.add(Component.translatable("cfhgui_quests.rail.progress", new Object[]{done0, n0}));
						}

						if (w0 > 0) {
							out.add(
								Component.translatable(w0 == 1 ? "cfhgui_quests.rail.waiting.one" : "cfhgui_quests.rail.waiting", new Object[]{w0}).withStyle(s -> s.withColor(16755200))
							);
						}

						return out;
					};
					groupTabs.add(tab);
				}
			}

			this.groups.tabs(groupTabs);
			this.groups.select(mine == null ? null : "group:" + mine.id());
			List<TabStrip.Tab> chapterTabs = new ArrayList<>();
			if (mine != null) {
				for (Chapter c : mine.chapters()) {
					TabStrip.Tab tab = new TabStrip.Tab(c.id, QuestText.title(c), QuestIcons.of(c.icon));
					boolean open = BookStats.open(c);
					BookStats.Stat own = BookStats.of(c);
					BookStats.Stat deep = BookStats.deep(c);
					tab.locked = !open;
					tab.mark = deep.claimable() > 0 ? 1 : (own.complete() ? 2 : 0);
					boolean shownNow = c.id.equals(this.shownId);
					tab.tip = () -> chapterTip(c, open, own, deep.claimable(), shownNow);
					chapterTabs.add(tab);
				}
			}

			this.chapters.tabs(chapterTabs);
			this.chapters.select(this.topId);
			if (mine != null && this.topId != null) {
				LAST_OF_GROUP.put(mine.id(), this.topId);
			}

			List<TabStrip.Tab> treeTabs = new ArrayList<>();
			Chapter top = book.chapter(this.topId);
			List<Trees.Branch> branches = top == null ? List.of() : Trees.branches(book, top, false);
			if (branches.size() > 1) {
				for (Trees.Branch b : branches) {
					Chapter c = b.chapter();
					TabStrip.Tab tab = new TabStrip.Tab(c.id, QuestText.title(c), QuestIcons.of(c.icon));
					boolean open = BookStats.open(c);
					BookStats.Stat own = BookStats.of(c);
					tab.locked = !open;
					tab.mark = own.claimable() > 0 ? 1 : (own.complete() ? 2 : 0);
					boolean shownNow = c.id.equals(this.shownId);
					tab.tip = () -> treeTip(b, open, own, shownNow);
					treeTabs.add(tab);
				}
			}

			this.trees.tabs(treeTabs);
			this.trees.select(this.shownId);
		}
	}

	private static List<Component> chapterTip(Chapter c, boolean open, BookStats.Stat stat, int waiting, boolean shownNow) {
		List<Component> out = new ArrayList<>();
		out.add(QuestText.title(c));
		if (!c.subtitle.isBlank()) {
			out.add(QuestText.text(c.subtitle).withStyle(s -> s.withColor(10526880)));
		}

		if (open) {
			if (stat.total() > 0) {
				out.add(Component.translatable("cfhgui_quests.rail.progress", new Object[]{stat.done(), stat.total()}));
			}

			if (waiting > 0) {
				out.add(
					Component.translatable(waiting == 1 ? "cfhgui_quests.rail.waiting.one" : "cfhgui_quests.rail.waiting", new Object[]{waiting})
						.withStyle(s -> s.withColor(16755200))
				);
			}

			if (shownNow && hasAbout(c)) {
				out.add(Component.translatable("cfhgui_quests.vanilla.tabs.again").withStyle(s -> s.withColor(8421504)));
			}

			return out;
		} else {
			out.add(Component.translatable("cfhgui_quests.rail.locked").withStyle(s -> s.withColor(16755200)));
			BookIndex index = QuestsState.index();
			int listed = 0;

			for (String need : c.requires) {
				String key = Ids.resolve(c.id, need);
				BookIndex.Entry e = index == null ? null : index.get(key);
				if (e != null && !QuestsState.progress().done(key)) {
					if (listed++ >= 5) {
						out.add(Component.literal("…"));
						break;
					}

					out.add(
						Component.literal("• ")
							.append(QuestText.title(e))
							.append(Component.literal("  (").append(QuestText.title(e.chapter())).append(")").withStyle(s -> s.withColor(8421504)))
					);
				}
			}

			return out;
		}
	}

	private static List<Component> treeTip(Trees.Branch b, boolean open, BookStats.Stat stat, boolean shownNow) {
		List<Component> out = new ArrayList<>();
		Chapter c = b.chapter();
		out.add(QuestText.title(c));
		BookIndex.Entry way = QuestsState.entry(b.gateway());
		if (!open) {
			out.add(
				(way == null
						? Component.translatable("cfhgui_quests.tabs.locked")
						: Component.translatable("cfhgui_quests.tabs.locked.by", new Object[]{QuestText.title(way)}))
					.withStyle(s -> s.withColor(16755200))
			);
			return out;
		} else {
			if (b.depth() > 0 && way != null) {
				out.add(Component.translatable("cfhgui_quests.vanilla.tabs.tree", new Object[]{QuestText.title(way.chapter())}).withStyle(s -> s.withColor(8421504)));
			}

			if (!c.subtitle.isBlank()) {
				out.add(QuestText.text(c.subtitle).withStyle(s -> s.withColor(10526880)));
			}

			if (stat.total() > 0) {
				out.add(Component.translatable("cfhgui_quests.rail.progress", new Object[]{stat.done(), stat.total()}));
			}

			if (stat.claimable() > 0) {
				out.add(
					Component.translatable(stat.claimable() == 1 ? "cfhgui_quests.rail.waiting.one" : "cfhgui_quests.rail.waiting", new Object[]{stat.claimable()})
						.withStyle(s -> s.withColor(16755200))
				);
			}

			if (shownNow && hasAbout(c)) {
				out.add(Component.translatable("cfhgui_quests.vanilla.tabs.again").withStyle(s -> s.withColor(8421504)));
			}

			return out;
		}
	}

	private void pickGroup(String tabId) {
		String id = tabId.substring("group:".length());
		VanillaBookScreen.Group mine = this.groupOf(this.topId);
		if (mine == null || !mine.id().equals(id)) {
			for (VanillaBookScreen.Group g : this.groupList) {
				if (g.id().equals(id)) {
					Chapter to = null;
					String last = LAST_OF_GROUP.get(id);

					for (Chapter c : g.chapters()) {
						if (c.id.equals(last) && BookStats.open(c)) {
							to = c;
						}
					}

					if (to == null) {
						for (Chapter cx : g.chapters()) {
							if (BookStats.open(cx)) {
								to = cx;
								break;
							}
						}
					}

					if (to != null) {
						this.showChapter(to.id, TreeView.Travel.ENTRANCE, null);
					}

					return;
				}
			}
		}
	}

	private void pickChapter(String chapterId) {
		this.showChapter(chapterId, TreeView.Travel.ENTRANCE, null);
	}

	private void chapterAgain(String chapterId) {
		if (chapterId.equals(this.shownId)) {
			this.about();
		} else {
			this.pickTree(chapterId);
		}
	}

	void about() {
		Chapter c = this.shown();
		if (c != null) {
			if (c.id.equals(this.page.aboutChapter())) {
				this.closeQuest();
			} else if (hasAbout(c)) {
				CFHGuiSounds.tick();
				this.canvas.select(null);
				this.page.showChapter(c.id);
				this.recover();
				this.canvas.fit();
			}
		}
	}

	private static boolean hasAbout(@Nullable Chapter c) {
		return c != null && (!c.description.isEmpty() || !c.subtitle.isBlank());
	}

	private void pickTree(String chapterId) {
		QuestBook book = QuestsState.book();
		Chapter from = this.shown();
		Chapter c = book == null ? null : book.chapter(chapterId);
		if (book != null && from != null && c != null && from != c) {
			String down = Trees.wayDown(book, from, c);
			if (down != null) {
				this.showChapter(c.id, TreeView.Travel.INTO, nearest(book, c, from, down));
			} else {
				String up = Trees.wayDown(book, c, from);
				this.showChapter(c.id, up != null ? TreeView.Travel.OUT : TreeView.Travel.ENTRANCE, up == null ? null : nearest(book, from, c, up));
			}
		}
	}

	private static String nearest(QuestBook book, Chapter below, Chapter above, String otherwise) {
		Chapter parent = book.chapter(below.parent);
		if (parent != null && parent != above && Trees.host(book, parent) == above) {
			String way = Trees.gateway(parent, below);
			return way == null ? otherwise : way;
		} else {
			return otherwise;
		}
	}

	private static void keepOpen(List<String> ways) {
		List<String> kept = QuestsConfig.get().openTrees;

		for (String way : ways) {
			if (!kept.contains(way)) {
				kept.add(way);
			}
		}
	}

	private static List<String> waysTo(QuestBook book, @Nullable String questKey) {
		BookIndex.Entry e = QuestsState.entry(questKey);
		return e == null ? List.of() : Trees.waysIn(book, e.chapter());
	}

	@Override
	public boolean opensInPlace(String questKey) {
		BookIndex.Entry e = QuestsState.entry(questKey);
		return e != null && e.quest().isInline() && this.canvas.opensInline();
	}

	@Override
	public boolean unfolded(String questKey) {
		return this.canvas.unfolded(questKey);
	}

	@Override
	public void unfold(String questKey, boolean open) {
		if (this.opensInPlace(questKey)) {
			this.canvas.unfold(questKey, open);
			if (open) {
				this.canvas.reveal(questKey);
			}
		}
	}

	void showChapter(String chapterId, TreeView.Travel how, @Nullable String through) {
		QuestBook book = QuestsState.book();
		Chapter asked = book == null ? null : book.chapter(chapterId);
		if (asked != null) {
			List<String> ways = Trees.waysIn(book, asked);
			Chapter c = ways.isEmpty() ? asked : Trees.host(book, asked);
			keepOpen(ways);
			keepOpen(waysTo(book, through));
			String way = ways.isEmpty() ? null : ways.get(ways.size() - 1);
			if (way != null && c.id.equals(this.shownId) && !this.canvas.travelling() && this.canvas.chapter() == c) {
				this.closeQuest();

				for (String w : ways) {
					this.canvas.unfold(w, true);
				}

				this.canvas.focus(way);
			} else {
				this.choose(c.id);
				QuestsConfig.get().lastChapter = c.id;
				this.closeQuest();
				this.fillTabs();
				this.recover();
				this.canvas.show(c, how, through);
				if (way != null) {
					this.canvas.focus(way);
				}

				this.chrome();
				AdvancementChapters.looking(c);
			}
		}
	}

	@Override
	public void enterTree(String questKey) {
		QuestBook book = QuestsState.book();
		BookIndex.Entry e = QuestsState.entry(questKey);
		Chapter sub = book != null && e != null && e.quest().isGateway() ? book.chapter(e.quest().subtree) : null;
		if (sub != null) {
			if (this.opensInPlace(questKey)) {
				this.unfold(questKey, true);
			} else if (BookStats.open(sub)) {
				this.showChapter(sub.id, Trees.host(book, e.chapter()).id.equals(this.shownId) ? TreeView.Travel.INTO : TreeView.Travel.ENTRANCE, questKey);
			}
		}
	}

	boolean leaveTree() {
		QuestBook book = QuestsState.book();
		Chapter c = this.shown();
		if (book != null && c != null && c.isSubtree()) {
			Chapter up = book.chapter(c.parent);
			if (up == null) {
				return false;
			} else {
				String through = Trees.gateway(up, c);
				this.showChapter(up.id, through != null ? TreeView.Travel.OUT : TreeView.Travel.ENTRANCE, through);
				return true;
			}
		} else {
			return false;
		}
	}

	@Override
	public void goTo(String questKey) {
		QuestBook book = QuestsState.book();
		BookIndex.Entry e = QuestsState.entry(questKey);
		if (book != null && e != null && QuestsState.status(questKey) != Rules.Status.HIDDEN) {
			this.search.close();
			this.setFocused(this.canvas);
			List<String> ways = Trees.waysIn(book, e.chapter());
			Chapter at = ways.isEmpty() ? e.chapter() : Trees.host(book, e.chapter());
			keepOpen(ways);
			if (!at.id.equals(this.shownId) || this.canvas.travelling()) {
				this.showChapter(at.id, TreeView.Travel.ENTRANCE, null);
			}

			this.canvas.cover(0, 0, this.page.openWidth(), 0);
			if (this.canvas.focus(questKey)) {
				this.openQuest(questKey);
			} else if (!ways.isEmpty()) {
				int i = ways.size() - 1;

				while (i >= 0 && !this.canvas.focus(ways.get(i))) {
					i--;
				}

				this.openQuest(questKey);
			} else {
				this.recover();
			}
		}
	}

	void openQuest(String questKey) {
		if (QuestsState.entry(questKey) != null) {
			this.canvas.select(questKey);
			this.page.show(questKey);
			this.recover();
			this.canvas.reveal(questKey);
		}
	}

	void closeQuest() {
		if (this.page.isOpen()) {
			this.page.close();
			this.recover();
			if (this.page.holds(this.getFocused())) {
				this.setFocused(this.canvas);
			}
		}
	}

	@Override
	public void closePage() {
		this.closeQuest();
	}

	private void recover() {
		this.canvas.cover(0, 0, this.page.cover(), 0);
	}

	void show(@Nullable String chapterId, @Nullable String questKey) {
		if (this.hadBook && this.minecraft != null) {
			if (questKey != null && QuestsState.entry(questKey) != null) {
				this.goTo(questKey);
			} else {
				if (chapterId != null && !chapterId.equals(this.shownId)) {
					this.showChapter(chapterId, TreeView.Travel.ENTRANCE, null);
				}
			}
		}
	}

	private void chrome() {
		BookStats.Stat stat = BookStats.book();
		this.bookFill.set(stat.fraction());
		int waiting = stat.claimable();
		if (this.claimAll != null) {
			this.claimAll.active = waiting > 0;
			this.claimAll
				.setMessage(
					waiting > 0
						? Component.translatable("cfhgui_quests.claim_all.count", new Object[]{waiting > 99 ? "99+" : String.valueOf(waiting)})
						: Component.translatable("cfhgui_quests.claim_all")
				);
			this.claimAll
				.setTooltip(
					Tooltip.create(
						waiting > 1
							? Component.translatable("cfhgui_quests.claim_all.tip.count", new Object[]{waiting})
							: Component.translatable(waiting == 1 ? "cfhgui_quests.claim_all.tip" : "cfhgui_quests.claim_all.none")
					)
				);
		}

		if (this.editButton != null) {
			Chapter c = this.shown();
			this.editButton.active = c != null && !c.readOnly;
			this.editButton
				.setTooltip(Tooltip.create(Component.translatable(this.editButton.active ? "cfhgui_quests.vanilla.edit.tip" : "cfhgui_quests.vanilla.edit.readonly")));
		}

		if (this.filterButton != null && this.filterButton.getValue() != this.canvas.filter()) {
			this.filterButton.setValue(this.canvas.filter());
		}
	}

	private void claimEverything() {
		QuestActions.claimAllAsking(
			(claimed, result) -> {
				if (result.ok()) {
					CFHGuiToasts.show(
						Component.translatable("cfhgui_quests.claim_all.done"),
						Component.translatable(claimed == 1 ? "cfhgui_quests.claim_all.done.one" : "cfhgui_quests.claim_all.done.many", new Object[]{claimed}),
						Icon.STAR_FILLED
					);
				} else {
					CFHGuiToasts.show(Component.translatable("cfhgui_quests.result.failed"), QuestActions.describe(result), Icon.WARNING);
				}
			}
		);
	}

	void filter(TreeFilter f) {
		this.canvas.filter(f);
		this.chrome();
	}

	private boolean context(@Nullable String questKey, double mouseX, double mouseY, float treeX, float treeY) {
		List<Item> items = new ArrayList<>();
		BookIndex.Entry e = QuestsState.entry(questKey);
		if (e == null) {
			items.add(Item.of(Component.translatable("cfhgui_quests.menu.fit"), Icon.FULLSCREEN, this.canvas::fit));
			items.add(Item.sep());
			Chapter c = this.shown();
			BookStats.Stat stat = BookStats.of(c);

			for (TreeFilter f : TreeFilter.values()) {
				int n = switch (f) {
					case ALL -> c == null ? 0 : c.quests.size();
					case TODO -> stat.open();
					case REWARDS -> stat.claimable();
					case DONE -> stat.done();
				};
				items.add(
					Item.checked(Component.translatable("cfhgui_quests.filter." + f.key() + ".count", new Object[]{n}), this.canvas.filter() == f, () -> this.filter(f))
				);
			}
		} else {
			String key = e.key();
			Rules.Status status = QuestsState.status(key);
			Quest q = e.quest();
			boolean secret = status == Rules.Status.LOCKED && q.visibility == Quest.Visibility.SECRET;
			this.canvas.select(key);
			items.add(Item.of(Component.translatable("cfhgui_quests.menu.open"), Icon.QUEST, () -> this.openQuest(key)));
			boolean pinned = QuestsState.pinned(key);
			if (pinned || !status.done() && !secret) {
				items.add(Item.of(Component.translatable(pinned ? "cfhgui_quests.unpin" : "cfhgui_quests.pin"), Icon.PIN, () -> QuestsState.pin(key, !pinned)));
			}

			if (this.opensInPlace(key)) {
				boolean unfolded = this.canvas.unfolded(key);
				if (!secret) {
					items.add(
						Item.of(
							Component.translatable(unfolded ? "cfhgui_quests.inline.fold" : "cfhgui_quests.inline.open"),
							unfolded ? Icon.MINUS : Icon.PLUS,
							() -> this.unfold(key, !unfolded)
						)
					);
				}
			} else if (q.isGateway() && status != Rules.Status.LOCKED) {
				items.add(Item.of(Component.translatable("cfhgui_quests.open_tree"), Icon.COMPASS, () -> this.enterTree(key)));
			}

			if (status == Rules.Status.CLAIMABLE) {
				boolean choice = false;

				for (int i = 0; i < q.rewards.size(); i++) {
					if (q.rewards.get(i).is("choice") && !QuestsState.state(key).claimed(i)) {
						choice = true;
					}
				}

				if (choice) {
					items.add(Item.of(Component.translatable("cfhgui_quests.menu.choose"), Icon.STAR_FILLED, () -> this.openQuest(key)));
				} else {
					items.add(Item.of(Component.translatable("cfhgui_quests.claim"), Icon.STAR_FILLED, () -> QuestActions.claim(key, -1, 0, null)));
				}
			}

			if (QuestsState.canEdit() && !e.chapter().readOnly) {
				items.add(Item.sep());
				if (!status.done()) {
					items.add(Item.of(Component.translatable("cfhgui_quests.menu.complete"), Icon.CHECK, () -> QuestActions.complete(key, null, null)));
				}

				items.add(
					Item.danger(
						Component.translatable("cfhgui_quests.menu.reset"),
						Icon.UNDO,
						() -> CFHGuiModal.confirmDanger(
								Component.translatable("cfhgui_quests.menu.reset.title"),
								Component.translatable("cfhgui_quests.menu.reset.body", new Object[]{QuestText.title(e)}),
								Component.translatable("cfhgui_quests.menu.reset.ok"),
								() -> QuestActions.reset(key, null, null)
							)
					)
				);
			}
		}

		Popups.open(new MenuPopup((int)mouseX, (int)mouseY, items, 130));
		return true;
	}

	private void onBookChanged() {
		QuestBook book = QuestsState.book();
		if (this.minecraft != null) {
			if (book != null != this.hadBook) {
				this.shownOnce = false;
				this.closeQuest();
				this.choose(book == null ? null : firstChapter(book));
				this.rebuildWidgets();
			} else if (book != null && this.shownOnce) {
				String was = this.shownId;
				if (book.chapter(this.shownId) == null) {
					this.choose(firstChapter(book));
				} else {
					this.choose(this.shownId);
				}

				this.fillTabs();
				Chapter c = book.chapter(this.shownId);
				if (c != null) {
					if (c.id.equals(was)) {
						this.canvas.rebuild(c);
					} else {
						this.canvas.show(c, TreeView.Travel.ENTRANCE, null);
					}
				}

				this.page.refresh();
				this.recover();
				this.chrome();
			}
		}
	}

	private void onProgressChanged(List<String> keys) {
		if (this.minecraft != null && this.shownOnce) {
			this.canvas.refresh();
			this.fillTabs();
			this.page.refresh();
			this.chrome();
		}
	}

	public void removed() {
		super.removed();
		this.canvas.remember();
		QuestsConfig.save();
		this.awayRevision = BookStats.revision();
		if (this.listening) {
			this.listening = false;
			QuestsState.BOOK_CHANGED.unregister(this.bookListener);
			QuestsState.PROGRESS_CHANGED.unregister(this.progressListener);
		}
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.hadBook && this.search.keyPressed(keyCode)) {
			return true;
		} else {
			GuiEventListener focus;
			boolean var10000;
			label143: {
				focus = this.getFocused();
				if (focus instanceof EditBox box && box.isFocused()) {
					var10000 = true;
					break label143;
				}

				var10000 = false;
			}

			boolean typing = var10000;
			if (keyCode == 256 && typing && focus instanceof EditBox box) {
				if (!box.getValue().isEmpty()) {
					box.setValue("");
				} else {
					this.setFocused(this.hadBook ? this.canvas : null);
				}

				return true;
			} else if (keyCode == 256 && this.page.isOpen()) {
				this.closeQuest();
				return true;
			} else {
				if (!typing && this.hadBook && QuestsState.available()) {
					if (QuestsClient.BOOK_KEY.matches(keyCode, scanCode)) {
						this.onClose();
						return true;
					}

					if (this.minecraft.options.keyAdvancements.matches(keyCode, scanCode) && AdvancementChapters.is(this.shown())) {
						this.onClose();
						return true;
					}

					boolean inPage = this.page.isOpen() && this.page.holds(focus);
					if (keyCode == 259 && !inPage && this.leaveTree()) {
						return true;
					}

					boolean ctrl = hasControlDown() || (modifiers & (Minecraft.ON_OSX ? 8 : 2)) != 0;
					if (keyCode == 266 || keyCode == 267) {
						int direction = keyCode == 267 ? 1 : -1;
						String to = ctrl ? this.groups.neighbour(direction) : this.chapters.neighbour(direction);
						if (to != null) {
							if (ctrl) {
								this.pickGroup(to);
							} else {
								this.pickChapter(to);
							}
						}

						return true;
					}

					if (keyCode == 47 || keyCode == 70 && ctrl) {
						this.swallowChar = true;
						this.setFocused(this.search.field());
						return true;
					}

					if (!inPage && treeKey(keyCode) && this.canvas.key(keyCode, scanCode, modifiers)) {
						return true;
					}

					if (!inPage && keyCode == 258) {
						return true;
					}
				}

				return super.keyPressed(keyCode, scanCode, modifiers);
			}
		}
	}

	private static boolean treeKey(int keyCode) {
		return switch (keyCode) {
			case 32, 45, 61, 257, 258, 262, 263, 264, 265, 268, 333, 334, 335 -> true;
			default -> false;
		};
	}

	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		this.swallowChar = false;
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	public boolean charTyped(char codePoint, int modifiers) {
		if (this.swallowChar) {
			this.swallowChar = false;
			return true;
		} else {
			return super.charTyped(codePoint, modifiers);
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.hadBook && this.search.mouseClicked(mouseX, mouseY, button)) {
			return true;
		} else if (this.hadBook && button == 0 && this.overTitle(mouseX, mouseY)) {
			this.about();
			return true;
		} else {
			boolean handled = super.mouseClicked(mouseX, mouseY, button);
			if (this.hadBook && !handled && button == 0 && this.search.overWell(mouseX, mouseY)) {
				this.setFocused(this.search.field());
				handled = true;
			}

			GuiEventListener focus = this.getFocused();
			if (this.hadBook && !(focus instanceof EditBox) && !this.page.holds(focus)) {
				this.setFocused(this.canvas);
			}

			return handled;
		}
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		return this.canvas.dragging() ? this.canvas.mouseDragged(mouseX, mouseY, button, dragX, dragY) : super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		boolean handled = super.mouseReleased(mouseX, mouseY, button);
		return this.canvas.mouseReleased(mouseX, mouseY, button) || handled;
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return this.hadBook && this.search.mouseScrolled(mouseX, mouseY, scrollY) ? true : super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private boolean pointerOnTree(int mouseX, int mouseY) {
		return !Popups.any() && !this.search.contains((double)mouseX, (double)mouseY)
			? this.inside.contains((double)mouseX, (double)mouseY) && !this.page.isMouseOver((double)mouseX, (double)mouseY)
			: false;
	}

	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		QuestBook book = QuestsState.book();
		boolean has = book != null && this.hadBook;
		this.header(g, mouseX, mouseY, has ? book : null);
		boolean over = has && this.pointerOnTree(mouseX, mouseY);
		if (has && this.shownId != null) {
			this.canvas.render(g, over ? mouseX : -9999, over ? mouseY : -9999, partialTick);
			this.page.render(g, mouseX, mouseY, partialTick);
		} else {
			this.nothing(g, book == null ? (this.minecraft.level == null ? "no_world" : "no_server") : "empty");
		}

		CanvasDraw.layer(g);
		WindowArt.window(g, this.window.x(), this.window.y(), this.window.w(), this.window.h(), 1.0F);
		if (has) {
			this.groups.render(g, mouseX, mouseY, partialTick);
			this.chapters.render(g, mouseX, mouseY, partialTick);
			this.trees.render(g, mouseX, mouseY, partialTick);
		}

		this.strip(g, mouseX, mouseY, partialTick, has);
		if (has) {
			this.painter.clearRight(this.page.left());
			if (over && this.shownId != null) {
				this.canvas.renderOver(g, mouseX, mouseY);
			}

			this.search.renderList(g, mouseX, mouseY, this.width, this.window.bottom() - 4);
		}
	}

	private void header(GuiGraphics g, int mouseX, int mouseY, @Nullable QuestBook book) {
		Theme t = Theme.current();
		boolean van = t.isVanilla();
		int cx = this.width / 2;
		Component name = (Component)(van ? this.getTitle() : Fonts.heading(this.getTitle()));
		int titleY = this.roomy ? 7 : 3;
		FormattedCharSequence title = CFHGuiDraw.truncate(name, this.width - 24);
		g.drawString(this.font, title, cx - this.font.width(title) / 2, titleY, van ? -1 : t.palette().text(), van);
		if (book != null) {
			BookStats.Stat stat = BookStats.book();
			if (stat.total() > 0) {
				int bw = Math.min(182, this.width - 40);
				int bx = cx - bw / 2;
				int by = this.roomy ? 21 : 14;
				WindowArt.progress(g, bx, by, bw, this.bookFill.get(), stat.complete(), 1.0F);
				int counted = Mth.clamp(Math.round(this.bookFill.get() * (float)stat.total()), 0, stat.total());
				Component count = Component.translatable("cfhgui_quests.rail.progress", new Object[]{counted, stat.total()});
				int room = bx - 10;
				if (this.font.width(count) <= room) {
					g.drawString(this.font, count, bx - 6 - this.font.width(count), by - 2, van ? -6250336 : t.palette().textMuted(), van);
				}

				int waiting = stat.claimable();
				if (waiting > 0) {
					Component w = Component.translatable(waiting == 1 ? "cfhgui_quests.rail.waiting.one" : "cfhgui_quests.rail.waiting", new Object[]{waiting});
					if (this.font.width(w) <= room) {
						g.drawString(this.font, w, bx + bw + 6, by - 2, van ? -22016 : t.palette().warning(), van);
					}
				}

				if (mouseX >= bx && mouseX < bx + bw && mouseY >= by - 2 && mouseY < by + 7) {
					CFHGuiTooltips.request(
						List.of(Component.translatable("cfhgui_quests.vanilla.book.progress", new Object[]{stat.done(), stat.total(), Math.round(stat.fraction() * 100.0F)})),
						null
					);
				}
			}
		}
	}

	private void strip(GuiGraphics g, int mouseX, int mouseY, float partialTick, boolean has) {
		Theme t = Theme.current();
		boolean van = t.isVanilla();
		QuestBook book = QuestsState.book();
		Chapter c = has ? this.shown() : null;
		Component name = (Component)(c == null ? Component.translatable("cfhgui_quests.title") : QuestText.title(c));
		if (c != null && book != null && c.isSubtree()) {
			Chapter top = Trees.top(book, c);
			if (top != c) {
				name = Component.empty().append(QuestText.title(top)).append(" › ").append(name);
			}
		}

		if (!van) {
			name = Fonts.heading(name);
		}

		int right = this.window.right() - 8;
		if (has) {
			this.search.renderField(g, mouseX, mouseY, partialTick);
			right = this.search.left() - 6;
			BookStats.Stat stat = BookStats.of(c);
			if (c != null && stat.total() > 0) {
				String count = stat.done() + " / " + stat.total();
				int cw = this.font.width(count);
				if (right - cw > this.window.x() + 60) {
					g.drawString(
						this.font, count, right - cw, this.window.y() + 6, stat.complete() ? (van ? -13735122 : t.palette().success()) : WindowArt.titleInkQuiet(), false
					);
					right -= cw + 8;
				}
			}
		}

		FormattedCharSequence shownName = CFHGuiDraw.truncate(name, right - this.window.x() - 8);
		int nameX = this.window.x() + 8;
		int nameW = this.font.width(shownName);
		int nameY = this.window.y() + 6;
		boolean link = hasAbout(c);
		this.titleLeft = nameX - 2;
		this.titleRight = link ? nameX + nameW + 2 : this.titleLeft;
		boolean hot = link && this.overTitle((double)mouseX, (double)mouseY) && !Popups.any();
		this.titleHover.set(hot);
		float lit = this.titleHover.get();
		g.drawString(this.font, shownName, nameX, nameY, Colors.lerp(WindowArt.titleInk(), van ? -16777216 : t.accent(), lit), false);
		if (lit > 0.02F) {
			g.fill(nameX, nameY + 9, nameX + nameW, nameY + 10, Colors.withAlpha(van ? -16777216 : t.accent(), Math.round(192.0F * lit)));
		}

		if (hot != this.titleHot) {
			this.titleHot = hot;
			this.titleHotSinceMs = Clock.nowMs();
		}

		if (hot && Clock.nowMs() - this.titleHotSinceMs > 350L) {
			CFHGuiTooltips.requestAt(List.of(Component.translatable("cfhgui_quests.tabs.about")), nameX, this.window.y() + 20);
		}

		if (c != null && !c.readOnly && !c.subtitle.isBlank()) {
			int from = nameX + nameW + 10;
			int room = right - from;
			if (room >= 60) {
				g.drawString(this.font, CFHGuiDraw.truncate(QuestText.text(c.subtitle), room), from, nameY, van ? -8421505 : t.palette().textDim(), false);
			}
		}
	}

	private boolean overTitle(double mouseX, double mouseY) {
		return mouseX >= (double)this.titleLeft
			&& mouseX < (double)this.titleRight
			&& mouseY >= (double)(this.window.y() + 3)
			&& mouseY < (double)(this.window.y() + 17);
	}

	private void nothing(GuiGraphics g, String state) {
		Theme t = Theme.current();
		boolean van = t.isVanilla();
		g.fill(this.inside.x(), this.inside.y(), this.inside.right(), this.inside.bottom(), van ? -16777216 : t.panel(t.palette().bg()));
		Component title = Component.translatable("cfhgui_quests.state." + state + ".title");
		List<FormattedCharSequence> lines = this.font
			.split(Component.translatable("cfhgui_quests.state." + state + ".body"), Mth.clamp(this.inside.w() - 24, 60, 280));
		int y = this.inside.centerY() - (13 + lines.size() * 10) / 2;
		CFHGuiDraw.textCentered(g, (Component)(van ? title : Fonts.heading(title)), this.inside.centerX(), y, van ? -1 : t.palette().text(), van);
		y += 13;

		for (FormattedCharSequence line : lines) {
			if (y + 9 > this.inside.bottom() - 2) {
				break;
			}

			g.drawString(this.font, line, this.inside.centerX() - this.font.width(line) / 2, y, van ? -6250336 : t.palette().textMuted(), van);
			y += 10;
		}

		if (this.inside.bottom() - 9 > y + 6) {
			CFHGuiDraw.textCentered(
				g, Component.translatable("advancements.sad_label"), this.inside.centerX(), this.inside.bottom() - 9, van ? -1 : t.palette().textDim(), van
			);
		}
	}

	private static record Group(String id, Component title, String icon, List<Chapter> chapters) {
	}
}
