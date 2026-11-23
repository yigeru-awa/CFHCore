package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiCraftingRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.VanillaEmiRecipeCategories;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.bom.BoM;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiReloadManager;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.mixin.MouseHandlerAccessor;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Style;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.crafting.CFHGuiCrafting;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

final class TreeShots {
	private static final String PREFIX = "cfhgui_crafting:tree.";
	private static final List<Runnable> PENDING = new ArrayList<>();
	private static final List<Object[]> LATER = new ArrayList<>();

	static void init() {
		if (System.getProperty("cfh.autoScreens") != null || Boolean.getBoolean("cfh.autoWorld")) {
			CFHGuiEvents.CLIENT_TICK_END.register(TreeShots::tick);

			for (String id : new LinkedHashSet<>(queued())) {
				String rest = id.substring("cfhgui_crafting:tree.".length());
				int eq = rest.indexOf(61);
				Runnable action = action(eq < 0 ? rest : rest.substring(0, eq), eq < 0 ? "" : rest.substring(eq + 1));
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

				if (id.startsWith("cfhgui_crafting:tree.")) {
					ids.add(id);
				}
			}
		}

		return ids;
	}

	@Nullable
	private static Runnable action(String verb, String arg) {
		return switch (verb) {
			case "check" -> TreeSelfCheck::run;
			case "goal" -> () -> goal(arg);
			case "sample" -> TreeShots::sample;
			case "chain" -> () -> chain(arg);
			case "big" -> TreeShots::big;
			case "dock" -> () -> TreeDock.INSTANCE.open();
			case "close" -> () -> TreeDock.INSTANCE.close();
			case "planner" -> TreeActions::openPlanner;
			case "select" -> () -> with(arg, (s, row) -> {
				if (row.node != null) {
					s.selected = row.node.path;
				}
			});
			case "click" -> () -> press(number(arg), 0);
			case "rclick" -> () -> press(number(arg), 1);
			case "hover" -> () -> point(number(arg));
			case "fold" -> () -> with(arg, (s, row) -> {
				if (row.node != null) {
					s.fold(row.node.path, !s.folded(row.node.path));
				}
			});
			case "foldmode" -> () -> {
			TreeSession s = Trees.active();
			if (s != null) {
				TreeView.foldMode(s, !s.foldMode);
			}
		};
			case "island" -> () -> island(arg);
			case "open" -> () -> open(number(arg), true);
			case "shut" -> () -> open(number(arg), false);
			case "given" -> () -> with(arg, (s, row) -> {
				if (row.node != null) {
					s.give(row.node.path, !row.node.given);
				}
			});
			case "batches" -> () -> {
			TreeSession s = Trees.active();
			if (s != null) {
				s.batches((long)number(arg));
			}
		};
			case "craft", "craftall" -> () -> with(
				arg,
				(s, row) -> {
					TreeNode node = row.node != null ? row.node : s.fold(row).next;
					boolean all = "craftall".equals(verb);
					CFHGuiCrafting.LOGGER
						.info(
							"[CFHGui Crafting] harness: craft row {}{}: {}",
							new Object[]{arg, all ? " (Shift: every time it is needed)" : "", node != null && TreeActions.craft(s, node, all) ? "made" : "not possible here"}
						);
				}
			);
			case "keep" -> () -> {
			TreeEntry e = Trees.shown();
			if (e != null) {
				Trees.save(e, true);
			}
		};
			case "name" -> () -> {
			TreeSession s = Trees.active();
			if (s != null) {
				s.rename(arg);
			}
		};
			case "rename" -> () -> {
			if (Minecraft.getInstance().screen instanceof PlannerScreen planner) {
				planner.rename();
			}
		};
			case "panel" -> () -> {
			if (Minecraft.getInstance().screen instanceof PlannerScreen planner) {
				planner.panel(!planner.panelOpen());
			}
		};
			case "graph" -> () -> graphView(true);
			case "outline" -> () -> graphView(false);
			case "fit" -> () -> {
			PlannerScreen planner = planner();
			if (planner != null) {
				planner.graph().fit();
			}
		};
			case "zoom" -> () -> {
			PlannerScreen planner = planner();
			if (planner != null) {
				planner.graph().zoomTo(decimal(arg));
			}
		};
			case "pan" -> () -> {
			PlannerScreen planner = planner();
			String[] d = arg.toLowerCase(Locale.ROOT).split("x");
			if (planner != null && d.length == 2) {
				planner.graph().panBy(decimal(d[0]), decimal(d[1]));
			}
		};
			case "graphperf" -> TreeShots::graphPerf;
			case "all" -> () -> {
			int[] at = TreeDock.INSTANCE.quick().placeOf(Math.min(4, Trees.quick().size() + (Trees.recent() != null ? 1 : 0)));
			SavedTreesPopup.open(at[0] - 11, at[1] + 14);
		};
			case "end" -> () -> view().scrollToEnd();
			case "clear" -> () -> {
			Popups.closeAll();
			CFHGuiToasts.clear();
			Trees.clear();
		};
			case "closepopups" -> Popups::closeAll;
			case "drag" -> () -> drag(arg);
			case "chipmenu" -> () -> chipMenu(number(arg));
			case "foldall" -> () -> timed("fold all", TreeSession::foldAll);
			case "unfoldall" -> () -> timed("unfold all", TreeSession::unfoldAll);
			case "perf" -> TreeShots::perf;
			case "style" -> () -> style(arg);
			case "window" -> () -> window(arg);
			default -> null;
		};
	}

	private static void timed(String what, Consumer<TreeSession> action) {
		TreeSession s = Trees.active();
		if (s != null) {
			long began = System.nanoTime();
			action.accept(s);
			int rows = s.rows().size();
			CFHGuiCrafting.LOGGER
				.info(
					"[CFHGui Crafting] harness: {} took {} ms; {} rows now",
					new Object[]{what, String.format(Locale.ROOT, "%.2f", (double)(System.nanoTime() - began) / 1000000.0), rows}
				);
		}
	}

	private static void perf() {
		TreeView view = view();
		int frames = 180;
		long[] sum = new long[]{0L};
		long[] worst = new long[]{0L};
		int[] count = new int[]{0};
		TreeView.probe = nanos -> {
			sum[0] += nanos;
			worst[0] = Math.max(worst[0], nanos);
			view.scrollBy(7);
			if (++count[0] >= 180) {
				TreeView.probe = null;
				CFHGuiCrafting.LOGGER
					.info(
						"[CFHGui Crafting] harness: a tree of {} rows drew in {} ms a frame on average, {} ms at worst, over {} frames of scrolling",
						new Object[]{
							view.rowCount(),
							String.format(Locale.ROOT, "%.3f", (double)sum[0] / 1000000.0 / (double)count[0]),
							String.format(Locale.ROOT, "%.3f", (double)worst[0] / 1000000.0),
							count[0]
						}
					);
			}
		};
	}

	private static void style(String name) {
		CFHGui.config().setStyle("VANILLA".equalsIgnoreCase(name.trim()) ? Style.VANILLA : Style.CFHGUI);
		MenuSlots.refresh();
		Reskin.invalidate();
	}

	private static void window(String size) {
		String[] wh = size.toLowerCase(Locale.ROOT).split("x");
		if (wh.length == 2 && number(wh[0]) > 0 && number(wh[1]) > 0) {
			GLFW.glfwSetWindowSize(Minecraft.getInstance().getWindow().getWindow(), number(wh[0]), number(wh[1]));
		}
	}

	private static int number(String text) {
		try {
			return Integer.parseInt(text.trim());
		} catch (NumberFormatException var2) {
			return 0;
		}
	}

	private static float decimal(String text) {
		try {
			return Float.parseFloat(text.trim());
		} catch (NumberFormatException var2) {
			return 1.0F;
		}
	}

	private static void with(String index, TreeShots.OnRow then) {
		TreeSession s = Trees.active();
		if (s != null) {
			List<TreeRow> rows = s.rows();
			int i = number(index);
			if (i >= 0 && i < rows.size()) {
				then.run(s, rows.get(i));
			} else {
				CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: the tree has no row {} (it has {})", i, rows.size());
			}
		}
	}

	private static TreeView view() {
		return Minecraft.getInstance().screen instanceof PlannerScreen planner ? planner.view() : TreeDock.INSTANCE.view();
	}

	@Nullable
	private static PlannerScreen planner() {
		return Minecraft.getInstance().screen instanceof PlannerScreen planner ? planner : null;
	}

	@Nullable
	private static int[] placeOf(int index) {
		TreeSession s = Trees.active();
		if (s != null && index >= 0 && index < s.rows().size()) {
			PlannerScreen planner = planner();
			String id = s.rows().get(index).id;
			return planner != null && PlannerScreen.graphMode() ? planner.graph().placeOf(id) : view().placeOf(id);
		} else {
			return null;
		}
	}

	private static void graphView(boolean on) {
		PlannerScreen planner = planner();
		if (planner != null) {
			planner.graph(on);
		} else if (TreeConfig.get().plannerGraph != on) {
			TreeConfig.get().plannerGraph = on;
			TreeConfig.save();
		}
	}

	private static void graphPerf() {
		PlannerScreen planner = planner();
		if (planner != null && PlannerScreen.graphMode()) {
			TreeGraph graph = planner.graph();
			int frames = 180;
			long[] sum = new long[]{0L};
			long[] worst = new long[]{0L};
			int[] count = new int[]{0};
			TreeView.probe = nanos -> {
				sum[0] += nanos;
				worst[0] = Math.max(worst[0], nanos);
				graph.sweep(9.0F);
				if (++count[0] >= 180) {
					TreeView.probe = null;
					CFHGuiCrafting.LOGGER
						.info(
							"[CFHGui Crafting] harness: the graph of {} nodes drew in {} ms a frame on average, {} ms at worst, over {} frames of panning at {}%",
							new Object[]{
								graph.nodeCount(),
								String.format(Locale.ROOT, "%.3f", (double)sum[0] / 1000000.0 / (double)count[0]),
								String.format(Locale.ROOT, "%.3f", (double)worst[0] / 1000000.0),
								count[0],
								Math.round(graph.zoom() * 100.0F)
							}
						);
				}
			};
		} else {
			CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: graphperf needs the planner's graph open");
		}
	}

	private static void press(int index, int button) {
		int[] at = placeOf(index);
		Minecraft mc = Minecraft.getInstance();
		if (at != null && mc.screen != null) {
			if (mc.screen instanceof PlannerScreen) {
				mc.screen.mouseClicked((double)at[0], (double)at[1], button);
				if (mc.screen != null) {
					mc.screen.mouseReleased((double)at[0], (double)at[1], button);
				}
			} else {
				TreeDock.INSTANCE.mouseClicked((double)at[0], (double)at[1], button);
				TreeDock.INSTANCE.mouseReleased((double)at[0], (double)at[1], button);
			}
		}
	}

	private static void point(int index) {
		int[] at = placeOf(index);
		if (at != null) {
			pointAt(at);
		}
	}

	private static void pointAt(int[] at) {
		Minecraft mc = Minecraft.getInstance();
		double scale = (double)mc.getWindow().getScreenWidth() / (double)mc.getWindow().getGuiScaledWidth();
		((MouseHandlerAccessor)mc.mouseHandler).cfhgui$setXpos((double)at[0] * scale);
		((MouseHandlerAccessor)mc.mouseHandler).cfhgui$setYpos((double)at[1] * scale);
	}

	private static void drag(String ends) {
		String[] ab = ends.split("-");
		if (ab.length == 2) {
			QuickAccess quick = TreeDock.INSTANCE.quick();
			int[] from = quick.placeOf(number(ab[0]));
			int[] to = quick.placeOf(number(ab[1]));
			pointAt(from);
			later(2, () -> {
				TreeDock.INSTANCE.mouseClicked((double)from[0], (double)from[1], 0);
				TreeDock.INSTANCE.mouseDragged((double)(from[0] + 8), (double)from[1], 0, 8.0, 0.0);
				TreeDock.INSTANCE.mouseDragged((double)to[0], (double)to[1], 0, (double)(to[0] - from[0] - 8), 0.0);
				pointAt(to);
			});
			later(
				5,
				() -> {
					TreeDock.INSTANCE.mouseReleased((double)to[0], (double)to[1], 0);
					CFHGuiCrafting.LOGGER
						.info(
							"[CFHGui Crafting] harness: chip {} dragged to place {}; quick access now {}",
							new Object[]{ab[0], ab[1], Trees.quick().stream().map(e -> e.name().getString()).toList()}
						);
				}
			);
		}
	}

	private static void chipMenu(int slot) {
		int[] at = TreeDock.INSTANCE.quick().placeOf(slot);
		pointAt(at);
		later(2, () -> TreeDock.INSTANCE.mouseClicked((double)at[0], (double)at[1], 1));
	}

	private static void later(int ticks, Runnable action) {
		LATER.add(new Object[]{ticks, action});
	}

	private static void island(String ends) {
		TreeSession s = Trees.active();
		String[] ab = ends.split("-");
		if (s != null && ab.length == 2) {
			List<TreeRow> rows = s.rows();
			int a = number(ab[0]);
			int b = number(ab[1]);
			if (a >= 0 && b >= 0 && a < rows.size() && b < rows.size() && rows.get(a).node != null && rows.get(b).node != null) {
				TreeRows.Refusal refusal = s.island(rows.get(a).node.path, rows.get(b).node.path);
				CFHGuiCrafting.LOGGER
					.info("[CFHGui Crafting] harness: island between rows {} and {}: {}", new Object[]{a, b, refusal == null ? "made" : "refused, " + refusal});
			}
		}
	}

	private static void open(int index, boolean open) {
		TreeSession s = Trees.active();
		if (s != null && index >= 0 && index < s.marks.islands.size()) {
			s.open(s.marks.islands.get(index), open);
		}
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

	@Nullable
	private static EmiRecipe recipeOf(EmiStack stack) {
		EmiRecipe preferred = BoM.getRecipe(stack);
		if (preferred != null) {
			return preferred;
		} else {
			EmiRecipe first = null;

			for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipesByOutput(stack)) {
				if (recipe.supportsRecipeTree() && !recipe.getOutputs().isEmpty() && recipe.getOutputs().get(0).isEqual(stack)) {
					if (recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING) {
						return recipe;
					}

					if (first == null) {
						first = recipe;
					}
				}
			}

			return first;
		}
	}

	private static void goal(String item) {
		EmiStack stack = stack(item);
		EmiRecipe recipe = stack == null ? null : recipeOf(stack);
		if (recipe == null) {
			CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: nothing makes {}", item);
		} else {
			Tree.start(recipe, false);
			TreeSession s = Trees.active();
			if (s != null) {
				CFHGuiCrafting.LOGGER
					.info(
						"[CFHGui Crafting] harness: tree of {}: {} places, {} steps",
						new Object[]{item, s.outline().all.size(), s.outline().all.stream().filter(TreeNode::step).count()}
					);
				List<TreeRow> rows = s.rows();

				for (int i = 0; i < rows.size(); i++) {
					TreeRow row = rows.get(i);
					String what = row.node != null
						? TreeActions.name(row.node.ingredient()).getString() + (row.node.step() ? "" : " (raw)")
						: "island of " + row.hidden.size();
					CFHGuiCrafting.LOGGER.info("[CFHGui Crafting] harness:   row {}: {}{}", new Object[]{i, "  ".repeat(row.depth), what});
				}
			}
		}
	}

	private static void sample() {
		EmiRecipe furnace = EmiApi.getRecipeManager().getRecipe(ResourceLocation.withDefaultNamespace("blast_furnace"));
		if (furnace == null) {
			CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: the viewer does not know the blast furnace's recipe");
		} else {
			TreeSession s = Trees.start(furnace);
			resolve(s, Items.FURNACE, "furnace");
			resolve(s, Items.SMOOTH_STONE, "smooth_stone");
			resolve(s, Items.STONE, "stone");
			resolve(s, Items.IRON_INGOT, "iron_ingot_from_smelting_raw_iron");
			TreeDock.INSTANCE.open();
		}
	}

	private static void chain(String arg) {
		String[] parts = arg.split("\\|");
		EmiStack stack = stack(parts[0]);
		EmiRecipe recipe = stack == null ? null : recipeOf(stack);
		if (recipe == null) {
			CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: nothing makes {}", parts[0]);
		} else {
			TreeSession s = Trees.start(recipe);

			for (int i = 1; i < parts.length; i++) {
				String name = parts[i].trim();
				ResourceLocation id = name.contains(":") ? ResourceLocation.tryParse(name) : ResourceLocation.withDefaultNamespace(name);
				EmiRecipe step = id == null ? null : EmiApi.getRecipeManager().getRecipe(id);
				Item made = step != null && !step.getOutputs().isEmpty() ? (Item)BuiltInRegistries.ITEM.getOptional(step.getOutputs().get(0).getId()).orElse(null) : null;
				if (made == null) {
					CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: the viewer does not know the recipe {}", name);
				} else {
					s.tree.addResolution(EmiStack.of(made), step);
				}
			}

			TreeDock.INSTANCE.open();
			CFHGuiCrafting.LOGGER
				.info(
					"[CFHGui Crafting] harness: chain of {}: {} places, {} steps",
					new Object[]{parts[0], s.outline().all.size(), s.outline().all.stream().filter(TreeNode::step).count()}
				);
		}
	}

	private static void resolve(TreeSession s, Item item, String recipeId) {
		EmiRecipe recipe = EmiApi.getRecipeManager().getRecipe(ResourceLocation.withDefaultNamespace(recipeId));
		if (recipe != null) {
			s.tree.addResolution(EmiStack.of(item), recipe);
		} else {
			CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: the viewer does not know the recipe {}", recipeId);
		}
	}

	private static void big() {
		List<Item> items = new ArrayList<>();

		for (Item item : BuiltInRegistries.ITEM) {
			if (item != Items.AIR) {
				items.add(item);
			}

			if (items.size() >= 1001) {
				break;
			}
		}

		int steps = Math.min(500, (items.size() - 1) / 2);
		List<EmiRecipe> recipes = new ArrayList<>();

		for (int i = 0; i < steps; i++) {
			List<EmiIngredient> inputs = List.of(EmiStack.of((ItemLike)items.get(2 * i + 1), 2L), EmiStack.of((ItemLike)items.get(2 * i + 2)));
			ResourceLocation id = ResourceLocation.fromNamespaceAndPath("cfhgui_crafting", "/tree/big/" + i);
			EmiRecipe recipe = new EmiCraftingRecipe(inputs, EmiStack.of((ItemLike)items.get(i)), id);
			recipes.add(recipe);
			TreeCodec.EXTRA.put(id, recipe);
		}

		TreeSession s = Trees.start(recipes.get(0));

		for (int i = 1; i < steps; i++) {
			s.tree.resolutions.put(EmiStack.of((ItemLike)items.get(i)), recipes.get(i));
		}

		for (int i = steps; i <= 2 * steps; i++) {
			s.tree.resolutions.put(EmiStack.of((ItemLike)items.get(i)), null);
		}

		s.tree.recalculate();
		s.rename(Component.translatable("cfhgui_crafting.tree.dev.big").getString());
		TreeDock.INSTANCE.open();
		CFHGuiCrafting.LOGGER
			.info(
				"[CFHGui Crafting] harness: the big tree has {} places, {} of them steps", s.outline().all.size(), s.outline().all.stream().filter(TreeNode::step).count()
			);
	}

	private static void whenLoaded(String id, Runnable action) {
		PENDING.add(() -> {
			try {
				action.run();
			} catch (RuntimeException var3) {
				CFHGuiCrafting.LOGGER.error("[CFHGui Crafting] harness: {} failed", id, var3);
			}
		});
	}

	private static void tick() {
		if (!LATER.isEmpty()) {
			List<Runnable> now = new ArrayList<>();
			Iterator<Object[]> it = LATER.iterator();

			while (it.hasNext()) {
				Object[] entry = it.next();
				int left = (Integer)entry[0] - 1;
				if (left > 0) {
					entry[0] = left;
				} else {
					it.remove();
					now.add((Runnable)entry[1]);
				}
			}

			for (Runnable action : now) {
				try {
					action.run();
				} catch (RuntimeException var4) {
					CFHGuiCrafting.LOGGER.error("[CFHGui Crafting] harness: a later step failed", var4);
				}
			}
		}

		if (EmiReloadManager.isLoaded() && !PENDING.isEmpty()) {
			List<Runnable> due = new ArrayList<>(PENDING);
			PENDING.clear();

			for (Runnable action : due) {
				action.run();
			}
		}
	}

	private TreeShots() {
	}

	private interface OnRow {
		void run(TreeSession var1, TreeRow var2);
	}
}
