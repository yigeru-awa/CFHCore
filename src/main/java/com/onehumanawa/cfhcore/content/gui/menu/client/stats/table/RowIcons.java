package com.onehumanawa.cfhcore.content.gui.menu.client.stats.table;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiAvatar;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiAvatar.Status;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

public final class RowIcons {
   public static final int SIZE = 16;
   private static final Quaternionf FLIP = (new Quaternionf()).rotateZ((float)Math.PI);
   private static final Map<EntityType<?>, LivingEntity> MODELS = new HashMap();
   private static final Set<EntityType<?>> BROKEN = new HashSet();
   private static @Nullable ClientLevel madeIn;

   public static void draw(GuiGraphics g, RowIcon icon, int x, int y, int color, float alpha) {
      if (icon instanceof RowIcon.Stack s) {
         g.renderFakeItem(s.stack(), x, y);
      } else if (icon instanceof RowIcon.Mob m) {
         if (!m.egg().isEmpty()) {
            g.renderFakeItem(m.egg(), x, y);
         } else if (m.type() == null || !mob(g, m.type(), x, y)) {
            Icons.draw(g, Icon.SWORD, x + 2, y + 2, 12, color);
         }
      } else if (icon instanceof RowIcon.Face f) {
         CFHGuiAvatar.draw(g, CFHGuiAvatar.skinFor(f.id(), f.name()), x, y, 16, f.online() == null ? Status.NONE : (f.online() ? Status.ONLINE : Status.OFFLINE), alpha);
      } else if (icon instanceof RowIcon.Glyph gl) {
         Icons.draw(g, gl.icon(), x + 2, y + 2, 12, color);
      }

   }

   public static @Nullable SpawnEggItem egg(@Nullable EntityType<?> type) {
      return type == null ? null : SpawnEggItem.byId(type);
   }

   public static void release() {
      MODELS.clear();
      BROKEN.clear();
      madeIn = null;
   }

   private static @Nullable LivingEntity model(EntityType<?> type) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return null;
      } else {
         if (madeIn != level) {
            release();
            madeIn = level;
         }

         if (BROKEN.contains(type)) {
            return null;
         } else {
            LivingEntity model = (LivingEntity)MODELS.get(type);
            if (model == null) {
               try {
                  Entity made = type.create(level);
                  if (made instanceof LivingEntity) {
                     LivingEntity living = (LivingEntity)made;
                     model = living;
                     MODELS.put(type, living);
                  } else {
                     BROKEN.add(type);
                  }
               } catch (LinkageError | RuntimeException e) {
                  BROKEN.add(type);
                  CFHGuiMenu.LOGGER.debug("[CFHGui Menu] {} cannot be made for the statistics screen: {}", EntityType.getKey(type), ((Throwable)e).toString());
               }
            }

            return model;
         }
      }
   }

   private static boolean mob(GuiGraphics g, EntityType<?> type, int x, int y) {
      LivingEntity e = model(type);
      if (e == null) {
         return false;
      } else {
         float bodyYaw = e.yBodyRot;
         float yaw = e.getYRot();
         float pitch = e.getXRot();
         float headYawO = e.yHeadRotO;
         float headYaw = e.yHeadRot;
         PoseStack pose = new PoseStack();
         pose.mulPose(g.pose().last().pose());
         EntityRenderer<? super LivingEntity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(e);
         if (renderer == null) {
            BROKEN.add(type);
            MODELS.remove(type);
            return false;
         } else {
            boolean offset;
            try {
               float scale = 14.0F / Math.max(Math.max(0.4F, e.getBbHeight()), Math.max(0.4F, e.getBbWidth()) * 1.25F);
               pose.translate((float)x + 8.0F, (float)(y + 16) - 1.0F, 50.0F);
               pose.scale(scale, scale, -scale);
               pose.mulPose(FLIP);
               e.yBodyRot = 205.0F;
               e.setYRot(205.0F);
               e.setXRot(0.0F);
               e.yHeadRot = 205.0F;
               e.yHeadRotO = 205.0F;
               Lighting.setupForEntityInInventory();
               Vec3 off = renderer.getRenderOffset(e, 1.0F);
               pose.translate(off.x(), off.y(), off.z());
               RenderSystem.runAsFancy(() -> renderer.render(e, 0.0F, 1.0F, pose, g.bufferSource(), 15728880));
               g.flush();
               boolean var14 = true;
               return var14;
            } catch (LinkageError | RuntimeException ex) {
               BROKEN.add(type);
               MODELS.remove(type);
               CFHGuiMenu.LOGGER.debug("[CFHGui Menu] {} cannot be drawn on the statistics screen: {}", EntityType.getKey(type), ((Throwable)ex).toString());

               try {
                  g.flush();
               } catch (RuntimeException var19) {
               }

               offset = false;
            } finally {
               Lighting.setupFor3DItems();
               e.yBodyRot = bodyYaw;
               e.setYRot(yaw);
               e.setXRot(pitch);
               e.yHeadRotO = headYawO;
               e.yHeadRot = headYaw;
            }

            return offset;
         }
      }
   }

   private RowIcons() {
   }
}
