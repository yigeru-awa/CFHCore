package com.onehumanawa.cfhcore.content.gui.config.option;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.onehumanawa.cfhcore.content.gui.config.ConfigPlatform;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

public final class KeySearch {
   public static boolean matches(KeyMapping m, String query, String extraText) {
      String q = normalize(query);
      if (q.isEmpty()) {
         return true;
      } else if (keyNames(m).contains(q)) {
         return true;
      } else if (q.codePointCount(0, q.length()) == 1) {
         return false;
      } else {
         String hay = (extraText + " " + text(m)).toLowerCase(Locale.ROOT);

         for(String tok : q.split(" ")) {
            if (!tok.isEmpty() && !hay.contains(tok)) {
               return false;
            }
         }

         return true;
      }
   }

   public static String text(KeyMapping m) {
      String var10000 = Component.translatable(m.getName()).getString();
      return (var10000 + " " + Component.translatable(m.getCategory()).getString() + " " + String.join(" ", keyNames(m))).toLowerCase(Locale.ROOT);
   }

   static String normalize(String s) {
      return s == null ? "" : s.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ").replaceAll(" ?\\+ ?", "+");
   }

   static Set<String> keyNames(KeyMapping m) {
      Set<String> out = new LinkedHashSet();
      if (m.isUnbound()) {
         return out;
      } else {
         InputConstants.Key key = KeyBinding.parse(m.saveString());
         out.add(normalize(key.getDisplayName().getString()));
         aliases(key, out);
         String modifier = ConfigPlatform.get().keyModifier(m);
         if (!modifier.isEmpty()) {
            List<String> plain = List.copyOf(out);

            for(String mod : "ctrl".equals(modifier) ? List.of("ctrl", "control", "cmd") : List.of(modifier)) {
               out.add(mod);

               for(String k : plain) {
                  out.add(mod + "+" + k);
               }
            }
         }

         return out;
      }
   }

   private static void aliases(InputConstants.Key key, Set<String> out) {
      int v = key.getValue();
      if (key.getType() == Type.MOUSE) {
         int n = v + 1;
         add(out, "mouse", "mouse " + n, "mouse" + n, "mb" + n, "m" + n, "button " + n);
         switch (v) {
            case 0 -> add(out, "lmb", "left click");
            case 1 -> add(out, "rmb", "right click");
            case 2 -> add(out, "mmb", "middle click");
         }

      } else if (key.getType() == Type.KEYSYM) {
         switch (v) {
            case 32:
               add(out, "space", "spacebar");
               break;
            case 96:
               add(out, "grave", "tilde");
               break;
            case 256:
               add(out, "esc", "escape");
               break;
            case 257:
            case 335:
               add(out, "enter", "return");
               break;
            case 259:
               add(out, "backspace");
               break;
            case 260:
               add(out, "ins", "insert");
               break;
            case 261:
               add(out, "del", "delete");
               break;
            case 262:
               add(out, "right", "right arrow");
               break;
            case 263:
               add(out, "left", "left arrow");
               break;
            case 264:
               add(out, "down", "down arrow");
               break;
            case 265:
               add(out, "up", "up arrow");
               break;
            case 266:
               add(out, "pgup", "page up");
               break;
            case 267:
               add(out, "pgdn", "page down");
               break;
            case 280:
               add(out, "caps", "caps lock", "capslock");
               break;
            case 340:
               add(out, "shift", "lshift");
               break;
            case 341:
               add(out, "ctrl", "control", "lctrl");
               break;
            case 342:
               add(out, "alt", "lalt");
               break;
            case 343:
            case 347:
               add(out, "win", "windows", "super", "cmd", "meta");
               break;
            case 344:
               add(out, "shift", "rshift");
               break;
            case 345:
               add(out, "ctrl", "control", "rctrl");
               break;
            case 346:
               add(out, "alt", "ralt", "alt gr");
               break;
            default:
               if (v >= 320 && v <= 329) {
                  int d = v - 320;
                  add(out, "numpad " + d, "numpad" + d, "num" + d);
               }
         }

      }
   }

   private static void add(Set<String> out, String... names) {
      for(String n : names) {
         out.add(n);
      }

   }

   private KeySearch() {
   }
}
