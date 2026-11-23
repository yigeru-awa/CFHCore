package com.onehumanawa.cfhcore.content.gui.veinminer.client;

import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.veinminer.VeinShape;
import net.minecraft.client.gui.GuiGraphics;

final class ShapeGlyphs {
	static final int SIZE = 12;
	private static final int[][][] RECTS = new int[][][]{
		{{4, 0, 3, 3}, {2, 3, 5, 2}, {4, 5, 3, 2}, {7, 5, 3, 3}, {0, 7, 3, 3}, {3, 7, 4, 2}, {7, 9, 3, 3}},
		{{3, 0, 6, 1}, {3, 11, 6, 1}, {3, 0, 1, 12}, {8, 0, 1, 12}, {5, 3, 2, 6}},
		{{0, 0, 12, 1}, {0, 11, 12, 1}, {0, 0, 1, 12}, {11, 0, 1, 12}, {4, 4, 4, 4}, {2, 2, 1, 1}, {9, 2, 1, 1}, {2, 9, 1, 1}, {9, 9, 1, 1}},
		{{0, 8, 12, 4}, {4, 4, 8, 4}, {8, 0, 4, 4}},
		{{0, 0, 3, 3}, {4, 0, 4, 3}, {9, 0, 3, 3}, {0, 4, 3, 4}, {4, 4, 4, 4}, {9, 4, 3, 4}, {0, 9, 3, 3}, {4, 9, 4, 3}, {9, 9, 3, 3}},
		{{3, 0, 6, 3}, {3, 4, 6, 4}, {3, 9, 6, 3}}
	};

	static void draw(GuiGraphics g, VeinShape shape, int x, int y, int argb) {
		for (int[] r : RECTS[shape.ordinal()]) {
			CFHGuiDraw.rect(g, x + r[0], y + r[1], r[2], r[3], argb);
		}
	}

	private ShapeGlyphs() {
	}
}
