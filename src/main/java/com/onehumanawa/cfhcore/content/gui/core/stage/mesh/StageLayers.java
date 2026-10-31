package com.onehumanawa.cfhcore.content.gui.core.stage.mesh;

import com.onehumanawa.cfhcore.content.gui.core.mixin.ItemBlockRenderTypesAccessor;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageTrouble;
import com.onehumanawa.cfhcore.content.gui.core.stage.platform.StageBlockRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public final class StageLayers {
   private static volatile boolean blockHooked;

   private StageLayers() {
   }

   public static RenderType fluid(FluidState fluid) {
      return StageBlockRenderer.get().fluidLayer(fluid);
   }

   public static RenderType fluidTable(FluidState fluid) {
      RenderType type = (RenderType)ItemBlockRenderTypesAccessor.cfhgui$typeByFluid().get(fluid.getType());
      return type != null ? type : RenderType.solid();
   }

   public static RenderType block(BlockState state) {
      if (!blockHooked) {
         try {
            return ItemBlockRenderTypes.getChunkRenderType(state);
         } catch (RuntimeException e) {
            blockHooked = true;
            StageTrouble.once("the game's lookup of the layer of a block", "the game's own table is read instead", e);
         }
      }

      return table(state);
   }

   public static RenderType moving(RenderType layer) {
      return layer == RenderType.translucent() ? RenderType.translucentMovingBlock() : layer;
   }

   public static RenderType drawnAs(RenderType layer) {
      return layer == RenderType.solid() ? RenderType.cutoutMipped() : layer;
   }

   private static RenderType table(BlockState state) {
      Block block = state.getBlock();
      if (block instanceof LeavesBlock) {
         return Minecraft.useFancyGraphics() ? RenderType.cutoutMipped() : RenderType.solid();
      } else {
         RenderType type = (RenderType)ItemBlockRenderTypesAccessor.cfhgui$typeByBlock().get(block);
         return type != null ? type : RenderType.solid();
      }
   }
}
