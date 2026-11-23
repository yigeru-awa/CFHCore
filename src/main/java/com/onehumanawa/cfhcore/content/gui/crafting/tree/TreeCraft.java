package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiPlayerInventory;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.handler.EmiCraftContext;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.handler.EmiRecipeHandler;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiRecipeFiller;
import com.onehumanawa.cfhcore.content.gui.crafting.CFHGuiCrafting;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.jetbrains.annotations.Nullable;

public final class TreeCraft {
	private static boolean complained;

	private static boolean safely(TreeCraft.Question question) {
		try {
			return question.ask();
		} catch (Throwable var2) {
			if (!complained) {
				complained = true;
				CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] a recipe handler threw while the tree asked it what can be made (said once)", var2);
			}

			return false;
		}
	}

	public static boolean make(EmiRecipe recipe, long times, boolean take) {
		AbstractContainerScreen<?> container = EmiApi.getHandledScreen();
		if (container == null) {
			return false;
		} else {
			int amount = (int)Math.max(1L, Math.min(2147483647L, times));

			try {
				return EmiRecipeFiller.performFill(
					recipe, container, EmiCraftContext.Type.CRAFTABLE, take ? EmiCraftContext.Destination.INVENTORY : EmiCraftContext.Destination.NONE, amount
				);
			} catch (Throwable var7) {
				CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] filling {} from the tree failed", recipe.getId(), var7);
				return false;
			}
		}
	}

	private TreeCraft() {
	}

	public static final class Look {
		private final EmiPlayerInventory inventory;
		@Nullable
		private final AbstractContainerScreen<?> container;
		private final Map<EmiRecipe, Boolean> once = new IdentityHashMap<>();
		private final Map<EmiRecipe, Boolean> here = new IdentityHashMap<>();
		private final Map<EmiRecipeHandler<?>, EmiPlayerInventory> handlerInventories = new IdentityHashMap<>();

		Look(EmiPlayerInventory inventory, @Nullable AbstractContainerScreen<?> container) {
			this.inventory = inventory;
			this.container = container;
		}

		public boolean ready(EmiRecipe recipe) {
			return this.once.computeIfAbsent(recipe, r -> TreeCraft.safely(() -> this.inventory.canCraft(r)));
		}

		public boolean ready(EmiRecipe recipe, long times) {
			return times <= 1L ? this.ready(recipe) : this.ready(recipe) && TreeCraft.safely(() -> this.inventory.canCraft(recipe, times));
		}

		public boolean here(EmiRecipe recipe) {
			return this.container == null ? false : this.here.computeIfAbsent(recipe, r -> TreeCraft.safely(() -> {
					EmiRecipeHandler handler = EmiRecipeFiller.getFirstValidHandler(r, this.container);
					if (handler != null && handler.supportsRecipe(r)) {
						EmiPlayerInventory inv = this.handlerInventories.computeIfAbsent(handler, h -> ((EmiRecipeHandler)h).getInventory(this.container));
						return handler.canCraft(r, new EmiCraftContext(this.container, inv, EmiCraftContext.Type.CRAFTABLE));
					} else {
						return false;
					}
				}));
		}
	}

	private interface Question {
		boolean ask();
	}
}
