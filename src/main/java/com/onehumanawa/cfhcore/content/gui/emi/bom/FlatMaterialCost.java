package com.onehumanawa.cfhcore.content.gui.emi.bom;

import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;

public class FlatMaterialCost {
	public EmiIngredient ingredient;
	public long amount;

	public FlatMaterialCost(EmiIngredient ingredient, long amount) {
		this.ingredient = ingredient;
		this.amount = amount;
	}

	public long getEffectiveAmount() {
		return this.amount;
	}
}
