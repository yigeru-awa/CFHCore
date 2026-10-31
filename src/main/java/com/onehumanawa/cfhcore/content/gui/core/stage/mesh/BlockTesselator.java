package com.onehumanawa.cfhcore.content.gui.core.stage.mesh;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageAnimations;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageResources;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageTrouble;
import com.onehumanawa.cfhcore.content.gui.core.stage.platform.StageBlockRenderer;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class BlockTesselator {
   private static final Set<Block> OWN_WAY = ConcurrentHashMap.newKeySet();
   private static final Set<BlockState> LEFT_OUT = ConcurrentHashMap.newKeySet();
   private static final Set<Fluid> DRY = ConcurrentHashMap.newKeySet();
   private static volatile int resources = -1;
   private static volatile @Nullable ModelBlockRenderer own;
   private final BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
   private final StageBlockRenderer renderer = StageBlockRenderer.get();
   private final RandomSource random;
   private final Held held = new Held();
   private final Matrix4f matrix = new Matrix4f();
   private final Matrix3f normal = new Matrix3f();

   public BlockTesselator(RandomSource random) {
      this.random = random;
      int gen = StageResources.generation();
      if (resources != gen) {
         resources = gen;
         LEFT_OUT.clear();
         DRY.clear();
      }

   }

   public BlockRenderDispatcher dispatcher() {
      return this.dispatcher;
   }

   public VertexConsumer hold(VertexConsumer target) {
      return this.held.to(target);
   }

   public boolean model(BlockState state, BlockPos pos, BlockAndTintGetter view, PoseStack pose, Function<RenderType, VertexConsumer> layer) {
      if (LEFT_OUT.contains(state)) {
         return false;
      } else {
         Block block = state.getBlock();
         PoseStack.Pose mark = pose.last();
         this.matrix.set(mark.pose());
         this.normal.set(mark.normal());
         RuntimeException game = null;
         if (!OWN_WAY.contains(block)) {
            this.held.begin();

            try {
               this.renderer.render(this.dispatcher, state, pos, view, pose, layer, this.random);
               this.held.commit();
               return true;
            } catch (RuntimeException e) {
               this.held.discard();
               this.restore(pose, mark);
               OWN_WAY.add(block);
               game = e;
            }
         }

         this.held.begin();

         try {
            this.renderer.renderWith(this.own(), this.dispatcher, state, pos, view, pose, layer, this.random);
            this.held.commit();
            if (game != null && !StageColors.broken(block)) {
               StageTrouble.once("the block " + id(block), "drawn by the scene's own block renderer", game);
            }

            return true;
         } catch (RuntimeException e) {
            this.held.discard();
            this.restore(pose, mark);
            LEFT_OUT.add(state);
            StageTrouble.once("the block " + id(block), "left out", e);
            return false;
         }
      }
   }

   public boolean liquid(BlockState state, FluidState fluid, BlockPos pos, BlockAndTintGetter view, VertexConsumer out) {
      Fluid type = fluid.getType();
      if (DRY.contains(type)) {
         return false;
      } else {
         this.held.begin();

         try {
            this.dispatcher.renderLiquid(pos, view, out, state, fluid);
            this.held.commit();
            return true;
         } catch (RuntimeException e) {
            this.held.discard();
            DRY.add(type);
            StageTrouble.once("the liquid " + String.valueOf(BuiltInRegistries.FLUID.getKey(type)), "left out", e);
            return false;
         }
      }
   }

   public void sprites(BlockState state, FluidState fluid, Set<TextureAtlasSprite> into) {
      try {
         if (!fluid.isEmpty()) {
            StageAnimations.collect(fluid, into);
         }

         if (state.getRenderShape() == RenderShape.MODEL) {
            StageAnimations.collect(this.dispatcher.getBlockModel(state), state, into);
         }
      } catch (RuntimeException e) {
         StageTrouble.once("the textures of " + id(state.getBlock()), "not kept moving", e);
      }

   }

   public void failed(BlockState state, RuntimeException e) {
      this.held.discard();
      LEFT_OUT.add(state);
      StageTrouble.once("the block " + id(state.getBlock()), "left out", e);
   }

   private void restore(PoseStack pose, PoseStack.Pose mark) {
      while(pose.last() != mark && !pose.clear()) {
         pose.popPose();
      }

      mark.pose().set(this.matrix);
      mark.normal().set(this.normal);
   }

   private ModelBlockRenderer own() {
      ModelBlockRenderer r = own;
      if (r == null) {
         r = this.renderer.modelRenderer(new StageColors(Minecraft.getInstance().getBlockColors()));
         own = r;
      }

      return r;
   }

   private static String id(Block block) {
      return String.valueOf(BuiltInRegistries.BLOCK.getKey(block));
   }

   private static final class Held implements VertexConsumer {
      private static final int F = 8;
      private VertexConsumer[] targets = new VertexConsumer[4];
      private int targetCount;
      private int current = -1;
      private int count;
      private int[] owner = new int[256];
      private float[] floats = new float[2048];
      private int[] colour = new int[256];
      private int[] light = new int[256];

      VertexConsumer to(VertexConsumer target) {
         for(int i = 0; i < this.targetCount; ++i) {
            if (this.targets[i] == target) {
               this.current = i;
               return this;
            }
         }

         if (this.targetCount == this.targets.length) {
            this.targets = (VertexConsumer[])Arrays.copyOf(this.targets, this.targetCount * 2);
         }

         this.targets[this.targetCount] = target;
         this.current = this.targetCount++;
         return this;
      }

      void begin() {
         this.count = 0;
      }

      void discard() {
         this.count = 0;
      }

      void commit() {
         for(int i = 0; i < this.count; ++i) {
            VertexConsumer t = this.targets[this.owner[i]];
            int o = i * 8;
            int c = this.colour[i];
            t.addVertex(this.floats[o], this.floats[o + 1], this.floats[o + 2]);
            t.setColor(c >> 16 & 255, c >> 8 & 255, c & 255, c >>> 24 & 255);
            t.setUv(this.floats[o + 3], this.floats[o + 4]);
            t.setUv2(this.light[i] & '\uffff', this.light[i] >>> 16 & '\uffff');
            t.setNormal(this.floats[o + 5], this.floats[o + 6], this.floats[o + 7]);
         }

         this.count = 0;
      }

      public VertexConsumer addVertex(float x, float y, float z) {
         if (this.current < 0) {
            return this;
         } else {
            if (this.count == this.owner.length) {
               this.owner = Arrays.copyOf(this.owner, this.count * 2);
               this.colour = Arrays.copyOf(this.colour, this.count * 2);
               this.light = Arrays.copyOf(this.light, this.count * 2);
               this.floats = Arrays.copyOf(this.floats, this.count * 2 * 8);
            }

            int o = this.count * 8;
            this.floats[o] = x;
            this.floats[o + 1] = y;
            this.floats[o + 2] = z;
            this.floats[o + 3] = this.floats[o + 4] = 0.0F;
            this.floats[o + 5] = 0.0F;
            this.floats[o + 6] = 1.0F;
            this.floats[o + 7] = 0.0F;
            this.owner[this.count] = this.current;
            this.colour[this.count] = -1;
            this.light[this.count] = 0;
            ++this.count;
            return this;
         }
      }

      public VertexConsumer setColor(int r, int g, int b, int a) {
         if (this.count > 0) {
            this.colour[this.count - 1] = (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
         }

         return this;
      }

      public VertexConsumer setUv(float u, float v) {
         if (this.count > 0) {
            this.floats[(this.count - 1) * 8 + 3] = u;
            this.floats[(this.count - 1) * 8 + 4] = v;
         }

         return this;
      }

      public VertexConsumer setUv1(int u, int v) {
         return this;
      }

      public VertexConsumer setUv2(int u, int v) {
         if (this.count > 0) {
            this.light[this.count - 1] = (v & '\uffff') << 16 | u & '\uffff';
         }

         return this;
      }

      public VertexConsumer setNormal(float x, float y, float z) {
         if (this.count > 0) {
            int o = (this.count - 1) * 8;
            this.floats[o + 5] = x;
            this.floats[o + 6] = y;
            this.floats[o + 7] = z;
         }

         return this;
      }
   }
}
