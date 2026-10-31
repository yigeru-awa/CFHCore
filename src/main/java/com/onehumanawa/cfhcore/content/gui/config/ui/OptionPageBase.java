package com.onehumanawa.cfhcore.content.gui.config.ui;

import com.onehumanawa.cfhcore.content.gui.config.ConfigSettings;
import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfig;
import com.onehumanawa.cfhcore.content.gui.config.api.SettingsTabs;
import com.onehumanawa.cfhcore.content.gui.config.option.KeyBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.KeySearch;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionResolvers;
import com.onehumanawa.cfhcore.content.gui.config.search.SearchIndex;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarPage;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSectionHeader;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSlider;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTabStrip;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTabStrip.Style;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public abstract class OptionPageBase extends SidebarPage implements TabHost, Enterable {
   private static final int STRIP_GAP = 6;
   private static final int COLUMN_GAP = 4;
   protected SidebarScreen screen;
   protected Rect area;
   protected @Nullable CFHGuiScrollPanel panel;
   private final List<OptionRow> rows = new ArrayList();
   private List<Section> cachedSections = List.of();
   private String filter = "";
   private @Nullable String pendingFocus;
   private double keepScroll = (double)-1.0F;
   private @Nullable Component emptyText;
   private Level level;
   private boolean indexed;
   private @Nullable String hostPath;
   private @Nullable Component hostTitle;
   private List<Tab> tabs;
   private int tabIndex;
   private boolean promoted;
   private List<Section> subs;
   private int subIndex;
   private @Nullable String wantTab;
   private @Nullable String wantSub;
   private @Nullable CFHGuiTabStrip strip;
   private @Nullable CFHGuiTabStrip subStrip;
   private int stripFrom;
   private int subFrom;
   private int enterDir;
   private boolean subChanged;
   private boolean enterAll;

   protected OptionPageBase(String id, Component title, Icon icon) {
      super(id, title, icon);
      this.level = OptionPageBase.Level.BASIC;
      this.indexed = true;
      this.tabs = List.of();
      this.subs = List.of();
      this.stripFrom = -1;
      this.subFrom = -1;
   }

   protected abstract List<Section> sections();

   protected boolean pills(String tabKey) {
      return false;
   }

   protected OptionPageBase emptyText(Component c) {
      this.emptyText = c;
      return this;
   }

   public OptionPageBase level(Level l) {
      this.level = l;
      return this;
   }

   public Level level() {
      return this.level;
   }

   public OptionPageBase indexed(boolean on) {
      this.indexed = on;
      return this;
   }

   public List<OptionRow> rows() {
      return this.rows;
   }

   public List<Section> currentSections() {
      return this.cachedSections;
   }

   public OptionPageBase hosted(String path, Component categoryTitle) {
      this.hostPath = path;
      this.hostTitle = categoryTitle;
      return this;
   }

   public boolean isHosted() {
      return this.hostPath != null;
   }

   public String path() {
      return this.hostPath != null ? this.hostPath : this.id();
   }

   public Component fullTitle() {
      Object var10000;
      if (this.hostTitle == null) {
         var10000 = this.title();
      } else {
         String var1 = this.hostTitle.getString();
         var10000 = Component.literal(var1 + " › " + this.title().getString());
      }

      return (Component)var10000;
   }

   private String memKey() {
      return this.path();
   }

   private List<Section> collect() {
      List<Section> all = new ArrayList(this.sections());
      if (!this.isHosted()) {
         for(SettingsTabs.Tab t : SettingsTabs.tabs(this.id())) {
            try {
               for(Section s : (List<Section>)t.sections().get()) {
                  if (s != null) {
                     all.add(s.tab(t.id(), t.title()));
                  }
               }
            } catch (Exception e) {
               CFHGuiConfig.LOGGER.warn("[CFHGui Config] tab {} of {} failed to build: {}", new Object[]{t.id(), this.id(), e.toString()});
            }
         }
      }

      if (this.level == OptionPageBase.Level.ALL) {
         return all;
      } else {
         boolean wantAdvanced = this.level == OptionPageBase.Level.ADVANCED;
         List<Section> out = new ArrayList(all.size());

         for(Section s : all) {
            Section f = s.filtered((it) -> s.isAdvanced(it) == wantAdvanced);
            if (!f.isEmpty()) {
               out.add(f);
            }
         }

         return out;
      }
   }

   public List<Section> sectionsNow() {
      return this.collect();
   }

   private static List<Tab> group(List<Section> visible) {
      Map<String, List<Section>> by = new LinkedHashMap();
      Map<String, Component> titles = new LinkedHashMap();

      for(Section s : visible) {
         ((List)by.computeIfAbsent(s.tabKey(), (k) -> new ArrayList())).add(s);
         titles.putIfAbsent(s.tabKey(), s.tabLabel());
      }

      List<Tab> out = new ArrayList();

      for(Map.Entry<String, List<Section>> e : by.entrySet()) {
         out.add(new Tab((String)e.getKey(), (Component)titles.get(e.getKey()), (List)e.getValue()));
      }

      return out;
   }

   private static int indexOf(List<Tab> tabs, @Nullable String key) {
      if (key == null) {
         return -1;
      } else {
         for(int i = 0; i < tabs.size(); ++i) {
            if (((Tab)tabs.get(i)).key().equals(key)) {
               return i;
            }
         }

         return -1;
      }
   }

   private static int sectionIndex(List<Section> secs, @Nullable String id) {
      if (id == null) {
         return -1;
      } else {
         for(int i = 0; i < secs.size(); ++i) {
            if (((Section)secs.get(i)).id.equals(id)) {
               return i;
            }
         }

         return -1;
      }
   }

   public void build(SidebarScreen screen, Rect area) {
      this.screen = screen;
      this.area = area;
      this.rows.clear();
      this.cachedSections = this.collect();
      List<Section> visible = new ArrayList();

      for(Section s : this.cachedSections) {
         if (!s.isEmpty()) {
            visible.add(s);
         }
      }

      double stripScroll = this.strip != null ? this.strip.scrollOffset() : (double)0.0F;
      double subScroll = this.subStrip != null ? this.subStrip.scrollOffset() : (double)0.0F;
      boolean keyboard = Minecraft.getInstance().getLastInputType().isKeyboard();
      boolean stripFocused = keyboard && this.strip != null && this.strip.isFocused();
      boolean subFocused = keyboard && this.subStrip != null && this.subStrip.isFocused();
      this.strip = null;
      this.subStrip = null;
      String f = this.filter.toLowerCase(Locale.ROOT).trim();
      if (!f.isEmpty()) {
         this.buildFiltered(visible);
      } else {
         List<Tab> groups = group(visible);
         List<Tab> primary;
         if (this.isHosted()) {
            this.promoted = false;
            primary = visible.isEmpty() ? List.of() : List.of(new Tab(this.id(), this.title(), visible));
         } else {
            this.promoted = groups.size() == 1 && ((Tab)groups.get(0)).sections().size() > 1 && this.pills(((Tab)groups.get(0)).key());
            if (this.promoted) {
               primary = new ArrayList();

               for(Section s : ((Tab)groups.get(0)).sections()) {
                  primary.add(new Tab(s.id, s.title, List.of(s)));
               }
            } else {
               primary = groups;
            }
         }

         this.tabs = primary;
         int ti = indexOf(primary, this.promoted ? this.wantSub : this.wantTab);
         if (ti >= 0) {
            ConfigSettings.setLastTab(this.memKey(), ((Tab)primary.get(ti)).key());
         } else {
            ti = indexOf(primary, ConfigSettings.lastTab(this.memKey()));
         }

         this.tabIndex = Math.max(0, ti);
         int y = area.y();
         if (primary.size() > 1) {
            CFHGuiTabStrip s = (new CFHGuiTabStrip(area.x(), y, area.w(), labels(primary), this.tabIndex, this::selectTab)).style(this.isHosted() ? Style.PILLS : Style.UNDERLINE);
            s.animateFrom(this.stripFrom >= 0 ? this.stripFrom : this.tabIndex, stripScroll);
            screen.addPageWidget(s);
            if (stripFocused) {
               screen.setFocused(s);
            }

            this.strip = s;
            y += s.getHeight() + 6;
         }

         Tab cur = primary.isEmpty() ? null : (Tab)primary.get(this.tabIndex);
         List<Section> shown = cur == null ? List.of() : cur.sections();
         this.subs = List.of();
         if (cur != null && !this.promoted && !this.isHosted() && shown.size() > 1 && this.pills(cur.key())) {
            this.subs = shown;
            String var10000 = this.memKey();
            String subKey = var10000 + "/" + cur.key();
            int si = cur.key().equals(this.wantTab) ? sectionIndex(shown, this.wantSub) : -1;
            if (si >= 0) {
               ConfigSettings.setLastTab(subKey, ((Section)shown.get(si)).id);
            } else {
               si = sectionIndex(shown, ConfigSettings.lastTab(subKey));
            }

            this.subIndex = Math.max(0, si);
            List<CFHGuiTabStrip.Tab> labels = new ArrayList();

            for(Section s : shown) {
               labels.add(new CFHGuiTabStrip.Tab(s.title));
            }

            CFHGuiTabStrip ss = (new CFHGuiTabStrip(area.x(), y, area.w(), labels, this.subIndex, this::selectSub)).style(Style.PILLS);
            ss.animateFrom(this.subFrom >= 0 ? this.subFrom : this.subIndex, this.subChanged ? (double)0.0F : subScroll);
            screen.addPageWidget(ss);
            if (subFocused) {
               screen.setFocused(ss);
            }

            this.subStrip = ss;
            y += ss.getHeight() + 6;
            shown = List.of((Section)shown.get(this.subIndex));
         }

         this.wantTab = null;
         this.wantSub = null;
         boolean headers = shown.size() > 1 || shown.size() == 1 && primary.size() <= 1 && ((Section)shown.get(0)).collapsible && !((Section)shown.get(0)).title.getString().isEmpty() && this.showSingleHeader();
         TabPanel p = new TabPanel(area.x(), y, area.w(), Math.max(20, area.bottom() - y));
         this.panel = p;
         int w = area.w() - 8;
         int cy = 0;
         boolean groupedHeaders = this.isHosted() && groups.size() > 1;

         for(Section s : shown) {
            cy = this.layoutSection(p, s, cy, w, headers, false, groupedHeaders ? groupedTitle(s) : null);
         }

         if (this.rows.isEmpty() && this.emptyText != null) {
            CFHGuiLabel l = (new CFHGuiLabel(0, cy + 8, w - 16, this.emptyText)).style(com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style.MUTED).wrap(true);
            p.add(l, 8, cy + 8);
            cy += l.getHeight() + 16;
         }

         this.finish(p, cy);
      }
   }

   private static List<CFHGuiTabStrip.Tab> labels(List<Tab> ts) {
      List<CFHGuiTabStrip.Tab> out = new ArrayList();

      for(Tab t : ts) {
         out.add(new CFHGuiTabStrip.Tab(t.title()));
      }

      return out;
   }

   private void buildFiltered(List<Section> visible) {
      TabPanel p = new TabPanel(this.area.x(), this.area.y(), this.area.w(), this.area.h());
      this.panel = p;
      int w = this.area.w() - 8;
      List<Tab> groups = group(visible);
      int cy = 0;

      for(Tab t : groups) {
         for(Section s : t.sections()) {
            String head = groups.size() > 1 && !t.title().getString().equals(s.title.getString()) ? t.title().getString() + " › " + s.title.getString() : s.title.getString();
            cy = this.layoutSection(p, s, cy, w, true, true, Component.literal(head));
         }
      }

      this.tabs = List.of();
      this.subs = List.of();
      this.finish(p, cy);
   }

   private int layoutSection(CFHGuiScrollPanel p, Section s, int y, int w, boolean headers, boolean filtering, @Nullable Component headerTitle) {
      String f = this.filter.toLowerCase(Locale.ROOT).trim();
      List<Section.Item> items = new ArrayList();

      for(Section.Item it : s.items) {
         if (it.binding() == null) {
            if (!filtering) {
               items.add(it);
            }
         } else if (!filtering || matches(it.binding(), s, f)) {
            items.add(it);
         }
      }

      if (items.isEmpty()) {
         return y;
      } else {
         String var10000 = this.id();
         String key = var10000 + ":" + s.id;
         boolean titled = headerTitle != null ? !headerTitle.getString().isEmpty() : !s.title.getString().isEmpty();
         boolean foldable = !filtering && s.collapsible;
         boolean collapsed = headers && titled && foldable && ConfigSettings.isCollapsed(key);
         if (headers && titled) {
            CFHGuiSectionHeader h = new CFHGuiSectionHeader(0, y, w, headerTitle != null ? headerTitle : s.title, s.bindings().size(), collapsed, foldable ? () -> {
               ConfigSettings.setCollapsed(key, !ConfigSettings.isCollapsed(key));
               this.rebuild();
            } : null, 0);
            p.add(h, 0, y);
            y += 22;
         }

         if (s.description != null && !collapsed && !filtering) {
            CFHGuiLabel d = (new CFHGuiLabel(0, y, w - 8, s.description)).style(com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style.MUTED).wrap(true);
            p.add(d, 4, y + 2);
            y += d.getHeight() + 8;
         }

         if (collapsed) {
            return y + 4;
         } else {
            SidebarScreen var16 = this.screen;
            int var34;
            if (var16 instanceof PageColumns) {
               PageColumns pc = (PageColumns)var16;
               var34 = Math.max(1, pc.columns(w));
            } else {
               var34 = 1;
            }

            int columns = var34;
            if (columns < 2) {
               for(Section.Item it : items) {
                  if (it.binding() != null) {
                     OptionResolvers.publish(it.binding());
                     OptionRow row = (new OptionRow(0, y, w, it.binding(), 0)).onChanged(this::onRowChanged);
                     this.rows.add(row);
                     p.add(row, 0, y);
                     y += 26;
                  } else if (it.custom() != null) {
                     AbstractWidget cw = (AbstractWidget)it.custom().apply(w);
                     if (cw != null) {
                        p.add(cw, 0, y + 2);
                        y += cw.getHeight() + 4;
                     }
                  }
               }

               return y + 6;
            } else {
               int colW = (w - 4 * (columns - 1)) / columns;
               OptionRow.Look look = colW < 236 ? OptionRow.Look.STACKED : OptionRow.Look.TILE;
               int rowH = OptionRow.heightOf(look);
               int col = 0;

               for(Section.Item it : items) {
                  if (it.binding() != null) {
                     OptionResolvers.publish(it.binding());
                     int x = col * (colW + 4);
                     OptionRow row = (new OptionRow(x, y, colW, it.binding(), 0, look)).onChanged(this::onRowChanged);
                     this.rows.add(row);
                     p.add(row, x, y);
                     ++col;
                     if (col >= columns) {
                        col = 0;
                        y += rowH + 4;
                     }
                  } else if (it.custom() != null) {
                     if (col > 0) {
                        col = 0;
                        y += rowH + 4;
                     }

                     AbstractWidget cw = (AbstractWidget)it.custom().apply(w);
                     if (cw != null) {
                        p.add(cw, 0, y);
                        y += cw.getHeight() + 4;
                     }
                  }
               }

               if (col > 0) {
                  y += rowH + 4;
               }

               return y + 6;
            }
         }
      }
   }

   private void finish(TabPanel p, int contentY) {
      int column = 0;

      for(OptionRow r : this.rows) {
         AbstractWidget var7 = r.control();
         if (var7 instanceof CFHGuiSlider s) {
            if (s.valueBeside()) {
               column = Math.max(column, s.naturalValueColumn());
            }
         }
      }

      for(OptionRow r : this.rows) {
         AbstractWidget var15 = r.control();
         if (var15 instanceof CFHGuiSlider s) {
            if (s.valueBeside()) {
               s.valueColumnAtLeast(column);
            }
         }
      }

      p.setContentHeight(contentY + 4);
      this.screen.addPageWidget(p);
      if (this.keepScroll >= (double)0.0F) {
         p.snapScroll(this.keepScroll);
         this.keepScroll = (double)-1.0F;
      }

      if (this.enterDir != 0) {
         p.slideFrom(this.enterDir);
         int i = 0;
         if (this.enterAll && this.strip != null) {
            i = Entrance.play(this.strip, i);
         }

         if ((this.subChanged || this.enterAll) && this.subStrip != null) {
            i = Entrance.play(this.subStrip, i);
         }

         Entrance.play(p, i);
      }

      this.enterDir = 0;
      this.enterAll = false;
      this.stripFrom = -1;
      this.subFrom = -1;
      this.subChanged = false;
      if (this.pendingFocus != null) {
         String id = this.pendingFocus;
         this.pendingFocus = null;

         for(OptionRow r : this.rows) {
            if (r.binding().id().equals(id)) {
               p.ensureVisible(r);
               r.flash();
               break;
            }
         }
      }

   }

   private static Component groupedTitle(Section s) {
      String tab = s.tabLabel().getString();
      String title = s.title.getString();
      if (title.isEmpty()) {
         return Component.literal(tab);
      } else {
         return (Component)(tab.equals(title) ? s.title : Component.literal(tab + " › " + title));
      }
   }

   protected boolean showSingleHeader() {
      return false;
   }

   private static boolean matches(OptionBinding b, Section s, String f) {
      if (b instanceof KeyBinding k) {
         KeyMapping var9 = k.mapping();
         String var10002 = b.searchText();
         return KeySearch.matches(var9, f, var10002 + " " + s.title.getString() + " " + s.tabLabel().getString());
      } else {
         String var10000 = b.searchText();
         String hay = (var10000 + " " + s.title.getString() + " " + s.tabLabel().getString()).toLowerCase(Locale.ROOT);

         for(String tok : f.split("\\s+")) {
            if (!tok.isEmpty() && !hay.contains(tok)) {
               return false;
            }
         }

         return true;
      }
   }

   private void selectTab(int i) {
      if (i != this.tabIndex && i >= 0 && i < this.tabs.size()) {
         ApplyQueue.flush();
         this.stripFrom = this.tabIndex;
         this.enterDir = i > this.tabIndex ? 1 : -1;
         this.subChanged = true;
         this.tabIndex = i;
         ConfigSettings.setLastTab(this.memKey(), ((Tab)this.tabs.get(i)).key());
         this.keepScroll = (double)0.0F;
         if (this.screen != null) {
            this.screen.refreshPage();
         }

      }
   }

   private void selectSub(int i) {
      if (i != this.subIndex && i >= 0 && i < this.subs.size() && !this.tabs.isEmpty()) {
         ApplyQueue.flush();
         this.subFrom = this.subIndex;
         this.enterDir = i > this.subIndex ? 1 : -1;
         this.subIndex = i;
         ConfigSettings.setLastTab(this.memKey() + "/" + ((Tab)this.tabs.get(this.tabIndex)).key(), ((Section)this.subs.get(i)).id);
         this.keepScroll = (double)0.0F;
         if (this.screen != null) {
            this.screen.refreshPage();
         }

      }
   }

   public void selectTab(String key) {
      this.wantTab = key;
      this.wantSub = key;

      for(Section s : this.collect()) {
         if (s.id.equals(key)) {
            this.wantTab = s.tabKey();
            break;
         }
      }

      if (this.isOnScreen()) {
         this.screen.refreshPage();
      }

   }

   private boolean isOnScreen() {
      if (this.screen == null) {
         return false;
      } else {
         SidebarPage cur = this.screen.currentPage();
         boolean var10000;
         if (cur != this) {
            label32: {
               if (cur instanceof CategoryHost) {
                  CategoryHost c = (CategoryHost)cur;
                  if (c.activeChild() == this) {
                     break label32;
                  }
               }

               var10000 = false;
               return var10000;
            }
         }

         var10000 = true;
         return var10000;
      }
   }

   public boolean cycleTab(int dir, boolean secondary) {
      if (!this.filter.isEmpty()) {
         return false;
      } else if (secondary && !this.subs.isEmpty() && this.subStrip != null) {
         int n = this.subs.size();
         int to = ((this.subIndex + dir) % n + n) % n;
         this.subStrip.setIndex(to);
         this.selectSub(to);
         return true;
      } else if (this.tabs.size() >= 2 && this.strip != null) {
         int n = this.tabs.size();
         int to = ((this.tabIndex + dir) % n + n) % n;
         this.strip.setIndex(to);
         this.selectTab(to);
         return true;
      } else {
         return false;
      }
   }

   public boolean hasTopTabs() {
      return !this.isHosted() && this.strip != null;
   }

   public int tabCount() {
      return Math.max(1, this.tabs.size());
   }

   public List<Section> currentTabSections() {
      if (this.tabs.isEmpty()) {
         List<Section> out = new ArrayList();

         for(Section s : this.cachedSections) {
            if (!s.isEmpty()) {
               out.add(s);
            }
         }

         return out;
      } else {
         return ((Tab)this.tabs.get(Math.min(this.tabIndex, this.tabs.size() - 1))).sections();
      }
   }

   public Component currentTabTitle() {
      String base = this.fullTitle().getString();
      return this.tabs.size() < 2 ? Component.literal(base) : Component.literal(base + " › " + ((Tab)this.tabs.get(Math.min(this.tabIndex, this.tabs.size() - 1))).title().getString());
   }

   public void rebuild() {
      if (this.screen != null) {
         this.keepScroll = this.panel != null ? this.panel.scrollAmount() : (double)-1.0F;
         this.screen.refreshPage();
      }
   }

   protected void onRowChanged(OptionRow row) {
      for(OptionRow r : this.rows) {
         if (r != row) {
            r.refresh();
         }
      }

   }

   public void filter(String text) {
      String t = text == null ? "" : text;
      if (!t.equals(this.filter)) {
         this.filter = t;
         this.keepScroll = (double)0.0F;
         if (this.isOnScreen()) {
            this.screen.refreshPage();
         }

      }
   }

   public String filter() {
      return this.filter;
   }

   public void filterSilently(String text) {
      this.filter = text == null ? "" : text;
   }

   public void enterFrom(int dir) {
      this.enterDir = Integer.signum(dir);
      this.enterAll = dir != 0;
      this.keepScroll = (double)0.0F;
   }

   public void focusRow(String bindingId) {
      this.pendingFocus = bindingId;
      this.filter = "";

      label25:
      for(Section s : this.collect()) {
         for(OptionBinding b : s.bindings()) {
            if (b.id().equals(bindingId)) {
               this.wantTab = s.tabKey();
               this.wantSub = s.id;
               ConfigSettings.setCollapsed(this.id() + ":" + s.id, false);
               break label25;
            }
         }
      }

      if (this.isOnScreen()) {
         this.screen.refreshPage();
      }

   }

   public void resetAll() {
      this.reset(this.collect());
   }

   public void resetTab() {
      this.reset(this.currentTabSections());
   }

   private void reset(List<Section> sections) {
      for(Section s : sections) {
         for(OptionBinding b : s.bindings()) {
            if (b.hasDefault() && b.type().snapshotable() && b.enabled()) {
               try {
                  b.reset();
               } catch (Exception var7) {
               }
            }
         }
      }

      this.rebuild();
   }

   public void refreshRows() {
      for(OptionRow r : this.rows) {
         r.refresh();
      }

   }

   public List<SearchIndex.Entry> searchEntries() {
      return this.searchEntries(this.path(), this.hostTitle != null ? this.hostTitle : this.title(), this.hostTitle != null ? this.title() : null);
   }

   public List<SearchIndex.Entry> searchEntries(String path, Component pageTitle, @Nullable Component tabTitle) {
      List<SearchIndex.Entry> out = new ArrayList();
      if (!this.indexed) {
         return out;
      } else {
         List<Section> visible = new ArrayList();

         for(Section s : this.collect()) {
            if (!s.isEmpty()) {
               visible.add(s);
            }
         }

         List<Tab> groups = group(visible);
         boolean single = groups.size() == 1;

         for(Tab t : groups) {
            for(Section s : t.sections()) {
               Component tab = tabTitle != null ? tabTitle : (single ? (t.sections().size() > 1 ? s.title : null) : t.title());

               for(OptionBinding b : s.bindings()) {
                  OptionResolvers.publish(b);
                  out.add(new SearchIndex.Entry(path, pageTitle, tab, s.title, b));
               }
            }
         }

         return out;
      }
   }

   public void onHide() {
      ApplyQueue.flush();
   }

   public void render(SidebarScreen screen, GuiGraphics g, Rect area, int mouseX, int mouseY, float partialTick) {
      if (!this.filter.isEmpty() && this.rows.isEmpty()) {
         int y = (this.panel != null ? this.panel.getY() : area.y()) + 24;
         CFHGuiDraw.textCentered(g, Component.translatable("cfhgui_config.search.no_matches"), area.centerX(), y, Theme.current().isVanilla() ? -6250336 : Theme.current().palette().textDim());
      }
   }

   public static record Tab(String key, Component title, List<Section> sections) {
   }

   public static enum Level {
      BASIC,
      ADVANCED,
      ALL;
   }
}
