package com.onehumanawa.cfhcore.content.gui.earlywindow.scene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class Island {
	static final int GRASS = -7226023;
	static final int FOLIAGE = -8934609;
	static final int WATER = -12618012;
	static final int HALF = 7;
	static final int LOW = -9;
	static final int HIGH = 9;
	private static final int SIZE = 15;
	private static final int LEVELS = 19;
	static final float RADIUS = 5.35F;
	private static final int TREE_X = 1;
	private static final int TREE_Z = 2;
	private static final int TRUNK = 5;
	private static final int[][] NORMALS = new int[][]{{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
	private static final float[] SHADE = new float[]{0.5F, 1.0F, 0.8F, 0.8F, 0.6F, 0.6F};
	private static final int[][][] CORNERS = new int[][][]{
		{{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}},
		{{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}},
		{{1, 1, 0}, {1, 0, 0}, {0, 0, 0}, {0, 1, 0}},
		{{0, 1, 1}, {0, 0, 1}, {1, 0, 1}, {1, 1, 1}},
		{{0, 1, 0}, {0, 0, 0}, {0, 0, 1}, {0, 1, 1}},
		{{1, 1, 1}, {1, 0, 1}, {1, 0, 0}, {1, 1, 0}}
	};
	private static final float OPEN = 1.0F;
	private static final float SHUT = 0.2F;
	final List<Island.Block> blocks = new ArrayList<>();
	private final Island.Block[] grid = new Island.Block[4275];
	private final Map<Island.Kind, Sheet.Region[]> faces = new EnumMap<>(Island.Kind.class);
	private final Map<Island.Kind, Sheet.Region> crumbs = new EnumMap<>(Island.Kind.class);
	private Sheet.Region[] water;
	private final float[] ao = new float[4];
	float top;
	float bottom;
	int light = -1;

	Island(Assets assets, Sheet sheet) {
		this.textures(assets, sheet);
		this.plan();
	}

	private void textures(Assets assets, Sheet sheet) {
		if (assets == null) {
			throw new IllegalStateException("no assets of the game");
		} else {
			Kit kit = new Kit(assets, sheet);

			for (String needed : new String[]{"grass_block_top", "grass_block_side", "grass_block_side_overlay", "dirt", "stone", "oak_leaves", "water_still"}) {
				if (!kit.has("minecraft:block/" + needed)) {
					throw new IllegalStateException("no texture block/" + needed);
				}
			}

			for (Island.Kind k : Island.Kind.values()) {
				Sheet.Region side = k == Island.Kind.GRASS_BLOCK ? grassSide(assets, sheet) : kit.texture("minecraft:block/" + k.side);
				this.faces.put(k, new Sheet.Region[]{kit.texture("minecraft:block/" + k.bottom), kit.texture("minecraft:block/" + k.top), side});
				this.crumbs.put(k, k == Island.Kind.GRASS_BLOCK ? kit.texture("minecraft:block/dirt") : kit.texture("minecraft:block/" + k.side));
			}

			Picture still = Picture.read(assets.read("minecraft", "textures/block/water_still.png"));
			if (still != null && still.h() >= still.w()) {
				Sheet.Region strip = sheet.add(still.argb(), still.w(), still.h());
				if (strip == null) {
					throw new IllegalStateException("no room for the water");
				} else {
					int frames = still.h() / still.w();
					this.water = new Sheet.Region[frames];

					for (int i = 0; i < frames; i++) {
						this.water[i] = new Sheet.Region(strip.x(), strip.y() + i * still.w(), still.w(), still.w(), true);
					}
				}
			} else {
				throw new IllegalStateException("no water");
			}
		}
	}

	private static Sheet.Region grassSide(Assets assets, Sheet sheet) {
		Picture side = Picture.read(assets.read("minecraft", "textures/block/grass_block_side.png"));
		Picture over = Picture.read(assets.read("minecraft", "textures/block/grass_block_side_overlay.png"));
		if (side != null && over != null && side.w() == over.w() && side.h() == over.h()) {
			int[] px = (int[])side.argb().clone();

			for (int i = 0; i < px.length; i++) {
				int o = over.argb()[i];
				int a = o >>> 24;
				if (a != 0) {
					int tinted = tint(o, -7226023);
					px[i] = Atlas.mix(px[i], tinted | 0xFF000000, (float)a / 255.0F);
				}
			}

			Sheet.Region region = sheet.add(px, side.w(), side.w());
			if (region == null) {
				throw new IllegalStateException("no room for the grass");
			} else {
				return region;
			}
		} else {
			throw new IllegalStateException("no grass side");
		}
	}

	static int tint(int argb, int tint) {
		int r = (argb >>> 16 & 0xFF) * (tint >>> 16 & 0xFF) / 255;
		int g = (argb >>> 8 & 0xFF) * (tint >>> 8 & 0xFF) / 255;
		int b = (argb & 0xFF) * (tint & 0xFF) / 255;
		return argb & 0xFF000000 | r << 16 | g << 8 | b;
	}

	private static float reach(int x, int z) {
		return (float)Math.sqrt((double)(x * x + z * z)) + (Atlas.hash(x, z, 101) - 0.5F) * 1.1F;
	}

	private static boolean pond(int x, int z) {
		return (x == -2 || x == -3) && (z == -1 || z == 0) || x == -2 && z == 1;
	}

	private static boolean hill(int x, int z) {
		float dx = (float)x - 2.3F;
		float dz = (float)z + 2.2F;
		return dx * dx + dz * dz <= 3.2F;
	}

	private void plan() {
		List<Island.Block> plan = new ArrayList<>();

		for (int x = -7; x <= 7; x++) {
			for (int z = -7; z <= 7; z++) {
				float r = reach(x, z);
				if (!(r > 5.35F)) {
					int ground = hill(x, z) && r < 4.1499996F ? 1 : 0;
					int depth = Math.max(1, Math.min(8, (int)(1.2F + (5.35F - r) * 1.1F + Atlas.hash(x, z, 102) * 1.5F)));

					for (int y = -depth; y <= ground; y++) {
						plan.add(new Island.Block(x, y, z, ground(x, y, z, ground, depth)));
					}
				}
			}
		}

		for (int y = 1; y <= 5; y++) {
			plan.add(new Island.Block(1, y, 2, Island.Kind.OAK_LOG));
		}

		for (int y = 3; y <= 6; y++) {
			int r = y >= 5 ? 1 : 2;

			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (dx != 0 || dz != 0 || y > 5) {
						boolean corner = Math.abs(dx) == r && Math.abs(dz) == r;
						if (!corner || y != 6 && !(Atlas.hash(dx + 9, dz + 9, 200 + y) < 0.5F)) {
							plan.add(new Island.Block(1 + dx, y, 2 + dz, Island.Kind.OAK_LEAVES));
						}
					}
				}
			}
		}

		Island.Kind[] flowers = new Island.Kind[]{Island.Kind.POPPY, Island.Kind.DANDELION, Island.Kind.CORNFLOWER, Island.Kind.OXEYE_DAISY, Island.Kind.AZURE_BLUET};
		List<Island.Block> plants = new ArrayList<>();

		for (Island.Block b : plan) {
			if (b.kind == Island.Kind.GRASS_BLOCK && (Math.abs(b.x - 1) > 1 || Math.abs(b.z - 2) > 1)) {
				float h = Atlas.hash(b.x, b.z, 103);
				if (h < 0.1F) {
					plants.add(new Island.Block(b.x, b.y + 1, b.z, flowers[(int)(Atlas.hash(b.x, b.z, 104) * (float)flowers.length)]));
				} else if (h < 0.34F) {
					plants.add(new Island.Block(b.x, b.y + 1, b.z, Island.Kind.SHORT_GRASS));
				}
			}
		}

		plan.sort(
			Comparator.<Island.Block>comparingInt(bx -> bx.y)
				.thenComparingDouble(bx -> Math.sqrt((double)(bx.x * bx.x + bx.z * bx.z)) + (double)(Atlas.hash(bx.x, bx.z, bx.y + 300) * 1.4F))
		);
		plants.sort(Comparator.comparingDouble(bx -> (double)Atlas.hash(bx.x, bx.z, 301)));
		this.blocks.addAll(plan);
		this.blocks.addAll(plants);
		this.top = -Float.MAX_VALUE;
		this.bottom = Float.MAX_VALUE;

		for (Island.Block bx : this.blocks) {
			this.top = Math.max(this.top, (float)(bx.y + 1));
			this.bottom = Math.min(this.bottom, (float)bx.y);
		}
	}

	private static Island.Kind ground(int x, int y, int z, int ground, int depth) {
		if (pond(x, z)) {
			if (y == ground) {
				return Island.Kind.WATER;
			}

			if (y == ground - 1) {
				return Island.Kind.SAND;
			}
		}

		if (y == ground) {
			return Island.Kind.GRASS_BLOCK;
		} else if (y >= ground - 2 - (Atlas.hash(x, z, 105) < 0.4F ? 1 : 0)) {
			return Island.Kind.DIRT;
		} else {
			float ore = Atlas.hash(x, y * 7 + 3, z + 50);
			boolean deep = y <= -6 || y == -depth && depth >= 5;
			if (deep) {
				if (ore < 0.05F) {
					return Island.Kind.DEEPSLATE_DIAMOND_ORE;
				} else if (ore < 0.1F) {
					return Island.Kind.DEEPSLATE_REDSTONE_ORE;
				} else if (ore < 0.14F) {
					return Island.Kind.DEEPSLATE_LAPIS_ORE;
				} else {
					return ore < 0.19F ? Island.Kind.DEEPSLATE_IRON_ORE : Island.Kind.DEEPSLATE;
				}
			} else if (ore < 0.08F) {
				return Island.Kind.COAL_ORE;
			} else if (ore < 0.13F) {
				return Island.Kind.IRON_ORE;
			} else if (ore < 0.17F) {
				return Island.Kind.COPPER_ORE;
			} else if (ore < 0.19F && y <= -3) {
				return Island.Kind.GOLD_ORE;
			} else if (ore < 0.21F && y <= -4) {
				return Island.Kind.REDSTONE_ORE;
			} else if (ore < 0.23F && y <= -4) {
				return Island.Kind.LAPIS_ORE;
			} else {
				float rock = Atlas.hash(x / 2, y / 2, z / 2 + 70);
				if (rock < 0.12F) {
					return Island.Kind.ANDESITE;
				} else {
					return rock > 0.95F ? Island.Kind.GRAVEL : Island.Kind.STONE;
				}
			}
		}
	}

	void stand(Island.Block b) {
		b.standing = true;
		this.grid[index(b.x, b.y, b.z)] = b;
	}

	void clear() {
		Arrays.fill(this.grid, null);

		for (Island.Block b : this.blocks) {
			b.standing = false;
			b.sent = -1.0F;
		}
	}

	private static int index(int x, int y, int z) {
		return ((y - -9) * 15 + z + 7) * 15 + x + 7;
	}

	private Island.Block at(int x, int y, int z) {
		return x >= -7 && x <= 7 && z >= -7 && z <= 7 && y >= -9 && y <= 9 ? this.grid[index(x, y, z)] : null;
	}

	private boolean solid(int x, int y, int z) {
		Island.Block b = this.at(x, y, z);
		return b != null && b.kind.solid();
	}

	private boolean shades(int x, int y, int z) {
		Island.Block b = this.at(x, y, z);
		return b != null && b.kind.shades();
	}

	void standing(Mesh solid) {
		solid.reset();
		solid.kind(4);

		for (Island.Block b : this.blocks) {
			if (b.standing) {
				switch (b.kind.shape) {
					case CUBE:
					case LEAVES:
						int f = 0;

						for (; f < 6; f++) {
							int[] n = NORMALS[f];
							Island.Block next = this.at(b.x + n[0], b.y + n[1], b.z + n[2]);
							if (next == null || !next.kind.solid()) {
								this.shade(b, f);
								this.face(solid, b.kind, f, b.x, b.y, b.z, 1.0F, this.ao);
							}
						}
						break;
					case CROSS:
						this.cross(solid, b.kind, b.x, b.y, b.z, 1.0F);
				}
			}
		}
	}

	int lit(int argb) {
		if (this.light == -1) {
			return argb;
		} else {
			int r = (argb >>> 16 & 0xFF) * (this.light >>> 16 & 0xFF) / 255;
			int g = (argb >>> 8 & 0xFF) * (this.light >>> 8 & 0xFF) / 255;
			int b = (argb & 0xFF) * (this.light & 0xFF) / 255;
			return argb & 0xFF000000 | r << 16 | g << 8 | b;
		}
	}

	void water(Mesh clear, int frame) {
		clear.reset();
		clear.kind(4);

		for (Island.Block b : this.blocks) {
			if (b.standing && b.kind.shape == Island.Shape.WATER) {
				this.liquid(clear, b, frame);
			}
		}
	}

	void falling(Mesh solid, Mesh clear, Island.Block b, float drop, float size, int frame) {
		Mesh into = b.kind.shape == Island.Shape.WATER ? clear : solid;
		into.reset();
		into.kind(4);
		into.push();
		into.translate((float)b.x + 0.5F, (float)b.y + drop + 0.5F, (float)b.z + 0.5F);
		into.scale(size);
		into.translate((float)(-b.x) - 0.5F, (float)(-b.y) - 0.5F, (float)(-b.z) - 0.5F);
		switch (b.kind.shape) {
			case CROSS:
				this.cross(into, b.kind, b.x, b.y, b.z, 1.0F);
				break;
			case WATER:
				Arrays.fill(this.ao, 1.0F);
				into.colour(this.lit(-12618012));
				quad(into, this.water[frame % this.water.length], 1, b.x, b.y, b.z, 0.875F, SHADE[1], this.ao);
				break;
			default:
				Arrays.fill(this.ao, 1.0F);

				for (int f = 0; f < 6; f++) {
					this.face(into, b.kind, f, b.x, b.y, b.z, 1.0F, this.ao);
				}
		}

		into.pop();
	}

	Sheet.Region crumb(Island.Kind kind, int u, int v) {
		Sheet.Region r = this.crumbs.get(kind);
		int bit = Math.max(1, r.w() / 4);
		return new Sheet.Region(r.x() + Math.min(r.w() - bit, u), r.y() + Math.min(r.h() - bit, v), bit, bit, false);
	}

	static int crumbTint(Island.Kind kind) {
		return kind == Island.Kind.OAK_LEAVES ? -8934609 : (kind == Island.Kind.SHORT_GRASS ? -7226023 : (kind == Island.Kind.WATER ? -12618012 : -1));
	}

	private void shade(Island.Block b, int f) {
		int[] n = NORMALS[f];
		int ox = b.x + n[0];
		int oy = b.y + n[1];
		int oz = b.z + n[2];

		for (int c = 0; c < 4; c++) {
			int[] corner = CORNERS[f][c];
			int ax = 0;
			int ay = 0;
			int az = 0;
			int bx = 0;
			int by = 0;
			int bz = 0;
			if (n[1] != 0) {
				ax = corner[0] == 1 ? 1 : -1;
				bz = corner[2] == 1 ? 1 : -1;
			} else if (n[2] != 0) {
				ax = corner[0] == 1 ? 1 : -1;
				by = corner[1] == 1 ? 1 : -1;
			} else {
				az = corner[2] == 1 ? 1 : -1;
				by = corner[1] == 1 ? 1 : -1;
			}

			boolean s1 = this.shades(ox + ax, oy + ay, oz + az);
			boolean s2 = this.shades(ox + bx, oy + by, oz + bz);
			boolean corn = s1 && s2 || this.shades(ox + ax + bx, oy + ay + by, oz + az + bz);
			this.ao[c] = (1.0F + (s1 ? 0.2F : 1.0F) + (s2 ? 0.2F : 1.0F) + (corn ? 0.2F : 1.0F)) / 4.0F;
		}
	}

	private void face(Mesh m, Island.Kind kind, int f, int x, int y, int z, float height, float[] light) {
		Sheet.Region[] regions = this.faces.get(kind);
		Sheet.Region region = f == 0 ? regions[0] : (f == 1 ? regions[1] : regions[2]);
		int tint = f == 1 ? kind.topTint : (kind == Island.Kind.GRASS_BLOCK ? 0 : kind.sideTint);
		m.colour(this.lit(tint == 0 ? -1 : tint | 0xFF000000));
		quad(m, region, f, x, y, z, height, SHADE[f], light);
	}

	private static void quad(Mesh m, Sheet.Region region, int f, int x, int y, int z, float height, float shade, float[] light) {
		int[][] corners = CORNERS[f];
		int[] n = NORMALS[f];
		float[] px = new float[4];
		float[] py = new float[4];
		float[] pz = new float[4];
		float[] u = new float[4];
		float[] v = new float[4];

		for (int c = 0; c < 4; c++) {
			px[c] = (float)(x + corners[c][0]);
			py[c] = (float)y + (float)corners[c][1] * height;
			pz[c] = (float)(z + corners[c][2]);
			u[c] = region.u(c != 0 && c != 1 ? 15.98F : 0.02F);
			float top = f >= 2 ? (1.0F - height) * 16.0F : 0.0F;
			v[c] = region.v(c != 0 && c != 3 ? 15.98F : Math.max(0.02F, top));
		}

		boolean flip = light[1] + light[3] > light[0] + light[2];
		int[] order = flip ? new int[]{1, 2, 3, 1, 3, 0} : new int[]{0, 1, 2, 0, 2, 3};

		for (int c : order) {
			m.vertex(px[c], py[c], pz[c], (float)n[0], (float)n[1], (float)n[2], u[c], v[c], shade * light[c]);
		}
	}

	private void cross(Mesh m, Island.Kind kind, int x, int y, int z, float light) {
		Sheet.Region r = this.faces.get(kind)[2];
		m.colour(this.lit(kind.sideTint == 0 ? -1 : kind.sideTint | 0xFF000000));
		float a = 0.15F;
		float b = 0.85F;
		float u0 = r.u(0.02F);
		float u1 = r.u(15.98F);
		float v0 = r.v(0.02F);
		float v1 = r.v(15.98F);
		float[][] planes = new float[][]{
			{(float)x + 0.15F, (float)z + 0.15F, (float)x + 0.85F, (float)z + 0.85F}, {(float)x + 0.15F, (float)z + 0.85F, (float)x + 0.85F, (float)z + 0.15F}
		};

		for (float[] p : planes) {
			float nx = p[3] - p[1];
			float nz = p[0] - p[2];
			float l = (float)Math.sqrt((double)(nx * nx + nz * nz));

			for (int side = 0; side < 2; side++) {
				float sx = side == 0 ? 1.0F : -1.0F;
				float ax = side == 0 ? p[0] : p[2];
				float az = side == 0 ? p[1] : p[3];
				float bx = side == 0 ? p[2] : p[0];
				float bz = side == 0 ? p[3] : p[1];
				float ua = side == 0 ? u0 : u1;
				float ub = side == 0 ? u1 : u0;
				m.vertex(ax, (float)(y + 1), az, sx * nx / l, 0.0F, sx * nz / l, ua, v0, light);
				m.vertex(ax, (float)y, az, sx * nx / l, 0.0F, sx * nz / l, ua, v1, light);
				m.vertex(bx, (float)y, bz, sx * nx / l, 0.0F, sx * nz / l, ub, v1, light);
				m.vertex(ax, (float)(y + 1), az, sx * nx / l, 0.0F, sx * nz / l, ua, v0, light);
				m.vertex(bx, (float)y, bz, sx * nx / l, 0.0F, sx * nz / l, ub, v1, light);
				m.vertex(bx, (float)(y + 1), bz, sx * nx / l, 0.0F, sx * nz / l, ub, v0, light);
			}
		}
	}

	private void liquid(Mesh m, Island.Block b, int frame) {
		Sheet.Region r = this.water[frame % this.water.length];
		m.colour(this.lit(-12618012));
		Arrays.fill(this.ao, 1.0F);

		for (int f = 1; f < 6; f++) {
			int[] n = NORMALS[f];
			Island.Block next = this.at(b.x + n[0], b.y + n[1], b.z + n[2]);
			if (next == null || !next.kind.solid() && next.kind.shape != Island.Shape.WATER) {
				quad(m, r, f, b.x, b.y, b.z, 0.875F, SHADE[f], this.ao);
			}
		}
	}

	static final class Block {
		final int x;
		final int y;
		final int z;
		final Island.Kind kind;
		float sent = -1.0F;
		boolean standing;

		Block(int x, int y, int z, Island.Kind kind) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.kind = kind;
		}
	}

	static enum Kind {
		GRASS_BLOCK(Island.Shape.CUBE, "grass_block_side", "grass_block_top", "dirt", -7226023, 0),
		DIRT(Island.Shape.CUBE, "dirt"),
		SAND(Island.Shape.CUBE, "sand"),
		STONE(Island.Shape.CUBE, "stone"),
		ANDESITE(Island.Shape.CUBE, "andesite"),
		GRAVEL(Island.Shape.CUBE, "gravel"),
		DEEPSLATE(Island.Shape.CUBE, "deepslate", "deepcfhgui_top", "deepcfhgui_top", 0, 0),
		COAL_ORE(Island.Shape.CUBE, "coal_ore"),
		IRON_ORE(Island.Shape.CUBE, "iron_ore"),
		COPPER_ORE(Island.Shape.CUBE, "copper_ore"),
		GOLD_ORE(Island.Shape.CUBE, "gold_ore"),
		REDSTONE_ORE(Island.Shape.CUBE, "redstone_ore"),
		LAPIS_ORE(Island.Shape.CUBE, "lapis_ore"),
		DEEPSLATE_IRON_ORE(Island.Shape.CUBE, "deepcfhgui_iron_ore"),
		DEEPSLATE_REDSTONE_ORE(Island.Shape.CUBE, "deepcfhgui_redstone_ore"),
		DEEPSLATE_LAPIS_ORE(Island.Shape.CUBE, "deepcfhgui_lapis_ore"),
		DEEPSLATE_DIAMOND_ORE(Island.Shape.CUBE, "deepcfhgui_diamond_ore"),
		OAK_LOG(Island.Shape.CUBE, "oak_log", "oak_log_top", "oak_log_top", 0, 0),
		OAK_LEAVES(Island.Shape.LEAVES, "oak_leaves", "oak_leaves", "oak_leaves", -8934609, -8934609),
		WATER(Island.Shape.WATER, "water_still", "water_still", "water_still", -12618012, -12618012),
		SHORT_GRASS(Island.Shape.CROSS, "short_grass", "short_grass", "short_grass", -7226023, -7226023),
		POPPY(Island.Shape.CROSS, "poppy"),
		DANDELION(Island.Shape.CROSS, "dandelion"),
		CORNFLOWER(Island.Shape.CROSS, "cornflower"),
		OXEYE_DAISY(Island.Shape.CROSS, "oxeye_daisy"),
		AZURE_BLUET(Island.Shape.CROSS, "azure_bluet");

		final Island.Shape shape;
		final String side;
		final String top;
		final String bottom;
		final int topTint;
		final int sideTint;

		private Kind(final Island.Shape shape, final String all) {
			this(shape, all, all, all, 0, 0);
		}

		private Kind(final Island.Shape shape, final String side, final String top, final String bottom, final int topTint, final int sideTint) {
			this.shape = shape;
			this.side = side;
			this.top = top;
			this.bottom = bottom;
			this.topTint = topTint;
			this.sideTint = sideTint;
		}

		boolean solid() {
			return this.shape == Island.Shape.CUBE;
		}

		boolean shades() {
			return this.shape == Island.Shape.CUBE || this.shape == Island.Shape.LEAVES;
		}
	}

	static enum Shape {
		CUBE,
		LEAVES,
		CROSS,
		WATER;
	}
}
