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
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

public final class ChapterRail extends CFHGuiWidget {
	public static final int WIDTH = 152;
	public static final int COMPACT = 34;
	private static final int HEAD_H = 52;
	private static final int ROW_H = 26;
	private static final int GROUP_H = 18;
	private static final Set<String> FOLDED = new HashSet<>();
	private final Consumer<Chapter> onPick;
	private final List<ChapterRail.Row> rows = new ArrayList<>();
	private final Map<String, ChapterRail.Row> byId = new HashMap<>();
	@Nullable
	private String selectedId;
	private final Anim scroll = new Anim(0.0F, 180, Ease.OUT_CUBIC);
	private final Anim highlight = new Anim(0.0F, 220, Ease.OUT_CUBIC);
	private final Anim bookFill = new Anim(0.0F, 700, Ease.OUT_CUBIC);
	private boolean highlightSet;
	private int contentH;
	@Nullable
	private ChapterRail.Row hoverRow;
	private long hoverSinceMs;
	private long shownAtMs;
	private BookStats.Stat book = BookStats.Stat.NONE;
	private int waitingAll;

	public ChapterRail(Consumer<Chapter> onPick) {
		super(0, 0, 152, 100, Component.translatable("cfhgui_quests.rail"));
		this.onPick = onPick;
		this.silent();
	}

	public static int widthFor(int screenWidth) {
		return screenWidth < 500 ? 34 : (screenWidth < 580 ? 128 : 152);
	}

	public void place(int x, int y, int w, int h) {
		this.setX(x);
		this.setY(y);
		this.setWidth(w);
		this.setHeight(h);
		this.scroll.snap(Mth.clamp(this.scroll.target(), 0.0F, (float)this.maxScroll()));
	}

	private boolean compact() {
		return this.getWidth() <= 40;
	}

	public void refresh() {
		QuestBook b = QuestsState.book();
		Map<String, ChapterRail.Row> old = new HashMap<>(this.byId);
		this.rows.clear();
		this.byId.clear();
		this.book = BookStats.book();
		this.waitingAll = this.book.claimable();
		this.bookFill.set(this.book.fraction());
		if (b == null) {
			this.contentH = 0;
		} else {
			if (this.shownAtMs == 0L) {
				this.shownAtMs = Clock.nowMs();
			}

			String group = null;
			ChapterRail.Row header = null;
			boolean folded = false;
			int y = 4;

			for (Chapter c : b.topChapters()) {
				boolean open = BookStats.open(c);
				if (!c.hidden || open) {
					String g = c.group == null ? "" : c.group;
					if (!g.equals(group == null ? "" : group) && !g.isEmpty()) {
						group = g;
						folded = FOLDED.contains(g);
						ChapterRail.Row r = old.getOrDefault("group:" + g, new ChapterRail.Row());
						r.chapter = null;
						r.id = "group:" + g;
						r.group = b.group(g);
						r.title = r.group == null ? Component.literal(g) : QuestText.text(r.group.title);
						r.icon = QuestIcons.of(r.group == null ? null : r.group.icon);
						r.folded = folded;
						r.count = 0;
						r.waiting = 0;
						r.y = y + 4;
						r.h = 18;
						y += 22;
						this.rows.add(r);
						this.byId.put(r.id, r);
						header = r;
					} else if (g.isEmpty()) {
						group = null;
						folded = false;
						header = null;
					}

					BookStats.Stat deep = BookStats.deep(c);
					if (header != null) {
						header.count++;
						header.waiting = header.waiting + deep.claimable();
					}

					if (!folded) {
						ChapterRail.Row r = old.getOrDefault(c.id, new ChapterRail.Row());
						r.chapter = c;
						r.id = c.id;
						r.title = QuestText.title(c);
						r.icon = QuestIcons.of(c.icon);
						r.open = open;
						r.stat = BookStats.of(c);
						r.waiting = deep.claimable();
						r.accent = QuestPainter.accentOf(c);
						r.fill.set(r.stat.fraction());
						r.tip = null;
						r.y = y;
						r.h = 26;
						y += 26;
						this.rows.add(r);
						this.byId.put(r.id, r);
					}
				}
			}

			this.contentH = y + 4;
			this.scroll.set(Mth.clamp(this.scroll.target(), 0.0F, (float)this.maxScroll()));
			this.select(this.selectedId);
		}
	}

	public void select(@Nullable String chapterId) {
		this.selectedId = chapterId;
		ChapterRail.Row r = chapterId == null ? null : this.byId.get(chapterId);
		if (r != null && r.chapter != null) {
			if (this.highlightSet) {
				this.highlight.set((float)r.y);
			} else {
				this.highlight.snap((float)r.y);
				this.highlightSet = true;
			}

			int view = this.listHeight();
			if ((float)r.y < this.scroll.target()) {
				this.scroll.set((float)Math.max(0, r.y - 4));
			} else if ((float)(r.y + r.h) > this.scroll.target() + (float)view) {
				this.scroll.set((float)Math.min(this.maxScroll(), r.y + r.h - view + 4));
			}
		}
	}

	@Nullable
	public Chapter neighbour(int direction) {
		int at = -1;

		for (int i = 0; i < this.rows.size(); i++) {
			if (this.rows.get(i).chapter != null && this.rows.get(i).id.equals(this.selectedId)) {
				at = i;
				break;
			}
		}

		for (int ix = at + direction; ix >= 0 && ix < this.rows.size(); ix += direction) {
			ChapterRail.Row r = this.rows.get(ix);
			if (r.chapter != null && r.open) {
				return r.chapter;
			}
		}

		return null;
	}

	private int listTop() {
		return this.getY() + (this.compact() ? 34 : 52);
	}

	private int listHeight() {
		return Math.max(1, this.getY() + this.getHeight() - this.listTop());
	}

	private int maxScroll() {
		return Math.max(0, this.contentH - this.listHeight());
	}

	@Nullable
	private ChapterRail.Row rowAt(double mx, double my) {
		if (!(mx < (double)this.getX())
			&& !(mx >= (double)(this.getX() + this.getWidth()))
			&& !(my < (double)this.listTop())
			&& !(my >= (double)(this.getY() + this.getHeight()))) {
			double cy = my - (double)this.listTop() + (double)this.scroll.get();

			for (ChapterRail.Row r : this.rows) {
				if (cy >= (double)r.y && cy < (double)(r.y + r.h)) {
					return r;
				}
			}

			return null;
		} else {
			return null;
		}
	}

	private List<Component> tip(ChapterRail.Row r) {
		if (r.tip != null) {
			return r.tip;
		} else {
			List<Component> out = new ArrayList<>();
			Chapter c = r.chapter;
			if (c == null) {
				return out;
			} else {
				out.add(Fonts.heading(r.title));
				if (!c.subtitle.isBlank()) {
					out.add(QuestText.text(c.subtitle).withStyle(s -> s.withColor(10592151)));
				}

				if (!r.open) {
					out.add(Component.translatable("cfhgui_quests.rail.locked").withStyle(s -> s.withColor(14722136)));
					BookIndex index = QuestsState.index();
					int listed = 0;

					for (String need : c.requires) {
						String key = Ids.resolve(c.id, need);
						BookIndex.Entry e = index == null ? null : index.get(key);
						if (e != null && !QuestsState.progress().everDone(key)) {
							if (listed++ >= 5) {
								out.add(Component.literal("…"));
								break;
							}

							out.add(
								Component.literal("• ")
									.append(QuestText.title(e))
									.append(Component.literal("  (").append(QuestText.title(e.chapter())).append(")").withStyle(s -> s.withColor(7236710)))
							);
						}
					}
				} else {
					for (int i = 0; i < c.description.size() && i < 2; i++) {
						String line = c.description.get(i);
						if (!line.isBlank()) {
							out.add(Component.literal(line.length() > 160 ? line.substring(0, 157) + "…" : line).withStyle(s -> s.withColor(10592151)));
						}
					}

					out.add(Component.translatable("cfhgui_quests.rail.progress", new Object[]{r.stat.done(), r.stat.total()}));
					if (r.waiting > 0) {
						out.add(
							Component.translatable(r.waiting == 1 ? "cfhgui_quests.rail.waiting.one" : "cfhgui_quests.rail.waiting", new Object[]{r.waiting})
								.withStyle(s -> s.withColor(14722136))
						);
					}
				}

				r.tip = out;
				return out;
			}
		}
	}

	public void onClick(double mouseX, double mouseY) {
		ChapterRail.Row r = this.rowAt(mouseX, mouseY);
		if (r != null) {
			if (r.chapter == null) {
				String g = r.id.substring("group:".length());
				if (!FOLDED.remove(g)) {
					FOLDED.add(g);
				}

				CFHGuiSounds.tick();
				this.refresh();
			} else if (r.open) {
				CFHGuiSounds.tick();
				if (!r.id.equals(this.selectedId)) {
					this.onPick.accept(r.chapter);
				}
			}
		}
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.contains(mouseX, mouseY) && this.maxScroll() > 0) {
			this.scroll.set(Mth.clamp(this.scroll.target() - (float)scrollY * 26.0F, 0.0F, (float)this.maxScroll()));
			return true;
		} else {
			return false;
		}
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.isFocused() && (keyCode == 264 || keyCode == 265)) {
			Chapter c = this.neighbour(keyCode == 264 ? 1 : -1);
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
			boolean compact = this.compact();
			if (van) {
				CFHGuiDraw.vanillaListBackground(g, x, y, w, h, Minecraft.getInstance().level != null);
				CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(-16777216, a), 0);
			} else {
				CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(t.chrome(p.bg2()), a), t.radius());
				CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(p.border(), a), t.radius());
			}

			this.head(g, van, compact, a);
			ChapterRail.Row over = this.isHovered() && g.containsPointInScissor(mouseX, mouseY) ? this.rowAt((double)mouseX, (double)mouseY) : null;
			if (over != this.hoverRow) {
				this.hoverRow = over;
				this.hoverSinceMs = Clock.nowMs();
			}

			int top = this.listTop();
			int listH = this.listHeight();
			int s = Math.round(this.scroll.get());
			CFHGuiDraw.scissor(g, x + 1, top, w - 2, listH - 1);
			ChapterRail.Row sel = this.selectedId == null ? null : this.byId.get(this.selectedId);
			if (sel != null && sel.chapter != null) {
				int hy = top + Math.round(this.highlight.get()) - s;
				if (van) {
					g.fill(x + 3, hy, x + w - 3, hy + 26 - 2, Colors.scaleAlpha(1358954495, a));
					CFHGuiDraw.outline(g, x + 3, hy, w - 6, 24, Colors.scaleAlpha(-1, a), 0);
				} else {
					CFHGuiDraw.pixelRound(g, x + 4, hy, w - 8, 24, Colors.scaleAlpha(p.surfaceActive(), a), t.radius());
					g.fill(x + 4, hy + 5, x + 6, hy + 26 - 7, Colors.scaleAlpha(sel.accent, a));
				}
			}

			long now = Clock.nowMs();
			float motion = t.motion();

			for (int i = 0; i < this.rows.size(); i++) {
				ChapterRail.Row r = this.rows.get(i);
				int ry = top + r.y - s;
				r.hover.set(r == over && (r.chapter == null || r.open));
				if (ry + r.h >= top && ry <= top + listH) {
					float in = motion <= 0.0F ? 1.0F : Mth.clamp(((float)(now - this.shownAtMs) - (float)i * 24.0F * motion) / (180.0F * motion), 0.0F, 1.0F);
					float ra = a * in;
					int slide = Math.round((1.0F - Ease.OUT_CUBIC.apply(in)) * -8.0F);
					if (r.chapter == null) {
						this.group(g, r, x + slide, ry, w, van, compact, ra);
					} else {
						this.chapter(g, r, x + slide, ry, w, van, compact, r == sel, ra);
					}
				}
			}

			CFHGuiDraw.unscissor(g);
			if (this.maxScroll() > 0) {
				int barH = Math.max(12, listH * listH / Math.max(1, this.contentH));
				int barY = top + Math.round((float)(listH - barH) * (this.scroll.get() / (float)this.maxScroll()));
				g.fill(x + w - 3, barY, x + w - 1, barY + barH, Colors.scaleAlpha(van ? -4144960 : p.borderStrong(), a));
			}

			if (over != null && over.chapter != null && Clock.nowMs() - this.hoverSinceMs > 350L) {
				CFHGuiTooltips.request(this.tip(over), this);
			} else if (over != null && over.chapter == null && compact && Clock.nowMs() - this.hoverSinceMs > 350L) {
				CFHGuiTooltips.request(over.title, this);
			}

			if (!van) {
				CFHGuiDraw.focusRing(g, x, y, w, h, this.focus() * a);
			}
		}
	}

	private void head(GuiGraphics g, boolean van, boolean compact, float a) {
		Theme t = Theme.current();
		Palette p = t.palette();
		Font font = CFHGuiDraw.font();
		int x = this.getX();
		int y = this.getY();
		int w = this.getWidth();
		QuestBook b = QuestsState.book();
		float frac = this.bookFill.get();
		int accent = van ? -8399233 : t.accent();
		int track = van ? -16777216 : p.surfaceActive();
		int cx = compact ? x + w / 2 : x + 21;
		int cy = compact ? y + 17 : y + 23;
		int r = compact ? 11 : 14;
		UiBits.ring(g, cx, cy, r, frac, track, this.book.complete() ? (van ? -11141291 : p.success()) : accent, a);
		String percent = Math.round(frac * 100.0F) + "%";
		if (!compact) {
			g.drawString(font, percent, cx - font.width(percent) / 2 + 1, cy - 4, Colors.scaleAlpha(van ? -1 : p.text(), a), van);
		} else if (this.waitingAll > 0) {
			CFHGuiDraw.pixelCircle(g, cx + r - 1, cy - r + 1, 2, Colors.scaleAlpha(van ? -22016 : p.warning(), a));
		}

		if (!compact && b != null) {
			int tx = x + 42;
			int tw = w - 42 - 6;
			g.drawString(font, CFHGuiDraw.truncate(Fonts.heading(QuestText.title(b)), tw), tx, y + 9, Colors.scaleAlpha(van ? -1 : p.text(), a), van);
			g.drawString(
				font,
				CFHGuiDraw.truncate(Component.translatable("cfhgui_quests.rail.progress", new Object[]{this.book.done(), this.book.total()}), tw),
				tx,
				y + 21,
				Colors.scaleAlpha(van ? -6250336 : p.textMuted(), a),
				van
			);
			if (this.waitingAll > 0) {
				Component waiting = Component.translatable(
					this.waitingAll == 1 ? "cfhgui_quests.rail.waiting.one" : "cfhgui_quests.rail.waiting", new Object[]{this.waitingAll}
				);
				g.drawString(font, CFHGuiDraw.truncate(waiting, tw), tx, y + 33, Colors.scaleAlpha(van ? -22016 : p.warning(), a), van);
			} else if (!b.subtitle.isBlank()) {
				CFHGuiDraw.drawScrollingText(g, QuestText.text(b.subtitle), tx, y + 33, tw, Colors.scaleAlpha(van ? -8355712 : p.textDim(), a), van);
			}
		}

		int line = this.listTop() - 1;
		g.fill(x + 4, line, x + w - 4, line + 1, Colors.scaleAlpha(van ? -16777216 : p.border(), a));
	}

	private void group(GuiGraphics g, ChapterRail.Row r, int x, int y, int w, boolean van, boolean compact, float a) {
		Theme t = Theme.current();
		Palette p = t.palette();
		Font font = CFHGuiDraw.font();
		float hov = r.hover.get();
		int fg = Colors.scaleAlpha(van ? Colors.lerp(-4144960, -96, hov) : Colors.lerp(p.textDim(), p.textMuted(), hov), a);
		if (compact) {
			g.fill(x + 8, y + r.h / 2, x + w - 8, y + r.h / 2 + 1, fg);
		} else {
			Icons.draw(g, r.folded ? Icon.CHEVRON_RIGHT : Icon.CHEVRON_DOWN, x + 7, y + 5, 8, fg);
			FormattedCharSequence title = CFHGuiDraw.truncate(Fonts.heading(r.title), w - 50);
			g.drawString(font, title, x + 18, y + 5, fg, van);
			int lx = x + 18 + font.width(title) + 5;
			if (r.folded) {
				String n = Integer.toString(r.count);
				g.drawString(font, n, lx, y + 5, Colors.scaleAlpha(van ? -8355712 : p.textDim(), a), van);
				lx += font.width(n) + 5;
				if (r.waiting > 0) {
					CFHGuiDraw.pixelCircle(g, lx + 2, y + 9, 2, Colors.scaleAlpha(van ? -22016 : p.warning(), a));
					lx += 9;
				}
			}

			g.fill(lx, y + 9, Math.max(lx, x + w - 8), y + 10, Colors.scaleAlpha(van ? -11513776 : p.border(), a));
		}
	}

	private void chapter(GuiGraphics g, ChapterRail.Row r, int x, int y, int w, boolean van, boolean compact, boolean selected, float a) {
		Theme t = Theme.current();
		Palette p = t.palette();
		Font font = CFHGuiDraw.font();
		float hov = r.hover.get();
		int rh = 24;
		if (hov > 0.01F && !selected) {
			if (van) {
				g.fill(x + 3, y, x + w - 3, y + 24, Colors.scaleAlpha(822083583, a * hov));
			} else {
				CFHGuiDraw.pixelRound(g, x + 4, y, w - 8, 24, Colors.scaleAlpha(p.surfaceHover(), a * hov), t.radius());
			}
		}

		boolean done = r.stat.complete();
		int text = !r.open
			? (van ? -8355712 : p.textDim())
			: (selected ? (van ? -1 : p.text()) : (van ? Colors.lerp(-3092272, -1, hov) : Colors.lerp(p.textMuted(), p.text(), hov)));
		int ix = compact ? x + w / 2 : x + 17;
		int iy = y + 12;
		if (a > 0.5F) {
			r.icon.draw(g, (float)ix, compact ? (float)(iy - 2) : (float)iy, 16.0F, Colors.scaleAlpha(text, a));
		}

		int bar = !r.open ? (van ? -11513776 : p.borderStrong()) : (done ? (van ? -11141291 : p.success()) : (van ? -8399233 : r.accent));
		int track = van ? -16777216 : (selected ? Colors.mix(p.surfaceActive(), -16777216, 0.3F) : p.surfaceActive());
		if (compact) {
			UiBits.bar(g, x + 7, y + 24 - 3, w - 14, 2, r.fill.get(), track, bar, a);
			if (r.waiting > 0) {
				CFHGuiDraw.pixelCircle(g, x + w - 8, y + 4, 2, Colors.scaleAlpha(van ? -22016 : p.warning(), a));
			}

			if (!r.open) {
				lock(g, x + w - 11, y + 24 - 11, a, van);
			}
		} else {
			int right = x + w - 8;
			int markW;
			if (!r.open) {
				lock(g, right - 6, y + 3, a, van);
				markW = 10;
			} else if (r.waiting > 0) {
				String n = r.waiting > 99 ? "99+" : Integer.toString(r.waiting);
				markW = Math.max(11, font.width(n) + 6);
				int c = van ? -22016 : p.warning();
				CFHGuiDraw.pixelRound(g, right - markW, y + 2, markW, 11, Colors.scaleAlpha(c, a), 3);
				g.drawString(font, n, right - markW + (markW - font.width(n) + 1) / 2, y + 4, Colors.scaleAlpha(-15461357, a), false);
			} else if (done) {
				Icons.draw(g, Icon.CHECK, right - 9, y + 3, 8, Colors.scaleAlpha(van ? -11141291 : p.success(), a));
				markW = 10;
			} else {
				String n = r.stat.done() + "/" + r.stat.total();
				markW = font.width(n);
				g.drawString(font, n, right - markW, y + 4, Colors.scaleAlpha(van ? -8355712 : p.textDim(), a), van);
			}

			int tx = x + 30;
			int tw = right - markW - 5 - tx;
			g.drawString(font, CFHGuiDraw.truncate(r.title, tw), tx, y + 4, Colors.scaleAlpha(text, a), van);
			UiBits.bar(g, tx, y + 16, right - tx, 2, r.fill.get(), track, bar, a);
		}
	}

	private static void lock(GuiGraphics g, int x, int y, float a, boolean van) {
		Icons.draw(g, Icon.LOCK, x, y, 8, Colors.scaleAlpha(van ? -8355712 : Theme.current().palette().textDim(), a));
	}

	protected void updateWidgetNarration(NarrationElementOutput out) {
		out.add(NarratedElementType.TITLE, this.getMessage());
	}

	private static final class Row {
		@Nullable
		Chapter chapter;
		String id = "";
		@Nullable
		QuestBook.Group group;
		Component title = Component.empty();
		QuestIcons.Drawable icon = QuestIcons.of((String)null);
		int y;
		int h;
		int count;
		boolean open;
		boolean folded;
		BookStats.Stat stat = BookStats.Stat.NONE;
		int waiting;
		int accent;
		final Anim hover = new Anim(0.0F, 140, Ease.OUT_CUBIC);
		final Anim fill = new Anim(0.0F, 520, Ease.OUT_CUBIC);
		@Nullable
		List<Component> tip;
	}
}
