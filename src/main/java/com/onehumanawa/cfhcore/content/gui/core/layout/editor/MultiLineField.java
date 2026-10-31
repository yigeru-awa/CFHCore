package com.onehumanawa.cfhcore.content.gui.core.layout.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class MultiLineField extends CFHGuiWidget {
   private static final int PAD = 4;
   private static final int LINE = 10;
   private String text = "";
   private int cursor;
   private int scrollLine;
   private @Nullable Consumer<String> onChange;
   private @Nullable Component placeholder;

   MultiLineField(int x, int y, int w, int h, Component narration) {
      super(x, y, w, h, narration);
      this.silent();
   }

   MultiLineField text(String v) {
      this.text = v == null ? "" : v.replace("\r\n", "\n").replace('\r', '\n');
      this.cursor = Math.min(this.cursor, this.text.length());
      return this;
   }

   MultiLineField onChange(Consumer<String> c) {
      this.onChange = c;
      return this;
   }

   MultiLineField placeholder(Component c) {
      this.placeholder = c;
      return this;
   }

   String value() {
      return this.text;
   }

   private List<String> lines() {
      return Arrays.asList(this.text.split("\n", -1));
   }

   private int[] lineCol(int pos) {
      int line = 0;
      int col = 0;

      for(int i = 0; i < pos && i < this.text.length(); ++i) {
         if (this.text.charAt(i) == '\n') {
            ++line;
            col = 0;
         } else {
            ++col;
         }
      }

      return new int[]{line, col};
   }

   private int posOf(int line, int col) {
      List<String> ls = this.lines();
      int pos = 0;

      for(int i = 0; i < line && i < ls.size(); ++i) {
         pos += ((String)ls.get(i)).length() + 1;
      }

      int l = Math.max(0, Math.min(line, ls.size() - 1));
      return pos + Math.max(0, Math.min(col, ((String)ls.get(l)).length()));
   }

   private int visibleLines() {
      return Math.max(1, (this.getHeight() - 8) / 10);
   }

   private void changed() {
      if (this.onChange != null) {
         this.onChange.accept(this.text);
      }

   }

   private void insert(String s) {
      if (s != null && !s.isEmpty()) {
         String clean = s.replace("\r\n", "\n").replace('\r', '\n');
         this.text = this.text.substring(0, this.cursor) + clean + this.text.substring(this.cursor);
         this.cursor += clean.length();
         this.changed();
      }
   }

   private void ensureCursorVisible() {
      int line = this.lineCol(this.cursor)[0];
      if (line < this.scrollLine) {
         this.scrollLine = line;
      } else if (line >= this.scrollLine + this.visibleLines()) {
         this.scrollLine = line - this.visibleLines() + 1;
      }

      this.scrollLine = Math.max(0, Math.min(this.scrollLine, Math.max(0, this.lines().size() - 1)));
   }

   public void onClick(double mouseX, double mouseY) {
      super.onClick(mouseX, mouseY);
      List<String> ls = this.lines();
      int line = Math.max(0, Math.min(ls.size() - 1, this.scrollLine + (int)((mouseY - (double)this.getY() - (double)4.0F) / (double)10.0F)));
      String l = (String)ls.get(line);
      int col = CFHGuiDraw.font().plainSubstrByWidth(l, Math.max(0, (int)(mouseX - (double)this.getX() - (double)4.0F))).length();
      this.cursor = this.posOf(line, col);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.isFocused() && this.active && this.visible) {
         boolean ctrl = EditorKeys.ctrl();
         switch (keyCode) {
            case 65:
               if (!ctrl) {
                  return false;
               }

               this.cursor = this.text.length();
               break;
            case 67:
               if (!ctrl) {
                  return false;
               }

               Minecraft.getInstance().keyboardHandler.setClipboard(this.text);
               break;
            case 86:
               if (!ctrl) {
                  return false;
               }

               this.insert(Minecraft.getInstance().keyboardHandler.getClipboard());
               break;
            case 257:
            case 335:
               this.insert("\n");
               break;
            case 259:
               if (this.cursor > 0) {
                  String var9 = this.text.substring(0, this.cursor - 1);
                  this.text = var9 + this.text.substring(this.cursor);
                  --this.cursor;
                  this.changed();
               }
               break;
            case 261:
               if (this.cursor < this.text.length()) {
                  String var10001 = this.text.substring(0, this.cursor);
                  this.text = var10001 + this.text.substring(this.cursor + 1);
                  this.changed();
               }
               break;
            case 262:
               if (this.cursor < this.text.length()) {
                  ++this.cursor;
               }
               break;
            case 263:
               if (this.cursor > 0) {
                  --this.cursor;
               }
               break;
            case 264:
               int[] lc = this.lineCol(this.cursor);
               if (lc[0] < this.lines().size() - 1) {
                  this.cursor = this.posOf(lc[0] + 1, lc[1]);
               } else {
                  this.cursor = this.text.length();
               }
               break;
            case 265:
               int[] lc2 = this.lineCol(this.cursor);
               if (lc2[0] > 0) {
                  this.cursor = this.posOf(lc2[0] - 1, lc2[1]);
               } else {
                  this.cursor = 0;
               }
               break;
            case 268:
               int[] lc3 = this.lineCol(this.cursor);
               this.cursor = this.posOf(lc3[0], 0);
               break;
            case 269:
               int[] lc4 = this.lineCol(this.cursor);
               this.cursor = this.posOf(lc4[0], 1073741823);
               break;
            default:
               return false;
         }

         this.ensureCursorVisible();
         return true;
      } else {
         return false;
      }
   }

   public boolean charTyped(char c, int modifiers) {
      if (this.isFocused() && this.active && this.visible && c >= ' ') {
         this.insert(String.valueOf(c));
         this.ensureCursorVisible();
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (!this.contains(mouseX, mouseY)) {
         return false;
      } else {
         int max = Math.max(0, this.lines().size() - this.visibleLines());
         if (max == 0) {
            return false;
         } else {
            this.scrollLine = Math.max(0, Math.min(max, this.scrollLine - (int)Math.signum(scrollY)));
            return true;
         }
      }
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, false);
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, true);
   }

   private void draw(GuiGraphics g, boolean vanilla) {
      Theme t = Theme.current();
      Palette p = t.palette();
      int x = this.getX();
      int y = this.getY();
      int w = this.getWidth();
      int h = this.getHeight();
      float foc = this.focus() + (float)(this.isFocused() ? 1 : 0);
      float hov = this.hover();
      if (vanilla) {
         CFHGuiDraw.vanillaTextField(g, x, y, w, h, this.isFocused());
      } else {
         int fill = Colors.lerp(Colors.lerp(p.bg2(), p.surface(), hov), p.surface(), Math.min(1.0F, foc));
         int border = Colors.lerp(Colors.lerp(p.border(), p.borderStrong(), hov), p.accent(), Math.min(1.0F, foc));
         CFHGuiDraw.pixelRound(g, x, y, w, h, fill, t.radius());
         CFHGuiDraw.outline(g, x, y, w, h, border, t.radius());
      }

      int fg = vanilla ? -2039584 : p.text();
      CFHGuiDraw.scissor(g, x + 1, y + 1, w - 2, h - 2);
      List<String> ls = this.lines();
      int[] lc = this.lineCol(this.cursor);
      int innerW = w - 8 - 2;
      int ly = y + 4;
      if (this.text.isEmpty() && this.placeholder != null && !this.isFocused()) {
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(this.placeholder, innerW), x + 4, ly, p.textDim(), false);
      }

      for(int i = this.scrollLine; i < ls.size() && ly < y + h - 4; ++i) {
         String line = (String)ls.get(i);
         int shift = 0;
         if (i == lc[0]) {
            int cw = CFHGuiDraw.width(line.substring(0, Math.min(lc[1], line.length())));
            if (cw > innerW) {
               shift = cw - innerW;
            }
         }

         String shown = shift > 0 ? line.substring(CFHGuiDraw.font().plainSubstrByWidth(line, shift).length()) : line;
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.font().plainSubstrByWidth(shown, innerW), x + 4, ly, fg, vanilla);
         if (this.isFocused() && i == lc[0] && Clock.nowMs() / 500L % 2L == 0L) {
            int cx = x + 4 + CFHGuiDraw.width(line.substring(0, Math.min(lc[1], line.length()))) - shift;
            g.fill(cx, ly - 1, cx + 1, ly + 9, fg);
         }

         ly += 10;
      }

      CFHGuiDraw.unscissor(g);
      int max = Math.max(0, ls.size() - this.visibleLines());
      if (max > 0) {
         int trackH = h - 4;
         int barH = Math.max(8, trackH * this.visibleLines() / ls.size());
         int by = y + 2 + (trackH - barH) * this.scrollLine / max;
         g.fill(x + w - 3, by, x + w - 2, by + barH, Colors.withAlpha(p.textDim(), 160));
      }

   }
}
