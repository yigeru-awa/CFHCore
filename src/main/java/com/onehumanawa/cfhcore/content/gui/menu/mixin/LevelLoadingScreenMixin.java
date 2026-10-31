package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import com.onehumanawa.cfhcore.content.gui.menu.client.loading.LoadingScreens;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelLoadingScreen.class})
public abstract class LevelLoadingScreenMixin extends Screen {
   @Shadow
   @Final
   private StoringChunkProgressListener progressListener;
   @Shadow
   private long lastNarration;

   protected LevelLoadingScreenMixin(Component title) {
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
         long now = Util.getMillis();
         if (now - this.lastNarration > 2000L) {
            this.lastNarration = now;
            this.triggerImmediateNarration(true);
         }

         LoadingScreens.render(this, g, mouseX, mouseY, partialTick, Component.translatable("cfhgui.loading.world"), Component.translatable("cfhgui.loading.spawn"), (float)this.progressListener.getProgress() / 100.0F, this.progressListener, List.of());
      }
   }
}
