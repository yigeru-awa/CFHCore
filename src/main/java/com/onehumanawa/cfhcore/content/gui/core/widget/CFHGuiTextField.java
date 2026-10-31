package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class CFHGuiTextField extends EditBox {
   public static final int HEIGHT = 20;
   private static final int PAD_X = 6;
   private static final int ICON_SLOT = 16;
   private static final int CLEAR_SLOT = 12;
   private final Anim focusAnim;
   private final Anim hoverAnim;
   private @Nullable Icon icon;
   private boolean clearButton;
   private @Nullable Component placeholder;
   private boolean invalid;
   private Runnable onEnter;
   private Runnable onEscape;

   public CFHGuiTextField(int x, int y, int width, int height, Component narration) {
      super(CFHGuiDraw.font(), x, y, Math.max(1, width), Math.max(1, height), narration);
      this.focusAnim = new Anim(0.0F, 160, Ease.OUT_CUBIC);
      this.hoverAnim = new Anim(0.0F, 140, Ease.OUT_CUBIC);
      this.setBordered(false);
      this.setMaxLength(256);
      this.setTextColor(Theme.current().text());
   }

   public CFHGuiTextField(int x, int y, int width, Component narration) {
      this(x, y, width, 20, narration);
   }

   public CFHGuiTextField placeholder(Component text) {
      this.placeholder = text;
      return this;
   }

   public CFHGuiTextField icon(@Nullable Icon icon) {
      this.icon = icon;
      return this;
   }

   public CFHGuiTextField clearButton(boolean on) {
      this.clearButton = on;
      return this;
   }

   public CFHGuiTextField onChange(Consumer<String> responder) {
      this.setResponder(responder);
      return this;
   }

   public CFHGuiTextField onEnter(Runnable r) {
      this.onEnter = r;
      return this;
   }

   public CFHGuiTextField onEscape(Runnable r) {
      this.onEscape = r;
      return this;
   }

   public CFHGuiTextField maxLength(int n) {
      this.setMaxLength(n);
      return this;
   }

   public CFHGuiTextField text(String value) {
      this.setValue(value == null ? "" : value);
      return this;
   }

   public void setInvalid(boolean invalid) {
      this.invalid = invalid;
   }

   public boolean isInvalid() {
      return this.invalid;
   }

   public int frameX() {
      return this.getX();
   }

   public int frameY() {
      return this.getY();
   }

   public int frameWidth() {
      return this.getWidth();
   }

   public int frameHeight() {
      return this.getHeight();
   }

   private int leftInset() {
      return 6 + (this.icon == null ? 0 : 16);
   }

   private int rightInset() {
      return 6 + (this.clearButton ? 12 : 0);
   }

   private int textDy() {
      return (this.getHeight() - 9) / 2 + 1;
   }

   public int getInnerWidth() {
      return Math.max(1, this.getWidth() - this.leftInset() - this.rightInset());
   }

   public int getScreenX(int charNum) {
      return super.getScreenX(charNum) + this.leftInset();
   }

   private boolean onClear(double mx, double my) {
      return this.clearButton && !this.getValue().isEmpty() && mx >= (double)(this.getX() + this.getWidth() - 6 - 12) && mx < (double)(this.getX() + this.getWidth()) && my >= (double)this.getY() && my < (double)(this.getY() + this.getHeight());
   }

   public void onClick(double mouseX, double mouseY) {
      super.onClick(mouseX - (double)this.leftInset(), mouseY);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.visible && this.active) {
         if (button == 0 && this.onClear(mouseX, mouseY)) {
            this.setValue("");
            CFHGuiSounds.tick();
            return true;
         } else {
            return super.mouseClicked(mouseX, mouseY, button);
         }
      } else {
         return false;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.isFocused() && this.active) {
         if ((keyCode == 257 || keyCode == 335) && this.onEnter != null) {
            this.onEnter.run();
            return true;
         }

         if (keyCode == 256 && this.onEscape != null) {
            this.onEscape.run();
            return true;
         }
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      if (this.visible) {
         Theme t = Theme.current();
         Palette p = t.palette();
         int x = this.getX();
         int y = this.getY();
         int w = this.getWidth();
         int h = this.getHeight();
         boolean hovered = this.active && this.isHovered() && g.containsPointInScissor(mouseX, mouseY);
         this.hoverAnim.set(hovered);
         this.focusAnim.set(this.isFocused());
         float foc = this.focusAnim.get();
         float hov = this.hoverAnim.get();
         if (t.isVanilla()) {
            CFHGuiDraw.vanillaTextField(g, x, y, w, h, this.isFocused());
            if (this.invalid) {
               CFHGuiDraw.outline(g, x, y, w, h, p.danger(), 0);
            }

            this.setTextColor(this.active ? -2039584 : -6250336);
         } else {
            int fill = Colors.lerp(Colors.lerp(p.bg2(), p.surface(), hov), p.surface(), foc);
            int border = this.invalid ? p.danger() : Colors.lerp(Colors.lerp(p.border(), p.borderStrong(), hov), p.accent(), foc);
            if (!this.active) {
               fill = Colors.withAlpha(p.bg2(), 128);
               border = Colors.withAlpha(p.border(), 128);
            }

            int line = Theme.setIn() ? t.fieldLine(border) : 0;
            fill = t.field(fill);
            border = t.fieldEdge(border);
            CFHGuiDraw.pixelRound(g, x, y, w, h, fill, t.radius());
            CFHGuiDraw.outline(g, x, y, w, h, border, t.radius());
            if (line != 0) {
               CFHGuiDraw.rect(g, x + t.radius(), y + h - 1, w - t.radius() * 2, 1, line);
            }

            this.setTextColor(this.active ? p.text() : p.textDim());
         }

         if (this.icon != null) {
            Icons.draw(g, this.icon, x + 6, y + (h - 12) / 2, 12, Colors.lerp(p.textDim(), p.textMuted(), Math.max(foc, hov)));
         }

         if (this.clearButton && !this.getValue().isEmpty()) {
            boolean over = this.onClear((double)mouseX, (double)mouseY);
            Icons.draw(g, Icon.CLOSE, x + w - 6 - 10, y + (h - 8) / 2, 8, over ? p.text() : p.textDim());
         }

         int dx = this.leftInset();
         int dy = this.textDy();
         g.pose().pushPose();
         g.pose().translate((float)dx, (float)dy, 0.0F);
         if (this.placeholder != null && this.getValue().isEmpty() && !this.isFocused()) {
            g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncateWords(this.placeholder, this.getInnerWidth()), x, y, p.textDim(), false);
         }

         super.renderWidget(g, mouseX - dx, mouseY - dy, partialTick);
         g.pose().popPose();
      }
   }
}
