package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import com.onehumanawa.cfhcore.content.gui.menu.client.creative.CFHGuiCreativeScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
   targets = {"dev.fallingcloud.betterinventory.client.InventorySwap"},
   remap = false
)
public abstract class BetterInventorySwapMixin {
   @Inject(
      method = {"buttonX"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0,
      remap = false
   )
   private static void cfhgui_menu$buttonX(Screen screen, CallbackInfoReturnable<Integer> cir) {
      if (screen instanceof CFHGuiCreativeScreen creative) {
         if (creative.laidOut()) {
            cir.setReturnValue(creative.swapX());
         }
      }

   }

   @Inject(
      method = {"buttonY"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0,
      remap = false
   )
   private static void cfhgui_menu$buttonY(Screen screen, CallbackInfoReturnable<Integer> cir) {
      if (screen instanceof CFHGuiCreativeScreen creative) {
         if (creative.laidOut()) {
            cir.setReturnValue(creative.swapY());
         }
      }

   }
}
