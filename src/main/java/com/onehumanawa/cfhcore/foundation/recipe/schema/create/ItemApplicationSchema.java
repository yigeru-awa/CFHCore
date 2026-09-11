package com.onehumanawa.cfhcore.foundation.recipe.schema.create;

import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

@RecipeSchemaType("create:item_application")
public class ItemApplicationSchema {

    public static final RecipeKey<String[]> INGREDIENTS_KEY = DeployingSchema.INGREDIENTS_KEY;
    public static final RecipeKey<String[]> RESULTS_KEY = DeployingSchema.RESULTS_KEY;

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("create:item_application")
                .key(INGREDIENTS_KEY)
                .key(RESULTS_KEY)
                .postProcessor((ctx, recipe) -> {
                    String[] ingredients = recipe.getValue(INGREDIENTS_KEY);
                    if (ingredients == null || ingredients.length < 2) {
                        ctx.warning("Item application recipe should have at least 2 ingredients (target + tool)");
                    }
                    String[] results = recipe.getValue(RESULTS_KEY);
                    if (results == null || results.length == 0) {
                        ctx.error("Item application recipe requires at least one result");
                    }
                })
                .idGenerator(recipe -> {
                    String[] ingredients = recipe.getValue(INGREDIENTS_KEY);
                    String[] results = recipe.getValue(RESULTS_KEY);
                    if (ingredients != null && ingredients.length > 0 && results != null && results.length > 0) {
                        return "itemapp_" + ingredients[0].replace(':', '_') + "_to_" + results[0].replace(':', '_');
                    }
                    return "";
                })
                .build();
    }
}
