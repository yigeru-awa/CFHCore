package com.onehumanawa.cfhcore.content.gui.quests.client.vanilla;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

final class WindowArt {
	static final int LEFT = 9;
	static final int TOP = 18;
	static final int RIGHT = 9;
	static final int BOTTOM = 9;
	private static final int TEX_W = 252;
	private static final int TEX_H = 140;
	private static final int CUT_L = 16;
	private static final int CUT_T = 24;
	private static final int CUT_R = 16;
	private static final int CUT_B = 16;
	static final int TITLE_INK = -12566464;
	static final ResourceLocation WINDOW = ResourceLocation.withDefaultNamespace("textures/gui/advancements/window.png");
	private static final ResourceLocation TITLE_BOX = sprite("advancements/title_box");
	private static final ResourceLocation BOX_OBTAINED = sprite("advancements/box_obtained");
	private static final ResourceLocation BOX_UNOBTAINED = sprite("advancements/box_unobtained");
	private static final ResourceLocation SLOT = sprite("container/slot");
	private static final ResourceLocation XP_BACKGROUND = sprite("hud/experience_bar_background");
	private static final ResourceLocation XP_PROGRESS = sprite("hud/experience_bar_progress");
	private static final ResourceLocation PAGE_BACK = sprite("recipe_book/page_backward");
	private static final ResourceLocation PAGE_BACK_LIT = sprite("recipe_book/page_backward_highlighted");
	private static final ResourceLocation PAGE_FORWARD = sprite("recipe_book/page_forward");
	private static final ResourceLocation PAGE_FORWARD_LIT = sprite("recipe_book/page_forward_highlighted");
	private static final ResourceLocation CROSS = sprite("widget/cross_button");
	private static final ResourceLocation CROSS_LIT = sprite("widget/cross_button_highlighted");
	static final ResourceLocation SEARCH = sprite("icon/search");
	static final ResourceLocation CHECKMARK = sprite("icon/checkmark");
	private static final ResourceLocation[][] FRAMES = new ResourceLocation[Quest.Frame.values().length][2];

	private static ResourceLocation sprite(String path) {
		return ResourceLocation.withDefaultNamespace(path);
	}

	private static boolean vanilla() {
		return Theme.current().isVanilla();
	}

	static void window(GuiGraphics g, int x, int y, int w, int h, float alpha) {
		if (!(alpha <= 0.004F) && w >= 32 && h >= 40) {
			Theme t = Theme.current();
			if (!t.isVanilla()) {
				Palette p = t.palette();
				int fill = Colors.scaleAlpha(t.chrome(p.bg2()), alpha);
				int line = Colors.scaleAlpha(p.border(), alpha);
				int r = Math.min(t.radius(), 3);
				CFHGuiDraw.shadow(g, x, y, w, h, 0.5F * alpha);

				for (int i = 0; i < r; i++) {
					g.fill(x + r - i, y + i, x + w - r + i, y + i + 1, fill);
					g.fill(x + r - i, y + h - 1 - i, x + w - r + i, y + h - i, fill);
				}

				g.fill(x, y + r, x + w, y + 18, fill);
				g.fill(x, y + h - 9, x + w, y + h - r, fill);
				g.fill(x, y + 18, x + 9, y + h - 9, fill);
				g.fill(x + w - 9, y + 18, x + w, y + h - 9, fill);
				CFHGuiDraw.outline(g, x, y, w, h, line, r);
				CFHGuiDraw.hline(g, x + r + 1, y + 1, w - 2 * r - 2, Colors.scaleAlpha(318767103, alpha));
				CFHGuiDraw.outline(g, x + 9 - 1, y + 18 - 1, w - 9 - 9 + 2, h - 18 - 9 + 2, line, 0);
			} else {
				g.setColor(1.0F, 1.0F, 1.0F, alpha);
				RenderSystem.enableBlend();
				int midW = w - 16 - 16;
				int midH = h - 24 - 16;
				int texMidW = 220;
				int texMidH = 100;
				g.blit(WINDOW, x, y, 0.0F, 0.0F, 16, 24, 256, 256);
				g.blit(WINDOW, x + w - 16, y, 236.0F, 0.0F, 16, 24, 256, 256);
				g.blit(WINDOW, x, y + h - 16, 0.0F, 124.0F, 16, 16, 256, 256);
				g.blit(WINDOW, x + w - 16, y + h - 16, 236.0F, 124.0F, 16, 16, 256, 256);

				for (int at = 0; at < midW; at += 220) {
					int piece = Math.min(220, midW - at);
					g.blit(WINDOW, x + 16 + at, y, 16.0F, 0.0F, piece, 24, 256, 256);
					g.blit(WINDOW, x + 16 + at, y + h - 16, 16.0F, 124.0F, piece, 16, 256, 256);
				}

				for (int at = 0; at < midH; at += 100) {
					int piece = Math.min(100, midH - at);
					g.blit(WINDOW, x, y + 24 + at, 0.0F, 24.0F, 16, piece, 256, 256);
					g.blit(WINDOW, x + w - 16, y + 24 + at, 236.0F, 24.0F, 16, piece, 256, 256);
				}

				g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
				RenderSystem.disableBlend();
			}
		}
	}

	static int titleInk() {
		return vanilla() ? -12566464 : Theme.current().palette().text();
	}

	static int titleInkQuiet() {
		return vanilla() ? -10461088 : Theme.current().palette().textMuted();
	}

	static void tab(GuiGraphics g, WindowArt.Side side, int x, int y, int place, boolean selected, float hover, float alpha) {
		if (!(alpha <= 0.004F)) {
			Theme t = Theme.current();
			if (t.isVanilla()) {
				RenderSystem.enableBlend();
				g.setColor(1.0F, 1.0F, 1.0F, alpha);
				g.blitSprite(side.sprites[place][selected ? 1 : 0], x, y, side.w, side.h);
				if (!selected && hover > 0.01F) {
					g.setColor(1.0F, 1.0F, 1.0F, alpha * 0.45F * hover);
					g.blitSprite(side.sprites[place][1], x, y, side.w, side.h);
				}

				g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
				RenderSystem.disableBlend();
			} else {
				Palette p = t.palette();
				int bx = x;
				int by = y;
				int bw = side.w;
				int bh = side.h;
				int sink = selected ? 0 : 2;
				switch (side) {
					case ABOVE:
						by = y + sink;
						bh = 29 - sink;
						break;
					case BELOW:
						by = y + 3;
						bh = 29 - sink;
						break;
					case LEFT:
						bx = x + sink;
						bw = 29 - sink;
						break;
					case RIGHT:
						bx = x + 3;
						bw = 29 - sink;
				}

				int fill = selected ? t.chrome(p.bg2()) : Colors.lerp(t.chrome(p.bg()), t.chrome(p.surfaceHover()), hover);
				int line = selected ? p.borderStrong() : Colors.lerp(p.border(), p.borderStrong(), hover);
				CFHGuiDraw.pixelRound(g, bx, by, bw, bh, Colors.scaleAlpha(fill, alpha), 2);
				CFHGuiDraw.outline(g, bx, by, bw, bh, Colors.scaleAlpha(line, alpha), 2);
				int seam = Colors.scaleAlpha(selected ? t.chrome(p.bg2()) : p.border(), alpha);
				int cap = Colors.scaleAlpha(t.accent(), alpha);
				switch (side) {
					case ABOVE:
						g.fill(bx + (selected ? 1 : 0), by + bh - 1, bx + bw - (selected ? 1 : 0), by + bh, seam);
						if (selected) {
							g.fill(bx + 3, by + 1, bx + bw - 3, by + 3, cap);
						}
						break;
					case BELOW:
						g.fill(bx + (selected ? 1 : 0), by, bx + bw - (selected ? 1 : 0), by + 1, seam);
						if (selected) {
							g.fill(bx + 3, by + bh - 3, bx + bw - 3, by + bh - 1, cap);
						}
						break;
					case LEFT:
						g.fill(bx + bw - 1, by + (selected ? 1 : 0), bx + bw, by + bh - (selected ? 1 : 0), seam);
						if (selected) {
							g.fill(bx + 1, by + 3, bx + 3, by + bh - 3, cap);
						}
						break;
					case RIGHT:
						g.fill(bx, by + (selected ? 1 : 0), bx + 1, by + bh - (selected ? 1 : 0), seam);
						if (selected) {
							g.fill(bx + bw - 3, by + 3, bx + bw - 1, by + bh - 3, cap);
						}
				}
			}
		}
	}

	static void pageArrow(GuiGraphics g, int x, int y, boolean forward, float lit, boolean enabled, float alpha) {
		if (!(alpha <= 0.004F)) {
			Theme t = Theme.current();
			if (t.isVanilla()) {
				RenderSystem.enableBlend();
				float shade = enabled ? 1.0F : 0.55F;
				g.setColor(shade, shade, shade, alpha * (enabled ? 1.0F : 0.7F));
				g.blitSprite(forward ? PAGE_FORWARD : PAGE_BACK, x, y, 12, 17);
				if (enabled && lit > 0.01F) {
					g.setColor(1.0F, 1.0F, 1.0F, alpha * lit);
					g.blitSprite(forward ? PAGE_FORWARD_LIT : PAGE_BACK_LIT, x, y, 12, 17);
				}

				g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
				RenderSystem.disableBlend();
			} else {
				Palette p = t.palette();
				int fill = !enabled ? Colors.withAlpha(p.surface(), 128) : Colors.lerp(p.surface(), p.surfaceHover(), lit);
				CFHGuiDraw.pixelRound(g, x, y, 12, 17, Colors.scaleAlpha(fill, alpha), 2);
				CFHGuiDraw.outline(g, x, y, 12, 17, Colors.scaleAlpha(Colors.lerp(p.border(), p.borderStrong(), lit), alpha), 2);
				int ink = Colors.scaleAlpha(!enabled ? p.textDim() : Colors.lerp(p.textMuted(), p.text(), lit), alpha);

				for (int i = 0; i < 4; i++) {
					int px = forward ? x + 4 + i : x + 7 - i;
					g.fill(px, y + 5 + i, px + 1, y + 12 - i, ink);
				}
			}
		}
	}

	static void cross(GuiGraphics g, int x, int y, float lit, float alpha) {
		if (!(alpha <= 0.004F)) {
			Theme t = Theme.current();
			if (t.isVanilla()) {
				RenderSystem.enableBlend();
				g.setColor(1.0F, 1.0F, 1.0F, alpha);
				g.blitSprite(CROSS, x, y, 14, 14);
				if (lit > 0.01F) {
					g.setColor(1.0F, 1.0F, 1.0F, alpha * lit);
					g.blitSprite(CROSS_LIT, x, y, 14, 14);
				}

				g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
				RenderSystem.disableBlend();
			} else {
				Palette p = t.palette();
				if (lit > 0.01F) {
					CFHGuiDraw.pixelRound(g, x, y, 14, 14, Colors.scaleAlpha(p.surfaceHover(), alpha * lit), 2);
				}

				int ink = Colors.scaleAlpha(Colors.lerp(p.textMuted(), p.text(), lit), alpha);

				for (int i = 0; i < 6; i++) {
					g.fill(x + 4 + i, y + 4 + i, x + 5 + i, y + 5 + i, ink);
					g.fill(x + 9 - i, y + 4 + i, x + 10 - i, y + 5 + i, ink);
				}
			}
		}
	}

	static ResourceLocation frameSprite(Quest.Frame frame, boolean obtained) {
		String kind = switch (frame) {
			case GOAL -> "goal";
			case CHALLENGE -> "challenge";
			default -> "task";
		};
		return sprite("advancements/" + kind + "_frame_" + (obtained ? "obtained" : "unobtained"));
	}

	static int cfhFrameTone(Rules.Status status, int accent, float hover) {
		Palette p = Theme.current().palette();

		int tone = switch (status) {
			case LOCKED, HIDDEN -> Colors.brighten(p.surface(), 0.06F);
			case COMPLETE -> Colors.mix(p.surfaceActive(), p.success(), 0.62F);
			case CLAIMABLE -> Colors.mix(p.surfaceActive(), p.warning(), 0.7F);
			case IN_PROGRESS -> Colors.mix(Colors.brighten(p.surfaceActive(), 0.22F), accent, 0.3F);
			default -> Colors.mix(Colors.brighten(p.surfaceActive(), 0.22F), accent, 0.12F);
		};
		return Colors.brighten(tone, 0.14F * hover) | 0xFF000000;
	}

	static void frame(GuiGraphics g, Quest.Frame frame, Rules.Status status, int accent, int cx, int cy, int half, float light, float hover, float alpha) {
		if (!(alpha <= 0.004F) && half > 0) {
			boolean van = vanilla();
			boolean made = status.done();
			float r = light;
			float gr = light;
			float b = light;
			if (!van) {
				int tone = cfhFrameTone(status, accent, hover);
				r = Math.min(1.0F, (float)Colors.red(tone) / 198.0F) * light;
				gr = Math.min(1.0F, (float)Colors.green(tone) / 198.0F) * light;
				b = Math.min(1.0F, (float)Colors.blue(tone) / 198.0F) * light;
			}

			g.setColor(r, gr, b, alpha);
			RenderSystem.enableBlend();
			g.blitSprite(FRAMES[frame.ordinal()][van && made ? 1 : 0], cx - half, cy - half, half * 2, half * 2);
			g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.disableBlend();
		}
	}

	static void titleBar(GuiGraphics g, int x, int y, int w, float fraction, Rules.Status status, int accent, float light, float alpha) {
		if (!(alpha <= 0.004F) && w >= 20) {
			Theme t = Theme.current();
			int gold = Mth.floor(Mth.clamp(fraction, 0.0F, 1.0F) * (float)w);
			if (fraction >= 1.0F) {
				gold = w;
			} else if (gold < 2) {
				gold = 0;
			} else if (gold > w - 2) {
				gold = w - 2;
			}

			if (!t.isVanilla()) {
				Palette p = t.palette();

				int lit = switch (status) {
					case COMPLETE -> p.success();
					case CLAIMABLE -> p.warning();
					default -> accent;
				};
				int rest = status != Rules.Status.LOCKED && status != Rules.Status.HIDDEN ? p.surfaceActive() : p.surface();
				int by = y + 3;
				int bh = 20;
				int r = Math.min(t.radius(), 3);
				CFHGuiDraw.shadow(g, x, by, w, 20, 0.5F * alpha);
				CFHGuiDraw.pixelRound(g, x, by, w, 20, Colors.scaleAlpha(rest, alpha), r);
				if (gold > 0) {
					g.enableScissor(x, by, x + gold, by + 20);
					CFHGuiDraw.pixelRound(g, x, by, w, 20, Colors.scaleAlpha(Colors.mix(rest, lit, 0.55F), alpha), r);
					g.disableScissor();
				}

				CFHGuiDraw.outline(g, x, by, w, 20, Colors.scaleAlpha(Colors.mix(p.borderStrong(), lit, status.done() ? 0.6F : 0.25F), alpha), r);
			} else {
				if (gold > 0) {
					bar(g, BOX_OBTAINED, x, y, w, 0, gold, light, alpha);
				}

				if (gold < w) {
					bar(g, BOX_UNOBTAINED, x, y, w, gold, w, light, alpha);
				}
			}
		}
	}

	private static void bar(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int from, int to, float light, float alpha) {
		g.enableScissor(x + from, y, x + to, y + 26);
		g.setColor(light, light, light, alpha);
		RenderSystem.enableBlend();
		int end = Math.min(10, w / 2);
		g.blitSprite(sprite, 200, 26, 0, 0, x, y, end, 26);
		g.blitSprite(sprite, 200, 26, 200 - end, 0, x + w - end, y, end, 26);

		for (int at = end; at < w - end; at += 180) {
			g.blitSprite(sprite, 200, 26, 10, 0, x + at, y, Math.min(180, w - end - at), 26);
		}

		g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
		g.disableScissor();
	}

	static void box(GuiGraphics g, int x, int y, int w, int h, float alpha) {
		if (!(alpha <= 0.004F) && w > 0 && h > 6) {
			Theme t = Theme.current();
			if (!t.isVanilla()) {
				Palette p = t.palette();
				CFHGuiDraw.popupPlate(g, x, y + 3, w, h - 6, p.surface(), p.borderStrong(), 0.6F, alpha);
			} else {
				g.setColor(1.0F, 1.0F, 1.0F, alpha);
				RenderSystem.enableBlend();
				g.blitSprite(TITLE_BOX, x, y, w, h);
				g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
				RenderSystem.disableBlend();
			}
		}
	}

	static void slot(GuiGraphics g, int x, int y, float alpha) {
		if (!(alpha <= 0.004F)) {
			Theme t = Theme.current();
			if (!t.isVanilla()) {
				Palette p = t.palette();
				CFHGuiDraw.pixelRound(g, x, y, 18, 18, Colors.scaleAlpha(Colors.mix(p.bg(), -16777216, 0.25F), alpha), 2);
				CFHGuiDraw.outline(g, x, y, 18, 18, Colors.scaleAlpha(p.border(), alpha), 2);
			} else {
				g.setColor(1.0F, 1.0F, 1.0F, alpha);
				RenderSystem.enableBlend();
				g.blitSprite(SLOT, x, y, 18, 18);
				g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
				RenderSystem.disableBlend();
			}
		}
	}

	static void well(GuiGraphics g, int x, int y, int w, int h, float focus, float alpha) {
		if (!(alpha <= 0.004F)) {
			Theme t = Theme.current();
			if (!t.isVanilla()) {
				Palette p = t.palette();
				CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(p.bg(), p.surface(), focus), alpha), 2);
				CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(p.border(), t.accent(), focus), alpha), 2);
			} else {
				g.fill(x, y, x + w, y + h, Colors.scaleAlpha(-7631989, alpha));
				g.fill(x, y, x + w - 1, y + 1, Colors.scaleAlpha(-13158601, alpha));
				g.fill(x, y, x + 1, y + h - 1, Colors.scaleAlpha(-13158601, alpha));
				g.fill(x + 1, y + h - 1, x + w, y + h, Colors.scaleAlpha(-1, alpha));
				g.fill(x + w - 1, y + 1, x + w, y + h, Colors.scaleAlpha(-1, alpha));
			}
		}
	}

	static void sprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h, float alpha) {
		if (!(alpha <= 0.004F)) {
			g.setColor(1.0F, 1.0F, 1.0F, alpha);
			RenderSystem.enableBlend();
			g.blitSprite(sprite, x, y, w, h);
			g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.disableBlend();
		}
	}

	static void progress(GuiGraphics g, int x, int y, int w, float fraction, boolean complete, float alpha) {
		if (!(alpha <= 0.004F) && w >= 6) {
			Theme t = Theme.current();
			float f = Mth.clamp(fraction, 0.0F, 1.0F);
			if (!t.isVanilla()) {
				Palette p = t.palette();
				CFHGuiDraw.pixelRound(g, x, y, w, 5, Colors.scaleAlpha(p.surfaceActive(), alpha), 1);
				int fw = f <= 0.0F ? 0 : Math.max(2, Math.round((float)w * f));
				if (fw > 0) {
					CFHGuiDraw.pixelRound(g, x, y, fw, 5, Colors.scaleAlpha(complete ? p.success() : t.accent(), alpha), 1);
					if (fw > 2) {
						CFHGuiDraw.hline(g, x + 1, y, fw - 2, Colors.scaleAlpha(956301311, alpha));
					}
				}
			} else {
				int bw = Math.min(w, 182);
				g.setColor(1.0F, 1.0F, 1.0F, alpha);
				RenderSystem.enableBlend();
				g.blitSprite(XP_BACKGROUND, 182, 5, 0, 0, x, y, bw - 2, 5);
				g.blitSprite(XP_BACKGROUND, 182, 5, 180, 0, x + bw - 2, y, 2, 5);
				int fw = f <= 0.0F ? 0 : (f >= 1.0F ? bw : Mth.clamp(Math.round((float)bw * f), 2, bw - 2));
				if (fw > 0) {
					g.blitSprite(XP_PROGRESS, 182, 5, 0, 0, x, y, Math.min(fw, bw - 2), 5);
					if (fw >= bw) {
						g.blitSprite(XP_PROGRESS, 182, 5, 180, 0, x + bw - 2, y, 2, 5);
					}
				}

				g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
				RenderSystem.disableBlend();
			}
		}
	}

	private WindowArt() {
	}

	static {
		for (Quest.Frame f : Quest.Frame.values()) {
			FRAMES[f.ordinal()][0] = frameSprite(f, false);
			FRAMES[f.ordinal()][1] = frameSprite(f, true);
		}
	}

	static enum Side {
		ABOVE("above", "left", "right", 28, 32, 6, 9),
		BELOW("below", "left", "right", 28, 32, 6, 6),
		LEFT("left", "top", "bottom", 32, 28, 10, 5),
		RIGHT("right", "top", "bottom", 32, 28, 6, 5);

		final int w;
		final int h;
		final int iconX;
		final int iconY;
		private final ResourceLocation[][] sprites = new ResourceLocation[3][2];

		private Side(final String name, final String first, final String last, final int w, final int h, final int iconX, final int iconY) {
			this.w = w;
			this.h = h;
			this.iconX = iconX;
			this.iconY = iconY;
			String[] places = new String[]{first, "middle", last};

			for (int i = 0; i < 3; i++) {
				this.sprites[i][0] = WindowArt.sprite("advancements/tab_" + name + "_" + places[i]);
				this.sprites[i][1] = WindowArt.sprite("advancements/tab_" + name + "_" + places[i] + "_selected");
			}
		}

		boolean horizontal() {
			return this == ABOVE || this == BELOW;
		}

		int pitch() {
			return this.horizontal() ? 32 : 28;
		}
	}
}
