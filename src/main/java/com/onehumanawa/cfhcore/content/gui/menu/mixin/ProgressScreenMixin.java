package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import com.onehumanawa.cfhcore.content.gui.menu.client.loading.LoadingScreens;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ProgressScreen.class})
public abstract class ProgressScreenMixin extends Screen {
   @Shadow
   private @Nullable Component header;
   @Shadow
   private @Nullable Component stage;
   @Shadow
   private int progress;
   @Shadow
   private boolean stop;

   protected ProgressScreenMixin(Component title) {
      super(title);
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cfhgui$render(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (LoadingScreens.active() && !this.stop) {
         ci.cancel();
         Component title = (Component)(this.header != null && !this.header.getString().isBlank() ? this.header : Component.translatable("cfhgui.loading.generic"));
         LoadingScreens.render(this, g, mouseX, mouseY, partialTick, title, this.stage, this.progress > 0 ? (float)this.progress / 100.0F : -1.0F, (StoringChunkProgressListener)null, List.of());
      }
   }
}
