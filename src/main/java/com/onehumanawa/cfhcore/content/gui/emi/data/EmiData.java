package com.onehumanawa.cfhcore.content.gui.emi.data;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiInfoRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipeSorting;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiWorldInteractionRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.render.EmiTexture;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.serializer.EmiIngredientSerializer;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class EmiData {
	public static Map<String, EmiRecipeCategoryProperties> categoryPriorities = Map.of();
	public static List<Predicate<EmiRecipe>> recipeFilters = List.of();
	public static List<Supplier<IndexStackData>> stackData = List.of();
	public static List<Supplier<EmiAlias>> aliases = List.of();
	public static List<Supplier<EmiRecipe>> recipes = List.of();

	public static void init(Consumer<EmiResourceReloadListener> register) {
		register.accept(new RecipeDefaultLoader());
		register.accept(new EmiTagExclusionsLoader());
		register.accept(new EmiDataLoader<>(EmiPort.id("emi:category_properties"), "category/properties", Maps::newHashMap, (map, json, id) -> {
			for (String k : json.keySet()) {
				if (GsonHelper.isObjectNode(json, k)) {
					EmiRecipeCategoryProperties props = (EmiRecipeCategoryProperties)map.computeIfAbsent(k, s -> new EmiRecipeCategoryProperties());
					JsonObject val = json.getAsJsonObject(k);
					if (GsonHelper.isNumberValue(val, "order")) {
						props.order = val.get("order").getAsInt();
					}

					if (GsonHelper.isObjectNode(val, "icon")) {
						JsonObject icon = val.getAsJsonObject("icon");
						if (GsonHelper.isStringValue(icon, "texture")) {
							props.icon = () -> new EmiTexture(EmiPort.id(GsonHelper.getAsString(icon, "texture")), 0, 0, 16, 16, 16, 16, 16, 16);
						} else if (GsonHelper.isStringValue(icon, "stack")) {
							props.icon = () -> EmiIngredientSerializer.getDeserialized(icon.get("stack"));
						}
					}

					if (GsonHelper.isObjectNode(val, "simplified_icon")) {
						JsonObject icon = val.getAsJsonObject("simplified_icon");
						if (GsonHelper.isStringValue(icon, "texture")) {
							props.simplified = () -> new EmiTexture(EmiPort.id(GsonHelper.getAsString(icon, "texture")), 0, 0, 16, 16, 16, 16, 16, 16);
						} else if (GsonHelper.isStringValue(icon, "stack")) {
							props.simplified = () -> EmiIngredientSerializer.getDeserialized(icon.get("stack"));
						}
					}

					if (GsonHelper.isStringValue(val, "sort")) {
						String var10 = GsonHelper.getAsString(val, "sort");
						switch (var10) {
							case "none":
								props.sort = EmiRecipeSorting.none();
								break;
							case "input_then_output":
								props.sort = EmiRecipeSorting.compareInputThenOutput();
								break;
							case "output_then_input":
								props.sort = EmiRecipeSorting.compareOutputThenInput();
								break;
							case "identifier":
								props.sort = EmiRecipeSorting.identifier();
						}
					}
				}
			}
		}, map -> categoryPriorities = (java.util.Map<String, EmiRecipeCategoryProperties>) (java.util.Map<?, ?>) map));
		register.accept(new EmiDataLoader<java.util.List<java.util.function.Predicate<EmiRecipe>>>(EmiPort.id("emi:recipe_filters"), "recipe/filters", Lists::newArrayList, (list, json, oid) -> {
			for (JsonElement el : GsonHelper.getAsJsonArray(json, "filters", new JsonArray())) {
				if (el.isJsonObject()) {
					JsonObject obj = el.getAsJsonObject();
					List<Predicate<EmiRecipe>> predicates = Lists.newArrayList();
					if (GsonHelper.isStringValue(obj, "id")) {
						String id = GsonHelper.getAsString(obj, "id");
						if (id.startsWith("/") && id.endsWith("/")) {
							Pattern pat = Pattern.compile(id.substring(1, id.length() - 1));
							predicates.add(r -> {
								String rid = r.getId() == null ? "null" : r.getId().toString();
								return pat.matcher(rid).find();
							});
						} else {
							predicates.add(r -> {
								String rid = r.getId() == null ? "null" : r.getId().toString();
								return rid.equals(id);
							});
						}
					}

					if (GsonHelper.isStringValue(obj, "category")) {
						String id = GsonHelper.getAsString(obj, "category");
						if (id.startsWith("/") && id.endsWith("/")) {
							Pattern pat = Pattern.compile(id.substring(1, id.length() - 1));
							predicates.add(r -> pat.matcher(r.getCategory().getId().toString()).find());
						} else {
							predicates.add(r -> r.getCategory().getId().toString().equals(id));
						}
					}

					if (predicates.size() <= 1) {
						list.addAll(predicates);
					} else {
						list.add(r -> {
							for (Predicate<EmiRecipe> p : predicates) {
								if (!p.test(r)) {
									return false;
								}
							}

							return true;
						});
					}
				}
			}
		}, list -> recipeFilters = (java.util.List<java.util.function.Predicate<EmiRecipe>>) (java.util.List<?>) list));
		register.accept(new EmiDataLoader<java.util.List<java.util.function.Supplier<IndexStackData>>>(EmiPort.id("emi:index_stacks"), "index/stacks", Lists::newArrayList, (list, json, oid) -> list.add(() -> {
				List<IndexStackData.Added> added = Lists.newArrayList();
				List<EmiIngredient> removed = Lists.newArrayList();
				List<IndexStackData.Filter> filters = Lists.newArrayList();
				if (GsonHelper.isArrayNode(json, "added")) {
					for (JsonElement el : json.getAsJsonArray("added")) {
						if (el.isJsonObject()) {
							JsonObject obj = el.getAsJsonObject();
							EmiIngredient stack = EmiIngredientSerializer.getDeserialized(obj.get("stack"));
							EmiIngredient after = EmiStack.EMPTY;
							if (obj.has("after")) {
								after = EmiIngredientSerializer.getDeserialized(obj.get("after"));
							}

							added.add(new IndexStackData.Added(stack, after));
						}
					}
				}

				if (GsonHelper.isArrayNode(json, "removed")) {
					for (JsonElement elx : json.getAsJsonArray("removed")) {
						removed.add(EmiIngredientSerializer.getDeserialized(elx));
					}
				}

				if (GsonHelper.isArrayNode(json, "filters")) {
					for (JsonElement elx : json.getAsJsonArray("filters")) {
						if (GsonHelper.isStringValue(elx)) {
							String id = elx.getAsString();
							if (id.startsWith("/") && id.endsWith("/")) {
								Pattern pat = Pattern.compile(id.substring(1, id.length() - 1));
								filters.add(new IndexStackData.Filter(s -> pat.matcher(s).find()));
							} else {
								filters.add(new IndexStackData.Filter(s -> s.equals(id)));
							}
						}
					}
				}

				boolean disable = GsonHelper.getAsBoolean(json, "disable", false);
				return new IndexStackData(disable, added, removed, filters);
			}), list -> stackData = (java.util.List<java.util.function.Supplier<IndexStackData>>) (java.util.List<?>) list));
		register.accept(
			new EmiDataLoader<java.util.List<java.util.function.Supplier<EmiAlias>>>(
				EmiPort.id("emi:aliases"),
				"aliases",
				Lists::newArrayList,
				(list, json, id) -> {
					if (GsonHelper.isArrayNode(json, "aliases")) {
						for (JsonElement el : json.getAsJsonArray("aliases")) {
							if (el.isJsonObject()) {
								JsonObject obj = el.getAsJsonObject();
								list.add(
									() -> new EmiAlias(
											getArrayOrSingleton(obj, "stacks").map(e -> EmiIngredientSerializer.getDeserialized(e)).toList(),
											getArrayOrSingleton(obj, "text").map(e -> e.getAsString()).toList()
										)
								);
							}
						}
					}
				},
				list -> aliases = (java.util.List<java.util.function.Supplier<EmiAlias>>) (java.util.List<?>) list
			)
		);
		register.accept(
			new EmiDataLoader<java.util.List<java.util.function.Supplier<EmiRecipe>>>(
				EmiPort.id("emi:recipe_additions"),
				"recipe/additions",
				Lists::newArrayList,
				(list, json, oid) -> {
					String s = GsonHelper.getAsString(json, "type", "");
					ResourceLocation id = EmiPort.id("emi:/generated/" + oid.getPath());
					if (s.equals("emi:info")) {
						list.add(
							() -> new EmiInfoRecipe(
									getArrayOrSingleton(json, "stacks").map(EmiIngredientSerializer::getDeserialized).toList(),
									getArrayOrSingleton(json, "text").map(t -> (net.minecraft.network.chat.Component)EmiPort.translatable(t.getAsString())).toList(),
									id
								)
						);
					} else if (s.equals("emi:world_interaction")) {
						list.add(() -> {
							EmiWorldInteractionRecipe.Builder builder = EmiWorldInteractionRecipe.builder();
							getArrayOrSingleton(json, "left").map(EmiIngredientSerializer::getDeserialized).forEach(i -> builder.leftInput(i));
							getArrayOrSingleton(json, "right").map(EmiIngredientSerializer::getDeserialized).forEach(i -> builder.rightInput(i, false));
							getArrayOrSingleton(json, "output").map(EmiIngredientSerializer::getDeserialized).forEach(i -> builder.output(i.getEmiStacks().get(0)));
							builder.id(id);
							return builder.build();
						});
					}
				},
				list -> recipes = (java.util.List<java.util.function.Supplier<EmiRecipe>>) (java.util.List<?>) list
			)
		);
	}

	private static Stream<JsonElement> getArrayOrSingleton(JsonObject json, String key) {
		return GsonHelper.isArrayNode(json, key) ? StreamSupport.stream(json.getAsJsonArray(key).spliterator(), false) : Stream.of(json.get(key));
	}
}
