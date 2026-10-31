package com.onehumanawa.cfhcore.content.gui.core.stage;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class StageBlockEntities {
   private static final Set<BlockEntityType<?>> ALONE = new HashSet();
   private static final Set<BlockEntityType<?>> LEFT_OUT = new HashSet();
   private static int resources = -1;
   private static final Matrix4f MATRIX = new Matrix4f();
   private static final Matrix3f NORMAL = new Matrix3f();

   private StageBlockEntities() {
   }

   public static boolean leftOut(BlockEntity be) {
      fresh();
      return LEFT_OUT.contains(be.getType());
   }

   private static void fresh() {
      if (resources != StageResources.generation()) {
         resources = StageResources.generation();
         ALONE.clear();
         LEFT_OUT.clear();
      }
   }

   public static boolean draw(StageLevel level, BlockEntity be, BlockEntityRenderer<BlockEntity> renderer, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
      fresh();
      BlockEntityType<?> type = be.getType();
      if (LEFT_OUT.contains(type)) {
         return false;
      } else {
         PoseStack.Pose mark = pose.last();
         MATRIX.set(mark.pose());
         NORMAL.set(mark.normal());
         RuntimeException first = null;
         if (!ALONE.contains(type)) {
            try {
               renderer.render(be, partial, pose, buffers, light, OverlayTexture.NO_OVERLAY);
               return true;
            } catch (RuntimeException e) {
               first = e;
               restore(pose, mark);
            }
         }

         level.alone(be.getBlockPos());

         RuntimeException last = null;
         boolean var11;
         try {
            String var10000;
            try {
               renderer.render(be, partial, pose, buffers, light, OverlayTexture.NO_OVERLAY);
               if (first != null) {
                  ALONE.add(type);
                  StageTrouble.once("the renderer of " + id(type), "drawn without the light of the block beside it", first);
               }

               boolean e = true;
               return e;
            } catch (RuntimeException e) {
               restore(pose, mark);
               LEFT_OUT.add(type);
               last = e;
               var10000 = id(type);
            }

            StageTrouble.once("the renderer of " + var10000, "left out", first != null ? first : last);
            var11 = false;
         } finally {
            level.alone((BlockPos)null);
         }

         return var11;
      }
   }

   private static void restore(PoseStack pose, PoseStack.Pose mark) {
      while(pose.last() != mark && !pose.clear()) {
         pose.popPose();
      }

      mark.pose().set(MATRIX);
      mark.normal().set(NORMAL);
   }

   private static String id(BlockEntityType<?> type) {
      return String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type));
   }
}
