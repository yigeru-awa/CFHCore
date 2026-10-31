package com.onehumanawa.cfhcore.content.gui.menu.client.lookat;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;

final class LookTarget {
   private LookTarget() {
   }

   static @Nullable LivingEntity of(Minecraft mc) {
      if (mc.player != null) {
         HitResult var2 = mc.hitResult;
         if (var2 instanceof EntityHitResult) {
            EntityHitResult hit = (EntityHitResult)var2;
            Entity entity = hit.getEntity();
            if (entity instanceof EnderDragonPart) {
               EnderDragonPart part = (EnderDragonPart)entity;
               entity = part.parentMob;
            }

            if (!(entity instanceof LivingEntity)) {
               entity = rider(entity);
            }

            if (entity instanceof LivingEntity) {
               LivingEntity living = (LivingEntity)entity;
               if (!(living instanceof ArmorStand)) {
                  if (living != mc.player && living.isAlive() && !living.isInvisibleTo(mc.player) && !tagHidden(living, mc.player)) {
                     return living;
                  }

                  return null;
               }
            }

            return null;
         }
      }

      return null;
   }

   private static @Nullable Entity rider(Entity vehicle) {
      LivingEntity steers = vehicle.getControllingPassenger();
      if (steers != null) {
         return steers;
      } else {
         List<Entity> riders = vehicle.getPassengers();

         for(int i = 0; i < riders.size(); ++i) {
            if (riders.get(i) instanceof LivingEntity) {
               return (Entity)riders.get(i);
            }
         }

         return null;
      }
   }

   private static boolean tagHidden(LivingEntity mob, Player player) {
      Team team = mob.getTeam();
      if (team == null) {
         return false;
      } else {
         Team own = player.getTeam();
         boolean var10000;
         switch (team.getNameTagVisibility()) {
            case NEVER -> var10000 = true;
            case HIDE_FOR_OTHER_TEAMS -> var10000 = own != null && !team.isAlliedTo(own);
            case HIDE_FOR_OWN_TEAM -> var10000 = own != null && team.isAlliedTo(own);
            default -> var10000 = false;
         }

         return var10000;
      }
   }
}
