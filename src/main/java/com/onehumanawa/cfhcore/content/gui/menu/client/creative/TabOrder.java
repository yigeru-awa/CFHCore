package com.onehumanawa.cfhcore.content.gui.menu.client.creative;

import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.Type;
import org.jetbrains.annotations.Nullable;

public final class TabOrder {
   private TabOrder() {
   }

   static List<String> stored() {
      List<String> order = CFHGuiMenu.config().creativeTabOrder;
      return order == null ? List.of() : order;
   }

   public static boolean custom() {
      return !stored().isEmpty();
   }

   public static void reset() {
      if (custom()) {
         CFHGuiMenu.configFile().update((c) -> c.creativeTabOrder = new ArrayList());
      }

   }

   static String id(CreativeModeTab tab) {
      ResourceLocation key = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
      return key == null ? "" : key.toString();
   }

   static boolean movable(CreativeModeTab tab) {
      return tab.getType() == Type.CATEGORY && !id(tab).isEmpty();
   }

   static List<CreativeModeTab> apply(List<CreativeModeTab> tabs, List<String> order) {
      if (order.isEmpty()) {
         return tabs;
      } else {
         List<CreativeModeTab> out = new ArrayList(tabs.size());

         for(String id : order) {
            for(CreativeModeTab tab : tabs) {
               if (id.equals(id(tab)) && !out.contains(tab)) {
                  out.add(tab);
                  break;
               }
            }
         }

         for(CreativeModeTab tab : tabs) {
            if (!out.contains(tab)) {
               out.add(tab);
            }
         }

         return out;
      }
   }

   static List<String> moved(List<CreativeModeTab> shown, CreativeModeTab moved, @Nullable CreativeModeTab before) {
      String id = id(moved);
      List<String> order = new ArrayList(stored());

      for(CreativeModeTab tab : shown) {
         String known = id(tab);
         if (!known.isEmpty() && !order.contains(known)) {
            order.add(known);
         }
      }

      Objects.requireNonNull(id);
      order.removeIf(id::equals);
      int at = before == null ? -1 : order.indexOf(id(before));
      if (at < 0) {
         at = 0;

         for(CreativeModeTab tab : shown) {
            if (tab != moved) {
               at = Math.max(at, order.indexOf(id(tab)) + 1);
            }
         }
      }

      order.add(at, id);
      return order;
   }

   static void store(List<String> order) {
      CFHGuiMenu.configFile().update((c) -> c.creativeTabOrder = new ArrayList(order));
   }
}
