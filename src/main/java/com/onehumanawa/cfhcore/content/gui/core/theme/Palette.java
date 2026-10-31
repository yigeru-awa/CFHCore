package com.onehumanawa.cfhcore.content.gui.core.theme;

import java.util.List;

public record Palette(int bg, int bg2, int surface, int surfaceHover, int surfaceActive, int border, int borderStrong, int text, int textMuted, int textDim, int accent, int accentHover, int accentText, int danger, int success, int warning, int overlay, int shadow) {
   public static final int DEFAULT_ACCENT = -7726052;
   public static final String FORMER_DEFAULT_ACCENT = "#D9805E";
   private static final double INK_CONTRAST = 3.2;
   private static volatile Ink lastInk = new Ink(0, 0, 0);
   public static final List<AccentPreset> ACCENTS = List.of(new AccentPreset("Garnet", -7726052), new AccentPreset("Terracotta", -2523042), new AccentPreset("Ember", -2069429), new AccentPreset("Amber", -2055080), new AccentPreset("Moss", -8670358), new AccentPreset("Mint", -10696536), new AccentPreset("Sky", -10770208), new AccentPreset("Cobalt", -10715910), new AccentPreset("Lavender", -6584608), new AccentPreset("Orchid", -3115319), new AccentPreset("Rose", -1742454), new AccentPreset("CFHGui", -6642252), new AccentPreset("Bone", -2567484));

   public int accentInk() {
      Ink hit = lastInk;
      if (hit.accent == this.accent && hit.bg == this.bg) {
         return hit.ink;
      } else {
         int ink = Colors.legibleOn(this.accent, this.bg, 3.2);
         lastInk = new Ink(this.accent, this.bg, ink);
         return ink;
      }
   }

   public static Palette dark(int accent) {
      return new Palette(-15329771, -15000806, -14540255, -14013912, -13487568, -13421777, -12237761, -1250588, -6185065, -9540506, accent, Colors.hover(accent), Colors.readableOn(accent), -1750963, -10702474, -2055080, -1610612736, 1711276032);
   }

   public static Palette vanilla(int accent) {
      return new Palette(-14803426, -14408668, -12961222, -12105913, -11184811, -16777216, -7631989, -1, -6250336, -9408400, accent, Colors.hover(accent), Colors.readableOn(accent), -43691, -11141291, -22016, -1342177280, 1711276032);
   }

   public Palette withAccent(int accent) {
      return new Palette(this.bg, this.bg2, this.surface, this.surfaceHover, this.surfaceActive, this.border, this.borderStrong, this.text, this.textMuted, this.textDim, accent, Colors.hover(accent), Colors.readableOn(accent), this.danger, this.success, this.warning, this.overlay, this.shadow);
   }

   private static record Ink(int accent, int bg, int ink) {
   }

   public static record AccentPreset(String name, int color) {
   }
}
