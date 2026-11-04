package com.onehumanawa.cfhcore.foundation.ctm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Ported from LDLib2's {@code Connections}. The eight-way connection state of one block
 * face, plus the four corner submap indices into the CTM sheet that follow from it.
 */
public class Connections {

	/** Base tile indices used when a corner is fully connected on both its edges. */
	protected static final int[] submapOffsets = { 4, 5, 1, 0 };

	/** Which connections each corner of the face is built from. */
	protected static final Connection[][] submapMap = new Connection[][] {
			{ Connection.DOWN, Connection.LEFT, Connection.DOWN_LEFT },
			{ Connection.DOWN, Connection.RIGHT, Connection.DOWN_RIGHT },
			{ Connection.UP, Connection.RIGHT, Connection.UP_RIGHT },
			{ Connection.UP, Connection.LEFT, Connection.UP_LEFT }
	};

	/** The four quarter tiles of the base texture, used when nothing connects. */
	protected final int[] submapIndices = new int[] { 18, 19, 17, 16 };

	private byte connections = 0;

	private Connections(Connection... connections) {
		for (Connection connection : connections) {
			add(connection);
		}
	}

	public static Connections of(Connection... connections) {
		return new Connections(connections);
	}

	public boolean contains(Connection connection) {
		return (connections & (1 << connection.ordinal())) > 0;
	}

	public void add(Connection connection) {
		this.connections |= 1 << connection.ordinal();
	}

	public boolean isEmpty() {
		return connections == 0;
	}

	public int[] getSubmapIndices() {
		return submapIndices;
	}

	public static Connections checkConnections(BlockAndTintGetter level, BlockPos pos, @Nonnull BlockState state,
			@Nullable Direction side) {
		Connections connections = Connections.of();
		if (side != null) {
			pos = pos.immutable();
			for (Connection connection : Connection.values()) {
				BlockPos offset = connection.transform(pos, side);
				BlockState adjacent = level.getBlockState(offset);
				if (ICTMPredicate.getPredicate(state).isConnected(level, state, pos, adjacent, offset, side)) {
					connections.add(connection);
				}
			}
		}
		// Map connections to submap indices
		for (int i = 0; i < 4; i++) {
			connections.fillSubmaps(i);
		}
		return connections;
	}

	/**
	 * @return True if the cached connectionMap holds a connection in <b>all</b> the given
	 *         directions.
	 */
	public boolean connectedAnd(Connection... dirs) {
		for (Connection dir : dirs) {
			if (!contains(dir)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return True if the cached connectionMap holds a connection in <b>one of</b> the
	 *         given directions.
	 */
	public boolean connectedOr(Connection... dirs) {
		for (Connection dir : dirs) {
			if (contains(dir)) {
				return true;
			}
		}
		return false;
	}

	protected void fillSubmaps(int idx) {
		Connection[] dirs = submapMap[idx];
		if (connectedOr(dirs[0], dirs[1])) {
			if (connectedAnd(dirs)) {
				// All dirs connected: fully connected face, use the base offset value
				submapIndices[idx] = submapOffsets[idx];
			} else {
				// The array is ordered so the first dir requires an offset of 2, the
				// second dir an offset of 8, on top of the corner's initial offset.
				submapIndices[idx] = submapOffsets[idx] + (contains(dirs[0]) ? 2 : 0) + (contains(dirs[1]) ? 8 : 0);
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		return connections == ((Connections) o).connections;
	}

	@Override
	public int hashCode() {
		return connections;
	}
}
