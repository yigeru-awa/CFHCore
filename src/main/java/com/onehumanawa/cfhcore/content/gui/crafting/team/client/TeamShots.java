package com.onehumanawa.cfhcore.content.gui.crafting.team.client;

import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.VanillaEmiRecipeCategories;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.bom.BoM;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarSide;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarType;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiFavorite;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiFavorites;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiReloadManager;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.mixin.MouseHandlerAccessor;
import com.onehumanawa.cfhcore.content.gui.crafting.CFHGuiCrafting;
import com.onehumanawa.cfhcore.content.gui.crafting.team.Pin;
import com.onehumanawa.cfhcore.content.gui.crafting.team.TeamPinsServer;
import com.onehumanawa.cfhcore.content.gui.crafting.team.TeamSelfCheck;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

final class TeamShots {
	private static final String PREFIX = "cfhgui_crafting:team.";
	private static final List<Runnable> PENDING = new ArrayList<>();
	private static int ready;

	static void init() {
		if (System.getProperty("cfh.autoScreens") != null || Boolean.getBoolean("cfh.autoWorld")) {
			CFHGuiEvents.CLIENT_TICK_END.register(TeamShots::tick);

			for (String id : new LinkedHashSet<>(queued())) {
				String rest = id.substring("cfhgui_crafting:team.".length());
				int eq = rest.indexOf(61);
				Runnable action = action(eq < 0 ? rest : rest.substring(0, eq), eq < 0 ? "" : rest.substring(eq + 1));
				if (action == null) {
					CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: no such shot: {}", id);
				} else {
					CoreActions.SCREEN_FACTORIES.put(id, parent -> {
						PENDING.add(() -> {
							try {
								action.run();
							} catch (RuntimeException var3x) {
								CFHGuiCrafting.LOGGER.error("[CFHGui Crafting] harness: {} failed", id, var3x);
							}
						});
						return Minecraft.getInstance().screen;
					});
				}
			}
		}
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

				if (id.startsWith("cfhgui_crafting:team.")) {
					ids.add(id);
				}
			}
		}

		return ids;
	}

	@Nullable
	private static Runnable action(String verb, String arg) {
		return switch (verb) {
			case "check" -> TeamSelfCheck::run;
			case "clear" -> TeamShots::clear;
			case "pin" -> () -> {
			EmiRecipe r = recipeOf(arg);
			if (r != null) {
				TeamPinsClient.pinRecipe(r);
			}
		};
			case "fakepin" -> () -> fakePin(arg);
			case "pintree" -> TeamTrees::press;
			case "page" -> () -> EmiScreenManager.focusSidebarType(SidebarType.TEAM);
			case "favorites" -> () -> EmiScreenManager.focusSidebarType(SidebarType.FAVORITES);
			case "hover" -> () -> {
			int[] at = stackAt(number(arg));
			if (at != null) {
				pointAt(at[0], at[1]);
			}
		};
			case "menu" -> () -> menu(number(arg));
			case "favmenu" -> () -> favoriteMenu(number(arg));
			case "hoverpin" -> () -> {
			if (PinButton.lastX >= 0) {
				pointAt(PinButton.lastX, PinButton.lastY);
			}
		};
			case "log" -> TeamShots::log;
			default -> null;
		};
	}

	private static void tick() {
		if (!EmiReloadManager.isLoaded()) {
			ready = 0;
		} else if (ready < 4) {
			ready++;
		} else if (!PENDING.isEmpty()) {
			List<Runnable> due = new ArrayList<>(PENDING);
			PENDING.clear();

			for (Runnable action : due) {
				action.run();
			}
		}
	}

	private static int number(String text) {
		try {
			return Integer.parseInt(text.trim());
		} catch (NumberFormatException var2) {
			return 0;
		}
	}

	@Nullable
	private static EmiRecipe recipeOf(String item) {
		ResourceLocation id = ResourceLocation.tryParse(item.trim());
		Item found = id == null ? null : (Item)BuiltInRegistries.ITEM.getOptional(id).orElse(null);
		if (found == null) {
			CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: no such item: {}", item);
			return null;
		} else {
			EmiStack stack = EmiStack.of(found);
			EmiRecipe preferred = BoM.getRecipe(stack);
			if (preferred != null) {
				return preferred;
			} else {
				EmiRecipe first = null;

				for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipesByOutput(stack)) {
					if (recipe.getId() != null && !recipe.getOutputs().isEmpty() && recipe.getOutputs().get(0).isEqual(stack)) {
						if (recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING) {
							return recipe;
						}

						if (first == null) {
							first = recipe;
						}
					}
				}

				if (first == null) {
					CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: nothing makes {}", item);
				}

				return first;
			}
		}
	}

	private static void clear() {
		List<Pin> pins = new ArrayList<>(TeamPinsClient.pins());

		for (Pin pin : pins) {
			TeamPinsClient.ask("unpin", pin.id, new byte[0], 0, null);
		}

		CFHGuiCrafting.LOGGER.info("[CFHGui Crafting] harness: {} pin(s) of the team taken off", pins.size());
	}

	private static void fakePin(String item) {
		EmiRecipe recipe = recipeOf(item);
		Minecraft mc = Minecraft.getInstance();
		IntegratedServer server = mc.getSingleplayerServer();
		if (recipe != null && recipe.getId() != null && server != null && mc.player != null) {
			UUID player = mc.player.getUUID();
			String id = recipe.getId().toString();
			server.execute(() -> CFHGuiCrafting.LOGGER.info("[CFHGui Crafting] harness: Alex pins {}: {}", id, TeamPinsServer.devPin(player, id, "Alex")));
		} else {
			CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: fakepin needs a world of this game's own and a recipe of {}", item);
		}
	}

	@Nullable
	private static int[] stackAt(int n) {
		EmiScreenManager.SidebarPanel panel = EmiScreenManager.getPanelFor(SidebarSide.LEFT);
		return panel != null && panel.space != null && n >= 0 && n < panel.space.pageSize ? new int[]{panel.space.getRawX(n) + 9, panel.space.getRawY(n) + 9} : null;
	}

	private static void menu(int n) {
		int[] at = stackAt(n);
		if (at != null) {
			pointAt(at[0], at[1]);
			EmiScreenManager.mouseClicked((double)at[0], (double)at[1], 1);
			EmiScreenManager.mouseReleased((double)at[0], (double)at[1], 1);
		}
	}

	private static void favoriteMenu(int n) {
		int[] at = stackAt(n);
		if (at != null && n < EmiFavorites.favorites.size()) {
			pointAt(at[0], at[1]);
			EmiFavorite fav = EmiFavorites.favorites.get(n);
			TeamMenus.favorite(fav, (double)at[0], (double)at[1]);
		}
	}

	private static void pointAt(int x, int y) {
		Minecraft mc = Minecraft.getInstance();
		double scale = (double)mc.getWindow().getScreenWidth() / (double)mc.getWindow().getGuiScaledWidth();
		((MouseHandlerAccessor)mc.mouseHandler).cfhgui$setXpos((double)x * scale);
		((MouseHandlerAccessor)mc.mouseHandler).cfhgui$setYpos((double)y * scale);
	}

	private static void log() {
		List<String> pins = new ArrayList<>();

		for (Pin pin : TeamPinsClient.pins()) {
			pins.add(pin.kind + " " + pin.recipe + " by " + pin.byName);
		}

		List<String> favorites = new ArrayList<>();

		for (EmiFavorite fav : EmiFavorites.favorites) {
			EmiIngredient stack = fav.getStack();
			favorites.add(
				(stack.isEmpty() ? "?" : stack.getEmiStacks().get(0).getId().toString())
					+ (fav.getRecipe() != null && fav.getRecipe().getId() != null ? " (" + fav.getRecipe().getId() + ")" : "")
			);
		}

		CFHGuiCrafting.LOGGER
			.info(
				"[CFHGui Crafting] harness: team {} of world {}: {} pin(s) {}; the Team page shows {}; favourites {}; quick access has {} of the team's tree(s)",
				new Object[]{TeamPinsClient.team(), TeamPinsClient.world(), pins.size(), pins, TeamPage.stacks().size(), favorites, TeamTrees.quick().size()}
			);
	}

	private TeamShots() {
	}
}
