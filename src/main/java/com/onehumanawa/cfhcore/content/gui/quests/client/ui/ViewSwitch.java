package com.onehumanawa.cfhcore.content.gui.quests.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class ViewSwitch extends CFHGuiWidget {
	private static final String[] ISLAND = new String[]{
		"..XX..XXX..", ".X..XX...X.", ".X.XXXX..X.", "...X.XX....", ".....XX....", "....XX.....", "....XX.....", "..XXXXXXX..", ".XXXXXXXXX.", "XXXXXXXXXXX"
	};
	private static final String[] TREE = new String[]{
		"....XXX....", "....XXX....", "....XXX....", ".....X.....", ".XXXXXXXXX.", ".X.......X.", ".X.......X.", "XXX.....XXX", "XXX.....XXX", "XXX.....XXX"
	};
	private static final int PICTURE_W = 11;
	private static final int PICTURE_H = 10;
	private static final int PAD = 5;
	private static final int GAP = 3;
	private static final int BARE = 20;
	private final boolean words;
	private final Runnable onPress;
	private final int split;
	private boolean islands;
	private final Anim slide = new Anim(0.0F, 180, Ease.OUT_CUBIC);
	@Nullable
	private Component busy;

	private static Component name(boolean islands) {
		return Component.translatable(islands ? "cfhgui_quests.view.islands.name" : "cfhgui_quests.view.tree.name");
	}

	private static int half(boolean islands, boolean words) {
		return words ? 19 + CFHGuiDraw.width(name(islands)) + 5 : 20;
	}

	static int width(boolean words) {
		return half(false, words) + half(true, words);
	}

	ViewSwitch(boolean islands, boolean words, Runnable onPress) {
		super(0, 0, width(words), 20, Component.translatable("cfhgui_quests.view"));
		this.islands = islands;
		this.words = words;
		this.onPress = onPress;
		this.split = half(false, words);
		this.slide.snap(islands ? 1.0F : 0.0F);
	}

	void lean(boolean toIslands) {
		this.islands = toIslands;
		this.slide.set(toIslands ? 1.0F : 0.0F);
	}

	ViewSwitch busy(@Nullable Component why) {
		this.busy = why;
		return this;
	}

	private boolean overIslands(double mouseX) {
		return mouseX >= (double)(this.getX() + this.split);
	}

	public void onClick(double mouseX, double mouseY) {
		super.onClick(mouseX, mouseY);
		if (this.busy == null && this.overIslands(mouseX) != this.islands) {
			this.onPress.run();
		}
	}

	public void playDownSound(SoundManager handler) {
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.active && this.visible && this.busy == null) {
			if ((keyCode != 263 || !this.islands) && (keyCode != 262 || this.islands) && keyCode != 257 && keyCode != 335 && keyCode != 32) {
				return false;
			} else {
				this.onPress.run();
				return true;
			}
		} else {
			return false;
		}
	}

	private void say(int mouseX) {
		this.tip(
			(Component)(this.busy != null
				? this.busy
				: Component.translatable(this.overIslands((double)mouseX) ? "cfhgui_quests.view.islands" : "cfhgui_quests.view.tree"))
		);
	}

	protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		Theme t = Theme.current();
		Palette p = t.palette();
		float a = this.effectiveAlpha();
		if (!(a <= 0.004F)) {
			int x = this.getX();
			int y = this.getY() + this.enterOffset();
			int w = this.getWidth();
			int h = this.getHeight();
			boolean free = this.busy == null;
			boolean hovered = this.isHovered();
			this.say(mouseX);
			CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(p.bg2(), a), t.radius());
			CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(free ? p.border() : Colors.withAlpha(p.border(), 128), a), t.radius());
			int inner = Math.max(0, t.radius() - 1);
			boolean otherHovered = free && hovered && this.overIslands((double)mouseX) != this.islands;
			if (otherHovered) {
				int ox = this.islands ? x : x + this.split;
				int ow = this.islands ? this.split : w - this.split;
				CFHGuiDraw.pixelRound(g, ox + 2, y + 2, ow - 4, h - 4, Colors.scaleAlpha(p.surfaceHover(), a * 0.6F), inner);
			}

			float s = Math.max(0.0F, Math.min(1.0F, this.slide.get()));
			int mx = x + Math.round((float)this.split * s);
			int mw = Math.round((float)this.split + (float)(w - this.split - this.split) * s);
			CFHGuiDraw.pixelRound(g, mx + 2, y + 2, mw - 4, h - 4, Colors.scaleAlpha(free ? p.surfaceActive() : p.surface(), a), inner);
			CFHGuiDraw.outline(g, mx + 2, y + 2, mw - 4, h - 4, Colors.scaleAlpha(free ? p.borderStrong() : p.border(), a), inner);
			CFHGuiDraw.rect(g, mx + 2 + inner, y + h - 3, mw - 4 - inner * 2, 1, Colors.scaleAlpha(p.accent(), a * (free ? 1.0F : 0.4F)));

			for (int i = 0; i < 2; i++) {
				boolean isles = i == 1;
				int fg = !free ? p.textDim() : (isles == this.islands ? p.text() : (otherHovered ? p.text() : p.textMuted()));
				this.half(g, isles ? x + this.split : x, y, isles ? w - this.split : this.split, h, isles, Colors.scaleAlpha(fg, a), false);
			}

			CFHGuiDraw.focusRing(g, x, y, w, h, this.focus() * a);
		}
	}

	protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		float a = this.effectiveAlpha();
		if (!(a <= 0.004F)) {
			int x = this.getX();
			int y = this.getY() + this.enterOffset();
			int w = this.getWidth();
			int h = this.getHeight();
			boolean free = this.busy == null;
			boolean hovered = this.isHovered();
			this.say(mouseX);

			for (int i = 0; i < 2; i++) {
				boolean isles = i == 1;
				int sx = isles ? x + this.split : x;
				int sw = isles ? w - this.split : this.split;
				boolean marked = isles == this.islands;
				float lift = !free ? 0.0F : (marked ? 1.0F : (hovered && this.overIslands((double)mouseX) == isles ? 0.6F : 0.0F));
				CFHGuiDraw.vanillaButton(g, sx, y, sw, h, lift, free, a);
				if (marked) {
					g.fill(sx + 1, y + 1, sx + sw - 1, y + h - 1, Colors.scaleAlpha(1073741824, a));
				}

				this.half(g, sx, y, sw, h, isles, Colors.scaleAlpha(!free ? -6250336 : (marked ? -96 : -2039584), a), true);
			}

			if (this.focus() > 0.5F) {
				CFHGuiDraw.outline(g, this.islands ? x + this.split : x, y, this.islands ? w - this.split : this.split, h, Colors.scaleAlpha(-1, a), 0);
			}
		}
	}

	private void half(GuiGraphics g, int x, int y, int w, int h, boolean isles, int fg, boolean shadow) {
		Component name = name(isles);
		int content = this.words ? 14 + CFHGuiDraw.width(name) : 11;
		int left = x + (w - content) / 2;
		picture(g, isles ? ISLAND : TREE, left, y + (h - 10) / 2, fg);
		if (this.words) {
			g.drawString(CFHGuiDraw.font(), name, left + 11 + 3, CFHGuiDraw.textY(y, h), fg, shadow);
		}
	}

	private static void picture(GuiGraphics g, String[] rows, int left, int top, int fg) {
		for (int row = 0; row < rows.length; row++) {
			String s = rows[row];
			int from = -1;

			for (int i = 0; i <= s.length(); i++) {
				boolean on = i < s.length() && s.charAt(i) == 'X';
				if (on && from < 0) {
					from = i;
				} else if (!on && from >= 0) {
					g.fill(left + from, top + row, left + i, top + row + 1, fg);
					from = -1;
				}
			}
		}
	}

	protected void updateWidgetNarration(NarrationElementOutput out) {
		out.add(
			NarratedElementType.TITLE,
			Component.translatable("gui.narrate.button", new Object[]{Component.empty().append(this.getMessage()).append(": ").append(name(this.islands))})
		);
		if (this.busy != null) {
			out.add(NarratedElementType.HINT, this.busy);
		} else {
			out.add(NarratedElementType.USAGE, Component.translatable(this.islands ? "cfhgui_quests.view.tree" : "cfhgui_quests.view.islands"));
		}
	}
}
