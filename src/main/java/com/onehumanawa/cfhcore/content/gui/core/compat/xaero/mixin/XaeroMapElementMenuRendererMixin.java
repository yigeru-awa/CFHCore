package com.onehumanawa.cfhcore.content.gui.core.compat.xaero.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.onehumanawa.cfhcore.content.gui.core.compat.xaero.XaeroScreenDraw;
import com.onehumanawa.cfhcore.content.gui.core.compat.xaero.XaeroStyle;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(
   targets = {"xaero.map.element.MapElementMenuRenderer"},
   remap = false
)
public abstract class XaeroMapElementMenuRendererMixin {
   @Unique
   private static boolean cfhgui$on() {
      return XaeroStyle.worldMap() && Reskin.dark();
   }

   @WrapOperation(
      method = {"renderMenuElement*"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V",
   remap = true
)},
      remap = false,
      require = 0
   )
   private void cfhgui$plate(GuiGraphics g, int x0, int y0, int x1, int y1, int color, Operation<Void> original) {
      if (cfhgui$on()) {
         CFHGuiDraw.pixelRound(g, x0, y0, x1 - x0, y1 - y0, XaeroScreenDraw.listPlate(), 2);
      } else {
         original.call(new Object[]{g, x0, y0, x1, y1, color});
      }

   }

   @WrapOperation(
      method = {"renderMenuElement*", "renderScroll"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I",
   remap = true
)},
      remap = false,
      require = 0
   )
   private int cfhgui$name(GuiGraphics g, Font font, String text, int x, int y, int color, Operation<Integer> original) {
      return !cfhgui$on() ? (Integer)original.call(new Object[]{g, font, text, x, y, color}) : g.drawString(font, text, x, y, XaeroScreenDraw.listText(color), false);
   }
}
