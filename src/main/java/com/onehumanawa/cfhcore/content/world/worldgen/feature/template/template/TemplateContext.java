package com.onehumanawa.cfhcore.content.world.worldgen.feature.template.template;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface TemplateContext {
   void recordState(BlockPos var1, BlockState var2);

   public interface Factory<T extends TemplateContext> {
      T createContext();
   }
}
