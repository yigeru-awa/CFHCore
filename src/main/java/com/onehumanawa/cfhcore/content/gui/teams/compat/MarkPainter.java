package com.onehumanawa.cfhcore.content.gui.teams.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class MarkPainter {
	private static final int RIM = -15724526;
	public static final int HALF = 6;

	public static void paint(GuiGraphics g, MapMark mark, boolean lit, float labelScale, float alpha) {
		float a = alpha * (mark.kind == MapMark.Kind.PING ? pingFade(mark.age()) : 1.0F);
		if (!(a <= 0.03F)) {
			int rim = Colors.scaleAlpha(lit ? -1 : -15724526, a);
			int color = Colors.scaleAlpha(0xFF000000 | mark.color, a);
			switch (mark.kind) {
				case TEAMMATE:
					g.fill(-6, -6, 6, 6, rim);
					g.fill(-5, -5, 5, 5, color);
					CFHGuiDraw.playerHead(g, mark.skin(), -4, -4, 8, a);
					if (mark.dead) {
						g.fill(-4, -4, 4, 4, Colors.scaleAlpha(-1341124590, a));
					}

					if (labelScale > 0.0F) {
						label(g, mark.label, 8, false, labelScale, a);
					}
					break;
				case WAYPOINT:
					g.drawManaged(() -> {
						CFHGuiDraw.pixelRound(g, -7, -7, 14, 14, rim, 3);
						CFHGuiDraw.pixelRound(g, -6, -6, 12, 12, color, 2);
					});
					glyph(g, mark.icon, mark.color, a);
					if (labelScale > 0.0F) {
						label(g, mark.label, 9, false, labelScale, a);
					}
					break;
				case PING:
					g.drawManaged(() -> {
						rings(g, mark, a);
						CFHGuiDraw.pixelCircle(g, 0, 0, 6, rim);
						CFHGuiDraw.pixelCircle(g, 0, 0, 5, color);
					});
					glyph(g, mark.icon, mark.color, a);
					if (labelScale > 0.0F) {
						label(g, mark.label, -9, true, labelScale, a);
					}
			}
		}
	}

	private static void glyph(GuiGraphics g, @Nullable Icon icon, int rgb, float alpha) {
		if (icon != null) {
			Icons.draw(g, icon, -4, -4, 8, Colors.scaleAlpha(Colors.readableOn(0xFF000000 | rgb), alpha));
		}
	}

	private static void rings(GuiGraphics g, MapMark mark, float alpha) {
		if (Theme.current().motion() <= 0.0F) {
			ring(g, 10, Colors.scaleAlpha(0xFF000000 | mark.color, 0.7F * alpha));
		} else {
			float t = (float)(System.currentTimeMillis() - mark.bornMs) / 1400.0F;

			for (int i = 0; i < 2; i++) {
				float p = Mth.frac(t + (float)i * 0.5F);
				ring(g, 7 + Math.round(p * 9.0F), Colors.scaleAlpha(0xFF000000 | mark.color, (1.0F - p) * alpha));
			}
		}
	}

	private static void ring(GuiGraphics g, int r, int color) {
		CFHGuiDraw.pixelRing(g, 0, 0, r, color);
		CFHGuiDraw.pixelRing(g, 0, 0, r + 1, color);
	}

	private static float pingFade(float age) {
		return age < 0.85F ? 1.0F : Math.max(0.0F, (1.0F - age) / 0.15F);
	}

	private static void label(GuiGraphics g, Component text, int edge, boolean above, float scale, float alpha) {
		if (!text.getString().isEmpty()) {
			Font font = Minecraft.getInstance().font;
			Theme theme = Theme.current();
			PoseStack pose = g.pose();
			pose.pushPose();
			pose.translate(0.0F, (float)edge, 0.0F);
			pose.scale(scale, scale, 1.0F);
			int w = font.width(text);
			int x = -w / 2;
			if (theme.isVanilla()) {
				int y = above ? -10 : 0;
				g.fill(x - 2, y, x + w + 2, y + 10, Colors.scaleAlpha(-1610612736, alpha));
				g.drawString(font, text, x, y + 1, Colors.scaleAlpha(-1, alpha), true);
			} else {
				int y = above ? -11 : 0;
				int plate = Colors.scaleAlpha(Colors.withAlpha(theme.bg(), 216), alpha);
				g.drawManaged(() -> CFHGuiDraw.pixelRound(g, x - 3, y, w + 6, 11, plate, 1));
				g.drawString(font, text, x, y + 2, Colors.scaleAlpha(theme.text(), alpha), false);
			}

			pose.popPose();
		}
	}

	private MarkPainter() {
	}
}
