package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.stats;

import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageWidget;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.Fmt;
import com.onehumanawa.cfhcore.content.gui.menu.client.pause.PauseActions;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.StatsScreenBase;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.StatsScreens;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.StatsTab;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.SampleStats;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.Summary;
import com.onehumanawa.cfhcore.content.gui.menu.mixin.StatsScreenAccessor;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class OverhaulStatsScreen extends StatsScreenBase {
   private static final int PANEL_PAD = 6;
   private final Anim slide;
   private @Nullable RecordScene scene;
   private @Nullable WordTabs words;
   private boolean sceneBroken;
   private int sceneVersion;
   private Rect stageRect;
   private Rect panel;
   private Rect area;
   private int margin;
   private int contentW;
   private int headTop;
   private int headRight;
   private boolean compact;
   private final Anim split;
   private boolean splitSet;
   private boolean onPlate;
   private boolean inPanel;

   public OverhaulStatsScreen(@Nullable Screen parent, StatsCounter counter, boolean preview, @Nullable StatsTab first) {
      super(parent, counter, preview, first);
      this.slide = new Anim(0.0F, 220, Ease.OUT_CUBIC);
      this.sceneVersion = Integer.MIN_VALUE;
      this.stageRect = new Rect(0, 0, 0, 0);
      this.panel = new Rect(0, 0, 0, 0);
      this.area = new Rect(0, 0, 0, 0);
      this.split = new Anim(0.0F, 320, Ease.OUT_CUBIC);
   }

   public static void register() {
      ScreenIds.register(OverhaulStatsScreen.class, "cfhgui:stats_overhaul", "Statistics (Overhaul)");
      MenuSlots.provide("minecraft:stats", Layout.OVERHAUL, OverhaulStatsScreen::replacing);
      CoreActions.SCREEN_FACTORIES.put("cfhgui:stats_overhaul", (p) -> StatsScreens.open(p, (StatsTab)null, Layout.OVERHAUL));

      for(StatsTab tab : StatsTab.values()) {
         CoreActions.SCREEN_FACTORIES.put("cfhgui:stats_overhaul/" + tab.id(), (p) -> StatsScreens.open(p, tab, Layout.OVERHAUL));
      }

   }

   public static @Nullable Screen replacing(@Nullable Screen vanilla) {
      if (vanilla instanceof StatsScreen && !(vanilla instanceof StatsScreenBase)) {
         StatsScreenAccessor opened = (StatsScreenAccessor)vanilla;
         return new OverhaulStatsScreen(opened.cfhgui$lastScreen(), opened.cfhgui$stats(), false, StatsScreens.takeFirstTab());
      } else {
         return null;
      }
   }

   public static Screen preview(@Nullable Screen parent, @Nullable StatsTab first) {
      SampleStats.register();
      return new OverhaulStatsScreen(parent, new StatsCounter(), true, first);
   }

   protected void build() {
      this.margin = Mth.clamp(this.width / 22, 12, 40);
      this.compact = this.height < 300;
      int full = Math.min(this.width - this.margin * 2, 640);
      int narrow = Mth.clamp(Math.round((float)this.width * 0.42F), 216, 400);
      int wide = Mth.clamp(Math.round((float)this.width * 0.56F), 230, 640);
      boolean wantScene = !this.sceneBroken && this.width >= 380 && this.height >= 170 && this.width - (this.margin + wide + 10) >= 110;
      if (wantScene && (this.scene == null || this.scene.stage().isClosed())) {
         try {
            this.scene = new RecordScene(this, this, this.content.summary());
            this.sceneVersion = this.feed.version();
         } catch (RuntimeException e) {
            CFHGuiMenu.LOGGER.warn("[CFHGui Menu] the statistics screen's scene could not be made, left out: {}", e.toString());
            this.sceneBroken = true;
            this.scene = null;
            wantScene = false;
         }
      }

      int headW = wantScene ? wide : full;
      boolean roomy = (this.tab == StatsTab.BLOCKS || this.tab == StatsTab.ITEMS) && wide < 300;
      this.contentW = wantScene ? (this.tab == StatsTab.OVERVIEW ? narrow : (roomy ? full : wide)) : full;
      if (!this.splitSet) {
         this.split.snap((float)this.contentW);
      } else {
         this.split.set((float)this.contentW);
      }

      this.splitSet = true;
      StageWidget view = null;
      if (wantScene && this.scene != null) {
         int stageX = this.margin + narrow + 10;
         this.stageRect = new Rect(stageX, 0, this.width - stageX, this.height);
         StageWidget stage = new StageWidget(this.stageRect.x(), this.stageRect.y(), this.stageRect.w(), this.stageRect.h(), this.scene.stage(), this.getTitle());
         view = stage;
         this.addRenderableOnly((g, mouseX, mouseY, partialTick) -> {
            Rect shows = this.visible();
            stage.active = shows.w() > 0;
            if (this.scene != null && shows.w() > 0) {
               this.scene.frame(this.stageRect, shows);
               g.enableScissor(shows.x(), shows.y(), shows.right(), shows.bottom());
               stage.render(g, mouseX, mouseY, partialTick);
               g.disableScissor();
            }
         });
      } else {
         this.stageRect = new Rect(0, 0, 0, 0);
         if (this.scene != null) {
            this.scene.close();
            this.scene = null;
         }
      }

      this.headTop = this.compact ? 6 : Math.max(8, this.margin / 2);
      this.add(new CFHGuiIconButton(this.margin - 4, this.headTop - 4, 16, Icon.ARROW_LEFT, Component.translatable("gui.back"), this::onClose));
      this.headRight = this.margin + headW;
      if (this.feed.subject() != null) {
         CFHGuiButton self = new CFHGuiButton(0, 0, 10, 16, Component.translatable("cfhgui.stats.back_to_self"), () -> this.viewPlayer((StatsLink.PlayerRef)null));
         self.variant(Variant.GHOST).icon(Icon.CLOSE).iconSize(8);
         int w = Math.min(self.preferredWidth(), Math.max(24, headW / 2));
         self.setWidth(w);
         self.setX(this.margin + headW - w);
         self.setY(this.headTop - 4);
         self.tip(Component.translatable("cfhgui.stats.back_to_self.tip", new Object[]{this.feed.shownName()}));
         this.add(self);
         this.headRight = self.getX() - 6;
      }

      int y = this.headTop + (this.compact ? 13 : 48);
      this.words = (WordTabs)this.add(new WordTabs(this.margin, y, headW, this.tabs, this.tab, this.slide, this::openTab));
      y += 21;
      this.panel = new Rect(this.margin - 6, y, this.contentW + 12, Math.max(40, this.height - y - Math.max(8, this.margin / 2)));
      this.area = this.panel.inset(6);
      this.addRenderableOnly((g, mouseX, mouseY, partialTick) -> {
         this.drawPlate(g);
         if (!this.onPlate) {
            this.onPlate = true;
            this.inPanel = !Theme.current().isVanilla();
            if (this.inPanel) {
               Theme.enterPanel();
            }

            g.enableScissor(this.panel.x(), this.panel.y(), this.panel.x() + this.plateW(), this.panel.bottom());
         }
      });
      this.buildContent(this.area);
      this.addRenderableOnly((g, mouseX, mouseY, partialTick) -> this.leavePlate(g));
      if (view != null) {
         this.addWidget(view);
      }

   }

   private int plateW() {
      return Math.round(this.split.get()) + 12;
   }

   private Rect visible() {
      int from = Math.max(this.stageRect.x(), this.margin - 6 + this.plateW() + 4);
      int w = this.stageRect.right() - from;
      return new Rect(from, this.stageRect.y(), w < 48 ? 0 : w, this.stageRect.h());
   }

   private void leavePlate(@Nullable GuiGraphics g) {
      if (this.onPlate) {
         this.onPlate = false;
         if (g != null) {
            g.disableScissor();
         }

         if (this.inPanel) {
            Theme.exitPanel();
         }

         this.inPanel = false;
      }
   }

   public void tick() {
      super.tick();
      if (this.scene != null && this.feed.version() != this.sceneVersion) {
         this.sceneVersion = this.feed.version();

         try {
            this.scene.show(new Summary(this.feed.shown()));
         } catch (RuntimeException e) {
            CFHGuiMenu.LOGGER.warn("[CFHGui Menu] the statistics screen's scene could not follow the numbers, left out: {}", e.toString());
            this.sceneBroken = true;
            this.relayout();
         }
      }

   }

   public void removed() {
      super.removed();
      this.leavePlate((GuiGraphics)null);
      if (this.scene != null) {
         this.scene.close();
         this.scene = null;
      }

   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.leavePlate((GuiGraphics)null);
      this.cfhguiBackground(g, mouseX, mouseY, partialTick);
      float a = this.open.get();
      if (!Theme.current().isVanilla()) {
         CFHGuiDraw.hgradient(g, 0, 0, Math.round((float)this.width * 0.7F), this.height, Colors.withAlpha(-16777216, Math.round(144.0F * a)), 0);
         CFHGuiDraw.vignette(g, 0, 0, this.width, this.height, 0.4F * a);
      }

   }

   private void drawPlate(GuiGraphics g) {
      CFHGuiDraw.floatingPanel(g, this.panel.x(), this.panel.y() + this.slide(), this.plateW(), this.panel.h(), this.open.get());
   }

   protected void renderFrame(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      float a = this.open.get();
      if (!(a <= 0.01F)) {
         int dy = this.slide();
         int ink = Colors.scaleAlpha(van ? -1 : p.text(), a);
         int muted = Colors.scaleAlpha(van ? -3618616 : p.textMuted(), a);
         int accent = Colors.scaleAlpha(van ? -2039584 : p.accentInk(), a);
         int x = this.margin + 16;
         int room = Math.max(40, this.headRight - x);
         String kicker = this.getTitle().getString().toUpperCase(Locale.ROOT);
         Component where = this.where();
         if (this.compact) {
            FormattedCharSequence name = CFHGuiDraw.truncate(Fonts.heading(this.feed.shownName()), room);
            g.drawString(this.font, name, x, this.headTop + dy, ink, true);
            MutableComponent rest = Component.literal(kicker);
            if (where != null) {
               rest.append(" · ").append(where);
            }

            int after = x + this.font.width(name) + 8;
            if (after < this.headRight - 30) {
               g.drawString(this.font, CFHGuiDraw.truncate(rest, this.headRight - after), after, this.headTop + dy, muted, true);
            }
         } else {
            g.drawString(this.font, kicker, x, this.headTop + dy, accent, true);
            FormattedCharSequence name = CFHGuiDraw.truncate(Fonts.heading(this.feed.shownName()), Math.max(40, (this.headRight - this.margin) / 2));
            g.pose().pushPose();
            g.pose().translate((float)this.margin, (float)(this.headTop + 14 + dy), 0.0F);
            g.pose().scale(2.0F, 2.0F, 1.0F);
            g.drawString(this.font, name, 0, 0, ink, true);
            g.pose().popPose();
            if (where != null) {
               g.drawString(this.font, CFHGuiDraw.truncate(where, this.headRight - this.margin), this.margin, this.headTop + 14 + 18 + 4 + dy, muted, true);
            }
         }

         this.drawPhase(g, this.area, true);
         if (this.scene != null && this.stageRect.w() > 0) {
            this.scene.labels(g, this.stageRect, this.visible(), a, van);
         }

      }
   }

   private @Nullable Component where() {
      if (this.feed.subject() != null) {
         Summary s = this.content.summary();
         if (this.feed.subject().online()) {
            return Component.translatable("cfhgui.stats.note.online");
         } else {
            return s.lastSeenMs > 0L ? Component.translatable("cfhgui.stats.last_seen", new Object[]{Fmt.ago(s.lastSeenMs)}) : Component.translatable("cfhgui.stats.note.offline");
         }
      } else if (this.minecraft != null && this.minecraft.level != null) {
         return Component.literal(PauseActions.worldName());
      } else {
         return this.preview ? Component.translatable("cfhgui.stats.preview") : null;
      }
   }

   protected @Nullable GuiEventListener tabsWidget() {
      return this.words;
   }

   public boolean onPanel() {
      return true;
   }
}
