package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({CreativeModeInventoryScreen.class})
public interface CreativeScreenAccessor {
   @Invoker("selectTab")
   void cfhgui_menu$selectTab(CreativeModeTab var1);

   @Accessor("searchBox")
   @Nullable EditBox cfhgui_menu$searchBox();

   @Accessor("destroyItemSlot")
   @Nullable Slot cfhgui_menu$destroySlot();

   @Accessor("selectedTab")
   static CreativeModeTab cfhgui_menu$selectedTab() {
      throw new AssertionError();
   }
}
