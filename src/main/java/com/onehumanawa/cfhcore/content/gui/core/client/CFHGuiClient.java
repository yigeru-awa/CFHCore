package com.onehumanawa.cfhcore.content.gui.core.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.quick.QuickActions;
import com.onehumanawa.cfhcore.content.gui.core.client.setup.CFHGuiSetupScreen;
import com.onehumanawa.cfhcore.content.gui.core.compat.maps.MapsCompat;
import com.onehumanawa.cfhcore.content.gui.core.compat.xaero.XaeroStyle;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiKeys;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutApplier;
import com.onehumanawa.cfhcore.content.gui.core.layout.editor.LayoutEditor;
import com.onehumanawa.cfhcore.content.gui.core.screen.ContainerMotion;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.core.screen.Transitions;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.BetterInventoryPalette;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.EarlyWindowProps;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageBootstrap;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import java.util.function.Function;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class CFHGuiClient {
   public static final KeyMapping EDITOR_KEY;
   public static final KeyMapping HUB_KEY;
   private static boolean initialised;
   private static boolean propsWritten;
   private static @Nullable Screen lastScreen;

   public static synchronized void init() {
      if (!initialised) {
         initialised = true;
         CFHGui.init();
         Theme.setActiveStyleResolver(() -> MenuSlots.styleFor(Minecraft.getInstance().screen));
         Theme.onChange(EarlyWindowProps::write);
         MenuSlots.onChange(EarlyWindowProps::write);
         Theme.reload();
         CFHGuiKeys.register(EDITOR_KEY);
         CFHGuiKeys.register(HUB_KEY);
         CoreActions.registerAll();
         CoreElements.registerAll();
         ScreenIds.register(CFHGuiHubScreen.class, "cfhgui:hub", "CFHGui hub");
         ScreenIds.register(CoreSettingsScreen.class, "cfhgui:settings", "CFHGui settings");
         ScreenIds.register(CFHGuiSetupScreen.class, "cfhgui:setup", "CFHGui setup");
         ScreenIds.register(FeatureTestScreen.class, "cfhgui:feature_test", "Feature gates (harness)");
         CoreActions.SCREEN_FACTORIES.put("cfhgui:feature_test", FeatureTestScreen::new);
         CoreActions.SCREEN_FACTORIES.put("cfhgui:setup", (p) -> new CFHGuiSetupScreen(p));
         VanillaScreenButtons.init();
         QuickActions.init();
         DevModeButton.init();
         CFHGuiCursor.init();
         CFHGuiEvents.CLIENT_TICK_END.register(CFHGuiClient::tick);
         CFHGuiEvents.KEY_PRESSED.register((CFHGuiEvents.KeyPress)(key, scan, mods) -> {
            if (HUB_KEY.matches(key, scan) && !HUB_KEY.isUnbound()) {
               Minecraft.getInstance().setScreen(new CFHGuiHubScreen((Screen)null));
               return true;
            } else {
               return false;
            }
         });
         DevHarness.init();
         XaeroStyle.init();
         MapsCompat.init();
         BetterInventoryPalette.init();
         StageBootstrap.init();
         CFHGui.LOGGER.info("[CFHGui] client ready ({} skin)", Theme.current().skin());
      }
   }

   private static void tick() {
      if (!propsWritten) {
         propsWritten = true;
         EarlyWindowProps.write();
      }

      StartupTime.tick(Minecraft.getInstance());
      BetterInventoryPalette.tick();

      while(EDITOR_KEY.consumeClick()) {
         if (Minecraft.getInstance().screen == null && CFHGui.config().devMode) {
            LayoutEditor.toggle();
         }
      }

   }

   public static void beforeScreenShown(@Nullable Screen screen) {
      Theme.activate(screen == null ? null : MenuSlots.styleFor(screen));
   }

   public static void onScreenChanged(@Nullable Screen screen) {
      StartupTime.screenSet(screen);
      Popups.onScreenChanged(screen);
      LayoutEditor.onScreenChanged(screen);
      Transitions.onScreenChanged(lastScreen, screen);
      ContainerMotion.onScreenChanged(lastScreen, screen);
      lastScreen = screen;
      CFHGuiEvents.SCREEN_OPENED.invoke((l) -> l.accept(screen));
   }

   public static void onScreenInit(Screen screen) {
      CFHGuiEvents.SCREEN_INIT_POST.invoke((l) -> l.accept(screen));
      LayoutApplier.apply(screen);
      LayoutEditor.onScreenInit(screen);
   }

   public static void onScreenRepositioned(Screen screen) {
      LayoutApplier.reposition(screen);
      LayoutEditor.onScreenInit(screen);
   }

   public static void onScreenRendered(Screen screen, GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Clock.onFrame();
      CFHGuiEvents.SCREEN_RENDER_POST.invoke((l) -> l.render(screen, g, mouseX, mouseY, partialTick));
      LayoutEditor.render(g, screen, mouseX, mouseY, partialTick);
      Popups.render(g, mouseX, mouseY, partialTick, screen.width, screen.height);
      CFHGuiToasts.render(g, mouseX, mouseY, screen.width);
      CFHGuiTooltips.render(g, mouseX, mouseY, screen.width, screen.height);
      Transitions.render(g, screen, screen.width, screen.height);
      DevHarness.screenFrame();
   }

   public static void onHudRendered(GuiGraphics g, float partialTick) {
      if (Minecraft.getInstance().screen == null) {
         Clock.onFrame();
      }

      CFHGuiEvents.HUD_RENDER.invoke((l) -> l.render(g, partialTick));
      if (Minecraft.getInstance().screen == null) {
         CFHGuiToasts.render(g, -1, -1, g.guiWidth());
      }

   }

   private CFHGuiClient() {
   }

   static {
      EDITOR_KEY = new KeyMapping("key.cfhgui.editor", Type.KEYSYM, 296, "key.categories.cfhgui");
      HUB_KEY = new KeyMapping("key.cfhgui.hub", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.cfhgui");
   }
}
