package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({JoinMultiplayerScreen.class})
public interface JoinMultiplayerScreenAccessor {
   @Accessor("lastScreen")
   Screen cfhgui$lastScreen();
}
