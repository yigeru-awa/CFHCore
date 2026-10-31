package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.onehumanawa.cfhcore.content.gui.core.client.CFHGuiClient;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenSwaps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Minecraft.class})
public abstract class MinecraftMixin {
   @Shadow
   public ClientLevel level;
   @Shadow
   public Screen screen;
   @Unique
   private boolean cfhgui$reentering;

   @Shadow
   public abstract void setScreen(Screen var1);

   @Inject(
      method = {"setScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cfhgui$swapScreen(Screen guiScreen, CallbackInfo ci) {
      if (!this.cfhgui$reentering) {
         Screen candidate = guiScreen;
         if (guiScreen == null && this.level == null) {
            candidate = new TitleScreen();
         }

         Screen swapped = ScreenSwaps.apply(candidate);
         CFHGuiClient.beforeScreenShown(swapped != null ? swapped : candidate);
         if (swapped != null && swapped != candidate) {
            ci.cancel();
            this.cfhgui$reentering = true;

            try {
               this.setScreen(swapped);
            } finally {
               this.cfhgui$reentering = false;
            }
         }

      }
   }

   @Inject(
      method = {"setScreen"},
      at = {@At("TAIL")}
   )
   private void cfhgui$screenChanged(Screen guiScreen, CallbackInfo ci) {
      CFHGuiClient.onScreenChanged(this.screen);
   }
}
