package com.onehumanawa.cfhcore.content.gui.core.client;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.Actions;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.ServerData.Type;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class CoreActions {
   public static final Map<String, Function<Screen, Screen>> SCREEN_FACTORIES = Collections.synchronizedMap(new LinkedHashMap());

   public static void openScreen(String id) {
      Minecraft mc = Minecraft.getInstance();
      Function<Screen, Screen> f = (Function)SCREEN_FACTORIES.get(id);
      if (f != null) {
         mc.setScreen((Screen)f.apply(mc.screen));
      } else if (id.startsWith("custom:")) {
         mc.setScreen(new CustomScreen(id.substring(7), mc.screen));
      } else {
         CFHGui.LOGGER.warn("[CFHGui] no screen factory for {}", id);
         CFHGuiToasts.show(Component.literal("Unknown screen"), Component.literal(id), Icon.WARNING);
      }
   }

   static void registerAll() {
      Minecraft mc = Minecraft.getInstance();
      Actions.register(new ActionType("cfhgui:open_screen", Component.translatable("cfhgui.action.open_screen"), List.of(ActionType.Arg.screen("screen", Component.translatable("cfhgui.action.arg.screen"))), (a) -> openScreen((String)a.getOrDefault("screen", "minecraft:title"))));
      Actions.register(new ActionType("cfhgui:back", Component.translatable("cfhgui.action.back"), List.of(), (a) -> {
         if (mc.screen != null) {
            mc.screen.onClose();
         }

      }));
      Actions.register(new ActionType("cfhgui:close", Component.translatable("cfhgui.action.close"), List.of(), (a) -> mc.setScreen((Screen)null)));
      Actions.register(new ActionType("cfhgui:open_url", Component.translatable("cfhgui.action.open_url"), List.of(ActionType.Arg.text("url", Component.translatable("cfhgui.action.arg.url"), "https://"), ActionType.Arg.bool("confirm", Component.translatable("cfhgui.action.arg.confirm"), true)), (a) -> {
         String url = (String)a.getOrDefault("url", "");
         if (!url.isBlank()) {
            if (Boolean.parseBoolean((String)a.getOrDefault("confirm", "true"))) {
               Screen prev = mc.screen;
               mc.setScreen(new ConfirmLinkScreen((ok) -> {
                  if (ok) {
                     CFHGuiPlatform.get().openUri(URI.create(url));
                  }

                  mc.setScreen(prev);
               }, url, true));
            } else {
               CFHGuiPlatform.get().openUri(URI.create(url));
            }

         }
      }));
      Actions.register(new ActionType("cfhgui:run_command", Component.translatable("cfhgui.action.run_command"), List.of(ActionType.Arg.text("command", Component.translatable("cfhgui.action.arg.command"), "/help")), (a) -> {
         String cmd = ((String)a.getOrDefault("command", "")).trim();
         if (!cmd.isEmpty() && mc.player != null && mc.getConnection() != null) {
            if (cmd.startsWith("/")) {
               cmd = cmd.substring(1);
            }

            mc.getConnection().sendCommand(cmd);
         }
      }));
      Actions.register(new ActionType("cfhgui:say", Component.translatable("cfhgui.action.say"), List.of(ActionType.Arg.text("message", Component.translatable("cfhgui.action.arg.message"), "")), (a) -> {
         String m = (String)a.getOrDefault("message", "");
         if (!m.isBlank() && mc.getConnection() != null) {
            mc.getConnection().sendChat(m);
         }

      }));
      Actions.register(new ActionType("cfhgui:join_server", Component.translatable("cfhgui.action.join_server"), List.of(ActionType.Arg.text("address", Component.translatable("cfhgui.action.arg.address"), "play.example.com"), ActionType.Arg.text("name", Component.translatable("cfhgui.action.arg.name"), "Server")), (a) -> joinServer((String)a.getOrDefault("address", ""), (String)a.getOrDefault("name", "Server"))));
      Actions.register(new ActionType("cfhgui:quit", Component.translatable("cfhgui.action.quit"), List.of(), (a) -> mc.stop()));
      Actions.register(new ActionType("cfhgui:open_folder", Component.translatable("cfhgui.action.open_folder"), List.of(ActionType.Arg.text("path", Component.translatable("cfhgui.action.arg.path"), "screenshots")), (a) -> {
         Path p = CFHGuiPlatform.get().gameDir().resolve((String)a.getOrDefault("path", "."));
         Util.getPlatform().openPath(p);
      }));
      Actions.register(new ActionType("cfhgui:copy_text", Component.translatable("cfhgui.action.copy_text"), List.of(ActionType.Arg.text("text", Component.translatable("cfhgui.action.arg.text"), "")), (a) -> {
         mc.keyboardHandler.setClipboard((String)a.getOrDefault("text", ""));
         CFHGuiToasts.show(Component.translatable("cfhgui.copied"), (Component)null, Icon.COPY);
      }));
      Actions.register(new ActionType("cfhgui:play_sound", Component.translatable("cfhgui.action.play_sound"), List.of(ActionType.Arg.text("sound", Component.translatable("cfhgui.action.arg.sound"), "minecraft:ui.button.click"), ActionType.Arg.number("pitch", Component.translatable("cfhgui.action.arg.pitch"), "1.0")), (a) -> {
         ResourceLocation rl = ResourceLocation.tryParse((String)a.getOrDefault("sound", ""));
         if (rl != null) {
            float pitch = 1.0F;

            try {
               pitch = Float.parseFloat((String)a.getOrDefault("pitch", "1"));
            } catch (NumberFormatException var5) {
            }

            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(rl), pitch));
         }
      }));
      Actions.register(new ActionType("cfhgui:toggle_fullscreen", Component.translatable("cfhgui.action.toggle_fullscreen"), List.of(), (a) -> {
         mc.getWindow().toggleFullScreen();
         mc.options.fullscreen().set(mc.getWindow().isFullscreen());
         mc.options.save();
      }));
      Actions.register(new ActionType("cfhgui:set_skin", Component.translatable("cfhgui.action.set_skin"), List.of(ActionType.Arg.choice("skin", Component.translatable("cfhgui.action.arg.skin"), "DARK", List.of("DARK"))), (a) -> {
         CFHGui.configFile().update((c) -> c.skin = "DARK");
         Theme.reload();
         CFHGuiSounds.tick();
      }));
      Actions.register(new ActionType("cfhgui:toast", Component.translatable("cfhgui.action.toast"), List.of(ActionType.Arg.text("title", Component.translatable("cfhgui.action.arg.title"), "Hello"), ActionType.Arg.text("body", Component.translatable("cfhgui.action.arg.body"), "")), (a) -> CFHGuiToasts.show(Component.literal((String)a.getOrDefault("title", "")), ((String)a.getOrDefault("body", "")).isEmpty() ? null : Component.literal((String)a.get("body")), Icon.INFO)));
      Actions.register(new ActionType("cfhgui:if_mod_loaded", Component.translatable("cfhgui.action.if_mod_loaded"), List.of(ActionType.Arg.text("mod", Component.translatable("cfhgui.action.arg.mod"), "cfhgui"), ActionType.Arg.text("then", Component.translatable("cfhgui.action.arg.then"), "cfhgui:open_screen"), ActionType.Arg.text("then_arg", Component.translatable("cfhgui.action.arg.then_arg"), "screen=cfhgui:hub")), (a) -> {
         if (CFHGuiPlatform.get().isModLoaded((String)a.getOrDefault("mod", ""))) {
            Actions.run((String)a.getOrDefault("then", ""), parseArgs((String)a.getOrDefault("then_arg", "")));
         }
      }));
      Actions.register(new ActionType("cfhgui:sequence", Component.translatable("cfhgui.action.sequence"), List.of(ActionType.Arg.text("actions", Component.translatable("cfhgui.action.arg.sequence"), "cfhgui:close; cfhgui:open_screen screen=cfhgui:hub")), (a) -> {
         for(String step : ((String)a.getOrDefault("actions", "")).split(";")) {
            String s = step.trim();
            if (!s.isEmpty()) {
               int sp = s.indexOf(32);
               Actions.run(sp < 0 ? s : s.substring(0, sp), sp < 0 ? Map.of() : parseArgs(s.substring(sp + 1)));
            }
         }

      }));
   }

   public static Map<String, String> parseArgs(String s) {
      Map<String, String> m = new LinkedHashMap();

      for(String part : s.trim().split("\\s+")) {
         int eq = part.indexOf(61);
         if (eq > 0) {
            m.put(part.substring(0, eq), part.substring(eq + 1));
         }
      }

      return m;
   }

   public static void joinServer(String address, String name) {
      if (address != null && !address.isBlank()) {
         Minecraft mc = Minecraft.getInstance();
         ServerData data = new ServerData(name != null && !name.isBlank() ? name : address, address, Type.OTHER);
         ConnectScreen.startConnecting((Screen)(mc.screen != null ? mc.screen : new TitleScreen()), mc, ServerAddress.parseString(address), data, false, (TransferState)null);
      }
   }

   private CoreActions() {
   }

   static {
      SCREEN_FACTORIES.put("minecraft:title", (p) -> new TitleScreen());
      SCREEN_FACTORIES.put("minecraft:select_world", SelectWorldScreen::new);
      SCREEN_FACTORIES.put("minecraft:multiplayer", JoinMultiplayerScreen::new);
      SCREEN_FACTORIES.put("minecraft:options", (p) -> new OptionsScreen(p, Minecraft.getInstance().options));
      SCREEN_FACTORIES.put("minecraft:inventory", (p) -> {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null) {
            return p;
         } else if (mc.gameMode != null && mc.gameMode.hasInfiniteItems()) {
            mc.player.connection.sendCommand("gamemode survival");
            return p;
         } else {
            return new InventoryScreen(mc.player);
         }
      });
      SCREEN_FACTORIES.put("minecraft:creative_inventory", (p) -> {
         Minecraft mc = Minecraft.getInstance();
         return (Screen)(mc.player != null && mc.player.connection != null ? new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), (Boolean)mc.options.operatorItemsTab().get()) : p);
      });
      SCREEN_FACTORIES.put("cfhgui:hub", CFHGuiHubScreen::new);
      SCREEN_FACTORIES.put("cfhgui:settings", CoreSettingsScreen::new);
   }
}
