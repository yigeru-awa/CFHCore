package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ReskinDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractSelectionList.class})
public abstract class AbstractSelectionListMixin extends AbstractContainerWidget {
   @Shadow
   @Final
   protected Minecraft minecraft;

   protected AbstractSelectionListMixin(int x, int y, int width, int height, Component message) {
      super(x, y, width, height, message);
   }

   @Inject(
      method = {"renderListBackground"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void cfhgui$listBackground(GuiGraphics g, CallbackInfo ci) {
      if (Reskin.dark()) {
         ci.cancel();
         ReskinDraw.listBackground(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.minecraft.level != null);
      }
   }

   @Inject(
      method = {"renderListSeparators"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void cfhgui$listSeparators(GuiGraphics g, CallbackInfo ci) {
      if (Reskin.dark()) {
         ci.cancel();
         ReskinDraw.listSeparators(g, this.getX(), this.getY(), this.getWidth(), this.getBottom(), this.minecraft.level != null);
      }
   }

   @Inject(
      method = {"renderSelection"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void cfhgui$selection(GuiGraphics g, int top, int width, int height, int outerColor, int innerColor, CallbackInfo ci) {
      if (Reskin.dark()) {
         ci.cancel();
         int x = this.getX() + (this.getWidth() - width) / 2;
         ReskinDraw.selection(g, x, top - 2, width, height + 4, (outerColor & 16777215) == 16777215);
      }
   }

   @WrapOperation(
      method = {"renderWidget"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"
)},
      require = 0
   )
   private void cfhgui$scroller(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h, Operation<Void> original) {
      if (!Reskin.dark()) {
         original.call(new Object[]{g, sprite, x, y, w, h});
      } else {
         String path = sprite.getPath();
         if (path.equals("widget/scroller_background")) {
            ReskinDraw.scrollTrack(g, x, y, w, h);
         } else if (path.equals("widget/scroller")) {
            ReskinDraw.scrollThumb(g, x, y, w, h);
         } else {
            original.call(new Object[]{g, sprite, x, y, w, h});
         }

      }
   }
}
