package com.onehumanawa.cfhcore.foundation.recipe.schema.create;

import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemArrayComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemComponent;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

/**
 * Schema for Create crushing recipes.
 */
@RecipeSchemaType("create:crushing")
public class CrushingSchema {

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("create:crushing")
                .key(RecipeKey.builder(ItemComponent.INSTANCE)
                        .name("ingredients")
                        .name("ingredient")
                        .input()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.builder(ItemArrayComponent.INSTANCE)
                        .name("results")
                        .output()
                        .alwaysWrite()
                        .build())
                .idGenerator(recipe -> {
                    String input = recipe.getValue(RecipeKey.builder(ItemComponent.INSTANCE).name("ingredients").build());
                    String[] results = recipe.getValue(RecipeKey.builder(ItemArrayComponent.INSTANCE).name("results").build());
                    if (input != null && results != null && results.length > 0) {
                        return "crush_" + input.replace(':', '_') + "_to_" + results[0].replace(':', '_');
                    }
                    return "";
                })
                .build();
    }
}