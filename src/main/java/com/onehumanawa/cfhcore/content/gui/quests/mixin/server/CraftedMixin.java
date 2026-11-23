package com.onehumanawa.cfhcore.content.gui.quests.mixin.server;

import com.onehumanawa.cfhcore.content.gui.quests.server.QuestEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemStack.class})
public abstract class CraftedMixin {
	@Inject(
		method = {"onCraftedBy"},
		at = {@At("HEAD")}
	)
	private void slateQuests$crafted(Level level, Player player, int amount, CallbackInfo ci) {
		if (player instanceof ServerPlayer crafter) {
			QuestEvents.crafted(crafter, (ItemStack)(Object)this, amount);
		}
	}
}
