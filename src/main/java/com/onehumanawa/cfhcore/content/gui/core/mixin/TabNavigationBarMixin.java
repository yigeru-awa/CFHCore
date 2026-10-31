package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ReskinDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.LinearLayout;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TabNavigationBar.class})
public abstract class TabNavigationBarMixin {
   @Shadow
   @Final
   private LinearLayout layout;
   @Shadow
   private int width;
   @Shadow
   @Final
   private ImmutableList<TabButton> tabButtons;

   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void cfhgui$render(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (Reskin.dark()) {
         ci.cancel();
         ReskinDraw.tabBar(g, 0, 0, this.width, this.layout.getY() + this.layout.getHeight());
         UnmodifiableIterator var6 = this.tabButtons.iterator();

         while(var6.hasNext()) {
            TabButton b = (TabButton)var6.next();
            b.render(g, mouseX, mouseY, partialTick);
         }

      }
   }
}
