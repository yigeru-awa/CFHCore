package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.onehumanawa.cfhcore.content.gui.core.client.CompactAutoScale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({Minecraft.class})
public abstract class GuiScaleMixin {
   @Shadow
   @Final
   public Options options;

   @ModifyExpressionValue(
      method = {"resizeDisplay"},
      at = {@At(
   value = "INVOKE",
   target = "Lcom/mojang/blaze3d/platform/Window;calculateScale(IZ)I"
)}
   )
   private int cfhgui$compactAutoScale(int scale) {
      return CompactAutoScale.apply(scale, this.options);
   }
}
