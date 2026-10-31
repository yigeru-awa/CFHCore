package com.onehumanawa.cfhcore.content.gui.core.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class KeyBinds {
   private static @Nullable Modifiers modifiers;

   private KeyBinds() {
   }

   public static void install(Modifiers impl) {
      modifiers = impl;
   }

   public static boolean supported() {
      return modifiers != null;
   }

   public static String modifier(KeyMapping mapping) {
      return modifiers == null ? "" : modifiers.of(mapping);
   }

   public static String defaultModifier(KeyMapping mapping) {
      return modifiers == null ? "" : modifiers.defaultOf(mapping);
   }

   public static void bind(KeyMapping mapping, InputConstants.Key key, String modifier) {
      if (modifiers != null) {
         modifiers.bind(mapping, key, modifier);
      } else {
         mapping.setKey(key);
      }

   }

   public static void reset(KeyMapping mapping) {
      bind(mapping, mapping.getDefaultKey(), defaultModifier(mapping));
   }

   public static String modifierOf(int keyCode) {
      String var10000;
      switch (keyCode) {
         case 340:
         case 344:
            var10000 = "shift";
            break;
         case 341:
         case 345:
            var10000 = "ctrl";
            break;
         case 342:
         case 346:
            var10000 = "alt";
            break;
         case 343:
         default:
            var10000 = "";
      }

      return var10000;
   }

   public static String held() {
      return Screen.hasControlDown() ? "ctrl" : (Screen.hasShiftDown() ? "shift" : (Screen.hasAltDown() ? "alt" : ""));
   }

   public static String save(KeyMapping mapping) {
      String modifier = modifier(mapping);
      return modifier.isEmpty() ? mapping.saveString() : mapping.saveString() + ":" + modifier;
   }

   public static String saveDefault(KeyMapping mapping) {
      String modifier = defaultModifier(mapping);
      return modifier.isEmpty() ? mapping.getDefaultKey().getName() : mapping.getDefaultKey().getName() + ":" + modifier;
   }

   public static InputConstants.Key key(@Nullable String saved) {
      if (saved != null && !saved.isEmpty()) {
         int colon = saved.indexOf(58);

         try {
            return InputConstants.getKey(colon < 0 ? saved : saved.substring(0, colon));
         } catch (RuntimeException var3) {
            return InputConstants.UNKNOWN;
         }
      } else {
         return InputConstants.UNKNOWN;
      }
   }

   public static String modifier(@Nullable String saved) {
      if (saved == null) {
         return "";
      } else {
         int colon = saved.indexOf(58);
         String name = colon < 0 ? "" : saved.substring(colon + 1).trim().toLowerCase(Locale.ROOT);
         return !name.equals("shift") && !name.equals("ctrl") && !name.equals("alt") ? (name.equals("control") ? "ctrl" : "") : name;
      }
   }

   public interface Modifiers {
      String of(KeyMapping var1);

      String defaultOf(KeyMapping var1);

      void bind(KeyMapping var1, InputConstants.Key var2, String var3);
   }
}
