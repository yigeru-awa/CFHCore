package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageBlend;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageResources;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageSoft;
import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.QuadMesh;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PivotNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PlayerNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.animation.definitions.BatAnimation;
import net.minecraft.client.model.BatModel;
import net.minecraft.client.model.MinecartModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class PlayBlock extends StageNode {
   static final float TILT = 24.0F;
   private static final float TILT_OPEN = 15.0F;
   private static final float CORNER = -45.0F;
   private static final float SIDE = 0.31543624F;
   private static final float SWAY = 6.0F;
   private static final float SWAY_PACE = 0.6F;
   static final float LID_GROWS = 0.16F;
   static final float LID_RISES = 0.01F;
   static final float TILE_GROWS = 0.09F;
   static final float TILE_RISES = 0.0F;
   private static final int OPEN_MS = 420;
   private static final int CLOSE_MS = 340;
   static final Vector3f LAMP = (new Vector3f(0.36F, 0.78F, 0.51F)).normalize();
   private static final float GLOW_AWAY = 0.3F;
   private static final float GLOW_TOWARDS = 1.0F;
   private static final float HALF = 4.0F;
   private static final ResourceLocation CART = ResourceLocation.withDefaultNamespace("textures/entity/minecart.png");
   private static final ResourceLocation BAT = ResourceLocation.withDefaultNamespace("textures/entity/bat.png");
   private static final Quaternionf FLIP;
   private static final List<float[]> DEV_OPEN;
   private final PlayCave cave = new PlayCave();
   private final PivotNode tilt;
   private final float grows;
   private final float rises;
   private final long madeMs = Clock.nowMs();
   private final Anim opened;
   private float lift;
   private float open;
   private float swayed;
   private @Nullable BlockQuads[] whole;
   private @Nullable BlockQuads[] shell;
   private @Nullable BlockQuads spill;
   private @Nullable BlockQuads inside;
   private @Nullable BlockQuads rails;
   private final float[] glow;
   private PlayCave.@Nullable Sprites sprites;
   private @Nullable TextureAtlasSprite pick;
   private int resources;
   private boolean broken;
   private int drawn;
   private static final int WAITS = 20;
   private @Nullable MinecartModel<Entity> cart;
   private @Nullable BatModel bat;
   private @Nullable PlayerModel<LivingEntity> wide;
   private @Nullable PlayerModel<LivingEntity> slim;
   private @Nullable ModelPart chestBottom;
   private @Nullable ModelPart chestLid;
   private @Nullable ModelPart chestLock;
   private final PlayerNode me;
   private int minerLight;
   private int chestLight;
   private @Nullable BufferBuilder batch;
   private final float[] where;
   private final Vector3f right;
   private final Vector3f up;
   private final Vector3f scratch;
   private static final float MINER_X = 3.6F;
   private static final float MINER_Z = 5.85F;
   private static final float MINER_SIZE = 0.9F;
   private static final float CHEST_X = 2.6F;
   private static final float CHEST_Z = 5.5F;
   private static final float CART_SPEED = 2.6F;
   private static final float CART_START = 1.2F;
   private static final float CART_STRAIGHT = 5.4F;
   private static final float CART_BEND = ((float)Math.PI / 4F);
   private static final float CART_AWAY = 5.5F;
   private static final float STRIKE_X = 3.8799999F;
   private static final float STRIKE_Y = 3.0F;
   private static final float STRIKE_Z = 5.05F;
   private static final float DRIP_EVERY = 1.7F;

   PlayBlock(PivotNode tilt, float grows, float rises) {
      this.opened = new Anim(0.0F, 420, Ease.LINEAR);
      this.glow = new float[6];
      this.resources = -1;
      this.me = PlayerNode.local();
      this.minerLight = -1;
      this.chestLight = -1;
      this.where = new float[4];
      this.right = new Vector3f();
      this.up = new Vector3f();
      this.scratch = new Vector3f();
      this.tilt = tilt;
      this.grows = grows;
      this.rises = rises;
      this.bounds(-4.0F, -4.0F, -4.0F, 4.0F, 4.0F, 4.0F);
      this.pickable(false);
      this.hoverFeel(0.0F, 1.0F);
      this.scale(0.03942953F);
      this.rotate(-45.0F, 0.0F, 0.0F);
      this.named("grass block");
   }

   void pointedAt(float amount) {
      this.lift = Mth.clamp(amount, 0.0F, 1.0F);
   }

   private static List<float[]> devOpen(@Nullable String spec) {
      List<float[]> held = new ArrayList();
      if (spec != null && !spec.isBlank()) {
         for(String entry : spec.split(";")) {
            String[] v = entry.trim().split("@");

            try {
               float open = Float.parseFloat(v[0].trim());
               held.add(new float[]{Mth.clamp(Math.abs(open), 0.0F, 1.0F), v.length > 1 ? Float.parseFloat(v[1].trim()) : Float.NaN, open < 0.0F ? 1.0F : 0.0F});
            } catch (NumberFormatException var8) {
            }
         }

         return List.copyOf(held);
      } else {
         return held;
      }
   }

   public void update(StageRenderContext ctx) {
      float motion = Theme.current().motion();
      float[] held = DEV_OPEN.isEmpty() ? null : (float[])DEV_OPEN.get((int)((Clock.nowMs() - this.madeMs) / 2000L % (long)DEV_OPEN.size()));
      float amount = this.lift;
      if (held != null) {
         amount = held[0] > 0.0F && held[2] == 0.0F ? 1.0F : 0.0F;
         this.opened.snap(held[0]);
      } else {
         boolean now = this.lift > 0.5F;
         this.opened.set(now ? 1.0F : 0.0F, now ? 420 : 340);
      }

      this.open = this.opened.get();
      float turn = -45.0F;
      if (held != null) {
         if (!Float.isNaN(held[1])) {
            turn += held[1];
         }
      } else if (motion > 0.0F) {
         this.swayed += Math.min(ctx.deltaMs, 100.0F) / 1000.0F / motion;
         turn += 6.0F * Mth.sin(this.swayed * 0.6F);
      }

      this.rotate(turn, 0.0F, 0.0F);
      this.scale(0.03942953F * (1.0F + this.grows * amount));
      this.at(0.0F, this.rises * amount, 0.0F);
      this.tilt.rotate(0.0F, Mth.lerp(amount, 24.0F, 15.0F), 0.0F);
   }

   private static float time(StageRenderContext ctx) {
      return Theme.current().motion() > 0.0F ? ctx.seconds() : 2.4F;
   }

   private float gone(PlayCave.Piece p) {
      float mouth = Mth.clamp((this.open - 0.14F * (float)p.face()) / 0.86F, 0.0F, 1.0F);
      return Mth.clamp((mouth - p.delay() * 0.55F) / 0.45F, 0.0F, 1.0F);
   }

   private float lightOut() {
      float t = Mth.clamp((this.open - 0.12F) / 0.6F, 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   private void ensure() {
      int gen = StageResources.generation();
      if (gen != this.resources) {
         this.release();
         this.resources = gen;
         this.broken = false;
      }

      if (!this.broken) {
         try {
            if (this.whole == null) {
               this.whole = this.cave.shell(true);
            }

            ++this.drawn;
            boolean wanted = this.open > 0.0F;
            if (this.shell == null && (wanted || this.drawn > 20)) {
               this.sprites = new PlayCave.Sprites();
               this.pick = QuadMesh.sprite(ResourceLocation.withDefaultNamespace("item/iron_pickaxe"));
               this.shell = this.cave.shell(false);
               this.spill = this.cave.spill();
               if (!wanted) {
                  return;
               }
            }

            if (this.inside == null && (wanted || this.drawn > 21)) {
               this.inside = this.cave.inside();
               this.rails = this.cave.rails();
               this.minerLight = this.cave.lightAt(3.8999999F, 3.2F, 6.15F);
               this.chestLight = this.cave.lightAt(2.8F, 2.8F, 5.7F);
               if (!wanted) {
                  return;
               }
            }

            if (this.cart == null && (wanted || this.drawn > 22)) {
               EntityModelSet models = Minecraft.getInstance().getEntityModels();
               this.cart = new MinecartModel(models.bakeLayer(ModelLayers.MINECART));
               this.bat = new BatModel(models.bakeLayer(ModelLayers.BAT));
               this.wide = new PlayerModel(models.bakeLayer(ModelLayers.PLAYER), false);
               this.slim = new PlayerModel(models.bakeLayer(ModelLayers.PLAYER_SLIM), true);
               ModelPart chest = models.bakeLayer(ModelLayers.CHEST);
               this.chestBottom = chest.getChild("bottom");
               this.chestLid = chest.getChild("lid");
               this.chestLock = chest.getChild("lock");
            }
         } catch (RuntimeException e) {
            this.broken = true;
            CFHGui.LOGGER.error("[CFHGui] title: the Play block could not be built", e);
         }

      }
   }

   private void release() {
      if (this.whole != null) {
         for(BlockQuads side : this.whole) {
            side.close();
         }

         this.whole = null;
      }

      if (this.shell != null) {
         for(BlockQuads side : this.shell) {
            side.close();
         }

         this.shell = null;
      }

      if (this.spill != null) {
         this.spill.close();
         this.spill = null;
      }

      if (this.inside != null) {
         this.inside.close();
         this.inside = null;
      }

      if (this.rails != null) {
         this.rails.close();
         this.rails = null;
      }

      this.sprites = null;
      this.cart = null;
   }

   public void dispose() {
      this.release();
   }

   protected void draw(StageRenderContext ctx) {
      this.ensure();
      if (this.whole != null) {
         float a = this.alpha();

         for(Direction d : Direction.values()) {
            this.scratch.set((float)d.getStepX(), (float)d.getStepY(), (float)d.getStepZ());
            this.model.transformDirection(this.scratch).normalize();
            this.glow[d.ordinal()] = Mth.lerp(Mth.clamp((this.scratch.dot(LAMP) + 0.2F) / 1.1F, 0.0F, 1.0F), 0.3F, 1.0F);
         }

         ctx.pose.pushPose();
         ctx.pose.translate(-4.0F, -4.0F, -4.0F);
         boolean shut = this.open <= 0.0F || this.inside == null || this.shell == null;
         BlockQuads[] sides = shut ? this.whole : this.shell;

         for(int i = 0; i < sides.length; ++i) {
            sides[i].draw(ctx, false, a, this.glow[i]);
         }

         if (!shut) {
            this.inside.draw(ctx, false, a, 1.0F);
            if (this.rails != null) {
               this.rails.draw(ctx, false, a, 1.0F);
            }

            this.pieces(ctx);
            this.living(ctx, time(ctx));
         }

         ctx.pose.popPose();
      }
   }

   private VertexConsumer atlas(StageRenderContext ctx) {
      if (ctx.soft) {
         return this.batch = StageSoft.begin(StageSoft.BLOCK);
      } else {
         this.batch = null;
         return ctx.buffers.getBuffer(RenderType.cutoutMipped());
      }
   }

   private void flush(float glow, boolean cull) {
      if (this.batch != null) {
         StageSoft.end(this.batch, StageSoft.BLOCK, TextureAtlas.LOCATION_BLOCKS, this.alpha(), cull, glow);
      }

      this.batch = null;
   }

   private VertexConsumer skinned(StageRenderContext ctx, ResourceLocation texture) {
      if (ctx.soft) {
         return this.batch = StageSoft.begin(StageSoft.ENTITY);
      } else {
         this.batch = null;
         return ctx.buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
      }
   }

   private void skinned(ResourceLocation texture) {
      if (this.batch != null) {
         StageSoft.end(this.batch, StageSoft.ENTITY, texture, this.alpha(), false, 1.0F);
      }

      this.batch = null;
   }

   private void pieces(StageRenderContext ctx) {
      PlayCave.Sprites s = this.sprites;
      if (s != null) {
         for(int face = 0; face < PlayCave.OUT.length; ++face) {
            Direction out = PlayCave.OUT[face];
            VertexConsumer vc = null;

            for(PlayCave.Piece p : this.cave.pieces) {
               if (p.face() == face) {
                  float g = this.gone(p);
                  if (!(g >= 1.0F)) {
                     if (vc == null) {
                        vc = this.atlas(ctx);
                     }

                     float sink = Math.min(0.9F, p.depth()) * Ease.OUT_CUBIC.apply(g);
                     float shrink = Mth.clamp((g - 0.25F) / 0.75F, 0.0F, 1.0F);
                     float size = 1.0F - shrink * shrink * (3.0F - 2.0F * shrink);
                     PlayCave.place(p, this.where);
                     ctx.pose.pushPose();
                     ctx.pose.translate(this.where[0] - (float)out.getStepX() * sink, this.where[1], this.where[2] - (float)out.getStepZ() * sink);
                     ctx.pose.scale(size, size, size);
                     this.cave.piece(vc, ctx.pose.last(), s, p);
                     ctx.pose.popPose();
                  }
               }
            }

            if (vc != null) {
               this.flush(this.glow[out.ordinal()], true);
            }
         }

      }
   }

   private void living(StageRenderContext ctx, float t) {
      if (this.cart != null && this.bat != null && this.wide != null && this.slim != null && this.chestBottom != null && this.chestLid != null && this.chestLock != null) {
         PoseStack pose = ctx.pose;
         int full = 15728880;
         int none = OverlayTexture.NO_OVERLAY;
         if (rolls(1.2F + t * 2.6F, this.where)) {
            pose.pushPose();
            pose.translate(this.where[0], 2.03F, this.where[1]);
            pose.mulPose(Axis.YP.rotation((float)Math.atan2((double)(-this.where[3]), (double)this.where[2])));
            pose.scale(0.8F, 0.8F, 0.8F);
            pose.translate(0.0F, 0.375F, 0.0F);
            pose.scale(-1.0F, -1.0F, 1.0F);
            VertexConsumer vc = this.skinned(ctx, CART);
            this.cart.root().render(pose, vc, 15728880, none, this.cave.lightAt(this.where[0], 2.7F, this.where[1]));
            this.skinned(CART);
            pose.popPose();
         }

         PlayerSkin skin = this.me.skin();
         PlayerModel<LivingEntity> m = skin.model() == Model.SLIM ? this.slim : this.wide;
         float beat = Mth.positiveModulo(t / 0.95F, 1.0F);
         float down = beat < 0.28F ? beat / 0.28F : 1.0F - Ease.IN_OUT_CUBIC.apply((beat - 0.28F) / 0.72F);
         float stroke = beat < 0.28F ? down * down : down;
         m.young = false;
         m.crouching = false;
         m.setAllVisible(true);
         m.head.resetPose();
         m.body.resetPose();
         m.rightArm.resetPose();
         m.leftArm.resetPose();
         m.rightLeg.resetPose();
         m.leftLeg.resetPose();
         m.head.xRot = 0.16F + 0.08F * stroke;
         m.rightArm.xRot = Mth.lerp(stroke, -2.0F, -0.7F);
         m.rightArm.zRot = 0.1F;
         m.leftArm.xRot = -0.35F + 0.12F * stroke;
         m.leftArm.zRot = -0.06F;
         m.rightLeg.xRot = 0.16F;
         m.leftLeg.xRot = -0.16F;
         m.hat.copyFrom(m.head);
         m.jacket.copyFrom(m.body);
         m.leftSleeve.copyFrom(m.leftArm);
         m.rightSleeve.copyFrom(m.rightArm);
         m.leftPants.copyFrom(m.leftLeg);
         m.rightPants.copyFrom(m.rightLeg);
         pose.pushPose();
         pose.translate(3.6F, 2.0F, 5.85F);
         pose.mulPose(FLIP);
         pose.scale(0.9F, 0.9F, 0.9F);
         pose.mulPose(FLIP);
         pose.scale(-1.0F, -1.0F, 1.0F);
         pose.translate(0.0F, -1.501F, 0.0F);
         VertexConsumer vc = this.skinned(ctx, skin.texture());
         m.renderToBuffer(pose, vc, 15728880, none, this.minerLight);
         this.skinned(skin.texture());
         if (this.pick != null) {
            m.rightArm.translateAndRotate(pose);
            vc = this.atlas(ctx);
            pickaxe(vc, pose.last(), this.pick, this.minerLight);
            this.flush(1.0F, false);
         }

         pose.popPose();
         float round = t * 0.9F;
         float bx = 4.95F + 0.7F * Mth.sin(round);
         float by = 3.5F + 0.2F * Mth.sin(t * 1.9F + 1.0F);
         float bz = 2.8F + 0.3F * Mth.cos(round);
         float heading = (float)Math.atan2((double)(0.7F * Mth.cos(round)), (double)(-0.3F * Mth.sin(round)));
         this.bat.root().getAllParts().forEach(ModelPart::resetPose);
         KeyframeAnimations.animate(this.bat, BatAnimation.BAT_FLYING, (long)(t * 1000.0F), 1.0F, this.scratch);
         pose.pushPose();
         pose.translate(bx, by - 0.4F, bz);
         pose.mulPose(Axis.YP.rotation(heading));
         pose.scale(0.85F, 0.85F, 0.85F);
         pose.mulPose(FLIP);
         pose.scale(-1.0F, -1.0F, 1.0F);
         pose.translate(0.0F, -1.501F, 0.0F);
         vc = this.skinned(ctx, BAT);
         this.bat.root().render(pose, vc, 15728880, none, this.cave.lightAt(bx, by, bz));
         this.skinned(BAT);
         pose.popPose();
         Material material = Sheets.CHEST_LOCATION;
         this.chestLid.xRot = 0.0F;
         this.chestLock.xRot = 0.0F;
         pose.pushPose();
         pose.translate(2.1F, 2.0F, 5.0F);
         if (ctx.soft) {
            this.batch = StageSoft.begin(StageSoft.ENTITY);
            vc = material.sprite().wrap(this.batch);
         } else {
            this.batch = null;
            vc = material.buffer(ctx.buffers, RenderType::entityCutout);
         }

         this.chestLid.render(pose, vc, 15728880, none, this.chestLight);
         this.chestLock.render(pose, vc, 15728880, none, this.chestLight);
         this.chestBottom.render(pose, vc, 15728880, none, this.chestLight);
         this.skinned(material.atlasLocation());
         pose.popPose();
      }
   }

   private static void pickaxe(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite, int argb) {
      float length = 0.8F;
      float grip = 0.2F;
      double lean = Math.toRadians((double)-18.0F);
      float uz = -((float)Math.cos(lean - (Math.PI / 4D))) * 0.8F;
      float uy = -((float)Math.sin(lean - (Math.PI / 4D))) * 0.8F;
      float vz = -((float)Math.cos(lean + (Math.PI / 4D))) * 0.8F;
      float vy = -((float)Math.sin(lean + (Math.PI / 4D))) * 0.8F;
      float x = -0.0625F;
      float y = 0.59375F - 0.2F * (uy + vy);
      float z = -0.0625F - 0.2F * (uz + vz);
      BlockQuads.vertex(vc, pose, -0.0625F, y, z, sprite, 0.0F, 1.0F, argb, 1.0F, 0.0F, 0.0F);
      BlockQuads.vertex(vc, pose, -0.0625F, y + uy, z + uz, sprite, 1.0F, 1.0F, argb, 1.0F, 0.0F, 0.0F);
      BlockQuads.vertex(vc, pose, -0.0625F, y + uy + vy, z + uz + vz, sprite, 1.0F, 0.0F, argb, 1.0F, 0.0F, 0.0F);
      BlockQuads.vertex(vc, pose, -0.0625F, y + vy, z + vz, sprite, 0.0F, 0.0F, argb, 1.0F, 0.0F, 0.0F);
   }

   private static boolean rolls(float way, float[] out) {
      float w = Mth.positiveModulo(way, 17.0854F);
      if (w < 5.4F) {
         out[0] = 0.6F + w;
         out[1] = 6.5F;
         out[2] = 1.0F;
         out[3] = 0.0F;
         return true;
      } else {
         float bend = w - 5.4F;
         if (bend < ((float)Math.PI / 4F)) {
            float a = bend / 0.5F;
            out[0] = 6.0F + 0.5F * Mth.sin(a);
            out[1] = 6.0F + 0.5F * Mth.cos(a);
            out[2] = Mth.cos(a);
            out[3] = -Mth.sin(a);
            return true;
         } else {
            float north = bend - ((float)Math.PI / 4F);
            if (north >= 5.4F) {
               return false;
            } else {
               out[0] = 6.5F;
               out[1] = 6.0F - north;
               out[2] = 0.0F;
               out[3] = -1.0F;
               return true;
            }
         }
      }
   }

   public boolean hasTranslucentPass() {
      return true;
   }

   protected void drawTranslucent(StageRenderContext ctx) {
      if (!(this.open <= 0.0F) && this.inside != null && this.spill != null) {
         float a = this.alpha();
         float out = this.lightOut();
         float t = time(ctx);
         ctx.pose.pushPose();
         ctx.pose.translate(-4.0F, -4.0F, -4.0F);
         this.spill.draw(ctx, true, a * out * (0.9F + 0.1F * flicker(t, 0.4F)), 1.0F);
         this.inside.draw(ctx, true, a, 1.0F);
         Matrix4f m = ctx.pose.last().pose();
         Matrix4f view = ctx.camera.view();
         this.right.set(view.m00(), view.m10(), view.m20());
         this.up.set(view.m01(), view.m11(), view.m21());
         this.modelInverse.transformDirection(this.right).normalize();
         this.modelInverse.transformDirection(this.up).normalize();
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.disableCull();
         BlockQuads.decalOn();
         StageBlend.over();
         BufferBuilder bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         this.drip(bb, m, t, a * out);
         MeshData mesh = bb.build();
         if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
         }

         StageBlend.add();
         bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

         for(PlayCave.Glow g : this.cave.glows) {
            float strength = g.flame() ? flicker(t, g.phase()) : 0.86F + 0.14F * Mth.sin(t * 1.3F + g.phase());
            this.disc(bb, m, g.x(), g.y(), g.z(), g.radius(), g.argb(), a * out * strength);
         }

         for(PlayCave.Glint g : this.cave.glints) {
            float wave = Math.max(0.0F, Mth.sin(t * g.pace() + g.phase()));
            float bright = wave * wave * wave;
            if (bright > 0.02F) {
               glint(bb, m, g, 0.07F + 0.09F * bright, a * out * bright);
            }
         }

         this.strike(bb, m, t, a * out);
         this.splash(bb, m, t, a * out);
         mesh = bb.build();
         if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
         }

         BlockQuads.decalOff();
         StageBlend.over();
         RenderSystem.enableCull();
         RenderSystem.depthMask(true);
         ctx.pose.popPose();
      }
   }

   private static float flicker(float t, float phase) {
      return 0.8F + 0.11F * Mth.sin(t * 9.1F + phase) + 0.09F * Mth.sin(t * 15.7F + phase * 2.3F);
   }

   private void disc(BufferBuilder bb, Matrix4f m, float x, float y, float z, float radius, int argb, float alpha) {
      int r = argb >> 16 & 255;
      int g = argb >> 8 & 255;
      int b = argb & 255;
      int full = Math.round((float)(argb >>> 24) * alpha);
      int half = Math.round((float)full * 0.4F);
      if (full > 0) {
         int slices = 12;
         float mid = radius * 0.42F;

         for(int i = 0; i < 12; ++i) {
            float a0 = (float)i / 12.0F * ((float)Math.PI * 2F);
            float a1 = (float)(i + 1) / 12.0F * ((float)Math.PI * 2F);
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);
            bb.addVertex(m, x, y, z).setColor(r, g, b, full);
            bb.addVertex(m, x, y, z).setColor(r, g, b, full);
            this.at(bb, m, x, y, z, c0 * mid, s0 * mid).setColor(r, g, b, half);
            this.at(bb, m, x, y, z, c1 * mid, s1 * mid).setColor(r, g, b, half);
            this.at(bb, m, x, y, z, c0 * mid, s0 * mid).setColor(r, g, b, half);
            this.at(bb, m, x, y, z, c0 * radius, s0 * radius).setColor(r, g, b, 0);
            this.at(bb, m, x, y, z, c1 * radius, s1 * radius).setColor(r, g, b, 0);
            this.at(bb, m, x, y, z, c1 * mid, s1 * mid).setColor(r, g, b, half);
         }

      }
   }

   private VertexConsumer at(BufferBuilder bb, Matrix4f m, float x, float y, float z, float across, float upward) {
      return bb.addVertex(m, x + this.right.x * across + this.up.x * upward, y + this.right.y * across + this.up.y * upward, z + this.right.z * across + this.up.z * upward);
   }

   private void spark(BufferBuilder bb, Matrix4f m, float x, float y, float z, float size, int argb, float alpha) {
      int r = argb >> 16 & 255;
      int g = argb >> 8 & 255;
      int b = argb & 255;
      int a = Math.round((float)(argb >>> 24) * alpha);
      if (a > 0) {
         this.at(bb, m, x, y, z, -size, 0.0F).setColor(r, g, b, a);
         this.at(bb, m, x, y, z, 0.0F, -size).setColor(r, g, b, a);
         this.at(bb, m, x, y, z, size, 0.0F).setColor(255, 255, 255, a);
         this.at(bb, m, x, y, z, 0.0F, size).setColor(r, g, b, a);
      }
   }

   private static void glint(BufferBuilder bb, Matrix4f m, PlayCave.Glint g, float size, float alpha) {
      int argb = g.argb();
      int r = argb >> 16 & 255;
      int gr = argb >> 8 & 255;
      int b = argb & 255;
      int a = Math.round((float)(argb >>> 24) * alpha);
      if (a > 0) {
         boolean x = g.across() == net.minecraft.core.Direction.Axis.X;
         boolean z = g.across() == net.minecraft.core.Direction.Axis.Z;
         float ax = x ? 0.0F : size;
         float ay = x ? size : 0.0F;
         float by = z ? size : 0.0F;
         float bz = z ? 0.0F : size;
         bb.addVertex(m, g.x() - ax, g.y() - ay, g.z()).setColor(r, gr, b, a);
         bb.addVertex(m, g.x(), g.y() - by, g.z() - bz).setColor(r, gr, b, a);
         bb.addVertex(m, g.x() + ax, g.y() + ay, g.z()).setColor(255, 255, 255, a);
         bb.addVertex(m, g.x(), g.y() + by, g.z() + bz).setColor(r, gr, b, a);
      }
   }

   private void strike(BufferBuilder bb, Matrix4f m, float t, float alpha) {
      float since = Mth.positiveModulo(t / 0.95F, 1.0F) - 0.28F;
      if (!(since < 0.0F) && !(since > 0.24F)) {
         float age = since / 0.24F;

         for(int i = 0; i < 4; ++i) {
            float angle = 0.5F + (float)i * 0.5F;
            float far = (0.12F + 0.07F * (float)i) * (1.0F - (1.0F - age) * (1.0F - age)) * 3.0F;
            float z = 5.05F + (0.15F + 0.5F * Mth.abs(Mth.cos(angle))) * far;
            float y = 3.0F + Mth.sin(angle) * far - age * age * 0.25F;
            this.spark(bb, m, 3.8799999F + ((float)i - 1.5F) * 0.3F * far, y, z, 0.07F, -5720, alpha * (1.0F - age));
         }

      }
   }

   private void splash(BufferBuilder bb, Matrix4f m, float t, float alpha) {
      for(int i = 0; i < 3; ++i) {
         float age = Mth.positiveModulo(t * 0.9F + (float)i * 0.37F, 1.0F);
         float x = 4.5F + ((float)i - 1.0F) * 0.24F + (i % 2 == 0 ? -0.12F : 0.12F) * age;
         float y = 1.94F + 1.1F * age * (1.0F - age);
         this.spark(bb, m, x, y, 2.35F + 0.14F * (float)i, 0.055F, -19641, alpha * 0.9F * (1.0F - age));
      }

   }

   private void drip(BufferBuilder bb, Matrix4f m, float t, float alpha) {
      float beat = Mth.positiveModulo(t / 1.7F, 1.0F);
      float x = 5.5F;
      float z = 3.5F;
      int a = Math.round(235.0F * alpha);
      if (a > 0) {
         if (beat < 0.6F) {
            float fall = Mth.clamp((beat - 0.42F) / 0.18F, 0.0F, 1.0F);
            float size = beat < 0.42F ? 0.03F + 0.045F * (beat / 0.42F) : 0.075F;
            float y = Mth.lerp(fall * fall, 3.6F - size, 1.91F);
            float tall = size * (1.0F + 0.9F * fall);
            this.at(bb, m, 5.5F, y, 3.5F, -size, -tall).setColor(143, 200, 255, a);
            this.at(bb, m, 5.5F, y, 3.5F, size, -tall).setColor(143, 200, 255, a);
            this.at(bb, m, 5.5F, y, 3.5F, size, tall).setColor(228, 244, 255, a);
            this.at(bb, m, 5.5F, y, 3.5F, -size, tall).setColor(228, 244, 255, a);
         } else {
            float age = (beat - 0.6F) / 0.4F;
            float half = 0.1F + 0.36F * (1.0F - (1.0F - age) * (1.0F - age));
            float thick = 0.07F;
            float y = 1.872F;
            int ra = Math.round((float)a * 0.75F * (1.0F - age));
            if (ra > 0) {
               for(int side = 0; side < 4; ++side) {
                  float ax = side == 0 ? -half : (side == 1 ? half - 0.07F : -half);
                  float bx = side == 0 ? -half + 0.07F : half;
                  float az = side == 2 ? -half : (side == 3 ? half - 0.07F : -half);
                  float bz = side == 2 ? -half + 0.07F : half;
                  bb.addVertex(m, 5.5F + ax, 1.872F, 3.5F + az).setColor(216, 238, 255, ra);
                  bb.addVertex(m, 5.5F + ax, 1.872F, 3.5F + bz).setColor(216, 238, 255, ra);
                  bb.addVertex(m, 5.5F + bx, 1.872F, 3.5F + bz).setColor(216, 238, 255, ra);
                  bb.addVertex(m, 5.5F + bx, 1.872F, 3.5F + az).setColor(216, 238, 255, ra);
               }

            }
         }
      }
   }

   static {
      FLIP = Axis.YP.rotationDegrees(180.0F);
      DEV_OPEN = devOpen(System.getProperty("cfhgui.titlePlayOpen"));
   }
}
