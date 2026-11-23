package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import java.util.Arrays;

final class GraphLayout {
	final float[] x;
	final int[] y;
	final float minX;
	final float maxX;
	final int maxY;

	private GraphLayout(float[] x, int[] y, float minX, float maxX, int maxY) {
		this.x = x;
		this.y = y;
		this.minX = minX;
		this.maxX = maxX;
		this.maxY = maxY;
	}

	static GraphLayout of(int[] parent, int[] width, int[] height, int[] depth, float gap, int levelGap) {
		int n = parent.length;
		if (n == 0) {
			return new GraphLayout(new float[0], new int[0], 0.0F, 0.0F, 0);
		} else {
			int[] first = new int[n];
			int[] next = new int[n];
			int[] last = new int[n];
			Arrays.fill(first, -1);
			Arrays.fill(next, -1);
			Arrays.fill(last, -1);
			int deepest = 0;

			for (int i = 0; i < n; i++) {
				deepest = Math.max(deepest, depth[i]);
				int p = parent[i];
				if (p >= 0) {
					if (first[p] < 0) {
						first[p] = i;
					} else {
						next[last[p]] = i;
					}

					last[p] = i;
				}
			}

			float[] rel = new float[n];
			float[][] left = new float[n][];
			float[][] right = new float[n][];

			for (int v = n - 1; v >= 0; v--) {
				float half = (float)width[v] / 2.0F;
				if (first[v] < 0) {
					left[v] = new float[]{-half};
					right[v] = new float[]{(float)width[v] - half};
				} else {
					float[] accL = null;
					float[] accR = null;
					float lastOff = 0.0F;

					for (int c = first[v]; c >= 0; c = next[c]) {
						float off = 0.0F;
						if (accL == null) {
							accL = (float[])left[c].clone();
							accR = (float[])right[c].clone();
						} else {
							off = -Float.MAX_VALUE;
							int common = Math.min(accR.length, left[c].length);

							for (int l = 0; l < common; l++) {
								off = Math.max(off, accR[l] + gap - left[c][l]);
							}

							if (accR.length < left[c].length) {
								int was = accL.length;
								accL = Arrays.copyOf(accL, left[c].length);
								accR = Arrays.copyOf(accR, left[c].length);

								for (int l = was; l < accL.length; l++) {
									accL[l] = left[c][l] + off;
									accR[l] = right[c][l] + off;
								}
							}

							for (int l = 0; l < common; l++) {
								accL[l] = Math.min(accL[l], left[c][l] + off);
								accR[l] = Math.max(accR[l], right[c][l] + off);
							}
						}

						rel[c] = off;
						lastOff = off;
						left[c] = right[c] = null;
					}

					float centre = lastOff / 2.0F;

					for (int c = first[v]; c >= 0; c = next[c]) {
						rel[c] -= centre;
					}

					float[] l2 = new float[accL.length + 1];
					float[] r2 = new float[accR.length + 1];
					l2[0] = -half;
					r2[0] = (float)width[v] - half;

					for (int l = 0; l < accL.length; l++) {
						l2[l + 1] = accL[l] - centre;
						r2[l + 1] = accR[l] - centre;
					}

					left[v] = l2;
					right[v] = r2;
				}
			}

			float[] x = new float[n];

			for (int ix = 0; ix < n; ix++) {
				x[ix] = parent[ix] < 0 ? 0.0F : x[parent[ix]] + rel[ix];
			}

			int[] tallest = new int[deepest + 1];

			for (int ix = 0; ix < n; ix++) {
				tallest[depth[ix]] = Math.max(tallest[depth[ix]], height[ix]);
			}

			int[] lineTop = new int[deepest + 1];

			for (int d = 1; d <= deepest; d++) {
				lineTop[d] = lineTop[d - 1] + tallest[d - 1] + levelGap;
			}

			int[] y = new int[n];
			float minX = Float.MAX_VALUE;
			float maxX = -Float.MAX_VALUE;
			int maxY = 0;

			for (int ix = 0; ix < n; ix++) {
				y[ix] = lineTop[depth[ix]];
				minX = Math.min(minX, x[ix] - (float)width[ix] / 2.0F);
				maxX = Math.max(maxX, x[ix] + (float)width[ix] - (float)width[ix] / 2.0F);
				maxY = Math.max(maxY, y[ix] + height[ix]);
			}

			return new GraphLayout(x, y, minX, maxX, maxY);
		}
	}
}
