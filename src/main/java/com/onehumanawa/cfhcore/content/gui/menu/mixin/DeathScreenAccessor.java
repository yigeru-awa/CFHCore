package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({DeathScreen.class})
public interface DeathScreenAccessor {
   @Accessor("causeOfDeath")
   @Nullable Component cfhgui$causeOfDeath();

   @Accessor("hardcore")
   boolean cfhgui$hardcore();
}
