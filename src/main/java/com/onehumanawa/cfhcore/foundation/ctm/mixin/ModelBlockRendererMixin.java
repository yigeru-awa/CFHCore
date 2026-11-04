package com.onehumanawa.cfhcore.foundation.ctm.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onehumanawa.cfhcore.foundation.ctm.CTMRenderState;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands the block being rendered over to {@link CTMRenderState} so CTM-wrapped baked
 * models can look up their neighbours while their quads are collected.
 */
@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {

	@Inject(method = "tesselateBlock", at = @At("HEAD"))
	private void cfhcore$ctm$pushRenderState(BlockAndTintGetter level, BakedModel model, BlockState state,
			BlockPos pos, PoseStack poseStack, VertexConsumer consumer, boolean checkSides, RandomSource random,
			long seed, int packedOverlay, CallbackInfo ci) {
		CTMRenderState.push(level, pos);
	}

	@Inject(method = "tesselateBlock", at = @At("RETURN"))
	private void cfhcore$ctm$popRenderState(BlockAndTintGetter level, BakedModel model, BlockState state,
			BlockPos pos, PoseStack poseStack, VertexConsumer consumer, boolean checkSides, RandomSource random,
			long seed, int packedOverlay, CallbackInfo ci) {
		CTMRenderState.pop();
	}
}
