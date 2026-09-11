package com.onehumanawa.cfhcore.foundation.recipe.schema.create;

import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

@RecipeSchemaType("create:compacting")
public class CompactingSchema {

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("create:compacting")
                .key(MixingSchema.INGREDIENT_KEY)
                .key(MixingSchema.RESULT_KEY)
                .key(MixingSchema.PROCESSING_TIME_KEY)
                .key(MixingSchema.HEAT_REQUIREMENT_KEY)
                .postProcessor((ctx, recipe) -> {
                    var ingredients = recipe.getValue(MixingSchema.INGREDIENT_KEY);
                    var results = recipe.getValue(MixingSchema.RESULT_KEY);
                    if (ingredients == null || ingredients.isEmpty()) {
                        ctx.error("Compacting recipe has no ingredients");
                    }
                    if (results == null || results.isEmpty()) {
                        ctx.error("Compacting recipe has no results");
                    }
                })
                .idGenerator(recipe -> {
                    var ingredients = recipe.getValue(MixingSchema.INGREDIENT_KEY);
                    var results = recipe.getValue(MixingSchema.RESULT_KEY);
                    StringBuilder sb = new StringBuilder("compact");
                    if (ingredients != null && !ingredients.items.isEmpty()) {
                        sb.append("_").append(ingredients.items.get(0).replace(':', '_'));
                    }
                    if (results != null && !results.items.isEmpty()) {
                        sb.append("_to_").append(results.items.get(0).replace(':', '_'));
                    }
                    return sb.toString();
                })
                .build();
    }
}
