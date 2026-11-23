package com.onehumanawa.cfhcore.content.gui.teams.client.hud;

final class LabelStack {
	private static final int MAX = 64;
	private static final int GAP = 2;
	private static final int[] RECTS = new int[256];
	private static int count;

	static void begin() {
		count = 0;
	}

	static int place(int x, int y, int w, int h, boolean up) {
		int at = y;

		for (int pass = 0; pass < 8; pass++) {
			boolean moved = false;

			for (int i = 0; i < count; i++) {
				int rx = RECTS[i * 4];
				int ry = RECTS[i * 4 + 1];
				int rw = RECTS[i * 4 + 2];
				int rh = RECTS[i * 4 + 3];
				if (x < rx + rw + 2 && x + w + 2 > rx && at < ry + rh + 2 && at + h + 2 > ry) {
					at = up ? ry - h - 2 : ry + rh + 2;
					moved = true;
				}
			}

			if (!moved) {
				break;
			}
		}

		if (count < 64) {
			RECTS[count * 4] = x;
			RECTS[count * 4 + 1] = at;
			RECTS[count * 4 + 2] = w;
			RECTS[count * 4 + 3] = h;
			count++;
		}

		return at;
	}

	private LabelStack() {
	}
}
