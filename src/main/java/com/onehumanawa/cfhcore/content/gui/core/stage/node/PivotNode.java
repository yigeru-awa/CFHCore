package com.onehumanawa.cfhcore.content.gui.core.stage.node;

import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

public class PivotNode extends StageNode {
   private @Nullable Consumer<StageRenderContext> frame;

   public PivotNode() {
      this.pickable(false);
      this.hoverFeel(0.0F, 1.0F);
   }

   public PivotNode onFrame(@Nullable Consumer<StageRenderContext> callback) {
      this.frame = callback;
      return this;
   }

   public void update(StageRenderContext ctx) {
      if (this.frame != null) {
         this.frame.accept(ctx);
      }

   }

   protected void draw(StageRenderContext ctx) {
   }
}
