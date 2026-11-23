package com.onehumanawa.cfhcore.content.gui.server.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({AbstractContainerScreen.class})
public interface ContainerScreenPlaceAccessor {
	@Accessor("leftPos")
	int cfhServer$leftPos();

	@Accessor("topPos")
	int cfhServer$topPos();

	@Accessor("imageWidth")
	int cfhServer$imageWidth();
}
