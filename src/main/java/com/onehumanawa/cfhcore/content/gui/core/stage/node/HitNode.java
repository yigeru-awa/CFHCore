package com.onehumanawa.cfhcore.content.gui.core.stage.node;

import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;

public class HitNode extends StageNode {
   public HitNode() {
      this.pickable(true);
      this.hoverFeel(0.0F, 1.0F);
   }

   public HitNode box(float width, float height, float depth) {
      this.bounds(-width / 2.0F, 0.0F, -depth / 2.0F, width / 2.0F, height, depth / 2.0F);
      return this;
   }

   protected void draw(StageRenderContext ctx) {
   }
}
