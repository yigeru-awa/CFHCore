package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.emi.EmiRenderHelper;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStackInteraction;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.Bounds;
import com.onehumanawa.cfhcore.content.gui.emi.input.EmiInput;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.emi.screen.RecipeScreen;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.crafting.look.Look;
import com.onehumanawa.cfhcore.content.gui.crafting.look.SidebarLook;
import com.onehumanawa.cfhcore.content.gui.crafting.team.client.TeamTrees;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class TreeDock implements TreeDockSeam.Dock, TreeView.Host {
	static final TreeDock INSTANCE = new TreeDock();
	private static final int MIN_WIDTH = 78;
	private static final int MAX_WIDTH = 320;
	private static final int MARGIN = 3;
	private static final int PAD = 4;
	private static final int BESIDE = 8;
	private static final int FOOT_CLEAR = 25;
	private static final int HEADER = 20;
	private static final int FOOTER = 19;
	private final TreeView view = new TreeView(this);
	private final QuickAccess quick = new QuickAccess();
	private Bounds bounds = Bounds.EMPTY;
	@Nullable
	private Bounds room;
	@Nullable
	private Screen roomOf;
	private final Anim shown = new Anim(1.0F, 180, Ease.OUT_CUBIC);
	private boolean focused;
	private boolean pressedInside;
	private final CFHGuiIconButton foldButton = button(Icon.SELECTION, () -> {
		TreeSession s = Trees.active();
		if (s != null) {
			TreeView.foldMode(s, !s.foldMode);
		}
	});
	private final CFHGuiIconButton keepButton = button(Icon.BOOKMARK, () -> {
		TreeEntry e = Trees.shown();
		if (e != null) {
			Trees.save(e, !e.saved);
		}
	});
	private final CFHGuiIconButton plannerButton = button(Icon.FULLSCREEN, TreeActions::openPlanner);
	private final CFHGuiIconButton moreButton = button(Icon.DOTS, () -> {
		TreeSession s = Trees.active();
		if (s != null) {
			TreeMenus.tree(s, (double)this.moreX(), (double)this.moreY(), false);
		}
	});
	private final CFHGuiIconButton lessButton = button(Icon.MINUS, () -> this.batches(-1));
	private final CFHGuiIconButton moreBatchesButton = button(Icon.PLUS, () -> this.batches(1));
	private final CFHGuiIconButton teamButton = TeamTrees.button(16);
	private final List<CFHGuiIconButton> buttons = List.of(
		this.foldButton, this.keepButton, this.plannerButton, this.moreButton, this.lessButton, this.moreBatchesButton, this.teamButton
	);
	private int batchX;
	private int batchW;
	private int footY;

	private static CFHGuiIconButton button(Icon icon, Runnable press) {
		CFHGuiIconButton b = new CFHGuiIconButton(0, 0, 16, icon, Component.empty(), press);
		b.visible = false;
		return b;
	}

	private int moreX() {
		return this.moreButton.getX();
	}

	private int moreY() {
		return this.moreButton.getY() + this.moreButton.getHeight() + 1;
	}

	@Override
	public boolean isOpen() {
		return Trees.store().dockOpen;
	}

	void open() {
		if (!this.isOpen()) {
			Trees.store().dockOpen = true;
			Trees.store().touch();
			this.shown.snap(0.0F);
			this.shown.set(1.0F);
			TreeDockSeam.changed();
		}
	}

	void close() {
		if (this.isOpen()) {
			Trees.store().dockOpen = false;
			Trees.store().touch();
			this.focused = false;
			TreeDockSeam.changed();
		}
	}

	boolean fits() {
		return this.room != null && this.roomOf == Minecraft.getInstance().screen && widthFor(this.room) >= 78;
	}

	@Override
	public void buttonPressed() {
		CFHGuiSounds.click();
		if (this.isOpen() && !this.bounds.empty()) {
			this.close();
		} else if (this.isOpen() || !this.fits() && this.room != null) {
			TreeActions.openPlanner();
		} else {
			this.open();
		}
	}

	private static int widthFor(Bounds room) {
		int set = TreeConfig.get().dockWidth;
		return Math.min(set > 0 ? Mth.clamp(set, 78, 320) : 320, room.width() - 3 - 8);
	}

	@Override
	public Bounds layout(Screen screen, Bounds room, List<Bounds> exclusion) {
		this.room = room;
		this.roomOf = screen;
		this.bounds = Bounds.EMPTY;
		if (!this.isOpen()) {
			return Bounds.EMPTY;
		} else {
			int x = room.x() + 3;
			int width = widthFor(room);
			int top = 4;
			int bottom = SidebarLook.shownHeight(screen.height) - 25;

			for (Bounds area : exclusion) {
				if (!area.empty() && area.x() < x + width && area.right() > x && area.y() < bottom && area.bottom() > top) {
					int narrow = Math.min(width, area.x() - x - 8);
					boolean low = area.y() + area.height() / 2 > screen.height / 2;
					int cutTop = low ? top : Math.max(top, area.bottom() + 2);
					int cutBottom = low ? Math.min(bottom, area.y() - 2) : bottom;
					long keptNarrow = narrow >= 78 ? (long)narrow * (long)(bottom - top) : -1L;
					long keptCut = (long)width * (long)Math.max(0, cutBottom - cutTop);
					if (keptNarrow >= keptCut) {
						width = narrow;
					} else {
						top = cutTop;
						bottom = cutBottom;
					}
				}
			}

			if (width >= 78 && bottom - top >= 80) {
				this.bounds = new Bounds(x, top, width, bottom - top);
				return this.bounds;
			} else {
				return Bounds.EMPTY;
			}
		}
	}

	@Override
	public int reserve(Screen screen) {
		if (!this.isOpen()) {
			return 0;
		} else {
			int set = TreeConfig.get().dockWidth;
			int wanted = set > 0 ? Mth.clamp(set, 78, 320) : Mth.clamp(Math.round((float)screen.width * 0.3F), 132, 320);
			int left = screen.width - 176 - 44;
			return left < 89 ? 0 : Math.min(wanted + 3 + 8, left);
		}
	}

	@Override
	public void render(EmiDrawContext context, int mouseX, int mouseY, float delta) {
		if (!this.bounds.empty()) {
			GuiGraphics g = context.raw();
			TreeSession s = Trees.sync();
			Theme t = Theme.current();
			Palette p = t.palette();
			boolean van = t.isVanilla();
			float a = Mth.clamp(this.shown.get(), 0.0F, 1.0F);
			int bx = this.bounds.x() - Math.round((1.0F - a) * 10.0F);
			int by = this.bounds.y();
			int bw = this.bounds.width();
			int bh = this.bounds.height();
			if (!Look.plate(g, bx, by, bw, bh)) {
				CFHGuiDraw.floatingPanel(g, bx, by, bw, bh, a);
			}

			if (!(a < 0.6F)) {
				Theme.enterPanel();

				try {
					int ix = bx + 4;
					int iw = bw - 8;
					int top = by + 3;
					this.header(g, s, ix, top, iw, mouseX, mouseY, delta);
					top += 20;
					int track = van ? 1627389951 : p.border();
					if (s == null) {
						g.fill(ix, top, ix + iw, top + 1, track);
					} else {
						TreePaint.bar(g, ix, top, iw, van ? 1 : 2, s.fraction(), track, s.complete() ? TreePaint.success() : TreePaint.accent());
					}

					top += 4;
					if (QuickAccess.any()) {
						this.quick.setBounds(ix, top, iw);
						this.quick.render(g, mouseX, mouseY, delta);
						top += 28;
					} else {
						this.quick.hidden();
					}

					int foot = by + bh - 19 - 2;
					if (s != null) {
						this.view.setBounds(bx + 1, top, bw - 2, foot - top);
						this.view.render(g, s, mouseX, mouseY, delta);
						g.fill(ix, foot, ix + iw, foot + 1, van ? 1090519039 : p.border());
						this.footer(g, s, ix, foot + 2, iw, mouseX, mouseY, delta);
						return;
					}

					this.view.render(g, null, mouseX, mouseY, delta);
					this.empty(g, ix, top, iw, foot - top);

					for (CFHGuiIconButton b : this.buttons) {
						b.visible = false;
					}
				} finally {
					Theme.exitPanel();
				}
			}
		}
	}

	private void header(GuiGraphics g, @Nullable TreeSession s, int x, int y, int w, int mouseX, int mouseY, float delta) {
		Theme t = Theme.current();
		boolean van = t.isVanilla();
		int right = x + w;
		if (s != null) {
			boolean narrow = w < 140;
			TreeEntry entry = Trees.shown();
			right = place(this.moreButton, right, y + 2, true);
			if (!narrow) {
				right = place(this.plannerButton, right, y + 2, true);
			} else {
				this.plannerButton.visible = false;
			}

			if (!narrow && entry != null) {
				this.keepButton.active = entry.writable();
				TreeChrome.keep(this.keepButton, entry);
				right = place(this.keepButton, right, y + 2, true);
			} else {
				this.keepButton.visible = false;
			}

			if (w >= 150 && TeamTrees.sync(this.teamButton)) {
				right = place(this.teamButton, right, y + 2, true);
			} else {
				this.teamButton.visible = false;
			}

			TreeChrome.foldMode(this.foldButton, s);
			right = place(this.foldButton, right, y + 2, true);
			this.moreButton.render(g, mouseX, mouseY, delta);
			if (this.plannerButton.visible) {
				this.plannerButton.render(g, mouseX, mouseY, delta);
			}

			if (this.keepButton.visible) {
				this.keepButton.render(g, mouseX, mouseY, delta);
			}

			if (this.teamButton.visible) {
				this.teamButton.render(g, mouseX, mouseY, delta);
			}

			this.foldButton.render(g, mouseX, mouseY, delta);
		}

		Component title = Fonts.heading((Component)(s == null ? Component.translatable("cfhgui_crafting.tree.title") : s.displayName()));
		FormattedCharSequence shownTitle = CFHGuiDraw.truncate(title, right - x - 4);
		g.drawString(CFHGuiDraw.font(), shownTitle, x + 1, CFHGuiDraw.textY(y, 20), van ? -1 : t.text(), van);
	}

	private static int place(CFHGuiIconButton b, int right, int y, boolean visible) {
		b.visible = visible;
		b.setX(right - b.getWidth());
		b.setY(y);
		return right - b.getWidth() - 1;
	}

	private void footer(GuiGraphics g, TreeSession s, int x, int y, int w, int mouseX, int mouseY, float delta) {
		Theme t = Theme.current();
		boolean van = t.isVanilla();
		this.lessButton.active = s.tree.batches > 1L;
		place(this.lessButton, x + 16, y, true);
		String count = "x" + s.tree.batches;
		this.batchW = Math.max(14, CFHGuiDraw.width(count) + 6);
		this.batchX = x + 17;
		this.footY = y;
		place(this.moreBatchesButton, this.batchX + this.batchW + 16, y, true);
		boolean overCount = mouseX >= this.batchX && mouseX < this.batchX + this.batchW && mouseY >= y && mouseY < y + 16 && !Popups.any();
		g.drawString(CFHGuiDraw.font(), count, this.batchX + (this.batchW - CFHGuiDraw.width(count)) / 2, y + 4, overCount ? TreePaint.accent() : TreePaint.text(), van);
		this.lessButton.render(g, mouseX, mouseY, delta);
		this.moreBatchesButton.render(g, mouseX, mouseY, delta);
		if (overCount) {
			CFHGuiTooltips.request(
				List.of(
					Component.translatable("cfhgui_crafting.tree.batches", new Object[]{s.tree.batches}),
					Component.translatable("cfhgui_crafting.tree.batches.tip").withStyle(st -> st.withColor(TreePaint.dim() & 16777215))
				),
				null
			);
		}

		int left = this.batchX + this.batchW + 18 + 4;
		Component state;
		int color;
		if (s.complete()) {
			state = Component.translatable("cfhgui_crafting.tree.state.done");
			color = TreePaint.success();
		} else if (s.stepsReady() > 0) {
			state = Component.translatable("cfhgui_crafting.tree.state.ready", new Object[]{s.stepsReady()});
			color = van ? -171 : TreePaint.accent();
		} else {
			state = TreeTips.counted("cfhgui_crafting.tree.state.steps", (long)s.steps(), s.stepsDone(), s.steps());
			color = TreePaint.muted();
		}

		Component fitting = state;
		if (CFHGuiDraw.width(state) > x + w - left) {
			fitting = Component.literal(s.stepsDone() + "/" + s.steps());
		}

		if (CFHGuiDraw.width(fitting) <= x + w - left) {
			g.drawString(CFHGuiDraw.font(), fitting, x + w - CFHGuiDraw.width(fitting), y + 4, color, van);
		}
	}

	private void empty(GuiGraphics g, int x, int y, int w, int h) {
		boolean van = Theme.current().isVanilla();
		List<FormattedCharSequence> lines = new ArrayList<>(
			CFHGuiDraw.font().split(Component.translatable(QuickAccess.any() ? "cfhgui_crafting.tree.empty" : "cfhgui_crafting.tree.empty.none"), Math.max(40, w - 8))
		);
		int total = 20 + lines.size() * 10;
		int ty = y + Math.max(4, (h - total) / 2);
		Icons.draw(g, Icon.LAN, x + w / 2 - 8, ty, 16, TreePaint.dim());
		ty += 22;

		for (FormattedCharSequence line : lines) {
			g.drawString(CFHGuiDraw.font(), line, x + (w - CFHGuiDraw.width(line)) / 2, ty, TreePaint.muted(), van);
			ty += 10;
		}
	}

	void tooltip(Screen screen, GuiGraphics g, int mouseX, int mouseY) {
		if (!this.bounds.empty() && screen == this.roomOf && this.isOpen() && !Popups.any() && !TreeDockSeam.bounds().empty()) {
			if (screen instanceof AbstractContainerScreen || screen instanceof RecipeScreen) {
				if (screen instanceof AbstractContainerScreen<?> container && !container.getMenu().getCarried().isEmpty()) {
					return;
				}

				if (EmiScreenManager.draggedStack.isEmpty() && !EmiScreenManager.isDisabled()) {
					List<ClientTooltipComponent> tip = this.view.contains((double)mouseX, (double)mouseY) ? this.view.tooltip() : null;
					if (tip != null && !tip.isEmpty()) {
						EmiRenderHelper.drawTooltip(screen, EmiDrawContext.wrap(g), tip, mouseX, mouseY);
					}
				}
			}
		}
	}

	private boolean inside(double mx, double my) {
		return !this.bounds.empty()
			&& mx >= (double)this.bounds.x()
			&& mx < (double)this.bounds.right()
			&& my >= (double)this.bounds.y()
			&& my < (double)this.bounds.bottom();
	}

	@Override
	public EmiStackInteraction hovered(int mouseX, int mouseY) {
		return this.inside((double)mouseX, (double)mouseY) && this.view.contains((double)mouseX, (double)mouseY)
			? this.view.hoveredStack()
			: EmiStackInteraction.EMPTY;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		this.pressedInside = this.inside(mouseX, mouseY);
		this.focused = this.pressedInside;
		if (!this.pressedInside) {
			return false;
		} else {
			for (CFHGuiIconButton b : this.buttons) {
				if (b.visible && b.mouseClicked(mouseX, mouseY, button)) {
					return true;
				}
			}

			if (QuickAccess.any() && this.quick.mouseClicked(mouseX, mouseY, button)) {
				return true;
			} else {
				TreeSession s = Trees.active();
				if (s != null
					&& button == 0
					&& mouseX >= (double)this.batchX
					&& mouseX < (double)(this.batchX + this.batchW)
					&& mouseY >= (double)this.footY
					&& mouseY < (double)(this.footY + 16)) {
					s.batches(s.tree.cost.getIdealBatch(s.tree.goal, 1L, 1L));
					CFHGuiSounds.click();
					return true;
				} else {
					return this.view.mouseClicked(mouseX, mouseY, button) ? true : button == 0 || button == 1;
				}
			}
		}
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		for (CFHGuiIconButton b : this.buttons) {
			b.mouseReleased(mouseX, mouseY, button);
		}

		boolean taken = this.quick.mouseReleased(mouseX, mouseY, button);
		taken |= this.view.mouseReleased(mouseX, mouseY, button);
		boolean was = this.pressedInside && (button == 0 || button == 1);
		this.pressedInside = false;
		return taken || was;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		return this.quick.mouseDragged(mouseX, mouseY) ? true : this.view.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (!this.inside(mouseX, mouseY)) {
			return false;
		} else {
			TreeSession s = Trees.active();
			if (s != null && mouseY >= (double)(this.footY - 2)) {
				long step = (long)Math.signum(amount) * (long)(EmiInput.isShiftDown() ? 16 : 1);
				if (step != 0L) {
					s.batches(s.tree.batches == 1L && step > 1L ? step : s.tree.batches + step);
				}

				return true;
			} else {
				this.view.mouseScrolled(mouseX, mouseY, amount);
				return true;
			}
		}
	}

	private void batches(int by) {
		TreeSession s = Trees.active();
		if (s != null) {
			s.batches(s.tree.batches + (long)by * (long)(EmiInput.isShiftDown() ? 16 : 1));
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		int key = TreeConfig.get().dockKey;
		if (key > 0 && keyCode == key && (modifiers & 7) == 0) {
			this.buttonPressed();
			return true;
		} else if (!this.bounds.empty() && this.isOpen()) {
			boolean over = this.inside((double)EmiScreenManager.lastMouseX, (double)EmiScreenManager.lastMouseY);
			return !over && !this.focused ? false : this.view.keyPressed(keyCode, scanCode, modifiers);
		} else {
			return false;
		}
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		return false;
	}

	@Override
	public void chosen(TreeSession session, TreeNode node) {
		if (Minecraft.getInstance().screen instanceof RecipeScreen && node.recipe() != null) {
			TreeActions.showRecipe(node);
		}
	}

	TreeView view() {
		return this.view;
	}

	QuickAccess quick() {
		return this.quick;
	}

	Bounds bounds() {
		return this.bounds;
	}

	private TreeDock() {
		this.plannerButton.tip(Component.translatable("cfhgui_crafting.tree.planner"));
		this.moreButton.tip(Component.translatable("cfhgui_crafting.tree.more"));
		this.lessButton.tip(Component.translatable("cfhgui_crafting.tree.batches.less"));
		this.moreBatchesButton.tip(Component.translatable("cfhgui_crafting.tree.batches.more"));
	}
}
