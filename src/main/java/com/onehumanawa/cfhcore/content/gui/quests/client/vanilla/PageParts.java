package com.onehumanawa.cfhcore.content.gui.quests.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
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
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

final class PageParts {
	private static final String[] CHECK = new String[]{"......X", ".....XX", "X...XX.", "XX.XX..", ".XXX...", "..X...."};

	static void text(GuiGraphics g, FormattedCharSequence text, int x, int y, int color, float alpha) {
		if (!(alpha <= 0.004F)) {
			g.drawString(CFHGuiDraw.font(), text, x, y, Colors.scaleAlpha(color, Math.max(0.03F, alpha)), Theme.current().isVanilla());
		}
	}

	static void check(GuiGraphics g, int x, int y, int color, float alpha) {
		int ink = Colors.scaleAlpha(color, alpha);

		for (int row = 0; row < CHECK.length; row++) {
			String s = CHECK[row];
			int from = -1;

			for (int i = 0; i <= s.length(); i++) {
				boolean on = i < s.length() && s.charAt(i) == 'X';
				if (on && from < 0) {
					from = i;
				} else if (!on && from >= 0) {
					g.fill(x + from, y + row, x + i, y + row + 1, ink);
					from = -1;
				}
			}
		}
	}

	private PageParts() {
	}

	static final class Caption extends PageParts.Still {
		@Nullable
		private final Component note;

		Caption(int w, Component title, @Nullable Component note) {
			super(w, 12, title);
			this.note = note;
		}

		@Override
		void draw(GuiGraphics g, int mouseX, int mouseY) {
			float a = this.effectiveAlpha();
			int y = this.getY() + this.enterOffset();
			int noteW = this.note == null ? 0 : CFHGuiDraw.width(this.note) + 6;
			CFHGuiDraw.sectionRule(g, this.getMessage(), this.getX(), y, this.getWidth() - noteW, a);
			if (this.note != null) {
				PageParts.text(g, this.note.getVisualOrderText(), this.getX() + this.getWidth() - CFHGuiDraw.width(this.note), y + 1, PageParts.Ink.MUTED.color(), a);
			}
		}
	}

	static final class Cross extends CFHGuiWidget {
		private final Runnable onPress;

		Cross(Component label, Runnable onPress) {
			super(0, 0, 14, 14, label);
			this.onPress = onPress;
			this.tip(label);
		}

		public void onClick(double mouseX, double mouseY) {
			super.onClick(mouseX, mouseY);
			this.onPress.run();
		}

		public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			if (this.isFocused() && (keyCode == 257 || keyCode == 32 || keyCode == 335)) {
				CFHGuiSounds.click();
				this.onPress.run();
				return true;
			} else {
				return false;
			}
		}

		protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			this.draw(g);
		}

		protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			this.draw(g);
		}

		private void draw(GuiGraphics g) {
			WindowArt.cross(g, this.getX(), this.getY() + Math.round(this.press()), Math.max(this.hover(), this.focus()), this.effectiveAlpha());
		}
	}

	static final class Fact extends PageParts.Still {
		private final Supplier<Component> words;
		private final PageParts.Ink ink;
		private final List<FormattedCharSequence> lines = new ArrayList<>();
		private String laidOut = "";
		private final int maxLines;

		Fact(int w, PageParts.Ink ink, Supplier<Component> words) {
			super(w, 10, Component.empty());
			this.words = words;
			this.ink = ink;
			this.lay(words.get(), 3);
			this.maxLines = Math.max(1, this.lines.size());
			this.setHeight(this.lines.size() * 9 + 1);
		}

		private void lay(Component c, int max) {
			this.laidOut = c.getString();
			this.lines.clear();
			Font font = CFHGuiDraw.font();
			List<FormattedCharSequence> all = font.split(c, Math.max(30, this.getWidth()));

			for (int i = 0; i < all.size() && i < max; i++) {
				this.lines.add(all.get(i));
			}

			if (all.size() > max && !this.lines.isEmpty()) {
				this.lines.set(this.lines.size() - 1, CFHGuiDraw.truncate(c, Math.max(30, this.getWidth())));
			}
		}

		@Override
		void draw(GuiGraphics g, int mouseX, int mouseY) {
			float a = this.effectiveAlpha();
			if (!(a <= 0.004F)) {
				Component now = this.words.get();
				if (!this.laidOut.equals(now.getString())) {
					this.lay(now, this.maxLines);
				}

				int y = this.getY() + this.enterOffset();

				for (FormattedCharSequence line : this.lines) {
					PageParts.text(g, line, this.getX(), y, this.ink.color(), a);
					y += 9;
				}
			}
		}
	}

	static enum Ink {
		TEXT,
		MUTED,
		DIM,
		GOOD,
		GOLD,
		SAID,
		SAID_CHALLENGE;

		int color() {
			Theme t = Theme.current();
			Palette p = t.palette();
			if (!t.isVanilla()) {
				return switch (this) {
					case TEXT -> p.text();
					default -> p.textMuted();
					case DIM -> p.textDim();
					case GOOD -> p.success();
					case GOLD -> p.warning();
				};
			} else {
				return switch (this) {
					case TEXT -> -1;
					case MUTED -> -5592406;
					case DIM -> -8355712;
					case GOOD, SAID -> -11141291;
					case GOLD -> -22016;
					case SAID_CHALLENGE -> -5635926;
				};
			}
		}
	}

	static final class RewardRow extends PageParts.Still {
		private final QuestIcons.Drawable icon;
		private final List<FormattedCharSequence> label;
		@Nullable
		private final Component note;
		private final boolean claimed;
		private final boolean waiting;
		private final int reserve;
		private final int indent;
		private final Supplier<List<Component>> tip;

		RewardRow(
			int w,
			QuestIcons.Drawable icon,
			Component words,
			@Nullable Component note,
			boolean claimed,
			boolean waiting,
			int reserve,
			int indent,
			Supplier<List<Component>> tip
		) {
			super(w, 22, words);
			this.icon = icon;
			this.note = note;
			this.claimed = claimed;
			this.waiting = waiting;
			this.reserve = reserve;
			this.indent = indent;
			this.tip = tip;
			List<FormattedCharSequence> lines = CFHGuiDraw.font().split(words, Math.max(40, w - indent - 24 - reserve - (claimed ? 12 : 0)));
			this.label = lines.size() > 2 ? lines.subList(0, 2) : lines;
			this.setHeight(Math.max(22, (this.label.size() + (note == null ? 0 : 1)) * 10 + 4));
		}

		@Override
		void draw(GuiGraphics g, int mouseX, int mouseY) {
			Theme t = Theme.current();
			boolean van = t.isVanilla();
			float a = this.effectiveAlpha();
			if (!(a <= 0.004F)) {
				int x = this.getX() + this.indent;
				int y = this.getY() + this.enterOffset();
				int w = this.getWidth() - this.indent - this.reserve;
				WindowArt.slot(g, x, y + 2, a);
				if (a > 0.5F) {
					this.icon.draw(g, (float)(x + 9), (float)(y + 11), 16.0F, Colors.scaleAlpha(van ? -12566464 : t.palette().textMuted(), a));
				}

				if (this.waiting) {
					float breath = t.motion() > 0.0F ? 0.5F + 0.5F * (float)Math.sin((double)Clock.nowMs() / 260.0) : 1.0F;
					CFHGuiDraw.outline(
						g, x - 1, y + 1, 20, 20, Colors.scaleAlpha(Colors.withAlpha(PageParts.Ink.GOLD.color(), Math.round(112.0F + 143.0F * breath)), a), van ? 0 : 2
					);
				}

				int lines = this.label.size() + (this.note == null ? 0 : 1);
				int ty = y + (lines == 1 ? 7 : 2);

				for (FormattedCharSequence line : this.label) {
					PageParts.text(g, line, x + 24, ty, (this.claimed ? PageParts.Ink.DIM : PageParts.Ink.TEXT).color(), a);
					ty += 10;
				}

				if (this.note != null) {
					PageParts.text(g, CFHGuiDraw.truncate(this.note, w - 24), x + 24, ty, PageParts.Ink.DIM.color(), a);
				}

				if (this.claimed) {
					g.pose().pushPose();
					g.pose().translate(0.0F, 0.0F, 200.0F);
					g.fill(x + 1, y + 3, x + 17, y + 19, Colors.scaleAlpha(van ? -1876877023 : Colors.withAlpha(t.palette().bg(), 160), a));
					g.pose().popPose();
					if (van) {
						WindowArt.sprite(g, WindowArt.CHECKMARK, x + w - 10, y + 7, 9, 8, a);
					} else {
						PageParts.check(g, x + w - 9, y + 8, PageParts.Ink.GOOD.color(), a);
					}
				}

				if (this.over(g, mouseX, mouseY) && mouseX < this.getX() + this.getWidth() - this.reserve) {
					List<Component> said = this.tip.get();
					if (!said.isEmpty()) {
						CFHGuiTooltips.request(said, this);
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

		protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			this.draw(g, mouseX, mouseY);
		}

		protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			this.draw(g, mouseX, mouseY);
		}

		abstract void draw(GuiGraphics var1, int var2, int var3);
	}

	static final class TaskRow extends PageParts.Still {
		private final QuestIcons.Drawable icon;
		private final List<FormattedCharSequence> label;
		private final String count;
		private final boolean done;
		private final boolean locked;
		private final boolean bar;
		private final int reserve;
		private final Anim fill = new Anim(0.0F, 480, Ease.OUT_CUBIC);
		private final long movedAtMs;
		private final List<Component> tip;

		TaskRow(
			int w,
			QuestIcons.Drawable icon,
			Component words,
			String count,
			float from,
			float fraction,
			boolean done,
			boolean locked,
			boolean bar,
			int reserve,
			List<Component> tip
		) {
			super(w, 22, words);
			this.icon = icon;
			this.count = count;
			this.done = done;
			this.locked = locked;
			this.bar = bar;
			this.reserve = reserve;
			this.tip = tip;
			int countW = count.isEmpty() ? 0 : CFHGuiDraw.width(count) + 6;
			List<FormattedCharSequence> lines = CFHGuiDraw.font().split(words, Math.max(40, w - 24 - reserve - countW));
			this.label = lines.size() > 2 ? lines.subList(0, 2) : lines;
			this.fill.snap(from);
			this.fill.set(fraction);
			this.movedAtMs = fraction > from + 0.001F ? Clock.nowMs() : 0L;
			this.setHeight(Math.max(22, this.label.size() * 10 + (bar ? 10 : 2)));
		}

		@Override
		void draw(GuiGraphics g, int mouseX, int mouseY) {
			Theme t = Theme.current();
			float a = this.effectiveAlpha();
			if (!(a <= 0.004F)) {
				int x = this.getX();
				int y = this.getY() + this.enterOffset();
				int w = this.getWidth() - this.reserve;
				WindowArt.slot(g, x, y + 2, a);
				if (a > 0.5F) {
					this.icon
						.draw(
							g,
							(float)(x + 9),
							(float)(y + 11),
							16.0F,
							Colors.scaleAlpha(t.isVanilla() ? -12566464 : (this.locked ? t.palette().textDim() : t.palette().textMuted()), a)
						);
				}

				long since = this.movedAtMs == 0L ? 9999L : Clock.nowMs() - this.movedAtMs;
				float flash = since < 700L && t.motion() > 0.0F ? 1.0F - (float)since / 700.0F : 0.0F;
				int ink = Colors.lerp((this.done ? PageParts.Ink.GOOD : (this.locked ? PageParts.Ink.DIM : PageParts.Ink.TEXT)).color(), -1, flash * 0.6F);
				int ty = y + (!this.bar && this.label.size() <= 1 ? 7 : 2);

				for (FormattedCharSequence line : this.label) {
					PageParts.text(g, line, x + 24, ty, ink, a);
					ty += 10;
				}

				if (!this.count.isEmpty()) {
					PageParts.text(
						g,
						Component.literal(this.count).getVisualOrderText(),
						x + w - CFHGuiDraw.width(this.count),
						y + (!this.bar && this.label.size() <= 1 ? 7 : 2),
						(this.done ? PageParts.Ink.GOOD : PageParts.Ink.MUTED).color(),
						a
					);
				}

				if (this.bar) {
					WindowArt.progress(g, x + 24, ty + 1, w - 24, this.fill.get(), this.done, a);
				}

				if (!this.tip.isEmpty() && this.over(g, mouseX, mouseY) && mouseX < x + w) {
					CFHGuiTooltips.request(this.tip, this);
				}
			}
		}
	}

	static final class Words extends PageParts.Still {
		private final List<FormattedCharSequence> lines;
		private final PageParts.Ink ink;

		Words(int w, Component words, PageParts.Ink ink, int maxLines) {
			super(w, 9, words);
			this.ink = ink;
			List<FormattedCharSequence> all = CFHGuiDraw.font().split(words, Math.max(30, w));
			this.lines = new ArrayList<>(all.size() > maxLines ? all.subList(0, maxLines) : all);
			if (all.size() > maxLines && maxLines > 0) {
				this.lines.set(maxLines - 1, CFHGuiDraw.truncate(words, Math.max(30, w)));
			}

			this.setHeight(Math.max(1, this.lines.size()) * 9);
		}

		@Override
		void draw(GuiGraphics g, int mouseX, int mouseY) {
			float a = this.effectiveAlpha();
			int y = this.getY() + this.enterOffset();

			for (FormattedCharSequence line : this.lines) {
				PageParts.text(g, line, this.getX(), y, this.ink.color(), a);
				y += 9;
			}
		}
	}
}
