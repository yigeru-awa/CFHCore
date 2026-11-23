package com.onehumanawa.cfhcore.content.gui.quests.client.view;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public interface QuestPainter {
	static int accentOf(@Nullable Chapter chapter) {
		int theme = Theme.current().accent();
		return chapter != null && !chapter.accent.isEmpty() ? Colors.fromHex(chapter.accent, theme) | 0xFF000000 : theme;
	}

	default float unit() {
		return 30.0F;
	}

	default float radius() {
		return 11.0F;
	}

	void backdrop(GuiGraphics var1, QuestPainter.View var2);

	default void decoration(GuiGraphics g, QuestPainter.View view, Chapter.Decoration decoration, float sx, float sy, float scale) {
	}

	void link(GuiGraphics var1, QuestPainter.View var2, LinkView var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10);

	void node(GuiGraphics var1, QuestPainter.View var2, NodeView var3, float var4, float var5, float var6, float var7);

	default void beginNodes(GuiGraphics g, QuestPainter.View view) {
	}

	default void endNodes(GuiGraphics g, QuestPainter.View view) {
	}

	default void opened(GuiGraphics g, QuestPainter.View view, NodeView node, float sx, float sy, float scale, float t) {
	}

	default void celebrated(GuiGraphics g, QuestPainter.View view, NodeView node, float sx, float sy, float scale, float t) {
	}

	default void frame(GuiGraphics g, QuestPainter.View view) {
	}

	void card(GuiGraphics var1, QuestPainter.View var2, NodeView var3, float var4, float var5, float var6, int var7, float var8, Rect var9);

	void previewPlate(GuiGraphics var1, QuestPainter.View var2, SubtreePreview var3, int var4, int var5, int var6, int var7, float var8);

	default void inlayPlate(
		GuiGraphics g, QuestPainter.View view, Inlay inlay, int x, int y, int w, int h, float hangX, float hangY, float scale, float open, float words, float alpha
	) {
		if (!(alpha <= 0.01F) && w >= 2 && h >= 2) {
			Theme t = Theme.current();
			Palette p = t.palette();
			boolean van = t.isVanilla();
			int accent = van ? -1 : view.accent();
			int fill = van ? -15461356 : Colors.mix(p.bg(), -16777216, 0.2F) | 0xFF000000;
			inlay.ground = fill;
			if (van) {
				g.fill(x - 1, y - 1, x + w + 1, y + h + 1, Colors.scaleAlpha(-16777216, alpha));
				g.fill(x, y, x + w, y + h, Colors.scaleAlpha(fill, alpha));
				CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(-7631989, alpha), 0);
			} else {
				CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(fill, alpha), t.radius());
				CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(Colors.mix(p.borderStrong(), accent, 0.55F), alpha), t.radius());
			}

			int head = Math.round(0.75F * this.unit() * scale);
			int hx = Math.round(hangX);
			g.fill(hx - 1, Math.round(hangY), hx + 1, Math.min(y + h, y + head), Colors.scaleAlpha(accent, alpha));
			if (!(words <= 0.02F) && !(scale < 0.55F) && h >= head) {
				int left = hx - 4 - (x + 6);
				int right = x + w - 6 - (hx + 5);
				int room = Math.max(left, right);
				if (room >= 30) {
					g.drawString(
						CFHGuiDraw.font(),
						CFHGuiDraw.truncate(inlay.title, room),
						left >= right ? x + 6 : hx + 5,
						y + Math.max(2, (head - 8) / 2),
						Colors.scaleAlpha(van ? -1 : p.text(), words * alpha),
						van
					);
				}
			}
		}
	}

	default void stem(GuiGraphics g, QuestPainter.View view, LinkView stem, float[] xs, float[] ys, int points, float scale, float alpha, float grown) {
		if (!(alpha <= 0.01F) && !(grown <= 0.01F)) {
			int color = Colors.scaleAlpha(Theme.current().isVanilla() ? -1 : view.accent(), alpha * (0.45F + 0.55F * stem.lit()));
			float total = 0.0F;

			for (int i = 1; i < points; i++) {
				total += Math.abs(xs[i] - xs[i - 1]) + Math.abs(ys[i] - ys[i - 1]);
			}

			float left = total * Math.min(1.0F, grown);

			for (int i = 1; i < points && left > 0.0F; i++) {
				float len = Math.abs(xs[i] - xs[i - 1]) + Math.abs(ys[i] - ys[i - 1]);
				float k = len <= 0.0F ? 1.0F : Math.min(1.0F, left / len);
				CanvasDraw.line(g, xs[i - 1], ys[i - 1], xs[i - 1] + (xs[i] - xs[i - 1]) * k, ys[i - 1] + (ys[i] - ys[i - 1]) * k, scale < 0.85F ? 1.0F : 2.0F, color);
				left -= len;
			}
		}
	}

	default void veil(GuiGraphics g, QuestPainter.View view, float alpha) {
		if (!(alpha <= 0.004F)) {
			Rect a = view.area();
			Theme t = Theme.current();
			g.fill(a.x(), a.y(), a.right(), a.bottom(), Colors.scaleAlpha(t.isVanilla() ? -15724528 : Colors.mix(t.bg(), -16777216, 0.25F), alpha));
		}
	}

	default void control(GuiGraphics g, QuestPainter.View view, QuestPainter.Control control, int x, int y, int size, float hover, boolean enabled) {
		Theme t = Theme.current();
		Palette p = t.palette();

		Icon icon = switch (control) {
			case ZOOM_IN -> Icon.PLUS;
			case ZOOM_OUT -> Icon.MINUS;
			case FIT -> Icon.FULLSCREEN;
		};
		g.flush();
		if (t.isVanilla()) {
			CFHGuiDraw.vanillaButton(g, x, y, size, size, hover, enabled, 1.0F);
			Icons.draw(g, icon, x + (size - 8) / 2, y + (size - 8) / 2, 8, enabled ? -1 : -6250336);
		} else {
			int fill = Colors.lerp(p.surface(), p.surfaceHover(), hover);
			int border = Colors.lerp(p.border(), p.borderStrong(), hover);
			CFHGuiDraw.shadow(g, x, y, size, size, 0.4F);
			CFHGuiDraw.pixelRound(g, x, y, size, size, enabled ? fill : Colors.withAlpha(p.surface(), 176), 2);
			CFHGuiDraw.outline(g, x, y, size, size, border, 2);
			g.flush();
			Icons.draw(g, icon, x + (size - 8) / 2, y + (size - 8) / 2, 8, !enabled ? p.textDim() : Colors.lerp(p.textMuted(), p.text(), hover));
		}
	}

	default void readout(GuiGraphics g, QuestPainter.View view, Component text, int rightX, int y, float alpha) {
		if (!(alpha <= 0.02F)) {
			Theme t = Theme.current();
			int w = CFHGuiDraw.width(text) + 8;
			if (t.isVanilla()) {
				g.fill(rightX - w, y, rightX, y + 12, Colors.scaleAlpha(-1073741824, alpha));
			} else {
				CFHGuiDraw.pixelRound(g, rightX - w, y, w, 12, Colors.scaleAlpha(Colors.withAlpha(t.palette().bg(), 208), alpha), 2);
			}

			g.drawString(CFHGuiDraw.font(), text, rightX - w + 4, y + 2, Colors.scaleAlpha(t.isVanilla() ? -1 : t.palette().textMuted(), alpha), t.isVanilla());
		}
	}

	default void overview(GuiGraphics g, QuestPainter.View view, GraphModel graph, QuestPainter.Overview o, float alpha) {
		if (!(alpha <= 0.02F) && !graph.isEmpty()) {
			Theme t = Theme.current();
			Palette p = t.palette();
			boolean van = t.isVanilla();
			int x = o.x();
			int y = o.y();
			int w = o.w();
			int h = o.h();
			if (van) {
				g.fill(x, y, x + w, y + h, Colors.scaleAlpha(-134217728, alpha));
				CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(-1, alpha * 0.6F), 0);
			} else {
				CFHGuiDraw.shadow(g, x, y, w, h, 0.4F * alpha);
				CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(Colors.withAlpha(p.bg(), 248), alpha), 2);
				CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(p.border(), alpha), 2);
			}

			int dot = o.scale() >= 3.0F ? 2 : 1;

			for (NodeView n : graph.nodes) {
				int color = switch (n.status) {
					case COMPLETE -> van ? -11141291 : p.success();
					case CLAIMABLE -> van ? -22016 : p.warning();
					case AVAILABLE, IN_PROGRESS -> van ? -1 : view.accent();
					default -> van ? -9474193 : p.borderStrong();
				};
				int px = Math.round(o.sx(n.x));
				int py = Math.round(o.sy(n.y));
				if (px >= x + 1 && py >= y + 1 && px + dot <= x + w - 1 && py + dot <= y + h - 1) {
					g.fill(px, py, px + dot, py + dot, Colors.scaleAlpha(color, alpha));
				}
			}

			int rx0 = Math.max(x + 1, Math.round(o.sx(o.vx0())));
			int ry0 = Math.max(y + 1, Math.round(o.sy(o.vy0())));
			int rx1 = Math.min(x + w - 1, Math.round(o.sx(o.vx1())));
			int ry1 = Math.min(y + h - 1, Math.round(o.sy(o.vy1())));
			if (rx1 - rx0 >= 2 && ry1 - ry0 >= 2) {
				g.fill(rx0, ry0, rx1, ry1, Colors.scaleAlpha(Colors.withAlpha(van ? -1 : view.accent(), 34), alpha));
				CFHGuiDraw.outline(g, rx0, ry0, rx1 - rx0, ry1 - ry0, Colors.scaleAlpha(van ? -1 : view.accent(), alpha), 0);
			}
		}
	}

	default void empty(GuiGraphics g, QuestPainter.View view, Component text) {
		Theme t = Theme.current();
		Rect a = view.area();
		CFHGuiDraw.textCentered(g, text, a.centerX(), a.centerY() - 4, t.isVanilla() ? -6250336 : t.palette().textDim(), t.isVanilla());
	}

	public static enum Control {
		ZOOM_IN,
		ZOOM_OUT,
		FIT;
	}

	public static record Overview(int x, int y, int w, int h, float scale, float ox, float oy, float vx0, float vy0, float vx1, float vy1) {
		public float sx(float treeX) {
			return this.ox + treeX * this.scale;
		}

		public float sy(float treeY) {
			return this.oy + treeY * this.scale;
		}
	}

	public static record View(Rect area, float zoom, float camX, float camY, float seconds, int mouseX, int mouseY, @Nullable Chapter chapter, int accent) {
		public QuestPainter.View of(@Nullable Chapter other) {
			return new QuestPainter.View(this.area, this.zoom, this.camX, this.camY, this.seconds, this.mouseX, this.mouseY, other, QuestPainter.accentOf(other));
		}
	}
}
