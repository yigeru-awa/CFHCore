package com.onehumanawa.cfhcore.content.gui.core.client.quick;

import com.onehumanawa.cfhcore.content.gui.core.platform.Loader;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

final class ViewerEffects {
   private static final boolean VIEWER = CFHGuiPlatform.get().isModLoaded("emi");
   private static final boolean PAGE_BUTTONS;
   private static final boolean INVENTORY_TABS;
   private static @Nullable Field location;
   private static boolean looked;

   static int @Nullable [] overPanel(AbstractContainerScreen<?> screen, int left, int top, int panelWidth) {
      if (VIEWER && screen instanceof EffectRenderingInventoryScreen && onTop()) {
         LocalPlayer player = Minecraft.getInstance().player;
         int count = player == null ? 0 : player.getActiveEffects().size();
         if (count == 0) {
            return null;
         } else {
            boolean creative = screen instanceof CreativeModeInventoryScreen;
            int y = top - 34;
            if (creative || INVENTORY_TABS) {
               y -= 28;
            }

            if (creative && PAGE_BUTTONS) {
               y -= 22;
            }

            int step = count == 1 ? 122 : (count > 5 ? (panelWidth - 32) / (count - 1) : 34);
            int width = (count - 1) * step + (count == 1 ? 120 : 32);
            return new int[]{left + (panelWidth - width) / 2, y, width, 32};
         }
      } else {
         return null;
      }
   }

   private static boolean onTop() {
      if (!looked) {
         looked = true;

         try {
            location = Class.forName("dev.emi.emi.config.EmiConfig").getField("effectLocation");
         } catch (LinkageError | ReflectiveOperationException var2) {
            location = null;
         }
      }

      if (location == null) {
         return false;
      } else {
         try {
            Object value = location.get(null);
            boolean var10000;
            if (value instanceof Enum) {
               Enum<?> e = (Enum)value;
               if ("TOP".equals(e.name())) {
                  var10000 = true;
                  return var10000;
               }
            }

            var10000 = false;
            return var10000;
         } catch (RuntimeException | ReflectiveOperationException var3) {
            return false;
         }
      }
   }

   private ViewerEffects() {
   }

   static {
      PAGE_BUTTONS = CFHGuiPlatform.get().loader() != Loader.FABRIC;
      INVENTORY_TABS = CFHGuiPlatform.get().isModLoaded("inventorytabs");
   }
}
