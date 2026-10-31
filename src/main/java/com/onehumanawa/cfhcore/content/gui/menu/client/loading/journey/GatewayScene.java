package com.onehumanawa.cfhcore.content.gui.menu.client.loading.journey;

import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageFinish;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.IdleMotion;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.BlocksNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.BurstNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.FxNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.MotesNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PlayerNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PlayerNode.Pose;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;

final class GatewayScene {
   static final int LOOKING = 0;
   static final int ANSWERED = 1;
   static final int SECURED = 2;
   static final int JOINING = 3;
   static final int THROUGH = 4;
   private static final float GATE_Y = 3.5F;
   private static final int VIOLET = -6005505;
   private final Stage stage = new Stage();
   private final BlocksNode fire;
   private final FxNode pool;
   private final FxNode sparks;
   private final FxNode halo;
   private final MotesNode motes;
   private final BurstNode burst;
   private final PlayerNode player;
   private final Jumper steve;
   private final Jumper alex;
   private final Jumper self;
   private final StageFinish finish = StageFinish.cinematic().vignette(0.36F);
   private final boolean leaving;
   private float lit;
   private float near;
   private float through;
   private float since;
   private boolean fired;

   GatewayScene(boolean leaving) {
      this.leaving = leaving;
      Minecraft mc = Minecraft.getInstance();
      this.stage.gradient(-16448502, -15068378);
      this.stage.fog(true, 13.0F, 30.0F, -15594212);
      this.stage.soft(true);
      this.stage.finish(this.finish);
      this.stage.resolutionScale(mc.getWindow().getWidth() <= 1600 ? 2.0F : 1.5F);
      this.stage.camera().clip(0.05F, 120.0F);
      this.stage.camera().idle(IdleMotion.sway(19000.0F, 2.2F, 0.5F, 0.02F));
      this.stage.lighting().key(1.5F, 5.5F, 6.0F).keyColor(0.62F, 0.58F, 0.7F).ambientColor(0.25F, 0.24F, 0.33F).fill(-1.0F, 0.4F, 0.3F, 0.1F, 0.12F, 0.22F).rim(0.72F, 0.45F, 1.0F, 0.3F).sun(0.3F, 1.0F, 0.7F);
      ((FxNode)this.stage.add(FxNode.glow(20.0F, -14607573).fade(2.4F))).at(0.0F, 0.005F, 1.0F);
      BlocksNode terrace = new BlocksNode(this.stage.level(), 8, 2, 7);
      terrace.fill(0, 0, 0, 7, 0, 6, Blocks.POLISHED_BLACKSTONE_BRICKS);
      terrace.fill(1, 1, 1, 6, 1, 5, Blocks.POLISHED_BLACKSTONE_BRICKS);
      terrace.fill(2, 1, 1, 5, 1, 5, Blocks.POLISHED_DEEPSLATE);

      for(int[] c : new int[][]{{0, 0, 0}, {7, 0, 0}, {0, 0, 6}, {7, 0, 6}}) {
         terrace.put(c[0], c[1], c[2], Blocks.CHISELED_POLISHED_BLACKSTONE);
      }

      for(int[] c : new int[][]{{3, 0, 6}, {6, 0, 2}, {1, 1, 4}, {6, 1, 1}}) {
         terrace.put(c[0], c[1], c[2], Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
      }

      terrace.put(1, 1, 1, Blocks.GILDED_BLACKSTONE).put(6, 1, 5, Blocks.GILDED_BLACKSTONE);
      ((BlocksNode)this.stage.add(terrace.done())).centered();
      BlocksNode gate = new BlocksNode(this.stage.level(), 4, 5, 1);
      gate.fill(0, 0, 0, 3, 0, 0, Blocks.OBSIDIAN).fill(0, 4, 0, 3, 4, 0, Blocks.OBSIDIAN);
      gate.fill(0, 1, 0, 0, 3, 0, Blocks.OBSIDIAN).fill(3, 1, 0, 3, 3, 0, Blocks.OBSIDIAN);
      gate.put(0, 3, 0, Blocks.CRYING_OBSIDIAN).put(3, 1, 0, Blocks.CRYING_OBSIDIAN).put(2, 4, 0, Blocks.CRYING_OBSIDIAN);
      ((BlocksNode)this.stage.add(gate.done())).centered().at(0.0F, 2.0F, 0.0F);
      BlockState portal = (BlockState)Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, Axis.X);
      this.fire = new BlocksNode(this.stage.level(), 2, 3, 1);

      for(int x = 0; x < 2; ++x) {
         for(int y = 0; y < 3; ++y) {
            this.fire.put(x, y, 0, portal);
         }
      }

      ((BlocksNode)this.stage.add(this.fire.done())).centered().at(0.0F, 3.0F, 0.0F);
      this.fire.alpha(0.0F);
      this.pool = (FxNode)((FxNode)this.stage.add(FxNode.glow(4.2F, -1333115408).fade(1.5F))).at(0.0F, 2.02F, 1.2F);
      this.halo = (FxNode)((FxNode)this.stage.add(FxNode.glow(9.0F, 947077072).fade(1.2F))).at(0.0F, 0.02F, 3.0F);
      this.sparks = (FxNode)((FxNode)this.stage.add(FxNode.sparkles(30, 2.2F, 3.2F, 1.2F, 0.06F, -1651457, 3L))).at(0.0F, 3.5F, 0.5F);
      this.motes = (MotesNode)((MotesNode)this.stage.add(new MotesNode(52, 6.0F, 4.4F, 4.0F, 0.03F, -1866101761, 9L))).at(0.0F, 2.0F, 0.6F);
      this.burst = (BurstNode)((BurstNode)this.stage.add(new BurstNode(34, 1.5F, 0.07F, -6005505, 21L))).at(0.0F, 2.6F, 0.2F);
      this.player = PlayerNode.local();
      this.player.lookAtCursor(false).pose(Pose.STAND);
      ((PlayerNode)this.stage.add(this.player)).at(-1.25F, 2.0F, 2.1F).yaw(168.0F);
      this.self = new Jumper(this.player, -1.25F, 2.1F, 168.0F, -0.25F);
      this.steve = this.jumper(new PlayerNode(ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png"), false), 1.1F, 2.3F, 196.0F, 0.2F);
      this.alex = this.jumper(new PlayerNode(ResourceLocation.withDefaultNamespace("textures/entity/player/slim/alex.png"), true), 2.75F, 2.3F, 206.0F, -0.15F);
      if (leaving) {
         this.steve.node.visible(false);
         this.alex.node.visible(false);
      }

      if (leaving) {
         this.lit = 1.0F;
         this.fired = true;
      }

      this.glow();
   }

   private Jumper jumper(PlayerNode node, float x, float z, float yaw, float lane) {
      node.lookAtCursor(false).pose(Pose.STAND);
      ((PlayerNode)this.stage.add(node)).at(x, 2.0F, z).yaw(yaw);
      return new Jumper(node, x, z, yaw, lane);
   }

   Stage stage() {
      return this.stage;
   }

   boolean leaving() {
      return this.leaving;
   }

   private void glow() {
      float l = this.lit;
      this.fire.alpha(Math.min(1.0F, l * 1.25F));
      this.pool.opacity(0.69F * l);
      this.halo.opacity(0.22F * l);
      this.sparks.opacity(l);
      this.motes.strength(0.25F + 0.75F * l);
      this.finish.bloom(0.42F + 0.34F * l);
      this.stage.lighting().keyColor(Mth.lerp(l, 0.62F, 0.66F), Mth.lerp(l, 0.58F, 0.46F), Mth.lerp(l, 0.7F, 0.95F)).key(Mth.lerp(l, 1.5F, 0.4F), Mth.lerp(l, 5.5F, 3.6F), Mth.lerp(l, 6.0F, 2.6F));
      this.stage.lighting().rim(0.72F, 0.45F, 1.0F, 0.16F + 0.3F * l);
   }

   void render(GuiGraphics g, int width, int height, int step, float partialTick) {
      float motion = Theme.current().motion();
      float now = this.stage.timeMs();
      float dt = Math.min(0.25F, Math.max(0.0F, now - this.since) / 1000.0F);
      this.since = now;
      float wantLit;
      float wantNear;
      float wantThrough;
      if (this.leaving) {
         wantLit = 0.0F;
         wantNear = 0.0F;
         wantThrough = 0.0F;
      } else {
         wantLit = step <= 0 ? 0.0F : (step == 1 ? 0.6F : (step == 2 ? 0.8F : 1.0F));
         wantNear = Mth.clamp((float)step / 3.0F, 0.0F, 1.0F);
         wantThrough = step >= 4 ? 1.0F : 0.0F;
      }

      float secs = now / 1000.0F;
      if (!this.leaving) {
         if (this.lit > 0.5F || wantThrough > 0.0F) {
            this.steve.go(secs);
         }

         if (this.steve.goneFor(secs) > 0.6F) {
            this.alex.go(secs);
         }

         if (wantThrough > 0.0F && this.alex.goneFor(secs) > 0.45F) {
            this.self.go(secs);
         }
      }

      float viewThrough = this.self.started >= 0.0F ? wantThrough : 0.0F;
      if (motion <= 0.0F) {
         this.lit = wantLit;
         this.near = wantNear;
         this.through = wantThrough;
      } else {
         this.lit += Mth.clamp(wantLit - this.lit, -dt * (this.leaving ? 0.55F : 2.0F), dt * 1.5F);
         this.near += (wantNear - this.near) * Math.min(1.0F, dt * 1.6F);
         this.through += Mth.clamp(viewThrough - this.through, -dt, dt * 0.62F);
      }

      if (!this.fired && this.lit > 0.05F) {
         this.fired = true;
         this.burst.fire();
      }

      if (!this.leaving) {
         this.steve.move(secs, motion);
         this.alex.move(secs, motion);
         this.self.move(secs, motion);
      }

      this.glow();
      this.aim((float)width / (float)Math.max(1, height));
      this.stage.render(g, 0, 0, width, height, (double)-1.0F, (double)-1.0F, partialTick);
   }

   private void aim(float aspect) {
      float narrow = aspect < 1.5F ? (1.5F - aspect) * 9.0F : 0.0F;
      float n = this.near * this.near * (3.0F - 2.0F * this.near);
      float x = Mth.lerp(n, 4.0F, 3.1F);
      float y = Mth.lerp(n, 3.5F, 3.3F);
      float z = Mth.lerp(n, 14.2F, 12.2F);
      float tx = Mth.lerp(n, -0.4F, -0.3F);
      float ty = Mth.lerp(n, 3.5F, 3.6F);
      float tz = 0.0F;
      float fov = Mth.lerp(n, 38.0F, 37.0F) + narrow;
      if (this.through > 0.0F) {
         float t = this.through * this.through * (3.0F - 2.0F * this.through);
         x = Mth.lerp(t, x, 0.0F);
         y = Mth.lerp(t, y, 3.5F);
         z = Mth.lerp(t, z, 0.75F);
         tx = Mth.lerp(t, tx, 0.0F);
         ty = Mth.lerp(t, ty, 3.5F);
         tz = Mth.lerp(t, tz, -4.0F);
         fov = Mth.lerp(t, fov, 58.0F);
      }

      this.stage.camera().at(x, y, z).lookAt(tx, ty, tz).fov(fov);
   }

   float through() {
      return this.through;
   }

   void close() {
      this.stage.close();
   }

   private static final class Jumper {
      private static final float SPEED = 4.6F;
      private static final float LEAP = 0.5F;
      private static final float FRONT = 1.3F;
      private static final float BEYOND = -1.1F;
      final PlayerNode node;
      private final float x0;
      private final float z0;
      private final float yaw0;
      private final float lane;
      float started = -1.0F;

      Jumper(PlayerNode node, float x, float z, float yaw, float lane) {
         this.node = node;
         this.x0 = x;
         this.z0 = z;
         this.yaw0 = yaw;
         this.lane = lane;
      }

      void go(float secs) {
         if (this.started < 0.0F) {
            this.started = secs;
         }

      }

      float goneFor(float secs) {
         return this.started < 0.0F ? -1.0F : secs - this.started;
      }

      void move(float secs, float motion) {
         if (!(this.started < 0.0F)) {
            if (motion <= 0.0F) {
               this.node.visible(false);
            } else {
               float t = secs - this.started;
               float dx = this.lane - this.x0;
               float dz = 1.3F - this.z0;
               float dist = (float)Math.sqrt((double)(dx * dx + dz * dz));
               float run = dist / 4.6F;
               float y = 2.0F;
               float x;
               float z;
               if (t < run) {
                  float k = t / run;
                  float eased = k * k * (3.0F - 2.0F * k) * 0.35F + k * 0.65F;
                  x = Mth.lerp(eased, this.x0, this.lane);
                  z = Mth.lerp(eased, this.z0, 1.3F);
                  this.node.leaping(false).stride(t * 15.0F, Math.min(1.0F, t * 4.0F));
                  this.node.yaw(turn(this.yaw0, (float)Math.toDegrees(Math.atan2((double)dx, (double)dz)), Math.min(1.0F, t * 6.0F)));
               } else {
                  float k = Math.min(1.0F, (t - run) / 0.5F);
                  x = this.lane;
                  z = Mth.lerp(k, 1.3F, -1.1F);
                  y = 2.0F + 0.95F * Mth.sin(k * (float)Math.PI) + 0.25F * k;
                  this.node.stride(0.0F, 0.0F).leaping(true);
                  this.node.yaw(180.0F);
               }

               this.node.at(x, y, z);
               this.node.alpha(Mth.clamp((z + 0.45F) / 0.8F, 0.0F, 1.0F));
               this.node.visible(z > -0.45F);
            }
         }
      }

      private static float turn(float from, float to, float k) {
         return from + Mth.wrapDegrees(to - from) * k;
      }
   }
}
