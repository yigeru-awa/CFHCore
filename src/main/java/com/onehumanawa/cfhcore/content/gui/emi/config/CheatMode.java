package com.onehumanawa.cfhcore.content.gui.emi.config;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import net.minecraft.network.chat.Component;

public enum CheatMode implements ConfigEnum {
	TRUE("true"),
	FALSE("false"),
	CREATIVE("creative");

	private final String name;

	private CheatMode(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Component getText() {
		return EmiPort.translatable("emi.cheat_mode." + this.name.replace("-", "_"));
	}
}
