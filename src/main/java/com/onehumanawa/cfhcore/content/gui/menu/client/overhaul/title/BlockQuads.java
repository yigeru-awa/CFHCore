package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageAnimations;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageSoft;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

final class BlockQuads implements AutoCloseable {
   private @Nullable VertexBuffer solid;
   private @Nullable VertexBuffer clear;
   private final Set<TextureAtlasSprite> animated = new HashSet();
   private boolean decal;
   private static final float[] SCRATCH = new float[20];

   private BlockQuads() {
   }

   static Builder builder() {
      return new Builder();
   }

   static void decalOn() {
      RenderSystem.enablePolygonOffset();
      RenderSystem.polygonOffset(0.0F, -10.0F);
   }

   static void decalOff() {
      RenderSystem.polygonOffset(0.0F, 0.0F);
      RenderSystem.disablePolygonOffset();
   }

   void draw(StageRenderContext ctx, boolean clearLayer, float alpha, float glow) {
      VertexBuffer vbo = clearLayer ? this.clear : this.solid;
      if (vbo != null && !(alpha <= 0.004F)) {
         StageAnimations.keep(this.animated);
         Matrix4f pose = ctx.pose.last().pose();
         if (this.decal) {
            decalOn();
         }

         try {
            if (!ctx.soft) {
               RenderType type = clearLayer ? RenderType.translucent() : RenderType.cutoutMipped();
               float[] was = (float[])RenderSystem.getShaderColor().clone();
               type.setupRenderState();
               RenderSystem.setShaderColor(was[0], was[1], was[2], was[3] * alpha);
               vbo.bind();
               vbo.drawWithShader(pose, ctx.camera.projection(), RenderSystem.getShader());
               VertexBuffer.unbind();
               type.clearRenderState();
               RenderSystem.setShaderColor(was[0], was[1], was[2], was[3]);
               return;
            }

            StageSoft.draw(vbo, pose, ctx.camera.projection(), TextureAtlas.LOCATION_BLOCKS, alpha, clearLayer, glow);
         } finally {
            if (this.decal) {
               decalOff();
            }

         }

      }
   }

   public void close() {
      if (this.solid != null) {
         this.solid.close();
         this.solid = null;
      }

      if (this.clear != null) {
         this.clear.close();
         this.clear = null;
      }

   }

   private static void corners(Direction face, float x0, float y0, float z0, float x1, float y1, float z1, float u0, float v0, float u1, float v1, float[] out) {
      switch (face) {
         case UP:
            put(out, 0, x0, y1, z0, u0, v0);
            put(out, 1, x0, y1, z1, u0, v1);
            put(out, 2, x1, y1, z1, u1, v1);
            put(out, 3, x1, y1, z0, u1, v0);
            break;
         case DOWN:
            put(out, 0, x0, y0, z1, u0, v1);
            put(out, 1, x0, y0, z0, u0, v0);
            put(out, 2, x1, y0, z0, u1, v0);
            put(out, 3, x1, y0, z1, u1, v1);
            break;
         case NORTH:
            put(out, 0, x0, y0, z0, u0, v1);
            put(out, 1, x0, y1, z0, u0, v0);
            put(out, 2, x1, y1, z0, u1, v0);
            put(out, 3, x1, y0, z0, u1, v1);
            break;
         case SOUTH:
            put(out, 0, x1, y0, z1, u1, v1);
            put(out, 1, x1, y1, z1, u1, v0);
            put(out, 2, x0, y1, z1, u0, v0);
            put(out, 3, x0, y0, z1, u0, v1);
            break;
         case WEST:
            put(out, 0, x0, y0, z1, u1, v1);
            put(out, 1, x0, y1, z1, u1, v0);
            put(out, 2, x0, y1, z0, u0, v0);
            put(out, 3, x0, y0, z0, u0, v1);
            break;
         default:
            put(out, 0, x1, y0, z0, u0, v1);
            put(out, 1, x1, y1, z0, u0, v0);
            put(out, 2, x1, y1, z1, u1, v0);
            put(out, 3, x1, y0, z1, u1, v1);
      }

   }

   private static void put(float[] out, int corner, float x, float y, float z, float u, float v) {
      int o = corner * 5;
      out[o] = x;
      out[o + 1] = y;
      out[o + 2] = z;
      out[o + 3] = u;
      out[o + 4] = v;
   }

   static void rect(VertexConsumer vc, PoseStack.Pose pose, Direction face, float x0, float y0, float z0, float x1, float y1, float z1, TextureAtlasSprite sprite, float u0, float v0, float u1, float v1, int argb) {
      corners(face, x0, y0, z0, x1, y1, z1, u0, v0, u1, v1, SCRATCH);

      for(int i = 0; i < 4; ++i) {
         int o = i * 5;
         vertex(vc, pose, SCRATCH[o], SCRATCH[o + 1], SCRATCH[o + 2], sprite, SCRATCH[o + 3], SCRATCH[o + 4], argb, (float)face.getStepX(), (float)face.getStepY(), (float)face.getStepZ());
      }

   }

   static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, TextureAtlasSprite sprite, float u, float v, int argb, float nx, float ny, float nz) {
      vc.addVertex(pose, x, y, z).setColor(argb).setUv(sprite.getU(u), sprite.getV(v)).setLight(15728880).setNormal(pose, nx, ny, nz);
   }

   static final class Builder {
      private final List<ByteBufferBuilder> memory = new ArrayList();
      private @Nullable BufferBuilder solid;
      private @Nullable BufferBuilder clear;
      private final Set<TextureAtlasSprite> sprites = new HashSet();
      private final float[] corner = new float[20];
      private boolean decal;

      private Builder() {
      }

      Builder decal() {
         this.decal = true;
         return this;
      }

      private BufferBuilder buffer(boolean clearLayer) {
         BufferBuilder bb = clearLayer ? this.clear : this.solid;
         if (bb == null) {
            ByteBufferBuilder mem = new ByteBufferBuilder(32768);
            this.memory.add(mem);
            bb = new BufferBuilder(mem, Mode.QUADS, DefaultVertexFormat.BLOCK);
            if (clearLayer) {
               this.clear = bb;
            } else {
               this.solid = bb;
            }
         }

         return bb;
      }

      Builder rect(boolean clearLayer, Direction face, float x0, float y0, float z0, float x1, float y1, float z1, TextureAtlasSprite sprite, float u0, float v0, float u1, float v1, Paint paint) {
         BlockQuads.corners(face, x0, y0, z0, x1, y1, z1, u0, v0, u1, v1, this.corner);
         return this.quad(clearLayer, sprite, this.corner, (float)face.getStepX(), (float)face.getStepY(), (float)face.getStepZ(), paint);
      }

      Builder quad(boolean clearLayer, TextureAtlasSprite sprite, float[] corners, float nx, float ny, float nz, Paint paint) {
         BufferBuilder bb = this.buffer(clearLayer);
         this.sprites.add(sprite);

         for(int i = 0; i < 4; ++i) {
            int o = i * 5;
            float x = corners[o];
            float y = corners[o + 1];
            float z = corners[o + 2];
            bb.addVertex(x, y, z).setColor(paint.at(x, y, z, nx, ny, nz)).setUv(sprite.getU(corners[o + 3]), sprite.getV(corners[o + 4])).setLight(15728880).setNormal(nx, ny, nz);
         }

         return this;
      }

      BlockQuads build() {
         BlockQuads mesh = new BlockQuads();
         mesh.decal = this.decal;
         if (StageAnimations.needed()) {
            for(TextureAtlasSprite s : this.sprites) {
               StageAnimations.add(s, mesh.animated);
            }
         }

         mesh.solid = upload(this.solid);
         mesh.clear = upload(this.clear);

         for(ByteBufferBuilder b : this.memory) {
            b.close();
         }

         this.memory.clear();
         return mesh;
      }

      private static @Nullable VertexBuffer upload(@Nullable BufferBuilder bb) {
         if (bb == null) {
            return null;
         } else {
            MeshData data = bb.build();
            if (data == null) {
               return null;
            } else {
               VertexBuffer vbo = new VertexBuffer(Usage.STATIC);
               vbo.bind();
               vbo.upload(data);
               VertexBuffer.unbind();
               return vbo;
            }
         }
      }
   }

   @FunctionalInterface
   interface Paint {
      int at(float var1, float var2, float var3, float var4, float var5, float var6);
   }
}
