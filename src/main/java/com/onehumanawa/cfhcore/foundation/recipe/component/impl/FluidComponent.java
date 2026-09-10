package com.onehumanawa.cfhcore.foundation.recipe.component.impl;

import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.foundation.ingredient.FluidSpec;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeContext;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeValidationContext;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;

/**
 * Component for fluid values in recipes.
 * Supports both individual fluids and fluid tags.
 */
public class FluidComponent implements RecipeComponent<FluidSpec> {

    public static final FluidComponent INSTANCE = new FluidComponent();

    @Override
    public FluidSpec read(JsonObject json, RecipeContext context) {
        if (!json.has("fluid") && !json.has("fluidTag")) {
            return FluidSpec.empty();
        }

        if (json.has("fluid")) {
            JsonObject fluidObj = json.get("fluid").getAsJsonObject();
            String id = fluidObj.has("id") ? fluidObj.get("id").getAsString() : fluidObj.get("fluid").getAsString();
            int amount = fluidObj.has("amount") ? fluidObj.get("amount").getAsInt() : 1000;
            return FluidSpec.of(id, amount);
        }

        if (json.has("fluidTag")) {
            String tag = json.get("fluidTag").getAsString();
            int amount = json.has("amount") ? json.get("amount").getAsInt() : 1000;
            return FluidSpec.tag(tag, amount);
        }

        return FluidSpec.empty();
    }

    @Override
    public void write(JsonObject json, FluidSpec value, RecipeContext context) {
        if (value == null || value.isEmpty()) return;

        if (value.isTag()) {
            json.addProperty("fluidTag", value.id());
        } else {
            JsonObject fluidObj = new JsonObject();
            fluidObj.addProperty("id", value.id());
            json.add("fluid", fluidObj);
        }
        json.addProperty("amount", value.amount());
    }

    @Override
    public boolean matches(FluidSpec value, ReplacementMatch match) {
        if (value == null || value.isEmpty()) return false;

        // For tag fluids, match against both "#tag" and "tag"
        if (value.isTag()) {
            return match.matches("#" + value.id()) || match.matches(value.id());
        }

        return match.matches(value.id());
    }

    @Override
    public boolean replace(FluidSpec value, ReplacementMatch match, Object replacement, RecipeContext context) {
        if (value == null || !matches(value, match)) return false;

        if (replacement instanceof String replacementStr) {
            FluidSpec newFluid;
            if (replacementStr.startsWith("#")) {
                newFluid = FluidSpec.tag(replacementStr.substring(1), value.amount());
            } else {
                newFluid = FluidSpec.of(replacementStr, value.amount());
            }
            context.recipe().setValue(RecipeKey.builder(INSTANCE).name("fluid").build(), newFluid);
            return true;
        }

        if (replacement instanceof FluidSpec replacementFluid) {
            context.recipe().setValue(RecipeKey.builder(INSTANCE).name("fluid").build(), replacementFluid);
            return true;
        }
        return false;
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
}