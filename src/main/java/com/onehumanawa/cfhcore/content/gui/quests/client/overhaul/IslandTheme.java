package com.onehumanawa.cfhcore.content.gui.quests.client.overhaul;

import com.onehumanawa.cfhcore.content.gui.quests.client.view.Backdrops;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

final class IslandTheme {
	final IslandTheme.Family family;
	boolean chosenGround;
	int below;
	int horizon;
	int low;
	int high;
	int zenith;
	float sunYaw;
	float sunEl;
	float sunGlow;
	float sunDisc = 4.0F;
	float sunReach = 38.0F;
	int sunColor = -3376;
	float stars;
	boolean starsBelow;
	int clouds = -1;
	float cloudOpacity;
	int glow;
	boolean gridBelow;
	final float[] key = new float[]{0.8F, 0.76F, 0.66F};
	final float[] ambient = new float[]{0.44F, 0.47F, 0.52F};
	final float[] fill = new float[]{0.13F, 0.17F, 0.25F};
	final float[] fillDir = new float[]{1.0F, 0.25F, -0.4F};
	final float[] rim = new float[]{1.0F, 0.94F, 0.8F, 0.18F};
	float brightness = 1.0F;
	float wrap = 0.5F;
	int motes = 1358952136;
	float moteRise = 1.0F;
	float moteSize = 0.06F;
	int moteCount = 70;
	float bloom = 0.4F;
	float vignette = 0.28F;
	float warmth = 0.01F;
	float saturation = 1.12F;
	float contrast = 1.06F;
	BlockState top = Blocks.GRASS_BLOCK.defaultBlockState();
	BlockState under = Blocks.DIRT.defaultBlockState();
	BlockState deep = Blocks.STONE.defaultBlockState();
	BlockState tip = Blocks.DEEPSLATE.defaultBlockState();
	BlockState[] patches = new BlockState[0];
	BlockState[] ores = new BlockState[0];
	BlockState plinth = Blocks.STONE_BRICKS.defaultBlockState();
	float relief = 1.0F;
	float spread = 0.4F;
	@Nullable
	BlockState fluid;
	BlockState bed = Blocks.GRAVEL.defaultBlockState();
	float ponds = -0.5F;
	float pondClear = 2.3F;
	int grassTint;
	int foliageTint;
	int waterTint;
	BlockState laid = Blocks.DIRT_PATH.defaultBlockState();
	BlockState marker = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
	BlockState loose = Blocks.COARSE_DIRT.defaultBlockState();
	BlockState post = Blocks.STONE_BRICK_WALL.defaultBlockState();
	BlockState cap = Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
	BlockState goal = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
	BlockState challenge = Blocks.GOLD_BLOCK.defaultBlockState();
	BlockState gatePost = Blocks.STONE_BRICKS.defaultBlockState();
	BlockState gateLintel = Blocks.STONE_BRICKS.defaultBlockState();
	BlockState gateKey = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
	int stone = 9079173;

	private IslandTheme(IslandTheme.Family family) {
		this.family = family;
	}

	static IslandTheme of(@Nullable Chapter chapter) {
		Backdrops.Kind kind = Backdrops.kind(chapter);
		if (kind == Backdrops.Kind.TEXTURE && chapter != null) {
			IslandTheme.Family tab = advancementTab(chapter.backdrop);
			if (tab != null) {
				return of(tab).settled();
			} else {
				Block block = blockOf(chapter.backdrop);
				IslandTheme t = of(block == null ? IslandTheme.Family.COURT : family(block));
				if (block != null) {
					t.ground(block);
				}

				return t.settled();
			}
		} else {
			return of(family(kind)).settled();
		}
	}

	private IslandTheme settled() {
		int map = this.top.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).col;
		float ground = ((float)(map >> 16 & 0xFF) * 0.299F + (float)(map >> 8 & 0xFF) * 0.587F + (float)(map & 0xFF) * 0.114F) / 255.0F;
		if (this.top.is(Blocks.GRASS_BLOCK)) {
			ground = 0.38F;
		}

		float[] up = new float[3];
		float[] front = new float[3];

		for (int c = 0; c < 3; c++) {
			up[c] = this.ambient[c] + 0.9F * this.key[c] + 0.6F * this.fill[c];
			front[c] = this.ambient[c] + 0.62F * this.key[c];
		}

		float above = up[0] * 0.299F + up[1] * 0.587F + up[2] * 0.114F;
		float mean = front[0] * 0.299F + front[1] * 0.587F + front[2] * 0.114F;
		float lit = ground * above;
		float shown = ground > 0.62F ? Mth.clamp(lit - 0.3F, 0.34F, 0.5F) : Mth.clamp(lit + 0.2F, 0.52F, 0.72F);
		float tone = Mth.clamp(shown / Math.max(0.3F, above), 0.3F, 0.8F);
		int rgb = 0;

		for (int c = 0; c < 3; c++) {
			float lean = mean <= 0.0F ? 1.0F : (float)Math.pow((double)Math.max(0.25F, front[c] / mean), 0.75);
			rgb = rgb << 8 | Math.round(255.0F * Math.min(1.0F, tone / lean));
		}

		this.stone = rgb;
		return this;
	}

	private static IslandTheme.Family family(Backdrops.Kind kind) {
		return switch (kind) {
			case MEADOW -> IslandTheme.Family.MEADOW;
			case CAVE -> IslandTheme.Family.CAVE;
			case NETHER -> IslandTheme.Family.NETHER;
			case END -> IslandTheme.Family.END;
			case OCEAN -> IslandTheme.Family.OCEAN;
			case PARCHMENT -> IslandTheme.Family.PARCHMENT;
			case WORKSHOP -> IslandTheme.Family.WORKSHOP;
			case GRID -> IslandTheme.Family.GRID;
			default -> IslandTheme.Family.STARS;
		};
	}

	@Nullable
	private static IslandTheme.Family advancementTab(String backdrop) {
		String b = backdrop.trim().toLowerCase(Locale.ROOT);
		int at = b.indexOf("gui/advancements/backgrounds/");
		if (at < 0) {
			return null;
		} else {
			String name = b.substring(at + "gui/advancements/backgrounds/".length());
			if (name.startsWith("nether")) {
				return IslandTheme.Family.NETHER;
			} else if (name.startsWith("end")) {
				return IslandTheme.Family.END;
			} else if (name.startsWith("adventure") || name.startsWith("husbandry")) {
				return IslandTheme.Family.MEADOW;
			} else {
				return name.startsWith("stone") ? IslandTheme.Family.CAVE : null;
			}
		}
	}

	@Nullable
	private static Block blockOf(String backdrop) {
		ResourceLocation id = ResourceLocation.tryParse(backdrop.trim().toLowerCase(Locale.ROOT));
		if (id == null) {
			return null;
		} else {
			String path = id.getPath();
			if (path.endsWith(".png")) {
				path = path.substring(0, path.length() - 4);
			}

			if (path.indexOf(47) >= 0) {
				path = path.substring(path.lastIndexOf(47) + 1);
			}

			for (String name : new String[]{path, strip(path, "_top"), strip(path, "_side"), strip(path, "_bottom"), strip(path, "_front")}) {
				ResourceLocation block = ResourceLocation.tryBuild(id.getNamespace(), name);
				if (block != null) {
					Block found = (Block)BuiltInRegistries.BLOCK.getOptional(block).orElse(null);
					if (found != null && found != Blocks.AIR) {
						return found;
					}
				}
			}

			return null;
		}
	}

	private static String strip(String s, String end) {
		return s.endsWith(end) ? s.substring(0, s.length() - end.length()) : s;
	}

	private static IslandTheme.Family family(Block block) {
		String n = BuiltInRegistries.BLOCK.getKey(block).getPath();
		if (has(n, "nether", "blackstone", "basalt", "crimson", "warped", "soul", "magma", "glowstone", "shroomlight")) {
			return IslandTheme.Family.NETHER;
		} else if (has(n, "end_stone", "purpur", "chorus", "obsidian")) {
			return IslandTheme.Family.END;
		} else if (has(n, "prismarine", "coral", "sea_", "sand") && !n.contains("soul")) {
			return IslandTheme.Family.OCEAN;
		} else if (has(n, "deepslate", "tuff", "dripstone", "amethyst", "calcite", "_ore", "cobbled")) {
			return IslandTheme.Family.CAVE;
		} else if (has(n, "planks", "_log", "_wood", "bookshelf", "barrel")) {
			return IslandTheme.Family.WORKSHOP;
		} else if (has(n, "quartz", "concrete", "iron_block", "glass")) {
			return IslandTheme.Family.GRID;
		} else {
			return has(n, "stone", "brick", "andesite", "diorite", "granite", "terracotta", "copper") ? IslandTheme.Family.COURT : IslandTheme.Family.MEADOW;
		}
	}

	private static boolean has(String name, String... words) {
		for (String w : words) {
			if (name.contains(w)) {
				return true;
			}
		}

		return false;
	}

	private void ground(Block block) {
		BlockState state = block.defaultBlockState();
		if (state.getRenderShape() == RenderShape.MODEL && state.getFluidState().isEmpty()) {
			state = with(state, BlockStateProperties.MOISTURE, 7);
			boolean soil = state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND);
			this.chosenGround = !state.is(Blocks.GRASS_BLOCK);
			this.top = state;
			this.patches = variants(block);
			if (!soil) {
				this.under = state;
				this.plinth = pick(this.plinth, state, Blocks.POLISHED_ANDESITE.defaultBlockState());
			}

			if (this.laid.getBlock() == block) {
				this.laid = pick(Blocks.SMOOTH_STONE.defaultBlockState(), state, Blocks.POLISHED_ANDESITE.defaultBlockState());
			}

			if (this.marker.getBlock() == block) {
				this.marker = pick(Blocks.COBBLESTONE.defaultBlockState(), state, Blocks.GRAVEL.defaultBlockState());
			}

			if (this.loose.getBlock() == block) {
				this.loose = pick(Blocks.GRAVEL.defaultBlockState(), state, Blocks.COARSE_DIRT.defaultBlockState());
			}

			if (state.is(Blocks.FARMLAND)) {
				this.under = Blocks.DIRT.defaultBlockState();
				this.laid = Blocks.DIRT_PATH.defaultBlockState();
				this.marker = Blocks.COARSE_DIRT.defaultBlockState();
				this.loose = Blocks.ROOTED_DIRT.defaultBlockState();
			}
		}
	}

	private static BlockState pick(BlockState wanted, BlockState not, BlockState otherwise) {
		return wanted.getBlock() == not.getBlock() ? otherwise : wanted;
	}

	private static BlockState[] variants(Block block) {
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
		List<BlockState> out = new ArrayList<>();

		for (String prefix : new String[]{"cracked_", "mossy_"}) {
			ResourceLocation v = ResourceLocation.tryBuild(id.getNamespace(), prefix + id.getPath());
			Block found = v == null ? null : (Block)BuiltInRegistries.BLOCK.getOptional(v).orElse(null);
			if (found != null && found != Blocks.AIR) {
				out.add(found.defaultBlockState());
			}
		}

		return out.toArray(new BlockState[0]);
	}

	static <T extends Comparable<T>> BlockState with(BlockState state, Property<T> property, T value) {
		return state.hasProperty(property) ? (BlockState)state.setValue(property, value) : state;
	}

	private static BlockState b(Block block) {
		return block.defaultBlockState();
	}

	private static BlockState[] list(Block... blocks) {
		BlockState[] out = new BlockState[blocks.length];

		for (int i = 0; i < blocks.length; i++) {
			out[i] = blocks[i].defaultBlockState();
		}

		return out;
	}

	private IslandTheme sky(int below, int horizon, int low, int high, int zenith) {
		this.below = below;
		this.horizon = horizon;
		this.low = low;
		this.high = high;
		this.zenith = zenith;
		return this;
	}

	private IslandTheme sun(float yaw, float elevation, int color, float glow) {
		this.sunYaw = yaw;
		this.sunEl = elevation;
		this.sunColor = color;
		this.sunGlow = glow;
		return this;
	}

	private IslandTheme light(float kr, float kg, float kb, float ar, float ag, float ab, float fr, float fg, float fb, float rr, float rg, float rb, float rim) {
		this.key[0] = kr;
		this.key[1] = kg;
		this.key[2] = kb;
		this.ambient[0] = ar;
		this.ambient[1] = ag;
		this.ambient[2] = ab;
		this.fill[0] = fr;
		this.fill[1] = fg;
		this.fill[2] = fb;
		this.rim[0] = rr;
		this.rim[1] = rg;
		this.rim[2] = rb;
		this.rim[3] = rim;
		return this;
	}

	private IslandTheme pedestal(Block post, Block cap, Block goal, Block challenge) {
		this.post = b(post);
		this.cap = b(cap);
		this.goal = b(goal);
		this.challenge = b(challenge);
		return this;
	}

	private IslandTheme gate(Block post, Block lintel, Block keystone) {
		this.gatePost = b(post);
		this.gateLintel = b(lintel);
		this.gateKey = b(keystone);
		return this;
	}

	private IslandTheme paths(Block laid, Block marker, Block loose) {
		this.laid = b(laid);
		this.marker = b(marker);
		this.loose = b(loose);
		return this;
	}

	private static IslandTheme of(IslandTheme.Family family) {
		IslandTheme t = new IslandTheme(family);
		switch (family) {
			case MEADOW:
			case COURT:
				t.sky(-9655826, -3152134, -6501387, -10773016, -13668400).sun(-28.0F, 26.0F, -3384, 0.8F);
				t.clouds = -591105;
				t.cloudOpacity = 0.9F;
				t.glow = 1224736767;
				t.light(0.82F, 0.77F, 0.66F, 0.45F, 0.49F, 0.55F, 0.13F, 0.17F, 0.26F, 1.0F, 0.95F, 0.8F, 0.18F);
				t.ores = list(Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.COPPER_ORE, Blocks.ANDESITE, Blocks.GRAVEL);
				t.fluid = b(Blocks.WATER);
				t.bed = b(Blocks.SAND);
				if (family == IslandTheme.Family.COURT) {
					t.top = b(Blocks.STONE_BRICKS);
					t.under = b(Blocks.STONE_BRICKS);
					t.patches = list(Blocks.CRACKED_STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS);
					t.plinth = b(Blocks.POLISHED_ANDESITE);
					t.relief = 0.25F;
					t.fluid = null;
					t.paths(Blocks.SMOOTH_STONE, Blocks.COBBLESTONE, Blocks.GRAVEL);
					t.chosenGround = true;
				}
				break;
			case CAVE:
				t.sky(-16118765, -15460316, -15789541, -16118765, -16447990);
				t.clouds = -14933714;
				t.cloudOpacity = 0.55F;
				t.glow = 977293436;
				t.light(0.47F, 0.47F, 0.54F, 0.42F, 0.44F, 0.56F, 0.09F, 0.08F, 0.13F, 0.75F, 0.62F, 1.0F, 0.28F);
				t.motes = 1221116080;
				t.moteRise = -0.6F;
				t.bloom = 0.6F;
				t.vignette = 0.42F;
				t.warmth = 0.0F;
				t.top = b(Blocks.STONE);
				t.under = b(Blocks.STONE);
				t.deep = b(Blocks.DEEPSLATE);
				t.tip = b(Blocks.DEEPSLATE);
				t.patches = list(Blocks.ANDESITE, Blocks.GRAVEL, Blocks.TUFF, Blocks.COBBLESTONE);
				t.ores = list(
					Blocks.DEEPSLATE_DIAMOND_ORE,
					Blocks.DEEPSLATE_REDSTONE_ORE,
					Blocks.DEEPSLATE_LAPIS_ORE,
					Blocks.DEEPSLATE_GOLD_ORE,
					Blocks.DEEPSLATE_IRON_ORE,
					Blocks.DEEPSLATE_EMERALD_ORE,
					Blocks.COBBLED_DEEPSLATE
				);
				t.plinth = b(Blocks.POLISHED_TUFF);
				t.paths(Blocks.POLISHED_ANDESITE, Blocks.COBBLED_DEEPSLATE, Blocks.GRAVEL);
				t.pedestal(Blocks.TUFF_BRICK_WALL, Blocks.POLISHED_TUFF_SLAB, Blocks.CHISELED_TUFF_BRICKS, Blocks.RAW_GOLD_BLOCK);
				t.gate(Blocks.TUFF_BRICKS, Blocks.POLISHED_TUFF, Blocks.CHISELED_TUFF);
				break;
			case NETHER:
				t.sky(-13890552, -10216434, -12578550, -14415610, -15596541);
				t.clouds = -14808312;
				t.cloudOpacity = 0.6F;
				t.glow = 2030000670;
				t.light(0.44F, 0.3F, 0.2F, 0.58F, 0.4F, 0.36F, 0.26F, 0.08F, 0.04F, 1.0F, 0.55F, 0.25F, 0.34F);
				t.fillDir[0] = 0.3F;
				t.fillDir[1] = -1.0F;
				t.fillDir[2] = 0.2F;
				t.motes = -654331824;
				t.moteRise = 6.0F;
				t.moteSize = 0.07F;
				t.moteCount = 90;
				t.bloom = 0.55F;
				t.vignette = 0.4F;
				t.warmth = 0.02F;
				t.saturation = 0.96F;
				t.top = b(Blocks.NETHERRACK);
				t.under = b(Blocks.NETHERRACK);
				t.deep = b(Blocks.NETHERRACK);
				t.tip = b(Blocks.SMOOTH_BASALT);
				t.patches = list(Blocks.CRIMSON_NYLIUM, Blocks.CRIMSON_NYLIUM, Blocks.SOUL_SOIL, Blocks.WARPED_NYLIUM);
				t.ores = list(Blocks.NETHER_QUARTZ_ORE, Blocks.NETHER_GOLD_ORE, Blocks.SMOOTH_BASALT, Blocks.MAGMA_BLOCK, Blocks.BASALT);
				t.plinth = b(Blocks.POLISHED_BLACKSTONE);
				t.fluid = b(Blocks.LAVA);
				t.bed = b(Blocks.MAGMA_BLOCK);
				t.paths(Blocks.POLISHED_BASALT, Blocks.SMOOTH_BASALT, Blocks.SOUL_SOIL);
				t.pedestal(Blocks.DIORITE_WALL, Blocks.QUARTZ_SLAB, Blocks.CHISELED_QUARTZ_BLOCK, Blocks.GOLD_BLOCK);
				t.gate(Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN);
				break;
			case END:
				t.sky(-16054508, -14872016, -15397596, -15923176, -16382964);
				t.stars = 0.9F;
				t.starsBelow = true;
				t.clouds = -10860912;
				t.glow = 1077946990;
				t.light(0.38F, 0.36F, 0.46F, 0.42F, 0.39F, 0.52F, 0.11F, 0.08F, 0.18F, 0.8F, 0.6F, 1.0F, 0.3F);
				t.motes = 2026345194;
				t.moteRise = 0.5F;
				t.bloom = 0.5F;
				t.vignette = 0.4F;
				t.warmth = 0.0F;
				t.top = b(Blocks.END_STONE);
				t.under = b(Blocks.END_STONE);
				t.deep = b(Blocks.END_STONE);
				t.tip = b(Blocks.END_STONE);
				t.relief = 0.6F;
				t.plinth = b(Blocks.END_STONE_BRICKS);
				t.paths(Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR, Blocks.END_STONE_BRICKS);
				t.pedestal(Blocks.END_STONE_BRICK_WALL, Blocks.PURPUR_SLAB, Blocks.PURPUR_PILLAR, Blocks.PEARLESCENT_FROGLIGHT);
				t.gate(Blocks.PURPUR_PILLAR, Blocks.PURPUR_BLOCK, Blocks.END_STONE_BRICKS);
				break;
			case OCEAN:
				t.sky(-13725744, -2690822, -7351312, -12607264, -14716984).sun(22.0F, 34.0F, -1048, 0.8F);
				t.clouds = -721921;
				t.cloudOpacity = 0.85F;
				t.glow = 1489564927;
				t.light(0.54F, 0.52F, 0.46F, 0.4F, 0.45F, 0.49F, 0.08F, 0.16F, 0.22F, 0.9F, 1.0F, 1.0F, 0.18F);
				t.motes = 1224736767;
				t.ponds = 0.05F;
				t.pondClear = 1.7F;
				t.spread = 1.7F;
				t.top = b(Blocks.SAND);
				t.under = b(Blocks.SANDSTONE);
				t.deep = b(Blocks.STONE);
				t.tip = b(Blocks.PRISMARINE);
				t.patches = list(Blocks.GRAVEL, Blocks.SAND, Blocks.SAND);
				t.ores = list(Blocks.PRISMARINE, Blocks.DARK_PRISMARINE, Blocks.GRAVEL, Blocks.CLAY);
				t.plinth = b(Blocks.PRISMARINE_BRICKS);
				t.relief = 0.45F;
				t.fluid = b(Blocks.WATER);
				t.bed = b(Blocks.SAND);
				t.waterTint = -12593944;
				t.paths(Blocks.PRISMARINE_BRICKS, Blocks.PRISMARINE, Blocks.SANDSTONE);
				t.pedestal(Blocks.PRISMARINE_WALL, Blocks.PRISMARINE_BRICK_SLAB, Blocks.DARK_PRISMARINE, Blocks.SEA_LANTERN);
				t.gate(Blocks.DARK_PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.SEA_LANTERN);
				break;
			case PARCHMENT:
				t.sky(-4617118, -865130, -2317196, -6525862, -10730924).sun(-40.0F, 9.0F, -9824, 0.9F);
				t.clouds = -729930;
				t.cloudOpacity = 0.7F;
				t.glow = 1358948030;
				t.light(0.52F, 0.43F, 0.3F, 0.44F, 0.4F, 0.37F, 0.13F, 0.12F, 0.18F, 1.0F, 0.85F, 0.6F, 0.2F);
				t.motes = 1493166256;
				t.warmth = 0.024F;
				t.top = b(Blocks.SMOOTH_SANDSTONE);
				t.under = b(Blocks.SANDSTONE);
				t.deep = b(Blocks.SANDSTONE);
				t.tip = b(Blocks.TERRACOTTA);
				t.patches = list(Blocks.CUT_SANDSTONE);
				t.ores = list(Blocks.CUT_SANDSTONE, Blocks.TERRACOTTA, Blocks.SMOOTH_SANDSTONE);
				t.plinth = b(Blocks.CHISELED_SANDSTONE);
				t.relief = 0.0F;
				t.paths(Blocks.BIRCH_PLANKS, Blocks.TERRACOTTA, Blocks.WHITE_TERRACOTTA);
				t.pedestal(Blocks.SANDSTONE_WALL, Blocks.SMOOTH_SANDSTONE_SLAB, Blocks.CHISELED_SANDSTONE, Blocks.CHISELED_BOOKSHELF);
				t.gate(Blocks.CUT_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.CHISELED_SANDSTONE);
				break;
			case WORKSHOP:
				t.sky(-15064528, -12958632, -14011836, -15065040, -15855076);
				t.stars = 0.45F;
				t.clouds = -13879992;
				t.cloudOpacity = 0.6F;
				t.glow = 1078220920;
				t.light(0.58F, 0.53F, 0.5F, 0.42F, 0.41F, 0.46F, 0.1F, 0.11F, 0.17F, 1.0F, 0.8F, 0.55F, 0.26F);
				t.motes = -1056978832;
				t.moteRise = 3.0F;
				t.moteCount = 40;
				t.bloom = 0.56F;
				t.vignette = 0.38F;
				t.warmth = 0.02F;
				t.top = b(Blocks.SPRUCE_PLANKS);
				t.under = b(Blocks.STONE_BRICKS);
				t.deep = b(Blocks.STONE_BRICKS);
				t.tip = b(Blocks.COBBLESTONE);
				t.ores = list(Blocks.CRACKED_STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.COBBLESTONE);
				t.plinth = b(Blocks.SMOOTH_STONE);
				t.relief = 0.0F;
				t.paths(Blocks.POLISHED_ANDESITE, Blocks.COBBLESTONE, Blocks.GRAVEL);
				t.pedestal(Blocks.STONE_BRICK_WALL, Blocks.SPRUCE_SLAB, Blocks.CHISELED_STONE_BRICKS, Blocks.COPPER_BLOCK);
				t.gate(Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_PLANKS, Blocks.CHISELED_STONE_BRICKS);
				break;
			case STARS:
			default:
				t.sky(-16250086, -15064508, -15591114, -16117978, -16447468).sun(-30.0F, 32.0F, -2234113, 0.5F);
				t.sunDisc = 5.0F;
				t.sunReach = 24.0F;
				t.stars = 1.0F;
				t.starsBelow = true;
				t.clouds = -13879724;
				t.cloudOpacity = 0.6F;
				t.glow = 1011506336;
				t.light(0.48F, 0.52F, 0.68F, 0.4F, 0.43F, 0.56F, 0.09F, 0.09F, 0.16F, 0.7F, 0.8F, 1.0F, 0.3F);
				t.motes = 1759307519;
				t.moteRise = 0.5F;
				t.bloom = 0.5F;
				t.vignette = 0.4F;
				t.warmth = 0.0F;
				t.top = b(Blocks.SMOOTH_BASALT);
				t.under = b(Blocks.TUFF);
				t.deep = b(Blocks.DEEPSLATE);
				t.tip = b(Blocks.SMOOTH_BASALT);
				t.patches = list(Blocks.TUFF);
				t.ores = list(Blocks.TUFF, Blocks.SMOOTH_BASALT, Blocks.AMETHYST_BLOCK, Blocks.CALCITE);
				t.plinth = b(Blocks.POLISHED_ANDESITE);
				t.relief = 0.5F;
				t.paths(Blocks.POLISHED_DIORITE, Blocks.ANDESITE, Blocks.TUFF);
				t.pedestal(Blocks.DIORITE_WALL, Blocks.POLISHED_DIORITE_SLAB, Blocks.POLISHED_DIORITE, Blocks.AMETHYST_BLOCK);
				t.gate(Blocks.POLISHED_DIORITE, Blocks.POLISHED_DIORITE, Blocks.CHISELED_QUARTZ_BLOCK);
				break;
			case GRID:
				t.sky(-15587254, -14795154, -15191464, -15719356, -16115664);
				t.clouds = -8410904;
				t.glow = 1210999984;
				t.gridBelow = true;
				t.light(0.42F, 0.44F, 0.5F, 0.42F, 0.44F, 0.5F, 0.08F, 0.11F, 0.18F, 0.7F, 0.85F, 1.0F, 0.22F);
				t.motes = 1086314751;
				t.moteCount = 30;
				t.warmth = 0.0F;
				t.top = b(Blocks.WHITE_CONCRETE);
				t.under = b(Blocks.LIGHT_GRAY_CONCRETE);
				t.deep = b(Blocks.LIGHT_GRAY_CONCRETE);
				t.tip = b(Blocks.LIGHT_GRAY_CONCRETE);
				t.plinth = b(Blocks.SMOOTH_STONE);
				t.relief = 0.0F;
				t.paths(Blocks.BLUE_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE);
				t.pedestal(Blocks.DIORITE_WALL, Blocks.SMOOTH_QUARTZ_SLAB, Blocks.CHISELED_QUARTZ_BLOCK, Blocks.LAPIS_BLOCK);
				t.gate(Blocks.QUARTZ_PILLAR, Blocks.QUARTZ_BLOCK, Blocks.CHISELED_QUARTZ_BLOCK);
		}

		return t;
	}

	static enum Family {
		MEADOW,
		CAVE,
		NETHER,
		END,
		OCEAN,
		PARCHMENT,
		WORKSHOP,
		STARS,
		GRID,
		COURT;
	}
}
