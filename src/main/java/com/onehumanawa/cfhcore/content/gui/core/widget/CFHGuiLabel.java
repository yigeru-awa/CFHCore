package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.List;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class CFHGuiLabel extends CFHGuiWidget {
   private Style style;
   private Align align;
   private boolean wrap;
   private int color;
   private boolean shadowOverride;
   private boolean shadow;

   public CFHGuiLabel(int x, int y, int width, Component text) {
      super(x, y, width, CFHGuiLabel.Style.BODY.lineHeight, text);
      this.style = CFHGuiLabel.Style.BODY;
      this.align = CFHGuiLabel.Align.LEFT;
      this.color = 0;
      this.active = false;
   }

   public CFHGuiLabel(int x, int y, Component text) {
      this(x, y, CFHGuiDraw.width(text) + 2, text);
   }

   public CFHGuiLabel style(Style s) {
      this.style = s;
      this.autoHeight();
      return this;
   }

   public Style style() {
      return this.style;
   }

   public CFHGuiLabel align(Align a) {
      this.align = a;
      return this;
   }

   public CFHGuiLabel wrap(boolean wrap) {
      this.wrap = wrap;
      this.autoHeight();
      return this;
   }

   public CFHGuiLabel color(int argb) {
      this.color = argb;
      return this;
   }

   public CFHGuiLabel shadow(boolean on) {
      this.shadowOverride = true;
      this.shadow = on;
      return this;
   }

   public CFHGuiLabel text(Component text) {
      this.setMessage(text);
      this.autoHeight();
      return this;
   }

   public void setWidth(int width) {
      super.setWidth(width);
      if (this.wrap) {
         this.autoHeight();
      }

   }

   public int lineHeight() {
      return this.style.lineHeight;
   }

   public int lineCount() {
      return this.wrap ? Math.max(1, this.lines().size()) : 1;
   }

   public CFHGuiLabel autoHeight() {
      this.setHeight(this.lineCount() * this.style.lineHeight);
      return this;
   }

   public static int heightFor(Component text, int width, Style style) {
      Component t = (Component)(style.heading() ? Fonts.heading(text) : text);
      return Math.max(1, CFHGuiDraw.font().split(t, Math.max(1, width)).size()) * style.lineHeight;
   }

   private List<FormattedCharSequence> lines() {
      return CFHGuiDraw.font().split(this.displayText(), Math.max(1, this.getWidth()));
   }

   private Component displayText() {
      return (Component)(this.style.heading() ? Fonts.heading(this.getMessage()) : this.getMessage());
   }

   private int fg() {
      if (this.color != 0) {
         return this.color;
      } else {
         Palette p = Theme.current().palette();
         int var10000;
         switch (this.style.ordinal()) {
            case 3 -> var10000 = p.textMuted();
            case 4 -> var10000 = p.textDim();
            default -> var10000 = p.text();
         }

         return var10000;
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return false;
   }

   public ComponentPath nextFocusPath(FocusNavigationEvent event) {
      return null;
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, this.shadowOverride ? this.shadow : false);
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, this.shadowOverride ? this.shadow : true);
   }

   private void draw(GuiGraphics g, boolean shadow) {
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F)) {
         int fg = Colors.scaleAlpha(this.fg(), a);
         int lh = this.style.lineHeight;
         int y = this.getY() + this.enterOffset() + Math.max(0, (lh - 9) / 2);
         if (this.wrap) {
            int ly = y;

            for(FormattedCharSequence s : this.lines()) {
               this.drawLine(g, s, ly, fg, shadow);
               ly += lh;
            }
         } else {
            this.drawLine(g, CFHGuiDraw.truncate(this.displayText(), this.getWidth()), y, fg, shadow);
         }

      }
   }

   private void drawLine(GuiGraphics g, FormattedCharSequence s, int y, int fg, boolean shadow) {
      int w = CFHGuiDraw.font().width(s);
      int var10000;
      switch (this.align.ordinal()) {
         case 1 -> var10000 = this.getX() + (this.getWidth() - w) / 2;
         case 2 -> var10000 = this.getX() + this.getWidth() - w;
         default -> var10000 = this.getX();
      }

      int x = var10000;
      g.drawString(CFHGuiDraw.font(), s, x, y, fg, shadow);
   }

   protected void updateWidgetNarration(NarrationElementOutput out) {
      out.add(NarratedElementType.TITLE, this.getMessage());
   }

   public static enum Style {
      HEADING(14),
      TITLE(12),
      BODY(10),
      MUTED(10),
      CAPTION(10);

      public final int lineHeight;

      private Style(final int lineHeight) {
         this.lineHeight = lineHeight;
      }

      public boolean heading() {
         return this == HEADING || this == TITLE;
      }
   }

   public static enum Align {
      LEFT,
      CENTER,
      RIGHT;
   }
}
