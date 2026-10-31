package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ReskinDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TabButton.class})
public abstract class TabButtonMixin extends AbstractWidget {
   @Shadow
   public abstract boolean isSelected();

   @Shadow
   public abstract void renderString(GuiGraphics var1, Font var2, int var3);

   protected TabButtonMixin(int x, int y, int width, int height, Component message) {
      super(x, y, width, height, message);
   }

   @Inject(
      method = {"renderWidget"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void cfhgui$render(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (Reskin.dark()) {
         ci.cancel();
         Palette p = Theme.current().palette();
         boolean selected = this.isSelected();
         float hover = ReskinDraw.hover(this);
         ReskinDraw.tab(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), selected, selected ? 0.0F : hover, this.isFocused());
         int color = !this.active ? p.textDim() : (selected ? p.text() : Colors.lerp(p.textMuted(), p.text(), hover));
         this.renderString(g, Minecraft.getInstance().font, color | -16777216);
      }
   }
}
