package com.onehumanawa.cfhcore.content.gui.core.client;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.layout.editor.EditorOverlay;
import com.onehumanawa.cfhcore.content.gui.core.layout.editor.LayoutEditor;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

public final class ScreenInput {
   private static int heldButton = -1;
   private static double lastX;
   private static double lastY;

   private static double guiX() {
      Minecraft mc = Minecraft.getInstance();
      return mc.mouseHandler.xpos() * (double)mc.getWindow().getGuiScaledWidth() / (double)mc.getWindow().getScreenWidth();
   }

   private static double guiY() {
      Minecraft mc = Minecraft.getInstance();
      return mc.mouseHandler.ypos() * (double)mc.getWindow().getGuiScaledHeight() / (double)mc.getWindow().getScreenHeight();
   }

   public static boolean mousePressed(int button) {
      Screen screen = Minecraft.getInstance().screen;
      if (screen == null) {
         return false;
      } else {
         double mx = guiX();
         double my = guiY();
         lastX = mx;
         lastY = my;
         heldButton = button;
         boolean taken = false;

         try {
            taken = Popups.mouseClicked(mx, my, button);
         } finally {
            if (!taken && heldButton == button) {
               heldButton = -1;
            }

         }

         if (taken) {
            return true;
         } else if (CFHGuiToasts.mouseClicked(mx, my, screen.width)) {
            return true;
         } else {
            EditorOverlay ed = LayoutEditor.overlay();
            if (ed != null && ed.screen() == screen && ed.mouseClicked(mx, my, button)) {
               heldButton = button;
               return true;
            } else {
               return false;
            }
         }
      }
   }

   public static boolean mouseReleased(int button) {
      Screen screen = Minecraft.getInstance().screen;
      double mx = guiX();
      double my = guiY();
      boolean wasHeld = heldButton == button;
      heldButton = -1;
      if (screen == null) {
         return false;
      } else {
         boolean consumed = Popups.mouseReleased(mx, my, button);
         EditorOverlay ed = LayoutEditor.overlay();
         if (ed != null && ed.screen() == screen && ed.mouseReleased(mx, my, button)) {
            consumed = true;
         }

         return consumed || wasHeld;
      }
   }

   public static void releasedElsewhere(int button) {
      if (heldButton == button) {
         heldButton = -1;
      }

   }

   public static void mouseMoved() {
      Screen screen = Minecraft.getInstance().screen;
      if (screen != null && heldButton >= 0) {
         double mx = guiX();
         double my = guiY();
         double dx = mx - lastX;
         double dy = my - lastY;
         lastX = mx;
         lastY = my;
         if (!Popups.mouseDragged(mx, my, heldButton, dx, dy)) {
            EditorOverlay ed = LayoutEditor.overlay();
            if (ed != null && ed.screen() == screen) {
               ed.mouseDragged(mx, my, heldButton, dx, dy);
            }

         }
      }
   }

   public static boolean mouseScrolled(double scrollX, double scrollY) {
      Screen screen = Minecraft.getInstance().screen;
      if (screen == null) {
         return false;
      } else {
         double mx = guiX();
         double my = guiY();
         if (Popups.mouseScrolled(mx, my, scrollX, scrollY)) {
            return true;
         } else {
            EditorOverlay ed = LayoutEditor.overlay();
            return ed != null && ed.screen() == screen && ed.mouseScrolled(mx, my, scrollX, scrollY);
         }
      }
   }

   public static boolean keyPressed(int key, int scancode, int modifiers) {
      Screen screen = Minecraft.getInstance().screen;
      if (screen == null) {
         return false;
      } else {
         label43: {
            if (CFHGuiClient.EDITOR_KEY.matches(key, scancode) && CFHGui.config().devMode) {
               GuiEventListener var5 = screen.getFocused();
               if (!(var5 instanceof EditBox)) {
                  break label43;
               }

               EditBox eb = (EditBox)var5;
               if (!eb.isFocused()) {
                  break label43;
               }
            }

            if (Popups.keyPressed(key, scancode, modifiers)) {
               return true;
            }

            EditorOverlay ed = LayoutEditor.overlay();
            return ed != null && ed.screen() == screen && ed.keyPressed(key, scancode, modifiers);
         }

         LayoutEditor.toggle();
         return true;
      }
   }

   public static boolean charTyped(char c, int modifiers) {
      Screen screen = Minecraft.getInstance().screen;
      if (screen == null) {
         return false;
      } else if (Popups.charTyped(c, modifiers)) {
         return true;
      } else {
         EditorOverlay ed = LayoutEditor.overlay();
         return ed != null && ed.screen() == screen && ed.charTyped(c, modifiers);
      }
   }

   private ScreenInput() {
   }
}
