package com.onehumanawa.cfhcore.content.gui.menu.client;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType.Arg;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.WorldActions;
import com.onehumanawa.cfhcore.content.gui.menu.client.screenshots.CFHGuiScreenshotsScreen;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

public final class MenuActions {
   public static List<ActionType> all() {
      return List.of(new ActionType("cfhgui:open_screenshots", Component.translatable("cfhgui.action.open_screenshots"), List.of(), (a) -> {
         Minecraft mc = Minecraft.getInstance();
         mc.setScreen(new CFHGuiScreenshotsScreen(mc.screen));
      }), new ActionType("cfhgui:open_worlds", Component.translatable("cfhgui.action.open_worlds"), List.of(), (a) -> {
         Minecraft mc = Minecraft.getInstance();
         mc.setScreen(new SelectWorldScreen(mc.screen));
      }), new ActionType("cfhgui:open_servers", Component.translatable("cfhgui.action.open_servers"), List.of(), (a) -> {
         Minecraft mc = Minecraft.getInstance();
         mc.setScreen(new JoinMultiplayerScreen(mc.screen));
      }), new ActionType("cfhgui:open_options", Component.translatable("cfhgui.action.open_options"), List.of(), (a) -> {
         Minecraft mc = Minecraft.getInstance();
         mc.setScreen(new OptionsScreen(mc.screen, mc.options));
      }), new ActionType("cfhgui:play_last", Component.translatable("cfhgui.action.play_last"), List.of(), (a) -> LastPlayed.quick().ifPresentOrElse((t) -> LastPlayed.play(t, Minecraft.getInstance().screen), () -> CFHGuiToasts.show(Component.translatable("cfhgui.title.nothing_yet"), Component.translatable("cfhgui.title.nothing_yet_hint"), Icon.INFO))), new ActionType("cfhgui:open_world", Component.translatable("cfhgui.action.open_world"), List.of(Arg.text("folder", Component.translatable("cfhgui.action.arg.folder"), "New World")), (a) -> WorldActions.play((String)a.getOrDefault("folder", ""), Minecraft.getInstance().screen)));
   }

   private MenuActions() {
   }
}
