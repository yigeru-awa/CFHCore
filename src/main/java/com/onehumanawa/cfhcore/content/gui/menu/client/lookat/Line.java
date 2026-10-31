package com.onehumanawa.cfhcore.content.gui.menu.client.lookat;

import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

final class Line {
   String s = "";
   FormattedCharSequence seq;
   int w;

   Line() {
      this.seq = FormattedCharSequence.EMPTY;
   }

   boolean set(String text, Font font) {
      if (text.equals(this.s)) {
         return false;
      } else {
         this.s = text;
         this.seq = text.isEmpty() ? FormattedCharSequence.EMPTY : Language.getInstance().getVisualOrder(FormattedText.of(text));
         this.w = text.isEmpty() ? 0 : font.width(this.seq);
         return true;
      }
   }

   boolean set(Component text, String plain, Font font, int room) {
      if (plain.equals(this.s)) {
         return false;
      } else {
         this.s = plain;
         this.seq = plain.isEmpty() ? FormattedCharSequence.EMPTY : CFHGuiDraw.truncate(text, room);
         this.w = plain.isEmpty() ? 0 : font.width(this.seq);
         return true;
      }
   }

   boolean clear() {
      if (this.s.isEmpty()) {
         return false;
      } else {
         this.s = "";
         this.seq = FormattedCharSequence.EMPTY;
         this.w = 0;
         return true;
      }
   }

   boolean empty() {
      return this.s.isEmpty();
   }
}
