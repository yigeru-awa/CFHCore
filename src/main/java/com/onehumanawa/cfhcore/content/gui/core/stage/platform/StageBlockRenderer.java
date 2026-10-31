package com.onehumanawa.cfhcore.content.gui.core.stage.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onehumanawa.cfhcore.content.gui.core.platform.Services;
import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.StageLayers;
import java.util.ServiceConfigurationError;
import java.util.function.Function;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public interface StageBlockRenderer {
   StageBlockRenderer VANILLA = (dispatcher, state, pos, view, pose, layer, random) -> dispatcher.renderBatched(state, pos, view, pose, (VertexConsumer)layer.apply(StageLayers.block(state)), true, random);

   static StageBlockRenderer get() {
      return StageBlockRenderer.Found.RENDERER;
   }

   void render(BlockRenderDispatcher var1, BlockState var2, BlockPos var3, BlockAndTintGetter var4, PoseStack var5, Function<RenderType, VertexConsumer> var6, RandomSource var7);

   default void renderWith(ModelBlockRenderer renderer, BlockRenderDispatcher dispatcher, BlockState state, BlockPos pos, BlockAndTintGetter view, PoseStack pose, Function<RenderType, VertexConsumer> layer, RandomSource random) {
      renderer.tesselateBlock(view, dispatcher.getBlockModel(state), state, pos, pose, (VertexConsumer)layer.apply(StageLayers.block(state)), true, random, state.getSeed(pos), OverlayTexture.NO_OVERLAY);
   }

   default ModelBlockRenderer modelRenderer(BlockColors colors) {
      return new ModelBlockRenderer(colors);
   }

   default RenderType fluidLayer(FluidState fluid) {
      return StageLayers.fluidTable(fluid);
   }

   default @Nullable BakedModel extraModel(ModelManager models, ResourceLocation id) {
      for(String variant : new String[]{"standalone", "fabric_resource"}) {
         BakedModel baked = models.getModel(new ModelResourceLocation(id, variant));
         if (baked != null && baked != models.getMissingModel()) {
            return baked;
         }
      }

      return null;
   }

   public static final class Found {
      static final StageBlockRenderer RENDERER = find();

      private static StageBlockRenderer find() {
         try {
            return (StageBlockRenderer)Services.load(StageBlockRenderer.class);
         } catch (ServiceConfigurationError | IllegalStateException var1) {
            return StageBlockRenderer.VANILLA;
         }
      }

      private Found() {
      }
   }
}
