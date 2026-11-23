package com.onehumanawa.cfhcore.content.gui.quests.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({AbstractContainerScreen.class})
public interface ContainerScreenPlaceAccessor {
	@Accessor("leftPos")
	int slateQuests$leftPos();

	@Accessor("topPos")
	int slateQuests$topPos();

	@Accessor("imageWidth")
	int slateQuests$imageWidth();
}
