package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Slot.class})
public interface SlotPositionAccessor {
   @Mutable
   @Accessor("x")
   void cfhgui_menu$setX(int var1);

   @Mutable
   @Accessor("y")
   void cfhgui_menu$setY(int var1);
}
