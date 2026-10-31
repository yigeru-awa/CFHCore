package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul;

import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.menu.client.creative.CFHGuiCreativeScreen;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.create.OverhaulCreateWorldScreen;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.death.OverhaulDeathScreen;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.pause.OverhaulPauseScreen;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.play.OverhaulPlayScreen;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.play.PlayShots;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.stats.OverhaulStatsScreen;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.OverhaulTitleScreen;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.set.TitleScene;
import com.onehumanawa.cfhcore.content.gui.menu.mixin.JoinMultiplayerScreenAccessor;
import com.onehumanawa.cfhcore.content.gui.menu.mixin.SelectWorldScreenAccessor;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

public final class OverhaulMenus {
   public static void register() {
      ScreenIds.register(OverhaulTitleScreen.class, "cfhgui:title_overhaul", "Main menu (Overhaul)");
      MenuSlots.provide("minecraft:title", Layout.OVERHAUL, (s) -> Minecraft.getInstance().isDemo() ? null : new OverhaulTitleScreen());
      CoreActions.SCREEN_FACTORIES.put("cfhgui:title_overhaul", (p) -> new OverhaulTitleScreen());

      for(TitleScene scene : TitleScene.values()) {
         CoreActions.SCREEN_FACTORIES.put("cfhgui:title_overhaul/" + scene.id(), (p) -> new OverhaulTitleScreen(scene));
      }

      ScreenIds.register(OverhaulPlayScreen.class, "cfhgui:play_overhaul", "Play (Overhaul)");
      MenuSlots.provide("minecraft:select_world", Layout.OVERHAUL, (s) -> new OverhaulPlayScreen(((SelectWorldScreenAccessor)s).cfhgui$lastScreen(), false));
      MenuSlots.provide("minecraft:multiplayer", Layout.OVERHAUL, (s) -> new OverhaulPlayScreen(((JoinMultiplayerScreenAccessor)s).cfhgui$lastScreen(), true));
      CoreActions.SCREEN_FACTORIES.put("cfhgui:play_overhaul", (p) -> new OverhaulPlayScreen(p, false));
      CoreActions.SCREEN_FACTORIES.put("cfhgui:play_overhaul_servers", (p) -> new OverhaulPlayScreen(p, true));
      if (System.getProperty("cfhgui.autoScreens") != null) {
         PlayShots.register();
      }

      ScreenIds.register(OverhaulPauseScreen.class, "cfhgui:pause_overhaul", "Pause menu (Overhaul)");
      MenuSlots.provide("minecraft:pause", Layout.OVERHAUL, (s) -> {
         OverhaulPauseScreen var10000;
         if (s instanceof PauseScreen p) {
            if (p.showsPauseMenu()) {
               var10000 = new OverhaulPauseScreen();
               return var10000;
            }
         }

         var10000 = null;
         return var10000;
      });
      CoreActions.SCREEN_FACTORIES.put("cfhgui:pause_overhaul", (p) -> new OverhaulPauseScreen());
      ScreenIds.register(OverhaulDeathScreen.class, "cfhgui:death_overhaul", "Respawn screen (Overhaul)");
      MenuSlots.provide("minecraft:death", Layout.OVERHAUL, OverhaulDeathScreen::replacing);
      CoreActions.SCREEN_FACTORIES.put("cfhgui:death_overhaul", (p) -> OverhaulDeathScreen.preview(false));
      OverhaulStatsScreen.register();
      MenuSlots.provide("minecraft:creative_inventory", Layout.OVERHAUL, CFHGuiCreativeScreen::replacing);
      ScreenIds.register(OverhaulCreateWorldScreen.class, "cfhgui:create_world", "Create world");
      MenuSlots.provide("minecraft:create_world", Layout.OVERHAUL, (s) -> {
         OverhaulCreateWorldScreen var10000;
         if (s instanceof CreateWorldScreen c) {
            var10000 = new OverhaulCreateWorldScreen(c, true);
         } else {
            var10000 = null;
         }

         return var10000;
      });
      CoreActions.SCREEN_FACTORIES.put("cfhgui:create_world", (p) -> {
         CreateWorldScreen.openFresh(Minecraft.getInstance(), p);
         return Minecraft.getInstance().screen;
      });
      MenuSlots.provide("minecraft:create_world", Layout.CUSTOM, (s) -> {
         OverhaulCreateWorldScreen var10000;
         if (s instanceof CreateWorldScreen c) {
            var10000 = new OverhaulCreateWorldScreen(c, false);
         } else {
            var10000 = null;
         }

         return var10000;
      });
   }

   private OverhaulMenus() {
   }
}
