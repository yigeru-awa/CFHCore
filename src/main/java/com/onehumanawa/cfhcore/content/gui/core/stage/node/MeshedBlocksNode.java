package com.onehumanawa.cfhcore.content.gui.core.stage.node;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageBlockEntities;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageLevel;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageResources;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageTrouble;
import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.BlockMesh;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class MeshedBlocksNode extends StageNode {
   protected final StageLevel level;
   protected final BlockPos origin;
   protected Vec3i size;
   private @Nullable BlockMesh mesh;
   private int meshLighting;
   private int meshResources;
   private boolean dirty;
   private boolean meshFailed;
   private boolean softLit;
   private boolean meshSoft;
   private final List<BlockEntity> blockEntities;
   private final List<BlockEntityTicker<BlockEntity>> tickers;
   private final List<BlockEntity> scratch;
   private boolean centered;
   private float offsetX;
   private float offsetY;
   private float offsetZ;
   private boolean customOffset;
   private int rim;

   protected MeshedBlocksNode(StageLevel level) {
      this.size = Vec3i.ZERO;
      this.meshLighting = -1;
      this.meshResources = -1;
      this.dirty = true;
      this.blockEntities = new ArrayList();
      this.tickers = new ArrayList();
      this.scratch = new ArrayList();
      this.level = level;
      this.origin = level.allocateRegion();
   }

   public MeshedBlocksNode centered() {
      this.centered = true;
      this.customOffset = false;
      this.applyBounds();
      return this;
   }

   public MeshedBlocksNode meshOffset(float x, float y, float z) {
      this.customOffset = true;
      this.offsetX = x;
      this.offsetY = y;
      this.offsetZ = z;
      this.bounds(x, y, z, x + (float)this.size.getX(), y + (float)this.size.getY(), z + (float)this.size.getZ());
      return this;
   }

   public MeshedBlocksNode softLit(boolean on) {
      this.softLit = on;
      return this;
   }

   public MeshedBlocksNode rim(int cells) {
      this.rim = Math.max(0, cells);
      this.dirty = true;
      return this;
   }

   public Vec3i size() {
      return this.size;
   }

   public BlockPos origin() {
      return this.origin;
   }

   protected void set(int x, int y, int z, BlockState state, @Nullable CompoundTag nbt) {
      this.level.place(new BlockPos(this.origin.getX() + x, this.origin.getY() + y, this.origin.getZ() + z), state, nbt);
   }

   protected void setStatic(int x, int y, int z, BlockState state) {
      this.level.place(new BlockPos(this.origin.getX() + x, this.origin.getY() + y, this.origin.getZ() + z), state, (CompoundTag)null, false);
   }

   protected BlockState get(int x, int y, int z) {
      return this.level.getBlockState(this.origin.getX() + x, this.origin.getY() + y, this.origin.getZ() + z);
   }

   protected void finishFill(Vec3i size) {
      this.size = size;
      this.applyBounds();
      this.blockEntities.clear();
      this.tickers.clear();
      BlockPos max = this.origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);

      for(BlockEntity be : this.level.blockEntitiesIn(this.origin, max, this.scratch)) {
         this.blockEntities.add(be);
         BlockEntityTicker<BlockEntity> ticker = null;
         BlockState s = be.getBlockState();
         Block var8 = s.getBlock();
         if (var8 instanceof EntityBlock eb) {
            try {
               ticker = eb.getTicker(this.level, s, (BlockEntityType<BlockEntity>)(BlockEntityType<?>)be.getType());
            } catch (Exception var9) {
            }
         }

         this.tickers.add(ticker);
      }

      this.dirty = true;
   }

   private void applyBounds() {
      if (this.customOffset) {
         this.bounds(this.offsetX, this.offsetY, this.offsetZ, this.offsetX + (float)this.size.getX(), this.offsetY + (float)this.size.getY(), this.offsetZ + (float)this.size.getZ());
      } else {
         this.offsetX = this.centered ? (float)(-this.size.getX()) / 2.0F : 0.0F;
         this.offsetY = 0.0F;
         this.offsetZ = this.centered ? (float)(-this.size.getZ()) / 2.0F : 0.0F;
         this.bounds(this.offsetX, 0.0F, this.offsetZ, this.offsetX + (float)this.size.getX(), (float)this.size.getY(), this.offsetZ + (float)this.size.getZ());
      }
   }

   public void invalidateMesh() {
      this.dirty = true;
   }

   public List<BlockEntity> blockEntities() {
      return this.blockEntities;
   }

   private void ensureMesh(StageRenderContext ctx) {
      int lg = ctx.lighting.generation();
      int rg = StageResources.generation();
      boolean soft = this.softLit && ctx.soft;
      if (this.mesh == null && !this.meshFailed || this.dirty || !soft && lg != this.meshLighting || rg != this.meshResources || soft != this.meshSoft) {
         if (this.mesh != null) {
            this.mesh.close();
         }

         this.meshFailed = false;

         try {
            BlockPos max = this.origin.offset(Math.max(0, this.size.getX() - 1 - this.rim), Math.max(0, this.size.getY() - 1), Math.max(0, this.size.getZ() - 1 - this.rim));
            this.mesh = BlockMesh.build(this.level, this.origin.offset(this.rim, 0, this.rim), max, this.origin, soft);
         } catch (RuntimeException e) {
            StageTrouble.once("the blocks of " + this.name().getString(), "not drawn until the resources or the light change", e);
            this.mesh = null;
            this.meshFailed = true;
         }

         this.meshLighting = lg;
         this.meshResources = rg;
         this.meshSoft = soft;
         this.dirty = false;
      }
   }

   public void tick(StageRenderContext ctx) {
      for(int i = 0; i < this.blockEntities.size(); ++i) {
         BlockEntityTicker<BlockEntity> t = (BlockEntityTicker)this.tickers.get(i);
         if (t != null) {
            BlockEntity be = (BlockEntity)this.blockEntities.get(i);

            try {
               t.tick(this.level, be.getBlockPos(), be.getBlockState(), be);
            } catch (Exception e) {
               this.tickers.set(i, null);
               CFHGui.LOGGER.warn("[CFHGui] stage: ticker of {} failed, stopped: {}", be.getBlockState(), e.toString());
            }
         }
      }

   }

   protected void draw(StageRenderContext ctx) {
      this.ensureMesh(ctx);
      ctx.pose.pushPose();
      ctx.pose.translate(this.offsetX, this.offsetY, this.offsetZ);
      if (this.mesh != null) {
         if (this.meshSoft) {
            this.mesh.drawSoft(false, ctx.pose.last().pose(), ctx.camera.projection(), this.alpha());
         } else {
            this.mesh.drawOpaque(ctx.pose.last().pose(), ctx.camera.projection());
         }
      }

      if (!this.blockEntities.isEmpty()) {
         BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();

         for(BlockEntity be : this.blockEntities) {
            if (!StageBlockEntities.leftOut(be)) {
               BlockEntityRenderer<BlockEntity> r = dispatcher.getRenderer(be);
               if (r != null) {
                  BlockPos p = be.getBlockPos();
                  ctx.pose.pushPose();
                  ctx.pose.translate((float)(p.getX() - this.origin.getX()), (float)(p.getY() - this.origin.getY()), (float)(p.getZ() - this.origin.getZ()));
                  StageBlockEntities.draw(this.level, be, r, ctx.partial, ctx.pose, ctx.buffers, ctx.light);
                  ctx.pose.popPose();
               }
            }
         }
      }

      ctx.pose.popPose();
   }

   public boolean hasTranslucentPass() {
      return true;
   }

   protected void drawTranslucent(StageRenderContext ctx) {
      if (this.mesh != null) {
         ctx.pose.pushPose();
         ctx.pose.translate(this.offsetX, this.offsetY, this.offsetZ);
         if (this.meshSoft) {
            this.mesh.drawSoft(true, ctx.pose.last().pose(), ctx.camera.projection(), this.alpha());
         } else {
            this.mesh.drawTranslucent(ctx.pose.last().pose(), ctx.camera.projection());
         }

         ctx.pose.popPose();
      }
   }

   public void dispose() {
      if (this.mesh != null) {
         this.mesh.close();
         this.mesh = null;
      }

      this.level.clear(this.origin, this.origin.offset(Math.max(0, this.size.getX() - 1), Math.max(0, this.size.getY() - 1), Math.max(0, this.size.getZ() - 1)));
      this.blockEntities.clear();
      this.tickers.clear();
   }
}
