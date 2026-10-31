package com.onehumanawa.cfhcore.content.gui.core.compat.xaero.mixin;

import com.onehumanawa.cfhcore.content.gui.core.compat.xaero.XaeroScreenDraw;
import com.onehumanawa.cfhcore.content.gui.core.compat.xaero.XaeroStyle;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
   targets = {"xaero.map.gui.GuiTexturedButton"},
   remap = false
)
public abstract class XaeroTexturedButtonMixin extends AbstractWidget {
   @Shadow
   protected int textureX;
   @Shadow
   protected int textureY;
   @Shadow
   protected int textureW;
   @Shadow
   protected int textureH;
   @Shadow
   protected ResourceLocation texture;

   protected XaeroTexturedButtonMixin(int x, int y, int width, int height, Component message) {
      super(x, y, width, height, message);
   }

   @Inject(
      method = {"renderWidget"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = true,
      require = 0
   )
   private void cfhgui$render(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (XaeroStyle.worldMap() && Reskin.dark() && this.texture != null) {
         try {
            XaeroScreenDraw.iconButton(g, this, this.texture, this.textureX, this.textureY, this.textureW, this.textureH);
         } catch (RuntimeException e) {
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            XaeroStyle.failed(2, "the world map's buttons", e);
            return;
         }

         ci.cancel();
      }
   }
}
