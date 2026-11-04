package com.onehumanawa.cfhcore.foundation.ctm;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from LDLib2's {@code ICTMPredicate}. Decides whether the texture of the block
 * at {@code pos} connects towards a given neighbour.
 */
public interface ICTMPredicate {

	/** Connects when the neighbour is the very same block state. */
	ICTMPredicate DEFAULT = (level, state, pos, sourceState, sourcePos, side) -> sourceState == state;

	/**
	 * Can the texture connect to the neighbouring block.
	 *
	 * @param level       world
	 * @param state       current blockstate
	 * @param pos         block position
	 * @param sourceState adjacent blockstate
	 * @param sourcePos   adjacent block position
	 * @param side        adjacent side
	 * @return is connected
	 */
	boolean isConnected(BlockAndTintGetter level, BlockState state, BlockPos pos, BlockState sourceState,
			BlockPos sourcePos, Direction side);

	@Nonnull
	static ICTMPredicate getPredicate(BlockState state) {
		if (state.getBlock() instanceof ICTMPredicate predicate) {
			return predicate;
		}
		return DEFAULT;
	}
}
