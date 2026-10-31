package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.play;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiBadge;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class DetailsList extends CFHGuiWidget {
   private static final int ROW_H = 12;
   private static final int HEAD_H = 12;
   private static final int HINT_H = 12;
   private static final int LETTERS = 7;
   private Component title = Component.empty();
   private Component subtitle = Component.empty();
   private List<Chip> chips = List.of();
   private List<Row> rows = List.of();
   private final Anim scroll;
   private final Anim swap;
   private int contentH;
   private int gapBelow;

   DetailsList(int x, int y, int width, int height) {
      super(x, y, width, height, Component.empty());
      this.scroll = new Anim(0.0F, 160, Ease.OUT_CUBIC);
      this.swap = new Anim(1.0F, 220, Ease.OUT_CUBIC);
      this.silent();
   }

   DetailsList gapBelow(int px) {
      this.gapBelow = Math.max(0, px);
      return this;
   }

   void show(Component title, Component subtitle, List<Chip> chips, List<Row> rows) {
      boolean other = !title.getString().equals(this.title.getString());
      this.title = title;
      this.subtitle = subtitle;
      this.chips = List.copyOf(chips);
      this.rows = new ArrayList(rows);
      this.setMessage(title);
      if (other) {
         this.scroll.snap(0.0F);
         this.swap.snap(0.0F);
         this.swap.set(1.0F);
      }

   }

   boolean isEmpty() {
      return this.title.getString().isEmpty();
   }

   private boolean roomy() {
      return this.getHeight() >= 132 && !this.subtitle.getString().isEmpty();
   }

   private int chipsWidth() {
      int w = 0;

      for(Chip c : this.chips) {
         w += CFHGuiDraw.width(c.text()) + 8 + 3;
      }

      return Math.max(0, w - 3);
   }

   private boolean chipsBeside() {
      if (this.chips.isEmpty()) {
         return false;
      } else {
         int name = CFHGuiDraw.font().width(Fonts.heading(this.title));
         return Math.min(name, this.getWidth() / 2) + 8 + this.chipsWidth() <= this.getWidth();
      }
   }

   private int headerHeight() {
      return 14 + (this.roomy() ? 11 : 0) + (!this.chips.isEmpty() && !this.chipsBeside() ? 15 : 0) + 3;
   }

   private int listTop() {
      return this.getY() + this.headerHeight();
   }

   private int listHeight() {
      int room = Math.max(0, this.getHeight() - this.headerHeight());
      return !this.overflows() ? room : Math.max(12, (room - 12) / 12 * 12);
   }

   private boolean overflows() {
      return this.contentH > this.getHeight() - this.headerHeight();
   }

   private int maxScroll() {
      return Math.max(0, this.contentH - this.listHeight());
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.isMouseOver(mouseX, mouseY) && this.maxScroll() > 0) {
         this.scroll.set(Mth.clamp(this.scroll.target() - (float)scrollY * 12.0F * 2.0F, 0.0F, (float)this.maxScroll()));
         return true;
      } else {
         return false;
      }
   }

   public void onClick(double mouseX, double mouseY) {
      super.onClick(mouseX, mouseY);
      Row r = this.rowAt(mouseY);
      if (r != null && r.copies() && r.value() != null) {
         Minecraft.getInstance().keyboardHandler.setClipboard(r.value().getString());
         CFHGuiToasts.show(Component.translatable("cfhgui.copied"), r.value(), Icon.COPY);
      }
   }

   private @Nullable Row rowAt(double mouseY) {
      if (!(mouseY < (double)this.listTop()) && !(mouseY >= (double)(this.listTop() + this.listHeight()))) {
         int y = this.listTop() - Math.round(this.scroll.get());

         for(Row r : this.rows) {
            int h = r.value() == null ? 12 : 12;
            if (mouseY >= (double)y && mouseY < (double)(y + h)) {
               return r;
            }

            y += h;
         }

         return null;
      } else {
         return null;
      }
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, mouseX, mouseY, false);
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, mouseX, mouseY, true);
   }

   private void draw(GuiGraphics g, int mouseX, int mouseY, boolean vanilla) {
      Theme t = Theme.current();
      Palette p = t.palette();
      float a = this.effectiveAlpha() * this.swap.get();
      if (!(a <= 0.004F)) {
         Font font = CFHGuiDraw.font();
         int x = this.getX();
         int w = this.getWidth();
         int top = this.getY() + this.enterOffset() + Math.round((1.0F - this.swap.get()) * 5.0F);
         int text = Colors.scaleAlpha(vanilla ? -1 : p.text(), a);
         int muted = Colors.scaleAlpha(vanilla ? -4144960 : p.textMuted(), a);
         int dim = Colors.scaleAlpha(vanilla ? -6250336 : p.textDim(), a);
         boolean beside = this.chipsBeside();
         int chipsW = this.chipsWidth();
         g.drawString(font, CFHGuiDraw.truncate(Fonts.heading(this.title), beside ? w - chipsW - 8 : w), x, top + 3, text, vanilla);
         if (beside) {
            this.chips(g, x + w - chipsW, top + 2, a);
         }

         int y = top + 14;
         if (this.roomy()) {
            g.drawString(font, CFHGuiDraw.truncate(this.subtitle, w), x, y, dim, vanilla);
            y += 11;
         }

         if (!this.chips.isEmpty() && !beside) {
            this.chips(g, x, y + 1, a);
            y += 15;
         }

         y += 3;
         int listTop = y;
         int listH = this.listHeight();
         Component tip = null;
         g.enableScissor(x - 2, y, x + w + 2, y + listH);
         y -= Math.round(this.scroll.get());
         boolean still = this.scroll.get() == this.scroll.target();
         int closing = Integer.MIN_VALUE;

         for(Row r : this.rows) {
            if (r.value() == null) {
               if (y + 12 > listTop && y < listTop + listH) {
                  CFHGuiDraw.sectionRule(g, r.label(), x, y + 1, w, a);
                  if (still && y + 12 == listTop + listH) {
                     closing = y + 6;
                  }
               }

               y += 12;
            } else {
               if (y + 12 > listTop && y < listTop + listH) {
                  boolean over = this.isHovered() && mouseY >= Math.max(y, listTop) && mouseY < Math.min(y + 12, listTop + listH) && mouseX >= x && mouseX < x + w;
                  if (over && r.copies()) {
                     CFHGuiDraw.rect(g, x - 2, y - 2, w + 4, 12, Colors.scaleAlpha(vanilla ? 822083583 : Colors.withAlpha(p.surfaceHover(), 192), a));
                     tip = Component.translatable("cfhgui.play.copy");
                  }

                  g.drawString(font, r.label(), x, y, muted, vanilla);
                  int room = w - font.width(r.label()) - 8;
                  FormattedCharSequence v = CFHGuiDraw.truncate(r.value(), room);
                  int color = r.color() != 0 ? Colors.scaleAlpha(r.color(), a) : text;
                  g.drawString(font, v, x + w - font.width(v) + 1, y, color, vanilla);
                  if (over && font.width(r.value()) > room) {
                     tip = r.value();
                  }
               }

               y += 12;
            }
         }

         this.contentH = y + Math.round(this.scroll.get()) - listTop;
         g.disableScissor();
         if (this.scroll.target() > (float)this.maxScroll()) {
            this.scroll.set((float)this.maxScroll());
         }

         if (this.overflows()) {
            float more = Mth.clamp(((float)this.maxScroll() - this.scroll.get()) / 10.0F, 0.0F, 1.0F);
            boolean closed = closing != Integer.MIN_VALUE;
            int rule = closed ? closing : listTop + listH;
            if (!closed) {
               CFHGuiDraw.hline(g, x, rule, w, Colors.scaleAlpha(vanilla ? 1090519039 : p.border(), a));
            }

            int bandTop = rule + 1;
            int bandBottom = top + this.getHeight() + this.gapBelow;
            int hy = bandTop + Math.max(0, (bandBottom - bandTop - 7) / 2);
            Component hint = Component.translatable(more > 0.5F ? "cfhgui.play.scroll_more" : "cfhgui.play.scroll_end");
            int hw = font.width(hint) - 1 + (more > 0.5F ? 11 : 0);
            if (hw <= w) {
               int hx = x + (w - hw) / 2;
               if (more > 0.5F) {
                  float breath = t.motion() > 0.0F ? 0.65F + 0.35F * Mth.sin((float)System.nanoTime() / 1.0E9F * 2.6F) : 1.0F;
                  Icons.draw(g, Icon.CHEVRON_DOWN, hx, hy, 8, Colors.scaleAlpha(vanilla ? -1 : p.accentInk(), a * breath));
               }

               g.drawString(font, hint, hx + (more > 0.5F ? 11 : 0), hy, dim, vanilla);
            }
         }

         if (tip != null) {
            CFHGuiTooltips.request(tip, this);
         }

      }
   }

   private void chips(GuiGraphics g, int x, int y, float a) {
      int cx = x;

      for(Chip c : this.chips) {
         int cw = CFHGuiDraw.width(c.text()) + 8;
         if (cx + cw > this.getX() + this.getWidth()) {
            break;
         }

         cx += CFHGuiBadge.draw(g, c.text(), cx, y, Colors.scaleAlpha(c.color(), a)) + 3;
      }

   }

   static record Row(Component label, @Nullable Component value, int color, boolean copies) {
      static Row of(Component label, Component value) {
         return new Row(label, value, 0, false);
      }

      static Row of(Component label, String value) {
         return new Row(label, Component.literal(value), 0, false);
      }

      static Row colored(Component label, Component value, int argb) {
         return new Row(label, value, argb, false);
      }

      static Row copy(Component label, String value) {
         return new Row(label, Component.literal(value), 0, true);
      }

      static Row heading(Component label) {
         return new Row(label, (Component)null, 0, false);
      }
   }

   static record Chip(Component text, int color) {
   }
}
