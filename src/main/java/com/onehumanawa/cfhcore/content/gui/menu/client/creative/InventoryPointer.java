package com.onehumanawa.cfhcore.content.gui.menu.client.creative;

import com.onehumanawa.cfhcore.content.gui.menu.mixin.MouseHandlerPosAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class InventoryPointer {
   private static final long WINDOW_NS = 600000000L;
   private static final String BETTER_INVENTORY = "dev.fallingcloud.betterinventory.";
   private static double x;
   private static double y;
   private static long closedAt;

   public static void leaving(@Nullable Screen current, @Nullable Screen next) {
      if (next == null && inventory(current)) {
         Minecraft mc = Minecraft.getInstance();
         x = mc.mouseHandler.xpos();
         y = mc.mouseHandler.ypos();
         closedAt = System.nanoTime();
      }
   }

   public static void opened() {
      if (closedAt != 0L) {
         Minecraft mc = Minecraft.getInstance();
         Screen now = mc.screen;
         if (now != null) {
            long since = System.nanoTime() - closedAt;
            closedAt = 0L;
            if (since <= 600000000L && inventory(now)) {
               GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), x, y);
               MouseHandler var5 = mc.mouseHandler;
               if (var5 instanceof MouseHandlerPosAccessor) {
                  MouseHandlerPosAccessor pos = (MouseHandlerPosAccessor)var5;
                  pos.cfhgui$setXpos(x);
                  pos.cfhgui$setYpos(y);
               }

            }
         }
      }
   }

   private static boolean inventory(@Nullable Screen s) {
      if (s == null) {
         return false;
      } else if (!(s instanceof InventoryScreen) && !(s instanceof CreativeModeInventoryScreen)) {
         String name = s.getClass().getName();
         return name.startsWith("dev.fallingcloud.betterinventory.") && name.endsWith("BetterInventoryScreen");
      } else {
         return true;
      }
   }

   private InventoryPointer() {
   }
}
