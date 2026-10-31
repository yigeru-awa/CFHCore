package com.onehumanawa.cfhcore.content.gui.core.stage.builds;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageBlockEntities;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageLevel;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageResources;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

final class BakedEntities implements AutoCloseable {
   private final List<Layer> layers = new ArrayList();
   private final int generation = StageResources.generation();
   private boolean closed;
   private static final Map<Class<?>, Optional<Method>> SPEED = new ConcurrentHashMap();

   private BakedEntities() {
   }

   boolean stale() {
      return this.generation != StageResources.generation();
   }

   static boolean moves(BlockEntity be, boolean far) {
      if (far) {
         return false;
      } else {
         ResourceLocation id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType());
         if (id != null) {
            String path = id.getPath();
            if (path.contains("encased") || path.equals("gearbox") || path.equals("clutch") || path.equals("gearshift") || path.endsWith("_gearshift")) {
               return false;
            }
         }

         return turns(be);
      }
   }

   private static boolean turns(BlockEntity be) {
      Optional<Method> m = (Optional)SPEED.computeIfAbsent(be.getClass(), (c) -> {
         try {
            Method get = c.getMethod("getSpeed");
            return get.getParameterCount() == 0 && get.getReturnType() == Float.TYPE ? Optional.of(get) : Optional.empty();
         } catch (RuntimeException | NoSuchMethodException var2) {
            return Optional.empty();
         }
      });
      if (m.isEmpty()) {
         return false;
      } else {
         try {
            return (Float)((Method)m.get()).invoke(be) != 0.0F;
         } catch (RuntimeException | ReflectiveOperationException var3) {
            return true;
         }
      }
   }

   static BakedEntities bake(List<BlockEntity> still, BlockPos origin, int light, Matrix3f facing, StageLevel level) {
      BakedEntities out = new BakedEntities();
      BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
      Capture capture = new Capture();
      PoseStack pose = new PoseStack();
      pose.last().normal().set(facing);
      Runnable restore = BuildEntities.cullingAnything();

      try {
         for(BlockEntity be : still) {
            if (!StageBlockEntities.leftOut(be)) {
               BlockEntityRenderer<BlockEntity> r = dispatcher.getRenderer(be);
               if (r != null) {
                  BlockPos p = be.getBlockPos();
                  pose.pushPose();
                  pose.translate((float)(p.getX() - origin.getX()), (float)(p.getY() - origin.getY()), (float)(p.getZ() - origin.getZ()));

                  try {
                     StageBlockEntities.draw(level, be, r, 0.0F, pose, capture, light);
                  } finally {
                     pose.popPose();
                  }
               }
            }
         }
      } finally {
         restore.run();
      }

      capture.upload(out.layers);
      return out;
   }

   void draw(Matrix4f modelView, Matrix4f projection) {
      if (!this.closed) {
         for(Layer l : this.layers) {
            l.type().setupRenderState();

            try {
               ShaderInstance shader = RenderSystem.getShader();
               if (shader != null) {
                  if (shader.CHUNK_OFFSET != null) {
                     shader.CHUNK_OFFSET.set(0.0F, 0.0F, 0.0F);
                  }

                  l.buffer().bind();
                  l.buffer().drawWithShader(modelView, projection, shader);
               }
            } finally {
               VertexBuffer.unbind();
               l.type().clearRenderState();
            }
         }

      }
   }

   public void close() {
      if (!this.closed) {
         this.closed = true;

         for(Layer l : this.layers) {
            l.buffer().close();
         }

         this.layers.clear();
      }
   }

   private static record Layer(RenderType type, VertexBuffer buffer) {
   }

   private static final class Capture implements MultiBufferSource {
      private final Map<RenderType, BufferBuilder> builders = new LinkedHashMap();
      private final List<ByteBufferBuilder> memory = new ArrayList();

      public VertexConsumer getBuffer(RenderType type) {
         BufferBuilder b = (BufferBuilder)this.builders.get(type);
         if (b == null) {
            ByteBufferBuilder m = new ByteBufferBuilder(Math.max(4096, type.bufferSize()));
            this.memory.add(m);
            b = new BufferBuilder(m, type.mode(), type.format());
            this.builders.put(type, b);
         }

         return b;
      }

      void upload(List<Layer> into) {
         List<Layer> blended = new ArrayList();

         try {
            for(Map.Entry<RenderType, BufferBuilder> e : this.builders.entrySet()) {
               MeshData data = ((BufferBuilder)e.getValue()).build();
               if (data != null) {
                  VertexBuffer vb = new VertexBuffer(Usage.STATIC);
                  vb.bind();
                  vb.upload(data);
                  VertexBuffer.unbind();
                  (blended((RenderType)e.getKey()) ? blended : into).add(new Layer((RenderType)e.getKey(), vb));
               }
            }
         } finally {
            for(ByteBufferBuilder m : this.memory) {
               m.close();
            }

         }

         into.addAll(blended);
      }

      private static boolean blended(RenderType type) {
         String name = type.toString();
         return name.contains("translucent") || name.contains("glint");
      }
   }
}
