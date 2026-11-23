package com.onehumanawa.cfhcore.content.gui.profile.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onehumanawa.cfhcore.content.gui.profile.client.AppliedLooks;
import com.onehumanawa.cfhcore.content.gui.profile.client.Cosmetic;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.jetbrains.annotations.Nullable;

public final class CosmeticsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	@Nullable
	private static Class<?> lastBuffers;
	private static boolean lastFromTheEyes;

	public CosmeticsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
		super(parent);
	}

	public void render(
		PoseStack pose,
		MultiBufferSource buffers,
		int light,
		AbstractClientPlayer player,
		float limbSwing,
		float limbSwingAmount,
		float partialTick,
		float age,
		float headYaw,
		float headPitch
	) {
		AppliedLooks.Applied look = AppliedLooks.of(player.getUUID());
		if (look != null && !look.models().isEmpty() && !player.isInvisible()) {
			boolean fromTheEyes = fromTheEyes(buffers);
			int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);

			for (Cosmetic c : look.models()) {
				if (!fromTheEyes || c.anchor() != Cosmetic.Anchor.HEAD) {
					CosmeticRenderer.draw(
						pose,
						(HumanoidModel<?>)this.getParentModel(),
						c,
						age,
						light,
						overlay,
						-1,
						cosmetic -> buffers.getBuffer(
								cosmetic.translucent() ? RenderType.entityTranslucent(cosmetic.texture()) : RenderType.entityCutoutNoCull(cosmetic.texture())
							)
					);
				}
			}
		}
	}

	private static boolean fromTheEyes(MultiBufferSource buffers) {
		Class<?> type = buffers.getClass();
		if (type != lastBuffers) {
			lastBuffers = type;
			lastFromTheEyes = type.getName().startsWith("com.xtracr.realcamera.");
		}

		return lastFromTheEyes;
	}
}
