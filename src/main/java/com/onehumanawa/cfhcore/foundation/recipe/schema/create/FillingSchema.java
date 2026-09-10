package com.onehumanawa.cfhcore.foundation.recipe.schema.create;

import com.onehumanawa.cfhcore.foundation.ingredient.FluidSpec;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.FluidComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemComponent;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

/**
 * Schema for Create filling recipes.
 */
@RecipeSchemaType("create:filling")
public class FillingSchema {

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("create:filling")
                .key(RecipeKey.builder(ItemComponent.INSTANCE)
                        .name("ingredients")
                        .name("ingredient")
                        .input()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.builder(FluidComponent.INSTANCE)
                        .name("fluid")
                        .input()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.builder(ItemComponent.INSTANCE)
                        .name("result")
                        .output()
                        .alwaysWrite()
                        .build())
                .postProcessor((ctx, recipe) -> {
                    FluidSpec fluid = recipe.getValue(RecipeKey.builder(FluidComponent.INSTANCE).name("fluid").build());
                    if (fluid == null || fluid.isEmpty()) {
                        ctx.error("Filling recipe requires a fluid");
                    }
                })
                .idGenerator(recipe -> {
                    String input = recipe.getValue(RecipeKey.builder(ItemComponent.INSTANCE).name("ingredients").build());
                    FluidSpec fluid = recipe.getValue(RecipeKey.builder(FluidComponent.INSTANCE).name("fluid").build());
                    String result = recipe.getValue(RecipeKey.builder(ItemComponent.INSTANCE).name("result").build());
                    if (input != null && fluid != null && result != null) {
                        return "fill_" + input.replace(':', '_') + "_with_" + fluid.id().replace(':', '_') + "_to_" + result.replace(':', '_');
                    }
                    return "";
                })
                .build();
    }
}