package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;

public class CFHGuiProgress extends CFHGuiWidget {
   private final Anim value;
   private boolean indeterminate;
   private int color;

   public CFHGuiProgress(int x, int y, int width, int height) {
      super(x, y, width, height, Component.empty());
      this.value = new Anim(0.0F, 300, Ease.OUT_CUBIC);
      this.color = 0;
      this.active = false;
   }

   public CFHGuiProgress set(float v) {
      this.value.set(Math.max(0.0F, Math.min(1.0F, v)));
      return this;
   }

   public CFHGuiProgress snap(float v) {
      this.value.snap(Math.max(0.0F, Math.min(1.0F, v)));
      return this;
   }

   public CFHGuiProgress indeterminate(boolean on) {
      this.indeterminate = on;
      return this;
   }

   public CFHGuiProgress color(int argb) {
      this.color = argb;
      return this;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return false;
   }

   public ComponentPath nextFocusPath(FocusNavigationEvent event) {
      return null;
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, Theme.current().palette().surfaceActive());
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, -16777216);
   }

   private void draw(GuiGraphics g, int track) {
      Palette p = Theme.current().palette();
      float a = this.effectiveAlpha();
      int x = this.getX();
      int y = this.getY();
      int w = this.getWidth();
      int h = this.getHeight();
      int fill = this.color != 0 ? this.color : p.accent();
      CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(track, a), 1);
      if (this.indeterminate) {
         float t = (float)(Clock.nowMs() % 1400L) / 1400.0F;
         int segW = Math.max(8, w / 3);
         int sx = x + Math.round((float)(w + segW) * t) - segW;
         CFHGuiDraw.scissor(g, x, y, w, h);
         CFHGuiDraw.pixelRound(g, sx, y, segW, h, Colors.scaleAlpha(fill, a), 1);
         CFHGuiDraw.unscissor(g);
      } else {
         int fw = Math.round((float)w * this.value.get());
         if (fw > 0) {
            CFHGuiDraw.pixelRound(g, x, y, fw, h, Colors.scaleAlpha(fill, a), 1);
         }
      }

   }
}
