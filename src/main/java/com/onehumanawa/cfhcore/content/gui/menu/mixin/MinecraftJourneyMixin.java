package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import com.onehumanawa.cfhcore.content.gui.menu.client.loading.journey.Journey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Minecraft.class})
public abstract class MinecraftJourneyMixin {
   @Inject(
      method = {"disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V"},
      at = {@At("HEAD")}
   )
   private void cfhgui_menu$leaving(Screen next, boolean keepResourcePacks, CallbackInfo ci) {
      Journey.leaving();
   }
}
