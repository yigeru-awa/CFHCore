package com.onehumanawa.cfhcore.content.worldgen.feature.template.decorator;

import java.util.function.Function;

import com.mojang.serialization.Codec;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import com.onehumanawa.cfhcore.content.worldgen.registries.CFHBuiltInRegistries;
import com.onehumanawa.cfhcore.content.worldgen.feature.template.template.TemplateContext;

public interface TemplateDecorator<T extends TemplateContext> {
    public static final Codec<TemplateDecorator<?>> CODEC = CFHBuiltInRegistries.TEMPLATE_DECORATOR_TYPE.byNameCodec().dispatch(TemplateDecorator::codec, Function.identity());
    
    void apply(LevelAccessor level, T buffer, RandomSource random, boolean modified);
    
    MapCodec<? extends TemplateDecorator<T>> codec();
}
