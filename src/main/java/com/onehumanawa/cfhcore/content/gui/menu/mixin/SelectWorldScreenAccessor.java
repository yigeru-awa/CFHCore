package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({SelectWorldScreen.class})
public interface SelectWorldScreenAccessor {
   @Accessor("lastScreen")
   Screen cfhgui$lastScreen();
}
