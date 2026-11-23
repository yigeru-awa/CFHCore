package com.onehumanawa.cfhcore.content.gui.quests.server.adv;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.jetbrains.annotations.Nullable;

final class AdvCriteria {
	private static final Map<String, String> COUNTED = Map.of(
		"minecraft:slept_in_bed",
		"minecraft:custom/minecraft:sleep_in_bed",
		"minecraft:villager_trade",
		"minecraft:custom/minecraft:traded_with_villager",
		"minecraft:bred_animals",
		"minecraft:custom/minecraft:animals_bred",
		"minecraft:enchanted_item",
		"minecraft:custom/minecraft:enchant_item",
		"minecraft:hero_of_the_village",
		"minecraft:custom/minecraft:raid_win"
	);
	private final MinecraftServer server;
	private final Provider registries;

	AdvCriteria(MinecraftServer server) {
		this.server = server;
		this.registries = server.registryAccess();
	}

	AdvCriteria.Piece read(Criterion<?> criterion) {
		String trigger = triggerId(criterion);

		try {
			JsonElement json = (JsonElement)Criterion.CODEC.encodeStart(this.registries.createSerializationContext(JsonOps.INSTANCE), criterion).result().orElse(null);
			if (json != null && json.isJsonObject()) {
				JsonObject conditions = json.getAsJsonObject().has("conditions") && json.getAsJsonObject().get("conditions").isJsonObject()
					? json.getAsJsonObject().getAsJsonObject("conditions")
					: new JsonObject();
				AdvCriteria.Piece piece = this.read(trigger, conditions);
				return piece == null ? AdvCriteria.Piece.cannot(trigger) : piece;
			} else {
				return AdvCriteria.Piece.cannot(trigger);
			}
		} catch (RuntimeException var6) {
			return AdvCriteria.Piece.cannot(trigger);
		}
	}

	static String triggerId(Criterion<?> criterion) {
		ResourceLocation id = BuiltInRegistries.TRIGGER_TYPES.getKey(criterion.trigger());
		return id == null ? "unknown" : id.toString();
	}

	@Nullable
	private AdvCriteria.Piece read(String trigger, JsonObject c) {
		switch (trigger) {
			case "minecraft:inventory_changed":
				return this.inventory(c);
			case "minecraft:recipe_crafted":
				return this.crafted(c);
			case "minecraft:player_killed_entity":
				return this.killed(c);
			case "minecraft:location":
			case "minecraft:tick":
				return this.place(c);
			case "minecraft:changed_dimension":
				if (!only(c, "to")) {
					return null;
				} else {
					if ("minecraft:overworld".equals(c.get("to").getAsString())) {
						return null;
					}

					Task t = task("location");
					t.dimension = c.get("to").getAsString();
					return AdvCriteria.Piece.of(t);
				}
			case "minecraft:placed_block":
				return this.placed(c);
			default:
				String stat = COUNTED.get(trigger);
				if (stat != null && c.size() <= 0) {
					Task t = task("stat");
					t.stat = stat;
					return AdvCriteria.Piece.of(t);
				} else {
					return null;
				}
		}
	}

	private static Task task(String type) {
		Task t = new Task();
		t.type = type;
		return t;
	}

	private static boolean only(JsonObject o, String required) {
		return o.size() == 1 && o.has(required);
	}

	@Nullable
	private AdvCriteria.Piece inventory(JsonObject c) {
		for (String key : c.keySet()) {
			if (!key.equals("items")) {
				return null;
			}
		}

		if (!c.has("items")) {
			return AdvCriteria.Piece.NOTHING;
		} else if (!c.get("items").isJsonArray()) {
			return null;
		} else {
			List<Task> tasks = new ArrayList<>();

			for (JsonElement el : c.getAsJsonArray("items")) {
				if (!el.isJsonObject()) {
					return null;
				}

				Task t = this.item(el.getAsJsonObject(), "item");
				if (t == null) {
					return null;
				}

				tasks.add(t);
			}

			return tasks.isEmpty() ? AdvCriteria.Piece.NOTHING : new AdvCriteria.Piece(tasks, "");
		}
	}

	@Nullable
	private Task item(JsonObject p, String type) {
		Set<String> known = Set.of("items", "count", "components", "predicates");

		for (String key : p.keySet()) {
			if (!known.contains(key)) {
				return null;
			}
		}

		List<String> heads = new ArrayList<>();
		if (!p.has("items")) {
			heads.add("*");
		} else if (!names(p.get("items"), heads)) {
			return null;
		}

		long count = 1L;
		if (p.has("count")) {
			JsonElement n = p.get("count");
			if (!n.isJsonObject() || !only(n.getAsJsonObject(), "min")) {
				return null;
			}

			count = n.getAsJsonObject().get("min").getAsLong();
			if (count < 1L) {
				return null;
			}
		}

		StringBuilder brackets = new StringBuilder();
		if (this.brackets(p, "components", '=', brackets) && this.brackets(p, "predicates", '~', brackets)) {
			StringBuilder spec = new StringBuilder();

			for (String head : heads) {
				if (spec.length() > 0) {
					spec.append(" | ");
				}

				spec.append(head);
				if (brackets.length() > 0) {
					spec.append('[').append((CharSequence)brackets).append(']');
				}
			}

			Task t = task(type);
			t.item = spec.toString();
			t.count = count;
			return t;
		} else {
			return null;
		}
	}

	private boolean brackets(JsonObject p, String key, char sign, StringBuilder into) {
		if (!p.has(key)) {
			return true;
		} else if (!p.get(key).isJsonObject()) {
			return false;
		} else {
			for (Entry<String, JsonElement> e : p.getAsJsonObject(key).entrySet()) {
				Tag value = (Tag)JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, e.getValue());
				if (value == null) {
					return false;
				}

				if (into.length() > 0) {
					into.append(',');
				}

				into.append(e.getKey()).append(sign).append(value);
			}

			return true;
		}
	}

	private static boolean names(JsonElement set, List<String> into) {
		if (set.isJsonPrimitive()) {
			into.add(set.getAsString());
			return true;
		} else if (set.isJsonArray() && !set.getAsJsonArray().isEmpty()) {
			for (JsonElement el : set.getAsJsonArray()) {
				if (!el.isJsonPrimitive()) {
					return false;
				}

				into.add(el.getAsString());
			}

			return true;
		} else {
			return false;
		}
	}

	@Nullable
	private static String one(JsonElement set) {
		List<String> all = new ArrayList<>();
		return names(set, all) && all.size() == 1 ? all.get(0) : null;
	}

	@Nullable
	private AdvCriteria.Piece crafted(JsonObject c) {
		if (!only(c, "recipe_id")) {
			return null;
		} else {
			ResourceLocation id = ResourceLocation.tryParse(c.get("recipe_id").getAsString());
			RecipeHolder<?> holder = id == null ? null : (RecipeHolder)this.server.getRecipeManager().byKey(id).orElse(null);
			if (holder == null) {
				return null;
			} else {
				Recipe<?> recipe = holder.value();
				boolean fixed = recipe instanceof CraftingRecipe && !recipe.isSpecial()
					|| recipe instanceof AbstractCookingRecipe
					|| recipe instanceof SingleItemRecipe
					|| recipe instanceof SmithingTransformRecipe;
				if (!fixed) {
					return null;
				} else {
					ItemStack result = recipe.getResultItem(this.registries);
					if (result.isEmpty()) {
						return null;
					} else {
						Task t = task("craft");
						t.item = BuiltInRegistries.ITEM.getKey(result.getItem()).toString();
						return AdvCriteria.Piece.of(t);
					}
				}
			}
		}
	}

	@Nullable
	private AdvCriteria.Piece killed(JsonObject c) {
		for (String key : c.keySet()) {
			if (!key.equals("entity")) {
				return null;
			}
		}

		Task t = task("kill");
		if (c.has("entity")) {
			JsonObject entity = self(c.get("entity"));
			if (entity == null) {
				return null;
			}

			if (entity.size() > 0) {
				if (!only(entity, "type")) {
					return null;
				}

				String type = one(entity.get("type"));
				if (type == null) {
					return null;
				}

				t.entity = type;
			}
		}

		return AdvCriteria.Piece.of(t);
	}

	@Nullable
	private static JsonObject self(JsonElement predicate) {
		if (predicate.isJsonObject()) {
			return predicate.getAsJsonObject();
		} else if (!predicate.isJsonArray()) {
			return null;
		} else {
			JsonArray list = predicate.getAsJsonArray();
			if (list.isEmpty()) {
				return new JsonObject();
			} else if (list.size() == 1 && list.get(0).isJsonObject()) {
				JsonObject condition = list.get(0).getAsJsonObject();
				if ("minecraft:entity_properties".equals(string(condition, "condition")) && "this".equals(string(condition, "entity"))) {
					for (String key : condition.keySet()) {
						if (!key.equals("condition") && !key.equals("entity") && !key.equals("predicate")) {
							return null;
						}
					}

					if (!condition.has("predicate")) {
						return new JsonObject();
					} else {
						return condition.get("predicate").isJsonObject() ? condition.getAsJsonObject("predicate") : null;
					}
				} else {
					return null;
				}
			} else {
				return null;
			}
		}
	}

	private static String string(JsonObject o, String key) {
		return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
	}

	@Nullable
	private AdvCriteria.Piece place(JsonObject c) {
		if (c.size() == 0) {
			return AdvCriteria.Piece.NOTHING;
		} else if (!only(c, "player")) {
			return null;
		} else {
			JsonObject player = self(c.get("player"));
			if (player == null) {
				return null;
			} else if (player.size() == 0) {
				return AdvCriteria.Piece.NOTHING;
			} else if (only(player, "location") && player.get("location").isJsonObject()) {
				JsonObject where = player.getAsJsonObject("location");
				if (where.size() == 0) {
					return AdvCriteria.Piece.NOTHING;
				} else {
					Task t = task("location");

					for (Entry<String, JsonElement> e : where.entrySet()) {
						String var7 = e.getKey();
						switch (var7) {
							case "biomes":
								String biome = one(e.getValue());
								if (biome == null) {
									return null;
								}

								t.biome = biome;
								break;
							case "structures":
								String structure = one(e.getValue());
								if (structure == null) {
									return null;
								}

								t.structure = structure;
								break;
							case "dimension":
								t.dimension = e.getValue().getAsString();
								break;
							case "position":
								if (!box(e.getValue(), t)) {
									return null;
								}
								break;
							default:
								return null;
						}
					}

					return AdvCriteria.Piece.of(t);
				}
			} else {
				return null;
			}
		}
	}

	private static boolean box(JsonElement position, Task into) {
		if (!position.isJsonObject()) {
			return false;
		} else {
			JsonObject p = position.getAsJsonObject();
			int[] from = new int[3];
			int[] size = new int[3];
			String[] axes = new String[]{"x", "y", "z"};

			for (int i = 0; i < 3; i++) {
				JsonElement range = p.get(axes[i]);
				if (range == null) {
					return false;
				}

				double min;
				double max;
				if (range.isJsonPrimitive()) {
					min = max = range.getAsDouble();
				} else {
					if (!range.isJsonObject() || !range.getAsJsonObject().has("min") || !range.getAsJsonObject().has("max")) {
						return false;
					}

					min = range.getAsJsonObject().get("min").getAsDouble();
					max = range.getAsJsonObject().get("max").getAsDouble();
				}

				from[i] = (int)Math.floor(min);
				size[i] = Math.max(1, (int)Math.floor(max) - from[i] + 1);
			}

			into.x = from[0];
			into.y = from[1];
			into.z = from[2];
			into.sizeX = size[0];
			into.sizeY = size[1];
			into.sizeZ = size[2];
			return true;
		}
	}

	@Nullable
	private AdvCriteria.Piece placed(JsonObject c) {
		if (c.size() == 0) {
			return AdvCriteria.Piece.of(task("place"));
		} else if (only(c, "location") && c.get("location").isJsonArray()) {
			JsonArray conditions = c.getAsJsonArray("location");
			if (conditions.size() == 1 && conditions.get(0).isJsonObject()) {
				JsonObject condition = conditions.get(0).getAsJsonObject();
				if ("minecraft:block_state_property".equals(string(condition, "condition")) && condition.size() == 2 && !string(condition, "block").isEmpty()) {
					Task t = task("place");
					t.block = string(condition, "block");
					return AdvCriteria.Piece.of(t);
				} else {
					return null;
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	static record Piece(List<Task> tasks, String why) {
		static final AdvCriteria.Piece NOTHING = new AdvCriteria.Piece(List.of(), "");

		static AdvCriteria.Piece of(Task task) {
			return new AdvCriteria.Piece(List.of(task), "");
		}

		static AdvCriteria.Piece cannot(String trigger) {
			return new AdvCriteria.Piece(List.of(), trigger);
		}

		boolean said() {
			return this.why.isEmpty();
		}
	}
}
