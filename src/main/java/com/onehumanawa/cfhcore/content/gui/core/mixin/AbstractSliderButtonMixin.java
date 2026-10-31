package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ReskinDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin({AbstractSliderButton.class})
public abstract class AbstractSliderButtonMixin extends AbstractWidget {
   @Shadow
   protected double value;

   protected AbstractSliderButtonMixin(int x, int y, int width, int height, Component message) {
      super(x, y, width, height, message);
   }

   @WrapOperation(
      method = {"renderWidget"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"
)},
      require = 0
   )
   private void cfhgui$sprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h, Operation<Void> original) {
      if (!Reskin.active()) {
         original.call(new Object[]{g, sprite, x, y, w, h});
      } else {
         boolean handle = sprite.getPath().startsWith("widget/slider_handle");
         float hover = ReskinDraw.hover(this);
         if (Theme.current().isVanilla()) {
            if (!this.active) {
               original.call(new Object[]{g, sprite, x, y, w, h});
            } else {
               if (handle) {
                  CFHGuiDraw.vanillaFace(g, CFHGuiDraw.SLIDER_HANDLE, CFHGuiDraw.SLIDER_HANDLE_HIGHLIGHTED, (ResourceLocation)null, x, y, w, h, hover, true, this.alpha);
               } else {
                  CFHGuiDraw.vanillaFace(g, CFHGuiDraw.SLIDER, CFHGuiDraw.SLIDER_HIGHLIGHTED, (ResourceLocation)null, x, y, w, h, hover, true, this.alpha);
               }

            }
         } else {
            if (handle) {
               ReskinDraw.sliderHandle(g, x, y, w, h, hover, this.active);
            } else {
               ReskinDraw.sliderTrack(g, x, y, w, h, (float)this.value, hover, this.active);
            }

         }
      }
   }

   @ModifyArg(
      method = {"renderWidget"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/components/AbstractSliderButton;renderScrollingString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;II)V"
),
      index = 3,
      require = 0
   )
   private int cfhgui$textColor(int color) {
      if (!Reskin.dark()) {
         return color;
      } else {
         int rgb = color & 16777215;
         int a = color & -16777216;
         if (rgb == 16777215) {
            return a | Theme.current().palette().text() & 16777215;
         } else {
            return rgb == 10526880 ? a | Theme.current().palette().textDim() & 16777215 : color;
         }
      }
   }
}
