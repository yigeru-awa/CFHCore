package com.onehumanawa.cfhcore.content.gui.core.stage.node;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageAnimations;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageResources;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageSoft;
import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.StageModels;
import com.onehumanawa.cfhcore.content.gui.core.stage.platform.StageBlockRenderer;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ModelNode extends StageNode {
   private final ResourceLocation modelId;
   private final @Nullable BlockState state;
   private @Nullable ResourceLocation fallbackId;
   private @Nullable BakedModel model;
   private boolean standingIn;
   private int resources;
   private long lookedAt;
   private RenderType layer;
   private float spinDegPerSec;
   private float spin;
   private float spinBoost;
   private boolean gameModel;
   private float glow;
   private @Nullable Vector3f lamp;
   private float lampAway;
   private float lampTowards;
   private final Vector3f faceNormal;
   private int copies;
   private float stepX;
   private float stepY;
   private float stepZ;
   private final Set<TextureAtlasSprite> animated;
   private @Nullable ResourceLocation scrollFrom;
   private @Nullable ResourceLocation scrollTo;
   private @Nullable TextureAtlasSprite fromSprite;
   private @Nullable TextureAtlasSprite toSprite;
   private float scrollSpeed;
   private float scrollPhase;
   private final RandomSource random;
   private final Quaternionf spinRotation;

   public ModelNode(ResourceLocation modelId) {
      this(modelId, (BlockState)null);
   }

   private ModelNode(ResourceLocation modelId, @Nullable BlockState state) {
      this.resources = -1;
      this.layer = RenderType.cutoutMipped();
      this.spinBoost = 1.0F;
      this.faceNormal = new Vector3f();
      this.copies = 1;
      this.animated = new HashSet();
      this.random = RandomSource.create(7L);
      this.spinRotation = new Quaternionf();
      this.modelId = modelId;
      this.state = state;
      this.named(modelId.getPath());
   }

   public static ModelNode of(BlockState state) {
      return new ModelNode(BuiltInRegistries.BLOCK.getKey(state.getBlock()), state);
   }

   public ModelNode fallback(@Nullable ResourceLocation model) {
      this.fallbackId = model;
      this.resources = -1;
      return this;
   }

   public ModelNode gameModel(boolean on) {
      this.gameModel = on;
      this.resources = -1;
      return this;
   }

   public ModelNode glow(float amount) {
      this.glow = Math.max(0.0F, Math.min(1.0F, amount));
      return this;
   }

   public ModelNode lamp(@Nullable Vector3f towards, float shaded, float lit) {
      this.lamp = towards == null ? null : (new Vector3f(towards)).normalize();
      this.lampAway = Math.max(0.0F, Math.min(1.0F, shaded));
      this.lampTowards = Math.max(0.0F, Math.min(1.0F, lit));
      return this;
   }

   public ModelNode layer(RenderType type) {
      this.layer = type;
      return this;
   }

   public ModelNode spin(float degPerSec) {
      this.spinDegPerSec = degPerSec;
      return this;
   }

   public ModelNode boostSpin(float factor) {
      this.spinBoost = factor;
      return this;
   }

   public float spinAngle() {
      return this.spin;
   }

   public ModelNode spinAngle(float degrees) {
      this.spin = degrees % 360.0F;
      return this;
   }

   public ModelNode repeat(int count, float x, float y, float z) {
      this.copies = Math.max(1, count);
      this.stepX = x;
      this.stepY = y;
      this.stepZ = z;
      return this;
   }

   public void update(StageRenderContext ctx) {
      if (Theme.current().motion() > 0.0F) {
         this.spin = (this.spin + this.spinDegPerSec * this.spinBoost * ctx.deltaMs / 1000.0F) % 360.0F;
         if (this.spinBoost != 1.0F) {
            this.spinBoost += (1.0F - this.spinBoost) * (1.0F - (float)Math.exp((double)(-ctx.deltaMs / 400.0F)));
         }
      }

   }

   public ModelNode scroll(ResourceLocation from, ResourceLocation to, float speed, float phase) {
      this.scrollFrom = from;
      this.scrollTo = to;
      this.scrollSpeed = speed;
      this.scrollPhase = phase;
      this.resources = -1;
      return this;
   }

   protected void draw(StageRenderContext ctx) {
      int gen = StageResources.generation();
      if (gen != this.resources || (this.model == null || this.standingIn) && ctx.nowMs - this.lookedAt > 1000L) {
         boolean stoodIn = this.standingIn && gen == this.resources;
         this.model = this.state != null ? resolve(this.state) : resolve(this.modelId, this.gameModel);
         this.standingIn = false;
         if (this.model == null && this.fallbackId != null) {
            this.model = resolve(this.fallbackId, this.gameModel);
            this.standingIn = this.model != null;
         }

         if (this.standingIn && !stoodIn) {
            CFHGui.LOGGER.info("[CFHGui] stage: model {} is not there to be drawn; {} stands in for it", this.modelId, this.fallbackId);
         } else if (stoodIn && !this.standingIn && this.model != null) {
            CFHGui.LOGGER.info("[CFHGui] stage: model {} is there now; its stand-in goes", this.modelId);
         }

         this.resources = gen;
         this.lookedAt = ctx.nowMs;
         this.animated.clear();
         if (this.model != null && StageAnimations.needed()) {
            StageAnimations.collect(this.model, this.state, this.animated);
         }

         this.fromSprite = this.scrollFrom != null ? atlasSprite(this.scrollFrom) : null;
         this.toSprite = this.scrollTo != null ? atlasSprite(this.scrollTo) : null;
      }

      if (this.model != null) {
         StageAnimations.keep(this.animated);
         float slide = this.fromSprite != null && this.toSprite != null ? Mth.positiveModulo(ctx.seconds() * this.scrollSpeed * (Theme.current().motion() > 0.0F ? 1.0F : 0.0F) + this.scrollPhase, 1.0F) : -1.0F;
         ctx.pose.pushPose();
         if (this.spin != 0.0F) {
            ctx.pose.mulPose(this.spinRotation.rotationY((float)Math.toRadians((double)this.spin)));
         }

         ctx.pose.translate(-0.5F, 0.0F, -0.5F);
         BufferBuilder soft = ctx.soft ? StageSoft.begin(StageSoft.BLOCK) : null;
         VertexConsumer vc = (VertexConsumer)(soft != null ? soft : ctx.buffers.getBuffer(this.alpha() < 1.0F ? RenderType.translucentMovingBlock() : this.layer));

         for(int i = 0; i < this.copies; ++i) {
            if (i > 0) {
               ctx.pose.translate(this.stepX, this.stepY, this.stepZ);
            }

            for(Direction d : Direction.values()) {
               this.random.setSeed(42L);
               this.emit(ctx, vc, this.model.getQuads(this.state, d, this.random), soft != null, slide);
            }

            this.random.setSeed(42L);
            this.emit(ctx, vc, this.model.getQuads(this.state, (Direction)null, this.random), soft != null, slide);
         }

         if (soft != null) {
            StageSoft.end(soft, StageSoft.BLOCK, TextureAtlas.LOCATION_BLOCKS, this.alpha(), true, this.lamp != null ? 1.0F : this.glow);
         }

         ctx.pose.popPose();
      }
   }

   private static @Nullable TextureAtlasSprite atlasSprite(ResourceLocation id) {
      TextureAtlasSprite s = (TextureAtlasSprite)Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(id);
      return s.contents().name().equals(id) ? s : null;
   }

   public static boolean available(ResourceLocation model, boolean gameModel) {
      return resolve(model, gameModel) != null;
   }

   public static boolean available(BlockState state) {
      return resolve(state) != null;
   }

   private static @Nullable BakedModel resolve(ResourceLocation id, boolean gameModel) {
      if (gameModel) {
         BakedModel baked = StageBlockRenderer.get().extraModel(Minecraft.getInstance().getModelManager(), id);
         if (baked != null && !empty(baked, (BlockState)null)) {
            return baked;
         }
      }

      BakedModel own = StageModels.get(id);
      return own != null && !empty(own, (BlockState)null) ? own : null;
   }

   private static @Nullable BakedModel resolve(BlockState state) {
      if (state.isAir()) {
         return null;
      } else {
         BakedModel baked = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
         return baked != null && baked != Minecraft.getInstance().getModelManager().getMissingModel() && !empty(baked, state) ? baked : null;
      }
   }

   private static boolean empty(BakedModel m, @Nullable BlockState state) {
      RandomSource rnd = RandomSource.create(42L);
      if (!m.getQuads(state, (Direction)null, rnd).isEmpty()) {
         return false;
      } else {
         for(Direction d : Direction.values()) {
            rnd.setSeed(42L);
            if (!m.getQuads(state, d, rnd).isEmpty()) {
               return false;
            }
         }

         return true;
      }
   }

   private void emit(StageRenderContext ctx, VertexConsumer vc, List<BakedQuad> quads, boolean soft, float slide) {
      for(BakedQuad q : quads) {
         float s = soft ? 1.0F : ctx.lighting.shade(q.getDirection(), q.isShade());
         if (slide >= 0.0F && q.getSprite() == this.fromSprite) {
            this.emitSliding(ctx, vc, q, s, slide);
         } else {
            vc.putBulkData(ctx.pose.last(), q, s, s, s, 1.0F, soft && this.lamp != null ? this.lampLight(q) : ctx.light, OverlayTexture.NO_OVERLAY);
         }
      }

   }

   private int lampLight(BakedQuad q) {
      int[] v = q.getVertices();
      int stride = v.length / 4;
      float x0 = Float.intBitsToFloat(v[0]);
      float y0 = Float.intBitsToFloat(v[1]);
      float z0 = Float.intBitsToFloat(v[2]);
      float ax = Float.intBitsToFloat(v[stride]) - x0;
      float ay = Float.intBitsToFloat(v[stride + 1]) - y0;
      float az = Float.intBitsToFloat(v[stride + 2]) - z0;
      float bx = Float.intBitsToFloat(v[stride * 2]) - x0;
      float by = Float.intBitsToFloat(v[stride * 2 + 1]) - y0;
      float bz = Float.intBitsToFloat(v[stride * 2 + 2]) - z0;
      this.faceNormal.set(ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx);
      if (this.spin != 0.0F) {
         this.faceNormal.rotate(this.spinRotation);
      }

      super.model.transformDirection(this.faceNormal);
      float length = this.faceNormal.length();
      float facing = length > 1.0E-6F ? Mth.clamp((this.faceNormal.dot(this.lamp) / length + 0.2F) / 1.1F, 0.0F, 1.0F) : 0.5F;
      return Math.round(Mth.lerp(facing, this.lampAway, this.lampTowards) * 240.0F) | 15728640;
   }

   private void emitSliding(StageRenderContext ctx, VertexConsumer vc, BakedQuad q, float s, float slide) {
      TextureAtlasSprite from = this.fromSprite;
      TextureAtlasSprite to = this.toSprite;
      int[] v = q.getVertices();
      int stride = v.length / 4;
      PoseStack.Pose pose = ctx.pose.last();
      Vec3i n = q.getDirection().getNormal();
      float fu = from.getU1() - from.getU0();
      float fv = from.getV1() - from.getV0();

      for(int i = 0; i < 4; ++i) {
         int o = i * stride;
         float u = (Float.intBitsToFloat(v[o + 4]) - from.getU0()) / fu;
         float t = (Float.intBitsToFloat(v[o + 5]) - from.getV0()) / fv;
         vc.addVertex(pose, Float.intBitsToFloat(v[o]), Float.intBitsToFloat(v[o + 1]), Float.intBitsToFloat(v[o + 2])).setColor(s, s, s, 1.0F).setUv(Mth.lerp(u, to.getU0(), to.getU1()), Mth.lerp((t + slide) * 0.5F, to.getV0(), to.getV1())).setOverlay(OverlayTexture.NO_OVERLAY).setLight(ctx.light).setNormal(pose, (float)n.getX(), (float)n.getY(), (float)n.getZ());
      }

   }
}
