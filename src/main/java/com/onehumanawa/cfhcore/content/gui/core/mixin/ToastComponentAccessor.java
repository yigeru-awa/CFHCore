package com.onehumanawa.cfhcore.content.gui.core.mixin;

import java.util.List;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ToastComponent.class})
public interface ToastComponentAccessor {
   @Accessor("visible")
   List<?> cfhgui$visible();
}
