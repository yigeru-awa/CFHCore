package com.onehumanawa.cfhcore.content.gui.core.client.setup;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.preview.ScreenPreview;
import com.onehumanawa.cfhcore.content.gui.core.client.settings.LayoutStyleRows;
import com.onehumanawa.cfhcore.content.gui.core.config.CoreConfig;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCard;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class CFHGuiSetupScreen extends CFHGuiScreen {
   private static final int CARD_PAD = 10;
   private static final int GAP = 8;
   private static final int BOTTOM_H = 34;
   private static final int PANEL_PAD = 2;
   private static final int END_PAD = 2;
   private static final int TITLE_ROW = 16;
   private static final int MIN_FRAME = 70;
   private static final int FRAME_MARGIN = 6;
   private static final int WINDOW_W = 16;
   private static final int WINDOW_H = 9;
   private final @Nullable Runnable onDone;
   private int titleY;
   private Rect bottom;
   private final ScreenPreview menusPreview;

   public CFHGuiSetupScreen(@Nullable Screen parent, @Nullable Runnable onDone) {
      super(Component.translatable("cfhgui.setup.title"), parent);
      this.bottom = new Rect(0, 0, 0, 0);
      this.menusPreview = new ScreenPreview(() -> MenuSlots.preview("minecraft:title", TitleScreen::new));
      this.onDone = onDone;
      this.showHeader = false;
      this.showBack = false;
      this.panoramaBackground = true;
   }

   public CFHGuiSetupScreen(@Nullable Screen parent) {
      this(parent, (Runnable)null);
   }

   public static boolean wanted() {
      return !CFHGui.config().setupDone;
   }

   public boolean isPauseScreen() {
      return false;
   }

   protected void build() {
      boolean compact = this.height < 260;
      this.titleY = compact ? 6 : 14;
      int top = this.titleY + (compact ? 26 : 34);
      int contentW = Math.min(this.width - 24, 680);
      int x0 = (this.width - contentW) / 2;
      Rect body = new Rect(x0, top, contentW, this.height - top - 34);
      this.bottom = new Rect(x0, this.height - 34, contentW, 34);
      CFHGuiScrollPanel panel = (CFHGuiScrollPanel)this.add((new CFHGuiScrollPanel(body.x(), body.y(), body.w(), body.h())).padding(2));
      int inner = panel.innerWidth();
      boolean wide = inner >= 470 && !compact;
      int cardsW = wide ? Math.round((float)inner * 0.52F) : inner;
      int previewW = wide ? inner - cardsW - 8 : inner;
      Row containers = this.containersCard(panel, 0, cardsW, body.h() - 4);
      if (wide) {
         panel.add(new PreviewWidget(previewW, containers.height(), CFHGuiSetupScreen.PreviewWidget.Kind.CONTAINER, containers.caption(), containers.head()), cardsW + 8, containers.y());
         panel.setContentHeight(containers.bottom() + 2);
      } else {
         int y = containers.bottom() + 8;
         int containerH = 14 + frameHeight(previewW, 176, 166, 166);
         panel.add(new PreviewWidget(previewW, containerH, CFHGuiSetupScreen.PreviewWidget.Kind.CONTAINER, 0, 14), 0, y);
         panel.setContentHeight(y + containerH + 2);
      }

      int bw = 110;
      this.add((new CFHGuiButton(this.bottom.right() - 110, this.bottom.y() + 7, 110, Component.translatable("cfhgui.setup.confirm"), this::confirm)).variant(CFHGuiButton.Variant.PRIMARY).icon(Icon.CHECK));
   }

   private Row containersCard(CFHGuiScrollPanel panel, int y, int w, int room) {
      CFHGuiToggle toggle = new CFHGuiToggle(0, 0, w - 20 - 22, Fonts.heading(LayoutStyleRows.containersLabel()), CFHGui.config().reskinContainers, (v) -> {
         LayoutStyleRows.applyContainers(v);
         this.changed();
      });
      return this.optionCard(panel, y, w, Icon.BLOCK, (Component)null, toggle, Component.translatable("cfhgui.setup.containers.desc"), (Component)null, Math.min(10 + toggle.getHeight() + 70, Math.max(0, room)));
   }

   private Row optionCard(CFHGuiScrollPanel panel, int y, int w, Icon icon, @Nullable Component title, AbstractWidget control, Component description, @Nullable Component note, int minHeight) {
      CFHGuiCard card = (new CFHGuiCard(0, 0, w, 10)).flat();
      int inner = w - 20;
      int textX = 32;
      card.add(new IconGlyph(icon), 10, 12);
      int cy = 10;
      int head;
      int caption;
      if (title != null) {
         card.add((new CFHGuiLabel(0, 0, inner - 22, Fonts.heading(title))).style(CFHGuiLabel.Style.TITLE), 32, cy);
         head = cy + 16;
         caption = cy;
         cy += 16;
      } else {
         head = cy + control.getHeight();
         caption = cy + Math.max(0, (control.getHeight() - 9) / 2);
      }

      card.add(control, 32, cy);
      cy += control.getHeight() + 6;
      CFHGuiLabel desc = (new CFHGuiLabel(0, 0, inner - 22, description)).style(CFHGuiLabel.Style.MUTED).wrap(true);
      card.add(desc, 32, cy);
      cy += desc.getHeight();
      if (note != null) {
         cy += 4;
         CFHGuiLabel n = (new CFHGuiLabel(0, 0, inner - 22, note)).style(CFHGuiLabel.Style.CAPTION).wrap(true);
         card.add(n, 32, cy);
         cy += n.getHeight();
      }

      card.setHeight(Math.max(cy + 10, minHeight));
      panel.add(card, 0, y);
      return new Row(y, card.getHeight(), head, caption);
   }

   private static int frameHeight(int width, int aw, int ah, int max) {
      long wanted = Math.round((double)((width - 6) * ah) / (double)aw) + 6L;
      return (int)Math.max(70L, Math.min((long)max, wanted));
   }

   private static Rect fit(Rect box, int aw, int ah) {
      float s = Math.min((float)box.w() / (float)aw, (float)box.h() / (float)ah);
      int w = Math.max(1, Math.round((float)aw * s));
      int h = Math.max(1, Math.round((float)ah * s));
      return new Rect(box.x() + (box.w() - w) / 2, box.y() + (box.h() - h) / 2, w, h);
   }

   private void changed() {
      Reskin.invalidate();
      this.menusPreview.invalidate();
      this.rebuildWidgets();
   }

   private void confirm() {
      CFHGui.configFile().update((c) -> c.setupDone = true);
      Theme.reload();
      Reskin.invalidate();
      if (this.onDone != null) {
         this.onDone.run();
      } else {
         this.minecraft.setScreen(this.parent);
      }

   }

   public void back() {
      this.confirm();
   }

   public void tick() {
      super.tick();
      this.menusPreview.tick();
   }

   public void removed() {
      super.removed();
      this.menusPreview.close();
   }

   protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      int x0 = this.bottom.x();
      Component title = Fonts.heading((Component)Component.translatable("cfhgui.setup.title"));
      g.drawString(this.font, title, x0, this.titleY, van ? -1 : p.text(), van);
      if (!van) {
         CFHGuiDraw.accentCap(g, x0, this.titleY + 11, Math.min(48, this.font.width(title)), 1.0F);
      }

      int subY = this.titleY + (this.height < 260 ? 12 : 16);
      g.drawString(this.font, CFHGuiDraw.truncate(Component.translatable("cfhgui.setup.subtitle"), this.bottom.w()), x0, subY, van ? -4144960 : p.textMuted(), van);
      Component hint = Component.translatable("cfhgui.setup.hint");
      int hintW = this.bottom.w() - 120;
      if (hintW > 60) {
         g.drawString(this.font, CFHGuiDraw.truncate(hint, hintW), x0, CFHGuiDraw.textY(this.bottom.y(), 34), van ? -6250336 : p.textDim(), van);
      }

   }

   private static record Row(int y, int height, int head, int caption) {
      int bottom() {
         return this.y + this.height;
      }
   }

   private static final class IconGlyph extends CFHGuiWidget {
      private final Icon icon;

      IconGlyph(Icon icon) {
         super(0, 0, 16, 16, Component.empty());
         this.icon = icon;
         this.active = false;
      }

      public boolean mouseClicked(double mx, double my, int button) {
         return false;
      }

      public ComponentPath nextFocusPath(FocusNavigationEvent e) {
         return null;
      }

      protected void renderDark(GuiGraphics g, int mx, int my, float pt) {
         Icons.draw(g, this.icon, this.getX(), this.getY() + this.enterOffset(), 16, Colors.scaleAlpha(Theme.current().accentInk(), this.effectiveAlpha()));
      }

      protected void renderVanilla(GuiGraphics g, int mx, int my, float pt) {
         Icons.draw(g, this.icon, this.getX(), this.getY() + this.enterOffset(), 16, Colors.scaleAlpha(-1, this.effectiveAlpha()));
      }
   }

   private final class PreviewWidget extends CFHGuiWidget {
      static final int CAPTION_H = 14;
      private final Kind kind;
      private final int captionY;
      private final int frameTop;

      PreviewWidget(final int w, final int h, final Kind kind, final int captionY, final int frameTop) {
         super(0, 0, w, h, Component.translatable(kind == CFHGuiSetupScreen.PreviewWidget.Kind.MENUS ? "cfhgui.setup.preview.menus" : "cfhgui.setup.preview.containers"));
         this.kind = kind;
         this.captionY = captionY;
         this.frameTop = frameTop;
         this.active = false;
      }

      public boolean mouseClicked(double mx, double my, int button) {
         return false;
      }

      public ComponentPath nextFocusPath(FocusNavigationEvent e) {
         return null;
      }

      protected void renderDark(GuiGraphics g, int mx, int my, float pt) {
         this.draw(g, pt);
      }

      protected void renderVanilla(GuiGraphics g, int mx, int my, float pt) {
         this.draw(g, pt);
      }

      private void draw(GuiGraphics g, float partialTick) {
         float a = this.effectiveAlpha();
         if (!(a <= 0.004F)) {
            int x = this.getX();
            int y = this.getY() + this.enterOffset();
            int w = this.getWidth();
            int h = this.getHeight();
            CoreConfig cfg = CFHGui.config();
            Component caption = this.kind == CFHGuiSetupScreen.PreviewWidget.Kind.MENUS ? Component.translatable("cfhgui.setup.preview.menus") : Component.translatable("cfhgui.setup.preview.containers." + (cfg.reskinContainers ? "custom" : "vanilla"));
            CFHGuiDraw.sectionRule(g, caption, x, y + this.captionY, w, a);
            Rect frame = new Rect(x, y + this.frameTop, w, h - this.frameTop);
            CFHGuiDraw.floatingPanel(g, frame.x(), frame.y(), frame.w(), frame.h(), a);
            Rect in = frame.inset(3);
            if (this.kind == CFHGuiSetupScreen.PreviewWidget.Kind.MENUS) {
               Rect window = CFHGuiSetupScreen.fit(in, 16, 9);
               if (!CFHGuiSetupScreen.this.menusPreview.render(g, window.x(), window.y(), window.w(), window.h(), a, partialTick)) {
                  SetupPreview.menus(g, window, cfg.hasCustomLayout(), cfg.customStyle(), a);
               }
            } else {
               SetupPreview.container(g, in, cfg.reskinContainers, a);
            }

         }
      }

      static enum Kind {
         MENUS,
         CONTAINER;
      }
   }
}
