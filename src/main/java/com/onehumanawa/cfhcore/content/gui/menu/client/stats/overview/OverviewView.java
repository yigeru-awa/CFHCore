package com.onehumanawa.cfhcore.content.gui.menu.client.stats.overview;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.StatFmt;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.StatMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.StatsHost;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.Summary;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.table.RowIcons;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.table.StatTable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class OverviewView {
   private static final int GAP = 8;
   private static final int LINE_H = 11;
   private static final int CHIP_H = 24;
   private final StatsHost host;
   private final State state;
   private OverviewModel model;
   private String shape;
   private @Nullable Runnable relayout;
   private @Nullable CFHGuiScrollPanel panel;
   private final List<Card> cards = new ArrayList();

   public OverviewView(StatsHost host, State state, OverviewModel model) {
      this.host = host;
      this.state = state;
      this.model = model;
      this.shape = model.shape();
   }

   public OverviewView onRelayout(Runnable r) {
      this.relayout = r;
      return this;
   }

   public void model(OverviewModel next) {
      this.model = next;
      String nextShape = next.shape();
      if (!nextShape.equals(this.shape)) {
         this.shape = nextShape;
         if (this.relayout != null && this.panel != null) {
            this.relayout.run();
         }

      } else {
         int i = 0;
         boolean resized = false;

         for(Card card : this.cards) {
            if (card instanceof GroupCard) {
               GroupCard gc = (GroupCard)card;
               gc.group = (OverviewModel.Group)next.groups.get(i++);
            } else if (card instanceof RecordsCard) {
               RecordsCard rc = (RecordsCard)card;
               rc.lines = next.records;
            }

            int before = card.getHeight();
            card.arrange();
            resized |= card.getHeight() != before;
         }

         if (resized && this.relayout != null && this.panel != null) {
            this.relayout.run();
         }

      }
   }

   public @Nullable AbstractWidget panel() {
      return this.panel;
   }

   public void build(Rect area, Consumer<AbstractWidget> add) {
      this.cards.clear();
      CFHGuiScrollPanel p = new CFHGuiScrollPanel(area.x(), area.y(), area.w(), area.h());
      int inner = p.innerWidth();
      int columns = Mth.clamp(inner / 176, 1, 3);
      int cardW = (inner - (columns - 1) * 8) / columns;
      List<GroupCard> made = new ArrayList();

      for(OverviewModel.Group group : this.model.groups) {
         made.add(new GroupCard(group, cardW));
      }

      int[] column = columnsFor(made, columns);
      int[] bottom = new int[columns];
      GroupCard[] last = new GroupCard[columns];

      for(int i = 0; i < made.size(); ++i) {
         GroupCard card = (GroupCard)made.get(i);
         int col = column[i];
         p.add(card, col * (cardW + 8), bottom[col]);
         bottom[col] += card.getHeight() + 8;
         last[col] = card;
         this.cards.add(card);
      }

      int y = 0;

      for(int b : bottom) {
         y = Math.max(y, b);
      }

      for(int c = 0; c < columns; ++c) {
         if (last[c] != null && bottom[c] < y) {
            last[c].stretch(y - bottom[c]);
         }
      }

      RecordsCard records = new RecordsCard(this.model.records, inner);
      p.add(records, 0, y);
      this.cards.add(records);
      p.setContentHeight(y + records.getHeight() + 2);
      p.snapScroll(this.state.scroll);
      this.panel = p;
      add.accept(p);
   }

   private static int[] columnsFor(List<GroupCard> cards, int columns) {
      int n = cards.size();
      int[] best = new int[n];
      if (columns > 1 && n != 0) {
         int[] h = new int[n];

         for(int i = 0; i < n; ++i) {
            h[i] = ((GroupCard)cards.get(i)).getHeight() + 8;
         }

         if (Math.pow((double)columns, (double)n) > (double)60000.0F) {
            int[] sum = new int[columns];

            for(int i = 0; i < n; ++i) {
               int col = 0;

               for(int c = 1; c < columns; ++c) {
                  if (sum[c] < sum[col]) {
                     col = c;
                  }
               }

               best[i] = col;
               sum[col] += h[i];
            }

            return best;
         } else {
            int[] at = new int[n];
            int[] sum = new int[columns];
            int total = (int)Math.pow((double)columns, (double)n);
            int bestMax = Integer.MAX_VALUE;
            int bestSpread = Integer.MAX_VALUE;

            for(int code = 0; code < total; ++code) {
               int rest = code;

               for(int i = n - 1; i >= 0; --i) {
                  at[i] = rest % columns;
                  rest /= columns;
               }

               Arrays.fill(sum, 0);

               for(int i = 0; i < n; ++i) {
                  sum[at[i]] += h[i];
               }

               int max = 0;
               int min = Integer.MAX_VALUE;

               for(int s : sum) {
                  max = Math.max(max, s);
                  min = Math.min(min, s);
               }

               if (max < bestMax || max == bestMax && max - min < bestSpread) {
                  bestMax = max;
                  bestSpread = max - min;
                  System.arraycopy(at, 0, best, 0, n);
               }
            }

            return best;
         }
      } else {
         return best;
      }
   }

   private static int value(GuiGraphics g, StatFmt.Shown shown, int right, int y, int color, boolean shadow) {
      int heart = shown.hearts() ? 10 : 0;
      int w = CFHGuiDraw.width(shown.text());
      g.drawString(CFHGuiDraw.font(), shown.text(), right - heart - w, y, color, shadow);
      if (shown.hearts()) {
         StatTable.heart(g, right - 9, y, (float)Colors.alpha(color) / 255.0F);
      }

      return w + heart;
   }

   public static final class State {
      public double scroll;
   }

   private static record Hit(int x, int y, int w, int h, OverviewModel.Line line) {
   }

   private abstract class Card extends CFHGuiWidget {
      protected final List<Hit> hits = new ArrayList();
      protected int cursor = -1;

      Card(final int w, final Component title) {
         super(0, 0, w, 10, title);
      }

      abstract void arrange();

      abstract void drawContent(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6, float var7, boolean var8);

      protected @Nullable Hit hitAt(double mouseX, double mouseY) {
         double rx = mouseX - (double)this.getX();
         double ry = mouseY - (double)this.getY();

         for(Hit h : this.hits) {
            if (rx >= (double)h.x() && rx < (double)(h.x() + h.w()) && ry >= (double)h.y() && ry < (double)(h.y() + h.h())) {
               return h;
            }
         }

         return null;
      }

      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         if (this.active && this.visible && this.contains(mouseX, mouseY)) {
            Hit hit = this.hitAt(mouseX, mouseY);
            if (hit == null) {
               return false;
            } else {
               this.cursor = this.hits.indexOf(hit);
               if (button == 1) {
                  this.menu(hit.line(), mouseX, mouseY);
                  return true;
               } else if (button == 0 && hit.line().leadsTo() != null) {
                  CFHGuiSounds.click();
                  OverviewView.this.host.openTab(hit.line().leadsTo());
                  return true;
               } else {
                  return button == 0;
               }
            }
         } else {
            return false;
         }
      }

      public void playDownSound(SoundManager handler) {
      }

      public void setFocused(boolean focused) {
         super.setFocused(focused);
         if (!focused) {
            this.cursor = -1;
         } else if (this.cursor < 0 && CFHGuiDraw.focusShown() && !this.hits.isEmpty()) {
            this.cursor = 0;
         }

      }

      public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
         if (this.active && this.visible && !this.hits.isEmpty()) {
            switch (keyCode) {
               case 32:
               case 257:
               case 335:
                  if (this.cursor < 0) {
                     return false;
                  }

                  OverviewModel.Line line = ((Hit)this.hits.get(this.cursor)).line();
                  if (line.leadsTo() != null) {
                     OverviewView.this.host.openTab(line.leadsTo());
                  } else {
                     this.menuAtCursor();
                  }
                  break;
               case 67:
                  if (!Screen.hasControlDown() || this.cursor < 0) {
                     return false;
                  }

                  OverviewModel.Line line2 = ((Hit)this.hits.get(this.cursor)).line();
                  StatMenu.copy(OverviewView.this.host, new StatMenu.Subject(line2.label(), line2.value(), line2.key()));
                  break;
               case 264:
                  if (this.cursor >= this.hits.size() - 1) {
                     return false;
                  }

                  ++this.cursor;
                  break;
               case 265:
                  if (this.cursor <= 0) {
                     return false;
                  }

                  --this.cursor;
                  break;
               case 299:
                  if (!Screen.hasShiftDown() || !this.menuAtCursor()) {
                     return false;
                  }
                  break;
               case 348:
                  if (!this.menuAtCursor()) {
                     return false;
                  }
                  break;
               default:
                  return false;
            }

            return true;
         } else {
            return false;
         }
      }

      private boolean menuAtCursor() {
         if (this.cursor >= 0 && this.cursor < this.hits.size()) {
            Hit h = (Hit)this.hits.get(this.cursor);
            this.menu(h.line(), (double)(this.getX() + h.x()) + (double)h.w() / (double)2.0F, (double)(this.getY() + h.y() + h.h()));
            return true;
         } else {
            return false;
         }
      }

      private void menu(OverviewModel.Line line, double mouseX, double mouseY) {
         StatMenu.open(OverviewView.this.host, mouseX, mouseY, new StatMenu.Subject(line.label(), line.value(), line.unknown() ? null : line.key()), List.of());
      }

      protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.draw(g, mouseX, mouseY, false);
      }

      protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.draw(g, mouseX, mouseY, true);
      }

      private void draw(GuiGraphics g, int mouseX, int mouseY, boolean van) {
         float a = this.effectiveAlpha();
         if (!(a <= 0.004F)) {
            if (OverviewView.this.panel != null) {
               OverviewView.this.state.scroll = OverviewView.this.panel.scrollAmount();
            }

            Theme t = Theme.current();
            Palette p = t.palette();
            int x = this.getX();
            int y = this.getY() + this.enterOffset();
            int w = this.getWidth();
            int h = this.getHeight();
            boolean plate = !OverviewView.this.host.onPanel();
            if (plate) {
               if (van) {
                  CFHGuiDraw.floatingPanel(g, x, y, w, h, a);
               } else {
                  CFHGuiDraw.plate(g, x, y, w, h, p.surface(), p.border(), 0.4F, a);
               }

               Theme.enterPanel();
            }

            try {
               Hit over = this.isHovered() ? this.hitAt((double)mouseX, (double)mouseY) : null;
               Hit lit = over != null ? over : (this.cursor >= 0 && this.cursor < this.hits.size() && this.isFocused() && CFHGuiDraw.focusShown() ? (Hit)this.hits.get(this.cursor) : null);
               if (lit != null) {
                  int fill = van ? 620756991 : t.panel(p.surfaceHover());
                  CFHGuiDraw.pixelRound(g, x + lit.x(), y + lit.y(), lit.w(), lit.h(), Colors.scaleAlpha(fill, a), van ? 0 : 2);
               }

               this.drawContent(g, x, y, w, mouseX, mouseY, a, van);
               if (over != null) {
                  this.tooltip(over.line());
               }
            } finally {
               if (plate) {
                  Theme.exitPanel();
               }

            }

            CFHGuiDraw.focusRing(g, x, y, w, h, this.focus() * a);
         }
      }

      private void tooltip(OverviewModel.Line line) {
         List<Component> lines = new ArrayList(3);
         String exact = line.value().exact();
         lines.add(!exact.isEmpty() && !line.unknown() ? Component.translatable("cfhgui.stats.line", new Object[]{line.label(), exact}) : line.label());
         if (line.tip() != null) {
            lines.add(line.tip().copy().withStyle(ChatFormatting.GRAY));
         }

         if (line.leadsTo() != null) {
            lines.add(Component.translatable("cfhgui.stats.hint.open", new Object[]{line.leadsTo().title()}).withStyle(ChatFormatting.DARK_GRAY));
         }

         CFHGuiTooltips.request(lines, (AbstractWidget)null);
      }

      protected void updateWidgetNarration(NarrationElementOutput out) {
         out.add(NarratedElementType.TITLE, this.getMessage());
         if (this.cursor >= 0 && this.cursor < this.hits.size()) {
            OverviewModel.Line line = ((Hit)this.hits.get(this.cursor)).line();
            out.add(NarratedElementType.HINT, Component.translatable("cfhgui.stats.line", new Object[]{line.label(), line.value().spoken()}));
         }

      }
   }

   private final class GroupCard extends Card {
      private OverviewModel.Group group;
      private int scale = 2;
      private int heroH;
      private int barY = -1;
      private int linesY;
      private int stretch;
      private boolean captionBeside;

      GroupCard(final OverviewModel.Group group, final int w) {
         super(w, group.title());
         this.group = group;
         this.arrange();
      }

      void stretch(int extra) {
         this.stretch = Math.max(0, extra);
         this.arrange();
      }

      void arrange() {
         this.hits.clear();
         int w = this.getWidth();
         this.scale = w >= 150 ? 2 : 1;
         int valueW = CFHGuiDraw.width(Fonts.heading(this.group.hero().value().text())) * this.scale + (this.group.hero().value().hearts() ? 9 * this.scale + 2 : 0);
         this.captionBeside = valueW + 8 + CFHGuiDraw.width(this.group.hero().label()) <= w - 16;
         this.heroH = 9 * this.scale + (this.captionBeside ? 2 : 12);
         int y = 20;
         this.hits.add(new Hit(4, y - 2, w - 8, this.heroH + 2, this.group.hero()));
         y += this.heroH + 4;
         if (!this.group.ways().isEmpty()) {
            this.barY = y;
            y += 10;
         } else {
            this.barY = -1;
         }

         this.linesY = y;

         for(OverviewModel.Line line : this.group.lines()) {
            this.hits.add(new Hit(4, y, w - 8, 11, line));
            y += 11;
         }

         this.height = y + 6 + this.stretch;
      }

      private int tone(int index, boolean van) {
         Palette p = Theme.current().palette();
         float f = (float)index / 4.0F;
         return van ? Colors.lerp(-1, -9408400, f) : Colors.lerp(p.text(), p.textDim(), f);
      }

      void drawContent(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY, float a, boolean van) {
         Palette p = Theme.current().palette();
         int ink = Colors.scaleAlpha(van ? -1 : p.text(), a);
         int soft = Colors.scaleAlpha(van ? -5197648 : p.textMuted(), a);
         int dim = Colors.scaleAlpha(van ? -7303024 : p.textDim(), a);
         CFHGuiDraw.sectionRule(g, this.group.title(), x + 8, y + 6, w - 16, a);
         OverviewModel.Line hero = this.group.hero();
         Component big = Fonts.heading(hero.value().text());
         g.pose().pushPose();
         g.pose().translate((float)(x + 8), (float)(y + 20), 0.0F);
         g.pose().scale((float)this.scale, (float)this.scale, 1.0F);
         g.drawString(CFHGuiDraw.font(), big, 0, 0, ink, van);
         g.pose().popPose();
         int bigW = CFHGuiDraw.width(big) * this.scale;
         if (hero.value().hearts()) {
            g.pose().pushPose();
            g.pose().translate((float)(x + 8 + bigW + 2), (float)(y + 20), 0.0F);
            g.pose().scale((float)this.scale, (float)this.scale, 1.0F);
            StatTable.heart(g, 0, 1, a);
            g.pose().popPose();
            bigW += 9 * this.scale + 2;
         }

         if (this.captionBeside) {
            g.drawString(CFHGuiDraw.font(), hero.label(), x + 8 + bigW + 6, y + 20 + 7 * (this.scale - 1), soft, van);
         } else {
            g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(hero.label(), w - 16), x + 8, y + 20 + 9 * this.scale + 2, soft, van);
         }

         if (this.barY >= 0) {
            this.bar(g, x + 8, y + this.barY, w - 16, a, van);
         }

         int ly = y + this.linesY;
         int index = 0;

         for(OverviewModel.Line line : this.group.lines()) {
            int lx = x + 8;
            if (index < this.group.ways().size()) {
               CFHGuiDraw.rect(g, lx, ly + 2, 5, 5, Colors.scaleAlpha(this.tone(index, van), a));
               lx += 9;
            }

            int valueW = OverviewView.value(g, line.value(), x + w - 8, ly + 1, line.unknown() ? dim : ink, van);
            g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(line.label(), x + w - 8 - valueW - 6 - lx), lx, ly + 1, soft, van);
            ly += 11;
            ++index;
         }

      }

      private void bar(GuiGraphics g, int x, int y, int w, float a, boolean van) {
         Palette p = Theme.current().palette();
         CFHGuiDraw.rect(g, x, y, w, 6, Colors.scaleAlpha(van ? 1610612736 : Colors.withAlpha(p.surfaceActive(), 192), a));
         long total = Math.max(1L, this.group.waysTotal());
         int px = x;
         long shownCm = 0L;
         List<Summary.Way> ways = this.group.ways();

         for(int i = 0; i < ways.size(); ++i) {
            shownCm += ((Summary.Way)ways.get(i)).cm();
            int end = x + (int)Math.round((double)w * (double)shownCm / (double)total);
            if (end > px) {
               CFHGuiDraw.rect(g, px, y, Math.max(1, end - px - (end < x + w ? 1 : 0)), 6, Colors.scaleAlpha(this.tone(i, van), a));
            }

            px = end;
         }

         if (px < x + w - 1) {
            CFHGuiDraw.rect(g, px, y, x + w - px, 6, Colors.scaleAlpha(this.tone(4, van), a));
         }

      }
   }

   private final class RecordsCard extends Card {
      private List<OverviewModel.Line> lines;
      private int columns = 1;
      private int chipW;

      RecordsCard(final List<OverviewModel.Line> lines, final int w) {
         super(w, Component.translatable("cfhgui.stats.group.records"));
         this.lines = lines;
         this.arrange();
      }

      void arrange() {
         this.hits.clear();
         int w = this.getWidth();
         this.columns = Mth.clamp((w - 8) / 138, 1, 4);
         this.chipW = (w - 8) / this.columns;
         int i = 0;

         for(OverviewModel.Line line : this.lines) {
            this.hits.add(new Hit(4 + i % this.columns * this.chipW, 20 + i / this.columns * 24, this.chipW, 22, line));
            ++i;
         }

         int rows = this.lines.isEmpty() ? 1 : (this.lines.size() + this.columns - 1) / this.columns;
         this.height = 20 + rows * 24 + 4;
      }

      void drawContent(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY, float a, boolean van) {
         Palette p = Theme.current().palette();
         int ink = Colors.scaleAlpha(van ? -1 : p.text(), a);
         int soft = Colors.scaleAlpha(van ? -5197648 : p.textMuted(), a);
         CFHGuiDraw.sectionRule(g, this.getMessage(), x + 8, y + 6, w - 16, a);
         if (this.lines.isEmpty()) {
            g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(Component.translatable("cfhgui.stats.records.empty"), w - 16), x + 8, y + 26, Colors.scaleAlpha(van ? -7303024 : p.textDim(), a), van);
         } else {
            for(Hit hit : this.hits) {
               int cx = x + hit.x();
               int cy = y + hit.y();
               RowIcons.draw(g, hit.line().icon(), cx + 3, cy + 3, soft, a);
               FormattedCharSequence caption = CFHGuiDraw.truncate(hit.line().label(), hit.w() - 27);
               g.drawString(CFHGuiDraw.font(), caption, cx + 23, cy + 2, soft, van);
               g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(Component.literal(hit.line().value().text()), hit.w() - 27), cx + 23, cy + 12, ink, van);
            }

         }
      }
   }
}
