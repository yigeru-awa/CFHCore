package com.onehumanawa.cfhcore.content.gui.quests.mixin.client;

import java.util.List;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ToastComponent.class})
public interface ToastStackAccessor {
	@Accessor("visible")
	List<?> slateQuests$visible();
}
