package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.RichText;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CanvasDraw;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class Bits {
	static void well(GuiGraphics g, int x, int y, int size, float hover, boolean van, float a) {
		Palette p = Theme.current().palette();
		if (van) {
			g.fill(x, y, x + size, y + size, Colors.scaleAlpha(-7631989, a));
			g.fill(x, y, x + size - 1, y + 1, Colors.scaleAlpha(-13158601, a));
			g.fill(x, y, x + 1, y + size - 1, Colors.scaleAlpha(-13158601, a));
			g.fill(x + 1, y + size - 1, x + size, y + size, Colors.scaleAlpha(-1, a));
			g.fill(x + size - 1, y + 1, x + size, y + size, Colors.scaleAlpha(-1, a));
			if (hover > 0.01F) {
				g.fill(x + 1, y + 1, x + size - 1, y + size - 1, Colors.scaleAlpha(1627389951, a * hover));
			}
		} else {
			CFHGuiDraw.pixelRound(g, x, y, size, size, Colors.scaleAlpha(Colors.mix(p.bg(), -16777216, 0.25F), a), 2);
			CFHGuiDraw.outline(g, x, y, size, size, Colors.scaleAlpha(Colors.lerp(p.border(), p.accent(), hover), a), 2);
		}
	}

	private Bits() {
	}

	static final class IconWell extends CFHGuiWidget {
		private final Supplier<QuestIcons.Drawable> icon;
		private final Runnable onPress;

		IconWell(int size, Component narration, Supplier<QuestIcons.Drawable> icon, Runnable onPress) {
			super(0, 0, size, size, narration);
			this.icon = icon;
			this.onPress = onPress;
		}

		public void onClick(double mouseX, double mouseY) {
			super.onClick(mouseX, mouseY);
			this.onPress.run();
		}

		public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			if (this.active && (keyCode == 257 || keyCode == 32 || keyCode == 335)) {
				CFHGuiSounds.click();
				this.flashPress();
				this.onPress.run();
				return true;
			} else {
				return false;
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
				Palette p = Theme.current().palette();
				int x = this.getX();
				int y = this.getY() + this.enterOffset() + Math.round(this.press());
				int size = this.getWidth();
				Bits.well(g, x, y, size, Math.max(this.hover(), this.focus()), van, a);
				QuestIcons.Drawable d = this.icon.get();
				if (a > 0.5F) {
					if (d != null && !d.isEmpty()) {
						d.draw(
							g, (float)x + (float)size / 2.0F, (float)y + (float)size / 2.0F, Math.min(16.0F, (float)size - 2.0F), Colors.scaleAlpha(van ? -1 : p.textMuted(), a)
						);
					} else {
						Icons.draw(g, Icon.PLUS, x + (size - 8) / 2, y + (size - 8) / 2, 8, Colors.scaleAlpha(van ? -13158601 : p.textDim(), a));
					}
				}

				if (!van) {
					CFHGuiDraw.focusRing(g, x, y, size, size, this.focus() * a);
				}
			}
		}
	}

	static final class ListRow extends CFHGuiWidget {
		static final int HEIGHT = 20;
		private static final int ACT = 13;
		@Nullable
		private final QuestIcons.Drawable icon;
		@Nullable
		private final Component note;
		@Nullable
		private final Runnable onOpen;
		@Nullable
		private Runnable up;
		@Nullable
		private Runnable down;
		@Nullable
		private Runnable remove;
		private boolean marked;
		private int indent;
		@Nullable
		private Component removeTip;

		ListRow(int width, @Nullable QuestIcons.Drawable icon, Component label, @Nullable Component note, @Nullable Runnable onOpen) {
			super(0, 0, width, 20, label);
			this.icon = icon;
			this.note = note;
			this.onOpen = onOpen;
		}

		Bits.ListRow actions(@Nullable Runnable up, @Nullable Runnable down, @Nullable Runnable remove) {
			this.up = up;
			this.down = down;
			this.remove = remove;
			return this;
		}

		Bits.ListRow marked(boolean on) {
			this.marked = on;
			return this;
		}

		Bits.ListRow indent(int px) {
			this.indent = px;
			return this;
		}

		Bits.ListRow removeTip(Component tip) {
			this.removeTip = tip;
			return this;
		}

		private int actions() {
			return (this.up == null && this.down == null ? 0 : 2) + (this.remove != null ? 1 : 0);
		}

		private int actionAt(double mx, double my) {
			if (!(my < (double)this.getY()) && !(my >= (double)(this.getY() + this.getHeight()))) {
				int x = this.getX() + this.getWidth() - 3;
				if (this.remove != null) {
					x -= 13;
					if (mx >= (double)x && mx < (double)(x + 13)) {
						return 2;
					}
				}

				if (this.up != null || this.down != null) {
					x -= 13;
					if (mx >= (double)x && mx < (double)(x + 13)) {
						return 1;
					}

					x -= 13;
					if (mx >= (double)x && mx < (double)(x + 13)) {
						return 0;
					}
				}

				return -1;
			} else {
				return -1;
			}
		}

		private void run(int action) {
			Runnable r = action == 0 ? this.up : (action == 1 ? this.down : (action == 2 ? this.remove : this.onOpen));
			if (r != null) {
				CFHGuiSounds.tick();
				r.run();
			}
		}

		public void onClick(double mouseX, double mouseY) {
			super.onClick(mouseX, mouseY);
			this.run(this.actionAt(mouseX, mouseY));
		}

		public void playDownSound(SoundManager handler) {
		}

		public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			if (!this.active || !this.visible) {
				return false;
			} else if (keyCode == 257 || keyCode == 32 || keyCode == 335) {
				this.flashPress();
				this.run(-1);
				return true;
			} else if (keyCode == 261 && this.remove != null) {
				this.run(2);
				return true;
			} else if ((modifiers & 4) != 0 && keyCode == 265 && this.up != null) {
				this.run(0);
				return true;
			} else if ((modifiers & 4) != 0 && keyCode == 264 && this.down != null) {
				this.run(1);
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
			float a = this.effectiveAlpha();
			if (!(a <= 0.004F)) {
				Theme t = Theme.current();
				Palette p = t.palette();
				Font font = CFHGuiDraw.font();
				int x = this.getX();
				int y = this.getY() + this.enterOffset();
				int w = this.getWidth();
				int h = this.getHeight();
				float lift = Math.max(this.hover(), this.focus());
				if (van) {
					g.fill(x, y, x + w, y + h, Colors.scaleAlpha(this.marked ? 1358954495 : Colors.lerp(419430399, 956301311, lift), a));
				} else {
					int fill = this.marked ? p.surfaceActive() : Colors.lerp(p.surface(), p.surfaceHover(), lift);
					CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(fill, a), t.radius());
					CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(p.border(), p.borderStrong(), lift), a), t.radius());
					if (this.marked) {
						g.fill(x, y + 4, x + 2, y + h - 4, Colors.scaleAlpha(t.accent(), a));
					}
				}

				int tx = x + 4 + this.indent;
				if (this.icon != null) {
					if (a > 0.5F) {
						this.icon.draw(g, (float)(tx + 7), (float)y + (float)h / 2.0F, 14.0F, Colors.scaleAlpha(van ? -1 : p.textMuted(), a));
					}

					tx += 18;
				}

				boolean shown = this.actions() > 0 && (this.isHovered() || this.isFocused() && CFHGuiDraw.focusShown());
				int right = x + w - 4 - (shown ? this.actions() * 13 : 0);
				int noteW = this.note != null && !shown ? Math.min(font.width(this.note), Math.max(0, (right - tx) / 2)) : 0;
				int text = !this.active ? (van ? -6250336 : p.textDim()) : (van ? -1 : Colors.lerp(p.text(), -1, lift * 0.3F));
				FormattedCharSequence label = CFHGuiDraw.truncate(this.getMessage(), right - tx - (noteW > 0 ? noteW + 6 : 0));
				g.drawString(font, label, tx, CFHGuiDraw.textY(y, h), Colors.scaleAlpha(text, a), van);
				if (noteW > 0) {
					g.drawString(font, CFHGuiDraw.truncate(this.note, noteW), right - noteW, CFHGuiDraw.textY(y, h), Colors.scaleAlpha(van ? -6250336 : p.textDim(), a), van);
				}

				if (shown) {
					int over = this.isHovered() && g.containsPointInScissor(mouseX, mouseY) ? this.actionAt((double)mouseX, (double)mouseY) : -1;
					int ax = x + w - 3;
					if (this.remove != null) {
						ax -= 13;
						this.act(g, Icon.CLOSE, ax, y, over == 2, true, true, van, a);
					}

					if (this.up != null || this.down != null) {
						ax -= 13;
						this.act(g, Icon.CHEVRON_DOWN, ax, y, over == 1, this.down != null, false, van, a);
						ax -= 13;
						this.act(g, Icon.CHEVRON_UP, ax, y, over == 0, this.up != null, false, van, a);
					}

					if (over == 0 && this.up != null) {
						CFHGuiTooltips.request(Tr.t("row.up"), this);
					} else if (over == 1 && this.down != null) {
						CFHGuiTooltips.request(Tr.t("row.down"), this);
					} else if (over == 2) {
						CFHGuiTooltips.request((Component)(this.removeTip != null ? this.removeTip : Tr.t("row.remove")), this);
					}
				}

				if (!van) {
					CFHGuiDraw.focusRing(g, x, y, w, h, this.focus() * a);
				}
			}
		}

		private void act(GuiGraphics g, Icon glyph, int x, int y, boolean over, boolean enabled, boolean danger, boolean van, float a) {
			Palette p = Theme.current().palette();
			int h = this.getHeight();
			if (over && enabled) {
				CFHGuiDraw.pixelRound(
					g, x, y + 3, 13, h - 6, Colors.scaleAlpha(danger ? Colors.withAlpha(p.danger(), 64) : (van ? 1627389951 : p.surfaceActive()), a), van ? 0 : 2
				);
			}

			int ink = !enabled
				? (van ? -10461088 : Colors.withAlpha(p.textDim(), 112))
				: (over ? (danger ? (van ? -43691 : p.danger()) : (van ? -1 : p.text())) : (van ? -4144960 : p.textMuted()));
			Icons.draw(g, glyph, x + 2, y + (h - 8) / 2, 8, Colors.scaleAlpha(ink, a));
		}
	}

	static final class Reading extends CFHGuiWidget {
		private final Supplier<QuestIcons.Drawable> icon;
		private final Supplier<Component> words;

		Reading(int width, Supplier<QuestIcons.Drawable> icon, Supplier<Component> words) {
			super(0, 0, width, 22, Tr.t("reads"));
			this.icon = icon;
			this.words = words;
			this.active = false;
		}

		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			return false;
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
			float a = this.effectiveAlpha();
			if (!(a <= 0.004F)) {
				Theme t = Theme.current();
				Palette p = t.palette();
				Font font = CFHGuiDraw.font();
				int x = this.getX();
				int y = this.getY() + this.enterOffset();
				int w = this.getWidth();
				int h = this.getHeight();
				if (van) {
					g.fill(x, y, x + w, y + h, Colors.scaleAlpha(1610612736, a));
				} else {
					CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(Colors.mix(p.bg(), -16777216, 0.2F), a), t.radius());
				}

				Bits.well(g, x + 2, y + 2, 18, 0.0F, van, a);
				QuestIcons.Drawable d = this.icon.get();
				if (a > 0.5F && d != null) {
					d.draw(g, (float)(x + 11), (float)(y + 11), 16.0F, Colors.scaleAlpha(van ? -1 : p.textMuted(), a));
				}

				List<FormattedCharSequence> lines = font.split((FormattedText)this.words.get(), Math.max(20, w - 30));
				int n = Math.min(2, lines.size());
				int ty = y + (h - n * 10) / 2 + 1;

				for (int i = 0; i < n; i++) {
					g.drawString(font, lines.get(i), x + 25, ty, Colors.scaleAlpha(van ? -1 : p.text(), a), van);
					ty += 10;
				}
			}
		}
	}

	static final class RichBox extends CFHGuiWidget {
		private RichText.Layout layout;
		private int scroll;
		private final Component empty;

		RichBox(int width, int height, List<String> lines, Component empty) {
			super(0, 0, width, height, Tr.t("text.preview"));
			this.empty = empty;
			this.active = false;
			this.set(lines);
		}

		void set(List<String> lines) {
			this.layout = RichText.of(lines).layout(Math.max(24, this.getWidth() - 12));
			this.scroll = Mth.clamp(this.scroll, 0, this.max());
		}

		private int max() {
			return Math.max(0, this.layout.height() + 10 - this.getHeight());
		}

		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			if (this.max() <= 0) {
				return false;
			} else if ((!(scrollY > 0.0) || this.scroll > 0) && (!(scrollY < 0.0) || this.scroll < this.max())) {
				this.scroll = Mth.clamp(this.scroll - (int)Math.round(scrollY * 12.0), 0, this.max());
				return true;
			} else {
				return false;
			}
		}

		public boolean isMouseOver(double mouseX, double mouseY) {
			return this.visible && this.contains(mouseX, mouseY);
		}

		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			return false;
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
			float a = this.effectiveAlpha();
			if (!(a <= 0.004F)) {
				Theme t = Theme.current();
				Palette p = t.palette();
				int x = this.getX();
				int y = this.getY() + this.enterOffset();
				int w = this.getWidth();
				int h = this.getHeight();
				if (van) {
					g.fill(x, y, x + w, y + h, Colors.scaleAlpha(-1610612736, a));
					CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(-9474193, a), 0);
				} else {
					CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(Colors.mix(p.bg(), -16777216, 0.2F), a), t.radius());
					CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(p.border(), a), t.radius());
				}

				if (this.layout.height() <= 0) {
					g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(this.empty, w - 12), x + 6, y + 6, Colors.scaleAlpha(van ? -8355712 : p.textDim(), a), van);
				} else {
					g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
					this.layout.draw(g, x + 6, y + 5 - this.scroll, -9999, -9999, a);
					g.disableScissor();
					if (this.max() > 0) {
						int barH = Math.max(8, (h - 4) * h / (this.layout.height() + 10));
						int barY = y + 2 + Math.round((float)(h - 4 - barH) * ((float)this.scroll / (float)this.max()));
						g.fill(x + w - 3, barY, x + w - 1, barY + barH, Colors.scaleAlpha(van ? -4144960 : p.borderStrong(), a));
					}
				}
			}
		}
	}

	static final class ShapeRow extends CFHGuiWidget {
		static final int CELL = 16;
		private final List<Quest.Shape> options = new ArrayList<>();
		private final boolean inherits;
		private final Quest.Shape inherited;
		private final Consumer<Quest.Shape> onPick;
		private int selected;
		private int cursor;
		private final List<Anim> hovers = new ArrayList<>();

		ShapeRow(int width, @Nullable Quest.Shape value, boolean inherits, Quest.Shape inherited, Consumer<Quest.Shape> onPick) {
			super(0, 0, width, 16, Tr.field("shape"));
			this.inherits = inherits;
			this.inherited = inherited;
			this.onPick = onPick;
			if (inherits) {
				this.options.add(null);
			}

			this.options.addAll(List.of(Quest.Shape.values()));
			this.selected = Math.max(0, this.options.indexOf(value));
			this.cursor = this.selected;

			for (int i = 0; i < this.options.size(); i++) {
				this.hovers.add(new Anim(0.0F, 120, Ease.OUT_CUBIC));
			}

			this.setHeight(this.rows() * 16);
		}

		private int perRow() {
			return Math.max(1, this.getWidth() / 16);
		}

		private int rows() {
			return (this.options.size() + this.perRow() - 1) / this.perRow();
		}

		private int at(double mx, double my) {
			int col = (int)((mx - (double)this.getX()) / 16.0);
			int row = (int)((my - (double)this.getY()) / 16.0);
			if (!(mx < (double)this.getX()) && col < this.perRow() && !(my < (double)this.getY())) {
				int i = row * this.perRow() + col;
				return i >= 0 && i < this.options.size() ? i : -1;
			} else {
				return -1;
			}
		}

		private void pick(int i) {
			if (i >= 0 && i < this.options.size()) {
				this.cursor = i;
				if (i != this.selected) {
					this.selected = i;
					CFHGuiSounds.tick();
					this.onPick.accept(this.options.get(i));
				}
			}
		}

		private Component name(int i) {
			Quest.Shape s = this.options.get(i);
			return s == null ? Tr.t("v.shape.chapter.named", Tr.value("shape", this.inherited)) : Tr.value("shape", s);
		}

		public void onClick(double mouseX, double mouseY) {
			super.onClick(mouseX, mouseY);
			this.pick(this.at(mouseX, mouseY));
		}

		public void playDownSound(SoundManager handler) {
		}

		public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			if (this.active && this.visible) {
				switch (keyCode) {
					case 262:
						this.pick(Math.min(this.options.size() - 1, this.cursor + 1));
						break;
					case 263:
						this.pick(Math.max(0, this.cursor - 1));
						break;
					default:
						return false;
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
			float a = this.effectiveAlpha();
			if (!(a <= 0.004F)) {
				Theme t = Theme.current();
				Palette p = t.palette();
				int over = this.isHovered() && this.active && g.containsPointInScissor(mouseX, mouseY) ? this.at((double)mouseX, (double)mouseY) : -1;
				int dy = this.enterOffset();

				for (int i = 0; i < this.options.size(); i++) {
					Anim h = this.hovers.get(i);
					h.set(i == over);
					float hv = h.get();
					int x = this.getX() + i % this.perRow() * 16;
					int y = this.getY() + i / this.perRow() * 16 + dy;
					boolean sel = i == this.selected;
					Quest.Shape shape = this.options.get(i) == null ? this.inherited : this.options.get(i);
					int accent = van ? -1 : t.accent();
					if (sel) {
						if (van) {
							CFHGuiDraw.outline(g, x, y, 16, 16, Colors.scaleAlpha(-1, a), 0);
						} else {
							CFHGuiDraw.pixelRound(g, x, y, 16, 16, Colors.scaleAlpha(Colors.withAlpha(accent, 48), a), 2);
						}
					} else if (hv > 0.01F) {
						CFHGuiDraw.pixelRound(g, x, y, 16, 16, Colors.scaleAlpha(van ? 1090519039 : p.surfaceHover(), a * hv), van ? 0 : 2);
					}

					int ink = sel ? accent : (van ? Colors.lerp(-6250336, -1, hv) : Colors.lerp(p.textDim(), p.text(), hv));
					g.pose().pushPose();
					g.pose().translate((float)x + 8.0F, (float)y + 8.0F, 0.0F);
					CanvasDraw.shape(g, shape, 5, Colors.scaleAlpha(van ? -12829636 : p.bg2(), a), Colors.scaleAlpha(ink, a));
					g.pose().popPose();
					if (this.options.get(i) == null) {
						g.fill(x + 5, y + 16 - 2, x + 16 - 5, y + 16 - 1, Colors.scaleAlpha(ink, a));
					}
				}

				if (over >= 0) {
					CFHGuiTooltips.request(this.name(over), this);
				}

				if (this.isFocused() && CFHGuiDraw.focusShown() && !van) {
					CFHGuiDraw.focusRing(g, this.getX() + this.cursor % this.perRow() * 16, this.getY() + this.cursor / this.perRow() * 16 + dy, 16, 16, this.focus() * a);
				}
			}
		}

		protected void updateWidgetNarration(NarrationElementOutput out) {
			out.add(NarratedElementType.TITLE, this.getMessage());
			out.add(NarratedElementType.HINT, this.name(this.cursor));
		}
	}
}
