package com.onehumanawa.cfhcore.content.gui.veinminer.vein;

import com.onehumanawa.cfhcore.content.gui.veinminer.VeinShape;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.IdentityHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class VeinFinder {
	private static final int[][] NEIGHBOURS = neighbours();

	private static int[][] neighbours() {
		int[][] out = new int[26][];
		int i = 0;

		for (Direction d : Direction.values()) {
			out[i++] = new int[]{d.getStepX(), d.getStepY(), d.getStepZ()};
		}

		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					int manhattan = Math.abs(x) + Math.abs(y) + Math.abs(z);
					if (manhattan >= 2) {
						out[i++] = new int[]{x, y, z};
					}
				}
			}
		}

		return out;
	}

	public static VeinFinder.Result find(
		BlockGetter level,
		BlockPos start,
		BlockState startState,
		VeinShape shape,
		VeinFinder.Context ctx,
		VeinRules rules,
		@Nullable VeinFinder.Harvest canHarvest,
		int limit,
		VeinFinder.Result out
	) {
		out.clear();
		if (!rules.shapeEnabled(shape)) {
			out.reason = VeinFinder.Reason.SHAPE_OFF;
			return out;
		} else {
			VeinFinder.Reason startReason = check(level, start, startState, rules, canHarvest);
			if (startReason != VeinFinder.Reason.OK) {
				out.reason = startReason;
				return out;
			} else {
				int max = Math.max(1, limit);
				out.positions.add(start.asLong());
				if (max <= 1) {
					out.truncated = true;
					return out;
				} else {
					switch (shape) {
						case SHAPELESS:
							shapeless(level, start, startState, ctx, rules, canHarvest, max, out);
							break;
						case TUNNEL_1X2:
						case TUNNEL_3X3:
						case SQUARE_3X3:
							tunnel(level, start, shape, ctx, rules, canHarvest, max, out);
							break;
						case STAIRCASE:
							staircase(level, start, ctx, rules, canHarvest, max, out);
							break;
						case COLUMN:
							column(level, start, ctx, rules, canHarvest, max, out);
					}

					return out;
				}
			}
		}
	}

	public static VeinFinder.Reason check(BlockGetter level, BlockPos pos, BlockState state, VeinRules rules, @Nullable VeinFinder.Harvest canHarvest) {
		if (!state.isAir() && !(state.getBlock() instanceof LiquidBlock)) {
			if (state.getBlock() instanceof GameMasterBlock || state.getDestroySpeed(level, pos) < 0.0F) {
				return VeinFinder.Reason.UNBREAKABLE;
			} else if (!rules.allowBlockEntities && state.hasBlockEntity()) {
				return VeinFinder.Reason.BLOCK_ENTITY;
			} else if (!rules.blockAllowed(state)) {
				return VeinFinder.Reason.DENIED;
			} else {
				return canHarvest != null && !canHarvest.test(state, pos) ? VeinFinder.Reason.NEEDS_TOOL : VeinFinder.Reason.OK;
			}
		} else {
			return VeinFinder.Reason.NO_BLOCK;
		}
	}

	private static void shapeless(
		BlockGetter level,
		BlockPos start,
		BlockState startState,
		VeinFinder.Context ctx,
		VeinRules rules,
		@Nullable VeinFinder.Harvest canHarvest,
		int max,
		VeinFinder.Result out
	) {
		boolean diagonal = ctx.diagonals() && rules.allowDiagonals;
		int reach = diagonal ? 26 : 6;
		IdentityHashMap<Block, Object[]> keys = new IdentityHashMap<>();
		LongOpenHashSet seen = new LongOpenHashSet();
		LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
		MutableBlockPos at = new MutableBlockPos();
		seen.add(start.asLong());
		queue.enqueue(start.asLong());

		while (!queue.isEmpty()) {
			long from = queue.dequeueLong();
			int fx = BlockPos.getX(from);
			int fy = BlockPos.getY(from);
			int fz = BlockPos.getZ(from);

			for (int i = 0; i < reach; i++) {
				int[] d = NEIGHBOURS[i];
				at.set(fx + d[0], fy + d[1], fz + d[2]);
				long key = at.asLong();
				if (seen.add(key)) {
					BlockState state = level.getBlockState(at);
					if (!state.isAir() && rules.sameGroup(startState, state, keys) && check(level, at, state, rules, canHarvest) == VeinFinder.Reason.OK) {
						if (out.positions.size() >= max) {
							out.truncated = true;
							return;
						}

						out.positions.add(key);
						queue.enqueue(key);
					}
				}
			}
		}
	}

	private static void tunnel(
		BlockGetter level,
		BlockPos start,
		VeinShape shape,
		VeinFinder.Context ctx,
		VeinRules rules,
		@Nullable VeinFinder.Harvest canHarvest,
		int max,
		VeinFinder.Result out
	) {
		Direction into = ctx.face().getOpposite();
		boolean flat = ctx.face().getAxis() == Axis.Y;
		Direction u = flat ? ctx.facing().getClockWise() : into.getClockWise();
		Direction v = flat ? ctx.facing() : Direction.UP;
		int[][] section;
		if (shape == VeinShape.TUNNEL_1X2) {
			int second = flat ? 1 : (start.getY() <= ctx.feetY() ? 1 : -1);
			section = new int[][]{{0, 0}, {0, second}};
		} else {
			section = new int[][]{{0, 0}, {0, 1}, {0, -1}, {-1, 0}, {1, 0}, {-1, 1}, {1, 1}, {-1, -1}, {1, -1}};
		}

		int depth = shape == VeinShape.SQUARE_3X3 ? 1 : max;
		MutableBlockPos at = new MutableBlockPos();

		for (int k = 0; k < depth; k++) {
			boolean any = k == 0;

			for (int[] s : section) {
				at.set(
					start.getX() + into.getStepX() * k + u.getStepX() * s[0] + v.getStepX() * s[1],
					start.getY() + into.getStepY() * k + u.getStepY() * s[0] + v.getStepY() * s[1],
					start.getZ() + into.getStepZ() * k + u.getStepZ() * s[0] + v.getStepZ() * s[1]
				);
				if ((k != 0 || s[0] != 0 || s[1] != 0) && patternBlock(level, at, rules, canHarvest)) {
					any = true;
					if (out.positions.size() >= max) {
						out.truncated = true;
						return;
					}

					out.positions.add(at.asLong());
				}
			}

			if (!any) {
				return;
			}
		}
	}

	private static void staircase(
		BlockGetter level, BlockPos start, VeinFinder.Context ctx, VeinRules rules, @Nullable VeinFinder.Harvest canHarvest, int max, VeinFinder.Result out
	) {
		Direction f = ctx.facing();
		int dy = ctx.lookingUp() ? 1 : -1;
		MutableBlockPos at = new MutableBlockPos();

		for (int k = 0; k < max; k++) {
			boolean any = k == 0;

			for (int h = 0; h < 3; h++) {
				if (k != 0 || h != 0) {
					at.set(start.getX() + f.getStepX() * k, start.getY() + dy * k + dy * h, start.getZ() + f.getStepZ() * k);
					if (patternBlock(level, at, rules, canHarvest)) {
						any = true;
						if (out.positions.size() >= max) {
							out.truncated = true;
							return;
						}

						out.positions.add(at.asLong());
					}
				}
			}

			if (!any) {
				return;
			}
		}
	}

	private static void column(
		BlockGetter level, BlockPos start, VeinFinder.Context ctx, VeinRules rules, @Nullable VeinFinder.Harvest canHarvest, int max, VeinFinder.Result out
	) {
		boolean up = ctx.face() == Direction.DOWN || ctx.face() != Direction.UP && ctx.lookingUp();
		int dy = up ? 1 : -1;
		MutableBlockPos at = new MutableBlockPos();

		for (int k = 1; k < max + 1; k++) {
			at.set(start.getX(), start.getY() + dy * k, start.getZ());
			if (!patternBlock(level, at, rules, canHarvest)) {
				return;
			}

			if (out.positions.size() >= max) {
				out.truncated = true;
				return;
			}

			out.positions.add(at.asLong());
		}
	}

	private static boolean patternBlock(BlockGetter level, BlockPos pos, VeinRules rules, @Nullable VeinFinder.Harvest canHarvest) {
		BlockState state = level.getBlockState(pos);
		return check(level, pos, state, rules, canHarvest) != VeinFinder.Reason.OK ? false : state.getDestroySpeed(level, pos) > 0.0F;
	}

	private VeinFinder() {
	}

	public static record Context(Direction face, Direction facing, boolean lookingUp, int feetY, boolean diagonals) {
	}

	@FunctionalInterface
	public interface Harvest {
		boolean test(BlockState var1, BlockPos var2);
	}

	public static enum Reason {
		OK,
		NO_BLOCK,
		UNBREAKABLE,
		DENIED,
		BLOCK_ENTITY,
		NEEDS_TOOL,
		SHAPE_OFF;
	}

	public static final class Result {
		public final LongArrayList positions = new LongArrayList();
		public boolean truncated;
		public VeinFinder.Reason reason = VeinFinder.Reason.OK;

		public void clear() {
			this.positions.clear();
			this.truncated = false;
			this.reason = VeinFinder.Reason.OK;
		}

		public int size() {
			return this.positions.size();
		}

		public boolean isEmpty() {
			return this.positions.isEmpty();
		}

		public long get(int i) {
			return this.positions.getLong(i);
		}
	}
}
