package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ContainerReskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ReskinDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractContainerScreen.class})
public abstract class ContainerScreenMixin {
   private static final String RENDER_BG = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V";
   private static final String RENDER_LABELS = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V";

   @Inject(
      method = {"renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;III)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private static void cfhgui$slotHighlight(GuiGraphics g, int x, int y, int blitOffset, CallbackInfo ci) {
      if (Reskin.containers()) {
         ci.cancel();
         ReskinDraw.containerSlotHighlight(g, x, y, blitOffset);
      }
   }

   @Inject(
      method = {"renderBackground"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V"
)},
      require = 0
   )
   private void cfhgui$beforeBg(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      ContainerReskin.beginBackground();
   }

   @Inject(
      method = {"renderBackground"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V",
   shift = Shift.AFTER
)},
      require = 0
   )
   private void cfhgui$afterBg(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      ContainerReskin.endBackground(g);
   }

   @Inject(
      method = {"render"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V"
)},
      require = 0
   )
   private void cfhgui$beforeLabels(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      ContainerReskin.beginLabels();
   }

   @Inject(
      method = {"render"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V",
   shift = Shift.AFTER
)},
      require = 0
   )
   private void cfhgui$afterLabels(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      ContainerReskin.endLabels();
   }
}
