package com.onehumanawa.cfhcore.content.gui.emi.config;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import net.minecraft.network.chat.Component;

public enum SidebarSide implements ConfigEnum {
	NONE("none"),
	LEFT("left"),
	RIGHT("right"),
	TOP("top"),
	BOTTOM("bottom");

	private final String name;

	private SidebarSide(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Component getText() {
		return EmiPort.translatable("emi.sidebar." + this.name);
	}
}
