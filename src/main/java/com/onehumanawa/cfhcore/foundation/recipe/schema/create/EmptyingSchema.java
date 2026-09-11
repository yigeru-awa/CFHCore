package com.onehumanawa.cfhcore.foundation.recipe.schema.create;

import com.onehumanawa.cfhcore.foundation.ingredient.FluidSpec;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

@RecipeSchemaType("create:emptying")
public class EmptyingSchema {

    public static final RecipeKey<MixingSchema.MixingList> INGREDIENT_KEY = MixingSchema.INGREDIENT_KEY;
    public static final RecipeKey<MixingSchema.MixingList> RESULT_KEY = MixingSchema.RESULT_KEY;

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("create:emptying")
                .key(INGREDIENT_KEY)
                .key(RESULT_KEY)
                .postProcessor((ctx, recipe) -> {
                    MixingSchema.MixingList ingredients = recipe.getValue(INGREDIENT_KEY);
                    MixingSchema.MixingList results = recipe.getValue(RESULT_KEY);
                    if (ingredients == null || ingredients.items.isEmpty()) {
                        ctx.error("Emptying recipe requires an item input");
                    }
                    if (results == null || results.items.isEmpty()) {
                        ctx.error("Emptying recipe requires an item output");
                    }
                    if (firstFluid(results) == null) {
                        ctx.error("Emptying recipe requires a fluid output");
                    }
                })
                .idGenerator(recipe -> {
                    MixingSchema.MixingList ingredients = recipe.getValue(INGREDIENT_KEY);
                    MixingSchema.MixingList results = recipe.getValue(RESULT_KEY);
                    FluidSpec fluid = firstFluid(results);
                    if (ingredients != null && !ingredients.items.isEmpty()
                            && results != null && !results.items.isEmpty()) {
                        String id = "empty_" + ingredients.items.get(0).replace(':', '_')
                                + "_to_" + results.items.get(0).replace(':', '_');
                        if (fluid != null) {
                            id = id + "_" + fluid.id().replace(':', '_');
                        }
                        return id;
                    }
                    return "";
                })
                .build();
    }

    private static FluidSpec firstFluid(MixingSchema.MixingList results) {
        if (results == null) {
            return null;
        }
        for (FluidSpec fluid : results.fluids) {
            if (fluid != null && !fluid.isEmpty()) {
                return fluid;
            }
        }
        return null;
    }
}
