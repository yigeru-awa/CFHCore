package com.onehumanawa.cfhcore.foundation.recipeeditor.client;

import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCard;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSeparator;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class RecipeEditorScreen extends CFHGuiScreen {
	private final CFHGuiSearchField searchField;
	private final List<CategoryButton> categoryButtons = new ArrayList<>();
	private String selectedCategory = null;

	public RecipeEditorScreen(@Nullable Screen parent) {
		super(Component.translatable("cfhgui_recipeeditor.screen.title"), parent);
		this.maxContentWidth = 900;
		this.searchField = new CFHGuiSearchField(0, 0, 300, this::onSearchChanged);
	}

	@Override
	protected void build() {
		Rect content = contentRect();
		int y = content.y();

		// Search field
		this.searchField.setX(content.x());
		this.searchField.setY(y);
		this.add(this.searchField);
		y += 28;

		// Category list (placeholder - will be populated from JEI at runtime)
		CFHGuiLabel catLabel = new CFHGuiLabel(content.x(), y, Component.translatable("cfhgui_recipeeditor.category.title"));
		this.add(catLabel);
		y += 20;

		// Placeholder categories - in reality these would come from JEI
		String[] categories = {"crafting", "smelting", "blasting", "smoking", "campfire_cooking", "stonecutting", "smithing"};
		for (String cat : categories) {
			CategoryButton btn = new CategoryButton(content.x(), y, content.w(), 24, cat, this);
			this.add(btn);
			categoryButtons.add(btn);
			y += 28;
		}

		y += 12;
		CFHGuiSeparator sep = new CFHGuiSeparator(content.x(), y, content.w(), true);
		this.add(sep);
		y += 16;

		// Recipe list area (placeholder)
		CFHGuiCard card = new CFHGuiCard(content.x(), y, content.w(), 200);
		card.add(new CFHGuiLabel(8, 8, Component.translatable("cfhgui_recipeeditor.recipes.placeholder")), 8, 8);
		this.add(card);
	}

	private void onSearchChanged(String query) {
		// Filter categories/recipes by search query
	}

	void onCategorySelected(String category) {
		this.selectedCategory = category;
	}

	@Override
	protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		// Content rendered via widgets
	}

	private RecipeEditorScreen() {
		this(null);
	}

	private class CategoryButton extends CFHGuiWidget {
		private final String category;
		private boolean selected;
		private final RecipeEditorScreen parentScreen;

		public CategoryButton(int x, int y, int width, int height, String category, RecipeEditorScreen parent) {
			super(x, y, width, height, Component.literal(category));
			this.category = category;
			this.parentScreen = parent;
		}

		public String category() {
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
			g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), selected ? t.palette().accent() : t.palette().bg());
			CFHGuiDraw.rect(g, getX(), getY(), getWidth(), getHeight(), t.palette().border());
			g.drawString(font, Component.literal(category), getX() + 8, getY() + 6, t.palette().text(), false);
		}

		@Override
		public void onClick(double mouseX, double mouseY) {
			for (CategoryButton cb : parentScreen.categoryButtons) {
				cb.setSelected(false);
			}
			this.selected = true;
			parentScreen.onCategorySelected(category);
		}
	}
}