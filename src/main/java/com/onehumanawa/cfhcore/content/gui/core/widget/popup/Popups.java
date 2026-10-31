package com.onehumanawa.cfhcore.content.gui.core.widget.popup;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class Popups {
   private static final Deque<Popup> STACK = new ArrayDeque();
   private static final List<Popup> CLOSING = new ArrayList();
   private static final Anim dim;
   private static @Nullable Screen owner;

   public static void open(Popup popup) {
      STACK.push(popup);
      owner = Minecraft.getInstance().screen;
      if (popup.isModal()) {
         dim.set(1.0F);
      }

   }

   public static void close(Popup popup) {
      if (STACK.remove(popup)) {
         popup.onClose();
         if (popup.beginClose()) {
            CLOSING.add(popup);
         }
      }

      if (STACK.stream().noneMatch(Popup::isModal)) {
         dim.set(0.0F);
      }

   }

   public static void closeTop() {
      Popup p = (Popup)STACK.peek();
      if (p != null) {
         close(p);
      }

   }

   public static void closeAll() {
      while(!STACK.isEmpty()) {
         close((Popup)STACK.peek());
      }

      CLOSING.clear();
      dim.snap(0.0F);
   }

   public static @Nullable Popup top() {
      return (Popup)STACK.peek();
   }

   public static boolean any() {
      return !STACK.isEmpty();
   }

   public static boolean isOpen(Popup popup) {
      return STACK.contains(popup);
   }

   public static void onScreenChanged(@Nullable Screen screen) {
      if (screen != owner) {
         closeAll();
      }

      owner = screen;
   }

   public static void render(GuiGraphics g, int mouseX, int mouseY, float partialTick, int w, int h) {
      float d = dim.get();
      CLOSING.removeIf(Popup::closeFinished);
      boolean any = !STACK.isEmpty() || !CLOSING.isEmpty();
      if (!(d <= 0.01F) || any) {
         newLayer(g);
         if (d > 0.01F) {
            g.fill(0, 0, w, h, Colors.scaleAlpha(Theme.current().palette().overlay(), d));
         }

         if (any) {
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 300.0F);

            for(Popup p : CLOSING) {
               p.render(g, -1, -1, partialTick);
            }

            Popup[] arr = (Popup[])STACK.toArray(new Popup[0]);

            for(int i = arr.length - 1; i >= 0; --i) {
               if (i < arr.length - 1 || !CLOSING.isEmpty()) {
                  newLayer(g);
               }

               arr[i].render(g, mouseX, mouseY, partialTick);
            }

            g.pose().popPose();
            newLayer(g);
         }
      }
   }

   private static void newLayer(GuiGraphics g) {
      g.flush();
      RenderSystem.clear(256, Minecraft.ON_OSX);
   }

   public static boolean mouseClicked(double mx, double my, int button) {
      Popup p = (Popup)STACK.peek();
      if (p == null) {
         return false;
      } else if (p.contains(mx, my)) {
         p.mouseClicked(mx, my, button);
         return true;
      } else if (p.isModal()) {
         return true;
      } else {
         close(p);
         return true;
      }
   }

   public static boolean mouseReleased(double mx, double my, int button) {
      Popup p = (Popup)STACK.peek();
      return p != null && (p.mouseReleased(mx, my, button) || p.isModal());
   }

   public static boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
      Popup p = (Popup)STACK.peek();
      return p != null && (p.mouseDragged(mx, my, button, dx, dy) || p.isModal());
   }

   public static boolean mouseScrolled(double mx, double my, double sx, double sy) {
      Popup p = (Popup)STACK.peek();
      if (p == null) {
         return false;
      } else if (!p.contains(mx, my) && !p.isModal()) {
         return false;
      } else {
         p.mouseScrolled(mx, my, sx, sy);
         return true;
      }
   }

   public static boolean keyPressed(int key, int scan, int mods) {
      Popup p = (Popup)STACK.peek();
      if (p == null) {
         return false;
      } else if (p.keyPressed(key, scan, mods)) {
         return true;
      } else if (key == 256) {
         close(p);
         return true;
      } else {
         return p.isModal();
      }
   }

   public static boolean charTyped(char c, int mods) {
      Popup p = (Popup)STACK.peek();
      return p != null && (p.charTyped(c, mods) || p.isModal());
   }

   private Popups() {
   }

   static {
      dim = new Anim(0.0F, 160, Ease.OUT_CUBIC);
   }
}
