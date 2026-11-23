package com.onehumanawa.cfhcore.content.gui.emi.handler;

import com.google.common.collect.Lists;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiCraftingRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.VanillaEmiRecipeCategories;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.handler.StandardRecipeHandler;
import com.onehumanawa.cfhcore.content.gui.emi.mixin.accessor.CraftingResultSlotAccessor;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;

public class CoercedRecipeHandler<T extends AbstractContainerMenu> implements StandardRecipeHandler<T> {
	private ResultSlot output;
	private CraftingContainer inv;

	public CoercedRecipeHandler(ResultSlot output) {
		this.output = output;
		this.inv = ((CraftingResultSlotAccessor)output).getInput();
	}

	@Override
	public Slot getOutputSlot(AbstractContainerMenu handler) {
		return this.output;
	}

	@Override
	public List<Slot> getInputSources(AbstractContainerMenu handler) {
		Minecraft client = Minecraft.getInstance();
		List<Slot> slots = Lists.newArrayList();
		if (this.output != null) {
			for (Slot slot : handler.slots) {
				if (slot.isActive() && slot.mayPickup(client.player) && slot != this.output) {
					slots.add(slot);
				}
			}
		}

		return slots;
	}

	@Override
	public List<Slot> getCraftingSlots(AbstractContainerMenu handler) {
		List<Slot> slots = Lists.newArrayList();
		int width = this.inv.getWidth();
		int height = this.inv.getHeight();

		for (int i = 0; i < 9; i++) {
			slots.add(null);
		}

		for (Slot slot : handler.slots) {
			if (slot.container == this.inv && slot.getContainerSlot() < width * height && slot.getContainerSlot() >= 0) {
				int index = slot.getContainerSlot();
				index = index * 3 / width;
				slots.set(index, slot);
			}
		}

		return slots;
	}

	@Override
	public boolean supportsRecipe(EmiRecipe recipe) {
		if (recipe.getCategory() != VanillaEmiRecipeCategories.CRAFTING || !recipe.supportsRecipeTree()) {
			return false;
		} else {
			return recipe instanceof EmiCraftingRecipe crafting ? crafting.canFit(this.inv.getWidth(), this.inv.getHeight()) : true;
		}
	}
}
