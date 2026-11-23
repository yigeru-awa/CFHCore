package com.onehumanawa.cfhcore.content.gui.quests.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsConfig;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestActions;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsClient;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.RichText;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.BookStats;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CanvasDraw;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestCanvas;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CFHGuiQuestPainter;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.TreeFilter;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.TreeView;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public class QuestBookScreen extends CFHGuiScreen implements QuestSheet.Host, RichText.Host {
	private static final int VEIL_IN_MS = 110;
	private static final int VEIL_OUT_MS = 260;
	private static final int NARROW = 560;
	private TreeView tree;
	private final QuestBookScreen.TreeFactory factory;
	private final Map<Layout, TreeView> trees = new EnumMap<>(Layout.class);
	private final ChapterRail rail = new ChapterRail(this::pickChapter);
	private final TreeTabs tabs = new TreeTabs(this::pickTree, this::toggleIntro);
	private final QuestSheet sheet = new QuestSheet(this);
	private final SearchBox search = new SearchBox(this::goTo);
	private final ChapterIntro intro = new ChapterIntro();
	@Nullable
	private String topId;
	@Nullable
	private String shownId;
	@Nullable
	private String pendingQuest;
	private boolean shownOnce;
	private boolean hadBook;
	private Rect treeRect = new Rect(0, 0, 0, 0);
	@Nullable
	private BookExtensions.SidePanel sidePanel;
	private Rect sideRect = new Rect(0, 0, 0, 0);
	private final Anim sideIn = new Anim(1.0F, 240, Ease.OUT_CUBIC);
	private final List<AbstractWidget> sideWidgets = new ArrayList<>();
	@Nullable
	private CFHGuiButton claimAll;
	@Nullable
	private CFHGuiButton filterButton;
	@Nullable
	private ViewSwitch viewSwitch;
	@Nullable
	private TreeView switchingTo;
	private final Anim veil = new Anim(0.0F, 110, Ease.OUT_CUBIC);
	private final Runnable bookListener = this::onBookChanged;
	private final Consumer<List<String>> progressListener = this::onProgressChanged;
	private boolean listening;
	@Nullable
	private Consumer<QuestBookScreen> onReady;

	public QuestBookScreen(@Nullable Screen parent) {
		this(parent, screen -> new QuestCanvas(0, 0, 10, 10, new CFHGuiQuestPainter()));
	}

	public QuestBookScreen(@Nullable Screen parent, QuestBookScreen.TreeFactory factory) {
		super(Component.translatable("cfhgui_quests.title"), parent);
		BookStats.revision();
		this.factory = factory;
		this.tree = this.wire(factory.create(this));
		this.trees.put(this.layout(), this.tree);
		this.intro.host(this);
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

	private TreeView wire(TreeView made) {
		made.onOpen(key -> {
			if (key == null) {
				this.closeQuest();
			} else {
				this.openQuest(key);
			}
		});
		made.onEnter(this::enterTree);
		made.onContext(this::context);
		return made;
	}

	@Nullable
	private static String firstChapter(QuestBook book) {
		for (Chapter c : book.topChapters()) {
			if (BookStats.open(c)) {
				return c.id;
			}
		}

		List<Chapter> top = book.topChapters();
		return top.isEmpty() ? null : top.get(0).id;
	}

	public Component getTitle() {
		QuestBook book = QuestsState.book();
		return book == null ? super.getTitle() : QuestText.title(book);
	}

	public boolean isPauseScreen() {
		return false;
	}

	public TreeView tree() {
		return this.tree;
	}

	@Nullable
	public QuestCanvas canvas() {
		return this.tree instanceof QuestCanvas c ? c : null;
	}

	@Nullable
	public Chapter shown() {
		QuestBook book = QuestsState.book();
		return book == null ? null : book.chapter(this.shownId);
	}

	public ChapterRail rail() {
		return this.rail;
	}

	public TreeTabs tabs() {
		return this.tabs;
	}

	public QuestSheet sheet() {
		return this.sheet;
	}

	public void rebuild() {
		this.rebuildWidgets();
	}

	@Nullable
	public Screen parentScreen() {
		return this.parent;
	}

	public void show(@Nullable String chapterId, @Nullable String questKey) {
		if (this.hadBook && this.minecraft != null) {
			if (questKey != null && QuestsState.entry(questKey) != null) {
				this.goTo(questKey);
			} else {
				if (chapterId != null && !chapterId.equals(this.shownId)) {
					this.showChapter(chapterId);
				}
			}
		}
	}

	public void sidePanel(@Nullable BookExtensions.SidePanel panel) {
		if (this.sidePanel != panel) {
			if (this.sidePanel != null) {
				this.sidePanel.closed();
			}

			this.sidePanel = panel;
			if (panel != null) {
				this.closeQuest();
				this.sideIn.snap(0.0F);
				this.sideIn.set(1.0F);
			}

			if (this.minecraft != null) {
				this.rebuildWidgets();
			}
		}
	}

	@Nullable
	public BookExtensions.SidePanel sidePanel() {
		return this.sidePanel;
	}

	public int sidePanelOffset() {
		return this.sidePanel == null ? 0 : Math.round((1.0F - this.sideIn.get()) * (float)(this.sideRect.w() + 6));
	}

	private boolean inPlace() {
		return this.tree.opensInline() && this.sidePanel == null;
	}

	private QuestBookScreen.Landing landing(QuestBook book, Chapter chapter) {
		if (!this.inPlace()) {
			return new QuestBookScreen.Landing(chapter, List.of());
		} else {
			List<String> ways = Trees.waysIn(book, chapter);
			return ways.isEmpty() ? new QuestBookScreen.Landing(chapter, ways) : new QuestBookScreen.Landing(Trees.host(book, chapter), ways);
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

	private List<String> waysTo(QuestBook book, @Nullable String questKey) {
		BookIndex.Entry e = QuestsState.entry(questKey);
		return e != null && this.inPlace() ? Trees.waysIn(book, e.chapter()) : List.of();
	}

	@Override
	public boolean opensInPlace(String questKey) {
		BookIndex.Entry e = QuestsState.entry(questKey);
		return e != null && e.quest().isInline() && this.inPlace();
	}

	@Override
	public boolean unfolded(String questKey) {
		return this.tree.unfolded(questKey);
	}

	@Override
	public void unfold(String questKey, boolean open) {
		if (this.opensInPlace(questKey)) {
			this.tree.unfold(questKey, open);
			if (open) {
				this.tree.reveal(questKey);
			}
		}
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

	protected void build() {
		if (!this.listening) {
			this.listening = true;
			QuestsState.BOOK_CHANGED.register(this.bookListener);
			QuestsState.PROGRESS_CHANGED.register(this.progressListener);
		}

		QuestBook book = QuestsState.book();
		this.hadBook = book != null;
		this.claimAll = null;
		this.filterButton = null;
		this.sideWidgets.clear();
		if (book != null) {
			Rect r = this.contentRect();
			boolean narrow = this.width < 560;
			this.claimAll = (CFHGuiButton)this.addHeaderAction(
				new CFHGuiButton(0, 0, narrow ? 44 : 90, 20, Component.translatable("cfhgui_quests.claim_all"), this::claimEverything)
					.variant(Variant.PRIMARY)
					.icon(Icon.STAR_FILLED)
			);
			this.filterButton = (CFHGuiButton)this.addHeaderAction(
				new CFHGuiButton(0, 0, narrow ? 20 : 86, 20, Component.empty(), this::filterMenu).variant(Variant.SECONDARY).icon(Icon.FILTER)
			);
			CFHGuiSearchField field = (CFHGuiSearchField)this.addHeaderAction(this.search.field(this.width >= 620 ? 132 : (narrow ? 76 : 104)));
			this.slashFocusTarget = field;
			this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.KEYBOARD, Component.empty(), () -> {
			}).tip(this.keysHelp()));
			if (QuestsUi.hasSettings()) {
				this.addHeaderAction(
					new CFHGuiIconButton(0, 0, 20, Icon.SETTINGS, Component.translatable("cfhgui_quests.settings.open_from_book"), () -> QuestsUi.openSettings(this))
				);
			}

			BookExtensions.each(e -> e.header(this, x$0 -> this.addHeaderAction(x$0)));
			this.viewSwitch = null;
			if (bothViews()) {
				int titleEnd = this.headerTitleX() + this.font.width(Fonts.heading(this.getTitle())) + 12;
				if (!narrow && this.headerActionsLeft() - 4 - titleEnd < ViewSwitch.width(true)) {
					this.claimAll.setWidth(44);
					this.filterButton.setWidth(20);
					field.setWidth(76);
					this.reflowHeaderActions();
				}

				int room = this.headerActionsLeft() - 4 - titleEnd;
				this.viewSwitch = (ViewSwitch)this.addHeaderAction(new ViewSwitch(this.layout() == Layout.OVERHAUL, room >= ViewSwitch.width(true), this::switchView));
				this.viewSwitch.busy(this.sidePanel != null ? Component.translatable("cfhgui_quests.view.busy") : null);
			}

			int railW = ChapterRail.widthFor(this.width);
			this.rail.place(r.x(), r.y(), railW, r.h());
			int tx = r.x() + railW + 6;
			int tw = r.right() - tx;
			this.tabs.place(tx, r.y(), tw);
			this.treeRect = new Rect(tx, r.y() + 22, tw, r.h() - 22);
			this.tree.place(this.treeRect.x(), this.treeRect.y(), this.treeRect.w(), this.treeRect.h());
			this.sheet.place(this.treeRect);
			this.intro.place(this.treeRect);
			BookExtensions.SidePanel panel = this.sidePanel;
			if (panel != null) {
				int pw = Math.min(panel.width(this.treeRect.w()), Math.max(120, this.treeRect.w() - 40));
				this.sideRect = new Rect(this.treeRect.right() - pw, this.treeRect.y(), pw, this.treeRect.h());
				this.intro.place(new Rect(this.treeRect.x(), this.treeRect.y(), this.treeRect.w() - pw, this.treeRect.h()));

				try {
					panel.build(this, this.sideRect.inset(8, 8), w -> {
						this.sideWidgets.add(w);
						this.add(w);
					});
				} catch (Exception var12) {
					CFHGuiQuests.LOGGER.error("[CFHGui Quests] a side panel of the quest book could not be built", var12);
				}
			} else {
				this.sideRect = new Rect(0, 0, 0, 0);
				this.add(this.sheet);
			}

			this.add(this.rail);
			this.add(this.tabs);
			this.add(this.tree.widget());
			if (!this.shownOnce) {
				this.shownOnce = true;
				this.rail.refresh();
				Chapter asked = book.chapter(this.shownId);
				if (asked != null) {
					QuestBookScreen.Landing at = this.landing(book, asked);
					Chapter c = at.chapter();
					keepOpen(at.ways());
					keepOpen(this.waysTo(book, this.pendingQuest));
					this.choose(c.id);
					QuestsConfig.get().lastChapter = c.id;
					this.rail.select(this.topId);
					this.tabs.inlineTabs(!this.inPlace());
					this.tabs.show(book.chapter(this.topId), this.shownId);
					if (this.pendingQuest == null) {
						this.intro.arrive(c);
					} else {
						this.intro.dismiss();
					}

					if (this.pendingQuest != null && this.sidePanel == null) {
						this.tree.cover(0, 0, this.sheet.openWidth(), 0);
					} else {
						this.recover();
					}

					this.tree.show(c, TreeView.Travel.ENTRANCE, null);
					if (this.pendingQuest == null && !at.ways().isEmpty()) {
						this.tree.focus(at.ways().get(at.ways().size() - 1));
					}

					BookExtensions.each(e -> e.chapterShown(this, c));
				}

				if (this.pendingQuest != null) {
					String key = this.pendingQuest;
					this.pendingQuest = null;
					if (this.tree.focus(key)) {
						this.openQuest(key);
					} else {
						this.recover();
					}
				}
			} else {
				this.tabs.inlineTabs(!this.inPlace());
				Chapter now = book.chapter(this.shownId);
				if (now != null && this.landing(book, now).chapter() != now) {
					this.present(now.id, TreeView.Travel.ENTRANCE, null, false);
				} else {
					this.tabs.refresh();
					this.recover();
				}
			}

			this.chrome();
			if (this.onReady != null) {
				Consumer<QuestBookScreen> ready = this.onReady;
				this.onReady = null;
				ready.accept(this);
			}
		}
	}

	QuestBookScreen onReady(Consumer<QuestBookScreen> then) {
		this.onReady = then;
		return this;
	}

	void hideIntro() {
		this.intro.dismiss();
		this.recover();
	}

	void demoSearch(String query) {
		if (this.slashFocusTarget != null) {
			this.setFocused(this.slashFocusTarget);
			this.slashFocusTarget.setFocused(true);
			this.slashFocusTarget.setValue(query);
		}
	}

	public void filter(TreeFilter f) {
		this.tree.filter(f);
		this.chrome();
	}

	private int covered() {
		return this.sidePanel != null ? this.sideRect.w() : this.sheet.cover();
	}

	private void recover() {
		this.tree.cover(0, 0, this.covered(), this.intro.cover());
	}

	private List<Component> keysHelp() {
		List<Component> out = new ArrayList<>();
		out.add(Fonts.heading(Component.translatable("cfhgui_quests.keys.title")));

		for (String k : new String[]{"drag", "wheel", "space", "tab", "enter", "twice", "fold", "back", "page", "slash"}) {
			out.add(Component.translatable("cfhgui_quests.keys." + k));
		}

		if (bothViews()) {
			out.add(Component.translatable("cfhgui_quests.keys.view"));
		}

		out.add(Component.translatable("cfhgui_quests.keys.esc"));
		return out;
	}

	private void chrome() {
		int waiting = BookStats.book().claimable();
		if (this.claimAll != null) {
			boolean narrow = this.claimAll.getWidth() < 60;
			this.claimAll.active = waiting > 0;
			String count = waiting > 99 ? "99+" : String.valueOf(waiting);
			this.claimAll
				.setMessage(
					narrow
						? Component.literal(waiting > 0 ? count : "")
						: (waiting > 0 ? Component.translatable("cfhgui_quests.claim_all.count", new Object[]{count}) : Component.translatable("cfhgui_quests.claim_all"))
				);
			Component what = waiting > 1
				? Component.translatable("cfhgui_quests.claim_all.tip.count", new Object[]{waiting})
				: Component.translatable(waiting == 1 ? "cfhgui_quests.claim_all.tip" : "cfhgui_quests.claim_all.none");
			this.claimAll.tip(narrow ? List.of(Fonts.heading(Component.translatable("cfhgui_quests.claim_all")), what) : List.of(what));
		}

		if (this.filterButton != null) {
			boolean narrow = this.filterButton.getWidth() < 60;
			Component shows = Component.translatable("cfhgui_quests.filter." + this.tree.filter().key());
			this.filterButton.setMessage((Component)(narrow ? Component.empty() : shows));
			this.filterButton.variant(this.tree.filter() == TreeFilter.ALL ? Variant.SECONDARY : Variant.PRIMARY);
			this.filterButton
				.tip(narrow ? List.of(Fonts.heading(shows), Component.translatable("cfhgui_quests.filter.tip")) : List.of(Component.translatable("cfhgui_quests.filter.tip")));
		}
	}

	private Layout layout() {
		return this.tree instanceof QuestCanvas ? Layout.CUSTOM : Layout.OVERHAUL;
	}

	private static boolean bothViews() {
		Set<Layout> available = MenuSlots.available("cfhgui_quests:book");
		return available.contains(Layout.CUSTOM) && available.contains(Layout.OVERHAUL);
	}

	public void switchView() {
		if (this.minecraft != null && this.hadBook && bothViews() && this.sidePanel == null) {
			if (this.switchingTo != null) {
				this.switchingTo = null;
				this.veil.set(0.0F, 260);
				MenuSlots.setLayoutOverride("cfhgui_quests:book", this.layout() == MenuSlots.globalLayout() ? null : this.layout());
				if (this.viewSwitch != null) {
					this.viewSwitch.lean(this.layout() == Layout.OVERHAUL);
				}

				CFHGuiSounds.tick();
			} else {
				Layout to = this.layout() == Layout.CUSTOM ? Layout.OVERHAUL : Layout.CUSTOM;
				MenuSlots.setLayoutOverride("cfhgui_quests:book", to == MenuSlots.globalLayout() ? null : to);
				TreeView next = this.trees.get(to);
				if (next == null) {
					Chapter here = this.shown();
					QuestsClient.target(here == null ? null : here.id, this.sheet.key());
					Screen made = MenuSlots.create("cfhgui_quests:book", this);
					QuestsClient.takeTarget();
					if (!(made instanceof QuestBookScreen other) || other.layout() != to) {
						if (made != null) {
							QuestsClient.target(here == null ? null : here.id, this.sheet.key());
							this.minecraft.setScreen(made);
							return;
						} else {
							return;
						}
					}

					next = this.wire(other.factory.create(this));
					this.trees.put(to, next);
				}

				CFHGuiSounds.tick();
				this.switchingTo = next;
				if (this.viewSwitch != null) {
					this.viewSwitch.lean(to == Layout.OVERHAUL);
				}

				this.veil.set(1.0F, 110);
			}
		}
	}

	private void change(TreeView next) {
		QuestBook book = QuestsState.book();
		TreeView old = this.tree;
		String open = this.sheet.key();
		String was = this.shownId;
		old.remember();
		next.filter(old.filter());
		this.tree = next;
		BookIndex.Entry e = QuestsState.entry(open);
		Chapter asked = book == null ? null : (e != null ? e.chapter() : book.chapter(this.shownId));
		QuestBookScreen.Landing at = asked == null ? null : this.landing(book, asked);
		if (at != null) {
			keepOpen(at.ways());
			this.choose(at.chapter().id);
			QuestsConfig.get().lastChapter = at.chapter().id;
		}

		this.rebuildWidgets();
		if (at != null) {
			Chapter c = at.chapter();
			this.rail.select(this.topId);
			this.tabs.show(book.chapter(this.topId), this.shownId);
			if (!c.id.equals(was)) {
				if (this.sheet.isOpen()) {
					this.intro.dismiss();
				} else {
					this.intro.arrive(c);
				}
			}

			this.recover();
			this.tree.show(c, TreeView.Travel.ENTRANCE, null);
			if (e != null) {
				if (this.tree.focus(open)) {
					this.tree.reveal(open);
				}
			} else if (!at.ways().isEmpty()) {
				this.tree.focus(at.ways().get(at.ways().size() - 1));
			}

			this.chrome();
			BookExtensions.each(x -> x.chapterShown(this, c));
		}
	}

	public void showChapter(String chapterId, TreeView.Travel how, @Nullable String through) {
		this.present(chapterId, how, through, false);
	}

	public void showChapter(String chapterId) {
		this.showChapter(chapterId, TreeView.Travel.ENTRANCE, null);
	}

	private void present(String chapterId, TreeView.Travel how, @Nullable String through, boolean keepSheet) {
		QuestBook book = QuestsState.book();
		Chapter asked = book == null ? null : book.chapter(chapterId);
		if (asked != null) {
			QuestBookScreen.Landing at = this.landing(book, asked);
			Chapter c = at.chapter();
			keepOpen(at.ways());
			keepOpen(this.waysTo(book, through));
			String way = at.ways().isEmpty() ? null : at.ways().get(at.ways().size() - 1);
			if (way != null && c.id.equals(this.shownId) && !this.tree.travelling() && this.tree.chapter() == c) {
				if (!keepSheet) {
					this.closeQuest();
				}

				for (String w : at.ways()) {
					this.tree.unfold(w, true);
				}

				this.tree.focus(way);
			} else {
				this.choose(c.id);
				QuestsConfig.get().lastChapter = c.id;
				if (!keepSheet) {
					this.closeQuest();
				}

				this.rail.select(this.topId);
				this.tabs.inlineTabs(!this.inPlace());
				this.tabs.show(book.chapter(this.topId), this.shownId);
				if (keepSheet && this.sheet.isOpen()) {
					this.intro.dismiss();
				} else {
					this.intro.arrive(c);
				}

				this.recover();
				this.tree.show(c, how, through);
				if (way != null && !keepSheet) {
					this.tree.focus(way);
				}

				this.chrome();
				BookExtensions.each(e -> e.chapterShown(this, c));
			}
		}
	}

	private void pickChapter(Chapter c) {
		this.showChapter(c.id, TreeView.Travel.ENTRANCE, null);
	}

	private void pickTree(Chapter c) {
		QuestBook book = QuestsState.book();
		Chapter from = this.shown();
		if (book != null && from != null && from != c) {
			String down = Trees.wayDown(book, from, c);
			if (down != null) {
				this.showChapter(c.id, TreeView.Travel.INTO, this.nearest(book, c, from, down));
			} else {
				String up = Trees.wayDown(book, c, from);
				this.showChapter(c.id, up != null ? TreeView.Travel.OUT : TreeView.Travel.ENTRANCE, up == null ? null : this.nearest(book, from, c, up));
			}
		}
	}

	private String nearest(QuestBook book, Chapter below, Chapter above, String otherwise) {
		Chapter parent = book.chapter(below.parent);
		if (this.inPlace() && parent != null && parent != above && Trees.host(book, parent) == above) {
			String way = Trees.gateway(parent, below);
			return way == null ? otherwise : way;
		} else {
			return otherwise;
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
				this.showChapter(sub.id, this.landing(book, e.chapter()).chapter().id.equals(this.shownId) ? TreeView.Travel.INTO : TreeView.Travel.ENTRANCE, questKey);
			}
		}
	}

	public boolean leaveTree() {
		QuestBook book = QuestsState.book();
		Chapter c = this.shown();
		if (book != null && c != null && c.isSubtree()) {
			Chapter parent = book.chapter(c.parent);
			if (parent == null) {
				return false;
			} else {
				String through = Trees.gateway(parent, c);
				this.showChapter(parent.id, through != null ? TreeView.Travel.OUT : TreeView.Travel.ENTRANCE, through);
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
			this.setFocused(this.tree.widget());
			QuestBookScreen.Landing at = this.landing(book, e.chapter());
			keepOpen(at.ways());
			if (!at.chapter().id.equals(this.shownId) || this.tree.travelling()) {
				this.showChapter(at.chapter().id, TreeView.Travel.ENTRANCE, null);
			}

			this.intro.dismiss();
			if (this.sidePanel == null) {
				this.tree.cover(0, 0, this.sheet.openWidth(), 0);
			}

			if (this.tree.focus(questKey)) {
				this.openQuest(questKey);
			} else if (!at.ways().isEmpty()) {
				int i = at.ways().size() - 1;

				while (i >= 0 && !this.tree.focus(at.ways().get(i))) {
					i--;
				}

				this.openQuest(questKey);
			} else {
				this.recover();
			}
		}
	}

	public void openQuest(String questKey) {
		if (QuestsState.entry(questKey) != null) {
			this.tree.select(questKey);
			this.intro.dismiss();
			if (this.sidePanel != null) {
				this.recover();
			} else {
				this.sheet.show(questKey);
				this.recover();
				this.tree.reveal(questKey);
			}
		}
	}

	public void closeQuest() {
		if (this.sheet.isOpen()) {
			this.sheet.close();
			this.recover();
			if (this.sheet.holds(this.getFocused())) {
				this.setFocused(this.tree.widget());
			}
		}
	}

	@Override
	public void closeSheet() {
		this.closeQuest();
	}

	private void toggleIntro() {
		this.intro.toggle(this.shown());
		this.recover();
	}

	@Override
	public void quest(String key) {
		this.goTo(key);
	}

	@Override
	public void url(String url) {
		this.sheet.url(url);
	}

	private void claimEverything() {
		QuestActions.claimAllAsking(
			(waiting, result) -> {
				if (result.ok()) {
					CFHGuiToasts.show(
						Component.translatable("cfhgui_quests.claim_all.done"),
						Component.translatable(waiting == 1 ? "cfhgui_quests.claim_all.done.one" : "cfhgui_quests.claim_all.done.many", new Object[]{waiting}),
						Icon.STAR_FILLED
					);
				} else {
					CFHGuiToasts.show(Component.translatable("cfhgui_quests.result.failed"), QuestActions.describe(result), Icon.WARNING);
				}
			}
		);
	}

	private void filterMenu() {
		if (this.filterButton != null) {
			int w = Math.max(120, this.filterButton.getWidth());
			Popups.open(
				new MenuPopup(Math.min(this.filterButton.getX(), this.width - w - 4), this.filterButton.getY() + this.filterButton.getHeight() + 2, this.filterItems(), w)
			);
		}
	}

	private List<Item> filterItems() {
		BookStats.Stat stat = BookStats.of(this.shown());
		Chapter c = this.shown();
		int all = c == null ? 0 : c.quests.size();
		List<Item> items = new ArrayList<>();

		for (TreeFilter f : TreeFilter.values()) {
			int n = switch (f) {
				case ALL -> all;
				case TODO -> stat.open();
				case REWARDS -> stat.claimable();
				case DONE -> stat.done();
			};
			items.add(Item.checked(Component.translatable("cfhgui_quests.filter." + f.key() + ".count", new Object[]{n}), this.tree.filter() == f, () -> {
				this.tree.filter(f);
				this.chrome();
			}));
		}

		return items;
	}

	private boolean context(@Nullable String questKey, double mouseX, double mouseY, float treeX, float treeY) {
		List<Item> items = new ArrayList<>();
		BookIndex.Entry e = QuestsState.entry(questKey);
		if (e == null) {
			items.add(Item.of(Component.translatable("cfhgui_quests.menu.fit"), Icon.FULLSCREEN, this.tree::fit));
			items.add(Item.sep());
			items.addAll(this.filterItems());
		} else {
			String key = e.key();
			Rules.Status status = QuestsState.status(key);
			Quest q = e.quest();
			boolean secret = status == Rules.Status.LOCKED && q.visibility == Quest.Visibility.SECRET;
			this.tree.select(key);
			items.add(Item.of(Component.translatable("cfhgui_quests.menu.open"), Icon.QUEST, () -> this.openQuest(key)));
			boolean pinned = QuestsState.pinned(key);
			if (pinned || !status.done() && !secret) {
				items.add(Item.of(Component.translatable(pinned ? "cfhgui_quests.unpin" : "cfhgui_quests.pin"), Icon.PIN, () -> QuestsState.pin(key, !pinned)));
			}

			if (this.opensInPlace(key)) {
				boolean unfolded = this.tree.unfolded(key);
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

				Chapter asked = book.chapter(this.shownId);
				if (asked != null) {
					QuestBookScreen.Landing at = this.landing(book, asked);
					if (at.chapter() != asked) {
						keepOpen(at.ways());
						this.choose(at.chapter().id);
					}
				}

				this.rail.refresh();
				this.rail.select(this.topId);
				this.tabs.inlineTabs(!this.inPlace());
				this.tabs.show(book.chapter(this.topId), this.shownId);
				Chapter c = book.chapter(this.shownId);
				if (c != null) {
					if (c.id.equals(was)) {
						this.tree.rebuild(c);
					} else {
						this.tree.show(c, TreeView.Travel.ENTRANCE, null);
					}
				}

				this.sheet.refresh();
				this.recover();
				this.chrome();
				BookExtensions.each(e -> e.chapterShown(this, c));
			}
		}
	}

	private void onProgressChanged(List<String> keys) {
		if (this.minecraft != null && this.shownOnce) {
			this.tree.refresh();
			this.rail.refresh();
			this.tabs.refresh();
			this.sheet.refresh();
			this.chrome();
		}
	}

	public void removed() {
		super.removed();
		this.tree.remember();
		QuestsConfig.save();
		if (this.listening) {
			this.listening = false;
			QuestsState.BOOK_CHANGED.unregister(this.bookListener);
			QuestsState.PROGRESS_CHANGED.unregister(this.progressListener);
		}

		if (this.sidePanel != null) {
			this.sidePanel.closed();
		}

		BookExtensions.each(e -> e.closed(this));
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.search.keyPressed(keyCode)) {
			return true;
		} else {
			GuiEventListener focus = this.getFocused();
			if (this.sidePanel != null && this.sideWidgets.contains(focus) && this.panelKey(this.sidePanel, keyCode, scanCode, modifiers)) {
				return true;
			} else {
				boolean var10000;
				label131: {
					if (focus instanceof EditBox box && box.isFocused()) {
						var10000 = true;
						break label131;
					}

					var10000 = false;
				}

				boolean typing = var10000;
				if (keyCode == 256 && typing && focus instanceof EditBox box) {
					if (!box.getValue().isEmpty()) {
						box.setValue("");
					} else {
						this.setFocused(this.hadBook ? this.tree.widget() : null);
					}

					return true;
				} else if (keyCode == 256 && this.sheet.isOpen()) {
					this.closeQuest();
					return true;
				} else {
					if (!typing && QuestsState.available()) {
						if (QuestsClient.BOOK_KEY.matches(keyCode, scanCode)) {
							this.onClose();
							return true;
						}

						if (BookExtensions.key(this, keyCode, scanCode, modifiers)) {
							return true;
						}

						boolean inPanel = this.sheet.isOpen() && this.sheet.holds(focus) || this.sideWidgets.contains(focus);
						if (keyCode == 259 && !inPanel && this.leaveTree()) {
							return true;
						}

						if (keyCode == 266 || keyCode == 267) {
							Chapter c = this.rail.neighbour(keyCode == 267 ? 1 : -1);
							if (c != null) {
								this.pickChapter(c);
							}

							return true;
						}

						if (keyCode == 70 && hasControlDown() && this.slashFocusTarget != null) {
							this.setFocused(this.slashFocusTarget);
							return true;
						}

						if (keyCode == 86 && (modifiers & 14) == 0 && this.sidePanel == null && bothViews()) {
							this.switchView();
							return true;
						}

						if (!inPanel && treeKey(keyCode) && this.tree.key(keyCode, scanCode, modifiers)) {
							return true;
						}

						if (!inPanel && keyCode == 258) {
							return true;
						}
					}

					return super.keyPressed(keyCode, scanCode, modifiers);
				}
			}
		}
	}

	private boolean panelKey(BookExtensions.SidePanel panel, int keyCode, int scanCode, int modifiers) {
		try {
			return panel.keyPressed(this, keyCode, scanCode, modifiers);
		} catch (Exception var6) {
			CFHGuiQuests.LOGGER.error("[CFHGui Quests] a side panel of the quest book threw on a key", var6);
			return false;
		}
	}

	private static boolean treeKey(int keyCode) {
		return switch (keyCode) {
			case 32, 45, 61, 257, 258, 262, 263, 264, 265, 268, 333, 334, 335 -> true;
			default -> false;
		};
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.search.mouseClicked(mouseX, mouseY, button)) {
			return true;
		} else {
			boolean introWas = this.intro.isOpen();
			boolean introTook = this.intro.mouseClicked(mouseX, mouseY, button);
			if (introWas != this.intro.isOpen()) {
				this.recover();
			}

			if (introTook) {
				return true;
			} else {
				boolean handled = super.mouseClicked(mouseX, mouseY, button);
				GuiEventListener focus = this.getFocused();
				if (QuestsState.available() && !(focus instanceof EditBox) && !this.sheet.holds(focus) && !this.sideWidgets.contains(focus)) {
					this.setFocused(this.tree.widget());
				}

				return handled;
			}
		}
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		return this.tree.dragging()
			? this.tree.widget().mouseDragged(mouseX, mouseY, button, dragX, dragY)
			: super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		boolean handled = super.mouseReleased(mouseX, mouseY, button);
		return this.tree.widget().mouseReleased(mouseX, mouseY, button) || handled;
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.search.mouseScrolled(mouseX, mouseY, scrollY)) {
			return true;
		} else {
			return this.intro.mouseScrolled(mouseX, mouseY, scrollY) ? true : super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}
	}

	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		if (this.switchingTo != null && this.veil.get() >= 0.99F) {
			TreeView next = this.switchingTo;
			this.switchingTo = null;
			this.change(next);
			this.veil.set(0.0F, 260);
		}

		boolean book = QuestsState.available() && this.hadBook;
		boolean scene = book && this.tree.fillsScreen();
		if (!scene) {
			this.renderBackground(g, mouseX, mouseY, partialTick);
		}

		if (book) {
			boolean over = this.pointerOnTree(mouseX, mouseY);
			this.tree.widget().render(g, over ? mouseX : -9999, over ? mouseY : -9999, partialTick);
			this.veil(g);
			if (!scene) {
				this.frame(g);
			}

			this.intro.render(g, mouseX, mouseY, this.tree.travelling());
			if (this.sidePanel != null) {
				this.sidePlate(g, mouseX, mouseY, partialTick);
			}
		}

		for (Renderable r : this.renderableList()) {
			if (r != this.tree.widget() && r != this.sheet) {
				r.render(g, mouseX, mouseY, partialTick);
			}
		}

		if (book && this.sidePanel == null) {
			this.sheet.render(g, mouseX, mouseY, partialTick);
		}

		if (this.showHeader) {
			this.renderHeader(g);
		}

		this.renderContent(g, mouseX, mouseY, partialTick);
	}

	private void veil(GuiGraphics g) {
		float a = this.veil.get();
		if (!(a <= 0.01F)) {
			Theme t = Theme.current();
			CanvasDraw.layer(g);
			g.fill(0, 0, this.width, this.height, Colors.scaleAlpha(t.isVanilla() ? -15724528 : t.bg() | 0xFF000000, a));
		}
	}

	private boolean pointerOnTree(int mouseX, int mouseY) {
		if (Popups.any() || this.switchingTo != null || this.search.contains((double)mouseX, (double)mouseY) || this.intro.contains((double)mouseX, (double)mouseY)) {
			return false;
		} else {
			return this.sidePanel != null && this.sideRect.contains((double)mouseX, (double)mouseY) ? false : !this.sheet.isMouseOver((double)mouseX, (double)mouseY);
		}
	}

	private void frame(GuiGraphics g) {
		Theme t = Theme.current();
		Rect r = this.treeRect;
		int c = t.isVanilla() ? -16777216 : t.palette().border();
		g.fill(r.x() - 1, r.y(), r.x(), r.bottom() + 1, c);
		g.fill(r.right(), r.y(), r.right() + 1, r.bottom() + 1, c);
		g.fill(r.x() - 1, r.bottom(), r.right() + 1, r.bottom() + 1, c);
		g.fill(r.x() - 1, r.y() - 22, r.x(), r.y(), c);
		g.fill(r.right(), r.y() - 22, r.right() + 1, r.y(), c);
		g.fill(r.x() - 1, r.y() - 22 - 1, r.right() + 1, r.y() - 22, c);
	}

	private void sidePlate(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		Theme t = Theme.current();
		Palette p = t.palette();
		Rect s = this.sideRect;
		CanvasDraw.layer(g);
		int slide = this.sidePanelOffset();
		if (slide != 0) {
			g.enableScissor(this.treeRect.x(), this.treeRect.y(), this.treeRect.right(), this.treeRect.bottom());
			g.pose().pushPose();
			g.pose().translate((float)slide, 0.0F, 0.0F);
		}

		if (t.isVanilla()) {
			g.fill(s.x(), s.y(), s.right(), s.bottom(), -15987700);
			CFHGuiDraw.vanillaDialog(g, s.x(), s.y(), s.w(), s.h(), 1.0F);
		} else {
			g.fill(s.x() - 2, s.y() + 3, s.x(), s.bottom(), p.shadow());
			CFHGuiDraw.popupPlate(g, s.x(), s.y(), s.w(), s.h(), p.bg2(), p.borderStrong(), 0.0F, 1.0F);
		}

		BookExtensions.SidePanel panel = this.sidePanel;
		if (panel != null) {
			try {
				panel.render(g, s, mouseX - slide, mouseY, partialTick);
			} catch (Exception var11) {
				CFHGuiQuests.LOGGER.error("[CFHGui Quests] a side panel of the quest book threw while drawing", var11);
			}
		}

		if (slide != 0) {
			g.pose().popPose();
			g.disableScissor();
		}
	}

	protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		QuestBook book = QuestsState.book();
		if (book != null && !book.chapters.isEmpty() && this.shownId != null) {
			this.search.render(g, mouseX, mouseY, this.width);
			if (this.pointerOnTree(mouseX, mouseY)) {
				this.tree.renderOver(g, mouseX, mouseY);
			}
		} else {
			this.nothing(g, book == null ? (this.inWorld() ? "no_server" : "no_world") : "empty");
		}
	}

	private void nothing(GuiGraphics g, String which) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		Rect r = this.contentRect();
		int cx = r.x() + r.w() / 2;
		int cy = r.y() + r.h() / 2;
		int w = Math.min(280, r.w() - 24);
		List<FormattedCharSequence> lines = this.font.split(Component.translatable("cfhgui_quests.state." + which + ".body"), w - 24);
		int h = 62 + lines.size() * 10;
		int x = cx - w / 2;
		int y = cy - h / 2;
		if (van) {
			CFHGuiDraw.vanillaDialog(g, x, y, w, h, 1.0F);
		} else {
			CFHGuiDraw.plate(g, x, y, w, h, p.surface(), p.border(), 0.5F, 1.0F);
		}

		if (!van) {
			CFHGuiDraw.pixelCircle(g, cx, y + 20, 11, Colors.withAlpha(p.accent(), 48));
		}

		Icons.draw(g, Icon.QUEST, cx - 8, y + 12, 16, van ? -1 : p.accentInk());
		CFHGuiDraw.textCentered(g, Fonts.heading(Component.translatable("cfhgui_quests.state." + which + ".title")), cx, y + 38, van ? -1 : p.text(), van);
		int ty = y + 52;

		for (FormattedCharSequence line : lines) {
			g.drawString(this.font, line, cx - this.font.width(line) / 2, ty, van ? -6250336 : p.textMuted(), van);
			ty += 10;
		}
	}

	private static record Landing(Chapter chapter, List<String> ways) {
	}

	@FunctionalInterface
	public interface TreeFactory {
		TreeView create(QuestBookScreen var1);
	}
}
