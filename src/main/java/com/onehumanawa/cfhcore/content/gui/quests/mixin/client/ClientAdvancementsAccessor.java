package com.onehumanawa.cfhcore.content.gui.quests.mixin.client;

import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientAdvancements.class})
public interface ClientAdvancementsAccessor {
	@Accessor("progress")
	Map<AdvancementHolder, AdvancementProgress> slateQuests$progress();

	@Accessor("selectedTab")
	@Nullable
	AdvancementHolder slateQuests$selectedTab();
}
