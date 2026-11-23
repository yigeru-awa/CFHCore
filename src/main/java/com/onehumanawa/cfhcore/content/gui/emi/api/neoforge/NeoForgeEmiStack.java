package com.onehumanawa.cfhcore.content.gui.emi.api.neoforge;

import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import net.neoforged.neoforge.fluids.FluidStack;

public final class NeoForgeEmiStack {
	public static EmiStack of(FluidStack stack) {
		return EmiStack.of(stack.getFluid(), stack.getComponentsPatch(), (long)stack.getAmount());
	}
}
