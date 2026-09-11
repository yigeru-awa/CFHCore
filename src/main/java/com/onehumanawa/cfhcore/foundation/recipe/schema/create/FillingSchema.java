package com.onehumanawa.cfhcore.foundation.recipe.schema.create;

import com.onehumanawa.cfhcore.foundation.ingredient.FluidSpec;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

@RecipeSchemaType("create:filling")
public class FillingSchema {

    public static final RecipeKey<MixingSchema.MixingList> INGREDIENT_KEY = MixingSchema.INGREDIENT_KEY;
    public static final RecipeKey<MixingSchema.MixingList> RESULT_KEY = MixingSchema.RESULT_KEY;

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("create:filling")
                .key(INGREDIENT_KEY)
                .key(RESULT_KEY)
                .postProcessor((ctx, recipe) -> {
                    MixingSchema.MixingList ingredients = recipe.getValue(INGREDIENT_KEY);
                    MixingSchema.MixingList results = recipe.getValue(RESULT_KEY);
                    if (ingredients == null || ingredients.items.isEmpty()) {
                        ctx.error("Filling recipe requires an item input");
                    }
                    if (firstFluid(ingredients) == null) {
                        ctx.error("Filling recipe requires a fluid input");
                    }
                    if (results == null || results.items.isEmpty()) {
                        ctx.error("Filling recipe requires an item output");
                    }
                })
                .idGenerator(recipe -> {
                    MixingSchema.MixingList ingredients = recipe.getValue(INGREDIENT_KEY);
                    MixingSchema.MixingList results = recipe.getValue(RESULT_KEY);
                    FluidSpec fluid = firstFluid(ingredients);
                    if (ingredients != null && !ingredients.items.isEmpty() && fluid != null
                            && results != null && !results.items.isEmpty()) {
                        return "fill_" + ingredients.items.get(0).replace(':', '_')
                                + "_with_" + fluid.id().replace(':', '_')
                                + "_to_" + results.items.get(0).replace(':', '_');
                    }
                    return "";
                })
                .build();
    }

    private static FluidSpec firstFluid(MixingSchema.MixingList ingredients) {
        if (ingredients == null) {
            return null;
        }
        for (FluidSpec fluid : ingredients.fluids) {
            if (fluid != null && !fluid.isEmpty()) {
                return fluid;
            }
        }
        return null;
    }
}
