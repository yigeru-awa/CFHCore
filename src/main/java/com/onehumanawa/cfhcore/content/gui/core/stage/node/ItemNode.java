package com.onehumanawa.cfhcore.content.gui.core.stage.node;

import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

public class ItemNode extends StageNode {
   private ItemStack stack;
   private ItemDisplayContext context;
   private float spinDegPerSec;
   private float bobBlocks;
   private float spin;
   private float size;
   private final Quaternionf spinRotation;

   public ItemNode(ItemStack stack) {
      this.context = ItemDisplayContext.FIXED;
      this.size = 1.0F;
      this.spinRotation = new Quaternionf();
      this.stack = stack;
      this.bounds(-0.4F, 0.1F, -0.4F, 0.4F, 0.9F, 0.4F);
      this.hoverFeel(0.06F, 1.08F);
      this.named(stack.getHoverName());
   }

   public ItemNode stack(ItemStack s) {
      this.stack = s;
      this.named(s.getHoverName());
      return this;
   }

   public ItemStack stack() {
      return this.stack;
   }

   public ItemNode context(ItemDisplayContext c) {
      this.context = c;
      return this;
   }

   public ItemNode spin(float degPerSec) {
      this.spinDegPerSec = degPerSec;
      return this;
   }

   public ItemNode bob(float blocks) {
      this.bobBlocks = blocks;
      return this;
   }

   public ItemNode size(float blocks) {
      this.size = blocks;
      this.bounds(-0.4F * blocks, 0.5F - 0.4F * blocks, -0.4F * blocks, 0.4F * blocks, 0.5F + 0.4F * blocks, 0.4F * blocks);
      return this;
   }

   public void update(StageRenderContext ctx) {
      if (Theme.current().motion() > 0.0F) {
         this.spin = (this.spin + this.spinDegPerSec * ctx.deltaMs / 1000.0F) % 360.0F;
      }

   }

   protected void draw(StageRenderContext ctx) {
      if (!this.stack.isEmpty()) {
         float bob = this.bobBlocks > 0.0F && Theme.current().motion() > 0.0F ? Mth.sin(ctx.seconds() * 2.2F) * this.bobBlocks : 0.0F;
         ctx.pose.pushPose();
         ctx.pose.translate(0.0F, 0.5F + bob, 0.0F);
         if (this.spin != 0.0F) {
            ctx.pose.mulPose(this.spinRotation.rotationY((float)Math.toRadians((double)this.spin)));
         }

         if (this.size != 1.0F) {
            ctx.pose.scale(this.size, this.size, this.size);
         }

         Minecraft.getInstance().getItemRenderer().renderStatic(this.stack, this.context, ctx.light, OverlayTexture.NO_OVERLAY, ctx.pose, ctx.buffers, ctx.level, 0);
         ctx.pose.popPose();
      }
   }
}
