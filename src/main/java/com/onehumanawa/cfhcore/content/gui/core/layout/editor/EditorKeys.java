package com.onehumanawa.cfhcore.content.gui.core.layout.editor;

import net.minecraft.client.gui.screens.Screen;

final class EditorKeys {
   static final int ESCAPE = 256;
   static final int ENTER = 257;
   static final int TAB = 258;
   static final int BACKSPACE = 259;
   static final int DELETE = 261;
   static final int RIGHT = 262;
   static final int LEFT = 263;
   static final int DOWN = 264;
   static final int UP = 265;
   static final int HOME = 268;
   static final int END = 269;
   static final int F1 = 290;
   static final int F12 = 301;
   static final int KP_ENTER = 335;
   static final int A = 65;
   static final int C = 67;
   static final int D = 68;
   static final int G = 71;
   static final int L = 76;
   static final int P = 80;
   static final int S = 83;
   static final int V = 86;
   static final int Y = 89;
   static final int Z = 90;

   static boolean ctrl() {
      return Screen.hasControlDown();
   }

   static boolean shift() {
      return Screen.hasShiftDown();
   }

   static boolean alt() {
      return Screen.hasAltDown();
   }

   static boolean isEnter(int key) {
      return key == 257 || key == 335;
   }

   private EditorKeys() {
   }
}
