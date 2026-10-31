package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({MouseHandler.class})
public interface MouseHandlerPosAccessor {
   @Accessor("xpos")
   void cfhgui$setXpos(double var1);

   @Accessor("ypos")
   void cfhgui$setYpos(double var1);
}
