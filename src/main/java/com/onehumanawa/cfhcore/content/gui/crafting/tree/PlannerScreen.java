package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.emi.EmiRenderHelper;
import com.onehumanawa.cfhcore.content.gui.emi.EmiUtil;
import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStackInteraction;
import com.onehumanawa.cfhcore.content.gui.emi.bom.BoM;
import com.onehumanawa.cfhcore.content.gui.emi.config.EmiConfig;
import com.onehumanawa.cfhcore.content.gui.emi.input.EmiBind;
import com.onehumanawa.cfhcore.content.gui.emi.input.EmiInput;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiHistory;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiReloadManager;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.emi.screen.RecipeScreen;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
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
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.crafting.team.client.TeamPinsClient;
import com.onehumanawa.cfhcore.content.gui.crafting.team.client.TeamTrees;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public abstract class PlannerScreen extends CFHGuiScreen implements TreeView.Host {
	public AbstractContainerScreen<?> old;
	private static final int GAP = 6;
	private static final int CELL = 20;
	private static final int COST_MIN = 44;
	private static final int COST_MAX = 64;
	private static final int PANEL_MIN = 156;
	private static final int PANEL_MAX = 250;
	private final TreeView view = new TreeView(this);
	private final TreeGraph graph = new TreeGraph(this);
	private PlannerViewSwitch viewSwitch;
	private final QuickAccess quick = new QuickAccess();
	private final Anim entrance = new Anim(0.0F, 240, Ease.OUT_CUBIC);
	private PlannerPanel panel;
	private CFHGuiIconButton lessButton;
	private CFHGuiIconButton moreButton;
	private CFHGuiIconButton keepButton;
	private CFHGuiIconButton foldButton;
	private CFHGuiIconButton unfoldAllButton;
	private CFHGuiIconButton foldAllButton;
	private CFHGuiIconButton panelButton;
	private CFHGuiIconButton menuButton;
	@Nullable
	private CFHGuiIconButton teamButton;
	private CFHGuiButton countButton;
	private CFHGuiTextField nameField;
	private boolean naming;
	private Rect area = Rect.of(0, 0, 0, 0);
	private Rect treeRect = this.area;
	private Rect costRect = this.area;
	private Rect panelRect = this.area;
	private int leftW;
	private int titleRight;
	private int costScroll;
	private final List<PlannerScreen.Cell> cells = new ArrayList<>();
	private int costContent;
	@Nullable
	private EmiStackInteraction hoveredStack;
	@Nullable
	private List<ClientTooltipComponent> tooltip;

	protected PlannerScreen(@Nullable AbstractContainerScreen<?> old) {
		super(Component.translatable("cfhgui_crafting.planner.title"), old);
		this.old = old;
		this.entrance.snap(0.0F);
		this.entrance.set(1.0F);
	}

	TreeView view() {
		return this.view;
	}

	TreeGraph graph() {
		return this.graph;
	}

	static boolean graphMode() {
		return TreeConfig.get().plannerGraph;
	}

	void graph(boolean on) {
		if (graphMode() != on) {
			TreeConfig.get().plannerGraph = on;
			TreeConfig.save();
			if (this.viewSwitch != null) {
				this.viewSwitch.show(on);
			}

			CFHGuiSounds.click();
			TreeSession s = Trees.active();
			if (s != null && s.selected != null) {
				if (on) {
					this.graph.reveal(s.selected);
				} else {
					this.view.reveal(s.selected);
				}
			}
		}
	}

	void rename() {
		if (Trees.active() != null) {
			this.naming(true, false);
		}
	}

	void panel(boolean open) {
		if (TreeConfig.get().plannerPanel != open) {
			TreeConfig.get().plannerPanel = open;
			TreeConfig.save();
			this.rebuildWidgets();
		}
	}

	protected void build() {
		RecipeScreen.resolve = null;
		this.menuButton = (CFHGuiIconButton)this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.DOTS, Component.translatable("cfhgui_crafting.tree.more"), () -> {
			TreeSession s = Trees.active();
			if (s != null) {
				TreeMenus.tree(s, (double)this.menuButton.getX(), (double)(this.menuButton.getY() + 21), true);
			}
		}));
		this.panelButton = (CFHGuiIconButton)this.addHeaderAction(
			new CFHGuiIconButton(0, 0, 20, Icon.PANEL, Component.empty(), () -> this.panel(!TreeConfig.get().plannerPanel))
		);
		this.foldAllButton = (CFHGuiIconButton)this.addHeaderAction(
			new CFHGuiIconButton(0, 0, 20, Icon.WINDOWED, Component.translatable("cfhgui_crafting.tree.menu.fold_all"), () -> {
				TreeSession s = Trees.active();
				if (s != null) {
					s.foldAll();
				}
			})
		);
		this.unfoldAllButton = (CFHGuiIconButton)this.addHeaderAction(
			new CFHGuiIconButton(0, 0, 20, Icon.FULLSCREEN, Component.translatable("cfhgui_crafting.tree.menu.unfold_all"), () -> {
				TreeSession s = Trees.active();
				if (s != null) {
					s.unfoldAll();
				}
			})
		);
		this.foldButton = (CFHGuiIconButton)this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.SELECTION, Component.empty(), () -> {
			TreeSession s = Trees.active();
			if (s != null) {
				TreeView.foldMode(s, !s.foldMode);
			}
		}));
		this.keepButton = (CFHGuiIconButton)this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.BOOKMARK, Component.empty(), () -> {
			TreeEntry e = Trees.shown();
			if (e != null) {
				Trees.save(e, !e.saved);
			}
		}));
		this.teamButton = TeamPinsClient.active() ? (CFHGuiIconButton)this.addHeaderAction(TeamTrees.button(20)) : null;
		this.moreButton = (CFHGuiIconButton)this.addHeaderAction(
			new CFHGuiIconButton(0, 0, 20, Icon.PLUS, Component.translatable("cfhgui_crafting.tree.batches.more"), () -> this.batches(1))
		);
		this.countButton = (CFHGuiButton)this.addHeaderAction(new CFHGuiButton(0, 0, 30, 20, Component.literal("x1"), this::ideal).variant(Variant.GHOST));
		this.lessButton = (CFHGuiIconButton)this.addHeaderAction(
			new CFHGuiIconButton(0, 0, 20, Icon.MINUS, Component.translatable("cfhgui_crafting.tree.batches.less"), () -> this.batches(-1))
		);
		this.viewSwitch = (PlannerViewSwitch)this.addHeaderAction(new PlannerViewSwitch(graphMode(), this.width >= 600, () -> this.graph(!graphMode())));
		this.nameField = (CFHGuiTextField)this.add(new CFHGuiTextField(this.headerTitleX() - 4, 7, 160, 18, Component.translatable("cfhgui_crafting.tree.rename.title")));
		this.nameField.maxLength(48);
		this.nameField.onEnter(() -> this.naming(false, true));
		this.nameField.onEscape(() -> this.naming(false, false));
		this.nameField.visible = false;
		this.naming = false;
		this.panel = new PlannerPanel(x$0 -> {
			AbstractWidget var10000 = (AbstractWidget)this.add(x$0);
		});
		this.area = this.contentRect();
		boolean open = TreeConfig.get().plannerPanel;
		int panelW = open ? Mth.clamp(Math.round((float)this.area.w() * 0.28F), 156, 250) : 0;
		this.leftW = open ? this.area.w() - panelW - 6 : this.area.w();
		this.placeCost(44);
		this.panelRect = open ? Rect.of(this.area.right() - panelW, this.area.y(), panelW, this.area.h()) : Rect.of(0, 0, 0, 0);
		this.panel.setRect(this.panelRect);
	}

	private void placeCost(int costH) {
		this.treeRect = Rect.of(this.area.x(), this.area.y(), this.leftW, this.area.h() - costH - 6);
		this.costRect = Rect.of(this.area.x(), this.area.bottom() - costH, this.leftW, costH);
	}

	private void batches(int by) {
		TreeSession s = Trees.active();
		if (s != null) {
			s.batches(s.tree.batches + (long)by * (long)(EmiInput.isShiftDown() ? 16 : 1));
		}
	}

	private void ideal() {
		TreeSession s = Trees.active();
		if (s != null) {
			s.batches(s.tree.cost.getIdealBatch(s.tree.goal, 1L, 1L));
		}
	}

	private void naming(boolean on, boolean keep) {
		TreeSession s = Trees.active();
		if (on && s != null) {
			this.nameField.setValue(s.displayName().getString());
			this.nameField.visible = true;
			this.nameField.moveCursorToEnd(false);
			this.nameField.setHighlightPos(0);
			this.setFocused(this.nameField);
			this.nameField.setFocused(true);
			this.naming = true;
		} else {
			if (this.naming && keep && s != null) {
				s.rename(this.nameField.getValue());
			}

			this.naming = false;
			this.nameField.visible = false;
			this.nameField.setFocused(false);
			if (this.getFocused() == this.nameField) {
				this.setFocused(null);
			}
		}
	}

	public Component getTitle() {
		if (this.naming) {
			return Component.empty();
		} else {
			TreeSession s = Trees.active();
			return s == null ? super.getTitle() : s.displayName();
		}
	}

	private void prepare(@Nullable TreeSession s) {
		boolean tree = s != null;

		for (CFHGuiIconButton b : List.of(
			this.lessButton, this.moreButton, this.keepButton, this.foldButton, this.unfoldAllButton, this.foldAllButton, this.menuButton
		)) {
			b.visible = tree;
		}

		this.countButton.visible = tree;
		this.viewSwitch.visible = tree;
		this.viewSwitch.show(graphMode());
		TreeChrome.panel(this.panelButton, TreeConfig.get().plannerPanel);
		if (!tree) {
			this.panel.hidden();
			if (this.naming) {
				this.naming(false, false);
			}
		} else {
			String count = "x" + s.tree.batches;
			if (!count.equals(this.countButton.getMessage().getString())) {
				this.countButton.setMessage(Component.literal(count));
				this.countButton.setWidth(Math.max(26, CFHGuiDraw.width(count) + 12));
				this.countButton
					.tip(
						List.of(
							Component.translatable("cfhgui_crafting.tree.batches", new Object[]{s.tree.batches}),
							Component.translatable("cfhgui_crafting.tree.batches.tip").withStyle(st -> st.withColor(TreePaint.dim() & 16777215))
						)
					);
				this.reflowHeaderActions();
			}

			this.lessButton.active = s.tree.batches > 1L;
			TreeChrome.foldMode(this.foldButton, s);
			TreeEntry entry = Trees.shown();
			if (entry != null) {
				this.keepButton.active = entry.writable();
				TreeChrome.keep(this.keepButton, entry);
			}

			if (this.teamButton != null) {
				TeamTrees.sync(this.teamButton);
			}

			this.titleRight = this.headerActionsLeft() - 10;
			if (this.naming) {
				this.nameField.setWidth(Mth.clamp(CFHGuiDraw.width(this.nameField.getValue()) + 40, 120, Math.max(120, this.titleRight - this.nameField.getX())));
			}

			this.flowCost(s);
			if (TreeConfig.get().plannerPanel) {
				this.panel.prepare(s);
			} else {
				this.panel.hidden();
			}
		}
	}

	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(g, mouseX, mouseY, partialTick);
		float a = Mth.clamp(this.entrance.get(), 0.0F, 1.0F);
		plate(g, this.treeRect, a);
		if (Trees.active() != null) {
			plate(g, this.costRect, a);
			if (this.panelRect.w() > 0) {
				plate(g, this.panelRect, a);
			}
		}
	}

	private static void plate(GuiGraphics g, Rect r, float alpha) {
		if (r.w() > 0) {
			Theme t = Theme.current();
			if (t.isVanilla()) {
				g.fill(r.x(), r.y(), r.right(), r.bottom(), Colors.scaleAlpha(-1476395008, alpha));
				CFHGuiDraw.outline(g, r.x(), r.y(), r.w(), r.h(), Colors.scaleAlpha(-16777216, alpha), 0);
				CFHGuiDraw.outline(g, r.x() + 1, r.y() + 1, r.w() - 2, r.h() - 2, Colors.scaleAlpha(956301311, alpha), 0);
			} else {
				CFHGuiDraw.plate(g, r.x(), r.y(), r.w(), r.h(), t.palette().bg2(), t.palette().border(), 0.4F, alpha);
			}
		}
	}

	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.tooltip = null;
		this.hoveredStack = null;
		TreeSession s = EmiReloadManager.isLoaded() ? Trees.sync() : null;
		this.prepare(s);
		super.render(g, mouseX, mouseY, partialTick);
		if (this.tooltip != null && !this.tooltip.isEmpty() && !Popups.any()) {
			EmiRenderHelper.drawTooltip(this, EmiDrawContext.wrap(g), this.tooltip, mouseX, mouseY);
		}
	}

	protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		TreeSession s = EmiReloadManager.isLoaded() ? Trees.active() : null;
		boolean free = !Popups.any();
		Theme.enterPanel();

		try {
			int top = this.treeRect.y() + 3;
			if (QuickAccess.any()) {
				this.quick.setBounds(this.treeRect.x() + 5, top, this.treeRect.w() - 10);
				this.quick.render(g, mouseX, mouseY, partialTick);
				top += 28;
				g.fill(this.treeRect.x() + 5, top - 1, this.treeRect.right() - 5, top, Theme.current().isVanilla() ? 1090519039 : Theme.current().palette().border());
			} else {
				this.quick.hidden();
			}

			if (s != null) {
				if (graphMode()) {
					this.graph.setBounds(this.treeRect.x() + 1, top + 1, this.treeRect.w() - 2, this.treeRect.bottom() - top - 2);
					this.graph.render(g, s, mouseX, mouseY, partialTick);
					if (free && this.graph.contains((double)mouseX, (double)mouseY)) {
						this.tooltip = this.graph.tooltip();
						EmiStackInteraction node = this.graph.hoveredStack();
						if (!node.isEmpty()) {
							this.hoveredStack = node;
						}
					}
				} else {
					this.view.setBounds(this.treeRect.x() + 1, top + 2, this.treeRect.w() - 2, this.treeRect.bottom() - top - 4);
					this.view.render(g, s, mouseX, mouseY, partialTick);
					if (free && this.view.contains((double)mouseX, (double)mouseY)) {
						this.tooltip = this.view.tooltip();
						EmiStackInteraction row = this.view.hoveredStack();
						if (!row.isEmpty()) {
							this.hoveredStack = row;
						}
					}
				}

				if (!this.naming && free && this.overTitle((double)mouseX, (double)mouseY)) {
					int tx = this.headerTitleX() + Math.min(CFHGuiDraw.width(Fonts.heading(this.getTitle())), this.titleRight - this.headerTitleX()) + 4;
					Icons.draw(g, Icon.EDIT, tx, 12, 8, TreePaint.muted());
				}

				if (this.panelRect.w() > 0) {
					this.panel.render(g, s, mouseX, mouseY, partialTick, free);
					if (this.panel.contains((double)mouseX, (double)mouseY)) {
						if (this.panel.tooltip() != null) {
							this.tooltip = this.panel.tooltip();
						}

						if (this.panel.hoveredStack() != null) {
							this.hoveredStack = this.panel.hoveredStack();
						}
					}
				}

				this.cost(g, s, mouseX, mouseY, partialTick, free);
				return;
			}

			this.view.render(g, null, mouseX, mouseY, partialTick);
			this.graph.render(g, null, mouseX, mouseY, partialTick);
			this.empty(g);
		} finally {
			Theme.exitPanel();
		}
	}

	private boolean overTitle(double mx, double my) {
		int tx = this.headerTitleX();
		return Trees.active() != null
			&& my >= 4.0
			&& my < 28.0
			&& mx >= (double)(tx - 2)
			&& mx < (double)Math.min(this.titleRight, tx + CFHGuiDraw.width(Fonts.heading(this.getTitle())) + 16);
	}

	private void empty(GuiGraphics g) {
		boolean van = Theme.current().isVanilla();
		int width = this.treeRect.w() - 40;
		List<FormattedCharSequence> lines = this.font
			.split(Component.translatable(EmiReloadManager.isLoaded() ? "cfhgui_crafting.planner.empty" : "emi.reloading"), Math.min(300, width));
		List<FormattedCharSequence> how = this.font
			.split(Component.translatable(QuickAccess.any() ? "cfhgui_crafting.tree.empty" : "cfhgui_crafting.tree.empty.none"), Math.min(300, width));
		int cx = this.treeRect.x() + this.treeRect.w() / 2;
		int y = this.treeRect.y() + this.treeRect.h() / 2 - 24 - (lines.size() + how.size()) * 5;
		Icons.draw(g, Icon.LAN, cx - 12, y, 24, TreePaint.dim());
		y += 32;

		for (FormattedCharSequence line : lines) {
			g.drawString(this.font, line, cx - this.font.width(line) / 2, y, TreePaint.muted(), van);
			y += 11;
		}

		y += 6;

		for (FormattedCharSequence line : how) {
			g.drawString(this.font, line, cx - this.font.width(line) / 2, y, TreePaint.dim(), van);
			y += 10;
		}
	}

	private void flowCost(TreeSession s) {
		int x = this.area.x() + 7;
		int right = this.area.x() + this.leftW - 7;
		this.cells.clear();
		int cx = x;
		int cy = 0;

		for (TreeFigures.Raw raw : s.raws()) {
			String text = (raw.estimated ? "~" : "")
				+ (
					raw.missing <= 0L
						? TreeTips.amount(raw.ingredient, raw.wanted, 1.0F)
						: TreeTips.amount(raw.ingredient, raw.have(), 1.0F) + "/" + TreeTips.amount(raw.ingredient, raw.wanted, 1.0F)
				);
			int w = 20 + this.font.width(text) + 8;
			if (cx + w > right && cx > x) {
				cx = x;
				cy += 20;
			}

			this.cells.add(new PlannerScreen.Cell(raw, false, cx, cy, w, text, null));
			cx += w;
		}

		if (!s.leftovers().isEmpty()) {
			Component caption = Component.translatable("emi.leftovers");
			int cw = this.font.width(caption) + 8;
			if (cx > x && cx + 12 + cw + 40 > right) {
				cx = x;
				cy += 20;
			} else if (cx > x) {
				cx += 12;
			}

			this.cells.add(new PlannerScreen.Cell(null, true, cx, cy, cw, null, caption));
			cx += cw;

			for (TreeFigures.Raw raw : s.leftovers()) {
				String text = (raw.estimated ? "~" : "") + TreeTips.amount(raw.ingredient, raw.wanted, 1.0F);
				int w = 20 + this.font.width(text) + 8;
				if (cx + w > right && cx > x) {
					cx = x;
					cy += 20;
				}

				this.cells.add(new PlannerScreen.Cell(raw, true, cx, cy, w, text, null));
				cx += w;
			}
		}

		this.costContent = cy + 20;
		int rows = Math.max(1, Math.min(this.area.h() >= 230 ? 2 : 1, this.costContent / 20));
		this.placeCost(Mth.clamp(24 + rows * 20, 44, 64));
	}

	private void cost(GuiGraphics g, TreeSession s, int mouseX, int mouseY, float partialTick, boolean free) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		int x = this.costRect.x() + 7;
		int right = this.costRect.right() - 7;
		int top = this.costRect.y() + 5;
		g.drawString(this.font, Fonts.heading(Component.translatable("emi.total_cost")), x, top, van ? -1 : p.textMuted(), van);
		Component far = s.complete()
			? Component.translatable("cfhgui_crafting.tree.state.done")
			: TreeTips.counted("cfhgui_crafting.planner.cost.progress", (long)s.steps(), Math.round(s.fraction() * 100.0F), s.stepsDone(), s.steps());
		g.drawString(this.font, far, right - this.font.width(far), top, s.complete() ? TreePaint.success() : TreePaint.dim(), van);
		TreePaint.bar(g, x, top + 11, right - x, van ? 1 : 2, s.fraction(), van ? 1627389951 : p.border(), s.complete() ? TreePaint.success() : TreePaint.accent());
		int areaTop = top + 16;
		int areaBottom = this.costRect.bottom() - 3;
		this.costScroll = Mth.clamp(this.costScroll, 0, Math.max(0, this.costContent - (areaBottom - areaTop)));
		g.enableScissor(this.costRect.x() + 1, areaTop, this.costRect.right() - 1, areaBottom);

		for (PlannerScreen.Cell cell : this.cells) {
			int y = areaTop + cell.y - this.costScroll;
			if (y + 20 > areaTop && y < areaBottom) {
				if (cell.caption != null) {
					g.drawString(this.font, cell.caption, cell.x, y + 6, TreePaint.dim(), van);
				} else {
					boolean over = free && mouseX >= cell.x && mouseX < cell.x + cell.w - 4 && mouseY >= Math.max(y, areaTop) && mouseY < Math.min(y + 20, areaBottom);
					boolean have = !cell.leftover && cell.raw.missing <= 0L;
					TreePaint.plate(g, cell.x, y + 1, over ? 1.0F : 0.0F, have, 0, false, 1.0F);
					cell.raw.ingredient.render(g, cell.x + 1, y + 2, partialTick, -3);
					g.drawString(this.font, cell.text, cell.x + 21, y + 6, cell.leftover ? TreePaint.muted() : (have ? TreePaint.success() : TreePaint.warning()), van);
					if (over) {
						this.hoveredStack = new EmiStackInteraction(cell.raw.ingredient, null, true);
						List<ClientTooltipComponent> tip = new ArrayList<>(cell.raw.ingredient.getTooltip());
						tip.add(
							ClientTooltipComponent.create(
								Component.translatable(
										cell.leftover ? "cfhgui_crafting.planner.cost.leftover" : (have ? "cfhgui_crafting.planner.cost.have" : "cfhgui_crafting.planner.cost.missing"),
										new Object[]{
											TreeTips.amount(cell.raw.ingredient, cell.leftover ? cell.raw.wanted : cell.raw.missing, 1.0F),
											TreeTips.amount(cell.raw.ingredient, cell.raw.have(), 1.0F),
											TreeTips.amount(cell.raw.ingredient, cell.raw.wanted, 1.0F)
										}
									)
									.withStyle(st -> st.withColor((cell.leftover ? TreePaint.muted() : (have ? TreePaint.success() : TreePaint.warning())) & 16777215))
									.getVisualOrderText()
							)
						);
						this.tooltip = tip;
					}
				}
			}
		}

		g.disableScissor();
		if (this.costContent > areaBottom - areaTop) {
			int view = areaBottom - areaTop;
			int barH = Math.max(8, view * view / this.costContent);
			int by = areaTop + (view - barH) * this.costScroll / Math.max(1, this.costContent - view);
			CFHGuiDraw.rect(g, this.costRect.right() - 4, by, 2, barH, van ? -8355712 : p.borderStrong());
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.naming && !this.nameField.isMouseOver(mouseX, mouseY)) {
			this.naming(false, true);
		}

		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		} else {
			TreeSession s = Trees.active();
			if (QuickAccess.any() && this.quick.mouseClicked(mouseX, mouseY, button)) {
				return true;
			} else if (s == null) {
				return false;
			} else if (button == 0 && this.overTitle(mouseX, mouseY)) {
				this.naming(true, false);
				CFHGuiSounds.tick();
				return true;
			} else if (graphMode() ? !this.graph.mouseClicked(mouseX, mouseY, button) : !this.view.mouseClicked(mouseX, mouseY, button)) {
				if (this.panelRect.w() > 0 && this.panel.mouseClicked(mouseX, mouseY, button)) {
					return true;
				} else if (this.hoveredStack != null && EmiScreenManager.stackInteraction(this.hoveredStack, bindx -> bindx.matchesMouse(button))) {
					return true;
				} else {
					Function<EmiBind, Boolean> bind = b -> b.matchesMouse(button);
					if (bind.apply(EmiConfig.back)) {
						EmiHistory.pop();
						return true;
					} else {
						return false;
					}
				}
			} else {
				return true;
			}
		}
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		boolean taken = this.quick.mouseReleased(mouseX, mouseY, button);
		taken |= graphMode() ? this.graph.mouseReleased(mouseX, mouseY, button) : this.view.mouseReleased(mouseX, mouseY, button);
		if (this.panelRect.w() > 0) {
			taken |= this.panel.mouseReleased(mouseX, mouseY, button);
		}

		return taken || super.mouseReleased(mouseX, mouseY, button);
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (this.quick.mouseDragged(mouseX, mouseY)) {
			return true;
		} else {
			return (graphMode() ? !this.graph.mouseDragged(mouseX, mouseY, button, dragX, dragY) : !this.view.mouseDragged(mouseX, mouseY, button, dragX, dragY))
				? super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
				: true;
		}
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		TreeSession s = Trees.active();
		if (s != null) {
			if (this.countButton.isMouseOver(mouseX, mouseY) || this.lessButton.isMouseOver(mouseX, mouseY) || this.moreButton.isMouseOver(mouseX, mouseY)) {
				long by = (long)Math.signum(scrollY);
				if (by != 0L) {
					long step = EmiInput.isShiftDown() ? by * 16L : (EmiInput.isControlDown() ? (by > 0L ? s.tree.batches : -s.tree.batches / 2L) : by);
					s.batches(s.tree.batches == 1L && step > 1L ? step : s.tree.batches + step);
				}

				return true;
			}

			if (graphMode() ? this.graph.mouseScrolled(mouseX, mouseY, scrollY) : this.view.mouseScrolled(mouseX, mouseY, scrollY)) {
				return true;
			}

			if (this.panelRect.w() > 0 && this.panel.mouseScrolled(mouseX, mouseY, scrollY)) {
				return true;
			}

			if (this.costRect.contains(mouseX, mouseY)) {
				this.costScroll = this.costScroll - (int)Math.signum(scrollY) * 20;
				return true;
			}
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.naming) {
			return this.nameField.keyPressed(keyCode, scanCode, modifiers) ? true : keyCode != 256 || super.keyPressed(keyCode, scanCode, modifiers);
		} else {
			TreeSession s = Trees.active();
			if (s == null || (graphMode() ? !this.graph.keyPressed(keyCode, scanCode, modifiers) : !this.view.keyPressed(keyCode, scanCode, modifiers))) {
				if (keyCode == 256) {
					this.onClose();
					return true;
				} else if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
					this.onClose();
					return true;
				} else {
					Function<EmiBind, Boolean> bind = b -> b.matchesKey(keyCode, scanCode);
					if (bind.apply(EmiConfig.back)) {
						EmiHistory.pop();
						return true;
					} else if (this.panelRect.w() > 0 && this.panel.keyPressed(keyCode, scanCode, modifiers)) {
						return true;
					} else if (this.hoveredStack != null && EmiScreenManager.stackInteraction(this.hoveredStack, bind)) {
						return true;
					} else {
						if (EmiInput.isControlDown() && keyCode == 82) {
							List<EmiRecipe> recipes = EmiApi.getRecipeManager().getRecipes();

							for (int i = 0; i < 100000 && !recipes.isEmpty(); i++) {
								EmiRecipe recipe = recipes.get(EmiUtil.RANDOM.nextInt(recipes.size()));
								if (recipe.supportsRecipeTree()) {
									Trees.start(recipe);
									return true;
								}
							}
						}

						return super.keyPressed(keyCode, scanCode, modifiers);
					}
				}
			} else {
				return true;
			}
		}
	}

	@Override
	public void chosen(TreeSession session, TreeNode node) {
	}

	public void back() {
		if (this.naming) {
			this.naming(false, true);
		}

		Trees.flush();
		Popups.closeAll();
		this.minecraft.setScreen(this.old);
	}

	public boolean isPauseScreen() {
		return false;
	}

	static boolean shows(EmiRecipe recipe) {
		return BoM.tree != null && BoM.tree.goal.recipe == recipe;
	}

	boolean panelOpen() {
		return this.panelRect.w() > 0;
	}

	private static record Cell(TreeFigures.Raw raw, boolean leftover, int x, int y, int w, @Nullable String text, @Nullable Component caption) {
	}
}
