package com.onehumanawa.cfhcore.content.gui.emi.api.neoforge;

import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import java.util.Arrays;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

public final class NeoForgeEmiIngredient {
	public static EmiIngredient of(SizedIngredient ingredient) {
		return EmiIngredient.of(ingredient.ingredient(), (long)ingredient.count());
	}

	public static EmiIngredient of(FluidIngredient ingredient) {
		return EmiIngredient.of(Arrays.stream(ingredient.getStacks()).map(NeoForgeEmiStack::of).toList());
	}

	public static EmiIngredient of(SizedFluidIngredient ingredient) {
		return of(ingredient.ingredient()).setAmount((long)ingredient.amount());
	}
}
