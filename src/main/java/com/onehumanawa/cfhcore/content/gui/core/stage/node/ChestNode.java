package com.onehumanawa.cfhcore.content.gui.core.stage.node;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageSoft;
import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.PropQuads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class ChestNode extends StageNode {
   public static final ResourceLocation TEXTURE = CFHGui.id("textures/stage/chest.png");
   private static final int TEX = 64;
   private static final float HINGE_Y = 9.0F;
   private static final float HINGE_Z = -7.0F;
   private final Anim lid;
   private float lidAngle;
   private boolean open;
   private boolean vanilla;
   private @Nullable ModelPart bottomPart;
   private @Nullable ModelPart lidPart;
   private @Nullable ModelPart lockPart;

   public ChestNode() {
      this.lid = new Anim(0.0F, 640, Ease.OUT_BACK);
      this.lidAngle = 97.0F;
      this.bounds(-0.4375F, 0.0F, -0.4375F, 0.4375F, 0.875F, 0.4375F);
      this.hoverFeel(0.0F, 1.0F);
      this.named("chest");
   }

   public ChestNode vanilla(boolean on) {
      this.vanilla = on;
      return this;
   }

   public boolean vanilla() {
      return this.vanilla;
   }

   public ChestNode open(boolean open) {
      this.open = open;
      this.lid.set(open);
      return this;
   }

   public ChestNode openNow(boolean open) {
      this.open = open;
      this.lid.snap(open ? 1.0F : 0.0F);
      return this;
   }

   public boolean isOpen() {
      return this.open;
   }

   public float openness() {
      return this.lid.get();
   }

   public ChestNode lidAngle(float degrees) {
      this.lidAngle = degrees;
      return this;
   }

   public float lidAngle() {
      return this.lidAngle;
   }

   protected void draw(StageRenderContext ctx) {
      float a = this.alpha();
      if (this.vanilla) {
         this.drawVanilla(ctx, a);
      } else {
         if (ctx.soft) {
            BufferBuilder bb = StageSoft.begin(StageSoft.ENTITY);
            this.emit(ctx, bb, a);
            StageSoft.end(bb, StageSoft.ENTITY, TEXTURE, a);
         } else {
            this.emit(ctx, ctx.buffers.getBuffer(a < 1.0F ? RenderType.entityTranslucent(TEXTURE) : RenderType.entityCutout(TEXTURE)), a);
         }

      }
   }

   private void drawVanilla(StageRenderContext ctx, float a) {
      if (this.lidPart == null) {
         ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.CHEST);
         this.bottomPart = root.getChild("bottom");
         this.lidPart = root.getChild("lid");
         this.lockPart = root.getChild("lock");
      }

      float angle = (float)(-Math.toRadians((double)(this.lidAngle * this.lid.get())));
      this.lidPart.xRot = angle;
      this.lockPart.xRot = angle;
      Material material = Sheets.CHEST_LOCATION;
      ctx.pose.pushPose();
      ctx.pose.translate(-0.5F, 0.0F, -0.5F);
      if (ctx.soft) {
         BufferBuilder bb = StageSoft.begin(StageSoft.ENTITY);
         VertexConsumer out = material.sprite().wrap(bb);
         this.renderVanilla(ctx, out);
         StageSoft.end(bb, StageSoft.ENTITY, material.atlasLocation(), a);
      } else {
         this.renderVanilla(ctx, material.buffer(ctx.buffers, a < 1.0F ? RenderType::entityTranslucent : RenderType::entityCutout));
      }

      ctx.pose.popPose();
   }

   private void renderVanilla(StageRenderContext ctx, VertexConsumer out) {
      this.lidPart.render(ctx.pose, out, ctx.light, OverlayTexture.NO_OVERLAY);
      this.lockPart.render(ctx.pose, out, ctx.light, OverlayTexture.NO_OVERLAY);
      this.bottomPart.render(ctx.pose, out, ctx.light, OverlayTexture.NO_OVERLAY);
   }

   private void emit(StageRenderContext ctx, VertexConsumer out, float alpha) {
      PropQuads base = new PropQuads(out, ctx.pose.last(), 64, 64, alpha);
      base.face(-7.0F, 0.0F, 7.0F, 14.0F, 0.0F, 0.0F, 0.0F, 9.0F, 0.0F, 0.0F, 19.0F, 14.0F, 28.0F, 0.78F, 1.0F);
      base.face(7.0F, 0.0F, -7.0F, -14.0F, 0.0F, 0.0F, 0.0F, 9.0F, 0.0F, 14.0F, 19.0F, 28.0F, 28.0F, 0.78F, 1.0F);
      base.face(7.0F, 0.0F, 7.0F, 0.0F, 0.0F, -14.0F, 0.0F, 9.0F, 0.0F, 14.0F, 19.0F, 28.0F, 28.0F, 0.78F, 1.0F);
      base.face(-7.0F, 0.0F, -7.0F, 0.0F, 0.0F, 14.0F, 0.0F, 9.0F, 0.0F, 14.0F, 19.0F, 28.0F, 28.0F, 0.78F, 1.0F);
      base.face(-7.0F, 0.0F, -7.0F, 14.0F, 0.0F, 0.0F, 0.0F, 0.0F, 14.0F, 42.0F, 0.0F, 56.0F, 14.0F, 0.6F, 0.6F);
      base.face(-7.0F, 9.0F, 7.0F, 14.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 28.0F, 13.0F, 42.0F, 14.0F);
      base.face(-7.0F, 9.0F, -6.0F, 14.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 28.0F, 0.0F, 42.0F, 1.0F);
      base.face(-7.0F, 9.0F, 6.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, -12.0F, 28.0F, 1.0F, 29.0F, 13.0F);
      base.face(6.0F, 9.0F, 6.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, -12.0F, 41.0F, 1.0F, 42.0F, 13.0F);
      base.face(6.0F, 1.0F, 6.0F, -12.0F, 0.0F, 0.0F, 0.0F, 8.0F, 0.0F, 28.0F, 14.0F, 40.0F, 22.0F, 0.7F, 1.0F);
      base.face(-6.0F, 1.0F, -6.0F, 12.0F, 0.0F, 0.0F, 0.0F, 8.0F, 0.0F, 28.0F, 14.0F, 40.0F, 22.0F, 0.7F, 1.0F);
      base.face(-6.0F, 1.0F, 6.0F, 0.0F, 0.0F, -12.0F, 0.0F, 8.0F, 0.0F, 28.0F, 14.0F, 40.0F, 22.0F, 0.7F, 1.0F);
      base.face(6.0F, 1.0F, -6.0F, 0.0F, 0.0F, 12.0F, 0.0F, 8.0F, 0.0F, 28.0F, 14.0F, 40.0F, 22.0F, 0.7F, 1.0F);
      base.face(-6.0F, 1.0F, 6.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F, -12.0F, 29.0F, 1.0F, 41.0F, 13.0F, 0.8F, 0.8F);
      ctx.pose.pushPose();
      ctx.pose.translate(0.0F, 0.5625F, -0.4375F);
      ctx.pose.mulPose(Axis.XP.rotationDegrees(-this.lidAngle * this.lid.get()));
      PropQuads top = new PropQuads(out, ctx.pose.last(), 64, 64, alpha);
      top.face(-7.0F, 5.0F, 14.0F, 14.0F, 0.0F, 0.0F, 0.0F, 0.0F, -14.0F, 0.0F, 0.0F, 14.0F, 14.0F);
      top.face(-7.0F, 0.0F, 14.0F, 14.0F, 0.0F, 0.0F, 0.0F, 5.0F, 0.0F, 0.0F, 14.0F, 14.0F, 19.0F);
      top.face(7.0F, 0.0F, 0.0F, -14.0F, 0.0F, 0.0F, 0.0F, 5.0F, 0.0F, 14.0F, 14.0F, 28.0F, 19.0F);
      top.face(7.0F, 0.0F, 14.0F, 0.0F, 0.0F, -14.0F, 0.0F, 5.0F, 0.0F, 14.0F, 14.0F, 28.0F, 19.0F);
      top.face(-7.0F, 0.0F, 0.0F, 0.0F, 0.0F, 14.0F, 0.0F, 5.0F, 0.0F, 14.0F, 14.0F, 28.0F, 19.0F);
      top.face(-7.0F, 0.0F, 0.0F, 14.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 14.0F, 0.0F, 28.0F, 1.0F);
      top.face(-7.0F, 0.0F, 13.0F, 14.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 14.0F, 13.0F, 28.0F, 14.0F);
      top.face(-7.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 12.0F, 14.0F, 1.0F, 15.0F, 13.0F);
      top.face(6.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 12.0F, 27.0F, 1.0F, 28.0F, 13.0F);
      top.face(6.0F, 0.0F, 13.0F, -12.0F, 0.0F, 0.0F, 0.0F, 4.0F, 0.0F, 40.0F, 18.0F, 52.0F, 14.0F, 1.0F, 0.84F);
      top.face(-6.0F, 0.0F, 1.0F, 12.0F, 0.0F, 0.0F, 0.0F, 4.0F, 0.0F, 40.0F, 18.0F, 52.0F, 14.0F, 1.0F, 0.84F);
      top.face(-6.0F, 0.0F, 13.0F, 0.0F, 0.0F, -12.0F, 0.0F, 4.0F, 0.0F, 40.0F, 18.0F, 52.0F, 14.0F, 1.0F, 0.84F);
      top.face(6.0F, 0.0F, 1.0F, 0.0F, 0.0F, 12.0F, 0.0F, 4.0F, 0.0F, 40.0F, 18.0F, 52.0F, 14.0F, 1.0F, 0.84F);
      top.face(-6.0F, 4.0F, 1.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F, 12.0F, 15.0F, 1.0F, 27.0F, 13.0F, 0.94F, 0.94F);
      top.face(-1.0F, -2.0F, 15.0F, 2.0F, 0.0F, 0.0F, 0.0F, 4.0F, 0.0F, 28.0F, 22.0F, 30.0F, 26.0F);
      top.face(1.0F, -2.0F, 15.0F, 0.0F, 0.0F, -1.0F, 0.0F, 4.0F, 0.0F, 30.0F, 22.0F, 31.0F, 26.0F);
      top.face(-1.0F, -2.0F, 14.0F, 0.0F, 0.0F, 1.0F, 0.0F, 4.0F, 0.0F, 30.0F, 22.0F, 31.0F, 26.0F);
      top.face(-1.0F, 2.0F, 15.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 31.0F, 22.0F, 33.0F, 23.0F);
      top.face(-1.0F, -2.0F, 14.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 31.0F, 22.0F, 33.0F, 23.0F, 0.8F, 0.8F);
      ctx.pose.popPose();
   }
}
