package com.onehumanawa.cfhcore.content.gui.menu.client.options;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Flow;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarPage;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop.Kind;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSeparator;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSlider;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style;
import com.onehumanawa.cfhcore.content.gui.menu.MenuConfig;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.MenuClient;
import com.onehumanawa.cfhcore.content.gui.menu.client.creative.CreativeOptions;
import com.onehumanawa.cfhcore.content.gui.menu.client.infohud.InfoHudOptions;
import com.onehumanawa.cfhcore.content.gui.menu.client.lookat.LookAtOptions;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.TitlePlayIcon;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.TitleStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.NarratorStatus;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.ChatOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OnlineOptionsScreen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.telemetry.TelemetryInfoScreen;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.OptionEnum;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;
import org.jetbrains.annotations.Nullable;

public final class CFHGuiOptionsScreen extends SidebarScreen {
   private boolean dirty;
   private long dirtyMs;

   public CFHGuiOptionsScreen(@Nullable Screen parent) {
      super(Component.translatable("options.title"), parent, "cfhgui:options");
   }

   protected void definePages(List<SidebarPage> pages) {
      pages.add(new GeneralPage());
      pages.add(new Page("video", Component.translatable("options.video"), Icon.MONITOR, (s) -> new VideoSettingsScreen(s, Minecraft.getInstance(), opts()), this::videoQuick));
      pages.add(new Page("sounds", Component.translatable("options.sounds"), Icon.VOLUME, (s) -> new SoundOptionsScreen(s, opts()), this::soundQuick));
      pages.add(new Page("controls", Component.translatable("options.controls"), Icon.KEYBOARD, (s) -> new ControlsScreen(s, opts()), this::controlsQuick));
      pages.add(new Page("chat", Component.translatable("options.chat.title"), Icon.CHAT, (s) -> new ChatOptionsScreen(s, opts()), this::chatQuick));
      pages.add(new Page("skin", Component.translatable("options.skinCustomisation"), Icon.USER, (s) -> new SkinCustomizationScreen(s, opts()), this::skinQuick));
      pages.add(new Page("language", Component.translatable("options.language"), Icon.LANGUAGE, (s) -> new LanguageSelectScreen(s, opts(), Minecraft.getInstance().getLanguageManager()), this::languageQuick));
      pages.add(new Page("accessibility", Component.translatable("options.accessibility.title"), Icon.ACCESSIBILITY, (s) -> new AccessibilityOptionsScreen(s, opts()), this::accessibilityQuick));
      pages.add(new Page("online", Component.translatable("options.online"), Icon.WIFI, (s) -> new OnlineOptionsScreen(s, opts()), this::onlineQuick));
      pages.add(new Page("packs", Component.translatable("options.resourcepack"), Icon.PACK, (s) -> {
         Minecraft mc = Minecraft.getInstance();
         return new PackSelectionScreen(mc.getResourcePackRepository(), (repo) -> {
            mc.options.updateResourcePacks(repo);
            mc.setScreen(s);
         }, mc.getResourcePackDirectory(), Component.translatable("resourcePack.title"));
      }, (Consumer)null));
      pages.add(new Page("telemetry", Component.translatable("options.telemetry"), Icon.INFO, (s) -> new TelemetryInfoScreen(s, opts()), (Consumer)null));
      pages.add(new Page("credits", Component.translatable("options.credits_and_attribution"), Icon.HEART, CreditsAndAttributionScreen::new, (Consumer)null));
      pages.add(new CFHGuiPage());
      pages.add(new MenuPage());
   }

   private static Options opts() {
      return Minecraft.getInstance().options;
   }

   private void markDirty() {
      this.dirty = true;
      this.dirtyMs = Clock.nowMs();
   }

   private void saveNow() {
      if (this.dirty) {
         this.dirty = false;
         opts().save();
      }
   }

   public void tick() {
      super.tick();
      if (this.dirty && Clock.nowMs() - this.dirtyMs > 800L) {
         this.saveNow();
      }

   }

   public void removed() {
      super.removed();
      this.saveNow();
   }

   private static int[] range(OptionInstance<Integer> opt, int defMin, int defMax) {
      try {
         Object v = OptionInstance.class.getMethod("values").invoke(opt);
         if (v instanceof OptionInstance.IntRange r) {
            return new int[]{r.minInclusive(), r.maxInclusive()};
         }
      } catch (Throwable var5) {
      }

      return new int[]{defMin, defMax};
   }

   private CFHGuiSlider intSlider(int w, Component label, OptionInstance<Integer> opt, int defMin, int defMax, int step, @Nullable DoubleFunction<String> fmt) {
      int[] r = range(opt, defMin, defMax);
      return (new CFHGuiSlider(0, 0, w, label, (double)r[0], (double)r[1], (double)step, (double)(Integer)opt.get(), fmt != null ? fmt : (v) -> Integer.toString((int)Math.round(v)), (v) -> {
         opt.set((int)Math.round(v));
         this.markDirty();
      })).compact(true);
   }

   private CFHGuiSlider percentSlider(int w, Component label, OptionInstance<Double> opt) {
      return (new CFHGuiSlider(0, 0, w, label, (double)0.0F, (double)1.0F, 0.01, (Double)opt.get(), (v) -> Math.round(v * (double)100.0F) + "%", (v) -> {
         opt.set(v);
         this.markDirty();
      })).compact(true);
   }

   private CFHGuiToggle toggle(int w, Component label, OptionInstance<Boolean> opt) {
      return new CFHGuiToggle(0, 0, w, label, (Boolean)opt.get(), (v) -> {
         opt.set(v);
         this.markDirty();
      });
   }

   private <T> CFHGuiDropdown<T> dropdown(int w, Component label, OptionInstance<T> opt, List<T> values, Function<T, Component> labeler) {
      return (new CFHGuiDropdown<T>(0, 0, w, values, opt.get(), labeler, (v) -> {
         opt.set(v);
         this.markDirty();
      })).label(label);
   }

   private CFHGuiButton open(int w, Component label, Icon icon, Supplier<Screen> screen) {
      return (new CFHGuiButton(0, 0, w, label, () -> {
         this.saveNow();
         Minecraft.getInstance().setScreen((Screen)screen.get());
      })).icon(icon).variant(Variant.PRIMARY);
   }

   private static Component fovLabel(double v) {
      int i = (int)Math.round(v);
      if (i == 70) {
         return Component.translatable("options.fov.min");
      } else {
         return i == 110 ? Component.translatable("options.fov.max") : Component.literal(Integer.toString(i));
      }
   }

   private void column(SidebarScreen screen, Rect area, Consumer<Col> fill) {
      CFHGuiScrollPanel panel = (CFHGuiScrollPanel)screen.addPageWidget(new CFHGuiScrollPanel(area.x(), area.y(), area.w(), area.h()));
      Col col = new Col(panel, Math.min(area.w() - 10, 400));
      fill.accept(col);
      panel.setContentHeight(col.flow.maxY() + 4);
   }

   private void videoQuick(Col c) {
      Options o = opts();
      c.add(this.intSlider(c.w, Component.translatable("options.renderDistance"), o.renderDistance(), 2, 32, 1, (v) -> Component.translatable("options.chunks", new Object[]{(int)Math.round(v)}).getString()));
      c.add(this.intSlider(c.w, Component.translatable("options.simulationDistance"), o.simulationDistance(), 5, 32, 1, (v) -> Component.translatable("options.chunks", new Object[]{(int)Math.round(v)}).getString()));
      c.add(this.percentSlider(c.w, Component.translatable("options.gamma"), o.gamma()));
      c.add(this.dropdown(c.w, Component.translatable("options.graphics"), o.graphicsMode(), Arrays.asList(GraphicsStatus.values()), OptionEnum::getCaption));
      c.add(this.toggle(c.w, Component.translatable("options.vsync"), o.enableVsync()));
      c.add(this.toggle(c.w, Component.translatable("options.fullscreen"), o.fullscreen()));
      c.add(this.intSlider(c.w, Component.translatable("options.framerateLimit"), o.framerateLimit(), 10, 260, 10, (v) -> Math.round(v) >= 260L ? Component.translatable("options.framerateLimit.max").getString() : Component.translatable("options.framerate", new Object[]{(int)Math.round(v)}).getString()));
      c.add(this.toggle(c.w, Component.translatable("options.viewBobbing"), o.bobView()));
      c.add(this.toggle(c.w, Component.translatable("options.entityShadows"), o.entityShadows()));
   }

   private void soundQuick(Col c) {
      Options o = opts();

      for(SoundSource s : SoundSource.values()) {
         c.add(this.percentSlider(c.w, Component.translatable("soundCategory." + s.getName()), o.getSoundSourceOptionInstance(s)));
      }

      c.add(this.toggle(c.w, Component.translatable("options.showSubtitles"), o.showSubtitles()));
      c.add(this.toggle(c.w, Component.translatable("options.directionalAudio"), o.directionalAudio()));
   }

   private void controlsQuick(Col c) {
      Options o = opts();
      c.add(this.open(c.w, Component.translatable("controls.keybinds.title"), Icon.KEYBOARD, () -> new KeyBindsScreen(this, o)).variant(Variant.SECONDARY));
      c.add(this.percentSlider(c.w, Component.translatable("options.sensitivity"), o.sensitivity()));
      c.add(this.toggle(c.w, Component.translatable("options.invertMouse"), o.invertYMouse()));
      c.add(this.toggle(c.w, Component.translatable("options.rawMouseInput"), o.rawMouseInput()));
      c.add(this.toggle(c.w, Component.translatable("options.autoJump"), o.autoJump()));
      c.add(this.toggle(c.w, Component.translatable("options.toggleCrouch"), o.toggleCrouch()));
      c.add(this.toggle(c.w, Component.translatable("options.discrete_mouse_scroll"), o.discreteMouseScroll()));
   }

   private void chatQuick(Col c) {
      Options o = opts();
      c.add(this.dropdown(c.w, Component.translatable("options.chat.visibility"), o.chatVisibility(), Arrays.asList(ChatVisiblity.values()), OptionEnum::getCaption));
      c.add(this.percentSlider(c.w, Component.translatable("options.chat.opacity"), o.chatOpacity()));
      c.add(this.percentSlider(c.w, Component.translatable("options.chat.scale"), o.chatScale()));
      c.add(this.toggle(c.w, Component.translatable("options.chat.links"), o.chatLinks()));
      c.add(this.toggle(c.w, Component.translatable("options.chat.links.prompt"), o.chatLinksPrompt()));
   }

   private void skinQuick(Col c) {
      c.add(this.dropdown(c.w, Component.translatable("options.mainHand"), opts().mainHand(), Arrays.asList(HumanoidArm.values()), OptionEnum::getCaption));
   }

   private void languageQuick(Col c) {
      Minecraft mc = Minecraft.getInstance();
      LanguageInfo info = mc.getLanguageManager().getLanguage(mc.getLanguageManager().getSelected());
      c.note(Component.translatable("cfhgui.options.current_language", new Object[]{info == null ? Component.literal(mc.getLanguageManager().getSelected()) : info.toComponent()}));
      c.add(this.toggle(c.w, Component.translatable("options.forceUnicodeFont"), opts().forceUnicodeFont()));
   }

   private void accessibilityQuick(Col c) {
      Options o = opts();
      c.add(this.dropdown(c.w, Component.translatable("options.narrator"), o.narrator(), Arrays.asList(NarratorStatus.values()), NarratorStatus::getName));
      c.add(this.toggle(c.w, Component.translatable("options.accessibility.high_contrast"), o.highContrast()));
      c.add(this.toggle(c.w, Component.translatable("options.reducedDebugInfo"), o.reducedDebugInfo()));
      c.add(this.toggle(c.w, Component.translatable("options.hideLightningFlashes"), o.hideLightningFlash()));
      c.add(this.toggle(c.w, Component.translatable("options.darkMojangStudiosBackgroundColor"), o.darkMojangStudiosBackground()));
      c.add(this.percentSlider(c.w, Component.translatable("options.accessibility.panorama_speed"), o.panoramaSpeed()));
      c.add(this.percentSlider(c.w, Component.translatable("options.screenEffectScale"), o.screenEffectScale()));
      c.add(this.percentSlider(c.w, Component.translatable("options.fovEffectScale"), o.fovEffectScale()));
   }

   private void onlineQuick(Col c) {
      Options o = opts();
      c.add(this.toggle(c.w, Component.translatable("options.realmsNotifications"), o.realmsNotifications()));
      c.add(this.toggle(c.w, Component.translatable("options.allowServerListing"), o.allowServerListing()));
   }

   private static final class Col {
      final CFHGuiScrollPanel panel;
      final Flow flow = Flow.column(0, 0, 6);
      final int w;

      Col(CFHGuiScrollPanel panel, int w) {
         this.panel = panel;
         this.w = w;
      }

      <T extends AbstractWidget> T add(T widget) {
         widget.setWidth(this.w);
         this.panel.add(this.flow.place(widget), widget.getX(), widget.getY());
         return widget;
      }

      void section(Component caption) {
         this.add(new CFHGuiSeparator(0, 0, this.w, caption));
      }

      void note(Component text) {
         this.add((new CFHGuiLabel(0, 0, this.w, text)).style(Style.MUTED).wrap(true));
      }
   }

   private final class GeneralPage extends SidebarPage {
      GeneralPage() {
         super("general", Component.translatable("cfhgui.options.general"), Icon.SLIDERS);
      }

      public void build(SidebarScreen screen, Rect area) {
         Minecraft mc = Minecraft.getInstance();
         Options o = CFHGuiOptionsScreen.opts();
         CFHGuiOptionsScreen.this.column(screen, area, (c) -> {
            c.section(Component.translatable("cfhgui.options.section.play"));
            c.add(CFHGuiOptionsScreen.this.intSlider(c.w, Component.translatable("options.fov"), o.fov(), 30, 110, 1, (v) -> CFHGuiOptionsScreen.fovLabel(v).getString()));
            c.add(CFHGuiOptionsScreen.this.intSlider(c.w, Component.translatable("options.renderDistance"), o.renderDistance(), 2, 32, 1, (v) -> Component.translatable("options.chunks", new Object[]{(int)Math.round(v)}).getString()));
            if (mc.level != null) {
               boolean canChange = mc.hasSingleplayerServer() && !mc.level.getLevelData().isDifficultyLocked();
               CFHGuiDropdown<Difficulty> diff = (new CFHGuiDropdown<Difficulty>(0, 0, c.w, Arrays.asList(Difficulty.values()), mc.level.getDifficulty(), Difficulty::getDisplayName, (d) -> {
                  if (mc.getConnection() != null) {
                     mc.getConnection().send(new ServerboundChangeDifficultyPacket(d));
                  }

               })).label(Component.translatable("options.difficulty"));
               diff.enabled(canChange);
               if (!canChange) {
                  diff.tip(Component.translatable(mc.level.getLevelData().isDifficultyLocked() ? "difficulty.lock.title" : "cfhgui.options.difficulty_server"));
               }

               c.add(diff);
               if (canChange) {
                  c.add((new CFHGuiButton(0, 0, c.w, Component.translatable("difficulty.lock.title"), () -> CFHGuiModal.confirmDanger(Component.translatable("difficulty.lock.title"), Component.translatable("difficulty.lock.question", new Object[]{mc.level.getDifficulty().getDisplayName()}), Component.translatable("difficulty.lock.title"), () -> {
                        if (mc.getConnection() != null) {
                           mc.getConnection().send(new ServerboundLockDifficultyPacket(true));
                        }

                        CFHGuiOptionsScreen.this.refreshPage();
                     }))).icon(Icon.LOCK).variant(Variant.GHOST));
               }
            }

            c.section(Component.translatable("cfhgui.options.section.display"));
            int maxScale = mc.getWindow().calculateScale(0, mc.isEnforceUnicode());
            List<Integer> scales = new ArrayList();

            for(int i = 0; i <= maxScale; ++i) {
               scales.add(i);
            }

            c.add(CFHGuiOptionsScreen.this.dropdown(c.w, Component.translatable("options.guiScale"), o.guiScale(), scales, (ix) -> ix == 0 ? Component.translatable("options.guiScale.auto") : Component.literal(Integer.toString(ix))));
            c.add(CFHGuiOptionsScreen.this.toggle(c.w, Component.translatable("options.fullscreen"), o.fullscreen()));
            c.add(CFHGuiOptionsScreen.this.toggle(c.w, Component.translatable("options.vsync"), o.enableVsync()));
            c.add(CFHGuiOptionsScreen.this.intSlider(c.w, Component.translatable("options.framerateLimit"), o.framerateLimit(), 10, 260, 10, (v) -> Math.round(v) >= 260L ? Component.translatable("options.framerateLimit.max").getString() : Component.translatable("options.framerate", new Object[]{(int)Math.round(v)}).getString()));
            c.section(Component.translatable("cfhgui.options.section.sound"));
            c.add(CFHGuiOptionsScreen.this.percentSlider(c.w, Component.translatable("soundCategory.master"), o.getSoundSourceOptionInstance(SoundSource.MASTER)));
            c.add(CFHGuiOptionsScreen.this.percentSlider(c.w, Component.translatable("soundCategory.music"), o.getSoundSourceOptionInstance(SoundSource.MUSIC)));
         });
      }
   }

   private final class Page extends SidebarPage {
      private final Function<Screen, Screen> opener;
      private final @Nullable Consumer<Col> quick;

      Page(final String id, final Component title, final Icon icon, final @Nullable Function<Screen, Screen> opener, final Consumer<Col> quick) {
         super(id, title, icon);
         this.opener = opener;
         this.quick = quick;
      }

      public void build(SidebarScreen screen, Rect area) {
         CFHGuiOptionsScreen.this.column(screen, area, (c) -> {
            c.add(CFHGuiOptionsScreen.this.open(c.w, Component.translatable("cfhgui.options.open", new Object[]{this.title()}), this.icon(), () -> (Screen)this.opener.apply(CFHGuiOptionsScreen.this)));
            c.note(Component.translatable("cfhgui.options.open_hint"));
            if (this.quick != null) {
               c.section(Component.translatable("cfhgui.options.section.quick"));
               this.quick.accept(c);
            }

         });
      }
   }

   private final class CFHGuiPage extends SidebarPage {
      CFHGuiPage() {
         super("cfhgui", Component.translatable("cfhgui.name"), Icon.CFHGUI);
      }

      public void build(SidebarScreen screen, Rect area) {
         CFHGuiOptionsScreen.this.column(screen, area, (c) -> {
            c.add(CFHGuiOptionsScreen.this.open(c.w, Component.translatable("cfhgui.settings.title"), Icon.PALETTE, () -> (Screen)((Function)CoreActions.SCREEN_FACTORIES.get("cfhgui:settings")).apply(CFHGuiOptionsScreen.this)));
            c.add(CFHGuiOptionsScreen.this.open(c.w, Component.translatable("cfhgui.hub.title"), Icon.CFHGUI, () -> (Screen)((Function)CoreActions.SCREEN_FACTORIES.get("cfhgui:hub")).apply(CFHGuiOptionsScreen.this)).variant(Variant.SECONDARY));
            if (Modules.isLoaded("cfhgui_config")) {
               Function<Screen, Screen> hub = (Function)CoreActions.SCREEN_FACTORIES.get("cfhgui_config:hub");
               if (hub != null) {
                  c.add(CFHGuiOptionsScreen.this.open(c.w, Component.translatable("cfhgui.options.config_hub"), Icon.SETTINGS, () -> (Screen)hub.apply(CFHGuiOptionsScreen.this)).variant(Variant.SECONDARY));
               } else {
                  CFHGuiPlatform.get().otherModConfigScreen("cfhgui_config").ifPresent((f) -> c.add(CFHGuiOptionsScreen.this.open(c.w, Component.translatable("cfhgui.options.config_hub"), Icon.SETTINGS, () -> (Screen)f.apply(CFHGuiOptionsScreen.this)).variant(Variant.SECONDARY)));
               }
            }

            c.note(Component.translatable("cfhgui.options.cfhgui_hint"));
         });
      }
   }

   private final class MenuPage extends SidebarPage {
      MenuPage() {
         super("menu", Component.translatable("cfhgui.name"), Icon.HOME);
      }

      private CFHGuiToggle cfgToggle(int w, String key, boolean value, Consumer<Boolean> set) {
         return new CFHGuiToggle(0, 0, w, Component.translatable("cfhgui.options.menu." + key), value, (v) -> CFHGuiMenu.configFile().update((c) -> set.accept(v)));
      }

      public void build(SidebarScreen screen, Rect area) {
         MenuConfig m = CFHGuiMenu.config();
         CFHGuiOptionsScreen.this.column(screen, area, (c) -> {
            c.section(Component.translatable("cfhgui.options.menu.section.screens"));
            c.note(Component.translatable("cfhgui.options.menu.screens_hint"));
            c.add(this.cfgToggle(c.w, "title_screen", m.titleScreen, (v) -> m.titleScreen = v));
            c.add(this.cfgToggle(c.w, "worlds_screen", m.worldsScreen, (v) -> m.worldsScreen = v));
            c.add(this.cfgToggle(c.w, "servers_screen", m.serversScreen, (v) -> m.serversScreen = v));
            c.add(this.cfgToggle(c.w, "pause_screen", m.pauseScreen, (v) -> m.pauseScreen = v));
            c.add(this.cfgToggle(c.w, "options_screen", m.optionsScreen, (v) -> m.optionsScreen = v));
            c.add(this.cfgToggle(c.w, "disconnected_screen", m.disconnectedScreen, (v) -> m.disconnectedScreen = v));
            c.add(this.cfgToggle(c.w, "loading_screens", m.loadingScreens, (v) -> {
               m.loadingScreens = v;
               MenuClient.refreshLoadingSupport();
            }));
            c.add(this.cfgToggle(c.w, "tab_list", m.tabList, (v) -> m.tabList = v).tip(Component.translatable("cfhgui.options.menu.tab_list.tip")));
            c.section(Component.translatable("cfhgui.options.menu.section.title"));
            c.add(this.cfgToggle(c.w, "panorama", m.panorama, (v) -> m.panorama = v));
            c.add((new CFHGuiDropdown<Kind>(0, 0, c.w, List.of(Kind.values()), MenuBackdrop.kind(), MenuBackdrop.Kind::title, (v) -> {
               CFHGui.configFile().update((x) -> x.menuBackground = v.id());
               Theme.reload();
            })).label(Component.translatable("cfhgui.options.menu.menu_background")).tip(Component.translatable("cfhgui.options.menu.menu_background.tip")));
            c.add(this.cfgToggle(c.w, "splash", m.splash, (v) -> m.splash = v));
            c.add(this.cfgToggle(c.w, "show_logo", m.showLogo, (v) -> m.showLogo = v));
            c.add(this.cfgToggle(c.w, "continue_card", m.showContinueCard, (v) -> m.showContinueCard = v));
            c.add(this.cfgToggle(c.w, "account_card", m.showAccountCard, (v) -> m.showAccountCard = v));
            c.add(this.cfgToggle(c.w, "mods_button", m.showModsButton, (v) -> m.showModsButton = v));
            c.add(this.cfgToggle(c.w, "footer", m.showFooter, (v) -> m.showFooter = v));
            c.add(this.cfgToggle(c.w, "confirm_quit_game", m.confirmQuitGame, (v) -> m.confirmQuitGame = v));
            c.add(this.cfgToggle(c.w, "skip_onboarding", m.skipOnboarding, (v) -> m.skipOnboarding = v));
            c.add((new CFHGuiDropdown<TitleStyle>(0, 0, c.w, List.of(TitleStyle.values()), TitleStyle.of(m.titleTextures), TitleStyle::title, (v) -> CFHGuiMenu.configFile().update((x) -> x.titleTextures = v.id()))).label(Component.translatable("cfhgui.options.menu.title_textures")).tip(Component.translatable("cfhgui.options.menu.title_textures.tip")));
            c.add(this.cfgToggle(c.w, "title_chests", m.titleChests, (v) -> m.titleChests = v).tip(Component.translatable("cfhgui.options.menu.title_chests.tip")));
            c.add((new CFHGuiDropdown<TitlePlayIcon>(0, 0, c.w, List.of(TitlePlayIcon.values()), TitlePlayIcon.of(m.titlePlayIcon), TitlePlayIcon::title, (v) -> CFHGuiMenu.configFile().update((x) -> x.titlePlayIcon = v.id()))).label(Component.translatable("cfhgui.options.menu.title_play_icon")).tip(Component.translatable("cfhgui.options.menu.title_play_icon.tip")));
            c.section(Component.translatable("cfhgui.options.menu.section.lists"));
            c.add(this.cfgToggle(c.w, "world_details", m.worldsShowDetails, (v) -> m.worldsShowDetails = v));
            c.add(this.cfgToggle(c.w, "auto_refresh", m.serverAutoRefresh, (v) -> m.serverAutoRefresh = v));
            c.add((new CFHGuiSlider(0, 0, c.w, Component.translatable("cfhgui.options.menu.refresh_seconds"), (double)5.0F, (double)120.0F, (double)5.0F, (double)m.serverRefreshSeconds, (v) -> (int)v + " s", (v) -> CFHGuiMenu.configFile().update((x) -> x.serverRefreshSeconds = (int)v))).compact(true));
            c.add(this.cfgToggle(c.w, "show_lan", m.showLan, (v) -> m.showLan = v));
            c.add(this.cfgToggle(c.w, "show_recent", m.showRecent, (v) -> m.showRecent = v));
            c.add(this.cfgToggle(c.w, "show_community", m.showCommunity, (v) -> m.showCommunity = v));
            c.section(Component.translatable("cfhgui.options.menu.section.pause"));
            c.add(this.cfgToggle(c.w, "confirm_quit", m.confirmQuit, (v) -> m.confirmQuit = v));
            c.add(this.cfgToggle(c.w, "session_time", m.showSessionTime, (v) -> m.showSessionTime = v));
            c.section(Component.translatable("cfhgui.options.menu.section.stats"));
            c.add((new CFHGuiSlider(0, 0, c.w, Component.translatable("cfhgui.options.menu.stats_afk_minutes"), (double)1.0F, (double)30.0F, (double)1.0F, (double)Math.max(1, Math.min(30, m.statsAfkMinutes)), (v) -> Component.translatable("cfhgui.options.menu.stats_afk_minutes.value", new Object[]{(int)v}).getString(), (v) -> CFHGuiMenu.configFile().update((x) -> x.statsAfkMinutes = (int)v))).compact(true).tip(Component.translatable("cfhgui.options.menu.stats_afk_minutes.tip")));
            c.add(this.cfgToggle(c.w, "stats_rankings", m.statsRankings, (v) -> m.statsRankings = v).tip(Component.translatable("cfhgui.options.menu.stats_rankings.tip")));

            for(AbstractWidget row : CreativeOptions.rows(c.w)) {
               c.add(row);
            }

            for(AbstractWidget row : InfoHudOptions.rows(c.w, CFHGuiOptionsScreen.this::refreshPage)) {
               c.add(row);
            }

            for(AbstractWidget row : LookAtOptions.rows(c.w, CFHGuiOptionsScreen.this::refreshPage)) {
               c.add(row);
            }

         });
      }
   }
}
