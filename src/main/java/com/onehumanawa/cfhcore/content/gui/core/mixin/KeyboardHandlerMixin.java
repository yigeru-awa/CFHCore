package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.onehumanawa.cfhcore.content.gui.core.client.ScreenInput;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardHandler.class})
public abstract class KeyboardHandlerMixin {
   @Shadow
   @Final
   private Minecraft minecraft;

   @Inject(
      method = {"keyPress"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cfhgui$keyPress(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
      if (window == this.minecraft.getWindow().getWindow() && this.minecraft.screen != null && this.minecraft.getOverlay() == null) {
         if ((action == 1 || action == 2) && ScreenInput.keyPressed(key, scancode, modifiers)) {
            ci.cancel();
         }

      }
   }

   @Inject(
      method = {"charTyped"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cfhgui$charTyped(long window, int codePoint, int modifiers, CallbackInfo ci) {
      if (window == this.minecraft.getWindow().getWindow() && this.minecraft.screen != null && this.minecraft.getOverlay() == null) {
         if (Character.charCount(codePoint) == 1 && ScreenInput.charTyped((char)codePoint, modifiers)) {
            ci.cancel();
         }

      }
   }
}
