package com.onehumanawa.cfhcore.content.gui.core.layout.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.ColorPickerPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class ColorRow extends CFHGuiWidget {
   private static final int SWATCH = 20;
   private final EditorTextField hex;
   private final Consumer<String> onChange;
   private String value;

   ColorRow(int x, int y, int w, String value, Consumer<String> onChange) {
      super(x, y, w, 20, Component.translatable("cfhgui.color"));
      this.value = value == null ? "" : value;
      this.onChange = onChange;
      this.hex = new EditorTextField(x + 20 + 4, y, w - 20 - 4, Component.translatable("cfhgui.color"));
      this.hex.setMaxLength(9);
      this.hex.text(this.value);
      this.hex.placeholder(EditorText.t("props.color_theme"));
      this.hex.onChange((s) -> {
         String v = s.trim();
         boolean ok = v.isEmpty() || Colors.fromHex(v, Integer.MIN_VALUE) != Integer.MIN_VALUE;
         this.hex.setInvalid(!ok);
         if (ok && !v.equals(this.value)) {
            this.value = v;
            onChange.accept(v);
         }

      });
      this.silent();
   }

   EditorTextField field() {
      return this.hex;
   }

   private int swatchColor() {
      return this.value.isEmpty() ? Theme.current().surface() : Colors.fromHex(this.value, -16777216);
   }

   private void layoutHex() {
      this.hex.setX(this.getX() + 20 + 4 + 6);
      this.hex.setY(this.getY() + 5);
      this.hex.setWidth(Math.max(1, this.getWidth() - 20 - 4 - 12));
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.visible && this.active) {
         this.layoutHex();
         if (this.hex.mouseClicked(mouseX, mouseY, button)) {
            this.hex.setFocused(true);
            return true;
         } else if (button == 0 && mouseX >= (double)this.getX() && mouseX < (double)(this.getX() + 20) && mouseY >= (double)this.getY() && mouseY < (double)(this.getY() + this.getHeight())) {
            this.hex.setFocused(false);
            Popups.open(new ColorPickerPopup(this.getX(), this.getY() + 20 + 2, this.swatchColor(), (c) -> {
               String v = Colors.toHex(c);
               this.hex.text(v);
            }));
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return this.hex.isFocused() && this.hex.keyPressed(keyCode, scanCode, modifiers);
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

   boolean textFocused() {
      return this.hex.isFocused();
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, mouseX, mouseY, partialTick);
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, mouseX, mouseY, partialTick);
   }

   private void draw(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      this.layoutHex();
      int c = this.swatchColor();
      g.fill(this.getX(), this.getY(), this.getX() + 20, this.getY() + 20, -6645094);

      for(int yy = 0; yy < 20; yy += 5) {
         for(int xx = yy / 5 % 2 * 5; xx < 20; xx += 10) {
            g.fill(this.getX() + xx, this.getY() + yy, this.getX() + Math.min(20, xx + 5), this.getY() + Math.min(20, yy + 5), -10855846);
         }
      }

      CFHGuiDraw.pixelRound(g, this.getX(), this.getY(), 20, 20, c, t.radius());
      CFHGuiDraw.outline(g, this.getX(), this.getY(), 20, 20, Colors.lerp(p.borderStrong(), p.text(), this.hover()), t.radius());
      this.hex.render(g, mouseX, mouseY, partialTick);
   }
}
