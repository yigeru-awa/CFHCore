package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import com.onehumanawa.cfhcore.content.gui.menu.client.loading.LoadingScreens;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ReceivingLevelScreen.class})
public abstract class ReceivingLevelScreenMixin extends Screen {
   protected ReceivingLevelScreenMixin(Component title) {
      super(title);
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cfhgui$render(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (LoadingScreens.active()) {
         ci.cancel();
         LoadingScreens.render(this, g, mouseX, mouseY, partialTick, Component.translatable("multiplayer.downloadingTerrain"), (Component)null, -1.0F, (StoringChunkProgressListener)null, List.of());
      }
   }
}
