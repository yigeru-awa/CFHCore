package com.onehumanawa.cfhcore.content.gui.core.widget.popup;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class ColorPickerPopup implements Popup {
   private static final int SQ = 64;
   private static final int HUE_W = 10;
   private static final int PAD = 6;
   private static final int SWATCH = 10;
   private static final int SWATCH_GAP = 2;
   private static final int READOUT_H = 14;
   private final int x;
   private final int y;
   private final int w;
   private final int h;
   private final float[] hsv;
   private final int original;
   private final IntConsumer onChange;
   private final Anim open;
   private int dragging;
   private boolean closing;

   public ColorPickerPopup(int atX, int atY, int color, IntConsumer onChange) {
      this.open = new Anim(0.0F, 140, Ease.OUT_CUBIC);
      this.hsv = Colors.rgbToHsv(color);
      this.original = color | -16777216;
      this.onChange = onChange;
      this.w = 92;
      this.h = 88 + this.swatchRows() * 12 - 2 + 14;
      int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
      int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
      this.x = Math.max(2, Math.min(atX, sw - this.w - 2));
      this.y = atY + this.h > sh - 2 ? Math.max(2, atY - this.h - 24) : atY;
      this.open.snap(0.0F);
      this.open.set(1.0F);
   }

   private int swatchesPerRow() {
      return (this.w - 12 + 2) / 12;
   }

   private int swatchRows() {
      return (Palette.ACCENTS.size() + this.swatchesPerRow() - 1) / this.swatchesPerRow();
   }

   private int sqX() {
      return this.x + 6;
   }

   private int sqY() {
      return this.y + 6;
   }

   private int hueX() {
      return this.x + 12 + 64;
   }

   private int swatchY() {
      return this.y + 12 + 64;
   }

   private int readoutY() {
      return this.swatchY() + this.swatchRows() * 12 - 2 + 6;
   }

   private int current() {
      return Colors.hsvToRgb(this.hsv[0], this.hsv[1], this.hsv[2]);
   }

   public boolean contains(double mx, double my) {
      return !this.closing && mx >= (double)this.x && mx < (double)(this.x + this.w) && my >= (double)this.y && my < (double)(this.y + this.h);
   }

   private void apply() {
      this.onChange.accept(this.current());
   }

   private void pick(double mx, double my) {
      if (this.dragging == 1) {
         this.hsv[1] = (float)Math.max((double)0.0F, Math.min((double)1.0F, (mx - (double)this.sqX()) / (double)63.0F));
         this.hsv[2] = (float)Math.max((double)0.0F, Math.min((double)1.0F, (double)1.0F - (my - (double)this.sqY()) / (double)63.0F));
         this.apply();
      } else if (this.dragging == 2) {
         this.hsv[0] = (float)Math.max((double)0.0F, Math.min((double)0.999F, (my - (double)this.sqY()) / (double)64.0F));
         this.apply();
      }

   }

   public boolean mouseClicked(double mx, double my, int button) {
      if (mx >= (double)this.sqX() && mx < (double)(this.sqX() + 64) && my >= (double)this.sqY() && my < (double)(this.sqY() + 64)) {
         this.dragging = 1;
      } else {
         if (!(mx >= (double)this.hueX()) || !(mx < (double)(this.hueX() + 10)) || !(my >= (double)this.sqY()) || !(my < (double)(this.sqY() + 64))) {
            int sy = this.swatchY();
            int per = this.swatchesPerRow();
            int rows = this.swatchRows();
            if (my >= (double)sy && my < (double)(sy + rows * 12)) {
               int col = (int)((mx - (double)this.x - (double)6.0F) / (double)12.0F);
               int row = (int)((my - (double)sy) / (double)12.0F);
               int i = row * per + col;
               if (col >= 0 && col < per && i >= 0 && i < Palette.ACCENTS.size()) {
                  float[] p = Colors.rgbToHsv(((Palette.AccentPreset)Palette.ACCENTS.get(i)).color());
                  System.arraycopy(p, 0, this.hsv, 0, 3);
                  this.apply();
               }
            } else if (my >= (double)this.readoutY() && my < (double)(this.readoutY() + 14) && mx >= (double)(this.x + this.w - 6 - 30) && mx < (double)(this.x + this.w - 6 - 15)) {
               float[] p = Colors.rgbToHsv(this.original);
               System.arraycopy(p, 0, this.hsv, 0, 3);
               this.apply();
            }

            return true;
         }

         this.dragging = 2;
      }

      this.pick(mx, my);
      return true;
   }

   public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
      if (this.dragging == 0) {
         return false;
      } else {
         this.pick(mx, my);
         return true;
      }
   }

   public boolean mouseReleased(double mx, double my, int button) {
      this.dragging = 0;
      return true;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      float step = 0.05F;
      switch (keyCode) {
         case 257:
         case 335:
            Popups.close(this);
            return true;
         case 262:
            this.hsv[1] = Math.min(1.0F, this.hsv[1] + 0.05F);
            break;
         case 263:
            this.hsv[1] = Math.max(0.0F, this.hsv[1] - 0.05F);
            break;
         case 264:
            if ((modifiers & 1) != 0) {
               this.hsv[0] = (this.hsv[0] + 0.05F) % 1.0F;
            } else {
               this.hsv[2] = Math.max(0.0F, this.hsv[2] - 0.05F);
            }
            break;
         case 265:
            if ((modifiers & 1) != 0) {
               this.hsv[0] = (this.hsv[0] - 0.05F + 1.0F) % 1.0F;
            } else {
               this.hsv[2] = Math.min(1.0F, this.hsv[2] + 0.05F);
            }
            break;
         default:
            return false;
      }

      this.apply();
      return true;
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      float o = Math.max(0.0F, Math.min(1.0F, this.open.get()));
      int a = Math.round(255.0F * Math.min(1.0F, o * 1.4F));
      if (a > 2) {
         g.pose().pushPose();
         g.pose().translate(0.0F, (float)Math.round((1.0F - o) * -4.0F), 0.0F);
         if (t.isVanilla()) {
            g.fill(this.x, this.y, this.x + this.w, this.y + this.h, Colors.withAlpha(0, Math.min(a, 240)));
            CFHGuiDraw.outline(g, this.x, this.y, this.w, this.h, Colors.withAlpha(16777215, a), 0);
         } else {
            CFHGuiDraw.popupPlate(g, this.x, this.y, this.w, this.h, p.surface(), p.borderStrong(), 0.6F, (float)a / 255.0F);
         }

         for(int j = 0; j < 64; j += 4) {
            for(int i = 0; i < 64; i += 4) {
               g.fill(this.sqX() + i, this.sqY() + j, this.sqX() + i + 4, this.sqY() + j + 4, Colors.withAlpha(Colors.hsvToRgb(this.hsv[0], ((float)i + 2.0F) / 64.0F, 1.0F - ((float)j + 2.0F) / 64.0F), a));
            }
         }

         int cx = this.sqX() + Math.round(this.hsv[1] * 63.0F);
         int cy = this.sqY() + Math.round((1.0F - this.hsv[2]) * 63.0F);
         CFHGuiDraw.outline(g, cx - 3, cy - 3, 7, 7, Colors.withAlpha(16777215, a), 0);
         CFHGuiDraw.outline(g, cx - 2, cy - 2, 5, 5, Colors.withAlpha(0, a), 0);

         for(int j = 0; j < 64; j += 2) {
            g.fill(this.hueX(), this.sqY() + j, this.hueX() + 10, this.sqY() + j + 2, Colors.withAlpha(Colors.hsvToRgb(((float)j + 1.0F) / 64.0F, 1.0F, 1.0F), a));
         }

         int hy = this.sqY() + Math.round(this.hsv[0] * 64.0F);
         g.fill(this.hueX() - 1, hy - 1, this.hueX() + 10 + 1, hy + 1, Colors.withAlpha(16777215, a));
         g.fill(this.hueX() - 1, hy - 2, this.hueX() + 10 + 1, hy - 1, Colors.withAlpha(0, a));
         g.fill(this.hueX() - 1, hy + 1, this.hueX() + 10 + 1, hy + 2, Colors.withAlpha(0, a));
         int per = this.swatchesPerRow();
         int cur = this.current();

         for(int i = 0; i < Palette.ACCENTS.size(); ++i) {
            int sx = this.x + 6 + i % per * 12;
            int sy = this.swatchY() + i / per * 12;
            int c = ((Palette.AccentPreset)Palette.ACCENTS.get(i)).color();
            CFHGuiDraw.pixelRound(g, sx, sy, 10, 10, Colors.withAlpha(c, a), 2);
            if ((c & 16777215) == (cur & 16777215)) {
               CFHGuiDraw.outline(g, sx - 1, sy - 1, 12, 12, Colors.withAlpha(p.text(), a), 2);
            }
         }

         int ry = this.readoutY();
         g.drawString(CFHGuiDraw.font(), Colors.toHex(cur), this.x + 6, ry + 3, Colors.withAlpha(p.text(), a), t.isVanilla());
         CFHGuiDraw.pixelRound(g, this.x + this.w - 6 - 30, ry, 15, 14, Colors.withAlpha(this.original, a), 0);
         CFHGuiDraw.pixelRound(g, this.x + this.w - 6 - 15, ry, 15, 14, Colors.withAlpha(cur, a), 0);
         CFHGuiDraw.outline(g, this.x + this.w - 6 - 30, ry, 30, 14, Colors.withAlpha(p.borderStrong(), a), 0);
         g.pose().popPose();
      }
   }

   public boolean beginClose() {
      if (Theme.current().motion() <= 0.0F) {
         return false;
      } else {
         this.closing = true;
         this.open.set(0.0F, 100);
         return true;
      }
   }

   public boolean closeFinished() {
      return this.open.get() <= 0.02F;
   }

   public Component title() {
      return Component.translatable("cfhgui.color");
   }
}
