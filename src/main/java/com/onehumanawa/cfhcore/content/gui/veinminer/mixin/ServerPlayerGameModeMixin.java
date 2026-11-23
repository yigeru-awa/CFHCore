package com.onehumanawa.cfhcore.content.gui.veinminer.mixin;

import com.onehumanawa.cfhcore.content.gui.veinminer.server.VeinExtras;
import com.onehumanawa.cfhcore.content.gui.veinminer.server.VeinServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
	@Unique
	@Nullable
	private BlockPos cfhVeinminer$minedPos;
	@Unique
	@Nullable
	private Direction cfhVeinminer$minedFace;
	@Unique
	@Nullable
	private VeinServer.Plan cfhVeinminer$plan;
	@Unique
	@Nullable
	private VeinExtras.Use cfhVeinminer$use;

	@Inject(
		method = {"handleBlockBreakAction"},
		at = {@At("HEAD")}
	)
	private void cfhVeinminer$action(BlockPos pos, Action action, Direction face, int maxBuildHeight, int sequence, CallbackInfo ci) {
		if (action == Action.START_DESTROY_BLOCK || action == Action.STOP_DESTROY_BLOCK) {
			this.cfhVeinminer$minedPos = pos.immutable();
			this.cfhVeinminer$minedFace = face;
		}
	}

	@Inject(
		method = {"destroyBlock"},
		at = {@At("HEAD")}
	)
	private void cfhVeinminer$before(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		Direction face = null;
		if (pos.equals(this.cfhVeinminer$minedPos)) {
			face = this.cfhVeinminer$minedFace;
			this.cfhVeinminer$minedPos = null;
		}

		this.cfhVeinminer$plan = VeinServer.beforeBreak(this.player, this.level, pos, face);
	}

	@Inject(
		method = {"destroyBlock"},
		at = {@At("RETURN")}
	)
	private void cfhVeinminer$after(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		VeinServer.Plan plan = this.cfhVeinminer$plan;
		this.cfhVeinminer$plan = null;
		if (plan != null) {
			VeinServer.afterBreak(this.player, plan, cir.getReturnValueZ());
		}
	}

	@Inject(
		method = {"useItemOn"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void cfhVeinminer$useBefore(
		ServerPlayer user, Level world, ItemStack stack, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir
	) {
		VeinExtras.Use use = VeinExtras.before(user, world, stack, hand, hit);
		if (use == VeinExtras.HANDLED) {
			cir.setReturnValue(InteractionResult.SUCCESS);
		} else {
			this.cfhVeinminer$use = use;
		}
	}

	@Inject(
		method = {"useItemOn"},
		at = {@At("RETURN")}
	)
	private void cfhVeinminer$useAfter(
		ServerPlayer user, Level world, ItemStack stack, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir
	) {
		VeinExtras.Use use = this.cfhVeinminer$use;
		this.cfhVeinminer$use = null;
		if (use != null) {
			VeinExtras.after(use, (InteractionResult)cir.getReturnValue());
		}
	}
}
