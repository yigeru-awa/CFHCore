package com.onehumanawa.cfhcore.content.gui.core.stage.mesh;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageAnimations;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageSoft;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public final class QuadMesh implements AutoCloseable {
   private static final Direction[] DIRECTIONS = Direction.values();
   private final VertexBuffer[] opaque = new VertexBuffer[6];
   private final VertexBuffer[] translucent = new VertexBuffer[6];
   private int quads;
   private final Set<TextureAtlasSprite> animated = new HashSet();

   private QuadMesh() {
   }

   public static Builder builder() {
      return new Builder();
   }

   public static TextureAtlasSprite sprite(ResourceLocation id) {
      return (TextureAtlasSprite)Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(id);
   }

   public static TextureAtlasSprite sprite(String blockTexture) {
      if (blockTexture.indexOf(58) >= 0) {
         ResourceLocation id = ResourceLocation.tryParse(blockTexture);
         if (id != null) {
            return sprite(id);
         }
      }

      return sprite(ResourceLocation.withDefaultNamespace("block/" + blockTexture));
   }

   public int quadCount() {
      return this.quads;
   }

   public void draw(boolean translucentLayer, Matrix4f modelView, Matrix4f projection, Shade shade, float brightness, float alpha) {
      VertexBuffer[] buffers = translucentLayer ? this.translucent : this.opaque;
      RenderType type = translucentLayer ? RenderType.translucent() : RenderType.cutoutMipped();
      StageAnimations.keep(this.animated);
      float[] was = (float[])RenderSystem.getShaderColor().clone();
      boolean any = false;

      for(int i = 0; i < 6; ++i) {
         VertexBuffer vbo = buffers[i];
         if (vbo != null) {
            if (!any) {
               type.setupRenderState();
               any = true;
            }

            float s = brightness * shade.of(DIRECTIONS[i]);
            RenderSystem.setShaderColor(s, s, s, alpha);
            vbo.bind();
            vbo.drawWithShader(modelView, projection, RenderSystem.getShader());
         }
      }

      if (any) {
         VertexBuffer.unbind();
         type.clearRenderState();
      }

      RenderSystem.setShaderColor(was[0], was[1], was[2], was[3]);
   }

   public void drawSoft(boolean translucentLayer, Matrix4f modelView, Matrix4f projection, float alpha) {
      this.drawSoft(translucentLayer, modelView, projection, alpha, 0.0F);
   }

   public void drawSoft(boolean translucentLayer, Matrix4f modelView, Matrix4f projection, float alpha, float gloss) {
      VertexBuffer[] buffers = translucentLayer ? this.translucent : this.opaque;
      StageAnimations.keep(this.animated);

      for(int i = 0; i < 6; ++i) {
         if (buffers[i] != null) {
            StageSoft.draw(buffers[i], modelView, projection, TextureAtlas.LOCATION_BLOCKS, alpha, translucentLayer, 0.0F, gloss);
         }
      }

   }

   public void close() {
      for(int i = 0; i < 6; ++i) {
         if (this.opaque[i] != null) {
            this.opaque[i].close();
            this.opaque[i] = null;
         }

         if (this.translucent[i] != null) {
            this.translucent[i].close();
            this.translucent[i] = null;
         }
      }

   }

   public static final class Builder {
      private final BufferBuilder[] opaque = new BufferBuilder[6];
      private final BufferBuilder[] translucent = new BufferBuilder[6];
      private final List<ByteBufferBuilder> memory = new ArrayList();
      private final Set<TextureAtlasSprite> sprites = new HashSet();
      private int quads;

      private Builder() {
      }

      private BufferBuilder buffer(boolean translucentLayer, Direction face) {
         BufferBuilder[] set = translucentLayer ? this.translucent : this.opaque;
         BufferBuilder bb = set[face.ordinal()];
         if (bb == null) {
            ByteBufferBuilder mem = new ByteBufferBuilder(16384);
            this.memory.add(mem);
            bb = new BufferBuilder(mem, Mode.QUADS, DefaultVertexFormat.BLOCK);
            set[face.ordinal()] = bb;
         }

         return bb;
      }

      public Builder face(boolean translucentLayer, Direction face, int x, int y, int z, TextureAtlasSprite sprite, int texels, int argb) {
         BufferBuilder bb = this.buffer(translucentLayer, face);
         this.sprites.add(sprite);
         int a;
         int b;
         switch (face.getAxis()) {
            case Y:
               a = x;
               b = z;
               break;
            case Z:
               a = x;
               b = y;
               break;
            default:
               a = z;
               b = y;
         }

         int t = Math.max(1, Math.min(16, texels));
         int span = Math.max(1, 16 / t);
         float u0 = (float)(Math.floorMod(a, span) * t) / 16.0F;
         float u1 = u0 + (float)t / 16.0F;
         int row = face.getAxis() == Axis.Y ? Math.floorMod(b, span) : span - 1 - Math.floorMod(b, span);
         float v0 = (float)(row * t) / 16.0F;
         float v1 = v0 + (float)t / 16.0F;
         float x1 = (float)x + 1.0F;
         float y1 = (float)y + 1.0F;
         float z1 = (float)z + 1.0F;
         float nx = (float)face.getStepX();
         float ny = (float)face.getStepY();
         float nz = (float)face.getStepZ();
         switch (face) {
            case UP:
               vertex(bb, sprite, (float)x, y1, (float)z, u0, v0, argb, nx, ny, nz);
               vertex(bb, sprite, (float)x, y1, z1, u0, v1, argb, nx, ny, nz);
               vertex(bb, sprite, x1, y1, z1, u1, v1, argb, nx, ny, nz);
               vertex(bb, sprite, x1, y1, (float)z, u1, v0, argb, nx, ny, nz);
               break;
            case DOWN:
               vertex(bb, sprite, (float)x, (float)y, z1, u0, v1, argb, nx, ny, nz);
               vertex(bb, sprite, (float)x, (float)y, (float)z, u0, v0, argb, nx, ny, nz);
               vertex(bb, sprite, x1, (float)y, (float)z, u1, v0, argb, nx, ny, nz);
               vertex(bb, sprite, x1, (float)y, z1, u1, v1, argb, nx, ny, nz);
               break;
            case NORTH:
               vertex(bb, sprite, (float)x, (float)y, (float)z, u0, v1, argb, nx, ny, nz);
               vertex(bb, sprite, (float)x, y1, (float)z, u0, v0, argb, nx, ny, nz);
               vertex(bb, sprite, x1, y1, (float)z, u1, v0, argb, nx, ny, nz);
               vertex(bb, sprite, x1, (float)y, (float)z, u1, v1, argb, nx, ny, nz);
               break;
            case SOUTH:
               vertex(bb, sprite, x1, (float)y, z1, u1, v1, argb, nx, ny, nz);
               vertex(bb, sprite, x1, y1, z1, u1, v0, argb, nx, ny, nz);
               vertex(bb, sprite, (float)x, y1, z1, u0, v0, argb, nx, ny, nz);
               vertex(bb, sprite, (float)x, (float)y, z1, u0, v1, argb, nx, ny, nz);
               break;
            case WEST:
               vertex(bb, sprite, (float)x, (float)y, z1, u1, v1, argb, nx, ny, nz);
               vertex(bb, sprite, (float)x, y1, z1, u1, v0, argb, nx, ny, nz);
               vertex(bb, sprite, (float)x, y1, (float)z, u0, v0, argb, nx, ny, nz);
               vertex(bb, sprite, (float)x, (float)y, (float)z, u0, v1, argb, nx, ny, nz);
               break;
            default:
               vertex(bb, sprite, x1, (float)y, (float)z, u0, v1, argb, nx, ny, nz);
               vertex(bb, sprite, x1, y1, (float)z, u0, v0, argb, nx, ny, nz);
               vertex(bb, sprite, x1, y1, z1, u1, v0, argb, nx, ny, nz);
               vertex(bb, sprite, x1, (float)y, z1, u1, v1, argb, nx, ny, nz);
         }

         ++this.quads;
         return this;
      }

      private static void vertex(BufferBuilder bb, TextureAtlasSprite sprite, float x, float y, float z, float u, float v, int argb, float nx, float ny, float nz) {
         bb.addVertex(x, y, z).setColor(argb).setUv(sprite.getU(u), sprite.getV(v)).setLight(15728880).setNormal(nx, ny, nz);
      }

      public QuadMesh build() {
         QuadMesh mesh = new QuadMesh();
         mesh.quads = this.quads;
         if (StageAnimations.needed()) {
            for(TextureAtlasSprite s : this.sprites) {
               StageAnimations.add(s, mesh.animated);
            }
         }

         for(int i = 0; i < 6; ++i) {
            mesh.opaque[i] = upload(this.opaque[i]);
            mesh.translucent[i] = upload(this.translucent[i]);
         }

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
   public interface Shade {
      float of(Direction var1);
   }
}
