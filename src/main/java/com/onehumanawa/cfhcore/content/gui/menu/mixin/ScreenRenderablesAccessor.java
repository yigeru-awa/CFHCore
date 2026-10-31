package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import java.util.List;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Screen.class})
public interface ScreenRenderablesAccessor {
   @Accessor("renderables")
   List<Renderable> cfhgui_menu$renderables();
}
