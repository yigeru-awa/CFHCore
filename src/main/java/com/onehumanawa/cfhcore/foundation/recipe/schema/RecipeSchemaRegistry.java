package com.onehumanawa.cfhcore.foundation.recipe.schema;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.slf4j.Logger;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registry for recipe schemas by recipe type ID.
 */
public class RecipeSchemaRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<ResourceLocation, RecipeSchema> SCHEMAS = new HashMap<>();
    private static final Map<ResourceLocation, ResourceLocation> TYPE_TO_SCHEMA = new HashMap<>();

    // ================================================================
    // Annotation-driven auto discovery
    // ================================================================

    /**
     * Discovers and registers every schema annotated with {@link RecipeSchemaType}
     * across all loaded mod files.
     * <p>
     * Discovery relies on NeoForge's {@link ModFileScanData}, which works both in a
     * development environment and inside a production jar. A class-level annotation
     * registers all {@code static RecipeSchema} fields of that class, while a
     * field-level annotation registers a single field.
     *
     * @return the number of schemas registered by this call
     */
    public static int autoDiscover() {
        int count = 0;
        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            count += discoverIn(scanData);
        }
        return count;
    }

    private static int discoverIn(ModFileScanData scanData) {
        int count = 0;
        for (ModFileScanData.AnnotationData data : scanData.getAnnotatedBy(RecipeSchemaType.class, ElementType.TYPE).toList()) {
            count += registerFromClass(data);
        }
        for (ModFileScanData.AnnotationData data : scanData.getAnnotatedBy(RecipeSchemaType.class, ElementType.FIELD).toList()) {
            count += registerFromField(data);
        }
        return count;
    }

    private static int registerFromClass(ModFileScanData.AnnotationData data) {
        String className = data.clazz().getClassName();
        String annotationValue = annotationValue(data);
        try {
            Class<?> clazz = Class.forName(className, true, RecipeSchemaRegistry.class.getClassLoader());
            List<Field> schemaFields = new ArrayList<>();
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && RecipeSchema.class.isAssignableFrom(field.getType())) {
                    schemaFields.add(field);
                }
            }
            int registered = 0;
            for (Field field : schemaFields) {
                field.setAccessible(true);
                if (field.get(null) instanceof RecipeSchema schema) {
                    String typeId = (!annotationValue.isEmpty() && schemaFields.size() == 1)
                            ? annotationValue
                            : schema.recipeType();
                    register(typeId, schema);
                    registered++;
                }
            }
            if (registered == 0) {
                LOGGER.warn("@RecipeSchemaType on {} matched no static RecipeSchema field", className);
            }
            return registered;
        } catch (Throwable t) {
            LOGGER.error("Failed to auto-register recipe schemas from class {}", className, t);
            return 0;
        }
    }

    private static int registerFromField(ModFileScanData.AnnotationData data) {
        String className = data.clazz().getClassName();
        String memberName = data.memberName();
        String annotationValue = annotationValue(data);
        try {
            Class<?> clazz = Class.forName(className, true, RecipeSchemaRegistry.class.getClassLoader());
            Field field = clazz.getDeclaredField(memberName);
            field.setAccessible(true);
            if (field.get(null) instanceof RecipeSchema schema) {
                String typeId = !annotationValue.isEmpty() ? annotationValue : schema.recipeType();
                register(typeId, schema);
                return 1;
            }
            LOGGER.warn("@RecipeSchemaType field {}.{} is not a static RecipeSchema", className, memberName);
            return 0;
        } catch (Throwable t) {
            LOGGER.error("Failed to auto-register recipe schema from field {}.{}", className, memberName, t);
            return 0;
        }
    }

    private static String annotationValue(ModFileScanData.AnnotationData data) {
        return data.annotationData().get("value") instanceof String s ? s : "";
    }

    public static void register(ResourceLocation typeId, RecipeSchema schema) {
        SCHEMAS.put(typeId, schema);
        TYPE_TO_SCHEMA.put(typeId, typeId);
    }

    public static void register(ResourceLocation typeId, ResourceLocation schemaId, RecipeSchema schema) {
        SCHEMAS.put(schemaId, schema);
        TYPE_TO_SCHEMA.put(typeId, schemaId);
    }

    public static void register(String typeId, RecipeSchema schema) {
        register(ResourceLocation.parse(typeId), schema);
    }

    public static Optional<RecipeSchema> get(ResourceLocation typeId) {
        ResourceLocation schemaId = TYPE_TO_SCHEMA.getOrDefault(typeId, typeId);
        return Optional.ofNullable(SCHEMAS.get(schemaId));
    }

    public static Optional<RecipeSchema> get(String typeId) {
        return get(ResourceLocation.parse(typeId));
    }

    public static RecipeSchema getOrThrow(ResourceLocation typeId) {
        return get(typeId).orElseThrow(() -> new IllegalArgumentException("No schema registered for type: " + typeId));
    }

    public static boolean hasSchema(ResourceLocation typeId) {
        return SCHEMAS.containsKey(TYPE_TO_SCHEMA.getOrDefault(typeId, typeId));
    }

    public static Set<ResourceLocation> registeredTypes() {
        return Set.copyOf(SCHEMAS.keySet());
    }

    public static void clear() {
        SCHEMAS.clear();
        TYPE_TO_SCHEMA.clear();
    }
}