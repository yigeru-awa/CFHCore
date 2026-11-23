package com.onehumanawa.cfhcore.content.gui.emi.mixin.conversion;

import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStackConvertible;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({Fluid.class})
public class FluidMixin implements EmiStackConvertible {
	@Override
	public EmiStack emi() {
		return EmiStack.of((Fluid)(Object)this);
	}

	@Override
	public EmiStack emi(long amount) {
		return EmiStack.of((Fluid)(Object)this, amount);
	}
}
