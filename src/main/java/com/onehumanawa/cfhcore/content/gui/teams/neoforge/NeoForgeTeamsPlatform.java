package com.onehumanawa.cfhcore.content.gui.teams.neoforge;

import com.onehumanawa.cfhcore.content.gui.teams.server.platform.TeamsPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;

public final class NeoForgeTeamsPlatform implements TeamsPlatform {
	@Override
	public boolean isFakePlayer(Player player) {
		return player instanceof FakePlayer;
	}

	@Override
	public boolean holdsItems(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		try {
			if (level.getCapability(ItemHandler.BLOCK, pos, state, blockEntity, null) != null) {
				return true;
			}

			for (Direction side : Direction.values()) {
				if (level.getCapability(ItemHandler.BLOCK, pos, state, blockEntity, side) != null) {
					return true;
				}
			}
		} catch (RuntimeException var9) {
		}

		return false;
	}

	@Override
	public boolean breakAllowed(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
		return !((BreakEvent)NeoForge.EVENT_BUS.post(new BreakEvent(level, pos, state, player))).isCanceled();
	}
}
