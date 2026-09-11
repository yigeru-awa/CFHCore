package com.onehumanawa.cfhcore.foundation.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.onehumanawa.cfhcore.foundation.RegistryHelper;
import com.onehumanawa.cfhcore.foundation.ingredient.TagModificationHandler;
import org.jetbrains.annotations.Nullable;

import com.onehumanawa.cfhcore.CFHAllCreativeModeTabs;
import com.onehumanawa.cfhcore.CFHCore;
import com.onehumanawa.cfhcore.foundation.TooltipHelper;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.ItemLike;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class ItemRegistrar {
    private final String path;
    private final boolean valid;
    private final List<UnaryOperator<Item.Properties>> operators = new ArrayList<>();
    private final List<AttributeEntry> attributes = new ArrayList<>();
    private final List<ResourceLocation> pendingTags = new ArrayList<>();
    private ResourceKey<CreativeModeTab> tab = CFHAllCreativeModeTabs.MAIN_TAB_KEY;
    private String enName;
    private String zhName;
    private String summaryEn;
    private String summaryZh;
    private ResourceLocation texturePath;

    public ItemRegistrar(String path) {
        this.valid = RegistryHelper.checkPath("item", path);
        this.path = path == null ? "" : path;
        this.enName = RegistryHelper.defaultName(this.path);
    }

    public ItemRegistrar stacksTo(int size) {
        return properties(p -> p.stacksTo(size));
    }

    public ItemRegistrar stackSize(int size) {
        return stacksTo(size);
    }

    public ItemRegistrar rarity(String rarity) {
        return rarity(RegistryHelper.rarity(rarity));
    }

    public ItemRegistrar rarity(Rarity rarity) {
        return properties(p -> p.rarity(rarity));
    }

    public ItemRegistrar fireResistant() {
        return fireResistant(true);
    }

    public ItemRegistrar fireResistant(boolean fireResistant) {
        return fireResistant ? properties(Item.Properties::fireResistant) : this;
    }

    public ItemRegistrar glow() {
        return glow(true);
    }

    public ItemRegistrar glow(boolean glow) {
        return properties(p -> p.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, glow));
    }

    public ItemRegistrar durability(int durability) {
        return properties(p -> p.durability(durability));
    }

    public ItemRegistrar unbreakable() {
        return unbreakable(true);
    }

    public ItemRegistrar unbreakable(boolean showInTooltip) {
        return properties(p -> p.component(DataComponents.UNBREAKABLE, new Unbreakable(showInTooltip)));
    }

    public ItemRegistrar noRepair() {
        return properties(Item.Properties::setNoRepair);
    }

    public ItemRegistrar food(Consumer<FoodRegistrar> food) {
        FoodRegistrar registrar = new FoodRegistrar();
        food.accept(registrar);
        return properties(p -> p.food(registrar.build()));
    }

    public ItemRegistrar food(FoodProperties food) {
        return properties(p -> p.food(food));
    }

    public ItemRegistrar craftRemainder(ItemLike remainder) {
        return craftRemainder(() -> remainder);
    }

    public ItemRegistrar craftRemainder(Supplier<? extends ItemLike> remainder) {
        return properties(p -> p.craftRemainder(remainder.get().asItem()));
    }

    public ItemRegistrar jukeboxPlayable(ResourceKey<JukeboxSong> song) {
        return properties(p -> p.jukeboxPlayable(song));
    }

    public ItemRegistrar attribute(Attribute attribute, double amount, AttributeModifier.Operation operation, EquipmentSlotGroup slot) {
        return attribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute), amount, operation, slot);
    }

    public ItemRegistrar attribute(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation, EquipmentSlotGroup slot) {
        attributes.add(new AttributeEntry(attribute, amount, operation, slot));
        return this;
    }

    public ItemRegistrar attributes(ItemAttributeModifiers modifiers) {
        return properties(p -> p.attributes(modifiers));
    }

    public ItemRegistrar tab(String tabId) {
        return tab(ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.parse(tabId)));
    }

    public ItemRegistrar tab(ResourceKey<CreativeModeTab> tab) {
        this.tab = tab;
        return this;
    }

    public ItemRegistrar defaultCreativeTab() {
        return tab(CFHAllCreativeModeTabs.MAIN_TAB_KEY);
    }

    public ItemRegistrar noTab() {
        this.tab = null;
        return this;
    }

    public ItemRegistrar texturePath(String texturePath) {
        return texturePath(ResourceLocation.parse(texturePath));
    }

    public ItemRegistrar texturePath(ResourceLocation texturePath) {
        this.texturePath = texturePath;
        return this;
    }

    public ItemRegistrar translation(String en) {
        return translation(en, "");
    }

    public ItemRegistrar translation(String en, String zh) {
        this.enName = en;
        this.zhName = zh;
        return this;
    }

    public ItemRegistrar summary(String en, String zh) {
        this.summaryEn = en;
        this.summaryZh = zh;
        return this;
    }

    public ItemRegistrar properties(UnaryOperator<Item.Properties> operator) {
        operators.add(operator);
        return this;
    }

    public ItemRegistrar tag(String tagId) {
        ResourceLocation id = RegistryHelper.parseTagId(tagId);
        if (id == null) {
            CFHCore.LOGGER.error("[Registry] Invalid tag id '{}' for item '{}'; ignored", tagId, path);
            return this;
        }
        pendingTags.add(id);
        return this;
    }

    public ItemRegistrar tag(ResourceLocation tagId) {
        if (tagId != null) {
            pendingTags.add(tagId);
        }
        return this;
    }

    @Nullable
    public ItemEntry<Item> register() {
        if (!valid) {
            return null;
        }
        RegistryHelper.ensureInitialized();
        ItemBuilder<Item, CreateRegistrate> builder = CFHCore.registrate().item(path, Item::new);
        builder.properties(this::apply);
        builder.lang(enName);
        if (texturePath != null) {
            ResourceLocation texture = texturePath;
            builder.model((ctx, prov) -> prov.generated(ctx::getEntry, texture));
        }
        if (!attributes.isEmpty()) {
            ItemAttributeModifiers.Builder modifiers = ItemAttributeModifiers.builder();
            int index = 0;
            for (AttributeEntry entry : attributes) {
                modifiers.add(entry.attribute(), modifier(entry, index), entry.slot());
                index++;
            }
            ItemAttributeModifiers built = modifiers.build();
            builder.properties(p -> p.attributes(built));
        }
        if (tab != null) {
            builder.tab(tab);
        }
        ItemEntry<Item> entry = builder.register();
        RegistryHelper.collectLang(RegistryHelper.itemLangKey(path), enName, zhName);
        if (summaryEn != null || summaryZh != null) {
            TooltipHelper.tooltip(entry.getId().toString()).summary(summaryEn, summaryZh).register();
        }
        if (!pendingTags.isEmpty()) {
            List<ResourceLocation> self = List.of(entry.getId());
            for (ResourceLocation tagId : pendingTags) {
                TagModificationHandler.addItemAdditions(TagKey.create(Registries.ITEM, tagId), self);
            }
        }
        return entry;
    }

    private Item.Properties apply(Item.Properties properties) {
        Item.Properties result = properties;
        for (UnaryOperator<Item.Properties> operator : operators) {
            result = operator.apply(result);
        }
        return result;
    }

    private AttributeModifier modifier(AttributeEntry entry, int index) {
        String name = entry.attribute().unwrapKey().map(key -> key.location().getPath()).orElse("modifier");
        return new AttributeModifier(RegistryHelper.id(path + "/" + name + "_" + index), entry.amount(), entry.operation());
    }

    private record AttributeEntry(Holder<Attribute> attribute, double amount,
                                  AttributeModifier.Operation operation, EquipmentSlotGroup slot) {}
}
