package com.onehumanawa.cfhcore.foundation.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.onehumanawa.cfhcore.foundation.RegistryHelper;
import com.onehumanawa.cfhcore.foundation.ingredient.TagModificationHandler;
import org.jetbrains.annotations.Nullable;

import com.onehumanawa.cfhcore.CFHAllCreativeModeTabs;
import com.onehumanawa.cfhcore.CFHCore;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class BlockRegistrar {
    private final String path;
    private final boolean valid;
    private final List<UnaryOperator<BlockBehaviour.Properties>> operators = new ArrayList<>();
    private final List<ResourceLocation> pendingTags = new ArrayList<>();
    private UnaryOperator<Item.Properties> itemOperator;
    private boolean itemEnabled = true;
    private ResourceKey<CreativeModeTab> tab = CFHAllCreativeModeTabs.MAIN_TAB_KEY;
    private String enName;
    private String zhName;
    private String summaryEn;
    private String summaryZh;
    private ResourceLocation texturePath;

    public BlockRegistrar(String path) {
        this.valid = RegistryHelper.checkPath("block", path);
        this.path = path == null ? "" : path;
        this.enName = RegistryHelper.defaultName(this.path);
    }

    public BlockRegistrar hardness(float destroyTime) {
        return properties(p -> p.destroyTime(destroyTime));
    }

    public BlockRegistrar resistance(float explosionResistance) {
        return properties(p -> p.explosionResistance(explosionResistance));
    }

    public BlockRegistrar strength(float strength) {
        return properties(p -> p.strength(strength));
    }

    public BlockRegistrar strength(float destroyTime, float explosionResistance) {
        return properties(p -> p.strength(destroyTime, explosionResistance));
    }

    public BlockRegistrar instabreak() {
        return properties(BlockBehaviour.Properties::instabreak);
    }

    public BlockRegistrar requiresCorrectTool() {
        return requiresCorrectTool(true);
    }

    public BlockRegistrar requiresCorrectTool(boolean required) {
        return required ? properties(BlockBehaviour.Properties::requiresCorrectToolForDrops) : this;
    }

    public BlockRegistrar lightLevel(int lightLevel) {
        return properties(p -> p.lightLevel(state -> lightLevel));
    }

    public BlockRegistrar luminance(int lightLevel) {
        return lightLevel(lightLevel);
    }

    public BlockRegistrar sound(SoundType sound) {
        return properties(p -> p.sound(sound));
    }

    public BlockRegistrar mapColor(MapColor color) {
        return properties(p -> p.mapColor(color));
    }

    public BlockRegistrar mapColor(DyeColor color) {
        return properties(p -> p.mapColor(color));
    }

    public BlockRegistrar friction(float friction) {
        return properties(p -> p.friction(friction));
    }

    public BlockRegistrar speedFactor(float speedFactor) {
        return properties(p -> p.speedFactor(speedFactor));
    }

    public BlockRegistrar jumpFactor(float jumpFactor) {
        return properties(p -> p.jumpFactor(jumpFactor));
    }

    public BlockRegistrar noCollision() {
        return properties(BlockBehaviour.Properties::noCollission);
    }

    public BlockRegistrar noOcclusion() {
        return properties(BlockBehaviour.Properties::noOcclusion);
    }

    public BlockRegistrar randomTicks() {
        return properties(BlockBehaviour.Properties::randomTicks);
    }

    public BlockRegistrar dynamicShape() {
        return properties(BlockBehaviour.Properties::dynamicShape);
    }

    public BlockRegistrar replaceable() {
        return properties(BlockBehaviour.Properties::replaceable);
    }

    public BlockRegistrar ignitedByLava() {
        return properties(BlockBehaviour.Properties::ignitedByLava);
    }

    public BlockRegistrar forceSolidOn() {
        return properties(BlockBehaviour.Properties::forceSolidOn);
    }

    public BlockRegistrar noTerrainParticles() {
        return properties(BlockBehaviour.Properties::noTerrainParticles);
    }

    public BlockRegistrar pushReaction(String reaction) {
        PushReaction resolved = RegistryHelper.byName(PushReaction.class, reaction, "push reaction");
        return resolved == null ? this : pushReaction(resolved);
    }

    public BlockRegistrar pushReaction(PushReaction reaction) {
        return properties(p -> p.pushReaction(reaction));
    }

    public BlockRegistrar instrument(String instrument) {
        NoteBlockInstrument resolved = RegistryHelper.byName(NoteBlockInstrument.class, instrument, "note block instrument");
        return resolved == null ? this : instrument(resolved);
    }

    public BlockRegistrar instrument(NoteBlockInstrument instrument) {
        return properties(p -> p.instrument(instrument));
    }

    public BlockRegistrar offsetType(String offsetType) {
        BlockBehaviour.OffsetType resolved = RegistryHelper.byName(BlockBehaviour.OffsetType.class, offsetType, "offset type");
        return resolved == null ? this : offsetType(resolved);
    }

    public BlockRegistrar offsetType(BlockBehaviour.OffsetType offsetType) {
        return properties(p -> p.offsetType(offsetType));
    }

    public BlockRegistrar redstoneConductor(boolean value) {
        return properties(p -> p.isRedstoneConductor((state, level, pos) -> value));
    }

    public BlockRegistrar suffocating(boolean value) {
        return properties(p -> p.isSuffocating((state, level, pos) -> value));
    }

    public BlockRegistrar viewBlocking(boolean value) {
        return properties(p -> p.isViewBlocking((state, level, pos) -> value));
    }

    public BlockRegistrar postProcess(boolean value) {
        return properties(p -> p.hasPostProcess((state, level, pos) -> value));
    }

    public BlockRegistrar emissiveRendering(boolean value) {
        return properties(p -> p.emissiveRendering((state, level, pos) -> value));
    }

    public BlockRegistrar validSpawn(boolean value) {
        return properties(p -> p.isValidSpawn((state, level, pos, type) -> value));
    }

    public BlockRegistrar noLootTable() {
        return properties(BlockBehaviour.Properties::noLootTable);
    }

    public BlockRegistrar dropsLike(Block block) {
        return dropsLike(() -> block);
    }

    public BlockRegistrar dropsLike(Supplier<? extends Block> block) {
        return properties(p -> p.lootFrom(block));
    }

    public BlockRegistrar noItem() {
        this.itemEnabled = false;
        return this;
    }

    public BlockRegistrar item(UnaryOperator<Item.Properties> operator) {
        this.itemOperator = operator;
        return this;
    }

    public BlockRegistrar tab(String tabId) {
        return tab(ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.parse(tabId)));
    }

    public BlockRegistrar tab(ResourceKey<CreativeModeTab> tab) {
        this.tab = tab;
        return this;
    }

    public BlockRegistrar defaultCreativeTab() {
        return tab(CFHAllCreativeModeTabs.MAIN_TAB_KEY);
    }

    public BlockRegistrar noTab() {
        this.tab = null;
        return this;
    }

    public BlockRegistrar texturePath(String texturePath) {
        return texturePath(ResourceLocation.parse(texturePath));
    }

    public BlockRegistrar texturePath(ResourceLocation texturePath) {
        this.texturePath = texturePath;
        return this;
    }

    public BlockRegistrar translation(String en) {
        return translation(en, "");
    }

    public BlockRegistrar translation(String en, String zh) {
        this.enName = en;
        this.zhName = zh;
        return this;
    }

    public BlockRegistrar summary(String en, String zh) {
        this.summaryEn = en;
        this.summaryZh = zh;
        return this;
    }

    public BlockRegistrar properties(UnaryOperator<BlockBehaviour.Properties> operator) {
        operators.add(operator);
        return this;
    }

    public BlockRegistrar tag(String tagId) {
        ResourceLocation id = RegistryHelper.parseTagId(tagId);
        if (id == null) {
            CFHCore.LOGGER.error("[Registry] Invalid tag id '{}' for block '{}'; ignored", tagId, path);
            return this;
        }
        pendingTags.add(id);
        return this;
    }

    public BlockRegistrar tag(ResourceLocation tagId) {
        if (tagId != null) {
            pendingTags.add(tagId);
        }
        return this;
    }

    @Nullable
    public BlockEntry<Block> register() {
        if (!valid) {
            return null;
        }
        RegistryHelper.ensureInitialized();
        BlockBuilder<Block, CreateRegistrate> builder = CFHCore.registrate().block(path, Block::new);
        builder.properties(this::apply);
        builder.lang(enName);
        if (texturePath != null) {
            ResourceLocation texture = texturePath;
            builder.blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(), prov.models().cubeAll(ctx.getName(), texture)));
        }
        if (itemEnabled) {
            ItemBuilder<BlockItem, BlockBuilder<Block, CreateRegistrate>> item = builder.item();
            if (itemOperator != null) {
                item.properties(p -> itemOperator.apply(p));
            }
            if (tab != null) {
                item.tab(tab);
            }
            item.build();
        }
        BlockEntry<Block> entry = builder.register();
        RegistryHelper.collectLang(RegistryHelper.blockLangKey(path), enName, zhName);
        if (summaryEn != null) {
            RegistryHelper.collectLang(RegistryHelper.blockLangKey(path) + ".tooltip.summary", summaryEn, summaryZh);
        }
        if (!pendingTags.isEmpty()) {
            List<ResourceLocation> self = List.of(entry.getId());
            for (ResourceLocation tagId : pendingTags) {
                TagModificationHandler.addBlockAdditions(TagKey.create(Registries.BLOCK, tagId), self);
                if (itemEnabled) {
                    TagModificationHandler.addItemAdditions(TagKey.create(Registries.ITEM, tagId), self);
                }
            }
        }
        return entry;
    }

    private BlockBehaviour.Properties apply(BlockBehaviour.Properties properties) {
        BlockBehaviour.Properties result = properties;
        for (UnaryOperator<BlockBehaviour.Properties> operator : operators) {
            result = operator.apply(result);
        }
        return result;
    }
}
