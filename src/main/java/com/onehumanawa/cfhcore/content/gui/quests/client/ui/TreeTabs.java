package com.onehumanawa.cfhcore.content.gui.quests.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.BookStats;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestPainter;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class TreeTabs extends CFHGuiWidget {
	public static final int HEIGHT = 22;
	private static final int PAD = 8;
	private static final int STEP = 7;
	private static final int INFO = 14;
	private final Consumer<Chapter> onPick;
	@Nullable
	private final Runnable onInfo;
	private final List<TreeTabs.Tab> tabs = new ArrayList<>();
	@Nullable
	private String shownId;
	@Nullable
	private String topId;
	private final Anim markX = new Anim(0.0F, 240, Ease.OUT_CUBIC);
	private final Anim markW = new Anim(0.0F, 240, Ease.OUT_CUBIC);
	private final Anim scroll = new Anim(0.0F, 200, Ease.OUT_CUBIC);
	private final Anim infoHover = new Anim(0.0F, 140, Ease.OUT_CUBIC);
	private boolean markSet;
	private boolean inlineTabs = true;
	private int contentW;
	@Nullable
	private TreeTabs.Tab hoverTab;
	private boolean hoverInfo;
	private long hoverSinceMs;

	public TreeTabs(Consumer<Chapter> onPick, @Nullable Runnable onInfo) {
		super(0, 0, 100, 22, Component.translatable("cfhgui_quests.tabs"));
		this.onPick = onPick;
		this.onInfo = onInfo;
		this.silent();
	}

	public void place(int x, int y, int w) {
		this.setX(x);
		this.setY(y);
		this.setWidth(w);
		this.setHeight(22);
		this.layout();
	}

	public void inlineTabs(boolean on) {
		this.inlineTabs = on;
	}

	public void show(@Nullable Chapter top, @Nullable String shown) {
		boolean other = top == null || !top.id.equals(this.topId);
		this.topId = top == null ? null : top.id;
		this.shownId = shown;
		if (other) {
			this.tabs.clear();
			this.markSet = false;
			this.scroll.snap(0.0F);
		}

		this.refresh();
	}

	public void refresh() {
		QuestBook book = QuestsState.book();
		Chapter top = book == null ? null : book.chapter(this.topId);
		List<TreeTabs.Tab> old = new ArrayList<>(this.tabs);
		this.tabs.clear();
		if (book != null && top != null) {
			for (Trees.Branch b : Trees.branches(book, top, this.inlineTabs)) {
				TreeTabs.Tab tab = null;

				for (TreeTabs.Tab o : old) {
					if (o.branch.chapter().id.equals(b.chapter().id)) {
						tab = o;
						break;
					}
				}

				if (tab == null) {
					tab = new TreeTabs.Tab();
				}

				tab.branch = b;
				tab.title = QuestText.title(b.chapter());
				tab.icon = QuestIcons.of(b.chapter().icon);
				tab.open = BookStats.open(b.chapter());
				tab.stat = BookStats.of(b.chapter());
				tab.waiting = tab.stat.claimable();
				tab.accent = QuestPainter.accentOf(b.chapter());
				tab.count = tab.stat.done() + "/" + tab.stat.total();
				this.tabs.add(tab);
			}

			this.layout();
		}
	}

	private void layout() {
		Font font = CFHGuiDraw.font();
		int x = 2;

		for (TreeTabs.Tab t : this.tabs) {
			t.x = x;
			t.w = 8 + t.branch.depth() * 7 + 14 + font.width(Fonts.heading(t.title)) + 6 + (t.open ? font.width(t.count) : 8) + 8;
			x += t.w;
		}

		this.contentW = x + 2;
		TreeTabs.Tab sel = this.selected();
		if (sel != null) {
			if (this.markSet) {
				this.markX.set((float)sel.x);
				this.markW.set((float)sel.w);
			} else {
				this.markX.snap((float)sel.x);
				this.markW.snap((float)sel.w);
				this.markSet = true;
			}

			int view = this.tabsWidth();
			float s = this.scroll.target();
			if ((float)(sel.x - 8) < s) {
				s = (float)(sel.x - 8);
			} else if ((float)(sel.x + sel.w + 8) > s + (float)view) {
				s = (float)(sel.x + sel.w + 8 - view);
			}

			this.scroll.set(Mth.clamp(s, 0.0F, (float)Math.max(0, this.contentW - view)));
		}
	}

	@Nullable
	private TreeTabs.Tab selected() {
		for (TreeTabs.Tab t : this.tabs) {
			if (t.branch.chapter().id.equals(this.shownId)) {
				return t;
			}
		}

		return null;
	}

	public List<Trees.Branch> branches() {
		List<Trees.Branch> out = new ArrayList<>();

		for (TreeTabs.Tab t : this.tabs) {
			out.add(t.branch);
		}

		return out;
	}

	@Nullable
	public Chapter neighbour(int direction) {
		int at = -1;

		for (int i = 0; i < this.tabs.size(); i++) {
			if (this.tabs.get(i).branch.chapter().id.equals(this.shownId)) {
				at = i;
				break;
			}
		}

		for (int ix = at + direction; ix >= 0 && ix < this.tabs.size(); ix += direction) {
			if (this.tabs.get(ix).open) {
				return this.tabs.get(ix).branch.chapter();
			}
		}

		return null;
	}

	private boolean hasInfo() {
		TreeTabs.Tab sel = this.selected();
		return this.onInfo != null && sel != null && !sel.branch.chapter().description.isEmpty();
	}

	private int tabsWidth() {
		int side = this.hasInfo() ? 20 : 0;
		return Math.max(40, Math.min(this.getWidth() - side, Math.max(this.contentW, this.getWidth() * 3 / 5)));
	}

	private int infoX() {
		return this.getX() + this.getWidth() - 14 - 4;
	}

	@Nullable
	private TreeTabs.Tab tabAt(double mx, double my) {
		if (!(my < (double)this.getY())
			&& !(my >= (double)(this.getY() + this.getHeight()))
			&& !(mx < (double)this.getX())
			&& !(mx >= (double)(this.getX() + Math.min(this.tabsWidth(), this.getWidth())))) {
			double cx = mx - (double)this.getX() + (double)this.scroll.get();

			for (TreeTabs.Tab t : this.tabs) {
				if (cx >= (double)t.x && cx < (double)(t.x + t.w)) {
					return t;
				}
			}

			return null;
		} else {
			return null;
		}
	}

	private boolean overInfo(double mx, double my) {
		return this.hasInfo()
			&& mx >= (double)this.infoX()
			&& mx < (double)(this.infoX() + 14)
			&& my >= (double)(this.getY() + 3)
			&& my < (double)(this.getY() + 3 + 14);
	}

	public void onClick(double mouseX, double mouseY) {
		if (this.overInfo(mouseX, mouseY)) {
			CFHGuiSounds.tick();
			if (this.onInfo != null) {
				this.onInfo.run();
			}
		} else {
			TreeTabs.Tab t = this.tabAt(mouseX, mouseY);
			if (t != null && t.open && !t.branch.chapter().id.equals(this.shownId)) {
				CFHGuiSounds.tick();
				this.onPick.accept(t.branch.chapter());
			}
		}
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int max = Math.max(0, this.contentW - this.tabsWidth());
		if (this.contains(mouseX, mouseY) && max > 0) {
			double d = scrollX != 0.0 ? scrollX : scrollY;
			this.scroll.set(Mth.clamp(this.scroll.target() - (float)d * 30.0F, 0.0F, (float)max));
			return true;
		} else {
			return false;
		}
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.isFocused() && (keyCode == 262 || keyCode == 263)) {
			Chapter c = this.neighbour(keyCode == 262 ? 1 : -1);
			if (c != null) {
				CFHGuiSounds.tick();
				this.onPick.accept(c);
			}

			return true;
		} else {
			return false;
		}
	}

	protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY, false);
	}

	protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY, true);
	}

	private void draw(GuiGraphics g, int mouseX, int mouseY, boolean van) {
		Theme t = Theme.current();
		Palette p = t.palette();
		Font font = CFHGuiDraw.font();
		float a = this.effectiveAlpha();
		if (!(a <= 0.004F)) {
			int x = this.getX();
			int y = this.getY();
			int w = this.getWidth();
			int h = this.getHeight();
			if (van) {
				CFHGuiDraw.vanillaSeparator(g, x, y + h - 2, w, true, Minecraft.getInstance().level != null);
			} else {
				CFHGuiDraw.rect(g, x, y, w, h, Colors.scaleAlpha(t.chrome(p.bg2()), a));
				g.fill(x, y + h - 1, x + w, y + h, Colors.scaleAlpha(p.border(), a));
			}

			boolean inside = this.isHovered() && g.containsPointInScissor(mouseX, mouseY);
			TreeTabs.Tab over = inside ? this.tabAt((double)mouseX, (double)mouseY) : null;
			boolean info = inside && this.overInfo((double)mouseX, (double)mouseY);
			if (over != this.hoverTab || info != this.hoverInfo) {
				this.hoverTab = over;
				this.hoverInfo = info;
				this.hoverSinceMs = Clock.nowMs();
			}

			this.infoHover.set(info);
			int view = Math.min(this.tabsWidth(), w);
			int s = Math.round(this.scroll.get());
			TreeTabs.Tab sel = this.selected();
			CFHGuiDraw.scissor(g, x, y, view, h);

			for (TreeTabs.Tab tab : this.tabs) {
				int tx = x + tab.x - s;
				tab.hover.set(tab == over && tab.open && tab != sel);
				if (tx + tab.w >= x && tx <= x + view) {
					float hov = tab.hover.get();
					boolean on = tab == sel;
					if (van) {
						CFHGuiDraw.vanillaTab(g, tx, y, tab.w, h, on, on ? 0.6F : hov * 0.6F, a);
					} else if (hov > 0.01F) {
						CFHGuiDraw.pixelRound(g, tx + 2, y + 2, tab.w - 4, h - 6, Colors.scaleAlpha(p.surfaceHover(), a * hov), t.radius());
					}

					int fg = !tab.open
						? (van ? -8355712 : Colors.withAlpha(p.textDim(), 176))
						: (on ? (van ? -1 : p.text()) : (van ? Colors.lerp(-4144960, -1, hov) : Colors.lerp(p.textDim(), p.textMuted(), Math.max(hov, 0.4F))));
					int ty = y + (van && !on ? 3 : 0);
					int th = h - 2;
					int cx = tx + 8;

					for (int d = 0; d < tab.branch.depth(); d++) {
						Icons.draw(g, Icon.CHEVRON_RIGHT, cx - 1, ty + (th - 6) / 2 + 1, 6, Colors.scaleAlpha(van ? -8355712 : p.textDim(), a));
						cx += 7;
					}

					if (a > 0.5F) {
						tab.icon.draw(g, (float)(cx + 5), (float)(ty + th / 2 + 1), 10.0F, Colors.scaleAlpha(on && !van ? tab.accent : fg, a));
					}

					cx += 14;
					FormattedCharSequence title = Fonts.heading(tab.title).getVisualOrderText();
					g.drawString(font, title, cx, CFHGuiDraw.textY(ty, th), Colors.scaleAlpha(fg, a), van);
					cx += font.width(title) + 6;
					if (!tab.open) {
						Icons.draw(g, Icon.LOCK, cx, ty + (th - 8) / 2 + 1, 8, Colors.scaleAlpha(van ? -8355712 : p.textDim(), a));
					} else {
						int cc = tab.waiting > 0 ? (van ? -22016 : p.warning()) : (tab.stat.complete() ? (van ? -11141291 : p.success()) : (van ? -8355712 : p.textDim()));
						g.drawString(font, tab.count, cx, CFHGuiDraw.textY(ty, th), Colors.scaleAlpha(cc, a), van);
					}
				}
			}

			if (sel != null && !van) {
				int mx = x + Math.round(this.markX.get()) - s;
				int mw = Math.round(this.markW.get());
				CFHGuiDraw.rect(g, mx + 5, y + h - 2, Math.max(2, mw - 10), 2, Colors.scaleAlpha(sel.accent, a));
			}

			CFHGuiDraw.unscissor(g);
			if (!van && this.contentW > view) {
				int shade = Colors.scaleAlpha(t.chrome(p.bg2()), a);
				if (s > 0) {
					CFHGuiDraw.hgradient(g, x, y, 12, h - 1, shade, Colors.withAlpha(shade, 0));
				}

				if (s < this.contentW - view) {
					CFHGuiDraw.hgradient(g, x + view - 12, y, 12, h - 1, Colors.withAlpha(shade, 0), shade);
				}
			}

			int right = this.hasInfo() ? this.infoX() - 4 : x + w - 6;
			int left = x + Math.min(this.contentW, view) + 10;
			if (sel != null && right - left > 40 && !sel.branch.chapter().subtitle.isBlank()) {
				FormattedCharSequence sub = CFHGuiDraw.truncate(QuestText.text(sel.branch.chapter().subtitle), right - left);
				g.drawString(font, sub, right - font.width(sub), CFHGuiDraw.textY(y, h - 2), Colors.scaleAlpha(van ? -6250336 : p.textDim(), a), van);
			}

			if (this.hasInfo()) {
				float hovx = this.infoHover.get();
				int ix = this.infoX();
				int iy = y + 3;
				if (van) {
					CFHGuiDraw.vanillaButton(g, ix, iy, 14, 14, hovx, true, a);
				} else if (hovx > 0.01F) {
					CFHGuiDraw.pixelRound(g, ix, iy, 14, 14, Colors.scaleAlpha(p.surfaceHover(), a * hovx), 2);
				}

				Icons.draw(g, Icon.INFO, ix + 3, iy + 3, 8, Colors.scaleAlpha(van ? -1 : Colors.lerp(p.textDim(), p.text(), hovx), a));
			}

			if (Clock.nowMs() - this.hoverSinceMs > 350L) {
				if (info) {
					CFHGuiTooltips.request(Component.translatable("cfhgui_quests.tabs.about"), this);
				} else if (over != null) {
					CFHGuiTooltips.request(this.tip(over), this);
				}
			}

			if (!van) {
				CFHGuiDraw.focusRing(g, x, y, w, h, this.focus() * a);
			}
		}
	}

	private List<Component> tip(TreeTabs.Tab tab) {
		List<Component> out = new ArrayList<>();
		Chapter c = tab.branch.chapter();
		out.add(Fonts.heading(tab.title));
		if (!tab.open) {
			BookIndex.Entry way = QuestsState.entry(tab.branch.gateway());
			out.add(
				(way == null
						? Component.translatable("cfhgui_quests.tabs.locked")
						: Component.translatable("cfhgui_quests.tabs.locked.by", new Object[]{QuestText.title(way)}))
					.withStyle(s -> s.withColor(14722136))
			);
			return out;
		} else {
			if (!c.subtitle.isBlank()) {
				out.add(QuestText.text(c.subtitle).withStyle(s -> s.withColor(10592151)));
			}

			out.add(Component.translatable("cfhgui_quests.rail.progress", new Object[]{tab.stat.done(), tab.stat.total()}));
			if (tab.waiting > 0) {
				out.add(
					Component.translatable(tab.waiting == 1 ? "cfhgui_quests.rail.waiting.one" : "cfhgui_quests.rail.waiting", new Object[]{tab.waiting})
						.withStyle(s -> s.withColor(14722136))
				);
			}

			return out;
		}
	}

	protected void updateWidgetNarration(NarrationElementOutput out) {
		out.add(NarratedElementType.TITLE, this.getMessage());
	}

	private static final class Tab {
		Trees.Branch branch;
		Component title = Component.empty();
		QuestIcons.Drawable icon = QuestIcons.of((String)null);
		boolean open;
		BookStats.Stat stat = BookStats.Stat.NONE;
		int waiting;
		int accent;
		int x;
		int w;
		String count = "";
		final Anim hover = new Anim(0.0F, 140, Ease.OUT_CUBIC);
	}
}
