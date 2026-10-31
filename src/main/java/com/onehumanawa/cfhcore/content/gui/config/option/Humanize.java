package com.onehumanawa.cfhcore.content.gui.config.option;

import java.util.Locale;

public final class Humanize {
   public static String key(String key) {
      if (key != null && !key.isEmpty()) {
         StringBuilder sb = new StringBuilder(key.length() + 8);
         char prev = 0;

         for(int i = 0; i < key.length(); ++i) {
            char c = key.charAt(i);
            if (c != '_' && c != '-' && c != '.') {
               if (!Character.isUpperCase(c) || i <= 0 || !Character.isLowerCase(prev) && !Character.isDigit(prev)) {
                  if (Character.isUpperCase(c) && i > 0 && i + 1 < key.length() && Character.isUpperCase(prev) && Character.isLowerCase(key.charAt(i + 1))) {
                     sb.append(' ').append(Character.toLowerCase(c));
                  } else {
                     sb.append(sb.length() == 0 ? Character.toUpperCase(c) : c);
                  }
               } else {
                  sb.append(' ').append(Character.toLowerCase(c));
               }
            } else if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
               sb.append(' ');
            }

            prev = c;
         }

         return sb.toString().trim();
      } else {
         return "";
      }
   }

   public static String enumName(String name) {
      if (name != null && !name.isEmpty()) {
         if (name.chars().anyMatch(Character::isLowerCase) && !name.contains("_")) {
            return key(name);
         } else {
            String[] parts = name.toLowerCase(Locale.ROOT).split("[_\\s]+");
            StringBuilder sb = new StringBuilder();

            for(String p : parts) {
               if (!p.isEmpty()) {
                  if (sb.length() > 0) {
                     sb.append(' ');
                  }

                  sb.append(sb.length() == 0 ? Character.toUpperCase(p.charAt(0)) + p.substring(1) : p);
               }
            }

            return sb.toString();
         }
      } else {
         return "";
      }
   }

   public static String fileName(String file) {
      String s = file;
      int dot = file.lastIndexOf(46);
      if (dot > 0) {
         s = file.substring(0, dot);
      }

      return key(s);
   }

   private Humanize() {
   }
}
