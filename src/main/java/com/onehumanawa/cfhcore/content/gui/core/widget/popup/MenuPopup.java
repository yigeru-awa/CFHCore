package com.onehumanawa.cfhcore.content.gui.core.widget.popup;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class MenuPopup implements Popup {
   public static final int ROW = 14;
   public static final int SEP = 5;
   public static final int PAD = 3;
   private final List<Item> items;
   private int x;
   private int y;
   private int w;
   private int h;
   private int hovered;
   private int scroll;
   private final Anim open;
   private final int minWidth;
   private final boolean opensUpward;
   private boolean closing;
   private int mouseLastX;
   private int mouseLastY;

   public MenuPopup(int atX, int atY, List<Item> items, int minWidth) {
      this.hovered = -1;
      this.open = new Anim(0.0F, 140, Ease.OUT_CUBIC);
      this.mouseLastX = Integer.MIN_VALUE;
      this.mouseLastY = Integer.MIN_VALUE;
      this.items = new ArrayList(items);
      this.minWidth = minWidth;
      this.open.snap(0.0F);
      this.open.set(1.0F);
      this.opensUpward = this.layout(atX, atY);
   }

   public MenuPopup(int atX, int atY, List<Item> items) {
      this(atX, atY, items, 100);
   }

   private boolean layout(int atX, int atY) {
      int maxW = this.minWidth;
      int height = 6;

      for(Item it : this.items) {
         if (it.separator) {
            height += 5;
         } else {
            height += 14;
            maxW = Math.max(maxW, CFHGuiDraw.width(it.label) + 12 + (it.icon != null ? 16 : 0) + (it.checked ? 12 : 0) + 4);
         }
      }

      Minecraft mc = Minecraft.getInstance();
      int sw = mc.getWindow().getGuiScaledWidth();
      int sh = mc.getWindow().getGuiScaledHeight();
      this.w = Math.min(maxW, sw - 4);
      this.h = Math.min(height, sh - 8);
      this.x = atX + this.w > sw - 2 ? Math.max(2, atX - this.w) : Math.max(2, atX);
      boolean up = false;
      if (atY + this.h > sh - 2) {
         if (atY - this.h - 1 >= 2) {
            this.y = atY - this.h - 1;
            up = true;
         } else {
            this.y = Math.max(2, sh - 2 - this.h);
         }
      } else {
         this.y = atY;
      }

      return up;
   }

   public int width() {
      return this.w;
   }

   public int height() {
      return this.h;
   }

   public MenuPopup highlight(int index) {
      if (index >= 0 && index < this.items.size() && !((Item)this.items.get(index)).separator) {
         this.hovered = index;
         this.ensureVisible(index);
      }

      return this;
   }

   public boolean contains(double mx, double my) {
      return !this.closing && mx >= (double)this.x && mx < (double)(this.x + this.w) && my >= (double)this.y && my < (double)(this.y + this.h);
   }

   private int itemTop(int index) {
      int yy = 3;

      for(int i = 0; i < index; ++i) {
         yy += ((Item)this.items.get(i)).separator ? 5 : 14;
      }

      return yy;
   }

   private int itemAt(double my) {
      int yy = this.y + 3 - this.scroll;

      for(int i = 0; i < this.items.size(); ++i) {
         int rh = ((Item)this.items.get(i)).separator ? 5 : 14;
         if (my >= (double)yy && my < (double)(yy + rh)) {
            return ((Item)this.items.get(i)).separator ? -1 : i;
         }

         yy += rh;
      }

      return -1;
   }

   private int contentHeight() {
      int hh = 6;

      for(Item it : this.items) {
         hh += it.separator ? 5 : 14;
      }

      return hh;
   }

   private boolean scrollable() {
      return this.contentHeight() > this.h;
   }

   private void ensureVisible(int index) {
      if (this.scrollable()) {
         int top = this.itemTop(index);
         int bottom = top + 14;
         if (top - this.scroll < 3) {
            this.scroll = Math.max(0, top - 3);
         } else if (bottom - this.scroll > this.h - 3) {
            this.scroll = Math.min(this.contentHeight() - this.h, bottom - this.h + 3);
         }

      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      float o = Math.max(0.0F, Math.min(1.0F, this.open.get()));
      int a = Math.round(255.0F * Math.min(1.0F, o * 1.4F));
      if (a > 2) {
         if (!this.closing && (mouseX != this.mouseLastX || mouseY != this.mouseLastY)) {
            this.mouseLastX = mouseX;
            this.mouseLastY = mouseY;
            if (this.contains((double)mouseX, (double)mouseY)) {
               this.hovered = this.itemAt((double)mouseY);
            }
         }

         int slide = Math.round((1.0F - o) * 4.0F);
         int yy0 = this.opensUpward ? this.y + slide : this.y - slide;
         if (t.isVanilla()) {
            g.fill(this.x, yy0, this.x + this.w, yy0 + this.h, Colors.withAlpha(0, Math.min(a, 240)));
            CFHGuiDraw.outline(g, this.x, yy0, this.w, this.h, Colors.withAlpha(16777215, a), 0);
         } else {
            CFHGuiDraw.popupPlate(g, this.x, yy0, this.w, this.h, p.surface(), p.borderStrong(), 0.6F, (float)a / 255.0F);
         }

         CFHGuiDraw.scissor(g, this.x, yy0, this.w, this.h);
         int yy = yy0 + 3 - this.scroll;

         for(int i = 0; i < this.items.size(); ++i) {
            Item it = (Item)this.items.get(i);
            if (it.separator) {
               CFHGuiDraw.hline(g, this.x + 4, yy + 2, this.w - 8, Colors.withAlpha(t.isVanilla() ? 5263440 : p.border(), a));
               yy += 5;
            } else {
               boolean hov = i == this.hovered && it.enabled;
               if (hov) {
                  CFHGuiDraw.pixelRound(g, this.x + 2, yy, this.w - 4, 14, Colors.withAlpha(t.isVanilla() ? 4210752 : (it.danger ? Colors.mix(p.surfaceHover(), p.danger(), 0.22F) : p.surfaceHover()), a), t.radius() > 0 ? 2 : 0);
               }

               int tx = this.x + 6;
               int fg = Colors.withAlpha(!it.enabled ? p.textDim() : (it.danger ? p.danger() : (hov ? p.text() : (t.isVanilla() ? -2039584 : p.textMuted()))), a);
               if (it.icon != null) {
                  Icons.draw(g, it.icon, tx, yy + 2, 10, fg);
                  tx += 14;
               }

               int avail = this.w - (tx - this.x) - (it.checked ? 14 : 6);
               g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(it.label, avail), tx, yy + 3, fg, t.isVanilla());
               if (it.checked) {
                  Icons.draw(g, Icon.CHECK, this.x + this.w - 12, yy + 3, 8, Colors.withAlpha(p.accentInk(), a));
               }

               yy += 14;
            }
         }

         CFHGuiDraw.unscissor(g);
         if (this.scrollable()) {
            int max = this.contentHeight() - this.h;
            int barH = Math.max(8, this.h * this.h / this.contentHeight());
            int barY = yy0 + 2 + (int)((float)(this.h - 4 - barH) * ((float)this.scroll / (float)Math.max(1, max)));
            CFHGuiDraw.rect(g, this.x + this.w - 3, barY, 2, barH, Colors.withAlpha(t.isVanilla() ? 8421504 : p.borderStrong(), a));
         }

      }
   }

   private void activate(int i) {
      Item it = (Item)this.items.get(i);
      if (it.enabled) {
         CFHGuiSounds.click();
         Popups.close(this);
         if (it.action != null) {
            it.action.run();
         }

      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      int i = this.itemAt(mouseY);
      if (i < 0) {
         return true;
      } else {
         this.activate(i);
         return true;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      int max = Math.max(0, this.contentHeight() - this.h);
      this.scroll = (int)Math.max((double)0.0F, Math.min((double)max, (double)this.scroll - scrollY * (double)14.0F));
      return true;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode != 264 && keyCode != 265) {
         if (keyCode != 268 && keyCode != 269) {
            if ((keyCode == 257 || keyCode == 335 || keyCode == 32) && this.hovered >= 0) {
               this.activate(this.hovered);
               return true;
            } else {
               return false;
            }
         } else {
            int dir = keyCode == 268 ? 1 : -1;
            int i = keyCode == 268 ? -1 : this.items.size();

            for(int n = 0; n < this.items.size(); ++n) {
               i += dir;
               if (!((Item)this.items.get(i)).separator && ((Item)this.items.get(i)).enabled) {
                  this.hovered = i;
                  this.ensureVisible(i);
                  break;
               }
            }

            return true;
         }
      } else {
         int dir = keyCode == 264 ? 1 : -1;
         int i = this.hovered;

         for(int n = 0; n < this.items.size(); ++n) {
            i = ((i + dir) % this.items.size() + this.items.size()) % this.items.size();
            if (!((Item)this.items.get(i)).separator && ((Item)this.items.get(i)).enabled) {
               this.hovered = i;
               this.ensureVisible(i);
               break;
            }
         }

         return true;
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

   public static record Item(Component label, @Nullable Icon icon, boolean enabled, boolean checked, boolean separator, boolean danger, @Nullable Runnable action) {
      public static Item of(Component label, Runnable action) {
         return new Item(label, (Icon)null, true, false, false, false, action);
      }

      public static Item of(Component label, Icon icon, Runnable action) {
         return new Item(label, icon, true, false, false, false, action);
      }

      public static Item danger(Component label, Icon icon, Runnable action) {
         return new Item(label, icon, true, false, false, true, action);
      }

      public static Item disabled(Component label, @Nullable Icon icon) {
         return new Item(label, icon, false, false, false, false, (Runnable)null);
      }

      public static Item checked(Component label, boolean checked, Runnable action) {
         return new Item(label, (Icon)null, true, checked, false, false, action);
      }

      public static Item sep() {
         return new Item(Component.empty(), (Icon)null, false, false, true, false, (Runnable)null);
      }
   }
}
