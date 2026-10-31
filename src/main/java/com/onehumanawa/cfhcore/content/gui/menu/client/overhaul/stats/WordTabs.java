package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.stats;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.StatsTab;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

final class WordTabs extends CFHGuiWidget {
   static final int HEIGHT = 16;
   private static final int GAP = 12;
   private static final int ICON = 10;
   private final List<StatsTab> tabs;
   private final List<Component> words = new ArrayList();
   private final List<Anim> lifts = new ArrayList();
   private final StatsTab open;
   private final Consumer<StatsTab> onPick;
   private final Anim slide;
   private final boolean spelt;

   WordTabs(int x, int y, int w, List<StatsTab> tabs, StatsTab open, Anim slide, Consumer<StatsTab> onPick) {
      super(x, y, w, 16, Component.translatable("cfhgui.stats.tabs"));
      this.tabs = List.copyOf(tabs);
      this.open = open;
      this.slide = slide;
      this.onPick = onPick;
      int all = 0;

      for(StatsTab t : tabs) {
         Component word = Fonts.heading(t.title());
         this.words.add(word);
         this.lifts.add(new Anim(0.0F, 140, Ease.OUT_CUBIC));
         all += 14 + CFHGuiDraw.width(word) + 12;
      }

      this.spelt = all - 12 <= w;
      slide.set((float)Math.max(0, tabs.indexOf(open)));
   }

   private boolean worded(int i) {
      return this.spelt || this.tabs.get(i) == this.open;
   }

   private int tabW(int i) {
      return 10 + (this.worded(i) ? 4 + CFHGuiDraw.width((Component)this.words.get(i)) : 0);
   }

   private int tabX(int i) {
      int x = this.getX();

      for(int k = 0; k < i; ++k) {
         x += this.tabW(k) + 12;
      }

      return x;
   }

   private int tabAt(double mouseX, double mouseY) {
      if (!this.contains(mouseX, mouseY)) {
         return -1;
      } else {
         for(int i = 0; i < this.tabs.size(); ++i) {
            int tx = this.tabX(i);
            if (mouseX >= (double)tx - (double)6.0F && mouseX < (double)(tx + this.tabW(i)) + (double)6.0F) {
               return i;
            }
         }

         return -1;
      }
   }

   public void onClick(double mouseX, double mouseY) {
      int i = this.tabAt(mouseX, mouseY);
      if (i >= 0) {
         super.onClick(mouseX, mouseY);
         this.pick(i);
      }
   }

   private void pick(int i) {
      if (i >= 0 && i < this.tabs.size() && this.tabs.get(i) != this.open) {
         CFHGuiSounds.tick();
         this.onPick.accept((StatsTab)this.tabs.get(i));
      }
   }

   public void playDownSound(SoundManager handler) {
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.active && this.visible && !this.tabs.isEmpty()) {
         int at = Math.max(0, this.tabs.indexOf(this.open));
         switch (keyCode) {
            case 262:
               this.pick(Math.min(this.tabs.size() - 1, at + 1));
               break;
            case 263:
               this.pick(Math.max(0, at - 1));
               break;
            case 264:
            case 265:
            case 266:
            case 267:
            default:
               return false;
            case 268:
               this.pick(0);
               break;
            case 269:
               this.pick(this.tabs.size() - 1);
         }

         return true;
      } else {
         return false;
      }
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Palette p = Theme.current().palette();
      this.draw(g, mouseX, mouseY, p.accentInk(), p.accentInk(), p.text(), p.textMuted());
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, mouseX, mouseY, -1, -1, -1, -3618616);
   }

   private void draw(GuiGraphics g, int mouseX, int mouseY, int bar, int mark, int lit, int rest) {
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F)) {
         int y = this.getY() + this.enterOffset();
         int over = this.isHovered() ? this.tabAt((double)mouseX, (double)mouseY) : -1;

         for(int i = 0; i < this.tabs.size(); ++i) {
            StatsTab t = (StatsTab)this.tabs.get(i);
            boolean isOpen = t == this.open;
            Anim lift = (Anim)this.lifts.get(i);
            lift.set(i == over && !isOpen);
            float up = lift.get();
            int tx = this.tabX(i);
            int ink = Colors.scaleAlpha(isOpen ? lit : Colors.lerp(rest, lit, up), a);
            Icons.draw(g, t.icon, tx, y + 1, 10, Colors.scaleAlpha(isOpen ? mark : Colors.lerp(rest, lit, up), a));
            if (this.worded(i)) {
               g.drawString(CFHGuiDraw.font(), (Component)this.words.get(i), tx + 10 + 4, y + 2, ink, true);
            } else if (i == over) {
               CFHGuiTooltips.request(t.title(), this);
            }
         }

         float s = Math.max(0.0F, Math.min((float)(this.tabs.size() - 1), this.slide.get()));
         int i0 = (int)Math.floor((double)s);
         int i1 = Math.min(this.tabs.size() - 1, i0 + 1);
         float f = s - (float)i0;
         int bx = Math.round((float)this.tabX(i0) + (float)(this.tabX(i1) - this.tabX(i0)) * f);
         int bw = Math.round((float)this.tabW(i0) + (float)(this.tabW(i1) - this.tabW(i0)) * f);
         CFHGuiDraw.rect(g, bx, y + 16 - 3, bw, 2, Colors.scaleAlpha(bar, a));
         int at = Math.max(0, this.tabs.indexOf(this.open));
         CFHGuiDraw.focusRing(g, this.tabX(at) - 2, y - 1, this.tabW(at) + 4, 16, this.focus() * a);
      }
   }

   protected void updateWidgetNarration(NarrationElementOutput out) {
      out.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.tab", new Object[]{this.open.title()}));
      out.add(NarratedElementType.POSITION, Component.translatable("narrator.position.tab", new Object[]{Math.max(0, this.tabs.indexOf(this.open)) + 1, this.tabs.size()}));
   }
}
