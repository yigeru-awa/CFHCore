package com.onehumanawa.cfhcore.content.gui.core.screen.reskin;

import com.onehumanawa.cfhcore.content.gui.core.mixin.ContainerScreenAccessor;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

public final class ContainerReskin {
   private static final String CREATIVE_TAB = "container/creative_inventory/tab_";
   private static final String CREATIVE_SCROLLER = "container/creative_inventory/scroller";
   private static boolean inBackground;
   private static boolean backgroundDone;
   private static boolean inLabels;
   private static @Nullable AbstractContainerScreen<?> current;

   private ContainerReskin() {
   }

   public static void beginBackground() {
      inBackground = Reskin.containers() && Minecraft.getInstance().screen instanceof AbstractContainerScreen && !BetterInventoryPalette.paints(Minecraft.getInstance().screen);
      current = inBackground ? (AbstractContainerScreen)Minecraft.getInstance().screen : null;
      backgroundDone = false;
   }

   public static void endBackground(GuiGraphics g) {
      if (inBackground && !backgroundDone && current != null) {
         ContainerScreenAccessor acc = (ContainerScreenAccessor)current;
         ReskinDraw.containerTint(g, acc.cfhgui$leftPos(), acc.cfhgui$topPos(), acc.cfhgui$imageWidth(), acc.cfhgui$imageHeight());
         drawSlots(g, current, acc.cfhgui$leftPos(), acc.cfhgui$topPos());
      }

      inBackground = false;
      current = null;
   }

   public static void ownBackground() {
      backgroundDone = true;
   }

   public static void beginLabels() {
      inLabels = Reskin.containers();
   }

   public static void endLabels() {
      inLabels = false;
   }

   public static boolean replaceBackground(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h) {
      if (inBackground && current != null) {
         if (!texture.getPath().startsWith("textures/gui/")) {
            return false;
         } else {
            ContainerScreenAccessor acc = (ContainerScreenAccessor)current;
            int left = acc.cfhgui$leftPos();
            int top = acc.cfhgui$topPos();
            int iw = acc.cfhgui$imageWidth();
            int ih = acc.cfhgui$imageHeight();
            if (w == iw && x == left && y >= top && y < top + ih) {
               if (!backgroundDone) {
                  backgroundDone = true;
                  ReskinDraw.containerPanel(g, left, top, iw, ih);
                  drawSlots(g, current, left, top);
                  if (current instanceof InventoryScreen) {
                     ReskinDraw.containerInset(g, left + 25, top + 7, 51, 72);
                     ReskinDraw.containerArrow(g, left + 133, top + 30);
                  } else if (current instanceof CraftingScreen) {
                     ReskinDraw.containerArrow(g, left + 90, top + 35);
                  } else if (current instanceof AbstractFurnaceScreen) {
                     ReskinDraw.containerArrow(g, left + 79, top + 35);
                  } else if (current instanceof CreativeModeInventoryScreen) {
                     ReskinDraw.containerInset(g, left + 174, top + 17, 16, 114);
                  }
               }

               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private static void drawSlots(GuiGraphics g, AbstractContainerScreen<?> screen, int left, int top) {
      for(Slot slot : screen.getMenu().slots) {
         if (slot.isActive()) {
            ReskinDraw.containerSlot(g, left + slot.x - 1, top + slot.y - 1);
         }
      }

   }

   public static boolean replaceSprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h) {
      if (inBackground && current instanceof CreativeModeInventoryScreen) {
         String path = sprite.getPath();
         if (path.startsWith("container/creative_inventory/tab_")) {
            ReskinDraw.containerTab(g, x, y, w, h, path.contains("_selected"));
            return true;
         } else if (path.startsWith("container/creative_inventory/scroller")) {
            ReskinDraw.containerThumb(g, x, y, w, h, !path.endsWith("disabled"));
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static int labelColor(int color) {
      if (!inLabels) {
         return color;
      } else {
         int rgb = color & 16777215;
         int max = Math.max(rgb >> 16 & 255, Math.max(rgb >> 8 & 255, rgb & 255));
         if (max > 112) {
            return color;
         } else {
            int alpha = (color & -67108864) == 0 ? -16777216 : color & -16777216;
            return alpha | Theme.current().containerPalette().text() & 16777215;
         }
      }
   }
}
