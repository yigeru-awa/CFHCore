package com.onehumanawa.cfhcore.content.gui.menu.client.infohud;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;

final class Seg {
   private static final List<Seg> ALL = new ArrayList();
   String s = "";
   int w;
   final BakedText baked = new BakedText();

   Seg() {
      ALL.add(this);
   }

   void set(String text, Font font) {
      if (!text.equals(this.s)) {
         this.s = text;
         this.w = font.width(text);
      }
   }

   void clear() {
      this.s = "";
      this.w = 0;
   }

   boolean empty() {
      return this.s.isEmpty();
   }

   static void remeasure(Font font) {
      for(int i = 0; i < ALL.size(); ++i) {
         Seg seg = (Seg)ALL.get(i);
         seg.w = seg.s.isEmpty() ? 0 : font.width(seg.s);
      }

   }
}
