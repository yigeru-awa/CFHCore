package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import com.onehumanawa.cfhcore.content.gui.menu.client.creative.InventoryPointer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Minecraft.class})
public abstract class InventoryPointerMixin {
   @Shadow
   public @Nullable Screen screen;

   @Inject(
      method = {"setScreen"},
      at = {@At("HEAD")}
   )
   private void cfhgui$pointerLeaving(@Nullable Screen next, CallbackInfo ci) {
      InventoryPointer.leaving(this.screen, next);
   }

   @Inject(
      method = {"setScreen"},
      at = {@At("TAIL")}
   )
   private void cfhgui$pointerBack(@Nullable Screen next, CallbackInfo ci) {
      InventoryPointer.opened();
   }
}
