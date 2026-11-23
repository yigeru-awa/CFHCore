package com.onehumanawa.cfhcore.content.gui.quests.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.BookStats;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CanvasDraw;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.GraphModel;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestPainter;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CFHGuiQuestPainter;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.TreeThumb;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class SheetParts {
	static void slot(GuiGraphics g, int x, int y, boolean van, float a) {
		Palette p = Theme.current().palette();
		if (van) {
			g.fill(x, y, x + 18, y + 18, Colors.scaleAlpha(-7631989, a));
			g.fill(x, y, x + 17, y + 1, Colors.scaleAlpha(-13158601, a));
			g.fill(x, y, x + 1, y + 17, Colors.scaleAlpha(-13158601, a));
			g.fill(x + 1, y + 17, x + 18, y + 18, Colors.scaleAlpha(-1, a));
			g.fill(x + 17, y + 1, x + 18, y + 18, Colors.scaleAlpha(-1, a));
		} else {
			CFHGuiDraw.pixelRound(g, x, y, 18, 18, Colors.scaleAlpha(Colors.mix(p.bg(), -16777216, 0.25F), a), 2);
			CFHGuiDraw.outline(g, x, y, 18, 18, Colors.scaleAlpha(p.border(), a), 2);
		}
	}

	static int flow(List<SheetParts.QuestChip> chips, int width, int top, SheetParts.Placer place) {
		int x = 0;
		int y = top;

		for (SheetParts.QuestChip c : chips) {
			if (x > 0 && x + c.getWidth() > width) {
				x = 0;
				y += 17;
			}

			place.at(c, x, y);
			x += c.getWidth() + 4;
		}

		return chips.isEmpty() ? top : y + 14;
	}

	static List<SheetParts.Tags.Tag> tags() {
		return new ArrayList<>();
	}

	static int dot(Rules.Status status, int accent) {
		return UiBits.statusColor(status, accent);
	}

	private SheetParts() {
	}

	static final class Caption extends SheetParts.Still {
		@Nullable
		private final Component note;

		Caption(int w, Component title, @Nullable Component note) {
			super(w, 12, title);
			this.note = note;
			this.active = false;
		}

		protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			this.draw(g, false);
		}

		protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			this.draw(g, true);
		}

		private void draw(GuiGraphics g, boolean van) {
			float a = this.effectiveAlpha();
			int y = this.getY() + this.enterOffset();
			int noteW = this.note == null ? 0 : CFHGuiDraw.width(this.note) + 6;
			CFHGuiDraw.sectionRule(g, this.getMessage(), this.getX(), y, this.getWidth() - noteW, a);
			if (this.note != null) {
				CFHGuiDraw.textRight(g, this.note, this.getX() + this.getWidth(), y + 1, Colors.scaleAlpha(van ? -6250336 : Theme.current().palette().textDim(), a), van);
			}
		}
	}

	static final class Fact extends SheetParts.Still {
		private static final int MAX_LINES = 3;
		@Nullable
		private final Icon icon;
		@Nullable
		private final ResourceLocation face;
		private final Supplier<Component> words;
		private final int color;
		private final int textX;
		private final List<FormattedCharSequence> lines = new ArrayList<>();
		private String laidOut = "";
		private final int maxLines;

		Fact(int w, @Nullable Icon icon, @Nullable ResourceLocation face, int color, Supplier<Component> words) {
			super(w, 12, Component.empty());
			this.icon = icon;
			this.face = face;
			this.words = words;
			this.color = color;
			this.active = false;
			this.textX = face == null && icon == null ? 0 : 12;
			this.lay(words.get(), 3);
			this.maxLines = Math.max(1, this.lines.size());
			this.setHeight(this.lines.size() * 10 + 2);
		}

		private void lay(Component c, int max) {
			this.laidOut = c.getString();
			this.lines.clear();
			List<FormattedCharSequence> all = CFHGuiDraw.font().split(c, Math.max(30, this.getWidth() - this.textX));

			for (int i = 0; i < all.size() && i < max; i++) {
				this.lines.add(all.get(i));
			}

			if (all.size() > max && !this.lines.isEmpty()) {
				this.lines.set(this.lines.size() - 1, CFHGuiDraw.truncate(c, Math.max(30, this.getWidth() - this.textX)));
			}
		}

		protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			this.draw(g, false);
		}

		protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			this.draw(g, true);
		}

		private void draw(GuiGraphics g, boolean van) {
			float a = this.effectiveAlpha();
			if (!(a <= 0.004F)) {
				Component c0 = this.words.get();
				if (!this.laidOut.equals(c0.getString())) {
					this.lay(c0, this.maxLines);
				}

				int x = this.getX();
				int y = this.getY() + this.enterOffset();
				int c = this.color != 0 ? this.color : (van ? -6250336 : Theme.current().palette().textMuted());
				if (this.face != null) {
					CFHGuiDraw.playerHead(g, this.face, x, y + 1, 8, a);
				} else if (this.icon != null) {
					Icons.draw(g, this.icon, x, y + 1, 8, Colors.scaleAlpha(c, a));
				}

				int ty = y + 1;

				for (FormattedCharSequence line : this.lines) {
					g.drawString(CFHGuiDraw.font(), line, x + this.textX, ty, Colors.scaleAlpha(c, a), van);
					ty += 10;
				}
			}
		}
	}

	interface Placer {
		void at(SheetParts.QuestChip var1, int var2, int var3);
	}

	static final class QuestChip extends CFHGuiWidget {
		static final int HEIGHT = 14;
		private final int dot;
		private final Runnable onPress;
		private final FormattedCharSequence label;

		QuestChip(Component title, int maxWidth, int dot, Runnable onPress) {
			super(0, 0, 10, 14, title);
			this.dot = dot;
			this.onPress = onPress;
			this.label = CFHGuiDraw.truncate(title, Math.max(20, maxWidth - 18));
			this.setWidth(CFHGuiDraw.width(this.label) + 18);
		}

		public void onClick(double mouseX, double mouseY) {
			super.onClick(mouseX, mouseY);
			this.onPress.run();
		}

		public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			if (keyCode != 257 && keyCode != 32 && keyCode != 335) {
				return false;
			} else {
				CFHGuiSounds.click();
				this.flashPress();
				this.onPress.run();
				return true;
			}
		}

		protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			Theme t = Theme.current();
			Palette p = t.palette();
			float a = this.effectiveAlpha();
			float hov = this.hover();
			if (!(a <= 0.004F)) {
				int x = this.getX();
				int y = this.getY() + this.enterOffset() + Math.round(this.press());
				int w = this.getWidth();
				CFHGuiDraw.pixelRound(g, x, y, w, 14, Colors.scaleAlpha(Colors.lerp(p.surface(), p.surfaceHover(), hov), a), 3);
				CFHGuiDraw.outline(g, x, y, w, 14, Colors.scaleAlpha(Colors.lerp(p.border(), this.dot, hov * 0.8F), a), 3);
				CFHGuiDraw.pixelCircle(g, x + 7, y + 7, 2, Colors.scaleAlpha(this.dot, a));
				g.drawString(CFHGuiDraw.font(), this.label, x + 13, y + 3, Colors.scaleAlpha(Colors.lerp(p.textMuted(), p.text(), hov), a), false);
				CFHGuiDraw.focusRing(g, x, y, w, 14, this.focus() * a);
			}
		}

		protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			float a = this.effectiveAlpha();
			float hov = Math.max(this.hover(), this.focus());
			if (!(a <= 0.004F)) {
				int x = this.getX();
				int y = this.getY() + this.enterOffset() + Math.round(this.press());
				int w = this.getWidth();
				CFHGuiDraw.vanillaButton(g, x, y, w, 14, hov, true, a);
				g.fill(x + 5, y + 5, x + 9, y + 9, Colors.scaleAlpha(this.dot, a));
				g.drawString(CFHGuiDraw.font(), this.label, x + 13, y + 3, Colors.scaleAlpha(-1, a), true);
			}
		}
	}

	static final class RewardRow extends SheetParts.Still {
		private final QuestIcons.Drawable icon;
		private final List<FormattedCharSequence> label;
		@Nullable
		private final Component note;
		private final boolean claimed;
		private final boolean waiting;
		private final int reserve;
		private final Supplier<List<Component>> tip;

		RewardRow(
			int w, QuestIcons.Drawable icon, Component words, @Nullable Component note, boolean claimed, boolean waiting, int reserve, Supplier<List<Component>> tip
		) {
			super(w, 22, words);
			this.icon = icon;
			this.note = note;
			this.claimed = claimed;
			this.waiting = waiting;
			this.reserve = reserve;
			this.tip = tip;
			List<FormattedCharSequence> lines = CFHGuiDraw.font().split(words, Math.max(40, w - 24 - reserve - (claimed ? 12 : 0)));
			this.label = lines.size() > 2 ? lines.subList(0, 2) : lines;
			this.setHeight(Math.max(22, (this.label.size() + (note == null ? 0 : 1)) * 10 + 4));
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
				int y = this.getY() + this.enterOffset();
				int w = this.getWidth() - this.reserve;
				SheetParts.slot(g, x, y + 1, van, a);
				if (this.waiting && !van) {
					CFHGuiDraw.outline(
						g,
						x,
						y + 1,
						18,
						18,
						Colors.scaleAlpha(Colors.withAlpha(p.warning(), Math.round(144.0F + 96.0F * (0.5F + 0.5F * (float)Math.sin((double)Clock.nowMs() / 260.0)))), a),
						2
					);
				}

				if (a > 0.5F) {
					this.icon.draw(g, (float)(x + 9), (float)(y + 10), 16.0F, Colors.scaleAlpha(van ? -1 : p.textMuted(), a));
				}

				int text = this.claimed ? (van ? -6250336 : p.textDim()) : (van ? -1 : p.text());
				int ty = y + (this.label.size() + (this.note == null ? 0 : 1) == 1 ? 6 : 2);

				for (FormattedCharSequence line : this.label) {
					g.drawString(font, line, x + 24, ty, Colors.scaleAlpha(text, a), van);
					ty += 10;
				}

				if (this.note != null) {
					g.drawString(font, CFHGuiDraw.truncate(this.note, w - 24), x + 24, ty, Colors.scaleAlpha(van ? -8355712 : p.textDim(), a), van);
				}

				if (this.claimed) {
					g.pose().pushPose();
					g.pose().translate(0.0F, 0.0F, 200.0F);
					g.fill(x + 1, y + 2, x + 17, y + 18, Colors.scaleAlpha(van ? -2138338421 : Colors.withAlpha(p.bg(), 160), a));
					g.pose().popPose();
					Icons.draw(g, Icon.CHECK, x + w - 9, y + 7, 8, Colors.scaleAlpha(van ? -11141291 : p.success(), a));
				}

				if (this.over(g, mouseX, mouseY) && mouseX < x + w) {
					List<Component> lines = this.tip.get();
					if (!lines.isEmpty()) {
						CFHGuiTooltips.request(lines, this);
					}
				}
			}
		}
	}

	private abstract static class Still extends CFHGuiWidget {
		Still(int w, int h, Component message) {
			super(0, 0, w, h, message);
			this.silent();
			this.active = false;
		}

		public ComponentPath nextFocusPath(FocusNavigationEvent event) {
			return null;
		}

		public void onClick(double mouseX, double mouseY) {
		}

		protected void updateWidgetNarration(NarrationElementOutput out) {
			out.add(NarratedElementType.TITLE, this.getMessage());
		}

		boolean over(GuiGraphics g, int mouseX, int mouseY) {
			return this.isHovered() && g.containsPointInScissor(mouseX, mouseY);
		}
	}

	static final class Tags extends SheetParts.Still {
		private final List<SheetParts.Tags.Tag> tags;
		private final int[] xs;
		private final int[] ys;
		private final int[] ws;

		Tags(int w, List<SheetParts.Tags.Tag> tags) {
			super(w, 12, Component.empty());
			this.tags = tags;
			this.xs = new int[tags.size()];
			this.ys = new int[tags.size()];
			this.ws = new int[tags.size()];
			int x = 0;
			int y = 0;

			for (int i = 0; i < tags.size(); i++) {
				this.ws[i] = CFHGuiDraw.width(tags.get(i).label()) + 8;
				if (x > 0 && x + this.ws[i] > w) {
					x = 0;
					y += 15;
				}

				this.xs[i] = x;
				this.ys[i] = y;
				x += this.ws[i] + 4;
			}

			this.setHeight(y + 12);
		}

		protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			this.draw(g, mouseX, mouseY);
		}

		protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			this.draw(g, mouseX, mouseY);
		}

		private void draw(GuiGraphics g, int mouseX, int mouseY) {
			float a = this.effectiveAlpha();
			if (!(a <= 0.004F)) {
				int x0 = this.getX();
				int y0 = this.getY() + this.enterOffset();
				boolean over = this.over(g, mouseX, mouseY);

				for (int i = 0; i < this.tags.size(); i++) {
					SheetParts.Tags.Tag tag = this.tags.get(i);
					CFHGuiDraw.chip(g, tag.label(), x0 + this.xs[i], y0 + this.ys[i], tag.color(), a);
					if (over
						&& tag.tip() != null
						&& mouseX >= x0 + this.xs[i]
						&& mouseX < x0 + this.xs[i] + this.ws[i]
						&& mouseY >= y0 + this.ys[i]
						&& mouseY < y0 + this.ys[i] + 12) {
						CFHGuiTooltips.request(tag.tip(), this);
					}
				}
			}
		}

		static record Tag(Component label, int color, @Nullable Component tip) {
		}
	}

	static final class TaskRow extends SheetParts.Still {
		private final QuestIcons.Drawable icon;
		private final List<FormattedCharSequence> label;
		private final String count;
		private final boolean done;
		private final boolean locked;
		private final int reserve;
		private final Anim fill = new Anim(0.0F, 480, Ease.OUT_CUBIC);
		private final long movedAtMs;
		private final List<Component> tip;

		TaskRow(
			int w, QuestIcons.Drawable icon, Component words, String count, float from, float fraction, boolean done, boolean locked, int reserve, List<Component> tip
		) {
			super(w, 22, words);
			this.icon = icon;
			this.count = count;
			this.done = done;
			this.locked = locked;
			this.reserve = reserve;
			this.tip = tip;
			int countW = (count.isEmpty() ? 0 : CFHGuiDraw.width(count) + 6) + (done ? 11 : 0);
			List<FormattedCharSequence> lines = CFHGuiDraw.font().split(words, Math.max(40, w - 24 - reserve - countW));
			this.label = lines.size() > 2 ? lines.subList(0, 2) : lines;
			this.fill.snap(from);
			this.fill.set(fraction);
			this.movedAtMs = fraction > from + 0.001F ? Clock.nowMs() : 0L;
			this.setHeight(Math.max(22, this.label.size() * 10 + 8));
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
				int y = this.getY() + this.enterOffset();
				int w = this.getWidth() - this.reserve;
				SheetParts.slot(g, x, y + 1, van, a);
				if (a > 0.5F) {
					this.icon.draw(g, (float)(x + 9), (float)(y + 10), 16.0F, Colors.scaleAlpha(van ? -1 : (this.locked ? p.textDim() : p.textMuted()), a));
				}

				int good = van ? -11141291 : p.success();
				int text = this.done ? good : (this.locked ? (van ? -6250336 : p.textDim()) : (van ? -1 : p.text()));
				int ty = y + 2;

				for (FormattedCharSequence line : this.label) {
					g.drawString(font, line, x + 24, ty, Colors.scaleAlpha(text, a), van);
					ty += 10;
				}

				if (this.done) {
					Icons.draw(g, Icon.CHECK, x + w - 9, y + 2, 8, Colors.scaleAlpha(good, a));
				}

				if (!this.count.isEmpty()) {
					g.drawString(
						font,
						this.count,
						x + w - font.width(this.count) - (this.done ? 11 : 0),
						y + 2,
						Colors.scaleAlpha(this.done ? good : (van ? -6250336 : p.textMuted()), a),
						van
					);
				}

				float f = this.fill.get();
				long since = this.movedAtMs == 0L ? 9999L : Clock.nowMs() - this.movedAtMs;
				float flash = since < 700L && t.motion() > 0.0F ? 1.0F - (float)since / 700.0F : 0.0F;
				int lit = this.done ? good : (van ? -8399233 : t.accent());
				UiBits.bar(g, x + 24, ty + 1, w - 24, 2, f, van ? -16777216 : p.surfaceActive(), Colors.lerp(lit, -1, flash * 0.7F), a);
				if (flash > 0.0F) {
					g.fill(x + 24, ty, x + 24 + Math.round((float)(w - 24) * f), ty + 4, Colors.scaleAlpha(Colors.withAlpha(lit, 80), a * flash));
				}

				if (!this.tip.isEmpty() && this.over(g, mouseX, mouseY) && mouseX < x + w) {
					CFHGuiTooltips.request(this.tip, this);
				}
			}
		}
	}

	static final class TreeCard extends CFHGuiWidget {
		private static final float THUMB_SCALE = 0.62F;
		private static final int HEAD = 17;
		private static final int FOOT = 7;
		private final Chapter chapter;
		private final GraphModel graph;
		private final CFHGuiQuestPainter painter = new CFHGuiQuestPainter();
		private final BookStats.Stat stat;
		private final boolean open;
		private final Runnable onPress;
		private GuiGraphics frameG;
		private final Runnable paint = this::paint;

		TreeCard(int w, Chapter chapter, boolean open, Runnable onPress) {
			super(0, 0, w, 78, QuestText.title(chapter));
			this.chapter = chapter;
			this.graph = GraphModel.of(chapter);
			this.stat = BookStats.of(chapter);
			this.open = open;
			this.onPress = onPress;
			this.active = open;
			float gw = this.graph.questWidth() + 1.2F;
			float gh = this.graph.questHeight() + 1.2F;
			float scale = this.graph.isEmpty() ? 0.62F : Mth.clamp(Math.min(0.62F, (float)(w - 8) / (gw * this.painter.unit())), 0.25F, 0.62F);
			int thumbH = this.graph.isEmpty() ? 30 : Mth.clamp(Math.round(gh * this.painter.unit() * scale), 30, 150);
			this.setHeight(17 + thumbH + 7);
		}

		public void onClick(double mouseX, double mouseY) {
			super.onClick(mouseX, mouseY);
			this.onPress.run();
		}

		public ComponentPath nextFocusPath(FocusNavigationEvent event) {
			return null;
		}

		protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			this.draw(g, false);
		}

		protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			this.draw(g, true);
		}

		private void draw(GuiGraphics g, boolean van) {
			Theme t = Theme.current();
			Palette p = t.palette();
			Font font = CFHGuiDraw.font();
			float a = this.effectiveAlpha();
			float hov = this.hover();
			if (!(a <= 0.004F)) {
				int x = this.getX();
				int y = this.getY() + this.enterOffset();
				int w = this.getWidth();
				int h = this.getHeight();
				int accent = van ? -1 : QuestPainter.accentOf(this.chapter);
				int ground = van ? -15461356 : Colors.mix(p.bg(), -16777216, 0.3F);
				if (van) {
					g.fill(x, y, x + w, y + h, Colors.scaleAlpha(-16777216, a));
					g.fill(x + 1, y + 1, x + w - 1, y + h - 1, Colors.scaleAlpha(ground, a));
					CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(-7631989, -1, hov), a), 0);
				} else {
					CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(ground, a), t.radius());
					CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(Colors.mix(p.border(), accent, 0.35F), accent, hov), a), t.radius());
				}

				if (a > 0.5F) {
					this.painter.on(ground);
					this.frameG = g;
					CanvasDraw.batch(g, this.paint);
					this.frameG = null;
					CanvasDraw.layer(g);
				}

				String count = this.stat.done() + " / " + this.stat.total();
				int cw = font.width(count);
				QuestIcons.of(this.chapter.icon).draw(g, (float)(x + 10), (float)(y + 9), 10.0F, Colors.scaleAlpha(accent, a));
				g.drawString(font, CFHGuiDraw.truncate(Fonts.heading(this.getMessage()), w - 26 - cw - 6), x + 18, y + 5, Colors.scaleAlpha(van ? -1 : p.text(), a), van);
				g.drawString(font, count, x + w - 6 - cw, y + 5, Colors.scaleAlpha(van ? -6250336 : accent, a), van);
				UiBits.bar(
					g,
					x + 5,
					y + h - 4,
					w - 10,
					1,
					this.stat.fraction(),
					van ? -16777216 : p.surfaceActive(),
					this.stat.complete() ? (van ? -11141291 : p.success()) : (van ? -8399233 : accent),
					a
				);
				if (!this.open) {
					g.fill(x + 1, y + 1, x + w - 1, y + h - 1, Colors.scaleAlpha(Colors.withAlpha(ground, 176), a));
					Icons.draw(g, Icon.LOCK, x + w / 2 - 6, y + h / 2 - 4, 12, Colors.scaleAlpha(van ? -6250336 : p.textDim(), a));
				}

				if (!van) {
					CFHGuiDraw.focusRing(g, x, y, w, h, this.focus() * a);
				}
			}
		}

		private void paint() {
			int x = this.getX();
			int y = this.getY() + this.enterOffset();
			Rect box = new Rect(x + 4, y + 17, this.getWidth() - 8, this.getHeight() - 17 - 7);
			float seconds = Theme.current().motion() > 0.0F ? (float)(Clock.nowMs() % 3600000L) / 1000.0F : 0.0F;
			TreeThumb.draw(this.frameG, this.painter, this.graph, TreeThumb.view(this.chapter, box, seconds), box.x(), box.y(), box.w(), box.h(), 0.62F, 1.0F);
		}
	}
}
