package com.onehumanawa.cfhcore.content.gui.quests.mixin.server;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.onehumanawa.cfhcore.content.gui.quests.server.QuestEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerPlayerGameMode.class})
public abstract class BreakBlockMixin {
	@Shadow
	protected ServerLevel level;
	@Shadow
	@Final
	protected ServerPlayer player;

	@Inject(
		method = {"destroyBlock"},
		at = {@At("HEAD")}
	)
	private void slateQuests$beforeBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir, @Share("slateQuestsStood") LocalRef<BlockState> stood) {
		stood.set(this.level.getBlockState(pos));
	}

	@Inject(
		method = {"destroyBlock"},
		at = {@At("RETURN")}
	)
	private void slateQuests$afterBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir, @Share("slateQuestsStood") LocalRef<BlockState> stood) {
		BlockState before = (BlockState)stood.get();
		if (before != null && cir.getReturnValueZ()) {
			if (!this.level.getBlockState(pos).is(before.getBlock())) {
				QuestEvents.broke(this.player, before);
			}
		}
	}
}
