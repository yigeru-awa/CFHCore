package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.onehumanawa.cfhcore.content.gui.core.client.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MouseHandler.class})
public abstract class MouseHandlerMixin {
   @Shadow
   @Final
   private Minecraft minecraft;

   @Inject(
      method = {"onPress"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cfhgui$onPress(long window, int button, int action, int mods, CallbackInfo ci) {
      if (window == this.minecraft.getWindow().getWindow()) {
         if (this.minecraft.screen != null && this.minecraft.getOverlay() == null) {
            if (action == 1) {
               if (ScreenInput.mousePressed(button)) {
                  ci.cancel();
               }
            } else if (action == 0 && ScreenInput.mouseReleased(button)) {
               ci.cancel();
            }

         } else {
            if (action == 0) {
               ScreenInput.releasedElsewhere(button);
            }

         }
      }
   }

   @Inject(
      method = {"onScroll"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cfhgui$onScroll(long window, double xoffset, double yoffset, CallbackInfo ci) {
      if (window == this.minecraft.getWindow().getWindow() && this.minecraft.screen != null && this.minecraft.getOverlay() == null) {
         boolean discrete = (Boolean)(Object)(Object)this.minecraft.options.discreteMouseScroll().get();
         double sens = (Double)(Object)(Object)this.minecraft.options.mouseWheelSensitivity().get();
         double sx = (discrete ? Math.signum(xoffset) : xoffset) * sens;
         double sy = (discrete ? Math.signum(yoffset) : yoffset) * sens;
         if (ScreenInput.mouseScrolled(sx, sy)) {
            ci.cancel();
         }

      }
   }

   @Inject(
      method = {"onMove"},
      at = {@At("HEAD")}
   )
   private void cfhgui$onMove(long window, double xpos, double ypos, CallbackInfo ci) {
      if (window == this.minecraft.getWindow().getWindow()) {
         ScreenInput.mouseMoved();
      }

   }
}
