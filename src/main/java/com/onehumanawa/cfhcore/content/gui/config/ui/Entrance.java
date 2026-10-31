package com.onehumanawa.cfhcore.content.gui.config.ui;

import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCard;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;

public final class Entrance {
   public static int play(GuiEventListener w, int index) {
      int delay = Math.min(200, index * 18);
      if (w instanceof OptionRow r) {
         r.playEntrance(delay);
         return index + 1;
      } else if (w instanceof CFHGuiWidget sw) {
         sw.playEntrance(delay);
         return index + 1;
      } else if (w instanceof CFHGuiCard c) {
         c.playEntrance(delay);
         return index + 1;
      } else {
         int i = index;
         if (w instanceof ContainerEventHandler c) {
            for(GuiEventListener child : c.children()) {
               i = play(child, i);
            }
         }

         return i;
      }
   }

   private Entrance() {
   }
}
