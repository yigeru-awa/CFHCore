package com.onehumanawa.cfhcore.content.gui.earlywindow.scene;

final class Atlas {
	static final int SIDE = 256;
	private final int[] px = new int[65536];
	private int ox;
	private int oy;
	private int seed;

	static int[] paint() {
		Atlas a = new Atlas();
		a.all();
		return a.px;
	}

	private void all() {
		this.at(Tile.WHITE);
		this.fill(-1);
		this.casing(Tile.CASING, -12895432, -6052964, -8092547, -10526886, -9744587);
		this.plate(Tile.CASING_TOP, -12895432, -6052964, -8092547, -10526886);
		this.casing(Tile.BRASS, -10863596, -734358, -2711491, -5934550, -10796500);
		this.plate(Tile.BRASS_TOP, -10863596, -734358, -2711491, -5934550);
		this.copper();
		this.plate(Tile.COPPER_TOP, -10867170, -1336200, -3640241, -6795206);
		this.glass();
		this.belt();
		this.beltSide();
		this.shaft();
		this.cog();
		this.cogFace();
		this.plinthTop();
		this.plinthSide();
		this.metal(Tile.IRON, -11249569, -1841430, -4604476, -8025453);
		this.metal(Tile.IRON_DARK, -14408150, -8617590, -10854810, -12828859);
		this.raw();
		this.bar();
		this.barTop();
		this.gauge();
		this.flap();
		this.stone(Tile.STONE, -1250074, -4934482, -9145233, -10329506, true);
		this.stone(Tile.STONE_B, -1250074, -4934482, -9145233, -10329506, true);
		this.stone(Tile.STONE_C, -1250074, -4934482, -9145233, -10329506, true);
		this.stone(Tile.STONE_SIDE, -7434615, -9408404, -11513779, -12698052, false);
		this.pipe();
		this.crate();
		this.tunnel();
		this.bed();
		this.beam();
		this.lamp();
		this.glassBack();
		this.ore();
		this.plates();
	}

	private void casing(Tile tile, int outline, int light, int mid, int dark, int wood) {
		this.at(tile);

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int e = edge(x, y);
				int c;
				if (e == 0) {
					c = outline;
				} else if (e == 1) {
					boolean lit = x == 1 || y == 1;
					boolean shaded = x == 14 || y == 14;
					c = lit && shaded ? mid : (lit ? light : dark);
				} else if (e == 2) {
					c = shade(mid, 0.96F + this.rnd(x, y) * 0.08F);
				} else if (e == 3) {
					c = x != 3 && y != 3 ? shade(wood, 0.78F) : shade(wood, 0.52F);
				} else {
					int board = (y - 4) / 4;
					float tone = 0.94F + hash(board, 3, this.seed) * 0.12F;
					float grain = 0.95F + hash(x / 2, y, this.seed) * 0.1F;
					c = shade(wood, tone * grain);
					if ((y - 4) % 4 == 0 && y > 4) {
						c = shade(wood, 0.66F);
					}

					if ((y - 4) % 4 == 1) {
						c = shade(c, 1.08F);
					}
				}

				this.set(x, y, c);
			}
		}

		for (int[] at : new int[][]{{2, 2}, {13, 2}, {2, 13}, {13, 13}}) {
			this.set(at[0], at[1], light);
			this.set(at[0] + (at[0] < 8 ? 1 : -1), at[1] + (at[1] < 8 ? 1 : -1), dark);
		}
	}

	private void plate(Tile tile, int outline, int light, int mid, int dark) {
		this.at(tile);

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int e = edge(x, y);
				int c;
				if (e == 0) {
					c = outline;
				} else if (e == 1) {
					boolean lit = x == 1 || y == 1;
					boolean shaded = x == 14 || y == 14;
					c = lit && shaded ? mid : (lit ? light : dark);
				} else if (e == 3) {
					c = x != 3 && y != 3 ? light : dark;
				} else {
					c = shade(mid, 0.95F + this.rnd(x, y) * 0.1F);
					if (e >= 4 && (x == 7 || x == 8 || y == 7 || y == 8)) {
						c = shade(mid, 0.84F);
					}

					if (e >= 6) {
						c = x + y <= 15 ? light : dark;
					}
				}

				this.set(x, y, c);
			}
		}
	}

	private void copper() {
		this.at(Tile.COPPER);
		int outline = -10867170;
		int light = -1336200;
		int mid = -3640241;
		int dark = -6795206;

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int e = edge(x, y);
				int c;
				if (e == 0) {
					c = -10867170;
				} else if (e == 1) {
					boolean lit = x == 1 || y == 1;
					boolean shaded = x == 14 || y == 14;
					c = lit && shaded ? -3640241 : (lit ? -1336200 : -6795206);
				} else {
					c = shade(-3640241, 0.93F + hash(0, y, this.seed) * 0.14F);
					if (this.rnd(x, y) > 0.9F) {
						c = shade(c, 1.12F);
					}

					if (y == 7) {
						c = -6795206;
					}

					if (y == 8) {
						c = -1336200;
					}
				}

				this.set(x, y, c);
			}
		}

		for (int[] at : new int[][]{{3, 3}, {12, 3}, {3, 12}, {12, 12}}) {
			this.set(at[0], at[1], -1336200);
			this.set(at[0] + 1, at[1] + 1, -10867170);
		}
	}

	private void glass() {
		this.at(Tile.GLASS);

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int c = 315945463;
				int e = edge(x, y);
				if (e == 0) {
					c = -2132477700;
				}

				if (e == 1 && (x == 1 || y == 1)) {
					c = 822083583;
				}

				int d = x + y;
				if ((d == 10 || d == 11) && x >= 3 && x <= 8) {
					c = 1560281087;
				}

				if (d == 13 && x >= 6 && x <= 8) {
					c = 889192447;
				}

				this.set(x, y, c);
			}
		}
	}

	private void glassBack() {
		this.at(Tile.GLASS_BACK);

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				this.set(x, y, edge(x, y) == 0 ? 1355078882 : 215282167);
			}
		}
	}

	private void ore() {
		this.at(Tile.ORE);

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				float blotch = hash(x / 3, y / 3, this.seed) * 0.55F + hash((x + 1) / 2, (y + 2) / 2, this.seed + 5) * 0.45F;
				int c = 0;
				if (blotch > 0.5F && edge(x, y) > 0) {
					float tone = hash(x, y, this.seed + 9);
					c = tone > 0.75F ? -1 : (tone > 0.3F ? -2960686 : -6645094);
				}

				this.set(x, y, c);
			}
		}
	}

	private void belt() {
		this.at(Tile.BELT);
		int base = -13092805;

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int c;
				if (y == 0 || y == 15) {
					c = -14474459;
				} else if (y != 1 && y != 14) {
					int across = y <= 7 ? 7 - y : y - 8;
					int d = Math.floorMod(x + across, 8);
					c = d == 0 ? -10197909 : (d == 7 ? -14013907 : shade(-13092805, 0.95F + this.rnd(x, y) * 0.1F));
					if (y == 2 || y == 13) {
						c = shade(c, 0.85F);
					}
				} else {
					c = -11184805;
				}

				this.set(x, y, c);
			}
		}
	}

	private void beltSide() {
		this.at(Tile.BELT_SIDE);

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int c;
				if (y == 0) {
					c = -10790047;
				} else if (y < 4) {
					c = x % 4 == 1 && y == 2 ? -9539979 : -13816528;
				} else if (y == 4) {
					c = -15000803;
				} else {
					c = -14474459;
				}

				this.set(x, y, c);
			}
		}
	}

	private void shaft() {
		this.at(Tile.SHAFT);
		int[] round = new int[]{
			-5658206,
			-5658206,
			-7039859,
			-7039859,
			-8026753,
			-8026753,
			-9145233,
			-11645366,
			-9145233,
			-8026753,
			-8026753,
			-7039859,
			-7039859,
			-6381929,
			-6381929,
			-10526886
		};

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int c = shade(round[y], 0.97F + this.rnd(x, y) * 0.06F);
				if (x == 0 || x == 8) {
					c = shade(c, 0.78F);
				}

				if (x == 1 || x == 9) {
					c = shade(c, 1.08F);
				}

				this.set(x, y, c);
			}
		}
	}

	private void cog() {
		this.at(Tile.COG);
		int wood = -8561605;

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int c = shade(-8561605, (0.92F + hash(x / 3, y / 8, this.seed) * 0.1F) * (0.96F + this.rnd(x, y) * 0.08F));
				if (y % 4 == 3) {
					c = shade(-8561605, 0.72F);
				}

				int e = edge(x, y);
				if (e == 0) {
					c = shade(-8561605, x != 0 && y != 0 ? 0.6F : 1.18F);
				}

				this.set(x, y, c);
			}
		}
	}

	private void cogFace() {
		this.at(Tile.COG_FACE);
		int wood = -8561605;
		int rim = -10928086;
		int hub = -7566203;

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				float dx = (float)x - 7.5F;
				float dy = (float)y - 7.5F;
				float r = (float)Math.sqrt((double)(dx * dx + dy * dy));
				double angle = Math.atan2((double)dy, (double)dx);
				int c;
				if (r < 1.2F) {
					c = -12961225;
				} else if (r < 2.6F) {
					c = dx + dy < 0.0F ? shade(-7566203, 1.14F) : shade(-7566203, 0.86F);
				} else if (r < 3.6F) {
					c = -12833763;
				} else if (r < 6.4F) {
					double slice = Math.abs((angle / (Math.PI / 4) % 1.0 + 1.0) % 1.0 - 0.5);
					c = slice > 0.4 ? shade(-8561605, 0.7F) : shade(-8561605, 0.94F + this.rnd(x, y) * 0.12F);
				} else {
					c = shade(-10928086, 0.94F + this.rnd(x, y) * 0.12F);
				}

				this.set(x, y, c);
			}
		}
	}

	private void plinthTop() {
		this.at(Tile.PLINTH_TOP);
		int base = -13619148;

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int c = shade(-13619148, 0.94F + this.rnd(x, y) * 0.12F);
				if (x == 0 || y == 0) {
					c = -12237494;
				}

				if (x == 15 || y == 15) {
					c = -14935009;
				}

				if (x == 0 && y == 15 || x == 15 && y == 0) {
					c = -13619148;
				}

				int d = x - y;
				if (edge(x, y) > 1 && (d == 3 || d == 4)) {
					c = shade(c, 1.12F);
				}

				this.set(x, y, c);
			}
		}
	}

	private void plinthSide() {
		this.at(Tile.PLINTH_SIDE);

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int c;
				if (y == 0) {
					c = -734358;
				} else if (y == 1) {
					c = -4815826;
				} else if (y == 2) {
					c = -15592941;
				} else {
					int course = (y - 3) / 3;
					boolean joint = (y - 3) % 3 == 2 || Math.floorMod(x + course * 8, 16) == 0;
					c = joint ? -15198182 : shade(-13948113, 0.92F + this.rnd(x, y) * 0.14F);
					if (!joint && (y - 3) % 3 == 0) {
						c = shade(c, 1.2F);
					}
				}

				this.set(x, y, c);
			}
		}
	}

	private void metal(Tile tile, int outline, int light, int mid, int dark) {
		this.at(tile);

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int e = edge(x, y);
				int c;
				if (e == 0) {
					c = outline;
				} else if (e == 1) {
					boolean lit = x == 1 || y == 1;
					boolean shaded = x == 14 || y == 14;
					c = lit && shaded ? mid : (lit ? light : dark);
				} else {
					c = shade(mid, 0.95F + hash(x, 0, this.seed) * 0.05F + this.rnd(x, y) * 0.05F);
					int d = x + y;
					if (d == 13 || d == 14) {
						c = shade(c, 1.1F);
					}
				}

				this.set(x, y, c);
			}
		}

		for (int[] at : new int[][]{{3, 3}, {12, 3}, {3, 12}, {12, 12}}) {
			this.set(at[0], at[1], light);
			this.set(at[0] + 1, at[1] + 1, outline);
		}
	}

	private void raw() {
		this.at(Tile.RAW);
		int stone = -9344157;

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				float blotch = hash(x / 4, y / 4, this.seed) * 0.5F + hash((x + 2) / 3, (y + 1) / 3, this.seed + 3) * 0.5F;
				int c = shade(-9344157, 0.82F + blotch * 0.3F + this.rnd(x, y) * 0.06F);
				int e = edge(x, y);
				if (e == 0) {
					c = shade(-9344157, x != 0 && y != 0 ? 0.58F : 1.22F);
				}

				if (hash(x / 2, y / 2, this.seed + 11) > 0.92F) {
					c = shade(-9344157, 0.6F);
				}

				this.set(x, y, c);
			}
		}
	}

	private void bar() {
		this.at(Tile.BAR);

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int v = 200 - y * 4;
				if (y == 0) {
					v = 255;
				}

				if (x == 0) {
					v = Math.min(255, v + 24);
				}

				if (x == 15 || y == 15) {
					v = 112;
				}

				v = Math.round((float)v * (0.97F + this.rnd(x, y) * 0.06F));
				this.set(x, y, grey(Math.min(255, v)));
			}
		}
	}

	private void barTop() {
		this.at(Tile.BAR_TOP);

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int v = 226;
				int e = edge(x, y);
				if (e == 0) {
					v = x != 0 && y != 0 ? 154 : 255;
				}

				int d = x + y;
				if (e > 0 && (d == 11 || d == 12)) {
					v = 255;
				}

				if (e > 0 && d == 20) {
					v = 244;
				}

				v = Math.round((float)v * (0.97F + this.rnd(x, y) * 0.06F));
				this.set(x, y, grey(Math.min(255, v)));
			}
		}
	}

	private void gauge() {
		this.at(Tile.GAUGE);

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				float dx = (float)x - 7.5F;
				float dy = (float)y - 7.5F;
				float r = (float)Math.sqrt((double)(dx * dx + dy * dy));
				int c;
				if (r > 7.9F) {
					c = 0;
				} else if (r > 6.4F) {
					c = dx + dy < 0.0F ? -734358 : -5934550;
				} else if (r > 5.6F) {
					c = -11912670;
				} else {
					c = shade(-989230, 0.96F + this.rnd(x, y) * 0.05F);
					if (dx + dy > 5.0F) {
						c = shade(c, 0.9F);
					}
				}

				this.set(x, y, c);
			}
		}

		for (int i = 0; i <= 4; i++) {
			double a = Math.toRadians((double)(210 - i * 60));
			int x = (int)Math.round(7.5 + Math.cos(a) * 4.4);
			int y = (int)Math.round(7.5 - Math.sin(a) * 4.4);
			this.set(x, y, -12964322);
		}
	}

	private void flap() {
		this.at(Tile.FLAP);

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int strip = x % 4;
				int c = strip == 0 ? -11908529 : (strip == 3 ? -15066596 : shade(-13750734, 0.94F + this.rnd(x, y) * 0.12F));
				if (y == 15) {
					c = strip != 0 && strip != 3 ? -15066596 : 0;
				}

				if (y == 0) {
					c = -10526886;
				}

				this.set(x, y, c);
			}
		}
	}

	private void stone(Tile tile, int light, int mid, int dark, int crack, boolean bevel) {
		this.at(tile);

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				float blotch = hash(x / 4, y / 4, this.seed) * 0.5F + hash((x + 2) / 3, (y + 1) / 3, this.seed + 3) * 0.5F;
				int c = shade(mid, 0.86F + blotch * 0.28F + this.rnd(x, y) * 0.05F);
				int e = edge(x, y);
				if (bevel) {
					if (e == 0) {
						c = x != 0 && y != 0 ? dark : light;
					}

					if (e == 1) {
						c = x != 1 && y != 1 ? shade(dark, 1.22F) : shade(light, 0.86F);
					}

					if (x == 0 && y == 15 || x == 15 && y == 0) {
						c = mid;
					}

					if (x == 1 && y == 14 || x == 14 && y == 1) {
						c = mid;
					}
				} else if (e == 0) {
					c = shade(c, x != 0 && y != 0 ? 0.78F : 1.12F);
				}

				this.set(x, y, c);
			}
		}
	}

	private void plates() {
		this.at(Tile.PLATES);

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int v = y % 2 == 0 ? 255 : 162;
				v = Math.round((float)v * (0.93F + hash(x / 3, y / 2, this.seed) * 0.14F));
				if (x == 0) {
					v = Math.min(255, v + 18);
				}

				if (x == 15) {
					v = Math.round((float)v * 0.72F);
				}

				this.set(x, y, grey(Math.min(255, v)));
			}
		}

		this.at(Tile.PLATE_TOP);

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int vx = 220;
				int e = edge(x, y);
				if (e == 0) {
					vx = x != 0 && y != 0 ? 150 : 255;
				}

				int d = x + y;
				if (e > 0 && (d == 12 || d == 13)) {
					vx = 255;
				}

				if (e > 0 && d == 21) {
					vx = 240;
				}

				vx = Math.round((float)vx * (0.97F + this.rnd(x, y) * 0.06F));
				this.set(x, y, grey(Math.min(255, vx)));
			}
		}
	}

	private void pipe() {
		this.at(Tile.PIPE);
		int[] round = new int[]{-7714253, -3640241, -1006206, -2061210, -3640241, -4691642, -5743554, -7714253};

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int c = shade(round[y / 2], 0.96F + this.rnd(x, y) * 0.08F);
				if (x % 8 == 0) {
					c = shade(c, 0.7F);
				}

				if (x % 8 == 1) {
					c = shade(c, 1.12F);
				}

				this.set(x, y, c);
			}
		}
	}

	private void crate() {
		this.at(Tile.CRATE);
		int frame = -7574974;
		int board = -5667494;

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int e = edge(x, y);
				int c;
				if (e == 0) {
					c = shade(-7574974, x != 0 && y != 0 ? 0.62F : 1.16F);
				} else if (e == 1) {
					c = shade(-7574974, 0.95F + this.rnd(x, y) * 0.1F);
				} else if (e == 2) {
					c = shade(-5667494, x != 2 && y != 2 ? 0.85F : 0.6F);
				} else {
					c = shade(-5667494, (0.94F + hash(x / 3, 0, this.seed) * 0.1F) * (0.96F + this.rnd(x, y) * 0.08F));
					if (x % 3 == 2) {
						c = shade(-5667494, 0.74F);
					}

					if (Math.abs(x - y) <= 1) {
						c = shade(-7574974, x - y == 1 ? 0.7F : 1.0F);
					}
				}

				this.set(x, y, c);
			}
		}
	}

	private void tunnel() {
		this.at(Tile.TUNNEL);
		int outline = -12895432;
		int light = -6052964;
		int mid = -8092547;
		int dark = -10526886;

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int e = edge(x, y);
				int c;
				if (e == 0) {
					c = -12895432;
				} else if (e == 1) {
					boolean lit = x == 1 || y == 1;
					boolean shaded = x == 14 || y == 14;
					c = lit && shaded ? -8092547 : (lit ? -6052964 : -10526886);
				} else if (e == 2) {
					c = shade(-8092547, 0.96F + this.rnd(x, y) * 0.08F);
				} else if (e == 3) {
					c = x != 3 && y != 3 ? -14010820 : -15855084;
				} else {
					c = -15064280;
					int d = x + y;
					if (d == 12 || d == 13) {
						c = -12956587;
					}

					if (d == 18) {
						c = -14010048;
					}
				}

				this.set(x, y, c);
			}
		}
	}

	private void bed() {
		this.at(Tile.BED);

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int c = shade(-12763839, 0.94F + this.rnd(x, y) * 0.1F);
				if (y == 0) {
					c = -10855840;
				}

				if (y == 15) {
					c = -14803424;
				}

				if (x == 0) {
					c = -11184806;
				}

				if (x == 15) {
					c = -14408666;
				}

				if (x >= 4 && x <= 11 && (y == 3 || y == 5 || y == 7)) {
					c = -15000803;
				}

				if (x >= 4 && x <= 11 && (y == 4 || y == 6 || y == 8)) {
					c = -11776942;
				}

				if ((x == 2 || x == 13) && (y == 2 || y == 12)) {
					c = -7697776;
				}

				this.set(x, y, c);
			}
		}
	}

	private void beam() {
		this.at(Tile.BEAM);
		int mid = -8487304;

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int c = shade(-8487304, 0.94F + this.rnd(x, y) * 0.1F);
				if (x == 0) {
					c = -5855585;
				}

				if (x == 3) {
					c = -11316401;
				}

				if (x > 3) {
					c = shade(-8487304, (x % 4 == 0 ? 1.22F : (x % 4 == 3 ? 0.68F : 1.0F)) * (0.95F + this.rnd(x, y) * 0.1F));
				}

				if (y % 8 == 7) {
					c = shade(c, 0.7F);
				}

				if (y % 8 == 0) {
					c = shade(c, 1.15F);
				}

				this.set(x, y, c);
			}
		}
	}

	private void lamp() {
		this.at(Tile.LAMP);

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int e = edge(x, y);
				int c;
				if (e == 0) {
					c = -10863596;
				} else if (e == 1) {
					c = x != 1 && y != 1 ? -5934550 : -734358;
				} else {
					float dx = (float)x - 7.5F;
					float dy = (float)y - 7.5F;
					float r = (float)Math.sqrt((double)(dx * dx + dy * dy)) / 8.0F;
					c = mix(-3132, -19641, Math.min(1.0F, r * 1.2F));
					if (x % 4 == 0 || y % 4 == 0) {
						c = mix(c, -5934550, 0.55F);
					}
				}

				this.set(x, y, c);
			}
		}
	}

	private void at(Tile tile) {
		this.ox = tile.column * 16;
		this.oy = tile.row * 16;
		this.seed = tile.ordinal() * 7919 + 17;
	}

	private void fill(int argb) {
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				this.set(x, y, argb);
			}
		}
	}

	private void set(int x, int y, int argb) {
		if (x >= 0 && y >= 0 && x <= 15 && y <= 15) {
			this.px[(this.oy + y) * 256 + this.ox + x] = argb;
		}
	}

	private static int edge(int x, int y) {
		return Math.min(Math.min(x, 15 - x), Math.min(y, 15 - y));
	}

	private float rnd(int x, int y) {
		return hash(x, y, this.seed);
	}

	static float hash(int x, int y, int seed) {
		int h = x * 374761393 + y * 668265263 + seed * 1274126177;
		h = (h ^ h >>> 13) * 1274126177;
		h ^= h >>> 16;
		return (float)(h & 16777215) / 1.6777216E7F;
	}

	private static int grey(int v) {
		int c = Math.max(0, Math.min(255, v));
		return 0xFF000000 | c << 16 | c << 8 | c;
	}

	static int shade(int argb, float f) {
		int r = Math.min(255, Math.round((float)(argb >>> 16 & 0xFF) * f));
		int g = Math.min(255, Math.round((float)(argb >>> 8 & 0xFF) * f));
		int b = Math.min(255, Math.round((float)(argb & 0xFF) * f));
		return argb & 0xFF000000 | r << 16 | g << 8 | b;
	}

	static int fuller(int argb, float by) {
		int r = argb >>> 16 & 0xFF;
		int g = argb >>> 8 & 0xFF;
		int b = argb & 0xFF;
		float grey = 0.299F * (float)r + 0.587F * (float)g + 0.114F * (float)b;
		int nr = Math.max(0, Math.min(255, Math.round(grey + ((float)r - grey) * by)));
		int ng = Math.max(0, Math.min(255, Math.round(grey + ((float)g - grey) * by)));
		int nb = Math.max(0, Math.min(255, Math.round(grey + ((float)b - grey) * by)));
		return argb & 0xFF000000 | nr << 16 | ng << 8 | nb;
	}

	static int mix(int a, int b, float t) {
		float u = Math.max(0.0F, Math.min(1.0F, t));
		int al = Math.round((float)(a >>> 24) + (float)((b >>> 24) - (a >>> 24)) * u);
		int r = Math.round((float)(a >>> 16 & 0xFF) + (float)((b >>> 16 & 0xFF) - (a >>> 16 & 0xFF)) * u);
		int g = Math.round((float)(a >>> 8 & 0xFF) + (float)((b >>> 8 & 0xFF) - (a >>> 8 & 0xFF)) * u);
		int bl = Math.round((float)(a & 0xFF) + (float)((b & 0xFF) - (a & 0xFF)) * u);
		return al << 24 | r << 16 | g << 8 | bl;
	}
}
