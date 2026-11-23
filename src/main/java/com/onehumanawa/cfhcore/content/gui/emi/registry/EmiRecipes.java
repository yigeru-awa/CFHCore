package com.onehumanawa.cfhcore.content.gui.emi.registry;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.EmiUtil;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipeCategory;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipeDecorator;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipeManager;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipeSorting;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.ListEmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.config.EmiConfig;
import com.onehumanawa.cfhcore.content.gui.emi.data.EmiData;
import com.onehumanawa.cfhcore.content.gui.emi.data.EmiRecipeCategoryProperties;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiHidden;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiLog;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiReloadLog;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.ProxyRecipeManager;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.dev.EmiDev;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class EmiRecipes {
	public static volatile EmiRecipes.Worker activeWorker = null;
	public static EmiRecipeManager manager = EmiRecipes.Manager.EMPTY;
	public static List<Consumer<Consumer<EmiRecipe>>> lateRecipes = Lists.newArrayList();
	public static List<Predicate<EmiRecipe>> invalidators = Lists.newArrayList();
	public static List<EmiRecipeCategory> categories = Lists.newArrayList();
	private static Map<EmiRecipeCategory, List<EmiIngredient>> workstations = Maps.newHashMap();
	private static List<EmiRecipe> recipes = Lists.newArrayList();
	public static Map<EmiStack, List<EmiRecipe>> byWorkstation = Maps.newHashMap();
	public static List<EmiRecipeDecorator> decorators = Lists.newArrayList();

	public static void clear() {
		setWorker(null);
		lateRecipes.clear();
		invalidators.clear();
		categories.clear();
		workstations.clear();
		recipes.clear();
		byWorkstation.clear();
		decorators.clear();
		manager = EmiRecipes.Manager.EMPTY;
		ProxyRecipeManager.bakeIds();
	}

	public static void bake() {
		long start = System.currentTimeMillis();
		recipes.addAll(EmiData.recipes.stream().map(r -> r.get()).toList());
		categories.sort((a, b) -> EmiRecipeCategoryProperties.getOrder(a) - EmiRecipeCategoryProperties.getOrder(b));
		invalidators.addAll(EmiData.recipeFilters);
		invalidators.add(r -> {
			for (EmiIngredient i : Iterables.concat(r.getInputs(), r.getOutputs(), r.getCatalysts())) {
				if (EmiHidden.isDisabled(i)) {
					return true;
				}
			}

			return false;
		});
		List<EmiRecipe> filtered = recipes.stream().filter(r -> {
			for (Predicate<EmiRecipe> predicate : invalidators) {
				if (predicate.test(r)) {
					return false;
				}
			}

			return true;
		}).toList();
		Map<EmiRecipeCategory, List<EmiIngredient>> filteredWorkstations = Maps.newHashMap();

		for (Entry<EmiRecipeCategory, List<EmiIngredient>> entry : workstations.entrySet()) {
			List<EmiIngredient> w = entry.getValue().stream().filter(s -> !EmiHidden.isDisabled(s)).toList();
			if (!w.isEmpty()) {
				filteredWorkstations.put(entry.getKey(), w);
			}
		}

		manager = new EmiRecipes.Manager(categories, filteredWorkstations, filtered, false);
		setWorker(new EmiRecipes.Worker(categories, filteredWorkstations, filtered));
		EmiLog.info("Baked " + recipes.size() + " recipes in " + (System.currentTimeMillis() - start) + "ms");
	}

	public static void addCategory(EmiRecipeCategory category) {
		categories.add(category);
	}

	public static void addWorkstation(EmiRecipeCategory category, EmiIngredient workstation) {
		workstations.computeIfAbsent(category, k -> Lists.newArrayList()).add(workstation);
	}

	public static void addRecipe(EmiRecipe recipe) {
		recipes.add(recipe);
	}

	private static synchronized void setWorker(EmiRecipes.Worker worker) {
		activeWorker = worker;
		if (worker != null) {
			new Thread(activeWorker).start();
		}
	}

	private static class Manager implements EmiRecipeManager {
		public static final EmiRecipeManager EMPTY = new EmiRecipes.Manager();
		private final List<EmiRecipeCategory> categories;
		private final Map<EmiRecipeCategory, List<EmiIngredient>> workstations;
		private final List<EmiRecipe> recipes;
		private Map<EmiStack, List<EmiRecipe>> byInput = new Object2ObjectOpenCustomHashMap(new EmiStackList.ComparisonHashStrategy());
		private Map<EmiStack, List<EmiRecipe>> byOutput = new Object2ObjectOpenCustomHashMap(new EmiStackList.ComparisonHashStrategy());
		private Map<EmiRecipeCategory, List<EmiRecipe>> byCategory = Maps.newHashMap();
		private Map<ResourceLocation, EmiRecipe> byId = Maps.newHashMap();

		private Manager() {
			this.categories = List.of();
			this.workstations = Map.of();
			this.recipes = List.of();
		}

		public Manager(List<EmiRecipeCategory> categories, Map<EmiRecipeCategory, List<EmiIngredient>> workstations, List<EmiRecipe> recipes, boolean doSort) {
			this.categories = categories.stream().distinct().toList();
			this.workstations = workstations;
			this.recipes = List.copyOf(recipes);
			Object2IntMap<ResourceLocation> duplicateIds = new Object2IntOpenHashMap();
			Set<ResourceLocation> incorrectIds = new ObjectArraySet();

			for (EmiRecipe recipe : recipes) {
				ResourceLocation id = recipe.getId();
				EmiRecipeCategory category = recipe.getCategory();
				if (!categories.contains(category)) {
					EmiReloadLog.warn("Recipe " + id + " loaded with unregistered category: " + category.getId());
				}

				if (EmiConfig.logNonTagIngredients && recipe.supportsRecipeTree()) {
					Set<EmiIngredient> seen = new ObjectArraySet(0);

					for (EmiIngredient ingredient : recipe.getInputs()) {
						if (ingredient instanceof ListEmiIngredient && !seen.contains(ingredient)) {
							EmiReloadLog.warn("Recipe " + recipe.getId() + " uses non-tag ingredient: " + ingredient);
							seen.add(ingredient);
						}
					}
				}

				this.byCategory.computeIfAbsent(category, a -> Lists.newArrayList()).add(recipe);
				if (id != null) {
					if (this.byId.containsKey(id)) {
						duplicateIds.put(id, duplicateIds.getOrDefault(id, 1) + 1);
					} else {
						this.byId.put(id, recipe);
					}

					if (EmiConfig.devMode && !id.getPath().startsWith("/") && !ProxyRecipeManager.hasId(id)) {
						incorrectIds.add(id);
					}
				}
			}

			if (EmiConfig.devMode) {
				ObjectIterator var20 = duplicateIds.keySet().iterator();

				while (var20.hasNext()) {
					ResourceLocation idx = (ResourceLocation)var20.next();
					EmiReloadLog.warn(duplicateIds.getInt(idx) + " recipes loaded with the same id: " + idx);
				}

				for (ResourceLocation idx : incorrectIds) {
					EmiReloadLog.warn("Recipe " + idx + " not present in recipe manager. Consider prefixing its path with '/' if it is synthetic.");
				}
			}

			Map<EmiStack, List<EmiRecipe>> byInput = this.byInput;
			Map<EmiStack, List<EmiRecipe>> byOutput = this.byOutput;

			for (EmiRecipeCategory categoryx : this.byCategory.keySet()) {
				String key = EmiUtil.translateId("emi.category.", categoryx.getId());
				if (categoryx.getName().equals(EmiPort.translatable(key)) && !I18n.exists(key)) {
					EmiReloadLog.warn("Untranslated recipe category " + categoryx.getId());
				}

				List<EmiRecipe> cRecipes = this.byCategory.get(categoryx);
				Comparator<EmiRecipe> sort = EmiRecipeCategoryProperties.getSort(categoryx);
				if (doSort && sort != EmiRecipeSorting.none()) {
					cRecipes = cRecipes.stream().sorted(sort).collect(Collectors.toList());
					EmiRecipeSorter.clear();
				}

				this.byCategory.put(categoryx, cRecipes);

				for (EmiRecipe recipe : cRecipes) {
					for (EmiIngredient ingredientx : recipe.getInputs()) {
						for (EmiStack stack : ingredientx.getEmiStacks()) {
							slate$index(byInput, stack, recipe);
						}
					}

					for (EmiIngredient ingredientx : recipe.getCatalysts()) {
						for (EmiStack stack : ingredientx.getEmiStacks()) {
							slate$index(byInput, stack, recipe);
						}
					}

					for (EmiStack stack : recipe.getOutputs()) {
						slate$index(byOutput, stack, recipe);
					}
				}
			}

			for (Entry<EmiStack, List<EmiRecipe>> entry : byInput.entrySet()) {
				entry.setValue(List.copyOf(entry.getValue()));
			}

			for (Entry<EmiStack, List<EmiRecipe>> entry : byOutput.entrySet()) {
				entry.setValue(List.copyOf(entry.getValue()));
			}

			for (EmiRecipeCategory categoryx : workstations.keySet()) {
				List<EmiIngredient> w = workstations.getOrDefault(categoryx, null);
				if (w != null) {
					workstations.put(categoryx, w.stream().distinct().toList());
				} else {
					EmiReloadLog.warn("Recipe category illegally self-mutated during recipe bake, causing recipe loss: " + categoryx);
				}
			}

			Map<EmiStack, List<List<EmiRecipe>>> served = Maps.newHashMap();

			for (Entry<EmiRecipeCategory, List<EmiRecipe>> entry : this.byCategory.entrySet()) {
				for (EmiIngredient ingredientx : workstations.getOrDefault(entry.getKey(), List.of())) {
					for (EmiStack stack : ingredientx.getEmiStacks()) {
						List<List<EmiRecipe>> lists = served.computeIfAbsent(stack, s -> Lists.newArrayListWithCapacity(1));
						if (!lists.contains(entry.getValue())) {
							lists.add(entry.getValue());
						}
					}
				}
			}

			for (Entry<EmiStack, List<List<EmiRecipe>>> entry : served.entrySet()) {
				List<List<EmiRecipe>> lists = entry.getValue();
				if (lists.size() == 1) {
					EmiRecipes.byWorkstation.put(entry.getKey(), Collections.unmodifiableList(lists.get(0)));
				} else {
					List<EmiRecipe> all = Lists.newArrayList();

					for (List<EmiRecipe> list : lists) {
						all.addAll(list);
					}

					EmiRecipes.byWorkstation.put(entry.getKey(), all);
				}
			}

			if (EmiConfig.devMode) {
				EmiDev.duplicateRecipeIds = duplicateIds.keySet();
				EmiDev.incorrectRecipeIds = incorrectIds;
			}
		}

		private static void slate$index(Map<EmiStack, List<EmiRecipe>> index, EmiStack stack, EmiRecipe recipe) {
			List<EmiRecipe> list = index.get(stack);
			if (list == null) {
				list = new ArrayList<>(2);
				index.put(stack.copy(), list);
			} else if (list.get(list.size() - 1) == recipe) {
				return;
			}

			list.add(recipe);
		}

		@Override
		public List<EmiRecipeCategory> getCategories() {
			return this.categories;
		}

		@Override
		public List<EmiIngredient> getWorkstations(EmiRecipeCategory category) {
			return this.workstations.getOrDefault(category, List.of());
		}

		@Override
		public List<EmiRecipe> getRecipes() {
			return this.recipes;
		}

		@Override
		public List<EmiRecipe> getRecipes(EmiRecipeCategory category) {
			return this.byCategory.getOrDefault(category, List.of());
		}

		@Nullable
		@Override
		public EmiRecipe getRecipe(ResourceLocation id) {
			return this.byId.getOrDefault(id, null);
		}

		@Override
		public List<EmiRecipe> getRecipesByInput(EmiStack stack) {
			return this.byInput.getOrDefault(stack, List.of());
		}

		@Override
		public List<EmiRecipe> getRecipesByOutput(EmiStack stack) {
			return this.byOutput.getOrDefault(stack, List.of());
		}
	}

	private static class Worker implements Runnable {
		private List<EmiRecipeCategory> categories;
		private Map<EmiRecipeCategory, List<EmiIngredient>> workstations;
		private List<EmiRecipe> recipes;

		public Worker(List<EmiRecipeCategory> categories, Map<EmiRecipeCategory, List<EmiIngredient>> workstations, List<EmiRecipe> recipes) {
			this.categories = categories;
			this.workstations = workstations;
			this.recipes = recipes;
		}

		@Override
		public void run() {
			long startTime = System.currentTimeMillis();
			EmiRecipes.Manager manager = new EmiRecipes.Manager(this.categories, this.workstations, this.recipes, true);
			if (EmiRecipes.activeWorker == this) {
				long endTime = System.currentTimeMillis();
				EmiLog.info("Baked recipes after reload in " + (endTime - startTime) + "ms");
				EmiRecipes.manager = manager;
			}

			EmiRecipes.setWorker(null);
		}
	}
}
