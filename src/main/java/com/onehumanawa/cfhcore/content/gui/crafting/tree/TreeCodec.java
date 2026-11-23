package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiResolutionRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.serializer.EmiIngredientSerializer;
import com.onehumanawa.cfhcore.content.gui.emi.bom.BoM;
import com.onehumanawa.cfhcore.content.gui.emi.bom.MaterialNode;
import com.onehumanawa.cfhcore.content.gui.emi.bom.MaterialTree;
import com.onehumanawa.cfhcore.content.gui.crafting.CFHGuiCrafting;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class TreeCodec {
	static final Map<ResourceLocation, EmiRecipe> EXTRA = new ConcurrentHashMap<>();

	@Nullable
	public static JsonObject write(MaterialTree tree, TreeMarks marks) {
		EmiRecipe recipe = tree.goal.recipe;
		if (recipe != null && recipe.getId() != null) {
			JsonElement goal = written(tree.goal.ingredient);
			if (goal == null) {
				return null;
			} else {
				JsonObject json = new JsonObject();
				json.add("goal", goal);
				json.addProperty("recipe", recipe.getId().toString());
				if (tree.batches != 1L) {
					json.addProperty("batches", tree.batches);
				}

				JsonArray resolutions = new JsonArray();

				for (Entry<EmiIngredient, EmiRecipe> e : tree.resolutions.entrySet()) {
					JsonElement stack = written(e.getKey());
					if (stack != null) {
						JsonObject o = new JsonObject();
						o.add("stack", stack);
						EmiRecipe chosen = e.getValue();
						if (chosen instanceof EmiResolutionRecipe) {
							EmiResolutionRecipe choice = (EmiResolutionRecipe)chosen;
							JsonElement to = written(choice.stack);
							if (to == null) {
								continue;
							}

							o.add("choice", to);
						} else if (chosen != null) {
							if (chosen.getId() == null) {
								continue;
							}

							o.addProperty("recipe", chosen.getId().toString());
						}

						resolutions.add(o);
					}
				}

				if (!resolutions.isEmpty()) {
					json.add("resolutions", resolutions);
				}

				JsonArray found = new JsonArray();
				found(tree, tree.goal, found, new HashSet<>());
				if (!found.isEmpty()) {
					json.add("found", found);
				}

				marks.write(json);
				return json;
			}
		} else {
			return null;
		}
	}

	private static void found(MaterialTree tree, MaterialNode node, JsonArray into, Set<EmiIngredient> seen) {
		if (node.children != null) {
			for (MaterialNode child : node.children) {
				EmiRecipe recipe = child.recipe;
				if (recipe != null
					&& !(recipe instanceof EmiResolutionRecipe)
					&& recipe.getId() != null
					&& !tree.resolutions.containsKey(child.ingredient)
					&& BoM.getRecipe(child.ingredient) == null
					&& seen.add(child.ingredient)) {
					JsonElement stack = written(child.ingredient);
					if (stack != null) {
						JsonObject o = new JsonObject();
						o.add("stack", stack);
						o.addProperty("recipe", recipe.getId().toString());
						into.add(o);
					}
				}

				found(tree, child, into, seen);
			}
		}
	}

	@Nullable
	private static JsonElement written(EmiIngredient ingredient) {
		try {
			JsonElement el = EmiIngredientSerializer.getSerialized(ingredient.copy().setAmount(1L).setChance(1.0F));
			return el != null && !el.isJsonNull() ? el : null;
		} catch (RuntimeException var2) {
			return null;
		}
	}

	@Nullable
	public static TreeSession read(JsonObject json) {
		try {
			EmiRecipe recipe = recipe(json.get("recipe"));
			if (recipe != null && !recipe.getOutputs().isEmpty()) {
				MaterialTree tree = new MaterialTree(recipe);
				JsonElement batches = json.get("batches");
				if (batches != null && batches.isJsonPrimitive()) {
					tree.batches = Math.max(1L, batches.getAsLong());
				}

				JsonElement resolutions = json.get("resolutions");
				if (resolutions != null && resolutions.isJsonArray()) {
					for (JsonElement el : resolutions.getAsJsonArray()) {
						if (el.isJsonObject()) {
							JsonObject o = el.getAsJsonObject();
							EmiIngredient stack = EmiIngredientSerializer.getDeserialized(o.get("stack"));
							if (!stack.isEmpty()) {
								if (o.has("choice")) {
									EmiIngredient to = EmiIngredientSerializer.getDeserialized(o.get("choice"));
									if (!to.isEmpty() && to.getEmiStacks().size() == 1) {
										tree.resolutions.put(stack, new EmiResolutionRecipe(stack, to.getEmiStacks().get(0)));
									}
								} else if (o.has("recipe")) {
									EmiRecipe chosen = recipe(o.get("recipe"));
									if (chosen != null) {
										tree.resolutions.put(stack, chosen);
									}
								} else {
									tree.resolutions.put(stack, null);
								}
							}
						}
					}
				}

				TreeSession session = new TreeSession(tree, TreeMarks.read(json));
				JsonElement found = json.get("found");
				if (found != null && found.isJsonArray()) {
					for (JsonElement elx : found.getAsJsonArray()) {
						if (elx.isJsonObject()) {
							EmiIngredient stack = EmiIngredientSerializer.getDeserialized(elx.getAsJsonObject().get("stack"));
							EmiRecipe kept = recipe(elx.getAsJsonObject().get("recipe"));
							if (!stack.isEmpty() && kept != null) {
								session.found.put(stack, kept);
							}
						}
					}
				}

				return session.grown();
			} else {
				return null;
			}
		} catch (RuntimeException var11) {
			CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] a saved tree could not be read: {}", var11.toString());
			return null;
		}
	}

	@Nullable
	public static EmiStack goal(JsonObject json) {
		try {
			EmiIngredient goal = EmiIngredientSerializer.getDeserialized(json.get("goal"));
			return goal.isEmpty() ? null : goal.getEmiStacks().get(0);
		} catch (RuntimeException var2) {
			return null;
		}
	}

	@Nullable
	public static String goalRecipe(@Nullable JsonObject json) {
		JsonElement el = json == null ? null : json.get("recipe");
		return el != null && el.isJsonPrimitive() ? el.getAsString() : null;
	}

	@Nullable
	private static EmiRecipe recipe(@Nullable JsonElement id) {
		if (id != null && id.isJsonPrimitive()) {
			ResourceLocation key = ResourceLocation.tryParse(id.getAsString());
			if (key == null) {
				return null;
			} else {
				EmiRecipe recipe = EmiApi.getRecipeManager().getRecipe(key);
				return recipe != null ? recipe : EXTRA.get(key);
			}
		} else {
			return null;
		}
	}

	private TreeCodec() {
	}
}
