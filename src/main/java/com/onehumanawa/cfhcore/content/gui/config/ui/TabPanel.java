package com.onehumanawa.cfhcore.content.gui.config.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import net.minecraft.client.gui.GuiGraphics;

public class TabPanel extends CFHGuiScrollPanel {
   private static final int SLIDE_PX = 8;
   private final int baseY;
   private final Anim slide;

   public TabPanel(int x, int y, int width, int height) {
      super(x, y, width, height);
      this.slide = new Anim(0.0F, 240, Ease.OUT_CUBIC);
      this.baseY = y;
   }

   public TabPanel slideFrom(int dir) {
      if (dir != 0) {
         this.slide.snap(8.0F);
         this.slide.set(0.0F);
      }

      return this;
   }

   protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      int y = this.baseY + Math.round(this.slide.get());
      if (this.getY() != y) {
         this.setY(y);
      }

      super.renderWidget(g, mouseX, mouseY, partialTick);
   }
}
