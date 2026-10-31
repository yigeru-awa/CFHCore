package com.onehumanawa.cfhcore.content.gui.menu.client.stats;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.client.Minecraft;

final class StatsShots {
   static void register() {
      for(String skin : new String[]{"DARK", "VANILLA"}) {
         CoreActions.SCREEN_FACTORIES.put("cfhgui:stats_shot/skin/" + skin.toLowerCase(Locale.ROOT), (parent) -> {
            CFHGui.config().skin = skin;
            Theme.reload();
            Reskin.invalidate();
            return parent;
         });
      }

      for(String size : System.getProperty("cfhgui.stats.shotSizes", "854x480,1280x720,1920x1080").split(",")) {
         String[] wh = size.trim().toLowerCase(Locale.ROOT).split("x");
         if (wh.length == 2) {
            int w;
            int h;
            try {
               w = Integer.parseInt(wh[0].trim());
               h = Integer.parseInt(wh[1].trim());
            } catch (NumberFormatException var8) {
               continue;
            }

            CoreActions.SCREEN_FACTORIES.put("cfhgui:stats_shot/size/" + w + "x" + h, (parent) -> {
               Minecraft.getInstance().getWindow().setWindowed(w, h);
               return parent;
            });
         }
      }

      for(int scale = 0; scale <= 4; ++scale) {
          int finalScale = scale;
          CoreActions.SCREEN_FACTORIES.put("cfhgui:stats_shot/scale/" + scale, (parent) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.guiScale().set(finalScale);
            mc.resizeDisplay();
            return parent;
         });
      }

   }

   private StatsShots() {
   }
}
