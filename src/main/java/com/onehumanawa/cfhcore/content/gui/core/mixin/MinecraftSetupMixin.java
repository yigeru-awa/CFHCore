package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.onehumanawa.cfhcore.content.gui.core.client.setup.CFHGuiSetupScreen;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Minecraft.class})
public abstract class MinecraftSetupMixin {
   @Inject(
      method = {"addInitialScreens"},
      at = {@At("HEAD")}
   )
   private void cfhgui$setupScreen(List<Function<Runnable, Screen>> output, CallbackInfo ci) {
      if (CFHGuiSetupScreen.wanted()) {
         output.add((next) -> new CFHGuiSetupScreen((Screen)null, next));
      }

   }
}
