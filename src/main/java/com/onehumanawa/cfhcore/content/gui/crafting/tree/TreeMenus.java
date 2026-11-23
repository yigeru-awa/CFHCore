package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.bom.BoM;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiContextMenu;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.crafting.team.client.TeamTrees;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

final class TreeMenus {
	private static Component text(String key, Object... args) {
		return Component.translatable("cfhgui_crafting.tree.menu." + key, args);
	}

	static void row(TreeSession session, TreeNode node, double x, double y) {
		List<Item> items = new ArrayList<>();
		boolean goal = node.depth == 0;
		if (TreeActions.hasRecipes(node)) {
			items.add(Item.of(text(node.hasRecipe() ? "another_recipe" : "choose_recipe"), Icon.LIST, () -> TreeActions.chooseRecipe(node)));
		}

		if (!goal && node.hasRecipe()) {
			if (node.given) {
				items.add(Item.of(text("make"), Icon.HAMMER, () -> {
					session.give(node.path, false);
					Trees.synthetic();
				}));
			} else {
				items.add(Item.of(text("given"), Icon.LOCK, () -> {
					session.give(node.path, true);
					Trees.synthetic();
				}));
				int places = TreeActions.places(session, node);
				if (places > 1) {
					items.add(Item.of(text("given_everywhere", places), Icon.LOCK, () -> TreeActions.fetchEverywhere(session, node)));
				}
			}
		} else if (!goal && TreeActions.fetched(session, node)) {
			items.add(Item.of(text("make"), Icon.HAMMER, () -> TreeActions.makeAgain(session, node)));
		}

		if (!items.isEmpty()) {
			items.add(Item.sep());
		}

		if (node.hasChildren()) {
			boolean folded = session.folded(node.path);
			items.add(Item.of(text(folded ? "unfold" : "fold"), folded ? Icon.CHEVRON_DOWN : Icon.CHEVRON_RIGHT, () -> session.fold(node.path, !folded)));
		}

		items.add(Item.of(text("island"), Icon.SELECTION, () -> {
			session.foldMode = true;
			session.firstPoint = node.path;
		}));
		items.add(Item.sep());
		items.add(Item.of(text("copy_list"), Icon.COPY, () -> TreeActions.copyShoppingList(session)));
		CFHGuiContextMenu.open(x, y, items);
	}

	static void island(TreeSession session, Island island, double x, double y) {
		List<Item> items = new ArrayList<>();
		items.add(
			Item.of(text(island.open ? "island_fold" : "island_unfold"), island.open ? Icon.CHEVRON_RIGHT : Icon.CHEVRON_DOWN, () -> session.open(island, !island.open))
		);
		items.add(Item.danger(text("island_remove"), Icon.TRASH, () -> session.remove(island)));
		CFHGuiContextMenu.open(x, y, items);
	}

	static void tree(TreeSession session, double x, double y, boolean planner) {
		List<Item> items = new ArrayList<>();
		items.add(Item.of(text("rename"), Icon.EDIT, () -> rename(session)));
		if (!planner) {
			items.add(Item.of(text("planner"), Icon.FULLSCREEN, TreeActions::openPlanner));
		}

		items.add(Item.sep());
		items.add(Item.of(text("unfold_all"), Icon.CHEVRON_DOWN, session::unfoldAll));
		items.add(Item.of(text("fold_all"), Icon.CHEVRON_RIGHT, session::foldAll));
		items.add(Item.sep());
		items.add(Item.of(text("copy_list"), Icon.COPY, () -> TreeActions.copyShoppingList(session)));
		items.add(Item.checked(text("crafting_mode"), BoM.craftingMode, () -> TreeActions.craftingMode(!BoM.craftingMode)));
		TeamTrees.menuItems(items);
		items.add(Item.sep());
		items.add(Item.danger(text("clear"), Icon.CLOSE, Trees::clear));
		CFHGuiContextMenu.open(x, y, items);
	}

	static void entry(TreeEntry entry, double x, double y) {
		List<Item> items = new ArrayList<>();
		if (!entry.unreadable()) {
			items.add(Item.of(text("show"), Icon.EYE, () -> Trees.show(entry)));
		}

		items.add(Item.of(text("rename"), Icon.EDIT, () -> rename(entry)));
		if (entry.saved) {
			items.add(Item.of(text(entry.pinned ? "unpin" : "pin"), Icon.PIN, () -> {
				entry.pinned = !entry.pinned;
				Trees.store().touch();
				Trees.store().save();
			}));
		} else {
			items.add(Item.of(text("save"), Icon.BOOKMARK, () -> Trees.save(entry, true)));
		}

		items.add(Item.sep());
		items.add(Item.danger(text("delete"), Icon.TRASH, () -> Trees.delete(entry)));
		CFHGuiContextMenu.open(x, y, items);
	}

	static void recipe(EmiRecipe recipe, double x, double y) {
		List<Item> items = new ArrayList<>();
		items.add(Item.of(text("recipe_start"), Icon.LAN, () -> Tree.start(recipe, false, true)));
		items.add(Item.of(text("recipe_quick"), Icon.BOOKMARK, () -> {
			Tree.start(recipe, false);
			TreeEntry e = Trees.shown();
			if (e != null) {
				Trees.save(e, true);
			}
		}));
		items.add(Item.of(text("recipe_planner"), Icon.FULLSCREEN, () -> Tree.start(recipe, true)));
		CFHGuiContextMenu.open(x, y, items);
	}

	static void rename(TreeSession session) {
		CFHGuiModal.prompt(
			Component.translatable("cfhgui_crafting.tree.rename.title"),
			Component.translatable("cfhgui_crafting.tree.rename.body"),
			session.marks.name.isBlank() ? session.displayName().getString() : session.marks.name,
			session::rename
		);
	}

	static void rename(TreeEntry entry) {
		TreeSession s = entry.session();
		if (s != null) {
			CFHGuiModal.prompt(
				Component.translatable("cfhgui_crafting.tree.rename.title"),
				Component.translatable("cfhgui_crafting.tree.rename.body"),
				s.marks.name.isBlank() ? s.displayName().getString() : s.marks.name,
				name -> {
					s.rename(name);
					if (entry.write()) {
						Trees.store().touch();
					}

					Trees.store().save();
				}
			);
		}
	}

	private TreeMenus() {
	}
}
