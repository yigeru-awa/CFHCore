package com.onehumanawa.cfhcore.foundation.recipe.schema;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class or a static {@link RecipeSchema} field for automatic registration
 * into the {@link RecipeSchemaRegistry}.
 * <p>
 * Classes carrying this annotation are discovered at mod initialization time via
 * NeoForge's {@code ModFileScanData}, so new schemas only need to be annotated &mdash;
 * no manual registration call is required.
 * <p>
 * Two usage modes are supported:
 * <ul>
 *     <li><b>On a type</b> &mdash; registers every {@code static RecipeSchema} field of the
 *     class. When {@link #value()} is non-empty and the class declares exactly one schema
 *     field, that value is used as the recipe type id; otherwise each schema's own
 *     {@link RecipeSchema#recipeType()} is used.</li>
 *     <li><b>On a field</b> &mdash; registers that single {@code static RecipeSchema} field.
 *     Useful for classes exposing several schemas (e.g. the smelting family).</li>
 * </ul>
 * <p>
 * Example:
 * <pre>{@code
 * @RecipeSchemaType("minecraft:crafting_shaped")
 * public class ShapedSchema {
 *     public static final RecipeSchema INSTANCE = create();
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface RecipeSchemaType {

    /**
     * The recipe type id this schema handles, e.g. {@code "minecraft:crafting_shaped"}.
     * <p>
     * When left empty, the id is taken from {@link RecipeSchema#recipeType()}.
     */
    String value() default "";
}