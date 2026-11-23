package com.onehumanawa.cfhcore.content.gui.core.client;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.settings.LayoutStyleRows;
import com.onehumanawa.cfhcore.content.gui.core.client.setup.CFHGuiSetupScreen;
import com.onehumanawa.cfhcore.content.gui.core.config.CoreConfig;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Flow;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.ContainerMotion;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.EarlyWindowProps;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.PixelFont;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCard;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCheckbox;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiColorField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiProgress;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSegmented;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSlider;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSwatches;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class CoreSettingsScreen extends CFHGuiScreen {
   private static final int CARD_PAD = 10;
   private static final int ROW_GAP = 6;
   private static final int CARD_GAP = 8;
   private CFHGuiSwatches swatches;
   private CFHGuiColorField customColor;
   private @Nullable CFHGuiScrollPanel panel;

   public CoreSettingsScreen(@Nullable Screen parent) {
      super(Component.translatable("cfhgui.settings.title"), parent);
      this.maxContentWidth = 400;
   }

   public void rebuildWidgets() {
      double scroll = this.panel != null ? this.panel.scrollAmount() : (double)0.0F;
      super.rebuildWidgets();
      if (this.panel != null && scroll > (double)0.0F) {
         this.panel.snapScroll(scroll);
      }

   }

   protected void build() {
      Rect c = this.contentRect();
      CoreConfig cfg = CFHGui.config();
      this.panel = (CFHGuiScrollPanel)this.add((new CFHGuiScrollPanel(c.x(), c.y(), c.w(), c.h())).padding(4));
      int w = this.panel.innerWidth();
      int y = 0;
      Section preview = new Section(w, Component.translatable("cfhgui.settings.section.preview"));
      int half = (preview.rowW - 6) / 2;
      CFHGuiButton primary = (new CFHGuiButton(0, 0, half, Component.translatable("cfhgui.settings.preview.primary"), () -> {
      })).variant(CFHGuiButton.Variant.PRIMARY).icon(Icon.SPARKLE);
      CFHGuiButton secondary = new CFHGuiButton(half + 6, 0, half, Component.translatable("cfhgui.settings.preview.secondary"), () -> {
      });
      preview.row(primary);
      preview.card.add(secondary, 10 + half + 6, primary.getY());
      preview.row(new CFHGuiToggle(0, 0, half, Component.translatable("cfhgui.settings.preview.toggle"), true, (v) -> {
      }));
      CFHGuiTextField field = (new CFHGuiTextField(half + 6, 0, half, Component.translatable("cfhgui.settings.preview.field"))).placeholder(Component.translatable("cfhgui.settings.preview.field")).clearButton(true);
      preview.card.add(field, 10 + half + 6, preview.flow.y() - 12 - 14);
      preview.row(new CFHGuiCheckbox(0, 0, half, Component.translatable("cfhgui.settings.preview.checkbox"), true, (v) -> {
      }));
      CFHGuiProgress progress = (new CFHGuiProgress(half + 6, 0, half, 6)).snap(0.66F);
      preview.card.add(progress, 10 + half + 6, preview.flow.y() - 6 - 16 + 5);
      this.panel.add(preview.finish(), 0, y);
      y += preview.card.getHeight() + 8;
      if (Modules.isLoaded("cfhgui_config")) {
         Section modes = new Section(w, Component.translatable("cfhgui.settings.section.modes"));
         modes.row((new CFHGuiLabel(0, 0, modes.rowW, Component.translatable("cfhgui.settings.in_config"))).style(CFHGuiLabel.Style.MUTED).wrap(true));
         modes.flow.skip(2);
         modes.row((new CFHGuiButton(0, 0, 200, Component.translatable("cfhgui.settings.open_interface"), () -> CoreActions.openScreen("cfhgui_config:hub/interface"))).icon(Icon.SLIDERS).variant(CFHGuiButton.Variant.SECONDARY));
         this.panel.add(modes.finish(), 0, y);
         y += modes.card.getHeight() + 8;
      } else {
         Section modes = new Section(w, Component.translatable("cfhgui.settings.section.modes"));
         modes.row(LayoutStyleRows.containersToggle(0, 0, modes.rowW, (v) -> this.rebuildWidgets()));
         modes.row((new CFHGuiLabel(0, 0, modes.rowW, Component.translatable("cfhgui.setup.containers.desc"))).style(CFHGuiLabel.Style.MUTED).wrap(true));
         modes.flow.skip(4);
         modes.row((new CFHGuiButton(0, 0, 130, 16, Component.translatable("cfhgui.settings.run_setup"), () -> this.minecraft.setScreen(new CFHGuiSetupScreen(this)))).icon(Icon.SPARKLE).variant(CFHGuiButton.Variant.GHOST).leftAligned());
         this.panel.add(modes.finish(), 0, y);
         y += modes.card.getHeight() + 8;
         Section menus = new Section(w, Component.translatable("cfhgui.settings.menus"));
         if ((MenuSlots.uiModuleLoaded() || MenuSlots.windowAlone("cfhgui:loading")) && CFHGuiPlatform.get().isModLoaded("create")) {
            menus.row((new CFHGuiLabel(0, 0, menus.rowW, Component.translatable("cfhgui.settings.startup_scene"))).style(CFHGuiLabel.Style.MUTED));
            menus.row((new CFHGuiSegmented<String>(0, 0, menus.rowW, List.of("factory", "island"), "island".equalsIgnoreCase(cfg.startupScene) ? "island" : "factory", (s) -> Component.translatable("cfhgui.startup_scene." + s), (s) -> this.save((x) -> x.startupScene = s))).tip(Component.translatable("cfhgui.settings.startup_scene.tip")));
            menus.flow.skip(4);
         }

         menus.row((new CFHGuiToggle(0, 0, menus.rowW, Component.translatable("cfhgui.settings.startup_timer"), cfg.startupTimer, (v) -> {
            this.save((x) -> x.startupTimer = v);
            EarlyWindowProps.write();
         })).tip(Component.translatable("cfhgui.settings.startup_timer.tip")));
         this.panel.add(menus.finish(), 0, y);
         y += menus.card.getHeight() + 8;
      }

      Section look = new Section(w, Component.translatable("cfhgui.settings.section.look"));
      look.row((new CFHGuiLabel(0, 0, look.rowW, Component.translatable("cfhgui.settings.accent"))).style(CFHGuiLabel.Style.MUTED));
      int accent = Colors.fromHex(cfg.accent, -7726052);
      this.swatches = (CFHGuiSwatches)look.row(new CFHGuiSwatches(0, 0, look.rowW, Palette.ACCENTS, accent, (argb) -> {
         this.save((x) -> x.accent = Colors.toHex(argb));
         if (this.customColor != null) {
            this.customColor.setColor(argb);
         }

      }));
      this.customColor = (CFHGuiColorField)look.row(new CFHGuiColorField(0, 0, look.rowW, accent, (argb) -> {
         this.save((x) -> x.accent = Colors.toHex(argb));
         if (this.swatches != null) {
            this.swatches.setSelectedColor(argb);
         }

      }));
      look.flow.skip(2);
      look.row((new CFHGuiSlider(0, 0, look.rowW, Component.translatable("cfhgui.settings.radius"), (double)0.0F, (double)4.0F, (double)1.0F, (double)cfg.radius, (v) -> Integer.toString((int)v) + " px", (v) -> this.save((x) -> x.radius = (int)v))).compact(true));
      look.row((new CFHGuiToggle(0, 0, look.rowW, Component.translatable("cfhgui.settings.smooth_corners"), cfg.smoothCorners, (v) -> this.save((x) -> x.smoothCorners = v))).tip(Component.translatable("cfhgui.settings.smooth_corners.tip")));
      look.row((new CFHGuiLabel(0, 0, look.rowW, Component.translatable("cfhgui.settings.cursor"))).style(CFHGuiLabel.Style.MUTED));
      look.row((new CFHGuiSegmented<CFHGuiCursor.Style>(0, 0, look.rowW, List.of(CFHGuiCursor.Style.values()), CFHGuiCursor.style(), CFHGuiCursor.Style::title, (s) -> this.save((x) -> x.cursor = s.name()))).tip(Component.translatable("cfhgui.settings.cursor.tip")));
      CFHGuiSegmented<PixelFont> pixelFont = new CFHGuiSegmented<PixelFont>(0, 0, look.rowW, List.of(PixelFont.values()), PixelFont.parse(cfg.pixelFont), PixelFont::label, (f) -> this.save((x) -> x.pixelFont = f.key()));
      pixelFont.active = cfg.headingFont;
      look.row(new CFHGuiToggle(0, 0, look.rowW, Component.translatable("cfhgui.settings.heading_font"), cfg.headingFont, (v) -> {
         this.save((x) -> x.headingFont = v);
         pixelFont.active = v;
      }));
      look.row((new CFHGuiLabel(0, 0, look.rowW, Component.translatable("cfhgui.settings.pixel_font"))).style(CFHGuiLabel.Style.MUTED));
      look.row(pixelFont);
      look.row(new CFHGuiToggle(0, 0, look.rowW, Component.translatable("cfhgui.settings.blur_in_game"), cfg.blurInGame, (v) -> this.save((x) -> x.blurInGame = v)));
      CFHGuiToggle translucent = new CFHGuiToggle(0, 0, look.rowW, Component.translatable("cfhgui.settings.translucent_panels"), cfg.translucentPanels, (v) -> this.save((x) -> x.translucentPanels = v));
      translucent.tip(Component.translatable("cfhgui.settings.translucent_panels.tip"));
      translucent.active = cfg.customStyle();
      look.row(translucent);
      look.row((new CFHGuiToggle(0, 0, look.rowW, Component.translatable("cfhgui.settings.compact_auto_scale"), cfg.compactAutoScale, (v) -> {
         this.save((x) -> x.compactAutoScale = v);
         if ((Integer)this.minecraft.options.guiScale().get() == 0) {
            Minecraft var10000 = this.minecraft;
            Minecraft var10001 = this.minecraft;
            Objects.requireNonNull(var10001);
            var10000.tell(var10001::resizeDisplay);
         }

      })).tip(Component.translatable("cfhgui.settings.compact_auto_scale.tip")));
      if (MenuSlots.uiModuleLoaded()) {
         look.row((new CFHGuiLabel(0, 0, look.rowW, Component.translatable("cfhgui.settings.menu_background"))).style(CFHGuiLabel.Style.MUTED));
         look.row((new CFHGuiSegmented<MenuBackdrop.Kind>(0, 0, look.rowW, List.of(MenuBackdrop.Kind.values()), MenuBackdrop.Kind.of(cfg.menuBackground), MenuBackdrop.Kind::title, (k) -> this.save((x) -> x.menuBackground = k.id()))).optionTip((k) -> Component.translatable("cfhgui.menu_background." + k.id() + ".tip")));
      }

      this.panel.add(look.finish(), 0, y);
      y += look.card.getHeight() + 8;
      Section motion = new Section(w, Component.translatable("cfhgui.settings.section.motion"));
      motion.row((new CFHGuiSlider(0, 0, motion.rowW, Component.translatable("cfhgui.settings.motion"), (double)0.0F, (double)2.0F, (double)0.25F, cfg.motion, (v) -> v <= (double)0.0F ? Component.translatable("cfhgui.settings.motion.off").getString() : "%.2fx".formatted(v), (v) -> this.save((x) -> x.motion = v))).compact(true));
      motion.row(new CFHGuiToggle(0, 0, motion.rowW, Component.translatable("cfhgui.settings.transitions"), cfg.transitions, (v) -> this.save((x) -> x.transitions = v)));
      CFHGuiSlider openMs = (new CFHGuiSlider(0, 0, motion.rowW, Component.translatable("cfhgui.settings.container_open_ms"), (double)60.0F, (double)300.0F, (double)10.0F, (double)cfg.containerOpenMs, (v) -> (int)v + " ms", (v) -> this.save((x) -> x.containerOpenMs = (int)v))).compact(true);
      openMs.active = ContainerMotion.Style.parse(cfg.containerOpen) != ContainerMotion.Style.OFF;
      motion.row((new CFHGuiLabel(0, 0, motion.rowW, Component.translatable("cfhgui.settings.container_open"))).style(CFHGuiLabel.Style.MUTED));
      motion.row((new CFHGuiSegmented<ContainerMotion.Style>(0, 0, motion.rowW, List.of(ContainerMotion.Style.values()), ContainerMotion.Style.parse(cfg.containerOpen), (s) -> Component.translatable("cfhgui.container_open." + s.key()), (s) -> {
         this.save((x) -> x.containerOpen = s.name());
         openMs.active = s != ContainerMotion.Style.OFF;
      })).tip(Component.translatable("cfhgui.settings.container_open.tip")));
      motion.row(openMs);
      motion.row(new CFHGuiToggle(0, 0, motion.rowW, Component.translatable("cfhgui.settings.ui_sounds"), cfg.uiSounds, (v) -> this.save((x) -> x.uiSounds = v)));
      motion.row(new CFHGuiToggle(0, 0, motion.rowW - 90, Component.translatable("cfhgui.settings.toasts"), cfg.toasts, (v) -> this.save((x) -> x.toasts = v)));
      CFHGuiButton test = (new CFHGuiButton(0, 0, 84, 16, Component.translatable("cfhgui.settings.test_toast"), () -> CFHGuiToasts.show(Component.translatable("cfhgui.toast.test.title"), Component.translatable("cfhgui.toast.test.body"), Icon.BELL))).variant(CFHGuiButton.Variant.GHOST).icon(Icon.BELL);
      motion.card.add(test, 10 + motion.rowW - 84, motion.flow.y() - 6 - 18);
      this.panel.add(motion.finish(), 0, y);
      y += motion.card.getHeight() + 8;
      Section inventory = new Section(w, Component.translatable("cfhgui.settings.section.inventory"));
      inventory.row((new CFHGuiToggle(0, 0, inventory.rowW, Component.translatable("cfhgui.settings.quick_actions"), cfg.quickActions, (v) -> this.save((x) -> x.quickActions = v))).tip(Component.translatable("cfhgui.settings.quick_actions.tip")));
      this.panel.add(inventory.finish(), 0, y);
      y += inventory.card.getHeight() + 8;
      Section restyle = new Section(w, Component.translatable("cfhgui.settings.section.restyle"));
      restyle.row((new CFHGuiLabel(0, 0, restyle.rowW, Component.translatable("cfhgui.settings.restyle.hint"))).style(CFHGuiLabel.Style.MUTED).wrap(true));
      restyle.row((new CFHGuiDropdown<String>(0, 0, restyle.rowW, List.of("VANILLA_AND_CFH", "ALLOWLIST", "ALL_NON_CONTAINER", "NONE"), cfg.reskinScope, (s) -> Component.translatable("cfhgui.reskin." + s.toLowerCase(Locale.ROOT)), (s) -> {
         this.save((x) -> x.reskinScope = s);
         Reskin.invalidate();
      })).label(Component.translatable("cfhgui.settings.reskin_scope")));
      this.panel.add(restyle.finish(), 0, y);
      y += restyle.card.getHeight() + 8;

      Section devSec = new Section(w, Component.translatable("cfhgui.settings.section.dev"));
      devSec.row((new CFHGuiToggle(0, 0, devSec.rowW, Component.translatable("cfhgui.settings.dev_mode"), cfg.devMode, (v) -> this.save((x) -> x.devMode = v))).tip(Component.translatable("cfhgui.settings.dev_mode.tip")));
      devSec.row(new CFHGuiToggle(0, 0, devSec.rowW, Component.translatable("cfhgui.settings.dev_grid"), cfg.devGrid, (v) -> this.save((x) -> x.devGrid = v)));
      devSec.row((new CFHGuiSlider(0, 0, devSec.rowW, Component.translatable("cfhgui.settings.dev_snap"), (double)1.0F, (double)16.0F, (double)1.0F, (double)cfg.devSnap, (v) -> Integer.toString((int)v) + " px", (v) -> this.save((x) -> x.devSnap = (int)v))).compact(true));
      devSec.row((new CFHGuiDropdown<DevModeButton.Place>(0, 0, devSec.rowW, List.of(DevModeButton.Place.values()), DevModeButton.place(), DevModeButton.Place::title, (p) -> this.save((x) -> x.devPencil = p.name()))).label(Component.translatable("cfhgui.settings.dev_pencil")).tip(Component.translatable("cfhgui.settings.dev_pencil.tip")));
      devSec.flow.skip(2);
      int bw = (devSec.rowW - 6) / 2;
      CFHGuiButton folder = (new CFHGuiButton(0, 0, bw, Component.translatable("cfhgui.settings.open_config_folder"), () -> Util.getPlatform().openPath(JsonConfig.dir()))).icon(Icon.FOLDER);
      devSec.row(folder);
      devSec.card.add((new CFHGuiButton(0, 0, bw, Component.translatable("cfhgui.settings.reset"), this::confirmReset)).variant(CFHGuiButton.Variant.DANGER).icon(Icon.UNDO), 10 + bw + 6, folder.getY());
      this.panel.add(devSec.finish(), 0, y);
      y += devSec.card.getHeight();
      this.panel.setContentHeight(y);
   }

   private void switchRow(Section s, String key, boolean value, Consumer<Boolean> onChange) {
      s.row(new CFHGuiToggle(0, 0, s.rowW, Component.translatable("cfhgui.setup." + key), value, onChange));
      s.row((new CFHGuiLabel(0, 0, s.rowW, Component.translatable("cfhgui.setup." + key + ".desc"))).style(CFHGuiLabel.Style.MUTED).wrap(true));
      s.flow.skip(4);
   }

   private void confirmReset() {
      CFHGuiModal.confirmDanger(Component.translatable("cfhgui.settings.reset.title"), Component.translatable("cfhgui.settings.reset.body"), Component.translatable("cfhgui.settings.reset"), () -> {
         CFHGui.configFile().reset();
         Theme.reload();
         Reskin.invalidate();
         this.rebuildWidgets();
      });
   }

   private void save(Consumer<CoreConfig> edit) {
      CFHGui.configFile().update(edit);
      Theme.reload();
   }

   private final class Section {
      final CFHGuiCard card;
      final Flow flow;
      final int rowW;

      Section(final int width, final Component title) {
         this.card = (new CFHGuiCard(0, 0, width, 40)).flat();
         this.rowW = width - 20;
         this.flow = Flow.column(10, 10, 6);
         if (title != null) {
            this.row((new CFHGuiLabel(0, 0, this.rowW, title)).style(CFHGuiLabel.Style.TITLE));
            this.flow.skip(2);
         }

      }

      <T extends AbstractWidget> T row(T w) {
         this.flow.place(w);
         this.card.add(w, w.getX(), w.getY());
         return w;
      }

      CFHGuiCard finish() {
         this.card.setHeight(this.flow.maxY() + 10);
         return this.card;
      }
   }
}
