package com.onehumanawa.cfhcore.foundation.registry;

import java.util.function.Supplier;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.ItemLike;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class FoodRegistrar {
    private final FoodProperties.Builder builder = new FoodProperties.Builder();
    private Supplier<? extends ItemLike> remainder;

    FoodRegistrar() {}

    public FoodRegistrar nutrition(int nutrition) {
        builder.nutrition(nutrition);
        return this;
    }

    public FoodRegistrar saturation(float saturation) {
        builder.saturationModifier(saturation);
        return this;
    }

    public FoodRegistrar saturationModifier(float saturation) {
        builder.saturationModifier(saturation);
        return this;
    }

    public FoodRegistrar alwaysEdible() {
        builder.alwaysEdible();
        return this;
    }

    public FoodRegistrar fastToEat() {
        builder.fast();
        return this;
    }

    public FoodRegistrar fast() {
        builder.fast();
        return this;
    }

    public FoodRegistrar effect(MobEffect effect, int duration, int amplifier, float chance) {
        return effect(() -> effect, duration, amplifier, chance);
    }

    public FoodRegistrar effect(Supplier<? extends MobEffect> effect, int duration, int amplifier, float chance) {
        builder.effect(() -> instance(effect.get(), duration, amplifier), chance);
        return this;
    }

    public FoodRegistrar effect(Holder<MobEffect> effect, int duration, int amplifier, float chance) {
        builder.effect(() -> new MobEffectInstance(effect, duration, amplifier), chance);
        return this;
    }

    public FoodRegistrar effect(MobEffectInstance effect, float chance) {
        builder.effect(() -> effect, chance);
        return this;
    }

    public FoodRegistrar usingConvertsTo(ItemLike remainder) {
        return usingConvertsTo(() -> remainder);
    }

    public FoodRegistrar usingConvertsTo(Supplier<? extends ItemLike> remainder) {
        this.remainder = remainder;
        return this;
    }

    FoodProperties build() {
        if (remainder != null) {
            builder.usingConvertsTo(remainder.get());
        }
        return builder.build();
    }

    private static MobEffectInstance instance(MobEffect effect, int duration, int amplifier) {
        return new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), duration, amplifier);
    }
}
