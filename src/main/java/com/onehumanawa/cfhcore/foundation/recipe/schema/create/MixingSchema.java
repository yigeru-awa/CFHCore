package com.onehumanawa.cfhcore.foundation.recipe.schema.create;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.foundation.ingredient.FluidSpec;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeContext;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeValidationContext;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.FluidComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.IntegerComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.StringComponent;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RecipeSchemaType("create:mixing")
public class MixingSchema {

    public static final MixingListComponent INGREDIENTS = new MixingListComponent();
    public static final MixingListComponent RESULTS = new MixingListComponent();

    public static final RecipeKey<MixingList> INGREDIENT_KEY =
            RecipeKey.builder(INGREDIENTS).name("ingredients").input().alwaysWrite().build();
    public static final RecipeKey<MixingList> RESULT_KEY =
            RecipeKey.builder(RESULTS).name("results").output().alwaysWrite().build();
    public static final RecipeKey<Integer> PROCESSING_TIME_KEY =
            RecipeKey.builder(new IntegerComponent("processing_time", 1, 36000, 100))
                    .name("processing_time")
                    .optional()
                    .defaultValue(100)
                    .build();
    public static final RecipeKey<String> HEAT_REQUIREMENT_KEY =
            RecipeKey.builder(new StringComponent("heat_requirement", "none", Set.of("none", "heated", "superheated")))
                    .name("heat_requirement")
                    .optional()
                    .defaultValue("none")
                    .build();

    public static final RecipeSchema INSTANCE = create();

    public static class MixingList {
        public final List<String> items = new ArrayList<>();
        public final List<FluidSpec> fluids = new ArrayList<>();

        final List<Integer> itemSlots = new ArrayList<>();
        final List<Integer> fluidSlots = new ArrayList<>();
        JsonArray source;

        public boolean isEmpty() {
            return items.isEmpty() && fluids.isEmpty();
        }

        public void clear() {
            items.clear();
            fluids.clear();
            itemSlots.clear();
            fluidSlots.clear();
            source = null;
        }
    }

    public static class MixingListComponent implements RecipeComponent<MixingList> {

        @Override
        public MixingList read(JsonObject json, RecipeContext context) {
            MixingList list = new MixingList();
            JsonElement element = locate(json, context);
            if (element == null || !element.isJsonArray()) {
                return list;
            }

            JsonArray array = element.getAsJsonArray();
            list.source = array;
            for (int i = 0; i < array.size(); i++) {
                JsonElement entry = array.get(i);
                if (!entry.isJsonObject()) continue;
                JsonObject object = entry.getAsJsonObject();
                if (isFluid(object)) {
                    FluidSpec fluid = FluidComponent.INSTANCE.read(object, RecipeContext.empty());
                    if (!fluid.isEmpty()) {
                        list.fluids.add(fluid);
                        list.fluidSlots.add(i);
                    }
                    continue;
                }
                String id = ItemComponent.readId(object);
                if (id != null) {
                    list.items.add(id);
                    list.itemSlots.add(i);
                }
            }

            return list;
        }

        @Override
        public void write(JsonObject json, MixingList value, RecipeContext context) {
            if (value == null || value.isEmpty()) return;

            boolean useId = useId(context);
            String field = targetField(json, context);
            if (field == null) {
                field = useId ? "results" : "ingredients";
            }

            JsonArray source = value.source;
            if (source != null && source.size() == value.itemSlots.size() + value.fluidSlots.size()) {
                for (int i = 0; i < value.items.size(); i++) {
                    String item = value.items.get(i);
                    if (item == null || item.isEmpty()) continue;
                    JsonObject target = ItemComponent.firstObject(source.get(value.itemSlots.get(i)));
                    if (target != null) {
                        ItemComponent.applyTo(target, item, useId);
                    }
                }
                for (int i = 0; i < value.fluids.size(); i++) {
                    JsonElement entry = source.get(value.fluidSlots.get(i));
                    if (entry != null && entry.isJsonObject()) {
                        applyFluid(entry.getAsJsonObject(), value.fluids.get(i), useId);
                    }
                }
                json.add(field, source);
                return;
            }

            JsonArray array = new JsonArray();
            for (String item : value.items) {
                if (item == null || item.isEmpty()) continue;
                array.add(ItemComponent.createEntry(item, useId));
            }
            for (FluidSpec fluid : value.fluids) {
                if (fluid == null || fluid.isEmpty()) continue;
                array.add(useId ? fluid.toResultJson() : fluid.toJson());
            }
            if (array.isEmpty()) return;

            json.add(field, array);
        }

        @Override
        public boolean matches(MixingList value, ReplacementMatch match) {
            if (value == null) return false;
            for (String item : value.items) {
                if (match.matches(item)) return true;
            }
            for (FluidSpec fluid : value.fluids) {
                if (FluidComponent.INSTANCE.matches(fluid, match)) return true;
            }
            return false;
        }

        @Override
        public boolean replace(MixingList value, ReplacementMatch match, Object replacement,
                               RecipeContext context) {
            if (value == null) return false;

            boolean replaced = false;
            for (int i = 0; i < value.items.size(); i++) {
                String item = value.items.get(i);
                if (!match.matches(item)) continue;
                String replacementId = ItemComponent.toId(replacement);
                if (replacementId == null) continue;
                value.items.set(i, replacementId);
                replaced = true;
            }

            for (int i = 0; i < value.fluids.size(); i++) {
                FluidSpec fluid = value.fluids.get(i);
                if (!FluidComponent.INSTANCE.matches(fluid, match)) continue;
                FluidSpec replacementFluid = FluidComponent.toFluidSpec(replacement, fluid.amount());
                if (replacementFluid == null) continue;
                value.fluids.set(i, replacementFluid);
                replaced = true;
            }

            if (replaced && context != null) {
                context.setValue(value);
            }

            return replaced;
        }

        @Override
        public MixingList defaultValue() {
            return new MixingList();
        }

        @Override
        public void validate(MixingList value, RecipeValidationContext context) {
            if (value == null) {
                context.error("Ingredient list cannot be null");
                return;
            }
            if (value.isEmpty()) {
                context.warning("Ingredient list is empty");
                return;
            }
            for (String item : value.items) {
                ItemComponent.INSTANCE.validate(item, context);
            }
            for (FluidSpec fluid : value.fluids) {
                FluidComponent.INSTANCE.validate(fluid, context);
            }
        }
    }

    static boolean isFluid(JsonObject object) {
        if (object.has("fluid") || object.has("fluidTag")) {
            return true;
        }
        String type = object.has("type") && object.get("type").isJsonPrimitive()
                ? object.get("type").getAsString()
                : "";
        return "neoforge:single".equals(type) || "neoforge:tag".equals(type);
    }

    static void applyFluid(JsonObject object, FluidSpec fluid, boolean useId) {
        if (fluid == null || fluid.isEmpty()) return;

        object.remove("fluid");
        object.remove("fluidTag");
        object.remove("tag");
        object.remove("id");
        if (useId) {
            object.remove("type");
            object.addProperty("id", fluid.id());
        } else if (fluid.isTag()) {
            object.addProperty("type", "neoforge:tag");
            object.addProperty("tag", fluid.id());
        } else {
            object.addProperty("type", "neoforge:single");
            object.addProperty("fluid", fluid.id());
        }
        object.addProperty("amount", fluid.amount());
    }

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("create:mixing")
                .key(INGREDIENT_KEY)
                .key(RESULT_KEY)
                .key(PROCESSING_TIME_KEY)
                .key(HEAT_REQUIREMENT_KEY)
                .postProcessor((ctx, recipe) -> {
                    MixingList ingredients = recipe.getValue(INGREDIENT_KEY);
                    MixingList results = recipe.getValue(RESULT_KEY);

                    if (ingredients == null || ingredients.isEmpty()) {
                        ctx.error("Mixing recipe has no ingredients");
                    }

                    if (results == null || results.isEmpty()) {
                        ctx.error("Mixing recipe has no results");
                    }
                })
                .idGenerator(recipe -> {
                    MixingList ingredients = recipe.getValue(INGREDIENT_KEY);
                    MixingList results = recipe.getValue(RESULT_KEY);

                    StringBuilder sb = new StringBuilder("mixing");
                    if (ingredients != null) {
                        for (String item : ingredients.items) {
                            sb.append("_").append(item.replace(':', '_'));
                        }
                    }
                    if (results != null) {
                        for (String item : results.items) {
                            sb.append("_to_").append(item.replace(':', '_'));
                        }
                    }
                    return sb.toString();
                })
                .build();
    }
}
