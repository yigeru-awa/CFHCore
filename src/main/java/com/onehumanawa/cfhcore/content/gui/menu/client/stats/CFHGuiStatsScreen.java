package com.onehumanawa.cfhcore.content.gui.menu.client.stats;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTabStrip;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.SampleStats;
import com.onehumanawa.cfhcore.content.gui.menu.mixin.StatsScreenAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public final class CFHGuiStatsScreen extends StatsScreenBase {
   private static final int PAD = 12;
   private static final int MAX_W = 880;
   private int headerH = 32;
   private Rect area = new Rect(0, 0, 0, 0);
   private @Nullable CFHGuiTabStrip strip;
   private int slideFrom = -1;
   private double slideScroll;
   private int titleRight;
   private int lastIndex = -1;

   public CFHGuiStatsScreen(@Nullable Screen parent, StatsCounter counter, boolean preview, @Nullable StatsTab first) {
      super(parent, counter, preview, first);
   }

   public static @Nullable Screen replacing(@Nullable Screen vanilla) {
      if (vanilla instanceof StatsScreen && !(vanilla instanceof StatsScreenBase)) {
         StatsScreenAccessor opened = (StatsScreenAccessor)vanilla;
         return new CFHGuiStatsScreen(opened.cfhgui$lastScreen(), opened.cfhgui$stats(), false, StatsScreens.takeFirstTab());
      } else {
         return null;
      }
   }

   public static Screen preview(@Nullable Screen parent, @Nullable StatsTab first) {
      SampleStats.register();
      return new CFHGuiStatsScreen(parent, new StatsCounter(), true, first);
   }

   protected void build() {
      this.headerH = this.height < 300 ? 26 : 32;
      int contentW = Math.min(this.width - 24, 880);
      int left = (this.width - contentW) / 2;
      this.add(new CFHGuiIconButton(8, (this.headerH - 20) / 2, 20, Icon.ARROW_LEFT, Component.translatable("gui.back"), this::onClose));
      this.titleRight = this.width - 12;
      if (this.feed.subject() != null) {
         CFHGuiButton self = new CFHGuiButton(0, 0, 10, 20, Component.translatable("cfhgui.stats.back_to_self"), () -> this.viewPlayer((StatsLink.PlayerRef)null));
         self.variant(Variant.GHOST).icon(Icon.CLOSE).iconSize(8);
         int w = Math.min(self.preferredWidth(), Math.max(24, this.width / 2 - 12));
         self.setWidth(w);
         self.setX(this.width - 12 - w);
         self.setY((this.headerH - 20) / 2);
         self.tip(Component.translatable("cfhgui.stats.back_to_self.tip", new Object[]{this.feed.shownName()}));
         this.add(self);
         this.titleRight = self.getX() - 8;
      }

      int at = Math.max(0, this.tabs.indexOf(this.tab));
      CFHGuiTabStrip made = new CFHGuiTabStrip(left, this.headerH, contentW, this.stripTabs(true), at, (i) -> this.pick(i));
      if (made.overflows()) {
         made = new CFHGuiTabStrip(left, this.headerH, contentW, this.stripTabs(false), at, (i) -> this.pick(i));
      }

      if (this.slideFrom >= 0 && this.slideFrom != at) {
         made.animateFrom(this.slideFrom, this.slideScroll);
      }

      this.slideFrom = -1;
      this.strip = (CFHGuiTabStrip)this.add(made);
      int top = this.headerH + 22 + 6;
      this.area = new Rect(left, top, contentW, Math.max(40, this.height - top - 8));
      this.buildContent(this.area);
   }

   private List<CFHGuiTabStrip.Tab> stripTabs(boolean icons) {
      List<CFHGuiTabStrip.Tab> out = new ArrayList(this.tabs.size());

      for(StatsTab t : this.tabs) {
         out.add(icons ? new CFHGuiTabStrip.Tab(t.title(), t.icon) : new CFHGuiTabStrip.Tab(t.title()));
      }

      return out;
   }

   private void pick(int index) {
      if (index >= 0 && index < this.tabs.size()) {
         this.openTab((StatsTab)this.tabs.get(index));
      }

   }

   protected void tabChanged() {
      if (this.strip != null) {
         this.slideFrom = this.lastIndex;
         this.slideScroll = this.strip.scrollOffset();
      }

   }

   public void openTab(StatsTab next) {
      this.lastIndex = this.tabs.indexOf(this.tab);
      super.openTab(next);
   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.cfhguiBackground(g, mouseX, mouseY, partialTick);
      Theme t = Theme.current();
      if (!t.isVanilla() && this.minecraft != null && (this.minecraft.level != null || MenuBackdrop.active())) {
         CFHGuiDraw.rect(g, 0, 0, this.width, this.headerH, t.chrome(t.palette().bg()));
      }

   }

   protected void renderFrame(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      float a = this.open.get();
      int tx = 34;
      int ty = CFHGuiDraw.textY(0, this.headerH);
      Component words = (Component)(this.feed.subject() == null ? this.getTitle() : Component.translatable("cfhgui.stats.title.of", new Object[]{this.getTitle(), this.feed.shownName()}));
      FormattedCharSequence title = CFHGuiDraw.truncate(Fonts.heading(words), this.titleRight - 34);
      if (van) {
         g.drawString(this.font, title, 34, ty, Colors.scaleAlpha(-1, a), true);
         CFHGuiDraw.vanillaSeparator(g, 0, this.headerH - 2, this.width, true, MenuBackdrop.worldBehind());
      } else {
         g.drawString(this.font, title, 34, ty, Colors.scaleAlpha(p.text(), a), false);
         CFHGuiDraw.hline(g, 0, this.headerH - 1, this.width, p.border());
         CFHGuiDraw.accentCap(g, 34, this.headerH - 2, Math.max(16, Math.min(this.font.width(title), 48)), a);
      }

      this.drawPhase(g, this.area, false);
   }

   protected @Nullable GuiEventListener tabsWidget() {
      return this.strip;
   }

   public boolean onPanel() {
      return false;
   }
}
