package com.onehumanawa.cfhcore.content.gui.emi.mixin.conversion;

import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStackConvertible;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({ItemStack.class})
public class ItemStackMixin implements EmiStackConvertible {
	@Override
	public EmiStack emi() {
		return EmiStack.of((ItemStack)(Object)this);
	}

	@Override
	public EmiStack emi(long amount) {
		return EmiStack.of((ItemStack)(Object)this, amount);
	}
}
