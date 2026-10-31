package com.onehumanawa.cfhcore.content.gui.core.stage.builds;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageAnimations;
import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.BlockTesselator;
import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.StageLayers;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

final class BuildMesher {
   static final int PIECE = 32;
   static final List<RenderType> LAYERS = List.of(RenderType.solid(), RenderType.cutoutMipped(), RenderType.cutout(), RenderType.translucent());
   private static final boolean OCCLUSION = !Boolean.getBoolean("cfhgui.buildNoOcclusion");

   private BuildMesher() {
   }

   static Piece mesh(BuildVolume volume, BlockAndTintGetter view, int index, int x0, int y0, int z0, boolean soft, @Nullable Cull cull, Vector3f sortFrom) {
      Map<RenderType, BufferBuilder> builders = new LinkedHashMap();
      Map<RenderType, List<ByteBufferBuilder>> memory = new LinkedHashMap();
      BlockTesselator blocks = new BlockTesselator(RandomSource.create());
      PoseStack pose = new PoseStack();
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
      Set<TextureAtlasSprite> animated = new HashSet();
      boolean track = StageAnimations.needed();
      Set<BlockState> seen = new HashSet();
      Sink sink = new Sink(soft, volume.lit(), cull, OCCLUSION && cull != null && cull.eyes().length > 0 ? volume : null);
      int x1 = Math.min(volume.sizeX(), x0 + 32);
      int y1 = Math.min(volume.sizeY(), y0 + 32);
      int z1 = Math.min(volume.sizeZ(), z0 + 32);
      boolean meshed = false;
      ModelBlockRenderer.enableCaching();

      try {
         for(int y = y0; y < y1; ++y) {
            for(int z = z0; z < z1; ++z) {
               for(int x = x0; x < x1; ++x) {
                  BlockState state = volume.get(x, y, z);
                  if (!state.isAir()) {
                     pos.set(x, y, z);

                     try {
                        int emission = soft ? state.getLightEmission() : 0;
                        FluidState fluid = state.getFluidState();
                        if (!fluid.isEmpty()) {
                           RenderType type = layerOf(StageLayers.fluid(fluid));
                           sink.to(blocks.hold(builder(builders, memory, type)), emission, (float)(x & -16), (float)(y & -16), (float)(z & -16));
                           blocks.liquid(state, fluid, pos, view, sink);
                        }

                        if (state.getRenderShape() == RenderShape.MODEL) {
                           pose.setIdentity();
                           pose.translate((float)x, (float)y, (float)z);
                           blocks.model(state, pos, view, pose, (typex) -> sink.to(blocks.hold(builder(builders, memory, layerOf(typex))), emission, 0.0F, 0.0F, 0.0F));
                        }

                        if (track && seen.add(state)) {
                           blocks.sprites(state, fluid, animated);
                        }
                     } catch (RuntimeException e) {
                        blocks.failed(state, e);
                     }
                  }
               }
            }
         }

         meshed = true;
      } finally {
         ModelBlockRenderer.clearCache();
         if (!meshed) {
            for(List<ByteBufferBuilder> mem : memory.values()) {
               for(ByteBufferBuilder b : mem) {
                  b.close();
               }
            }
         }

      }

      List<Layer> layers = new ArrayList();
      int quads = 0;

      for(Map.Entry<RenderType, BufferBuilder> e : builders.entrySet()) {
         MeshData data = ((BufferBuilder)e.getValue()).build();
         List<ByteBufferBuilder> mem = (List)memory.get(e.getKey());
         if (data == null) {
            for(ByteBufferBuilder b : mem) {
               b.close();
            }
         } else {
            quads += data.drawState().vertexCount() / 4;
            if (e.getKey() == RenderType.translucent()) {
               ByteBufferBuilder indices = new ByteBufferBuilder(Math.max(256, data.drawState().indexCount() * 4));
               mem.add(indices);
               data.sortQuads(indices, VertexSorting.byDistance(sortFrom.x, sortFrom.y, sortFrom.z));
            }

            layers.add(new Layer((RenderType)e.getKey(), data, mem));
         }
      }

      layers.sort((a, bx) -> Integer.compare(LAYERS.indexOf(a.type()), LAYERS.indexOf(bx.type())));
      return new Piece(index, layers, animated, quads);
   }

   private static RenderType layerOf(RenderType type) {
      return LAYERS.contains(type) ? type : RenderType.translucent();
   }

   private static BufferBuilder builder(Map<RenderType, BufferBuilder> builders, Map<RenderType, List<ByteBufferBuilder>> memory, RenderType type) {
      BufferBuilder bb = (BufferBuilder)builders.get(type);
      if (bb == null) {
         ByteBufferBuilder mem = new ByteBufferBuilder(type == RenderType.solid() ? 262144 : 65536);
         ((List)memory.computeIfAbsent(type, (t) -> new ArrayList())).add(mem);
         bb = new BufferBuilder(mem, Mode.QUADS, DefaultVertexFormat.BLOCK);
         builders.put(type, bb);
      }

      return bb;
   }

   static record Layer(RenderType type, MeshData data, List<ByteBufferBuilder> memory) {
      void close() {
         try {
            this.data.close();
         } catch (RuntimeException var3) {
         }

         for(ByteBufferBuilder b : this.memory) {
            b.close();
         }

      }
   }

   static record Piece(int index, List<Layer> layers, Set<TextureAtlasSprite> animated, int quads) {
      void close() {
         for(Layer l : this.layers) {
            l.close();
         }

      }
   }

   static record Cull(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float[] eyes) {
   }

   private static final class Sink implements VertexConsumer {
      private final boolean soft;
      private final boolean lit;
      private final @Nullable Cull cull;
      private final @Nullable BuildVolume occluders;
      private VertexConsumer out;
      private int emission;
      private float ox;
      private float oy;
      private float oz;
      private final float[] px = new float[4];
      private final float[] py = new float[4];
      private final float[] pz = new float[4];
      private final float[] u = new float[4];
      private final float[] v = new float[4];
      private final float[] nx = new float[4];
      private final float[] ny = new float[4];
      private final float[] nz = new float[4];
      private final int[] color = new int[4];
      private final int[] uv1 = new int[4];
      private final int[] uv2 = new int[4];
      private int n = -1;
      private float fx;
      private float fy;
      private float fz;

      Sink(boolean soft, boolean lit, @Nullable Cull cull, @Nullable BuildVolume occluders) {
         this.soft = soft;
         this.lit = lit;
         this.cull = cull;
         this.occluders = occluders;
      }

      Sink to(VertexConsumer out, int emission, float ox, float oy, float oz) {
         this.out = out;
         this.emission = emission;
         this.ox = ox;
         this.oy = oy;
         this.oz = oz;
         this.n = -1;
         return this;
      }

      public VertexConsumer addVertex(float x, float y, float z) {
         ++this.n;
         if (this.n >= 4) {
            this.n = 0;
         }

         this.px[this.n] = x + this.ox;
         this.py[this.n] = y + this.oy;
         this.pz[this.n] = z + this.oz;
         this.color[this.n] = -1;
         this.u[this.n] = this.v[this.n] = 0.0F;
         this.uv1[this.n] = 0;
         this.uv2[this.n] = 0;
         this.nx[this.n] = 0.0F;
         this.ny[this.n] = 1.0F;
         this.nz[this.n] = 0.0F;
         return this;
      }

      public VertexConsumer setColor(int r, int g, int b, int a) {
         if (this.n >= 0) {
            this.color[this.n] = (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
         }

         return this;
      }

      public VertexConsumer setUv(float uu, float vv) {
         if (this.n >= 0) {
            this.u[this.n] = uu;
            this.v[this.n] = vv;
         }

         return this;
      }

      public VertexConsumer setUv1(int a, int b) {
         if (this.n >= 0) {
            this.uv1[this.n] = (b & '\uffff') << 16 | a & '\uffff';
         }

         return this;
      }

      public VertexConsumer setUv2(int a, int b) {
         if (this.n >= 0) {
            this.uv2[this.n] = (b & '\uffff') << 16 | a & '\uffff';
         }

         return this;
      }

      public VertexConsumer setNormal(float x, float y, float z) {
         if (this.n < 0) {
            return this;
         } else {
            this.nx[this.n] = x;
            this.ny[this.n] = y;
            this.nz[this.n] = z;
            if (this.n == 3) {
               this.emit();
            }

            return this;
         }
      }

      private void emit() {
         this.n = -1;
         if (this.cull == null || this.seen(this.cull)) {
            if (this.occluders == null || this.cull == null || !this.hidden(this.occluders, this.cull.eyes())) {
               for(int i = 0; i < 4; ++i) {
                  int light;
                  if (this.soft) {
                     int received = this.lit ? this.uv2[i] & '\uffff' : 0;
                     light = received << 16 | this.emission << 4;
                  } else {
                     light = this.uv2[i];
                  }

                  this.out.addVertex(this.px[i], this.py[i], this.pz[i]);
                  int c = this.color[i];
                  this.out.setColor(c >> 16 & 255, c >> 8 & 255, c & 255, c >>> 24 & 255);
                  this.out.setUv(this.u[i], this.v[i]);
                  this.out.setUv2(light & '\uffff', light >>> 16 & '\uffff');
                  this.out.setNormal(this.nx[i], this.ny[i], this.nz[i]);
               }

            }
         }
      }

      private boolean seen(Cull c) {
         float ax = this.px[1] - this.px[0];
         float ay = this.py[1] - this.py[0];
         float az = this.pz[1] - this.pz[0];
         float bx = this.px[2] - this.px[0];
         float by = this.py[2] - this.py[0];
         float bz = this.pz[2] - this.pz[0];
         this.fx = ay * bz - az * by;
         this.fy = az * bx - ax * bz;
         this.fz = ax * by - ay * bx;
         if (this.fx * this.fx + this.fy * this.fy + this.fz * this.fz < 1.0E-12F) {
            float cx = this.px[3] - this.px[0];
            float cy = this.py[3] - this.py[0];
            float cz = this.pz[3] - this.pz[0];
            this.fx = by * cz - bz * cy;
            this.fy = bz * cx - bx * cz;
            this.fz = bx * cy - by * cx;
            if (this.fx * this.fx + this.fy * this.fy + this.fz * this.fz < 1.0E-12F) {
               this.fx = this.fy = this.fz = 0.0F;
               return true;
            }
         }

         float qx = this.fx > 0.0F ? c.maxX() : c.minX();
         float qy = this.fy > 0.0F ? c.maxY() : c.minY();
         float qz = this.fz > 0.0F ? c.maxZ() : c.minZ();
         return this.fx * (qx - this.px[0]) + this.fy * (qy - this.py[0]) + this.fz * (qz - this.pz[0]) > 0.0F;
      }

      private boolean hidden(BuildVolume v, float[] eyes) {
         float len = (float)Math.sqrt((double)(this.fx * this.fx + this.fy * this.fy + this.fz * this.fz));
         if (len < 1.0E-6F) {
            return false;
         } else {
            float lx = this.fx / len * 0.02F;
            float ly = this.fy / len * 0.02F;
            float lz = this.fz / len * 0.02F;
            float mx = (this.px[0] + this.px[1] + this.px[2] + this.px[3]) / 4.0F;
            float my = (this.py[0] + this.py[1] + this.py[2] + this.py[3]) / 4.0F;
            float mz = (this.pz[0] + this.pz[1] + this.pz[2] + this.pz[3]) / 4.0F;

            for(int e = 0; e < eyes.length; e += 3) {
               float ex = eyes[e];
               float ey = eyes[e + 1];
               float ez = eyes[e + 2];
               if (v.clearLine(mx + lx, my + ly, mz + lz, ex, ey, ez)) {
                  return false;
               }
            }

            int corners = Math.min(eyes.length, 6);

            for(int i = 0; i < 4; ++i) {
               float cx = this.px[i] + (mx - this.px[i]) * 0.1F + lx;
               float cy = this.py[i] + (my - this.py[i]) * 0.1F + ly;
               float cz = this.pz[i] + (mz - this.pz[i]) * 0.1F + lz;

               for(int e = 0; e < corners; e += 3) {
                  if (v.clearLine(cx, cy, cz, eyes[e], eyes[e + 1], eyes[e + 2])) {
                     return false;
                  }
               }
            }

            return true;
         }
      }
   }
}
