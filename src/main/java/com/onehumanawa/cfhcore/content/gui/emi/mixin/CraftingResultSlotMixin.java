package com.onehumanawa.cfhcore.content.gui.emi.mixin;

import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiSidebars;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.ProxyRecipeManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ResultSlot.class})
public class CraftingResultSlotMixin {
	@Shadow
	@Final
	private CraftingContainer craftSlots;
	@Shadow
	@Final
	private Player player;

	@Inject(
		at = {@At("HEAD")},
		method = {"checkTakeAchievements(Lnet/minecraft/world/item/ItemStack;)V"}
	)
	private void onCrafted(ItemStack stack, CallbackInfo info) {
		Level world = this.player.level();
		if (world.isClientSide) {
			CraftingRecipe crafting = ProxyRecipeManager.getFirst(RecipeType.CRAFTING, this.craftSlots.asPositionedCraftInput().input());
			if (crafting != null) {
				EmiRecipe recipe = EmiApi.getRecipeManager().getRecipe(ProxyRecipeManager.getId(crafting));
				if (recipe != null) {
					EmiSidebars.craft(recipe);
				}
			}
		}
	}
}
