package com.onehumanawa.cfhcore.content.gui.emi.recipe.special;

import com.onehumanawa.cfhcore.content.gui.emi.EmiUtil;
import com.onehumanawa.cfhcore.content.gui.emi.api.render.EmiTexture;
import com.onehumanawa.cfhcore.content.gui.emi.api.render.EmiTooltipComponents;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.WidgetHolder;
import com.onehumanawa.cfhcore.content.gui.emi.recipe.EmiSmithingRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.ProxyRecipeManager;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;

public class EmiSmithingTrimRecipe extends EmiSmithingRecipe {
	private final SmithingRecipe recipe;
	private final int uniq = EmiUtil.RANDOM.nextInt();

	public EmiSmithingTrimRecipe(EmiIngredient template, EmiIngredient input, EmiIngredient addition, EmiStack output, SmithingRecipe recipe) {
		super(template, input, addition, output, ProxyRecipeManager.getId(recipe));
		this.recipe = recipe;
	}

	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		widgets.addTexture(EmiTexture.EMPTY_ARROW, 62, 1);
		widgets.addSlot(this.template, 0, 0);
		widgets.addGeneratedSlot(r -> this.getStack(r, 0), this.uniq, 18, 0)
			.appendTooltip(() -> EmiTooltipComponents.getIngredientTooltipComponent(this.input.getEmiStacks()));
		widgets.addGeneratedSlot(r -> this.getStack(r, 1), this.uniq, 36, 0)
			.appendTooltip(() -> EmiTooltipComponents.getIngredientTooltipComponent(this.addition.getEmiStacks()));
		widgets.addGeneratedSlot(r -> this.getStack(r, 2), this.uniq, 94, 0).recipeContext(this);
	}

	private EmiStack getStack(Random r, int i) {
		EmiStack input = this.input.getEmiStacks().get(r.nextInt(this.input.getEmiStacks().size()));
		EmiStack addition = this.addition.getEmiStacks().get(r.nextInt(this.addition.getEmiStacks().size()));
		SmithingRecipeInput inv = new SmithingRecipeInput(this.template.getEmiStacks().get(0).getItemStack(), input.getItemStack(), addition.getItemStack());
		Minecraft client = Minecraft.getInstance();
		return new EmiStack[]{input, addition, EmiStack.of(this.recipe.assemble(inv, client.level.registryAccess()))}[i];
	}
}
