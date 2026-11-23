package com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiAvatar;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiAvatar.Status;
import com.onehumanawa.cfhcore.content.gui.teams.model.BannerData;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public final class TeamsDraw {
	public static void text(GuiGraphics g, Component text, int x, int y, int color, float alpha) {
		if (!(alpha <= 0.004F)) {
			g.drawString(CFHGuiDraw.font(), text, x, y, Colors.scaleAlpha(color, alpha), Theme.current().isVanilla());
		}
	}

	public static void text(GuiGraphics g, FormattedCharSequence text, int x, int y, int color, float alpha) {
		if (!(alpha <= 0.004F)) {
			g.drawString(CFHGuiDraw.font(), text, x, y, Colors.scaleAlpha(color, alpha), Theme.current().isVanilla());
		}
	}

	public static int textFit(GuiGraphics g, Component text, int x, int y, int width, int color, float alpha) {
		FormattedCharSequence seq = CFHGuiDraw.truncate(text, width);
		text(g, seq, x, y, color, alpha);
		return CFHGuiDraw.width(seq);
	}

	public static void textRight(GuiGraphics g, Component text, int right, int y, int color, float alpha) {
		text(g, text, right - CFHGuiDraw.width(text), y, color, alpha);
	}

	public static void textCentered(GuiGraphics g, Component text, int cx, int y, int color, float alpha) {
		text(g, text, cx - CFHGuiDraw.width(text) / 2, y, color, alpha);
	}

	public static boolean cut(Component text, int width) {
		return CFHGuiDraw.width(text) > width;
	}

	public static void card(GuiGraphics g, int x, int y, int w, int h, float lift, float alpha) {
		if (!(alpha <= 0.004F) && w > 0 && h > 0) {
			Theme t = Theme.current();
			Palette p = t.palette();
			if (t.isVanilla()) {
				CFHGuiDraw.rect(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(1610612736, Integer.MIN_VALUE, lift), alpha));
				CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(-12566464, -6250336, lift), alpha), 0);
			} else {
				CFHGuiDraw.plate(g, x, y, w, h, Colors.lerp(p.surface(), p.surfaceHover(), lift), Colors.lerp(p.border(), p.borderStrong(), lift), 0.3F + 0.3F * lift, alpha);
			}
		}
	}

	public static void well(GuiGraphics g, int x, int y, int w, int h, float alpha) {
		if (!(alpha <= 0.004F) && w > 0 && h > 0) {
			Theme t = Theme.current();
			Palette p = t.palette();
			if (t.isVanilla()) {
				CFHGuiDraw.rect(g, x, y, w, h, Colors.scaleAlpha(-1073741824, alpha));
				CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(-16777216, alpha), 0);
				CFHGuiDraw.outline(g, x - 1, y - 1, w + 2, h + 2, Colors.scaleAlpha(-7631989, alpha), 0);
			} else {
				CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(Colors.brighten(p.bg(), -0.25F), alpha), t.radius());
				CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(p.border(), alpha), t.radius());
			}
		}
	}

	public static void wash(GuiGraphics g, int x, int y, int w, int h, int rgb, float alpha) {
		if (!(alpha <= 0.004F)) {
			int strength = Theme.current().isVanilla() ? 64 : 48;
			CFHGuiDraw.hgradient(g, x, y, w, h, Colors.scaleAlpha(Colors.withAlpha(rgb, strength), alpha), Colors.withAlpha(rgb, 0));
		}
	}

	public static int opaque(int rgb) {
		return 0xFF000000 | rgb;
	}

	public static int ink(int rgb) {
		int c = opaque(rgb);
		float floor = Theme.current().isVanilla() ? 0.6F : 0.42F;
		float l = Colors.luminance(c);
		if (l < floor) {
			c = Colors.brighten(c, Math.min(0.75F, (floor - l) * 1.6F));
		}

		return c;
	}

	public static int caption() {
		Theme t = Theme.current();
		return t.isVanilla() ? t.palette().textMuted() : t.palette().textDim();
	}

	public static int bannerHeight(int w) {
		return w * 2 + 2;
	}

	public static void banner(GuiGraphics g, @Nullable BannerData banner, int x, int y, int w, float unfurl, float sway, float alpha) {
		if (!(alpha <= 0.004F)) {
			int h = w * 2;
			float u = Math.max(0.0F, Math.min(1.0F, unfurl));
			int shown = Math.round((float)h * u);
			if (shown > 0) {
				CFHGuiDraw.rect(g, x + 2, y + 4, w, shown, Colors.scaleAlpha(1342177280, alpha));
				int pad = (int)Math.ceil((double)sway) + 1;
				CFHGuiDraw.scissor(g, x - pad, y + 2, w + pad * 2, shown);
				BannerArt.flag(g, banner, (float)x, (float)(y + 2), (float)w, (float)h, alpha, sway);
				CFHGuiDraw.unscissor(g);
			}

			CFHGuiDraw.rect(g, x - 1, y, w + 2, 2, Colors.scaleAlpha(-10730458, alpha));
			CFHGuiDraw.rect(g, x - 1, y, w + 2, 1, Colors.scaleAlpha(-7706564, alpha));
		}
	}

	public static void bannerSmall(GuiGraphics g, @Nullable BannerData banner, int x, int y, int w, float alpha) {
		if (!(alpha <= 0.004F)) {
			CFHGuiDraw.rect(g, x + 1, y + 1, w, w * 2, Colors.scaleAlpha(1342177280, alpha));
			BannerArt.flag(g, banner, (float)x, (float)y, (float)w, alpha);
		}
	}

	public static int chip(GuiGraphics g, Component label, int x, int y, int color, float alpha) {
		return CFHGuiDraw.chip(g, label, x, y, color, alpha);
	}

	public static int chipWidth(Component label) {
		return CFHGuiDraw.width(label) + 8;
	}

	public static int chipRight(GuiGraphics g, Component label, int right, int y, int color, float alpha) {
		int w = chipWidth(label);
		CFHGuiDraw.chip(g, label, right - w, y, color, alpha);
		return right - w;
	}

	public static void rowButton(GuiGraphics g, int x, int y, int w, int h, Component label, boolean primary, boolean hot, float alpha) {
		if (!(alpha <= 0.004F)) {
			Theme t = Theme.current();
			Palette p = t.palette();
			int fg;
			if (t.isVanilla()) {
				CFHGuiDraw.vanillaButton(g, x, y, w, h, hot ? 1.0F : 0.0F, true, alpha);
				if (primary) {
					CFHGuiDraw.rect(g, x + 1, y + 1, w - 2, h - 2, Colors.scaleAlpha(Colors.withAlpha(p.accent(), 90), alpha));
				}

				fg = -1;
			} else if (primary) {
				CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(hot ? p.accentHover() : p.accent(), alpha), t.radius());
				fg = p.accentText();
			} else {
				CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(hot ? p.surfaceActive() : p.surfaceHover(), alpha), t.radius());
				CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(hot ? p.borderStrong() : p.border(), alpha), t.radius());
				fg = hot ? p.text() : p.textMuted();
			}

			FormattedCharSequence seq = CFHGuiDraw.truncate(label, w - 6);
			g.drawString(CFHGuiDraw.font(), seq, x + (w - CFHGuiDraw.width(seq)) / 2, CFHGuiDraw.textY(y, h), Colors.scaleAlpha(fg, alpha), t.isVanilla());
		}
	}

	public static void bar(GuiGraphics g, int x, int y, int w, int h, float fraction, int color, float alpha) {
		if (!(alpha <= 0.004F) && w > 0) {
			Theme t = Theme.current();
			int track = t.isVanilla() ? -16777216 : t.palette().surfaceActive();
			CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(track, alpha), 1);
			int fw = Math.round((float)w * Math.max(0.0F, Math.min(1.0F, fraction)));
			if (fw > 0) {
				CFHGuiDraw.pixelRound(g, x, y, fw, h, Colors.scaleAlpha(color, alpha), 1);
				if (h >= 3 && fw > 2) {
					CFHGuiDraw.hline(g, x + 1, y, fw - 2, Colors.scaleAlpha(Colors.withAlpha(16777215, 56), alpha));
				}
			}
		}
	}

	public static void head(GuiGraphics g, UUID id, String name, int x, int y, int size, @Nullable Boolean online, float alpha) {
		if (!(alpha <= 0.004F)) {
			Status status = online == null ? Status.NONE : (online ? Status.ONLINE : Status.OFFLINE);
			CFHGuiAvatar.draw(g, CFHGuiAvatar.skinFor(id, name), x, y, size, status, alpha);
		}
	}

	public static void glyphTile(GuiGraphics g, Icon icon, int x, int y, int size, int color, float alpha) {
		if (!(alpha <= 0.004F)) {
			Theme t = Theme.current();
			int c = opaque(color);
			if (t.isVanilla()) {
				CFHGuiDraw.rect(g, x, y, size, size, Colors.scaleAlpha(-1610612736, alpha));
				CFHGuiDraw.outline(g, x, y, size, size, Colors.scaleAlpha(Colors.withAlpha(c, 192), alpha), 0);
			} else {
				CFHGuiDraw.pixelRound(g, x, y, size, size, Colors.scaleAlpha(Colors.withAlpha(c, 48), alpha), 2);
				CFHGuiDraw.outline(g, x, y, size, size, Colors.scaleAlpha(Colors.withAlpha(c, 144), alpha), 2);
			}

			int glyph = size >= 20 ? 12 : 8;
			Icons.draw(g, icon, x + (size - glyph) / 2, y + (size - glyph) / 2, glyph, Colors.scaleAlpha(ink(color & 16777215), alpha));
		}
	}

	public static int empty(GuiGraphics g, Rect area, Icon icon, Component title, @Nullable Component body, float alpha) {
		Theme t = Theme.current();
		Palette p = t.palette();
		int bodyW = Math.min(area.w() - 16, 240);
		List<FormattedCharSequence> lines = body == null ? List.of() : CFHGuiDraw.font().split(body, Math.max(40, bodyW));
		int h = 42 + lines.size() * 10;
		int y = area.y() + Math.max(0, (area.h() - h) / 2 - 8);
		int cx = area.centerX();
		if (alpha > 0.004F) {
			int plate = t.isVanilla() ? 1342177280 : Colors.withAlpha(p.surface(), 255);
			CFHGuiDraw.pixelCircle(g, cx, y + 12, 12, Colors.scaleAlpha(plate, alpha));
			if (!t.isVanilla()) {
				CFHGuiDraw.pixelRing(g, cx, y + 12, 12, Colors.scaleAlpha(p.border(), alpha));
			}

			Icons.draw(g, icon, cx - 6, y + 6, 12, Colors.scaleAlpha(t.isVanilla() ? -4144960 : p.textMuted(), alpha));
		}

		y += 30;
		Component head = Fonts.heading(title);
		text(g, CFHGuiDraw.truncate(head, area.w() - 8), cx - Math.min(area.w() - 8, CFHGuiDraw.width(head)) / 2, y, t.isVanilla() ? -1 : p.text(), alpha);
		y += 14;

		for (FormattedCharSequence line : lines) {
			text(g, line, cx - CFHGuiDraw.width(line) / 2, y, p.textMuted(), alpha);
			y += 10;
		}

		return y + 6;
	}

	public static int emptyButtonsY(int areaH, @Nullable Component body, int width) {
		int bodyW = Math.min(width - 16, 240);
		int lines = body == null ? 0 : CFHGuiDraw.font().split(body, Math.max(40, bodyW)).size();
		int h = 42 + lines * 10;
		return Math.max(0, (areaH - h) / 2 - 8) + 30 + 14 + lines * 10 + 6;
	}

	private TeamsDraw() {
	}
}
