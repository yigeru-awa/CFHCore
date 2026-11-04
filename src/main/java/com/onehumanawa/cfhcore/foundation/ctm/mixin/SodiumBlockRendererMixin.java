package com.onehumanawa.cfhcore.foundation.ctm.mixin;

import com.onehumanawa.cfhcore.foundation.ctm.CTMRenderState;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * The sodium counterpart of {@link ModelBlockRendererMixin}: sodium's terrain meshing
 * pipeline never touches {@code ModelBlockRenderer}, so the render state has to be
 * pushed here instead.
 * <p>
 * The hook sits on {@link AbstractBlockRenderContext#bufferDefaultModel}, the single
 * place sodium asks a vanilla-style baked model for its quads — every terrain block
 * goes through it. The context object itself carries everything needed at that point:
 * {@link #slice} implements {@code BlockAndTintGetter} under world coordinates and
 * {@link #pos} is the block being rendered. Blocks just outside the slice are not part
 * of it, so connections towards the slice border read as open there.
 * <p>
 * (Hooking {@code pipeline.BlockRenderer#renderModel} instead would avoid the
 * non-terrain callers, but {@code slice} and {@code pos} are declared on this class, so
 * shadowing from a mixin bound to the subclass is not possible.)
 */
@Mixin(AbstractBlockRenderContext.class)
public abstract class SodiumBlockRendererMixin {

	@Shadow
	@Nullable
	protected LevelSlice slice;

	@Shadow
	@Nullable
	protected BlockPos pos;

	@Inject(method = "bufferDefaultModel", at = @At("HEAD"))
	private void cfhcore$ctm$pushRenderState(BakedModel model, BlockState state, CallbackInfo ci) {
		if (slice != null && pos != null)
			CTMRenderState.push(slice, pos);
	}

	@Inject(method = "bufferDefaultModel", at = @At("RETURN"))
	private void cfhcore$ctm$popRenderState(BakedModel model, BlockState state, CallbackInfo ci) {
		if (slice != null && pos != null)
			CTMRenderState.pop();
	}
}
