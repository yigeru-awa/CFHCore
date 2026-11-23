package com.onehumanawa.cfhcore.content.gui.teams.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ReskinDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

final class Ground {
	private static final int WORLD_SHADE = 1342177280;

	private static boolean inWorld() {
		return Minecraft.getInstance().level != null;
	}

	private static void vanilla(GuiGraphics g, int x, int y, int w, int h) {
		boolean world = inWorld();
		if (world) {
			CFHGuiDraw.rect(g, x, y, w, h, 1342177280);
		}

		CFHGuiDraw.vanillaListBackground(g, x, y, w, h, world);
	}

	static void band(GuiGraphics g, int x, int y, int w, int h) {
		if (w > 0 && h > 0) {
			if (Theme.current().isVanilla()) {
				vanilla(g, x, y, w, h);
			} else {
				ReskinDraw.listBackground(g, x, y, w, h, inWorld());
			}
		}
	}

	static void rule(GuiGraphics g, int x, int y, int w, boolean upper) {
		if (w > 0) {
			if (Theme.current().isVanilla()) {
				CFHGuiDraw.vanillaSeparator(g, x, y, w, upper, inWorld());
			} else {
				CFHGuiDraw.hline(g, x, y + (upper ? 1 : 0), w, Theme.current().palette().border());
			}
		}
	}

	static void divider(GuiGraphics g, int x, int y, int h) {
		if (h > 0) {
			if (Theme.current().isVanilla()) {
				CFHGuiDraw.vline(g, x, y, h, 1711276032);
				CFHGuiDraw.vline(g, x + 1, y, h, 654311423);
			} else {
				CFHGuiDraw.vline(g, x, y, h, Theme.current().palette().border());
			}
		}
	}

	static void plate(GuiGraphics g, int x, int y, int w, int h) {
		if (w > 0 && h > 0) {
			Theme t = Theme.current();
			if (t.isVanilla()) {
				vanilla(g, x, y, w, h);
				CFHGuiDraw.vanillaSeparator(g, x, y - 2, w, true, inWorld());
				CFHGuiDraw.vanillaSeparator(g, x, y + h, w, false, inWorld());
			} else {
				Palette p = t.palette();
				CFHGuiDraw.panel(g, x, y, w, h, t.panel(p.bg2()), p.border());
			}
		}
	}

	private Ground() {
	}
}
