package com.onehumanawa.cfhcore.content.gui.menu.client.stats;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.StatsFeed;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.table.RowIcons;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public abstract class StatsScreenBase extends StatsScreen implements StatsHost {
   protected final boolean preview;
   protected final StatsFeed feed;
   protected final StatsContent content;
   protected StatsTab tab;
   protected List<StatsTab> tabs = List.of();
   protected final Anim open;
   private final List<Renderable> own;
   private int contentFrom;
   private int contentTo;
   private boolean opened;
   private boolean entered;
   private boolean tabOpened;
   private @Nullable UUID builtSubject;
   private boolean rebuilding;
   private boolean relayoutDue;

   protected StatsScreenBase(@Nullable Screen parent, StatsCounter counter, boolean preview, @Nullable StatsTab first) {
      super(parent, counter);
      this.open = new Anim(0.0F, 260, Ease.OUT_CUBIC);
      this.own = new ArrayList();
      this.preview = preview;
      this.feed = new StatsFeed(counter, preview);
      this.content = new StatsContent(this, this::relayout);
      this.tab = first == null ? StatsTab.OVERVIEW : first;
   }

   protected final void init() {
      this.own.clear();
      this.tabs = this.availableTabs();
      if (!this.tabs.contains(this.tab)) {
         this.tab = StatsTab.OVERVIEW;
      }

      this.builtSubject = this.feed.subject() == null ? null : this.feed.subject().id();
      if (!this.opened) {
         this.opened = true;
         this.open.set(1.0F);
         this.feed.open();
      }

      this.build();
      if (!this.entered) {
         this.entered = true;
         int i = 0;

         for(Renderable r : this.own) {
            i = CFHGuiScreen.entrance(r, i);
         }
      } else if (this.tabOpened) {
         int i = 0;

         for(int k = this.contentFrom; k < Math.min(this.contentTo, this.own.size()); ++k) {
            i = CFHGuiScreen.entrance((Renderable)this.own.get(k), i);
         }
      }

      this.tabOpened = false;
   }

   protected abstract void build();

   protected final void buildContent(Rect area) {
      this.contentFrom = this.own.size();
      this.content.build(this.tab, area, this::add);
      this.contentTo = this.own.size();
   }

   protected final <T extends GuiEventListener & Renderable & NarratableEntry> T add(T widget) {
      this.own.add(widget);
      return (T)this.addRenderableWidget(widget);
   }

   private List<StatsTab> availableTabs() {
      boolean server = StatsFeed.provider().isPresent();
      List<StatsTab> out = new ArrayList();

      for(StatsTab t : StatsTab.values()) {
         if (!t.server || server && (t != StatsTab.RANKINGS || CFHGuiMenu.config().statsRankings)) {
            out.add(t);
         }
      }

      return out;
   }

   protected final void relayout() {
      this.relayoutDue = true;
   }

   private void relayoutNow() {
      this.relayoutDue = false;
      if (this.minecraft != null && !this.rebuilding) {
         this.rebuilding = true;
         boolean onTabs = this.getFocused() != null && this.getFocused() == this.tabsWidget();

         try {
            this.rebuildWidgets();
         } finally {
            this.rebuilding = false;
         }

         GuiEventListener tabsNow = this.tabsWidget();
         if (onTabs && tabsNow != null) {
            this.setFocused(tabsNow);
         }

      }
   }

   protected abstract @Nullable GuiEventListener tabsWidget();

   protected void repositionElements() {
      this.relayoutNow();
   }

   public void onStatsUpdated() {
      this.feed.onGameStats();
   }

   public void initLists() {
   }

   public void initButtons() {
   }

   public void setActiveList(@Nullable ObjectSelectionList<?> list) {
   }

   public boolean isPauseScreen() {
      return this.feed.answered();
   }

   public void onClose() {
      Popups.closeAll();
      if (this.minecraft != null) {
         this.minecraft.setScreen(this.lastScreen);
      }

   }

   public void removed() {
      super.removed();
      this.feed.close();
      this.content.close();
      RowIcons.release();
   }

   public void added() {
      super.added();
      if (this.opened) {
         this.feed.resume();
      }

   }

   public void tick() {
      super.tick();
      if (this.relayoutDue) {
         this.relayoutNow();
      }

      this.feed.tick();
      UUID subject = this.feed.subject() == null ? null : this.feed.subject().id();
      if (this.availableTabs().equals(this.tabs) && Objects.equals(subject, this.builtSubject)) {
         this.content.tick();
      } else {
         this.relayout();
      }
   }

   public StatsFeed feed() {
      return this.feed;
   }

   public boolean canChat() {
      return !this.preview && this.minecraft != null && this.minecraft.player != null && this.minecraft.getConnection() != null;
   }

   public boolean canRank() {
      return CFHGuiMenu.config().statsRankings && StatsFeed.provider().isPresent();
   }

   public void rankBy(String key) {
      this.content.rankBy(key);
      this.openTab(StatsTab.RANKINGS);
   }

   public void viewPlayer(StatsLink.@Nullable PlayerRef player) {
      this.feed.view(player);
   }

   public void openTab(StatsTab next) {
      if (next != this.tab && this.tabs.contains(next)) {
         this.tab = next;
         this.tabOpened = true;
         this.tabChanged();
         this.relayout();
      }
   }

   protected void tabChanged() {
   }

   protected final void cycleTab(int step) {
      if (!this.tabs.isEmpty()) {
         int at = Math.max(0, this.tabs.indexOf(this.tab));
         this.openTab((StatsTab)this.tabs.get(((at + step) % this.tabs.size() + this.tabs.size()) % this.tabs.size()));
      }
   }

   private boolean typing() {
      GuiEventListener var2 = this.getFocused();
      boolean var10000;
      if (var2 instanceof EditBox box) {
         if (box.isFocused()) {
            var10000 = true;
            return var10000;
         }
      }

      var10000 = false;
      return var10000;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         CFHGuiSearchField search = this.content.search();
         if (search != null && search.isFocused() && !search.getValue().isEmpty()) {
            search.setValue("");
            return true;
         } else {
            this.onClose();
            return true;
         }
      } else if (keyCode == 258 && hasControlDown()) {
         this.cycleTab(hasShiftDown() ? -1 : 1);
         return true;
      } else {
         GuiEventListener focused = this.getFocused();
         if (focused != null && focused.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
         } else {
            if (!this.typing() && !hasControlDown() && !hasAltDown()) {
               if (!hasShiftDown() && keyCode >= 49 && keyCode <= 57 && keyCode - 49 < this.tabs.size()) {
                  this.openTab((StatsTab)this.tabs.get(keyCode - 49));
                  return true;
               }

               AbstractWidget target = this.content.keyTarget();
               if (target != null && target != focused && keyCode >= 264 && keyCode <= 269 && target.keyPressed(keyCode, scanCode, modifiers)) {
                  this.setFocused(target);
                  return true;
               }

               if (keyCode == 263 || keyCode == 262) {
                  this.cycleTab(keyCode == 263 ? -1 : 1);
                  return true;
               }
            }

            return super.keyPressed(keyCode, scanCode, modifiers);
         }
      }
   }

   public boolean charTyped(char character, int modifiers) {
      if (character == '/' && !this.typing()) {
         CFHGuiSearchField search = this.content.search();
         if (search != null) {
            this.setFocused(search);
            return true;
         }
      }

      return super.charTyped(character, modifiers);
   }

   public final void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      if (this.relayoutDue) {
         this.relayoutNow();
      }

      boolean covered = Popups.any();
      int mx = covered ? -9999 : mouseX;
      int my = covered ? -9999 : mouseY;
      super.render(g, mx, my, partialTick);
      this.renderFrame(g, mx, my, partialTick);
   }

   protected abstract void renderFrame(GuiGraphics var1, int var2, int var3, float var4);

   protected final void cfhguiBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      if (this.minecraft != null) {
         boolean inWorld = this.minecraft.level != null;
         if (t.isVanilla()) {
            super.renderBackground(g, mouseX, mouseY, partialTick);
         } else {
            if (inWorld) {
               if (t.blurInGame()) {
                  this.renderBlurredBackground(partialTick);
               }

               g.fill(0, 0, this.width, this.height, t.palette().overlay());
            } else if (MenuBackdrop.active() && MenuBackdrop.paint(g, this.width, this.height, partialTick)) {
               MenuBackdrop.drawVeil(g, 0, 0, this.width, this.height);
            } else {
               g.fill(0, 0, this.width, this.height, t.bg());
               CFHGuiDraw.vignette(g, 0, 0, this.width, this.height, 0.3F);
            }

         }
      }
   }

   protected final void drawPhase(GuiGraphics g, Rect area, boolean shadow) {
      StatsContent.Phase phase = this.content.phase(this.tab);
      if (phase != StatsContent.Phase.READY) {
         Theme t = Theme.current();
         boolean van = t.isVanilla();
         float a = this.open.get();
         Component message = this.content.message(this.tab);
         FormattedCharSequence text = CFHGuiDraw.truncate(message, area.w() - 16);
         int textW = CFHGuiDraw.width(text);
         int cx = area.x() + area.w() / 2;
         int cy = area.y() + area.h() / 2;
         int muted = Colors.scaleAlpha(van ? -4144960 : t.palette().textMuted(), a);
         if (phase == StatsContent.Phase.LOADING) {
            CFHGuiSpinner.draw(g, cx - (textW + 16) / 2, cy - 5, 10, Colors.scaleAlpha(van ? -1 : t.palette().textMuted(), a));
            g.drawString(this.font, text, cx - (textW + 16) / 2 + 16, cy - 4, muted, van || shadow);
         } else {
            g.drawString(this.font, text, cx - textW / 2, cy - 4, muted, van || shadow);
         }

      }
   }

   protected final int slide() {
      return Math.round((1.0F - this.open.get()) * 6.0F);
   }
}
