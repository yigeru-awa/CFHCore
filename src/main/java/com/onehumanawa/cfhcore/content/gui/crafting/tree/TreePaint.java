package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

final class TreePaint {
	static final String[] CARET_RIGHT = new String[]{"X..", "XX.", "XXX", "XX.", "X.."};
	static final String[] CARET_DOWN = new String[]{"XXXXX", ".XXX.", "..X.."};
	static final String[] TICK = new String[]{"......X", ".....XX", "X...XX.", "XX.XX..", ".XXX...", "..X...."};
	static final String[] TICK_SMALL = new String[]{"....X", "X..XX", "XXXX.", ".XX.."};
	static final String[] DIAMOND = new String[]{"..X..", ".XXX.", "XXXXX", ".XXX.", "..X.."};
	static final String[] DIAMOND_HOLLOW = new String[]{"..X..", ".X.X.", "X...X", ".X.X.", "..X.."};
	static final String[] FOLD = new String[]{"X...X", ".X.X.", "..X..", ".....", "..X..", ".X.X.", "X...X"};
	static final String[] UNFOLD = new String[]{"..X..", ".X.X.", "X...X", ".....", "X...X", ".X.X.", "..X.."};
	static final String[] PLUS = new String[]{"..X..", "..X..", "XXXXX", "..X..", "..X.."};
	static final String[] MINUS = new String[]{"XXXXX"};
	static final String[] CLOCK = new String[]{".XXX.", "X.X.X", "X.XXX", "X...X", ".XXX."};
	static final String[] CLOCK_SMALL = new String[]{".X.", ".XX", "..."};
	static final String[] LINK = new String[]{".XX.XX.", "X..X..X", ".XX.XX."};
	static final String[] LINK_SMALL = new String[]{".X.X.", "X.X.X", ".X.X."};
	static final String[] DOTS = new String[]{"X.X.X"};
	static final String[] GIVEN = new String[]{"XXXXX", "X...X", "X...X", "XXXXX"};
	private static final Map<Long, int[]> OUTLINES = new HashMap<>();
	private static final Map<Integer, int[]> RINGS = new HashMap<>();

	static void pixels(GuiGraphics g, String[] rows, int x, int y, int color) {
		if (color >>> 24 != 0) {
			for (int row = 0; row < rows.length; row++) {
				String s = rows[row];
				int from = -1;

				for (int i = 0; i <= s.length(); i++) {
					boolean on = i < s.length() && s.charAt(i) == 'X';
					if (on && from < 0) {
						from = i;
					} else if (!on && from >= 0) {
						g.fill(x + from, y + row, x + i, y + row + 1, color);
						from = -1;
					}
				}
			}
		}
	}

	static void pixelsCentered(GuiGraphics g, String[] rows, int cx, int cy, int color) {
		pixels(g, rows, cx - rows[0].length() / 2, cy - rows.length / 2, color);
	}

	static boolean vanilla() {
		return Theme.current().isVanilla();
	}

	static int text() {
		return vanilla() ? -1 : Theme.current().palette().text();
	}

	static int muted() {
		return vanilla() ? -3750202 : Theme.current().palette().textMuted();
	}

	static int dim() {
		return vanilla() ? -7631989 : Theme.current().palette().textDim();
	}

	static int accent() {
		return vanilla() ? -1 : Theme.current().palette().accent();
	}

	static int success() {
		return vanilla() ? -11141291 : Theme.current().palette().success();
	}

	static int warning() {
		return vanilla() ? -22016 : Theme.current().palette().warning();
	}

	static int danger() {
		return vanilla() ? -43691 : Theme.current().palette().danger();
	}

	static int rail() {
		return vanilla() ? 1895825407 : Theme.current().palette().borderStrong();
	}

	static int ground() {
		return vanilla() ? -15724528 : Theme.current().palette().bg();
	}

	static void text(GuiGraphics g, FormattedCharSequence text, int x, int y, int color) {
		g.drawString(CFHGuiDraw.font(), text, x, y, color, vanilla());
	}

	static void text(GuiGraphics g, String text, int x, int y, int color) {
		g.drawString(CFHGuiDraw.font(), text, x, y, color, vanilla());
	}

	static void plate(GuiGraphics g, int x, int y, float hover, boolean done, int ring, boolean dashed, float alpha) {
		if (!(alpha <= 0.004F)) {
			Theme t = Theme.current();
			Palette p = t.palette();
			if (t.isVanilla()) {
				g.fill(x, y, x + 18, y + 18, Colors.scaleAlpha(Colors.lerp(-7631989, -5723992, hover), alpha));
				g.fill(x, y, x + 17, y + 1, Colors.scaleAlpha(-13158601, alpha));
				g.fill(x, y, x + 1, y + 17, Colors.scaleAlpha(-13158601, alpha));
				g.fill(x + 1, y + 17, x + 18, y + 18, Colors.scaleAlpha(-1, alpha));
				g.fill(x + 17, y + 1, x + 18, y + 18, Colors.scaleAlpha(-1, alpha));
				if (done) {
					g.fill(x + 1, y + 1, x + 17, y + 17, Colors.scaleAlpha(1347813205, alpha));
				}
			} else {
				int fill = Colors.lerp(p.surface(), p.surfaceActive(), hover);
				int border = Colors.lerp(p.border(), p.borderStrong(), hover);
				if (done) {
					fill = Colors.mix(fill, p.success(), 0.14F);
					border = Colors.mix(border, p.success(), 0.45F);
				}

				CFHGuiDraw.pixelRound(g, x, y, 18, 18, Colors.scaleAlpha(fill, alpha), 2);
				if (ring == 0 || dashed) {
					CFHGuiDraw.outline(g, x, y, 18, 18, Colors.scaleAlpha(border, alpha), 2);
				}
			}

			if (ring != 0) {
				if (dashed) {
					dashes(g, x - 1, y - 1, 20, 20, 3, 3, 2, 0.0F, Colors.scaleAlpha(ring, alpha));
				} else if (t.isVanilla()) {
					CFHGuiDraw.outline(g, x - 1, y - 1, 20, 20, Colors.scaleAlpha(ring, alpha), 0);
				} else {
					CFHGuiDraw.outline(g, x, y, 18, 18, Colors.scaleAlpha(ring, alpha), 2);
				}
			}
		}
	}

	static void rail(GuiGraphics g, int x, int fromY, int y, int toX, int color) {
		if (color >>> 24 != 0) {
			if (y - 1 > fromY) {
				g.fill(x, fromY, x + 1, y - 1, color);
			}

			g.fill(x + 1, y - 1, x + 2, y, color);
			if (toX > x + 2) {
				g.fill(x + 2, y, toX, y + 1, color);
			}
		}
	}

	static void trunk(GuiGraphics g, int x, int fromY, int toY, int color) {
		if (color >>> 24 != 0 && toY > fromY) {
			g.fill(x, fromY, x + 1, toY, color);
		}
	}

	static void railLight(GuiGraphics g, int x, int fromY, int y, int toX, float at, int length, int color) {
		if (color >>> 24 != 0) {
			int across = Math.max(0, toX - (x + 2));
			int down = Math.max(0, y - 1 - fromY);
			int head = Math.round(at);
			int tail = head - length;
			int a0 = Math.max(0, tail);
			int a1 = Math.min(across, head);
			if (a1 > a0) {
				g.fill(toX - a1, y, toX - a0, y + 1, color);
			}

			if (head > across && tail <= across) {
				g.fill(x + 1, y - 1, x + 2, y, color);
			}

			int d0 = Math.max(0, tail - across - 1);
			int d1 = Math.min(down, head - across - 1);
			if (d1 > d0) {
				g.fill(x, y - 1 - d1, x + 1, y - 1 - d0, color);
			}
		}
	}

	static int railLength(int x, int fromY, int y, int toX) {
		return Math.max(0, toX - (x + 2)) + 1 + Math.max(0, y - 1 - fromY);
	}

	private static int[] outline(int w, int h, int radius) {
		int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
		return OUTLINES.computeIfAbsent((long)w << 40 | (long)h << 16 | (long)r, k -> {
			ArrayList<int[]> px = new ArrayList<>();

			for (int x = r; x < w - r; x++) {
				px.add(new int[]{x, 0});
			}

			for (int i = 1; i <= r; i++) {
				px.add(new int[]{w - 1 - (r - i), i});
			}

			for (int y = r + 1; y < h - r; y++) {
				px.add(new int[]{w - 1, y});
			}

			for (int i = 1; i <= r; i++) {
				px.add(new int[]{w - 1 - i, h - 1 - (r - i)});
			}

			for (int x = w - r - 2; x >= r; x--) {
				px.add(new int[]{x, h - 1});
			}

			for (int i = 1; i <= r; i++) {
				px.add(new int[]{r - i, h - 1 - i});
			}

			for (int y = h - r - 2; y > r; y--) {
				px.add(new int[]{0, y});
			}

			if (r > 0) {
				px.add(new int[]{0, r});
			}

			for (int i = 1; i < r; i++) {
				px.add(new int[]{i, r - i});
			}

			int[] out = new int[px.size() * 2];

			for (int i = 0; i < px.size(); i++) {
				out[i * 2] = px.get(i)[0];
				out[i * 2 + 1] = px.get(i)[1];
			}

			return out;
		});
	}

	static void dashes(GuiGraphics g, int x, int y, int w, int h, int radius, int on, int off, float phase, int color) {
		if (color >>> 24 != 0 && w >= 3 && h >= 3) {
			int[] line = outline(w, h, radius);
			int n = line.length / 2;
			int period = on + off;
			int shift = Math.floorMod(Math.round(phase), period);
			int runX = 0;
			int runY = 0;
			int runLen = 0;

			for (int i = 0; i < n; i++) {
				if (Math.floorMod(i - shift, period) < on) {
					int px = x + line[i * 2];
					int py = y + line[i * 2 + 1];
					if (runLen > 0 && py == runY && px == runX + runLen) {
						runLen++;
					} else if (runLen > 0 && py == runY && px == runX - 1) {
						runX--;
						runLen++;
					} else {
						if (runLen > 0) {
							g.fill(runX, runY, runX + runLen, runY + 1, color);
						}

						runX = px;
						runY = py;
						runLen = 1;
					}
				}
			}

			if (runLen > 0) {
				g.fill(runX, runY, runX + runLen, runY + 1, color);
			}
		}
	}

	static void dashesDown(GuiGraphics g, int x, int fromY, int toY, int on, int off, int color) {
		if (color >>> 24 != 0) {
			int y = fromY;

			while (y < toY) {
				g.fill(x, y, x + 1, Math.min(toY, y + on), color);
				y += on + off;
			}
		}
	}

	private static int[] ring(int r) {
		return RINGS.computeIfAbsent(
			r,
			k -> {
				ArrayList<int[]> px = new ArrayList<>();
				float rr = ((float)r + 0.5F) * ((float)r + 0.5F);

				for (int dy = -r; dy <= r; dy++) {
					for (int dx = -r; dx <= r; dx++) {
						if (!((float)(dx * dx + dy * dy) > rr)) {
							boolean edge = (float)((dx - 1) * (dx - 1) + dy * dy) > rr
								|| (float)((dx + 1) * (dx + 1) + dy * dy) > rr
								|| (float)(dx * dx + (dy - 1) * (dy - 1)) > rr
								|| (float)(dx * dx + (dy + 1) * (dy + 1)) > rr;
							if (edge) {
								px.add(new int[]{dx, dy});
							}
						}
					}
				}

				px.sort((a, b) -> Double.compare(clock(a[0], a[1]), clock(b[0], b[1])));
				int[] out = new int[px.size() * 2];

				for (int i = 0; i < px.size(); i++) {
					out[i * 2] = px.get(i)[0];
					out[i * 2 + 1] = px.get(i)[1];
				}

				return out;
			}
		);
	}

	private static double clock(int x, int y) {
		double a = Math.atan2((double)x, (double)(-y));
		return a < 0.0 ? a + (Math.PI * 2) : a;
	}

	static void ring(GuiGraphics g, int cx, int cy, int r, float fraction, int color) {
		if (color >>> 24 != 0 && r >= 1 && !(fraction <= 0.0F)) {
			int[] px = ring(r);
			int n = px.length / 2;
			int upTo = Math.min(n, Math.round((float)n * Mth.clamp(fraction, 0.0F, 1.0F)));

			for (int i = 0; i < upTo; i++) {
				int x = cx + px[i * 2];
				int y = cy + px[i * 2 + 1];
				g.fill(x, y, x + 1, y + 1, color);
			}
		}
	}

	static void ringDashes(GuiGraphics g, int cx, int cy, int r, int on, int off, float phase, int color) {
		if (color >>> 24 != 0 && r >= 1) {
			int[] px = ring(r);
			int n = px.length / 2;
			int period = on + off;
			int shift = Math.floorMod(Math.round(phase), period);

			for (int i = 0; i < n; i++) {
				if (Math.floorMod(i - shift, period) < on) {
					int x = cx + px[i * 2];
					int y = cy + px[i * 2 + 1];
					g.fill(x, y, x + 1, y + 1, color);
				}
			}
		}
	}

	static void bar(GuiGraphics g, int x, int y, int w, int h, float fraction, int track, int lit) {
		if (w > 0) {
			g.fill(x, y, x + w, y + h, track);
			int fw = fraction <= 0.0F ? 0 : Math.max(1, Math.min(w, Math.round((float)w * fraction)));
			if (fw > 0) {
				g.fill(x, y, x + fw, y + h, lit);
			}
		}
	}

	static void badge(GuiGraphics g, int cx, int cy, int color, String[] mark, float alpha) {
		int ink = Colors.scaleAlpha(-15724528, alpha);
		CFHGuiDraw.pixelCircle(g, cx, cy, 4, ink);
		CFHGuiDraw.pixelCircle(g, cx, cy, 3, Colors.scaleAlpha(color, alpha));
		pixelsCentered(g, mark, cx, cy, ink);
	}

	private TreePaint() {
	}
}
