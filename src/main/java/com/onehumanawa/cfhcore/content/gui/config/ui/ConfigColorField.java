package com.onehumanawa.cfhcore.content.gui.config.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.ColorPickerPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class ConfigColorField extends CFHGuiWidget {
   private static final int SWATCH = 20;
   private int color;
   private final IntConsumer onChange;
   private final ConfigTextField hex;

   public ConfigColorField(int x, int y, int width, int color, IntConsumer onChange) {
      super(x, y, width, 20, Component.translatable("cfhgui.color"));
      this.color = color | -16777216;
      this.onChange = onChange;
      this.hex = new ConfigTextField(x + 20 + 4, y, Math.max(40, width - 20 - 4), Component.translatable("cfhgui.color"));
      this.hex.setValue(Colors.toHex(this.color));
      this.hex.maxLength(9);
      this.hex.onChange((s) -> {
         int c = Colors.fromHex(s, Integer.MIN_VALUE);
         this.hex.setInvalid(c == Integer.MIN_VALUE);
         if (c != Integer.MIN_VALUE && c != this.color) {
            this.color = c;
            if (onChange != null) {
               onChange.accept(c);
            }
         }

      });
   }

   public int color() {
      return this.color;
   }

   public void setColor(int argb) {
      this.color = argb | -16777216;
      if (!this.hex.getValue().equalsIgnoreCase(Colors.toHex(this.color))) {
         this.hex.setValue(Colors.toHex(this.color));
      }

      this.hex.setInvalid(false);
   }

   private void layout() {
      this.hex.setX(this.getX() + 20 + 4);
      this.hex.setY(this.getY());
      this.hex.setWidth(Math.max(40, this.getWidth() - 20 - 4));
   }

   private void openPicker() {
      Popups.open(new ColorPickerPopup(this.getX(), this.getY() + 22, this.color, (c) -> {
         this.setColor(c);
         if (this.onChange != null) {
            this.onChange.accept(c);
         }

      }));
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.active && this.visible) {
         this.layout();
         if (this.hex.mouseClicked(mouseX, mouseY, button)) {
            return true;
         } else if (button == 0 && this.contains(mouseX, mouseY) && mouseX < (double)(this.getX() + 20 + 2)) {
            this.hex.setFocused(false);
            this.openPicker();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.hex.box().isFocused()) {
         return this.hex.keyPressed(keyCode, scanCode, modifiers);
      } else if (!this.active || !this.visible || keyCode != 257 && keyCode != 32) {
         return false;
      } else {
         this.openPicker();
         return true;
      }
   }

   public boolean charTyped(char c, int modifiers) {
      return this.hex.charTyped(c, modifiers);
   }

   public void setFocused(boolean focused) {
      super.setFocused(focused);
      if (!focused) {
         this.hex.setFocused(false);
      }

   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      this.layout();
      CFHGuiDraw.pixelRound(g, this.getX(), this.getY(), 20, 20, this.color, t.radius());
      CFHGuiDraw.outline(g, this.getX(), this.getY(), 20, 20, Colors.lerp(p.borderStrong(), p.text(), this.hover()), t.radius());
      CFHGuiDraw.focusRing(g, this.getX(), this.getY(), 20, 20, (this.isFocused() && !this.hex.box().isFocused() ? 1.0F : 0.0F) * this.effectiveAlpha());
      this.hex.render(g, mouseX, mouseY, partialTick);
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.layout();
      CFHGuiDraw.pixelRound(g, this.getX(), this.getY(), 20, 20, this.color, 0);
      CFHGuiDraw.outline(g, this.getX(), this.getY(), 20, 20, !(this.hover() > 0.5F) && !this.isFocused() ? -16777216 : -1, 0);
      this.hex.render(g, mouseX, mouseY, partialTick);
   }
}
