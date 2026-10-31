package com.onehumanawa.cfhcore.content.gui.core.screen;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiBadge;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTabStrip;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public abstract class SidebarScreen extends CFHGuiScreen {
   public static final int NAV_W = 118;
   public static final int NAV_W_NARROW = 30;
   public static final int ROW_H = 20;
   public static final int NAV_TOP = 6;
   public static final int PAGE_TITLE_H = 20;
   private static final int PAGE_TITLE_GAP = 10;
   public static final int TOP_NAV_H = 30;
   private final List<SidebarPage> pages = new ArrayList();
   private final List<AbstractWidget> pageWidgets = new ArrayList();
   private final List<AbstractWidget> pageActions = new ArrayList();
   private @Nullable CFHGuiTabStrip topStrip;
   private int current;
   private final Anim navHighlight;
   private int hoverRow;
   private final String rememberKey;
   private static final Map<String, Integer> LAST_PAGE = new HashMap();
   private int fitWidthFor;
   private int fitWidth;
   private final List<Anim> navHover;

   protected SidebarScreen(Component title, @Nullable Screen parent, String rememberKey) {
      super(title, parent);
      this.navHighlight = new Anim(0.0F, 200, Ease.OUT_CUBIC);
      this.hoverRow = -1;
      this.fitWidthFor = -1;
      this.fitWidth = 118;
      this.navHover = new ArrayList();
      this.rememberKey = rememberKey;
      this.current = (Integer)LAST_PAGE.getOrDefault(rememberKey, 0);
   }

   protected abstract void definePages(List<SidebarPage> var1);

   public List<SidebarPage> pages() {
      return this.pages;
   }

   public int currentIndex() {
      return this.current;
   }

   public @Nullable SidebarPage currentPage() {
      return this.pages.isEmpty() ? null : (SidebarPage)this.pages.get(Math.min(this.current, this.pages.size() - 1));
   }

   protected boolean narrow() {
      return this.width < 420;
   }

   protected boolean overhaulNav() {
      return false;
   }

   protected Component navTitle(SidebarPage page) {
      return page.title();
   }

   protected int navRowHeight() {
      if (!this.overhaulNav()) {
         return 20;
      } else {
         int n = Math.max(1, this.pages.size());
         return Math.max(16, Math.min(24, (this.height - 32 - this.navTopPad() - 4) / n));
      }
   }

   protected int navTopPad() {
      return this.overhaulNav() ? 8 : 6;
   }

   protected int hoveredNavRow() {
      return this.hoverRow;
   }

   protected float navHighlightRow() {
      return Math.max(0.0F, Math.min((float)Math.max(0, this.pages.size() - 1), this.navHighlight.get()));
   }

   protected boolean topNav() {
      return false;
   }

   protected int navWidth() {
      return this.narrow() ? 30 : this.labelFitWidth();
   }

   private int navW() {
      if (this.topNav()) {
         return 0;
      } else if (this.narrow()) {
         return 30;
      } else if (!this.overhaulNav()) {
         return Math.max(this.navWidth(), this.labelFitWidth());
      } else {
         int widest = 0;
         if (this.font != null) {
            for(SidebarPage p : this.pages) {
               widest = Math.max(widest, this.font.width(Fonts.heading(this.navTitle(p))) + (p.badge() > 0 ? 24 : 0));
            }
         }

         return Math.max(96, Math.min(widest + 40, Math.max(118, this.width / 4)));
      }
   }

   private int labelFitWidth() {
      if (this.fitWidthFor != this.width) {
         this.fitWidthFor = this.width;
         int need = 118;
         if (this.font != null) {
            for(SidebarPage p : this.pages) {
               need = Math.max(need, this.font.width(p.title()) + 32 + (p.badge() > 0 ? 24 : 0));
            }
         }

         this.fitWidth = Math.min(need, Math.max(118, this.width * 2 / 5));
      }

      return this.fitWidth;
   }

   public Rect navRect() {
      return this.topNav() ? new Rect(0, 32, this.width, 30) : new Rect(0, 32, this.navW(), this.height - 32);
   }

   public Rect pageTitleRect() {
      int top = this.topNav() ? 62 : 38;
      return new Rect(this.navW() + 12, top, this.width - this.navW() - 24, 20);
   }

   protected boolean showPageTitle() {
      return true;
   }

   public Rect pageRect() {
      Rect t = this.pageTitleRect();
      int top = !this.showPageTitle() && this.pageActions.isEmpty() ? t.y() : t.bottom() + 10;
      return new Rect(t.x(), top, t.w(), this.height - top - 12);
   }

   protected void build() {
      if (this.pages.isEmpty()) {
         this.definePages(this.pages);
      }

      this.fitWidthFor = -1;
      if (this.current >= this.pages.size()) {
         this.current = 0;
      }

      this.navHighlight.snap((float)this.current);
      this.pageWidgets.clear();
      this.pageActions.clear();
      this.topStrip = null;
      if (this.topNav() && !this.pages.isEmpty()) {
         List<CFHGuiTabStrip.Tab> tabs = new ArrayList();

         for(SidebarPage p : this.pages) {
            tabs.add(new CFHGuiTabStrip.Tab(p.title(), p.icon(), p.badge()));
         }

         CFHGuiTabStrip strip = (new CFHGuiTabStrip(12, 36, this.width - 24, tabs, this.current, this::showPage)).style(CFHGuiTabStrip.Style.UNDERLINE);
         this.addRenderableWidget(strip);
         this.topStrip = strip;
      }

      SidebarPage page = this.currentPage();
      if (page != null) {
         page.build(this, this.pageRect());
      }

   }

   public <T extends GuiEventListener & Renderable & NarratableEntry> T addPageWidget(T widget) {
      if (widget instanceof AbstractWidget w) {
         this.pageWidgets.add(w);
      }

      return (T)this.addRenderableWidget(widget);
   }

   public <T extends AbstractWidget> T addPageAction(T widget) {
      Rect t = this.pageTitleRect();
      int x = t.right();

      for(AbstractWidget a : this.pageActions) {
         x = a.getX();
      }

      widget.setX(x - widget.getWidth() - (this.pageActions.isEmpty() ? 0 : 4));
      widget.setY(t.y() + (t.h() - widget.getHeight()) / 2);
      this.pageActions.add(widget);
      this.pageWidgets.add(widget);
      this.addRenderableWidget(widget);
      return widget;
   }

   private void tearDownPage() {
      for(AbstractWidget w : this.pageWidgets) {
         this.removeWidget(w);
      }

      this.pageWidgets.clear();
      this.pageActions.clear();
   }

   public void showPage(int index) {
      if (index >= 0 && index < this.pages.size() && index != this.current) {
         SidebarPage old = this.currentPage();
         if (old != null) {
            old.onHide();
         }

         Popups.closeAll();
         this.tearDownPage();
         this.current = index;
         LAST_PAGE.put(this.rememberKey, index);
         this.navHighlight.set((float)index);
         if (this.topStrip != null && this.topStrip.index() != index) {
            this.topStrip.setIndex(index);
         }

         CFHGuiSounds.tick();
         SidebarPage page = this.currentPage();
         if (page != null) {
            page.build(this, this.pageRect());
         }

         int i = 0;

         for(AbstractWidget w : this.pageWidgets) {
            i = entrance(w, i);
         }

         this.setFocused((GuiEventListener)null);
      }
   }

   public void showPage(String id) {
      for(int i = 0; i < this.pages.size(); ++i) {
         if (((SidebarPage)this.pages.get(i)).id().equals(id)) {
            this.showPage(i);
            return;
         }
      }

   }

   public void refreshPage() {
      this.tearDownPage();
      SidebarPage page = this.currentPage();
      if (page != null) {
         page.build(this, this.pageRect());
      }

   }

   public void tick() {
      super.tick();
      SidebarPage page = this.currentPage();
      if (page != null) {
         page.tick();
      }

   }

   public void removed() {
      super.removed();
      SidebarPage page = this.currentPage();
      if (page != null) {
         page.onHide();
      }

   }

   private int rowAt(double mx, double my) {
      if (this.topNav()) {
         return -1;
      } else {
         Rect nav = this.navRect();
         if (nav.contains(mx, my) && !(my < (double)(nav.y() + this.navTopPad()))) {
            int i = (int)((my - (double)nav.y() - (double)this.navTopPad()) / (double)this.navRowHeight());
            return i >= 0 && i < this.pages.size() ? i : -1;
         } else {
            return -1;
         }
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         int row = this.rowAt(mouseX, mouseY);
         if (row >= 0) {
            this.showPage(row);
            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.pages.isEmpty() && hasControlDown()) {
         if (keyCode == 258) {
            this.showPage(((this.current + (hasShiftDown() ? -1 : 1)) % this.pages.size() + this.pages.size()) % this.pages.size());
            return true;
         }

         if (keyCode >= 49 && keyCode <= 57 && keyCode - 49 < this.pages.size()) {
            this.showPage(keyCode - 49);
            return true;
         }
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.hoverRow = this.rowAt((double)mouseX, (double)mouseY);
      this.renderBackground(g, mouseX, mouseY, partialTick);
      this.renderNav(g, mouseX, mouseY);
      this.renderPageTitle(g);
      SidebarPage page = this.currentPage();
      if (page != null) {
         page.render(this, g, this.pageRect(), mouseX, mouseY, partialTick);
      }

      for(Renderable r : this.renderableList()) {
         r.render(g, mouseX, mouseY, partialTick);
      }

      if (this.showHeader) {
         this.renderHeader(g);
      }

      this.renderContent(g, mouseX, mouseY, partialTick);
   }

   protected void renderNav(GuiGraphics g, int mouseX, int mouseY) {
      Theme t = Theme.current();
      Palette p = t.palette();
      Rect nav = this.navRect();
      if (this.topNav()) {
         CFHGuiDraw.hline(g, 12, nav.bottom() - 1, this.width - 24, t.isVanilla() ? -9474193 : Colors.withAlpha(p.border(), 192));
      } else if (this.overhaulNav()) {
         this.renderOverhaulNav(g, nav);
      } else {
         boolean icons = this.narrow();
         if (t.isVanilla()) {
            CFHGuiDraw.vanillaListBackground(g, nav.x(), nav.y(), nav.w(), nav.h(), this.minecraft.level != null);
            CFHGuiDraw.vline(g, nav.right() - 1, nav.y(), nav.h(), -16777216);
         } else {
            g.fill(nav.x(), nav.y(), nav.right(), nav.bottom(), t.chrome(p.bg2()));
            CFHGuiDraw.vline(g, nav.right() - 1, nav.y(), nav.h(), p.border());
         }

         float s = Math.max(0.0F, Math.min((float)Math.max(0, this.pages.size() - 1), this.navHighlight.get()));
         int hy = nav.y() + 6 + Math.round(s * 20.0F);
         if (t.isVanilla()) {
            g.fill(nav.x() + 3, hy, nav.right() - 4, hy + 20, 1627389951);
            CFHGuiDraw.outline(g, nav.x() + 3, hy, nav.w() - 7, 20, -1, 0);
         } else {
            CFHGuiDraw.pixelRound(g, nav.x() + 4, hy, nav.w() - 8, 20, p.surfaceActive(), t.radius());
            CFHGuiDraw.rect(g, nav.x() + 4, hy + 4, 2, 12, p.accent());
         }

         for(int i = 0; i < this.pages.size(); ++i) {
            SidebarPage page = (SidebarPage)this.pages.get(i);
            int ry = nav.y() + 6 + i * 20;
            boolean sel = i == this.current;
            boolean hov = i == this.hoverRow;
            if (hov && !sel && !t.isVanilla()) {
               CFHGuiDraw.pixelRound(g, nav.x() + 4, ry, nav.w() - 8, 20, p.surfaceHover(), t.radius());
            }

            int fg = sel ? p.text() : (hov ? p.textMuted() : (t.isVanilla() ? -4144960 : p.textDim()));
            int ix = icons ? nav.x() + (nav.w() - 12) / 2 : nav.x() + 10;
            Icons.draw(g, page.icon(), ix, ry + 4, 12, sel && !t.isVanilla() ? p.accentInk() : fg);
            if (!icons) {
               int badgeW = page.badge() > 0 ? 24 : 0;
               g.drawString(this.font, CFHGuiDraw.truncate(page.title(), nav.w() - 32 - badgeW), nav.x() + 26, CFHGuiDraw.textY(ry, 20), fg, t.isVanilla());
               if (page.badge() > 0) {
                  CFHGuiBadge.drawCount(g, page.badge(), nav.right() - 20, ry + 5);
               }
            } else {
               if (page.badge() > 0) {
                  CFHGuiDraw.pixelCircle(g, nav.right() - 8, ry + 5, 1, p.accent());
               }

               if (hov) {
                  CFHGuiTooltips.request(page.title(), (AbstractWidget)null);
               }
            }
         }

      }
   }

   private void renderOverhaulNav(GuiGraphics g, Rect nav) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      boolean icons = this.narrow();
      int rowH = this.navRowHeight();
      int top = nav.y() + this.navTopPad();

      while(this.navHover.size() < this.pages.size()) {
         this.navHover.add(new Anim(0.0F, 160, Ease.OUT_CUBIC));
      }

      if (van) {
         int line = -16777216;
         CFHGuiDraw.vgradient(g, nav.right() - 1, nav.y(), 1, 18, Colors.withAlpha(-16777216, 0), -16777216);
         CFHGuiDraw.rect(g, nav.right() - 1, nav.y() + 18, 1, Math.max(0, nav.h() - 36), -16777216);
         CFHGuiDraw.vgradient(g, nav.right() - 1, nav.bottom() - 18, 1, 18, -16777216, Colors.withAlpha(-16777216, 0));
      } else {
         CFHGuiDraw.rect(g, nav.x(), nav.y(), nav.w(), nav.h(), t.chrome(p.bg2()));
         CFHGuiDraw.vline(g, nav.right() - 1, nav.y(), nav.h(), p.border());
      }

      int hy = top + Math.round(this.navHighlightRow() * (float)rowH);
      int accent = van ? -1 : p.accent();
      CFHGuiDraw.hgradient(g, nav.x() + 3, hy + 2, nav.w() - 8, rowH - 4, Colors.withAlpha(accent, van ? 56 : 70), Colors.withAlpha(accent, 0));
      CFHGuiDraw.rect(g, nav.x() + 3, hy + 3, 2, rowH - 6, accent);

      for(int i = 0; i < this.pages.size(); ++i) {
         SidebarPage page = (SidebarPage)this.pages.get(i);
         int ry = top + i * rowH;
         boolean sel = i == this.current;
         Anim hover = (Anim)this.navHover.get(i);
         hover.set(i == this.hoverRow);
         float h = hover.get();
         int rest = van ? -5197648 : p.textDim();
         int lit = van ? -1 : p.text();
         int fg = sel ? lit : Colors.lerp(rest, van ? -2039584 : p.textMuted(), h);
         int push = Math.round(2.0F * h);
         int ix = icons ? nav.x() + (nav.w() - 12) / 2 : nav.x() + 11 + push;
         Icons.draw(g, page.icon(), ix, ry + (rowH - 12) / 2, 12, sel && !van ? p.accentInk() : fg);
         if (icons) {
            if (page.badge() > 0) {
               CFHGuiDraw.pixelCircle(g, nav.right() - 8, ry + 5, 1, accent);
            }

            if (i == this.hoverRow) {
               CFHGuiTooltips.request(this.navTitle(page), (AbstractWidget)null);
            }
         } else {
            int badgeW = page.badge() > 0 ? 24 : 0;
            g.drawString(this.font, CFHGuiDraw.truncate(Fonts.heading(this.navTitle(page)), nav.w() - 34 - badgeW), nav.x() + 28 + push, CFHGuiDraw.textY(ry, rowH), fg, van);
            if (page.badge() > 0) {
               CFHGuiBadge.drawCount(g, page.badge(), nav.right() - 22, ry + (rowH - 10) / 2);
            }
         }
      }

   }

   protected void renderPageTitle(GuiGraphics g) {
      SidebarPage page = this.currentPage();
      if (page != null && (this.showPageTitle() || !this.pageActions.isEmpty())) {
         Theme t = Theme.current();
         Palette p = t.palette();
         Rect r = this.pageTitleRect();
         int right = r.right();

         for(AbstractWidget a : this.pageActions) {
            right = Math.min(right, a.getX() - 8);
         }

         g.drawString(this.font, CFHGuiDraw.truncate(Fonts.heading(page.title()), right - r.x()), r.x(), CFHGuiDraw.textY(r.y(), r.h()), t.isVanilla() ? -1 : p.text(), t.isVanilla());
         CFHGuiDraw.hline(g, r.x(), r.bottom() + 1, r.w(), t.isVanilla() ? -9474193 : Colors.withAlpha(p.border(), 192));
      }
   }
}
