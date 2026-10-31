package com.onehumanawa.cfhcore.content.gui.core.neoforge.mixin;

import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ReskinDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractContainerScreen.class})
public abstract class NeoContainerSlotHighlightMixin {
   @Inject(
      method = {"renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private static void cfhgui$slotHighlightColored(GuiGraphics g, int x, int y, int blitOffset, int color, CallbackInfo ci) {
      if (Reskin.containers()) {
         ci.cancel();
         ReskinDraw.containerSlotHighlight(g, x, y, blitOffset);
      }
   }
}
