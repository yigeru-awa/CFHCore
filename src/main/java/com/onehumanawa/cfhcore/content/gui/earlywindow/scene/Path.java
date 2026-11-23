package com.onehumanawa.cfhcore.content.gui.earlywindow.scene;

final class Path {
	private static final float BEND = 0.3F;
	private final float[] x;
	private final float[] y;
	private final float[] s;
	private final float[] slope;

	Path(float... points) {
		int n = points.length / 2;
		this.x = new float[n];
		this.y = new float[n];
		this.s = new float[n];
		this.slope = new float[n - 1];

		for (int i = 0; i < n; i++) {
			this.x[i] = points[i * 2];
			this.y[i] = points[i * 2 + 1];
			if (i != 0) {
				float dx = this.x[i] - this.x[i - 1];
				float dy = this.y[i] - this.y[i - 1];
				this.s[i] = this.s[i - 1] + (float)Math.sqrt((double)(dx * dx + dy * dy));
				this.slope[i - 1] = (float)Math.toDegrees(Math.atan2((double)dy, (double)dx));
			}
		}
	}

	float length() {
		return this.s[this.s.length - 1];
	}

	float to(int i) {
		return this.s[i];
	}

	void at(float along, float[] out) {
		float a = Math.max(0.0F, Math.min(this.length(), along));
		int i = 0;

		while (i < this.slope.length - 1 && a > this.s[i + 1]) {
			i++;
		}

		float span = this.s[i + 1] - this.s[i];
		float t = span <= 0.0F ? 0.0F : (a - this.s[i]) / span;
		out[0] = this.x[i] + (this.x[i + 1] - this.x[i]) * t;
		out[1] = this.y[i] + (this.y[i + 1] - this.y[i]) * t;
		float turn = this.slope[i];
		if (i > 0 && a - this.s[i] < 0.3F) {
			turn = mix(this.slope[i - 1], this.slope[i], 0.5F + 0.5F * (a - this.s[i]) / 0.3F);
		} else if (i < this.slope.length - 1 && this.s[i + 1] - a < 0.3F) {
			turn = mix(this.slope[i], this.slope[i + 1], 0.5F - 0.5F * (this.s[i + 1] - a) / 0.3F);
		}

		out[2] = turn;
	}

	private static float mix(float a, float b, float t) {
		return a + (b - a) * t;
	}
}
