package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.death;

import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;

final class FallenNode extends StageNode {
   private static final float DEATH_TICKS = 20.0F;
   private static final float HALF_LENGTH = 0.94F;
   private static final float HALF_WIDTH = 0.24F;
   private boolean broken;

   FallenNode() {
      this.bounds(-1.0F, 0.0F, -0.4F, 1.0F, 0.6F, 0.4F);
      this.hoverFeel(0.0F, 1.0F);
      this.pickable(false);
      this.named("you");
   }

   protected void draw(StageRenderContext ctx) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer p = mc.player;
      if (p != null && !this.broken) {
         EntityRenderer<? super LocalPlayer> renderer = mc.getEntityRenderDispatcher().getRenderer(p);
         if (renderer != null) {
            float body = p.yBodyRot;
            float bodyO = p.yBodyRotO;
            float yaw = p.getYRot();
            float yawO = p.yRotO;
            float pitch = p.getXRot();
            float pitchO = p.xRotO;
            float head = p.yHeadRot;
            float headO = p.yHeadRotO;
            int death = p.deathTime;
            float time = Theme.current().motion() > 0.0F ? Math.min(20.0F, 1.0F + ctx.timeMs / 50.0F) : 20.0F;
            float fallen = Math.min(1.0F, Mth.sqrt((time - 1.0F) / 20.0F * 1.6F));
            ctx.pose.pushPose();

            try {
               p.yBodyRot = 0.0F;
               p.yBodyRotO = 0.0F;
               p.setYRot(0.0F);
               p.yRotO = 0.0F;
               p.setXRot(0.0F);
               p.xRotO = 0.0F;
               p.yHeadRot = 0.0F;
               p.yHeadRotO = 0.0F;
               p.deathTime = (int)time;
               ctx.pose.translate(-0.94F * fallen, 0.24F * fallen, 0.0F);
               renderer.render(p, 0.0F, time - (float)((int)time), ctx.pose, ctx.buffers, ctx.light);
            } catch (RuntimeException e) {
               this.broken = true;
               CFHGuiMenu.LOGGER.warn("[CFHGui Menu] the player could not be drawn on the respawn screen, left out: {}", e.toString());
            } finally {
               ctx.pose.popPose();
               p.yBodyRot = body;
               p.yBodyRotO = bodyO;
               p.setYRot(yaw);
               p.yRotO = yawO;
               p.setXRot(pitch);
               p.xRotO = pitchO;
               p.yHeadRot = head;
               p.yHeadRotO = headO;
               p.deathTime = death;
            }

         }
      }
   }
}
