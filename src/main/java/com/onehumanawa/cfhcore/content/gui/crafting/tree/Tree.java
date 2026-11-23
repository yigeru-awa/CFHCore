package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents.ScreenRender;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.crafting.CFHGuiCrafting;

public final class Tree {
	public static void init() {
		CFHGuiEvents.CLIENT_TICK_END.register(Trees::tick);
		CFHGuiEvents.CLIENT_LEFT_SERVER.register(Trees::flush);
		CFHGuiEvents.SCREEN_RENDER_POST
			.register((ScreenRender)(screen, graphics, mouseX, mouseY, partialTick) -> TreeDock.INSTANCE.tooltip(screen, graphics, mouseX, mouseY));
		TreeDockSeam.install(TreeDock.INSTANCE);
		TreeShots.init();
		if (CFHGuiPlatform.get().isModLoaded("cfhgui_config")) {
			try {
				TreeSettings.register();
			} catch (Throwable var1) {
				CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] the tree's settings could not be added to CFHGui Config: {}", var1.toString());
			}
		}
	}

	public static void start(EmiRecipe recipe, boolean planner) {
		start(recipe, planner, false);
	}

	public static void start(EmiRecipe recipe, boolean planner, boolean fresh) {
		TreeSession now = Trees.active();
		if (fresh || now == null || now.tree.goal.recipe != recipe) {
			TreeEntry mine = fresh ? null : Trees.of(recipe);
			if (mine == null) {
				Trees.start(recipe);
			} else if (mine != Trees.shown() && !Trees.show(mine)) {
				Trees.start(recipe);
			}
		}

		TreeDock dock = TreeDock.INSTANCE;
		if (planner || !TreeConfig.get().openDockWithNewTree) {
			TreeActions.openPlanner();
		} else if (dock.isOpen()) {
			if (dock.bounds().empty()) {
				TreeActions.openPlanner();
			}
		} else if (dock.fits()) {
			dock.open();
		} else {
			TreeActions.openPlanner();
		}
	}

	public static boolean recipeButton(EmiRecipe recipe, int button) {
		if (button == 0) {
			start(recipe, false);
			return true;
		} else if (button == 1) {
			TreeMenus.recipe(recipe, (double)EmiScreenManager.lastMouseX, (double)EmiScreenManager.lastMouseY);
			return true;
		} else {
			return false;
		}
	}

	private Tree() {
	}
}
