package com.onehumanawa.cfhcore.content.gui.core.theme;

public enum Skin {
   DARK,
   VANILLA;

   public static Skin parse(String s) {
      return "VANILLA".equalsIgnoreCase(s) ? VANILLA : DARK;
   }
}
