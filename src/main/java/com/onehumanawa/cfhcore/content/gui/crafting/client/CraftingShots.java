package com.onehumanawa.cfhcore.content.gui.crafting.client;

import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.handler.EmiCraftContext;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.bom.BoM;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarType;
import com.onehumanawa.cfhcore.content.gui.emi.platform.EmiClient;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiRecipeFiller;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiFavorites;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiReloadManager;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiSidebars;
import com.onehumanawa.cfhcore.content.gui.emi.screen.ConfigScreen;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.crafting.CFHGuiCrafting;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

public final class CraftingShots {
	private static final String PREFIX = "cfhgui_crafting:";
	private static final List<Runnable> PENDING = new ArrayList<>();
	private static boolean cleared;

	public static void init() {
		if (System.getProperty("cfh.autoScreens") != null || Boolean.getBoolean("cfh.autoWorld")) {
			CFHGuiEvents.CLIENT_TICK_END.register(CraftingShots::tick);
			screen("inventory", parent -> {
				Minecraft mc = Minecraft.getInstance();
				return (Screen)(mc.player == null ? parent : new InventoryScreen(mc.player));
			});
			screen("config", ConfigScreen::new);
			screen("crafting_table", parent -> {
				openCraftingTable();
				return parent;
			});

			for (String id : new LinkedHashSet<>(queued())) {
				String rest = id.substring("cfhgui_crafting:".length());
				int slash = rest.indexOf(47);
				if (slash >= 0) {
					Runnable action = action(rest.substring(0, slash), rest.substring(slash + 1));
					if (action == null) {
						CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: no such shot: {}", id);
					} else {
						CoreActions.SCREEN_FACTORIES.put(id, parent -> {
							whenLoaded(id, action);
							return Minecraft.getInstance().screen;
						});
					}
				}
			}
		}
	}

	private static void screen(String name, Function<Screen, Screen> factory) {
		CoreActions.SCREEN_FACTORIES.put("cfhgui_crafting:" + name, factory);
	}

	private static List<String> queued() {
		List<String> ids = new ArrayList<>();

		for (String property : new String[]{"cfh.autoScreens", "cfh.autoWorldScreens"}) {
			for (String entry : System.getProperty(property, "").split(",")) {
				String id = entry.trim();
				int hash = id.indexOf(35);
				if (hash >= 0) {
					id = id.substring(0, hash);
				}

				int at = id.lastIndexOf(64);
				if (at > 0) {
					id = id.substring(0, at);
				}

				if (id.startsWith("cfhgui_crafting:")) {
					ids.add(id);
				}
			}
		}

		return ids;
	}

	@Nullable
	private static Runnable action(String verb, String arg) {
		return switch (verb) {
			case "recipes" -> () -> {
			EmiStack stack = stack(arg);
			if (stack != null) {
				EmiApi.displayRecipes(stack);
			}
		};
			case "uses" -> () -> {
			EmiStack stack = stack(arg);
			if (stack != null) {
				EmiApi.displayUses(stack);
			}
		};
			case "tree" -> () -> tree(arg);
			case "fill" -> () -> fill(arg);
			case "favorite" -> () -> {
			EmiStack stack = stack(arg);
			if (stack != null) {
				EmiFavorites.addFavorite(stack);
				EmiScreenManager.repopulatePanels(SidebarType.FAVORITES);
			}
		};
			case "search" -> () -> EmiApi.setSearchText(arg);
			case "click" -> () -> press(arg, 0);
			case "rclick" -> () -> press(arg, 1);
			case "key" -> () -> key(arg);
			default -> null;
		};
	}

	@Nullable
	private static EmiStack stack(String item) {
		ResourceLocation id = ResourceLocation.tryParse(item.trim());
		Item found = id == null ? null : (Item)BuiltInRegistries.ITEM.getOptional(id).orElse(null);
		if (found == null) {
			CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: no such item: {}", item);
			return null;
		} else {
			return EmiStack.of(found);
		}
	}

	private static List<EmiRecipe> recipesOf(EmiStack stack) {
		List<EmiRecipe> recipes = new ArrayList<>();
		EmiRecipe preferred = BoM.getRecipe(stack);
		if (preferred != null) {
			recipes.add(preferred);
		}

		for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipesByOutput(stack)) {
			if (recipe != preferred && recipe.getOutputs().stream().anyMatch(o -> o.isEqual(stack))) {
				recipes.add(recipe);
			}
		}

		return recipes;
	}

	private static void tree(String item) {
		EmiStack stack = stack(item);
		if (stack != null) {
			List<EmiRecipe> recipes = recipesOf(stack);
			if (recipes.isEmpty()) {
				CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: nothing makes {}", item);
			} else {
				BoM.setGoal(recipes.get(0));
				EmiApi.viewRecipeTree();
			}
		}
	}

	private static void fill(String item) {
		EmiStack stack = stack(item);
		AbstractContainerScreen<?> container = EmiApi.getHandledScreen();
		if (stack != null) {
			if (container == null) {
				CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: fill/{} needs an open container", item);
			} else {
				for (EmiRecipe recipe : recipesOf(stack)) {
					if (EmiRecipeFiller.getFirstValidHandler(recipe, container) != null) {
						boolean filled = EmiRecipeFiller.performFill(recipe, container, EmiCraftContext.Type.FILL_BUTTON, EmiCraftContext.Destination.NONE, 1);
						CFHGuiCrafting.LOGGER
							.info(
								"[CFHGui Crafting] harness: fill {} ({}) into {}: {}, {}",
								new Object[]{
									item,
									recipe.getId(),
									container.getClass().getSimpleName(),
									filled ? "done" : "not possible (are the ingredients there?)",
									EmiClient.onServer ? "the server fills it (packet)" : "the client fills it (clicks)"
								}
							);
						return;
					}
				}

				CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: no recipe of {} can be filled into {}", item, container.getClass().getSimpleName());
			}
		}
	}

	private static void press(String at, int button) {
		Minecraft mc = Minecraft.getInstance();
		String[] xy = at.trim().split("x");
		if (mc.screen != null && xy.length == 2) {
			double x;
			double y;
			try {
				x = Double.parseDouble(xy[0]) * (double)mc.getWindow().getGuiScaledWidth();
				y = Double.parseDouble(xy[1]) * (double)mc.getWindow().getGuiScaledHeight();
			} catch (NumberFormatException var9) {
				CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: not a place on the screen: {}", at);
				return;
			}

			if (!(mc.screen instanceof AbstractContainerScreen) || !EmiScreenManager.mouseClicked(x, y, button)) {
				mc.screen.mouseClicked(x, y, button);
			}

			if (mc.screen != null) {
				if (!(mc.screen instanceof AbstractContainerScreen) || !EmiScreenManager.mouseReleased(x, y, button)) {
					mc.screen.mouseReleased(x, y, button);
				}
			}
		}
	}

	private static void key(String code) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen != null) {
			int key;
			try {
				key = Integer.parseInt(code.trim());
			} catch (NumberFormatException var4) {
				CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: not a key code: {}", code);
				return;
			}

			if (!(mc.screen instanceof AbstractContainerScreen) || !EmiScreenManager.keyPressed(key, 0, 0)) {
				mc.screen.keyPressed(key, 0, 0);
			}
		}
	}

	private static void openCraftingTable() {
		Minecraft mc = Minecraft.getInstance();
		IntegratedServer server = mc.getSingleplayerServer();
		if (server != null && mc.player != null) {
			UUID who = mc.player.getUUID();
			server.execute(() -> {
				ServerPlayer player = server.getPlayerList().getPlayer(who);
				if (player != null) {
					ServerLevel level = player.serverLevel();
					BlockPos pos = player.blockPosition().relative(player.getDirection());
					if (!level.getBlockState(pos).is(Blocks.CRAFTING_TABLE)) {
						level.setBlockAndUpdate(pos, Blocks.CRAFTING_TABLE.defaultBlockState());
					}

					MenuProvider menu = level.getBlockState(pos).getMenuProvider(level, pos);
					if (menu != null) {
						player.openMenu(menu);
					}
				}
			});
		} else {
			CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: crafting_table needs a world of this game's own (-PautoWorld=true)");
		}
	}

	private static void whenLoaded(String id, Runnable action) {
		Runnable safe = () -> {
			try {
				action.run();
			} catch (RuntimeException var3) {
				CFHGuiCrafting.LOGGER.error("[CFHGui Crafting] harness: {} failed", id, var3);
			}
		};
		if (EmiReloadManager.isLoaded() && PENDING.isEmpty()) {
			clearOnce();
			safe.run();
		} else {
			PENDING.add(safe);
		}
	}

	private static void tick() {
		if (EmiReloadManager.isLoaded()) {
			clearOnce();
			if (!PENDING.isEmpty()) {
				List<Runnable> due = new ArrayList<>(PENDING);
				PENDING.clear();

				for (Runnable action : due) {
					action.run();
				}
			}
		}
	}

	private static void clearOnce() {
		if (!cleared) {
			cleared = true;
			EmiFavorites.favorites.clear();
			EmiSidebars.lookupHistory.clear();
			EmiSidebars.craftHistory.clear();
			BoM.tree = null;
			EmiApi.setSearchText("");
			EmiScreenManager.repopulatePanels(SidebarType.FAVORITES);
		}
	}

	private CraftingShots() {
	}
}
