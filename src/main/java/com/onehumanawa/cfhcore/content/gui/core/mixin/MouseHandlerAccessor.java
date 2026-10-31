package com.onehumanawa.cfhcore.content.gui.core.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({MouseHandler.class})
public interface MouseHandlerAccessor {
   @Accessor("xpos")
   void cfhgui$setXpos(double var1);

   @Accessor("ypos")
   void cfhgui$setYpos(double var1);

   @Invoker("onPress")
   void cfhgui$onPress(long var1, int var3, int var4, int var5);
}
