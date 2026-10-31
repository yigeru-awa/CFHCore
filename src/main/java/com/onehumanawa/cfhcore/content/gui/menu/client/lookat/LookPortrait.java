package com.onehumanawa.cfhcore.content.gui.menu.client.lookat;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

final class LookPortrait {
   private static final float BUST = 1.6F;
   private static final float HEADROOM = 0.18F;
   private static final float HORSE_HEADROOM = 0.45F;
   private static final float TURN = -25.0F;
   private static final Quaternionf FLIP = (new Quaternionf()).rotateZ((float)Math.PI);
   private static final Quaternionf TURNED = new Quaternionf();
   private static final PoseStack POSE = new PoseStack();
   private static final Set<EntityType<?>> BROKEN = new HashSet();
   private static final Runnable DRAW = LookPortrait::draw;
   private static @Nullable EntityRenderer<? super LivingEntity> renderer;
   private static @Nullable LivingEntity drawn;
   private static @Nullable GuiGraphics graphics;
   private static float yaw;
   private static float partial;

   private LookPortrait() {
   }

   static boolean of(LivingEntity mob) {
      return !BROKEN.contains(mob.getType());
   }

   static boolean draw(GuiGraphics g, LivingEntity mob, float left, float top, int w, int h, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
      EntityRenderer<? super LivingEntity> own = dispatcher.getRenderer(mob);
      if (own == null) {
         BROKEN.add(mob.getType());
         return false;
      } else {
         float tall = Math.max(0.4F, mob.getBbHeight());
         float wide = Math.max(0.4F, mob.getBbWidth());
         float over = LookFacts.horse(mob) ? tall * 0.45F : 0.18F;
         float scale = Math.min((float)(h - 2) / (Math.min(tall, wide * 1.6F) + over), (float)(w - 2) / (wide * 1.25F));
         float feet = Math.max(top + (float)h - 1.0F, top + 1.0F + (tall + over) * scale);
         float body = Mth.rotLerp(partialTick, mob.yBodyRotO, mob.yBodyRot);
         Window window = mc.getWindow();
         double pixels = window.getGuiScale();
         int clipW = (int)Math.round((double)(w - 2) * pixels);
         int clipH = (int)Math.round((double)(h - 2) * pixels);
         Entity picked = dispatcher.crosshairPickEntity;

         while(!POSE.clear()) {
            POSE.popPose();
         }

         POSE.setIdentity();
         POSE.last().pose().set(g.pose().last().pose());

         boolean var23;
         try {
            POSE.translate(left + (float)w / 2.0F, feet, 50.0F);
            POSE.scale(scale, scale, -scale);
            POSE.mulPose(FLIP);
            POSE.mulPose(TURNED.rotationY((body - 180.0F + -25.0F) * ((float)Math.PI / 180F)));
            Vec3 offset = own.getRenderOffset(mob, partialTick);
            POSE.translate(offset.x(), offset.y(), offset.z());
            renderer = own;
            drawn = mob;
            graphics = g;
            yaw = Mth.lerp(partialTick, mob.yRotO, mob.getYRot());
            partial = partialTick;
            dispatcher.crosshairPickEntity = null;
            RenderSystem.enableScissor((int)Math.round((double)(left + 1.0F) * pixels), window.getHeight() - (int)Math.round((double)(top + 1.0F) * pixels) - clipH, clipW, clipH);
            Lighting.setupForEntityInInventory();
            RenderSystem.runAsFancy(DRAW);
            g.flush();
            var23 = true;
            return var23;
         } catch (LinkageError | RuntimeException e) {
            BROKEN.add(mob.getType());
            CFHGuiMenu.LOGGER.info("[CFHGui Menu] look-at plate: {} cannot be drawn small ({}); its plate shows without the figure", EntityType.getKey(mob.getType()), ((Throwable)e).toString());

            try {
               g.flush();
            } catch (RuntimeException var28) {
            }

            var23 = false;
         } finally {
            RenderSystem.disableScissor();
            Lighting.setupFor3DItems();
            dispatcher.crosshairPickEntity = picked;
            renderer = null;
            drawn = null;
            graphics = null;
         }

         return var23;
      }
   }

   private static void draw() {
      renderer.render(drawn, yaw, partial, POSE, graphics.bufferSource(), 15728880);
   }
}
