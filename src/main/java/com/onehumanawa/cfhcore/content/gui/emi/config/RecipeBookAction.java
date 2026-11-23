package com.onehumanawa.cfhcore.content.gui.emi.config;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import net.minecraft.network.chat.Component;

public enum RecipeBookAction implements ConfigEnum {
	DEFAULT("default"),
	TOGGLE_CRAFTABLES("toggle-craftables"),
	TOGGLE_VISIBILITY("toggle-visibility");

	public final String name;

	private RecipeBookAction(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Component getText() {
		return EmiPort.translatable("emi.recipe_book_action." + this.name.replace("-", "_"));
	}
}
