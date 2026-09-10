package com.onehumanawa.cfhcore.foundation.recipe.schema.vanilla;

import com.onehumanawa.cfhcore.foundation.recipe.RecipeValidationContext;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemArrayComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.StringComponent;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

/**
 * Schema for vanilla shapeless crafting recipes.
 */
@RecipeSchemaType("minecraft:crafting_shapeless")
public class ShapelessSchema {

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("minecraft:crafting_shapeless")
                .key(RecipeKey.builder(ItemArrayComponent.INSTANCE)
                        .name("ingredients")
                        .input()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.builder(ItemComponent.INSTANCE)
                        .name("result")
                        .output()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.builder(new StringComponent("group"))
                        .name("group")
                        .optional()
                        .build())
                .postProcessor((ctx, recipe) -> {
                    String[] ingredients = recipe.getValue(RecipeKey.builder(ItemArrayComponent.INSTANCE).name("ingredients").build());
                    if (ingredients == null || ingredients.length == 0) {
                        ctx.error("Shapeless recipe has no ingredients");
                    }
                })
                .idGenerator(recipe -> {
                    String[] ingredients = recipe.getValue(RecipeKey.builder(ItemArrayComponent.INSTANCE).name("ingredients").build());
                    String result = recipe.getValue(RecipeKey.builder(ItemComponent.INSTANCE).name("result").build());
                    if (ingredients != null && result != null) {
                        return String.join("_", ingredients) + "_to_" + result.replace(':', '_');
                    }
                    return "";
                })
                .build();
    }
}