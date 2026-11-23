package com.onehumanawa.cfhcore.content.gui.crafting.look;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;

final class LookDraw {
	static final int PAPER = -3750202;
	static final int PLATE_PAD = 3;
	static final int DOCK_REACH = 5;

	static int cornerOf(int height) {
		return Math.min(Theme.current().radius(), height >= 16 ? 3 : 2);
	}

	static int face(GuiGraphics g, int x, int y, int w, int h, LookDraw.Tone tone, float hover, float press, boolean enabled, float alpha) {
		Theme t = Theme.current();
		Palette p = t.palette();
		int py = y + nudge(press);
		if (t.isVanilla()) {
			if (tone != LookDraw.Tone.GHOST) {
				CFHGuiDraw.vanillaButton(g, x, py, w, h, enabled ? hover : 0.0F, enabled, alpha);
			} else if (hover > 0.01F) {
				CFHGuiDraw.vanillaButton(g, x, py, w, h, hover, enabled, alpha * hover);
			}

			int fg = !enabled ? -6250336 : (tone == LookDraw.Tone.ON ? -96 : Colors.lerp(tone == LookDraw.Tone.GHOST ? -2039584 : -1, -1, hover));
			return Colors.scaleAlpha(fg, alpha);
		} else {
			int r = cornerOf(h);
			int fill;
			int border;
			int fg;
			switch (tone) {
				case GHOST:
					fill = Colors.lerp(0, p.surfaceHover(), hover);
					border = Colors.lerp(0, p.border(), hover);
					fg = Colors.lerp(p.textMuted(), p.text(), hover);
					break;
				case ON:
					fill = Colors.lerp(Colors.mix(p.surface(), p.accent(), 0.2F), Colors.mix(p.surface(), p.accent(), 0.32F), hover);
					border = Colors.withAlpha(p.accent(), 192);
					fg = Colors.lerp(Colors.brighten(p.accent(), 0.25F), Colors.brighten(p.accent(), 0.45F), hover);
					break;
				default:
					fill = Colors.lerp(p.surface(), p.surfaceHover(), hover);
					border = Colors.lerp(p.border(), p.borderStrong(), hover);
					fg = Colors.lerp(p.text(), -1, hover * 0.3F);
			}

			if (!enabled) {
				fill = tone == LookDraw.Tone.GHOST ? 0 : Colors.withAlpha(p.surface(), 128);
				border = tone == LookDraw.Tone.GHOST ? 0 : Colors.withAlpha(p.border(), 128);
				fg = Colors.withAlpha(p.textDim(), 176);
			}

			fill = Colors.brighten(fill, -0.12F * press);
			if (enabled && tone != LookDraw.Tone.GHOST) {
				CFHGuiDraw.shadow(g, x, py, w, h, Colors.scaleAlpha(p.shadow(), 0.35F * alpha * (1.0F - press)), r);
			}

			CFHGuiDraw.pixelRound(g, x, py, w, h, Colors.scaleAlpha(fill, alpha), r);
			CFHGuiDraw.outline(g, x, py, w, h, Colors.scaleAlpha(border, alpha), r);
			return Colors.scaleAlpha(fg, alpha);
		}
	}

	static int nudge(float press) {
		return Math.round(press);
	}

	static int glyphSize(int side) {
		return side >= 24 ? 16 : (side >= 18 ? 12 : 8);
	}

	static void glyph(GuiGraphics g, Icon icon, int x, int y, int w, int h, int size, int color) {
		Icons.draw(g, icon, x + (w - size) / 2, y + (h - size) / 2, size, color);
	}

	static void panelPlate(GuiGraphics g, int x, int y, int w, int h, int pad, int screenW, int screenH, float alpha) {
		int x0 = x - pad;
		int y0 = y - pad;
		int x1 = x + w + pad;
		int y1 = y + h + pad;
		if (x0 <= 5) {
			x0 = -4;
		}

		if (y0 <= 5) {
			y0 = -4;
		}

		if (x1 >= screenW - 5) {
			x1 = screenW + 4;
		}

		if (y1 >= screenH - 5) {
			y1 = screenH + 4;
		}

		CFHGuiDraw.floatingPanel(g, x0, y0, x1 - x0, y1 - y0, alpha);
	}

	static void card(GuiGraphics g, int x, int y, int w, int h) {
		Theme t = Theme.current();
		int r = Math.min(t.radius(), 3);
		CFHGuiDraw.shadow(g, x, y, w, h, Colors.withAlpha(0, 112), r);
		CFHGuiDraw.pixelRound(g, x, y, w, h, -3750202, r);
		CFHGuiDraw.hline(g, x + r, y, w - 2 * r, -1776412);
		CFHGuiDraw.vline(g, x, y + r, h - 2 * r, -2434342);
		CFHGuiDraw.hline(g, x + r, y + h - 1, w - 2 * r, -6513508);
		CFHGuiDraw.vline(g, x + w - 1, y + r, h - 2 * r, -5723992);
	}

	static void chip(GuiGraphics g, int x, int y, int w, int h, float hover) {
		Palette p = Theme.current().palette();
		CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.lerp(p.surface(), p.surfaceHover(), hover), 2);
		CFHGuiDraw.outline(g, x, y, w, h, Colors.lerp(p.border(), p.borderStrong(), hover), 2);
	}

	private LookDraw() {
	}

	static enum Tone {
		GHOST,
		SOLID,
		ON;
	}
}
