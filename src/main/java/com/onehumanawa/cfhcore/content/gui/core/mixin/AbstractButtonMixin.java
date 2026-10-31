package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ReskinDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractButton.class})
public abstract class AbstractButtonMixin extends AbstractWidget {
   @Unique
   private boolean cfhgui$pushed;

   protected AbstractButtonMixin(int x, int y, int width, int height, Component message) {
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
   private void cfhgui$face(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h, Operation<Void> original) {
      if (!Reskin.active()) {
         original.call(new Object[]{g, sprite, x, y, w, h});
      } else {
         float hover = ReskinDraw.hover(this);
         float press = ReskinDraw.press(this);
         int dy = Math.round(press);
         if (dy != 0 && !this.cfhgui$pushed) {
            g.pose().pushPose();
            g.pose().translate(0.0F, (float)dy, 0.0F);
            this.cfhgui$pushed = true;
         }

         if (!Theme.current().isVanilla()) {
            ReskinDraw.button(g, this, x, y, w, h, hover, press);
         } else if (this.active && (CFHGuiDraw.BUTTON.equals(sprite) || CFHGuiDraw.BUTTON_HIGHLIGHTED.equals(sprite))) {
            CFHGuiDraw.vanillaFace(g, CFHGuiDraw.BUTTON, CFHGuiDraw.BUTTON_HIGHLIGHTED, (ResourceLocation)null, x, y, w, h, hover, true, this.alpha);
         } else {
            original.call(new Object[]{g, sprite, x, y, w, h});
         }
      }
   }

   @Inject(
      method = {"renderWidget"},
      at = {@At("TAIL")},
      require = 0
   )
   private void cfhgui$tail(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (this.cfhgui$pushed) {
         this.cfhgui$pushed = false;
         g.pose().popPose();
      }

   }

   @ModifyArg(
      method = {"renderWidget"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/components/AbstractButton;renderString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V"
),
      index = 2,
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
