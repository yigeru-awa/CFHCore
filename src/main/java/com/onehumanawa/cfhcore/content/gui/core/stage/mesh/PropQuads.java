package com.onehumanawa.cfhcore.content.gui.core.stage.mesh;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector3f;

public final class PropQuads {
   private static final float PX = 0.0625F;
   private final VertexConsumer out;
   private final PoseStack.Pose pose;
   private final float texW;
   private final float texH;
   private final int alpha;
   private final Vector3f normal = new Vector3f();

   public PropQuads(VertexConsumer out, PoseStack.Pose pose, int textureWidth, int textureHeight, float alpha) {
      this.out = out;
      this.pose = pose;
      this.texW = (float)textureWidth;
      this.texH = (float)textureHeight;
      this.alpha = Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F);
   }

   public PropQuads face(float px, float py, float pz, float ux, float uy, float uz, float vx, float vy, float vz, float u0, float v0, float u1, float v1, float shadeLow, float shadeHigh) {
      this.normal.set(ux, uy, uz).cross(vx, vy, vz);
      if (this.normal.lengthSquared() < 1.0E-9F) {
         return this;
      } else {
         this.normal.normalize();
         this.vertex(px, py, pz, u0, v1, shadeLow);
         this.vertex(px + ux, py + uy, pz + uz, u1, v1, shadeLow);
         this.vertex(px + ux + vx, py + uy + vy, pz + uz + vz, u1, v0, shadeHigh);
         this.vertex(px + vx, py + vy, pz + vz, u0, v0, shadeHigh);
         return this;
      }
   }

   public PropQuads face(float px, float py, float pz, float ux, float uy, float uz, float vx, float vy, float vz, float u0, float v0, float u1, float v1) {
      return this.face(px, py, pz, ux, uy, uz, vx, vy, vz, u0, v0, u1, v1, 1.0F, 1.0F);
   }

   private void vertex(float x, float y, float z, float u, float v, float shade) {
      int c = Math.round(Math.max(0.0F, Math.min(1.0F, shade)) * 255.0F);
      this.out.addVertex(this.pose, x * 0.0625F, y * 0.0625F, z * 0.0625F).setColor(c, c, c, this.alpha).setUv(u / this.texW, v / this.texH).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(this.pose, this.normal.x, this.normal.y, this.normal.z);
   }
}
