package com.onehumanawa.cfhcore.content.gui.earlywindow.scene;

final class Sheet {
	static final int SIDE = 1024;
	private final int[] px = new int[1048576];
	private int nextX;
	private int shelfY = 257;
	private int shelfH;
	private boolean changed = true;

	Sheet() {
		int[] own = Atlas.paint();

		for (int y = 0; y < 256; y++) {
			System.arraycopy(own, y * 256, this.px, y * 1024, 256);
		}
	}

	Sheet.Region add(int[] argb, int w, int h) {
		if (w + 2 > 1024) {
			return null;
		} else {
			if (this.nextX + w + 2 > 1024) {
				this.shelfY = this.shelfY + this.shelfH;
				this.nextX = 0;
				this.shelfH = 0;
			}

			if (this.shelfY + h + 2 > 1024) {
				return null;
			} else {
				int x0 = this.nextX + 1;
				int y0 = this.shelfY + 1;
				boolean sheer = false;

				for (int y = -1; y <= h; y++) {
					for (int x = -1; x <= w; x++) {
						int sx = Math.max(0, Math.min(w - 1, x));
						int sy = Math.max(0, Math.min(h - 1, y));
						int c = argb[sy * w + sx];
						this.px[(y0 + y) * 1024 + x0 + x] = c;
						int a = c >>> 24;
						if (a > 12 && a < 243) {
							sheer = true;
						}
					}
				}

				this.nextX += w + 2;
				this.shelfH = Math.max(this.shelfH, h + 2);
				this.changed = true;
				return new Sheet.Region(x0, y0, w, h, sheer);
			}
		}
	}

	int[] pixels() {
		return this.px;
	}

	boolean takeChanged() {
		boolean was = this.changed;
		this.changed = false;
		return was;
	}

	static record Region(int x, int y, int w, int h, boolean sheer) {
		float u(float u) {
			return ((float)this.x + u / 16.0F * (float)this.w) / 1024.0F;
		}

		float v(float v) {
			return ((float)this.y + v / 16.0F * (float)this.h) / 1024.0F;
		}
	}
}
