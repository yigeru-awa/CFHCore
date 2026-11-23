package com.onehumanawa.cfhcore.content.gui.quests.client.overhaul;

import com.onehumanawa.cfhcore.content.gui.core.stage.node.BlocksNode;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.GraphModel;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.NodeView;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import org.jetbrains.annotations.Nullable;

final class IslandBuilder {
	static final int UNIT = 2;
	static final int BASE = 12;
	static final int ABOVE = 11;
	private static final int MARGIN = 8;
	private static final int MARGIN_FAR = 14;
	private static final float LAND = 2.7F;
	private static final int FLAT = 1;
	private static final int SPOT = 2;
	private static final int ROUTE = 4;
	private static final int FOOT = 8;
	private static final int POND = 16;
	private static final int NO_TALL = 32;
	private static final int USED = 64;

	static long signature(Chapter chapter) {
		long h = 1125899906842597L;
		h = 31L * h + (long)chapter.id.hashCode();
		h = 31L * h + (long)chapter.backdrop.hashCode();
		h = 31L * h + (long)chapter.accent.hashCode();

		for (Quest q : chapter.quests) {
			h = 31L * h + (long)q.id.hashCode();
			h = 31L * h + (long)Float.floatToIntBits(q.x);
			h = 31L * h + (long)Float.floatToIntBits(q.y);
			h = 31L * h + (long)Float.floatToIntBits(q.size);
			h = 31L * h + (long)(q.isGateway() ? 1 : 0) + (long)(q.hideLines ? 2 : 0) + (long)(q.hideOutLines ? 4 : 0);
			h = 31L * h + (long)q.deps.hashCode();
		}

		return h;
	}

	static IslandBuilder.Layout plan(Chapter chapter, GraphModel graph, IslandTheme theme) {
		IslandBuilder.Layout l = new IslandBuilder.Layout();
		l.signature = signature(chapter);
		l.seed = chapter.id.hashCode() * 31 + 7;

		for (NodeView n : graph.all) {
			IslandBuilder.Site s = new IslandBuilder.Site(n.key, n.index, Math.round(n.x * 2.0F), Math.round(n.y * 2.0F), n.size, n.gateway());
			seat(l.sites, s);
			l.sites.add(s);
			l.byKey.put(s.key, s);
		}

		int minX = 0;
		int maxX = 0;
		int minZ = 0;
		int maxZ = 0;
		boolean first = true;

		for (IslandBuilder.Site s : l.sites) {
			int reach = s.gate ? 1 : 0;
			if (first || s.x - reach < minX) {
				minX = s.x - reach;
			}

			if (first || s.x + reach > maxX) {
				maxX = s.x + reach;
			}

			if (first || s.z < minZ) {
				minZ = s.z;
			}

			if (first || s.z > maxZ) {
				maxZ = s.z;
			}

			first = false;
		}

		l.ox = Math.floorDiv(minX + maxX, 2);
		l.oz = Math.floorDiv(minZ + maxZ, 2);

		for (IslandBuilder.Site s : l.sites) {
			s.x = s.x - l.ox;
			s.z = s.z - l.oz;
		}

		l.questMinX = minX - l.ox;
		l.questMaxX = maxX - l.ox;
		l.questMinZ = minZ - l.oz;
		l.questMaxZ = maxZ - l.oz;
		l.minX = l.questMinX - 8;
		l.minZ = l.questMinZ - 14;
		l.w = l.questMaxX - l.questMinX + 1 + 16;
		l.d = l.questMaxZ - l.questMinZ + 1 + 8 + 14;
		int cells = l.w * l.d;
		l.dist = new float[cells];
		l.inland = new float[cells];
		l.land = new boolean[cells];
		l.level = new byte[cells];
		l.flags = new byte[cells];
		l.thick = new int[cells];
		l.routeGround = new BlockState[cells];
		Arrays.fill(l.dist, 1000000.0F);
		if (l.sites.isEmpty()) {
			l.dist[l.at(0, 0)] = -1.5F;
		}

		for (int i = 0; i < l.sites.size(); i++) {
			IslandBuilder.Site a = l.sites.get(i);

			for (int j = i + 1; j < l.sites.size(); j++) {
				IslandBuilder.Site b = l.sites.get(j);
				float far = Mth.sqrt((float)((a.x - b.x) * (a.x - b.x) + (a.z - b.z) * (a.z - b.z)));
				if (far < a.room) {
					a.room = far;
				}

				if (far < b.room) {
					b.room = far;
				}
			}
		}

		for (IslandBuilder.Site s : l.sites) {
			byte[] var10000 = l.flags;
			int var10001 = l.at(s.x, s.z);
			var10000[var10001] = (byte)(var10000[var10001] | 2);
			l.dist[l.at(s.x, s.z)] = -(s.gate ? 2.7F : 1.6F + 0.75F * Math.min(2.2F, s.size));
			if (s.gate) {
				var10000 = l.flags;
				var10001 = l.at(s.x - 1, s.z);
				var10000[var10001] = (byte)(var10000[var10001] | 8);
				var10000 = l.flags;
				var10001 = l.at(s.x + 1, s.z);
				var10000[var10001] = (byte)(var10000[var10001] | 8);
			}

			int reachx = s.gate ? 2 : 1;

			for (int dz = -1; dz <= 1; dz++) {
				for (int dx = -reachx; dx <= reachx; dx++) {
					var10000 = l.flags;
					var10001 = l.at(s.x + dx, s.z + dz);
					var10000[var10001] = (byte)(var10000[var10001] | 1);
				}
			}
		}

		for (NodeView n : graph.all) {
			if (!n.quest.hideLines) {
				IslandBuilder.Site to = l.byKey.get(n.key);

				for (String dep : n.entry.deps()) {
					IslandBuilder.Site from = l.byKey.get(dep);
					if (from != null && to != null && from != to) {
						BookIndex.Entry before = QuestsState.entry(dep);
						if (before == null || !before.quest().hideOutLines) {
							List<Integer> path = new ArrayList<>();
							line(from.x, from.z, to.x, to.z, (xx, zx) -> {
								int ix = l.at(xx, zx);
								if ((l.flags[ix] & 10) == 0) {
									path.add(xx << 16 | zx & 65535);
									l.flags[ix] = (byte)(l.flags[ix] | 4);
									l.dist[ix] = Math.min(l.dist[ix], -0.7F);
								}
							});
							int[] out = new int[path.size()];

							for (int i = 0; i < out.length; i++) {
								out[i] = path.get(i);
							}

							l.routes.put(IslandBuilder.Layout.routeKey(from.index, to.index), new IslandBuilder.Route(from.index, to.index, out));
						}
					}
				}
			}
		}

		float[] shape = (float[])l.dist.clone();
		tie(l, shape);
		lobes(l, shape);
		chamfer(l.dist, l.w, l.d);
		chamfer(shape, l.w, l.d);
		float rough = theme.relief <= 0.0F ? 0.55F : 1.0F;

		for (int z = 0; z < l.d; z++) {
			for (int x = 0; x < l.w; x++) {
				int i = z * l.w + x;
				if (x != 0 && z != 0 && x != l.w - 1 && z != l.d - 1) {
					float nx = (1.5F * noise((float)(x + l.minX) / 6.5F, (float)(z + l.minZ) / 6.5F, l.seed) + 0.6F * (hash(x + l.minX, z + l.minZ, l.seed + 7) - 0.5F))
						* rough;
					l.land[i] = shape[i] + nx < 2.7F + theme.spread;
				}
			}
		}

		boolean[] outside = outside(l);

		for (int i = 0; i < cells; i++) {
			if (!l.land[i] && !outside[i]) {
				l.land[i] = true;
				l.flags[i] = (byte)(l.flags[i] | 16);
			}
		}

		for (int ix = 0; ix < cells; ix++) {
			l.inland[ix] = l.land[ix] ? 1000000.0F : 0.0F;
		}

		chamfer(l.inland, l.w, l.d);
		l.landMinX = Integer.MAX_VALUE;
		l.landMinZ = Integer.MAX_VALUE;
		l.landMaxX = Integer.MIN_VALUE;
		l.landMaxZ = Integer.MIN_VALUE;

		for (int z = 0; z < l.d; z++) {
			for (int xx = 0; xx < l.w; xx++) {
				int ix = z * l.w + xx;
				if (l.land[ix]) {
					l.landCells++;
					l.landMinX = Math.min(l.landMinX, xx + l.minX);
					l.landMaxX = Math.max(l.landMaxX, xx + l.minX);
					l.landMinZ = Math.min(l.landMinZ, z + l.minZ);
					l.landMaxZ = Math.max(l.landMaxZ, z + l.minZ);
				}
			}
		}

		if (l.landCells == 0) {
			l.landMinX = l.landMinZ = -1;
			l.landMaxX = l.landMaxZ = 1;
		}

		for (IslandBuilder.Site s : l.sites) {
			for (int dz = -1; dz <= 7; dz++) {
				for (int dx = -2; dx <= 2; dx++) {
					if (l.inside(s.x + dx, s.z + dz)) {
						byte[] var83 = l.flags;
						int var88 = l.at(s.x + dx, s.z + dz);
						var83[var88] = (byte)(var83[var88] | 32);
					}
				}
			}
		}

		for (IslandBuilder.Route r : l.routes.values()) {
			for (int c : r.cells) {
				int xxx = c >> 16;
				int z = (short)(c & 65535);

				for (int dz = 0; dz <= 3; dz++) {
					for (int dxx = -1; dxx <= 1; dxx++) {
						if (l.inside(xxx + dxx, z + dz)) {
							byte[] var84 = l.flags;
							int var89 = l.at(xxx + dxx, z + dz);
							var84[var89] = (byte)(var84[var89] | 32);
						}
					}
				}
			}
		}

		for (int z = 0; z < l.d; z++) {
			for (int xxx = 0; xxx < l.w; xxx++) {
				int ix = z * l.w + xxx;
				if (l.land[ix]) {
					if (theme.relief > 0.0F
						&& l.inland[ix] >= 2.0F
						&& l.dist[ix] >= theme.pondClear
						&& noise((float)(xxx + l.minX) / 5.0F, (float)(z + l.minZ) / 5.0F, l.seed + 3) < theme.ponds) {
						l.flags[ix] = (byte)(l.flags[ix] | 16);
					}

					if (l.dist[ix] < 1.4F) {
						l.flags[ix] = (byte)(l.flags[ix] & -17);
					}
				}
			}
		}

		boolean[] alone = new boolean[cells];

		for (int z = 1; z < l.d - 1; z++) {
			for (int xxxx = 1; xxxx < l.w - 1; xxxx++) {
				int ix = z * l.w + xxxx;
				alone[ix] = (l.flags[ix] & 16) != 0 && ((l.flags[ix - 1] | l.flags[ix + 1] | l.flags[ix - l.w] | l.flags[ix + l.w]) & 16) == 0;
			}
		}

		for (int ix = 0; ix < cells; ix++) {
			if (alone[ix]) {
				l.flags[ix] = (byte)(l.flags[ix] & -17);
			}
		}

		for (int z = 0; z < l.d; z++) {
			for (int xxxx = 0; xxxx < l.w; xxxx++) {
				int ixx = z * l.w + xxxx;
				if (l.land[ixx]) {
					int wx = xxxx + l.minX;
					int wz = z + l.minZ;
					float hill = noise((float)wx / 5.0F, (float)wz / 5.0F, l.seed + 3);
					int level = 0;
					if (l.dist[ixx] >= 2.1F && theme.relief > 0.0F && (l.flags[ixx] & 16) == 0) {
						if (hill > 0.3F / theme.relief && l.inland[ixx] >= 1.5F) {
							level = 1;
						}

						if (hill > 0.62F / theme.relief && l.inland[ixx] >= 2.5F && l.dist[ixx] >= 3.2F && (l.flags[ixx] & 32) == 0) {
							level = 2;
						}
					}

					l.level[ixx] = (byte)level;
					float t = 0.6F + (l.inland[ixx] - 1.0F) * 1.25F + hash(wx, wz, l.seed + 11) * 1.6F;
					if (l.inland[ixx] >= 2.0F && hash(wx, wz, l.seed + 12) < 0.1F) {
						t += 2.0F + 2.0F * hash(wx, wz, l.seed + 13);
					}

					l.thick[ixx] = Mth.clamp(Math.round(t), 1, 9) + level;
				}
			}
		}

		return l;
	}

	private static void seat(List<IslandBuilder.Site> placed, IslandBuilder.Site s) {
		if (!free(placed, s, s.x, s.z)) {
			for (int r = 1; r <= 12; r++) {
				int bestX = 0;
				int bestZ = 0;
				float best = Float.MAX_VALUE;

				for (int dz = -r; dz <= r; dz++) {
					for (int dx = -r; dx <= r; dx++) {
						if (Math.max(Math.abs(dx), Math.abs(dz)) == r && free(placed, s, s.x + dx, s.z + dz)) {
							float far = (float)(dx * dx + dz * dz) + 0.01F * (float)(dx + 2 * dz);
							if (far < best) {
								best = far;
								bestX = dx;
								bestZ = dz;
							}
						}
					}
				}

				if (best < Float.MAX_VALUE) {
					s.x += bestX;
					s.z += bestZ;
					return;
				}
			}
		}
	}

	private static boolean free(List<IslandBuilder.Site> placed, IslandBuilder.Site s, int x, int z) {
		for (IslandBuilder.Site o : placed) {
			int reach = 2 + (s.gate ? 1 : 0) + (o.gate ? 1 : 0);
			if (Math.abs(o.z - z) < 2 && Math.abs(o.x - x) < reach) {
				return false;
			}
		}

		return true;
	}

	private static void line(int x0, int z0, int x1, int z1, IslandBuilder.Cell cell) {
		int dx = Math.abs(x1 - x0);
		int dz = Math.abs(z1 - z0);
		int sx = Integer.signum(x1 - x0);
		int sz = Integer.signum(z1 - z0);
		int x = x0;
		int z = z0;
		int ix = 0;
		int iz = 0;

		while (ix < dx || iz < dz) {
			if (iz < dz && (ix >= dx || (long)(1 + 2 * ix) * (long)dz > (long)(1 + 2 * iz) * (long)dx)) {
				z += sz;
				iz++;
			} else {
				x += sx;
				ix++;
			}

			if (x == x1 && z == z1) {
				break;
			}

			cell.at(x, z);
		}
	}

	private static void lobes(IslandBuilder.Layout l, float[] shape) {
		int span = l.questMaxX - l.questMinX;
		int n = Mth.clamp(Math.round((float)span / 8.0F), 1, 6);

		for (int k = 0; k < n; k++) {
			float fx = (float)l.questMinX + ((float)k + 0.5F) * (float)(span + 1) / (float)n + (hash(k, 3, l.seed + 17) - 0.5F) * 3.0F;
			int far = Integer.MAX_VALUE;

			for (IslandBuilder.Site s : l.sites) {
				if (Math.abs((float)s.x - fx) <= 5.0F) {
					far = Math.min(far, s.z);
				}
			}

			if (far == Integer.MAX_VALUE) {
				far = l.questMinZ;
			}

			int x = Math.round(fx);
			int z = far - 4 - Math.round(hash(k, 5, l.seed + 18) * 2.0F);
			if (l.inside(x, z)) {
				shape[l.at(x, z)] = Math.min(shape[l.at(x, z)], -(1.2F + 1.3F * hash(k, 7, l.seed + 19)));
			}
		}
	}

	private static void tie(IslandBuilder.Layout l, float[] shape) {
		int n = l.sites.size();
		if (n >= 2) {
			boolean[] in = new boolean[n];
			float[] best = new float[n];
			int[] link = new int[n];
			Arrays.fill(best, Float.MAX_VALUE);
			best[0] = 0.0F;

			for (int round = 0; round < n; round++) {
				int next = -1;

				for (int i = 0; i < n; i++) {
					if (!in[i] && (next < 0 || best[i] < best[next])) {
						next = i;
					}
				}

				in[next] = true;
				IslandBuilder.Site a = l.sites.get(next);
				if (round > 0) {
					IslandBuilder.Site b = l.sites.get(link[next]);
					line(a.x, a.z, b.x, b.z, (x, z) -> {
						int ix = l.at(x, z);
						shape[ix] = Math.min(shape[ix], 0.3F);
					});
				}

				for (int ix = 0; ix < n; ix++) {
					if (!in[ix]) {
						IslandBuilder.Site b = l.sites.get(ix);
						float far = (float)((a.x - b.x) * (a.x - b.x) + (a.z - b.z) * (a.z - b.z));
						if (far < best[ix]) {
							best[ix] = far;
							link[ix] = next;
						}
					}
				}
			}
		}
	}

	private static void chamfer(float[] d, int w, int h) {
		float side = 1.0F;
		float across = 1.4142135F;

		for (int z = 0; z < h; z++) {
			for (int x = 0; x < w; x++) {
				int i = z * w + x;
				float v = d[i];
				if (x > 0) {
					v = Math.min(v, d[i - 1] + 1.0F);
				}

				if (z > 0) {
					v = Math.min(v, d[i - w] + 1.0F);
					if (x > 0) {
						v = Math.min(v, d[i - w - 1] + 1.4142135F);
					}

					if (x < w - 1) {
						v = Math.min(v, d[i - w + 1] + 1.4142135F);
					}
				}

				d[i] = v;
			}
		}

		for (int z = h - 1; z >= 0; z--) {
			for (int x = w - 1; x >= 0; x--) {
				int ix = z * w + x;
				float vx = d[ix];
				if (x < w - 1) {
					vx = Math.min(vx, d[ix + 1] + 1.0F);
				}

				if (z < h - 1) {
					vx = Math.min(vx, d[ix + w] + 1.0F);
					if (x < w - 1) {
						vx = Math.min(vx, d[ix + w + 1] + 1.4142135F);
					}

					if (x > 0) {
						vx = Math.min(vx, d[ix + w - 1] + 1.4142135F);
					}
				}

				d[ix] = vx;
			}
		}
	}

	private static boolean[] outside(IslandBuilder.Layout l) {
		boolean[] out = new boolean[l.w * l.d];
		int[] stack = new int[l.w * l.d];
		int top = 0;

		for (int x = 0; x < l.w; x++) {
			stack[top++] = x;
			stack[top++] = (l.d - 1) * l.w + x;
		}

		for (int z = 1; z < l.d - 1; z++) {
			stack[top++] = z * l.w;
			stack[top++] = z * l.w + l.w - 1;
		}

		for (int k = 0; k < top; k++) {
			out[stack[k]] = true;
		}

		while (top > 0) {
			int i = stack[--top];
			int x = i % l.w;
			int z = i / l.w;
			if (x > 0) {
				top = spread(l, out, stack, top, i - 1);
			}

			if (x < l.w - 1) {
				top = spread(l, out, stack, top, i + 1);
			}

			if (z > 0) {
				top = spread(l, out, stack, top, i - l.w);
			}

			if (z < l.d - 1) {
				top = spread(l, out, stack, top, i + l.w);
			}
		}

		return out;
	}

	private static int spread(IslandBuilder.Layout l, boolean[] out, int[] stack, int top, int i) {
		if (!out[i] && !l.land[i]) {
			out[i] = true;
			stack[top] = i;
			return top + 1;
		} else {
			return top;
		}
	}

	static float hash(int x, int z, int salt) {
		int h = x * 374761393 + z * 668265263 + salt * -1640531535;
		h = (h ^ h >>> 13) * 1274126177;
		h ^= h >>> 16;
		return (float)(h & 16777215) / 1.6777216E7F;
	}

	static float noise(float x, float z, int salt) {
		int x0 = Mth.floor(x);
		int z0 = Mth.floor(z);
		float tx = smooth(x - (float)x0);
		float tz = smooth(z - (float)z0);
		float a = Mth.lerp(tx, hash(x0, z0, salt), hash(x0 + 1, z0, salt));
		float b = Mth.lerp(tx, hash(x0, z0 + 1, salt), hash(x0 + 1, z0 + 1, salt));
		return Mth.lerp(tz, a, b) * 2.0F - 1.0F;
	}

	private static float smooth(float t) {
		return t * t * (3.0F - 2.0F * t);
	}

	static void raise(IslandBuilder.Layout l, IslandTheme theme, BlocksNode land) {
		IslandBuilder.Work w = new IslandBuilder.Work(l, theme, land);
		l.lamps.clear();

		for (int z = l.minZ; z < l.minZ + l.d; z++) {
			for (int x = l.minX; x < l.minX + l.w; x++) {
				if (w.land(x, z)) {
					column(w, x, z);
				}
			}
		}

		switch (theme.family) {
			case MEADOW:
				if (theme.top.is(Blocks.FARMLAND)) {
					field(w);
				} else if (!theme.chosenGround) {
					meadow(w);
				} else {
					court(w);
				}
				break;
			case COURT:
				court(w);
				break;
			case CAVE:
				cave(w);
				break;
			case NETHER:
				nether(w);
				break;
			case END:
				end(w);
				break;
			case OCEAN:
				ocean(w);
				break;
			case PARCHMENT:
				study(w);
				break;
			case WORKSHOP:
				workshop(w);
			case GRID:
				break;
			default:
				stars(w);
		}

		l.dist = null;
		l.inland = null;
	}

	private static void column(IslandBuilder.Work w, int x, int z) {
		IslandBuilder.Layout l = w.l;
		IslandTheme t = w.t;
		int i = l.at(x, z);
		int flags = l.flags[i];
		int level = l.level[i];
		boolean pond = (flags & 16) != 0;
		BlockState surface = surface(w, x, z);
		if ((flags & 10) != 0) {
			surface = t.plinth;
		}

		int from = level;
		if (pond) {
			if (t.fluid != null) {
				boolean deep = t.family == IslandTheme.Family.OCEAN && l.inland[i] >= 3.0F && w.h(x, z, 21) < 0.5F;
				w.set(x, 0, z, t.fluid);
				if (deep) {
					w.set(x, -1, z, t.fluid);
				}

				from = deep ? -2 : -1;
				surface = t.bed;
			} else {
				from = -1;
			}
		}

		int thick = Math.max(pond ? 2 : 1, l.thick[i] - (level - from));
		int soil = 2 + (w.h(x, z, 22) < 0.4F ? 1 : 0);

		for (int k = 0; k < thick; k++) {
			int y = from - k;
			if (y < -11) {
				break;
			}

			BlockState state;
			if (k == 0) {
				state = surface;
			} else if (k <= soil) {
				state = t.under;
			} else if (k >= thick - 1 - (w.h(x, z, 23) < 0.35F ? 1 : 0) && thick >= 5) {
				state = t.tip;
			} else {
				state = rock(w, x, y, z);
			}

			if (k == 0 && (flags & 4) != 0 && !pond) {
				l.routeGround[i] = surface.is(Blocks.FARMLAND) ? t.under : surface;
			} else {
				w.set(x, y, z, state);
			}
		}

		int foot = from - thick;
		if (foot >= -11) {
			if (t.family == IslandTheme.Family.CAVE && w.h(x, z, 24) < 0.22F) {
				w.set(
					x,
					foot,
					z,
					IslandTheme.with(
						IslandTheme.with(Blocks.POINTED_DRIPSTONE.defaultBlockState(), BlockStateProperties.VERTICAL_DIRECTION, Direction.DOWN),
						BlockStateProperties.DRIPSTONE_THICKNESS,
						DripstoneThickness.TIP
					)
				);
			} else if (t.family == IslandTheme.Family.MEADOW && !t.chosenGround && thick <= 3 && w.h(x, z, 24) < 0.3F) {
				w.set(x, foot, z, Blocks.HANGING_ROOTS.defaultBlockState());
			} else if (t.family == IslandTheme.Family.NETHER && thick >= 4 && w.h(x, z, 24) < 0.07F) {
				w.set(x, foot, z, Blocks.GLOWSTONE.defaultBlockState());
			}
		}
	}

	private static BlockState surface(IslandBuilder.Work w, int x, int z) {
		IslandTheme t = w.t;
		float patch = noise((float)x / 3.3F, (float)z / 3.3F, w.l.seed + 31);
		switch (t.family) {
			case CAVE:
				if (!t.chosenGround && w.h(x, z, 36) < 0.035F) {
					Block[] found = new Block[]{Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.COPPER_ORE, Blocks.COAL_ORE};
					return found[(int)(w.h(x, z, 37) * (float)found.length) % found.length].defaultBlockState();
				}
				break;
			case NETHER:
				if (!t.chosenGround) {
					if (patch > 0.36F) {
						return w.h(x >> 3, z >> 3, 33) < 0.25F ? Blocks.WARPED_NYLIUM.defaultBlockState() : Blocks.CRIMSON_NYLIUM.defaultBlockState();
					}

					if (patch < -0.55F) {
						return Blocks.SOUL_SOIL.defaultBlockState();
					}

					return t.top;
				}
			case END:
			case OCEAN:
			default:
				break;
			case PARCHMENT:
				if (!t.chosenGround) {
					return Math.floorMod(x, 5) != 0 && Math.floorMod(z, 5) != 0 ? t.top : Blocks.WHITE_TERRACOTTA.defaultBlockState();
				}
				break;
			case WORKSHOP:
				if (!t.chosenGround) {
					if (w.inland(x, z) < 1.6F) {
						return w.h(x, z, 32) < 0.25F ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
					}

					return Math.floorMod(x, 4) == 0 ? IslandTheme.with(Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), BlockStateProperties.AXIS, Axis.Z) : t.top;
				}
				break;
			case GRID:
				return Math.floorMod(x, 4) != 0 && Math.floorMod(z, 4) != 0 ? t.top : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
		}

		return t.patches.length > 0 && Math.abs(patch) > 0.42F && w.h(x, z, 34) < 0.7F
			? t.patches[Math.min(t.patches.length - 1, (int)(w.h(x >> 2, z >> 2, 35) * (float)t.patches.length))]
			: t.top;
	}

	private static BlockState rock(IslandBuilder.Work w, int x, int y, int z) {
		IslandTheme t = w.t;
		if (t.ores.length == 0) {
			return t.deep;
		} else {
			float ore = hash(x * 7 + y * 131, z * 3 - y * 17, w.l.seed + 41);
			return ore < 0.2F ? t.ores[(int)(ore / 0.2F * (float)t.ores.length) % t.ores.length] : t.deep;
		}
	}

	private static BlockState b(Block block) {
		return block.defaultBlockState();
	}

	private static BlockState leaves(Block block) {
		return IslandTheme.with(block.defaultBlockState(), BlockStateProperties.PERSISTENT, true);
	}

	private static void tree(IslandBuilder.Work w, int x, int z, Block log, Block leaf, int height) {
		int ground = w.level(x, z);

		for (int i = 1; i <= height; i++) {
			w.set(x, ground + i, z, b(log));
		}

		BlockState crown = leaves(leaf);

		for (int dy = height - 2; dy <= height + 1; dy++) {
			int reach = dy >= height ? 1 : 2;

			for (int dx = -reach; dx <= reach; dx++) {
				for (int dz = -reach; dz <= reach; dz++) {
					if (dx != 0 || dz != 0 || dy >= height) {
						boolean corner = Math.abs(dx) == reach && Math.abs(dz) == reach;
						if ((!corner || reach != 1 && !(w.h(x + dx * 3 + dy, z + dz * 5 - dy, 51) < 0.55F)) && w.get(x + dx, ground + dy + 1, z + dz).isAir()) {
							w.set(x + dx, ground + dy + 1, z + dz, crown);
						}
					}
				}
			}
		}

		w.use(x, z, 3);
	}

	private static void meadow(IslandBuilder.Work w) {
		Block[] flowers = new Block[]{Blocks.POPPY, Blocks.DANDELION, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.ALLIUM};
		int trees = Mth.clamp(w.l.landCells / 110, 1, 7);
		int boulders = Mth.clamp(w.l.landCells / 160, 1, 5);

		for (int i : w.shuffled(61)) {
			int x = i % w.l.w + w.l.minX;
			int z = i / w.l.w + w.l.minZ;
			if (trees > 0 && w.tall(x, z, 3.6F) && w.level(x, z) <= 1) {
				boolean birch = w.h(x, z, 62) < 0.3F;
				tree(w, x, z, birch ? Blocks.BIRCH_LOG : Blocks.OAK_LOG, birch ? Blocks.BIRCH_LEAVES : Blocks.OAK_LEAVES, 4 + (w.h(x, z, 63) < 0.5F ? 1 : 0));
				trees--;
			} else if (boulders > 0 && w.open(x, z) && w.dist(x, z) >= 1.8F && w.inland(x, z) >= 1.5F && w.h(x, z, 64) < 0.5F) {
				int g = w.level(x, z);
				w.set(x, g + 1, z, w.h(x, z, 65) < 0.5F ? b(Blocks.MOSSY_COBBLESTONE) : b(Blocks.COBBLESTONE));
				if (w.open(x + 1, z) && w.level(x + 1, z) == g) {
					w.set(x + 1, g, z, b(Blocks.MOSSY_COBBLESTONE));
				}

				w.use(x, z, 1);
				boulders--;
			}
		}

		for (int z = w.l.minZ; z < w.l.minZ + w.l.d; z++) {
			for (int x = w.l.minX; x < w.l.minX + w.l.w; x++) {
				if (w.land(x, z)) {
					if (w.is(x, z, 16)) {
						if (w.t.fluid != null && w.h(x, z, 66) < 0.2F) {
							w.set(x, 1, z, b(Blocks.LILY_PAD));
						}
					} else if (w.small(x, z) && w.get(x, w.level(x, z), z).is(Blocks.GRASS_BLOCK)) {
						float r = w.h(x, z, 67);
						int on = w.level(x, z) + 1;
						if (r < 0.22F) {
							w.set(x, on, z, b(Blocks.SHORT_GRASS));
						} else if (r < 0.26F) {
							w.set(x, on, z, b(Blocks.FERN));
						} else if (r < 0.34F) {
							w.set(x, on, z, b(flowers[(int)(w.h(x, z, 68) * (float)flowers.length) % flowers.length]));
						}
					}
				}
			}
		}
	}

	private static void field(IslandBuilder.Work w) {
		int bales = Mth.clamp(w.l.landCells / 140, 1, 4);
		int watchers = 1;

		for (int i : w.shuffled(71)) {
			int x = i % w.l.w + w.l.minX;
			int z = i / w.l.w + w.l.minZ;
			if (watchers > 0 && w.tall(x, z, 2.8F)) {
				int g = w.level(x, z);
				w.set(x, g + 1, z, b(Blocks.OAK_FENCE));
				w.set(x, g + 2, z, b(Blocks.HAY_BLOCK));
				w.set(x, g + 3, z, b(Blocks.CARVED_PUMPKIN));
				w.use(x, z, 1);
				watchers--;
			} else if (bales > 0 && w.open(x, z) && w.dist(x, z) >= 1.8F && w.h(x, z, 72) < 0.4F) {
				int g = w.level(x, z);
				w.set(x, g + 1, z, b(Blocks.HAY_BLOCK));
				if (!w.is(x, z, 32) && w.h(x, z, 73) < 0.5F) {
					w.set(x, g + 2, z, b(Blocks.HAY_BLOCK));
				} else if (w.open(x + 1, z)) {
					w.set(x + 1, w.level(x + 1, z) + 1, z, b(Blocks.COMPOSTER));
				}

				w.use(x, z, 1);
				bales--;
			}
		}

		BlockState[] crops = new BlockState[]{
			IslandTheme.with(b(Blocks.WHEAT), BlockStateProperties.AGE_7, 7),
			IslandTheme.with(b(Blocks.WHEAT), BlockStateProperties.AGE_7, 7),
			IslandTheme.with(b(Blocks.CARROTS), BlockStateProperties.AGE_7, 7),
			IslandTheme.with(b(Blocks.POTATOES), BlockStateProperties.AGE_7, 7),
			IslandTheme.with(b(Blocks.BEETROOTS), BlockStateProperties.AGE_3, 3)
		};

		for (int z = w.l.minZ; z < w.l.minZ + w.l.d; z++) {
			for (int x = w.l.minX; x < w.l.minX + w.l.w; x++) {
				if (w.small(x, z) && w.get(x, w.level(x, z), z).is(Blocks.FARMLAND) && w.h(x, z, 74) < 0.72F) {
					w.set(x, w.level(x, z) + 1, z, crops[(int)(w.h(x >> 2, z >> 2, 75) * (float)crops.length) % crops.length]);
				}
			}
		}
	}

	private static void court(IslandBuilder.Work w) {
		int posts = Mth.clamp(w.l.landCells / 150, 1, 5);
		int stubs = Mth.clamp(w.l.landCells / 120, 1, 6);
		boolean stone = !w.t.top.is(BlockTags.DIRT);

		for (int i : w.shuffled(81)) {
			int x = i % w.l.w + w.l.minX;
			int z = i / w.l.w + w.l.minZ;
			if (posts > 0 && w.tall(x, z, 2.4F)) {
				int g = w.level(x, z);
				w.set(x, g + 1, z, w.t.post);
				w.set(x, g + 2, z, w.t.post);
				w.set(x, g + 3, z, b(Blocks.LANTERN));
				w.lamp((float)x, (float)g + 2.6F, (float)z, 1.0F, 0.72F, 0.4F, 6.0F);
				w.use(x, z, 2);
				posts--;
			} else if (stubs > 0 && stone && w.open(x, z) && w.dist(x, z) >= 1.8F && w.inland(x, z) >= 1.5F && w.h(x, z, 82) < 0.4F) {
				int g = w.level(x, z);
				BlockState piece = w.t.patches.length > 0 ? w.t.patches[(int)(w.h(x, z, 83) * (float)w.t.patches.length) % w.t.patches.length] : w.t.top;
				w.set(x, g + 1, z, piece);
				if (w.open(x + 1, z) && w.level(x + 1, z) == g && w.h(x, z, 84) < 0.6F) {
					w.set(x + 1, g + 1, z, w.t.top);
				}

				w.use(x, z, 1);
				stubs--;
			}
		}

		for (int z = w.l.minZ; z < w.l.minZ + w.l.d; z++) {
			for (int x = w.l.minX; x < w.l.minX + w.l.w; x++) {
				if (w.open(x, z) && !(w.h(x, z, 85) > 0.025F) && w.get(x, w.level(x, z) + 1, z).isAir()) {
					w.set(x, w.level(x, z) + 1, z, b(Blocks.MOSS_CARPET));
				}
			}
		}
	}

	private static void cave(IslandBuilder.Work w) {
		int geodes = 1 + w.l.landCells / 260;
		int torches = Mth.clamp(w.l.landCells / 110, 1, 6);

		for (int i : w.shuffled(91)) {
			int x = i % w.l.w + w.l.minX;
			int z = i / w.l.w + w.l.minZ;
			if (geodes > 0 && w.open(x, z) && w.dist(x, z) >= 2.3F && w.inland(x, z) >= 2.0F) {
				int g = w.level(x, z);
				w.set(x, g, z, b(Blocks.BUDDING_AMETHYST));
				w.set(x, g + 1, z, b(Blocks.AMETHYST_CLUSTER));

				for (int[] o : new int[][]{{1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1, 1}}) {
					int ax = x + o[0];
					int az = z + o[1];
					if (w.open(ax, az) && w.level(ax, az) == g) {
						float r = w.h(ax, az, 92);
						w.set(ax, g, az, r < 0.6F ? b(Blocks.AMETHYST_BLOCK) : b(Blocks.CALCITE));
						if (r < 0.45F) {
							w.set(ax, g + 1, az, r < 0.2F ? b(Blocks.LARGE_AMETHYST_BUD) : b(Blocks.MEDIUM_AMETHYST_BUD));
						}
					}
				}

				w.lamp((float)x, (float)g + 1.2F, (float)z, 0.62F, 0.36F, 0.95F, 5.0F);
				w.use(x, z, 2);
				geodes--;
			} else if (torches > 0 && w.open(x, z) && w.dist(x, z) >= 1.6F && w.h(x, z, 93) < 0.35F) {
				int g = w.level(x, z);
				w.set(x, g + 1, z, b(Blocks.TORCH));
				w.lamp((float)x, (float)g + 1.4F, (float)z, 1.0F, 0.66F, 0.3F, 5.5F);
				w.use(x, z, 1);
				torches--;
			}
		}

		BlockState stalagmite = IslandTheme.with(b(Blocks.POINTED_DRIPSTONE), BlockStateProperties.DRIPSTONE_THICKNESS, DripstoneThickness.TIP);
		BlockState lichen = IslandTheme.with(b(Blocks.GLOW_LICHEN), MultifaceBlock.getFaceProperty(Direction.DOWN), true);

		for (int z = w.l.minZ; z < w.l.minZ + w.l.d; z++) {
			for (int x = w.l.minX; x < w.l.minX + w.l.w; x++) {
				if (w.small(x, z)) {
					float r = w.h(x, z, 94);
					int on = w.level(x, z) + 1;
					if (r < 0.035F) {
						w.set(x, on, z, stalagmite);
					} else if (r < 0.06F) {
						w.set(x, on, z, b(Blocks.BROWN_MUSHROOM));
					} else if (r < 0.1F) {
						w.set(x, on, z, lichen);
					}
				}
			}
		}
	}

	private static void nether(IslandBuilder.Work w) {
		int fungi = Mth.clamp(w.l.landCells / 120, 1, 6);
		int pillars = Mth.clamp(w.l.landCells / 170, 1, 4);

		for (int i : w.shuffled(101)) {
			int x = i % w.l.w + w.l.minX;
			int z = i / w.l.w + w.l.minZ;
			if (fungi > 0 && w.tall(x, z, 3.4F)) {
				int g = w.level(x, z);
				boolean warped = w.get(x, g, z).is(Blocks.WARPED_NYLIUM);
				int h = 3 + (w.h(x, z, 102) < 0.5F ? 1 : 0);

				for (int k = 1; k <= h; k++) {
					w.set(x, g + k, z, b(warped ? Blocks.WARPED_STEM : Blocks.CRIMSON_STEM));
				}

				BlockState cap = b(warped ? Blocks.WARPED_WART_BLOCK : Blocks.NETHER_WART_BLOCK);

				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						w.set(x + dx, g + h + 1, z + dz, cap);
						if ((dx != 0 || dz != 0) && w.h(x + dx, z + dz, 103) < 0.5F) {
							w.set(x + dx, g + h, z + dz, w.h(x + dx, z + dz, 104) < 0.35F ? b(Blocks.SHROOMLIGHT) : cap);
						}
					}
				}

				w.lamp((float)x, (float)(g + h), (float)z + 1.0F, 1.0F, 0.6F, 0.26F, 6.0F);
				w.use(x, z, 3);
				fungi--;
			} else if (pillars > 0 && w.tall(x, z, 2.4F) && w.h(x, z, 105) < 0.4F) {
				int g = w.level(x, z);
				int h = 2 + (int)(w.h(x, z, 106) * 2.5F);

				for (int k = 1; k <= h; k++) {
					w.set(x, g + k, z, b(Blocks.BASALT));
				}

				if (w.open(x + 1, z) && w.level(x + 1, z) == g) {
					w.set(x + 1, g + 1, z, b(Blocks.BASALT));
				}

				w.use(x, z, 1);
				pillars--;
			}
		}

		for (int z = w.l.minZ; z < w.l.minZ + w.l.d; z++) {
			for (int x = w.l.minX; x < w.l.minX + w.l.w; x++) {
				if (w.small(x, z)) {
					int g = w.level(x, z);
					BlockState ground = w.get(x, g, z);
					float r = w.h(x, z, 107);
					if (ground.is(Blocks.CRIMSON_NYLIUM)) {
						if (r < 0.2F) {
							w.set(x, g + 1, z, b(Blocks.CRIMSON_ROOTS));
						} else if (r < 0.27F) {
							w.set(x, g + 1, z, b(Blocks.CRIMSON_FUNGUS));
						}
					} else if (ground.is(Blocks.WARPED_NYLIUM)) {
						if (r < 0.18F) {
							w.set(x, g + 1, z, b(Blocks.WARPED_ROOTS));
						} else if (r < 0.3F) {
							w.set(x, g + 1, z, b(Blocks.NETHER_SPROUTS));
						} else if (r < 0.36F) {
							w.set(x, g + 1, z, b(Blocks.WARPED_FUNGUS));
						}
					} else if (ground.is(Blocks.SOUL_SOIL)) {
						if (r < 0.07F && w.dist(x, z) >= 1.6F) {
							w.set(x, g + 1, z, b(Blocks.SOUL_FIRE));
						}
					} else if (ground.is(Blocks.NETHERRACK) && r < 0.02F && w.dist(x, z) >= 1.8F) {
						w.set(x, g + 1, z, b(Blocks.FIRE));
					}
				}
			}
		}

		lights(w, 108, 1.0F, 0.42F, 0.12F, 6.5F);
	}

	private static void lights(IslandBuilder.Work w, int salt, float r, float g, float b, float reach) {
		for (int i : w.shuffled(salt)) {
			if (w.l.lamps.size() >= 5) {
				return;
			}

			int x = i % w.l.w + w.l.minX;
			int z = i / w.l.w + w.l.minZ;
			if (w.land(x, z) && w.is(x, z, 16) && !w.is(x, z, 64)) {
				w.lamp((float)x, 0.8F, (float)z, r, g, b, reach);
				w.use(x, z, 4);
			}
		}
	}

	private static void end(IslandBuilder.Work w) {
		int pillars = 1 + w.l.landCells / 150;
		int chorus = Mth.clamp(w.l.landCells / 60, 2, 12);
		BlockState stem = IslandTheme.with(IslandTheme.with(b(Blocks.CHORUS_PLANT), BlockStateProperties.UP, true), BlockStateProperties.DOWN, true);

		for (int i : w.shuffled(111)) {
			int x = i % w.l.w + w.l.minX;
			int z = i / w.l.w + w.l.minZ;
			if (pillars > 0 && w.tall(x, z, 3.0F)) {
				int g = w.level(x, z);
				int h = 3 + (int)(w.h(x, z, 112) * 3.0F);

				for (int k = 1; k <= h; k++) {
					w.set(x, g + k, z, b(k == h && w.h(x, z, 113) < 0.5F ? Blocks.CRYING_OBSIDIAN : Blocks.OBSIDIAN));
				}

				w.set(x, g + h + 1, z, b(Blocks.END_ROD));
				w.lamp((float)x, (float)(g + h) + 1.5F, (float)z, 0.9F, 0.82F, 1.0F, 7.0F);
				w.use(x, z, 2);
				pillars--;
			} else if (chorus > 0 && w.open(x, z) && w.dist(x, z) >= 2.0F && w.h(x, z, 114) < 0.5F) {
				int g = w.level(x, z);
				int h = w.is(x, z, 32) ? 1 : 2 + (int)(w.h(x, z, 115) * 2.5F);

				for (int k = 1; k <= h; k++) {
					w.set(x, g + k, z, stem);
				}

				w.set(x, g + h + 1, z, IslandTheme.with(b(Blocks.CHORUS_FLOWER), BlockStateProperties.AGE_5, w.h(x, z, 116) < 0.25F ? 5 : 0));
				w.use(x, z, 1);
				chorus--;
			}
		}
	}

	private static void ocean(IslandBuilder.Work w) {
		Block[] corals = new Block[]{Blocks.TUBE_CORAL, Blocks.BRAIN_CORAL, Blocks.BUBBLE_CORAL, Blocks.FIRE_CORAL, Blocks.HORN_CORAL};
		Block[] coralBlocks = new Block[]{
			Blocks.TUBE_CORAL_BLOCK, Blocks.BRAIN_CORAL_BLOCK, Blocks.BUBBLE_CORAL_BLOCK, Blocks.FIRE_CORAL_BLOCK, Blocks.HORN_CORAL_BLOCK
		};
		int ruins = Mth.clamp(w.l.landCells / 170, 1, 4);
		int lanterns = Mth.clamp(w.l.landCells / 110, 1, 5);

		for (int i : w.shuffled(121)) {
			int x = i % w.l.w + w.l.minX;
			int z = i / w.l.w + w.l.minZ;
			if (ruins > 0 && w.tall(x, z, 2.6F)) {
				int g = w.level(x, z);
				int h = 2 + (w.h(x, z, 122) < 0.5F ? 1 : 0);

				for (int k = 1; k <= h; k++) {
					w.set(x, g + k, z, b(Blocks.PRISMARINE_BRICKS));
				}

				w.set(x, g + h + 1, z, b(Blocks.SEA_LANTERN));
				w.lamp((float)x, (float)(g + h) + 1.5F, (float)z, 0.6F, 0.95F, 1.0F, 6.5F);
				w.use(x, z, 2);
				ruins--;
			} else if (lanterns > 0 && w.open(x, z) && w.dist(x, z) >= 1.6F && w.h(x, z, 123) < 0.3F) {
				w.set(x, w.level(x, z), z, b(Blocks.SEA_LANTERN));
				w.use(x, z, 2);
				lanterns--;
			}
		}

		for (int z = w.l.minZ; z < w.l.minZ + w.l.d; z++) {
			for (int x = w.l.minX; x < w.l.minX + w.l.w; x++) {
				if (w.land(x, z)) {
					float r = w.h(x, z, 124);
					if (w.is(x, z, 16)) {
						boolean deep = w.get(x, -1, z).is(Blocks.WATER);
						if (deep) {
							if (r < 0.5F) {
								w.set(x, -1, z, b(Blocks.KELP_PLANT));
								w.set(x, 0, z, b(Blocks.KELP));
							}
						} else if (r < 0.3F) {
							w.set(x, 0, z, b(Blocks.SEAGRASS));
						} else if (r < 0.42F) {
							w.set(x, 0, z, b(corals[(int)(w.h(x, z, 125) * (float)corals.length) % corals.length]));
						} else if (r < 0.5F) {
							w.set(
								x,
								0,
								z,
								IslandTheme.with(
									IslandTheme.with(b(Blocks.SEA_PICKLE), BlockStateProperties.PICKLES, 1 + (int)(w.h(x, z, 126) * 3.99F)), BlockStateProperties.WATERLOGGED, true
								)
							);
						}
					} else if (w.open(x, z)) {
						boolean shore = w.is(x + 1, z, 16) || w.is(x - 1, z, 16) || w.is(x, z + 1, 16) || w.is(x, z - 1, 16);
						int g = w.level(x, z);
						if (shore && r < 0.16F) {
							w.set(x, g, z, b(coralBlocks[(int)(w.h(x, z, 127) * (float)coralBlocks.length) % coralBlocks.length]));
						} else if (shore && r < 0.3F && w.get(x, g, z).is(Blocks.SAND)) {
							int h = w.is(x, z, 32) ? 1 : 2 + (w.h(x, z, 128) < 0.4F ? 1 : 0);

							for (int k = 1; k <= h; k++) {
								w.set(x, g + k, z, b(Blocks.SUGAR_CANE));
							}
						} else if (r < 0.02F && w.get(x, g, z).is(Blocks.SAND)) {
							w.set(x, g + 1, z, b(Blocks.DEAD_BUSH));
						}
					}
				}
			}
		}
	}

	private static void study(IslandBuilder.Work w) {
		int shelves = Mth.clamp(w.l.landCells / 55, 2, 14);
		int lecterns = Mth.clamp(w.l.landCells / 200, 1, 3);
		int rugs = Mth.clamp(w.l.landCells / 130, 1, 5);
		Block[] candles = new Block[]{Blocks.CANDLE, Blocks.WHITE_CANDLE, Blocks.RED_CANDLE, Blocks.YELLOW_CANDLE};
		Block[] wool = new Block[]{Blocks.RED_CARPET, Blocks.BROWN_CARPET, Blocks.PURPLE_CARPET};

		for (int i : w.shuffled(131)) {
			int x = i % w.l.w + w.l.minX;
			int z = i / w.l.w + w.l.minZ;
			if (w.open(x, z) && !(w.dist(x, z) < 1.7F)) {
				int g = w.level(x, z);
				if (rugs > 0 && w.h(x, z, 141) < 0.25F && w.open(x + 1, z) && w.open(x, z + 1) && w.open(x + 1, z + 1) && w.open(x + 2, z) && w.open(x + 2, z + 1)) {
					BlockState rug = b(wool[(int)(w.h(x, z, 142) * (float)wool.length) % wool.length]);

					for (int dx = 0; dx <= 2; dx++) {
						for (int dz = 0; dz <= 1; dz++) {
							w.set(x + dx, g + 1, z + dz, rug);
						}
					}

					w.use(x + 1, z, 2);
					rugs--;
				} else if (lecterns > 0 && w.h(x, z, 132) < 0.3F) {
					w.set(x, g + 1, z, IslandTheme.with(b(Blocks.LECTERN), BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
					w.use(x, z, 2);
					lecterns--;
				} else if (shelves > 0) {
					int h = !w.is(x, z, 32) && !(w.dist(x, z) < 2.6F) ? 1 + (int)(w.h(x, z, 133) * 2.99F) : 1;

					for (int k = 1; k <= h; k++) {
						w.set(x, g + k, z, b(k == h && w.h(x, z, 134) < 0.3F ? Blocks.CHISELED_BOOKSHELF : Blocks.BOOKSHELF));
					}

					if (w.h(x, z, 135) < 0.5F) {
						w.set(
							x,
							g + h + 1,
							z,
							IslandTheme.with(
								IslandTheme.with(
									b(candles[(int)(w.h(x, z, 136) * (float)candles.length) % candles.length]), BlockStateProperties.CANDLES, 1 + (int)(w.h(x, z, 137) * 2.99F)
								),
								BlockStateProperties.LIT,
								true
							)
						);
						w.lamp((float)x, (float)(g + h) + 1.4F, (float)z, 1.0F, 0.74F, 0.4F, 4.5F);
					}

					w.use(x, z, 1);
					shelves--;
				}
			}
		}

		for (int z = w.l.minZ; z < w.l.minZ + w.l.d; z++) {
			for (int x = w.l.minX; x < w.l.minX + w.l.w; x++) {
				if (w.small(x, z) && !(w.h(x, z, 138) > 0.035F)) {
					w.set(
						x,
						w.level(x, z) + 1,
						z,
						IslandTheme.with(
							IslandTheme.with(
								b(candles[(int)(w.h(x, z, 139) * (float)candles.length) % candles.length]), BlockStateProperties.CANDLES, 1 + (int)(w.h(x, z, 140) * 3.99F)
							),
							BlockStateProperties.LIT,
							true
						)
					);
				}
			}
		}
	}

	private static void workshop(IslandBuilder.Work w) {
		BlockState[] benches = new BlockState[]{
			b(Blocks.CRAFTING_TABLE),
			b(Blocks.SMITHING_TABLE),
			b(Blocks.FLETCHING_TABLE),
			b(Blocks.CARTOGRAPHY_TABLE),
			b(Blocks.STONECUTTER),
			b(Blocks.ANVIL),
			IslandTheme.with(b(Blocks.GRINDSTONE), BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR),
			b(Blocks.BARREL),
			b(Blocks.LOOM),
			b(Blocks.CAULDRON),
			IslandTheme.with(IslandTheme.with(b(Blocks.FURNACE), BlockStateProperties.LIT, true), BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH),
			IslandTheme.with(IslandTheme.with(b(Blocks.BLAST_FURNACE), BlockStateProperties.LIT, true), BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH),
			b(Blocks.COMPOSTER),
			b(Blocks.HAY_BLOCK)
		};
		int lanterns = Mth.clamp(w.l.landCells / 100, 1, 6);
		int work = Mth.clamp(w.l.landCells / 30, 3, 24);

		for (int i : w.shuffled(141)) {
			int x = i % w.l.w + w.l.minX;
			int z = i / w.l.w + w.l.minZ;
			if (lanterns > 0 && w.tall(x, z, 2.2F)) {
				int g = w.level(x, z);
				w.set(x, g + 1, z, b(Blocks.SPRUCE_FENCE));
				w.set(x, g + 2, z, b(Blocks.SPRUCE_FENCE));
				w.set(x, g + 3, z, b(Blocks.LANTERN));
				w.lamp((float)x, (float)g + 2.7F, (float)z, 1.0F, 0.7F, 0.38F, 7.0F);
				w.use(x, z, 2);
				lanterns--;
			} else if (work > 0 && w.open(x, z) && w.dist(x, z) >= 1.7F && w.inland(x, z) >= 1.6F) {
				w.set(x, w.level(x, z) + 1, z, benches[(int)(w.h(x, z, 142) * (float)benches.length) % benches.length]);
				w.use(x, z, 1);
				work--;
			}
		}
	}

	private static void stars(IslandBuilder.Work w) {
		int rods = Mth.clamp(w.l.landCells / 110, 1, 5);

		for (int i : w.shuffled(151)) {
			int x = i % w.l.w + w.l.minX;
			int z = i / w.l.w + w.l.minZ;
			if (rods > 0 && w.tall(x, z, 2.4F)) {
				int g = w.level(x, z);
				w.set(x, g + 1, z, w.t.post);
				w.set(x, g + 2, z, b(Blocks.END_ROD));
				w.lamp((float)x, (float)g + 2.4F, (float)z, 0.82F, 0.88F, 1.0F, 6.5F);
				w.use(x, z, 2);
				rods--;
			}
		}

		Block[] buds = new Block[]{Blocks.SMALL_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.LARGE_AMETHYST_BUD, Blocks.AMETHYST_CLUSTER};

		for (int z = w.l.minZ; z < w.l.minZ + w.l.d; z++) {
			for (int x = w.l.minX; x < w.l.minX + w.l.w; x++) {
				if (w.small(x, z) && !(w.h(x, z, 152) > 0.05F)) {
					int g = w.level(x, z);
					if (w.h(x, z, 153) < 0.5F) {
						w.set(x, g, z, b(Blocks.AMETHYST_BLOCK));
					}

					w.set(x, g + 1, z, b(buds[(int)(w.h(x, z, 154) * (float)buds.length) % buds.length]));
				}
			}
		}
	}

	private IslandBuilder() {
	}

	@FunctionalInterface
	private interface Cell {
		void at(int var1, int var2);
	}

	static record Lamp(float x, float y, float z, float r, float g, float b, float reach) {
	}

	static final class Layout {
		long signature;
		int seed;
		int minX;
		int minZ;
		int w;
		int d;
		int ox;
		int oz;
		final List<IslandBuilder.Site> sites = new ArrayList<>();
		final Map<String, IslandBuilder.Site> byKey = new HashMap<>();
		final Map<Long, IslandBuilder.Route> routes = new HashMap<>();
		int questMinX;
		int questMaxX;
		int questMinZ;
		int questMaxZ;
		int landMinX;
		int landMaxX;
		int landMinZ;
		int landMaxZ;
		int landCells;
		final List<IslandBuilder.Lamp> lamps = new ArrayList<>();
		float[] dist;
		float[] inland;
		boolean[] land;
		byte[] level;
		byte[] flags;
		int[] thick;
		BlockState[] routeGround;

		int at(int x, int z) {
			return (z - this.minZ) * this.w + (x - this.minX);
		}

		boolean inside(int x, int z) {
			return x >= this.minX && z >= this.minZ && x < this.minX + this.w && z < this.minZ + this.d;
		}

		static long routeKey(int from, int to) {
			return (long)from << 32 | (long)to & 4294967295L;
		}

		@Nullable
		IslandBuilder.Route route(int from, int to) {
			return this.routes.get(routeKey(from, to));
		}

		float cellX(float treeX) {
			return treeX * 2.0F - (float)this.ox;
		}

		float cellZ(float treeY) {
			return treeY * 2.0F - (float)this.oz;
		}

		float radius() {
			return 0.5F
				* Mth.sqrt(
					(float)(this.landMaxX - this.landMinX + 1) * (float)(this.landMaxX - this.landMinX + 1)
						+ (float)(this.landMaxZ - this.landMinZ + 1) * (float)(this.landMaxZ - this.landMinZ + 1)
				);
		}
	}

	static final class Route {
		final int from;
		final int to;
		final int[] cells;

		Route(int from, int to, int[] cells) {
			this.from = from;
			this.to = to;
			this.cells = cells;
		}
	}

	static final class Site {
		final String key;
		final int index;
		int x;
		int z;
		final float size;
		final boolean gate;
		float room = 9.0F;

		Site(String key, int index, int x, int z, float size, boolean gate) {
			this.key = key;
			this.index = index;
			this.x = x;
			this.z = z;
			this.size = size;
			this.gate = gate;
		}
	}

	private static final class Work {
		final IslandBuilder.Layout l;
		final IslandTheme t;
		final BlocksNode land;

		Work(IslandBuilder.Layout l, IslandTheme t, BlocksNode land) {
			this.l = l;
			this.t = t;
			this.land = land;
		}

		boolean inside(int x, int z) {
			return this.l.inside(x, z);
		}

		boolean land(int x, int z) {
			return this.l.inside(x, z) && this.l.land[this.l.at(x, z)];
		}

		int flags(int x, int z) {
			return this.l.inside(x, z) ? this.l.flags[this.l.at(x, z)] : 0;
		}

		boolean is(int x, int z, int flag) {
			return (this.flags(x, z) & flag) != 0;
		}

		int level(int x, int z) {
			return this.l.level[this.l.at(x, z)];
		}

		float dist(int x, int z) {
			return this.l.dist[this.l.at(x, z)];
		}

		float inland(int x, int z) {
			return this.l.inland[this.l.at(x, z)];
		}

		float h(int x, int z, int salt) {
			return IslandBuilder.hash(x, z, this.l.seed + salt);
		}

		void set(int x, int level, int z, BlockState state) {
			this.land.put(x - this.l.minX, 11 + level, z - this.l.minZ, state);
		}

		BlockState get(int x, int level, int z) {
			int y = 11 + level;
			return this.land.inside(x - this.l.minX, y, z - this.l.minZ) ? this.land.at(x - this.l.minX, y, z - this.l.minZ) : Blocks.AIR.defaultBlockState();
		}

		boolean open(int x, int z) {
			return this.land(x, z) && (this.flags(x, z) & 95) == 0;
		}

		boolean small(int x, int z) {
			return this.open(x, z) && this.dist(x, z) >= 0.0F;
		}

		boolean tall(int x, int z, float clear) {
			return this.open(x, z) && !this.is(x, z, 32) && this.dist(x, z) >= clear && this.inland(x, z) >= 2.0F;
		}

		void use(int x, int z, int reach) {
			for (int dz = -reach; dz <= reach; dz++) {
				for (int dx = -reach; dx <= reach; dx++) {
					if (this.inside(x + dx, z + dz)) {
						byte[] var10000 = this.l.flags;
						int var10001 = this.l.at(x + dx, z + dz);
						var10000[var10001] = (byte)(var10000[var10001] | 64);
					}
				}
			}
		}

		int[] shuffled(int salt) {
			Integer[] order = new Integer[this.l.w * this.l.d];

			for (int i = 0; i < order.length; i++) {
				order[i] = i;
			}

			int w = this.l.w;
			int minX = this.l.minX;
			int minZ = this.l.minZ;
			int seed = this.l.seed + salt;
			Arrays.sort(order, (a, b) -> Float.compare(IslandBuilder.hash(a % w + minX, a / w + minZ, seed), IslandBuilder.hash(b % w + minX, b / w + minZ, seed)));
			int[] out = new int[order.length];

			for (int i = 0; i < out.length; i++) {
				out[i] = order[i];
			}

			return out;
		}

		void lamp(float x, float y, float z, float r, float g, float b, float reach) {
			if (this.l.lamps.size() < 5) {
				this.l.lamps.add(new IslandBuilder.Lamp(x, y, z, r, g, b, reach));
			}
		}
	}
}
