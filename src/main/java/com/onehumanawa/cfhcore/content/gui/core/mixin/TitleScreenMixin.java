package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutApplier;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutAware;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutBackground;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutStore;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ReskinDraw;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TitleScreen.class})
public abstract class TitleScreenMixin extends Screen implements LayoutAware {
   @Unique
   private static final String CFHGUI$LOGO = "logo";
   @Shadow
   private float panoramaFade;

   protected TitleScreenMixin(Component title) {
      super(title);
   }

   public List<LayoutAware.Part> layoutParts() {
      return List.of(new LayoutAware.Part("logo", Component.translatable("cfhgui.layout.part.logo"), Rect.of(this.width / 2 - 128, 30, 256, 52)));
   }

   @Unique
   private boolean cfhgui$logoHidden() {
      return LayoutStore.get(LayoutApplier.layoutId(this)).hides("logo");
   }

   @Inject(
      method = {"renderPanorama"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void cfhgui$scene(GuiGraphics g, float partialTick, CallbackInfo ci) {
      if (LayoutBackground.render(this, g, partialTick, PANORAMA)) {
         ci.cancel();
      } else if (MenuBackdrop.active() && MenuBackdrop.paint(g, this.width, this.height, partialTick)) {
         ci.cancel();
         this.renderBlurredBackground(partialTick);
         this.renderMenuBackground(g);
      }
   }

   @Inject(
      method = {"renderPanorama"},
      at = {@At("TAIL")},
      require = 0
   )
   private void cfhgui$veil(GuiGraphics g, float partialTick, CallbackInfo ci) {
      if (Reskin.dark()) {
         ReskinDraw.panoramaVeil(g, this.width, this.height, this.panoramaFade);
      }
   }

   @WrapOperation(
      method = {"render"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/components/LogoRenderer;renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IF)V"
)},
      require = 0
   )
   private void cfhgui$logo(LogoRenderer logo, GuiGraphics g, int width, float alpha, Operation<Void> original) {
      if (!this.cfhgui$logoHidden()) {
         original.call(new Object[]{logo, g, width, alpha});
      }

   }

   @WrapOperation(
      method = {"render"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/components/SplashRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/client/gui/Font;I)V"
)},
      require = 0
   )
   private void cfhgui$splash(SplashRenderer splash, GuiGraphics g, int width, Font font, int color, Operation<Void> original) {
      if (!this.cfhgui$logoHidden()) {
         original.call(new Object[]{splash, g, width, font, color});
      }

   }
}
