package com.onehumanawa.cfhcore.content.gui.emi.data;

import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import java.util.List;
import net.minecraft.network.chat.Component;

public record EmiAlias(List<EmiIngredient> stacks, List<String> keys) {
	public static record Baked(List<EmiIngredient> stacks, List<Component> text) {
	}
}
