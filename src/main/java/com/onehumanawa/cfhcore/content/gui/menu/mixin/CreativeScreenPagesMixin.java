package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.onehumanawa.cfhcore.content.gui.menu.client.creative.CFHGuiCreativeScreen;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({CreativeModeInventoryScreen.class})
public abstract class CreativeScreenPagesMixin {
   @WrapOperation(
      method = {"render"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"
)},
      require = 0
   )
   private int cfhgui_menu$noPageCount(GuiGraphics g, Font font, FormattedCharSequence text, int x, int y, int color, Operation<Integer> original) {
      return (Object)this instanceof CFHGuiCreativeScreen ? 0 : (Integer)original.call(new Object[]{g, font, text, x, y, color});
   }
}
