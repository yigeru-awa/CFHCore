package com.onehumanawa.cfhcore.foundation.recipe.schema.create;

import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemArrayComponent;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

@RecipeSchemaType("create:deploying")
public class DeployingSchema {

    public static final RecipeKey<String[]> INGREDIENTS_KEY =
            RecipeKey.builder(ItemArrayComponent.INSTANCE).name("ingredients").input().alwaysWrite().build();
    public static final RecipeKey<String[]> RESULTS_KEY =
            RecipeKey.builder(ItemArrayComponent.INSTANCE).names("results", "result").output().alwaysWrite().build();

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("create:deploying")
                .key(INGREDIENTS_KEY)
                .key(RESULTS_KEY)
                .postProcessor((ctx, recipe) -> {
                    String[] ingredients = recipe.getValue(INGREDIENTS_KEY);
                    if (ingredients == null || ingredients.length < 2) {
                        ctx.warning("Deploying recipe should have at least 2 ingredients (item + tool)");
                    }
                    String[] results = recipe.getValue(RESULTS_KEY);
                    if (results == null || results.length == 0) {
                        ctx.error("Deploying recipe requires at least one result");
                    }
                })
                .idGenerator(recipe -> {
                    String[] ingredients = recipe.getValue(INGREDIENTS_KEY);
                    String[] results = recipe.getValue(RESULTS_KEY);
                    if (ingredients != null && ingredients.length > 0 && results != null && results.length > 0) {
                        return "deploy_" + ingredients[0].replace(':', '_') + "_to_" + results[0].replace(':', '_');
                    }
                    return "";
                })
                .build();
    }
}
