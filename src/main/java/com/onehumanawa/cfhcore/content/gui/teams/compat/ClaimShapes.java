package com.onehumanawa.cfhcore.content.gui.teams.compat;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.level.ChunkPos;

public final class ClaimShapes {
	private static final int E = 0;
	private static final int S = 1;
	private static final int W = 2;
	private static final int N = 3;
	private static final int[] TURNS = new int[]{3, 0, 1};

	public static List<ClaimShapes.Shape> of(MapClaims.Dim land) {
		return (List<ClaimShapes.Shape>)(land.isEmpty() ? new ArrayList<>() : of(land.chunks()));
	}

	static List<ClaimShapes.Shape> of(Long2ObjectMap<MapClaims.Team> chunks) {
		List<ClaimShapes.Shape> out = new ArrayList<>();
		LongOpenHashSet seen = new LongOpenHashSet();
		LongArrayList stack = new LongArrayList();
		LongArrayList cells = new LongArrayList();
		ObjectIterator var5 = Long2ObjectMaps.fastIterable(chunks).iterator();

		while (var5.hasNext()) {
			Entry<MapClaims.Team> e = (Entry<MapClaims.Team>)var5.next();
			long start = e.getLongKey();
			if (seen.add(start)) {
				MapClaims.Team team = (MapClaims.Team)e.getValue();
				cells.clear();
				stack.push(start);

				while (!stack.isEmpty()) {
					long c = stack.popLong();
					cells.add(c);
					int x = ChunkPos.getX(c);
					int z = ChunkPos.getZ(c);
					grow(chunks, team, seen, stack, x + 1, z);
					grow(chunks, team, seen, stack, x - 1, z);
					grow(chunks, team, seen, stack, x, z + 1);
					grow(chunks, team, seen, stack, x, z - 1);
				}

				out.add(shape(chunks, team, cells));
			}
		}

		return out;
	}

	private static void grow(Long2ObjectMap<MapClaims.Team> chunks, MapClaims.Team team, LongOpenHashSet seen, LongArrayList stack, int x, int z) {
		long n = ChunkPos.asLong(x, z);
		if (chunks.get(n) == team && seen.add(n)) {
			stack.push(n);
		}
	}

	private static ClaimShapes.Shape shape(Long2ObjectMap<MapClaims.Team> chunks, MapClaims.Team team, LongArrayList cells) {
		Long2IntOpenHashMap out = new Long2IntOpenHashMap();
		long first = Long.MAX_VALUE;
		int firstX = 0;
		int firstZ = 0;

		for (int i = 0; i < cells.size(); i++) {
			long c = cells.getLong(i);
			int x = ChunkPos.getX(c);
			int z = ChunkPos.getZ(c);
			long order = (long)z << 32 | (long)x & 4294967295L;
			if (order < first) {
				first = order;
				firstX = x;
				firstZ = z;
			}

			if (chunks.get(ChunkPos.asLong(x, z - 1)) != team) {
				leave(out, x, z, 0);
			}

			if (chunks.get(ChunkPos.asLong(x + 1, z)) != team) {
				leave(out, x + 1, z, 1);
			}

			if (chunks.get(ChunkPos.asLong(x, z + 1)) != team) {
				leave(out, x + 1, z + 1, 2);
			}

			if (chunks.get(ChunkPos.asLong(x - 1, z)) != team) {
				leave(out, x, z + 1, 3);
			}
		}

		Long2IntOpenHashMap used = new Long2IntOpenHashMap();
		List<int[]> rings = new ArrayList<>();
		ObjectIterator hull = out.long2IntEntrySet().iterator();

		while (hull.hasNext()) {
			it.unimi.dsi.fastutil.longs.Long2IntMap.Entry e = (it.unimi.dsi.fastutil.longs.Long2IntMap.Entry)hull.next();
			long corner = e.getLongKey();

			for (int remaining = e.getIntValue() & ~used.get(corner); remaining != 0; remaining = e.getIntValue() & ~used.get(corner)) {
				rings.add(ring(out, used, corner, Integer.numberOfTrailingZeros(remaining)));
			}
		}

		int[] hullx = null;
		long hullArea = Long.MIN_VALUE;
		List<int[]> holes = new ArrayList<>();

		for (int[] r : rings) {
			long area = area(r);
			if (area > 0L && area > hullArea) {
				if (hullx != null) {
					holes.add(hullx);
				}

				hullx = r;
				hullArea = area;
			} else {
				holes.add(r);
			}
		}

		if (hullx == null) {
			hullx = rings.isEmpty() ? new int[0] : rings.get(0);
		}

		return new ClaimShapes.Shape(team, cells.size(), hullx, holes, team.id + "@" + firstX + "," + firstZ);
	}

	private static void leave(Long2IntOpenHashMap out, int x, int z, int direction) {
		long corner = ChunkPos.asLong(x, z);
		out.put(corner, out.get(corner) | 1 << direction);
	}

	private static int[] ring(Long2IntOpenHashMap out, Long2IntOpenHashMap used, long start, int direction) {
		IntArrayList points = new IntArrayList();
		long corner = start;
		int d = direction;
		int previous = -1;

		while (true) {
			used.put(corner, used.get(corner) | 1 << d);
			if (d != previous) {
				points.add(ChunkPos.getX(corner));
				points.add(ChunkPos.getZ(corner));
			}

			previous = d;
			corner = step(corner, d);
			int edges = out.get(corner);
			int next = -1;

			for (int turn : TURNS) {
				int candidate = d + turn & 3;
				if ((edges & 1 << candidate) != 0) {
					next = candidate;
					break;
				}
			}

			if (next < 0 || (used.get(corner) & 1 << next) != 0) {
				if (d == direction && points.size() > 4) {
					points.removeElements(0, 2);
				}

				return points.toIntArray();
			}

			d = next;
		}
	}

	private static long step(long corner, int direction) {
		int x = ChunkPos.getX(corner);
		int z = ChunkPos.getZ(corner);

		return switch (direction) {
			case 0 -> ChunkPos.asLong(x + 1, z);
			case 1 -> ChunkPos.asLong(x, z + 1);
			case 2 -> ChunkPos.asLong(x - 1, z);
			default -> ChunkPos.asLong(x, z - 1);
		};
	}

	private static long area(int[] ring) {
		long sum = 0L;
		int i = 0;

		for (int n = ring.length; i < n; i += 2) {
			int j = (i + 2) % n;
			sum += (long)ring[i] * (long)ring[j + 1] - (long)ring[j] * (long)ring[i + 1];
		}

		return sum;
	}

	private ClaimShapes() {
	}

	public static final class Shape {
		public final MapClaims.Team team;
		public final int chunks;
		public final int[] hull;
		public final List<int[]> holes;
		public final String key;
		public final int stamp;
		public final int minX;
		public final int minZ;
		public final int maxX;
		public final int maxZ;

		Shape(MapClaims.Team team, int chunks, int[] hull, List<int[]> holes, String key) {
			this.team = team;
			this.chunks = chunks;
			this.hull = hull;
			this.holes = holes;
			this.key = key;
			int minX = Integer.MAX_VALUE;
			int minZ = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE;
			int maxZ = Integer.MIN_VALUE;

			for (int i = 0; i < hull.length; i += 2) {
				minX = Math.min(minX, hull[i]);
				maxX = Math.max(maxX, hull[i]);
				minZ = Math.min(minZ, hull[i + 1]);
				maxZ = Math.max(maxZ, hull[i + 1]);
			}

			this.minX = minX;
			this.minZ = minZ;
			this.maxX = maxX;
			this.maxZ = maxZ;
			int h = team.look * 31 + Arrays.hashCode(hull);

			for (int[] hole : holes) {
				h = h * 31 + Arrays.hashCode(hole);
			}

			this.stamp = h;
		}
	}
}
