package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.onehumanawa.cfhcore.content.gui.core.screen.ContainerMotion;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ReskinDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Screen.class})
public abstract class ScreenBackgroundMixin {
   @Shadow
   protected Minecraft minecraft;
   @Shadow
   public int width;
   @Shadow
   public int height;
   @Shadow
   @Final
   private static ResourceLocation INWORLD_MENU_BACKGROUND;

   @Unique
   private boolean cfhgui$own() {
      return (Object)(Object)this instanceof CFHGuiScreen;
   }

   @Inject(
      method = {"renderMenuBackground(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void cfhgui$menuBackground(GuiGraphics g, int x, int y, int w, int h, CallbackInfo ci) {
      boolean scene = MenuBackdrop.active();
      if (!this.cfhgui$own() && Reskin.dark()) {
         ci.cancel();
         if (scene) {
            MenuBackdrop.drawVeil(g, x, y, w, h);
         } else {
            ReskinDraw.menuBackground(g, x, y, w, h, this.minecraft.level != null);
         }

      } else {
         if (scene) {
            ci.cancel();
            Screen.renderMenuBackgroundTexture(g, INWORLD_MENU_BACKGROUND, x, y, 0.0F, 0.0F, w, h);
         }

      }
   }

   @Inject(
      method = {"renderTransparentBackground"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void cfhgui$transparentBackground(GuiGraphics g, CallbackInfo ci) {
      if (!this.cfhgui$own()) {
         boolean moving = ContainerMotion.active();
         if (Reskin.dark() || moving) {
            ci.cancel();
            float fade = ContainerMotion.backdropAlpha();
            ContainerMotion.beginBackdrop(g);
            if (Reskin.dark()) {
               ReskinDraw.transparentBackground(g, this.width, this.height, fade);
            } else {
               g.fillGradient(0, 0, this.width, this.height, Colors.scaleAlpha(-1072689136, fade), Colors.scaleAlpha(-804253680, fade));
            }

            ContainerMotion.endBackdrop(g);
         }
      }
   }

   @Inject(
      method = {"renderPanorama"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void cfhgui$panorama(GuiGraphics g, float partialTick, CallbackInfo ci) {
      if (!((Object)(Object)this instanceof TitleScreen)) {
         if (MenuBackdrop.active() && MenuBackdrop.paint(g, this.width, this.height, partialTick)) {
            ci.cancel();
         } else if (!this.cfhgui$own() && Reskin.dark()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"renderBlurredBackground"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void cfhgui$blur(float partialTick, CallbackInfo ci) {
      if (GlStateManager.getBoundFramebuffer() != this.minecraft.getMainRenderTarget().frameBufferId) {
         ci.cancel();
      } else if (!this.cfhgui$own() && Reskin.dark()) {
         if (this.minecraft.level == null || !Theme.current().blurInGame()) {
            ci.cancel();
         }

      }
   }
}
