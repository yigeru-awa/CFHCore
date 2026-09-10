package com.onehumanawa.cfhcore.foundation.recipe.schema.create;

import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.IntegerComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemArrayComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemComponent;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

/**
 * Schema for Create sequenced assembly recipes.
 * <p>
 * Sequenced assembly recipes consist of a base ingredient, a transitional
 * (incomplete) item, a sequence of assembly steps and a weighted result pool.
 * They can be added and removed, but do not support input/output replacement
 * through recipe filters (see RecipeModificationHandler).
 */
@RecipeSchemaType("create:sequenced_assembly")
public class SequencedAssemblySchema {

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("create:sequenced_assembly")
                .key(RecipeKey.builder(ItemComponent.INSTANCE)
                        .name("ingredient")
                        .input()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.builder(ItemComponent.INSTANCE)
                        .name("transitional_item")
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.builder(ItemArrayComponent.INSTANCE)
                        .name("results")
                        .output()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.builder(new IntegerComponent("loops", 1, 1000, 1))
                        .name("loops")
                        .optional()
                        .defaultValue(1)
                        .build())
                .postProcessor((ctx, recipe) -> {
                    String ingredient = recipe.getValue(RecipeKey.builder(ItemComponent.INSTANCE).name("ingredient").build());
                    String transitional = recipe.getValue(RecipeKey.builder(ItemComponent.INSTANCE).name("transitional_item").build());
                    if (ingredient == null || ingredient.isEmpty()) {
                        ctx.error("Sequenced assembly recipe has no ingredient");
                    }
                    if (transitional == null || transitional.isEmpty()) {
                        ctx.error("Sequenced assembly recipe has no transitional_item");
                    }
                })
                .idGenerator(recipe -> {
                    String ingredient = recipe.getValue(RecipeKey.builder(ItemComponent.INSTANCE).name("ingredient").build());
                    if (ingredient != null) {
                        return "assembly_" + ingredient.replace(':', '_');
                    }
                    return "";
                })
                .build();
    }
}
