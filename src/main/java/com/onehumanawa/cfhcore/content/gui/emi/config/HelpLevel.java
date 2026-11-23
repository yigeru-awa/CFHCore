package com.onehumanawa.cfhcore.content.gui.emi.config;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import net.minecraft.network.chat.Component;

public enum HelpLevel implements ConfigEnum {
	VERBOSE("verbose"),
	NORMAL("normal"),
	NONE("none");

	public final String name;

	private HelpLevel(String name) {
		this.name = name;
	}

	public boolean has(HelpLevel other) {
		return other.ordinal() >= this.ordinal();
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Component getText() {
		return EmiPort.translatable("emi.help_level." + this.name.replace("-", "_"));
	}
}
