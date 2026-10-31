package com.onehumanawa.cfhcore.content.gui.core.compat.maps.xaero;

import com.onehumanawa.cfhcore.content.gui.core.compat.maps.MapMenus;
import com.onehumanawa.cfhcore.content.gui.core.map.MapAction;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

final class XaeroOptions {
   private static final int DANGER = Palette.dark(-7726052).danger() & 16777215;
   private static final int FITS = 144;
   private static final String CUT = "…";

   static RightClickOption title(String name, int index, IRightClickableElement target) {
      final Component words = fit(Component.literal(name));
      return new RightClickOption(name, index, target) {
         public void onAction(Screen screen) {
         }

         public Component getDisplayName() {
            return words;
         }
      };
   }

   static RightClickOption action(final MapAction action, int index, IRightClickableElement target) {
      Component label = fit(action.label());
      final Component words = (Component)(!action.enabled() ? label.copy().withStyle(ChatFormatting.DARK_GRAY) : (action.danger() ? label.copy().withStyle((s) -> s.withColor(DANGER)) : label));
      return new RightClickOption(action.label().getString(), index, target) {
         public void onAction(Screen screen) {
            MapMenus.run(action);
         }

         public Component getDisplayName() {
            return words;
         }
      };
   }

   static RightClickOption places(int index, IRightClickableElement target) {
      final Component label = fit(MapMenus.placesLabel());
      return new RightClickOption(label.getString(), index, target) {
         public void onAction(Screen screen) {
            MapMenus.places();
         }

         public Component getDisplayName() {
            return label;
         }
      };
   }

   private static Component fit(Component label) {
      Font font = Minecraft.getInstance().font;
      return (Component)(font.width(label) <= 144 ? label : Component.literal(font.plainSubstrByWidth(label.getString(), 144 - font.width("…")) + "…"));
   }

   private XaeroOptions() {
   }
}
