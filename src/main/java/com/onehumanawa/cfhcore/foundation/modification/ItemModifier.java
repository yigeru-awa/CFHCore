package com.onehumanawa.cfhcore.foundation.modification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.world.item.BlockItem;
import org.jetbrains.annotations.Nullable;

import com.onehumanawa.cfhcore.CFHCore;
import com.onehumanawa.cfhcore.foundation.RegistryHelper;
import com.onehumanawa.cfhcore.foundation.registry.FoodRegistrar;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

@SuppressWarnings({"unused", "UnusedReturnValue", "unchecked", "rawtypes"})
public final class ItemModifier {
    private final ResourceLocation id;
    private final TagKey<Item> tag;
    private final boolean valid;
    private final List<BiConsumer<Item, DataComponentPatch.Builder>> components = new ArrayList<>();
    private final List<Consumer<Item>> fields = new ArrayList<>();
    private final List<AttributeEntry> attributes = new ArrayList<>();

    public ItemModifier(@Nullable ResourceLocation id) {
        this.id = id;
        this.tag = null;
        this.valid = id != null;
    }

    public ItemModifier(@Nullable TagKey<Item> tag) {
        this.id = null;
        this.tag = tag;
        this.valid = tag != null;
    }

    public ItemModifier stacksTo(int size) {
        return set(DataComponents.MAX_STACK_SIZE, size);
    }

    public ItemModifier stackSize(int size) {
        return stacksTo(size);
    }

    public ItemModifier durability(int durability) {
        return set(DataComponents.MAX_DAMAGE, durability);
    }

    public ItemModifier rarity(String rarity) {
        return rarity(RegistryHelper.rarity(rarity));
    }

    public ItemModifier rarity(Rarity rarity) {
        return set(DataComponents.RARITY, rarity);
    }

    public ItemModifier food(Consumer<FoodRegistrar> food) {
        FoodRegistrar registrar = new FoodRegistrar();
        food.accept(registrar);
        return food(registrar.build());
    }

    public ItemModifier food(FoodProperties food) {
        return set(DataComponents.FOOD, food);
    }

    public ItemModifier noFood() {
        return remove(DataComponents.FOOD);
    }

    public ItemModifier fireResistant() {
        return fireResistant(true);
    }

    public ItemModifier fireResistant(boolean fireResistant) {
        return fireResistant ? set(DataComponents.FIRE_RESISTANT, Unit.INSTANCE) : remove(DataComponents.FIRE_RESISTANT);
    }

    public ItemModifier glow() {
        return glow(true);
    }

    public ItemModifier glow(boolean glow) {
        return set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, glow);
    }

    public ItemModifier unbreakable() {
        return unbreakable(true);
    }

    public ItemModifier unbreakable(boolean showInTooltip) {
        return set(DataComponents.UNBREAKABLE, new Unbreakable(showInTooltip));
    }

    public ItemModifier breakable() {
        return remove(DataComponents.UNBREAKABLE);
    }

    public ItemModifier repairCost(int repairCost) {
        return set(DataComponents.REPAIR_COST, repairCost);
    }

    public ItemModifier tool(Tool tool) {
        return set(DataComponents.TOOL, tool);
    }

    public ItemModifier tool(float miningSpeed, int damagePerBlock) {
        return tool(new Tool(List.of(), miningSpeed, damagePerBlock));
    }

    public ItemModifier tool(String tagId, float miningSpeed) {
        ResourceLocation parsed = RegistryHelper.parseTagId(tagId);
        if (parsed == null) {
            CFHCore.LOGGER.error("[Modification] Invalid tag id '{}' for item {}; ignored", tagId, target());
            return this;
        }
        return tool(TagKey.create(Registries.BLOCK, parsed), miningSpeed);
    }

    public ItemModifier tool(TagKey<Block> tag, float miningSpeed) {
        return add((item, builder) -> builder.set(DataComponents.TOOL,
                new Tool(List.of(Tool.Rule.minesAndDrops(tag, miningSpeed)), miningSpeed, 1)));
    }

    public ItemModifier noTool() {
        return remove(DataComponents.TOOL);
    }

    public <T> ItemModifier component(DataComponentType<T> type, T value) {
        return set(type, value);
    }

    public ItemModifier removeComponent(DataComponentType<?> type) {
        return remove(type);
    }

    public ItemModifier patch(Consumer<DataComponentPatch.Builder> patch) {
        return add((item, builder) -> patch.accept(builder));
    }

    public ItemModifier attribute(Attribute attribute, double amount, AttributeModifier.Operation operation, EquipmentSlotGroup slot) {
        return attribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute), amount, operation, slot);
    }

    public ItemModifier attribute(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation, EquipmentSlotGroup slot) {
        if (valid) {
            attributes.add(new AttributeEntry(attribute, amount, operation, slot));
        }
        return this;
    }

    public ItemModifier attributes(ItemAttributeModifiers modifiers) {
        return set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
    }

    public ItemModifier craftRemainder(ItemLike remainder) {
        return craftRemainder(() -> remainder);
    }

    public ItemModifier craftRemainder(Supplier<? extends ItemLike> remainder) {
        return field(item -> Reflections.set(item, "craftingRemainingItem", remainder.get().asItem()));
    }

    public ItemModifier noCraftRemainder() {
        return field(item -> Reflections.set(item, "craftingRemainingItem", null));
    }

    public ItemModifier noRepair() {
        return canRepair(false);
    }

    public ItemModifier canRepair(boolean canRepair) {
        return field(item -> Reflections.set(item, "canRepair", canRepair));
    }

    public ItemModifier burnTime(int burnTime) {
        if (!valid) {
            return this;
        }
        if (tag != null) {
            ModificationHandler.setBurnTimeTag(tag.location(), burnTime);
        } else {
            ModificationHandler.setBurnTime(id, burnTime);
        }
        return this;
    }

    public ItemModifier nameKey(String key) {
        if (!valid) {
            return this;
        }
        if (tag != null) {
            CFHCore.LOGGER.error("[Modification] nameKey() cannot be applied to every item of tag {}; ignored", tag.location());
            return this;
        }
        return field(item -> nameKey(item, key));
    }

    public ItemModifier translation(String en) {
        return translation(en, "");
    }

    public ItemModifier translation(String en, String zh) {
        if (!valid) {
            return this;
        }
        if (tag != null) {
            CFHCore.LOGGER.error("[Modification] translation() cannot be applied to every item of tag {}; ignored", tag.location());
            return this;
        }
        String key = ModificationHandler.overrideKey(id);
        RegistryHelper.collectLang(key, en, zh);
        return nameKey(key);
    }

    public ItemModifier hide() {
        if (!valid) {
            return this;
        }
        if (tag != null) {
            ModificationHandler.hideTag(tag.location());
        } else {
            ModificationHandler.hide(id);
        }
        return this;
    }

    public ItemModifier disable() {
        if (!valid) {
            return this;
        }
        if (tag != null) {
            ModificationHandler.disableTag(tag.location());
        } else {
            ModificationHandler.disable(id);
        }
        return this;
    }

    @Nullable
    public ResourceLocation id() {
        return id;
    }

    @Nullable
    public TagKey<Item> tag() {
        return tag;
    }

    public String target() {
        return tag != null ? "#" + tag.location() : String.valueOf(id);
    }

    public void apply() {
        if (!valid) {
            return;
        }
        if (!attributes.isEmpty()) {
            add((item, builder) -> builder.set(DataComponents.ATTRIBUTE_MODIFIERS, merged(item)));
        }
        if (tag != null) {
            ModificationHandler.addItemTagModifier(this);
        } else {
            ModificationHandler.addItemModifier(this);
        }
    }

    void applyComponents(ModifyDefaultComponentsEvent event, Item item) {
        if (components.isEmpty()) {
            return;
        }
        event.modify(item, builder -> components.forEach(operation -> operation.accept(item, builder)));
    }

    void applyFields(Item item) {
        fields.forEach(operation -> operation.accept(item));
    }

    void applyLate(Item item) {
        applyPatchedComponents(item);
        applyFields(item);
    }

    private void applyPatchedComponents(Item item) {
        if (components.isEmpty()) {
            return;
        }
        DataComponentPatch.Builder builder = DataComponentPatch.builder();
        components.forEach(operation -> operation.accept(item, builder));
        DataComponentPatch patch = builder.build();
        if (patch.isEmpty()) {
            return;
        }
        try {
            DataComponentMap.Builder map = DataComponentMap.builder().addAll(item.components());
            for (Map.Entry<DataComponentType<?>, Optional<?>> entry : patch.entrySet()) {
                map.set((DataComponentType) entry.getKey(), entry.getValue().orElse(null));
            }
            Reflections.set(item, "components", Item.Properties.validateComponents(map.build()));
        } catch (RuntimeException e) {
            CFHCore.LOGGER.error("[Modification] Cannot patch default components of {}: {}",
                    BuiltInRegistries.ITEM.getKey(item), e.toString());
        }
    }

    private ItemAttributeModifiers merged(Item item) {
        ItemAttributeModifiers current = item.components().get(DataComponents.ATTRIBUTE_MODIFIERS);
        ItemAttributeModifiers result = current == null ? ItemAttributeModifiers.EMPTY : current;
        int index = 0;
        for (AttributeEntry entry : attributes) {
            result = result.withModifierAdded(entry.attribute(), modifier(item, entry, index), entry.slot());
            index++;
        }
        return result;
    }

    private AttributeModifier modifier(Item item, AttributeEntry entry, int index) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        String name = entry.attribute().unwrapKey().map(attribute -> attribute.location().getPath()).orElse("modifier");
        return new AttributeModifier(CFHCore.asResource((key == null ? "item" : key.getPath()) + "/" + name + "_" + index),
                entry.amount(), entry.operation());
    }

    private static void nameKey(Item item, String key) {
        if (item instanceof BlockItem blockItem) {
            Reflections.set(blockItem.getBlock(), "descriptionId", key);
        } else {
            Reflections.set(item, "descriptionId", key);
        }
    }

    private <T> ItemModifier set(DataComponentType<T> type, T value) {
        return add((item, builder) -> builder.set(type, value));
    }

    private ItemModifier remove(DataComponentType<?> type) {
        return add((item, builder) -> builder.remove(type));
    }

    private ItemModifier add(BiConsumer<Item, DataComponentPatch.Builder> operation) {
        if (valid) {
            components.add(operation);
        }
        return this;
    }

    private ItemModifier field(Consumer<Item> operation) {
        if (valid) {
            fields.add(operation);
        }
        return this;
    }

    private record AttributeEntry(Holder<Attribute> attribute, double amount,
                                  AttributeModifier.Operation operation, EquipmentSlotGroup slot) {}
}
