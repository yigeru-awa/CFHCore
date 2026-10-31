package com.onehumanawa.cfhcore.content.gui.core.stage.mesh;

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
import com.onehumanawa.cfhcore.content.gui.core.stage.StageLevel;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageSoft;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.joml.Matrix4f;

public final class BlockMesh implements AutoCloseable {
   private final List<Layer> layers = new ArrayList();
   private int quads;
   private final Set<TextureAtlasSprite> animated = new HashSet();

   private BlockMesh() {
   }

   public static BlockMesh build(StageLevel level, BlockPos min, BlockPos max, BlockPos origin) {
      return build(level, min, max, origin, false);
   }

   public static BlockMesh build(StageLevel level, BlockPos min, BlockPos max, BlockPos origin, boolean soft) {
      if (!soft) {
         return mesh(level, min, max, origin, false);
      } else {
         level.flatShade(true);

         BlockMesh var5;
         try {
            var5 = mesh(level, min, max, origin, true);
         } finally {
            level.flatShade(false);
         }

         return var5;
      }
   }

   private static BlockMesh mesh(StageLevel level, BlockPos min, BlockPos max, BlockPos origin, boolean soft) {
      BlockMesh mesh = new BlockMesh();
      Map<RenderType, BufferBuilder> builders = new LinkedHashMap();
      List<ByteBufferBuilder> memory = new ArrayList();
      BlockTesselator blocks = new BlockTesselator(RandomSource.create());
      PoseStack pose = new PoseStack();
      OffsetConsumer offset = new OffsetConsumer();
      GlowConsumer glow = new GlowConsumer();
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
      StageView view = new StageView(level);
      boolean track = StageAnimations.needed();
      Set<BlockState> seen = new HashSet();

      try {
         for(int y = min.getY(); y <= max.getY(); ++y) {
            for(int z = min.getZ(); z <= max.getZ(); ++z) {
               for(int x = min.getX(); x <= max.getX(); ++x) {
                  BlockState state = level.getBlockState(x, y, z);
                  if (!state.isAir()) {
                     pos.set(x, y, z);

                     try {
                        int emission = soft ? state.getLightEmission() : 0;
                        FluidState fluid = state.getFluidState();
                        if (!fluid.isEmpty()) {
                           VertexConsumer bb = blocks.hold(builder(builders, memory, StageLayers.fluid(fluid)));
                           offset.wrap((VertexConsumer)(soft ? glow.wrap(bb, emission) : bb), (float)((x & -16) - origin.getX()), (float)((y & -16) - origin.getY()), (float)((z & -16) - origin.getZ()));
                           blocks.liquid(state, fluid, pos, view, offset);
                        }

                        if (state.getRenderShape() == RenderShape.MODEL) {
                           pose.setIdentity();
                           pose.translate((float)(x - origin.getX()), (float)(y - origin.getY()), (float)(z - origin.getZ()));
                           blocks.model(state, pos, view, pose, (type) -> {
                              VertexConsumer bb = blocks.hold(builder(builders, memory, type));
                              return (VertexConsumer)(soft ? glow.wrap(bb, emission) : bb);
                           });
                        }

                        if (track && seen.add(state)) {
                           blocks.sprites(state, fluid, mesh.animated);
                        }
                     } catch (RuntimeException e) {
                        blocks.failed(state, e);
                     }
                  }
               }
            }
         }

         for(Map.Entry<RenderType, BufferBuilder> e : builders.entrySet()) {
            MeshData data = ((BufferBuilder)e.getValue()).build();
            if (data != null) {
               mesh.quads += data.drawState().vertexCount() / 4;
               VertexBuffer vbo = new VertexBuffer(Usage.STATIC);
               vbo.bind();
               vbo.upload(data);
               VertexBuffer.unbind();
               mesh.layers.add(new Layer((RenderType)e.getKey(), vbo, e.getKey() == RenderType.translucent() || e.getKey() == RenderType.tripwire()));
            }
         }
      } catch (RuntimeException e) {
         mesh.close();
         throw e;
      } finally {
         for(ByteBufferBuilder b : memory) {
            b.close();
         }

      }

      mesh.layers.sort((a, bx) -> Integer.compare(order(a.type), order(bx.type)));
      return mesh;
   }

   private static int order(RenderType t) {
      if (t == RenderType.solid()) {
         return 0;
      } else if (t == RenderType.cutoutMipped()) {
         return 1;
      } else if (t == RenderType.cutout()) {
         return 2;
      } else {
         return t == RenderType.translucent() ? 4 : 3;
      }
   }

   private static BufferBuilder builder(Map<RenderType, BufferBuilder> builders, List<ByteBufferBuilder> memory, RenderType type) {
      BufferBuilder bb = (BufferBuilder)builders.get(type);
      if (bb == null) {
         ByteBufferBuilder mem = new ByteBufferBuilder(type.bufferSize());
         memory.add(mem);
         bb = new BufferBuilder(mem, Mode.QUADS, DefaultVertexFormat.BLOCK);
         builders.put(type, bb);
      }

      return bb;
   }

   public void drawOpaque(Matrix4f modelView, Matrix4f projection) {
      StageAnimations.keep(this.animated);

      for(Layer l : this.layers) {
         if (!l.translucent) {
            draw(l, modelView, projection);
         }
      }

   }

   public void drawTranslucent(Matrix4f modelView, Matrix4f projection) {
      StageAnimations.keep(this.animated);

      for(Layer l : this.layers) {
         if (l.translucent) {
            draw(l, modelView, projection);
         }
      }

   }

   private static void draw(Layer l, Matrix4f modelView, Matrix4f projection) {
      RenderType state = StageLayers.drawnAs(l.type);
      state.setupRenderState();
      l.buffer.bind();
      l.buffer.drawWithShader(modelView, projection, RenderSystem.getShader());
      VertexBuffer.unbind();
      state.clearRenderState();
   }

   public void drawSoft(boolean translucent, Matrix4f modelView, Matrix4f projection, float alpha) {
      StageAnimations.keep(this.animated);

      for(Layer l : this.layers) {
         if (l.translucent == translucent) {
            RenderType drawn = StageLayers.drawnAs(l.type);
            if (drawn == RenderType.cutout()) {
               StageSoft.alphaTest(0.1F, true);
            } else if (drawn == RenderType.cutoutMipped()) {
               StageSoft.alphaTest(0.5F, false);
            } else {
               StageSoft.plainAlpha();
            }

            StageSoft.draw(l.buffer, modelView, projection, TextureAtlas.LOCATION_BLOCKS, alpha, translucent, 1.0F, translucent ? 1.0F : 0.0F);
         }
      }

      StageSoft.plainAlpha();
   }

   public int quadCount() {
      return this.quads;
   }

   public boolean isEmpty() {
      return this.layers.isEmpty();
   }

   public void close() {
      for(Layer l : this.layers) {
         l.buffer.close();
      }

      this.layers.clear();
   }

   private static record Layer(RenderType type, VertexBuffer buffer, boolean translucent) {
   }

   private static final class GlowConsumer implements VertexConsumer {
      private VertexConsumer delegate;
      private int emission;

      GlowConsumer wrap(VertexConsumer delegate, int emission) {
         this.delegate = delegate;
         this.emission = emission;
         return this;
      }

      public VertexConsumer addVertex(float x, float y, float z) {
         this.delegate.addVertex(x, y, z);
         return this;
      }

      public VertexConsumer setColor(int r, int g, int b, int a) {
         this.delegate.setColor(r, g, b, a);
         return this;
      }

      public VertexConsumer setUv(float u, float v) {
         this.delegate.setUv(u, v);
         return this;
      }

      public VertexConsumer setUv1(int u, int v) {
         this.delegate.setUv1(u, v);
         return this;
      }

      public VertexConsumer setUv2(int u, int v) {
         this.delegate.setUv2(this.emission << 4, 0);
         return this;
      }

      public VertexConsumer setNormal(float x, float y, float z) {
         this.delegate.setNormal(x, y, z);
         return this;
      }
   }

   private static final class OffsetConsumer implements VertexConsumer {
      private VertexConsumer delegate;
      private float ox;
      private float oy;
      private float oz;

      void wrap(VertexConsumer delegate, float ox, float oy, float oz) {
         this.delegate = delegate;
         this.ox = ox;
         this.oy = oy;
         this.oz = oz;
      }

      public VertexConsumer addVertex(float x, float y, float z) {
         this.delegate.addVertex(x + this.ox, y + this.oy, z + this.oz);
         return this;
      }

      public VertexConsumer setColor(int r, int g, int b, int a) {
         this.delegate.setColor(r, g, b, a);
         return this;
      }

      public VertexConsumer setUv(float u, float v) {
         this.delegate.setUv(u, v);
         return this;
      }

      public VertexConsumer setUv1(int u, int v) {
         this.delegate.setUv1(u, v);
         return this;
      }

      public VertexConsumer setUv2(int u, int v) {
         this.delegate.setUv2(u, v);
         return this;
      }

      public VertexConsumer setNormal(float x, float y, float z) {
         this.delegate.setNormal(x, y, z);
         return this;
      }
   }
}
