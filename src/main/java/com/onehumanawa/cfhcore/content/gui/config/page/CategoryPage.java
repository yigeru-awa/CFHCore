package com.onehumanawa.cfhcore.content.gui.config.page;

import com.onehumanawa.cfhcore.content.gui.config.ConfigSettings;
import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfig;
import com.onehumanawa.cfhcore.content.gui.config.api.SettingsTabs;
import com.onehumanawa.cfhcore.content.gui.config.search.SearchIndex;
import com.onehumanawa.cfhcore.content.gui.config.ui.ApplyQueue;
import com.onehumanawa.cfhcore.content.gui.config.ui.CategoryHost;
import com.onehumanawa.cfhcore.content.gui.config.ui.Enterable;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionPageBase;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.config.ui.TabHost;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarPage;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTabStrip;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class CategoryPage extends SidebarPage implements TabHost, CategoryHost {
   private static final int STRIP_GAP = 6;
   private final List<SidebarPage> builtins;
   private final Map<String, ContributedTabPage> contributed = new HashMap();
   private @Nullable String activeId;
   private @Nullable SidebarScreen screen;
   private Rect childArea = new Rect(0, 0, 0, 0);
   private @Nullable CFHGuiTabStrip strip;
   private int stripFrom = -1;
   private int enterDir;
   private String filterText = "";
   private final Map<String, String> appliedFilter = new HashMap();

   public CategoryPage(String id, Component title, Icon icon, List<? extends SidebarPage> builtins) {
      super(id, title, icon);
      this.builtins = new ArrayList(builtins);
   }

   public List<SidebarPage> children() {
      record Ordered(int order, SidebarPage page) {
      }

      List<Ordered> all = new ArrayList();

      for(int i = 0; i < this.builtins.size(); ++i) {
         all.add(new Ordered(i, (SidebarPage)this.builtins.get(i)));
      }

      for(SettingsTabs.Tab t : SettingsTabs.tabs(this.id())) {
         ContributedTabPage p = (ContributedTabPage)this.contributed.get(t.id());
         if (p == null || p.tab() != t) {
            p = new ContributedTabPage(t);
            this.contributed.put(t.id(), p);
         }

         all.add(new Ordered(t.order(), p));
      }

      all.sort((a, b) -> Integer.compare(a.order(), b.order()));
      List<SidebarPage> out = new ArrayList();

      for(Ordered o : all) {
         SidebarPage var6 = o.page();
         if (var6 instanceof OptionPageBase opb) {
            opb.hosted(this.id() + "/" + o.page().id(), this.title());
         }

         out.add(o.page());
      }

      return out;
   }

   private int activeIndex(List<SidebarPage> kids) {
      String want = this.activeId != null ? this.activeId : ConfigSettings.lastTab(this.id());

      for(int i = 0; i < kids.size(); ++i) {
         if (((SidebarPage)kids.get(i)).id().equals(want)) {
            return i;
         }
      }

      return 0;
   }

   public @Nullable SidebarPage activeChild() {
      List<SidebarPage> kids = this.children();
      return kids.isEmpty() ? null : (SidebarPage)kids.get(this.activeIndex(kids));
   }

   public String activePath() {
      SidebarPage c = this.activeChild();
      return c == null ? this.id() : this.id() + "/" + c.id();
   }

   public @Nullable CFHGuiTabStrip strip() {
      return this.strip;
   }

   public void build(SidebarScreen screen, Rect area) {
      this.screen = screen;
      List<SidebarPage> kids = this.children();
      if (!kids.isEmpty()) {
         int idx = this.activeIndex(kids);
         this.activeId = ((SidebarPage)kids.get(idx)).id();
         double scroll = this.strip != null ? this.strip.scrollOffset() : (double)0.0F;
         boolean keyboard = Minecraft.getInstance().getLastInputType().isKeyboard();
         boolean stripFocused = keyboard && this.strip != null && this.strip.isFocused();
         this.strip = null;
         Rect sub = area;
         if (kids.size() > 1) {
            List<CFHGuiTabStrip.Tab> tabs = new ArrayList();

            for(SidebarPage k : kids) {
               tabs.add(new CFHGuiTabStrip.Tab(k.title(), k.icon()));
            }

            CFHGuiTabStrip s = new CFHGuiTabStrip(area.x(), area.y(), area.w(), tabs, idx, this::select);
            s.animateFrom(this.stripFrom >= 0 ? this.stripFrom : idx, scroll);
            screen.addPageWidget(s);
            if (stripFocused) {
               screen.setFocused(s);
            }

            this.strip = s;
            int top = area.y() + s.getHeight() + 6;
            sub = new Rect(area.x(), top, area.w(), area.bottom() - top);
         }

         this.childArea = sub;
         SidebarPage child = (SidebarPage)kids.get(idx);
         this.applyFilter(child);
         if (this.enterDir != 0 && child instanceof Enterable) {
            Enterable e = (Enterable)child;
            e.enterFrom(this.enterDir);
         }

         this.enterDir = 0;
         this.stripFrom = -1;

         try {
            child.build(screen, sub);
         } catch (Exception e) {
            CFHGuiConfig.LOGGER.error("[CFHGui Config] tab {} of {} failed to build", new Object[]{child.id(), this.id(), e});
         }

      }
   }

   private void applyFilter(SidebarPage child) {
      if (!this.filterText.equals(this.appliedFilter.get(child.id()))) {
         this.appliedFilter.put(child.id(), this.filterText);
         if (child instanceof OptionPageBase) {
            OptionPageBase o = (OptionPageBase)child;
            o.filterSilently(this.filterText);
         } else if (child instanceof ModsPage) {
            ModsPage m = (ModsPage)child;
            m.filterSilently(this.filterText);
         }

      }
   }

   private void select(int i) {
      List<SidebarPage> kids = this.children();
      int cur = this.activeIndex(kids);
      if (i != cur && i >= 0 && i < kids.size()) {
         ApplyQueue.flush();
         ((SidebarPage)kids.get(cur)).onHide();
         this.stripFrom = cur;
         this.enterDir = i > cur ? 1 : -1;
         this.activeId = ((SidebarPage)kids.get(i)).id();
         ConfigSettings.setLastTab(this.id(), this.activeId);
         if (this.screen != null && this.screen.currentPage() == this) {
            this.screen.refreshPage();
         }

      }
   }

   public boolean select(String childId) {
      List<SidebarPage> kids = this.children();

      for(int i = 0; i < kids.size(); ++i) {
         if (((SidebarPage)kids.get(i)).id().equals(childId)) {
            if (this.screen != null && this.screen.currentPage() == this) {
               if (this.strip != null) {
                  this.strip.setIndex(i);
               }

               this.select(i);
            } else {
               this.activeId = childId;
               ConfigSettings.setLastTab(this.id(), childId);
            }

            return true;
         }
      }

      return false;
   }

   public @Nullable SidebarPage child(String childId) {
      for(SidebarPage k : this.children()) {
         if (k.id().equals(childId)) {
            return k;
         }
      }

      return null;
   }

   public void filter(String text) {
      this.filterText = text == null ? "" : text;
      SidebarPage c = this.activeChild();
      if (c != null) {
         this.appliedFilter.put(c.id(), this.filterText);
      }

      if (c instanceof OptionPageBase o) {
         o.filter(this.filterText);
      } else if (c instanceof ModsPage m) {
         m.filter(this.filterText);
      }

   }

   public boolean cycleTab(int dir, boolean secondary) {
      SidebarPage c = this.activeChild();
      if (secondary) {
         boolean var9;
         if (c instanceof TabHost) {
            TabHost th = (TabHost)c;
            if (th.cycleTab(dir, false)) {
               var9 = true;
               return var9;
            }
         }

         var9 = false;
         return var9;
      } else {
         List<SidebarPage> kids = this.children();
         if (kids.size() >= 2) {
            int n = kids.size();
            int to = ((this.activeIndex(kids) + dir) % n + n) % n;
            if (this.strip != null) {
               this.strip.setIndex(to);
            }

            this.select(to);
            return true;
         } else {
            boolean var10000;
            if (c instanceof TabHost) {
               TabHost th = (TabHost)c;
               if (th.cycleTab(dir, false)) {
                  var10000 = true;
                  return var10000;
               }
            }

            var10000 = false;
            return var10000;
         }
      }
   }

   public boolean hasTopTabs() {
      return this.strip != null;
   }

   public int tabCount() {
      return Math.max(1, this.children().size());
   }

   public List<SearchIndex.Entry> searchEntries() {
      List<SearchIndex.Entry> out = new ArrayList();

      for(SidebarPage k : this.children()) {
         String var10000 = this.id();
         String path = var10000 + "/" + k.id();

         try {
            if (k instanceof OptionPageBase o) {
               out.addAll(o.searchEntries(path, this.title(), k.title()));
            } else if (k instanceof ModsPage m) {
               out.addAll(m.searchEntries(path, this.title(), k.title()));
            } else if (k instanceof PresetsPage pr) {
               out.addAll(pr.searchEntries());
            }
         } catch (Exception e) {
            CFHGuiConfig.LOGGER.warn("[CFHGui Config] cannot index {}: {}", path, e.toString());
         }
      }

      return out;
   }

   public List<Section> allSections() {
      List<Section> out = new ArrayList();

      for(SidebarPage k : this.children()) {
         if (k instanceof OptionPageBase o) {
            out.addAll(o.currentSections());
         }
      }

      return out;
   }

   public void render(SidebarScreen screen, GuiGraphics g, Rect area, int mouseX, int mouseY, float partialTick) {
      SidebarPage c = this.activeChild();
      if (c != null) {
         c.render(screen, g, this.childArea, mouseX, mouseY, partialTick);
      }

   }

   public void tick() {
      SidebarPage c = this.activeChild();
      if (c != null) {
         c.tick();
      }

   }

   public void onHide() {
      SidebarPage c = this.activeChild();
      if (c != null) {
         c.onHide();
      }

   }
}
