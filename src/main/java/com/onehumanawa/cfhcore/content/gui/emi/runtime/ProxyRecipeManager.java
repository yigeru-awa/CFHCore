package com.onehumanawa.cfhcore.content.gui.emi.runtime;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class ProxyRecipeManager {
	private static final Minecraft client = Minecraft.getInstance();
	private static Map<Recipe<?>, ResourceLocation> recipeIds = Map.of();

	public static boolean isAvailable() {
		return getRaw() != null;
	}

	public static RecipeManager getRaw() {
		Level world = client.level;
		return world == null ? null : world.getRecipeManager();
	}

	public static ResourceLocation getId(Recipe<?> recipe) {
		return recipeIds.get(recipe);
	}

	public static boolean hasId(ResourceLocation id) {
		return recipeIds.containsValue(id);
	}

	@Nullable
	public static Recipe<?> getRecipe(ResourceLocation id) {
		RecipeHolder<?> entry = getRecipeEntry(id);
		return entry == null ? null : entry.value();
	}

	@Nullable
	public static RecipeHolder<?> getRecipeEntry(ResourceLocation id) {
		RecipeManager raw = getRaw();
		return raw != null && id != null ? (RecipeHolder)raw.byKey(id).orElse(null) : null;
	}

	public static <I extends RecipeInput, T extends Recipe<I>> Stream<T> streamMatches(RecipeType<T> type, I inventory) {
		RecipeManager raw = getRaw();
		return raw == null ? Stream.empty() : raw.getRecipesFor(type, inventory, client.level).stream().map(e -> (T)e.value());
	}

	public static <I extends RecipeInput, T extends Recipe<I>> List<T> getMatches(RecipeType<T> type, I inventory) {
		return streamMatches(type, inventory).toList();
	}

	@Nullable
	public static <I extends RecipeInput, T extends Recipe<I>> T getFirst(RecipeType<T> type, I inventory) {
		RecipeManager raw = getRaw();
		return raw == null ? null : raw.getRecipeFor(type, inventory, client.level).map(e -> (T)e.value()).orElse(null);
	}

	public static void bakeIds() {
		RecipeManager raw = getRaw();
		if (raw != null) {
			recipeIds = new Reference2ObjectOpenHashMap();

			for (RecipeHolder<?> entry : raw.getRecipes()) {
				recipeIds.put(entry.value(), entry.id());
			}
		}
	}
}
