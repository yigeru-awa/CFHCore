package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Minecraft.class})
public abstract class MinecraftDeathScreenMixin {
   @Shadow
   public @Nullable LocalPlayer player;
   @Shadow
   public @Nullable ClientLevel level;
   @Shadow
   private boolean clientLevelTeardownInProgress;

   @Shadow
   public abstract void setScreen(@Nullable Screen var1);

   @Inject(
      method = {"setScreen"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void cfhgui_menu$deathScreenOfTheGame(Screen guiScreen, CallbackInfo ci) {
      if (guiScreen == null && this.level != null && this.player != null && !this.clientLevelTeardownInProgress) {
         if (this.player.isDeadOrDying() && this.player.shouldShowDeathScreen()) {
            if (MenuSlots.effective("minecraft:death") != Layout.VANILLA) {
               ci.cancel();
               this.setScreen(new DeathScreen((Component)null, this.level.getLevelData().isHardcore()));
            }
         }
      }
   }
}
