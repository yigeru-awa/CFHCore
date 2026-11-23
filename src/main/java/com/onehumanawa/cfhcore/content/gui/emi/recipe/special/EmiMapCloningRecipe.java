package com.onehumanawa.cfhcore.content.gui.emi.recipe.special;

import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiPatternCraftingRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.GeneratedSlotWidget;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.SlotWidget;
import java.util.List;
import java.util.Random;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class EmiMapCloningRecipe extends EmiPatternCraftingRecipe {
	public EmiMapCloningRecipe(ResourceLocation id) {
		super(List.of(EmiStack.of(Items.FILLED_MAP), EmiStack.of(Items.MAP)), EmiStack.of(Items.FILLED_MAP), id);
	}

	@Override
	public SlotWidget getInputWidget(int slot, int x, int y) {
		if (slot == 0) {
			return new SlotWidget(EmiStack.of(Items.FILLED_MAP), x, y);
		} else {
			int s = slot - 1;
			return new GeneratedSlotWidget(r -> {
				int amount = r.nextInt(8) + 1;
				return s < amount ? EmiStack.of(Items.MAP) : EmiStack.EMPTY;
			}, this.unique, x, y);
		}
	}

	@Override
	public SlotWidget getOutputWidget(int x, int y) {
		return new GeneratedSlotWidget(r -> EmiStack.of(Items.FILLED_MAP, (long)(r.nextInt(8) + 2)), this.unique, x, y);
	}

	public EmiStack getAmount(Random random, Item item) {
		return EmiStack.of(item);
	}
}
