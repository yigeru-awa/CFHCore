package com.onehumanawa.cfhcore.content.gui.config.page;

import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.resolver.CoreBindings;
import com.onehumanawa.cfhcore.content.gui.config.resolver.VanillaOptions;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionPageBase;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.client.setup.CFHGuiSetupScreen;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutStore;
import com.onehumanawa.cfhcore.content.gui.core.layout.editor.LayoutEditor;
import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlot;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public final class InterfacePage extends OptionPageBase {
   public InterfacePage() {
      super("interface", Component.translatable("cfhgui_config.page.interface"), Icon.PALETTE);
   }

   private static void editTitle() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         if (!CFHGui.config().devMode) {
            CFHGui.configFile().update((c) -> c.devMode = true);
         }

         mc.setScreen(new TitleScreen());
         if (mc.screen != null) {
            LayoutEditor.start(mc.screen);
         }

      }
   }

   protected List<Section> sections() {
      List<Section> out = new ArrayList();
      Section modes = Section.of("modes", Component.translatable("cfhgui_config.interface.modes"));
      modes.addAll(CoreBindings.all("reskinContainers", "quickActions"));
      modes.add(Binding.of("interface:run_setup", OptionType.ACTION, (Component)Component.translatable("cfhgui_config.interface.setup")).tooltip((Component)Component.translatable("cfhgui_config.interface.setup.tip")).actionIcon(Icon.SPARKLE).action(Component.translatable("cfhgui_config.row.open"), () -> {
         Minecraft mc = Minecraft.getInstance();
         mc.setScreen(new CFHGuiSetupScreen(mc.screen));
      }).searchWords("setup layout style containers vanilla custom"));
      modes.add(Binding.of("interface:open_hub", OptionType.ACTION, (Component)Component.translatable("cfhgui.hub.title")).tooltip((Component)Component.translatable("cfhgui_config.interface.hub.tip")).actionIcon(Icon.CFHGUI).action(Component.translatable("cfhgui_config.row.open"), () -> CoreActions.openScreen("cfhgui:hub")).searchWords("hub cfhgui modules"));
      out.add(modes);
      Section menus = Section.of("menus", Component.translatable("cfhgui.settings.menus"));

      for(MenuSlot slot : MenuSlots.all()) {
         if ("cfhgui:loading".equals(slot.id()) && CFHGuiPlatform.get().isModLoaded("create")) {
            menus.addAll(CoreBindings.all("startupScene"));
         }

         if ("cfhgui:loading".equals(slot.id())) {
            menus.addAll(CoreBindings.all("startupTimer"));
         }
      }

      out.add(menus);
      Section look = Section.of("look", Component.translatable("cfhgui_config.interface.look"));
      look.custom((x$0) -> new SwatchRow(x$0));
      look.addAll(CoreBindings.all("accent", "radius", "smoothCorners", "cursor", "headingFont", "pixelFont", "blurInGame", "translucentPanels", "menuBackground"));
      out.add(look);
      out.add(Section.of("motion", Component.translatable("cfhgui_config.interface.motion"), CoreBindings.all("motion", "transitions", "containerOpen", "containerOpenMs", "uiSounds", "toasts")));
      out.add(Section.of("restyle", Component.translatable("cfhgui_config.interface.restyle"), CoreBindings.all("reskinScope", "reskinAllowlist", "reskinDenylist")).advanced());
      out.add(Section.of("vanilla", Component.translatable("cfhgui_config.interface.vanilla"), VanillaOptions.all("guiScale", "darkMojangStudiosBackground", "hideSplashTexts", "panoramaScrollSpeed", "reducedDebugInfo")));
      if (Modules.isLoaded("cfhgui")) {
         Section menu = SimplePages.cfhguiModule("menu", "cfhgui", "cfhgui_config.interface.menu");
         menu.add(Binding.of("interface:edit_title", OptionType.ACTION, (Component)Component.translatable("cfhgui_config.interface.edit_title")).tooltip((Component)Component.translatable("cfhgui_config.interface.edit_title.tip")).actionIcon(Icon.EDIT).enabledIf(() -> Minecraft.getInstance().level == null).action(Component.translatable("cfhgui_config.row.open"), InterfacePage::editTitle).searchWords("main menu title screen buttons hide background image modpack layout editor"));
         menu.add(Binding.of("interface:open_layouts_folder", OptionType.ACTION, (Component)Component.translatable("cfhgui_config.interface.layouts_folder")).tooltip((Component)Component.translatable("cfhgui_config.interface.layouts_folder.tip")).actionIcon(Icon.FOLDER).action(Component.translatable("cfhgui_config.row.open"), () -> {
            Path dir = LayoutStore.dir();

            try {
               Files.createDirectories(dir);
            } catch (IOException var2) {
            }

            Util.getPlatform().openPath(dir);
         }).searchWords("layouts folder modpack main menu"));
         out.add(menu);
      }

      Section dev = Section.of("dev", Component.translatable("cfhgui_config.interface.dev"), CoreBindings.all("devMode", "devGrid", "devSnap", "devPencil")).advanced();
      dev.add(Binding.of("interface:open_config_folder", OptionType.ACTION, (Component)Component.translatable("cfhgui.settings.open_config_folder")).actionIcon(Icon.FOLDER).action(Component.translatable("cfhgui_config.row.open"), () -> Util.getPlatform().openPath(JsonConfig.dir())).searchWords("folder config cfhgui"));
      out.add(dev);
      return out;
   }

   final class SwatchRow extends CFHGuiWidget {
      private static final int SW = 16;
      private static final int GAP = 4;
      private int hovered = -1;

      SwatchRow(final int width) {
         super(0, 0, width, 22, Component.translatable("cfhgui_config.core.accent_presets"));
      }

      private int indexAt(double mx) {
         int i = (int)((mx - (double)this.getX() - (double)2.0F) / (double)20.0F);
         return i >= 0 && i < Palette.ACCENTS.size() && mx >= (double)(this.getX() + 2) ? i : -1;
      }

      private void choose(int i) {
         if (i >= 0 && i < Palette.ACCENTS.size()) {
            CoreBindings.get("accent").ifPresent((b) -> b.set(((Palette.AccentPreset)Palette.ACCENTS.get(i)).color()));
            CFHGuiSounds.tick();
            InterfacePage.this.refreshRows();
         }
      }

      public void onClick(double mouseX, double mouseY) {
         super.onClick(mouseX, mouseY);
         this.choose(this.indexAt(mouseX));
      }

      public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
         if (this.active && this.visible) {
            int cur = this.currentIndex();
            if (keyCode == 263) {
               this.choose(Math.max(0, cur - 1));
               return true;
            } else if (keyCode == 262) {
               this.choose(Math.min(Palette.ACCENTS.size() - 1, cur + 1));
               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }

      private int currentIndex() {
         int accent = Theme.current().accent();

         for(int i = 0; i < Palette.ACCENTS.size(); ++i) {
            if (((Palette.AccentPreset)Palette.ACCENTS.get(i)).color() == accent) {
               return i;
            }
         }

         return -1;
      }

      protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.draw(g, mouseX, mouseY, false);
      }

      protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.draw(g, mouseX, mouseY, true);
      }

      private void draw(GuiGraphics g, int mouseX, int mouseY, boolean vanilla) {
         Palette p = Theme.current().palette();
         float a = this.effectiveAlpha();
         int y = this.getY() + this.enterOffset() + 3;
         this.hovered = this.isHovered() ? this.indexAt((double)mouseX) : -1;
         int cur = this.currentIndex();

         for(int i = 0; i < Palette.ACCENTS.size(); ++i) {
            int sx = this.getX() + 2 + i * 20;
            if (sx + 16 > this.getX() + this.getWidth()) {
               break;
            }

            int c = ((Palette.AccentPreset)Palette.ACCENTS.get(i)).color();
            CFHGuiDraw.pixelRound(g, sx, y, 16, 16, Colors.scaleAlpha(c, a), vanilla ? 0 : 2);
            if (i == cur) {
               CFHGuiDraw.outline(g, sx - 1, y - 1, 18, 18, Colors.scaleAlpha(vanilla ? -1 : p.text(), a), vanilla ? 0 : 3);
            } else if (i == this.hovered) {
               CFHGuiDraw.outline(g, sx, y, 16, 16, Colors.scaleAlpha(-1, 0.6F * a), vanilla ? 0 : 2);
            }
         }

         if (this.hovered >= 0) {
            CFHGuiTooltips.request(Component.literal(((Palette.AccentPreset)Palette.ACCENTS.get(this.hovered)).name()), this);
         }

         CFHGuiDraw.focusRing(g, this.getX(), y, Math.min(this.getWidth(), Palette.ACCENTS.size() * 20), 16, this.focus() * a);
      }
   }
}
