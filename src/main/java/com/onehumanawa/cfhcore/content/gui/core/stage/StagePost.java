package com.onehumanawa.cfhcore.content.gui.core.stage;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

final class StagePost implements AutoCloseable {
   private static final int DOWNSCALE = 4;
   private static @Nullable ShaderInstance bright;
   private static @Nullable ShaderInstance blur;
   private static @Nullable ShaderInstance finish;
   private static boolean failed;
   private @Nullable TextureTarget a;
   private @Nullable TextureTarget b;
   private final Matrix4f identity = new Matrix4f();

   static boolean ready() {
      if (failed) {
         return false;
      } else if (finish != null) {
         return true;
      } else {
         try {
            ResourceManager resources = Minecraft.getInstance().getResourceManager();
            bright = new ShaderInstance(resources, "cfhgui:cfhgui_stage_bright", DefaultVertexFormat.POSITION_TEX);
            blur = new ShaderInstance(resources, "cfhgui:cfhgui_stage_blur", DefaultVertexFormat.POSITION_TEX);
            finish = new ShaderInstance(resources, "cfhgui:cfhgui_stage_finish", DefaultVertexFormat.POSITION_TEX);
            return true;
         } catch (Exception e) {
            failed = true;
            drop();
            CFHGui.LOGGER.warn("[CFHGui] stage: scenes are shown without their finish (glow, grade): {}", e.toString());
            return false;
         }
      }
   }

   static void reload() {
      drop();
      failed = false;
   }

   private static void drop() {
      if (bright != null) {
         bright.close();
         bright = null;
      }

      if (blur != null) {
         blur.close();
         blur = null;
      }

      if (finish != null) {
         finish.close();
         finish = null;
      }

   }

   static @Nullable ShaderInstance finishShader() {
      return finish;
   }

   void glow(TextureTarget scene, StageFinish look) {
      int w = Math.max(1, scene.width / 4);
      int h = Math.max(1, scene.height / 4);
      if (this.a == null || this.a.width != w || this.a.height != h) {
         this.release();
         this.a = target(w, h);
         this.b = target(w, h);
      }

      ShaderInstance brightPass = bright;
      ShaderInstance blurPass = blur;
      if (brightPass != null && blurPass != null && this.a != null && this.b != null) {
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.disableBlend();
         RenderSystem.disableCull();
         RenderSystem.setProjectionMatrix(this.identity, VertexSorting.ORTHOGRAPHIC_Z);
         brightPass.safeGetUniform("Threshold").set(look.threshold);
         pass(brightPass, scene.getColorTextureId(), this.a);

         for(int i = 0; i < 2; ++i) {
            float reach = look.spread * (float)(i + 1);
            blurPass.safeGetUniform("Step").set(reach / (float)w, 0.0F);
            pass(blurPass, this.a.getColorTextureId(), this.b);
            blurPass.safeGetUniform("Step").set(0.0F, reach / (float)h);
            pass(blurPass, this.b.getColorTextureId(), this.a);
         }

         RenderSystem.enableCull();
         RenderSystem.enableBlend();
         RenderSystem.depthMask(true);
         RenderSystem.enableDepthTest();
      }
   }

   int glowTexture() {
      return this.a == null ? 0 : this.a.getColorTextureId();
   }

   private static TextureTarget target(int w, int h) {
      TextureTarget t = new TextureTarget(w, h, false, Minecraft.ON_OSX);
      t.setFilterMode(9729);
      t.setClearColor(0.0F, 0.0F, 0.0F, 1.0F);
      return t;
   }

   private static void pass(ShaderInstance shader, int texture, TextureTarget into) {
      into.bindWrite(true);
      RenderSystem.setShader(() -> shader);
      RenderSystem.setShaderTexture(0, texture);
      BufferBuilder bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      bb.addVertex(-1.0F, -1.0F, 0.0F).setUv(0.0F, 0.0F);
      bb.addVertex(1.0F, -1.0F, 0.0F).setUv(1.0F, 0.0F);
      bb.addVertex(1.0F, 1.0F, 0.0F).setUv(1.0F, 1.0F);
      bb.addVertex(-1.0F, 1.0F, 0.0F).setUv(0.0F, 1.0F);
      BufferUploader.drawWithShader(bb.buildOrThrow());
   }

   private void release() {
      if (this.a != null) {
         this.a.destroyBuffers();
         this.a = null;
      }

      if (this.b != null) {
         this.b.destroyBuffers();
         this.b = null;
      }

   }

   public void close() {
      this.release();
   }
}
