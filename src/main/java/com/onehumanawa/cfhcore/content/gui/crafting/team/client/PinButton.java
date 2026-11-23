package com.onehumanawa.cfhcore.content.gui.crafting.team.client;

import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.widget.RecipeButtonWidget;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.crafting.look.LookButtons;
import com.onehumanawa.cfhcore.content.gui.crafting.team.Pin;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

public final class PinButton extends RecipeButtonWidget {
	static int lastX = -1;
	static int lastY = -1;

	public static boolean wanted(EmiRecipe recipe) {
		try {
			return TeamPinsClient.active() && recipe != null && recipe.getId() != null && !recipe.getOutputs().isEmpty();
		} catch (RuntimeException var2) {
			return false;
		}
	}

	public PinButton(int x, int y, EmiRecipe recipe) {
		super(x, y, 0, 0, recipe);
	}

	@Override
	public void render(GuiGraphics raw, int mouseX, int mouseY, float delta) {
		Pin pin = TeamPinsClient.recipePin(this.recipe);
		boolean enabled = pin == null || TeamPinsClient.mayUnpin(pin);
		LookButtons.smallButton(this, raw, this.x, this.y, Icon.PIN, pin != null, enabled, this.getBounds().contains(mouseX, mouseY));
		Matrix4f at = raw.pose().last().pose();
		lastX = Math.round(at.m30()) + this.x + 6;
		lastY = Math.round(at.m31()) + this.y + 6;
	}

	@Override
	public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
		Pin pin = TeamPinsClient.recipePin(this.recipe);
		List<Component> lines = new ArrayList<>();
		if (pin == null) {
			lines.add(Component.translatable("cfhgui_crafting.team.recipe.pin"));
			lines.add(TeamText.hint("cfhgui_crafting.team.recipe.pin.tip"));
		} else {
			lines.add(Component.translatable("cfhgui_crafting.team.recipe.pinned"));
			lines.add(TeamText.pinnedBy(pin));
			lines.add(TeamText.hint(TeamPinsClient.mayUnpin(pin) ? "cfhgui_crafting.team.recipe.unpin.tip" : "cfhgui_crafting.team.recipe.theirs.tip"));
		}

		List<ClientTooltipComponent> out = new ArrayList<>();

		for (Component line : lines) {
			out.add(ClientTooltipComponent.create(line.getVisualOrderText()));
		}

		return out;
	}

	@Override
	public boolean mouseClicked(int mouseX, int mouseY, int button) {
		if (button != 0) {
			return false;
		} else {
			Pin pin = TeamPinsClient.recipePin(this.recipe);
			if (pin == null) {
				TeamPinsClient.pinRecipe(this.recipe);
			} else {
				if (!TeamPinsClient.mayUnpin(pin)) {
					return false;
				}

				TeamPinsClient.unpin(pin);
			}

			this.playButtonSound();
			return true;
		}
	}
}
