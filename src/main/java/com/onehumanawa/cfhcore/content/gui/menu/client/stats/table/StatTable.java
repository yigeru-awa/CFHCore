package com.onehumanawa.cfhcore.content.gui.menu.client.stats.table;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiList;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.StatFmt;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.StatNames;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.StatKind;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class StatTable {
   public static final int ROW_H = 18;
   public static final int TOOL_H = 20;
   public static final int FOOT_H = 20;
   public static final int GAP = 4;
   private static final int HEAD_WORDS = 14;
   private static final int HEAD_PICTURES = 20;
   private final List<Column> columns;
   private final State state;
   private final Listener listener;
   private final int ownSort;
   private boolean searchable = true;
   private boolean icons = true;
   private boolean ranks;
   private boolean pickOnClick;
   private Component emptyText = Component.empty();
   private @Nullable Runnable relayout;
   private List<TableRow> all = List.of();
   private List<TableRow> shown = List.of();
   private final List<String> namespaces = new ArrayList();
   private long[] largest = new long[0];
   private int[] widestText = new int[0];
   private @Nullable TableRow pinned;
   private int dataRows;
   private boolean onPanel;
   private boolean footBuilt;
   private int bodyRoom = Integer.MAX_VALUE;
   private Rect ground = new Rect(0, 0, 0, 0);
   private @Nullable CFHGuiSearchField search;
   private Head head;
   private @Nullable Body body;
   private int[] columnW = new int[0];
   private int nameW;
   private boolean pictures;
   private long lastClickMs;
   private @Nullable String lastClickId;

   public StatTable(List<Column> columns, State state, int ownSort, Listener listener) {
      this.columns = List.copyOf(columns);
      this.state = state;
      this.ownSort = ownSort;
      this.listener = listener;
      this.largest = new long[columns.size()];
      this.widestText = new int[columns.size()];
   }

   public StatTable plain() {
      this.searchable = false;
      return this;
   }

   public StatTable noIcons() {
      this.icons = false;
      return this;
   }

   public StatTable ranked() {
      this.ranks = true;
      return this;
   }

   public StatTable pickOnClick() {
      this.pickOnClick = true;
      return this;
   }

   public StatTable empty(Component text) {
      this.emptyText = text;
      return this;
   }

   public StatTable onRelayout(Runnable r) {
      this.relayout = r;
      return this;
   }

   public void rows(List<TableRow> rows) {
      this.all = rows;
      TreeSet<String> spaces = new TreeSet();

      for(TableRow r : rows) {
         if (!r.section && !r.namespace.isEmpty()) {
            spaces.add(r.namespace);
         }
      }

      boolean hadFilter = this.namespaces.size() > 1;
      boolean hasFilter = spaces.size() > 1;
      boolean changed = !this.namespaces.equals(new ArrayList(spaces));
      this.namespaces.clear();
      this.namespaces.addAll(spaces);
      if (!this.state.mod.isEmpty() && !spaces.contains(this.state.mod)) {
         this.state.mod = "";
      }

      this.arrange();
      if (this.body != null && this.relayout != null && (hadFilter != hasFilter || hasFilter && changed)) {
         this.relayout.run();
      }

   }

   public void pinned(@Nullable TableRow row) {
      this.pinned = row;
      if (this.body != null && this.relayout != null && this.footBuilt != this.wantsFoot()) {
         this.relayout.run();
      }

   }

   public StatTable onPanel(boolean on) {
      this.onPanel = on;
      return this;
   }

   public List<TableRow> rows() {
      return this.all;
   }

   private void arrange() {
      String[] words = this.state.query.trim().toLowerCase(Locale.ROOT).split("\\s+");
      List<TableRow> out = new ArrayList(this.all.size());
      List<TableRow> run = new ArrayList();
      TableRow section = null;
      int count = 0;

      for(int i = 0; i <= this.all.size(); ++i) {
         TableRow r = i < this.all.size() ? (TableRow)this.all.get(i) : null;
         if (r != null && !r.section) {
            if (this.state.mod.isEmpty() || this.state.mod.equals(r.namespace)) {
               boolean match = true;

               for(String w : words) {
                  if (!w.isEmpty() && !r.search.contains(w)) {
                     match = false;
                     break;
                  }
               }

               if (match) {
                  run.add(r);
               }
            }
         } else {
            if (!run.isEmpty()) {
               this.sort(run);
               if (section != null) {
                  out.add(section);
               }

               out.addAll(run);
               count += run.size();
               run.clear();
            }

            section = r;
         }
      }

      this.shown = out;
      this.dataRows = count;

      for(int c = 0; c < this.largest.length; ++c) {
         long max = 0L;
         int widest = 0;
         boolean bar = ((Column)this.columns.get(c)).bar();

         for(TableRow r : out) {
            if (!r.section && r.value(c) != Long.MIN_VALUE) {
               max = Math.max(max, r.value(c));
               if (bar) {
                  widest = Math.max(widest, CFHGuiDraw.width(r.written(c, this.kindOf(r, c)).text()));
               }
            }
         }

         this.largest[c] = max;
         this.widestText[c] = widest;
      }

      if (this.body != null) {
         this.body.emptyText((Component)(this.all.isEmpty() ? this.emptyText : Component.translatable("cfhgui.stats.no_match")));
         this.body.items(out);
         if (this.relayout != null && this.footBuilt != this.wantsFoot()) {
            this.relayout.run();
         }
      }

   }

   private boolean wantsFoot() {
      TableRow line = this.pinned;
      if (line == null) {
         return false;
      } else if (this.shown.size() * 18 > this.bodyRoom) {
         return true;
      } else {
         for(TableRow r : this.shown) {
            if (!r.section && r.id.equals(line.id)) {
               return false;
            }
         }

         return true;
      }
   }

   private int effectiveSort() {
      return this.state.sort == Integer.MIN_VALUE ? this.ownSort : this.state.sort;
   }

   private void sort(List<TableRow> rows) {
      int by = this.effectiveSort();
      if (by != Integer.MIN_VALUE) {
         Comparator<TableRow> byName = Comparator.comparing((r) -> r.label, String.CASE_INSENSITIVE_ORDER);
         if (by == -1) {
            rows.sort(this.state.sort != Integer.MIN_VALUE && !this.state.ascending ? byName.reversed() : byName);
         } else {
            boolean up = this.state.sort != Integer.MIN_VALUE && this.state.ascending;
            rows.sort((a, b) -> {
               long va = a.value(by);
               long vb = b.value(by);
               if (va != Long.MIN_VALUE && vb != Long.MIN_VALUE) {
                  if (va != vb) {
                     return up ? Long.compare(va, vb) : Long.compare(vb, va);
                  }
               } else if (va != vb) {
                  return va == Long.MIN_VALUE ? 1 : -1;
               }

               return byName.compare(a, b);
            });
         }
      }
   }

   public void build(Rect area, Consumer<AbstractWidget> add) {
      int y = area.y();
      this.search = null;
      if (this.searchable) {
         boolean filter = this.namespaces.size() > 1;
         int filterW = filter ? Mth.clamp(area.w() / 3, 70, 130) : 0;
         int searchW = Mth.clamp(area.w() - filterW - (filter ? 4 : 0), 60, 200);
         this.search = new CFHGuiSearchField(area.x(), y, searchW, (q) -> {
            if (!q.equals(this.state.query)) {
               this.state.query = q;
               this.state.scroll = (double)0.0F;
               if (this.body != null) {
                  this.body.snapScroll((double)0.0F);
               }

               this.arrange();
            }
         });
         this.search.setValue(this.state.query);
         add.accept(this.search);
         if (filter) {
            List<String> options = new ArrayList();
            options.add("");
            options.addAll(this.namespaces);
            CFHGuiDropdown<String> mods = new CFHGuiDropdown<String>(area.x() + searchW + 4, y, filterW, options, this.state.mod, (ns) -> ns.isEmpty() ? Component.translatable("cfhgui.stats.filter.all") : Component.literal(StatNames.modName(ns)), (ns) -> {
               this.state.mod = ns;
               this.state.scroll = (double)0.0F;
               if (this.body != null) {
                  this.body.snapScroll((double)0.0F);
               }

               this.arrange();
            });
            mods.tip(Component.translatable("cfhgui.stats.filter.tip"));
            add.accept(mods);
         }

         y += 24;
      }

      int pad = this.onPanel ? 0 : 4;
      this.ground = new Rect(area.x(), y, area.w(), Math.max(0, area.bottom() - y));
      int x = area.x() + pad;
      int w = area.w() - pad * 2;
      int bottom = area.bottom() - (this.onPanel ? 0 : 3);
      if (!this.onPanel) {
         y += 3;
      }

      this.layout(w - 8);
      int headH = this.pictures ? 20 : 14;
      this.head = new Head(x, y, w, headH);
      add.accept(this.head);
      y += headH + 1;
      this.bodyRoom = bottom - y;
      this.footBuilt = this.wantsFoot();
      int footH = this.footBuilt ? 22 : 0;
      this.body = new Body(x, y, w, Math.max(18, this.bodyRoom - footH));
      this.body.emptyText((Component)(this.all.isEmpty() ? this.emptyText : Component.translatable("cfhgui.stats.no_match")));
      this.body.items(this.shown);
      this.body.snapScroll(this.state.scroll);
      add.accept(this.body);
      if (this.footBuilt) {
         add.accept(new Foot(x, bottom - 20, w, 20));
      }

   }

   public @Nullable CFHGuiSearchField searchField() {
      return this.search;
   }

   public @Nullable AbstractWidget list() {
      return this.body;
   }

   public boolean nearEnd() {
      return this.body != null && (double)this.body.maxScroll() - this.body.scrollAmount() < (double)72.0F;
   }

   private void layout(int w) {
      int n = this.columns.size();
      this.columnW = new int[n];
      int lead = (this.icons ? 22 : 4) + (this.ranks ? 26 : 0);
      int nameMin = lead + 56;

      for(Column c : this.columns) {
         if (c.bar()) {
            int widest = 0;

            for(TableRow r : this.shown) {
               if (!r.section) {
                  widest = Math.max(widest, r.nameWidth());
               }
            }

            nameMin = Math.max(nameMin, Math.min(lead + widest + 10, Math.max(w * 9 / 20, w - 72)));
            break;
         }
      }

      int room = Math.max(0, w - nameMin);
      int total = 0;
      this.pictures = false;

      for(int i = 0; i < n; ++i) {
         Column c = (Column)this.columns.get(i);
         int share = n == 0 ? 0 : room / n;
         int want = Math.max(c.width(), CFHGuiDraw.width(c.title()) + 14);
         this.columnW[i] = c.bar() ? Math.max(40, room - (n - 1) * 30) : Mth.clamp(share, 26, want);
         total += this.columnW[i];
         if (c.sprite() != null && CFHGuiDraw.width(c.title()) + 12 > this.columnW[i]) {
            this.pictures = true;
         }
      }

      if (this.pictures) {
         int widest = 0;

         for(TableRow r : this.shown) {
            if (!r.section) {
               widest = Math.max(widest, r.nameWidth());
            }
         }

         int missing = lead + widest + 8 - (w - total);
         if (missing > 0) {
            int each = (missing + n - 1) / n;

            for(int i = 0; i < n; ++i) {
               if (!((Column)this.columns.get(i)).bar()) {
                  int need = Math.max(34, CFHGuiDraw.width(StatFmt.count(i < this.largest.length ? this.largest[i] : 0L)) + 12);
                  int give = Math.min(each, this.columnW[i] - need);
                  if (give > 0) {
                     int[] var10000 = this.columnW;
                     var10000[i] -= give;
                     total -= give;
                  }
               }
            }
         }
      }

      this.nameW = Math.max(30, w - total);
   }

   private int columnAt(int x, double mouseX) {
      int cx = x + this.nameW;

      for(int i = 0; i < this.columnW.length; ++i) {
         if (mouseX >= (double)cx && mouseX < (double)(cx + this.columnW[i])) {
            return i;
         }

         cx += this.columnW[i];
      }

      return -1;
   }

   private StatKind kindOf(TableRow row, int column) {
      return row.kind != null ? row.kind : ((Column)this.columns.get(column)).kind();
   }

   public StatFmt.@Nullable Shown shownAt(TableRow row, int column) {
      long v = row.value(column);
      return v != Long.MIN_VALUE && column >= 0 && column < this.columns.size() ? row.written(column, this.kindOf(row, column)) : null;
   }

   public Component labelAt(TableRow row, int column) {
      if (this.columns.size() > 1 && column >= 0 && column < this.columns.size()) {
         Column c = (Column)this.columns.get(column);
         return Component.translatable("cfhgui.stats.of_type", new Object[]{row.name, c.tip() != null ? c.tip() : c.title()});
      } else {
         return row.name;
      }
   }

   public int mainColumn() {
      int by = this.effectiveSort();
      return by >= 0 && by < this.columns.size() ? by : (this.columns.isEmpty() ? -1 : 0);
   }

   private void drawRow(GuiGraphics g, TableRow row, int x, int y, int w, boolean hovered, boolean lit, int mouseX, int mouseY, float a) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      if (row.section) {
         CFHGuiDraw.sectionRule(g, row.name, x + 3, y + 4 + 1, w - 6, a);
      } else {
         boolean chosen = row.id.equals(this.state.selected);
         if (van) {
            if (hovered || chosen) {
               g.fill(x, y, x + w, y + 18, Colors.scaleAlpha(chosen ? 1342177280 : 620756991, a));
            }

            if (chosen && lit) {
               CFHGuiDraw.outline(g, x, y, w, 18, Colors.scaleAlpha(-1, a), 0);
            }
         } else {
            int fill = chosen ? p.surfaceActive() : (hovered ? p.surfaceHover() : 0);
            if (fill != 0) {
               CFHGuiDraw.pixelRound(g, x, y, w, 18, Colors.scaleAlpha(t.panel(fill), a), t.radius());
            }

            if (chosen && lit) {
               CFHGuiDraw.outline(g, x, y, w, 18, Colors.scaleAlpha(Colors.withAlpha(p.accent(), 144), a), t.radius());
            }
         }

         if (row.own) {
            int mark = van ? -1 : p.accent();
            CFHGuiDraw.hgradient(g, x, y + 1, Math.min(w, 120), 16, Colors.withAlpha(mark, Math.round((float)(van ? 40 : 64) * a)), Colors.withAlpha(mark, 0));
            CFHGuiDraw.rect(g, x, y + 2, 2, 14, Colors.scaleAlpha(mark, a));
         }

         boolean quiet = row.empty() && !this.columns.isEmpty();
         int ink = Colors.scaleAlpha(van ? (quiet ? -6250336 : -1) : (quiet ? p.textDim() : p.text()), a);
         int soft = Colors.scaleAlpha(van ? -5723992 : p.textDim(), a);
         int ty = y + 5;
         int tx = x + 4;
         if (this.ranks) {
            if (row.rank > 0) {
               g.drawString(CFHGuiDraw.font(), "#" + row.rank, tx, ty, soft, van);
            }

            tx += 26;
         }

         if (this.icons) {
            RowIcons.draw(g, row.icon, tx - 1, y + 1, Colors.scaleAlpha(van ? -2039584 : p.textMuted(), a), a);
            tx += 20;
         }

         int nameRoom = x + this.nameW - tx - 4;
         FormattedCharSequence name = CFHGuiDraw.truncate(row.name, nameRoom);
         g.drawString(CFHGuiDraw.font(), name, tx, ty, ink, van);
         int nameEnd = tx + CFHGuiDraw.width(name);
         boolean cut = row.nameWidth() > nameRoom;
         boolean noted = row.note != null && nameEnd + 8 + CFHGuiDraw.width(row.note) <= x + this.nameW - 4;
         if (noted) {
            g.drawString(CFHGuiDraw.font(), row.note, nameEnd + 6, ty, soft, van);
         }

         int cx = x + this.nameW;
         int over = -1;

         for(int c = 0; c < this.columnW.length; ++c) {
            int cw = this.columnW[c];
            long v = row.value(c);
            boolean cellHover = hovered && mouseX >= cx && mouseX < cx + cw;
            if (cellHover) {
               over = c;
            }

            if (v == Long.MIN_VALUE) {
               g.drawString(CFHGuiDraw.font(), "-", cx + cw - 4 - CFHGuiDraw.width("-"), ty, soft, van);
            } else {
               StatKind kind = this.kindOf(row, c);
               StatFmt.Shown s = row.written(c, kind);
               int heart = s.hearts() ? 10 : 0;
               String text = s.text();
               Column col = (Column)this.columns.get(c);
               if (kind == StatKind.COUNT && !col.bar() && CFHGuiDraw.width(text) > cw - 6 - heart) {
                  text = row.writtenShort(c);
               }

               int textW = CFHGuiDraw.width(text);
               int right = cx + cw - 4 - heart;
               int cellInk = v == 0L ? soft : (cellHover && !van ? Colors.scaleAlpha(p.text(), a) : ink);
               g.drawString(CFHGuiDraw.font(), text, right - textW, ty, cellInk, van);
               if (s.hearts()) {
                  heart(g, right + 1, ty, a);
               }

               if (col.bar()) {
                  int barX = cx + 6;
                  int barW = right - Math.max(textW, c < this.widestText.length ? this.widestText[c] : 0) - 8 - barX;
                  if (barW >= 16) {
                     long max = c < this.largest.length ? this.largest[c] : 0L;
                     CFHGuiDraw.rect(g, barX, y + 9 - 2, barW, 4, Colors.scaleAlpha(van ? 1610612736 : Colors.withAlpha(p.surfaceActive(), 192), a));
                     if (max > 0L && v > 0L) {
                        int fillW = Math.max(1, (int)Math.round((double)barW * (double)v / (double)max));
                        int tone = van ? (hovered ? -1 : -4144960) : (hovered ? p.text() : p.textMuted());
                        CFHGuiDraw.rect(g, barX, y + 9 - 2, fillW, 4, Colors.scaleAlpha(tone, a));
                     }
                  }
               }
            }

            cx += cw;
         }

         if (hovered) {
            this.tooltip(row, over, mouseX < x + this.nameW, cut || row.note != null && !noted);
         }

      }
   }

   public static void heart(GuiGraphics g, int x, int y, float alpha) {
      if (!(alpha <= 0.02F)) {
         g.setColor(1.0F, 1.0F, 1.0F, alpha);
         g.blitSprite(ResourceLocation.withDefaultNamespace("hud/heart/full"), x, y - 1, 9, 9);
         g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private void tooltip(TableRow row, int column, boolean overName, boolean hidden) {
      List<Component> lines = new ArrayList(3);
      if (column >= 0) {
         Column c = (Column)this.columns.get(column);
         StatFmt.Shown s = this.shownAt(row, column);
         Component what = this.columns.size() > 1 ? (c.tip() != null ? c.tip() : c.title()) : row.name;
         if (s == null) {
            lines.add(Component.translatable("cfhgui.stats.cell.none", new Object[]{what}));
         } else {
            lines.add(Component.translatable("cfhgui.stats.line", new Object[]{what, s.exact()}));
         }
      } else if (overName) {
         boolean modded = !row.namespace.isEmpty() && !"minecraft".equals(row.namespace);
         if (!hidden && !modded) {
            return;
         }

         lines.add(row.name);
         if (row.note != null) {
            lines.add(row.note.copy().withStyle(ChatFormatting.GRAY));
         }

         if (modded) {
            lines.add(Component.literal(StatNames.modName(row.namespace)).withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal(row.id).withStyle(ChatFormatting.DARK_GRAY));
         }
      }

      if (!lines.isEmpty()) {
         CFHGuiTooltips.request(lines, (AbstractWidget)null);
      }

   }

   private void sortBy(int title) {
      boolean firstUp = title == -1;
      if (this.state.sort != title) {
         this.state.sort = title;
         this.state.ascending = firstUp;
      } else if (this.state.ascending == firstUp) {
         this.state.ascending = !firstUp;
      } else {
         this.state.sort = Integer.MIN_VALUE;
         this.state.ascending = false;
      }

      CFHGuiSounds.tick();
      this.arrange();
   }

   private static int sum(int[] values) {
      int s = 0;

      for(int v : values) {
         s += v;
      }

      return s;
   }

   public static final class State {
      public static final int OWN = Integer.MIN_VALUE;
      public static final int NAME = -1;
      public int sort = Integer.MIN_VALUE;
      public boolean ascending;
      public String query = "";
      public String mod = "";
      public double scroll;
      public @Nullable String selected;
   }

   public interface Listener {
      void menu(TableRow var1, int var2, double var3, double var5);

      default void activate(TableRow row) {
      }

      default void copy(TableRow row, int column) {
      }
   }

   private final class Head extends CFHGuiWidget {
      Head(final int x, final int y, final int w, final int h) {
         super(x, y, w, h, Component.translatable("cfhgui.stats.col.name"));
      }

      private int rowsW() {
         return StatTable.this.body != null ? StatTable.this.body.rowsWidth() : this.getWidth() - 8;
      }

      private int titleAt(double mouseX, double mouseY) {
         if (!this.contains(mouseX, mouseY)) {
            return Integer.MIN_VALUE;
         } else if (mouseX < (double)(this.getX() + StatTable.this.nameW)) {
            return -1;
         } else {
            int c = StatTable.this.columnAt(this.getX(), mouseX);
            return c < 0 ? Integer.MIN_VALUE : c;
         }
      }

      public void onClick(double mouseX, double mouseY) {
         int title = this.titleAt(mouseX, mouseY);
         if (title != Integer.MIN_VALUE) {
            super.onClick(mouseX, mouseY);
            StatTable.this.sortBy(title);
         }
      }

      public void playDownSound(SoundManager handler) {
      }

      public ComponentPath nextFocusPath(FocusNavigationEvent event) {
         return null;
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
            Palette p = Theme.current().palette();
            int x = this.getX();
            int y = this.getY() + this.enterOffset();
            int h = this.getHeight();
            if (!StatTable.this.onPanel) {
               if (van) {
                  CFHGuiDraw.vanillaListBackground(g, StatTable.this.ground.x(), StatTable.this.ground.y(), StatTable.this.ground.w(), StatTable.this.ground.h(), Minecraft.getInstance().level != null);
               } else {
                  CFHGuiDraw.plate(g, StatTable.this.ground.x(), StatTable.this.ground.y() + this.enterOffset(), StatTable.this.ground.w(), StatTable.this.ground.h(), p.bg2(), p.border(), 0.4F, a);
               }
            }

            if (this.rowsW() != StatTable.this.nameW + StatTable.sum(StatTable.this.columnW)) {
               StatTable.this.layout(this.rowsW());
            }

            int over = this.isHovered() ? this.titleAt((double)mouseX, (double)mouseY) : Integer.MIN_VALUE;
            int by = StatTable.this.effectiveSort();
            boolean chosenByPlayer = StatTable.this.state.sort != Integer.MIN_VALUE;
            boolean up = by == -1 ? !chosenByPlayer || StatTable.this.state.ascending : chosenByPlayer && StatTable.this.state.ascending;
            int rest = van ? -3618616 : p.textDim();
            int lit = van ? -1 : p.text();
            int ty = y + h - 11;
            Component nameTitle = Component.translatable("cfhgui.stats.col.name_count", new Object[]{Component.translatable("cfhgui.stats.col.name"), StatFmt.count((long)StatTable.this.dataRows)});
            int nameInk = by == -1 && chosenByPlayer ? lit : (over == -1 ? Colors.lerp(rest, lit, 0.6F) : rest);
            FormattedCharSequence nameSeq = CFHGuiDraw.truncate(nameTitle, StatTable.this.nameW - 16);
            g.drawString(CFHGuiDraw.font(), nameSeq, x + 4, ty, Colors.scaleAlpha(nameInk, a), van);
            if (by == -1 && chosenByPlayer) {
               this.arrow(g, x + 6 + CFHGuiDraw.width(nameSeq), ty, up, true, van, a);
            }

            int cx = x + StatTable.this.nameW;

            for(int c = 0; c < StatTable.this.columnW.length; ++c) {
               Column col = (Column)StatTable.this.columns.get(c);
               int cw = StatTable.this.columnW[c];
               boolean sorted = by == c;
               int ink = Colors.scaleAlpha(sorted && chosenByPlayer ? lit : (over == c ? Colors.lerp(rest, lit, 0.6F) : rest), a);
               int arrowW = sorted ? 9 : 0;
               if (StatTable.this.pictures && col.sprite() != null) {
                  int sx = cx + cw - 20 - arrowW;
                  if (over == c && !van) {
                     CFHGuiDraw.pixelRound(g, sx, y, 18, 18, Colors.scaleAlpha(p.surfaceHover(), a), 2);
                  }

                  g.setColor(1.0F, 1.0F, 1.0F, a);
                  g.blitSprite(col.sprite(), sx, y, 18, 18);
                  g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                  if (sorted) {
                     this.arrow(g, cx + cw - 11, y + 6, up, chosenByPlayer, van, a);
                  }
               } else {
                  FormattedCharSequence title = CFHGuiDraw.truncate(col.title(), cw - 6 - arrowW);
                  g.drawString(CFHGuiDraw.font(), title, cx + cw - 4 - arrowW - CFHGuiDraw.width(title), ty, ink, van);
                  if (sorted) {
                     this.arrow(g, cx + cw - 11, ty, up, chosenByPlayer, van, a);
                  }
               }

               if (over == c) {
                  Component what = col.tip() != null ? col.tip() : col.title();
                  CFHGuiTooltips.request(List.of(what, Component.translatable("cfhgui.stats.col.sort_hint").withStyle(ChatFormatting.GRAY)), (AbstractWidget)null);
               }

               cx += cw;
            }

            if (over == -1) {
               CFHGuiTooltips.request(List.of(Component.translatable("cfhgui.stats.col.sort_hint").withStyle(ChatFormatting.GRAY)), (AbstractWidget)null);
            }

            CFHGuiDraw.hline(g, x, y + h, this.rowsW(), Colors.scaleAlpha(van ? 1358954495 : p.border(), a));
         }
      }

      private void arrow(GuiGraphics g, int x, int y, boolean up, boolean chosen, boolean van, float a) {
         Palette p = Theme.current().palette();
         int color = van ? (chosen ? -96 : -6250336) : (chosen ? p.accentInk() : p.textDim());
         Icons.draw(g, up ? Icon.CHEVRON_UP : Icon.CHEVRON_DOWN, x, y, 8, Colors.scaleAlpha(color, a));
      }

      protected void updateWidgetNarration(NarrationElementOutput out) {
         out.add(NarratedElementType.TITLE, this.getMessage());
      }
   }

   private final class Body extends CFHGuiList<TableRow> {
      Body(final int x, final int y, final int w, final int h) {
         super(x, y, w, h, 18, new CFHGuiList.RowRenderer<TableRow>() {
            public void render(GuiGraphics g, TableRow item, int index, int rx, int ry, int rw, int rh, boolean hovered, boolean selected, int mouseX, int mouseY) {
               StatTable.this.drawRow(g, item, rx, ry, rw, hovered, StatTable.this.body != null && StatTable.this.body.isFocused() && CFHGuiDraw.focusShown(), mouseX, mouseY, StatTable.this.body == null ? 1.0F : StatTable.this.body.effectiveAlpha());
            }

            public boolean click(TableRow item, int index, int rx, int ry, int rw, int rh, double mouseX, double mouseY, int button) {
               if (item.section) {
                  return true;
               } else {
                  StatTable.this.state.selected = item.id;
                  if (button == 1) {
                     StatTable.this.listener.menu(item, StatTable.this.columnAt(rx, mouseX), mouseX, mouseY);
                  } else if (button == 0) {
                     long now = Clock.nowMs();
                     if (StatTable.this.pickOnClick) {
                        CFHGuiSounds.click();
                        StatTable.this.listener.activate(item);
                     } else if (item.id.equals(StatTable.this.lastClickId) && now - StatTable.this.lastClickMs < 350L) {
                        StatTable.this.lastClickId = null;
                        StatTable.this.listener.activate(item);
                     } else {
                        StatTable.this.lastClickId = item.id;
                        StatTable.this.lastClickMs = now;
                        CFHGuiSounds.tick();
                     }
                  }

                  return true;
               }
            }
         });
         this.gap(0);
         this.plainRows();
      }

      private int chosenIndex() {
         if (StatTable.this.state.selected == null) {
            return -1;
         } else {
            List<TableRow> rows = this.items();

            for(int i = 0; i < rows.size(); ++i) {
               if (((TableRow)rows.get(i)).id.equals(StatTable.this.state.selected)) {
                  return i;
               }
            }

            return -1;
         }
      }

      private void choose(int from, int step) {
         List<TableRow> rows;
         for(rows = this.items(); from >= 0 && from < rows.size() && ((TableRow)rows.get(from)).section; from += step) {
         }

         if (from >= 0 && from < rows.size()) {
            StatTable.this.state.selected = ((TableRow)rows.get(from)).id;
            this.ensureVisible(from);
         }
      }

      public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
         if (this.active && this.visible && !this.items().isEmpty()) {
            int at = this.chosenIndex();
            int last = this.items().size() - 1;
            int page = Math.max(1, this.getHeight() / 18 - 1);
            switch (keyCode) {
               case 67:
                  if (!Screen.hasControlDown() || at < 0) {
                     return false;
                  }

                  StatTable.this.listener.copy((TableRow)this.items().get(at), StatTable.this.mainColumn());
                  break;
               case 257:
               case 335:
                  if (at < 0) {
                     return false;
                  }

                  StatTable.this.listener.activate((TableRow)this.items().get(at));
                  break;
               case 264:
                  this.choose(at < 0 ? 0 : Math.min(last, at + 1), 1);
                  break;
               case 265:
                  this.choose(at < 0 ? 0 : Math.max(0, at - 1), at < 0 ? 1 : -1);
                  break;
               case 266:
                  this.choose(Math.max(0, Math.max(0, at) - page), 1);
                  break;
               case 267:
                  this.choose(Math.min(last, Math.max(0, at) + page), -1);
                  break;
               case 268:
                  this.choose(0, 1);
                  break;
               case 269:
                  this.choose(last, -1);
                  break;
               case 299:
                  if (!Screen.hasShiftDown() || !this.menuAt(at)) {
                     return false;
                  }
                  break;
               case 348:
                  if (!this.menuAt(at)) {
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

      private boolean menuAt(int index) {
         if (index < 0) {
            return false;
         } else {
            int y = this.getY() + index * 18 - (int)Math.round(this.scrollAmount());
            StatTable.this.listener.menu((TableRow)this.items().get(index), StatTable.this.mainColumn(), (double)(this.getX() + StatTable.this.nameW), (double)Mth.clamp(y + 18, this.getY(), this.getY() + this.getHeight()));
            return true;
         }
      }

      protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         StatTable.this.state.scroll = this.scrollAmount();
         if (this.rowsWidth() != StatTable.this.nameW + StatTable.sum(StatTable.this.columnW)) {
            StatTable.this.layout(this.rowsWidth());
         }

         if (!StatTable.this.onPanel) {
            Theme.enterPanel();
         }

         try {
            super.renderDark(g, mouseX, mouseY, partialTick);
         } finally {
            if (!StatTable.this.onPanel) {
               Theme.exitPanel();
            }

         }

      }

      protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         StatTable.this.state.scroll = this.scrollAmount();
         if (this.rowsWidth() != StatTable.this.nameW + StatTable.sum(StatTable.this.columnW)) {
            StatTable.this.layout(this.rowsWidth());
         }

         super.renderVanilla(g, mouseX, mouseY, partialTick);
      }

      protected void updateWidgetNarration(NarrationElementOutput out) {
         int at = this.chosenIndex();
         if (at >= 0) {
            TableRow row = (TableRow)this.items().get(at);
            out.add(NarratedElementType.TITLE, row.name);
            int c = StatTable.this.mainColumn();
            StatFmt.Shown s = c < 0 ? null : StatTable.this.shownAt(row, c);
            if (s != null) {
               out.add(NarratedElementType.HINT, Component.literal(s.spoken()));
            }

            out.add(NarratedElementType.POSITION, Component.translatable("narrator.position.list", new Object[]{at + 1, this.items().size()}));
         }
      }
   }

   private final class Foot extends CFHGuiWidget {
      Foot(final int x, final int y, final int w, final int h) {
         super(x, y, w, h, Component.translatable("cfhgui.stats.rank.own"));
      }

      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         if (this.active && this.visible && StatTable.this.pinned != null && this.contains(mouseX, mouseY)) {
            if (button == 1) {
               StatTable.this.listener.menu(StatTable.this.pinned, StatTable.this.columnAt(this.getX(), mouseX), mouseX, mouseY);
            } else {
               if (button != 0) {
                  return false;
               }

               StatTable.this.listener.activate(StatTable.this.pinned);
            }

            return true;
         } else {
            return false;
         }
      }

      public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
         if (StatTable.this.pinned != null && this.active) {
            if (keyCode != 257 && keyCode != 335 && keyCode != 32) {
               return false;
            } else {
               StatTable.this.listener.activate(StatTable.this.pinned);
               return true;
            }
         } else {
            return false;
         }
      }

      public void playDownSound(SoundManager handler) {
      }

      protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.draw(g, mouseX, mouseY);
      }

      protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.draw(g, mouseX, mouseY);
      }

      private void draw(GuiGraphics g, int mouseX, int mouseY) {
         TableRow row = StatTable.this.pinned;
         float a = this.effectiveAlpha();
         if (row != null && !(a <= 0.004F)) {
            Theme t = Theme.current();
            int x = this.getX();
            int y = this.getY() + this.enterOffset();
            int w = StatTable.this.body != null ? StatTable.this.body.rowsWidth() : this.getWidth() - 8;
            CFHGuiDraw.hline(g, x, y - 2, w, Colors.scaleAlpha(t.isVanilla() ? 1358954495 : t.palette().border(), a));
            boolean hovered = this.isHovered() && this.contains((double)mouseX, (double)mouseY);
            String chosen = StatTable.this.state.selected;
            StatTable.this.state.selected = null;
            boolean onGround = !StatTable.this.onPanel && !t.isVanilla();
            if (onGround) {
               Theme.enterPanel();
            }

            try {
               StatTable.this.drawRow(g, row, x, y + (this.getHeight() - 18) / 2, w, hovered, false, mouseX, mouseY, a);
            } finally {
               if (onGround) {
                  Theme.exitPanel();
               }

               StatTable.this.state.selected = chosen;
            }

            CFHGuiDraw.focusRing(g, x, y, w, this.getHeight(), this.focus() * a);
         }
      }
   }
}
