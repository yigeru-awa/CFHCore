package com.onehumanawa.cfhcore.content.gui.config.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class ConfigTextField extends CFHGuiWidget {
   public static final int HEIGHT = 20;
   private static final int PAD_X = 6;
   private static final int PAD_Y = 5;
   private final EditBox box;
   private @Nullable Icon icon;
   private boolean clearButton;
   private @Nullable Component placeholder;
   private boolean invalid;
   private @Nullable Runnable onEnter;
   private @Nullable Runnable onEscape;

   public ConfigTextField(int x, int y, int width, int height, Component narration) {
      super(x, y, width, height, narration);
      this.box = new EditBox(CFHGuiDraw.font(), 0, 0, 10, 10, narration);
      this.box.setBordered(false);
      this.box.setMaxLength(256);
      this.silent();
      this.layoutBox();
   }

   public ConfigTextField(int x, int y, int width, Component narration) {
      this(x, y, width, 20, narration);
   }

   public ConfigTextField placeholder(@Nullable Component text) {
      this.placeholder = text;
      return this;
   }

   public ConfigTextField icon(@Nullable Icon icon) {
      this.icon = icon;
      this.layoutBox();
      return this;
   }

   public ConfigTextField clearButton(boolean on) {
      this.clearButton = on;
      this.layoutBox();
      return this;
   }

   public ConfigTextField onChange(Consumer<String> responder) {
      this.box.setResponder(responder);
      return this;
   }

   public ConfigTextField onEnter(@Nullable Runnable r) {
      this.onEnter = r;
      return this;
   }

   public ConfigTextField onEscape(@Nullable Runnable r) {
      this.onEscape = r;
      return this;
   }

   public ConfigTextField maxLength(int n) {
      this.box.setMaxLength(n);
      return this;
   }

   public ConfigTextField text(@Nullable String value) {
      this.box.setValue(value == null ? "" : value);
      return this;
   }

   public String getValue() {
      return this.box.getValue();
   }

   public void setValue(@Nullable String value) {
      this.box.setValue(value == null ? "" : value);
   }

   public void setInvalid(boolean invalid) {
      this.invalid = invalid;
   }

   public void setEditable(boolean editable) {
      this.box.setEditable(editable);
   }

   public EditBox box() {
      return this.box;
   }

   private void layoutBox() {
      int left = this.icon == null ? 0 : 16;
      int right = this.clearButton ? 14 : 0;
      this.box.setX(this.getX() + 6 + left);
      this.box.setY(this.getY() + 5);
      this.box.setWidth(Math.max(1, this.getWidth() - 12 - left - right));
      this.box.setHeight(Math.max(1, this.getHeight() - 10));
   }

   public void setX(int x) {
      super.setX(x);
      this.layoutBox();
   }

   public void setY(int y) {
      super.setY(y);
      this.layoutBox();
   }

   public void setWidth(int w) {
      super.setWidth(w);
      this.layoutBox();
   }

   public void setHeight(int h) {
      super.setHeight(h);
      this.layoutBox();
   }

   private boolean onClear(double mx, double my) {
      return this.clearButton && !this.box.getValue().isEmpty() && mx >= (double)(this.getX() + this.getWidth() - 16) && mx < (double)(this.getX() + this.getWidth()) && my >= (double)this.getY() && my < (double)(this.getY() + this.getHeight());
   }

   public void setFocused(boolean focused) {
      super.setFocused(focused);
      this.box.setFocused(focused);
   }

   public void onClick(double mouseX, double mouseY) {
      super.onClick(mouseX, mouseY);
      this.layoutBox();
      if (this.onClear(mouseX, mouseY)) {
         this.box.setValue("");
         CFHGuiSounds.tick();
      } else {
         if (!this.box.isFocused()) {
            this.box.setFocused(true);
         }

         if (mouseX >= (double)this.box.getX() && mouseX < (double)(this.box.getX() + this.box.getWidth())) {
            this.box.mouseClicked(mouseX, Math.max((double)this.box.getY(), Math.min(mouseY, (double)(this.box.getY() + this.box.getHeight() - 1))), 0);
         } else {
            this.box.moveCursorToEnd(false);
         }

      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.active && this.visible && this.box.isFocused()) {
         if ((keyCode == 257 || keyCode == 335) && this.onEnter != null) {
            this.onEnter.run();
            return true;
         } else if (keyCode == 256 && this.onEscape != null) {
            this.onEscape.run();
            return true;
         } else {
            return this.box.keyPressed(keyCode, scanCode, modifiers);
         }
      } else {
         return false;
      }
   }

   public boolean charTyped(char c, int modifiers) {
      return this.active && this.visible && this.box.isFocused() && this.box.charTyped(c, modifiers);
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      float a = this.effectiveAlpha();
      float foc = this.box.isFocused() ? 1.0F : 0.0F;
      float hov = this.hover();
      int x = this.getX();
      int y = this.getY();
      int w = this.getWidth();
      int h = this.getHeight();
      int face = Colors.lerp(Colors.lerp(p.bg2(), p.surface(), hov), p.surface(), foc);
      int edge = this.invalid ? p.danger() : Colors.lerp(Colors.lerp(p.border(), p.borderStrong(), hov), p.accent(), foc);
      int line = Theme.setIn() ? t.fieldLine(edge) : 0;
      int fill = t.field(face);
      int border = t.fieldEdge(edge);
      CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(fill, a), t.radius());
      CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(border, a), t.radius());
      if (line != 0) {
         CFHGuiDraw.rect(g, x + t.radius(), y + h - 1, w - t.radius() * 2, 1, Colors.scaleAlpha(line, a));
      }

      this.box.setTextColor(this.active ? p.text() : p.textDim());
      this.drawInner(g, mouseX, mouseY, partialTick, p, false);
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Palette p = Theme.current().palette();
      int x = this.getX();
      int y = this.getY();
      int w = this.getWidth();
      int h = this.getHeight();
      CFHGuiDraw.vanillaTextField(g, x, y, w, h, this.box.isFocused());
      if (this.invalid) {
         CFHGuiDraw.outline(g, x, y, w, h, p.danger(), 0);
      }

      this.box.setTextColor(this.active ? -2039584 : -6250336);
      this.drawInner(g, mouseX, mouseY, partialTick, p, true);
   }

   private void drawInner(GuiGraphics g, int mouseX, int mouseY, float partialTick, Palette p, boolean vanilla) {
      this.layoutBox();
      int x = this.getX();
      int y = this.getY();
      int w = this.getWidth();
      int h = this.getHeight();
      if (this.icon != null) {
         Icons.draw(g, this.icon, x + 6, y + (h - 12) / 2, 12, this.box.isFocused() ? p.textMuted() : p.textDim());
      }

      if (this.clearButton && !this.box.getValue().isEmpty()) {
         Icons.draw(g, Icon.CLOSE, x + w - 14, y + (h - 8) / 2, 8, this.onClear((double)mouseX, (double)mouseY) ? p.text() : p.textDim());
      }

      if (this.placeholder != null && this.box.getValue().isEmpty() && !this.box.isFocused()) {
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(this.placeholder, this.box.getWidth()), this.box.getX(), this.box.getY(), vanilla ? -8355712 : p.textDim(), false);
      }

      this.box.render(g, mouseX, mouseY, partialTick);
   }
}
