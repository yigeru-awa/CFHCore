package com.onehumanawa.cfhcore.content.gui.core.mixin;

import java.util.Map;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ItemBlockRenderTypes.class})
public interface ItemBlockRenderTypesAccessor {
   @Accessor("TYPE_BY_BLOCK")
   static Map<Block, RenderType> cfhgui$typeByBlock() {
      throw new AssertionError();
   }

   @Accessor("TYPE_BY_FLUID")
   static Map<Fluid, RenderType> cfhgui$typeByFluid() {
      throw new AssertionError();
   }
}
