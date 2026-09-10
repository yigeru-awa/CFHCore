package com.onehumanawa.cfhcore.foundation.recipe.schema.create;

import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemArrayComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemComponent;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

/**
 * Schema for Create item application recipes.
 */
@RecipeSchemaType("create:item_application")
public class ItemApplicationSchema {

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("create:item_application")
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
                .idGenerator(recipe -> {
                    String[] ingredients = recipe.getValue(RecipeKey.builder(ItemArrayComponent.INSTANCE).name("ingredients").build());
                    String result = recipe.getValue(RecipeKey.builder(ItemComponent.INSTANCE).name("result").build());
                    if (ingredients != null && ingredients.length > 0 && result != null) {
                        return "itemapp_" + ingredients[0].replace(':', '_') + "_to_" + result.replace(':', '_');
                    }
                    return "";
                })
                .build();
    }
}