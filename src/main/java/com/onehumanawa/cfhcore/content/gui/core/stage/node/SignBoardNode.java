package com.onehumanawa.cfhcore.content.gui.core.stage.node;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageSoft;
import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.PropQuads;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.WoodType;

public class SignBoardNode extends StageNode {
   public static final ResourceLocation TEXTURE = CFHGui.id("textures/stage/sign.png");
   private static final float THICK = 1.25F;
   public static final float FACE_Z = 0.078125F;
   public static final float FACE_MID_Y = 0.25F;
   private boolean vanilla;

   public SignBoardNode() {
      this.bounds(-0.5F, 0.0F, 0.0F, 0.5F, 0.5F, 0.078125F);
      this.hoverFeel(0.0F, 1.0F);
      this.named("sign");
   }

   public SignBoardNode vanilla(boolean on) {
      this.vanilla = on;
      return this;
   }

   protected void draw(StageRenderContext ctx) {
      float a = this.alpha();
      if (this.vanilla) {
         Material oak = Sheets.getSignMaterial(WoodType.OAK);
         if (ctx.soft) {
            BufferBuilder bb = StageSoft.begin(StageSoft.ENTITY);
            emitVanilla(ctx, oak.sprite().wrap(bb), a);
            StageSoft.end(bb, StageSoft.ENTITY, oak.atlasLocation(), a);
         } else {
            emitVanilla(ctx, oak.buffer(ctx.buffers, a < 1.0F ? RenderType::entityTranslucent : RenderType::entityCutout), a);
         }

      } else {
         if (ctx.soft) {
            BufferBuilder bb = StageSoft.begin(StageSoft.ENTITY);
            emit(ctx, bb, a);
            StageSoft.end(bb, StageSoft.ENTITY, TEXTURE, a);
         } else {
            emit(ctx, ctx.buffers.getBuffer(a < 1.0F ? RenderType.entityTranslucent(TEXTURE) : RenderType.entityCutout(TEXTURE)), a);
         }

      }
   }

   private static void emitVanilla(StageRenderContext ctx, VertexConsumer out, float alpha) {
      PropQuads q = new PropQuads(out, ctx.pose.last(), 64, 32, alpha);
      q.face(-8.0F, 0.0F, 1.25F, 16.0F, 0.0F, 0.0F, 0.0F, 8.0F, 0.0F, 2.0F, 2.0F, 26.0F, 14.0F, 0.92F, 1.0F);
      q.face(8.0F, 0.0F, 0.0F, -16.0F, 0.0F, 0.0F, 0.0F, 8.0F, 0.0F, 28.0F, 2.0F, 52.0F, 14.0F, 0.8F, 0.8F);
      q.face(-8.0F, 8.0F, 1.25F, 16.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.25F, 2.0F, 0.0F, 26.0F, 2.0F);
      q.face(-8.0F, 0.0F, 0.0F, 16.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.25F, 26.0F, 0.0F, 50.0F, 2.0F, 0.75F, 0.75F);
      q.face(8.0F, 0.0F, 1.25F, 0.0F, 0.0F, -1.25F, 0.0F, 8.0F, 0.0F, 26.0F, 2.0F, 28.0F, 14.0F, 0.85F, 0.95F);
      q.face(-8.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.25F, 0.0F, 8.0F, 0.0F, 0.0F, 2.0F, 2.0F, 14.0F, 0.85F, 0.95F);
   }

   private static void emit(StageRenderContext ctx, VertexConsumer out, float alpha) {
      PropQuads q = new PropQuads(out, ctx.pose.last(), 32, 16, alpha);
      q.face(-8.0F, 0.0F, 1.25F, 16.0F, 0.0F, 0.0F, 0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 16.0F, 8.0F, 0.92F, 1.0F);
      q.face(8.0F, 0.0F, 0.0F, -16.0F, 0.0F, 0.0F, 0.0F, 8.0F, 0.0F, 16.0F, 0.0F, 32.0F, 8.0F, 0.8F, 0.8F);
      q.face(-8.0F, 8.0F, 1.25F, 16.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.25F, 0.0F, 8.0F, 16.0F, 9.0F);
      q.face(-8.0F, 0.0F, 0.0F, 16.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.25F, 0.0F, 9.0F, 16.0F, 10.0F, 0.75F, 0.75F);
      q.face(8.0F, 0.0F, 1.25F, 0.0F, 0.0F, -1.25F, 0.0F, 8.0F, 0.0F, 0.0F, 8.0F, 1.0F, 10.0F, 0.85F, 0.95F);
      q.face(-8.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.25F, 0.0F, 8.0F, 0.0F, 0.0F, 8.0F, 1.0F, 10.0F, 0.85F, 0.95F);
   }
}
