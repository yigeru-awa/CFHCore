package com.onehumanawa.cfhcore.foundation.recipe.component.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.foundation.ingredient.FluidSpec;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeContext;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeValidationContext;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponent;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;

public class FluidComponent implements RecipeComponent<FluidSpec> {

    public static final FluidComponent INSTANCE = new FluidComponent();

    @Override
    public FluidSpec read(JsonObject json, RecipeContext context) {
        JsonElement element = locate(json, context);
        if (!element.isJsonObject()) {
            return FluidSpec.empty();
        }

        JsonObject object = element.getAsJsonObject();
        int amount = object.has("amount") ? object.get("amount").getAsInt() : 1000;

        if (object.has("fluidTag")) {
            return FluidSpec.tag(object.get("fluidTag").getAsString(), amount);
        }
        if (object.has("tag")) {
            return FluidSpec.tag(object.get("tag").getAsString(), amount);
        }
        if (object.has("fluid")) {
            String id = ItemComponent.readId(object.get("fluid"));
            return id != null ? FluidSpec.of(id, amount) : FluidSpec.empty();
        }
        if (object.has("id")) {
            return FluidSpec.of(object.get("id").getAsString(), amount);
        }
        return FluidSpec.empty();
    }

    @Override
    public void write(JsonObject json, FluidSpec value, RecipeContext context) {
        if (value == null || value.isEmpty()) return;

        String field = targetField(json, context);
        if (field == null || "fluid".equals(field) || "fluidTag".equals(field)) {
            if (value.isTag()) {
                json.addProperty("fluidTag", value.id());
            } else {
                JsonObject fluidObject = new JsonObject();
                fluidObject.addProperty("id", value.id());
                json.add("fluid", fluidObject);
            }
            json.addProperty("amount", value.amount());
            return;
        }

        JsonObject element = new JsonObject();
        if (value.isTag()) {
            element.addProperty("fluidTag", value.id());
        } else {
            element.addProperty("fluid", value.id());
        }
        element.addProperty("amount", value.amount());
        json.add(field, element);
    }

    @Override
    public boolean matches(FluidSpec value, ReplacementMatch match) {
        if (value == null || value.isEmpty()) return false;

        if (value.isTag()) {
            return match.matches("#" + value.id()) || match.matches(value.id());
        }

        return match.matches(value.id());
    }

    @Override
    public boolean replace(FluidSpec value, ReplacementMatch match, Object replacement, RecipeContext context) {
        if (value == null || !matches(value, match)) return false;

        FluidSpec replacementFluid = toFluidSpec(replacement, value.amount());
        if (replacementFluid == null) return false;
        if (context != null) {
            context.setValue(replacementFluid);
        }
        return true;
    }

    @Override
    public FluidSpec defaultValue() {
        return FluidSpec.empty();
    }

    @Override
    public void validate(FluidSpec value, RecipeValidationContext context) {
        if (value == null) {
            context.error("Fluid value cannot be null");
            return;
        }
        if (value.isEmpty()) {
            context.warning("Fluid value is empty");
        }
        if (value.amount() <= 0) {
            context.warning("Fluid amount should be positive, got: {}", value.amount());
        }
    }

    public static FluidSpec toFluidSpec(Object replacement, int fallbackAmount) {
        if (replacement instanceof FluidSpec spec) {
            return spec;
        }
        if (replacement instanceof String id) {
            if (id.isEmpty()) return null;
            return id.startsWith("#") ? FluidSpec.tag(id.substring(1), fallbackAmount) : FluidSpec.of(id, fallbackAmount);
        }
        return null;
    }
}
