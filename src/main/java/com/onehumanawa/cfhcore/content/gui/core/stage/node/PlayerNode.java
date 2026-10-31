package com.onehumanawa.cfhcore.content.gui.core.stage.node;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageSoft;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PlayerNode extends StageNode {
   private static final Quaternionf FLIP;
   private static final Vector3f HEAD;
   private static int localSkins;
   private static final Map<UUID, CompletableFuture<Optional<GameProfile>>> FETCHED;
   private final Supplier<PlayerSkin> skin;
   private @Nullable PlayerModel<LivingEntity> wideModel;
   private @Nullable PlayerModel<LivingEntity> slimModel;
   private Pose pose;
   private boolean lookAtCursor;
   private boolean breathe;
   private Set<Part> shown;
   private float sitDrop;
   private float stridePhase;
   private float strideAmount;
   private boolean leaping;
   private float lookYaw;
   private float lookPitch;
   private final Vector3f head;
   private final Vector3f point;
   private static @Nullable Supplier<? extends PlayerNode> localFactory;

   public PlayerNode(GameProfile profile) {
      this.pose = PlayerNode.Pose.STAND;
      this.lookAtCursor = true;
      this.breathe = true;
      this.shown = EnumSet.allOf(Part.class);
      this.sitDrop = 0.5F;
      this.head = new Vector3f();
      this.point = new Vector3f();

      Supplier<PlayerSkin> s;
      try {
         s = skinOf(profile);
      } catch (Exception var5) {
         PlayerSkin fallback = DefaultPlayerSkin.get(profile.getId());
         s = () -> fallback;
      }

      this.skin = s;
      this.bounds(-0.4F, 0.0F, -0.4F, 0.4F, 1.9F, 0.4F);
      this.hoverFeel(0.03F, 1.03F);
      this.named(profile.getName() == null ? "player" : profile.getName());
   }

   public PlayerNode(UUID uuid, @Nullable String name) {
      this(new GameProfile(uuid, name != null && !name.isEmpty() ? name : "?"));
   }

   private static Supplier<PlayerSkin> skinOf(GameProfile profile) {
      Minecraft mc = Minecraft.getInstance();
      final SkinManager skins = mc.getSkinManager();
      if (profile.getProperties().containsKey("textures")) {
         return skins.lookupInsecure(profile);
      } else if (profile.getId().equals(mc.getUser().getProfileId())) {
         return skins.lookupInsecure(mc.getGameProfile());
      } else {
         final PlayerSkin fallback = DefaultPlayerSkin.get(profile.getId());
         final CompletableFuture<Optional<GameProfile>> fetch = (CompletableFuture)FETCHED.computeIfAbsent(profile.getId(), (id) -> CompletableFuture.supplyAsync(() -> Optional.ofNullable(mc.getMinecraftSessionService().fetchProfile(id, false)).map(ProfileResult::profile), Util.nonCriticalIoPool()).exceptionally((e) -> Optional.empty()));
         return new Supplier<PlayerSkin>() {
            private @Nullable Supplier<PlayerSkin> found;

            public PlayerSkin get() {
               if (this.found == null) {
                  Optional<GameProfile> full = (Optional)fetch.getNow(null);
                  if (full == null) {
                     return fallback;
                  }

                  this.found = full.isPresent() ? skins.lookupInsecure((GameProfile)full.get()) : () -> fallback;
               }

               return (PlayerSkin)this.found.get();
            }
         };
      }
   }

   public static GameProfile profile(UUID uuid, @Nullable String name) {
      Minecraft mc = Minecraft.getInstance();
      return uuid.equals(mc.getUser().getProfileId()) ? mc.getGameProfile() : new GameProfile(uuid, name != null && !name.isEmpty() ? name : "?");
   }

   public PlayerNode(ResourceLocation texture, boolean slim) {
      this.pose = PlayerNode.Pose.STAND;
      this.lookAtCursor = true;
      this.breathe = true;
      this.shown = EnumSet.allOf(Part.class);
      this.sitDrop = 0.5F;
      this.head = new Vector3f();
      this.point = new Vector3f();
      PlayerSkin fixed = new PlayerSkin(texture, (String)null, (ResourceLocation)null, (ResourceLocation)null, slim ? Model.SLIM : Model.WIDE, true);
      this.skin = () -> fixed;
      this.bounds(-0.4F, 0.0F, -0.4F, 0.4F, 1.9F, 0.4F);
      this.hoverFeel(0.03F, 1.03F);
      this.named("player");
   }

   public PlayerNode(Supplier<PlayerSkin> skin) {
      this.pose = PlayerNode.Pose.STAND;
      this.lookAtCursor = true;
      this.breathe = true;
      this.shown = EnumSet.allOf(Part.class);
      this.sitDrop = 0.5F;
      this.head = new Vector3f();
      this.point = new Vector3f();
      this.skin = skin;
      this.bounds(-0.4F, 0.0F, -0.4F, 0.4F, 1.9F, 0.4F);
      this.hoverFeel(0.03F, 1.03F);
      this.named("player");
   }

   public static void localFactory(@Nullable Supplier<? extends PlayerNode> factory) {
      localFactory = factory;
   }

   public static PlayerNode local() {
      Supplier<? extends PlayerNode> factory = localFactory;
      if (factory != null) {
         try {
            PlayerNode made = (PlayerNode)factory.get();
            if (made != null) {
               return made;
            }
         } catch (Exception e) {
            CFHGui.LOGGER.warn("[CFHGui] stage: the player's look could not be made: {}", e.toString());
         }
      }

      User u = Minecraft.getInstance().getUser();
      return new PlayerNode(profile(u.getProfileId(), u.getName()));
   }

   public static PlayerNode fromPng(Path png, boolean slim) {
      ResourceLocation tex = loadLocalSkin(png);
      return tex == null ? new PlayerNode(UUID.nameUUIDFromBytes(png.toString().getBytes()), png.getFileName().toString()) : new PlayerNode(tex, slim);
   }

   public static @Nullable ResourceLocation loadLocalSkin(Path png) {
      try {
         InputStream in = Files.newInputStream(png);

         ResourceLocation var4;
         label57: {
            ResourceLocation id;
            try {
               NativeImage image = NativeImage.read(in);
               if (image.getWidth() == 64 && (image.getHeight() == 64 || image.getHeight() == 32)) {
                  if (image.getHeight() == 32) {
                     image = upgradeLegacySkin(image);
                  }

                  int var10000 = localSkins++;
                  id = CFHGui.id("stage/skin/" + var10000);
                  Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
                  var4 = id;
                  break label57;
               }

               image.close();
               CFHGui.LOGGER.warn("[CFHGui] stage: skin {} is not 64x64 or 64x32", png);
               id = null;
            } catch (Throwable var6) {
               if (in != null) {
                  try {
                     in.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (in != null) {
               in.close();
            }

            return id;
         }

         if (in != null) {
            in.close();
         }

         return var4;
      } catch (Exception e) {
         CFHGui.LOGGER.warn("[CFHGui] stage: cannot load skin {}: {}", png, e.toString());
         return null;
      }
   }

   public static NativeImage upgradeLegacySkin(NativeImage legacy) {
      NativeImage image = new NativeImage(64, 64, true);
      image.copyFrom(legacy);
      legacy.close();
      image.fillRect(0, 32, 64, 32, 0);
      image.copyRect(4, 16, 16, 32, 4, 4, true, false);
      image.copyRect(8, 16, 16, 32, 4, 4, true, false);
      image.copyRect(0, 20, 24, 32, 4, 12, true, false);
      image.copyRect(4, 20, 16, 32, 4, 12, true, false);
      image.copyRect(8, 20, 8, 32, 4, 12, true, false);
      image.copyRect(12, 20, 16, 32, 4, 12, true, false);
      image.copyRect(44, 16, -8, 32, 4, 4, true, false);
      image.copyRect(48, 16, -8, 32, 4, 4, true, false);
      image.copyRect(40, 20, 0, 32, 4, 12, true, false);
      image.copyRect(44, 20, -8, 32, 4, 12, true, false);
      image.copyRect(48, 20, -16, 32, 4, 12, true, false);
      image.copyRect(52, 20, -8, 32, 4, 12, true, false);
      return image;
   }

   public PlayerNode pose(Pose p) {
      this.pose = p;
      this.boundsMax.y = p == PlayerNode.Pose.SIT ? 1.35F : 1.9F;
      return this;
   }

   public Pose pose() {
      return this.pose;
   }

   public PlayerNode lookAtCursor(boolean on) {
      this.lookAtCursor = on;
      return this;
   }

   public PlayerNode breathe(boolean on) {
      this.breathe = on;
      return this;
   }

   public PlayerNode show(Set<Part> parts) {
      this.shown = parts.isEmpty() ? EnumSet.allOf(Part.class) : EnumSet.copyOf(parts);
      return this;
   }

   public boolean shows(Part part) {
      return this.shown.contains(part);
   }

   protected void drawExtras(StageRenderContext ctx, PlayerModel<LivingEntity> model) {
   }

   public PlayerNode stride(float phase, float amount) {
      this.stridePhase = phase;
      this.strideAmount = Mth.clamp(amount, 0.0F, 1.0F);
      return this;
   }

   public PlayerNode leaping(boolean on) {
      this.leaping = on;
      return this;
   }

   public PlayerNode sitDrop(float blocks) {
      this.sitDrop = blocks;
      return this;
   }

   public PlayerSkin skin() {
      return (PlayerSkin)this.skin.get();
   }

   public void update(StageRenderContext ctx) {
      float targetYaw = 0.0F;
      float targetPitch = 0.0F;
      if (this.lookAtCursor && ctx.hasMouse) {
         this.head.set(HEAD);
         this.model.transformPosition(this.head);
         this.point.set(this.head).sub(ctx.rayOrigin);
         float t = Math.max(0.0F, this.point.dot(ctx.rayDir));
         this.point.set(ctx.rayDir).mul(t).add(ctx.rayOrigin);
         this.modelInverse.transformPosition(this.point);
         float dx = this.point.x - HEAD.x;
         float dy = this.point.y - HEAD.y;
         float dz = this.point.z - HEAD.z;
         float horiz = (float)Math.sqrt((double)(dx * dx + dz * dz));
         if (horiz > 1.0E-4F || Math.abs(dy) > 1.0E-4F) {
            targetYaw = Mth.clamp((float)Math.toDegrees(Math.atan2((double)(-dx), (double)dz)), -70.0F, 70.0F);
            targetPitch = Mth.clamp((float)Math.toDegrees(-Math.atan2((double)dy, (double)horiz)), -45.0F, 45.0F);
         }
      }

      float k = Theme.current().motion() <= 0.0F ? 1.0F : 1.0F - (float)Math.exp((double)(-ctx.deltaMs / 140.0F));
      this.lookYaw += (targetYaw - this.lookYaw) * k;
      this.lookPitch += (targetPitch - this.lookPitch) * k;
   }

   private PlayerModel<LivingEntity> model(boolean slim) {
      if (slim) {
         if (this.slimModel == null) {
            this.slimModel = new PlayerModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
         }

         return this.slimModel;
      } else {
         if (this.wideModel == null) {
            this.wideModel = new PlayerModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
         }

         return this.wideModel;
      }
   }

   protected void draw(StageRenderContext ctx) {
      PlayerSkin s = this.skin();
      ResourceLocation texture = s.texture();
      PlayerModel<LivingEntity> m = this.model(s.model() == Model.SLIM);
      this.setupPose(m, ctx);
      ctx.pose.pushPose();
      if (this.pose == PlayerNode.Pose.SIT) {
         ctx.pose.translate(0.0F, -this.sitDrop, 0.0F);
      }

      ctx.pose.mulPose(FLIP);
      ctx.pose.scale(-1.0F, -1.0F, 1.0F);
      ctx.pose.translate(0.0F, -1.501F, 0.0F);
      if (ctx.soft) {
         BufferBuilder bb = StageSoft.begin(StageSoft.ENTITY);
         m.renderToBuffer(ctx.pose, bb, ctx.light, OverlayTexture.NO_OVERLAY, -1);
         StageSoft.end(bb, StageSoft.ENTITY, texture, this.alpha(), false);
      } else {
         VertexConsumer vc = ctx.buffers.getBuffer(RenderType.entityTranslucent(texture));
         int color = Math.round(this.alpha() * 255.0F) << 24 | 16777215;
         m.renderToBuffer(ctx.pose, vc, ctx.light, OverlayTexture.NO_OVERLAY, color);
      }

      this.drawExtras(ctx, m);
      ctx.pose.popPose();
   }

   private void setupPose(PlayerModel<LivingEntity> m, StageRenderContext ctx) {
      float t = ctx.seconds();
      boolean anim = Theme.current().motion() > 0.0F;
      m.young = false;
      m.riding = false;
      m.crouching = false;
      m.swimAmount = 0.0F;
      m.attackTime = 0.0F;
      m.leftArmPose = ArmPose.EMPTY;
      m.rightArmPose = ArmPose.EMPTY;
      m.setAllVisible(true);
      m.head.resetPose();
      m.body.resetPose();
      m.rightArm.resetPose();
      m.leftArm.resetPose();
      m.rightLeg.resetPose();
      m.leftLeg.resetPose();
      float breath = this.breathe && anim ? Mth.sin(t * 1.6F) : 0.0F;
      float sway = anim ? Mth.sin(t * 1.3F) * 0.04F : 0.0F;
      m.head.yRot = (float)Math.toRadians((double)this.lookYaw);
      m.head.xRot = (float)Math.toRadians((double)this.lookPitch) + breath * 0.02F;
      ModelPart var10000 = m.body;
      var10000.y += breath * 0.15F;
      var10000 = m.head;
      var10000.y += breath * 0.15F;
      var10000 = m.rightArm;
      var10000.y += breath * 0.15F;
      var10000 = m.leftArm;
      var10000.y += breath * 0.15F;
      m.rightArm.xRot = sway;
      m.leftArm.xRot = -sway;
      m.rightArm.zRot = 0.04F + breath * 0.02F;
      m.leftArm.zRot = -0.04F - breath * 0.02F;
      switch (this.pose.ordinal()) {
         case 1:
            m.rightLeg.xRot = -1.4137167F;
            m.rightLeg.yRot = ((float)Math.PI / 10F);
            m.rightLeg.zRot = 0.07853982F;
            m.leftLeg.xRot = -1.4137167F;
            m.leftLeg.yRot = (-(float)Math.PI / 10F);
            m.leftLeg.zRot = -0.07853982F;
            var10000 = m.rightArm;
            var10000.xRot -= ((float)Math.PI / 5F);
            var10000 = m.leftArm;
            var10000.xRot -= ((float)Math.PI / 5F);
            break;
         case 2:
            float w = anim ? Mth.sin(t * 9.0F) : 0.0F;
            m.rightArm.xRot = -2.9F + w * 0.12F;
            m.rightArm.zRot = -0.5F - w * 0.35F;
            m.rightArm.yRot = 0.0F;
            break;
         default:
            if (this.leaping) {
               m.rightArm.xRot = -2.7F;
               m.leftArm.xRot = -2.7F;
               m.rightArm.zRot = 0.18F;
               m.leftArm.zRot = -0.18F;
               m.rightLeg.xRot = 0.55F;
               m.leftLeg.xRot = 0.25F;
            } else if (this.strideAmount > 0.0F) {
               float swing = Mth.cos(this.stridePhase) * 1.25F * this.strideAmount;
               m.rightLeg.xRot = swing;
               m.leftLeg.xRot = -swing;
               m.rightArm.xRot = -swing * 0.85F;
               m.leftArm.xRot = swing * 0.85F;
            }
      }

      m.hat.copyFrom(m.head);
      m.jacket.copyFrom(m.body);
      m.leftSleeve.copyFrom(m.leftArm);
      m.rightSleeve.copyFrom(m.rightArm);
      m.leftPants.copyFrom(m.leftLeg);
      m.rightPants.copyFrom(m.rightLeg);
      if (this.shown.size() < PlayerNode.Part.values().length) {
         m.head.visible = m.hat.visible = this.shown.contains(PlayerNode.Part.HEAD);
         m.body.visible = m.jacket.visible = this.shown.contains(PlayerNode.Part.BODY);
         m.rightArm.visible = m.rightSleeve.visible = this.shown.contains(PlayerNode.Part.RIGHT_ARM);
         m.leftArm.visible = m.leftSleeve.visible = this.shown.contains(PlayerNode.Part.LEFT_ARM);
         m.rightLeg.visible = m.rightPants.visible = this.shown.contains(PlayerNode.Part.RIGHT_LEG);
         m.leftLeg.visible = m.leftPants.visible = this.shown.contains(PlayerNode.Part.LEFT_LEG);
      }

   }

   public StageNode named(Component c) {
      return super.named(c);
   }

   static {
      FLIP = Axis.YP.rotationDegrees(180.0F);
      HEAD = new Vector3f(0.0F, 1.62F, 0.0F);
      FETCHED = new ConcurrentHashMap();
   }

   public static enum Pose {
      STAND,
      SIT,
      WAVE;
   }

   public static enum Part {
      HEAD,
      BODY,
      RIGHT_ARM,
      LEFT_ARM,
      RIGHT_LEG,
      LEFT_LEG;
   }
}
