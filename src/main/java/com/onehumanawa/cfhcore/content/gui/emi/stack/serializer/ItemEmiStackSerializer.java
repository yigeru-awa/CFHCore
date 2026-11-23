package com.onehumanawa.cfhcore.content.gui.emi.stack.serializer;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.ItemEmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.serializer.EmiStackSerializer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class ItemEmiStackSerializer implements EmiStackSerializer<ItemEmiStack> {
	@Override
	public String getType() {
		return "item";
	}

	@Override
	public EmiStack create(ResourceLocation id, DataComponentPatch componentChanges, long amount) {
		ItemStack stack = new ItemStack((Holder)EmiPort.getItemRegistry().getHolder(id).orElseThrow(), 1, componentChanges);
		return EmiStack.of(stack, amount);
	}
}
