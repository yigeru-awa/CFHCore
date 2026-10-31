package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.stats;

import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.pause.LivePlayer;
import net.minecraft.client.Minecraft;

final class SelfFigure extends StageNode {
   private final LivePlayer live = new LivePlayer();

   SelfFigure() {
      this.bounds(-this.live.reach(), 0.0F, -this.live.reach(), this.live.reach(), this.live.height() - 0.05F, this.live.reach());
      this.hoverFeel(0.0F, 1.0F);
      this.pickable(false);
      this.named(Minecraft.getInstance().getUser().getName());
   }

   protected void draw(StageRenderContext ctx) {
      this.live.draw(ctx);
   }
}
