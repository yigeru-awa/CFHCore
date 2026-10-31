package com.onehumanawa.cfhcore.content.gui.config.search;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class SearchPopup implements Popup {
   public static final int ROW = 18;
   public static final int MAX_ROWS = 8;
   private final int x;
   private final int y;
   private final int w;
   private List<SearchIndex.Hit> hits = new ArrayList();
   private int selected = 0;
   private final Consumer<SearchIndex.Hit> onPick;

   public SearchPopup(int x, int y, int w, Consumer<SearchIndex.Hit> onPick) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.onPick = onPick;
   }

   public void setHits(List<SearchIndex.Hit> newHits) {
      this.hits = newHits;
      this.selected = 0;
   }

   public boolean isEmpty() {
      return this.hits.isEmpty();
   }

   private int height() {
      return Math.min(this.hits.size(), 8) * 18 + 6;
   }

   public boolean contains(double mx, double my) {
      return !this.hits.isEmpty() && mx >= (double)this.x && mx < (double)(this.x + this.w) && my >= (double)this.y && my < (double)(this.y + this.height());
   }

   private void pick(int i) {
      if (i >= 0 && i < this.hits.size()) {
         SearchIndex.Hit h = (SearchIndex.Hit)this.hits.get(i);
         Popups.close(this);
         CFHGuiSounds.tick();
         this.onPick.accept(h);
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      if (!this.hits.isEmpty()) {
         Theme t = Theme.current();
         Palette p = t.palette();
         int h = this.height();
         int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
         int px = Math.min(this.x, sw - this.w - 2);
         if (t.isVanilla()) {
            g.fill(px, this.y, px + this.w, this.y + h, -267382768);
            CFHGuiDraw.outline(g, px, this.y, this.w, h, -1, 0);
         } else {
            CFHGuiDraw.popupPlate(g, px, this.y, this.w, h, p.surface(), p.borderStrong(), 0.6F, 1.0F);
         }

         int ry = this.y + 3;

         for(int i = 0; i < Math.min(this.hits.size(), 8); ++i) {
            SearchIndex.Hit hit = (SearchIndex.Hit)this.hits.get(i);
            boolean hov = mouseX >= px && mouseX < px + this.w && mouseY >= ry && mouseY < ry + 18;
            if (hov) {
               this.selected = i;
            }

            boolean sel = i == this.selected;
            if (sel) {
               CFHGuiDraw.pixelRound(g, px + 2, ry, this.w - 4, 18, t.isVanilla() ? 1090519039 : p.surfaceHover(), t.radius() > 0 ? 2 : 0);
            }

            Component crumb = hit.entry().crumb();
            int fg = sel ? p.text() : (t.isVanilla() ? -2039584 : p.textMuted());
            g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(hit.entry().binding().label(), this.w - 14), px + 8, ry + 1, fg, t.isVanilla());
            g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(crumb, this.w - 14), px + 8, ry + 10, Colors.withAlpha(p.textDim(), 255), false);
            ry += 18;
         }

         if (this.hits.size() > 8) {
            Icons.draw(g, Icon.DOTS, px + this.w - 14, this.y + h - 10, 8, p.textDim());
         }

      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      int i = (int)((mouseY - (double)this.y - (double)3.0F) / (double)18.0F);
      if (i >= 0 && i < Math.min(this.hits.size(), 8)) {
         this.pick(i);
      }

      return true;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.hits.isEmpty()) {
         return false;
      } else if (keyCode == 264) {
         this.selected = Math.min(this.hits.size() - 1, this.selected + 1);
         return true;
      } else if (keyCode == 265) {
         this.selected = Math.max(0, this.selected - 1);
         return true;
      } else if (keyCode != 257 && keyCode != 335) {
         return false;
      } else {
         this.pick(this.selected);
         return true;
      }
   }
}
