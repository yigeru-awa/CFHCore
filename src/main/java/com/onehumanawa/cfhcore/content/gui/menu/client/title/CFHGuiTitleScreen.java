package com.onehumanawa.cfhcore.content.gui.menu.client.title;

import com.mojang.math.Axis;
import com.mojang.realmsclient.RealmsMainScreen;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ElementType;
import com.onehumanawa.cfhcore.content.gui.core.layout.ElementTypes;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutApplier;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutAware;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutBackground;
import com.onehumanawa.cfhcore.content.gui.core.layout.ScreenLayout;
import com.onehumanawa.cfhcore.content.gui.core.layout.WidgetKey;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.module.Features;
import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.menu.MenuConfig;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.ModsScreenOpener;
import com.onehumanawa.cfhcore.content.gui.menu.client.screenshots.CFHGuiScreenshotsScreen;
import com.onehumanawa.cfhcore.content.gui.menu.mixin.SplashRendererAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class CFHGuiTitleScreen extends CFHGuiScreen implements LayoutAware, LayoutBackground.Host {
   private static final int NAV_W = 176;
   private static final String LOGO = "logo";
   private static final String FOOTER = "footer";
   private final LogoRenderer logo = new LogoRenderer(false);
   private final long openedMs = Clock.nowMs();
   private @Nullable String splash;
   private @Nullable ContinueCard continueCard;
   private int logoY;
   private int navBottom;
   private int navLeft;
   private int navTop;
   private int navCount;
   private boolean wide;
   private boolean logoShown = true;
   private boolean footerShown = true;

   public CFHGuiTitleScreen() {
      super(Component.translatable("narrator.screen.title"), (Screen)null);
      this.showHeader = false;
      this.showBack = false;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public boolean isPauseScreen() {
      return false;
   }

   protected void build() {
      Minecraft mc = Minecraft.getInstance();
      MenuConfig cfg = CFHGuiMenu.config();
      ScreenLayout layout = LayoutApplier.current(this);
      this.logoShown = !layout.hides("logo");
      this.footerShown = !layout.hides("footer");
      if (this.continueCard != null) {
         this.continueCard.close();
         this.continueCard = null;
      }

      if (this.splash == null && cfg.splash) {
         SplashRenderer s = mc.getSplashManager().getSplash();
         this.splash = s == null ? null : ((SplashRendererAccessor)s).cfhgui$splash();
      }

      boolean compact = this.height < 300;
      this.wide = this.width >= 400;
      this.logoY = compact ? 4 : 20;
      int top = this.logoY + 51 + (compact ? 8 : 14);
      int rowH = compact ? 20 : 24;
      int gap = compact ? 2 : 4;
      int contentW = Math.min(this.width - 24, 560);
      this.navLeft = this.wide ? (this.width - contentW) / 2 : (this.width - 176) / 2;
      this.navTop = top;
      int y = top;
      this.navCount = 0;

      for(NavButton b : this.navButtons(mc, cfg)) {
         String key = WidgetKey.first(b.getMessage());
         if (!layout.hides(key)) {
            b.setX(this.navLeft);
            b.setY(y);
            b.setHeight(rowH);
            this.add(b);
            if (!layout.moves(key)) {
               y += rowH + gap;
               ++this.navCount;
            }
         }
      }

      this.navBottom = this.navCount > 0 ? y - gap : top;
      int footerTop = this.height - 12;
      int cw;
      int cx;
      int cy;
      if (this.wide) {
         cx = this.navLeft + 176 + 20;
         cy = top;
         cw = contentW - 176 - 20;
      } else {
         cx = this.navLeft;
         cy = this.navBottom + 8;
         cw = 176;
      }

      if (cfg.showContinueCard && !layout.hides("#ContinueCard#0") && cy + 66 <= footerTop) {
         this.continueCard = (ContinueCard)this.add(new ContinueCard(cx, cy, cw, 66, this));
         if (!layout.moves("#ContinueCard#0")) {
            cy += 74;
         }
      }

      if (cfg.showAccountCard && !layout.hides("#AccountCard#0") && cy + 36 <= footerTop) {
         this.add(new AccountCard(cx, cy, cw, 36, this));
         if (!layout.moves("#AccountCard#0")) {
            cy += 44;
         }
      }

      if (cy + 20 <= footerTop) {
         List<CFHGuiIconButton> icons = new ArrayList();
         icons.add(new CFHGuiIconButton(0, cy, 20, Icon.LANGUAGE, Component.translatable("options.language"), () -> mc.setScreen(new LanguageSelectScreen(this, mc.options, mc.getLanguageManager()))));
         icons.add(new CFHGuiIconButton(0, cy, 20, Icon.ACCESSIBILITY, Component.translatable("options.accessibility.title"), () -> mc.setScreen(new AccessibilityOptionsScreen(this, mc.options))));
         if (mc.allowsRealms()) {
            icons.add(new CFHGuiIconButton(0, cy, 20, Icon.REALMS, Component.translatable("menu.online"), () -> mc.setScreen(new RealmsMainScreen(this))));
         }

         int bx = cx;
         boolean any = false;

         for(CFHGuiIconButton b : icons) {
            String key = WidgetKey.first(b.getMessage());
            if (!layout.hides(key)) {
               b.setX(bx);
               this.add(b);
               if (!layout.moves(key)) {
                  bx += 24;
                  any = true;
               }
            }
         }

         if (any) {
            cy += 28;
         }
      }

      if (this.wide && cfg.showFriendsPanel && Modules.isLoaded("cfhgui_multiplayer")) {
         int fh = footerTop - 6 - cy;
         if (fh >= 60) {
            this.friendsPanel(cx, cy, cw, fh).ifPresent((x$0) -> {
               AbstractWidget var10000 = (AbstractWidget)this.add(x$0);
            });
         }
      }

   }

   public List<LayoutAware.Part> layoutParts() {
      int footerY = this.height - 11;
      return List.of(new LayoutAware.Part("logo", Component.translatable("cfhgui.layout.part.logo"), Rect.of(this.width / 2 - 128, this.logoY, 256, 51)), new LayoutAware.Part("footer", Component.translatable("cfhgui.layout.part.footer"), Rect.of(12, footerY - 1, Math.max(1, this.width - 24), 10)));
   }

   private List<NavButton> navButtons(Minecraft mc, MenuConfig cfg) {
      List<NavButton> nav = new ArrayList();
      nav.add(new NavButton(0, 0, 176, Icon.SINGLEPLAYER, Component.translatable("menu.singleplayer"), () -> mc.setScreen(new SelectWorldScreen(this))));
      NavButton mp = new NavButton(0, 0, 176, Icon.MULTIPLAYER, Component.translatable("menu.multiplayer"), () -> mc.setScreen(new JoinMultiplayerScreen(this)));
      if (!mc.allowsMultiplayer()) {
         mp.enabled(false);
         mp.tip(mc.isNameBanned() ? Component.translatable("title.multiplayer.disabled.banned.name") : Component.translatable("title.multiplayer.disabled"));
      }

      nav.add(mp);
      if (Features.present("cfhgui_multiplayer")) {
         nav.add(new NavButton(0, 0, 176, Icon.FRIENDS, Component.translatable("cfhgui.title.friends"), () -> MenuSlots.open("cfhgui_multiplayer:hub", this)));
      }

      nav.add(new NavButton(0, 0, 176, Icon.CAMERA, Component.translatable("cfhgui.screenshots.title"), () -> mc.setScreen(new CFHGuiScreenshotsScreen(this))));
      if (cfg.showModsButton) {
         Optional<Function<Screen, Screen>> mods = ModsScreenOpener.factory();
         mods.ifPresent((f) -> nav.add(new NavButton(0, 0, 176, Icon.MODS, Component.translatable("cfhgui.title.mods"), () -> mc.setScreen((Screen)f.apply(this)))));
      }

      nav.add(new NavButton(0, 0, 176, Icon.SETTINGS, Component.translatable("menu.options"), () -> mc.setScreen(new OptionsScreen(this, mc.options))));
      nav.add((new NavButton(0, 0, 176, Icon.QUIT, Component.translatable("menu.quit"), () -> quit(mc))).danger());
      return nav;
   }

   private static void quit(Minecraft mc) {
      if (!CFHGuiMenu.config().confirmQuitGame) {
         mc.stop();
      } else {
         MutableComponent var10000 = Component.translatable("menu.quit");
         MutableComponent var10001 = Component.translatable("cfhgui.title.confirm_quit");
         MutableComponent var10002 = Component.translatable("menu.quit");
         Objects.requireNonNull(mc);
         CFHGuiModal.confirmDanger(var10000, var10001, var10002, mc::stop);
      }
   }

   private Optional<AbstractWidget> friendsPanel(int x, int y, int w, int h) {
      ElementType type = (ElementType)ElementTypes.get("cfhgui_multiplayer:friends_panel").orElse(null);
      if (type == null) {
         return Optional.empty();
      } else {
         try {
            ScreenLayout.Element e = new ScreenLayout.Element();
            e.id = "title_friends";
            e.type = type.id();
            e.place = new ScreenLayout.Placement("TOP_LEFT", x, y, w, h);
            return Optional.ofNullable(type.create(this, e, x, y, w, h, () -> {
            }));
         } catch (Exception ex) {
            CFHGuiMenu.LOGGER.warn("[CFHGui Menu] friends panel failed to build: {}", ex.toString());
            return Optional.empty();
         }
      }
   }

   public void tick() {
      super.tick();
      if (this.continueCard != null) {
         this.continueCard.tick();
      }

   }

   public void removed() {
      super.removed();
      if (this.continueCard != null) {
         this.continueCard.close();
      }

   }

   private float fadeIn() {
      return Theme.current().motion() <= 0.0F ? 1.0F : Mth.clamp((float)(Clock.nowMs() - this.openedMs) / 700.0F, 0.0F, 1.0F);
   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      MenuConfig cfg = CFHGuiMenu.config();
      Theme t = Theme.current();
      float fade = this.fadeIn();
      if (!LayoutBackground.drawOwn(this, g, partialTick, PANORAMA)) {
         if (MenuBackdrop.active()) {
            super.renderBackground(g, mouseX, mouseY, partialTick);
         } else if (cfg.panorama) {
            PANORAMA.render(g, this.width, this.height, fade, partialTick);
            if (!t.isVanilla()) {
               g.fill(0, 0, this.width, this.height, Colors.withAlpha(921101, Math.round(96.0F * fade)));
               CFHGuiDraw.hgradient(g, 0, 0, Math.min(this.width, 240), this.height, Colors.withAlpha(0, Math.round(88.0F * fade)), 0);
               CFHGuiDraw.vgradient(g, 0, this.height - 70, this.width, 70, 0, Colors.withAlpha(0, Math.round(144.0F * fade)));
            }
         } else if (t.isVanilla()) {
            CFHGuiDraw.vanillaPanel(g, 0, 0, this.width, this.height, false);
         } else {
            super.renderBackground(g, mouseX, mouseY, partialTick);
         }
      }

      if (this.logoShown) {
         this.renderLogo(g, fade);
      }

      if (!t.isVanilla() && this.navCount > 0) {
         int plateTop = this.navTop - 6;
         CFHGuiDraw.floatingPanel(g, this.navLeft - 6, plateTop, 188, this.navBottom - plateTop + 6, fade);
      }

   }

   private void renderLogo(GuiGraphics g, float alpha) {
      if (CFHGuiMenu.config().showLogo) {
         this.logo.renderLogo(g, this.width, alpha, this.logoY);
      } else {
         Palette p = Theme.current().palette();
         g.pose().pushPose();
         g.pose().translate((float)this.width / 2.0F, (float)(this.logoY + 8), 0.0F);
         g.pose().scale(3.0F, 3.0F, 1.0F);
         Component word = Fonts.heading(Component.literal("CFHGui"));
         g.drawString(this.font, word, -this.font.width(word) / 2, 0, Colors.scaleAlpha(p.text(), alpha), Theme.current().isVanilla());
         g.pose().popPose();
         Component sub = Component.translatable("cfhgui.title.wordmark_sub");
         g.drawString(this.font, sub, this.width / 2 - this.font.width(sub) / 2, this.logoY + 40, Colors.scaleAlpha(Theme.current().isVanilla() ? -4144960 : p.textMuted(), alpha), Theme.current().isVanilla());
      }
   }

   protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      float fade = this.fadeIn();
      if (this.logoShown && this.splash != null && !this.splash.isEmpty() && t.isVanilla()) {
         this.renderSplash(g, fade);
      }

      if (CFHGuiMenu.config().showFooter && this.footerShown) {
         int fg = Colors.scaleAlpha(t.isVanilla() ? -1 : p.textMuted(), fade);
         String var10000 = SharedConstants.getCurrentVersion().getName();
         Component version = Component.literal("Minecraft " + var10000 + " · " + CFHGuiPlatform.get().loader().displayName + " · " + CFHGuiPlatform.get().allMods().size() + " mods · CFHGui " + CFHGui.version());
         Component copyright = Component.translatable("title.credits");
         int footerY = this.height - 11;
         if (this.navBottom + 4 <= footerY && this.width - this.font.width(copyright) - 24 > this.navLeft + this.font.width(version)) {
            g.drawString(this.font, version, this.navLeft, footerY, fg, t.isVanilla());
            g.drawString(this.font, copyright, this.width - 12 - this.font.width(copyright), footerY, fg, t.isVanilla());
         } else {
            g.drawString(this.font, copyright, this.width - 12 - this.font.width(copyright), footerY, fg, t.isVanilla());
            g.drawString(this.font, version, this.width - 12 - this.font.width(version), footerY - 10, fg, t.isVanilla());
         }

      }
   }

   private void renderSplash(GuiGraphics g, float fade) {
      Theme t = Theme.current();
      float in = t.motion() <= 0.0F ? 1.0F : Ease.OUT_BACK.apply(Mth.clamp((float)(Clock.nowMs() - this.openedMs - 300L) / 400.0F, 0.0F, 1.0F));
      if (!(in <= 0.01F)) {
         g.pose().pushPose();
         g.pose().translate((float)this.width / 2.0F + 123.0F, (float)this.logoY + 39.0F, 0.0F);
         g.pose().mulPose(Axis.ZP.rotationDegrees(-20.0F));
         float s = 1.8F - Mth.abs(Mth.sin((float)(Util.getMillis() % 1000L) / 1000.0F * ((float)Math.PI * 2F)) * 0.1F);
         s = s * 100.0F / (float)(this.font.width(this.splash) + 32);
         s *= in;
         g.pose().scale(s, s, s);
         int color = t.isVanilla() ? -256 : t.accent();
         g.drawCenteredString(this.font, this.splash, 0, -8, Colors.scaleAlpha(color, fade));
         g.pose().popPose();
      }
   }
}
