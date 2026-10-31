package com.onehumanawa.cfhcore.content.gui.core.stage.node;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageBlockEntities;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageLevel;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageView;
import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.BlockTesselator;
import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.StageLayers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BlockNode extends StageNode {
   private final StageLevel level;
   private final BlockPos levelPos;
   private BlockState state;
   private @Nullable BlockEntity blockEntity;
   private @Nullable BlockEntityTicker<BlockEntity> ticker;
   private boolean tickerBroken;
   private boolean modelBroken;
   private @Nullable BlockTesselator blocks;
   private @Nullable List<RenderType> layers;
   private boolean open;
   private final RandomSource random = RandomSource.create(42L);
   private static final VertexConsumer NOWHERE = new VertexConsumer() {
      public VertexConsumer addVertex(float x, float y, float z) {
         return this;
      }

      public VertexConsumer setColor(int r, int g, int b, int a) {
         return this;
      }

      public VertexConsumer setUv(float u, float v) {
         return this;
      }

      public VertexConsumer setUv1(int u, int v) {
         return this;
      }

      public VertexConsumer setUv2(int u, int v) {
         return this;
      }

      public VertexConsumer setNormal(float x, float y, float z) {
         return this;
      }
   };

   public BlockNode(StageLevel level, BlockState state) {
      this.level = level;
      this.levelPos = level.allocateRegion();
      this.setState(state);
      this.named(state.getBlock().getName());
   }

   public BlockNode state(BlockState s) {
      this.setState(s);
      return this;
   }

   public BlockState state() {
      return this.state;
   }

   public @Nullable BlockEntity blockEntity() {
      return this.blockEntity;
   }

   private void setState(BlockState s) {
      this.state = s;
      this.level.place(this.levelPos, s, (CompoundTag)null);
      this.blockEntity = this.level.getBlockEntity(this.levelPos);
      this.ticker = null;
      this.tickerBroken = false;
      this.modelBroken = false;
      this.layers = null;
      if (this.blockEntity != null) {
         Block var3 = s.getBlock();
         if (var3 instanceof EntityBlock) {
            EntityBlock eb = (EntityBlock)var3;

            try {
               this.ticker = eb.getTicker(this.level, s, (BlockEntityType<BlockEntity>)(BlockEntityType<?>)this.blockEntity.getType());
            } catch (Exception var4) {
               this.ticker = null;
            }
         }
      }

   }

   public BlockNode open(boolean open) {
      this.open = open;
      BlockEntity var3 = this.blockEntity;
      if (var3 instanceof ChestBlockEntity chest) {
         chest.triggerEvent(1, open ? 1 : 0);
      }

      return this;
   }

   public BlockNode openNow(boolean open) {
      this.open(open);
      BlockEntity var3 = this.blockEntity;
      if (var3 instanceof ChestBlockEntity chest) {
         for(int i = 0; i < 12; ++i) {
            ChestBlockEntity.lidAnimateTick(this.level, this.levelPos, this.state, chest);
         }
      }

      return this;
   }

   public boolean isOpen() {
      return this.open;
   }

   public BlockNode toggle() {
      return this.open(!this.open);
   }

   public BlockNode signText(DyeColor color, boolean glowing, Component... lines) {
      BlockEntity var5 = this.blockEntity;
      if (var5 instanceof SignBlockEntity sign) {
         SignText text = (new SignText()).setColor(color).setHasGlowingText(glowing);

         for(int i = 0; i < Math.min(4, lines.length); ++i) {
            text = text.setMessage(i, lines[i]);
         }

         sign.setText(text, true);
      }

      return this;
   }

   public BlockNode signText(Component... lines) {
      return this.signText(DyeColor.BLACK, false, lines);
   }

   public void tick(StageRenderContext ctx) {
      if (this.ticker != null && !this.tickerBroken && this.blockEntity != null) {
         try {
            this.ticker.tick(this.level, this.levelPos, this.state, this.blockEntity);
         } catch (Exception e) {
            this.tickerBroken = true;
            CFHGui.LOGGER.warn("[CFHGui] stage: ticker of {} failed, stopped: {}", this.state, e.toString());
         }

      }
   }

   protected void draw(StageRenderContext ctx) {
      Minecraft mc = Minecraft.getInstance();
      ctx.pose.pushPose();
      ctx.pose.translate(-0.5F, 0.0F, -0.5F);
      if (this.state.getRenderShape() == RenderShape.MODEL && !this.modelBroken) {
         if (this.blocks == null) {
            this.blocks = new BlockTesselator(this.random);
         }

         StageView view = new StageView(this.level);
         if (this.layers == null) {
            this.layers = this.layers(view, ctx.pose);
         }

         for(RenderType layer : this.layers) {
            VertexConsumer vc = ctx.buffers.getBuffer(this.alpha() < 1.0F ? RenderType.translucentMovingBlock() : StageLayers.moving(layer));
            if (!this.blocks.model(this.state, this.levelPos, view, ctx.pose, (t) -> this.blocks.hold(t == layer ? vc : NOWHERE))) {
               this.modelBroken = true;
               break;
            }
         }
      }

      if (this.blockEntity != null && !StageBlockEntities.leftOut(this.blockEntity)) {
         BlockEntityRenderer<BlockEntity> renderer = mc.getBlockEntityRenderDispatcher().getRenderer(this.blockEntity);
         if (renderer != null) {
            StageBlockEntities.draw(this.level, this.blockEntity, renderer, ctx.partial, ctx.pose, ctx.buffers, ctx.light);
         }
      }

      ctx.pose.popPose();
   }

   private List<RenderType> layers(StageView view, PoseStack pose) {
      List<RenderType> found = new ArrayList(2);
      if (!this.blocks.model(this.state, this.levelPos, view, pose, (t) -> {
         if (!found.contains(t)) {
            found.add(t);
         }

         return this.blocks.hold(NOWHERE);
      })) {
         this.modelBroken = true;
      }

      return found;
   }

   public void dispose() {
      this.level.clear(this.levelPos, this.levelPos);
      this.blockEntity = null;
      this.ticker = null;
   }
}
