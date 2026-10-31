package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.pause;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class LivePlayer {
   private boolean broken;
   private final List<EntityType<?>> failed = new ArrayList(0);
   private final float reach;
   private final float height;

   public LivePlayer() {
      LocalPlayer p = Minecraft.getInstance().player;
      float side = 0.4F;
      float up = 1.95F;
      if (p != null && p.getVehicle() != null) {
         float[] size = new float[2];
         measure(p.getRootVehicle(), p.getRootVehicle(), size);
         side = Math.max(side, size[0]);
         up = Math.max(up, size[1] + 0.15F);
      }

      this.reach = side;
      this.height = up;
   }

   private static void measure(Entity entity, Entity root, float[] out) {
      double dx = entity.getX() - root.getX();
      double dz = entity.getZ() - root.getZ();
      out[0] = Math.max(out[0], (float)Math.sqrt(dx * dx + dz * dz) + entity.getBbWidth() / 2.0F);
      out[1] = Math.max(out[1], (float)(entity.getY() - root.getY()) + entity.getBbHeight());
      List<Entity> riders = entity.getPassengers();

      for(int i = 0; i < riders.size(); ++i) {
         measure((Entity)riders.get(i), root, out);
      }

   }

   public float reach() {
      return this.reach;
   }

   public float height() {
      return this.height;
   }

   public static double groundY(LocalPlayer player) {
      return player.getRootVehicle().getY();
   }

   public static float seat(LocalPlayer player) {
      return (float)(player.getY() - player.getRootVehicle().getY());
   }

   public boolean draw(StageRenderContext ctx) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer p = mc.player;
      if (p == null) {
         return true;
      } else {
         EntityRenderer<? super LocalPlayer> renderer = mc.getEntityRenderDispatcher().getRenderer(p);
         if (!this.broken && renderer != null) {
            float partial = mc.getTimer().getGameTimeDeltaPartialTick(true);
            Entity root = p.getRootVehicle();
            boolean riding = root != p;
            float var10000;
            if (!riding) {
               var10000 = 0.0F;
            } else {
               Entity var10 = p.getVehicle();
               if (var10 instanceof LivingEntity) {
                  LivingEntity carrier = (LivingEntity)var10;
                  var10000 = Mth.rotLerp(partial, carrier.yBodyRotO, carrier.yBodyRot);
               } else {
                  var10000 = Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot);
               }
            }

            float facing = var10000;
            double ox = Mth.lerp((double)partial, root.xOld, root.getX());
            double oy = Mth.lerp((double)partial, root.yOld, root.getY());
            double oz = Mth.lerp((double)partial, root.zOld, root.getZ());
            if (riding) {
               ctx.pose.pushPose();
               ctx.pose.mulPose(Axis.YP.rotationDegrees(facing));
               this.others(ctx, root, p, partial, ox, oy, oz);
            }

            float body = p.yBodyRot;
            float bodyO = p.yBodyRotO;
            float yaw = p.getYRot();
            float yawO = p.yRotO;
            float pitch = p.getXRot();
            float pitchO = p.xRotO;
            float head = p.yHeadRot;
            float headO = p.yHeadRotO;
            PoseStack.Pose mark = ctx.pose.last();

            try {
               p.yBodyRot = facing;
               p.yBodyRotO = facing;
               p.setYRot(facing);
               p.yRotO = facing;
               p.setXRot(0.0F);
               p.xRotO = 0.0F;
               p.yHeadRot = facing;
               p.yHeadRotO = facing;
               if (riding) {
                  ctx.pose.pushPose();
                  ctx.pose.translate(Mth.lerp((double)partial, p.xOld, p.getX()) - ox, Mth.lerp((double)partial, p.yOld, p.getY()) - oy, Mth.lerp((double)partial, p.zOld, p.getZ()) - oz);
               }

               renderer.render(p, facing, partial, ctx.pose, ctx.buffers, ctx.light);
               if (riding) {
                  ctx.pose.popPose();
               }
            } catch (RuntimeException e) {
               while(ctx.pose.last() != mark && !ctx.pose.clear()) {
                  ctx.pose.popPose();
               }

               this.broken = true;
               CFHGuiMenu.LOGGER.warn("[CFHGui Menu] the player could not be drawn on a menu's scene: {}", e.toString());
            } finally {
               p.yBodyRot = body;
               p.yBodyRotO = bodyO;
               p.setYRot(yaw);
               p.yRotO = yawO;
               p.setXRot(pitch);
               p.xRotO = pitchO;
               p.yHeadRot = head;
               p.yHeadRotO = headO;
               if (riding) {
                  ctx.pose.popPose();
               }

            }

            return !this.broken;
         } else {
            return false;
         }
      }
   }

   private void others(StageRenderContext ctx, Entity entity, LocalPlayer player, float partial, double ox, double oy, double oz) {
      if (entity != player && !this.failed.contains(entity.getType())) {
         EntityRenderer<? super Entity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
         PoseStack.Pose mark = ctx.pose.last();

         try {
            if (renderer != null) {
               ctx.pose.pushPose();
               Vec3 offset = renderer.getRenderOffset(entity, partial);
               ctx.pose.translate(Mth.lerp((double)partial, entity.xOld, entity.getX()) - ox + offset.x(), Mth.lerp((double)partial, entity.yOld, entity.getY()) - oy + offset.y(), Mth.lerp((double)partial, entity.zOld, entity.getZ()) - oz + offset.z());
               renderer.render(entity, Mth.lerp(partial, entity.yRotO, entity.getYRot()), partial, ctx.pose, ctx.buffers, ctx.light);
               ctx.pose.popPose();
            }
         } catch (RuntimeException e) {
            while(ctx.pose.last() != mark && !ctx.pose.clear()) {
               ctx.pose.popPose();
            }

            this.failed.add(entity.getType());
            CFHGuiMenu.LOGGER.warn("[CFHGui Menu] {} could not be drawn with the player on a menu's scene, left out: {}", EntityType.getKey(entity.getType()), e.toString());
         }
      }

      List<Entity> riders = entity.getPassengers();

      for(int i = 0; i < riders.size(); ++i) {
         this.others(ctx, (Entity)riders.get(i), player, partial, ox, oy, oz);
      }

   }
}
