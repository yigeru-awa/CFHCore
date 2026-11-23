package com.onehumanawa.cfhcore.content.gui.profile.mixin;

import com.onehumanawa.cfhcore.content.gui.profile.client.render.CosmeticsLayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerRenderer.class})
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	private PlayerRendererMixin(Context context, PlayerModel<AbstractClientPlayer> model, float shadow) {
		super(context, model, shadow);
	}

	@Inject(
		method = {"<init>"},
		at = {@At("TAIL")}
	)
	private void slate$cosmetics(Context context, boolean slim, CallbackInfo ci) {
		this.addLayer(new CosmeticsLayer(this));
	}
}
