package com.onehumanawa.cfhcore.content.world.worldgen.feature.template.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import com.onehumanawa.cfhcore.content.world.registries.CFHWorldGenBuiltInRegistries;
import com.onehumanawa.cfhcore.content.world.worldgen.feature.template.template.Dimensions;
import com.onehumanawa.cfhcore.content.world.worldgen.feature.template.template.TemplateContext;

public interface TemplatePlacement<T extends TemplateContext> {
   Codec<TemplatePlacement<?>> CODEC = CFHWorldGenBuiltInRegistries.TEMPLATE_PLACEMENT_TYPE.byNameCodec().dispatch(TemplatePlacement::codec, Function.identity());

   boolean canPlaceAt(LevelAccessor var1, BlockPos var2, Dimensions var3);

   boolean canReplaceAt(LevelAccessor var1, BlockPos var2);

   T createContext();

   MapCodec<? extends TemplatePlacement<T>> codec();
}
