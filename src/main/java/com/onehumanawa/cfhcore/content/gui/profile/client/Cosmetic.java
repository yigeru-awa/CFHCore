package com.onehumanawa.cfhcore.content.gui.profile.client;

import com.onehumanawa.cfhcore.content.gui.profile.CFHGuiProfile;
import com.onehumanawa.cfhcore.content.gui.profile.Slot;
import java.util.function.Supplier;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class Cosmetic {
	private final String id;
	private final Slot slot;
	private final Cosmetic.Kind kind;
	private final ResourceLocation texture;
	private final Cosmetic.Anchor anchor;
	private final Cosmetic.Motion motion;
	private final boolean translucent;
	@Nullable
	private final Supplier<ModelPart> builder;
	@Nullable
	private ModelPart built;
	private final float midX;
	private final float midY;
	private final float midZ;
	private final float reach;

	private Cosmetic(
		String id,
		Slot slot,
		Cosmetic.Kind kind,
		Cosmetic.Anchor anchor,
		Cosmetic.Motion motion,
		boolean translucent,
		@Nullable Supplier<ModelPart> builder,
		float midX,
		float midY,
		float midZ,
		float reach
	) {
		this.id = id;
		this.slot = slot;
		this.kind = kind;
		this.texture = CFHGuiProfile.id("textures/cosmetic/" + id + ".png");
		this.anchor = anchor;
		this.motion = motion;
		this.translucent = translucent;
		this.builder = builder;
		this.midX = midX;
		this.midY = midY;
		this.midZ = midZ;
		this.reach = reach;
	}

	static Cosmetic layer(String id, Slot slot) {
		return new Cosmetic(id, slot, Cosmetic.Kind.LAYER, Cosmetic.Anchor.BODY, Cosmetic.Motion.STILL, false, null, 0.0F, 0.0F, 0.0F, 8.0F);
	}

	static Cosmetic model(
		String id,
		Slot slot,
		Cosmetic.Anchor anchor,
		Cosmetic.Motion motion,
		boolean translucent,
		Supplier<ModelPart> builder,
		float midX,
		float midY,
		float midZ,
		float reach
	) {
		return new Cosmetic(id, slot, Cosmetic.Kind.MODEL, anchor, motion, translucent, builder, midX, midY, midZ, reach);
	}

	public String id() {
		return this.id;
	}

	public Slot slot() {
		return this.slot;
	}

	public Cosmetic.Kind kind() {
		return this.kind;
	}

	public boolean isModel() {
		return this.kind == Cosmetic.Kind.MODEL;
	}

	public ResourceLocation texture() {
		return this.texture;
	}

	public Cosmetic.Anchor anchor() {
		return this.anchor;
	}

	public Cosmetic.Motion motion() {
		return this.motion;
	}

	public boolean translucent() {
		return this.translucent;
	}

	public Component name() {
		return Component.translatable("cfhgui_profile.cosmetic." + this.id);
	}

	public float midX() {
		return this.midX;
	}

	public float midY() {
		return this.midY;
	}

	public float midZ() {
		return this.midZ;
	}

	public float reach() {
		return this.reach;
	}

	public ModelPart part() {
		if (this.built == null) {
			if (this.builder == null) {
				throw new IllegalStateException(this.id + " is a layer, it has no model");
			}

			this.built = this.builder.get();
		}

		return this.built;
	}

	public static enum Anchor {
		HEAD,
		BODY,
		RIGHT_ARM,
		LEFT_ARM,
		RIGHT_LEG,
		LEFT_LEG,
		BESIDE;
	}

	public static enum Kind {
		LAYER,
		MODEL;
	}

	public static enum Motion {
		STILL,
		BOB,
		FLAP,
		BOUNCE,
		HOVER;
	}
}
