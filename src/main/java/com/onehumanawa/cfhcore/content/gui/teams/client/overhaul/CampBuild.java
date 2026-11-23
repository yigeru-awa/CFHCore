package com.onehumanawa.cfhcore.content.gui.teams.client.overhaul;

import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.BlocksNode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class CampBuild {
	static final int FIRE_X = 0;
	static final int FIRE_Z = 3;
	static final int MAST_X = -2;
	static final int MAST_Z = -5;
	static final float HALF_X = 14.0F;
	static final float HALF_Z = 10.5F;
	static final float MID_Z = -2.0F;
	private static final float POWER = 2.4F;
	private static final int MIN_X = -16;
	private static final int MAX_X = 16;
	private static final int MIN_Z = -14;
	private static final int MAX_Z = 10;
	private static final int[][] TREES = new int[][]{{-13, -4, 7, 0, 2}, {6, -11, 8, 0, 4}, {-6, -11, 6, 1, 4}, {13, -3, 4, 2, 2}};
	private static final int[][] BUSHES = new int[][]{{-13, 2}, {13, 2}, {-10, -9}, {12, -8}, {-2, -12}};

	static float rim(int x, int z) {
		double fx = (double)((float)Math.abs(x) / 14.0F);
		double fz = (double)(Math.abs((float)z - -2.0F) / 10.5F);
		float f = (float)(Math.pow(fx, 2.4F) + Math.pow(fz, 2.4F));
		return f + 0.14F * (Build.smooth((float)x, (float)z, 4.2F, 11) - 0.5F);
	}

	static boolean flat(int x, int z) {
		return rim(x, z) < 0.86F;
	}

	private static int floor(int x, int z) {
		float r = rim(x, z);
		return r < 0.86F ? 0 : (r < 0.95F ? -1 : -2);
	}

	static CampBuild.Plan build(Stage stage, CampBuild.Tier tier, DyeColor cloth, boolean camp) {
		CampBuild.Plan plan = new CampBuild.Plan();
		boolean[][] taken = new boolean[33][25];
		int order = 1;
		take(taken, -1, 2, 1, 4);
		take(taken, -3, -6, -1, -4);
		if (camp) {
			take(taken, -4, -1, -3, -1);
			take(taken, 10, -2, 10, -1);
			if (tier.has(CampBuild.Tier.TENTS)) {
				take(taken, 3, -1, 4, -1);
				take(taken, 8, -3, 12, 0);
				take(taken, -12, -3, -8, 0);
				take(taken, 1, -3, 2, -3);
				take(taken, -5, -3, -4, -3);
			}

			if (tier.has(CampBuild.Tier.CAMP)) {
				take(taken, -11, -8, -5, -4);
				take(taken, -7, -2, -7, -2);
				take(taken, 7, -2, 7, -2);
			}

			if (tier.has(CampBuild.Tier.HALL)) {
				take(taken, 2, -9, 11, -3);
			}

			if (tier.has(CampBuild.Tier.STRONGHOLD)) {
				take(taken, -1, -12, 3, -8);
			}
		}

		plan.parts.add(new CampBuild.Part("ground", ground(stage, tier, camp, taken), 0));
		Piece fire = new Piece(stage, "fire", 0, 0, 3, 0, 0, 3);
		fire.put(0, 0, 3, camp ? "campfire[lit=true,signal_fire=false,facing=south]" : "campfire[lit=false,signal_fire=false,facing=south]");
		plan.parts.add(new CampBuild.Part(fire.key, fire.done(stage), order++));
		plan.parts.add(new CampBuild.Part("mast", mast(stage, camp && tier.has(CampBuild.Tier.CAMP), plan), order++));
		if (!camp) {
			return plan;
		} else {
			Piece log = new Piece(stage, "log_left", -4, 0, -1, -3, 0, -1);
			log.fill(-4, 0, -1, -3, 0, -1, "oak_log[axis=x]");
			plan.parts.add(new CampBuild.Part(log.key, log.done(stage), order++));
			Piece bed = new Piece(stage, "bed_right", 10, 0, -2, 10, 0, -1);
			bed.put(10, 0, -2, "white_carpet").put(10, 0, -1, Build.carpet(cloth));
			plan.parts.add(new CampBuild.Part(bed.key, bed.done(stage), order++));
			if (tier.has(CampBuild.Tier.TENTS)) {
				boolean pale = cloth == DyeColor.WHITE || cloth == DyeColor.LIGHT_GRAY;
				BlockState canvas = Build.of(pale ? "light_gray_wool" : "white_wool");
				BlockState shade = Build.of(pale ? "gray_wool" : "light_gray_wool");
				plan.parts.add(new CampBuild.Part("tent_right", tent(stage, "tent_right", 8, -3, 5, 4, canvas, Build.wool(cloth), shade), order++));
				Piece bed2 = new Piece(stage, "bed_left", -10, 0, -2, -10, 0, -1);
				bed2.put(-10, 0, -2, "white_carpet").put(-10, 0, -1, Build.carpet(cloth));
				plan.parts.add(new CampBuild.Part(bed2.key, bed2.done(stage), order));
				plan.parts.add(new CampBuild.Part("tent_left", tent(stage, "tent_left", -12, -3, 5, 4, canvas, Build.wool(cloth), shade), order++));
				Piece log2 = new Piece(stage, "log_right", 3, 0, -1, 4, 0, -1);
				log2.fill(3, 0, -1, 4, 0, -1, "oak_log[axis=x]");
				plan.parts.add(new CampBuild.Part(log2.key, log2.done(stage), order++));
				plan.parts.add(new CampBuild.Part("supplies", supplies(stage, tier), order++));
			}

			if (tier.has(CampBuild.Tier.CAMP)) {
				plan.parts.add(new CampBuild.Part("tent_command", commandTent(stage, cloth, plan), order++));

				for (int x : new int[]{-7, 7}) {
					Piece post = new Piece(stage, "lantern_" + (x < 0 ? "left" : "right"), x, 0, -2, x, 2, -2);
					post.put(x, 0, -2, "spruce_fence").put(x, 1, -2, "spruce_fence").put(x, 2, -2, "lantern[hanging=false]");
					plan.parts.add(new CampBuild.Part(post.key, post.done(stage), order++));
					plan.lanterns.add(new CampBuild.Lantern((float)x, 2.4F, -2.0F, post.key));
				}
			}

			if (tier.has(CampBuild.Tier.HALL)) {
				plan.parts.add(new CampBuild.Part("hall", hall(stage, plan), order++));
			}

			if (tier.has(CampBuild.Tier.STRONGHOLD)) {
				palisade(stage, taken, plan, order);
				order += 3;
				plan.parts.add(new CampBuild.Part("tower", tower(stage, plan), order));
			}

			return plan;
		}
	}

	private static void take(boolean[][] taken, int x0, int z0, int x1, int z1) {
		for (int x = Math.max(-16, x0); x <= Math.min(16, x1); x++) {
			for (int z = Math.max(-14, z0); z <= Math.min(10, z1); z++) {
				taken[x - -16][z - -14] = true;
			}
		}
	}

	private static boolean taken(boolean[][] taken, int x, int z) {
		return x < -16 || x > 16 || z < -14 || z > 10 || taken[x - -16][z - -14];
	}

	private static float sq(float v) {
		return v * v;
	}

	private static float yard(int x, int z, CampBuild.Tier tier) {
		float grown = 0.82F + 0.06F * (float)tier.ordinal();
		return sq((float)x / (8.6F * grown)) + sq(((float)z - 1.6F) / (4.2F * grown));
	}

	private static BlockState surface(int x, int z, CampBuild.Tier tier, boolean camp) {
		float c = Build.chance(x, z, 3);
		float byFire = sq((float)(x - 0)) + sq((float)(z - 3));
		if (byFire < 2.6F) {
			return Build.of("coarse_dirt");
		} else {
			if (camp) {
				float y = yard(x, z, tier);
				if (y < 1.0F && c < 0.86F - 0.62F * y) {
					return c < 0.3F ? Build.of("coarse_dirt") : Build.of("dirt_path");
				}

				if (tier.has(CampBuild.Tier.HALL) && x >= 5 && x <= 6 && z >= -3 && z <= -1 && c < 0.8F) {
					return Build.of("dirt_path");
				}
			}

			return c > 0.965F ? Build.of("moss_block") : Build.of("grass_block");
		}
	}

	private static BlocksNode ground(Stage stage, CampBuild.Tier tier, boolean camp, boolean[][] taken) {
		Piece p = new Piece(stage, "ground", -16, -13, -14, 16, 11, 10);
		String[] rock = new String[]{"stone", "stone", "stone", "stone", "andesite", "cobblestone", "mossy_cobblestone", "tuff"};
		String[] flowers = new String[]{"dandelion", "poppy", "oxeye_daisy", "cornflower", "azure_bluet", "allium"};
		boolean[][] wooded = new boolean[33][25];

		for (int[] tree : TREES) {
			wooded[tree[0] - -16][tree[1] - -14] = true;
		}

		for (int[] bush : BUSHES) {
			wooded[bush[0] - -16][bush[1] - -14] = true;
		}

		for (int x = -16; x <= 16; x++) {
			for (int z = -14; z <= 10; z++) {
				float r = rim(x, z);
				if (!(r >= 1.0F)) {
					int top = floor(x, z) - 1;
					int depth = 1 + Math.round(7.2F * (float)Math.pow((double)Math.max(0.0F, 1.0F - r), 0.7) + 1.3F * Build.smooth((float)x, (float)z, 3.1F, 7));
					int bottom = top - depth + 1;

					for (int y = bottom; y <= top; y++) {
						int below = top - y;
						BlockState s;
						if (below == 0) {
							s = top == -1 ? surface(x, z, tier, camp) : Build.of("grass_block");
						} else if (below <= 2) {
							s = Build.of(Build.chance(x * 31 + y, z, 5) < 0.22F ? "rooted_dirt" : "dirt");
						} else {
							s = Build.of(rock[(int)(Build.chance(x * 17 + y, z * 13 - y, 6) * (float)rock.length) % rock.length]);
						}

						p.put(x, y, z, s);
					}

					float hang = Build.chance(x, z, 8);
					if (depth <= 3 && hang < 0.5F) {
						p.put(x, bottom - 1, z, "hanging_roots");
					} else if (depth > 3 && hang < 0.2F) {
						p.put(x, bottom - 1, z, "pointed_dripstone[vertical_direction=down,thickness=tip]");
					}

					if (p.at(x, top, z).is(Blocks.GRASS_BLOCK) && !taken(taken, x, z) && !wooded[x - -16][z - -14] && (!camp || !(yard(x, z, tier) < 1.15F))) {
						float grow = Build.chance(x, z, 4);
						if (grow < 0.24F) {
							p.put(x, top + 1, z, "short_grass");
						} else if (grow < 0.3F) {
							p.put(x, top + 1, z, "fern");
						} else if (grow < 0.37F) {
							p.put(x, top + 1, z, flowers[(int)(Build.chance(x, z, 9) * (float)flowers.length) % flowers.length]);
						}
					}
				}
			}
		}

		for (int[] tree : TREES) {
			if (!(rim(tree[0], tree[1]) >= 1.0F) && !taken(taken, tree[0], tree[1])) {
				int y = floor(tree[0], tree[1]);
				if (tree[3] == 0) {
					spruce(p, taken, tree[0], y, tree[1], tree[2], tree[4]);
				} else {
					broadleaf(p, taken, tree[0], y, tree[1], tree[2], tree[3] == 1 ? "birch_log" : "oak_log", tree[3] == 1 ? "birch_leaves" : "oak_leaves");
				}
			}
		}

		for (int[] bush : BUSHES) {
			if (rim(bush[0], bush[1]) < 1.0F && !taken(taken, bush[0], bush[1])) {
				p.put(bush[0], floor(bush[0], bush[1]), bush[1], leaves("oak_leaves"));
			}
		}

		for (int[] at : new int[][]{{-9, 7}, {8, 7}, {-15, -3}, {10, -10}}) {
			if (rim(at[0], at[1]) < 0.95F && !taken(taken, at[0], at[1])) {
				p.put(at[0], floor(at[0], at[1]), at[1], "mossy_cobblestone");
			}
		}

		return p.done(stage);
	}

	static BlocksNode farLand(Stage stage, int halfX, int halfZ, int seed) {
		Piece p = new Piece(stage, "far_land_" + seed, -halfX - 1, -8, -halfZ - 1, halfX + 1, 9, halfZ + 1);

		for (int x = -halfX; x <= halfX; x++) {
			for (int z = -halfZ; z <= halfZ; z++) {
				float r = sq((float)x / (float)halfX) + sq((float)z / (float)halfZ) + 0.3F * (Build.smooth((float)x, (float)z, 3.3F, 40 + seed) - 0.5F);
				if (!(r >= 1.0F)) {
					int depth = 1 + Math.round(6.0F * (float)Math.pow((double)(1.0F - r), 0.7));

					for (int y = -depth; y < 0; y++) {
						p.put(x, y, z, y == -1 ? "grass_block" : (y >= -3 ? "dirt" : "stone"));
					}
				}
			}
		}

		boolean[][] none = new boolean[33][25];
		if (seed % 2 == 0) {
			spruce(p, none, -halfX / 3, 0, 0, 6, 2);
		} else {
			broadleaf(p, none, halfX / 3, 0, -halfZ / 3, 5, "oak_log", "oak_leaves");
		}

		if (halfX > 5) {
			spruce(p, none, halfX / 2, 0, halfZ / 3, 5, 2);
		}

		return p.done(stage);
	}

	private static BlockState leaves(String name) {
		return Build.of(name + "[persistent=true]");
	}

	private static void leaf(Piece p, boolean[][] taken, int x, int y, int z, BlockState crown) {
		if (!taken(taken, x, z) && p.empty(x, y, z)) {
			p.put(x, y, z, crown);
		}
	}

	private static void spruce(Piece p, boolean[][] taken, int x, int y, int z, int height, int from) {
		for (int i = 0; i < height; i++) {
			p.put(x, y + i, z, "spruce_log[axis=y]");
		}

		BlockState crown = leaves("spruce_leaves");

		for (int dy = from; dy <= height + 1; dy++) {
			int reach = dy > height ? 0 : (dy == height ? 1 : ((height - dy) % 2 == 0 ? 2 : 1));

			for (int dx = -reach; dx <= reach; dx++) {
				for (int dz = -reach; dz <= reach; dz++) {
					if ((reach != 2 || Math.abs(dx) != 2 || Math.abs(dz) != 2) && (dx != 0 || dz != 0 || dy >= height)) {
						leaf(p, taken, x + dx, y + dy, z + dz, crown);
					}
				}
			}
		}
	}

	private static void broadleaf(Piece p, boolean[][] taken, int x, int y, int z, int height, String log, String leaf) {
		for (int i = 0; i < height; i++) {
			p.put(x, y + i, z, log + "[axis=y]");
		}

		BlockState crown = leaves(leaf);

		for (int dy = height - 2; dy <= height + 1; dy++) {
			int reach = dy >= height ? 1 : 2;

			for (int dx = -reach; dx <= reach; dx++) {
				for (int dz = -reach; dz <= reach; dz++) {
					if ((Math.abs(dx) != reach || Math.abs(dz) != reach || reach != 1 && !(Build.chance(x + dx, z + dz, 20 + dy) < 0.5F))
						&& (dx != 0 || dz != 0 || dy >= height)) {
						leaf(p, taken, x + dx, y + dy, z + dz, crown);
					}
				}
			}
		}
	}

	private static BlocksNode mast(Stage stage, boolean plinth, CampBuild.Plan plan) {
		Piece p = new Piece(stage, "mast", -3, 0, -6, -1, 7, -4);
		int foot = 0;
		if (plinth) {
			p.put(-2, 0, -5, "chiseled_stone_bricks");
			p.put(-2, 0, -4, "stone_brick_stairs[facing=north,half=bottom,shape=straight]");
			p.put(-2, 0, -6, "stone_brick_stairs[facing=south,half=bottom,shape=straight]");
			p.put(-1, 0, -5, "stone_brick_stairs[facing=west,half=bottom,shape=straight]");
			p.put(-3, 0, -5, "stone_brick_stairs[facing=east,half=bottom,shape=straight]");
			foot = 1;
		}

		for (int y = foot; y < foot + 6; y++) {
			p.put(-2, y, -5, "spruce_fence");
		}

		plan.mastTop = (float)(foot + 6);
		return p.done(stage);
	}

	private static BlocksNode tent(Stage stage, String key, int x0, int z0, int width, int depth, BlockState canvas, BlockState ridge, BlockState shade) {
		Piece p = new Piece(stage, key, x0, 0, z0, x0 + width - 1, width / 2, z0 + depth - 1);
		shell(p, x0, z0, width, depth, canvas, ridge, shade);
		return p.done(stage);
	}

	private static void shell(Piece p, int x0, int z0, int width, int depth, BlockState canvas, BlockState ridge, BlockState shade) {
		int rise = width / 2;
		int front = z0 + depth - 1;

		for (int z = z0; z <= front; z++) {
			for (int step = 0; step <= rise; step++) {
				BlockState s = step == rise ? ridge : canvas;
				p.put(x0 + step, step, z, s);
				p.put(x0 + width - 1 - step, step, z, s);
			}
		}

		for (int step = 1; step <= rise; step++) {
			for (int y = 0; y < step; y++) {
				p.put(x0 + step, y, z0, shade);
				p.put(x0 + width - 1 - step, y, z0, shade);
			}
		}
	}

	private static BlocksNode supplies(Stage stage, CampBuild.Tier tier) {
		Piece p = new Piece(stage, "supplies", -5, 0, -3, 2, 1, -3);
		p.put(1, 0, -3, "barrel[facing=up]").put(2, 0, -3, "barrel[facing=south]").put(-4, 0, -3, "crafting_table");
		if (tier.has(CampBuild.Tier.CAMP)) {
			p.put(2, 1, -3, "hay_block[axis=x]").put(-5, 0, -3, "smithing_table");
		}

		return p.done(stage);
	}

	private static BlocksNode commandTent(Stage stage, DyeColor cloth, CampBuild.Plan plan) {
		Piece p = new Piece(stage, "tent_command", -11, 0, -8, -5, 3, -4);
		shell(p, -11, -8, 7, 4, Build.wool(cloth), Build.of(cloth == DyeColor.WHITE ? "light_gray_wool" : "white_wool"), Build.wool(cloth));
		p.fill(-10, 0, -6, -6, 0, -5, Build.carpet(cloth));
		p.put(-8, 0, -7, "spruce_slab[type=top]").put(-8, 1, -7, "lantern[hanging=false]").put(-9, 0, -7, "cartography_table").put(-7, 0, -7, "barrel[facing=up]");
		plan.lanterns.add(new CampBuild.Lantern(-8.0F, 1.5F, -6.0F, "tent_command"));
		return p.done(stage);
	}

	private static BlocksNode hall(Stage stage, CampBuild.Plan plan) {
		Piece p = new Piece(stage, "hall", 2, 0, -9, 11, 7, -3);
		int x0 = 3;
		int x1 = 10;
		int back = -8;
		int front = -4;

		for (int x = 3; x <= 10; x++) {
			for (int z = -8; z <= -4; z++) {
				boolean wall = x == 3 || x == 10 || z == -8 || z == -4;
				if (wall) {
					boolean corner = (x == 3 || x == 10) && (z == -8 || z == -4);

					for (int y = 0; y <= 2; y++) {
						p.put(x, y, z, corner ? "dark_oak_log[axis=y]" : (y == 0 ? (Build.chance(x, z, 30) < 0.3F ? "mossy_cobblestone" : "cobblestone") : "spruce_planks"));
					}
				}
			}
		}

		p.put(6, 0, -4, "spruce_door[facing=north,half=lower,hinge=left,open=false]");
		p.put(6, 1, -4, "spruce_door[facing=north,half=upper,hinge=left,open=false]");

		for (int x : new int[]{4, 8}) {
			p.put(x, 1, -4, "glass_pane");
			p.put(x, 1, -5, "ochre_froglight[axis=y]");
		}

		for (int x = 2; x <= 11; x++) {
			for (int step = 0; step < 3; step++) {
				p.put(x, 2 + step, -3 - step, "dark_oak_stairs[facing=north,half=bottom,shape=straight]");
				p.put(x, 2 + step, -9 + step, "dark_oak_stairs[facing=south,half=bottom,shape=straight]");
			}

			p.put(x, 5, -6, "dark_oak_slab[type=bottom]");
		}

		for (int x : new int[]{3, 10}) {
			p.fill(x, 3, -7, x, 3, -5, "spruce_planks");
			p.put(x, 4, -6, "spruce_planks");
		}

		for (int y = 0; y <= 6; y++) {
			p.put(9, y, -7, Build.chance(9, y, 31) < 0.3F ? "mossy_cobblestone" : "cobblestone");
		}

		plan.chimney = new float[]{9.0F, 7.1F, -7.0F};
		p.put(5, 1, -3, "lantern[hanging=true]").put(7, 1, -3, "lantern[hanging=true]");
		plan.lanterns.add(new CampBuild.Lantern(6.0F, 1.5F, -2.8F, "hall"));
		return p.done(stage);
	}

	private static void palisade(Stage stage, boolean[][] taken, CampBuild.Plan plan, int order) {
		Piece[] lengths = new Piece[]{
			new Piece(stage, "palisade_left", -16, 0, -14, -6, 4, 0),
			new Piece(stage, "palisade_back", -5, 0, -14, 5, 4, 0),
			new Piece(stage, "palisade_right", 6, 0, -14, 16, 4, 0)
		};

		for (int x = -16; x <= 16; x++) {
			for (int z = -14; z <= -1; z++) {
				if (flat(x, z) && !taken(taken, x, z) && (!flat(x - 1, z) || !flat(x + 1, z) || !flat(x, z - 1) || !flat(x, z + 1))) {
					Piece p = lengths[x <= -6 ? 0 : (x <= 5 ? 1 : 2)];
					int height = 3 + Math.floorMod(x * 3 + z * 5, 2);

					for (int y = 0; y < height; y++) {
						p.put(x, y, z, y == height - 1 ? "stripped_spruce_log[axis=y]" : "spruce_log[axis=y]");
					}

					if (z == -1 && !flat(x < 0 ? x - 1 : x + 1, z)) {
						p.put(x, height, z, "torch");
						plan.lanterns.add(new CampBuild.Lantern((float)x, (float)height + 0.6F, (float)z, p.key));
					}
				}
			}
		}

		for (int i = 0; i < lengths.length; i++) {
			plan.parts.add(new CampBuild.Part(lengths[i].key, lengths[i].done(stage), order + i));
		}
	}

	private static BlocksNode tower(Stage stage, CampBuild.Plan plan) {
		int cx = 1;
		int cz = -10;
		Piece p = new Piece(stage, "tower", -1, 0, -12, 3, 9, -8);

		for (int dx : new int[]{-1, 1}) {
			for (int dz : new int[]{-1, 1}) {
				for (int y = 0; y <= 4; y++) {
					p.put(1 + dx, y, -10 + dz, "spruce_log[axis=y]");
				}

				p.put(1 + dx, 6, -10 + dz, "spruce_fence").put(1 + dx, 7, -10 + dz, "spruce_fence");
			}
		}

		p.fill(0, 5, -11, 2, 5, -9, "spruce_planks");
		p.put(1, 6, -11, "spruce_fence").put(0, 6, -10, "spruce_fence").put(2, 6, -10, "spruce_fence");
		p.fill(-1, 8, -12, 3, 8, -8, "dark_oak_slab[type=bottom]");
		p.fill(0, 8, -11, 2, 8, -9, "dark_oak_planks");
		p.put(1, 9, -10, "dark_oak_slab[type=bottom]");
		p.put(1, 7, -10, "lantern[hanging=true]");
		plan.lanterns.add(new CampBuild.Lantern(1.0F, 7.2F, -10.0F, "tower"));
		plan.towerBanner = new float[]{1.0F, 4.0F, -8.0F};
		return p.done(stage);
	}

	private CampBuild() {
	}

	static record Lantern(float x, float y, float z, String part) {
	}

	static record Part(String key, BlocksNode node, int order) {
	}

	static final class Plan {
		final List<CampBuild.Part> parts = new ArrayList<>();
		final List<CampBuild.Lantern> lanterns = new ArrayList<>();
		float mastTop;
		float[] chimney;
		float[] towerBanner;
	}

	static enum Tier {
		BEDROLL(1),
		TENTS(2),
		CAMP(4),
		HALL(6),
		STRONGHOLD(9);

		final int level;

		private Tier(final int level) {
			this.level = level;
		}

		static CampBuild.Tier of(int level) {
			CampBuild.Tier at = BEDROLL;

			for (CampBuild.Tier t : values()) {
				if (level >= t.level) {
					at = t;
				}
			}

			return at;
		}

		CampBuild.Tier next() {
			return this.ordinal() + 1 < values().length ? values()[this.ordinal() + 1] : null;
		}

		boolean has(CampBuild.Tier other) {
			return this.ordinal() >= other.ordinal();
		}
	}
}
