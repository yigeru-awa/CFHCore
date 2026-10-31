package com.onehumanawa.cfhcore.content.gui.core.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onehumanawa.cfhcore.content.gui.core.stage.platform.StageBlockRenderer;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.lighting.LightPipelineAwareModelBlockRenderer;
import org.jetbrains.annotations.Nullable;

public final class NeoForgeStageBlockRenderer implements StageBlockRenderer {
   public void render(BlockRenderDispatcher dispatcher, BlockState state, BlockPos pos, BlockAndTintGetter view, PoseStack pose, Function<RenderType, VertexConsumer> layer, RandomSource random) {
      BakedModel model = dispatcher.getBlockModel(state);
      ModelData data = modelData(model, state, pos, view);
      random.setSeed(state.getSeed(pos));

      for(RenderType type : model.getRenderTypes(state, random, data)) {
         dispatcher.renderBatched(state, pos, view, pose, (VertexConsumer)layer.apply(type), true, random, data, type);
      }

   }

   private static ModelData modelData(BakedModel model, BlockState state, BlockPos pos, BlockAndTintGetter view) {
      try {
         BlockEntity be = state.hasBlockEntity() ? view.getBlockEntity(pos) : null;
         return model.getModelData(view, pos, state, be != null ? be.getModelData() : ModelData.EMPTY);
      } catch (RuntimeException var5) {
         return ModelData.EMPTY;
      }
   }

   public void renderWith(ModelBlockRenderer renderer, BlockRenderDispatcher dispatcher, BlockState state, BlockPos pos, BlockAndTintGetter view, PoseStack pose, Function<RenderType, VertexConsumer> layer, RandomSource random) {
      BakedModel model = dispatcher.getBlockModel(state);
      random.setSeed(state.getSeed(pos));

      for(RenderType type : model.getRenderTypes(state, random, ModelData.EMPTY)) {
         renderer.tesselateBlock(view, model, state, pos, pose, (VertexConsumer)layer.apply(type), true, random, state.getSeed(pos), OverlayTexture.NO_OVERLAY, ModelData.EMPTY, type);
      }

   }

   public ModelBlockRenderer modelRenderer(BlockColors colors) {
      return new LightPipelineAwareModelBlockRenderer(colors);
   }

   public @Nullable BakedModel extraModel(ModelManager models, ResourceLocation id) {
      BakedModel baked = models.getModel(ModelResourceLocation.standalone(id));
      return baked != null && baked != models.getMissingModel() ? baked : null;
   }
}
