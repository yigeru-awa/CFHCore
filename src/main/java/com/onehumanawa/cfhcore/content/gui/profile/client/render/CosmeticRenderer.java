package com.onehumanawa.cfhcore.content.gui.profile.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.onehumanawa.cfhcore.content.gui.profile.client.Cosmetic;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

public final class CosmeticRenderer {
	public static void draw(PoseStack pose, HumanoidModel<?> model, Cosmetic cosmetic, float age, int light, int overlay, int argb, CosmeticRenderer.Sink sink) {
		if (cosmetic.isModel()) {
			ModelPart part = cosmetic.part();
			pose.pushPose();
			switch (cosmetic.anchor()) {
				case HEAD:
					model.head.translateAndRotate(pose);
					break;
				case RIGHT_ARM:
					model.rightArm.translateAndRotate(pose);
					break;
				case LEFT_ARM:
					model.leftArm.translateAndRotate(pose);
					break;
				case RIGHT_LEG:
					model.rightLeg.translateAndRotate(pose);
					break;
				case LEFT_LEG:
					model.leftLeg.translateAndRotate(pose);
					break;
				case BESIDE:
					float turn = age * 0.045F;
					pose.translate((9.5F + Mth.sin(turn) * 1.6F) / 16.0F, (-7.0F + Mth.sin(age * 0.11F) * 1.3F + model.head.y) / 16.0F, Mth.cos(turn) * 2.4F / 16.0F);
					pose.mulPose(Axis.YP.rotation(-0.5F + Mth.sin(turn) * 0.5F));
					break;
				default:
					model.body.translateAndRotate(pose);
			}

			move(part, cosmetic, age);
			if (cosmetic.motion() == Cosmetic.Motion.BOUNCE) {
				float s = Mth.sin(age * 0.16F);
				float foot = cosmetic.midY() + cosmetic.reach() * 0.5F;
				pose.translate(cosmetic.midX() / 16.0F, foot / 16.0F, cosmetic.midZ() / 16.0F);
				pose.scale(1.0F - s * 0.07F, 1.0F + s * 0.1F, 1.0F - s * 0.07F);
				pose.translate(-cosmetic.midX() / 16.0F, -foot / 16.0F, -cosmetic.midZ() / 16.0F);
			}

			part.render(pose, sink.open(cosmetic), light, overlay, argb);
			pose.popPose();
		}
	}

	private static void move(ModelPart part, Cosmetic cosmetic, float age) {
		switch (cosmetic.motion()) {
			case FLAP:
				float beatx = Mth.sin(age * 0.12F) * 0.22F + Mth.sin(age * 0.031F) * 0.06F;
				if (part.hasChild("left")) {
					part.getChild("left").yRot = -0.5F - beatx;
				}

				if (part.hasChild("right")) {
					part.getChild("right").yRot = 0.5F + beatx;
				}
				break;
			case HOVER:
				float beat = Mth.sin(age * 2.4F) * 0.5F;
				if (part.hasChild("left")) {
					part.getChild("left").zRot = -0.4F + beat;
				}

				if (part.hasChild("right")) {
					part.getChild("right").zRot = 0.4F - beat;
				}
		}
	}

	public static void drawAlone(PoseStack pose, Cosmetic cosmetic, float age, int light, int argb, CosmeticRenderer.Sink sink) {
		if (cosmetic.isModel()) {
			ModelPart part = cosmetic.part();
			move(part, cosmetic, age);
			pose.pushPose();
			float s = 8.0F / Math.max(1.0F, cosmetic.reach());
			pose.scale(s, -s, -s);
			pose.translate(-cosmetic.midX() / 16.0F, -cosmetic.midY() / 16.0F, -cosmetic.midZ() / 16.0F);
			part.render(pose, sink.open(cosmetic), light, OverlayTexture.NO_OVERLAY, argb);
			pose.popPose();
		}
	}

	private CosmeticRenderer() {
	}

	@FunctionalInterface
	public interface Sink {
		VertexConsumer open(Cosmetic var1);
	}
}
