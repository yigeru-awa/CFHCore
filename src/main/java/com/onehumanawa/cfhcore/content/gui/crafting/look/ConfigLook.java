package com.onehumanawa.cfhcore.content.gui.crafting.look;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ReskinDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class ConfigLook {
	public static boolean listBackground(GuiGraphics g, int x, int y, int w, int h) {
		if (!Look.cfh()) {
			return false;
		} else {
			ReskinDraw.listBackground(g, x, y, w, h, Minecraft.getInstance().level != null);
			return true;
		}
	}

	public static boolean listSeparators(GuiGraphics g, int x, int top, int w, int bottom) {
		if (!Look.cfh()) {
			return false;
		} else {
			ReskinDraw.listSeparators(g, x, top, w, bottom, Minecraft.getInstance().level != null);
			return true;
		}
	}

	public static boolean scrollbar(GuiGraphics g, int x, int top, int bottom, int thumbY, int thumbH) {
		if (!Look.cfh()) {
			return false;
		} else {
			ReskinDraw.scrollTrack(g, x, top, 6, bottom - top);
			ReskinDraw.scrollThumb(g, x, thumbY, 6, thumbH);
			return true;
		}
	}

	public static boolean row(GuiGraphics g, int x, int y, int w, int h, Component name, boolean hovered) {
		if (!Look.cfh()) {
			return false;
		} else {
			Theme t = Theme.current();
			Palette p = t.palette();
			CFHGuiDraw.pixelRound(g, x, y, w, h, t.panel(hovered ? p.surfaceHover() : p.surface()), t.radius());
			CFHGuiDraw.outline(g, x, y, w, h, hovered ? p.borderStrong() : p.border(), t.radius());
			g.drawString(CFHGuiDraw.font(), name, x + 6, CFHGuiDraw.textY(y, Math.min(h, 20)), p.text(), false);
			return true;
		}
	}

	public static Component value(Component upstream, boolean on) {
		if (!Look.cfh()) {
			return upstream;
		} else {
			int color = (on ? Look.palette().success() : Look.palette().danger()) & 16777215;
			return upstream.copy().withStyle(s -> s.withColor(color));
		}
	}

	public static int groupLine(int upstream) {
		return Look.cfh() ? Look.palette().borderStrong() : upstream;
	}

	public static boolean group(GuiGraphics g, Component text, int x, int y, int w, boolean collapsed, boolean hovered, boolean centered) {
		if (!Look.cfh()) {
			return false;
		} else {
			Palette p = Look.palette();
			Component title = Fonts.heading(text);
			int textW = CFHGuiDraw.width(title);
			int tx = centered ? x + (w - textW) / 2 : x + 20;
			int color = centered ? p.text() : p.textMuted();
			g.drawString(CFHGuiDraw.font(), title, tx, y + 3, hovered ? Colors.brighten(color, 0.2F) : color, false);
			Icons.draw(g, collapsed ? Icon.CHEVRON_RIGHT : Icon.CHEVRON_DOWN, tx - 12, y + 3, 8, !hovered && !collapsed ? p.textDim() : p.accentInk());
			if (centered) {
				int gap = 18;
				CFHGuiDraw.hline(g, x + 4, y + 7, Math.max(0, tx - 18 - x - 4), p.border());
				CFHGuiDraw.hline(g, tx + textW + 8, y + 7, Math.max(0, x + w - 4 - (tx + textW + 8)), p.border());
			}

			return true;
		}
	}

	public static boolean caption(GuiGraphics g, Component text, int x, int y, int w) {
		if (!Look.cfh()) {
			return false;
		} else {
			Palette p = Look.palette();
			Component title = Fonts.heading(text);
			int textW = CFHGuiDraw.width(title);
			int tx = x + (w - textW) / 2;
			g.drawString(CFHGuiDraw.font(), title, tx, y + 3, p.textMuted(), false);
			CFHGuiDraw.hline(g, x + 4, y + 7, Math.max(0, tx - 8 - x - 4), p.border());
			CFHGuiDraw.hline(g, tx + textW + 8, y + 7, Math.max(0, x + w - 4 - (tx + textW + 8)), p.border());
			return true;
		}
	}

	public static boolean title(GuiGraphics g, int centerX, int y) {
		if (!Look.cfh()) {
			return false;
		} else {
			Palette p = Look.palette();
			Component title = Fonts.heading(Component.translatable("cfhgui_crafting.look.settings"));
			int w = CFHGuiDraw.width(title);
			g.drawString(CFHGuiDraw.font(), title, centerX - w / 2, y, p.text(), false);
			CFHGuiDraw.accentCap(g, centerX - w / 2, y + 11, Math.max(16, Math.min(w, 48)), 1.0F);
			return true;
		}
	}

	private ConfigLook() {
	}
}
