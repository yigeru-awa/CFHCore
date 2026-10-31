package com.onehumanawa.cfhcore.content.gui.menu.client.lookat;

import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSeparator;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;

public final class LookAtOptions {
   private LookAtOptions() {
   }

   public static List<AbstractWidget> rows(int width, Runnable rebuild) {
      List<AbstractWidget> out = new ArrayList();

      for(LookAtSettings.Group group : LookAtSettings.groups()) {
         out.add(new CFHGuiSeparator(0, 0, width, group.pageTitle()));

         for(LookAtSettings.Setting s : group.settings()) {
            CFHGuiToggle toggle = new CFHGuiToggle(0, 0, width, s.label(), s.get(), (v) -> {
               s.set(v);
               if (s.structural) {
                  rebuild.run();
               }

            });
            toggle.tip(s.tooltip());
            toggle.enabled(s.enabled());
            out.add(toggle);
         }
      }

      return out;
   }
}
