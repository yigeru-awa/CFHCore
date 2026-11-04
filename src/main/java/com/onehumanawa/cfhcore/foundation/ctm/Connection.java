package com.onehumanawa.cfhcore.foundation.ctm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import javax.annotation.Nonnull;

/**
 * Ported from LDLib2's {@code Connection}. The eight neighbours of a quad face: four
 * edges and four corners, with the per-facing offset math needed to reach them from any
 * side of a block.
 */
public enum Connection {
	UP(Direction.UP),
	DOWN(Direction.DOWN),
	LEFT(Direction.EAST),
	RIGHT(Direction.WEST),
	UP_LEFT(Direction.UP, Direction.EAST),
	UP_RIGHT(Direction.UP, Direction.WEST),
	DOWN_LEFT(Direction.DOWN, Direction.EAST),
	DOWN_RIGHT(Direction.DOWN, Direction.WEST);

	public final Direction[] dirs;
	public final BlockPos[] offsets;

	Connection(Direction... dirs) {
		this.dirs = dirs;
		this.offsets = new BlockPos[6];
		for (Direction normal : Direction.values()) {
			BlockPos pos = BlockPos.ZERO;
			for (Direction dir : dirs) {
				if (normal.getAxis() == Direction.Axis.Y) {
					dir = dir.getOpposite();
				}
				pos = pos.relative(dir);
			}
			AABB rotated = rotate(new AABB(pos), normal);
			offsets[normal.ordinal()] = new BlockPos(
					Mth.floor((rotated.minX + rotated.maxX) / 2),
					Mth.floor((rotated.minY + rotated.maxY) / 2),
					Mth.floor((rotated.minZ + rotated.maxZ) / 2));
		}
	}

	@Nonnull
	public BlockPos getOffset(Direction normal) {
		return this.offsets[normal.ordinal()];
	}

	@Nonnull
	public BlockPos transform(BlockPos pos, Direction normal) {
		return pos.offset(getOffset(normal));
	}

	public Connection getOpposite() {
		return switch (this) {
			case UP -> DOWN;
			case DOWN -> UP;
			case LEFT -> RIGHT;
			case RIGHT -> LEFT;
			case UP_LEFT -> DOWN_RIGHT;
			case UP_RIGHT -> DOWN_LEFT;
			case DOWN_LEFT -> UP_RIGHT;
			case DOWN_RIGHT -> UP_LEFT;
		};
	}

	/** Rotates an AABB a quarter turn around the block center, ported from ShapeUtils. */
	private static AABB rotate(AABB aabb, Direction facing) {
		return switch (facing) {
			case SOUTH -> rotate(aabb, new Vector3f(0, 1, 0), 180);
			case EAST -> rotate(aabb, new Vector3f(0, 1, 0), -90);
			case WEST -> rotate(aabb, new Vector3f(0, 1, 0), 90);
			case UP -> rotate(aabb, new Vector3f(1, 0, 0), 90);
			case DOWN -> rotate(aabb, new Vector3f(1, 0, 0), -90);
			case NORTH -> aabb;
		};
	}

	private static AABB rotate(AABB aabb, Vector3f axis, double degree) {
		Vector3f min = new Vector3f((float) aabb.minX, (float) aabb.minY, (float) aabb.minZ).sub(0.5f, 0.5f, 0.5f);
		Vector3f max = new Vector3f((float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ).sub(0.5f, 0.5f, 0.5f);
		float radians = (float) Math.toRadians(degree);
		min.rotateAxis(radians, axis.x, axis.y, axis.z);
		max.rotateAxis(radians, axis.x, axis.y, axis.z);
		min.add(0.5f, 0.5f, 0.5f);
		max.add(0.5f, 0.5f, 0.5f);
		return new AABB(min.x, min.y, min.z, max.x, max.y, max.z);
	}
}
