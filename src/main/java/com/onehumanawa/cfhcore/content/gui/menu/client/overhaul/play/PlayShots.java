package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.play;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

public final class PlayShots {
   public static void register() {
      for(String skin : new String[]{"DARK", "VANILLA"}) {
         CoreActions.SCREEN_FACTORIES.put("cfhgui:shots/skin_" + skin.toLowerCase(Locale.ROOT), (p) -> {
            CFHGui.config().skin = skin;
            Theme.reload();
            Reskin.invalidate();
            return new TitleScreen();
         });
      }

      for(String size : System.getProperty("cfhgui.shots.windows", "").split("[+]")) {
         String[] wh = size.trim().toLowerCase(Locale.ROOT).split("x");
         if (wh.length == 2) {
            try {
               int w = Integer.parseInt(wh[0].trim());
               int h = Integer.parseInt(wh[1].trim());
               if (w >= 320 && h >= 240) {
                  CoreActions.SCREEN_FACTORIES.put("cfhgui:shots/window_" + w + "x" + h, (p) -> {
                     Minecraft.getInstance().getWindow().setWindowed(w, h);
                     return new TitleScreen();
                  });
               }
            } catch (NumberFormatException var7) {
            }
         }
      }

   }

   private PlayShots() {
   }
}
