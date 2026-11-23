package com.onehumanawa.cfhcore.content.gui.emi.stack.serializer;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.FluidEmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.serializer.EmiStackSerializer;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

public class FluidEmiStackSerializer implements EmiStackSerializer<FluidEmiStack> {
	@Override
	public String getType() {
		return "fluid";
	}

	@Override
	public EmiStack create(ResourceLocation id, DataComponentPatch componentChanges, long amount) {
		return EmiStack.of((Fluid)EmiPort.getFluidRegistry().get(id), componentChanges, amount);
	}
}
