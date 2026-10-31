package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.world.level.WorldDataConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({CreateWorldScreen.class})
public interface CreateWorldScreenAccess {
   @Accessor("lastScreen")
   Screen cfhgui$lastScreen();

   @Invoker("onCreate")
   void cfhgui$create();

   @Invoker("openExperimentsScreen")
   void cfhgui$openExperiments(WorldDataConfiguration var1);

   @Invoker("openDataPackSelectionScreen")
   void cfhgui$openDataPacks(WorldDataConfiguration var1);
}
