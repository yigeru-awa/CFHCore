package com.onehumanawa.cfhcore.content.gui.emi.bom;

import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;

public class FractionalMaterialCost {
	public EmiIngredient ingredient;
	public float amount;

	public FractionalMaterialCost(EmiIngredient ingredient, float amount) {
		this.ingredient = ingredient;
		this.amount = amount;
	}
}
