package com.onehumanawa.cfhcore.content.gui.core.client.preview;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Style;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class ScreenPreview implements AutoCloseable {
   private static final int VIRTUAL_WIDTH = 427;
   private static final int MAX_TEXTURE = 2048;
   private final Supplier<Screen> factory;
   private @Nullable Screen screen;
   private @Nullable TextureTarget target;
   private int vw;
   private int vh;
   private int tw;
   private int th;
   private boolean failed;
   private final int[] viewportWas = new int[4];
   private final int[] scissorBox = new int[4];

   public ScreenPreview(Supplier<Screen> factory) {
      this.factory = factory;
   }

   public void invalidate() {
      this.dispose();
      this.failed = false;
   }

   public boolean failed() {
      return this.failed;
   }

   public void tick() {
      if (this.screen != null) {
         try {
            this.screen.tick();
         } catch (Exception e) {
            this.fail("tick", e);
         }
      }

   }

   public boolean render(GuiGraphics g, int x, int y, int w, int h, float alpha, float partialTick) {
      if (!this.failed && w >= 32 && h >= 18 && !(alpha <= 0.004F)) {
         Minecraft mc = Minecraft.getInstance();
         Window win = mc.getWindow();
         int scale = (int)Math.max(1L, Math.round(win.getGuiScale()));
         int pw = Math.min(2048, w * scale);
         int ph = Math.min(2048, h * scale);
         int k = Math.max(1, Math.round((float)pw / 427.0F));
         int nvw = Math.max(1, pw / k);
         int nvh = Math.max(1, ph / k);

         try {
            if (this.target == null || this.tw != pw || this.th != ph) {
               if (this.target != null) {
                  this.target.destroyBuffers();
               }

               this.target = new TextureTarget(pw, ph, true, Minecraft.ON_OSX);
               this.target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
               this.tw = pw;
               this.th = ph;
            }

            if (this.screen == null || this.vw != nvw || this.vh != nvh) {
               if (this.screen != null) {
                  this.screen.removed();
               }

               this.vw = nvw;
               this.vh = nvh;
               MenuBackdrop.suspend();

               try {
                  this.screen = (Screen)this.factory.get();
                  this.screen.init(mc, this.vw, this.vh);
               } finally {
                  MenuBackdrop.resume();
               }
            }
         } catch (Exception e) {
            this.fail("build", e);
            return false;
         }

         g.flush();
         int framebufferWas = GL11.glGetInteger(36006);
         GL11.glGetIntegerv(2978, this.viewportWas);
         boolean scissorWas = GL11.glIsEnabled(3089);
         if (scissorWas) {
            GL11.glGetIntegerv(3088, this.scissorBox);
            RenderSystem.disableScissor();
         }

         Style outerStyle = Theme.activeStyle();
         Theme.activate(MenuSlots.styleFor(this.screen));
         MenuBackdrop.suspend();

         label222: {
            boolean var20;
            try {
               this.target.clear(Minecraft.ON_OSX);
               this.target.bindWrite(true);
               RenderSystem.setProjectionMatrix((new Matrix4f()).setOrtho(0.0F, (float)this.vw, (float)this.vh, 0.0F, 1000.0F, 21000.0F), VertexSorting.ORTHOGRAPHIC_Z);
               GuiGraphics inner = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
               this.screen.render(inner, -1, -1, partialTick);
               inner.flush();
               break label222;
            } catch (Exception e) {
               this.fail("render", e);
               var20 = false;
            } finally {
               MenuBackdrop.resume();
               Theme.activate(outerStyle);
               RenderSystem.setProjectionMatrix((new Matrix4f()).setOrtho(0.0F, (float)((double)win.getWidth() / win.getGuiScale()), (float)((double)win.getHeight() / win.getGuiScale()), 0.0F, 1000.0F, 21000.0F), VertexSorting.ORTHOGRAPHIC_Z);
               GlStateManager._glBindFramebuffer(36160, framebufferWas);
               RenderSystem.viewport(this.viewportWas[0], this.viewportWas[1], this.viewportWas[2], this.viewportWas[3]);
               if (scissorWas) {
                  RenderSystem.enableScissor(this.scissorBox[0], this.scissorBox[1], this.scissorBox[2], this.scissorBox[3]);
               }

               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               RenderSystem.enableDepthTest();
               RenderSystem.depthMask(true);
               RenderSystem.enableCull();
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }

            return var20;
         }

         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderTexture(0, this.target.getColorTextureId());
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
         RenderSystem.enableBlend();
         Matrix4f e = g.pose().last().pose();
         BufferBuilder b = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
         b.addVertex(e, (float)x, (float)y, 0.0F).setUv(0.0F, 1.0F);
         b.addVertex(e, (float)x, (float)(y + h), 0.0F).setUv(0.0F, 0.0F);
         b.addVertex(e, (float)(x + w), (float)(y + h), 0.0F).setUv(1.0F, 0.0F);
         b.addVertex(e, (float)(x + w), (float)y, 0.0F).setUv(1.0F, 1.0F);
         BufferUploader.drawWithShader(b.buildOrThrow());
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         return true;
      } else {
         return false;
      }
   }

   private void fail(String what, Exception e) {
      this.failed = true;
      CFHGui.LOGGER.warn("[CFHGui] screen preview failed to {} ({}); showing the painted preview instead", what, e.toString());
      this.dispose();
   }

   private void dispose() {
      if (this.screen != null) {
         try {
            this.screen.removed();
         } catch (Exception var2) {
         }

         this.screen = null;
      }

   }

   public void close() {
      this.dispose();
      if (this.target != null) {
         this.target.destroyBuffers();
         this.target = null;
      }

   }
}
