package com.onehumanawa.cfhcore.content.gui.core.stage;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.mixin.CameraAccessor;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class Stage implements AutoCloseable {
   public static final int MAX_TEXTURE = 4096;
   private static final float TICK_MS = 50.0F;
   private static final List<Stage> LIVE = new ArrayList();
   private static final boolean DEBUG = Files.exists(CFHGuiPlatform.get().configDir().resolve("cfhgui").resolve("stage-debug"), new LinkOption[0]);
   private final StageLighting lighting = new StageLighting();
   private final StageLevel level;
   private final StageCamera camera;
   private final List<StageNode> nodes;
   private final StageRenderContext ctx;
   private @Nullable TextureTarget target;
   private int targetW;
   private int targetH;
   private float resolutionScale;
   private int background;
   private int gradientTop;
   private int gradientBottom;
   private boolean gradient;
   private boolean fog;
   private float fogStart;
   private float fogEnd;
   private int fogColor;
   private float alpha;
   private boolean showFocusOutline;
   private int outlineColor;
   private @Nullable StageFinish finish;
   private @Nullable StagePost post;
   private boolean glowReady;
   private boolean soft;
   private float stylize;
   private boolean targetLinear;
   private long lastRenderMs;
   private float tickAccMs;
   private float timeMs;
   private long lastFrameNanos;
   private float frameMsAvg;
   private long frames;
   private @Nullable StageNode hovered;
   private @Nullable StageNode pressed;
   private @Nullable StageNode focused;
   private boolean focusVisible;
   private boolean focusByKey;
   private long hoverSinceMs;
   private @Nullable Screen owner;
   private boolean closed;
   private boolean walking;
   private final List<StageNode> leaving;
   private final Set<StageNode> failing;
   private int failingResources;
   private @Nullable Camera farCamera;
   private @Nullable Entity cameraAnchor;
   private final int[] scissorBox;
   private final int[] viewportWas;
   private final float[] savedFogColor;
   private final float[] savedShaderColor;
   private final Matrix4f savedProjection;
   private final Matrix4f identity;
   private @Nullable VertexSorting savedSorting;
   private static boolean depthLogged;

   private void debugReadback(Minecraft mc, int pw, int ph) {
      ByteBuffer px = BufferUtils.createByteBuffer(4);
      GL11.glReadPixels(pw / 2, ph / 2, 1, 1, 6408, 5121, px);
      int err = GL11.glGetError();
      int bound = GL11.glGetInteger(36006);
      CFHGui.LOGGER.info("[CFHGui] stage debug: frame {} fbo {} (bound {}) tex {} size {}x{} centre rgba=({},{},{},{}) glError={} shader={} nodes={} gradient={}", new Object[]{this.frames, this.target == null ? -1 : this.target.frameBufferId, bound, this.target == null ? -1 : this.target.getColorTextureId(), pw, ph, px.get(0) & 255, px.get(1) & 255, px.get(2) & 255, px.get(3) & 255, err, RenderSystem.getShader() == null ? "null" : RenderSystem.getShader().getName(), this.nodes.size(), this.gradient});
   }

   public Stage() {
      this.level = new StageLevel(this.lighting);
      this.camera = new StageCamera();
      this.nodes = new ArrayList();
      this.ctx = new StageRenderContext();
      this.resolutionScale = 1.0F;
      this.fogStart = 16.0F;
      this.fogEnd = 48.0F;
      this.fogColor = -15329771;
      this.alpha = 1.0F;
      this.showFocusOutline = true;
      this.leaving = new ArrayList();
      this.failing = Collections.newSetFromMap(new IdentityHashMap());
      this.failingResources = -1;
      this.scissorBox = new int[4];
      this.viewportWas = new int[4];
      this.savedFogColor = new float[4];
      this.savedShaderColor = new float[4];
      this.savedProjection = new Matrix4f();
      this.identity = new Matrix4f();
      StageResources.ensureRegistered();
      StageFaults.install();
      synchronized(LIVE) {
         LIVE.add(this);
      }
   }

   public StageLevel level() {
      return this.level;
   }

   public StageCamera camera() {
      return this.camera;
   }

   public StageLighting lighting() {
      return this.lighting;
   }

   public List<StageNode> nodes() {
      return this.nodes;
   }

   public <T extends StageNode> T add(T node) {
      this.nodes.add(node);
      return node;
   }

   public void remove(StageNode node) {
      if (this.walking) {
         if (!this.leaving.contains(node)) {
            this.leaving.add(node);
         }

      } else {
         if (this.nodes.remove(node)) {
            if (this.hovered == node) {
               this.hovered = null;
            }

            if (this.pressed == node) {
               this.pressed = null;
            }

            if (this.focused == node) {
               this.focused = null;
            }

            node.dispose();
         }

      }
   }

   public Stage background(int argb) {
      this.background = argb;
      this.gradient = false;
      return this;
   }

   public Stage gradient(int top, int bottom) {
      this.gradientTop = top;
      this.gradientBottom = bottom;
      this.gradient = true;
      return this;
   }

   public Stage resolutionScale(float s) {
      this.resolutionScale = Mth.clamp(s, 0.25F, 2.0F);
      return this;
   }

   public float resolutionScale() {
      return this.resolutionScale;
   }

   public Stage fog(boolean on, float start, float end, int color) {
      this.fog = on && end > start;
      this.fogStart = start;
      this.fogEnd = end;
      this.fogColor = color;
      return this;
   }

   public Stage fog(boolean on) {
      this.fog = on && this.fogEnd > this.fogStart;
      return this;
   }

   public Stage finish(@Nullable StageFinish look) {
      this.finish = look;
      return this;
   }

   public @Nullable StageFinish finish() {
      return this.finish;
   }

   public Stage soft(boolean on) {
      this.soft = on;
      return this;
   }

   public boolean soft() {
      return this.soft;
   }

   public Stage stylize(float amount) {
      this.stylize = Mth.clamp(amount, 0.0F, 1.0F);
      return this;
   }

   public float stylize() {
      return this.stylize;
   }

   public Stage alpha(float a) {
      this.alpha = Mth.clamp(a, 0.0F, 1.0F);
      return this;
   }

   public float alpha() {
      return this.alpha;
   }

   public Stage focusOutline(boolean on) {
      this.showFocusOutline = on;
      return this;
   }

   public Stage outlineColor(int argb) {
      this.outlineColor = argb;
      return this;
   }

   public Stage bind(@Nullable Screen screen) {
      this.owner = screen;
      return this;
   }

   public @Nullable StageNode hovered() {
      return this.hovered;
   }

   public @Nullable StageNode focused() {
      return this.focused;
   }

   public boolean isClosed() {
      return this.closed;
   }

   public float timeMs() {
      return this.timeMs;
   }

   public float frameMs() {
      return this.frameMsAvg;
   }

   public float lastFrameMs() {
      return (float)this.lastFrameNanos / 1000000.0F;
   }

   public int targetWidth() {
      return this.targetW;
   }

   public int targetHeight() {
      return this.targetH;
   }

   public long frames() {
      return this.frames;
   }

   public void focusVisible(boolean visible) {
      this.focusVisible = visible;
   }

   public void focus(@Nullable StageNode node) {
      if (this.focused != node) {
         if (this.focused != null) {
            this.focused.setFocused(false);
         }

         this.focused = node;
         if (this.focused != null) {
            this.focused.setFocused(true);
         }

      }
   }

   public boolean focusNext(int direction) {
      StageNode first = null;
      StageNode last = null;
      StageNode next = null;
      boolean passed = false;
      if (direction >= 0) {
         for(StageNode n : this.nodes) {
            if (n.pickable() && n.visible()) {
               if (first == null) {
                  first = n;
               }

               if (passed && next == null) {
                  next = n;
               }

               if (n == this.focused) {
                  passed = true;
               }
            }
         }
      } else {
         for(int i = this.nodes.size() - 1; i >= 0; --i) {
            StageNode n = (StageNode)this.nodes.get(i);
            if (n.pickable() && n.visible()) {
               if (first == null) {
                  first = n;
               }

               if (passed && next == null) {
                  next = n;
               }

               if (n == this.focused) {
                  passed = true;
               }
            }
         }
      }

      if (this.focused == null) {
         this.focus(first);
         return first != null;
      } else if (next == null) {
         this.focus((StageNode)null);
         return false;
      } else {
         this.focus(next);
         return true;
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.hovered != null) {
         this.pressed = this.hovered;
         this.pressed.setPressed(true);
         return true;
      } else {
         return false;
      }
   }

   public void cancelPress() {
      if (this.pressed != null) {
         this.pressed.setPressed(false);
         this.pressed = null;
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0 && this.pressed != null) {
         StageNode p = this.pressed;
         this.pressed = null;
         p.setPressed(false);
         if (p == this.hovered) {
            this.focusByKey = false;
            this.focus(p);
            CFHGuiSounds.click();
            p.activate();
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean keyPressed(int key, int scan, int modifiers) {
      boolean shift = (modifiers & 1) != 0;
      switch (key) {
         case 32:
         case 257:
         case 335:
            if (this.focused == null) {
               return false;
            }

            CFHGuiSounds.click();
            this.focused.activate();
            return true;
         case 258:
            this.focusByKey = true;
            return this.focusNext(shift ? -1 : 1);
         case 262:
         case 264:
            this.focusByKey = true;
            if (!this.focusNext(1)) {
               this.focusNext(1);
            }

            return true;
         case 263:
         case 265:
            this.focusByKey = true;
            if (!this.focusNext(-1)) {
               this.focusNext(-1);
            }

            return true;
         default:
            return false;
      }
   }

   public void render(GuiGraphics g, int x, int y, int w, int h, double mouseX, double mouseY, float partialTick) {
      if (!this.closed && w > 0 && h > 0) {
         long t0 = System.nanoTime();
         Minecraft mc = Minecraft.getInstance();
         long now = Clock.nowMs();
         float delta = this.lastRenderMs == 0L ? 16.0F : Math.min(250.0F, (float)(now - this.lastRenderMs));
         this.lastRenderMs = now;
         this.timeMs += delta;
         this.tickAccMs += delta;

         int ticks;
         for(ticks = 0; this.tickAccMs >= 50.0F && ticks < 4; ++ticks) {
            this.tickAccMs -= 50.0F;
         }

         if (this.tickAccMs > 50.0F) {
            this.tickAccMs = 0.0F;
         }

         double guiScale = mc.getWindow().getGuiScale();
         int pw = Mth.clamp((int)Math.round((double)w * guiScale * (double)this.resolutionScale), 1, 4096);
         int ph = Mth.clamp((int)Math.round((double)h * guiScale * (double)this.resolutionScale), 1, 4096);
         this.ensureTarget(pw, ph, this.finish != null || (long)pw != Math.round((double)w * guiScale) || (long)ph != Math.round((double)h * guiScale));
         this.camera.update(delta, (float)w / (float)h);
         this.ctx.buffers = mc.renderBuffers().bufferSource();
         this.ctx.stage = this;
         this.ctx.camera = this.camera;
         this.ctx.lighting = this.lighting;
         this.ctx.level = this.level;
         this.ctx.light = this.lighting.packedLight();
         this.ctx.partial = this.tickAccMs / 50.0F;
         this.ctx.deltaMs = delta;
         this.ctx.timeMs = this.timeMs;
         this.ctx.nowMs = now;
         ++this.ctx.frame;
         this.ctx.hasMouse = mouseX >= (double)x && mouseX < (double)(x + w) && mouseY >= (double)y && mouseY < (double)(y + h);
         if (this.ctx.hasMouse) {
            this.ctx.mouseNdcX = (float)((mouseX - (double)x) / (double)w * (double)2.0F - (double)1.0F);
            this.ctx.mouseNdcY = (float)((double)1.0F - (mouseY - (double)y) / (double)h * (double)2.0F);
            this.camera.ray(this.ctx.mouseNdcX, this.ctx.mouseNdcY, this.ctx.rayOrigin, this.ctx.rayDir);
         }

         for(int i = 0; i < ticks; ++i) {
            this.tick();
         }

         this.walking = true;

         try {
            for(int i = 0; i < this.nodes.size(); ++i) {
               StageNode n = (StageNode)this.nodes.get(i);
               n.update(this.ctx);
               n.prepare(this.ctx);
            }
         } finally {
            this.walking = false;
         }

         if (!this.leaving.isEmpty()) {
            for(StageNode n : this.leaving) {
               this.remove(n);
            }

            this.leaving.clear();
         }

         this.updateHover(now, x, y, w, h);
         g.flush();
         int framebufferWas = GL11.glGetInteger(36006);
         GL11.glGetIntegerv(2978, this.viewportWas);
         boolean scissorWas = GL11.glIsEnabled(3089);
         if (scissorWas) {
            GL11.glGetIntegerv(3088, this.scissorBox);
            RenderSystem.disableScissor();
         }

         float fogStartWas = RenderSystem.getShaderFogStart();
         float fogEndWas = RenderSystem.getShaderFogEnd();
         FogShape fogShapeWas = RenderSystem.getShaderFogShape();
         System.arraycopy(RenderSystem.getShaderFogColor(), 0, this.savedFogColor, 0, 4);
         System.arraycopy(RenderSystem.getShaderColor(), 0, this.savedShaderColor, 0, 4);
         int lightmapWas = RenderSystem.getShaderTexture(2);
         this.savedProjection.set(RenderSystem.getProjectionMatrix());
         this.savedSorting = RenderSystem.getVertexSorting();
         Matrix4fStack modelView = RenderSystem.getModelViewStack();
         modelView.pushMatrix();
         modelView.identity();
         RenderSystem.applyModelViewMatrix();

         try {
            this.renderPass(mc);
            if (DEBUG && this.frames < 3L) {
               this.debugReadback(mc, pw, ph);
            }

            this.glowReady = false;
            StageFinish look = this.finish;
            if (look != null && this.target != null && StagePost.ready()) {
               if (this.post == null) {
                  this.post = new StagePost();
               }

               this.post.glow(this.target, look);
               this.glowReady = true;
            }
         } catch (Exception e) {
            StageTrouble.once("drawing a scene", "that frame of it is not drawn whole", e);
         } finally {
            VertexBuffer.unbind();
            RenderSystem.setShaderTexture(2, lightmapWas);
            RenderSystem.setShaderFogStart(fogStartWas);
            RenderSystem.setShaderFogEnd(fogEndWas);
            RenderSystem.setShaderFogShape(fogShapeWas);
            RenderSystem.setShaderFogColor(this.savedFogColor[0], this.savedFogColor[1], this.savedFogColor[2], this.savedFogColor[3]);
            RenderSystem.setShaderColor(this.savedShaderColor[0], this.savedShaderColor[1], this.savedShaderColor[2], this.savedShaderColor[3]);
            RenderSystem.setProjectionMatrix(this.savedProjection, this.savedSorting != null ? this.savedSorting : VertexSorting.ORTHOGRAPHIC_Z);
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            Lighting.setupFor3DItems();
            this.restoreDispatchers(mc);
            GlStateManager._glBindFramebuffer(36160, framebufferWas);
            RenderSystem.viewport(this.viewportWas[0], this.viewportWas[1], this.viewportWas[2], this.viewportWas[3]);
            if (scissorWas) {
               RenderSystem.enableScissor(this.scissorBox[0], this.scissorBox[1], this.scissorBox[2], this.scissorBox[3]);
            }

            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(515);
            RenderSystem.depthMask(true);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
         }

         this.blit(g, x, y, w, h);
         this.lastFrameNanos = System.nanoTime() - t0;
         float ms = (float)this.lastFrameNanos / 1000000.0F;
         if (this.frames < 3L) {
            this.frameMsAvg = ms;
         } else {
            this.frameMsAvg = this.frameMsAvg * 0.9F + ms * 0.1F;
         }

         ++this.frames;
      }
   }

   private void renderPass(Minecraft mc) {
      TextureTarget fbo = this.target;
      if (fbo != null) {
         float bgA = this.background == 0 ? 0.0F : (float)(this.background >>> 24 & 255) / 255.0F;
         fbo.setClearColor((float)(this.background >> 16 & 255) / 255.0F * bgA, (float)(this.background >> 8 & 255) / 255.0F * bgA, (float)(this.background & 255) / 255.0F * bgA, bgA);
         fbo.clear(Minecraft.ON_OSX);
         fbo.bindWrite(true);
         if (!depthLogged) {
            depthLogged = true;
            CFHGui.LOGGER.info("[CFHGui] stage: depth buffer of {} bits, camera near {} far {}", new Object[]{GL30.glGetFramebufferAttachmentParameteri(36160, 36096, 33302), this.camera.near(), this.camera.far()});
         }

         RenderSystem.setProjectionMatrix(this.camera.projection(), VertexSorting.DISTANCE_TO_ORIGIN);
         RenderSystem.enableDepthTest();
         RenderSystem.depthFunc(515);
         RenderSystem.depthMask(true);
         RenderSystem.enableBlend();
         StageBlend.over();
         RenderSystem.enableCull();
         float b = this.lighting.brightness();
         RenderSystem.setShaderColor(b, b, b, 1.0F);
         if (this.fog) {
            RenderSystem.setShaderFogStart(this.fogStart);
            RenderSystem.setShaderFogEnd(this.fogEnd);
            RenderSystem.setShaderFogColor((float)(this.fogColor >> 16 & 255) / 255.0F, (float)(this.fogColor >> 8 & 255) / 255.0F, (float)(this.fogColor & 255) / 255.0F, 1.0F);
            RenderSystem.setShaderFogShape(FogShape.SPHERE);
         } else {
            RenderSystem.setShaderFogStart(Float.MAX_VALUE);
            RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
            RenderSystem.setShaderFogColor(0.0F, 0.0F, 0.0F, 0.0F);
         }

         if (this.gradient) {
            this.drawGradient();
         }

         mc.gameRenderer.lightTexture().turnOnLightLayer();
         this.lighting.applyShaderLights(this.camera.view());
         this.ctx.soft = this.soft && StageSoft.ready();
         if (this.ctx.soft) {
            StageSoft.light(this.lighting, this.camera.view(), this.timeMs / 1000.0F, Theme.current().motion() > 0.0F);
            StageSoft.stylize(this.stylize);
         }

         try {
            this.prepareDispatchers(mc);
         } catch (RuntimeException e) {
            StageTrouble.once("readying a scene's entity renderers", "the scene is drawn without what needs them", e);
         }

         this.ctx.pose.setIdentity();
         this.ctx.pose.mulPose(this.camera.view());
         if (this.failingResources != StageResources.generation()) {
            this.failing.clear();
            this.failingResources = StageResources.generation();
         }

         for(StageNode n : this.nodes) {
            if (n.visible() && !this.failing.contains(n)) {
               int tint = n.tint();
               if (!(n.alpha() < 1.0F) && tint == -1) {
                  this.renderNode(n, false);
               } else {
                  this.ctx.buffers.endBatch();
                  shaderColor(b, tint, n.alpha());

                  try {
                     this.renderNode(n, false);
                  } finally {
                     this.ctx.buffers.endBatch();
                     RenderSystem.setShaderColor(b, b, b, 1.0F);
                  }
               }
            }
         }

         this.ctx.buffers.endBatch();

         for(StageNode n : this.nodes) {
            if (n.visible() && n.hasTranslucentPass() && !this.failing.contains(n)) {
               boolean own = n.alpha() < 1.0F || n.tint() != -1;
               if (own) {
                  shaderColor(b, n.tint(), n.alpha());
               }

               this.renderNode(n, true);
               if (own) {
                  RenderSystem.setShaderColor(b, b, b, 1.0F);
               }
            }
         }

         this.ctx.buffers.endBatch();
         RenderSystem.depthMask(true);
         this.drawFocusOutline();
      }
   }

   private void renderNode(StageNode n, boolean translucent) {
      PoseStack.Pose mark = this.ctx.pose.last();

      try {
         if (translucent) {
            n.renderTranslucent(this.ctx);
         } else {
            n.render(this.ctx);
         }
      } catch (RuntimeException e) {
         this.failing.add(n);

         while(this.ctx.pose.last() != mark && !this.ctx.pose.clear()) {
            this.ctx.pose.popPose();
         }

         StageTrouble.once("a scene's " + n.name().getString() + " (" + n.getClass().getSimpleName() + ")", "left out of it", e);
      }

   }

   private static void shaderColor(float brightness, int tint, float alpha) {
      RenderSystem.setShaderColor(brightness * (float)(tint >> 16 & 255) / 255.0F, brightness * (float)(tint >> 8 & 255) / 255.0F, brightness * (float)(tint & 255) / 255.0F, alpha);
   }

   private void drawGradient() {
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.disableCull();
      RenderSystem.setProjectionMatrix(this.identity, VertexSorting.ORTHOGRAPHIC_Z);
      BufferBuilder bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bb.addVertex(-1.0F, 1.0F, 0.0F).setColor(this.gradientTop);
      bb.addVertex(-1.0F, -1.0F, 0.0F).setColor(this.gradientBottom);
      bb.addVertex(1.0F, -1.0F, 0.0F).setColor(this.gradientBottom);
      bb.addVertex(1.0F, 1.0F, 0.0F).setColor(this.gradientTop);
      BufferUploader.drawWithShader(bb.buildOrThrow());
      RenderSystem.setProjectionMatrix(this.camera.projection(), VertexSorting.DISTANCE_TO_ORIGIN);
      RenderSystem.enableCull();
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
   }

   private void drawFocusOutline() {
      StageNode f = this.focused;
      if (f != null && this.focusVisible && this.focusByKey && this.showFocusOutline && f.visible()) {
         float a = f.focus() * (0.75F + 0.25F * Mth.sin(this.timeMs / 1000.0F * 4.0F));
         if (!(a <= 0.01F)) {
            int color = this.outlineColor != 0 ? this.outlineColor : Theme.current().accent();
            float r = (float)(color >> 16 & 255) / 255.0F;
            float gr = (float)(color >> 8 & 255) / 255.0F;
            float bl = (float)(color & 255) / 255.0F;
            VertexConsumer lines = this.ctx.buffers.getBuffer(RenderType.lines());
            this.ctx.pose.pushPose();
            this.ctx.pose.mulPose(f.model);
            float pad = 0.04F + 0.02F * (1.0F - f.focus());
            LevelRenderer.renderLineBox(this.ctx.pose, lines, (double)(f.boundsMin().x - pad), (double)(f.boundsMin().y - pad), (double)(f.boundsMin().z - pad), (double)(f.boundsMax().x + pad), (double)(f.boundsMax().y + pad), (double)(f.boundsMax().z + pad), r, gr, bl, a);
            this.ctx.pose.popPose();
            this.ctx.buffers.endBatch(RenderType.lines());
         }
      }
   }

   private void blit(GuiGraphics g, int x, int y, int w, int h) {
      TextureTarget fbo = this.target;
      if (fbo != null) {
         StageFinish look = this.finish;
         ShaderInstance shader = StagePost.finishShader();
         if (this.glowReady && look != null && this.post != null && shader != null) {
            shader.safeGetUniform("Bloom").set(look.bloom);
            shader.safeGetUniform("Vignette").set(look.vignette);
            shader.safeGetUniform("Contrast").set(look.contrast);
            shader.safeGetUniform("Saturation").set(look.saturation);
            shader.safeGetUniform("Warmth").set(look.warmth);
            shader.safeGetUniform("Shadows").set(look.shadows[0], look.shadows[1], look.shadows[2]);
            shader.safeGetUniform("Highlights").set(look.highlights[0], look.highlights[1], look.highlights[2]);
            shader.safeGetUniform("Grain").set(look.grain);
            shader.safeGetUniform("Alpha").set(this.alpha);
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, fbo.getColorTextureId());
            RenderSystem.setShaderTexture(1, this.post.glowTexture());
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
            Matrix4f pose = g.pose().last().pose();
            BufferBuilder quad = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            quad.addVertex(pose, (float)x, (float)y, 0.0F).setUv(0.0F, 1.0F);
            quad.addVertex(pose, (float)x, (float)(y + h), 0.0F).setUv(0.0F, 0.0F);
            quad.addVertex(pose, (float)(x + w), (float)(y + h), 0.0F).setUv(1.0F, 0.0F);
            quad.addVertex(pose, (float)(x + w), (float)y, 0.0F).setUv(1.0F, 1.0F);
            BufferUploader.drawWithShader(quad.buildOrThrow());
            RenderSystem.defaultBlendFunc();
         } else {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, fbo.getColorTextureId());
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
            Matrix4f m = g.pose().last().pose();
            int a = Math.round(this.alpha * 255.0F);
            BufferBuilder bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            bb.addVertex(m, (float)x, (float)y, 0.0F).setUv(0.0F, 1.0F).setColor(a, a, a, a);
            bb.addVertex(m, (float)x, (float)(y + h), 0.0F).setUv(0.0F, 0.0F).setColor(a, a, a, a);
            bb.addVertex(m, (float)(x + w), (float)(y + h), 0.0F).setUv(1.0F, 0.0F).setColor(a, a, a, a);
            bb.addVertex(m, (float)(x + w), (float)y, 0.0F).setUv(1.0F, 1.0F).setColor(a, a, a, a);
            BufferUploader.drawWithShader(bb.buildOrThrow());
            RenderSystem.defaultBlendFunc();
         }
      }
   }

   private void ensureTarget(int w, int h, boolean smooth) {
      if (this.target == null) {
         this.target = new TextureTarget(w, h, true, Minecraft.ON_OSX);
         this.targetW = w;
         this.targetH = h;
         this.targetLinear = !smooth;
      } else if (this.targetW != w || this.targetH != h) {
         this.target.resize(w, h, Minecraft.ON_OSX);
         this.targetW = w;
         this.targetH = h;
         this.targetLinear = !smooth;
      }

      if (this.targetLinear != smooth) {
         this.target.setFilterMode(smooth ? 9729 : 9728);
         this.targetLinear = smooth;
      }

   }

   private void prepareDispatchers(Minecraft mc) {
      if (this.farCamera == null) {
         this.farCamera = new Camera();
         this.cameraAnchor = this.level.spawn(EntityType.MARKER, (double)0.0F, (double)-10000.0F, (double)0.0F, 0.0F);
         if (this.cameraAnchor != null) {
            CameraAccessor at = (CameraAccessor)this.farCamera;
            at.cfhgui$setInitialized(true);
            at.cfhgui$setLevel(this.level);
            at.cfhgui$setEntity(this.cameraAnchor);
            at.cfhgui$setPartialTickTime(1.0F);
            at.cfhgui$setRotation(this.cameraAnchor.getViewYRot(1.0F), this.cameraAnchor.getViewXRot(1.0F));
            at.cfhgui$setPosition(this.cameraAnchor.position());
         }
      }

      EntityRenderDispatcher erd = mc.getEntityRenderDispatcher();
      erd.prepare(this.level, this.farCamera, (Entity)null);
      erd.setRenderShadow(false);
      BlockEntityRenderDispatcher berd = mc.getBlockEntityRenderDispatcher();
      berd.prepare(this.level, this.farCamera, (HitResult)null);
   }

   private void restoreDispatchers(Minecraft mc) {
      EntityRenderDispatcher erd = mc.getEntityRenderDispatcher();
      erd.setRenderShadow(true);
      if (mc.level != null) {
         erd.prepare(mc.level, mc.gameRenderer.getMainCamera(), mc.crosshairPickEntity);
         mc.getBlockEntityRenderDispatcher().prepare(mc.level, mc.gameRenderer.getMainCamera(), mc.hitResult);
      }

   }

   private void tick() {
      for(StageNode n : this.nodes) {
         try {
            n.tick(this.ctx);
         } catch (RuntimeException e) {
            StageTrouble.once("a scene's " + n.name().getString() + " (" + n.getClass().getSimpleName() + ") as it moves", "the scene goes on without that step", e);
         }
      }

   }

   private void updateHover(long now, int x, int y, int w, int h) {
      StageNode best = null;
      double bestT = Double.POSITIVE_INFINITY;
      if (this.ctx.hasMouse) {
         for(StageNode n : this.nodes) {
            if (n.pickable() && n.visible()) {
               double t = n.pick(this.ctx.rayOrigin, this.ctx.rayDir);
               if (t < bestT) {
                  bestT = t;
                  best = n;
               }
            }
         }
      }

      if (best != this.hovered) {
         if (this.hovered != null) {
            this.hovered.setHovered(false);
         }

         this.hovered = best;
         this.hoverSinceMs = now;
         if (this.hovered != null) {
            this.hovered.setHovered(true);
         }
      }

      if (this.hovered != null && this.hovered.tooltip() != null && now - this.hoverSinceMs > 350L) {
         CFHGuiTooltips.requestWithin(List.of(this.hovered.tooltip()), x, y, w, h);
      }

   }

   public void close() {
      if (!this.closed) {
         this.closed = true;

         for(StageNode n : this.nodes) {
            try {
               n.dispose();
            } catch (Exception var6) {
            }
         }

         this.nodes.clear();
         this.hovered = this.pressed = this.focused = null;
         if (this.target != null) {
            this.target.destroyBuffers();
            this.target = null;
         }

         if (this.post != null) {
            this.post.close();
            this.post = null;
         }

         synchronized(LIVE) {
            LIVE.remove(this);
         }
      }
   }

   public static void onScreenOpened(@Nullable Screen screen) {
      List<Stage> toClose = new ArrayList();
      synchronized(LIVE) {
         for(Stage s : LIVE) {
            if (s.owner != null && s.owner != screen) {
               toClose.add(s);
            }
         }
      }

      for(Stage s : toClose) {
         s.close();
      }

   }

   public static int liveCount() {
      synchronized(LIVE) {
         return LIVE.size();
      }
   }

   public static int withAlpha(int argb, float alpha) {
      return Colors.withAlpha(argb, Math.round(alpha * 255.0F));
   }
}
