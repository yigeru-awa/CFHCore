package com.onehumanawa.cfhcore.content.gui.emi.screen.tooltip;

import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.mixin.accessor.OrderedTextTooltipComponentAccessor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.util.FormattedCharSequence;

public class EmiTextTooltipWrapper extends ClientTextTooltip {
	public EmiIngredient stack;

	public EmiTextTooltipWrapper(EmiIngredient stack, FormattedCharSequence text) {
		super(text);
		this.stack = stack;
	}

	public EmiTextTooltipWrapper(EmiIngredient stack, ClientTextTooltip original) {
		super(((OrderedTextTooltipComponentAccessor)original).getText());
		this.stack = stack;
	}
}
