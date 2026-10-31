package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import com.onehumanawa.cfhcore.content.gui.menu.client.debug.DebugRestyle;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DebugScreenOverlay.class})
public abstract class DebugScreenOverlayMixin {
   @Inject(
      method = {"renderLines"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void cfhgui$renderLines(GuiGraphics graphics, List<String> lines, boolean leftSide, CallbackInfo ci) {
      if (DebugRestyle.lines(graphics, lines, leftSide)) {
         ci.cancel();
      }

   }
}
