package com.onehumanawa.cfhcore.content.gui.core.compat.xaero.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.onehumanawa.cfhcore.content.gui.core.compat.xaero.XaeroScreenDraw;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
   targets = {"xaero.lib.client.gui.widget.dropdown.DropDownWidget"},
   remap = false
)
public abstract class XaeroDropDownWidgetMixin extends AbstractWidget {
   @Shadow
   private boolean closed;
   @Unique
   private boolean cfhgui$lit;
   @Unique
   private int cfhgui$edge;
   @Unique
   private int cfhgui$edgeX;
   @Unique
   private int cfhgui$edgeTop;
   @Unique
   private byte cfhgui$kind;

   protected XaeroDropDownWidgetMixin(int x, int y, int width, int height, Component message) {
      super(x, y, width, height, message);
   }

   @Unique
   private boolean cfhgui$menu() {
      if (this.cfhgui$kind == 0) {
         this.cfhgui$kind = (byte)(this.getClass().getName().contains("RightClick") ? 2 : 1);
      }

      return this.cfhgui$kind == 2;
   }

   @Inject(
      method = {"drawMenu"},
      at = {@At("HEAD")},
      require = 0
   )
   private void cfhgui$begin(CallbackInfo ci) {
      this.cfhgui$edge = 0;
   }

   @WrapOperation(
      method = {"drawSlot"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V",
   remap = true
)},
      remap = false,
      require = 0
   )
   private void cfhgui$row(GuiGraphics g, int x0, int y0, int x1, int y1, int color, Operation<Void> original) {
      if (!Reskin.dark()) {
         original.call(new Object[]{g, x0, y0, x1, y1, color});
      } else {
         this.cfhgui$lit = XaeroScreenDraw.dropdownLit(color);
         int fill = XaeroScreenDraw.dropdownRow(color);
         if (this.closed) {
            CFHGuiDraw.pixelRound(g, x0, y0, x1 - x0 + 1, y1 - y0 + 1, fill, 2);
         } else {
            original.call(new Object[]{g, x0, y0, x1, y1, fill});
         }

      }
   }

   @WrapOperation(
      method = {"drawSlot"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;hLine(IIII)V",
   remap = true
)},
      remap = false,
      require = 0
   )
   private void cfhgui$rule(GuiGraphics g, int x0, int x1, int y, int color, Operation<Void> original) {
      if (!Reskin.dark()) {
         original.call(new Object[]{g, x0, x1, y, color});
      }

   }

   @WrapOperation(
      method = {"drawSlot"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V",
   remap = true
)},
      remap = false,
      require = 0
   )
   private void cfhgui$label(GuiGraphics g, Font font, Component text, int centerX, int y, int color, Operation<Void> original) {
      if (!Reskin.dark()) {
         original.call(new Object[]{g, font, text, centerX, y, color});
      } else {
         XaeroScreenDraw.dropdownLabel(g, font, text, centerX, y, color, this.closed || this.cfhgui$lit, this.cfhgui$menu());
      }

   }

   @WrapOperation(
      method = {"drawMenu"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;vLine(IIII)V",
   remap = true
)},
      remap = false,
      require = 0
   )
   private void cfhgui$side(GuiGraphics g, int x, int y0, int y1, int color, Operation<Void> original) {
      if (!Reskin.dark()) {
         original.call(new Object[]{g, x, y0, y1, color});
      } else {
         int edge = XaeroScreenDraw.dropdownEdge(color, this.isHovered(), this.cfhgui$menu());
         if (this.cfhgui$edge++ == 0) {
            this.cfhgui$edgeX = x;
            this.cfhgui$edgeTop = y0;
         } else if (this.closed) {
            CFHGuiDraw.outline(g, this.cfhgui$edgeX, y0, x - this.cfhgui$edgeX + 1, y1 - y0 + 1, edge, 2);
            return;
         }

         if (!this.closed) {
            original.call(new Object[]{g, x, y0, y1, edge});
         }

      }
   }

   @WrapOperation(
      method = {"drawMenu"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;hLine(IIII)V",
   remap = true
)},
      remap = false,
      require = 0
   )
   private void cfhgui$edge(GuiGraphics g, int x0, int x1, int y, int color, Operation<Void> original) {
      if (!Reskin.dark()) {
         original.call(new Object[]{g, x0, x1, y, color});
      } else if (!this.closed) {
         original.call(new Object[]{g, x0, x1, y, XaeroScreenDraw.dropdownEdge(color, this.isHovered(), this.cfhgui$menu())});
         if (this.cfhgui$edge++ >= 3) {
            XaeroScreenDraw.dropdownShadow(g, x0, this.cfhgui$edgeTop, x1, y);
         }

      }
   }
}
