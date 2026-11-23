package com.onehumanawa.cfhcore.content.gui.emi.mixin;

import com.onehumanawa.cfhcore.content.gui.emi.config.EmiConfig;
import com.onehumanawa.cfhcore.content.gui.emi.config.RecipeBookAction;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarType;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({RecipeBookComponent.class})
public abstract class RecipeBookWidgetMixin {
	@Shadow
	public abstract boolean isVisible();

	@Shadow
	protected abstract void setVisible(boolean var1);

	@Inject(
		at = {@At("HEAD")},
		method = {"toggleVisibility"},
		cancellable = true
	)
	public void toggleOpen(CallbackInfo info) {
		if (EmiConfig.recipeBookAction != RecipeBookAction.DEFAULT) {
			if (EmiConfig.recipeBookAction == RecipeBookAction.TOGGLE_CRAFTABLES) {
				EmiScreenManager.toggleSidebarType(SidebarType.CRAFTABLES);
			} else if (EmiConfig.recipeBookAction == RecipeBookAction.TOGGLE_VISIBILITY) {
				EmiScreenManager.toggleVisibility(false);
			}

			if (this.isVisible()) {
				this.setVisible(false);
			}

			info.cancel();
		}
	}
}
