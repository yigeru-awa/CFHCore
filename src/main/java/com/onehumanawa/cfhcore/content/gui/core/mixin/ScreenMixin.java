package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.onehumanawa.cfhcore.content.gui.core.client.CFHGuiClient;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutApplier;
import com.onehumanawa.cfhcore.content.gui.core.screen.ContainerMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Screen.class})
public abstract class ScreenMixin implements LayoutApplier.ScreenAccess {
   @Unique
   private boolean cfhgui$inInit;
   @Unique
   private boolean cfhgui$inResize;
   @Unique
   private boolean cfhgui$builtInResize;

   @Shadow
   protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T var1);

   @Shadow
   protected abstract void removeWidget(GuiEventListener var1);

   @Shadow
   protected abstract void rebuildWidgets();

   public <T extends GuiEventListener & Renderable & NarratableEntry> T cfhgui$add(T widget) {
      return (T)(Object)(Object)this.addRenderableWidget(widget);
   }

   public void cfhgui$remove(GuiEventListener widget) {
      this.removeWidget(widget);
   }

   public void cfhgui$rebuild() {
      this.rebuildWidgets();
   }

   @Inject(
      method = {"init(Lnet/minecraft/client/Minecraft;II)V"},
      at = {@At("HEAD")}
   )
   private void cfhgui$initHead(Minecraft mc, int w, int h, CallbackInfo ci) {
      this.cfhgui$inInit = true;
   }

   @Inject(
      method = {"init(Lnet/minecraft/client/Minecraft;II)V"},
      at = {@At("TAIL")}
   )
   private void cfhgui$initTail(Minecraft mc, int w, int h, CallbackInfo ci) {
      this.cfhgui$inInit = false;
      if (this.cfhgui$inResize) {
         this.cfhgui$builtInResize = true;
      }

      CFHGuiClient.onScreenInit((Screen)(Object)(Object)this);
   }

   @Inject(
      method = {"rebuildWidgets"},
      at = {@At("TAIL")}
   )
   private void cfhgui$rebuilt(CallbackInfo ci) {
      if (!this.cfhgui$inInit) {
         if (this.cfhgui$inResize) {
            this.cfhgui$builtInResize = true;
         }

         CFHGuiClient.onScreenInit((Screen)(Object)(Object)this);
      }
   }

   @Inject(
      method = {"resize"},
      at = {@At("HEAD")},
      require = 0
   )
   private void cfhgui$resizeHead(Minecraft mc, int w, int h, CallbackInfo ci) {
      this.cfhgui$inResize = true;
      this.cfhgui$builtInResize = false;
   }

   @Inject(
      method = {"resize"},
      at = {@At("TAIL")},
      require = 0
   )
   private void cfhgui$resizeTail(Minecraft mc, int w, int h, CallbackInfo ci) {
      this.cfhgui$inResize = false;
      if (!this.cfhgui$builtInResize) {
         CFHGuiClient.onScreenRepositioned((Screen)(Object)(Object)this);
      }

   }

   @Inject(
      method = {"renderWithTooltip"},
      at = {@At("HEAD")}
   )
   private void cfhgui$renderStarts(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      ContainerMotion.begin((Screen)(Object)(Object)this, g);
   }

   @Inject(
      method = {"renderWithTooltip"},
      at = {@At("TAIL")}
   )
   private void cfhgui$rendered(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      ContainerMotion.end((Screen)(Object)(Object)this, g);
      CFHGuiClient.onScreenRendered((Screen)(Object)(Object)this, g, mouseX, mouseY, partialTick);
   }
}
