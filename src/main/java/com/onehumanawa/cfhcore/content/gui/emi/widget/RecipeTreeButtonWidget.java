package com.onehumanawa.cfhcore.content.gui.emi.widget;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.bom.BoM;
import com.onehumanawa.cfhcore.content.gui.crafting.tree.Tree;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

public class RecipeTreeButtonWidget extends RecipeButtonWidget {
	public RecipeTreeButtonWidget(int x, int y, EmiRecipe recipe) {
		super(x, y, 36, 0, recipe);
	}

	@Override
	public int getTextureOffset(int mouseX, int mouseY) {
		int v = super.getTextureOffset(mouseX, mouseY);
		if (BoM.tree != null && BoM.tree.goal.recipe == this.recipe) {
			v += 36;
		}

		return v;
	}

	@Override
	public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
		return List.of(
			ClientTooltipComponent.create(EmiPort.ordered(EmiPort.translatable("tooltip.emi.view_tree"))),
			ClientTooltipComponent.create(EmiPort.ordered(EmiPort.translatable("cfhgui_crafting.tree.recipe_button.more").withStyle(ChatFormatting.GRAY)))
		);
	}

	@Override
	public boolean mouseClicked(int mouseX, int mouseY, int button) {
		this.playButtonSound();
		if (Tree.recipeButton(this.recipe, button)) {
			return true;
		} else {
			BoM.setGoal(this.recipe);
			EmiApi.viewRecipeTree();
			return true;
		}
	}
}
