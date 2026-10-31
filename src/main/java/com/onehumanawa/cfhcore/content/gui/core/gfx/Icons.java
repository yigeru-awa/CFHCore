package com.onehumanawa.cfhcore.content.gui.core.gfx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class Icons {
   public static final ResourceLocation ATLAS = CFHGui.id("textures/gui/icons.png");

   public static void draw(GuiGraphics g, Icon icon, int x, int y, int color) {
      draw(g, icon, x, y, 16, color);
   }

   public static void draw(GuiGraphics g, Icon icon, int x, int y, int size, int color) {
      float a = (float)Colors.alpha(color) / 255.0F;
      if (!(a <= 0.004F)) {
         RenderSystem.enableBlend();
         g.setColor((float)Colors.red(color) / 255.0F, (float)Colors.green(color) / 255.0F, (float)Colors.blue(color) / 255.0F, a);
         g.blit(ATLAS, x, y, size, size, (float)icon.u(), (float)icon.v(), 16, 16, 256, Icon.rows() * 16);
         g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.disableBlend();
      }
   }

   private Icons() {
   }
}
