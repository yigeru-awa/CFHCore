package com.onehumanawa.cfhcore.content.gui.quests.mixin.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({AdvancementsScreen.class})
public interface AdvancementsScreenAccessor {
	@Accessor("lastScreen")
	@Nullable
	Screen slateQuests$lastScreen();
}
