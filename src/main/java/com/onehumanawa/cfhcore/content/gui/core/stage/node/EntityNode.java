package com.onehumanawa.cfhcore.content.gui.core.stage.node;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageLevel;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class EntityNode extends StageNode {
   private final StageLevel level;
   private final @Nullable Entity entity;
   private boolean tickEntity = true;
   private boolean tickBroken;
   private boolean lookAtCursor = true;
   private float lookYaw;
   private float lookPitch;
   private final Vector3f head = new Vector3f();
   private final Vector3f point = new Vector3f();

   public EntityNode(StageLevel level, EntityType<?> type) {
      this.level = level;
      this.entity = level.spawn(type, (double)0.0F, (double)0.0F, (double)0.0F, 0.0F);
      if (this.entity != null) {
         EntityDimensions d = this.entity.getDimensions(this.entity.getPose());
         float w = Math.max(0.3F, d.width());
         float h = Math.max(0.3F, d.height());
         this.bounds(-w / 2.0F, 0.0F, -w / 2.0F, w / 2.0F, h, w / 2.0F);
         this.hoverFeel(0.04F, 1.04F);
      }

      this.named(type.getDescription());
   }

   public @Nullable Entity entity() {
      return this.entity;
   }

   public EntityNode tickEntity(boolean on) {
      this.tickEntity = on;
      return this;
   }

   public EntityNode lookAtCursor(boolean on) {
      this.lookAtCursor = on;
      return this;
   }

   public EntityNode sitting(boolean sit) {
      Entity var3 = this.entity;
      if (var3 instanceof TamableAnimal t) {
         t.setInSittingPose(sit);
         t.setOrderedToSit(sit);
      }

      return this;
   }

   public EntityNode tamed(boolean tame) {
      Entity var3 = this.entity;
      if (var3 instanceof TamableAnimal t) {
         t.setTame(tame, false);
      }

      return this;
   }

   public void tick(StageRenderContext ctx) {
      if (this.entity != null && this.tickEntity && !this.tickBroken) {
         try {
            this.entity.setOldPosAndRot();
            Entity var3 = this.entity;
            if (var3 instanceof LivingEntity) {
               LivingEntity le = (LivingEntity)var3;
               le.yBodyRotO = le.yBodyRot;
               le.yHeadRotO = le.yHeadRot;
            }

            ++this.entity.tickCount;
            this.entity.tick();
            this.entity.setPos((double)0.0F, (double)0.0F, (double)0.0F);
            this.entity.setDeltaMovement((double)0.0F, (double)0.0F, (double)0.0F);
         } catch (Exception e) {
            this.tickBroken = true;
            CFHGui.LOGGER.warn("[CFHGui] stage: {} cannot tick without a world, left in its rest pose: {}", EntityType.getKey(this.entity.getType()), e.toString());
         }

      }
   }

   public void update(StageRenderContext ctx) {
      Entity var3 = this.entity;
      if (var3 instanceof LivingEntity le) {
         float var10 = 0.0F;
         float targetPitch = 0.0F;
         if (this.lookAtCursor && ctx.hasMouse) {
            this.head.set(0.0F, le.getEyeHeight(), 0.0F);
            this.model.transformPosition(this.head);
            this.point.set(this.head).sub(ctx.rayOrigin);
            float t = Math.max(0.0F, this.point.dot(ctx.rayDir));
            this.point.set(ctx.rayDir).mul(t).add(ctx.rayOrigin);
            this.modelInverse.transformPosition(this.point);
            float dx = this.point.x;
            float dy = this.point.y - le.getEyeHeight();
            float dz = this.point.z;
            float horiz = (float)Math.sqrt((double)(dx * dx + dz * dz));
            if (horiz > 1.0E-4F || Math.abs(dy) > 1.0E-4F) {
               var10 = Mth.clamp((float)Math.toDegrees(Math.atan2((double)(-dx), (double)dz)), -60.0F, 60.0F);
               targetPitch = Mth.clamp((float)Math.toDegrees(-Math.atan2((double)dy, (double)horiz)), -40.0F, 40.0F);
            }
         }

         float k = Theme.current().motion() <= 0.0F ? 1.0F : 1.0F - (float)Math.exp((double)(-ctx.deltaMs / 140.0F));
         this.lookYaw += (var10 - this.lookYaw) * k;
         this.lookPitch += (targetPitch - this.lookPitch) * k;
         le.yHeadRot = this.lookYaw;
         le.yHeadRotO = this.lookYaw;
         le.setXRot(this.lookPitch);
         le.xRotO = this.lookPitch;
         le.yBodyRot = 0.0F;
         le.yBodyRotO = 0.0F;
      }
   }

   protected void draw(StageRenderContext ctx) {
      if (this.entity != null) {
         EntityRenderer<? super Entity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(this.entity);
         if (renderer != null) {
            try {
               renderer.render(this.entity, 0.0F, ctx.partial, ctx.pose, ctx.buffers, ctx.light);
            } catch (Exception e) {
               this.visible = false;
               CFHGui.LOGGER.warn("[CFHGui] stage: {} cannot render on a stage, hidden: {}", EntityType.getKey(this.entity.getType()), e.toString());
            }

         }
      }
   }

   public void dispose() {
      if (this.entity != null) {
         this.level.discard(this.entity);
      }

   }
}
