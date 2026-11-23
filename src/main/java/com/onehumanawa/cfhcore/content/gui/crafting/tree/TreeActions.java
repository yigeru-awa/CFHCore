package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.emi.EmiRenderHelper;
import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.bom.BoM;
import com.onehumanawa.cfhcore.content.gui.emi.screen.BoMScreen;
import com.onehumanawa.cfhcore.content.gui.emi.screen.RecipeScreen;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class TreeActions {
	static boolean craft(TreeSession session, TreeNode node, boolean all) {
		EmiRecipe recipe = node.recipe();
		if (recipe != null && node.step()) {
			TreeFigures f = session.figures(node.path);
			if (f.state == TreeFigures.State.READY && f.here) {
				boolean made = TreeCraft.make(recipe, all ? Math.max(1L, f.crafts) : 1L, TreeConfig.get().craftIntoInventory);
				if (made) {
					CFHGuiSounds.click();
					Trees.lookSoon();
				}

				return made;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	static void chooseRecipe(TreeNode node) {
		EmiIngredient asked = node.asked();
		if (asked != null) {
			EmiApi.displayRecipes(asked);
			Minecraft mc = Minecraft.getInstance();
			if (mc.screen instanceof RecipeScreen screen) {
				RecipeScreen.resolve = asked;
				screen.init(mc, screen.width, screen.height);
				if (node.recipe() != null) {
					EmiApi.focusRecipe(node.recipe());
				}
			}
		}
	}

	static boolean hasRecipes(TreeNode node) {
		EmiIngredient asked = node.asked();
		if (asked != null && !asked.isEmpty()) {
			return asked.getEmiStacks().size() > 1 ? true : !EmiApi.getRecipeManager().getRecipesByOutput(asked.getEmiStacks().get(0)).isEmpty();
		} else {
			return false;
		}
	}

	static void showRecipe(TreeNode node) {
		EmiIngredient shown = node.ingredient();
		if (shown != null) {
			EmiApi.displayRecipes(shown);
			if (node.recipe() != null) {
				EmiApi.focusRecipe(node.recipe());
			}
		}
	}

	static void fetchEverywhere(TreeSession session, TreeNode node) {
		session.choose(node, null);
		Trees.synthetic();
	}

	static boolean fetched(TreeSession session, TreeNode node) {
		EmiIngredient asked = node.asked();
		return asked != null && session.tree.resolutions.containsKey(asked) && session.tree.resolutions.get(asked) == null;
	}

	static void makeAgain(TreeSession session, TreeNode node) {
		EmiIngredient asked = node.asked();
		if (asked != null) {
			session.tree.resolutions.remove(asked);
			session.tree.recalculate();
			Trees.synthetic();
		}
	}

	static int places(TreeSession session, TreeNode node) {
		EmiIngredient asked = node.asked();
		if (asked == null) {
			return 0;
		} else {
			int n = 0;

			for (TreeNode other : session.outline().all) {
				if (asked.equals(other.asked())) {
					n++;
				}
			}

			return n;
		}
	}

	static void copyShoppingList(TreeSession session) {
		StringBuilder text = new StringBuilder();
		text.append(Component.translatable("cfhgui_crafting.tree.list.title", new Object[]{session.displayName()}).getString());

		for (TreeFigures.Raw raw : session.raws()) {
			if (raw.missing > 0L) {
				text.append('\n')
					.append(raw.estimated ? "~" : "")
					.append(EmiRenderHelper.getAmountText(raw.ingredient, raw.missing).getString())
					.append("x ")
					.append(name(raw.ingredient).getString());
			}
		}

		Minecraft.getInstance().keyboardHandler.setClipboard(text.toString());
		CFHGuiToasts.show(Component.translatable("cfhgui_crafting.tree.list.copied"), null, Icon.COPY);
	}

	static Component name(EmiIngredient ingredient) {
		return ingredient.getEmiStacks().get(0).getName();
	}

	static void craftingMode(boolean on) {
		BoM.craftingMode = on;
		Trees.synthetic();
	}

	static void openPlanner() {
		EmiApi.viewRecipeTree();
	}

	static boolean inPlanner() {
		Screen screen = Minecraft.getInstance().screen;
		return screen instanceof BoMScreen;
	}

	private TreeActions() {
	}
}
