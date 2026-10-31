package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.ColorPickerPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class CFHGuiColorField extends CFHGuiWidget {
   private static final int SWATCH = 20;
   private static final int GAP = 4;
   private int color;
   private final IntConsumer onChange;
   private final CFHGuiTextField hex;
   private boolean pickerOpen;

   public CFHGuiColorField(int x, int y, int width, int color, IntConsumer onChange) {
      super(x, y, width, 20, Component.translatable("cfhgui.color"));
      this.color = color | -16777216;
      this.onChange = onChange;
      this.hex = new CFHGuiTextField(x + 20 + 4, y, width - 20 - 4, Component.translatable("cfhgui.color"));
      this.hex.setValue(Colors.toHex(this.color));
      this.hex.maxLength(9);
      this.hex.onChange((s) -> {
         int c = Colors.fromHex(s, Integer.MIN_VALUE);
         this.hex.setInvalid(c == Integer.MIN_VALUE);
         if (c != Integer.MIN_VALUE && (c | -16777216) != this.color) {
            this.color = c | -16777216;
            if (onChange != null) {
               onChange.accept(this.color);
            }
         }

      });
   }

   public int color() {
      return this.color;
   }

   public void setColor(int argb) {
      this.color = argb | -16777216;
      this.hex.setValue(Colors.toHex(this.color));
      this.hex.setInvalid(false);
   }

   public CFHGuiTextField hexField() {
      return this.hex;
   }

   private boolean overSwatch(double mx, double my) {
      return mx >= (double)this.getX() && mx < (double)(this.getX() + 20) && my >= (double)this.getY() && my < (double)(this.getY() + this.getHeight());
   }

   private void openPicker() {
      if (!this.pickerOpen) {
         this.pickerOpen = true;
         Popups.open(new ColorPickerPopup(this.getX(), this.getY() + this.getHeight() + 2, this.color, (c) -> {
            this.setColor(c);
            if (this.onChange != null) {
               this.onChange.accept(this.color);
            }

         }) {
            public void onClose() {
               CFHGuiColorField.this.pickerOpen = false;
            }
         });
      }
   }

   public void onClick(double mouseX, double mouseY) {
      super.onClick(mouseX, mouseY);
      if (this.overSwatch(mouseX, mouseY)) {
         this.openPicker();
      }

   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.active && this.visible) {
         if (this.hex.mouseClicked(mouseX, mouseY, button)) {
            this.hex.setFocused(true);
            return true;
         } else if (this.overSwatch(mouseX, mouseY) && button == 0) {
            this.hex.setFocused(false);
            return super.mouseClicked(mouseX, mouseY, button);
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.active && this.visible) {
         if (this.hex.isFocused() && this.hex.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
         } else if (keyCode != 257 && keyCode != 335 && (keyCode != 32 || this.hex.isFocused())) {
            return false;
         } else {
            this.openPicker();
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean charTyped(char c, int modifiers) {
      return this.hex.isFocused() && this.hex.charTyped(c, modifiers);
   }

   public void setFocused(boolean focused) {
      super.setFocused(focused);
      if (!focused) {
         this.hex.setFocused(false);
      }

   }

   private void place() {
      this.hex.setX(this.getX() + 20 + 4);
      this.hex.setY(this.getY());
      this.hex.setWidth(Math.max(1, this.getWidth() - 20 - 4));
      this.hex.setHeight(this.getHeight());
      this.hex.active = this.active;
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      float a = this.effectiveAlpha();
      this.place();
      int x = this.getX();
      int y = this.getY() + this.enterOffset();
      int h = this.getHeight();
      boolean over = this.isHovered() && this.overSwatch((double)mouseX, (double)mouseY);
      float lift = Math.max(over ? this.hover() : 0.0F, this.pickerOpen ? 1.0F : 0.0F);
      if (!t.isVanilla()) {
         CFHGuiDraw.shadow(g, x, y, 20, h, 0.3F * a);
      }

      CFHGuiDraw.pixelRound(g, x, y, 20, h, Colors.scaleAlpha(this.color, a), t.radius());
      CFHGuiDraw.hline(g, x + 2, y + 1, 16, Colors.scaleAlpha(Colors.withAlpha(16777215, 48), a));
      int border = t.isVanilla() ? Colors.lerp(-16777216, -1, lift) : Colors.lerp(p.borderStrong(), p.text(), lift);
      CFHGuiDraw.outline(g, x, y, 20, h, Colors.scaleAlpha(border, a), t.radius());
      if (this.isFocused() && !this.hex.isFocused()) {
         CFHGuiDraw.focusRing(g, x, y, 20, h, a);
      }

      this.hex.setAlpha(a);
      this.hex.render(g, mouseX, mouseY, partialTick);
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.renderDark(g, mouseX, mouseY, partialTick);
   }
}
