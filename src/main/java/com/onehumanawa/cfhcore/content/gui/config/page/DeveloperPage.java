package com.onehumanawa.cfhcore.content.gui.config.page;

import com.onehumanawa.cfhcore.content.gui.config.ConfigSettings;
import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfig;
import com.onehumanawa.cfhcore.content.gui.config.api.DeveloperSections;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import com.onehumanawa.cfhcore.content.gui.config.resolver.CoreBindings;
import com.onehumanawa.cfhcore.content.gui.config.ui.ApplyQueue;
import com.onehumanawa.cfhcore.content.gui.config.ui.Controls;
import com.onehumanawa.cfhcore.content.gui.config.ui.TabHost;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarPage;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTabStrip;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public final class DeveloperPage extends SidebarPage implements TabHost {
   public static final String ID = "developer";
   private static final int LINE = 20;
   private static final int CELL_GAP = 12;
   private static final int LABEL_GAP = 6;
   private static final int PAD = 6;
   private static final int STRIP_GAP = 6;
   private static final int CAPTION = 14;
   private static final String[] SWITCHES = new String[]{"devMode", "devGrid", "devSnap", "devPencil"};
   private final List<Cell> cells = new ArrayList();
   private Rect plate = new Rect(0, 0, 0, 0);
   private Rect panelArea = new Rect(0, 0, 0, 0);
   private Rect caption = new Rect(0, 0, 0, 0);
   private DeveloperSections.@Nullable Section shown;
   private @Nullable String activeId;
   private @Nullable SidebarScreen screen;
   private @Nullable CFHGuiTabStrip strip;
   private boolean single;
   private boolean broken;

   public DeveloperPage() {
      super("developer", Component.translatable("cfhgui_config.page.developer"), Icon.CODE);
   }

   private static int controlWidth(OptionBinding b) {
      byte var10000;
      switch (b.type()) {
         case BOOLEAN -> var10000 = 32;
         case CHOICE -> var10000 = 84;
         default -> var10000 = 72;
      }

      return var10000;
   }

   private int activeIndex(List<DeveloperSections.Section> sections) {
      String want = this.activeId != null ? this.activeId : ConfigSettings.lastTab("developer");

      for(int i = 0; i < sections.size(); ++i) {
         if (((DeveloperSections.Section)sections.get(i)).id().equals(want)) {
            return i;
         }
      }

      return 0;
   }

   public void build(SidebarScreen screen, Rect area) {
      this.screen = screen;
      this.cells.clear();
      this.strip = null;
      int x = area.x() + 6;
      int y = area.y() + 3;
      int right = area.right() - 6;

      for(String key : SWITCHES) {
         OptionBinding b = (OptionBinding)CoreBindings.get(key).orElse(null);
         if (b != null) {
            Component label = Component.translatable("cfhgui_config.developer." + key);
            int labelW = CFHGuiDraw.width(label);
            int controlW = controlWidth(b);
            int w = labelW + 6 + controlW;
            if (x + w > right && x > area.x() + 6) {
               x = area.x() + 6;
               y += 22;
            }

            this.cells.add(new Cell(label, x, y));
            AbstractWidget control = Controls.create(b, x + labelW + 6, y, controlW, (v) -> {
               b.set(v);
               CFHGuiSounds.tick();
            });
            if (control != null) {
               control.setY(y + (20 - control.getHeight()) / 2);
               control.active = b.enabled();
               if (control instanceof CFHGuiWidget) {
                  CFHGuiWidget sw = (CFHGuiWidget)control;
                  if (b.tooltip() != null) {
                     sw.tip(b.tooltip());
                  }
               }

               screen.addPageWidget(control);
            }

            x += w + 12;
         }
      }

      this.plate = new Rect(area.x(), area.y(), area.w(), y + 20 + 3 - area.y());
      List<DeveloperSections.Section> sections = DeveloperSections.sections();
      DeveloperSections.Section before = this.shown;
      this.shown = null;
      this.single = sections.size() == 1;
      int top = this.plate.bottom() + 6;
      if (!sections.isEmpty()) {
         int idx = this.activeIndex(sections);
         this.shown = (DeveloperSections.Section)sections.get(idx);
         this.activeId = this.shown.id();
         if (this.single) {
            this.caption = new Rect(area.x(), top, area.w(), 14);
            top += 16;
         } else {
            List<CFHGuiTabStrip.Tab> tabs = new ArrayList();

            for(DeveloperSections.Section s : sections) {
               tabs.add(new CFHGuiTabStrip.Tab(s.title(), s.icon()));
            }

            CFHGuiTabStrip s = new CFHGuiTabStrip(area.x(), top, area.w(), tabs, idx, this::select);
            screen.addPageWidget(s);
            this.strip = s;
            top += s.getHeight() + 6;
         }
      }

      this.panelArea = new Rect(area.x(), top, area.w(), Math.max(0, area.bottom() - top));
      if (before != null && before != this.shown) {
         hide(before);
      }

      this.broken = false;
      if (this.shown != null) {
         try {
            this.shown.panel().build(screen, this.panelArea);
         } catch (Exception e) {
            CFHGuiConfig.LOGGER.error("[CFHGui Config] the developer section {} failed to build", this.shown.id(), e);
            this.broken = true;
         }
      }

   }

   private static void hide(DeveloperSections.Section section) {
      try {
         section.panel().hidden();
      } catch (Exception e) {
         CFHGuiConfig.LOGGER.error("[CFHGui Config] the developer section {} threw", section.id(), e);
      }

   }

   private void select(int i) {
      List<DeveloperSections.Section> sections = DeveloperSections.sections();
      if (i >= 0 && i < sections.size() && i != this.activeIndex(sections)) {
         ApplyQueue.flush();
         this.activeId = ((DeveloperSections.Section)sections.get(i)).id();
         ConfigSettings.setLastTab("developer", this.activeId);
         if (this.screen != null && this.screen.currentPage() == this) {
            this.screen.refreshPage();
         }

      }
   }

   public boolean cycleTab(int dir, boolean secondary) {
      List<DeveloperSections.Section> sections = DeveloperSections.sections();
      if (!secondary && sections.size() >= 2) {
         int n = sections.size();
         int to = ((this.activeIndex(sections) + dir) % n + n) % n;
         if (this.strip != null) {
            this.strip.setIndex(to);
         }

         this.select(to);
         return true;
      } else {
         return false;
      }
   }

   public boolean hasTopTabs() {
      return false;
   }

   public int tabCount() {
      return Math.max(1, DeveloperSections.sections().size());
   }

   public void render(SidebarScreen screen, GuiGraphics g, Rect area, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      if (van) {
         g.fill(this.plate.x(), this.plate.y(), this.plate.right(), this.plate.bottom(), 1610612736);
         CFHGuiDraw.outline(g, this.plate.x(), this.plate.y(), this.plate.w(), this.plate.h(), 1090519039, 0);
      } else {
         CFHGuiDraw.pixelRound(g, this.plate.x(), this.plate.y(), this.plate.w(), this.plate.h(), t.panel(p.surface()), t.radius());
         CFHGuiDraw.outline(g, this.plate.x(), this.plate.y(), this.plate.w(), this.plate.h(), p.border(), t.radius());
      }

      for(Cell cell : this.cells) {
         g.drawString(CFHGuiDraw.font(), cell.label(), cell.x(), CFHGuiDraw.textY(cell.y(), 20), van ? -1 : p.text(), van);
      }

      DeveloperSections.Section section = this.shown;
      if (section == null) {
         Component none = Component.translatable("cfhgui_config.developer.none");
         int y = this.plate.bottom() + 14;

         for(FormattedCharSequence line : CFHGuiDraw.font().split(none, Math.max(40, area.w() - 16))) {
            g.drawString(CFHGuiDraw.font(), line, area.x() + 8, y, van ? -6250336 : p.textMuted(), van);
            y += 10;
         }

      } else {
         if (this.single) {
            int ink = van ? -1 : p.textMuted();
            Icons.draw(g, section.icon(), this.caption.x() + 1, this.caption.y() + (this.caption.h() - 10) / 2, 10, van ? -1 : p.accentInk());
            Component title = Fonts.heading(section.title());
            g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(title, this.caption.w() - 20), this.caption.x() + 15, CFHGuiDraw.textY(this.caption.y(), this.caption.h()), ink, van);
            int lx = this.caption.x() + 15 + Math.min(CFHGuiDraw.width(title), this.caption.w() - 20) + 6;
            if (lx < this.caption.right()) {
               CFHGuiDraw.hline(g, lx, this.caption.y() + this.caption.h() / 2, this.caption.right() - lx, van ? 1090519039 : Colors.withAlpha(p.border(), 192));
            }
         }

         if (!this.broken) {
            try {
               section.panel().render(screen, g, this.panelArea, mouseX, mouseY, partialTick);
            } catch (Exception e) {
               CFHGuiConfig.LOGGER.error("[CFHGui Config] the developer section {} failed to draw", section.id(), e);
               this.broken = true;
            }

         }
      }
   }

   public void tick() {
      DeveloperSections.Section section = this.shown;
      if (section != null && !this.broken) {
         try {
            section.panel().tick();
         } catch (Exception e) {
            CFHGuiConfig.LOGGER.error("[CFHGui Config] the developer section {} threw", section.id(), e);
            this.broken = true;
         }

      }
   }

   public void onHide() {
      DeveloperSections.Section section = this.shown;
      this.shown = null;
      if (section != null) {
         hide(section);
      }

   }

   private static record Cell(Component label, int x, int y) {
   }
}
