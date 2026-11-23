package com.onehumanawa.cfhcore.content.gui.quests.client.dev;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiList;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSegmented;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsClient;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.editor.QuestEditor;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.NodeView;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestCanvas;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CFHGuiQuestPainter;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.TreeView;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import com.onehumanawa.cfhcore.content.gui.quests.server.adv.AdvAccount;
import com.onehumanawa.cfhcore.content.gui.quests.server.adv.AdvOptions;
import com.onehumanawa.cfhcore.content.gui.quests.server.adv.AdvService;
import com.onehumanawa.cfhcore.content.gui.quests.server.adv.AdvTrees;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public final class AdvConvertScreen extends CFHGuiScreen {
	static final String PREVIEW = "~adv-preview";
	private static final int LINE = 20;
	private static final int GAP = 3;
	private static final int LABEL = 54;
	private static final int FOOT = 24;
	private static final int STRIP = 24;
	private static final int TREE_ROW = 16;
	private static final int ASK_AFTER_MS = 160;
	private final List<String> asked;
	private final List<AdvTrees.Tree> trees = new ArrayList<>();
	private int current;
	private boolean resolved;
	private final QuestCanvas canvas = new QuestCanvas(0, 0, 10, 10, new CFHGuiQuestPainter());
	@Nullable
	private AdvService.Preview preview;
	@Nullable
	private Component problem;
	private String shownRoot = "";
	private boolean dirty = true;
	private boolean converting;
	private long dirtyAtMs;
	private int askedNo;
	private int answeredNo;
	private Rect left = new Rect(0, 0, 0, 0);
	private Rect stage = new Rect(0, 0, 0, 0);
	private Rect strip = new Rect(0, 0, 0, 0);
	private Rect foot = new Rect(0, 0, 0, 0);
	@Nullable
	private CFHGuiButton convert;
	@Nullable
	private CFHGuiSegmented<String> exists;
	@Nullable
	private CFHGuiTextField idField;
	private boolean groupChosen;
	private static final int PARTLY = -2055080;
	private static final int KEPT = -9459492;
	private static final int HIDDEN = -4606808;

	public AdvConvertScreen(@Nullable Screen parent, List<String> roots) {
		super(Component.translatable("cfhgui_quests.adv.title"), parent);
		this.asked = new ArrayList<>(roots);
		this.canvas.onOpen(key -> {
			this.canvas.select(key);
			CFHGuiSounds.tick();
		});
	}

	private void resolve() {
		if (!this.resolved) {
			AdvService.Trees all = AdvClient.trees();
			if (all == null) {
				AdvClient.want();
			} else {
				this.resolved = true;

				for (String word : this.asked) {
					for (AdvTrees.Tree t : all.trees) {
						if ((word.equals("*") || t.root.equals(word) || t.namespace.equals(word)) && !this.trees.contains(t)) {
							this.trees.add(t);
						}
					}
				}

				if (!this.trees.isEmpty()) {
					if (!this.groupChosen) {
						AdvOptions o = AdvClient.options();
						AdvOptions before = null;

						for (AdvTrees.Tree tx : this.trees) {
							if (before == null && tx.options != null) {
								before = tx.options;
							}
						}

						if (before != null) {
							o.tasks = before.tasks;
							o.order = before.order;
							o.kind = before.kind;
							o.rewards = before.rewards;
							o.keys = before.keys;
							o.spacing = before.spacing;
							o.group = before.group;
							o.groupTitle = "";
						} else if (o.group.isEmpty()) {
							o.group = this.trees.get(0).namespace;
							o.groupTitle = this.trees.get(0).mod;
						}

						this.groupChosen = true;
					}
				}
			}
		}
	}

	@Nullable
	private AdvTrees.Tree tree() {
		return this.trees.isEmpty() ? null : this.trees.get(Math.min(this.current, this.trees.size() - 1));
	}

	private Component titleOf(AdvTrees.Tree t) {
		return AdvText.component(t.title, t.root);
	}

	private AdvOptions.Name name(AdvTrees.Tree t) {
		return AdvClient.options().names.computeIfAbsent(t.root, k -> new AdvOptions.Name());
	}

	protected void build() {
		this.resolve();
		Rect area = this.contentRect();
		int lw = Math.max(150, Math.min(210, area.w() * 42 / 100));
		this.left = new Rect(area.x(), area.y(), lw, area.h() - 24);
		int sx = this.left.right() + 8;
		this.stage = new Rect(sx, area.y(), area.right() - sx, area.h() - 24 - 24);
		this.strip = new Rect(sx, this.stage.bottom() + 2, this.stage.w(), 22);
		this.foot = new Rect(area.x(), area.bottom() - 20, AdvTreesPanel.clearRight(this, area.right(), area.bottom()) - area.x(), 20);
		this.convert = null;
		this.exists = null;
		this.idField = null;
		AdvTrees.Tree tree = this.tree();
		if (tree != null) {
			AdvOptions o = AdvClient.options().sanitize();
			int y = this.left.y();
			if (this.trees.size() > 1) {
				int rows = Math.min(this.trees.size(), 3);
				CFHGuiList<AdvTrees.Tree> list = new CFHGuiList<AdvTrees.Tree>(this.left.x(), y, this.left.w(), rows * 17 - 1, 16, this::treeRow);
				list.gap(1);
				list.items(this.trees);
				list.select(this.current);
				list.onSelect(t -> this.pick(this.trees.indexOf(t)));
				this.add(list);
				y += list.getHeight() + 6;
			}

			CFHGuiScrollPanel panel = new CFHGuiScrollPanel(this.left.x(), y, this.left.w(), this.left.bottom() - y);
			int inner = panel.innerWidth();
			boolean stacked = inner - 54 < 118;
			int cw = stacked ? inner : inner - 54;
			int py = 0;
			py = this.caption(panel, py, "chapter", inner);
			CFHGuiTextField id = new CFHGuiTextField(0, 0, cw, Component.translatable("cfhgui_quests.adv.opt.id"));
			id.placeholder(Component.literal(tree.chapter)).maxLength(64).text(this.name(tree).id);
			id.setInvalid(!this.name(tree).id.isEmpty() && !Ids.valid(this.name(tree).id));
			id.onChange(s -> {
				this.name(tree).id = s.trim();
				id.setInvalid(!s.isBlank() && !Ids.valid(s.trim()));
				this.changed();
			});
			id.setTooltip(Tooltip.create(Component.translatable("cfhgui_quests.adv.opt.id.tip")));
			this.idField = id;
			py = this.row(panel, py, "id", id, stacked, inner);
			CFHGuiTextField title = new CFHGuiTextField(0, 0, cw, Component.translatable("cfhgui_quests.adv.opt.name"));
			title.placeholder(this.titleOf(tree)).maxLength(128).text(this.name(tree).title);
			title.onChange(s -> {
				this.name(tree).title = s.trim();
				this.changed();
			});
			title.setTooltip(Tooltip.create(Component.translatable("cfhgui_quests.adv.opt.name.tip")));
			py = this.row(panel, py, "name", title, stacked, inner);
			List<AdvConvertScreen.Group> groups = this.groups(o);
			AdvConvertScreen.Group chosen = groups.get(0);

			for (AdvConvertScreen.Group g : groups) {
				if (g.id().equals(o.group)) {
					chosen = g;
				}
			}

			CFHGuiDropdown<AdvConvertScreen.Group> group = new CFHGuiDropdown<>(0, 0, cw, groups, chosen, AdvConvertScreen::groupLabel, gx -> {
				o.group = gx.id();
				o.groupTitle = gx.fresh() ? gx.title() : "";
				this.changed();
			});
			group.tip(Component.translatable("cfhgui_quests.adv.opt.group.tip"));
			py = this.row(panel, py, "group", group, stacked, inner);
			CFHGuiSegmented<String> there = this.segmented(cw, List.of("merge", "replace", "copy"), o.exists, "exists", v -> o.exists = v);
			this.exists = there;
			py = this.row(panel, py, "exists", there, stacked, inner);
			py = this.caption(panel, py + 4, "quests", inner);
			py = this.row(panel, py, "tasks", this.segmented(cw, List.of("translate", "advancement"), o.tasks, "tasks", v -> o.tasks = v), stacked, inner);
			py = this.row(panel, py, "order", this.segmented(cw, List.of("tree", "free"), o.order, "order", v -> o.order = v), stacked, inner);
			py = this.row(panel, py, "kind", this.segmented(cw, List.of("quest", "advancement"), o.kind, "kind", v -> o.kind = v), stacked, inner);
			py = this.row(
				panel, py, "names", this.segmented(cw, List.of("keys", "words"), o.keys ? "keys" : "words", "names", v -> o.keys = v.equals("keys")), stacked, inner
			);
			CFHGuiSegmented<Float> spacing = new CFHGuiSegmented<>(
				0,
				0,
				cw,
				List.of(1.0F, 1.5F, 2.0F),
				nearest(o.spacing),
				v -> Component.literal((double)v.floatValue() == Math.rint((double)v.floatValue()) ? Integer.toString(Math.round(v)) : Float.toString(v)),
				v -> {
					o.spacing = v;
					this.changed();
				}
			);
			spacing.tip(Component.translatable("cfhgui_quests.adv.opt.spacing.tip"));
			py = this.row(panel, py, "spacing", spacing, stacked, inner);
			CFHGuiToggle rewards = new CFHGuiToggle(0, 0, Math.min(cw, 60), Component.empty(), o.rewards, v -> {
				o.rewards = v;
				this.changed();
			});
			rewards.tip(Component.translatable("cfhgui_quests.adv.opt.rewards.tip"));
			py = this.row(panel, py, "rewards", rewards, stacked, inner);
			panel.setContentHeight(py + 2);
			this.add(panel);
			this.canvas.place(this.stage.x(), this.stage.y(), this.stage.w(), this.stage.h());
			this.add(this.canvas);
			int n = this.trees.size();
			Component label = Component.translatable(n == 1 ? "cfhgui_quests.adv.convert.one" : "cfhgui_quests.adv.convert", new Object[]{n});
			CFHGuiButton c = new CFHGuiButton(0, this.foot.y(), 20, 20, label, this::convert).variant(Variant.PRIMARY).icon(Icon.QUEST);
			c.setWidth(c.preferredWidth() + 8);
			c.setX(this.foot.right() - c.getWidth());
			this.add(c);
			this.convert = c;
			this.state();
		}
	}

	private static float nearest(float spacing) {
		return spacing <= 1.2F ? 1.0F : (spacing >= 1.8F ? 2.0F : 1.5F);
	}

	private int caption(CFHGuiScrollPanel panel, int y, String key, int width) {
		panel.add(new CFHGuiLabel(0, 0, width, Fonts.heading(Component.translatable("cfhgui_quests.adv.section." + key))).style(Style.CAPTION), 0, y);
		return y + 12;
	}

	private int row(CFHGuiScrollPanel panel, int y, String key, AbstractWidget control, boolean stacked, int inner) {
		CFHGuiLabel label = new CFHGuiLabel(0, 0, stacked ? inner : 50, Component.translatable("cfhgui_quests.adv.opt." + key)).style(Style.MUTED);
		if (stacked) {
			panel.add(label, 0, y);
			panel.add(control, 0, y + 10);
			return y + 10 + control.getHeight() + 3 + 1;
		} else {
			panel.add(label, 0, y + (20 - label.getHeight()) / 2);
			panel.add(control, 54 + (inner - 54 - control.getWidth()), y + (20 - control.getHeight()) / 2);
			return y + 20 + 3;
		}
	}

	private CFHGuiSegmented<String> segmented(int width, List<String> values, String value, String key, Consumer<String> set) {
		Function<String, Component> label = v -> Component.translatable("cfhgui_quests.adv.opt." + key + "." + v);
		CFHGuiSegmented<String> s = new CFHGuiSegmented<>(0, 0, width, values, value, label, v -> {
			set.accept(v);
			this.changed();
		});
		s.optionTip(v -> Component.translatable("cfhgui_quests.adv.opt." + key + "." + v + ".tip"));
		return s;
	}

	private List<AdvConvertScreen.Group> groups(AdvOptions o) {
		List<AdvConvertScreen.Group> out = new ArrayList<>();
		out.add(new AdvConvertScreen.Group("", "", false));
		AdvService.Trees all = AdvClient.trees();
		boolean has = o.group.isEmpty();
		if (all != null) {
			for (Entry<String, String> g : all.groups.entrySet()) {
				out.add(new AdvConvertScreen.Group(g.getKey(), g.getValue(), false));
				if (g.getKey().equals(o.group)) {
					has = true;
				}
			}
		}

		for (AdvTrees.Tree t : this.trees) {
			boolean known = false;

			for (AdvConvertScreen.Group gx : out) {
				if (gx.id().equals(t.namespace)) {
					known = true;
				}
			}

			if (!known) {
				out.add(new AdvConvertScreen.Group(t.namespace, t.mod, true));
			}

			if (t.namespace.equals(o.group)) {
				has = true;
			}
		}

		if (!has) {
			out.add(new AdvConvertScreen.Group(o.group, o.groupTitle.isEmpty() ? o.group : o.groupTitle, true));
		}

		return out;
	}

	private static Component groupLabel(AdvConvertScreen.Group g) {
		if (g.id().isEmpty()) {
			return Component.translatable("cfhgui_quests.adv.opt.group.none");
		} else {
			return g.fresh() ? Component.translatable("cfhgui_quests.adv.opt.group.new", new Object[]{g.title()}) : QuestText.text(g.title());
		}
	}

	private void treeRow(GuiGraphics g, AdvTrees.Tree t, int index, int x, int y, int w, int h, boolean hovered, boolean selected, int mouseX, int mouseY) {
		Palette p = Theme.current().palette();
		boolean van = Theme.current().isVanilla();
		QuestIcons.of(t.icon.isEmpty() ? "icon:trophy" : t.icon).draw(g, (float)x + 11.0F, (float)y + (float)h / 2.0F, 12.0F, van ? -1 : p.textMuted());
		int ink = van ? -1 : (selected ? p.text() : p.textMuted());
		g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(this.titleOf(t), w - 26), x + 21, CFHGuiDraw.textY(y, h), ink, van);
	}

	private void pick(int index) {
		if (index >= 0 && index < this.trees.size() && index != this.current) {
			this.current = index;
			this.preview = null;
			this.problem = null;
			this.dirty = true;
			this.dirtyAtMs = 0L;
			this.rebuildWidgets();
		}
	}

	private void changed() {
		this.dirty = true;
		this.dirtyAtMs = Clock.nowMs();
		this.state();
	}

	public void tick() {
		super.tick();
		if (!this.resolved) {
			this.resolve();
			if (this.resolved) {
				this.rebuildWidgets();
			}
		} else {
			AdvTrees.Tree tree = this.tree();
			if (tree != null && !this.converting) {
				if (this.dirty && Clock.nowMs() - this.dirtyAtMs >= 160L && AdvClient.state() == AdvClient.State.READY) {
					if (this.idValid()) {
						this.dirty = false;
						int no = ++this.askedNo;
						String root = tree.root;
						AdvClient.preview(root, AdvClient.options(), (p, why) -> {
							if (no == this.askedNo && this.tree() != null && this.tree().root.equals(root)) {
								this.answeredNo = no;
								this.preview = p;
								this.problem = why;
								if (p != null) {
									this.show(root, p);
								}

								this.state();
							}
						});
					}
				}
			}
		}
	}

	private boolean idValid() {
		for (AdvTrees.Tree t : this.trees) {
			AdvOptions.Name n = AdvClient.options().names.get(t.root);
			if (n != null && !n.id.isEmpty() && !Ids.valid(n.id)) {
				return false;
			}
		}

		return true;
	}

	private void show(String root, AdvService.Preview p) {
		QuestBook book = QuestsState.book();
		if (book != null) {
			Chapter c = BookJson.copy(p.chapter);
			c.id = "~adv-preview";
			c.unlisted = true;
			c.hidden = false;
			c.parent = "";
			c.focus = "";
			c.requires.clear();

			for (Quest q : c.quests) {
				q.looseDeps = true;
				q.visibility = Quest.Visibility.VISIBLE;
				q.subtree = "";
			}

			book.chapters.put("~adv-preview", c);
			QuestsState.reindex();
			if (root.equals(this.shownRoot)) {
				this.canvas.rebuild(c);
			} else {
				this.shownRoot = root;
				this.canvas.show(c, TreeView.Travel.ENTRANCE, null);
				this.canvas.lookAfresh();
				this.canvas.fit();
			}
		}
	}

	private static void clear() {
		QuestBook book = QuestsState.book();
		if (book != null && book.chapters.remove("~adv-preview") != null) {
			QuestsState.reindex();
		}
	}

	public void removed() {
		super.removed();
		clear();
		this.shownRoot = "";
		this.dirty = true;
		this.dirtyAtMs = 0L;
	}

	private void state() {
		boolean ready = AdvClient.state() == AdvClient.State.READY && !this.trees.isEmpty();
		if (this.convert != null) {
			this.convert.active = ready && !this.converting && this.idValid() && this.problem == null;
			this.convert
				.tip(
					!this.idValid()
						? Component.translatable("cfhgui_quests.adv.result.bad_id", new Object[]{this.idField == null ? "" : this.idField.getValue()})
						: Component.translatable("cfhgui_quests.adv.convert.tip")
				);
		}

		if (this.exists != null) {
			boolean there = this.preview != null && this.preview.exists;

			for (AdvTrees.Tree t : this.trees) {
				if (t.exists && this.name(t).id.isEmpty()) {
					there = true;
				}
			}

			this.exists.active = there;
			this.exists.tip(there ? Component.translatable("cfhgui_quests.adv.opt.exists.tip") : Component.translatable("cfhgui_quests.adv.opt.exists.none"));
		}
	}

	private void convert() {
		if (!this.converting && !this.trees.isEmpty() && this.idValid()) {
			this.converting = true;
			this.state();
			List<String> roots = new ArrayList<>();

			for (AdvTrees.Tree t : this.trees) {
				roots.add(t.root);
			}

			AdvOptions o = AdvClient.options();
			o.names.values().removeIf(n -> n.id.isEmpty() && n.title.isEmpty());
			AdvClient.convert(
				roots,
				o,
				(done, why) -> {
					this.converting = false;
					if (done != null && !done.accounts.isEmpty()) {
						int quests = 0;
						int kept = 0;

						for (AdvAccount a : done.accounts) {
							quests += a.quests;
							kept += a.kept;
						}

						CFHGuiToasts.show(
							Component.translatable(done.accounts.size() == 1 ? "cfhgui_quests.adv.done.one" : "cfhgui_quests.adv.done", new Object[]{done.accounts.size(), quests}),
							Component.translatable(kept == 0 ? "cfhgui_quests.adv.done.body.none" : "cfhgui_quests.adv.done.body", new Object[]{kept}),
							Icon.QUEST
						);
						String chapter = done.accounts.get(0).chapter;
						clear();
						if (QuestEditor.available()) {
							QuestEditor.open(this.origin(), chapter);
						} else {
							Minecraft.getInstance().setScreen(this.origin());
							QuestsClient.open(chapter, null);
						}
					} else {
						CFHGuiToasts.show(
							Component.translatable("cfhgui_quests.adv.failed"), (Component)(why == null ? Component.translatable("cfhgui_quests.result.failed") : why), Icon.WARNING
						);
						this.state();
					}
				}
			);
		}
	}

	@Nullable
	private Screen origin() {
		return this.parent instanceof AdvTreesScreen list ? list.origin() : this.parent;
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		boolean ctrl = (modifiers & (Minecraft.ON_OSX ? 8 : 2)) != 0;
		if ((keyCode == 257 || keyCode == 335) && ctrl && this.convert != null && this.convert.active) {
			this.convert();
			return true;
		} else {
			GuiEventListener focus = this.getFocused();
			return (focus == null || focus == this.canvas) && !this.trees.isEmpty() && this.canvas.key(keyCode, scanCode, modifiers)
				? true
				: super.keyPressed(keyCode, scanCode, modifiers);
		}
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		return this.canvas.dragging() ? this.canvas.mouseDragged(mouseX, mouseY, button, dragX, dragY) : super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		boolean was = this.canvas.dragging();
		boolean handled = super.mouseReleased(mouseX, mouseY, button);
		if (was && this.canvas.dragging()) {
			this.canvas.mouseReleased(mouseX, mouseY, button);
		}

		return handled;
	}

	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(g, mouseX, mouseY, partialTick);
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		AdvTrees.Tree tree = this.tree();
		if (tree == null) {
			this.none(g, van, p);

			for (Renderable r : this.renderableList()) {
				r.render(g, mouseX, mouseY, partialTick);
			}

			if (this.showHeader) {
				this.renderHeader(g);
			}
		} else {
			if (van) {
				CFHGuiDraw.outline(g, this.stage.x() - 1, this.stage.y() - 1, this.stage.w() + 2, this.stage.h() + 2, 1627389951, 0);
			} else {
				CFHGuiDraw.outline(g, this.stage.x() - 1, this.stage.y() - 1, this.stage.w() + 2, this.stage.h() + 2, p.border(), t.radius());
			}

			for (Renderable r : this.renderableList()) {
				r.render(g, mouseX, mouseY, partialTick);
			}

			this.marks(g, van, p);
			this.over(g, van, p);
			this.account(g, mouseX, mouseY, van, p);
			if (this.stage.contains((double)mouseX, (double)mouseY) && this.preview != null) {
				this.canvas.renderOver(g, mouseX, mouseY);
			}

			if (this.showHeader) {
				this.renderHeader(g);
			}
		}
	}

	private void none(GuiGraphics g, boolean van, Palette p) {
		Rect area = this.contentRect();
		int cx = area.centerX();
		int cy = area.centerY() - 10;
		AdvClient.State state = AdvClient.state();
		if (this.resolved || state != AdvClient.State.LOADING && state != AdvClient.State.READY) {
			String key = switch (state) {
				case NO_WORLD -> "no_world";
				case NO_SERVER -> "no_server";
				case NO_PERMISSION -> "no_permission";
				case FAILED -> "failed";
				default -> "no_tree";
			};
			Icons.draw(g, Icon.TROPHY, cx - 8, cy - 8, 16, van ? -1 : p.textDim());
			CFHGuiDraw.textCentered(g, Fonts.heading(Component.translatable("cfhgui_quests.adv.state." + key)), cx, cy + 14, van ? -1 : p.text(), van);
			CFHGuiDraw.textCentered(g, Component.translatable("cfhgui_quests.adv.state." + key + ".body"), cx, cy + 28, van ? -6250336 : p.textMuted(), van);
		} else {
			CFHGuiSpinner.draw(g, cx - 6, cy - 6, 12, van ? -1 : p.accentInk());
			CFHGuiDraw.textCentered(g, Component.translatable("cfhgui_quests.adv.state.loading"), cx, cy + 14, van ? -6250336 : p.textMuted(), van);
		}
	}

	private void marks(GuiGraphics g, boolean van, Palette p) {
		AdvService.Preview pv = this.preview;
		if (pv != null && !this.canvas.graph().isEmpty() && "~adv-preview".equals(this.canvas.graph().chapter.id)) {
			g.enableScissor(this.stage.x(), this.stage.y(), this.stage.right(), this.stage.bottom());

			for (NodeView n : this.canvas.graph().nodes) {
				String mark = pv.account.marks.get(n.quest.id);
				Quest was = pv.chapter.quest(n.quest.id);
				boolean hidden = was != null && (was.visibility == Quest.Visibility.HIDDEN || was.visibility == Quest.Visibility.INVISIBLE);
				if (mark != null || hidden) {
					float r = this.canvas.radiusOf(n);
					if (!(r < 2.2F)) {
						float sx = this.canvas.sx(n.x);
						float sy = this.canvas.sy(n.y);
						if (!(sx < (float)this.stage.x() - r)
							&& !(sx > (float)this.stage.right() + r)
							&& !(sy < (float)this.stage.y() - r)
							&& !(sy > (float)this.stage.bottom() + r)) {
							int d = Math.max(2, Math.min(6, Math.round(r * 0.42F)));
							if (mark != null) {
								dot(g, Math.round(sx + r * 0.72F), Math.round(sy - r * 0.72F), d, "kept".equals(mark) ? -9459492 : -2055080, "partly".equals(mark));
							}

							if (hidden) {
								dot(g, Math.round(sx - r * 0.72F), Math.round(sy - r * 0.72F), d, -4606808, false);
							}
						}
					}
				}
			}

			g.disableScissor();
		}
	}

	private static void dot(GuiGraphics g, int cx, int cy, int d, int color, boolean half) {
		CFHGuiDraw.pixelCircle(g, cx, cy, d / 2 + 1, -535818224);
		CFHGuiDraw.pixelCircle(g, cx, cy, d / 2, color);
		if (half) {
			g.fill(cx, cy - d / 2, cx + d / 2 + 1, cy + d / 2 + 1, -535818224);
		}
	}

	private void over(GuiGraphics g, boolean van, Palette p) {
		boolean waiting = this.preview == null && this.problem == null;
		if (waiting || this.problem != null || this.askedNo != this.answeredNo || this.converting) {
			int cx = this.stage.centerX();
			int cy = this.stage.centerY();
			if (this.problem != null) {
				g.fill(this.stage.x(), this.stage.y(), this.stage.right(), this.stage.bottom(), -1342177280);
				Icons.draw(g, Icon.WARNING, cx - 8, cy - 16, 16, van ? -171 : p.warning());
				int y = cy + 4;

				for (FormattedCharSequence line : this.font.split(this.problem, Math.max(60, this.stage.w() - 40))) {
					g.drawString(this.font, line, cx - this.font.width(line) / 2, y, van ? -1 : p.text(), van);
					y += 10;
				}
			} else {
				if (waiting) {
					g.fill(this.stage.x(), this.stage.y(), this.stage.right(), this.stage.bottom(), van ? -1879048192 : Colors.withAlpha(p.bg(), 192));
				}

				if (waiting) {
					CFHGuiSpinner.draw(g, cx - 6, cy - 6, 12, van ? -1 : p.accentInk());
				} else {
					CFHGuiSpinner.draw(g, this.stage.x() + 6, this.stage.y() + 6, 8, van ? -1 : p.accentInk());
				}
			}
		}
	}

	private void account(GuiGraphics g, int mouseX, int mouseY, boolean van, Palette p) {
		AdvService.Preview pv = this.preview;
		if (pv != null) {
			AdvAccount a = pv.account;
			int muted = van ? -6250336 : p.textMuted();
			int ink = van ? -1 : p.text();
			int x = this.strip.x() + 1;
			int y = this.strip.y() + 1;
			x = this.count(
				g, x, y, Component.translatable(a.quests == 1 ? "cfhgui_quests.adv.acc.quest" : "cfhgui_quests.adv.acc.quests", new Object[]{a.quests}), 0, false, ink, van
			);
			if (a.translated > 0) {
				x = this.count(g, x, y, Component.translatable("cfhgui_quests.adv.acc.translated", new Object[]{a.translated}), 0, false, muted, van);
			}

			if (a.partly > 0) {
				x = this.count(g, x, y, Component.translatable("cfhgui_quests.adv.acc.partly", new Object[]{a.partly}), -2055080, true, muted, van);
			}

			if (a.kept > 0) {
				x = this.count(g, x, y, Component.translatable("cfhgui_quests.adv.acc.kept", new Object[]{a.kept}), -9459492, false, muted, van);
			}

			if (a.hidden > 0) {
				x = this.count(g, x, y, Component.translatable("cfhgui_quests.adv.acc.hidden", new Object[]{a.hidden}), -4606808, false, muted, van);
			}

			if (a.rewards > 0 && x < this.strip.right() - 50) {
				this.count(g, x, y, Component.translatable("cfhgui_quests.adv.acc.rewards", new Object[]{a.rewards}), 0, false, muted, van);
			}

			NodeView at = this.stage.contains((double)mouseX, (double)mouseY) ? this.canvas.nodeAt((double)mouseX, (double)mouseY) : null;
			String key = at != null ? at.key : this.canvas.selected();
			Quest q = key == null ? null : pv.chapter.quest(Ids.questOf(key));
			Component line = q == null ? this.chapterLine(pv) : this.questLine(pv, q);
			g.drawString(this.font, CFHGuiDraw.truncate(line, this.strip.w() - 2), this.strip.x() + 1, y + 11, q == null ? muted : ink, van);
		}
	}

	private int count(GuiGraphics g, int x, int y, Component words, int mark, boolean half, int ink, boolean van) {
		int at = x;
		if (x + this.font.width(words) + (mark != 0 ? 9 : 0) > this.strip.right()) {
			return x;
		} else {
			if (mark != 0) {
				dot(g, x + 3, y + 4, 5, mark, half);
				at = x + 9;
			}

			g.drawString(this.font, words, at, y, ink, van);
			return at + this.font.width(words) + 9;
		}
	}

	private Component chapterLine(AdvService.Preview pv) {
		AdvAccount a = pv.account;
		return "merged".equals(a.mode)
			? Component.translatable("cfhgui_quests.adv.mode.merged", new Object[]{a.chapter, a.added, a.updated, a.edited, a.removed})
			: Component.translatable("cfhgui_quests.adv.mode." + a.mode, new Object[]{a.chapter});
	}

	private Component questLine(AdvService.Preview pv, Quest q) {
		MutableComponent line = Component.empty().append(QuestText.title(q)).append(": ");
		String mark = pv.account.marks.get(q.id);
		if ("kept".equals(mark)) {
			String why = pv.account.why.getOrDefault(q.id, "trigger");
			return line.append(Component.translatable("cfhgui_quests.adv.why." + why));
		} else if (q.tasks.isEmpty()) {
			return line.append(Component.translatable("cfhgui_quests.adv.tasks.none"));
		} else {
			int own = 0;

			for (Task task : q.tasks) {
				if (!task.is("advancement")) {
					own++;
				}
			}

			Component first = QuestText.label(q.tasks.get(0));
			if ("partly".equals(mark)) {
				return line.append(Component.translatable("cfhgui_quests.adv.tasks.partly", new Object[]{own, q.tasks.size() - own}));
			} else {
				return q.tasks.size() == 1
					? line.append(first)
					: line.append(Component.translatable(q.anyTask ? "cfhgui_quests.adv.tasks.any" : "cfhgui_quests.adv.tasks.all", new Object[]{q.tasks.size(), first}));
			}
		}
	}

	public boolean previewShown() {
		return this.preview != null && this.askedNo == this.answeredNo;
	}

	private static record Group(String id, String title, boolean fresh) {
	}
}
