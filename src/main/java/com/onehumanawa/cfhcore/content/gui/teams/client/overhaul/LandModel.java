package com.onehumanawa.cfhcore.content.gui.teams.client.overhaul;

import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.QuadMesh;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.TerrainNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.TerrainNode.Column;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class LandModel {
	static final int RADIUS = 12;
	static final int CHUNKS = 25;
	static final int PER = 4;
	static final int CELLS = 100;
	static final int BLOCKS = 4;
	private static final int WHITE = -1;
	private static final int DEEPEST = 3;
	private static final int HIGHEST = 46;
	private static final int RELIEF = 22;
	private static final long BUDGET_NS = 5000000L;
	private static final Column BLANK = new Column(1, "sandstone_top", -7700110, "sandstone_top", 0, -1, false, null, null, -1);
	private static final int GAP = 2;
	private static final int DARK_GAP = 12;
	private static final int THICKEST = 48;
	private static final int ALONG = 6;
	private static final LandModel.Sample VOID = new LandModel.Sample(Integer.MIN_VALUE, Integer.MIN_VALUE, "stone", -1, "stone", -1, null, null, -1);
	private static final LandModel.Face PLAIN = new LandModel.Face("white_concrete", -2);
	private static final LandModel.Face BED = new LandModel.Face("gravel", -1);
	private final TerrainNode node = new TerrainNode(100, 100).still();
	private final LandModel.Sample[] samples = new LandModel.Sample[10000];
	private final short[] top = new short[10000];
	private final short[] chunkTop = new short[625];
	private final boolean[] known = new boolean[625];
	private final boolean[] doubtful = new boolean[625];
	private final boolean[] laidFloating = new boolean[10000];
	private final int[] order;
	private int knownCount;
	private int scaledAt;
	private long quietUntilMs;
	private int originX;
	private int originZ;
	private String dimension = "";
	@Nullable
	private ClientLevel level;
	private boolean placed;
	private boolean invented;
	private int base = 56;
	private int rise = 2;
	private int version;
	private final Map<BlockState, LandModel.Face> tops = new IdentityHashMap<>();
	private final Map<BlockState, LandModel.Face> sides = new IdentityHashMap<>();
	private final Map<String, Boolean> textures = new HashMap<>();
	private final RandomSource random = RandomSource.create(42L);
	private final MutableBlockPos pos = new MutableBlockPos();

	LandModel() {
		List<Integer> byDistance = new ArrayList<>(625);

		for (int i = 0; i < 625; i++) {
			byDistance.add(i);
		}

		byDistance.sort((a, b) -> Integer.compare(far(a), far(b)));
		this.order = byDistance.stream().mapToInt(Integer::intValue).toArray();
	}

	private static int far(int chunk) {
		int x = chunk % 25 - 12;
		int z = chunk / 25 - 12;
		return x * x + z * z;
	}

	TerrainNode node() {
		return this.node;
	}

	int originX() {
		return this.originX;
	}

	int originZ() {
		return this.originZ;
	}

	String dimension() {
		return this.dimension;
	}

	int version() {
		return this.version;
	}

	int top(int cellX, int cellZ) {
		return cellX >= 0 && cellZ >= 0 && cellX < 100 && cellZ < 100 ? this.top[cellZ * 100 + cellX] : 0;
	}

	int chunkTop(int ix, int iz) {
		return ix >= 0 && iz >= 0 && ix < 25 && iz < 25 ? this.chunkTop[iz * 25 + ix] : 0;
	}

	float localX(double blockX) {
		return (float)((blockX - (double)this.originX * 16.0) / 4.0) - 50.0F;
	}

	float localZ(double blockZ) {
		return (float)((blockZ - (double)this.originZ * 16.0) / 4.0) - 50.0F;
	}

	int topAt(double blockX, double blockZ) {
		return this.top(Mth.floor((blockX - (double)this.originX * 16.0) / 4.0), Mth.floor((blockZ - (double)this.originZ * 16.0) / 4.0));
	}

	boolean read() {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel now = mc.level;
		Vec3 at = TeamsView.position();
		int midX = at == null ? 0 : Mth.floor(at.x) >> 4;
		int midZ = at == null ? 0 : Mth.floor(at.z) >> 4;
		String dim = TeamsView.dimension();
		boolean changed = false;
		if (!this.placed || now != this.level || !dim.equals(this.dimension) || Math.abs(midX - this.originX - 12) > 6 || Math.abs(midZ - this.originZ - 12) > 6) {
			this.placed = true;
			this.level = now;
			this.dimension = dim;
			this.originX = midX - 12;
			this.originZ = midZ - 12;
			this.wipe();
			changed = true;
		}

		if (now == null) {
			if (this.invented) {
				return changed;
			} else {
				this.invent();
				this.invented = true;
				this.version++;
				return true;
			}
		} else if (this.survey(now, mc)) {
			this.version++;
			return true;
		} else {
			if (changed) {
				this.version++;
			}

			return changed;
		}
	}

	private void wipe() {
		Arrays.fill(this.samples, null);
		Arrays.fill(this.known, false);
		Arrays.fill(this.doubtful, false);
		Arrays.fill(this.laidFloating, false);
		this.knownCount = 0;
		this.scaledAt = 0;
		this.quietUntilMs = 0L;
		this.invented = false;
		this.node.clear();

		for (int z = 0; z < 100; z++) {
			for (int x = 0; x < 100; x++) {
				this.node.put(x, z, BLANK);
			}
		}

		Arrays.fill(this.top, (short)1);
		Arrays.fill(this.chunkTop, (short)1);
	}

	private boolean survey(ClientLevel world, Minecraft mc) {
		if (this.knownCount < 625 && System.currentTimeMillis() >= this.quietUntilMs) {
			boolean roofed = world.dimensionType().hasCeiling();
			int roofY = mc.player == null ? world.getSeaLevel() : mc.player.getBlockY();
			long deadline = System.nanoTime() + 5000000L;
			List<Integer> fresh = new ArrayList<>();

			for (int i : this.order) {
				if (!this.known[i]) {
					int ix = i % 25;
					int iz = i / 25;
					LevelChunk chunk = world.getChunkSource().getChunk(this.originX + ix, this.originZ + iz, ChunkStatus.FULL, false);
					if (chunk != null) {
						for (int cz = 0; cz < 4; cz++) {
							for (int cx = 0; cx < 4; cx++) {
								int bx = (this.originX + ix << 4) + cx * 4 + 2;
								int bz = (this.originZ + iz << 4) + cz * 4 + 2;
								LandModel.Sample s = this.sample(world, chunk, bx, bz, roofed, roofY);
								this.samples[(iz * 4 + cz) * 100 + ix * 4 + cx] = s;
								if (s.floats() && !s.sure()) {
									this.doubtful[i] = true;
								}
							}
						}

						this.known[i] = true;
						this.knownCount++;
						fresh.add(i);
						if (System.nanoTime() > deadline) {
							break;
						}
					}
				}
			}

			if (fresh.isEmpty()) {
				this.quietUntilMs = System.currentTimeMillis() + 400L;
				return false;
			} else {
				if (this.scaledAt != 0 && this.knownCount < this.scaledAt * 3) {
					for (int ix : fresh) {
						this.lay(ix);
					}

					this.layNear(fresh);
				} else {
					this.scale(world, roofed, roofY);
					this.scaledAt = this.knownCount;

					for (int ix = 0; ix < 625; ix++) {
						if (this.known[ix]) {
							this.lay(ix);
						}
					}
				}

				return true;
			}
		} else {
			return false;
		}
	}

	private void layNear(List<Integer> fresh) {
		int r = 2;
		Set<Integer> again = new HashSet<>();

		for (int i : fresh) {
			int ix = i % 25;
			int iz = i / 25;

			for (int dz = -2; dz <= 2; dz++) {
				for (int dx = -2; dx <= 2; dx++) {
					int jx = ix + dx;
					int jz = iz + dz;
					int j = jz * 25 + jx;
					if (jx >= 0 && jz >= 0 && jx < 25 && jz < 25 && this.known[j] && this.doubtful[j] && !fresh.contains(j)) {
						again.add(j);
					}
				}
			}
		}

		for (int j : again) {
			if (this.changes(j)) {
				this.lay(j);
			}
		}
	}

	private boolean changes(int chunk) {
		int ix = chunk % 25;
		int iz = chunk / 25;

		for (int cz = 0; cz < 4; cz++) {
			for (int cx = 0; cx < 4; cx++) {
				int x = ix * 4 + cx;
				int z = iz * 4 + cz;
				LandModel.Sample s = this.samples[z * 100 + x];
				if (s != null && s != VOID && s.floats() && !s.sure() && this.floats(x, z, s) != this.laidFloating[z * 100 + x]) {
					return true;
				}
			}
		}

		return false;
	}

	private void scale(ClientLevel world, boolean roofed, int roofY) {
		if (roofed) {
			this.base = roofY - 30;
			this.rise = 2;
		} else {
			int[] heights = new int[10000];
			int n = 0;

			for (int iz = 0; iz < 25; iz++) {
				for (int ix = 0; ix < 25; ix++) {
					LevelChunk chunk = world.getChunkSource().getChunk(this.originX + ix, this.originZ + iz, ChunkStatus.FULL, false);
					if (chunk != null) {
						for (int c = 0; c < 16; c++) {
							heights[n++] = chunk.getHeight(Types.WORLD_SURFACE, c % 4 * 4 + 2, c / 4 * 4 + 2);
						}
					}
				}
			}

			if (n != 0) {
				Arrays.sort(heights, 0, n);
				int low = heights[n / 20];
				int high = heights[Math.min(n - 1, n * 99 / 100)];
				this.rise = Math.max(2, Mth.ceil((float)(high - low) / 22.0F));
				this.base = low - 4 * this.rise;
			}
		}
	}

	private int cells(int y) {
		return Mth.clamp(Math.round((float)(y - this.base) / (float)this.rise), 1, 46);
	}

	private void lay(int chunk) {
		int ix = chunk % 25;
		int iz = chunk / 25;
		int highest = 1;

		for (int cz = 0; cz < 4; cz++) {
			for (int cx = 0; cx < 4; cx++) {
				int x = ix * 4 + cx;
				int z = iz * 4 + cz;
				LandModel.Sample s = this.samples[z * 100 + x];
				if (s != null) {
					if (s == VOID) {
						this.node.put(x, z, Column.none());
						this.top[z * 100 + x] = 0;
					} else {
						Column column = this.column(s, true);
						boolean floating = this.floats(x, z, s);
						this.laidFloating[z * 100 + x] = floating;
						if (floating) {
							column = column.floating(this.cells(s.underside()) - 1, s.beneath() == null ? null : this.column(s.beneath(), false));
						}

						this.node.put(x, z, column);
						int reach = column.ground() + column.water() + (column.trunk() != null ? 2 : 0);
						this.top[z * 100 + x] = (short)reach;
						highest = Math.max(highest, reach);
					}
				}
			}
		}

		this.chunkTop[chunk] = (short)highest;
	}

	private Column column(LandModel.Sample s, boolean trees) {
		int ground = this.cells(s.ground());
		int water = 0;
		if (s.water() != Integer.MIN_VALUE) {
			int surface = Math.max(2, this.cells(s.water()));
			ground = Mth.clamp(ground, Math.max(1, surface - 3), surface - 1);
			water = surface - ground;
		}

		boolean tree = trees && s.trunk() != null && s.canopy() != null && water == 0;
		return new Column(ground, s.top(), s.topTint(), s.under(), water, s.waterTint(), false, tree ? s.trunk() : null, tree ? s.canopy() : null, s.canopyTint());
	}

	private LandModel.Sample sample(ClientLevel world, LevelChunk chunk, int bx, int bz, boolean roofed, int roofY) {
		int minY = world.getMinBuildHeight();
		int y = roofed ? this.underRoof(chunk, bx, bz, minY, roofY) : chunk.getHeight(Types.WORLD_SURFACE, bx & 15, bz & 15);
		if (!roofed && y < minY) {
			return VOID;
		} else {
			LandModel.Sample top = this.surface(world, chunk, bx, bz, y, minY);
			if (roofed) {
				return top;
			} else {
				int thickest = world.dimensionType().hasSkyLight() ? 48 : Integer.MAX_VALUE;
				int b = top.ground() - 1;

				int thick;
				for (thick = 1; b >= minY && thick < thickest && !open(chunk.getBlockState(this.pos.set(bx, b, bz))); thick++) {
					b--;
				}

				if (b >= minY && thick < thickest) {
					int underside = b + 1;
					int below = b;

					int gap;
					for (gap = 0; below >= minY && open(chunk.getBlockState(this.pos.set(bx, below, bz))); gap++) {
						below--;
					}

					if (below < minY) {
						return top.floating(underside, null, true);
					} else if (gap < 2) {
						return top;
					} else {
						boolean lit = world.getBrightness(LightLayer.SKY, this.pos.set(bx, b, bz)) > 0;
						return top.floating(underside, this.surface(world, chunk, bx, bz, below, minY), lit);
					}
				} else {
					return top;
				}
			}
		}
	}

	private static int height(LandModel.Sample s) {
		return s.water() != Integer.MIN_VALUE ? Math.max(s.ground(), s.water()) : s.ground();
	}

	private boolean floats(int x, int z, LandModel.Sample s) {
		if (!s.floats()) {
			return false;
		} else if (!s.sure() && !this.openAround(x, z, s)) {
			int gap = s.underside() - 1 - (s.beneath() == null ? -1073741824 : height(s.beneath()));
			return gap >= 12 && this.alongSure(x, z, s);
		} else {
			return true;
		}
	}

	private boolean openAround(int x, int z, LandModel.Sample s) {
		int y = s.underside() - 1;
		int size = 13;
		boolean[] seen = new boolean[169];
		int[] queue = new int[169];
		int head = 0;
		int tail = 0;
		seen[84] = true;
		queue[tail++] = 84;

		while (head < tail) {
			int q = queue[head++];
			int qx = q % 13;
			int qz = q / 13;

			for (int d = 0; d < 4; d++) {
				int lx = qx + (d == 0 ? 1 : (d == 1 ? -1 : 0));
				int lz = qz + (d == 2 ? 1 : (d == 3 ? -1 : 0));
				if (lx >= 0 && lz >= 0 && lx < 13 && lz < 13 && !seen[lz * 13 + lx]) {
					seen[lz * 13 + lx] = true;
					int nx = x + lx - 6;
					int nz = z + lz - 6;
					if (nx >= 0 && nz >= 0 && nx < 100 && nz < 100) {
						LandModel.Sample n = this.samples[nz * 100 + nx];
						if (n != null) {
							if (n == VOID || !n.floats() && height(n) < y) {
								return true;
							}

							if (n.floats() && n.underside() > y && (n.beneath() == null || height(n.beneath()) < y)) {
								queue[tail++] = lz * 13 + lx;
							}
						}
					}
				}
			}
		}

		return false;
	}

	private boolean alongSure(int x, int z, LandModel.Sample s) {
		for (int dz = -6; dz <= 6; dz++) {
			for (int dx = -6; dx <= 6; dx++) {
				int nx = x + dx;
				int nz = z + dz;
				if (nx >= 0 && nz >= 0 && nx < 100 && nz < 100) {
					LandModel.Sample n = this.samples[nz * 100 + nx];
					if (n != null && n.sure() && n.floats() && n.underside() <= s.ground() && s.underside() <= n.ground()) {
						return true;
					}
				}
			}
		}

		return false;
	}

	private static boolean open(BlockState state) {
		return state.isAir() || state.getFluidState().isEmpty() && !state.blocksMotion();
	}

	private LandModel.Sample surface(ClientLevel world, LevelChunk chunk, int bx, int bz, int y, int minY) {
		BlockState state = chunk.getBlockState(this.pos.set(bx, y, bz));
		String canopy = null;
		String trunk = null;
		int canopyTint = -1;
		boolean snowed = false;

		for (int guard = 0; y > minY && guard < 72; guard++) {
			if (state.is(BlockTags.LEAVES)) {
				if (canopy == null) {
					LandModel.Face leaf = this.face(state, Direction.UP);
					canopy = leaf.texture();
					canopyTint = this.tint(world, state, leaf);
				}
			} else if (canopy != null && state.is(BlockTags.LOGS)) {
				if (trunk == null) {
					trunk = this.face(state, Direction.NORTH).texture();
				}
			} else if (state.is(Blocks.SNOW)) {
				snowed = true;
			} else if (!state.getFluidState().isEmpty() || state.blocksMotion()) {
				break;
			}

			state = chunk.getBlockState(this.pos.set(bx, --y, bz));
		}

		if (canopy != null && trunk == null) {
			String own = canopy.replace("_leaves", "_log");
			trunk = !own.equals(canopy) && this.exists(own) ? own : "oak_log";
		}

		int water = Integer.MIN_VALUE;
		int waterTint = -1;
		if (!state.getFluidState().isEmpty()) {
			water = y;
			waterTint = state.getFluidState().is(FluidTags.LAVA) ? -34274 : lively(BiomeColors.getAverageWaterColor(world, this.pos));

			for (int depth = 0; y > minY && depth < 14 && !state.getFluidState().isEmpty(); depth++) {
				state = chunk.getBlockState(this.pos.set(bx, --y, bz));
			}

			canopy = null;
		}

		LandModel.Face ground = state.getFluidState().isEmpty() ? this.face(state, Direction.UP) : BED;
		String topTexture = snowed ? "snow" : ground.texture();
		int topTint = snowed ? -1 : this.tint(world, state, ground);
		BlockState below = chunk.getBlockState(this.pos.set(bx, y - 1, bz));
		String under = !below.isAir() && below.getFluidState().isEmpty() ? this.face(below, Direction.UP).texture() : ground.texture();
		return new LandModel.Sample(y, water, topTexture, topTint, under, waterTint, trunk, canopy, canopyTint);
	}

	private int underRoof(LevelChunk chunk, int bx, int bz, int minY, int roofY) {
		int y = roofY + 2;

		for (int guard = 0; y > minY && guard < 24 && !chunk.getBlockState(this.pos.set(bx, y, bz)).isAir(); guard++) {
			y--;
		}

		while (y > minY && chunk.getBlockState(this.pos.set(bx, y, bz)).isAir()) {
			y--;
		}

		return y;
	}

	private static int lively(int rgb) {
		int r = rgb >> 16 & 0xFF;
		int g = rgb >> 8 & 0xFF;
		int b = rgb & 0xFF;
		return 0xFF000000 | r * 3 / 5 << 16 | g * 9 / 10 << 8 | Math.min(255, b + (255 - b) / 2 + 12);
	}

	private int tint(ClientLevel world, BlockState state, LandModel.Face face) {
		if (face.tintIndex() == -2) {
			int c = state.getMapColor(world, this.pos).col;
			return c == 0 ? -7697782 : 0xFF000000 | c;
		} else if (face.tintIndex() < 0) {
			return -1;
		} else {
			int c = Minecraft.getInstance().getBlockColors().getColor(state, world, this.pos, face.tintIndex());
			return c == -1 ? -1 : 0xFF000000 | c;
		}
	}

	private LandModel.Face face(BlockState state, Direction side) {
		Map<BlockState, LandModel.Face> cache = side == Direction.UP ? this.tops : this.sides;
		LandModel.Face f = cache.get(state);
		if (f == null) {
			f = this.look(state, side);
			cache.put(state, f);
		}

		return f;
	}

	private LandModel.Face look(BlockState state, Direction side) {
		try {
			BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
			BakedQuad quad = null;
			List<BakedQuad> culled = model.getQuads(state, side, this.random);
			if (!culled.isEmpty()) {
				quad = culled.get(0);
			} else {
				for (BakedQuad q : model.getQuads(state, null, this.random)) {
					if (q.getDirection() == side) {
						quad = q;
						break;
					}
				}
			}

			TextureAtlasSprite sprite = quad != null ? quad.getSprite() : model.getParticleIcon();
			ResourceLocation name = sprite.contents().name();
			int tint = quad != null && quad.isTinted() ? quad.getTintIndex() : -1;
			if ("minecraft".equals(name.getNamespace()) && name.getPath().startsWith("block/")) {
				return new LandModel.Face(name.getPath().substring("block/".length()), tint);
			}

			if (!name.equals(MissingTextureAtlasSprite.getLocation()) && sprite.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) {
				return new LandModel.Face(name.toString(), tint);
			}
		} catch (Exception var9) {
		}

		return PLAIN;
	}

	private boolean exists(String texture) {
		return this.textures.computeIfAbsent(texture, t -> !QuadMesh.sprite(t).contents().name().equals(MissingTextureAtlasSprite.getLocation()));
	}

	private void invent() {
		TeamData team = TeamsState.team();
		double homeX = team != null && team.home != null ? team.home.x : (double)((this.originX + 12) * 16 + 8);
		double homeZ = team != null && team.home != null ? team.home.z : (double)((this.originZ + 12) * 16 + 8);
		int sea = 62;
		this.base = 54;
		this.rise = 2;

		for (int z = 0; z < 100; z++) {
			for (int x = 0; x < 100; x++) {
				int bx = this.originX * 16 + x * 4 + 2;
				int bz = this.originZ * 16 + z * 4 + 2;
				float near = Math.max(0.0F, 1.0F - (float)Math.hypot((double)bx - homeX, (double)bz - homeZ) / 110.0F);
				float h = 58.0F
					+ 34.0F * (Build.smooth((float)bx, (float)bz, 170.0F, 71) - 0.42F)
					+ 16.0F * (Build.smooth((float)bx, (float)bz, 58.0F, 72) - 0.5F)
					+ 5.0F * (Build.smooth((float)bx, (float)bz, 19.0F, 73) - 0.5F)
					+ 9.0F * near * near;
				int ground = Math.round(h);
				float wood = Build.smooth((float)bx, (float)bz, 46.0F, 74);
				float spot = Build.chance(bx, bz, 75);
				LandModel.Sample s;
				if (ground < 62) {
					String bed = 62 - ground > 5 ? "gravel" : "sand";
					s = new LandModel.Sample(ground, 62, bed, -1, bed, lively(4159204), null, null, -1);
				} else if (ground <= 63) {
					s = new LandModel.Sample(ground, Integer.MIN_VALUE, "sand", -1, "sand", -1, null, null, -1);
				} else if (ground > 84) {
					s = new LandModel.Sample(ground, Integer.MIN_VALUE, ground > 90 ? "snow" : "stone", -1, "stone", -1, null, null, -1);
				} else {
					boolean high = ground > 74;
					boolean tree = wood > 0.54F && spot < (wood - 0.5F) * 3.2F && near < 0.82F;
					boolean birch = !high && Build.smooth((float)bx, (float)bz, 90.0F, 76) > 0.6F;
					s = new LandModel.Sample(
						ground,
						Integer.MIN_VALUE,
						"grass_block_top",
						high ? -7948413 : -7226023,
						"dirt",
						-1,
						tree ? (high ? "spruce_log" : (birch ? "birch_log" : "oak_log")) : null,
						tree ? (high ? "spruce_leaves" : (birch ? "birch_leaves" : "oak_leaves")) : null,
						high ? -10380959 : (birch ? -8345771 : -8934609)
					);
				}

				this.samples[z * 100 + x] = s;
			}
		}

		for (int i = 0; i < 625; i++) {
			this.lay(i);
		}
	}

	private static record Face(String texture, int tintIndex) {
	}

	private static record Sample(
		int ground,
		int water,
		String top,
		int topTint,
		String under,
		int waterTint,
		@Nullable String trunk,
		@Nullable String canopy,
		int canopyTint,
		int underside,
		@Nullable LandModel.Sample beneath,
		boolean sure
	) {
		Sample(int ground, int water, String top, int topTint, String under, int waterTint, @Nullable String trunk, @Nullable String canopy, int canopyTint) {
			this(ground, water, top, topTint, under, waterTint, trunk, canopy, canopyTint, Integer.MIN_VALUE, null, false);
		}

		LandModel.Sample floating(int underside, @Nullable LandModel.Sample beneath, boolean sure) {
			return new LandModel.Sample(
				this.ground, this.water, this.top, this.topTint, this.under, this.waterTint, this.trunk, this.canopy, this.canopyTint, underside, beneath, sure
			);
		}

		boolean floats() {
			return this.underside != Integer.MIN_VALUE;
		}
	}
}
