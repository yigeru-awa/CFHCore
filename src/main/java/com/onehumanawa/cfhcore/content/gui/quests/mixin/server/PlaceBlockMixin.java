package com.onehumanawa.cfhcore.content.gui.quests.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.onehumanawa.cfhcore.content.gui.quests.server.QuestEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({BlockItem.class})
public abstract class PlaceBlockMixin {
	@WrapOperation(
		method = {"place"},
		at = {@At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/item/BlockItem;placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z"
		)}
	)
	private boolean slateQuests$placed(BlockItem item, BlockPlaceContext context, BlockState state, Operation<Boolean> original) {
		boolean placed = (Boolean)original.call(new Object[]{item, context, state});
		if (placed && context.getPlayer() instanceof ServerPlayer player) {
			QuestEvents.placedByItem(player, state);
		}

		return placed;
	}
}
