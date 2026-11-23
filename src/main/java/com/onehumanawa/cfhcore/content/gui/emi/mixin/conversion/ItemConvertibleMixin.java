package com.onehumanawa.cfhcore.content.gui.emi.mixin.conversion;

import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStackConvertible;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({ItemLike.class})
public interface ItemConvertibleMixin extends EmiStackConvertible {
	@Override
	default EmiStack emi() {
		return EmiStack.of((Item)(Object)this);
	}

	@Override
	default EmiStack emi(long amount) {
		return EmiStack.of((Item)(Object)this, amount);
	}
}
