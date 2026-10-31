package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ReskinDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({Checkbox.class})
public abstract class CheckboxMixin extends AbstractWidget {
   @Shadow
   private boolean selected;

   protected CheckboxMixin(int x, int y, int width, int height, Component message) {
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
   private void cfhgui$box(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h, Operation<Void> original) {
      if (!Reskin.active()) {
         original.call(new Object[]{g, sprite, x, y, w, h});
      } else {
         float hover = ReskinDraw.hover(this);
         if (Theme.current().isVanilla()) {
            CFHGuiDraw.vanillaCheckbox(g, x, y, w, this.selected, this.active ? hover : 0.0F, this.alpha);
         } else {
            ReskinDraw.checkbox(g, x, y, w, this.selected, hover, this.isFocused(), this.active);
         }
      }
   }
}
