package com.onehumanawa.cfhcore.content.gui.server.service;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.jetbrains.annotations.Nullable;

final class Vitals {
	private static final float DROP = 4.0F;
	private int food;
	private float saturation;
	private float exhaustion;
	@Nullable
	private final float[][] thirst;

	private Vitals(ServerPlayer p) {
		FoodData f = p.getFoodData();
		this.food = f.getFoodLevel();
		this.saturation = f.getSaturationLevel();
		this.exhaustion = Math.min(f.getExhaustionLevel(), 4.0F);
		this.thirst = Thirst.read(p);
	}

	static Vitals of(ServerPlayer p) {
		return new Vitals(p);
	}

	void hold(ServerPlayer p) {
		FoodData f = p.getFoodData();
		if (f.getFoodLevel() < this.food) {
			f.setFoodLevel(this.food);
		} else {
			this.food = f.getFoodLevel();
		}

		if (f.getSaturationLevel() < this.saturation) {
			f.setSaturation(this.saturation);
		} else {
			this.saturation = f.getSaturationLevel();
		}

		if (f.getExhaustionLevel() > this.exhaustion) {
			f.setExhaustion(this.exhaustion);
		} else {
			this.exhaustion = f.getExhaustionLevel();
		}

		if (this.thirst != null) {
			Thirst.hold(p, this.thirst);
		}
	}
}
