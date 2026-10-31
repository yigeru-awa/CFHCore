package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public class CFHGuiButton extends CFHGuiWidget {
   public static final int HEIGHT = 20;
   public static final int HEIGHT_SMALL = 16;
   public static final int HEIGHT_LARGE = 26;
   public static final int PAD = 6;
   protected @Nullable Icon icon;
   protected Variant variant;
   protected Runnable onPress;
   protected boolean centered;
   protected int iconSize;
   protected boolean scrolling;
   private boolean contentRight;

   public CFHGuiButton(int x, int y, int width, int height, Component label, Runnable onPress) {
      super(x, y, width, height, label);
      this.variant = CFHGuiButton.Variant.SECONDARY;
      this.centered = true;
      this.iconSize = 12;
      this.onPress = onPress;
   }

   public CFHGuiButton(int x, int y, int width, Component label, Runnable onPress) {
      this(x, y, width, 20, label, onPress);
   }

   public CFHGuiButton variant(Variant v) {
      this.variant = v;
      return this;
   }

   public CFHGuiButton icon(@Nullable Icon icon) {
      this.icon = icon;
      return this;
   }

   public CFHGuiButton iconSize(int size) {
      this.iconSize = size;
      return this;
   }

   public CFHGuiButton leftAligned() {
      this.centered = false;
      return this;
   }

   public CFHGuiButton onPress(Runnable r) {
      this.onPress = r;
      return this;
   }

   public CFHGuiButton scrollLongLabels() {
      this.scrolling = true;
      return this;
   }

   public CFHGuiButton tip(Component tooltip) {
      super.tip(tooltip);
      return this;
   }

   public CFHGuiButton tip(List<Component> lines) {
      super.tip(lines);
      return this;
   }

   public Variant variant() {
      return this.variant;
   }

   public @Nullable Icon icon() {
      return this.icon;
   }

   public int preferredWidth() {
      int text = this.getMessage().getString().isEmpty() ? 0 : CFHGuiDraw.width(this.getMessage());
      int ic = this.icon == null ? 0 : this.iconSize + (text > 0 ? 4 : 0);
      return text + ic + 12;
   }

   public void activate() {
      if (this.active && this.visible && !this.isLocked()) {
         this.playDownSound(Minecraft.getInstance().getSoundManager());
         this.flashPress();
         if (this.onPress != null) {
            this.onPress.run();
         }

      }
   }

   public void onClick(double mouseX, double mouseY) {
      if (!this.isLocked()) {
         super.onClick(mouseX, mouseY);
         if (this.onPress != null) {
            this.onPress.run();
         }

      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.active && this.visible) {
         if (keyCode != 257 && keyCode != 32 && keyCode != 335) {
            return false;
         } else {
            this.activate();
            return true;
         }
      } else {
         return false;
      }
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F)) {
         float hov = this.hover();
         float prs = this.press();
         float foc = this.focus();
         int x = this.getX();
         int y = this.getY() + this.enterOffset();
         int w = this.getWidth();
         int h = this.getHeight();
         int fill;
         int border;
         int fg;
         switch (this.variant.ordinal()) {
            case 0:
               fill = Colors.lerp(p.accent(), p.accentHover(), hov);
               border = Colors.brighten(fill, 0.15F);
               fg = p.accentText();
               break;
            case 1:
            default:
               fill = Colors.lerp(p.surface(), p.surfaceHover(), hov);
               border = Colors.lerp(p.border(), p.borderStrong(), hov);
               fg = Colors.lerp(p.text(), -1, hov * 0.3F);
               break;
            case 2:
               fill = Colors.lerp(0, p.surfaceHover(), hov);
               border = Colors.lerp(0, p.border(), hov);
               fg = Colors.lerp(p.textMuted(), p.text(), hov);
               break;
            case 3:
               fill = Colors.lerp(Colors.withAlpha(p.danger(), 48), p.danger(), hov);
               border = Colors.lerp(Colors.withAlpha(p.danger(), 160), Colors.brighten(p.danger(), 0.2F), hov);
               fg = Colors.lerp(p.danger(), -1, hov);
         }

         if (!this.active) {
            fill = this.variant == CFHGuiButton.Variant.GHOST ? 0 : Colors.withAlpha(p.surface(), 128);
            border = this.variant == CFHGuiButton.Variant.GHOST ? 0 : Colors.withAlpha(p.border(), 128);
            fg = p.textDim();
         } else if (this.isLocked()) {
            fill = Colors.lerp(Colors.withAlpha(p.surface(), 128), Colors.withAlpha(p.surfaceHover(), 160), hov);
            border = Colors.lerp(Colors.withAlpha(p.border(), 128), p.border(), hov);
            fg = Colors.lerp(p.textDim(), p.textMuted(), hov);
         }

         int py = y + Math.round(prs);
         fill = Colors.brighten(fill, -0.12F * prs);
         boolean setIn = Theme.setIn() && this.variant == CFHGuiButton.Variant.SECONDARY;
         if (setIn) {
            fill = t.field(fill);
            border = t.fieldEdge(border);
         } else if (this.variant == CFHGuiButton.Variant.SECONDARY || this.variant == CFHGuiButton.Variant.GHOST) {
            fill = t.control(fill);
            border = t.controlEdge(border);
         }

         if (!setIn && this.active && (this.variant != CFHGuiButton.Variant.GHOST || hov > 0.01F)) {
            CFHGuiDraw.shadow(g, x, py, w, h, 0.35F * a * (1.0F - prs) * (this.variant == CFHGuiButton.Variant.GHOST ? hov : 1.0F));
         }

         CFHGuiDraw.pixelRound(g, x, py, w, h, Colors.scaleAlpha(fill, a), t.radius());
         CFHGuiDraw.outline(g, x, py, w, h, Colors.scaleAlpha(border, a), t.radius());
         CFHGuiDraw.focusRing(g, x, py, w, h, foc * a);
         this.contentRight = setIn;

         try {
            this.drawContent(g, x, py, w, h, Colors.scaleAlpha(fg, a), false);
         } finally {
            this.contentRight = false;
         }

         if (this.isLocked()) {
            this.drawLock(g, x, py, w, h, Colors.scaleAlpha(fg, a));
         }

      }
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F)) {
         Palette p = Theme.current().palette();
         int x = this.getX();
         int y = this.getY() + this.enterOffset();
         int w = this.getWidth();
         int h = this.getHeight();
         int py = y + Math.round(this.press());
         float lift = Math.max(this.hover(), this.focus());
         if (this.variant == CFHGuiButton.Variant.GHOST) {
            if (lift > 0.01F) {
               CFHGuiDraw.vanillaButton(g, x, py, w, h, lift, this.active, a * lift);
            }

            int fg = this.active ? Colors.lerp(-2039584, -1, lift) : -6250336;
            this.drawContent(g, x, py, w, h, Colors.scaleAlpha(fg, a), true);
         } else {
            CFHGuiDraw.vanillaButton(g, x, py, w, h, this.active ? lift : 0.0F, this.active && !this.isLocked(), a);
            if (this.variant == CFHGuiButton.Variant.PRIMARY && this.active && !this.isLocked()) {
               CFHGuiDraw.rect(g, x + 1, py + 1, w - 2, h - 2, Colors.scaleAlpha(Colors.withAlpha(p.accent(), 90), a));
            }

            int fg = this.active && !this.isLocked() ? -1 : -6250336;
            if (this.variant == CFHGuiButton.Variant.DANGER && this.active) {
               fg = Colors.lerp(-38294, -1, lift * 0.5F);
            }

            this.drawContent(g, x, py, w, h, Colors.scaleAlpha(fg, a), true);
            if (this.isLocked()) {
               this.drawLock(g, x, py, w, h, Colors.scaleAlpha(fg, a));
            }

         }
      }
   }

   protected static int lockSize(int h) {
      return h >= 20 ? 8 : 6;
   }

   protected void drawLock(GuiGraphics g, int x, int y, int w, int h, int color) {
      int s = lockSize(h);
      Icons.draw(g, Icon.LOCK, x + w - s - 6 + 1, y + (h - s) / 2, s, color);
   }

   protected void drawContent(GuiGraphics g, int x, int y, int w, int h, int fg, boolean shadow) {
      boolean hasText = !this.getMessage().getString().isEmpty();
      int iconW = this.icon == null ? 0 : this.iconSize + (hasText ? 4 : 0);
      int lockW = this.isLocked() ? lockSize(h) + 4 : 0;
      int avail = Math.max(0, w - 12 - iconW - lockW);
      int fullW = hasText ? CFHGuiDraw.width(this.getMessage()) : 0;
      boolean overflow = fullW > avail;
      FormattedCharSequence text = hasText ? (this.scrolling ? this.getMessage().getVisualOrderText() : CFHGuiDraw.truncate(this.getMessage(), avail)) : null;
      int textW = text == null ? 0 : Math.min(avail, CFHGuiDraw.width(text));
      int contentW = iconW + textW;
      int cx = this.contentRight ? x + w - 6 - lockW - contentW : (this.centered ? x + (w - lockW - contentW) / 2 : x + 6);
      int ty = CFHGuiDraw.textY(y, h);
      if (this.icon != null) {
         Icons.draw(g, this.icon, cx, y + (h - this.iconSize) / 2, this.iconSize, fg);
         cx += iconW;
      }

      if (text != null) {
         if (this.scrolling && overflow) {
            CFHGuiDraw.drawScrollingText(g, text, cx, ty, avail, fg, shadow);
         } else {
            g.drawString(CFHGuiDraw.font(), text, cx, ty, fg, shadow);
         }

      }
   }

   public static enum Variant {
      PRIMARY,
      SECONDARY,
      GHOST,
      DANGER;
   }
}
