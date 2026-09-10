package com.onehumanawa.cfhcore.foundation.recipe.schema.create;

import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemArrayComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemComponent;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

/**
 * Schema for Create deploying recipes.
 */
@RecipeSchemaType("create:deploying")
public class DeployingSchema {

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("create:deploying")
                .key(RecipeKey.builder(ItemArrayComponent.INSTANCE)
                        .name("ingredients")
                        .input()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.builder(ItemComponent.INSTANCE)
                        .name("result")
                        .name("results")
                        .output()
                        .alwaysWrite()
                        .build())
                .postProcessor((ctx, recipe) -> {
                    String[] ingredients = recipe.getValue(RecipeKey.builder(ItemArrayComponent.INSTANCE).name("ingredients").build());
                    if (ingredients == null || ingredients.length < 2) {
                        ctx.warning("Deploying recipe should have at least 2 ingredients (item + tool)");
                    }
                })
                .idGenerator(recipe -> {
                    String[] ingredients = recipe.getValue(RecipeKey.builder(ItemArrayComponent.INSTANCE).name("ingredients").build());
                    String result = recipe.getValue(RecipeKey.builder(ItemComponent.INSTANCE).name("result").build());
                    if (ingredients != null && ingredients.length > 0 && result != null) {
                        return "deploy_" + ingredients[0].replace(':', '_') + "_to_" + result.replace(':', '_');
                    }
                    return "";
                })
                .build();
    }
}