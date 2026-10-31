package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;

public class CFHGuiSpinner extends CFHGuiWidget {
   public CFHGuiSpinner(int x, int y, int size) {
      super(x, y, size, size, Component.translatable("cfhgui.loading"));
      this.active = false;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return false;
   }

   public ComponentPath nextFocusPath(FocusNavigationEvent event) {
      return null;
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      draw(g, this.getX(), this.getY(), this.getWidth(), Colors.scaleAlpha(Theme.current().accent(), this.effectiveAlpha()));
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      draw(g, this.getX(), this.getY(), this.getWidth(), Colors.scaleAlpha(-1, this.effectiveAlpha()));
   }

   public static void draw(GuiGraphics g, int x, int y, int size, int color) {
      int dots = 8;
      int lit = (int)(Clock.nowMs() / 90L % 8L);
      float r = (float)(size - 2) / 2.0F;
      float cx = (float)x + (float)size / 2.0F;
      float cy = (float)y + (float)size / 2.0F;
      int dot = Math.max(1, size / 8);

      for(int i = 0; i < 8; ++i) {
         double ang = (double)i * Math.PI * (double)2.0F / (double)8.0F;
         int px = Math.round(cx + (float)Math.cos(ang) * r - (float)dot / 2.0F);
         int py = Math.round(cy + (float)Math.sin(ang) * r - (float)dot / 2.0F);
         int d = (i - lit + 8) % 8;
         float a = d == 0 ? 1.0F : (d == 1 ? 0.6F : (d == 2 ? 0.35F : 0.15F));
         g.fill(px, py, px + dot, py + dot, Colors.scaleAlpha(color, a));
      }

   }
}
