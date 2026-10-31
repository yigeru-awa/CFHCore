package com.onehumanawa.cfhcore.content.gui.core.layout.editor;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.DevModeButton;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class LayoutEditor {
   private static @Nullable EditorOverlay overlay;

   public static boolean isEditing() {
      return overlay != null;
   }

   public static boolean canEdit(@Nullable Screen screen) {
      return screen != null && CFHGui.config().devMode && !ScreenIds.isContainer(screen);
   }

   public static boolean holds(String layoutId) {
      EditorOverlay o = overlay;
      return o != null && o.session().layoutId.equals(layoutId) || EditorSession.parked(layoutId);
   }

   public static void toggle() {
      Screen s = Minecraft.getInstance().screen;
      if (overlay != null) {
         stop();
      } else if (canEdit(s)) {
         start(s);
      }

   }

   public static void start(Screen screen) {
      if (canEdit(screen)) {
         overlay = new EditorOverlay(screen);
         DevModeButton.editorToggled(screen, true);
         CFHGui.LOGGER.info("[CFHGui] editing layout of {}", ScreenIds.of(screen));
      }
   }

   public static void stop() {
      EditorOverlay o = overlay;
      if (o != null) {
         o.close();
      }

      overlay = null;
      if (o != null && Minecraft.getInstance().screen == o.screen()) {
         DevModeButton.editorToggled(o.screen(), false);
      }

   }

   public static void onScreenChanged(@Nullable Screen screen) {
      if (overlay != null && overlay.screen() != screen) {
         stop();
      }

   }

   public static void onScreenInit(Screen screen) {
      if (overlay != null && overlay.screen() == screen && !overlay.session().refreshing()) {
         overlay.onInit();
      }

   }

   public static @Nullable EditorOverlay overlay() {
      return overlay;
   }

   public static void render(GuiGraphics g, Screen screen, int mouseX, int mouseY, float partialTick) {
      if (overlay != null && overlay.screen() == screen) {
         overlay.render(g, mouseX, mouseY, partialTick);
      }

   }

   private LayoutEditor() {
   }
}
