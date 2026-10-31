package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({OptionsScreen.class})
public interface OptionsScreenAccessor {
   @Accessor("lastScreen")
   Screen cfhgui$lastScreen();
}
