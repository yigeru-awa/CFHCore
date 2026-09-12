package com.onehumanawa.cfhcore.content.world.worldgen.feature.template.decorator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import com.onehumanawa.cfhcore.content.world.registries.CFHWorldGenBuiltInRegistries;
import com.onehumanawa.cfhcore.content.world.worldgen.feature.template.template.TemplateContext;

public interface TemplateDecorator<T extends TemplateContext> {
   Codec<TemplateDecorator<?>> CODEC = CFHWorldGenBuiltInRegistries.TEMPLATE_DECORATOR_TYPE.byNameCodec().dispatch(TemplateDecorator::codec, Function.identity());

   void apply(LevelAccessor var1, T var2, RandomSource var3, boolean var4);

   MapCodec<? extends TemplateDecorator<T>> codec();
}
