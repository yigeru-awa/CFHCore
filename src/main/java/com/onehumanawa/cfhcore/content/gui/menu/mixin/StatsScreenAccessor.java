package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.stats.StatsCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({StatsScreen.class})
public interface StatsScreenAccessor {
   @Accessor("lastScreen")
   Screen cfhgui$lastScreen();

   @Accessor("stats")
   StatsCounter cfhgui$stats();
}
