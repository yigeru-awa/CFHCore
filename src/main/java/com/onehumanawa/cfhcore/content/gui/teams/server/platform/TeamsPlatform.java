package com.onehumanawa.cfhcore.content.gui.teams.server.platform;

import com.onehumanawa.cfhcore.content.gui.core.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface TeamsPlatform {
	static TeamsPlatform get() {
		return (TeamsPlatform)Services.load(TeamsPlatform.class);
	}

	boolean isFakePlayer(Player var1);

	boolean holdsItems(Level var1, BlockPos var2, BlockState var3, BlockEntity var4);

	boolean breakAllowed(ServerPlayer var1, ServerLevel var2, BlockPos var3, BlockState var4);
}
