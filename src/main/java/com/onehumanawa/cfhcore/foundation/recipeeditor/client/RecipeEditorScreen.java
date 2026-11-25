package com.onehumanawa.cfhcore.foundation.recipeeditor.client;

import com.google.common.collect.Lists;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCard;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSeparator;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipeCategory;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipeManager;
import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiReloadManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class RecipeEditorScreen extends CFHGuiScreen {
	private final CFHGuiSearchField searchField;
	private final List<CategoryButton> categoryButtons = new ArrayList<>();
	private final List<RecipeButton> recipeButtons = new ArrayList<>();
	private final CFHGuiScrollPanel categoryPanel;
	private final CFHGuiScrollPanel recipePanel;
	private String selectedCategory = null;
	private String searchQuery = "";

	public RecipeEditorScreen(@Nullable Screen parent) {
		super(Component.translatable("cfhgui_recipeeditor.screen.title"), parent);
		this.maxContentWidth = 900;
		this.searchField = new CFHGuiSearchField(0, 0, 300, this::onSearchChanged);
		this.categoryPanel = new CFHGuiScrollPanel(0, 0, 320, 280).padding(4).background(true);
		this.recipePanel = new CFHGuiScrollPanel(0, 0, 560, 480).padding(4).background(true);
	}

	@Override
	protected void build() {
		Rect content = contentRect();
		int y = content.y();

		// Search field
		this.searchField.setX(content.x());
		this.searchField.setY(y);
		this.add(this.searchField);
		y += 32;

		// Category panel (left side)
		int panelY = y;
		this.categoryPanel.setX(content.x());
		this.categoryPanel.setY(panelY);
		this.categoryPanel.setHeight(content.h() - (y - content.y()) - 16);
		this.add(this.categoryPanel);

		// Recipe panel (right side)
		this.recipePanel.setX(content.x() + 340);
		this.recipePanel.setY(panelY);
		this.recipePanel.setHeight(content.h() - (y - content.y()) - 16);
		this.add(this.recipePanel);

		// Load categories from EMI
		loadCategories();

		// Category title
		CFHGuiLabel catLabel = new CFHGuiLabel(this.categoryPanel.getX() + 8, this.categoryPanel.getY() - 20,
			Component.translatable("cfhgui_recipeeditor.category.title"));
		this.add(catLabel);

		// Recipe title
		CFHGuiLabel recLabel = new CFHGuiLabel(this.recipePanel.getX() + 8, this.recipePanel.getY() - 20,
			Component.translatable("cfhgui_recipeeditor.recipes.title"));
		this.add(recLabel);
	}

	private void loadCategories() {
		if (!EmiReloadManager.isLoaded()) {
			// Schedule retry when EMI is loaded
			Minecraft.getInstance().execute(() -> {
				if (EmiReloadManager.isLoaded()) {
					loadCategories();
				}
			});
			return; // EMI not ready yet
		}
		EmiRecipeManager manager = EmiApi.getRecipeManager();
		List<EmiRecipeCategory> categories = manager.getCategories();

		this.categoryPanel.clear();
		this.categoryButtons.clear();

		int relY = 4;
		for (EmiRecipeCategory cat : categories) {
			String catName = cat.getId().toString();
			// Filter by search query
			if (!this.searchQuery.isEmpty() && !catName.toLowerCase().contains(this.searchQuery.toLowerCase())) {
				continue;
			}

			CategoryButton btn = new CategoryButton(4, relY, this.categoryPanel.innerWidth() - 8, 24, cat, this);
			this.categoryPanel.add(btn, 4, relY);
			this.categoryButtons.add(btn);
			relY += 28;
		}
		this.categoryPanel.setContentHeight(relY);
	}

	private void loadRecipesForCategory(EmiRecipeCategory category) {
		if (!EmiReloadManager.isLoaded()) {
			// Schedule retry when EMI is loaded
			Minecraft.getInstance().execute(() -> {
				if (EmiReloadManager.isLoaded()) {
					loadRecipesForCategory(category);
				}
			});
			return; // EMI not ready yet
		}
		EmiRecipeManager manager = EmiApi.getRecipeManager();
		List<EmiRecipe> recipes = manager.getRecipes(category);

		this.recipePanel.clear();
		this.recipeButtons.clear();

		int relY = 4;
		for (EmiRecipe recipe : recipes) {
			String recipeId = recipe.getId().toString();
			// Filter by search query
			if (!this.searchQuery.isEmpty() && !recipeId.toLowerCase().contains(this.searchQuery.toLowerCase())) {
				continue;
			}

			RecipeButton btn = new RecipeButton(4, relY, this.recipePanel.innerWidth() - 8, 24, recipe, this);
			this.recipePanel.add(btn, 4, relY);
			this.recipeButtons.add(btn);
			relY += 28;
		}
		this.recipePanel.setContentHeight(relY);
	}

	private void onSearchChanged(String query) {
		this.searchQuery = query;
		loadCategories();
		// If a category is selected, reload its recipes too
		if (this.selectedCategory != null) {
			EmiRecipeManager manager = EmiApi.getRecipeManager();
			for (EmiRecipeCategory cat : manager.getCategories()) {
				if (cat.getId().toString().equals(this.selectedCategory)) {
					loadRecipesForCategory(cat);
					break;
				}
			}
		}
	}

	void onCategorySelected(EmiRecipeCategory category) {
		this.selectedCategory = category.getId().toString();
		// Update button selection states
		for (CategoryButton cb : this.categoryButtons) {
			cb.setSelected(cb.category().getId().toString().equals(this.selectedCategory));
		}
		loadRecipesForCategory(category);
	}

	@Override
	protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		// Content rendered via widgets
	}

	private RecipeEditorScreen() {
		this(null);
	}

	private static class CategoryButton extends CFHGuiWidget {
		private final EmiRecipeCategory category;
		private boolean selected;
		private final RecipeEditorScreen parentScreen;

		public CategoryButton(int x, int y, int width, int height, EmiRecipeCategory category, RecipeEditorScreen parent) {
			super(x, y, width, height, category.getName());
			this.category = category;
			this.parentScreen = parent;
		}

		public EmiRecipeCategory category() {
			return category;
		}

		public boolean isSelected() {
			return selected;
		}

		public void setSelected(boolean selected) {
			this.selected = selected;
		}

		@Override
		protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			renderButton(g, mouseX, mouseY);
		}

		@Override
		protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			renderButton(g, mouseX, mouseY);
		}

		private void renderButton(GuiGraphics g, int mouseX, int mouseY) {
			Theme t = Theme.current();
			g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
				selected ? t.palette().accent() : t.palette().bg());
			CFHGuiDraw.rect(g, getX(), getY(), getWidth(), getHeight(), t.palette().border());
			g.drawString(Minecraft.getInstance().font, getMessage(), getX() + 8, getY() + 6, t.palette().text(), false);
		}

		@Override
		public void onClick(double mouseX, double mouseY) {
			if (!this.isLocked()) {
				for (CategoryButton cb : parentScreen.categoryButtons) {
					cb.setSelected(false);
				}
				this.selected = true;
				this.flashPress();
				parentScreen.onCategorySelected(category);
			}
		}
	}

	private static class RecipeButton extends CFHGuiWidget {
		private final EmiRecipe recipe;
		private final RecipeEditorScreen parentScreen;

		public RecipeButton(int x, int y, int width, int height, EmiRecipe recipe, RecipeEditorScreen parent) {
			super(x, y, width, height, Component.literal(recipe.getId().toString()));
			this.recipe = recipe;
			this.parentScreen = parent;
		}

		@Override
		protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			renderButton(g, mouseX, mouseY);
		}

		@Override
		protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			renderButton(g, mouseX, mouseY);
		}

		private void renderButton(GuiGraphics g, int mouseX, int mouseY) {
			Theme t = Theme.current();
			g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), t.palette().bg());
			CFHGuiDraw.rect(g, getX(), getY(), getWidth(), getHeight(), t.palette().border());
			g.drawString(Minecraft.getInstance().font, getMessage(), getX() + 8, getY() + 6, t.palette().text(), false);
		}

		@Override
		public void onClick(double mouseX, double mouseY) {
			if (!this.isLocked()) {
				// Open recipe in EMI
				EmiApi.displayRecipe(this.recipe);
				this.flashPress();
			}
		}
	}
}