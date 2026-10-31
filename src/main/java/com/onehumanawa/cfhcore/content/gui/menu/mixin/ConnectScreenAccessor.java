package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({ConnectScreen.class})
public interface ConnectScreenAccessor {
   @Invoker("<init>")
   static ConnectScreen cfhgui$create(Screen parent, Component connectFailedTitle) {
      throw new AssertionError();
   }
}
