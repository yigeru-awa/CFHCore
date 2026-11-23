package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import com.onehumanawa.cfhcore.content.gui.core.client.DevHarness;
import com.onehumanawa.cfhcore.content.gui.menu.client.loading.OverhaulLoading;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LoadingOverlay.class})
public abstract class LoadingOverlayMixin {
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   @Final
   private ReloadInstance reload;
   @Shadow
   @Final
   private Consumer<Optional<Throwable>> onFinish;
   @Shadow
   @Final
   private boolean fadeIn;
   @Shadow
   private float currentProgress;
   @Shadow
   private long fadeOutStart;
   @Shadow
   private long fadeInStart;

   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cfhgui$factory(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (((Object)(Object)this).getClass() == LoadingOverlay.class && OverhaulLoading.active()) {
         long now = Util.getMillis();
         if (this.fadeIn && this.fadeInStart == -1L) {
            this.fadeInStart = now;
         }

         float out = this.fadeOutStart > -1L ? (float)(now - this.fadeOutStart) / 1000.0F : -1.0F;
         float in = this.fadeInStart > -1L ? (float)(now - this.fadeInStart) / 500.0F : -1.0F;
         float alpha;
         if (out >= 1.0F) {
            if (this.minecraft.screen != null) {
               this.minecraft.screen.render(graphics, 0, 0, partialTick);
            }

            alpha = 1.0F - Mth.clamp(out - 1.0F, 0.0F, 1.0F);
         } else if (this.fadeIn) {
            if (this.minecraft.screen != null && in < 1.0F) {
               this.minecraft.screen.render(graphics, mouseX, mouseY, partialTick);
            }

            alpha = Mth.clamp(in, 0.15F, 1.0F);
         } else {
            alpha = 1.0F;
         }

         graphics.flush();
         if (OverhaulLoading.draw(this.reload, alpha)) {
            ci.cancel();
            DevHarness.overlayFrame();
            this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + this.reload.getActualProgress() * 0.05F, 0.0F, 1.0F);
            if (out >= 2.0F) {
               this.minecraft.setOverlay((Overlay)null);
            }

            if (this.fadeOutStart == -1L && this.reload.isDone() && (!this.fadeIn || in >= 2.0F)) {
               try {
                  this.reload.checkExceptions();
                  this.onFinish.accept(Optional.empty());
               } catch (Throwable t) {
                  this.onFinish.accept(Optional.of(t));
               }

               this.fadeOutStart = Util.getMillis();
               if (this.minecraft.screen != null) {
                  this.minecraft.screen.init(this.minecraft, graphics.guiWidth(), graphics.guiHeight());
               }
            }

         }
      }
   }
}
