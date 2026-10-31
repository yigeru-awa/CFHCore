package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ReskinDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({EditBox.class})
public abstract class EditBoxMixin extends AbstractWidget {
   protected EditBoxMixin(int x, int y, int width, int height, Component message) {
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
   private void cfhgui$frame(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h, Operation<Void> original) {
      if (!Reskin.dark()) {
         original.call(new Object[]{g, sprite, x, y, w, h});
      } else {
         boolean focused = sprite.getPath().endsWith("highlighted");
         ReskinDraw.textField(g, x, y, w, h, focused, ReskinDraw.hover(this), this.active);
      }
   }
}
