package com.onehumanawa.cfhcore.content.gui.emi.screen;

import com.google.common.collect.Lists;
import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.EmiUtil;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipeDecorator;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.RecipeFillButtonWidget;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.TextWidget;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.Widget;
import com.onehumanawa.cfhcore.content.gui.emi.config.EmiConfig;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiRecipeFiller;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiRecipes;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiLog;
import com.onehumanawa.cfhcore.content.gui.emi.widget.RecipeDefaultButtonWidget;
import com.onehumanawa.cfhcore.content.gui.emi.widget.RecipeScreenshotButtonWidget;
import com.onehumanawa.cfhcore.content.gui.emi.widget.RecipeTreeButtonWidget;
import com.onehumanawa.cfhcore.content.gui.crafting.look.RecipeLook;
import com.onehumanawa.cfhcore.content.gui.crafting.team.client.PinButton;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class RecipeDisplay {
	public static final int DISPLAY_PADDING = 8;
	public final EmiRecipe recipe;
	private final int width;
	private final int height;
	private List<RecipeDisplay.ButtonType> rightButtons = Lists.newArrayList();
	private List<RecipeDisplay.ButtonType> leftButtons = Lists.newArrayList();
	private int leftWidth = 0;
	private int rightWidth = 0;
	private int rows = 0;
	public Throwable exception;

	public RecipeDisplay(EmiRecipe recipe) {
		this.recipe = recipe;
		this.width = recipe.getDisplayWidth();
		this.height = recipe.getDisplayHeight();
		if (EmiRecipeFiller.isSupported(recipe) && EmiConfig.recipeFillButton) {
			this.rightButtons.add(RecipeDisplay.ButtonType.FILL);
		}

		if (recipe.supportsRecipeTree()) {
			if (EmiConfig.recipeTreeButton) {
				this.rightButtons.add(RecipeDisplay.ButtonType.TREE);
			}

			if (EmiConfig.recipeDefaultButton) {
				this.rightButtons.add(RecipeDisplay.ButtonType.DEFAULT);
			}
		}

		if (EmiConfig.recipeScreenshotButton) {
			this.leftButtons.add(RecipeDisplay.ButtonType.SCREENSHOT);
		}

		if (RecipeLook.hasFavoriteButton(recipe)) {
			this.leftButtons.add(RecipeDisplay.ButtonType.FAVORITE);
		}

		if (PinButton.wanted(recipe)) {
			this.leftButtons.add(RecipeDisplay.ButtonType.PIN);
		}

		this.rows = Math.max(1, (this.height + 8 + 2) / 14);
		this.leftWidth = Math.max(0, (this.leftButtons.size() + this.rows - 1) / this.rows * 14 - 1);
		this.rightWidth = Math.max(0, (this.rightButtons.size() + this.rows - 1) / this.rows * 14 - 1);
		if (this.leftWidth > 0) {
			this.leftWidth = this.leftWidth + RecipeLook.buttonGap();
		}

		if (this.rightWidth > 0) {
			this.rightWidth = this.rightWidth + RecipeLook.buttonGap();
		}
	}

	public RecipeDisplay(Throwable exception) {
		this.recipe = null;
		this.width = 128;
		this.height = 64;
		this.exception = exception;
	}

	public WidgetGroup getWidgets(int x, int y, int availableWidth, int availableHeight) {
		int wx = x + (availableWidth - this.width) / 2;
		wx = Math.max(x + this.getLeftWidth(), Math.min(x + availableWidth - this.width - this.getRightWidth(), wx));
		int wWidth = this.width;
		int wHeight = Math.min(availableHeight, this.height);
		WidgetGroup widgets = new WidgetGroup(this.recipe, wx, y, wWidth, wHeight);
		if (this.recipe != null) {
			try {
				this.recipe.addWidgets(widgets);
				if (EmiConfig.showRecipeDecorators) {
					for (EmiRecipeDecorator decorator : EmiRecipes.decorators) {
						decorator.decorateRecipe(this.recipe, widgets);
					}
				}

				if (EmiConfig.devMode) {
					widgets.decorateDevMode();
				}

				this.addButtons(widgets, this.leftButtons, -17 - RecipeLook.buttonGap(), -14);
				this.addButtons(widgets, this.rightButtons, this.width + 5 + RecipeLook.buttonGap(), 14);
			} catch (Throwable var12) {
				EmiLog.error("Error constructing recipe widgets", var12);
				widgets = new WidgetGroup(this.recipe, wx, y, wWidth, wHeight);
				widgets.add(
					new TextWidget(EmiPort.ordered(EmiPort.translatable("emi.error.recipe.render")), wWidth / 2, wHeight / 2 - 5, ChatFormatting.RED.getColor(), true)
						.horizontalAlign(TextWidget.Alignment.CENTER)
				);
				if (this.exception != null) {
					@SuppressWarnings("unchecked")
				List<Component> text = (List<Component>)(List<?>) EmiUtil.getStackTrace(this.exception).stream().map(s -> EmiPort.literal(s)).toList();
					widgets.addTooltipText(text, 0, 0, wWidth, wHeight);
				}
			}
		} else {
			widgets.add(
				new TextWidget(EmiPort.ordered(EmiPort.translatable("emi.error.recipe.initialize")), wWidth / 2, wHeight / 2 - 5, ChatFormatting.RED.getColor(), true)
					.horizontalAlign(TextWidget.Alignment.CENTER)
			);
			if (this.exception != null) {
				@SuppressWarnings("unchecked")
				List<Component> text = (List<Component>)(List<?>) EmiUtil.getStackTrace(this.exception).stream().map(s -> EmiPort.literal(s)).toList();
				widgets.addTooltipText(text, 0, 0, wWidth, wHeight);
			}
		}

		return widgets;
	}

	private void addButtons(WidgetGroup widgets, List<RecipeDisplay.ButtonType> types, int x, int xOff) {
		int space = Math.min(8, this.height + 8 - (Math.min(this.rows, types.size()) * 14 - 2));
		int bottom = this.height + 4 - 12 - space / 2;
		int size = types.size();

		while (size > 0) {
			int used = Math.min(this.rows, size);
			int columnBottom = RecipeLook.buttonColumnBottom(this.height, used, bottom);
			List<RecipeDisplay.ButtonType> current = types.subList(size - used, size);
			int yOff = 0;

			for (RecipeDisplay.ButtonType type : current) {
				int by = columnBottom - yOff;

				widgets.add((Widget)(switch (type) {
					case FILL -> new RecipeFillButtonWidget(x, by, this.recipe);
					case TREE -> new RecipeTreeButtonWidget(x, by, this.recipe);
					case DEFAULT -> new RecipeDefaultButtonWidget(x, by, this.recipe);
					case SCREENSHOT -> new RecipeScreenshotButtonWidget(x, by, this.recipe);
					case FAVORITE -> RecipeLook.favoriteButton(x, by, this.recipe);
					case PIN -> new PinButton(x, by, this.recipe);
				}));
				yOff += 14;
			}

			size -= used;
			x += xOff;
		}
	}

	public int getLeftWidth() {
		return this.leftWidth;
	}

	public int getRightWidth() {
		return this.rightWidth;
	}

	public int getWidth() {
		return this.leftWidth + this.rightWidth + this.width;
	}

	public int getHeight() {
		return this.height;
	}

	private static enum ButtonType {
		FILL,
		TREE,
		DEFAULT,
		SCREENSHOT,
		FAVORITE,
		PIN;
	}
}
