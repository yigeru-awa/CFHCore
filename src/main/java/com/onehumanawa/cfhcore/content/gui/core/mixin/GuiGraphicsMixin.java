package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.onehumanawa.cfhcore.content.gui.core.screen.ContainerMotion;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.BetterInventoryPalette;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ContainerReskin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GuiGraphics.class})
public abstract class GuiGraphicsMixin {
   @Inject(
      method = {"blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void cfhgui$containerBlit(ResourceLocation atlas, int x, int y, int w, int h, float u, float v, int uw, int vh, int tw, int th, CallbackInfo ci) {
      BetterInventoryPalette.blit(atlas, u, v, uw, vh);
      if (ContainerReskin.replaceBackground((GuiGraphics)(Object)this, atlas, x, y, w, h)) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"blit(Lnet/minecraft/resources/ResourceLocation;IIIFFIIII)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void cfhgui$containerBlitZ(ResourceLocation atlas, int x, int y, int blitOffset, float u, float v, int w, int h, int tw, int th, CallbackInfo ci) {
      if (ContainerReskin.replaceBackground((GuiGraphics)(Object)this, atlas, x, y, w, h)) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"blitSprite(Lnet/minecraft/resources/ResourceLocation;IIIII)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void cfhgui$containerSprite(ResourceLocation sprite, int x, int y, int blitOffset, int w, int h, CallbackInfo ci) {
      if (ContainerReskin.replaceSprite((GuiGraphics)(Object)this, sprite, x, y, w, h)) {
         ci.cancel();
      }

   }

   @ModifyVariable(
      method = {"drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I", "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 2,
      require = 0
   )
   private int cfhgui$containerLabel(int color) {
      return ContainerReskin.labelColor(BetterInventoryPalette.text(color));
   }

   @ModifyVariable(
      method = {"fill(Lnet/minecraft/client/renderer/RenderType;IIIIII)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 5,
      require = 0
   )
   private int cfhgui$modFill(int color) {
      return BetterInventoryPalette.fill(color);
   }

   @ModifyVariable(
      method = {"enableScissor(IIII)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0,
      require = 0
   )
   private int cfhgui$scissorMinX(int minX) {
      return ContainerMotion.scissorX(minX);
   }

   @ModifyVariable(
      method = {"enableScissor(IIII)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 1,
      require = 0
   )
   private int cfhgui$scissorMinY(int minY) {
      return ContainerMotion.scissorY(minY);
   }

   @ModifyVariable(
      method = {"enableScissor(IIII)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 2,
      require = 0
   )
   private int cfhgui$scissorMaxX(int maxX) {
      return ContainerMotion.scissorX(maxX);
   }

   @ModifyVariable(
      method = {"enableScissor(IIII)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 3,
      require = 0
   )
   private int cfhgui$scissorMaxY(int maxY) {
      return ContainerMotion.scissorY(maxY);
   }
}
