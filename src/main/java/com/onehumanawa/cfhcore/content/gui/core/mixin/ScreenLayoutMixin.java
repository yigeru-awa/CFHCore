package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutBackground;
import com.onehumanawa.cfhcore.content.gui.core.layout.editor.EditorOverlay;
import com.onehumanawa.cfhcore.content.gui.core.layout.editor.LayoutEditor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(
   value = {Screen.class},
   priority = 1500
)
public abstract class ScreenLayoutMixin {
   @Shadow
   @Final
   protected static PanoramaRenderer PANORAMA;

   @Inject(
      method = {"renderWithTooltip"},
      at = {@At("HEAD")},
      require = 0
   )
   private void cfhgui$beginFrame(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      LayoutBackground.beginFrame((Screen)(Object)this);
   }

   @ModifyArgs(
      method = {"renderWithTooltip"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
),
      require = 0
   )
   private void cfhgui$editorMouse(Args args) {
      EditorOverlay ed = LayoutEditor.overlay();
      if (ed != null && ed.screen() == (Screen)(Object)this && ed.hidesScreenMouse()) {
         args.set(1, -9999);
         args.set(2, -9999);
      }

   }

   @WrapOperation(
      method = {"render"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/screens/Screen;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
)},
      require = 0
   )
   private void cfhgui$wrapBackground(Screen screen, GuiGraphics g, int mouseX, int mouseY, float partialTick, Operation<Void> original) {
      if (!LayoutBackground.render(screen, g, partialTick, PANORAMA)) {
         original.call(new Object[]{screen, g, mouseX, mouseY, partialTick});
      }

   }

   @Inject(
      method = {"renderBackground"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void cfhgui$directBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (LayoutBackground.render((Screen)(Object)this, g, partialTick, PANORAMA)) {
         ci.cancel();
      }

   }
}
