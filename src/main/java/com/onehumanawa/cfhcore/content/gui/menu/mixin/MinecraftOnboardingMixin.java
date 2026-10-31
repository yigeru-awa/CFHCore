package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Minecraft.class})
public abstract class MinecraftOnboardingMixin {
   @Shadow
   @Final
   public Options options;

   @Inject(
      method = {"addInitialScreens"},
      at = {@At("HEAD")}
   )
   private void cfhgui_menu$skipOnboarding(List<Function<Runnable, Screen>> output, CallbackInfo ci) {
      if (CFHGuiMenu.config().skipOnboarding) {
         this.options.onboardAccessibility = false;
      }

   }
}
