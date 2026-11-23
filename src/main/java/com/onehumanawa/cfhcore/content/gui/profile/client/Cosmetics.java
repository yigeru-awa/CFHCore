package com.onehumanawa.cfhcore.content.gui.profile.client;

import com.onehumanawa.cfhcore.content.gui.profile.Slot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import org.jetbrains.annotations.Nullable;

public final class Cosmetics {
	private static final Map<String, Cosmetic> ALL = new LinkedHashMap<>();

	private static void add(Cosmetic c) {
		ALL.put(c.id(), c);
	}

	@Nullable
	public static Cosmetic get(@Nullable String id) {
		return id == null ? null : ALL.get(id);
	}

	public static List<Cosmetic> all() {
		return new ArrayList<>(ALL.values());
	}

	public static List<Cosmetic> of(Slot slot) {
		List<Cosmetic> out = new ArrayList<>();

		for (Cosmetic c : ALL.values()) {
			if (c.slot() == slot) {
				out.add(c);
			}
		}

		return out;
	}

	private static ModelPart bake(MeshDefinition mesh, int width, int height) {
		return LayerDefinition.create(mesh, width, height).bakeRoot();
	}

	private static ModelPart topHat() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("brim", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -9.0F, -5.0F, 10.0F, 1.0F, 10.0F), PartPose.ZERO);
		root.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 11).addBox(-3.0F, -16.0F, -3.0F, 6.0F, 7.0F, 6.0F), PartPose.ZERO);
		ModelPart part = bake(mesh, 64, 32);
		part.zRot = 0.06F;
		return part;
	}

	private static ModelPart crown() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("band", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -10.0F, -4.5F, 9.0F, 2.0F, 9.0F), PartPose.ZERO);
		float[][] corners = new float[][]{{-4.5F, -4.5F}, {3.5F, -4.5F}, {-4.5F, 3.5F}, {3.5F, 3.5F}};

		for (int i = 0; i < corners.length; i++) {
			root.addOrReplaceChild("corner" + i, CubeListBuilder.create().texOffs(0, 11).addBox(corners[i][0], -12.0F, corners[i][1], 1.0F, 2.0F, 1.0F), PartPose.ZERO);
		}

		float[][] middles = new float[][]{{-0.5F, -4.5F}, {-0.5F, 3.5F}, {-4.5F, -0.5F}, {3.5F, -0.5F}};

		for (int i = 0; i < middles.length; i++) {
			root.addOrReplaceChild("middle" + i, CubeListBuilder.create().texOffs(8, 11).addBox(middles[i][0], -13.0F, middles[i][1], 1.0F, 3.0F, 1.0F), PartPose.ZERO);
		}

		return bake(mesh, 64, 32);
	}

	private static ModelPart strawHat() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("brim", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -9.0F, -7.0F, 14.0F, 1.0F, 14.0F), PartPose.ZERO);
		root.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 15).addBox(-4.0F, -12.0F, -4.0F, 8.0F, 3.0F, 8.0F), PartPose.ZERO);
		return bake(mesh, 64, 32);
	}

	private static ModelPart glasses() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("right", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -5.5F, -5.0F, 3.0F, 3.0F, 1.0F), PartPose.ZERO);
		root.addOrReplaceChild("left", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(1.0F, -5.5F, -5.0F, 3.0F, 3.0F, 1.0F), PartPose.ZERO);
		root.addOrReplaceChild("bridge", CubeListBuilder.create().texOffs(8, 0).addBox(-1.0F, -4.5F, -5.0F, 2.0F, 1.0F, 1.0F), PartPose.ZERO);
		root.addOrReplaceChild("temple_right", CubeListBuilder.create().texOffs(0, 4).addBox(-5.0F, -4.5F, -5.0F, 1.0F, 1.0F, 5.0F), PartPose.ZERO);
		root.addOrReplaceChild("temple_left", CubeListBuilder.create().texOffs(0, 4).mirror().addBox(4.0F, -4.5F, -5.0F, 1.0F, 1.0F, 5.0F), PartPose.ZERO);
		return bake(mesh, 32, 16);
	}

	private static ModelPart pack() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("sack", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, 1.0F, 2.0F, 6.0F, 8.0F, 3.0F), PartPose.ZERO);
		root.addOrReplaceChild("flap", CubeListBuilder.create().texOffs(18, 0).addBox(-3.0F, 1.0F, 5.0F, 6.0F, 3.0F, 1.0F), PartPose.ZERO);
		root.addOrReplaceChild("roll", CubeListBuilder.create().texOffs(0, 11).addBox(-2.0F, -1.0F, 1.5F, 4.0F, 2.0F, 4.0F), PartPose.ZERO);
		return bake(mesh, 64, 32);
	}

	private static ModelPart wings() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild(
			"left", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -1.0F, 0.0F, 9.0F, 11.0F, 1.0F), PartPose.offsetAndRotation(1.0F, 1.0F, 2.2F, 0.0F, -0.5F, 0.0F)
		);
		root.addOrReplaceChild(
			"right",
			CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-9.0F, -1.0F, 0.0F, 9.0F, 11.0F, 1.0F),
			PartPose.offsetAndRotation(-1.0F, 1.0F, 2.2F, 0.0F, 0.5F, 0.0F)
		);
		return bake(mesh, 64, 32);
	}

	private static ModelPart slime() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 10).addBox(-8.0F, -4.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.ZERO);
		root.addOrReplaceChild("skin", CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -5.2F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.ZERO);
		return bake(mesh, 32, 32);
	}

	private static ModelPart bee() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -1.5F, -2.0F, 3.0F, 3.0F, 4.0F), PartPose.ZERO);
		root.addOrReplaceChild(
			"left", CubeListBuilder.create().texOffs(0, 8).addBox(0.0F, 0.0F, -1.0F, 3.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.5F, -2.0F, 0.0F, 0.0F, 0.0F, -0.4F)
		);
		root.addOrReplaceChild(
			"right",
			CubeListBuilder.create().texOffs(0, 8).mirror().addBox(-3.0F, 0.0F, -1.0F, 3.0F, 1.0F, 2.0F),
			PartPose.offsetAndRotation(-0.5F, -2.0F, 0.0F, 0.0F, 0.0F, 0.4F)
		);
		return bake(mesh, 32, 32);
	}

	private Cosmetics() {
	}

	static {
		add(Cosmetic.model("hat_top", Slot.HAT, Cosmetic.Anchor.HEAD, Cosmetic.Motion.STILL, false, Cosmetics::topHat, 0.0F, -12.0F, 0.0F, 8.0F));
		add(Cosmetic.model("hat_crown", Slot.HAT, Cosmetic.Anchor.HEAD, Cosmetic.Motion.STILL, false, Cosmetics::crown, 0.0F, -10.0F, 0.0F, 7.0F));
		add(Cosmetic.model("hat_straw", Slot.HAT, Cosmetic.Anchor.HEAD, Cosmetic.Motion.STILL, false, Cosmetics::strawHat, 0.0F, -9.5F, 0.0F, 10.0F));
		add(Cosmetic.model("face_glasses", Slot.FACE, Cosmetic.Anchor.HEAD, Cosmetic.Motion.STILL, true, Cosmetics::glasses, 0.0F, -4.0F, -3.0F, 6.0F));
		add(Cosmetic.layer("face_bandana", Slot.FACE));
		add(Cosmetic.layer("shirt_hoodie", Slot.SHIRT));
		add(Cosmetic.layer("shirt_striped", Slot.SHIRT));
		add(Cosmetic.layer("pants_jeans", Slot.PANTS));
		add(Cosmetic.layer("pants_shorts", Slot.PANTS));
		add(Cosmetic.model("back_pack", Slot.BACK, Cosmetic.Anchor.BODY, Cosmetic.Motion.STILL, false, Cosmetics::pack, 0.0F, 4.5F, 3.5F, 7.0F));
		add(Cosmetic.model("back_wings", Slot.BACK, Cosmetic.Anchor.BODY, Cosmetic.Motion.FLAP, false, Cosmetics::wings, 0.0F, 5.0F, 3.0F, 12.0F));
		add(Cosmetic.layer("arm_iron_right", Slot.RIGHT_ARM));
		add(Cosmetic.layer("arm_bandage_right", Slot.RIGHT_ARM));
		add(Cosmetic.layer("arm_iron_left", Slot.LEFT_ARM));
		add(Cosmetic.layer("arm_bandage_left", Slot.LEFT_ARM));
		add(Cosmetic.layer("leg_wood_right", Slot.RIGHT_LEG));
		add(Cosmetic.layer("leg_stocking_right", Slot.RIGHT_LEG));
		add(Cosmetic.layer("leg_wood_left", Slot.LEFT_LEG));
		add(Cosmetic.layer("leg_stocking_left", Slot.LEFT_LEG));
		add(Cosmetic.model("pet_slime", Slot.PET, Cosmetic.Anchor.BODY, Cosmetic.Motion.BOUNCE, true, Cosmetics::slime, -6.5F, -3.0F, 0.0F, 4.5F));
		add(Cosmetic.model("pet_bee", Slot.PET, Cosmetic.Anchor.BESIDE, Cosmetic.Motion.HOVER, true, Cosmetics::bee, 0.0F, 0.0F, 0.0F, 4.0F));
	}
}
