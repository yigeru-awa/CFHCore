package com.onehumanawa.cfhcore.content.gui.quests.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class TabStrip extends CFHGuiWidget {
	static final int NONE = 0;
	static final int WAITING = 1;
	static final int DONE = 2;
	private final WindowArt.Side side;
	private final Consumer<String> onPick;
	@Nullable
	private final Consumer<String> onAgain;
	private List<TabStrip.Tab> tabs = List.of();
	@Nullable
	private String selected;
	private int page;
	private Rect window = new Rect(0, 0, 0, 0);
	private long shownAtMs;
	@Nullable
	private TabStrip.Tab hot;
	private int hotArrow;
	private long hotSinceMs;
	private final Anim back = new Anim(0.0F, 120, Ease.OUT_CUBIC);
	private final Anim forward = new Anim(0.0F, 120, Ease.OUT_CUBIC);
	private static final String[] BANG = new String[]{".X.", ".X.", ".X.", ".X.", "...", ".X."};
	private static final String[] LOCK = new String[]{".XXX.", "X...X", "XXXXX", "XX.XX", "XXXXX"};
	private static final String[] CHECK = new String[]{".....X", "....XX", "X..XX.", "XXXX..", ".XX..."};

	TabStrip(WindowArt.Side side, Consumer<String> onPick, @Nullable Consumer<String> onAgain) {
		super(0, 0, 10, 10, Component.translatable("cfhgui_quests.vanilla.tabs"));
		this.side = side;
		this.onPick = onPick;
		this.onAgain = onAgain;
		this.silent();
	}

	void place(Rect windowRect) {
		this.window = windowRect;
		switch (this.side) {
			case ABOVE:
				this.setX(windowRect.x());
				this.setY(windowRect.y() - 28);
				this.setWidth(windowRect.w());
				this.setHeight(32);
				break;
			case BELOW:
				this.setX(windowRect.x());
				this.setY(windowRect.bottom() - 4);
				this.setWidth(windowRect.w());
				this.setHeight(32);
				break;
			case LEFT:
				this.setX(windowRect.x() - 28);
				this.setY(windowRect.y());
				this.setWidth(32);
				this.setHeight(windowRect.h());
				break;
			case RIGHT:
				this.setX(windowRect.right() - 4);
				this.setY(windowRect.y());
				this.setWidth(32);
				this.setHeight(windowRect.h());
		}

		this.page = Mth.clamp(this.page, 0, Math.max(0, this.pages() - 1));
		this.turnToSelected();
	}

	void tabs(List<TabStrip.Tab> next) {
		List<TabStrip.Tab> old = this.tabs;
		boolean same = old.size() == next.size();

		for (int i = 0; i < next.size(); i++) {
			TabStrip.Tab t = next.get(i);
			if (same && !old.get(i).id.equals(t.id)) {
				same = false;
			}

			for (TabStrip.Tab o : old) {
				if (o.id.equals(t.id)) {
					t.hover.snap(o.hover.get());
					break;
				}
			}
		}

		boolean first = old.isEmpty() && !next.isEmpty();
		this.tabs = new ArrayList<>(next);
		if (first) {
			this.shownAtMs = Clock.nowMs();
		}

		this.page = Mth.clamp(this.page, 0, Math.max(0, this.pages() - 1));
		if (!same) {
			this.turnToSelected();
		}
	}

	void select(@Nullable String id) {
		boolean other = id == null ? this.selected != null : !id.equals(this.selected);
		this.selected = id;
		if (other) {
			this.turnToSelected();
		}
	}

	boolean isEmpty() {
		return this.tabs.isEmpty();
	}

	@Nullable
	String neighbour(int direction) {
		int at = -1;

		for (int i = 0; i < this.tabs.size(); i++) {
			if (this.tabs.get(i).id.equals(this.selected)) {
				at = i;
				break;
			}
		}

		for (int ix = at + direction; ix >= 0 && ix < this.tabs.size(); ix += direction) {
			if (!this.tabs.get(ix).locked) {
				return this.tabs.get(ix).id;
			}
		}

		return null;
	}

	private int slots() {
		return Math.max(1, this.side.horizontal() ? (this.window.w() + 4) / this.side.pitch() : this.window.h() / this.side.pitch());
	}

	private int perPage() {
		int slots = this.slots();
		return this.tabs.size() <= slots ? slots : Math.max(1, slots - 1);
	}

	private int pages() {
		return Math.max(1, (this.tabs.size() + this.perPage() - 1) / this.perPage());
	}

	private void turnToSelected() {
		if (this.selected != null) {
			for (int i = 0; i < this.tabs.size(); i++) {
				if (this.tabs.get(i).id.equals(this.selected)) {
					this.page = i / this.perPage();
					return;
				}
			}
		}
	}

	private int slotX(int k) {
		return switch (this.side) {
			case ABOVE, BELOW -> this.window.x() + this.side.pitch() * k;
			case LEFT -> this.window.x() - 28;
			case RIGHT -> this.window.right() - 4;
		};
	}

	private int slotY(int k) {
		return switch (this.side) {
			case ABOVE -> this.window.y() - 28;
			case BELOW -> this.window.bottom() - 4;
			case LEFT, RIGHT -> this.window.y() + this.side.pitch() * k;
		};
	}

	private boolean flush(int k) {
		return this.side.horizontal() ? this.slotX(k) + this.side.w == this.window.right() : this.slotY(k) + this.side.h == this.window.bottom();
	}

	@Nullable
	private TabStrip.Tab tabAt(double mx, double my) {
		int per = this.perPage();

		for (int k = 0; k < per; k++) {
			int i = this.page * per + k;
			if (i >= this.tabs.size()) {
				break;
			}

			int x = this.slotX(k);
			int y = this.slotY(k);
			if (mx >= (double)x && mx < (double)(x + this.side.w) && my >= (double)y && my < (double)(y + this.side.h)) {
				return this.tabs.get(i);
			}
		}

		return null;
	}

	private int arrowX() {
		return this.slotX(this.perPage()) + (this.side == WindowArt.Side.RIGHT ? 5 : 1);
	}

	private int arrowY() {
		return this.slotY(this.perPage()) + (this.side == WindowArt.Side.BELOW ? 9 : 5);
	}

	private int arrowAt(double mx, double my) {
		if (this.pages() <= 1) {
			return 0;
		} else {
			int x = this.arrowX();
			int y = this.arrowY();
			if (my < (double)y || my >= (double)(y + 17)) {
				return 0;
			} else if (mx >= (double)x && mx < (double)(x + 12)) {
				return -1;
			} else {
				return mx >= (double)(x + 14) && mx < (double)(x + 26) ? 1 : 0;
			}
		}
	}

	private void turn(int by) {
		int to = Mth.clamp(this.page + by, 0, this.pages() - 1);
		if (to != this.page) {
			this.page = to;
			CFHGuiSounds.tick();
		}
	}

	public boolean isMouseOver(double mouseX, double mouseY) {
		return this.visible && (this.tabAt(mouseX, mouseY) != null || this.arrowAt(mouseX, mouseY) != 0);
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.visible && button == 0) {
			int arrow = this.arrowAt(mouseX, mouseY);
			if (arrow != 0) {
				this.turn(arrow);
				return true;
			} else {
				TabStrip.Tab t = this.tabAt(mouseX, mouseY);
				if (t == null) {
					return false;
				} else {
					if (t.id.equals(this.selected)) {
						if (this.onAgain != null) {
							this.onAgain.accept(t.id);
						}
					} else if (!t.locked) {
						CFHGuiSounds.tick();
						this.onPick.accept(t.id);
					}

					return true;
				}
			}
		} else {
			return false;
		}
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.pages() > 1 && this.isMouseOver(mouseX, mouseY)) {
			this.turn(!(scrollY > 0.0) && !(scrollX > 0.0) ? 1 : -1);
			return true;
		} else {
			return false;
		}
	}

	public ComponentPath nextFocusPath(FocusNavigationEvent event) {
		return null;
	}

	public void onClick(double mouseX, double mouseY) {
	}

	protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY);
	}

	protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY);
	}

	private void draw(GuiGraphics g, int mouseX, int mouseY) {
		if (!this.tabs.isEmpty()) {
			Theme t = Theme.current();
			boolean van = t.isVanilla();
			float alpha = this.effectiveAlpha();
			boolean inside = g.containsPointInScissor(mouseX, mouseY);
			TabStrip.Tab over = inside ? this.tabAt((double)mouseX, (double)mouseY) : null;
			int arrow = inside ? this.arrowAt((double)mouseX, (double)mouseY) : 0;
			if (over != this.hot || arrow != this.hotArrow) {
				this.hot = over;
				this.hotArrow = arrow;
				this.hotSinceMs = Clock.nowMs();
			}

			int per = this.perPage();
			int slots = this.slots();
			long now = Clock.nowMs();
			float motion = t.motion();

			for (int pass = 0; pass < 3; pass++) {
				for (int k = 0; k < per; k++) {
					int i = this.page * per + k;
					if (i >= this.tabs.size()) {
						break;
					}

					TabStrip.Tab tab = this.tabs.get(i);
					boolean on = tab.id.equals(this.selected);
					float in = !(motion <= 0.0F) && this.shownAtMs != 0L
						? Mth.clamp(((float)(now - this.shownAtMs) - (float)k * 24.0F * motion) / (190.0F * motion), 0.0F, 1.0F)
						: 1.0F;
					int slide = Math.round((1.0F - Ease.OUT_CUBIC.apply(in)) * 7.0F);
					int x = this.slotX(k) + (this.side == WindowArt.Side.LEFT ? slide : (this.side == WindowArt.Side.RIGHT ? -slide : 0));
					int y = this.slotY(k) + (this.side == WindowArt.Side.ABOVE ? slide : (this.side == WindowArt.Side.BELOW ? -slide : 0));
					float a = alpha * Math.min(1.0F, in * 1.5F);
					if (pass == 0) {
						tab.hover.set(tab == over && !tab.locked && !on);
						WindowArt.tab(g, this.side, x, y, k == 0 ? 0 : (k == slots - 1 && this.flush(k) ? 2 : 1), on, tab.hover.get(), a);
					} else if (pass == 1) {
						if (a > 0.5F) {
							tab.icon
								.draw(
									g, (float)(x + this.side.iconX + 8), (float)(y + this.side.iconY + 8), 16.0F, van ? -13949406 : (on ? t.palette().text() : t.palette().textMuted())
								);
						}
					} else {
						this.marks(g, tab, x, y, on, a);
					}
				}

				if (pass == 0) {
					g.flush();
				}
			}

			if (this.pages() > 1) {
				this.back.set(arrow < 0 && this.page > 0);
				this.forward.set(arrow > 0 && this.page < this.pages() - 1);
				WindowArt.pageArrow(g, this.arrowX(), this.arrowY(), false, this.back.get(), this.page > 0, alpha);
				WindowArt.pageArrow(g, this.arrowX() + 14, this.arrowY(), true, this.forward.get(), this.page < this.pages() - 1, alpha);
			}

			if (Clock.nowMs() - this.hotSinceMs > 350L) {
				if (over != null) {
					if (over.said == null) {
						over.said = over.tip == null ? List.of(over.title) : over.tip.get();
					}

					CFHGuiTooltips.request(over.said, this);
				} else if (arrow != 0) {
					CFHGuiTooltips.request(Component.translatable("cfhgui_quests.vanilla.tabs.page", new Object[]{this.page + 1, this.pages()}), this);
				}
			}
		}
	}

	private void marks(GuiGraphics g, TabStrip.Tab tab, int x, int y, boolean on, float a) {
		Theme t = Theme.current();
		boolean van = t.isVanilla();
		int ix = x + this.side.iconX;
		int iy = y + this.side.iconY;
		if (tab.locked || tab.mark != 0) {
			g.pose().pushPose();
			g.pose().translate(0.0F, 0.0F, 200.0F);
			if (tab.locked) {
				g.fill(ix - 1, iy - 1, ix + 17, iy + 17, Colors.scaleAlpha(van ? -1879048192 : Colors.withAlpha(t.palette().bg(), 168), a));
				pixels(g, LOCK, ix + 10, iy + 10, van ? -2039584 : t.palette().textMuted(), a);
			} else if (tab.mark == 1) {
				int bob = t.motion() > 0.0F ? Math.round(Math.abs(Mth.sin((float)Clock.nowMs() / 310.0F)) * 2.0F) : 0;
				pixels(g, BANG, ix + 13, iy - 3 - bob, van ? -171 : t.palette().warning(), a);
			} else if (tab.mark == 2) {
				pixels(g, CHECK, ix + 10, iy - 2, van ? -11141291 : t.palette().success(), a);
			}

			g.pose().popPose();
		}
	}

	private static void pixels(GuiGraphics g, String[] rows, int x, int y, int color, float alpha) {
		g.fill(x - 1, y - 1, x + rows[0].length() + 1, y + rows.length + 1, Colors.scaleAlpha(-16777216, alpha));
		int ink = Colors.scaleAlpha(color, alpha);

		for (int row = 0; row < rows.length; row++) {
			String s = rows[row];
			int from = -1;

			for (int i = 0; i <= s.length(); i++) {
				boolean lit = i < s.length() && s.charAt(i) == 'X';
				if (lit && from < 0) {
					from = i;
				} else if (!lit && from >= 0) {
					g.fill(x + from, y + row, x + i, y + row + 1, ink);
					from = -1;
				}
			}
		}
	}

	protected void updateWidgetNarration(NarrationElementOutput out) {
		out.add(NarratedElementType.TITLE, this.getMessage());
	}

	static final class Tab {
		final String id;
		final Component title;
		final QuestIcons.Drawable icon;
		boolean locked;
		int mark = 0;
		@Nullable
		Supplier<List<Component>> tip;
		@Nullable
		private List<Component> said;
		private final Anim hover = new Anim(0.0F, 140, Ease.OUT_CUBIC);

		Tab(String id, Component title, QuestIcons.Drawable icon) {
			this.id = id;
			this.title = title;
			this.icon = icon;
		}
	}
}
