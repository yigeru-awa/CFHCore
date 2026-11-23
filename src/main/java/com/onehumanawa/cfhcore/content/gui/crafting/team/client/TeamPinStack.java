package com.onehumanawa.cfhcore.content.gui.crafting.team.client;

import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.render.EmiTooltipComponents;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiFavorite;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.crafting.team.Pin;
import com.onehumanawa.cfhcore.content.gui.crafting.tree.TreeCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class TeamPinStack extends EmiFavorite {
	public final Pin pin;

	private TeamPinStack(Pin pin, EmiIngredient stack, @Nullable EmiRecipe recipe) {
		super(stack, recipe);
		this.pin = pin;
	}

	@Nullable
	static TeamPinStack of(Pin pin) {
		EmiRecipe recipe = TeamPinsClient.recipe(pin.recipe);
		if (pin.isTree()) {
			EmiStack goal = pin.tree == null ? null : TreeCodec.goal(pin.tree);
			return goal != null && !goal.isEmpty() ? new TeamPinStack(pin, goal.copy().setAmount(1L), recipe) : null;
		} else {
			return recipe != null && !recipe.getOutputs().isEmpty() ? new TeamPinStack(pin, recipe.getOutputs().get(0), recipe) : null;
		}
	}

	EmiFavorite plain() {
		return new EmiFavorite(this.stack, this.recipe);
	}

	@Override
	public EmiIngredient copy() {
		return this.plain();
	}

	@Override
	public void render(GuiGraphics raw, int x, int y, float delta, int flags) {
		if (!this.pin.isTree()) {
			super.render(raw, x, y, delta, flags);
		} else {
			this.stack.render(raw, x, y, delta, flags & -3);
			if ((flags & 4) != 0) {
				raw.pose().pushPose();
				raw.pose().translate(0.0F, 0.0F, 250.0F);
				boolean van = Theme.current().isVanilla();
				int ground = van ? -534765536 : Theme.current().palette().bg() & 16777215 | -536870912;
				raw.fill(x + 9, y - 1, x + 17, y + 7, ground);
				Icons.draw(raw, Icon.LAN, x + 9, y - 1, 8, van ? -1 : Theme.current().palette().accentInk());
				raw.pose().popPose();
			}
		}
	}

	@Override
	public List<ClientTooltipComponent> getTooltip() {
		List<ClientTooltipComponent> list = new ArrayList<>();
		if (this.pin.isTree()) {
			list.add(EmiTooltipComponents.of(TeamTrees.name(this.pin)));
			list.add(EmiTooltipComponents.of(TeamText.quiet(Component.translatable("cfhgui_crafting.team.tree_of", new Object[]{TeamText.steps(this.pin.steps)}))));
		} else {
			list.addAll(super.getTooltip());
		}

		list.add(EmiTooltipComponents.of(TeamText.pinnedBy(this.pin)));
		list.add(EmiTooltipComponents.of(TeamText.hint(this.pin.isTree() ? "cfhgui_crafting.team.tip.tree_keys" : "cfhgui_crafting.team.tip.recipe_keys")));
		return list;
	}

	@Override
	public boolean isUnbatchable() {
		return this.pin.isTree() || super.isUnbatchable();
	}
}
