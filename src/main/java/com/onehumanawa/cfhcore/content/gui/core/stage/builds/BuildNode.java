package com.onehumanawa.cfhcore.content.gui.core.stage.builds;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageAnimations;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageBlockEntities;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageLevel;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageSoft;
import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.StageLayers;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;
import org.joml.FrustumIntersection;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BuildNode extends StageNode {
   private static final Set<BlockEntityType<?>> SKIPPED;
   private static final int SHOW_MS = 700;
   private static final long WAIT_MS = 9000L;
   private static final float LAND_FADE_FROM = 0.78F;
   private static final float LAND_FADE_ROUND = 2.6F;
   private final StageLevel level;
   private final BuildSpec spec;
   private final BuildCamera sceneCamera;
   private @Nullable BuildCamera camera;
   private @Nullable CompletableFuture<BuildVolume> volume;
   private boolean unreadable;
   private final List<Part> parts = new ArrayList();
   private final Anim shown;
   private boolean showing;
   private long createdMs;
   private @Nullable BlockPos origin;
   private final List<BlockEntity> entities;
   private final float[] received;
   private final List<VertexBuffer> batch;
   private boolean released;
   private @Nullable Part buildPart;
   private final Matrix4f placement;
   private @Nullable CompletableFuture<List<BuildVolume.Entity>> entityPlan;
   private static final double TURNING_SEEN = (double)48.0F;
   private final List<BlockEntity> still;
   private @Nullable BakedEntities baked;

   public BuildNode(StageLevel level, BuildSpec spec, BuildCamera camera) {
      this.shown = new Anim(0.0F, 700, Ease.OUT_CUBIC);
      this.createdMs = -1L;
      this.entities = new ArrayList();
      this.batch = new ArrayList();
      this.placement = new Matrix4f();
      this.still = new ArrayList();
      this.level = level;
      this.spec = spec;
      this.sceneCamera = camera;
      float s = spec.lightStrength();
      int c = spec.lightColor();
      this.received = new float[]{(float)(c >> 16 & 255) / 255.0F * s, (float)(c >> 8 & 255) / 255.0F * s, (float)(c & 255) / 255.0F * s};
      this.at(spec.at().x - 0.5F, spec.at().y, spec.at().z - 0.5F);
      this.pickable(false);
      this.hoverFeel(0.0F, 1.0F);
      this.named("build:" + spec.file());
      this.bounds(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
   }

   public BuildSpec spec() {
      return this.spec;
   }

   public boolean failed() {
      return this.unreadable || this.buildPart != null && this.buildPart.meshes != null && this.buildPart.meshes.failed();
   }

   public boolean showing() {
      return this.showing;
   }

   private void begin(StageRenderContext ctx) {
      if (!this.unreadable) {
         if (this.volume == null) {
            this.volume = BuildCache.volumeOf(this.spec);
            this.createdMs = ctx.nowMs;
         }

         if (this.buildPart == null) {
            if (!this.volume.isDone()) {
               return;
            }

            BuildVolume v;
            try {
               v = (BuildVolume)this.volume.join();
            } catch (RuntimeException var7) {
               this.unreadable = true;
               return;
            }

            Vector3f corner = new Vector3f((float)v.originX() - 0.5F, (float)v.originY(), (float)v.originZ() - 0.5F);
            this.at(corner.x, corner.y, corner.z);
            this.yaw(this.spec.yaw());
            this.placement.translation(corner).rotateY((float)Math.toRadians((double)this.spec.yaw()));
            this.camera = this.sceneCamera.moved(this.placement);
            Part p = new Part(this.spec.file(), (CompletableFuture)null, -1, false);
            p.meshes = BuildCache.acquire(this.spec, ctx.soft, this.camera);
            this.buildPart = p;
            this.parts.add(p);
            if (this.spec.land() != null) {
               int rings = BuildLand.rings(this.spec.land(), v.sizeX(), v.sizeZ()).length;

               for(int k = 0; k < rings; ++k) {
                  List var10000 = this.parts;
                  String var10004 = this.spec.file();
                  var10000.add(new Part(var10004 + " land " + k, BuildCache.landOf(this.spec, k), k, k == rings - 1));
               }
            }
         }

         for(Part p : this.parts) {
            if (p.meshes == null && p.land != null && p.land.isDone()) {
               BuildLand.Placed placed;
               try {
                  placed = (BuildLand.Placed)p.land.join();
               } catch (RuntimeException e) {
                  CFHGui.LOGGER.warn("[CFHGui] build {}: its land could not be made: {}", this.spec.file(), e.getCause() != null ? e.getCause().toString() : e.toString());
                  this.parts.remove(p);
                  return;
               }

               int cell = placed.ring().cell();
               p.scale = (float)cell;
               p.local.identity().translation((float)placed.ring().x0(), placed.floor(), (float)placed.ring().z0()).scale((float)cell);
               p.meshes = BuildCache.acquireLand(this.spec, p.ring, ctx.soft, this.sceneCamera.moved((new Matrix4f(this.placement)).mul(p.local)), p.outermost);
            }
         }

      }
   }

   public void update(StageRenderContext ctx) {
      if (!this.released && !this.parts.isEmpty()) {
         this.step(ctx);
      }
   }

   private void step(StageRenderContext ctx) {
      boolean ready = true;

      for(Part p : this.parts) {
         if (p.meshes == null) {
            ready = false;
         } else {
            p.meshes.pump();
            if (!p.meshes.restReady()) {
               ready = false;
            }
         }
      }

      BuildVolume v = this.buildPart != null && this.buildPart.meshes != null ? this.buildPart.meshes.loaded() : null;
      if (v != null && this.origin == null) {
         this.placeEntities(v);
      }

      if (!this.showing && v != null && (ready || ctx.nowMs - this.createdMs > 9000L)) {
         this.showing = true;
         if (Theme.current().motion() > 0.0F && ctx.nowMs - this.createdMs > 250L) {
            this.shown.set(1.0F);
         } else {
            this.shown.snap(1.0F);
         }
      }

   }

   private void collect(StageRenderContext ctx, Part p) {
      p.visible.clear();
      BuildMeshes m = p.meshes;
      if (m != null) {
         BuildVolume v = m.loaded();
         if (v != null) {
            p.clip.set(ctx.camera.projection()).mul(ctx.pose.last().pose());
            p.frustum.set(p.clip, false);
            p.full.set(this.model).mul(p.local).invert(p.inverse);
            p.inverse.transformPosition(ctx.camera.rendered().position, p.eye);
            int n = 32;
            int px = m.piecesX();
            int py = m.piecesY();
            int pz = m.piecesZ();
            float far = ctx.camera.far() / p.scale;
            Vector3f eye = p.eye;

            for(int i = 0; i < px * py * pz; ++i) {
               if (m.hasBlocks(i)) {
                  int x0 = i % px * 32;
                  int z0 = i / px % pz * 32;
                  int y0 = i / (px * pz) * 32;
                  float x1 = (float)Math.min(v.sizeX(), x0 + 32);
                  float y1 = (float)Math.min(v.sizeY(), y0 + 32);
                  float z1 = (float)Math.min(v.sizeZ(), z0 + 32);
                  if (p.frustum.testAab((float)x0, (float)y0, (float)z0, x1, y1, z1)) {
                     float dx = Math.max(Math.max((float)x0 - eye.x, 0.0F), eye.x - x1);
                     float dy = Math.max(Math.max((float)y0 - eye.y, 0.0F), eye.y - y1);
                     float dz = Math.max(Math.max((float)z0 - eye.z, 0.0F), eye.z - z1);
                     if (!(dx * dx + dy * dy + dz * dz > far * far)) {
                        if (!m.ready(i)) {
                           m.want(i);
                        } else {
                           p.visible.add(i);
                        }
                     }
                  }
               }
            }

            float ex = eye.x;
            float ey = eye.y;
            float ez = eye.z;
            p.visible.sort(Comparator.comparingDouble((ix) -> {
               double cx = ((double)(ix % px) + (double)0.5F) * (double)32.0F - (double)ex;
               double cy = ((double)(ix / (px * pz)) + (double)0.5F) * (double)32.0F - (double)ey;
               double cz = ((double)(ix / px % pz) + (double)0.5F) * (double)32.0F - (double)ez;
               return cx * cx + cy * cy + cz * cz;
            }));
         }
      }
   }

   protected void draw(StageRenderContext ctx) {
      if (!this.released) {
         this.begin(ctx);
         if (this.buildPart != null && this.showing) {
            float a = this.alpha() * this.shown.get();

            for(Part p : this.parts) {
               if (p.meshes != null) {
                  ctx.pose.pushPose();
                  ctx.pose.last().pose().mul(p.local);

                  try {
                     this.collect(ctx, p);
                     StageAnimations.keep(p.meshes.animated());
                     if (!(a <= 0.004F)) {
                        this.footprint(ctx, p, true);

                        try {
                           for(int layer = 0; layer < 3; ++layer) {
                              this.drawLayer(ctx, p, layer, a, false);
                           }
                        } finally {
                           this.footprint(ctx, p, false);
                        }
                     }
                  } finally {
                     ctx.pose.popPose();
                  }
               }
            }

            if (a > 0.004F) {
               this.drawEntities(ctx, a);
            }

         }
      }
   }

   private void footprint(StageRenderContext ctx, Part p, boolean on) {
      if (ctx.soft) {
         BuildVolume v = p.meshes != null ? p.meshes.loaded() : null;
         boolean fade = p.isLand() ? p.outermost : this.spec.fadeRound() > 0.0F;
         if (fade && v != null) {
            if (!on) {
               StageSoft.noFootprint();
            } else {
               float from = p.isLand() ? 0.78F : this.spec.fadeFrom();
               float round = p.isLand() ? 2.6F : this.spec.fadeRound();
               StageSoft.footprint((float)v.sizeX() / 2.0F, (float)v.sizeZ() / 2.0F, (float)v.sizeX() / 2.0F, (float)v.sizeZ() / 2.0F, from, round);
            }
         }
      }
   }

   public boolean hasTranslucentPass() {
      return true;
   }

   protected void drawTranslucent(StageRenderContext ctx) {
      if (this.buildPart != null && this.showing && !this.released) {
         float a = this.alpha() * this.shown.get();
         if (!(a <= 0.004F)) {
            for(int k = this.parts.size() - 1; k >= 0; --k) {
               Part p = (Part)this.parts.get(k);
               if (p.meshes != null && !p.visible.isEmpty()) {
                  ctx.pose.pushPose();
                  ctx.pose.last().pose().mul(p.local);
                  this.footprint(ctx, p, true);

                  try {
                     Collections.reverse(p.visible);
                     this.drawLayer(ctx, p, 3, a, true);
                     Collections.reverse(p.visible);
                  } finally {
                     this.footprint(ctx, p, false);
                     ctx.pose.popPose();
                  }
               }
            }

         }
      }
   }

   private void drawLayer(StageRenderContext ctx, Part p, int layer, float alpha, boolean translucent) {
      BuildMeshes m = p.meshes;
      if (m != null) {
         this.batch.clear();

         for(int i : p.visible) {
            VertexBuffer b = m.buffer(i, layer);
            if (b != null) {
               this.batch.add(b);
            }
         }

         if (!this.batch.isEmpty()) {
            Matrix4f modelView = ctx.pose.last().pose();
            RenderType type = StageLayers.drawnAs((RenderType)BuildMesher.LAYERS.get(layer));
            if (m.soft() && ctx.soft) {
               if (type == RenderType.cutout()) {
                  StageSoft.alphaTest(0.1F, true);
               } else if (type == RenderType.cutoutMipped()) {
                  StageSoft.alphaTest(0.5F, false);
               } else {
                  StageSoft.plainAlpha();
               }

               try {
                  boolean lit = !p.isLand() && m.loaded() != null && m.loaded().lit();
                  StageSoft.drawAll(this.batch, modelView, ctx.camera.projection(), TextureAtlas.LOCATION_BLOCKS, alpha, translucent, p.isLand() ? 1.0F : this.spec.glow(), translucent ? 0.8F : 0.0F, lit ? this.received : null);
               } finally {
                  StageSoft.plainAlpha();
               }

            } else {
               type.setupRenderState();

               try {
                  ShaderInstance shader = RenderSystem.getShader();
                  if (shader != null) {
                     shader.setDefaultUniforms(Mode.QUADS, modelView, ctx.camera.projection(), Minecraft.getInstance().getWindow());
                     if (shader.CHUNK_OFFSET != null) {
                        shader.CHUNK_OFFSET.set(0.0F, 0.0F, 0.0F);
                     }

                     shader.apply();

                     for(VertexBuffer b : this.batch) {
                        b.bind();
                        b.draw();
                     }

                     VertexBuffer.unbind();
                     shader.clear();
                     return;
                  }
               } finally {
                  type.clearRenderState();
               }

            }
         }
      }
   }

   private static List<BuildVolume.Entity> plan(BuildSpec spec, BuildVolume v, BuildCamera camera) {
      Vector3f rest = camera.rest();
      float reach = spec.entityReach();
      float[] eyes = camera.cull().eyes();
      List<BuildVolume.Entity> near = new ArrayList();
      int hidden = 0;

      for(BuildVolume.Entity e : v.entities()) {
         float dx = (float)e.pos().getX() + 0.5F - rest.x;
         float dy = (float)e.pos().getY() + 0.5F - rest.y;
         float dz = (float)e.pos().getZ() + 0.5F - rest.z;
         if (!(dx * dx + dy * dy + dz * dz > reach * reach)) {
            if (eyes.length > 0 && !seen(v, e.pos(), eyes)) {
               ++hidden;
            } else {
               near.add(e);
            }
         }
      }

      near.sort(Comparator.comparingDouble((ex) -> ex.pos().distToCenterSqr((double)rest.x, (double)rest.y, (double)rest.z)));
      CFHGui.LOGGER.debug("[CFHGui] build {}: {} block entities are drawn, {} hidden from the camera are not", new Object[]{spec.file(), near.size(), hidden});
      return (List<BuildVolume.Entity>)(near.size() > spec.maxEntities() ? new ArrayList(near.subList(0, spec.maxEntities())) : near);
   }

   private static boolean seen(BuildVolume v, BlockPos p, float[] eyes) {
      float x = (float)p.getX();
      float y = (float)p.getY();
      float z = (float)p.getZ();

      for(int e = 0; e < eyes.length; e += 3) {
         float ex = eyes[e];
         float ey = eyes[e + 1];
         float ez = eyes[e + 2];
         if (v.clearLine(x + 0.5F, y + 0.5F, z + 0.5F, ex, ey, ez)) {
            return true;
         }

         for(int c = 0; c < 8; ++c) {
            float cx = x + ((c & 1) == 0 ? -0.9F : 1.9F);
            float cy = y + ((c & 2) == 0 ? -0.9F : 1.9F);
            float cz = z + ((c & 4) == 0 ? -0.9F : 1.9F);
            if (v.clearLine(cx, cy, cz, ex, ey, ez)) {
               return true;
            }
         }
      }

      return false;
   }

   private void placeEntities(BuildVolume v) {
      if (this.spec.maxEntities() > 0 && !v.entities().isEmpty() && this.camera != null) {
         if (this.entityPlan == null) {
            BuildCamera cam = this.camera;
            this.entityPlan = CompletableFuture.supplyAsync(() -> plan(this.spec, v, cam), BuildWork.POOL);
         } else if (this.entityPlan.isDone()) {
            this.origin = this.level.allocateRegion();

            List<BuildVolume.Entity> near;
            try {
               near = (List)this.entityPlan.join();
            } catch (RuntimeException var11) {
               return;
            }

            BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
            int placed = 0;

            for(BuildVolume.Entity e : near) {
               if (placed >= this.spec.maxEntities()) {
                  break;
               }

               BlockPos at = this.origin.offset(e.pos());
               CompoundTag nbt = e.nbt();
               if (nbt != null) {
                  nbt = nbt.copy();
                  nbt.remove("profile");
                  nbt.remove("SkullOwner");
               }

               BlockEntity be = BuildEntities.make(at, e.state(), nbt);
               if (be == null && nbt != null) {
                  be = BuildEntities.make(at, e.state(), (CompoundTag)null);
               }

               if (be != null && !SKIPPED.contains(be.getType()) && dispatcher.getRenderer(be) != null) {
                  try {
                     this.level.place(at, e.state(), (CompoundTag)null, false);
                     this.level.setBlockEntity(be);
                  } catch (RuntimeException var12) {
                     this.level.place(at, Blocks.AIR.defaultBlockState(), (CompoundTag)null);
                     continue;
                  }

                  BuildEntities.virtual(be);
                  boolean far = e.pos().distToCenterSqr((double)this.camera.rest().x, (double)this.camera.rest().y, (double)this.camera.rest().z) > (double)2304.0F;
                  (BakedEntities.moves(be, far) ? this.entities : this.still).add(be);
                  ++placed;
               }
            }

            if (placed > 0) {
               CFHGui.LOGGER.debug("[CFHGui] build {}: {} block entities drawn, {} of them still", new Object[]{this.spec.file(), placed, this.still.size()});
            }

         }
      } else {
         this.origin = this.level.allocateRegion();
      }
   }

   private boolean baked(StageRenderContext ctx) {
      if (this.still.isEmpty()) {
         return false;
      } else if (this.baked != null && !this.baked.stale()) {
         return true;
      } else if (ctx.camera.timeline().playing()) {
         return false;
      } else {
         if (this.baked != null) {
            this.baked.close();
         }

         this.baked = BakedEntities.bake(this.still, this.origin, ctx.light, new Matrix3f(ctx.pose.last().normal()), this.level);
         return true;
      }
   }

   private void drawEntities(StageRenderContext ctx, float alpha) {
      if ((!this.entities.isEmpty() || !this.still.isEmpty()) && this.origin != null && !(alpha < 0.999F) && this.buildPart != null) {
         Runnable restore = BuildEntities.cullingAnything();

         try {
            this.drawEntitiesNow(ctx);
         } finally {
            restore.run();
         }

      }
   }

   private void drawEntitiesNow(StageRenderContext ctx) {
      BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
      int tint = this.spec.entityTint();
      float[] was = (float[])RenderSystem.getShaderColor().clone();
      if (tint != -1) {
         ctx.buffers.endBatch();
         RenderSystem.setShaderColor(was[0] * (float)(tint >> 16 & 255) / 255.0F, was[1] * (float)(tint >> 8 & 255) / 255.0F, was[2] * (float)(tint & 255) / 255.0F, was[3]);
      }

      try {
         boolean fromBuffers = this.baked(ctx);
         if (fromBuffers) {
            this.baked.draw(ctx.pose.last().pose(), ctx.camera.projection());
         }

         for(BlockEntity be : fromBuffers ? this.entities : this.allEntities()) {
            if (!StageBlockEntities.leftOut(be)) {
               BlockPos p = be.getBlockPos();
               int x = p.getX() - this.origin.getX();
               int y = p.getY() - this.origin.getY();
               int z = p.getZ() - this.origin.getZ();
               if (this.buildPart.frustum.testAab((float)x, (float)y, (float)z, (float)(x + 1), (float)(y + 1), (float)(z + 1))) {
                  BlockEntityRenderer<BlockEntity> r = dispatcher.getRenderer(be);
                  if (r != null) {
                     ctx.pose.pushPose();
                     ctx.pose.translate((float)x, (float)y, (float)z);

                     try {
                        StageBlockEntities.draw(this.level, be, r, ctx.partial, ctx.pose, ctx.buffers, ctx.light);
                     } finally {
                        ctx.pose.popPose();
                     }
                  }
               }
            }
         }
      } finally {
         if (tint != -1) {
            ctx.buffers.endBatch();
            RenderSystem.setShaderColor(was[0], was[1], was[2], was[3]);
         }

      }

   }

   private List<BlockEntity> allEntities() {
      if (this.still.isEmpty()) {
         return this.entities;
      } else {
         List<BlockEntity> all = new ArrayList(this.entities.size() + this.still.size());
         all.addAll(this.entities);
         all.addAll(this.still);
         return all;
      }
   }

   public void dispose() {
      this.released = true;
      if (this.baked != null) {
         this.baked.close();
         this.baked = null;
      }

      for(Part p : this.parts) {
         if (p.meshes != null) {
            BuildCache.release(p.meshes);
            p.meshes = null;
         }
      }

      this.parts.clear();
      this.buildPart = null;

      for(BlockEntity be : this.allEntities()) {
         this.level.place(be.getBlockPos(), Blocks.AIR.defaultBlockState(), (CompoundTag)null);
      }

      this.entities.clear();
      this.still.clear();
   }

   static {
      SKIPPED = Set.of(BlockEntityType.MOB_SPAWNER, BlockEntityType.TRIAL_SPAWNER, BlockEntityType.VAULT);
   }

   private final class Part {
      final String name;
      final Matrix4f local = new Matrix4f();
      @Nullable BuildMeshes meshes;
      final @Nullable CompletableFuture<BuildLand.Placed> land;
      final int ring;
      final boolean outermost;
      final FrustumIntersection frustum = new FrustumIntersection();
      final Matrix4f clip = new Matrix4f();
      final Matrix4f inverse = new Matrix4f();
      final Matrix4f full = new Matrix4f();
      final Vector3f eye = new Vector3f();
      final List<Integer> visible = new ArrayList();
      float scale = 1.0F;

      Part(final @Nullable String name, final CompletableFuture<BuildLand.Placed> land, final int ring, final boolean outermost) {
         this.name = name;
         this.land = land;
         this.ring = ring;
         this.outermost = outermost;
      }

      boolean isLand() {
         return this.land != null;
      }
   }
}
