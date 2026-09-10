package com.onehumanawa.cfhcore.foundation.recipe.schema.create;

import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemArrayComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemComponent;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

/**
 * Schema for Create sandpaper polishing recipes.
 * Single ingredient, single output, no processing time.
 */
@RecipeSchemaType("create:sandpaper_polishing")
public class SandpaperPolishingSchema {

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("create:sandpaper_polishing")
                .key(RecipeKey.builder(ItemArrayComponent.INSTANCE)
                        .name("ingredients")
                        .input()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.builder(ItemComponent.INSTANCE)
                        .name("results")
                        .name("result")
                        .output()
                        .alwaysWrite()
                        .build())
                .postProcessor((ctx, recipe) -> {
                    String[] ingredients = recipe.getValue(RecipeKey.builder(ItemArrayComponent.INSTANCE).name("ingredients").build());
                    if (ingredients == null || ingredients.length != 1) {
                        ctx.warning("Sandpaper polishing recipe expects exactly 1 ingredient");
                    }
                })
                .idGenerator(recipe -> {
                    String[] ingredients = recipe.getValue(RecipeKey.builder(ItemArrayComponent.INSTANCE).name("ingredients").build());
                    String result = recipe.getValue(RecipeKey.builder(ItemComponent.INSTANCE).name("results").build());
                    if (ingredients != null && ingredients.length > 0 && result != null) {
                        return "sandpaper_polish_" + ingredients[0].replace(':', '_') + "_to_" + result.replace(':', '_');
                    }
                    return "";
                })
                .build();
    }
}
