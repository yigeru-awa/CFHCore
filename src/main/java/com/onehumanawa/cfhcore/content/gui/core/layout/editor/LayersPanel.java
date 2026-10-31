package com.onehumanawa.cfhcore.content.gui.core.layout.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class LayersPanel {
   private static final int ROW = 16;
   private static final int TITLE_H = 18;
   private final EditorOverlay ed;
   private final List<Row> rows = new ArrayList();
   private Rect bounds = Rect.of(0, 0, 10, 10);
   private int scroll;
   private int pressRow = -1;
   private double pressY;
   private boolean dragging;
   private int dropSlot = -1;

   LayersPanel(EditorOverlay ed) {
      this.ed = ed;
   }

   void setBounds(Rect r) {
      this.bounds = r;
      this.clampScroll();
   }

   Rect bounds() {
      return this.bounds;
   }

   boolean contains(double mx, double my) {
      return this.bounds.contains(mx, my);
   }

   void refresh() {
      this.rows.clear();
      List<EditorItem> items = this.ed.canvas().items;
      List<EditorItem> custom = new ArrayList();
      List<EditorItem> vanilla = new ArrayList();

      for(EditorItem it : items) {
         (it.isCustom() ? custom : vanilla).add(it);
      }

      this.rows.add(new Row((EditorItem)null, EditorText.t("layers.elements")));

      for(int i = custom.size() - 1; i >= 0; --i) {
         this.rows.add(new Row((EditorItem)custom.get(i), (Component)null));
      }

      this.rows.add(new Row((EditorItem)null, EditorText.t("layers.widgets")));

      for(int i = vanilla.size() - 1; i >= 0; --i) {
         this.rows.add(new Row((EditorItem)vanilla.get(i), (Component)null));
      }

      this.clampScroll();
   }

   private int customCount() {
      int n = 0;

      for(Row r : this.rows) {
         if (r.item != null && r.item.isCustom()) {
            ++n;
         }
      }

      return n;
   }

   private int listTop() {
      return this.bounds.y() + 18;
   }

   private int listHeight() {
      return this.bounds.h() - 18 - 2;
   }

   private int maxScroll() {
      return Math.max(0, this.rows.size() * 16 - this.listHeight());
   }

   private void clampScroll() {
      this.scroll = Math.max(0, Math.min(this.scroll, this.maxScroll()));
   }

   private int rowAt(double my) {
      if (!(my < (double)this.listTop()) && !(my >= (double)(this.listTop() + this.listHeight()))) {
         int i = (int)((my - (double)this.listTop() + (double)this.scroll) / (double)16.0F);
         return i >= 0 && i < this.rows.size() ? i : -1;
      } else {
         return -1;
      }
   }

   boolean mouseClicked(double mx, double my, int button) {
      this.pressRow = -1;
      this.dragging = false;
      int i = this.rowAt(my);
      if (i < 0) {
         return true;
      } else {
         Row r = (Row)this.rows.get(i);
         if (r.item == null) {
            return true;
         } else {
            EditorItem it = r.item;
            EditorCanvas c = this.ed.canvas();
            int x0 = this.bounds.x() + 4;
            if (button == 0 && mx >= (double)x0 && mx < (double)(x0 + 12)) {
               c.setVisible(it, !it.visible());
               return true;
            } else if (button == 0 && it.isCustom() && mx >= (double)(x0 + 12) && mx < (double)(x0 + 24)) {
               c.setLocked(it, !it.locked());
               return true;
            } else {
               if (button == 0) {
                  if (EditorKeys.shift()) {
                     c.toggleSelect(it.id);
                  } else {
                     c.select(it.id, false);
                  }

                  if (it.isCustom()) {
                     this.pressRow = i;
                     this.pressY = my;
                  }
               }

               return true;
            }
         }
      }
   }

   void mouseDragged(double mx, double my) {
      if (this.pressRow >= 0) {
         if (!this.dragging && Math.abs(my - this.pressY) > (double)3.0F) {
            this.dragging = true;
         }

         if (this.dragging) {
            int n = this.customCount();
            double pos = (my - (double)this.listTop() + (double)this.scroll) / (double)16.0F - (double)1.0F;
            this.dropSlot = (int)Math.max(0L, Math.min((long)n, Math.round(pos)));
         }
      }
   }

   void mouseReleased(double mx, double my) {
      if (this.dragging && this.pressRow >= 0 && this.pressRow < this.rows.size() && this.dropSlot >= 0) {
         EditorItem it = ((Row)this.rows.get(this.pressRow)).item;
         if (it != null && it.isCustom()) {
            int n = this.customCount();
            int display = this.pressRow - 1;
            int slot = display < this.dropSlot ? this.dropSlot - 1 : this.dropSlot;
            this.ed.canvas().reorderElement(it, n - 1 - slot);
         }
      }

      this.pressRow = -1;
      this.dragging = false;
      this.dropSlot = -1;
   }

   boolean mouseScrolled(double mx, double my, double sx, double sy) {
      if (this.maxScroll() == 0) {
         return false;
      } else {
         this.scroll = (int)Math.max((double)0.0F, Math.min((double)this.maxScroll(), (double)this.scroll - sy * (double)16.0F));
         return true;
      }
   }

   void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      int x = this.bounds.x();
      int y = this.bounds.y();
      int w = this.bounds.w();
      int h = this.bounds.h();
      EditorStyle.panel(g, x, y, w, h);
      g.drawString(CFHGuiDraw.font(), Fonts.heading((Component)EditorText.t("layers.title")), x + 6, y + 5, EditorStyle.text(), EditorStyle.vanilla());
      int top = this.listTop();
      int lh = this.listHeight();
      int hoverRow = this.contains((double)mouseX, (double)mouseY) ? this.rowAt((double)mouseY) : -1;
      List<String> selection = this.ed.canvas().selection;
      CFHGuiDraw.scissor(g, x + 1, top, w - 2, lh);
      int ry = top - this.scroll;

      for(int i = 0; i < this.rows.size(); ry += 16) {
         if (ry + 16 >= top && ry <= top + lh) {
            Row r = (Row)this.rows.get(i);
            if (r.item == null) {
               g.drawString(CFHGuiDraw.font(), r.header, x + 6, ry + 4, EditorStyle.dim(), false);
               CFHGuiDraw.hline(g, x + 4, ry + 16 - 2, w - 8, Colors.withAlpha(EditorStyle.panelBorder(), 128));
            } else {
               EditorItem it = r.item;
               boolean sel = selection.contains(it.id);
               boolean hov = i == hoverRow;
               if (sel) {
                  CFHGuiDraw.rect(g, x + 2, ry, w - 4, 16, EditorStyle.rowActive());
                  CFHGuiDraw.rect(g, x + 2, ry + 2, 2, 12, EditorStyle.accent());
               } else if (hov) {
                  CFHGuiDraw.rect(g, x + 2, ry, w - 4, 16, EditorStyle.rowHover());
               }

               int x0 = x + 4;
               boolean vis = it.visible();
               Icons.draw(g, vis ? Icon.EYE : Icon.EYE_OFF, x0 + 1, ry + 3, 10, vis ? EditorStyle.muted() : EditorStyle.dim());
               int tx = x0 + 14;
               if (it.isCustom()) {
                  Icons.draw(g, it.locked() ? Icon.LOCK : Icon.UNLOCK, tx, ry + 3, 10, it.locked() ? EditorStyle.accent() : Colors.withAlpha(EditorStyle.dim(), 128));
                  tx += 13;
               }

               Icons.draw(g, it.icon(), tx, ry + 3, 10, vis ? EditorStyle.muted() : EditorStyle.dim());
               tx += 13;
               int fg = !vis ? EditorStyle.dim() : (sel ? EditorStyle.text() : EditorStyle.muted());
               g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(it.name(), x + w - 8 - tx), tx, ry + 4, fg, false);
            }
         }

         ++i;
      }

      if (this.dragging && this.dropSlot >= 0) {
         int ly = top - this.scroll + (1 + this.dropSlot) * 16;
         CFHGuiDraw.rect(g, x + 4, ly - 1, w - 8, 2, EditorStyle.accent());
      }

      CFHGuiDraw.unscissor(g);
      int max = this.maxScroll();
      if (max > 0) {
         int barH = Math.max(10, lh * lh / (this.rows.size() * 16));
         int by = top + (lh - barH) * this.scroll / max;
         CFHGuiDraw.rect(g, x + w - 4, by, 2, barH, Colors.withAlpha(EditorStyle.dim(), 160));
      }

   }

   private static record Row(@Nullable EditorItem item, @Nullable Component header) {
   }
}
