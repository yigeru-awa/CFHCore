package com.onehumanawa.cfhcore.content.gui.core.stage;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class StageSoft {
   public static final VertexFormat ENTITY;
   public static final VertexFormat BLOCK;
   private static @Nullable ShaderInstance entity;
   private static @Nullable ShaderInstance block;
   private static boolean failed;
   private static final Vector3f scratch;
   private static float muted;
   private static final float[] lampPositions;
   private static final float[] lampColors;
   private static final float[] quarter;
   private static float stylized;
   private static float alphaCutoff;
   private static boolean alphaBaseLevel;
   private static final float[] cast;
   private static final float[] footprint;
   private static float footprintFrom;
   private static float footprintRound;

   public static boolean ready() {
      if (failed) {
         return false;
      } else if (block != null) {
         return true;
      } else {
         try {
            ResourceManager resources = Minecraft.getInstance().getResourceManager();
            entity = new ShaderInstance(resources, "cfhgui:cfhgui_stage_soft", ENTITY);
            block = new ShaderInstance(resources, "cfhgui:cfhgui_stage_soft", BLOCK);
            return true;
         } catch (Exception e) {
            failed = true;
            drop();
            CFHGui.LOGGER.warn("[CFHGui] stage: scenes are lit the game's own way, the soft shader did not build: {}", e.toString());
            return false;
         }
      }
   }

   static void reload() {
      drop();
      failed = false;
   }

   private static void drop() {
      if (entity != null) {
         entity.close();
         entity = null;
      }

      if (block != null) {
         block.close();
         block = null;
      }

   }

   public static @Nullable ShaderInstance shader(VertexFormat format) {
      return format == BLOCK ? block : entity;
   }

   static void light(StageLighting rig, Matrix4f view, float seconds, boolean animate) {
      rig.lampsFor(view, seconds, animate, lampPositions, lampColors);

      for(ShaderInstance s : new ShaderInstance[]{entity, block}) {
         if (s != null) {
            System.arraycopy(lampPositions, 0, quarter, 0, 16);
            s.safeGetUniform("LampPosA").set(quarter);
            System.arraycopy(lampPositions, 16, quarter, 0, 16);
            s.safeGetUniform("LampPosB").set(quarter);
            System.arraycopy(lampColors, 0, quarter, 0, 16);
            s.safeGetUniform("LampColA").set(quarter);
            System.arraycopy(lampColors, 16, quarter, 0, 16);
            s.safeGetUniform("LampColB").set(quarter);
         }
      }

      light(rig, view);
   }

   static void light(StageLighting rig, Matrix4f view) {
      for(ShaderInstance s : new ShaderInstance[]{entity, block}) {
         if (s != null) {
            scratch.set(rig.keyPos());
            view.transformPosition(scratch);
            s.safeGetUniform("KeyPos").set(scratch.x, scratch.y, scratch.z);
            scratch.set(rig.fillDir());
            view.transformDirection(scratch);
            s.safeGetUniform("FillDir").set(scratch.x, scratch.y, scratch.z);
            float b = rig.brightness();
            s.safeGetUniform("KeyColor").set(rig.keyColor().x * b, rig.keyColor().y * b, rig.keyColor().z * b);
            s.safeGetUniform("FillColor").set(rig.fillColor().x * b, rig.fillColor().y * b, rig.fillColor().z * b);
            s.safeGetUniform("Ambient").set(rig.ambientColor().x * b, rig.ambientColor().y * b, rig.ambientColor().z * b);
            s.safeGetUniform("RimColor").set(rig.rimColor().x, rig.rimColor().y, rig.rimColor().z);
            s.safeGetUniform("RimStrength").set(rig.rimStrength() * b);
            s.safeGetUniform("Wrap").set(rig.wrap());
         }
      }

   }

   static void stylize(float amount) {
      float s = Math.max(0.0F, Math.min(1.0F, amount));
      stylized = s;

      for(ShaderInstance shader : new ShaderInstance[]{entity, block}) {
         if (shader != null) {
            shader.safeGetUniform("Stylize").set(s);
         }
      }

   }

   private static void bind(ResourceLocation texture) {
      RenderSystem.setShaderTexture(0, texture);
      if (stylized > 0.0F) {
         AbstractTexture var2 = Minecraft.getInstance().getTextureManager().getTexture(texture);
         if (var2 instanceof TextureAtlas) {
            TextureAtlas atlas = (TextureAtlas)var2;
            atlas.setFilter(false, true);
         }
      }

   }

   public static void muted(float amount) {
      muted = Math.max(0.0F, Math.min(1.0F, amount));
   }

   public static void cast(int rgb, float amount) {
      cast[0] = (float)(rgb >> 16 & 255) / 255.0F;
      cast[1] = (float)(rgb >> 8 & 255) / 255.0F;
      cast[2] = (float)(rgb & 255) / 255.0F;
      cast[3] = Math.max(0.0F, Math.min(1.0F, amount));
   }

   public static void noCast() {
      cast[3] = 0.0F;
   }

   public static void alphaTest(float cutoff, boolean baseLevel) {
      alphaCutoff = cutoff;
      alphaBaseLevel = baseLevel;
   }

   public static void plainAlpha() {
      alphaTest(0.1F, false);
   }

   public static void footprint(float middleX, float middleZ, float halfX, float halfZ, float from, float roundness) {
      footprint[0] = middleX;
      footprint[1] = middleZ;
      footprint[2] = 1.0F / Math.max(1.0E-4F, halfX);
      footprint[3] = 1.0F / Math.max(1.0E-4F, halfZ);
      footprintFrom = Math.max(0.0F, Math.min(0.999F, from));
      footprintRound = Math.max(1.0F, roundness);
   }

   public static void noFootprint() {
      footprintRound = 0.0F;
   }

   private static void footprint(ShaderInstance shader, boolean on) {
      if (on && footprintRound > 0.0F) {
         shader.safeGetUniform("Footprint").set(footprint[0], footprint[1], footprint[2], footprint[3]);
         shader.safeGetUniform("FootprintFade").set(footprintFrom, footprintRound);
      } else {
         shader.safeGetUniform("FootprintFade").set(1.0F, 0.0F);
      }

   }

   public static BufferBuilder begin(VertexFormat format) {
      return Tesselator.getInstance().begin(Mode.QUADS, format);
   }

   public static void end(BufferBuilder builder, VertexFormat format, ResourceLocation texture, float alpha) {
      end(builder, format, texture, alpha, true);
   }

   public static void end(BufferBuilder builder, VertexFormat format, ResourceLocation texture, float alpha, boolean cull) {
      end(builder, format, texture, alpha, cull, 0.0F);
   }

   public static void end(BufferBuilder builder, VertexFormat format, ResourceLocation texture, float alpha, boolean cull, float glow) {
      MeshData data = builder.build();
      if (data != null) {
         ShaderInstance shader = shader(format);
         if (shader == null) {
            data.close();
         } else {
            float[] was = (float[])RenderSystem.getShaderColor().clone();
            state(alpha);
            if (!cull) {
               RenderSystem.disableCull();
            }

            RenderSystem.setShader(() -> shader);
            bind(texture);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            shader.safeGetUniform("Muted").set(muted);
            shader.safeGetUniform("Cast").set(cast[0], cast[1], cast[2], cast[3]);
            footprint(shader, false);
            shader.safeGetUniform("Emissive").set(glow);
            BufferUploader.drawWithShader(data);
            shader.safeGetUniform("Emissive").set(0.0F);
            restore(was);
         }
      }
   }

   public static void draw(VertexBuffer buffer, Matrix4f modelView, Matrix4f projection, ResourceLocation texture, float alpha, boolean translucent) {
      draw(buffer, modelView, projection, texture, alpha, translucent, 0.0F);
   }

   public static void draw(VertexBuffer buffer, Matrix4f modelView, Matrix4f projection, ResourceLocation texture, float alpha, boolean translucent, float glow) {
      draw(buffer, modelView, projection, texture, alpha, translucent, glow, 0.0F);
   }

   public static void draw(VertexBuffer buffer, Matrix4f modelView, Matrix4f projection, ResourceLocation texture, float alpha, boolean translucent, float glow, float gloss) {
      ShaderInstance shader = block;
      if (shader != null) {
         float[] was = (float[])RenderSystem.getShaderColor().clone();
         state(translucent ? Math.min(alpha, 0.999F) : alpha);
         if (translucent) {
            RenderSystem.depthMask(false);
         }

         bind(texture);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
         shader.safeGetUniform("Muted").set(muted);
         shader.safeGetUniform("Cast").set(cast[0], cast[1], cast[2], cast[3]);
         footprint(shader, true);
         shader.safeGetUniform("Emissive").set(glow);
         shader.safeGetUniform("Gloss").set(gloss);
         shader.safeGetUniform("AlphaCutoff").set(alphaCutoff);
         shader.safeGetUniform("BaseLevel").set(alphaBaseLevel ? 1.0F : 0.0F);
         buffer.bind();
         buffer.drawWithShader(modelView, projection, shader);
         VertexBuffer.unbind();
         shader.safeGetUniform("Emissive").set(0.0F);
         shader.safeGetUniform("Gloss").set(0.0F);
         shader.safeGetUniform("AlphaCutoff").set(0.1F);
         shader.safeGetUniform("BaseLevel").set(0.0F);
         restore(was);
      }
   }

   public static void drawAll(List<VertexBuffer> buffers, Matrix4f modelView, Matrix4f projection, ResourceLocation texture, float alpha, boolean translucent, float glow, float gloss, @Nullable float[] received) {
      ShaderInstance shader = block;
      if (shader != null && !buffers.isEmpty()) {
         float[] was = (float[])RenderSystem.getShaderColor().clone();
         state(translucent ? Math.min(alpha, 0.999F) : alpha);
         if (translucent) {
            RenderSystem.depthMask(false);
         }

         bind(texture);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
         shader.safeGetUniform("Muted").set(muted);
         shader.safeGetUniform("Cast").set(cast[0], cast[1], cast[2], cast[3]);
         footprint(shader, true);
         shader.safeGetUniform("Emissive").set(glow);
         shader.safeGetUniform("Gloss").set(gloss);
         shader.safeGetUniform("AlphaCutoff").set(alphaCutoff);
         shader.safeGetUniform("BaseLevel").set(alphaBaseLevel ? 1.0F : 0.0F);
         if (received != null) {
            shader.safeGetUniform("Received").set(received[0], received[1], received[2]);
         }

         shader.setDefaultUniforms(Mode.QUADS, modelView, projection, Minecraft.getInstance().getWindow());
         shader.apply();

         for(VertexBuffer b : buffers) {
            b.bind();
            b.draw();
         }

         VertexBuffer.unbind();
         shader.clear();
         shader.safeGetUniform("Emissive").set(0.0F);
         shader.safeGetUniform("Gloss").set(0.0F);
         shader.safeGetUniform("AlphaCutoff").set(0.1F);
         shader.safeGetUniform("BaseLevel").set(0.0F);
         shader.safeGetUniform("Received").set(0.0F, 0.0F, 0.0F);
         restore(was);
      }
   }

   private static void restore(float[] shaderColor) {
      RenderSystem.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      RenderSystem.enableBlend();
      StageBlend.over();
   }

   private static void state(float alpha) {
      RenderSystem.enableDepthTest();
      RenderSystem.depthFunc(515);
      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      if (alpha < 1.0F) {
         RenderSystem.enableBlend();
         StageBlend.over();
      } else {
         RenderSystem.disableBlend();
      }

   }

   private StageSoft() {
   }

   static {
      ENTITY = DefaultVertexFormat.NEW_ENTITY;
      BLOCK = DefaultVertexFormat.BLOCK;
      scratch = new Vector3f();
      lampPositions = new float[32];
      lampColors = new float[32];
      quarter = new float[16];
      alphaCutoff = 0.1F;
      cast = new float[4];
      footprint = new float[]{0.0F, 0.0F, 1.0F, 1.0F};
      footprintFrom = 1.0F;
   }
}
