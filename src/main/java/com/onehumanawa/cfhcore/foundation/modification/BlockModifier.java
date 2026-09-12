package com.onehumanawa.cfhcore.foundation.modification;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import org.jetbrains.annotations.Nullable;

import com.onehumanawa.cfhcore.CFHCore;
import com.onehumanawa.cfhcore.foundation.RegistryHelper;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootTable;

@SuppressWarnings({"unused", "UnusedReturnValue", "unchecked"})
public final class BlockModifier {
    private final ResourceLocation id;
    private final TagKey<Block> tag;
    private final boolean valid;
    private final List<Consumer<BlockBehaviour.Properties>> operators = new ArrayList<>();
    private final List<Consumer<Block>> patches = new ArrayList<>();
    private boolean lootModified;
    private boolean statesModified;

    public BlockModifier(@Nullable ResourceLocation id) {
        this.id = id;
        this.tag = null;
        this.valid = id != null;
    }

    public BlockModifier(@Nullable TagKey<Block> tag) {
        this.id = null;
        this.tag = tag;
        this.valid = tag != null;
    }

    public BlockModifier hardness(float destroyTime) {
        return properties(p -> p.destroyTime(destroyTime));
    }

    public BlockModifier destroySpeed(float destroyTime) {
        return hardness(destroyTime);
    }

    public BlockModifier resistance(float explosionResistance) {
        return behaviour(p -> p.explosionResistance(explosionResistance));
    }

    public BlockModifier strength(float strength) {
        return properties(p -> p.strength(strength));
    }

    public BlockModifier strength(float destroyTime, float explosionResistance) {
        return properties(p -> p.strength(destroyTime, explosionResistance));
    }

    public BlockModifier instabreak() {
        return properties(BlockBehaviour.Properties::instabreak);
    }

    public BlockModifier indestructible() {
        return strength(-1.0F, 3600000.0F);
    }

    public BlockModifier requiresCorrectTool() {
        return requiresCorrectTool(true);
    }

    public BlockModifier requiresCorrectTool(boolean required) {
        return required
                ? properties(BlockBehaviour.Properties::requiresCorrectToolForDrops)
                : properties(p -> Reflections.set(p, "requiresCorrectToolForDrops", false));
    }

    public BlockModifier lightLevel(int lightLevel) {
        return properties(p -> p.lightLevel(state -> lightLevel));
    }

    public BlockModifier luminance(int lightLevel) {
        return lightLevel(lightLevel);
    }

    public BlockModifier sound(SoundType sound) {
        return behaviour(p -> p.sound(sound));
    }

    public BlockModifier sound(String sound) {
        SoundType resolved = soundType(sound);
        return resolved == null ? this : sound(resolved);
    }

    public BlockModifier mapColor(MapColor color) {
        return properties(p -> p.mapColor(color));
    }

    public BlockModifier mapColor(DyeColor color) {
        return properties(p -> p.mapColor(color));
    }

    public BlockModifier friction(float friction) {
        return behaviour(p -> p.friction(friction));
    }

    public BlockModifier speedFactor(float speedFactor) {
        return behaviour(p -> p.speedFactor(speedFactor));
    }

    public BlockModifier jumpFactor(float jumpFactor) {
        return behaviour(p -> p.jumpFactor(jumpFactor));
    }

    public BlockModifier noCollision() {
        return hasCollision(false);
    }

    public BlockModifier hasCollision(boolean hasCollision) {
        return hasCollision
                ? properties(p -> Reflections.set(p, "hasCollision", true))
                : properties(BlockBehaviour.Properties::noCollission);
    }

    public BlockModifier noOcclusion() {
        return occlusion(false);
    }

    public BlockModifier occlusion(boolean canOcclude) {
        return canOcclude
                ? properties(p -> Reflections.set(p, "canOcclude", true))
                : properties(BlockBehaviour.Properties::noOcclusion);
    }

    public BlockModifier randomTicks() {
        return randomTicks(true);
    }

    public BlockModifier randomTicks(boolean isRandomlyTicking) {
        return isRandomlyTicking
                ? properties(BlockBehaviour.Properties::randomTicks)
                : properties(p -> Reflections.set(p, "isRandomlyTicking", false));
    }

    public BlockModifier dynamicShape() {
        return dynamicShape(true);
    }

    public BlockModifier dynamicShape(boolean dynamicShape) {
        return dynamicShape
                ? properties(BlockBehaviour.Properties::dynamicShape)
                : properties(p -> Reflections.set(p, "dynamicShape", false));
    }

    public BlockModifier replaceable() {
        return replaceable(true);
    }

    public BlockModifier replaceable(boolean replaceable) {
        return replaceable
                ? properties(BlockBehaviour.Properties::replaceable)
                : properties(p -> Reflections.set(p, "replaceable", false));
    }

    public BlockModifier ignitedByLava() {
        return ignitedByLava(true);
    }

    public BlockModifier ignitedByLava(boolean ignitedByLava) {
        return ignitedByLava
                ? properties(BlockBehaviour.Properties::ignitedByLava)
                : properties(p -> Reflections.set(p, "ignitedByLava", false));
    }

    public BlockModifier air() {
        return air(true);
    }

    public BlockModifier air(boolean isAir) {
        return isAir
                ? properties(BlockBehaviour.Properties::air)
                : properties(p -> Reflections.set(p, "isAir", false));
    }

    public BlockModifier forceSolidOn() {
        return properties(BlockBehaviour.Properties::forceSolidOn);
    }

    public BlockModifier forceSolidOff() {
        return properties(BlockBehaviour.Properties::forceSolidOff);
    }

    public BlockModifier noTerrainParticles() {
        return terrainParticles(false);
    }

    public BlockModifier terrainParticles(boolean spawnTerrainParticles) {
        return spawnTerrainParticles
                ? properties(p -> Reflections.set(p, "spawnTerrainParticles", true))
                : properties(BlockBehaviour.Properties::noTerrainParticles);
    }

    public BlockModifier pushReaction(String reaction) {
        PushReaction resolved = RegistryHelper.byName(PushReaction.class, reaction, "push reaction");
        return resolved == null ? this : pushReaction(resolved);
    }

    public BlockModifier pushReaction(PushReaction reaction) {
        return properties(p -> p.pushReaction(reaction));
    }

    public BlockModifier instrument(String instrument) {
        NoteBlockInstrument resolved = RegistryHelper.byName(NoteBlockInstrument.class, instrument, "note block instrument");
        return resolved == null ? this : instrument(resolved);
    }

    public BlockModifier instrument(NoteBlockInstrument instrument) {
        return properties(p -> p.instrument(instrument));
    }

    public BlockModifier offsetType(String offsetType) {
        BlockBehaviour.OffsetType resolved = RegistryHelper.byName(BlockBehaviour.OffsetType.class, offsetType, "offset type");
        return resolved == null ? this : offsetType(resolved);
    }

    public BlockModifier offsetType(BlockBehaviour.OffsetType offsetType) {
        return properties(p -> p.offsetType(offsetType));
    }

    public BlockModifier redstoneConductor(boolean value) {
        return properties(p -> p.isRedstoneConductor((state, level, pos) -> value));
    }

    public BlockModifier suffocating(boolean value) {
        return properties(p -> p.isSuffocating((state, level, pos) -> value));
    }

    public BlockModifier viewBlocking(boolean value) {
        return properties(p -> p.isViewBlocking((state, level, pos) -> value));
    }

    public BlockModifier postProcess(boolean value) {
        return properties(p -> p.hasPostProcess((state, level, pos) -> value));
    }

    public BlockModifier emissiveRendering(boolean value) {
        return properties(p -> p.emissiveRendering((state, level, pos) -> value));
    }

    public BlockModifier validSpawn(boolean value) {
        return behaviour(p -> p.isValidSpawn((state, level, pos, type) -> value));
    }

    public BlockModifier noLootTable() {
        return loot(p -> p.noLootTable());
    }

    public BlockModifier dropsLike(Block block) {
        return dropsLike(() -> block);
    }

    public BlockModifier dropsLike(Supplier<? extends Block> block) {
        return loot(p -> {
            Reflections.set(p, "drops", null);
            Reflections.set(p, "lootTableSupplier", (Supplier<ResourceKey<LootTable>>) () -> block.get().getLootTable());
        });
    }

    public BlockModifier lootFrom(ResourceKey<LootTable> lootTable) {
        return loot(p -> Reflections.set(p, "drops", lootTable));
    }

    public BlockModifier lootFrom(String lootTableId) {
        ResourceLocation parsed = RegistryHelper.parseTagId(lootTableId);
        return parsed == null ? this : lootFrom(ResourceKey.create(Registries.LOOT_TABLE, parsed));
    }

    public BlockModifier properties(Consumer<BlockBehaviour.Properties> operator) {
        if (valid) {
            operators.add(operator);
            statesModified = true;
        }
        return this;
    }

    public BlockModifier nameKey(String key) {
        if (!valid) {
            return this;
        }
        if (tag != null) {
            CFHCore.LOGGER.error("[Modification] nameKey() cannot be applied to every block of tag {}; ignored", tag.location());
            return this;
        }
        return patch(block -> Reflections.set(block, "descriptionId", key));
    }

    public BlockModifier translation(String en) {
        return translation(en, "");
    }

    public BlockModifier translation(String en, String zh) {
        if (!valid) {
            return this;
        }
        if (tag != null) {
            CFHCore.LOGGER.error("[Modification] translation() cannot be applied to every block of tag {}; ignored", tag.location());
            return this;
        }
        String key = ModificationHandler.overrideKey(id);
        RegistryHelper.collectLang(key, en, zh);
        return nameKey(key);
    }

    public BlockModifier hide() {
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

    public BlockModifier disable() {
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
    public TagKey<Block> tag() {
        return tag;
    }

    public String target() {
        return tag != null ? "#" + tag.location() : String.valueOf(id);
    }

    public void apply() {
        if (!valid) {
            return;
        }
        if (tag != null) {
            ModificationHandler.addBlockTagModifier(this);
        } else {
            ModificationHandler.addBlockModifier(this);
        }
    }

    void applyTo(Block block) {
        BlockBehaviour.Properties properties = block.properties();
        operators.forEach(operator -> operator.accept(properties));
        syncBehaviour(block, properties);
        if (lootModified) {
            syncLoot(block, properties);
        }
        if (statesModified) {
            syncStates(block, properties);
        }
        patches.forEach(patch -> patch.accept(block));
    }

    private BlockModifier behaviour(Consumer<BlockBehaviour.Properties> operator) {
        if (valid) {
            operators.add(operator);
        }
        return this;
    }

    private BlockModifier patch(Consumer<Block> patch) {
        if (valid) {
            patches.add(patch);
        }
        return this;
    }

    private BlockModifier loot(Consumer<BlockBehaviour.Properties> operator) {
        if (!valid) {
            return this;
        }
        lootModified = true;
        return behaviour(operator);
    }

    private static void syncBehaviour(Block block, BlockBehaviour.Properties properties) {
        copy(properties, block, "hasCollision");
        copy(properties, block, "explosionResistance");
        copy(properties, block, "isRandomlyTicking");
        copy(properties, block, "soundType");
        copy(properties, block, "friction");
        copy(properties, block, "speedFactor");
        copy(properties, block, "jumpFactor");
        copy(properties, block, "dynamicShape");
        copy(properties, block, "requiredFeatures");
    }

    private static void syncLoot(Block block, BlockBehaviour.Properties properties) {
        Supplier<ResourceKey<LootTable>> supplier = () -> {
            ResourceKey<LootTable> drops = (ResourceKey<LootTable>) Reflections.get(properties, "drops");
            if (drops != null) {
                return drops;
            }
            Supplier<ResourceKey<LootTable>> fallback = (Supplier<ResourceKey<LootTable>>) Reflections.get(properties, "lootTableSupplier");
            return fallback == null ? defaultLootTable(block) : fallback.get();
        };
        Reflections.set(block, "lootTableSupplier", supplier);
        Reflections.set(block, "drops", Reflections.get(properties, "drops"));
    }

    private static void syncStates(Block block, BlockBehaviour.Properties properties) {
        ToIntFunction<BlockState> lightEmission = (ToIntFunction<BlockState>) Reflections.get(properties, "lightEmission");
        Function<BlockState, MapColor> mapColor = (Function<BlockState, MapColor>) Reflections.get(properties, "mapColor");
        Object destroyTime = Reflections.get(properties, "destroyTime");
        boolean dynamicShape = block.hasDynamicShape();
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            if (lightEmission != null) {
                Reflections.set(state, "lightEmission", lightEmission.applyAsInt(state));
            }
            if (mapColor != null) {
                Reflections.set(state, "mapColor", mapColor.apply(state));
            }
            if (destroyTime != null) {
                Reflections.set(state, "destroySpeed", destroyTime);
            }
            copy(properties, state, "isAir");
            copy(properties, state, "ignitedByLava");
            copy(properties, state, "liquid");
            copy(properties, state, "pushReaction");
            copy(properties, state, "requiresCorrectToolForDrops");
            copy(properties, state, "canOcclude");
            copy(properties, state, "isRedstoneConductor");
            copy(properties, state, "isSuffocating");
            copy(properties, state, "isViewBlocking");
            copy(properties, state, "hasPostProcess");
            copy(properties, state, "emissiveRendering");
            copy(properties, state, "offsetFunction");
            copy(properties, state, "spawnTerrainParticles");
            copy(properties, state, "instrument");
            copy(properties, state, "replaceable");
            if (dynamicShape) {
                Reflections.set(state, "cache", null);
            }
            state.initCache();
        }
    }

    private static void copy(BlockBehaviour.Properties properties, Object target, String name) {
        Reflections.copy(properties, name, target);
    }

    private static ResourceKey<LootTable> defaultLootTable(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return ResourceKey.create(Registries.LOOT_TABLE, id.withPrefix("blocks/"));
    }

    @Nullable
    private SoundType soundType(String name) {
        String normalized = name == null ? "" : name.trim().toUpperCase(Locale.ROOT);
        try {
            Field field = SoundType.class.getField(normalized);
            if (SoundType.class.isAssignableFrom(field.getType())) {
                return (SoundType) field.get(null);
            }
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
        }
        CFHCore.LOGGER.error("[Modification] Unknown sound type '{}' for block '{}'; ignored", name, id);
        return null;
    }
}
