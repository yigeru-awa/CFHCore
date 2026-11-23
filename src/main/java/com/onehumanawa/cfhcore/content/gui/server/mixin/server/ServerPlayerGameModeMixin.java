package com.onehumanawa.cfhcore.content.gui.server.mixin.server;

import com.onehumanawa.cfhcore.content.gui.server.service.Hooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerPlayerGameMode.class})
public abstract class ServerPlayerGameModeMixin {
	@Shadow
	@Final
	protected ServerPlayer player;
	@Shadow
	protected ServerLevel level;

	@Inject(
		method = {"handleBlockBreakAction"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void cfhServer$heldBreak(BlockPos pos, Action action, Direction face, int maxBuildHeight, int sequence, CallbackInfo ci) {
		if (Hooks.held(this.player)) {
			this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, pos));
			ci.cancel();
		}
	}

	@Inject(
		method = {"useItemOn"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void cfhServer$heldUseOn(
		ServerPlayer who, Level world, ItemStack stack, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir
	) {
		if (Hooks.held(who)) {
			who.inventoryMenu.sendAllDataToRemote();
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}

	@Inject(
		method = {"useItem"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void cfhServer$heldUse(ServerPlayer who, Level world, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (Hooks.held(who)) {
			who.inventoryMenu.sendAllDataToRemote();
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}
