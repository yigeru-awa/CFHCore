package com.onehumanawa.cfhcore.content.gui.core.client;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.settings.LayoutStyleRows;
import com.onehumanawa.cfhcore.content.gui.core.client.setup.CFHGuiSetupScreen;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.editor.LayoutEditor;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCard;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSwatches;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import java.util.List;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class CFHGuiHubScreen extends CFHGuiScreen {
   private static final int CARD_PAD = 10;
   private static final int CARD_GAP = 8;
   private static final int ROW_H = 20;

   public CFHGuiHubScreen(@Nullable Screen parent) {
      super(Component.translatable("cfhgui.hub.title"), parent);
      this.maxContentWidth = 440;
   }

   protected void build() {
      Rect c = this.contentRect();
      this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.SETTINGS, Component.translatable("cfhgui.hub.settings"), () -> this.minecraft.setScreen(new CoreSettingsScreen(this))));
      CFHGuiScrollPanel panel = (CFHGuiScrollPanel)this.add((new CFHGuiScrollPanel(c.x(), c.y(), c.w(), c.h())).padding(4));
      int w = panel.innerWidth();
      int inner = w - 20;
      int y = 0;
      CFHGuiCard themeCard = (new CFHGuiCard(0, 0, w, 0)).flat();
      int themeY = 10;
      themeCard.add((new CFHGuiLabel(0, 0, inner, Component.translatable("cfhgui.hub.theme"))).style(CFHGuiLabel.Style.TITLE), 10, themeY);
      themeY += 16;
      int colW = inner >= 300 ? (inner - 8) / 2 : inner;
      CFHGuiToggle containers = LayoutStyleRows.containersToggle(0, 0, colW, (v) -> {
      });
      containers.tip(Component.translatable("cfhgui.setup.containers.desc"));
      themeCard.add(containers, 10, themeY);
      themeY += 24;
      int themeAccent = Colors.fromHex(CFHGui.config().accent, -7726052);
      CFHGuiSwatches sw = new CFHGuiSwatches(0, 0, inner, Palette.ACCENTS, themeAccent, (argb) -> {
         CFHGui.configFile().update((cfg) -> cfg.accent = Colors.toHex(argb));
         Theme.reload();
      });
      themeCard.add(sw, 10, themeY);
      themeY += sw.getHeight() + 8;
      themeCard.add((new CFHGuiButton(0, 0, 130, 16, Component.translatable("cfhgui.hub.settings"), () -> this.minecraft.setScreen(new CoreSettingsScreen(this)))).icon(Icon.SETTINGS).variant(CFHGuiButton.Variant.GHOST).leftAligned(), 6, themeY);
      themeCard.add((new CFHGuiButton(0, 0, 130, 16, Component.translatable("cfhgui.settings.run_setup"), () -> this.minecraft.setScreen(new CFHGuiSetupScreen(this)))).icon(Icon.SPARKLE).variant(CFHGuiButton.Variant.GHOST).leftAligned(), 144, themeY);
      themeY += 26;
      themeCard.setHeight(themeY);
      panel.add(themeCard, 0, y);
      y += themeY + 8;

      List<CFHGuiModule> modules = Modules.all();
      if (modules.isEmpty()) {
         CFHGuiCard card = (new CFHGuiCard(0, 0, w, 0)).flat();
         card.add((new CFHGuiLabel(0, 0, inner, Component.translatable("cfhgui.hub.no_modules"))).style(CFHGuiLabel.Style.TITLE), 10, 10);
         CFHGuiLabel body = (new CFHGuiLabel(0, 0, inner, Component.translatable("cfhgui.hub.no_modules.body"))).style(CFHGuiLabel.Style.MUTED).wrap(true);
         card.add(body, 10, 28);
         card.setHeight(28 + body.getHeight() + 10);
         panel.add(card, 0, y);
         y += card.getHeight() + 8;
      }

      for(CFHGuiModule m : modules) {
         List<CFHGuiModule.HubEntry> entries = m.hubEntries();
         String desc = (String)CFHGuiPlatform.get().modInfo(m.id()).map((ix) -> ix.description()).orElse("");
         CFHGuiCard card = (new CFHGuiCard(0, 0, w, 0)).flat();
         int cy = 10;
         card.add(new IconLabel(m.icon()), 10, cy);
         card.add((new CFHGuiLabel(0, 0, inner - 90, m.displayName())).style(CFHGuiLabel.Style.TITLE), 32, cy + 3);
         card.add((new CFHGuiLabel(0, 0, 60, Component.literal(version(m.id())))).style(CFHGuiLabel.Style.CAPTION).align(CFHGuiLabel.Align.RIGHT), 10 + inner - 60, cy + 4);
         cy += 18;
         if (!desc.isEmpty()) {
            CFHGuiLabel d = (new CFHGuiLabel(0, 0, inner, Component.literal(desc))).style(CFHGuiLabel.Style.MUTED).wrap(true);
            card.add(d, 10, cy);
            cy += Math.min(d.getHeight(), 30) + 4;
         }

         if (!entries.isEmpty()) {
            cy += 2;
            int cols = inner >= 300 ? 2 : 1;
            int bw = (inner - (cols - 1) * 8) / cols;

            for(int i = 0; i < entries.size(); ++i) {
               CFHGuiModule.HubEntry e = (CFHGuiModule.HubEntry)entries.get(i);
               int bx = 10 + i % cols * (bw + 8);
               int by = cy + i / cols * 22;
               card.add((new CFHGuiButton(0, 0, bw, 18, e.label(), e.onClick())).icon(e.icon()).variant(CFHGuiButton.Variant.GHOST).leftAligned(), bx - 4, by);
            }

            cy += (entries.size() + cols - 1) / cols * 22;
         }

         cy += 8;
         card.setHeight(cy);
         panel.add(card, 0, y);
         y += cy + 8;
      }

      CFHGuiCard card = (new CFHGuiCard(0, 0, w, 0)).flat();
      int cy = 10;
      card.add((new CFHGuiLabel(0, 0, inner, Component.translatable("cfhgui.hub.dev"))).style(CFHGuiLabel.Style.TITLE), 10, cy);
      cy += 16;
      CFHGuiButton edit = (new CFHGuiButton(0, 0, 140, 16, Component.translatable("cfhgui.hub.edit_layout"), () -> {
         this.minecraft.setScreen(this.parent);
         if (this.minecraft.screen != null) {
            LayoutEditor.start(this.minecraft.screen);
         }

      })).icon(Icon.EDIT).variant(CFHGuiButton.Variant.GHOST).leftAligned();
      edit.enabled(CFHGui.config().devMode && this.parent != null);
      edit.tip(Component.translatable("cfhgui.hub.edit_layout.tip"));
      card.add(new CFHGuiToggle(0, 0, inner - 150, Component.translatable("cfhgui.settings.dev_mode"), CFHGui.config().devMode, (v) -> {
         CFHGui.configFile().update((cfg) -> cfg.devMode = v);
         edit.enabled(v && this.parent != null);
      }), 10, cy);
      card.add(edit, 10 + inner - 140 - 4, cy + 2);
      cy += 30;
      card.setHeight(cy);
      panel.add(card, 0, y);
      y += cy;
      panel.setContentHeight(y);
   }

   private static String version(String modId) {
      return (String)CFHGuiPlatform.get().modInfo(modId).map((i) -> "v" + i.version()).orElse("");
   }

   protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Component tag = Fonts.heading((Component)Component.literal("CFHGui " + CFHGui.version()));
      CFHGuiDraw.textRight(g, tag, this.width - 12 - 28, CFHGuiDraw.textY(0, 32), t.isVanilla() ? -6250336 : t.palette().textDim(), t.isVanilla());
   }

   private static final class IconLabel extends CFHGuiWidget {
      private final Icon icon;

      IconLabel(Icon icon) {
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
}
