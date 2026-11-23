package com.onehumanawa.cfhcore.content.gui.teams.client.ui.map;

import com.mojang.blaze3d.platform.NativeImage;
import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

public final class TerrainTiles {
	public static final int TILE_CHUNKS = 8;
	public static final int TILE_BLOCKS = 128;
	private static final long REPAINT_MS = 5000L;
	private static final long IDLE_MS = 45000L;
	private static final int MAX_TILES = 96;
	private static final int NEW_PER_FRAME = 14;
	private static final int OLD_PER_FRAME = 3;
	private static final long BUDGET_NANOS = 2500000L;
	private static final Long2ObjectOpenHashMap<TerrainTiles.Tile> TILES = new Long2ObjectOpenHashMap();
	@Nullable
	private static ClientLevel level;
	private static int serial;
	private static long lastLookMs;
	private static int roofedY;
	private static boolean smooth;
	private static final int[] HEIGHTS = new int[256];
	private static final int[] COLORS = new int[256];
	private static final int[] DEPTHS = new int[256];
	private static final MutableBlockPos POS = new MutableBlockPos();

	private static long key(int tx, int tz) {
		return ChunkPos.asLong(tx, tz);
	}

	public static void look(int minCx, int minCz, int maxCx, int maxCz, int midCx, int midCz) {
		Minecraft mc = Minecraft.getInstance();
		lastLookMs = System.currentTimeMillis();
		if (mc.level != level) {
			clear();
			level = mc.level;
		}

		ClientLevel lvl = level;
		if (lvl != null) {
			if (lvl.dimensionType().hasCeiling() && mc.player != null) {
				int y = mc.player.getBlockY();
				if (Math.abs(y - roofedY) > 6) {
					roofedY = y;
					forgetPictures();
				}
			}

			long now = lastLookMs;
			long deadline = System.nanoTime() + 2500000L;
			int fresh = 0;
			int old = 0;
			int reach = Math.max(Math.max(midCx - minCx, maxCx - midCx), Math.max(midCz - minCz, maxCz - midCz));

			for (int ring = 0; ring <= reach; ring++) {
				for (int dz = -ring; dz <= ring; dz++) {
					boolean edge = Math.abs(dz) == ring;

					for (int dx = -ring; dx <= ring; dx += !edge && ring != 0 ? 2 * ring : 1) {
						int cx = midCx + dx;
						int cz = midCz + dz;
						if (cx >= minCx && cx <= maxCx && cz >= minCz && cz <= maxCz) {
							TerrainTiles.Tile tile = (TerrainTiles.Tile)TILES.get(key(cx >> 3, cz >> 3));
							int slot = (cz & 7) * 8 + (cx & 7);
							long at = tile == null ? 0L : tile.painted[slot];
							LevelChunk chunk = lvl.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
							if (chunk == null) {
								if (at != 0L) {
									wipe(tile, cx, cz, slot);
								}
							} else {
								boolean isNew = at == 0L;
								if ((isNew || now - at >= 5000L) && (isNew ? fresh < 14 : old < 3) && System.nanoTime() <= deadline) {
									TerrainTiles.Tile target = tile != null ? tile : create(cx >> 3, cz >> 3);
									paint(lvl, chunk, target, cx, cz, slot, now);
									if (isNew) {
										fresh++;
									} else {
										old++;
									}
								}
							}
						}
					}
				}
			}

			ObjectIterator var29 = TILES.values().iterator();

			while (var29.hasNext()) {
				TerrainTiles.Tile t = (TerrainTiles.Tile)var29.next();
				if (t.dirty) {
					t.dirty = false;
					t.texture.upload();
				}
			}

			if (TILES.size() > 96) {
				trim(midCx >> 3, midCz >> 3);
			}
		}
	}

	public static void smooth(boolean on) {
		if (on != smooth) {
			smooth = on;
			ObjectIterator var1 = TILES.values().iterator();

			while (var1.hasNext()) {
				TerrainTiles.Tile t = (TerrainTiles.Tile)var1.next();
				t.texture.setFilter(on, false);
			}
		}
	}

	@Nullable
	public static ResourceLocation texture(int tx, int tz) {
		TerrainTiles.Tile t = (TerrainTiles.Tile)TILES.get(key(tx, tz));
		return t != null && t.pictures != 0 ? t.id : null;
	}

	public static boolean painted(int cx, int cz) {
		TerrainTiles.Tile t = (TerrainTiles.Tile)TILES.get(key(cx >> 3, cz >> 3));
		return t != null && t.painted[(cz & 7) * 8 + (cx & 7)] != 0L;
	}

	public static void tick() {
		if (!TILES.isEmpty()) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level != level || System.currentTimeMillis() - lastLookMs > 45000L) {
				clear();
			}
		}
	}

	public static void clear() {
		ObjectIterator var0 = TILES.values().iterator();

		while (var0.hasNext()) {
			TerrainTiles.Tile t = (TerrainTiles.Tile)var0.next();
			t.release();
		}

		TILES.clear();
		level = null;
	}

	private static TerrainTiles.Tile create(int tx, int tz) {
		TerrainTiles.Tile t = new TerrainTiles.Tile(tx, tz, serial++);
		if (smooth) {
			t.texture.setFilter(true, false);
		}

		TILES.put(key(tx, tz), t);
		return t;
	}

	private static void wipe(TerrainTiles.Tile tile, int cx, int cz, int slot) {
		tile.image.fillRect((cx & 7) * 16, (cz & 7) * 16, 16, 16, 0);
		tile.painted[slot] = 0L;
		tile.pictures--;
		tile.dirty = true;
	}

	private static void forgetPictures() {
		ObjectIterator var0 = TILES.values().iterator();

		while (var0.hasNext()) {
			TerrainTiles.Tile t = (TerrainTiles.Tile)var0.next();

			for (int i = 0; i < t.painted.length; i++) {
				if (t.painted[i] != 0L) {
					t.painted[i] = 1L;
				}
			}
		}
	}

	private static void trim(int midTx, int midTz) {
		while (TILES.size() > 96) {
			TerrainTiles.Tile far = null;
			long farthest = -1L;
			ObjectIterator<TerrainTiles.Tile> it = TILES.values().iterator();

			while (it.hasNext()) {
				TerrainTiles.Tile t = (TerrainTiles.Tile)it.next();
				long d = (long)(t.tx - midTx) * (long)(t.tx - midTx) + (long)(t.tz - midTz) * (long)(t.tz - midTz);
				if (d > farthest) {
					farthest = d;
					far = t;
				}
			}

			if (far == null) {
				return;
			}

			TILES.remove(key(far.tx, far.tz));
			far.release();
		}
	}

	private static void paint(ClientLevel lvl, LevelChunk chunk, TerrainTiles.Tile tile, int cx, int cz, int slot, long now) {
		boolean roofed = lvl.dimensionType().hasCeiling();
		int minY = lvl.getMinBuildHeight();
		int baseX = cx << 4;
		int baseZ = cz << 4;

		for (int lz = 0; lz < 16; lz++) {
			for (int lx = 0; lx < 16; lx++) {
				surface(lvl, chunk, baseX + lx, baseZ + lz, lx, lz, roofed, minY);
			}
		}

		LevelChunk north = lvl.getChunkSource().getChunk(cx, cz - 1, ChunkStatus.FULL, false);
		LevelChunk west = lvl.getChunkSource().getChunk(cx - 1, cz, ChunkStatus.FULL, false);
		int px = (cx & 7) * 16;
		int pz = (cz & 7) * 16;

		for (int lz = 0; lz < 16; lz++) {
			for (int lx = 0; lx < 16; lx++) {
				int i = lz * 16 + lx;
				int rgb = COLORS[i];
				if (rgb == 0) {
					tile.image.setPixelRGBA(px + lx, pz + lz, 0);
				} else {
					int h = HEIGHTS[i];
					float light = 1.0F;
					if (DEPTHS[i] > 0) {
						light = 1.0F - 0.055F * (float)Math.min(9, DEPTHS[i] - 1);
					} else {
						int hn = lz > 0 ? HEIGHTS[i - 16] : (north == null ? h : top(lvl, north, lx, 15, roofed, minY, h));
						int hw = lx > 0 ? HEIGHTS[i - 1] : (west == null ? h : top(lvl, west, 15, lz, roofed, minY, h));
						light = 1.0F + Math.max(-0.26F, Math.min(0.2F, (float)(h - hn) * 0.075F + (float)(h - hw) * 0.045F));
						light += Math.max(-0.06F, Math.min(0.08F, (float)(h - 64) * 0.0012F));
					}

					int r = Math.min(255, Math.round((float)(rgb >> 16 & 0xFF) * light));
					int g = Math.min(255, Math.round((float)(rgb >> 8 & 0xFF) * light));
					int b = Math.min(255, Math.round((float)(rgb & 0xFF) * light));
					tile.image.setPixelRGBA(px + lx, pz + lz, 0xFF000000 | b << 16 | g << 8 | r);
				}
			}
		}

		if (tile.painted[slot] == 0L) {
			tile.pictures++;
		}

		tile.painted[slot] = now;
		tile.dirty = true;
	}

	private static void surface(ClientLevel lvl, LevelChunk chunk, int wx, int wz, int lx, int lz, boolean roofed, int minY) {
		int i = lz * 16 + lx;
		int y = roofed ? underRoof(chunk, wx, wz, minY) : chunk.getHeight(Types.WORLD_SURFACE, lx, lz);
		BlockState state = null;

		MapColor color;
		for (color = MapColor.NONE; y >= minY; y--) {
			state = chunk.getBlockState(POS.set(wx, y, wz));
			color = state.getMapColor(lvl, POS);
			if (color != MapColor.NONE) {
				break;
			}
		}

		HEIGHTS[i] = y;
		DEPTHS[i] = 0;
		if (color == MapColor.NONE || state == null) {
			COLORS[i] = 0;
		} else if (!state.getFluidState().isEmpty() && color == MapColor.WATER) {
			int depth = 1;

			for (int d = y - 1; d >= minY && depth < 12 && !chunk.getBlockState(POS.set(wx, d, wz)).getFluidState().isEmpty(); depth++) {
				d--;
			}

			DEPTHS[i] = depth;
			POS.set(wx, y, wz);
			COLORS[i] = scale(lvl.getBlockTint(POS, BiomeColors.WATER_COLOR_RESOLVER), 0.92F);
		} else {
			if (color == MapColor.GRASS) {
				COLORS[i] = scale(lvl.getBlockTint(POS, BiomeColors.GRASS_COLOR_RESOLVER), 0.68F);
			} else if (color == MapColor.PLANT) {
				COLORS[i] = scale(lvl.getBlockTint(POS, BiomeColors.FOLIAGE_COLOR_RESOLVER), 0.56F);
			} else {
				COLORS[i] = scale(color.col, 0.86F);
			}
		}
	}

	private static int top(ClientLevel lvl, LevelChunk chunk, int lx, int lz, boolean roofed, int minY, int fallback) {
		if (roofed) {
			return fallback;
		} else {
			int y = chunk.getHeight(Types.WORLD_SURFACE, lx, lz);
			int wx = (chunk.getPos().x << 4) + lx;
			int wz = (chunk.getPos().z << 4) + lz;

			for (int guard = 0; y >= minY && guard < 8; guard++) {
				BlockState s = chunk.getBlockState(POS.set(wx, y, wz));
				if (s.getMapColor(lvl, POS) != MapColor.NONE) {
					return y;
				}

				y--;
			}

			return fallback;
		}
	}

	private static int underRoof(LevelChunk chunk, int wx, int wz, int minY) {
		int y = roofedY + 2;
		int guard = 0;

		while (y > minY && guard++ < 24 && !chunk.getBlockState(POS.set(wx, y, wz)).isAir()) {
			y--;
		}

		while (y > minY && chunk.getBlockState(POS.set(wx, y, wz)).isAir()) {
			y--;
		}

		return y;
	}

	private static int scale(int rgb, float f) {
		int r = Math.min(255, Math.round((float)(rgb >> 16 & 0xFF) * f));
		int g = Math.min(255, Math.round((float)(rgb >> 8 & 0xFF) * f));
		int b = Math.min(255, Math.round((float)(rgb & 0xFF) * f));
		int out = r << 16 | g << 8 | b;
		return out == 0 ? 65793 : out;
	}

	private TerrainTiles() {
	}

	private static final class Tile {
		final int tx;
		final int tz;
		final NativeImage image = new NativeImage(128, 128, true);
		final DynamicTexture texture;
		final ResourceLocation id;
		final long[] painted = new long[64];
		int pictures;
		boolean dirty;

		Tile(int tx, int tz, int serial) {
			this.tx = tx;
			this.tz = tz;
			this.texture = new DynamicTexture(this.image);
			this.id = CFHGuiTeams.id("dyn/terrain/" + serial);
			Minecraft.getInstance().getTextureManager().register(this.id, this.texture);
		}

		void release() {
			Minecraft.getInstance().getTextureManager().release(this.id);
		}
	}
}
