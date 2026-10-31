package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.pause;

import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PlayerNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PlayerNode.Pose;
import org.jetbrains.annotations.Nullable;

final class SelfNode extends StageNode {
   private final LivePlayer live = new LivePlayer();
   private @Nullable PlayerNode standIn;

   SelfNode() {
      this.bounds(-this.live.reach(), 0.0F, -this.live.reach(), this.live.reach(), this.live.height() - 0.05F, this.live.reach());
      this.hoverFeel(0.03F, 1.03F);
      this.named("you");
   }

   float reach() {
      return this.live.reach();
   }

   float height() {
      return this.live.height();
   }

   protected void draw(StageRenderContext ctx) {
      if (!this.live.draw(ctx)) {
         this.drawStandIn(ctx);
      }

   }

   private void drawStandIn(StageRenderContext ctx) {
      if (this.standIn == null) {
         this.standIn = PlayerNode.local().lookAtCursor(false).pose(Pose.STAND);
      }

      try {
         this.standIn.prepare(ctx);
         this.standIn.update(ctx);
         this.standIn.render(ctx);
      } catch (RuntimeException var3) {
      }

   }
}
